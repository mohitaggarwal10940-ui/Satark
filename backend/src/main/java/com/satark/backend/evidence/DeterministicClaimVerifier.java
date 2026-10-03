package com.satark.backend.evidence;

import com.satark.backend.model.Claim;
import com.satark.backend.model.Evidence;
import java.util.Locale;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Phase 11 (P4): offline deterministic verifier. Zero new dependencies.
 *
 * <p>Rules (MVP, no network):
 * <ul>
 *   <li>Regulatory claim <em>with</em> a SEBI-style ID → UNVERIFIED: an ID was
 *       mentioned but cannot be checked offline; points at sebi.gov.in.</li>
 *   <li>Regulatory claim <em>without</em> an ID → UNVERIFIED: nothing to look
 *       up; points at sebi.gov.in.</li>
 *   <li>Guaranteed-return / urgency / unofficial-channel / unknown / null →
 *       INSUFFICIENT_EVIDENCE: no official source could confirm such content
 *       offline; payment guidance instead of a link.</li>
 * </ul>
 *
 * <p>VERIFIED_TRUE / VERIFIED_FALSE are NEVER emitted offline — they require
 * a checked trusted source, which only a future online mode may cite (and
 * then only with an allowlisted URL). Every emitted URL is re-checked
 * against {@code TrustedSourceRegistry.isAllowlistedUrl}; on any doubt the
 * URL is nulled rather than risked.
 */
@Service
@RequiredArgsConstructor
public class DeterministicClaimVerifier implements ClaimVerifier {

    private static final Pattern SEBI_ID = Pattern.compile("IN[AH][A-Z0-9]{7,12}");

    private final TrustedSourceRegistry registry;

    @Override
    public Evidence verify(Claim claim) {
        if (claim == null || claim.getClaimText() == null || claim.getClaimText().isBlank()) {
            return evidence("(no claim provided)", EvidenceStatus.INSUFFICIENT_EVIDENCE,
                    "No claim content was available, so no verification could be attempted.", null);
        }
        String text = claim.getClaimText();
        String category = claim.getCategory() == null ? "" : claim.getCategory().trim().toUpperCase(Locale.ROOT);

        return switch (category) {
            case "REGULATORY_IMPERSONATION", "REGISTRATION_CLAIM" -> verifyRegulatory(text);
            case "GUARANTEED_RETURN" -> evidence(text, EvidenceStatus.INSUFFICIENT_EVIDENCE,
                    "Promised returns cannot be confirmed from any official source in this offline check. "
                            + "Treat guaranteed high returns as a warning signal, not a fact.",
                    null);
            case "URGENCY_PRESSURE" -> evidence(text, EvidenceStatus.INSUFFICIENT_EVIDENCE,
                    "Pressure tactics carry no verifiable factual content. "
                            + "Take time to verify everything else before acting.",
                    null);
            case "UNOFFICIAL_COMMUNICATION" -> evidence(text, EvidenceStatus.INSUFFICIENT_EVIDENCE,
                    "The channel or payment handle could not be verified against official records offline. "
                            + "Do not transfer money to unverified handles.",
                    null);
            default -> evidence(text, EvidenceStatus.INSUFFICIENT_EVIDENCE,
                    "This claim type has no applicable official verification source in the offline check.",
                    null);
        };
    }

    private Evidence verifyRegulatory(String text) {
        boolean hasId = SEBI_ID.matcher(text).find();
        String sebiHome = "https://www.sebi.gov.in";
        String url = registry.isAllowlistedUrl(sebiHome) ? sebiHome : null;
        if (hasId) {
            return evidence(text, EvidenceStatus.UNVERIFIED,
                    "A registration identifier was mentioned but could not be checked offline. "
                            + "Verify it yourself on the official SEBI portal before acting.",
                    url);
        }
        return evidence(text, EvidenceStatus.UNVERIFIED,
                "No registration number was provided, so the affiliation claim cannot be confirmed. "
                        + "Registered advisors must state their registration ID; check it on the official SEBI portal.",
                url);
    }

    private static Evidence evidence(String claim, EvidenceStatus status, String details, String url) {
        return new Evidence(claim, status.getCode(), details, url);
    }
}
