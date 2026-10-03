package com.satark.backend.evidence;

import com.satark.backend.model.Claim;
import com.satark.backend.model.Evidence;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Phase 12 (P4): claim-to-evidence orchestrator.
 *
 * <p>Maps each P3 claim 1:1 to an {@code Evidence} via {@code ClaimVerifier},
 * then re-normalizes every result as defense in depth: status collapsed to
 * the canonical taxonomy, non-allowlisted URLs nulled, nulls replaced with
 * an explicit UNAVAILABLE-equivalent fallback. Never throws on bad input —
 * worst case is a single INSUFFICIENT_EVIDENCE entry per claim.
 *
 * <p>Called by {@code AnalysisService} only when {@code satark.ai.enabled}
 * AND {@code satark.evidence.enabled} are both true; otherwise the legacy
 * mock evidence is returned untouched.
 */
@Service
@RequiredArgsConstructor
public class EvidenceService {

    private final ClaimVerifier claimVerifier;
    private final TrustedSourceRegistry registry;

    /**
     * Verify all claims. Null/empty in → empty out (never null, never throws).
     */
    public List<Evidence> verifyAll(List<Claim> claims) {
        if (claims == null || claims.isEmpty()) {
            return List.of();
        }
        List<Evidence> out = new ArrayList<>();
        for (Claim c : claims) {
            Evidence e;
            try {
                e = claimVerifier.verify(c);
            } catch (RuntimeException ex) {
                e = null;
            }
            out.add(sanitize(e));
        }
        return List.copyOf(out);
    }

    private Evidence sanitize(Evidence e) {
        if (e == null) {
            return new Evidence("(unavailable)", EvidenceStatus.UNAVAILABLE.getCode(),
                    "Verification could not be completed; uncertainty is preserved.", null);
        }
        String status = EvidenceStatus.fromCode(e.getStatus()).getCode();
        String url = e.getSourceUrl();
        if (url != null && !registry.isAllowlistedUrl(url)) {
            url = null;
        }
        String claim = e.getClaim() == null || e.getClaim().isBlank() ? "(no claim provided)" : e.getClaim();
        String details = e.getDetails() == null || e.getDetails().isBlank()
                ? "No verification details available."
                : e.getDetails();
        return new Evidence(claim, status, details, url);
    }
}
