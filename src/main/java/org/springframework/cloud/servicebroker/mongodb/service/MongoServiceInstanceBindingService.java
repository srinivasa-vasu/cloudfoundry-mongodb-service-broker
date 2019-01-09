package org.springframework.cloud.servicebroker.mongodb.service;

import static org.springframework.cloud.servicebroker.model.instance.OperationState.SUCCEEDED;
import static org.springframework.credhub.support.WriteMode.OVERWRITE;
import static org.springframework.credhub.support.permissions.Operation.READ;

import java.util.Collections;
import java.util.HashMap;
import java.util.Optional;

import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.servicebroker.exception.ServiceInstanceBindingDoesNotExistException;
import org.springframework.cloud.servicebroker.exception.ServiceInstanceBindingExistsException;
import org.springframework.cloud.servicebroker.model.binding.*;
import org.springframework.cloud.servicebroker.mongodb.model.ServiceInstanceBinding;
import org.springframework.cloud.servicebroker.mongodb.repository.MongoServiceInstanceBindingRepository;
import org.springframework.cloud.servicebroker.service.ServiceInstanceBindingService;
import org.springframework.credhub.core.CredHubOperations;
import org.springframework.credhub.support.CredentialDetails;
import org.springframework.credhub.support.CredentialName;
import org.springframework.credhub.support.ServiceInstanceCredentialName;
import org.springframework.credhub.support.json.JsonCredential;
import org.springframework.credhub.support.json.JsonCredentialRequest;
import org.springframework.credhub.support.permissions.Permission;
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

	private final MongoAdminService mongo;

	private final MongoServiceInstanceBindingRepository bindingRepository;

	private final CredHubOperations credHubOperations;

	private static final Logger LOGGER = LoggerFactory
			.getLogger(MongoServiceInstanceBindingService.class);

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

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("createServiceInstanceBinding: IN");
		}

		String bindingId = request.getBindingId();
		String serviceInstanceId = request.getServiceInstanceId();

		Optional<ServiceInstanceBinding> binding = bindingRepository.findById(bindingId);
		binding.ifPresent(serviceInstanceBinding -> {
			throw new ServiceInstanceBindingExistsException(serviceInstanceId, bindingId);
		});

		String password = RandomStringUtils.randomAlphanumeric(25);

		// TODO check if user already exists in the DB

		mongo.createUser(serviceInstanceId, bindingId, password);

		CredentialName credName = ServiceInstanceCredentialName.builder()
				.serviceBrokerName(request.getServiceInstanceId())
				.serviceOfferingName(request.getPlanId())
				.serviceBindingId(request.getBindingId())
				.credentialName(request.getBindingId()).build();

		// @formatter:off
		JsonCredentialRequest credhubRequest = JsonCredentialRequest.builder()
				.value(Collections.singletonMap("uri",
                 mongo.getConnectionString(serviceInstanceId, bindingId, password)))
				.name(credName).build();
        // @formatter:on

		CredentialDetails<JsonCredential> credhubResponse = credHubOperations
				.credentials().write(credhubRequest);
		credHubOperations.permissions().addPermissions(credName, Permission.builder()
				.app(request.getBindResource().getAppGuid()).operations(READ).build());

		LOGGER.info("Credhub response: " + credhubResponse.getId());

		bindingRepository.save(new ServiceInstanceBinding(bindingId, serviceInstanceId,
				credhubResponse.getValue(), null, request.getBindResource().getAppGuid()));

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("createServiceInstanceBinding: OUT");
		}

		return CreateServiceInstanceAppBindingResponse.builder()
				.credentials(new HashMap<String, Object>() {
					{
						put("credhub-ref", credhubResponse.getName().getName());
					}
				}).operation(String.valueOf(SUCCEEDED)).build();
	}

	@Override
	public DeleteServiceInstanceBindingResponse deleteServiceInstanceBinding(
			DeleteServiceInstanceBindingRequest request) {
		String bindingId = request.getBindingId();

		Optional<ServiceInstanceBinding> binding = bindingRepository.findById(bindingId);
		if(!binding.isPresent()){
			throw new ServiceInstanceBindingDoesNotExistException(bindingId);
		}

		mongo.deleteUser(binding.get().getServiceInstanceId(), bindingId);
		credHubOperations.credentials().deleteByName(ServiceInstanceCredentialName.builder()
				.serviceBrokerName(request.getServiceInstanceId())
				.serviceOfferingName(request.getPlanId())
				.serviceBindingId(request.getBindingId())
				.credentialName(request.getBindingId()).build());
		bindingRepository.deleteById(bindingId);
		return DeleteServiceInstanceBindingResponse.builder()
				.operation(String.valueOf(SUCCEEDED)).build();
	}

}
