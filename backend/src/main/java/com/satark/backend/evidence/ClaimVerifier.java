package com.satark.backend.evidence;

import com.satark.backend.model.Claim;
import com.satark.backend.model.Evidence;

/**
 * Phase 11 (P4): claim-to-evidence workflow contract.
 *
 * <p>Pure function over a single extracted claim. Null-safe, deterministic,
 * no I/O. Implementations must NEVER fabricate registration status, sources,
 * URLs, or verification results — when a source cannot be checked, the
 * uncertainty is preserved in the status, not guessed away.
 */
public interface ClaimVerifier {

    /**
     * Verify one claim against offline rules.
     *
     * @param claim extracted claim; may be null
     * @return evidence with a canonical status and an allowlisted-or-null
     *         URL; never null
     */
    Evidence verify(Claim claim);
}
