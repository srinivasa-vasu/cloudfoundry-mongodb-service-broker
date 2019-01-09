package org.springframework.cloud.servicebroker.mongodb.model;

import static org.springframework.cloud.servicebroker.mongodb.config.CatalogConfig.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.cloud.servicebroker.exception.ServiceBrokerException;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.mongodb.config.MongoConfig;
import org.springframework.cloud.servicebroker.mongodb.dto.ServiceKey;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

public class ServiceInstanceParams {

	enum ObjVars {
		//@formatter:off
		TOKEN("token"), 
		NAMESPACE("namespace"), 
		SERVICE_NAME("service_name", "servicename"), 
		MASTER_URL("master_url", "masterurl"), 
		EXPOSE_PORT("expose_port", "exposeport"), 
		CLUSTER_NAME("cluster_name", "clustername"), 
		IDENTITY("identity"), DEFAULT("");
		//@formatter:on

		private String[] keys;

		ObjVars(String... argKeys) {
			this.keys = argKeys;
		}

		String[] getKeys() {
			return keys;
		}

		String getInput() {
			return keys[0];
		}

		static ObjVars getObj(String input) {
			for (ObjVars var : values()) {
				for (String key : var.getKeys()) {
					if (key.equalsIgnoreCase(input)) {
						return var;
					}
				}
			}
			return DEFAULT;
		}

	}

	@JsonSerialize
	private final long unq = System.currentTimeMillis();

	@JsonSerialize
	@JsonProperty("namespace")
	private String namespace;
	@JsonSerialize
	@JsonProperty("service_name")
	private String name;
	@JsonSerialize
	@JsonProperty("access_token")
	private String accessToken;
	@JsonSerialize
	@JsonProperty("master_url")
	private String url;
	private int exposePort = 31000;
	private long serviceTimeout = 30;
	private String storage;
	private int replicas = 1;

	@JsonSerialize
	@JsonProperty("cluster_name")
	private String clusterName;
	@JsonSerialize
	@JsonProperty("identity")
	private String identity;
	@JsonSerialize
	@JsonProperty("service_key")
	private String serviceKey;

	@JsonSerialize
	private boolean autoMode;

	@JsonSerialize
	private String cfAPI;

	@JsonSerialize
	private String cfUaaAPI;

	/**
	 * Create a ServiceInstanceParams from a create request. If fields are not present in
	 * the request they will remain null in the ServiceInstance.
	 * @param request containing details of ServiceInstance
	 */
	public ServiceInstanceParams(CreateServiceInstanceRequest request,
								 MongoConfig config) {
		initialize(config);
		populate(request);
		populatePlanParams(request);
		populateRequestInfo(request);
	}

	public ServiceInstanceParams(String namespace, String name, String accessToken,
			String url, int exposePort, long serviceTimeout, String storage,
			int replicas) {
		this.namespace = namespace;
		this.name = name;
		this.accessToken = accessToken;
		this.url = url;
		this.exposePort = exposePort;
		this.serviceTimeout = serviceTimeout;
		this.storage = storage;
		this.replicas = replicas;
	}

	public ServiceInstanceParams() {
	}

	private void initialize(MongoConfig config) {
		setAccessToken(config.getAccessToken());
		setNamespace(config.getNamespace());
		setUrl(config.getMasterUrl());
		setName(config.getName());
		if (config.getPort() > 0) {
			setExposePort(config.getPort());
		}
		setServiceTimeout(config.getServiceTimeout());
	}

	private void populate(CreateServiceInstanceRequest request) {
		for (String key : request.getParameters().keySet()) {
			switch (ObjVars.getObj(key)) {
			case CLUSTER_NAME:
				setClusterName((String) request.getParameters().get(key));
				break;
			case IDENTITY:
				setIdentity((String) request.getParameters().get(key));
				break;
			case TOKEN:
				setAccessToken((String) request.getParameters().get(key));
				break;
			case NAMESPACE:
				setNamespace((String) request.getParameters().get(key));
				break;
			case MASTER_URL:
				setUrl((String) request.getParameters().get(key));
				break;
			case SERVICE_NAME:
				setName((String) request.getParameters().get(key));
				break;
			case EXPOSE_PORT:
				setExposePort(Integer.valueOf((String) request.getParameters().get(key)));
				break;
			case DEFAULT:
				// do nothing
				break;
			}
		}
	}

	private void populatePlanParams(CreateServiceInstanceRequest request) {
		request.getServiceDefinition().getPlans().stream()
				.filter(plan -> plan.getId().equalsIgnoreCase(request.getPlanId()))
				.findFirst()
				.ifPresent(plan -> ((List<Map<String, Object>>) plan.getMetadata()
						.getOrDefault(COSTS, new HashMap<>())).forEach(key -> {
							setStorage((String) key.getOrDefault(STORAGE, "128Mi"));
							setReplicas(Integer
									.valueOf((String) key.getOrDefault(REPLICAS, "1")));
						}));
	}

	private void populateRequestInfo(CreateServiceInstanceRequest request) {
		//@formatter:off
		setCfAPI("https://" + request.getApiInfoLocation().substring(0, request.getApiInfoLocation().indexOf("/")));
		setCfUaaAPI("https://uaa" + request.getApiInfoLocation().substring(request.getApiInfoLocation().indexOf("."),
                request.getApiInfoLocation().indexOf("/")) + "/oauth/token");
        //@formatter:on
	}

	public void populateServiceParams(ServiceKey serviceKey) {
		setUrl(serviceKey.getEntity().getCredentials().getMasterUrl());
		setAccessToken(serviceKey.getEntity().getCredentials().getToken());
		setNamespace("ns-" + unq);
		setServiceKey(serviceKey.getMetadata().getGuid());
		setAutoMode(true);
	}

	public void validateInputParams(CreateServiceInstanceRequest request) {
		String errorMsg = null;
		if (isEmpty(getAccessToken())) {
			errorMsg = ObjVars.TOKEN.getInput();
		}
		if (isEmpty(getUrl())) {
			errorMsg = ObjVars.MASTER_URL.getInput();
		}
		if (isEmpty(getNamespace())) {
			errorMsg = ObjVars.NAMESPACE.getInput();
		}
		if (!isEmpty(errorMsg)) {
			throw new ServiceBrokerException(
					errorMsg + " env not defined: " + request.getServiceInstanceId());
		}
	}

	private boolean isEmpty(String input) {
		return ((null == input) || (input.length() == 0));
	}

	public String getNamespace() {
		return namespace;
	}

	private void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public String getName() {
		if (name == null || name.isEmpty()) {
			name = "mongo-" + unq;
		}
		return name;
	}

	private void setName(String name) {
		this.name = name;
	}

	public String getAccessToken() {
		return accessToken;
	}

	private void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}

	public String getUrl() {
		return url;
	}

	private void setUrl(String url) {
		this.url = url;
	}

	private void setServiceTimeout(long serviceTimeout) {
		this.serviceTimeout = serviceTimeout;
	}

	public long getServiceTimeout() {
		return serviceTimeout;
	}

	private void setExposePort(int exposePort) {
		this.exposePort = exposePort;
	}

	public int getExposePort() {
		return exposePort;
	}

	public String getStorage() {
		return storage;
	}

	private void setStorage(String storage) {
		this.storage = storage;
	}

	public int getReplicas() {
		return replicas;
	}

	private void setReplicas(int replicas) {
		this.replicas = replicas;
	}

	public String getClusterName() {
		return clusterName;
	}

	public void setClusterName(String clusterName) {
		this.clusterName = clusterName;
	}

	public String getIdentity() {
		return identity;
	}

	public void setIdentity(String identity) {
		this.identity = identity;
	}

	public boolean isAutoMode() {
		return autoMode;
	}

	public void setAutoMode(boolean autoMode) {
		this.autoMode = autoMode;
	}

	public String getServiceKey() {
		return serviceKey;
	}

	public void setServiceKey(String serviceKey) {
		this.serviceKey = serviceKey;
	}

	public long getUnq() {
		return unq;
	}

	public String getCfAPI() {
		return cfAPI;
	}

	public void setCfAPI(String cfAPI) {
		this.cfAPI = cfAPI;
	}

	public String getCfUaaAPI() {
		return cfUaaAPI;
	}

	public void setCfUaaAPI(String cfUaaAPI) {
		this.cfUaaAPI = cfUaaAPI;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("ServiceInstanceParams{");
		sb.append("unq=").append(unq);
		sb.append(", namespace='").append(namespace).append('\'');
		sb.append(", name='").append(name).append('\'');
		sb.append(", url='").append(url).append('\'');
		sb.append(", exposePort=").append(exposePort);
		sb.append(", serviceTimeout=").append(serviceTimeout);
		sb.append(", storage='").append(storage).append('\'');
		sb.append(", replicas=").append(replicas);
		sb.append(", clusterName='").append(clusterName).append('\'');
		sb.append(", serviceKey='").append(serviceKey).append('\'');
		sb.append(", autoMode=").append(autoMode);
		sb.append(", cfAPI='").append(cfAPI).append('\'');
		sb.append(", cfUaaAPI='").append(cfUaaAPI).append('\'');
		sb.append('}');
		return sb.toString();
	}
}
