package org.springframework.cloud.servicebroker.mongodb.config;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Required;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;

@Configuration
@EnableMongoRepositories(basePackages = "org.springframework.cloud.servicebroker.mongodb.repository")
public class MongoConfig {

	@Value("${mongodb.host:localhost}")
	private String host;

	@Value("${mongodb.port:27017}")
	private int port;

	@Value("${mongodb.username:admin}")
	private String username;

	@Value("${mongodb.password:password}")
	private String password;

	@Value("${mongodb.authdb:admin}")
	private String authSource;

    @Value("${mongodb.authToken:}")
    private String accessToken;

    @Value("${mongodb.namespace:}")
	private String namespace;

    @Value("${mongodb.masterurl:}")
	private String masterUrl;

    @Value("${mongodb.servicename:mongo-od}")
	private String name;

    @Value("${mongodb.service.timeout:30}")
    private long serviceTimeout;

    @Value("${cf.user}")
    private String cfUser;

    @Value("${cf.password}")
    private String cfPassword;

	@Bean
	public MongoClient mongoClient() {
		final MongoCredential credential = MongoCredential.createScramSha1Credential(username, authSource, password.toCharArray());
		return new MongoClient(new ServerAddress(host, port), Arrays.asList(credential));
	}

    public int getPort() {
        return port;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getMasterUrl() {
        return masterUrl;
    }

    public String getName() {
        return name;
    }

    public long getServiceTimeout() {
        return serviceTimeout;
    }

    public String getCfUser() {
        return cfUser;
    }

    public String getCfPassword() {
        return cfPassword;
    }
}
