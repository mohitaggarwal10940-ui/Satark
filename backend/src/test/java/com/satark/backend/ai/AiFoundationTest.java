package com.satark.backend.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * AI pipeline is enabled by default; the NoOp fallback stays unavailable
 * so deterministic logic always has a safe offline path.
 * Pure unit tests — no Spring context, no MongoDB required.
 */
class AiFoundationTest {

    @Test
    void defaultsAreEnabled() {
        AiProperties props = new AiProperties();
        assertTrue(props.isEnabled());
        assertEquals("none", props.getProvider());
        assertEquals("none", props.getModel());
        assertEquals(4000, props.getMaxInputChars());
    }

    @Test
    void noOpClientIsNeverAvailable() {
        AiProperties props = new AiProperties();
        NoOpLlmClient client = new NoOpLlmClient(props);
        assertFalse(client.isAvailable());
        assertEquals("none", client.getProvider());
    }
}
