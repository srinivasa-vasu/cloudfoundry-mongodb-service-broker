package org.springframework.cloud.servicebroker.mongodb.dto;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ServiceKey {

	private Metadata metadata;
	private Entity entity;

	public static class Entity {
		private String name;

		@JsonProperty("credentials")
		private Credentials credentials;

		public static class Credentials {
			private String masterUrl;
			private String token;

			@JsonProperty("kubeconfig")
			private void unPackNested(Map<String, Object> kubeConfig) {
				List<Object> clusterList = (List<Object>) kubeConfig.get("clusters");
				setMasterUrl(((Map<String, Map<String, String>>) clusterList.get(0))
						.get("cluster").get("server"));

				List<Object> userList = (List<Object>) kubeConfig.get("users");
				setToken(((Map<String, Map<String, String>>) userList.get(0))
						.get("user").get("token"));
			}

			public void setMasterUrl(String masterUrl) {
				this.masterUrl = masterUrl;
			}

			public void setToken(String token) {
				this.token = token;
			}

			public String getMasterUrl() {
				return masterUrl;
			}

			public String getToken() {
				return token;
			}

			@Override
			public String toString() {
				final StringBuilder sb = new StringBuilder("Credentials{");
				sb.append("masterUrl='").append(masterUrl).append('\'');
				sb.append(", token='").append(token).append('\'');
				sb.append('}');
				return sb.toString();
			}
		}

		public String getName() {
			return name;
		}

		public Credentials getCredentials() {
			return credentials;
		}

		@Override
		public String toString() {
			final StringBuilder sb = new StringBuilder("Entity{");
			sb.append("name='").append(name).append('\'');
			sb.append(", credentials=").append(credentials);
			sb.append('}');
			return sb.toString();
		}
	}

	public Metadata getMetadata() {
		return metadata;
	}

	public Entity getEntity() {
		return entity;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("ServiceKey{");
		sb.append("metadata=").append(metadata);
		sb.append(", entity=").append(entity);
		sb.append('}');
		return sb.toString();
	}
}
