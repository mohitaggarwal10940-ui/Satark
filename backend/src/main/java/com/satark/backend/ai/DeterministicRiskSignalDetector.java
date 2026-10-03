package com.satark.backend.ai;

import com.satark.backend.model.Claim;
import com.satark.backend.model.RiskSignal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Phase 3 (P3): deterministic claim -&gt; signal mapper. Zero new dependencies.
 *
 * <p>NOT wired into AnalysisService yet (integration is Phase 7); the live
 * mock path is untouched. Emits at most one signal per category in fixed
 * order; descriptions are static safety copy (no user text echoed, no URLs
 * fabricated, no investment advice, no scam-probability language).
 */
@Service
public class DeterministicRiskSignalDetector implements RiskSignalDetector {

    @Override
    public List<RiskSignal> detect(List<Claim> claims, String rawText) {
        if (claims == null || claims.isEmpty()) {
            return List.of();
        }
        Map<ClaimCategory, String> confidenceByCategory = new EnumMap<>(ClaimCategory.class);
        for (Claim c : claims) {
            if (c == null) {
                continue;
            }
            ClaimCategory cat = RegulatoryRuleEngine.canonical(c.getCategory());
            if (cat == null) {
                continue;
            }
            // Keep highest confidence per category (HIGH beats MEDIUM).
            String conf = c.getConfidence() == null ? "MEDIUM" : c.getConfidence();
            confidenceByCategory.merge(cat, conf,
                    (a, b) -> "HIGH".equalsIgnoreCase(a) ? a : b);
        }

        boolean hasGuaranteed = confidenceByCategory.containsKey(ClaimCategory.GUARANTEED_RETURN);
        boolean hasUnofficial = confidenceByCategory.containsKey(ClaimCategory.UNOFFICIAL_COMMUNICATION);
        boolean criticalCombo = RegulatoryRuleEngine.isCriticalCombo(hasGuaranteed, hasUnofficial);

        List<RiskSignal> out = new ArrayList<>();
        for (ClaimCategory cat : ClaimCategory.values()) {
            if (!confidenceByCategory.containsKey(cat)) {
                continue;
            }
            String conf = confidenceByCategory.get(cat);
            switch (cat) {
                case GUARANTEED_RETURN -> out.add(new RiskSignal(
                        "Unrealistic Guaranteed Returns",
                        "Legitimate equity investments cannot guarantee fixed returns. "
                                + "SEBI regulations prohibit promising guaranteed profit.",
                        criticalCombo ? "CRITICAL" : RegulatoryRuleEngine.guaranteedSeverity(conf)));
                case REGULATORY_IMPERSONATION -> out.add(new RiskSignal(
                        "Unverified Advisor Credentials",
                        "The message claims regulatory affiliation without a verifiable "
                                + "registration ID. Check the advisor on sebi.gov.in before acting.",
                        RegulatoryRuleEngine.regulatorySeverity(conf)));
                case URGENCY_PRESSURE -> out.add(new RiskSignal(
                        "Artificial Urgency and Scarcity",
                        "Pressure tactics like limited slots or act-now demands are designed "
                                + "to rush decisions before claims can be verified.",
                        RegulatoryRuleEngine.urgencySeverity(conf)));
                case UNOFFICIAL_COMMUNICATION -> out.add(new RiskSignal(
                        "Unofficial Payment or Channel",
                        "Registered entities use official accounts and channels, not personal "
                                + "UPI handles or closed VIP groups.",
                        RegulatoryRuleEngine.unofficialSeverity(conf)));
                default -> {
                }
            }
        }
        return List.copyOf(out);
    }
}
