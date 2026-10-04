package com.satark.backend.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import tools.jackson.databind.ObjectMapper;
import com.satark.backend.model.Claim;
import com.satark.backend.model.Evidence;
import com.satark.backend.model.RiskSignal;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Phase 14 verification: wire-format parity with the Android client.
 *
 * <p>Android parses via {@code @SerializedName} keys
 * ({@code AnalysisResponse.kt}, {@code Claim.kt}, {@code RiskSignal.kt},
 * {@code Evidence.kt}, {@code AnalysisRequest.kt} — read-only, never
 * modified). This test serializes the backend DTOs with Jackson (the same
 * mapper Spring MVC uses) and pins the exact JSON key sets, so any rename
 * breaks loudly here instead of on device.
 */
class ApiContractParityTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void responseKeysMatchAndroid() throws Exception {
        var response = new AnalysisResponse("id-1", 82, "VERY_HIGH",
                List.of(new Claim("c", "GUARANTEED_RETURN", "HIGH")),
                List.of(new RiskSignal("t", "d", "HIGH")),
                List.of(new Evidence("c", "UNVERIFIED", "d", "https://www.sebi.gov.in")),
                "explanation", List.of("a1", "a2", "a3"));
        Set<String> keys = fieldNames(mapper.writeValueAsString(response));
        assertEquals(Set.of("analysisId", "riskScore", "riskLevel", "claims",
                "riskSignals", "evidence", "explanation", "recommendedActions"), keys);
    }

    @Test
    void nestedModelKeysMatchAndroid() throws Exception {
        var claimKeys = fieldNames(mapper.writeValueAsString(new Claim("c", "GUARANTEED_RETURN", "HIGH")));
        assertEquals(Set.of("claimText", "category", "confidence"), claimKeys);
        var signalKeys = fieldNames(mapper.writeValueAsString(new RiskSignal("t", "d", "HIGH")));
        assertEquals(Set.of("title", "description", "severity"), signalKeys);
        var evidenceKeys = fieldNames(
                mapper.writeValueAsString(new Evidence("c", "UNVERIFIED", "d", "https://www.sebi.gov.in")));
        assertEquals(Set.of("claim", "status", "details", "sourceUrl"), evidenceKeys);
    }

    @Test
    void requestKeysMatchAndroid() throws Exception {
        var node = mapper.readTree(mapper.writeValueAsString(new AnalysisRequest("TEXT", "hello", "en")));
        Set<String> keys = Stream.of("inputType", "text", "language")
                .filter(node::has).collect(Collectors.toSet());
        assertEquals(Set.of("inputType", "text", "language"), keys);
        assertTrue(node.get("inputType").asText().equals("TEXT"));
    }

    private Set<String> fieldNames(String json) throws Exception {
        return new HashSet<>(
                mapper.readTree(json).propertyNames()
        );
    }
}
