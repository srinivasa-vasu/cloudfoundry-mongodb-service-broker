package org.springframework.cloud.servicebroker.mongodb.service;

import static org.springframework.cloud.servicebroker.model.OperationState.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.PreDestroy;

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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

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
				ServiceInstanceParams objInstance = new ServiceInstanceParams(request,
						config);
				if (Optional.ofNullable(objInstance.getClusterName()).isPresent()
						&& Optional.ofNullable(objInstance.getIdentity()).isPresent()) {
					ServiceKey key = getClusterParams(objInstance, request);
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

	private ServiceKey getClusterParams(ServiceInstanceParams objInstance,
			CreateServiceInstanceRequest request) {
		ServiceKey key = null;
		ObjectMapper mapper = new ObjectMapper();
		String apiEndPoint = "https://" + request.getApiInfoLocation().substring(0,
				request.getApiInfoLocation().indexOf("/"));
		String serviceListAPI = apiEndPoint + "/v2/service_instances?q=name:"
				+ objInstance.getClusterName();
		String createServiceKeyAPI = apiEndPoint + "/v2/service_keys";
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set("Authorization", "Bearer " + objInstance.getIdentity());
		Optional<ServiceList> serviceObj = Optional
				.ofNullable(
						apiService
								.exchange(serviceListAPI, HttpMethod.GET,
										new HttpEntity(headers), ServiceList.class)
								.getBody());
		if (serviceObj.isPresent() && serviceObj.get().getTotalResults() > 0) {
			JsonNode body = mapper.createObjectNode();
			((ObjectNode) body).put("service_instance_guid",
					serviceObj.get().getResources().get(0).getMetadata().getGuid());
			((ObjectNode) body).put("name", "key-" + System.currentTimeMillis());
			key = apiService.exchange(createServiceKeyAPI, HttpMethod.POST,
					new HttpEntity<Object>(body.toString(), headers), ServiceKey.class)
					.getBody();
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
				operationStatus.put(instanceId, SUCCEEDED);
			}
			catch (Exception ex) {
				operationStatus.put(instanceId, FAILED);
				throw ex;
			}
		}));
		return new DeleteServiceInstanceResponse().withAsync(true);
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