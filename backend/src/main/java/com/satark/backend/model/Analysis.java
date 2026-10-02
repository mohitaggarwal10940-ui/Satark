package com.satark.backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "analyses")
public class Analysis {

    @Id
    private String id;

    private String inputType;

    private String inputText;

    private String language;

    private int riskScore;

    private String riskLevel;

    private List<Claim> claims;

    private List<RiskSignal> riskSignals;

    private List<Evidence> evidence;

    private String explanation;

    private List<String> recommendedActions;

    private LocalDateTime createdAt;
}