# Day 1 Decisions

**Project:** AI-Based Scam Message and Suspicious Link Detection System
**Date:** 14 September 2026
**Recorded by:** Zainab (Integration)

## 1. Message labels
Start with **binary classes: Safe and Scam** (matches scope Section 1.3 and the message use case). Other
report passages mention additional scam types (e.g. phishing, fraud) — flagged for supervisor review, not
implemented this week. Raseel and Hawra own the final class definitions before data selection.

## 2. SQLite location
**Conflict identified:** Section 5.2 of the original report places storage on the phone; Sections 5.1 and 5.3
describe the backend writing to the database.

** Confirmed decision: on-device storage.

The backend computes and returns a result.

The Android app (via Zainab's storage layer) saves that result locally using SQLiteOpenHelper.

FastAPI does not write directly to the phone's database.

The team has confirmed this resolves the conflict between Sections 5.2 and 5.1/5.3 of the original report.

## 3. URL verdicts and risk scores
- Google Safe Browsing returns **threat_match** or **no_known_threat** — not three ready-made verdict
  levels. We do not invent "Safe/Suspicious/Malicious" ourselves without a documented mapping policy.
- `no_known_threat` is **not** the same as a safety guarantee.
- `risk_score` stays **null/unset** until a documented calculation method exists — never fabricated from a
  no-match result.
- A model's `model_score` is a confidence number, not a claim of overall system accuracy.

## 4. API contract
Agreed and documented separately in `docs/api_contract.md`. Endpoints: `GET /health`,
`POST /api/v1/analyze-message`, `POST /api/v1/check-url`.

## 5. Tools standardized this week
| Component | Tools |
|---|---|
| App | Android Studio, Kotlin |
| Backend | Python, FastAPI |
| AI | RoBERTa (FacebookAI/roberta-base), PyTorch, Transformers, Pandas, scikit-learn |
| URL checking | Google Safe Browsing API |
| Local history | SQLite on Android (SQLiteOpenHelper) |
| Collaboration | Git, GitHub (Issues for tasks, Pull Requests for review) |

## 6. Repository structure
```
android/    - Nada's Kotlin project (Zainab adds networking + storage layers here)
backend/    - Rana and Amnah's FastAPI service
ai/         - Raseel and Hawra's model environment and scripts
docs/       - decisions.md, api_contract.md, week1_status.md
tests/fixtures/ - shared mock JSON responses (is_mock = true)
```
