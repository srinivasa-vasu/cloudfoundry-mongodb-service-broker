package org.springframework.cloud.servicebroker.mongodb.model;

import java.util.Map;

import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.DeleteServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.UpdateServiceInstanceRequest;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * An instance of a ServiceDefinition.
 *
 * @author sgreenberg@pivotal.io
 */
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
public class ServiceInstance {

	@JsonSerialize
	@JsonProperty("service_instance_id")
	private String id;

	@JsonSerialize
	@JsonProperty("service_id")
	private String serviceDefinitionId;

	@JsonSerialize
	@JsonProperty("plan_id")
	private String planId;

	@JsonSerialize
	@JsonProperty("organization_guid")
	private String organizationGuid;

	@JsonSerialize
	@JsonProperty("space_guid")
	private String spaceGuid;

	@JsonSerialize
	@JsonProperty("dashboard_url")
	private String dashboardUrl;

	@JsonSerialize
	@JsonProperty("parameters")
	private Map<String, Object> parameters;

	@JsonSerialize
	@JsonProperty("instance_params")
	private ServiceInstanceParams instanceParams;

	@SuppressWarnings("unused")
	private ServiceInstance() {
	}

	public ServiceInstance(String serviceInstanceId, String serviceDefinitionId,
			String planId, String organizationGuid, String spaceGuid, String dashboardUrl,
			ServiceInstanceParams params) {
		this.id = serviceInstanceId;
		this.serviceDefinitionId = serviceDefinitionId;
		this.planId = planId;
		this.organizationGuid = organizationGuid;
		this.spaceGuid = spaceGuid;
		this.dashboardUrl = dashboardUrl;
		this.instanceParams = params;
	}

	/**
	 * Create a ServiceInstance from a create request. If fields are not present in the
	 * request they will remain null in the ServiceInstance.
	 * @param request containing details of ServiceInstance
	 */
	public ServiceInstance(CreateServiceInstanceRequest request) {
		this.serviceDefinitionId = request.getServiceDefinitionId();
		this.planId = request.getPlanId();
		this.organizationGuid = (String) request.getContext().getProperty("organization_guid");
		this.spaceGuid = (String) request.getContext().getProperty("space_guid");
		this.id = request.getServiceInstanceId();
		this.parameters = request.getParameters();
	}

	public ServiceInstance(CreateServiceInstanceRequest request,
			ServiceInstanceParams params) {
		this.serviceDefinitionId = request.getServiceDefinitionId();
		this.planId = request.getPlanId();
		this.organizationGuid = (String) request.getContext().getProperty("organization_guid");
		this.spaceGuid = (String) request.getContext().getProperty("space_guid");
		this.id = request.getServiceInstanceId();
		this.parameters = request.getParameters();
		this.instanceParams = params;
	}

	/**
	 * Create a ServiceInstance from a delete request. If fields are not present in the
	 * request they will remain null in the ServiceInstance.
	 * @param request containing details of ServiceInstance
	 */
	public ServiceInstance(DeleteServiceInstanceRequest request) {
		this.id = request.getServiceInstanceId();
		this.planId = request.getPlanId();
		this.serviceDefinitionId = request.getServiceDefinitionId();
	}

	/**
	 * Create a service instance from an update request. If fields are not present in the
	 * request they will remain null in the ServiceInstance.
	 * @param request containing details of ServiceInstance
	 */
	public ServiceInstance(UpdateServiceInstanceRequest request) {
		this.id = request.getServiceInstanceId();
		this.planId = request.getPlanId();
	}

	public String getServiceInstanceId() {
		return id;
	}

	public String getServiceDefinitionId() {
		return serviceDefinitionId;
	}

	public String getPlanId() {
		return planId;
	}

	public String getOrganizationGuid() {
		return organizationGuid;
	}

	public String getSpaceGuid() {
		return spaceGuid;
	}

	public String getDashboardUrl() {
		return dashboardUrl;
	}

	public ServiceInstanceParams getInstanceParams() {
		return instanceParams;
	}

	public ServiceInstance and() {
		return this;
	}

	public ServiceInstance withDashboardUrl(String dashboardUrl) {
		this.dashboardUrl = dashboardUrl;
		return this;
	}
}
