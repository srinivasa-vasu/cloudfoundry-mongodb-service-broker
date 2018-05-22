package org.springframework.cloud.servicebroker.mongodb.model;

import org.springframework.cloud.servicebroker.exception.ServiceBrokerException;
import org.springframework.cloud.servicebroker.model.CreateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.mongodb.config.MongoConfig;

public class ServiceObjectInstance {

	enum ObjVars {
		TOKEN("token"), NAMESPACE("namespace"), SERVICE_NAME("service_name",
				"servicename"), MASTER_URL("master_url", "masterurl"), EXPOSE_PORT(
						"expose_port", "exposeport"), DEFAULT("");

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

	private String namespace;
	private String name;
	private String accessToken;
	private String url;
	private int exposePort = 31000;
	private long serviceTimeout = 30;

	/**
	 * Create a ServiceObjectInstance from a create request. If fields are not present in
	 * the request they will remain null in the ServiceInstance.
	 * @param request containing details of ServiceInstance
	 */
	public ServiceObjectInstance(CreateServiceInstanceRequest request,
			MongoConfig config) {
		initialize(config);
		populate(request);
		validateInputParams(request);
	}

	private void initialize(MongoConfig config) {
		setAccessToken(config.getAccessToken());
		setNamespace(config.getNamespace());
		setUrl(config.getMasterUrl());
		setName(config.getName());
		setExposePort(config.getPort());
		setServiceTimeout(config.getServiceTimeout());
	}

	private void populate(CreateServiceInstanceRequest request) {
		for (String key : request.getParameters().keySet()) {
			switch (ObjVars.getObj(key)) {
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

	private void validateInputParams(CreateServiceInstanceRequest request) {
		String errorMsg = null;
		if (isEmpty(getAccessToken())) {
			errorMsg = ObjVars.TOKEN.getInput();
		}
		if (isEmpty(getUrl())) {
			errorMsg = ObjVars.MASTER_URL.getInput();
		}
		if (isEmpty(getName())) {
			errorMsg = ObjVars.SERVICE_NAME.getInput();
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

	private int getExposePort() {
		return exposePort;
	}

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ServiceObjectInstance{");
        sb.append("namespace='").append(namespace).append('\'');
        sb.append(", name='").append(name).append('\'');
        sb.append(", accessToken='").append(accessToken).append('\'');
        sb.append(", url='").append(url).append('\'');
        sb.append(", exposePort=").append(exposePort);
        sb.append(", serviceTimeout=").append(serviceTimeout);
        sb.append('}');
        return sb.toString();
    }
}
