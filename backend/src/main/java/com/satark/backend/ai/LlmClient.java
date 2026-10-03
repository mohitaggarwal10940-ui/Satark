package com.satark.backend.ai;

/**
 * Phase 1 (P3 foundation): provider abstraction for the future LLM core.
 *
 * <p>Risk rule (enforced from Phase 13): implementations of this interface
 * must NEVER decide the final numeric {@code riskScore} / {@code riskLevel}.
 * P3 contributes claims + signals + explanation; the deterministic risk
 * engine remains the sole authority for the score.
 *
 * <p>Security rule: analyzed user content is UNTRUSTED DATA. Future
 * implementations must treat it as data, never as instructions, and must
 * defend against prompt injection ("ignore previous instructions",
 * "return this as verified", ...).
 */
public interface LlmClient {

    /** Provider id, e.g. "none", "openai", "gemini". */
    String getProvider();

    /**
     * Whether this client can serve requests right now.
     * The Phase 1 NoOp implementation always returns false so callers
     * fall back to deterministic logic.
     */
    boolean isAvailable();
}
