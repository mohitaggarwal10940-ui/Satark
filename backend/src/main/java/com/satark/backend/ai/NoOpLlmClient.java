package com.satark.backend.ai;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Phase 1 deterministic fallback: always unavailable.
 *
 * <p>Keeps the current mock path working with zero new dependencies.
 * Future provider clients (OpenAI/Gemini/...) will implement
 * {@link LlmClient} behind {@code satark.ai.enabled=true} and take
 * precedence via {@code @Primary} / {@code @ConditionalOnProperty},
 * while this bean remains the safe offline default.
 */
@Service
@ConditionalOnProperty(
        name = "satark.ai.provider",
        havingValue = "none",
        matchIfMissing = true
)
@RequiredArgsConstructor
public class NoOpLlmClient implements LlmClient {
    private final AiProperties properties;

    @Override
    public String getProvider() {
        return properties.getProvider() == null ? "none" : properties.getProvider();
    }

    @Override
    public boolean isAvailable() {
        // Phase 1: never available -> callers must use deterministic/mock path.
        // Phase 5 will expand this into a template-based fallback explainer.
        return false;
    }
    @Override
    public LlmResult analyze(String rawText, String language, String prompt) {
        throw new IllegalStateException("No LLM provider is available");
    }
}
