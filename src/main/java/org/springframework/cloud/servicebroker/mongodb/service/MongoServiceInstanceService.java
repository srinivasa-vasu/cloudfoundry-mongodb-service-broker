package org.springframework.cloud.servicebroker.mongodb.service;

import static org.springframework.cloud.servicebroker.model.OperationState.*;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.POST;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.PreDestroy;

import org.apache.tomcat.util.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.servicebroker.exception.ServiceBrokerException;
import org.springframework.cloud.servicebroker.exception.ServiceInstanceDoesNotExistException;
import org.springframework.cloud.servicebroker.model.*;
import org.springframework.cloud.servicebroker.mongodb.config.MongoConfig;
import org.springframework.cloud.servicebroker.mongodb.dto.ServiceKey;
import org.springframework.cloud.servicebroker.mongodb.dto.ServiceList;
import org.springframework.cloud.servicebroker.mongodb.exception.MongoServiceException;
import org.springframework.cloud.servicebroker.mongodb.model.ServiceInstance;
import org.springframework.cloud.servicebroker.mongodb.model.ServiceInstanceParams;
import org.springframework.cloud.servicebroker.mongodb.repository.MongoServiceInstanceRepository;
import org.springframework.cloud.servicebroker.service.ServiceInstanceService;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mongodb.client.MongoDatabase;

import freemarker.template.TemplateException;

/**
 * Mongo impl to manage service instances. Creating a service does the following: creates
 * a new database, saves the ServiceInstance info to the Mongo repository.
 * 
 * @author sgreenberg@pivotal.io
 */
@Service
public class MongoServiceInstanceService implements ServiceInstanceService {

	private MongoAdminService mongo;

	private MongoServiceInstanceRepository repository;

	private MongoK8sService k8sService;

	private MongoConfig config;

	private APIService apiService;

	private static final Logger LOGGER = LoggerFactory
			.getLogger(MongoServiceInstanceService.class);

	private final ExecutorService servicePool = Executors.newFixedThreadPool(10);

	private final Map<String, OperationState> operationStatus = new HashMap<>();

	private static final String SERVICE_INSTANCE = "/v2/service_instances";
	private static final String SERVICE_KEY = "/v2/service_keys";

	@Autowired
	public MongoServiceInstanceService(MongoAdminService mongo,
			MongoServiceInstanceRepository repository, MongoK8sService k8sService,
			MongoConfig config, APIService apiService) {
		this.mongo = mongo;
		this.repository = repository;
		this.k8sService = k8sService;
		this.config = config;
		this.apiService = apiService;
	}

	@Override
	public CreateServiceInstanceResponse createServiceInstance(
			final CreateServiceInstanceRequest request) {
		// TODO MongoDB dashboard
		operationStatus.put(request.getServiceInstanceId(), IN_PROGRESS);
		servicePool.execute(new Thread(() -> {
			try {
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("Initializing service instance id: "
							+ request.getServiceInstanceId());
				}
				ServiceInstanceParams objInstance = getServiceInstanceParams(request,
						config);
				if (Optional.ofNullable(objInstance.getClusterName()).isPresent()
						&& Optional.ofNullable(objInstance.getIdentity()).isPresent()) {
					ServiceKey key = processClusterParams(objInstance);
					objInstance.populateServiceParams(key);
				}
				objInstance.validateInputParams(request);
				ServiceInstance instance = new ServiceInstance(request, objInstance);
				if (k8sService.createK8sObjects(objInstance)) {
					if (LOGGER.isDebugEnabled()) {
						LOGGER.debug("K8s mongo objects created for instance id: "
								+ request.getServiceInstanceId());
					}
					// ServiceInstance instance =
					// repository.findOne(request.getServiceInstanceId());
					// if (instance != null) {
					// throw new
					// ServiceInstanceExistsException(request.getServiceInstanceId(),
					// request.getServiceDefinitionId());
					// }
					if (mongo.databaseExists(instance.getServiceInstanceId())) {
						// ensure the instance is empty
						mongo.deleteDatabase(instance.getServiceInstanceId());
					}
					MongoDatabase db = mongo
							.createDatabase(instance.getServiceInstanceId());
					if (db == null) {
						throw new MongoServiceException(
								"unable to create mongo database instance");
					}
					repository.save(instance);
					operationStatus.put(request.getServiceInstanceId(),
							OperationState.SUCCEEDED);
					if (LOGGER.isDebugEnabled()) {
						LOGGER.debug("Successfully created the instance id: "
								+ request.getServiceInstanceId());
					}
				}
				else {
					// remove objects if got created partially
					k8sService.deleteK8sObjects(objInstance);
					throw new MongoServiceException("unable to create mongo k8s objects");
				}
			}
			catch (IOException | InterruptedException | TemplateException
					| MongoServiceException ex) {
				operationStatus.put(request.getServiceInstanceId(), FAILED);
				throw new ServiceBrokerException("Failed to create new DB instance: "
						+ ex.getMessage() + ": " + request.getServiceInstanceId(), ex);
			}
		}));
		return new CreateServiceInstanceResponse().withAsync(true);
	}

	private ServiceInstanceParams getServiceInstanceParams(
			CreateServiceInstanceRequest request, MongoConfig config) {
		ServiceInstanceParams instance = new ServiceInstanceParams(request, config);
		if (Optional.ofNullable(config.getCfUser()).isPresent()
				&& Optional.ofNullable(config.getCfPassword()).isPresent()) {
			Optional<String> accessToken = Optional
					.ofNullable(getCfAccessToken(instance, config));
			if (accessToken.isPresent()) {
				instance.setIdentity(accessToken.get());
			}
			else {
				throw new MongoServiceException("Unable to retrieve cf access token");
			}
		}
		return instance;
	}

	private String getCfAccessToken(ServiceInstanceParams instance, MongoConfig config) {
		String accessToken = null;
		try {
			ObjectMapper mapper = new ObjectMapper();
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
			String authorization = "cf" + ":" + "";
			headers.set("Authorization", "Basic " + new String(Base64
					.encodeBase64(authorization.getBytes(Charset.forName("US-ASCII")))));
			MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
			body.add("username", config.getCfUser());
			body.add("password", config.getCfPassword());
			body.add("client_id", "cf");
			body.add("grant_type", "password");
			body.add("response_type", "token");
			ResponseEntity<String> output = apiService.exchange(instance.getCfUaaAPI(),
					POST, new HttpEntity<MultiValueMap>(body, headers), String.class);
			if (output.getStatusCode().is2xxSuccessful()) {
				accessToken = mapper.readTree(output.getBody()).get("access_token")
						.textValue();
			}
			else {
				throw new IOException("Unable to get access token: " + output.getBody());
			}
		}
		catch (IOException e) {
			LOGGER.error("Unable to get access token", e);
		}
		return accessToken;
	}

	private ServiceKey processClusterParams(ServiceInstanceParams objInstance) {
		ServiceKey key;
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set("Authorization", "Bearer " + objInstance.getIdentity());
		Optional<ServiceList> serviceObj = Optional.ofNullable(apiService.exchange(
				objInstance.getCfAPI() + SERVICE_INSTANCE + "?q=name:"
						+ objInstance.getClusterName(),
				HttpMethod.GET, new HttpEntity(headers), ServiceList.class).getBody());
		if (serviceObj.isPresent() && serviceObj.get().getTotalResults() > 0) {
			JsonNode body = new ObjectMapper().createObjectNode();
			((ObjectNode) body).put("service_instance_guid",
					serviceObj.get().getResources().get(0).getMetadata().getGuid());
			((ObjectNode) body).put("name", "key-" + objInstance.getUnq());
			key = apiService.exchange(objInstance.getCfAPI() + SERVICE_KEY, POST,
					new HttpEntity<Object>(body.toString(), headers), ServiceKey.class)
					.getBody();
		}
		else {
			throw new MongoServiceException(
					"CF service instance doesn't exist: " + objInstance.getClusterName());
		}
		return key;
	}

	@Override
	public GetLastServiceOperationResponse getLastOperation(
			GetLastServiceOperationRequest request) {
		OperationState state = operationStatus.get(request.getServiceInstanceId());
		if (state == IN_PROGRESS) {
			return new GetLastServiceOperationResponse().withOperationState(state);
		}
		operationStatus.remove(request.getServiceInstanceId());
		return new GetLastServiceOperationResponse().withOperationState(state);
	}

	ServiceInstance getServiceInstance(String id) {
		return repository.findOne(id);
	}

	@Override
	public DeleteServiceInstanceResponse deleteServiceInstance(
			DeleteServiceInstanceRequest request) throws MongoServiceException {
		operationStatus.put(request.getServiceInstanceId(), IN_PROGRESS);
		servicePool.execute(new Thread(() -> {
			String instanceId = request.getServiceInstanceId();
			try {
				ServiceInstance instance = repository.findOne(instanceId);
				if (instance == null) {
					throw new ServiceInstanceDoesNotExistException(instanceId);
				}
				mongo.deleteDatabase(instanceId);
				repository.delete(instanceId);
				k8sService.deleteK8sObjects(instance.getInstanceParams());
				if (instance.getInstanceParams().isAutoMode()) {
					deleteServiceKey(instance.getInstanceParams());
				}
				operationStatus.put(instanceId, SUCCEEDED);
			}
			catch (Exception ex) {
				operationStatus.put(instanceId, FAILED);
				throw ex;
			}
		}));
		return new DeleteServiceInstanceResponse().withAsync(true);
	}

	private void deleteServiceKey(ServiceInstanceParams objInstance) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set("Authorization", "Bearer " + getCfAccessToken(objInstance, config));
		ResponseEntity<String> response = apiService
				.exchange(
						objInstance.getCfAPI() + SERVICE_KEY + "/"
								+ objInstance.getServiceKey() + "?",
						DELETE, new HttpEntity<>(headers), String.class);
		if (!response.getStatusCode().is2xxSuccessful()) {
			throw new MongoServiceException("Unable to delete the CF service key: "
					+ objInstance.getServiceKey());
		}
	}

	@Override
	public UpdateServiceInstanceResponse updateServiceInstance(
			UpdateServiceInstanceRequest request) {
		// not implemented
		String instanceId = request.getServiceInstanceId();
		ServiceInstance instance = repository.findOne(instanceId);
		if (instance == null) {
			throw new ServiceInstanceDoesNotExistException(instanceId);
		}
		repository.delete(instanceId);
		ServiceInstance updatedInstance = new ServiceInstance(request);
		repository.save(updatedInstance);
		return new UpdateServiceInstanceResponse();
	}

	@PreDestroy
	public void shutdown() {
		servicePool.shutdown();
	}

}