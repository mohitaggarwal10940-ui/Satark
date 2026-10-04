package com.satark.backend.ai;

public interface LlmClient {

    String getProvider();

    boolean isAvailable();

    LlmResult analyze(
            String rawText,
            String language,
            String prompt
    );
}