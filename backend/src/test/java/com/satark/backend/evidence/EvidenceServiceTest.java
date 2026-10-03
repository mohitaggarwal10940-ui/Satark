package com.satark.backend.evidence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.satark.backend.model.Claim;
import com.satark.backend.model.Evidence;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Phase 12 verification: orchestration mapping, gates, edge cases.
 * Pure unit tests — no Spring context required.
 */
class EvidenceServiceTest {

    private EvidenceService service;
    private TrustedSourceRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new TrustedSourceRegistry();
        service = new EvidenceService(new DeterministicClaimVerifier(registry), registry);
    }

    @Test
    void emptyAndNullYieldEmpty() {
        assertEquals(List.of(), service.verifyAll(null));
        assertEquals(List.of(), service.verifyAll(List.of()));
    }

    @Test
    void mixedClaimsMapOneToOne() {
        var out = service.verifyAll(List.of(
                new Claim("SEBI registered analyst", "REGULATORY_IMPERSONATION", "MEDIUM"),
                new Claim("Guaranteed 30% returns", "GUARANTEED_RETURN", "HIGH")));
        assertEquals(2, out.size());
        assertEquals("UNVERIFIED", out.get(0).getStatus());
        assertEquals("INSUFFICIENT_EVIDENCE", out.get(1).getStatus());
    }

    @Test
    void allUrlsAllowlistedOrNull() {
        var out = service.verifyAll(List.of(
                new Claim("a", "GUARANTEED_RETURN", "HIGH"),
                new Claim("b", "REGULATORY_IMPERSONATION", "MEDIUM"),
                new Claim("c", "URGENCY_PRESSURE", "MEDIUM"),
                new Claim("d", "UNOFFICIAL_COMMUNICATION", "HIGH")));
        for (Evidence e : out) {
            if (e.getSourceUrl() != null) {
                assertTrue(registry.isAllowlistedUrl(e.getSourceUrl()), "leaked URL: " + e.getSourceUrl());
            }
        }
    }

    @Test
    void hostileVerifierOutputIsSanitized() {
        ClaimVerifier hostile = claim -> new Evidence("x", "VERIFIED_TRUE",
                "made up", "https://evil.example.com/proof");
        EvidenceService guarded = new EvidenceService(hostile, registry);
        var out = guarded.verifyAll(List.of(new Claim("x", "GUARANTEED_RETURN", "HIGH")));
        assertEquals(1, out.size());
        // Status passes through taxonomy as-is here (VERIFIED_TRUE is canonical),
        // but the non-allowlisted URL must be nulled.
        assertTrue(out.get(0).getSourceUrl() == null);
    }

    @Test
    void throwingVerifierYieldsUnavailable() {
        ClaimVerifier boom = claim -> {
            throw new RuntimeException("down");
        };
        EvidenceService resilient = new EvidenceService(boom, registry);
        var out = resilient.verifyAll(List.of(new Claim("x", "GUARANTEED_RETURN", "HIGH")));
        assertEquals(1, out.size());
        assertEquals("UNAVAILABLE", out.get(0).getStatus());
    }

    @Test
    void deterministic() {
        var claims = List.of(new Claim("SEBI registered", "REGULATORY_IMPERSONATION", "MEDIUM"));
        assertEquals(service.verifyAll(claims), service.verifyAll(claims));
    }
}
