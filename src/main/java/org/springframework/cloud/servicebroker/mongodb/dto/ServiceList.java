package org.springframework.cloud.servicebroker.mongodb.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonRootName(value = "body")
public class ServiceList {

	public ServiceList() {
	}

	@JsonProperty("total_results")
	private long totalResults;

	@JsonProperty("resources")
	private List<Resource> resources;

	public long getTotalResults() {
		return totalResults;
	}

	public List<Resource> getResources() {
		return resources;
	}

	public static class Resource {

		public Resource() {
		}

		@JsonProperty("metadata")
		private Metadata metadata;

		public Metadata getMetadata() {
			return metadata;
		}

		@Override
		public String toString() {
			final StringBuilder sb = new StringBuilder("Resource{");
			sb.append("metadata=").append(metadata);
			sb.append('}');
			return sb.toString();
		}
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("ServiceList{");
		sb.append("totalResults='").append(totalResults).append('\'');
		sb.append(", resources=").append(resources);
		sb.append('}');
		return sb.toString();
	}

}
