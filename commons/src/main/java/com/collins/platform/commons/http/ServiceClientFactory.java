package com.collins.platform.commons.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/** Builds {@link ServiceClient}s with sensible timeouts. */
@Component
public class ServiceClientFactory {

    private final RestClient.Builder builder;
    private final ObjectMapper mapper;

    public ServiceClientFactory(RestClient.Builder builder, ObjectMapper mapper) {
        this.builder = builder;
        this.mapper = mapper;
    }

    public ServiceClient create(String serviceName, String baseUrl) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(2));
        requestFactory.setReadTimeout(Duration.ofSeconds(10));
        RestClient client = builder.clone().baseUrl(baseUrl).requestFactory(requestFactory).build();
        return new ServiceClient(serviceName, client, mapper);
    }
}
