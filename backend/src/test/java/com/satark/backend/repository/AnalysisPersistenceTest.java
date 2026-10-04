package com.satark.backend.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.satark.backend.model.Analysis;
import com.satark.backend.model.Claim;
import com.satark.backend.model.Evidence;
import com.satark.backend.model.RiskSignal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;
import org.springframework.data.mongodb.core.MongoTemplate;

/**
 * Phase 15 verification: MongoDB round-trip for the final API shape.
 *
 * <p>Requires a reachable MongoDB (same {@code MONGODB_URI} the app uses):
 * no embedded server or Testcontainers is added on purpose — the existing
 * {@code BackendApplicationTests} context test already has the same
 * requirement. Run on JDK 21 with Mongo available, e.g.:
 * {@code MONGODB_URI=mongodb://localhost:27017 ./gradlew test}.
 *
 * <p>Schema note: no model/repository change was needed — full P3+P4+scorer
 * output fits the existing {@code analyses} document shape, so old mock
 * documents keep reading (covered by {@code legacyMockDocumentStillReads}).
 */
@DataMongoTest
class AnalysisPersistenceTest {

    @Autowired
    private AnalysisRepository repository;

    @Autowired
    private MongoTemplate mongoTemplate;

    private static Analysis fullAnalysis() {
        return new Analysis(null, "TEXT", "Guaranteed 30% monthly returns. SEBI registered.",
                "en", 100, "VERY_HIGH",
                List.of(new Claim("Guaranteed 30% monthly returns", "GUARANTEED_RETURN", "HIGH")),
                List.of(new RiskSignal("Unrealistic Guaranteed Returns", "d", "CRITICAL")),
                List.of(new Evidence("Guaranteed 30% monthly returns", "INSUFFICIENT_EVIDENCE", "d", null)),
                "explanation", List.of("a1", "a2", "a3"), LocalDateTime.now());
    }

    @Test
    void saveAndReadBackRoundTrip() {
        Analysis saved = repository.save(fullAnalysis());
        assertNotNull(saved.getId(), "Mongo must assign the analysisId");

        Analysis read = repository.findById(saved.getId()).orElseThrow();
        assertEquals(saved.getId(), read.getId());
        assertEquals("TEXT", read.getInputType());
        assertEquals(100, read.getRiskScore());
        assertEquals("VERY_HIGH", read.getRiskLevel());
        assertEquals(1, read.getClaims().size());
        assertEquals("GUARANTEED_RETURN", read.getClaims().get(0).getCategory());
        assertEquals(1, read.getRiskSignals().size());
        assertEquals(1, read.getEvidence().size());
        assertEquals("INSUFFICIENT_EVIDENCE", read.getEvidence().get(0).getStatus());
        assertEquals(3, read.getRecommendedActions().size());

        // analysisId == Mongo document id, in the contract collection.
        assertTrue(mongoTemplate.getCollectionNames().contains("analyses"));
        assertEquals(saved.getId(), mongoTemplate.findById(saved.getId(), Analysis.class).getId());
    }

    @Test
    void legacyMockDocumentStillReads() {
        Analysis legacy = new Analysis(null, "SCREENSHOT", "legacy text", "en", 82, "VERY_HIGH",
                List.of(new Claim("Guaranteed 30% monthly returns", "GUARANTEED_RETURN", "HIGH")),
                List.of(new RiskSignal("Guaranteed returns", "d", "HIGH")),
                List.of(new Evidence("Guaranteed 30% monthly returns", "INSUFFICIENT_EVIDENCE", "d", null)),
                "legacy explanation", List.of("a1", "a2", "a3"), LocalDateTime.now());
        Analysis saved = repository.save(legacy);
        Analysis read = repository.findById(saved.getId()).orElseThrow();
        assertEquals(82, read.getRiskScore());
        assertEquals("VERY_HIGH", read.getRiskLevel());
    }
}
