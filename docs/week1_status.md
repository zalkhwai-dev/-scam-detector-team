# Week 1 Status Report

**Consolidated by:** Zainab (Integration)
**Date:** _fill in on Day 7_

Status key: ✅ Complete &nbsp; 🟡 Partial/Temporary (mock) &nbsp; 🔴 Blocked

| Deliverable | Owner | Status | Evidence | Blocker (if any) | Next action |
|---|---|---|---|---|---|
| GitHub repo + decisions.md + api_contract.md | Zainab | ✅ | Repo scam-detector-team live, all 6 teammates added as collaborators, both docs pushed	— | | |
| Android project runs on emulator | Nada | 🔴 | | | |
| FastAPI starts, /docs reachable | Rana | ✅ | 	Confirmed by Zainab: main.py/schemas.py pushed to backend/app/, imports cleanly, /health and /docs both reachable| | |
| AI environment loads RoBERTa + tokenizer | Raseel | ✅ | 	Fine-tuned model delivered (config.json, model.safetensors, tokenizer files) via ai/README.md download link on GitHub | | |
| Dataset selected + documented | Hawra | ✅ | | | |
| /api/v1/analyze-message (mock) | Rana |✅ | Endpoint live, tested directly by Zainab (see below) | 	| |
| /api/v1/check-url (mock or real Google call) | Amnah | 🟡 | Endpoint live (mock, is_mock=true), tested directly by Zainab (see below). Real Google Safe Browsing call still pending Amnah| Real Google check not yet implemented | Waiting on Amnah for real URL logic |
| database_schema.sql | Amnah | 🔴 | | | |
| Android networking layer | Zainab | ✅ | NetworkClient.kt built (HttpURLConnection, off main thread via coroutines). Tested against Rana's real FastAPI backend, run locally on the same Mac: analyzeMessage() call succeeded, returned verdict=scam, model_score=0.87, is_mock=true| | |
| On-device SQLite save/read | Zainab |🟡 | | | |
| End-to-end request reaches backend + returns | Zainab, Nada, Rana | 🟡 |Confirmed against Rana's real FastAPI code (main.py/schemas.py, run locally): /health returned correctly, analyze_message() returned verdict=scam, model_score=0.87, is_mock=true, and the result was saved to and read back from SQLite.| | |
| Saved result survives app reopen | Zainab | ✅| Test app fully closed (swiped from recent apps) and reopened in the emulator; "Show Saved History" still listed previously saved scans| | |
| Invalid input handled without crash | Nada, Rana, Zainab | 🟡 | Backend validation confirmed via curl: empty text, malformed URL, and over-512-char text all correctly return 422 with a clear field-level message, not a crash. App-side confirmed too: Zainab's test app caught the resulting ApiError and displayed the message on screen instead of crashing | Only tested in Zainab's own throwaway test app, not Nada's real UI | Repeat once Nada's real UI exists |
| Service failure shows "unavailable", never defaults to Safe | Rana, Nada, Zainab | 🔴 | | | |
| Real model loaded into backend (replacing mock prediction)	 | Rana | 🟡 | 	In progress, Rana confirmed she is working on this now|Zainab to retest analyze_message() once done | confirm is_mock flips to false|
| AI model classes match agreed labels | Raseel, Hawra, Rana | 🟡 | Delivered model is 3-class (ham/smishing/spam), not the binary safe/scam agreed in docs/decisions.md. Rana's backend plans to map non-"ham" → Scam, combining smishing+spam into one score, without retraining| | Zainab to confirm calculation method with Rana once she finishes loading the model; write final decision into docs/decisions.md |

