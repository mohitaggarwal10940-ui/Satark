# P4 Evidence Model — SATARK Verification Layer (Phase 9+)

## Status

Foundation only. `evidence/` package holds the taxonomy (`EvidenceStatus`)
and the source value type (`TrustedSource`). No verification runs yet —
`AnalysisService` still returns the mock `INSUFFICIENT_EVIDENCE` entry until
Phase 12 (`EvidenceService`). Full source registry: Phase 10. Workflow:
Phase 11.

## Taxonomy (canonical, closed)

| Code | Meaning | Needs cited source? |
|---|---|---|
| `VERIFIED_TRUE` | Trusted source confirms the claim | YES |
| `VERIFIED_FALSE` | Trusted source contradicts the claim | YES |
| `UNVERIFIED` | Checked but not confirmed (e.g. no reg. number to look up) | no |
| `INSUFFICIENT_EVIDENCE` | Not enough info to attempt verification | no |
| `AMBIGUOUS` | Conflicting/unclear information | no |
| `UNAVAILABLE` | Source could not be checked (offline/no access) | no |

Rules: unknown/null/blank collapses to `INSUFFICIENT_EVIDENCE`
(`EvidenceStatus.fromCode`) — never upgrade uncertainty into a fact. Only
`VERIFIED_*` asserts facts (`assertsFact()`), and those require a cited
allowlisted URL (enforced in Phase 11/12).

## Migration note (no migration needed)

- `model/Evidence.status` stays a **String** on purpose: serialized codes are
  identical to the enum names, so existing Mongo documents (`analyses`
  collection) and the Android parser (`Evidence.kt`, stringly typed) keep
  working with zero migration.
- Legacy values in the wild — `INSUFFICIENT_EVIDENCE` (backend mock),
  `UNVERIFIED` (Android mock) — both resolve via `fromCode`.
- `sourceUrl` stays nullable; `TrustedSource` constructor rejects non-https
  origins. Free-form URL emission remains forbidden (evidence rule).

## Registry (Phase 10 — done)

`TrustedSourceRegistry` allowlist, priority order:

1. SEBI — `https://www.sebi.gov.in`
2. RBI — `https://www.rbi.org.in`
3. NSE — `https://www.nseindia.com`
4. BSE — `https://www.bseindia.com`
5. RBI Sachet — `https://sachet.rbi.org.in`
6. National Cyber Crime Portal — `https://cybercrime.gov.in`
7. SEBI SCORES — `https://scores.sebi.gov.in`

URL gate: `isAllowlistedUrl` requires https + exact host match
(`sebi-official.com`, `sebi.gov.in.evil.com`, plain http all fail — see
`TrustedSourceRegistryTest`). These are long-standing official portals;
live reachability checks are out of scope for the offline MVP
(`satark.evidence.offline-mode=true`).

## Workflow (Phase 11 — done)

`DeterministicClaimVerifier`: regulatory ± ID → `UNVERIFIED` (+ SEBI home,
re-gated); everything else → `INSUFFICIENT_EVIDENCE` (null URL).
`VERIFIED_*` never emitted offline.

## Orchestration (Phase 12 — done)

`EvidenceService.verifyAll`: 1:1 claim mapping, status normalization,
URL allowlist re-gate, null/exception hardening. Live by default; the legacy
mock evidence returns only when `satark.ai.enabled` or
`satark.evidence.enabled` is set false.

## Next (Phase 13 — done)

Deterministic risk scorer (`risk/` package) is the sole owner of
`riskScore`/`riskLevel`; evidence feeds it via +15 (VERIFIED_FALSE) /
−10 (VERIFIED_TRUE) / +0 (uncertainty preserved).
