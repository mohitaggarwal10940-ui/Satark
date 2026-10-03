package com.satark.backend.ai;

import com.satark.backend.model.Claim;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Phase 2 (P3): regex/lexicon claim extractor. Zero new dependencies.
 *
 * <p>NOT wired into AnalysisService yet (integration is Phase 7); the live
 * mock path is untouched. Output is at most one claim per canonical
 * category, in fixed order, so results are stable and MVP-sized.
 *
 * <p>Multilingual MVP: English + Hindi keywords plus language-agnostic
 * patterns (%, SEBI IDs, UPI handles, phone numbers). Other vernacular
 * languages fall back to the agnostic patterns; full coverage is Phase 8.
 */
@Service
@RequiredArgsConstructor
public class DeterministicClaimExtractor implements ClaimExtractor {

    private final AiProperties properties;

    private static final int SNIPPET_MAX = 200;

    // Language-agnostic strong patterns (HIGH confidence).
    private static final Pattern SEBI_ID = Pattern.compile("IN[AH][A-Z0-9]{7,12}");
    private static final Pattern PERCENT_RETURN =
            Pattern.compile("\\d+\\s*%[^.\\n।]{0,40}(return|profit|interest|monthly|per\\s*day|रिटर्न|मुनाफा|लाभ)");
    private static final Pattern GUARANTEED_PERCENT =
            Pattern.compile("(guarantee|guaranteed|assured|fixed|risk[- ]?free|zero\\s*risk|गारंटी|पक्का)[^.\\n।]{0,40}\\d+\\s*%");
    private static final Pattern UPI_HANDLE =
            Pattern.compile("[\\w.\\-]{2,}@[a-zA-Z]{2,}");
    private static final Pattern PHONE = Pattern.compile("(\\+?91[\\-\\s]?)?[6-9]\\d{9}");

    // Keyword lexicons (case-insensitive, MEDIUM confidence).
    private static final Pattern GUARANTEED_KW = Pattern.compile(
            "guarantee|assured|fixed return|zero risk|risk-free|risk free|double (your |the )?money|double money|jackpot|multibagger|गारंटी|निश्चित रिटर्न|पक्का मुनाफा",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern REGULATORY_KW = Pattern.compile(
            "sebi|\\brbi\\b|nse|bse|regist(ered|ration)|analyst|advisor|adviser|certificat|license|licence|सेबी|पंजीकृत|पंजीकरण|सरकारी",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern URGENCY_KW = Pattern.compile(
            "immediately|urgent|hurry|last chance|only \\d+ slots?|valid for \\d+ minutes?|act now|buy immediately|pay .*today|limited offer|today only|तुरंत|जल्दी|आखिरी मौका|सीमित|आज ही",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern UNOFFICIAL_KW = Pattern.compile(
            "telegram|whatsapp|vip (group|channel)|private (tip|group|channel)|join.*group|upi|pay .*upi|dm (me|now)|inbox",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    @Override
    public List<Claim> extract(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        int cap = properties != null ? properties.getMaxInputChars() : 4000;
        String bounded = text.length() > cap ? text.substring(0, cap) : text;

        Map<ClaimCategory, Claim> found = new EnumMap<>(ClaimCategory.class);
        for (String sentence : splitSentences(bounded)) {
            String snippet = snippet(sentence);
            if (snippet.isEmpty()) {
                continue;
            }
            matchGuaranteed(snippet, found);
            matchRegulatory(snippet, bounded, found);
            matchUrgency(snippet, found);
            matchUnofficial(snippet, bounded, found);
        }
        List<Claim> out = new ArrayList<>();
        for (ClaimCategory cat : ClaimCategory.values()) {
            if (found.containsKey(cat)) {
                out.add(found.get(cat));
            }
        }
        return List.copyOf(out);
    }

    private void matchGuaranteed(String snippet, Map<ClaimCategory, Claim> found) {
        if (found.containsKey(ClaimCategory.GUARANTEED_RETURN)) {
            return;
        }
        boolean strong = contains(GUARANTEED_PERCENT, snippet)
                || contains(PERCENT_RETURN, snippet)
                || (contains(GUARANTEED_KW, snippet) && snippet.matches("(?is).*\\d+.*%.*"));
        boolean weak = contains(GUARANTEED_KW, snippet);
        if (strong || weak) {
            found.put(ClaimCategory.GUARANTEED_RETURN,
                    new Claim(snippet, ClaimCategory.GUARANTEED_RETURN.getCode(), strong ? "HIGH" : "MEDIUM"));
        }
    }

    private void matchRegulatory(String snippet, String full, Map<ClaimCategory, Claim> found) {
        if (found.containsKey(ClaimCategory.REGULATORY_IMPERSONATION)) {
            return;
        }
        boolean strong = contains(SEBI_ID, snippet) || contains(SEBI_ID, full);
        boolean weak = contains(REGULATORY_KW, snippet);
        if (strong || weak) {
            found.put(ClaimCategory.REGULATORY_IMPERSONATION,
                    new Claim(snippet, ClaimCategory.REGULATORY_IMPERSONATION.getCode(), strong ? "HIGH" : "MEDIUM"));
        }
    }

    private void matchUrgency(String snippet, Map<ClaimCategory, Claim> found) {
        if (found.containsKey(ClaimCategory.URGENCY_PRESSURE)) {
            return;
        }
        if (contains(URGENCY_KW, snippet)) {
            boolean strong = snippet.toLowerCase(Locale.ROOT).contains("only")
                    || snippet.toLowerCase(Locale.ROOT).contains("immediately");
            found.put(ClaimCategory.URGENCY_PRESSURE,
                    new Claim(snippet, ClaimCategory.URGENCY_PRESSURE.getCode(), strong ? "HIGH" : "MEDIUM"));
        }
    }

    private void matchUnofficial(String snippet, String full, Map<ClaimCategory, Claim> found) {
        if (found.containsKey(ClaimCategory.UNOFFICIAL_COMMUNICATION)) {
            return;
        }
        boolean strong = contains(UPI_HANDLE, snippet)
                || contains(PHONE, snippet)
                || contains(UPI_HANDLE, full);
        boolean weak = contains(UNOFFICIAL_KW, snippet);
        if (strong || weak) {
            found.put(ClaimCategory.UNOFFICIAL_COMMUNICATION,
                    new Claim(snippet, ClaimCategory.UNOFFICIAL_COMMUNICATION.getCode(), strong ? "HIGH" : "MEDIUM"));
        }
    }

    private static boolean contains(Pattern p, String s) {
        Matcher m = p.matcher(s);
        return m.find();
    }

    /** Split on sentence boundaries incl. Devanagari danda. Never returns null. */
    static List<String> splitSentences(String text) {
        String[] parts = text.split("[.\\n!\\?।]+");
        List<String> out = new ArrayList<>();
        for (String p : parts) {
            String t = p.trim();
            if (!t.isEmpty()) {
                out.add(t);
            }
        }
        if (out.isEmpty() && !text.isBlank()) {
            out.add(text.trim());
        }
        return out;
    }

    private static String snippet(String sentence) {
        String s = sentence.trim().replaceAll("\\s+", " ");
        return s.length() > SNIPPET_MAX ? s.substring(0, SNIPPET_MAX) : s;
    }
}
