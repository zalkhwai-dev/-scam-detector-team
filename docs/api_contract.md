# API Contract (Week 1 Proposal)

Agreed Day 1. Rana implements this in FastAPI; Raseel, Amnah, and Zainab reviewed it; Nada uses it for
display. Fixes field names, types, and meanings so no team has to guess.

## Endpoints

| Endpoint | Request | Purpose |
|---|---|---|
| `GET /health` | none | Confirms backend is running - not that the model is loaded/ready. |
| `POST /api/v1/analyze-message` | JSON: `{ "text": "..." }` | Manually submitted English text. |
| `POST /api/v1/check-url` | JSON: `{ "url": "..." }` | A separate URL check through the backend. |

## Response fields

| Field | Type / meaning |
|---|---|
| `scan_type` | `"message"` or `"url"` |
| `status` | `"completed"` or `"needs_review"`. Service failures use error responses instead. |
| `verdict` | String or `null`. Messages: `"safe"` or `"scam"`. URLs: `null` until a verdict policy is defined. |
| `model_label` | Model's predicted class, or `null` for a URL check. |
| `model_score` | Number 0-1, the model's confidence - **not** system accuracy. `null` for a URL check. |
| `model_version` | Model version string, or `null` if not yet trained. |
| `is_mock` | Boolean - `true` while using a simulated response, `false` once a real trained model is used. |
| `url_status` | `"threat_match"` or `"no_known_threat"`; `null` for message analysis. Service failure is never reported as a no-match. |
| `risk_score` | 0-100 score. Stays `null` until a documented calculation method exists. |
| `summary` | Explanatory text, or `null`. |
| `highlights` | Array of `{start, end}` offsets into the original text (zero-based UTF-16 units, exclusive end - matches Kotlin string indexing). `[]` when unavailable. |

## Mock response (for integration testing only)

```json
{
  "scan_type": "message",
  "status": "completed",
  "verdict": "scam",
  "model_label": "scam",
  "model_score": 0.87,
  "model_version": null,
  "is_mock": true,
  "url_status": null,
  "risk_score": null,
  "highlights": [],
  "summary": null
}
```

Once a trained classifier exists: use `is_mock = false` and its real `model_version`. If there isn't enough
information to issue a verdict, return `status = "needs_review"` with `verdict = null`. The `0.87` above is
an invented number for interface testing only - never a real training result.

## Errors

| Code | Meaning |
|---|---|
| `422` | Invalid input (e.g. empty text, malformed URL) |
| `503` | Service unavailable (e.g. model or Google API down) |
| `504` | Timeout |

Error responses use `error.code` and `error.message` consistently so the app can show a clear failure
message.

## Rules everyone agreed to

- Never remap classes or calculate risk inside the Android UI - the backend/model owns that logic.
- Never default to "Safe" after an error.
- A URL found inside a message should be offered for a separate scan (does not auto-open).
- HTTPS is required per Section 4.3.3. A temporary local HTTP connection during development is a known
  limitation, not evidence the HTTPS requirement is met.
