package com.satark.backend.evidence;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Phase 10 (P4): configuration for the evidence layer.
 *
 * <p>Disabled-adjacent defaults: the MVP verifier is offline
 * ({@code offline-mode=true}), meaning it never contacts the network and
 * always preserves uncertainty (UNVERIFIED / INSUFFICIENT_EVIDENCE /
 * UNAVAILABLE). Live lookups are future scope and must go through
 * {@code TrustedSourceRegistry.isAllowlistedUrl} when they arrive.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "satark.evidence")
public class EvidenceProperties {

    /** Master switch for EvidenceService wiring. False restores mock evidence. */
    private boolean enabled = true;

    /** When true, no network verification is attempted. Default true for MVP safety. */
    private boolean offlineMode = true;
}
