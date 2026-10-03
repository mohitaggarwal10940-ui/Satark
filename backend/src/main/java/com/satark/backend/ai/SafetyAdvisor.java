package com.satark.backend.ai;

import com.satark.backend.model.Claim;
import com.satark.backend.model.RiskSignal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Phase 6 (P3): multilingual explanation + safety recommendations.
 *
 * <p>Deterministic template layer covering all 8 SATARK languages
 * (en/hi/ta/te/bn/mr/gu/kn, matching Android {@code SupportedLanguage}).
 * English/Hindi reuse the Phase 5 fallback templates; the six additional
 * languages add native templates here. Unknown codes fall back to English.
 *
 * <p>NOT wired into AnalysisService yet (integration is Phase 7). When a
 * future LLM is enabled, its text will be validated and, on any failure,
 * replaced by this advisor's output — never by invented content.
 *
 * <p>Safety rules: no investment recommendations, no scam-probability
 * language, uncertainty preserved, 1930 / cybercrime.gov.in referenced,
 * no user text echoed.
 */
@Service
@RequiredArgsConstructor
public class SafetyAdvisor {

    private final DeterministicFallbackExplainer fallback;

    /** Localized safety content. Fields never null. */
    public record Advice(String explanation, List<String> recommendedActions) {
    }

    public Advice advise(List<Claim> claims, List<RiskSignal> signals, String language) {
        String lang = PromptInjectionGuard.normalizeLanguage(language);
        return switch (lang) {
            case "hi", "en" -> {
                var f = fallback.explain(claims, signals, lang);
                yield new Advice(f.explanation(), f.recommendedActions());
            }
            case "ta" -> tamil(claims, signals);
            case "te" -> telugu(claims, signals);
            case "bn" -> bengali(claims, signals);
            case "mr" -> marathi(claims, signals);
            case "gu" -> gujarati(claims, signals);
            case "kn" -> kannada(claims, signals);
            default -> {
                var f = fallback.explain(claims, signals, "en");
                yield new Advice(f.explanation(), f.recommendedActions());
            }
        };
    }

    private static int signalCount(List<RiskSignal> signals) {
        if (signals == null) {
            return 0;
        }
        return (int) signals.stream().filter(s -> s != null).count();
    }

    private static boolean critical(List<RiskSignal> signals) {
        return signals != null && signals.stream()
                .anyMatch(s -> s != null && "CRITICAL".equalsIgnoreCase(s.getSeverity()));
    }

    private Advice tamil(List<Claim> claims, List<RiskSignal> signals) {
        int n = signalCount(signals);
        String explanation = n == 0
                ? "ஆஃப்லைன் சோதனையில் குறிப்பிட்ட எச்சரிக்கை வடிவங்கள் எதுவும் கண்டறியப்படவில்லை. "
                        + "பாதுகாப்பை உறுதிப்படுத்தப் போதிய தகவல் இல்லை; சுயாதீனமாகச் சரிபார்க்கவும்."
                : "இந்த செய்தியில் " + n + " எச்சரிக்கை சமிக்ஞைகள் உள்ளன"
                        + (critical(signals) ? ", இதில் பதிவு செய்யப்படாத திட்டங்களில் காணப்படும் தீவிர வடிவம் உள்ளது" : "")
                        + ". உரிமைகோரல்களை உறுதிப்படுத்தப் போதிய ஆதாரங்கள் இல்லை. "
                        + "சுயாதீனமாகச் சரிபார்க்கும் வரை செயல்பட வேண்டாம்.";
        return new Advice(explanation, List.of(
                "முழுமையாகச் சரிபார்க்கும் வரை எந்தப் பணப் பரிவர்த்தனையும் செய்யாதீர்கள்.",
                "அசல் செய்தி, ஸ்கிரீன்ஷாட் மற்றும் கட்டண விவரங்களைப் பாதுகாக்கவும்; அதிகாரப்பூர்வ செபி தளத்தில் (sebi.gov.in) ஆலோசகர் பதிவைச் சரிபார்க்கவும்.",
                "மோசடி நடந்திருந்தால், தேசிய சைபர் குற்ற உதவி எண் 1930-ஐ அழைக்கவும் அல்லது cybercrime.gov.in-ல் புகார் அளிக்கவும்."));
    }

    private Advice telugu(List<Claim> claims, List<RiskSignal> signals) {
        int n = signalCount(signals);
        String explanation = n == 0
                ? "ఆఫ్‌లైన్ తనిఖీలో నిర్దిష్ట హెచ్చరిక నమూనాలు కనుగొనబడలేదు. "
                        + "భద్రతను నిర్ధారించడానికి సమాచారం సరిపోదు; స్వతంత్రంగా ధృవీకరించండి."
                : "ఈ సందేశంలో " + n + " హెచ్చరిక సంకేతాలు ఉన్నాయి"
                        + (critical(signals) ? ", వీటిలో నమోదుకాని పథకాలలో కనిపించే తీవ్రమైన నమూనా ఉంది" : "")
                        + ". క్లెయిమ్‌లను ధృవీకరించడానికి తగిన ఆధారాలు లేవు. "
                        + "స్వతంత్రంగా ధృవీకరించే వరకు చర్య తీసుకోవద్దు.";
        return new Advice(explanation, List.of(
                "పూర్తిగా ధృవీకరించబడే వరకు ఎటువంటి డబ్బును బదిలీ చేయవద్దు.",
                "అసలు సందేశం, స్క్రీన్‌షాట్ మరియు చెల్లింపు వివరాలను భద్రపరచండి; అధికారిక సెబి పోర్టల్ (sebi.gov.in)లో సలహాదారు నమోదును తనిఖీ చేయండి.",
                "మోసం జరిగితే, జాతీయ సైబర్ క్రైమ్ హెల్ప్‌లైన్ 1930కు కాల్ చేయండి లేదా cybercrime.gov.inలో ఫిర్యాదు చేయండి."));
    }

    private Advice bengali(List<Claim> claims, List<RiskSignal> signals) {
        int n = signalCount(signals);
        String explanation = n == 0
                ? "অফলাইন পরীক্ষায় কোনো নির্দিষ্ট সতর্কতা ধরন পাওয়া যায়নি। "
                        + "নিরাপত্তা নিশ্চিত করার মতো পর্যাপ্ত তথ্য নেই; স্বাধীনভাবে যাচাই করুন।"
                : "এই বার্তায় " + n + "টি সতর্কতা সংকেত পাওয়া গেছে"
                        + (critical(signals) ? ", যার মধ্যে অনিবন্ধিত স্কিমগুলিতে দেখা যায় এমন গুরুতর ধরন রয়েছে" : "")
                        + "। দাবিগুলি যাচাই করার জন্য পর্যাপ্ত তথ্য নেই। "
                        + "স্বাধীনভাবে যাচাই না করা পর্যন্ত কোনো পদক্ষেপ নেবেন না।";
        return new Advice(explanation, List.of(
                "স্বাধীনভাবে যাচাই না করা পর্যন্ত কোনো টাকা পাঠাবেন না।",
                "মূল বার্তা, স্ক্রিনশট ও অর্থপ্রদানের বিবরণ সংরক্ষণ করুন; অফিসিয়াল সেবি পোর্টালে (sebi.gov.in) উপদেষ্টার নিবন্ধন যাচাই করুন।",
                "প্রতারণা হয়ে থাকলে জাতীয় সাইবার ক্রাইম হেল্পলাইন 1930-এ কল করুন বা cybercrime.gov.in-এ রিপোর্ট করুন।"));
    }

    private Advice marathi(List<Claim> claims, List<RiskSignal> signals) {
        int n = signalCount(signals);
        String explanation = n == 0
                ? "ऑफलाइन तपासणीत कोणतेही विशिष्ट धोक्याचे प्रकार आढळले नाहीत. "
                        + "सुरक्षिततेची खात्री करण्यासाठी माहिती अपुरी आहे; स्वतंत्रपणे पडताळणी करा."
                : "या संदेशात " + n + " धोक्याचे संकेत आढळले आहेत"
                        + (critical(signals) ? ", ज्यामध्ये अनोंदणीकृत योजनांमध्ये दिसणारा गंभीर प्रकार समाविष्ट आहे" : "")
                        + ". दाव्यांची पडताळणी करण्यासाठी पुरेशी माहिती नाही. "
                        + "स्वतंत्रपणे पडताळणी केल्याशिवाय कोणतीही कृती करू नका.";
        return new Advice(explanation, List.of(
                "स्वतंत्र पडताळणी होईपर्यंत कोणतेही पैसे पाठवू नका.",
                "मूळ संदेश, स्क्रीनशॉट आणि पेमेंट तपशील जतन करा; अधिकृत सेबी पोर्टलवर (sebi.gov.in) सल्लागार नोंदणी तपासा.",
                "फसवणूक झाली असल्यास राष्ट्रीय सायबर गुन्हे हेल्पलाइन 1930 वर कॉल करा किंवा cybercrime.gov.in वर तक्रार करा."));
    }

    private Advice gujarati(List<Claim> claims, List<RiskSignal> signals) {
        int n = signalCount(signals);
        String explanation = n == 0
                ? "ઑફલાઇન તપાસમાં કોઈ ચોક્કસ ચેતવણી પેટર્ન મળી નથી. "
                        + "સલામતીની ખાતરી માટે માહિતી અપૂરતી છે; સ્વતંત્ર રીતે ચકાસો."
                : "આ સંદેશમાં " + n + " ચેતવણી સંકેતો મળ્યા છે"
                        + (critical(signals) ? ", જેમાં અનરજિસ્ટર્ડ યોજનાઓમાં જોવા મળતી ગંભીર પેટર્ન સામેલ છે" : "")
                        + ". દાવાઓની ચકાસણી માટે પૂરતી માહિતી નથી. "
                        + "સ્વતંત્ર ચકાસણી વિના કોઈ કાર્યવાહી કરશો નહીં.";
        return new Advice(explanation, List.of(
                "સ્વતંત્ર ચકાસણી ન થાય ત્યાં સુધી કોઈ પૈસા ટ્રાન્સફર કરશો નહીં.",
                "મૂળ સંદેશ, સ્ક્રીનશૉટ અને ચુકવણી વિગતો સાચવો; સત્તાવાર સેબી પોર્ટલ (sebi.gov.in) પર સલાહકાર નોંધણી તપાસો.",
                "છેતરપિંડી થઈ હોય તો રાષ્ટ્રીય સાયબર ક્રાઇમ હેલ્પલાઇન 1930 પર કૉલ કરો અથવા cybercrime.gov.in પર ફરિયાદ કરો."));
    }

    private Advice kannada(List<Claim> claims, List<RiskSignal> signals) {
        int n = signalCount(signals);
        String explanation = n == 0
                ? "ಆಫ್‌ಲೈನ್ ಪರಿಶೀಲನೆಯಲ್ಲಿ ಯಾವುದೇ ನಿರ್ದಿಷ್ಟ ಎಚ್ಚರಿಕೆ ಮಾದರಿಗಳು ಕಂಡುಬಂದಿಲ್ಲ. "
                        + "ಸುರಕ್ಷತೆಯನ್ನು ಖಚಿತಪಡಿಸಲು ಮಾಹಿತಿ ಸಾಕಾಗುವುದಿಲ್ಲ; ಸ್ವತಂತ್ರವಾಗಿ ಪರಿಶೀಲಿಸಿ."
                : "ಈ ಸಂದೇಶದಲ್ಲಿ " + n + " ಎಚ್ಚರಿಕೆ ಸಂಕೇತಗಳು ಕಂಡುಬಂದಿವೆ"
                        + (critical(signals) ? ", ಇವುಗಳಲ್ಲಿ ನೋಂದಣಿಯಾಗದ ಯೋಜನೆಗಳಲ್ಲಿ ಕಂಡುಬರುವ ತೀವ್ರ ಮಾದರಿ ಸೇರಿದೆ" : "")
                        + ". ಹಕ್ಕುಗಳನ್ನು ಪರಿಶೀಲಿಸಲು ಸಾಕಷ್ಟು ಮಾಹಿತಿ ಇಲ್ಲ. "
                        + "ಸ್ವತಂತ್ರವಾಗಿ ಪರಿಶೀಲಿಸುವವರೆಗೆ ಯಾವುದೇ ಕ್ರಮ ತೆಗೆದುಕೊಳ್ಳಬೇಡಿ.";
        return new Advice(explanation, List.of(
                "ಸ್ವತಂತ್ರವಾಗಿ ಪರಿಶೀಲಿಸುವವರೆಗೆ ಯಾವುದೇ ಹಣವನ್ನು ವರ್ಗಾಯಿಸಬೇಡಿ.",
                "ಮೂಲ ಸಂದೇಶ, ಸ್ಕ್ರೀನ್‌ಶಾಟ್ ಮತ್ತು ಪಾವತಿ ವಿವರಗಳನ್ನು ಉಳಿಸಿ; ಅಧಿಕೃತ ಸೆಬಿ ಪೋರ್ಟಲ್ (sebi.gov.in)ನಲ್ಲಿ ಸಲಹೆಗಾರರ ನೋಂದಣಿಯನ್ನು ಪರಿಶೀಲಿಸಿ.",
                "ವಂಚನೆ ನಡೆದಿದ್ದರೆ, ರಾಷ್ಟ್ರೀಯ ಸೈಬರ್ ಕ್ರೈಂ ಸಹಾಯವಾಣಿ 1930 ಗೆ ಕರೆ ಮಾಡಿ ಅಥವಾ cybercrime.gov.in ನಲ್ಲಿ ದೂರು ನೀಡಿ."));
    }
}
