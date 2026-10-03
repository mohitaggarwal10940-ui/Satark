package com.satark.backend.evidence;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Phase 10 (P4): prioritized allowlist of official verification sources.
 *
 * <p>Priority (lower number wins): SEBI &gt; RBI &gt; NSE &gt; BSE &gt;
 * Sachet &gt; National Cyber Crime portal &gt; SEBI SCORES. These are the
 * official Indian investor-protection portals; a {@code sourceUrl} may only
 * ever be emitted if {@link #isAllowlistedUrl} accepts it. Nothing here
 * performs network I/O (MVP is offline — see {@code EvidenceProperties}).
 */
@Service
public class TrustedSourceRegistry {

    private final List<TrustedSource> sources = List.of(
            new TrustedSource("sebi", "SEBI", "https://www.sebi.gov.in", 1),
            new TrustedSource("rbi", "RBI", "https://www.rbi.org.in", 2),
            new TrustedSource("nse", "NSE", "https://www.nseindia.com", 3),
            new TrustedSource("bse", "BSE", "https://www.bseindia.com", 4),
            new TrustedSource("sachet", "RBI Sachet", "https://sachet.rbi.org.in", 5),
            new TrustedSource("cybercrime", "National Cyber Crime Portal", "https://cybercrime.gov.in", 6),
            new TrustedSource("scores", "SEBI SCORES", "https://scores.sebi.gov.in", 7));

    /** All sources in priority order. Never null, immutable. */
    public List<TrustedSource> allByPriority() {
        return sources;
    }

    /** Find by key (case-insensitive). Empty when unknown — never null. */
    public Optional<TrustedSource> findByKey(String key) {
        if (key == null || key.isBlank()) {
            return Optional.empty();
        }
        String k = key.trim().toLowerCase(Locale.ROOT);
        return sources.stream().filter(s -> s.key().equals(k)).findFirst();
    }

    /**
     * Whether a URL is safe to emit as {@code sourceUrl}: https scheme AND
     * exact host match against the allowlist (no sub-string tricks —
     * {@code sebi-official.com} or {@code sebi.gov.in.evil.com} fail).
     * Null/blank/malformed -&gt; false.
     */
    public boolean isAllowlistedUrl(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        final URI uri;
        try {
            uri = new URI(url.trim());
        } catch (Exception e) {
            return false;
        }
        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            return false;
        }
        String host = uri.getHost();
        if (host == null) {
            return false;
        }
        String h = host.toLowerCase(Locale.ROOT);
        for (TrustedSource s : sources) {
            String allowed;
            try {
                allowed = new URI(s.baseUrl()).getHost().toLowerCase(Locale.ROOT);
            } catch (Exception e) {
                continue;
            }
            if (h.equals(allowed)) {
                return true;
            }
        }
        return false;
    }
}
