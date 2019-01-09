package org.springframework.cloud.servicebroker.mongodb.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "broker")
public class BrokerConfig {

	private String username;

	private String password;

}
