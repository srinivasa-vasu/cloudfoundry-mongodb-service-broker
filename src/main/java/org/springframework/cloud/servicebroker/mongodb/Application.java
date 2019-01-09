package org.springframework.cloud.servicebroker.mongodb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.servicebroker.mongodb.config.BrokerConfig;

@SpringBootApplication
@EnableConfigurationProperties(BrokerConfig.class)
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
    }

}
