package com.satark.backend.risk;

import com.satark.backend.model.Evidence;
import com.satark.backend.model.RiskSignal;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

/**
 * Phase 13: calibrated concern scorer. Zero new dependencies.
 *
 * <p>Formula: {@code score = max(criticalFloor, min(100, Σ severityWeights +
 * evidenceAdjustments))}, clamped to 0–100.
 *
 * <p>Severity weights: CRITICAL 35, HIGH 22, MEDIUM 12, LOW 5 (unknown →
 * MEDIUM weight, never dropped silently into zero). Evidence adjustments:
 * VERIFIED_FALSE +15 (a trusted source contradicted the claim — strong
 * danger), VERIFIED_TRUE −10 (a trusted source confirmed it — less concern),
 * all uncertainty statuses +0 (preserved, never guessed). Any CRITICAL
 * signal forces a floor of 80 (README critical-combo rule).
 */
@Service
public class DeterministicRiskScorer implements RiskScorer {

    @Override
    public Result score(List<RiskSignal> signals, List<Evidence> evidence) {
        int base = 0;
        boolean critical = false;
        if (signals != null) {
            for (RiskSignal s : signals) {
                if (s == null) {
                    continue;
                }
                String sev = s.getSeverity() == null ? "MEDIUM" : s.getSeverity().trim().toUpperCase(Locale.ROOT);
                base += switch (sev) {
                    case "CRITICAL" -> 35;
                    case "HIGH" -> 22;
                    case "LOW" -> 5;
                    default -> 12;
                };
                if ("CRITICAL".equals(sev)) {
                    critical = true;
                }
            }
        }
        int adjustment = 0;
        if (evidence != null) {
            for (Evidence e : evidence) {
                if (e == null || e.getStatus() == null) {
                    continue;
                }
                String st = e.getStatus().trim().toUpperCase(Locale.ROOT);
                if ("VERIFIED_FALSE".equals(st)) {
                    adjustment += 15;
                } else if ("VERIFIED_TRUE".equals(st)) {
                    adjustment -= 10;
                }
            }
        }
        int capped = Math.max(0, Math.min(100, base + adjustment));
        int score = critical ? Math.max(80, capped) : capped;
        return new Result(score, RiskLevel.fromScore(score).getCode());
    }
}
