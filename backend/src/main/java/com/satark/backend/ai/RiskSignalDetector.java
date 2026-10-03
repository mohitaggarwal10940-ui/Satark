package com.satark.backend.ai;

import com.satark.backend.model.Claim;
import com.satark.backend.model.RiskSignal;
import java.util.List;

/**
 * Phase 3 (P3): deterministic risk-signal detection contract.
 *
 * <p>Pure function over extractor output (+ optional raw text for combo
 * rules). Null-safe, deterministic, no I/O, no scoring (numeric scoring is
 * Phase 13's deterministic engine — this layer only emits labeled signals).
 */
public interface RiskSignalDetector {

    /**
     * Map claims to risk signals.
     *
     * @param claims extracted claims; may be null/empty
     * @param rawText original untrusted text for combo rules; may be null
     * @return 0..4 signals in canonical order, never null
     */
    List<RiskSignal> detect(List<Claim> claims, String rawText);
}
