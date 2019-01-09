package org.springframework.cloud.servicebroker.mongodb.service;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.servicebroker.exception.ServiceBrokerException;
import org.springframework.cloud.servicebroker.exception.ServiceInstanceBindingDoesNotExistException;
import org.springframework.cloud.servicebroker.exception.ServiceInstanceBindingExistsException;
import org.springframework.cloud.servicebroker.model.binding.BindResource;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceAppBindingResponse;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.binding.DeleteServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.mongodb.IntegrationTestBase;
import org.springframework.cloud.servicebroker.mongodb.exception.MongoServiceException;
import org.springframework.cloud.servicebroker.mongodb.fixture.ServiceInstanceBindingFixture;
import org.springframework.cloud.servicebroker.mongodb.fixture.ServiceInstanceFixture;
import org.springframework.cloud.servicebroker.mongodb.model.ServiceInstance;
import org.springframework.cloud.servicebroker.mongodb.model.ServiceInstanceBinding;
import org.springframework.cloud.servicebroker.mongodb.repository.MongoServiceInstanceBindingRepository;

import com.mongodb.MongoClient;

public class MongoServiceInstanceBindingServiceTest extends IntegrationTestBase {

	@Autowired
	private MongoClient client;

	@Mock
	private MongoAdminService mongo;

	@Mock
	private MongoServiceInstanceBindingRepository repository;

	private MongoServiceInstanceBindingService service;

	private ServiceInstance instance;
	private ServiceInstanceBinding instanceBinding;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		service = new MongoServiceInstanceBindingService(mongo, repository, null);
		instance = ServiceInstanceFixture.getServiceInstance();
		instanceBinding = ServiceInstanceBindingFixture.getServiceInstanceBinding();
	}

	@After
	public void cleanup() {
		client.dropDatabase(DB_NAME);
	}

	// TODO Test if user already exists

	@Test
	public void newServiceInstanceBindingCreatedSuccessfully() throws Exception {

		when(repository.findById(any(String.class)).get()).thenReturn(null);
		// when(mongo.createUser(any(String.class), any(String.class),
		// any(String.class))).thenReturn(true);

		CreateServiceInstanceAppBindingResponse response = (CreateServiceInstanceAppBindingResponse) service
				.createServiceInstanceBinding(buildCreateRequest());

		assertNotNull(response);
		assertNotNull(response.getCredentials());
		assertNull(response.getSyslogDrainUrl());

		verify(repository).save(isA(ServiceInstanceBinding.class));
	}

	@Test(expected = ServiceInstanceBindingExistsException.class)
	public void serviceInstanceCreationFailsWithExistingInstance() throws Exception {

		when(repository.findById(any(String.class)).get())
				.thenReturn(ServiceInstanceBindingFixture.getServiceInstanceBinding());

		service.createServiceInstanceBinding(buildCreateRequest());
	}

	@Test(expected = ServiceBrokerException.class)
	public void serviceInstanceBindingCreationFailsWithUserCreationFailure()
			throws Exception {
		when(repository.findById(any(String.class)).get()).thenReturn(null);
		doThrow(new MongoServiceException("fail")).when(mongo)
				.createUser(any(String.class), any(String.class), any(String.class));

		service.createServiceInstanceBinding(buildCreateRequest());
	}

	/*
	 * @Test public void successfullyRetrieveServiceInstanceBinding() {
	 * ServiceInstanceBinding binding =
	 * ServiceInstanceBindingFixture.getServiceInstanceBinding();
	 * when(repository.findById(any(String.class)).get()).thenReturn(binding);
	 * 
	 * assertEquals(binding.getId(), service.getServiceInstanceBinding(binding).getId());
	 * }
	 */

	@Test
	public void serviceInstanceBindingDeletedSuccessfully() throws Exception {
		ServiceInstanceBinding binding = ServiceInstanceBindingFixture
				.getServiceInstanceBinding();
		when(repository.findById(any(String.class)).get()).thenReturn(binding);

		service.deleteServiceInstanceBinding(buildDeleteRequest());

		verify(mongo).deleteUser(binding.getServiceInstanceId(), binding.getId());
		verify(repository).deleteById(binding.getId());
	}

	@Test(expected = ServiceInstanceBindingDoesNotExistException.class)
	public void unknownServiceInstanceDeleteCallSuccessful() throws Exception {
		ServiceInstanceBinding binding = ServiceInstanceBindingFixture
				.getServiceInstanceBinding();

		when(repository.findById(any(String.class)).get()).thenReturn(null);

		service.deleteServiceInstanceBinding(buildDeleteRequest());

		verify(mongo, never()).deleteUser(binding.getServiceInstanceId(),
				binding.getId());
		verify(repository, never()).deleteById(binding.getId());
	}

	private CreateServiceInstanceBindingRequest buildCreateRequest() {
		return CreateServiceInstanceBindingRequest.builder()
				.bindingId(instanceBinding.getId())
				.bindResource(BindResource.builder().build())
				.serviceInstanceId(instance.getServiceInstanceId())
				.serviceDefinitionId(instance.getServiceDefinitionId())
				.planId(instance.getPlanId()).build();
	}

	private DeleteServiceInstanceBindingRequest buildDeleteRequest() {
		return DeleteServiceInstanceBindingRequest.builder()
				.bindingId(instanceBinding.getId())
				.serviceInstanceId(instance.getServiceInstanceId())
				.serviceDefinitionId(instance.getServiceDefinitionId())
				.planId(instance.getPlanId()).build();
	}

}
