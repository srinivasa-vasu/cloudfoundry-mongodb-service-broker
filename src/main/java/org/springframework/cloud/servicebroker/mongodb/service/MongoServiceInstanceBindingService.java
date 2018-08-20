package org.springframework.cloud.servicebroker.mongodb.service;

import static org.springframework.credhub.support.permissions.Operation.READ;

import java.util.Collections;
import java.util.HashMap;

import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.servicebroker.exception.ServiceInstanceBindingDoesNotExistException;
import org.springframework.cloud.servicebroker.exception.ServiceInstanceBindingExistsException;
import org.springframework.cloud.servicebroker.model.CreateServiceInstanceAppBindingResponse;
import org.springframework.cloud.servicebroker.model.CreateServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.CreateServiceInstanceBindingResponse;
import org.springframework.cloud.servicebroker.model.DeleteServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.mongodb.model.ServiceInstanceBinding;
import org.springframework.cloud.servicebroker.mongodb.repository.MongoServiceInstanceBindingRepository;
import org.springframework.cloud.servicebroker.service.ServiceInstanceBindingService;
import org.springframework.credhub.core.CredHubOperations;
import org.springframework.credhub.support.CredentialDetails;
import org.springframework.credhub.support.ServiceInstanceCredentialName;
import org.springframework.credhub.support.json.JsonCredential;
import org.springframework.credhub.support.json.JsonCredentialRequest;
import org.springframework.credhub.support.permissions.CredentialPermission;
import org.springframework.stereotype.Service;

/**
 * Mongo impl to bind services. Binding a service does the following: creates a new user
 * in the database (currently uses a default pwd of "password"), saves the
 * ServiceInstanceBinding info to the Mongo repository.
 * 
 * @author sgreenberg@pivotal.io
 */
@Service
public class MongoServiceInstanceBindingService implements ServiceInstanceBindingService {

	private MongoAdminService mongo;

	private MongoServiceInstanceBindingRepository bindingRepository;

	private CredHubOperations credHubOperations;

	private static final Logger LOGGER = LoggerFactory
			.getLogger(MongoServiceInstanceBindingService.class);

	@Autowired
	public MongoServiceInstanceBindingService(MongoAdminService mongo,
			MongoServiceInstanceBindingRepository bindingRepository,
			CredHubOperations credHubOperations) {
		this.mongo = mongo;
		this.bindingRepository = bindingRepository;
		this.credHubOperations = credHubOperations;
	}

	@Override
	public CreateServiceInstanceBindingResponse createServiceInstanceBinding(
			CreateServiceInstanceBindingRequest request) {

		String bindingId = request.getBindingId();
		String serviceInstanceId = request.getServiceInstanceId();

		ServiceInstanceBinding binding = bindingRepository.findOne(bindingId);
		if (binding != null) {
			throw new ServiceInstanceBindingExistsException(serviceInstanceId, bindingId);
		}

		String database = serviceInstanceId;
		String username = bindingId;
		String password = RandomStringUtils.randomAlphanumeric(25);

		// TODO check if user already exists in the DB

		mongo.createUser(database, username, password);

		// @formatter:off
		JsonCredentialRequest credhubRequest = JsonCredentialRequest.builder()
                .overwrite(true)
				.value(Collections.singletonMap("uri",
                (Object) mongo.getConnectionString(database, username, password)))
				.permission(CredentialPermission.builder().app(request.getBindResource().getAppGuid())
                .operations(READ).build())
				.name(ServiceInstanceCredentialName.builder()
                .serviceBrokerName(request.getServiceInstanceId())
                .serviceOfferingName(request.getPlanId())
                .serviceBindingId(request.getBindingId())
                .credentialName(request.getBindingId()).build())
				.build();
        // @formatter:on

		CredentialDetails<JsonCredential> credhubResponse = credHubOperations
				.write(credhubRequest);

		binding = new ServiceInstanceBinding(bindingId, serviceInstanceId,
				credhubResponse.getValue(), null, request.getBindResource().getAppGuid());
		bindingRepository.save(binding);

		return new CreateServiceInstanceAppBindingResponse()
				.withCredentials(new HashMap<String, Object>() {
					{
						put("credhub-ref", credhubResponse.getName().getName());
					}
				});
	}

	@Override
	public void deleteServiceInstanceBinding(
			DeleteServiceInstanceBindingRequest request) {
		String bindingId = request.getBindingId();
		ServiceInstanceBinding binding = getServiceInstanceBinding(bindingId);

		if (binding == null) {
			throw new ServiceInstanceBindingDoesNotExistException(bindingId);
		}

		mongo.deleteUser(binding.getServiceInstanceId(), bindingId);
		credHubOperations.deleteByName(ServiceInstanceCredentialName.builder()
				.serviceBrokerName(request.getServiceInstanceId())
				.serviceOfferingName(request.getPlanId())
				.serviceBindingId(request.getBindingId())
				.credentialName(request.getBindingId()).build());
		bindingRepository.delete(bindingId);
	}

	private ServiceInstanceBinding getServiceInstanceBinding(String id) {
		return bindingRepository.findOne(id);
	}

	//@formatter:off
/*
	public CredentialDetails<UserParametersRequest> generateCredential(
			CreateServiceInstanceBindingRequest bindingRequest) {
		UserParametersRequest request = UserParametersRequest.builder().overwrite(true)
				.name(ServiceInstanceCredentialName.builder()
						.serviceBrokerName(bindingRequest.getServiceInstanceId())
						.serviceOfferingName(bindingRequest.getPlanId())
						.serviceBindingId(bindingRequest.getBindingId())
						.credentialName("userparameter").build())
				.username(bindingRequest.getBindingId())
				.parameters(PasswordParameters.builder().length(20).excludeLower(false)
						.excludeUpper(false).excludeNumber(false).includeSpecial(true)
						.build())
				.build();
		return credHubOperations.generate(request);
	}
*/
    //@formatter:on

}
