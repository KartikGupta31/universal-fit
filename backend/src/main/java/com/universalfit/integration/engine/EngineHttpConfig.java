package com.universalfit.integration.engine;

import java.net.http.HttpClient;

import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;

/**
 * Forces HTTP/1.1 for outgoing RestClient calls.
 *
 * The JDK HttpClient defaults to HTTP/2 and, over plain http://, adds
 * "Connection: Upgrade" and "Upgrade: h2c". Uvicorn (httptools) stops reading
 * at that header and drops the request body, so FastAPI sees an empty body
 * and answers 422.
 */
@Configuration
public class EngineHttpConfig {

    static JdkClientHttpRequestFactory http11RequestFactory() {
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        return new JdkClientHttpRequestFactory(httpClient);
    }

    @Bean
    RestClientCustomizer http11RestClientCustomizer() {
        return builder -> builder.requestFactory(http11RequestFactory());
    }
}