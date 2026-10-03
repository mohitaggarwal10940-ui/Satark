package com.satark.backend.evidence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Phase 10 verification: allowlist order, lookup, URL gate.
 * Pure unit tests — no Spring context required.
 */
class TrustedSourceRegistryTest {

    private TrustedSourceRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new TrustedSourceRegistry();
    }

    @Test
    void priorityOrderIsOfficialFirst() {
        var all = registry.allByPriority();
        assertEquals(7, all.size());
        assertEquals("sebi", all.get(0).key());
        assertEquals("rbi", all.get(1).key());
        assertEquals("scores", all.get(all.size() - 1).key());
    }

    @Test
    void findByKeyIsCaseInsensitive() {
        assertTrue(registry.findByKey("SEBI").isPresent());
        assertTrue(registry.findByKey("cybercrime").isPresent());
        assertTrue(registry.findByKey("nope").isEmpty());
        assertTrue(registry.findByKey(null).isEmpty());
    }

    @Test
    void allowlistedUrlsPass() {
        assertTrue(registry.isAllowlistedUrl("https://www.sebi.gov.in/sebiweb/other/OtherAction.do?doRecognisedFpi=yes&intmId=13"));
        assertTrue(registry.isAllowlistedUrl("https://www.rbi.org.in/"));
        assertTrue(registry.isAllowlistedUrl("https://cybercrime.gov.in/"));
        assertTrue(registry.isAllowlistedUrl("https://scores.sebi.gov.in/"));
    }

    @Test
    void hostileUrlsFail() {
        assertFalse(registry.isAllowlistedUrl("http://www.sebi.gov.in/"));
        assertFalse(registry.isAllowlistedUrl("https://sebi-official.com/verify"));
        assertFalse(registry.isAllowlistedUrl("https://sebi.gov.in.evil.com/"));
        assertFalse(registry.isAllowlistedUrl("https://example.com/sebi"));
        assertFalse(registry.isAllowlistedUrl(null));
        assertFalse(registry.isAllowlistedUrl("   "));
        assertFalse(registry.isAllowlistedUrl("not a url"));
    }

    @Test
    void evidenceDefaultsAreEnabledOfflineSafe() {
        EvidenceProperties props = new EvidenceProperties();
        assertTrue(props.isEnabled());
        assertTrue(props.isOfflineMode());
    }
}
