package com.satark.backend.ai;

import java.time.Duration;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Phase 1 (P3 foundation): configuration holder for the future AI/NLP pipeline.
 *
 * <p>Enabled by default: {@code POST /api/analyze} runs the deterministic
 * P3 pipeline. Set {@code satark.ai.enabled=false} to restore the legacy
 * mock path (kept as fallback). No LLM provider SDK is required.
 *
 * <p>Safety: {@code maxInputChars} bounds how much untrusted user content is
 * ever forwarded to a future provider. Full user text must never be logged.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "satark.ai")
public class AiProperties {

    /** Master switch. When false, AnalysisService uses the legacy mock path. */
    private boolean enabled = true;

    /** Provider id: "none" | "openai" | "gemini" | etc. Reserved for Phase 1+. */
    private String provider = "none";

    /** Model id for the provider. Reserved for Phase 1+. */
    private String model = "none";

    /** HTTP timeout for future provider calls. */
    private Duration timeout = Duration.ofSeconds(10);

    /** Max chars of untrusted input forwarded to a future LLM. */
    private int maxInputChars = 4000;
}
