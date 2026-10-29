package com.collins.platform.commons.idempotency;

import com.collins.platform.commons.exception.BusinessRuleException;
import com.collins.platform.commons.exception.ConflictException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Stripe-style idempotency keys, stored with JdbcTemplate in each service's own database.
 * <ul>
 *   <li>First request with a key: runs the action and stores the response.</li>
 *   <li>Same key, same body: returns the stored response without running the action again.</li>
 *   <li>Same key, different body: rejected, because the client is reusing a key by mistake.</li>
 * </ul>
 * Each service needs the {@code idempotency_keys} table (see its V1 migration).
 */
@Service
public class IdempotencyService {

    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper mapper;

    public IdempotencyService(NamedParameterJdbcTemplate jdbc, ObjectMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    public <T> T execute(String scope, String key, Object request, Class<T> responseType, Supplier<T> action) {
        if (key == null || key.isBlank()) {
            throw new BusinessRuleException("IDEMPOTENCY_KEY_REQUIRED", "The Idempotency-Key header is required for this operation");
        }
        String hash = hash(request);
        int inserted = jdbc.update("""
                insert into idempotency_keys(scope, idem_key, request_hash, status)
                values (:scope, :key, :hash, 'IN_PROGRESS')
                on conflict (scope, idem_key) do nothing
                """, Map.of("scope", scope, "key", key, "hash", hash));

        if (inserted == 0) {
            List<Map<String, Object>> rows = jdbc.queryForList(
                    "select request_hash, status, response_json from idempotency_keys where scope = :scope and idem_key = :key",
                    Map.of("scope", scope, "key", key));
            Map<String, Object> row = rows.get(0);
            if (!hash.equals(row.get("request_hash"))) {
                throw new ConflictException("IDEMPOTENCY_KEY_REUSED", "This Idempotency-Key was already used with a different request");
            }
            if (!"COMPLETED".equals(row.get("status"))) {
                throw new ConflictException("REQUEST_IN_PROGRESS", "A request with this Idempotency-Key is still being processed");
            }
            return read((String) row.get("response_json"), responseType);
        }

        try {
            T result = action.get();
            jdbc.update("""
                    update idempotency_keys set status = 'COMPLETED', response_json = :json
                    where scope = :scope and idem_key = :key
                    """, new MapSqlParameterSource().addValue("json", write(result)).addValue("scope", scope).addValue("key", key));
            return result;
        } catch (RuntimeException ex) {
            jdbc.update("delete from idempotency_keys where scope = :scope and idem_key = :key", Map.of("scope", scope, "key", key));
            throw ex;
        }
    }

    private String hash(Object request) {
        try {
            byte[] bytes = mapper.writeValueAsString(request).getBytes(StandardCharsets.UTF_8);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (JsonProcessingException | NoSuchAlgorithmException e) {
            throw new IllegalStateException("Cannot hash request", e);
        }
    }

    private String write(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize response", e);
        }
    }

    private <T> T read(String json, Class<T> type) {
        try {
            return mapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot read stored response", e);
        }
    }
}
