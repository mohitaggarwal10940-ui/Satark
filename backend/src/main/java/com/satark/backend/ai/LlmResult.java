package com.satark.backend.ai;

import java.util.List;

public record LlmResult(
        String explanation,
        List<String> recommendedActions
) {
}