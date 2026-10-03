package com.satark.backend.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.satark.backend.model.Claim;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Phase 8 verification: curated multilingual + OCR-noise dataset, plus
 * synthetic noise-fuzz. Fails loudly per case with IDs for triage.
 * No Spring context, no MongoDB required.
 */
class P3DatasetTest {

    private DeterministicClaimExtractor extractor;
    private DeterministicRiskSignalDetector detector;

    @BeforeEach
    void setUp() {
        extractor = new DeterministicClaimExtractor(new AiProperties());
        detector = new DeterministicRiskSignalDetector();
    }

    @Test
    void datasetCasesPass() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root;
        try (InputStream in = getClass().getResourceAsStream("/p3-dataset.json")) {
            assertTrue(in != null, "p3-dataset.json missing from test resources");
            root = mapper.readTree(in);
        }
        List<String> failures = new ArrayList<>();
        int total = 0;
        for (JsonNode c : root.get("cases")) {
            total++;
            String id = c.get("id").asText();
            String text = c.get("text").asText();
            List<Claim> claims = extractor.extract(text);
            var codes = claims.stream().map(Claim::getCategory).collect(Collectors.toSet());
            for (JsonNode expected : c.get("expectCategories")) {
                if (!codes.contains(expected.asText())) {
                    failures.add(id + ": missing " + expected.asText() + " (got " + codes + ")");
                }
            }
            int min = c.get("minClaims").asInt();
            if (claims.size() < min) {
                failures.add(id + ": expected >=" + min + " claims, got " + claims.size());
            }
            // Detector must never break on extractor output.
            var signals = detector.detect(claims, text);
            if (signals == null || signals.size() > 4) {
                failures.add(id + ": detector output invalid");
            }
        }
        assertTrue(failures.isEmpty(), "dataset failures (" + failures.size() + "/" + total + "): " + failures);
        assertEquals(15, total, "dataset case count changed — update this assertion deliberately");
    }

    @Test
    void syntheticOcrNoisePreservesCoreClaims() {
        String base = "Guaranteed 30% monthly return. SEBI registered analyst. Only 5 slots left, pay fastprofit@upi now.";
        List<String> variants = List.of(
                base.toUpperCase(java.util.Locale.ROOT),
                base.replace(" ", "   ").replace(".", "..."),
                base.replace(".", "\n"),
                "  " + base + "  ",
                base.replace("SEBI", "Sebi").replace("Guaranteed", "GUARANTEED"));
        for (String v : variants) {
            var codes = extractor.extract(v).stream().map(Claim::getCategory).collect(Collectors.toSet());
            assertTrue(codes.contains("GUARANTEED_RETURN"), "noise variant lost GUARANTEED_RETURN: " + v);
            assertTrue(codes.contains("REGULATORY_IMPERSONATION"), "noise variant lost REGULATORY: " + v);
        }
    }
}
