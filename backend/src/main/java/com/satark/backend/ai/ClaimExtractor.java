package com.satark.backend.ai;

import com.satark.backend.model.Claim;
import java.util.List;

/**
 * Phase 2 (P3): deterministic claim-extraction contract.
 *
 * <p>Implementations are pure functions over UNTRUSTED user text: null-safe,
 * deterministic (same input -&gt; same output), no I/O, no logging of input,
 * no verification (verification is P4's job). Injection phrases in the text
 * ("ignore previous instructions", ...) are treated as data and never obeyed.
 */
public interface ClaimExtractor {

    /**
     * Extract atomic claims from raw text.
     *
     * @param text untrusted user content; may be null/blank
     * @return 0..4 claims in canonical category order, never null
     */
    List<Claim> extract(String text);
}
