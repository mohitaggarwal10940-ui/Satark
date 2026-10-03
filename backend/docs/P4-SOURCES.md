# P4 Evidence Sources & Limitations — SATARK Verification Layer

Companion to `P4-EVIDENCE.md` (taxonomy, registry, workflow, orchestration).
This file records, per P4 responsibility 9, what the verifier consults and —
more importantly — what it cannot do yet.

## Consulted sources (MVP: offline)

No live lookups are performed. The verifier applies deterministic rules and,
for regulatory claims, points at the allowlisted SEBI portal homepage so the
user can check. Priority order lives in `TrustedSourceRegistry`:

| # | Source | Origin | Used for today |
|---|---|---|---|
| 1 | SEBI | `https://www.sebi.gov.in` | link target for regulatory claims |
| 2 | RBI | `https://www.rbi.org.in` | allowlisted (future use) |
| 3 | NSE | `https://www.nseindia.com` | allowlisted (future use) |
| 4 | BSE | `https://www.bseindia.com` | allowlisted (future use) |
| 5 | RBI Sachet | `https://sachet.rbi.org.in` | allowlisted (future use) |
| 6 | National Cyber Crime Portal | `https://cybercrime.gov.in` | cited in safety actions |
| 7 | SEBI SCORES | `https://scores.sebi.gov.in` | cited in safety actions |

Only exact-host https URLs from this table may ever appear as `sourceUrl`
(`isAllowlistedUrl`; covered by `TrustedSourceRegistryTest`, re-gated in
`EvidenceService.sanitize`). No other host has ever been emitted — verified
by the no-fabrication battery in `DeterministicClaimVerifierTest` and the
hostile-output test in `EvidenceServiceTest`.

## Limitations (must read before going live)

1. **No registry lookups**: SEBI/RBI registration IDs are pattern-matched,
   never checked. Every regulatory claim stays `UNVERIFIED` offline.
2. **`VERIFIED_*` unreachable**: by design, until an online mode with cited
   fetches exists. Any future online verifier must attach the fetched
   allowlisted URL and keep the raw response for audit.
3. **Non-registration claims unverifiable offline**: guaranteed returns,
   urgency, channels → `INSUFFICIENT_EVIDENCE` with payment-safety guidance.
4. **No multilingual sources**: non-English claims are verified by the same
   content-agnostic rules; vernacular registry portals are future scope.
5. **Stale-portal risk**: origins are long-standing official domains, but
   reachability/shape is not probed by any test (offline MVP). Re-validate
   before enabling any online mode.

## What would unblock online verification

- An allowlisted HTTP client (timeouts, retries, offline fallback to current
  behavior), a per-source fetcher behind `satark.evidence.offline-mode=false`,
  response caching, and audit logging of fetched URLs (never user content).
- Explicit user-approval gate: going online changes the privacy posture.
