package com.satark.backend.risk;

import com.satark.backend.model.Evidence;
import com.satark.backend.model.RiskSignal;
import java.util.List;

/**
 * Phase 13: deterministic scoring contract — the SOLE authority for the
 * numeric {@code riskScore} / {@code riskLevel}.
 *
 * <p>Risk rule: no LLM (present or future) may decide the score. The scorer
 * takes only structured signals + evidence — there is deliberately NO score
 * input, so an LLM output can never override the result. Same inputs always
 * yield the same score.
 */
public interface RiskScorer {

    /** Scored concern. Fields never null. */
    record Result(int score, String riskLevel) {
    }

    /**
     * Score structured findings. Null/empty inputs → 0/LOW (never null,
     * never throws).
     */
    Result score(List<RiskSignal> signals, List<Evidence> evidence);
}
