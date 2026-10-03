package com.satark.backend.evidence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Phase 9 verification: taxonomy codes, normalization, back-compat.
 * Pure unit tests — no Spring context required.
 */
class EvidenceStatusTest {

    @Test
    void allCodesResolve() {
        for (EvidenceStatus s : EvidenceStatus.values()) {
            assertEquals(s, EvidenceStatus.fromCode(s.getCode()));
            assertEquals(s, EvidenceStatus.fromCode("  " + s.getCode().toLowerCase() + " "));
        }
        assertEquals(6, EvidenceStatus.values().length);
    }

    @Test
    void legacyPayloadsResolve() {
        // Strings already stored in Mongo / sent by mocks and Android.
        assertEquals(EvidenceStatus.INSUFFICIENT_EVIDENCE, EvidenceStatus.fromCode("INSUFFICIENT_EVIDENCE"));
        assertEquals(EvidenceStatus.UNVERIFIED, EvidenceStatus.fromCode("UNVERIFIED"));
    }

    @Test
    void unknownCollapsesToInsufficientNeverInvents() {
        assertEquals(EvidenceStatus.INSUFFICIENT_EVIDENCE, EvidenceStatus.fromCode("VERIFIED"));
        assertEquals(EvidenceStatus.INSUFFICIENT_EVIDENCE, EvidenceStatus.fromCode("REGISTERED"));
        assertEquals(EvidenceStatus.INSUFFICIENT_EVIDENCE, EvidenceStatus.fromCode(null));
        assertEquals(EvidenceStatus.INSUFFICIENT_EVIDENCE, EvidenceStatus.fromCode("   "));
    }

    @Test
    void onlyVerifiedStatusesAssertFact() {
        assertTrue(EvidenceStatus.VERIFIED_TRUE.assertsFact());
        assertTrue(EvidenceStatus.VERIFIED_FALSE.assertsFact());
        assertFalse(EvidenceStatus.UNVERIFIED.assertsFact());
        assertFalse(EvidenceStatus.INSUFFICIENT_EVIDENCE.assertsFact());
        assertFalse(EvidenceStatus.AMBIGUOUS.assertsFact());
        assertFalse(EvidenceStatus.UNAVAILABLE.assertsFact());
    }

    @Test
    void trustedSourceRejectsNonHttps() {
        assertThrows(IllegalArgumentException.class,
                () -> new TrustedSource("x", "X", "http://example.com", 1));
        TrustedSource sebi = new TrustedSource("sebi", "SEBI", "https://www.sebi.gov.in", 1);
        assertEquals("sebi", sebi.key());
    }
}
