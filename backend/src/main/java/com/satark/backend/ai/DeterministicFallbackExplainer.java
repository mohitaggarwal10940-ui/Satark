package com.satark.backend.ai;

import com.satark.backend.model.Claim;
import com.satark.backend.model.RiskSignal;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

/**
 * Phase 5 (P3): deterministic offline fallback.
 *
 * <p>Used whenever the LLM is disabled, unavailable, or times out (and until
 * Phase 7 wires the real pipeline, it documents the safe default the service
 * must fall back to). Pure template logic: no I/O, no model call, no user
 * text echoed, no fabricated facts.
 *
 * <p>Language MVP: English + Hindi templates; any other code falls back to
 * English. Full 8-language coverage is Phase 6/8. Safety copy rules: no
 * investment recommendations, no scam-probability language, always preserve
 * uncertainty and point to 1930 / cybercrime.gov.in / sebi.gov.in.
 */
@Service
public class DeterministicFallbackExplainer {

    /** Safe content when the AI path is unavailable. Never null fields. */
    public record FallbackContent(String explanation, List<String> recommendedActions) {
    }

    public FallbackContent explain(List<Claim> claims, List<RiskSignal> signals, String language) {
        String lang = PromptInjectionGuard.normalizeLanguage(language);
        int claimCount = claims == null ? 0 : (int) claims.stream().filter(c -> c != null).count();
        int signalCount = signals == null ? 0 : (int) signals.stream().filter(s -> s != null).count();
        boolean critical = signals != null && signals.stream()
                .anyMatch(s -> s != null && "CRITICAL".equalsIgnoreCase(s.getSeverity()));

        if ("hi".equals(lang)) {
            return hindi(claimCount, signalCount, critical);
        }
        return english(claimCount, signalCount, critical);
    }

    private FallbackContent english(int claims, int signals, boolean critical) {
        String explanation;
        if (signals == 0 && claims == 0) {
            explanation = "No specific warning patterns were detected by the offline check. "
                    + "The available information is insufficient to confirm safety, "
                    + "so please verify independently before acting.";
        } else if (critical) {
            explanation = "This message shows " + signals + " warning signal"
                    + (signals == 1 ? "" : "s")
                    + " including a critical pattern commonly seen in unregistered schemes. "
                    + "The available information is insufficient to verify its claims. "
                    + "Do not act until independently verified.";
        } else {
            explanation = "This message shows " + signals + " warning signal"
                    + (signals == 1 ? "" : "s")
                    + " commonly seen in suspicious investment content. "
                    + "The available information is insufficient to verify its claims. "
                    + "Do not act until independently verified.";
        }
        return new FallbackContent(explanation, List.of(
                "Do not transfer money until the claims are independently verified.",
                "Verify the advisor and registration details on the official SEBI website (sebi.gov.in).",
                "If fraud has occurred, call the National Cyber Crime Helpline 1930 or report at cybercrime.gov.in."));
    }

    private FallbackContent hindi(int claims, int signals, boolean critical) {
        String explanation;
        if (signals == 0 && claims == 0) {
            explanation = "ऑफ़लाइन जांच में कोई विशिष्ट चेतावनी पैटर्न नहीं मिला। "
                    + "सुरक्षा की पुष्टि के लिए उपलब्ध जानकारी अपर्याप्त है, "
                    + "इसलिए कार्रवाई से पहले स्वतंत्र रूप से सत्यापन करें।";
        } else if (critical) {
            explanation = "इस संदेश में " + signals + " चेतावनी संकेत मिले हैं, "
                    + "जिनमें अपंजीकृत योजनाओं में दिखने वाला गंभीर पैटर्न शामिल है। "
                    + "दावों के सत्यापन हेतु उपलब्ध जानकारी अपर्याप्त है। "
                    + "स्वतंत्र सत्यापन के बिना कोई कार्रवाई न करें।";
        } else {
            explanation = "इस संदेश में " + signals + " चेतावनी संकेत मिले हैं, "
                    + "जो संदिग्ध निवेश सामग्री में अक्सर देखे जाते हैं। "
                    + "दावों के सत्यापन हेतु उपलब्ध जानकारी अपर्याप्त है। "
                    + "स्वतंत्र सत्यापन के बिना कोई कार्रवाई न करें।";
        }
        return new FallbackContent(explanation, List.of(
                "स्वतंत्र सत्यापन होने तक कोई धनराशि ट्रांसफर न करें।",
                "सेबी की आधिकारिक वेबसाइट (sebi.gov.in) पर सलाहकार और पंजीकरण विवरण जांचें।",
                "यदि धोखाधड़ी हुई है, तो राष्ट्रीय साइबर हेल्पलाइन 1930 पर कॉल करें या cybercrime.gov.in पर रिपोर्ट करें।"));
    }

    /** Language this explainer can natively render (others fall back to English). */
    public static boolean supports(String language) {
        if (language == null) {
            return false;
        }
        String l = language.trim().toLowerCase(Locale.ROOT);
        return l.equals("en") || l.equals("hi");
    }
}
