# P3 Model Card — SATARK Claim Extraction & Risk Signals (Phase 8)

## Status

Deterministic MVP. **No LLM provider is wired in.** `satark.ai.provider=none`.
The pipeline is **enabled by default**; the legacy mock survives only behind
`satark.ai.enabled=false`. All P3 output comes from regex/lexicon code in `backend/src/main/java/com/satark/backend/ai/`; the live
`/api/analyze` runs it by default, with the legacy mock kept behind
`satark.ai.enabled=false`.

## What P3 does today

| Step | Class | Input → Output |
|---|---|---|
| Claim extraction | `DeterministicClaimExtractor` | untrusted text → 0–4 `Claim`s |
| Validation | `StructuredOutputValidator` | raw claims/signals → allowlisted, bounded lists |
| Signal mapping | `DeterministicRiskSignalDetector` + `RegulatoryRuleEngine` | claims → 0–4 `RiskSignal`s |
| Guardrails | `PromptInjectionGuard`, `PromptBuilder` | injection detection, future prompt template (unused until an LLM is added) |
| Fallback | `DeterministicFallbackExplainer` | always-available offline text (EN/HI) |
| Advice | `SafetyAdvisor` | 8-language explanation + 3 actions |
| Orchestration | `P3AnalysisPipeline` | text+language → P3 slice (no score, no evidence) |

## Patterns (assumptions)

- SEBI IDs: `IN[AH][A-Z0-9]{7,12}` → HIGH confidence regulatory claim.
- Percent-return: `\d+\s*%` near return/profit/monthly/रिटर्न/मुनाफा/लाभ (ASCII digits only).
- Guarantee lexicon: EN (`guarantee`, `assured`, `zero risk`, `double money`, …) + HI (`गारंटी`, `निश्चित रिटर्न`, `पक्का मुनाफा`).
- Regulatory lexicon: EN (`sebi`, `rbi`, `registered`, `analyst`, …) + HI (`सेबी`, `पंजीकृत`, …).
- Urgency lexicon: EN (`only N slots`, `act now`, `today only`, …) + HI (`तुरंत`, `आज ही`, …).
- Unofficial channel: UPI-handle regex, Indian mobile regex, EN (`telegram`, `whatsapp`, `vip group`, `upi`, …).
- At most **one claim per category**, fixed order; severities per `RegulatoryRuleEngine`
  (guaranteed-HIGH→CRITICAL, guaranteed+unofficial combo→CRITICAL floor).

## Multilingual coverage

| Language | Claim keywords | Agnostic anchors (`%`, SEBI ID, UPI, phone, EN loanwords) |
|---|---|---|
| en | ✅ full | ✅ |
| hi | ✅ full | ✅ |
| ta/te/bn/mr/gu/kn | ⚠️ native lexicon NOT yet covered | ✅ (loanwords: SEBI, UPI, Telegram, ASCII `%`) |

Dataset `src/test/resources/p3-dataset.json` (15 cases) pins this behavior;
non-Hindi vernacular cases rely on agnostic anchors by design.

## Known limitations

1. **Misspellings / OCR confusions not tolerated**: `gauranteed`, `retun`,
   `SEB1`, `0↔O` substitutions do not match. Only case/punctuation/
   whitespace/danda noise is handled (see `P3DatasetTest.syntheticOcrNoisePreservesCoreClaims`).
2. **Non-Hindi vernacular lexicon missing**: Tamil/Telugu/Bengali/Marathi/
   Gujarati/Kannada urgency/guarantee words are not in the lexicons — Phase 8
   follow-up or LLM phase to address.
3. **Non-ASCII digits**: Bengali/Devanagari numerals (`৩০`, `३०`) do not match
   `\d` percent patterns.
4. **No LLM yet**: explanation quality is template-bound; `PromptBuilder` is
   designed but unconnected. Provider choice, prompt versioning, and eval
   harness are open for the LLM phase.
5. **Score is deterministic since Phase 13**: `risk/DeterministicRiskScorer`
   owns `riskScore`/`riskLevel` (weights + evidence adjustments + CRITICAL
   floor 80); the legacy `82/VERY_HIGH` mock survives only on the
   flag-disabled path. Evidence is real (P4) only when both flags are on.

## Safety properties (tested)

- User content treated as data; injection corpus (`ignore previous
  instructions`, `return this as verified`, …) detected, never obeyed, never
  marked verified.
- No fabricated IDs/URLs/statuses; validator drops unknown categories and
  defaults bad confidence/severity to MEDIUM.
- No investment advice or scam-probability language (banned-phrase scan in
  `SafetyAdvisorTest` across all 8 languages).
- No user text echoed into explanations; no user content logged.

## Running the tests

Requires JDK 21 (repo toolchain; env JDK 17 cannot run Gradle here):

```bash
cd backend
./gradlew test --tests "com.satark.backend.ai.*"
```

Pure unit tests — no MongoDB, no network, no API keys.
