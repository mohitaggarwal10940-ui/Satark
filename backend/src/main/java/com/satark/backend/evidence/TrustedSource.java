package com.satark.backend.evidence;

/**
 * Phase 9 (P4): trusted-source value type.
 *
 * <p>Stub for Phase 10, which will add the {@code TrustedSourceRegistry}
 * allowlist (SEBI &gt; RBI &gt; NSE/BSE &gt; Sachet &gt; cybercrime/SCORES).
 * A source URL may only ever be emitted if it is built from one of these
 * entries — free-form URLs are forbidden by the evidence rule.
 *
 * @param key stable identifier, e.g. "sebi"
 * @param displayName human label, e.g. "SEBI"
 * @param baseUrl allowlisted https origin, e.g. "https://www.sebi.gov.in"
 * @param priority lower wins when several sources could cover a claim
 */
public record TrustedSource(String key, String displayName, String baseUrl, int priority) {

    public TrustedSource {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("key is required");
        }
        if (baseUrl == null || !baseUrl.startsWith("https://")) {
            throw new IllegalArgumentException("baseUrl must be an https origin");
        }
    }
}
