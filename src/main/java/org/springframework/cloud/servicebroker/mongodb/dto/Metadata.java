package org.springframework.cloud.servicebroker.mongodb.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Metadata {

    @JsonProperty("guid")
    private String guid;

    public String getGuid() {
        return guid;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Metadata{");
        sb.append("guid='").append(guid).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
