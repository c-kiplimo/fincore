package com.collins.platform.commons.http;

import com.collins.platform.commons.api.ApiRequest;
import com.collins.platform.commons.exception.UpstreamException;
import com.collins.platform.commons.web.RequestContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;

/**
 * Calls another platform service over REST using the standard envelope: sends {@code {payload}},
 * unwraps {@code payload} from {@code {meta, payload}}, and turns error responses into
 * {@link UpstreamException} with the downstream error code. Passes on X-Request-Id.
 */
public class ServiceClient {

    private final String serviceName;
    private final RestClient restClient;
    private final ObjectMapper mapper;

    public ServiceClient(String serviceName, RestClient restClient, ObjectMapper mapper) {
        this.serviceName = serviceName;
        this.restClient = restClient;
        this.mapper = mapper;
    }

    public <T> T post(String path, Object payload, Class<T> responseType) {
        return post(path, payload, mapper.constructType(responseType), Map.of());
    }

    public <T> T post(String path, Object payload, Class<T> responseType, Map<String, String> headers) {
        return post(path, payload, mapper.constructType(responseType), headers);
    }

    public <T> T get(String path, Class<T> responseType) {
        return exchange(() -> restClient.get().uri(path)
                .header(RequestContext.REQUEST_ID_HEADER, RequestContext.requestId())
                .retrieve().body(JsonNode.class), mapper.constructType(responseType));
    }

    public <T> List<T> getList(String path, Class<T> itemType) {
        JavaType listType = mapper.getTypeFactory().constructCollectionType(List.class, itemType);
        return exchange(() -> restClient.get().uri(path)
                .header(RequestContext.REQUEST_ID_HEADER, RequestContext.requestId())
                .retrieve().body(JsonNode.class), listType);
    }

    private <T> T post(String path, Object payload, JavaType type, Map<String, String> headers) {
        return exchange(() -> restClient.post().uri(path)
                .contentType(MediaType.APPLICATION_JSON)
                .header(RequestContext.REQUEST_ID_HEADER, RequestContext.requestId())
                .headers(h -> headers.forEach(h::set))
                .body(ApiRequest.of(payload))
                .retrieve().body(JsonNode.class), type);
    }

    private <T> T exchange(java.util.function.Supplier<JsonNode> call, JavaType type) {
        try {
            JsonNode body = call.get();
            if (body == null || body.get("payload") == null || body.get("payload").isNull()) {
                return null;
            }
            return mapper.convertValue(body.get("payload"), type);
        } catch (RestClientResponseException ex) {
            throw toUpstream(ex.getStatusCode(), ex.getResponseBodyAsString(), ex);
        } catch (ResourceAccessException ex) {
            throw new UpstreamException("UPSTREAM_UNAVAILABLE", serviceName + " is unreachable: " + ex.getMessage(), 503, ex);
        }
    }

    private UpstreamException toUpstream(HttpStatusCode status, String body, Exception cause) {
        String code = "UPSTREAM_ERROR";
        String message = serviceName + " returned " + status.value();
        try {
            JsonNode node = mapper.readTree(body);
            JsonNode errors = node.path("meta").path("errors");
            if (errors.isArray() && !errors.isEmpty()) {
                code = errors.get(0).path("code").asText(code);
                message = errors.get(0).path("message").asText(message);
            }
        } catch (JsonProcessingException ignored) {
            // body was not our envelope; keep the generic message
        }
        int mapped = status.is4xxClientError() ? status.value() : 502;
        return new UpstreamException(code, message, mapped, cause);
    }
}
