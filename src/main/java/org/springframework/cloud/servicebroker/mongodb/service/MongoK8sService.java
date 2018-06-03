package org.springframework.cloud.servicebroker.mongodb.service;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.servicebroker.mongodb.model.ServiceInstanceParams;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;

@Service
public class MongoK8sService {

	enum K8sObject {

		DISCOVERY_SERVICE("discovery_service.yml"), HEADLESS_SERVICE(
				"headless_service.yml"), STATEFULSET("statefulset.yml"), CONFIGMAP(
						"configmap.yml"), STORAGE_CLASS("storage_gcp.yml");

		private String fileName;
		private static final List<K8sObject> orderedList = new ArrayList<>();
		private static final List<K8sObject> reverseOrderedList = new ArrayList<>();

		K8sObject(String fileName) {
			setFileName(fileName);
		}

		public String getFileName() {
			return fileName;
		}

		public void setFileName(String fileName) {
			this.fileName = fileName;
		}

		static List<K8sObject> getOrderedList() {
			if (orderedList.isEmpty()) {
				orderedList.addAll(Arrays.asList(values()));
			}
			return orderedList;
		}

		static List<K8sObject> getReverseOrderedList() {
			if (reverseOrderedList.isEmpty()) {
				reverseOrderedList.addAll(getOrderedList());
				Collections.reverse(reverseOrderedList);
			}
			return reverseOrderedList;
		}

	}

	private static final Logger LOGGER = LoggerFactory.getLogger(MongoK8sService.class);
	private final Configuration config;
	private final RestTemplate restTemplate;
	private static final String CONTENT_TYPE = "application/yaml";
	private static final String BASE_URL = "/api/v1/namespaces/";
	private static final String BASE_URL_SF = "/apis/apps/v1/namespaces/";
	private static final String BASE_URL_STORAGE = "/apis/storage.k8s.io/v1/storageclasses";
	private static final List<String> STATUS_CODES = Arrays.asList("200", "201");

	public MongoK8sService(Configuration config) {
		this.config = config;
		this.config.setClassForTemplateLoading(this.getClass(), "/templates/");
		restTemplate = new RestTemplate(new TrustEverythingClientHttpRequestFactory());
		restTemplate.setErrorHandler(new NoErrorsResponseErrorHandler());
	}

	boolean createK8sObjects(ServiceInstanceParams serviceObj)
			throws IOException, InterruptedException, TemplateException {
		final HttpHeaders headers = new HttpHeaders();
		headers.set("Authorization", "Bearer " + serviceObj.getAccessToken());
		headers.set("Content-Type", CONTENT_TYPE);
		// if (LOGGER.isDebugEnabled()) {
		// LOGGER.debug(
		// "Deleting k8s objects if exist before creating a new service instance");
		// }
		// for (K8sObject obj : K8sObject.getOrderedList()) {
		// deleteObjectIfExists(obj, headers, serviceObj);
		// }
		for (K8sObject obj : K8sObject.getReverseOrderedList()) {
			ResponseEntity<String> result = createObject(obj, headers, serviceObj);
			if (!STATUS_CODES.contains(result.getStatusCode().toString())) {
				LOGGER.error(obj + " creation has failed with status code: "
						+ result.getStatusCode() + result.getBody());
				return false;
			}
		}
		if (!actionStatus(headers, serviceObj)) {
			LOGGER.error(
					"POD creation has failed or taking longer time to complete. Exceeded the threshold wait time");
			return false;
		}
		return true;
	}

	void deleteK8sObjects(ServiceInstanceParams serviceObj) {
		final HttpHeaders headers = new HttpHeaders();
		headers.set("Authorization", "Bearer " + serviceObj.getAccessToken());
		headers.set("Content-Type", CONTENT_TYPE);
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Deleting k8s objects as part of service instance deletion");
		}
		for (K8sObject obj : K8sObject.getOrderedList()) {
			deleteObjectIfExists(obj, headers, serviceObj);
		}
	}

	private ResponseEntity<String> createObject(K8sObject obj, HttpHeaders headers,
			ServiceInstanceParams serviceObj) throws IOException, TemplateException {
		ResponseEntity<String> result = restTemplate.exchange(
				getEndpoint(obj, serviceObj, false), HttpMethod.POST,
				new HttpEntity<>(
						FreeMarkerTemplateUtils.processTemplateIntoString(
								config.getTemplate(obj.getFileName()), serviceObj),
						headers),
				String.class);
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Creation of " + obj + " : ", result.getStatusCode(),
					result.getBody());
		}
		return result;
	}

	private void deleteObjectIfExists(K8sObject obj, HttpHeaders headers,
			ServiceInstanceParams serviceObj) {
		HttpEntity<String> entity = new HttpEntity<>(null, headers);
		ResponseEntity<String> result = restTemplate.exchange(
				getEndpoint(obj, serviceObj, true), HttpMethod.DELETE, entity,
				String.class);
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Deletion of " + obj.name() + ": ", result.getStatusCode(),
					result.getBody());
		}
	}

	private String getEndpoint(K8sObject obj, ServiceInstanceParams serviceObj,
			boolean delete) {
		String endpoint = "";
		switch (obj) {
		case STORAGE_CLASS:
			endpoint = serviceObj.getUrl() + BASE_URL_STORAGE;
			if (delete) {
				endpoint = endpoint + "/" + serviceObj.getName() + "-storage";
			}
			break;
		case CONFIGMAP:
			endpoint = serviceObj.getUrl() + BASE_URL + serviceObj.getNamespace()
					+ "/configmaps";
			if (delete) {
				endpoint = endpoint + "/" + serviceObj.getName() + "-config";
			}
			break;
		case DISCOVERY_SERVICE:
			endpoint = serviceObj.getUrl() + BASE_URL + serviceObj.getNamespace()
					+ "/services";
			if (delete) {
				endpoint = endpoint + "/" + serviceObj.getName() + "-discovery";
			}
			break;
		case HEADLESS_SERVICE:
			endpoint = serviceObj.getUrl() + BASE_URL + serviceObj.getNamespace()
					+ "/services";
			if (delete) {
				endpoint = endpoint + "/" + serviceObj.getName() + "-service";
			}
			break;
		case STATEFULSET:
			endpoint = serviceObj.getUrl() + BASE_URL_SF + serviceObj.getNamespace()
					+ "/statefulsets";
			if (delete) {
				endpoint = endpoint + "/" + serviceObj.getName();
			}
			break;
		}
		return endpoint;
	}

	private boolean actionStatus(HttpHeaders headers, ServiceInstanceParams serviceObj)
			throws IOException, InterruptedException {
		int threshold = 3;
		boolean status = true;
		HttpEntity<String> entity = new HttpEntity<>(null, headers);
		ObjectMapper mapper = new ObjectMapper();
		String podAPI = serviceObj.getUrl() + BASE_URL + serviceObj.getNamespace()
				+ "/pods/" + serviceObj.getName() + "-0/status";
		ResponseEntity<String> result;
		while (true) {
			result = restTemplate.exchange(podAPI, HttpMethod.GET, entity, String.class);
			JsonNode node = mapper.readTree(result.getBody());
			if (node.get("status").get("phase").textValue().equalsIgnoreCase("Running")) {
				break;
			}
			if (--threshold >= 0) {
				TimeUnit.SECONDS.sleep(serviceObj.getServiceTimeout());
			}
			else {
				status = false;
				break;
			}
		}
		return status;
	}

	private static final class NoErrorsResponseErrorHandler
			extends DefaultResponseErrorHandler {

		@Override
		public boolean hasError(ClientHttpResponse response) {
			return false;
		}

	}

	private static final class TrustEverythingClientHttpRequestFactory
			extends SimpleClientHttpRequestFactory {

		@Override
		protected HttpURLConnection openConnection(URL url, Proxy proxy)
				throws IOException {
			HttpURLConnection connection = super.openConnection(url, proxy);

			if (connection instanceof HttpsURLConnection) {
				HttpsURLConnection httpsConnection = (HttpsURLConnection) connection;

				httpsConnection.setSSLSocketFactory(
						getSslContext(new TrustEverythingTrustManager())
								.getSocketFactory());
				httpsConnection
						.setHostnameVerifier(new TrustEverythingHostNameVerifier());
			}

			return connection;
		}

		private static SSLContext getSslContext(TrustManager trustManager) {
			try {
				SSLContext sslContext = SSLContext.getInstance("SSL");
				sslContext.init(null, new TrustManager[] { trustManager }, null);
				return sslContext;
			}
			catch (KeyManagementException | NoSuchAlgorithmException e) {
				throw new RuntimeException(e);

			}

		}
	}

	private static final class TrustEverythingHostNameVerifier
			implements HostnameVerifier {

		@Override
		public boolean verify(String s, SSLSession sslSession) {
			return true;
		}

	}

	private static final class TrustEverythingTrustManager implements X509TrustManager {

		@Override
		public void checkClientTrusted(X509Certificate[] x509Certificates, String s) {
		}

		@Override
		public void checkServerTrusted(X509Certificate[] x509Certificates, String s) {
		}

		@Override
		public X509Certificate[] getAcceptedIssuers() {
			return new X509Certificate[0];
		}

	}

}
