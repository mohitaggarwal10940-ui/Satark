package com.satark.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AnalysisRequest(

        @NotBlank(message = "Input type is required")
        String inputType,

        @NotBlank(message = "Text cannot be empty")
        String text,

        String language
) {
}