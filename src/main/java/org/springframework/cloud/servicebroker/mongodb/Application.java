package org.springframework.cloud.servicebroker.mongodb;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import freemarker.template.Template;
import freemarker.template.TemplateException;

@SpringBootApplication
public class Application {

	public static void main(String[] args) throws IOException, TemplateException {
		SpringApplication.run(Application.class, args);

        /*RestTemplate restTemplate = new RestTemplate(new TrustEverythingClientHttpRequestFactory());
        restTemplate.setErrorHandler(new NoErrorsResponseErrorHandler());
        HttpHeaders headers = new HttpHeaders();
        //headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer "+"eyJhbGciOiJSUzI1NiIsImtpZCI6IiJ9.eyJpc3MiOiJrdWJlcm5ldGVzL3NlcnZpY2VhY2NvdW50Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9uYW1lc3BhY2UiOiJkZWZhdWx0Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9zZWNyZXQubmFtZSI6InN2LWFkbWluLXRva2VuLXFjZ2xtIiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9zZXJ2aWNlLWFjY291bnQubmFtZSI6InN2LWFkbWluIiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9zZXJ2aWNlLWFjY291bnQudWlkIjoiODM4Y2Y5NWYtNWJmOC0xMWU4LWFjODgtMDgwMDI3MWU4ZDc3Iiwic3ViIjoic3lzdGVtOnNlcnZpY2VhY2NvdW50OmRlZmF1bHQ6c3YtYWRtaW4ifQ.S6zfOZwyjNCR0qNNW_OH5RVXoUITeJg6juaS9lNds7MQtw2rwfHlzTRO6c2ds8ZvJBHWjA2PS8wuAtE-jatmel7gxc_1jHWAn_2fiCrjg7Xd1urA0Y6zJa1P-XyX0mIiA4NvkY8hmYeimIwDpU8bpTb4IGa_JpJ3n81VsBQd9JvzN7GM4k_SOYoh4HlR-6pRzP3TC6T1DReaA9c5QMLzjFsfKs0f4LNEcrzWeKLYiYEyl_iW7SzvYFxnD5PV5z6Qhmd5PAoYO09UyHTh-z5Q90_o4PS2NI4QCcxuVEz6OgeQgl0_IsjiSzWw6rlc3SEiLROLNuUUGQqQ8Ge73e5tGA");
        headers.set("Content-Type", "application/yaml");


        freemarker.template.Configuration freemarkerConfig = new freemarker.template.Configuration();
        freemarkerConfig.setClassForTemplateLoading(Application.class, "/templates/");

        //Template t = freemarkerConfig.getTemplate("namespace.yml");
        //String text = FreeMarkerTemplateUtils.processTemplateIntoString(t, null);
        //System.out.println(">>>>>>>>>>" + text);



       // MultiValueMap<String, String> body = new LinkedMultiValueMap<String, String>();
       // body.add("body", text);

        HttpEntity<String> entity = new HttpEntity<>(null, headers);
        ResponseEntity<String> result = restTemplate.exchange("https://192.168.99.100:8443/api/v1/namespaces/jhipwrr/pods/pksmint-mongodb-0/status", HttpMethod.GET, entity, String.class);

        System.out.println(result.getBody());
        ObjectMapper mapper = new ObjectMapper();
        JsonNode actualObj = mapper.readTree(result.getBody());
        System.out.println(actualObj.get("status").get("phase"));*/






    }



    /*private static final class NoErrorsResponseErrorHandler extends DefaultResponseErrorHandler {

        @Override
        public boolean hasError(ClientHttpResponse response) throws IOException {
            return false;
        }

    }

    private static final class TrustEverythingClientHttpRequestFactory extends SimpleClientHttpRequestFactory {

        @Override
        protected HttpURLConnection openConnection(URL url, Proxy proxy) throws IOException {
            HttpURLConnection connection = super.openConnection(url, proxy);

            if (connection instanceof HttpsURLConnection) {
                HttpsURLConnection httpsConnection = (HttpsURLConnection) connection;

                httpsConnection.setSSLSocketFactory(getSslContext(new TrustEverythingTrustManager()).getSocketFactory());
                httpsConnection.setHostnameVerifier(new TrustEverythingHostNameVerifier());
            }

            return connection;
        }

        private static SSLContext getSslContext(TrustManager trustManager) {
            try {
                SSLContext sslContext = SSLContext.getInstance("SSL");
                sslContext.init(null, new TrustManager[]{trustManager}, null);
                return sslContext;
            } catch (KeyManagementException | NoSuchAlgorithmException e) {
                throw new RuntimeException(e);

            }

        }
    }

    private static final class TrustEverythingHostNameVerifier implements HostnameVerifier {

        @Override
        public boolean verify(String s, SSLSession sslSession) {
            return true;
        }

    }

    private static final class TrustEverythingTrustManager implements X509TrustManager {

        @Override
        public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }

    }*/

}
