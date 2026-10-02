package com.satark.backend.controller;

import com.satark.backend.dto.AnalysisRequest;
import com.satark.backend.dto.AnalysisResponse;
import com.satark.backend.service.AnalysisService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisService analysisService;

    @PostMapping("/analyze")
    public ResponseEntity<AnalysisResponse> analyze(
            @Valid @RequestBody AnalysisRequest request
    ) {
        AnalysisResponse response = analysisService.analyze(request);

        return ResponseEntity.ok(response);
    }
}