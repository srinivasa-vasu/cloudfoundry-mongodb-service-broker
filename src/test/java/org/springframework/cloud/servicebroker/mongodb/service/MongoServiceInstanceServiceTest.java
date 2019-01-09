package org.springframework.cloud.servicebroker.mongodb.service;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.servicebroker.exception.ServiceBrokerException;
import org.springframework.cloud.servicebroker.exception.ServiceInstanceDoesNotExistException;
import org.springframework.cloud.servicebroker.exception.ServiceInstanceExistsException;
import org.springframework.cloud.servicebroker.model.catalog.ServiceDefinition;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceResponse;
import org.springframework.cloud.servicebroker.model.instance.DeleteServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.DeleteServiceInstanceResponse;
import org.springframework.cloud.servicebroker.mongodb.IntegrationTestBase;
import org.springframework.cloud.servicebroker.mongodb.fixture.ServiceInstanceFixture;
import org.springframework.cloud.servicebroker.mongodb.model.ServiceInstance;
import org.springframework.cloud.servicebroker.mongodb.repository.MongoServiceInstanceRepository;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;

public class MongoServiceInstanceServiceTest extends IntegrationTestBase {

	private static final String SVC_DEF_ID = "serviceDefinitionId";
	private static final String SVC_PLAN_ID = "servicePlanId";

	@Autowired
	private MongoClient client;

	@Mock
	private MongoAdminService mongo;

	@Mock
	private MongoServiceInstanceRepository repository;

	@Mock
	private MongoDatabase db;

	@Mock
	private ServiceDefinition serviceDefinition;

	private MongoServiceInstanceService service;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);

		service = new MongoServiceInstanceService(mongo, repository, null, null, null);
	}

	@After
	public void cleanup() {
		client.dropDatabase(DB_NAME);
	}

	@Test
	public void newServiceInstanceCreatedSuccessfully() throws Exception {
		when(repository.findById(any(String.class)).get()).thenReturn(null);
		when(mongo.databaseExists(any(String.class))).thenReturn(false);
		when(mongo.createDatabase(any(String.class))).thenReturn(db);

		CreateServiceInstanceResponse response = service
				.createServiceInstance(buildCreateRequest());

		assertNotNull(response);
		assertNull(response.getDashboardUrl());
		assertFalse(response.isAsync());

		verify(repository).save(isA(ServiceInstance.class));
	}

	@Test
	public void newServiceInstanceCreatedSuccessfullyWithExistingDB() throws Exception {

		when(repository.findById(any(String.class)).get()).thenReturn(null);
		when(mongo.databaseExists(any(String.class))).thenReturn(true);
		when(mongo.createDatabase(any(String.class))).thenReturn(db);

		CreateServiceInstanceRequest request = buildCreateRequest();
		CreateServiceInstanceResponse response = service.createServiceInstance(request);

		assertNotNull(response);
		assertNull(response.getDashboardUrl());
		assertFalse(response.isAsync());

		verify(mongo).deleteDatabase(request.getServiceInstanceId());
		verify(repository).save(isA(ServiceInstance.class));
	}

	@Test(expected = ServiceInstanceExistsException.class)
	public void serviceInstanceCreationFailsWithExistingInstance() throws Exception {
		when(repository.findById(any(String.class)).get())
				.thenReturn(ServiceInstanceFixture.getServiceInstance());

		service.createServiceInstance(buildCreateRequest());
	}

	@Test(expected = ServiceBrokerException.class)
	public void serviceInstanceCreationFailsWithDBCreationFailure() throws Exception {
		when(repository.findById(any(String.class)).get()).thenReturn(null);
		when(mongo.databaseExists(any(String.class))).thenReturn(false);
		when(mongo.createDatabase(any(String.class))).thenReturn(null);

		service.createServiceInstance(buildCreateRequest());
	}

	@Test
	public void successfullyRetrieveServiceInstance() {
		when(repository.findById(any(String.class)).get())
				.thenReturn(ServiceInstanceFixture.getServiceInstance());
		String id = ServiceInstanceFixture.getServiceInstance().getServiceInstanceId();
		assertEquals(id, service.getServiceInstance(id).getServiceInstanceId());
	}

	@Test
	public void serviceInstanceDeletedSuccessfully() throws Exception {
		ServiceInstance instance = ServiceInstanceFixture.getServiceInstance();
		when(repository.findById(any(String.class)).get()).thenReturn(instance);
		String id = instance.getServiceInstanceId();

		DeleteServiceInstanceResponse response = service
				.deleteServiceInstance(buildDeleteRequest());

		assertNotNull(response);
		assertFalse(response.isAsync());

		verify(mongo).deleteDatabase(id);
		verify(repository).deleteById(id);
	}

	@Test(expected = ServiceInstanceDoesNotExistException.class)
	public void unknownServiceInstanceDeleteCallSuccessful() throws Exception {
		when(repository.findById(any(String.class)).get()).thenReturn(null);

		DeleteServiceInstanceRequest request = buildDeleteRequest();

		DeleteServiceInstanceResponse response = service.deleteServiceInstance(request);

		assertNotNull(response);
		assertFalse(response.isAsync());

		verify(mongo).deleteDatabase(request.getServiceInstanceId());
		verify(repository).deleteById(request.getServiceInstanceId());
	}

	private CreateServiceInstanceRequest buildCreateRequest() {
		return CreateServiceInstanceRequest.builder().serviceDefinitionId(SVC_DEF_ID)
				.serviceInstanceId(ServiceInstanceFixture.getServiceInstance()
						.getServiceInstanceId())
				.planId(SVC_PLAN_ID).serviceDefinition(serviceDefinition).build();
	}

	private DeleteServiceInstanceRequest buildDeleteRequest() {
		return DeleteServiceInstanceRequest.builder().serviceDefinitionId(SVC_DEF_ID)
				.serviceInstanceId(ServiceInstanceFixture.getServiceInstance()
						.getServiceInstanceId())
				.planId(SVC_PLAN_ID).serviceDefinition(serviceDefinition).build();
	}
}
