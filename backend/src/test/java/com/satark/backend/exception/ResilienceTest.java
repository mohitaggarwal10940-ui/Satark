package com.satark.backend.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Phase 16 verification: failure injection — outage responses are safe,
 * static, and status-correct. Pure unit tests, no Spring context needed.
 */
class ResilienceTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void databaseOutageIs503WithoutLeak() {
        ResponseEntity<Map<String, Object>> res = handler.handleDataAccessException(
                new DataRetrievalFailureException("connection refused by mongodb at SECRET-HOST user text here"));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, res.getStatusCode());
        assertEquals("SERVICE_UNAVAILABLE", res.getBody().get("error"));
        String joined = String.join(" ", res.getBody().values().stream().map(String::valueOf).toList());
        assertFalse(joined.contains("SECRET-HOST"), "driver/user text leaked: " + joined);
    }

    @Test
    void genericFailureIs500WithoutLeak() {
        ResponseEntity<Map<String, Object>> res = handler.handleGeneralException(
                new RuntimeException("user pasted SECRET-TOKEN-12345 and db exploded"));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, res.getStatusCode());
        assertEquals("INTERNAL_SERVER_ERROR", res.getBody().get("error"));
        String joined = String.join(" ", res.getBody().values().stream().map(String::valueOf).toList());
        assertFalse(joined.contains("SECRET-TOKEN"), "exception/user text leaked: " + joined);
    }
}
