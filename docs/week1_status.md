# Week 1 Status Report

**Consolidated by:** Zainab (Integration)
**Date:** _fill in on Day 7_

Status key: ✅ Complete &nbsp; 🟡 Partial/Temporary (mock) &nbsp; 🔴 Blocked

| Deliverable | Owner | Status | Evidence | Blocker (if any) | Next action |
|---|---|---|---|---|---|
| GitHub repo + decisions.md + api_contract.md | Zainab | ✅ | Repo scam-detector-team live, all 6 teammates added as collaborators, both docs pushed	— | | |
| Android project runs on emulator | Nada | ✅ | AIScamDetector project delivered by Nada, builds and runs successfully. Zainab wired her real networking/storage code into it (RealScanRepository, RealHistoryRepository, replacing Nada's fixtures) | | |
| FastAPI starts, /docs reachable | Rana | ✅ | 	Confirmed by Zainab: main.py/schemas.py pushed to backend/app/, imports cleanly, /health and /docs both reachable| | |
| AI environment loads RoBERTa + tokenizer | Raseel | ✅ | 	Fine-tuned model delivered (config.json, model.safetensors, tokenizer files) via ai/README.md download link on GitHub | | |
| Dataset selected + documented | Hawra | ✅ | | | |
| /api/v1/analyze-message (mock) | Rana |✅ | Endpoint live, tested directly by Zainab (see below) | 	| |
| /api/v1/check-url (mock or real Google call) | Amnah | ✅ | Real Google Safe Browsing integration tested end-to-end with a real API key: known-malware test URL correctly returns `threat_match`, safe URL (google.com) correctly returns `no_known_threat`, `is_mock` correctly flips to `false`. Verified both via curl and inside the real Android app (History screen shows both results saved correctly)| |
| database_schema.sql | Amnah | ✅ | Delivered (5-table normalized design: users, scans, results, ai_model_logs, url_checks). Zainab reconciled her Android storage against it — see below| | |
| Android networking layer | Zainab | ✅ | NetworkClient.kt built (HttpURLConnection, off main thread via coroutines). Tested against Rana's real FastAPI backend, run locally on the same Mac: analyzeMessage() call succeeded, returned verdict=scam, model_score=0.87, is_mock=true| | |
| On-device SQLite save/read | Zainab |	✅ | ScanHistoryDbHelper.kt fully rewritten to match Amnah's real 5-table schema (was previously a single provisional table). Multi-table insert wrapped in one transaction, JOIN query reads it back. Verified two ways: in-app (History survives a full app close/reopen) and directly against the raw database file via adb/sqlite3 (confirmed real rows with correct data)| | |
| End-to-end request reaches backend + returns | Zainab, Nada, Rana | ✅ |Confirmed inside Nada's real AIScamDetector app (not a throwaway test project): submitted "You won a free prize, click here now!" through the real Scan Message screen, got back a correct Scam verdict with 87.0% risk score, matching Rana's real backend response| | |
| Saved result survives app reopen | Zainab | ✅| Confirmed in Nada's real app: History screen correctly shows past scans (message text, Scam/Safe verdict, correct risk %) after closing and reopening| | |
| Invalid input handled without crash | Nada, Rana, Zainab | ✅ |Confirmed inside Nada's real app: submitting an empty message shows a clean "enter message to scan" prompt, not a crash. Backend-side also confirmed via curl: empty text, malformed URL, over-512-char text all return clean 422 errorsI |  |
| Service failure shows "unavailable", never defaults to Safe | Rana, Nada, Zainab | ✅ |Confirmed inside Nada's real app: stopped Rana's backend, submitted a scan, app showed a clear error instead of crashing or defaulting to Safe| | |
| Honest "not yet scored" instead of fake risk score | Nada, Zainab | ✅ |CScanResult.riskScorePercent / HistoryEntry.riskScorePercent changed from non-nullable Double to Double? (Nada), matching RealScanRepository/RealHistoryRepository passing real null instead of a faked 0.0. UI now shows "not yet available"/"not yet scored" for URL scans instead of a misleading 0%. Verified in the real app| | |
| Full working app backed up to GitHub | Zainab | ✅ | Nada's complete AIScamDetector project (UI + Zainab's real integration code) pushed to android/ in the shared repo — previously existed only on Zainab's Mac. Added a .gitignore to exclude build output, local.properties, __pycache__, and large model files. Misplaced integration/ package folder (was outside the com.example.aiscamdetector namespace) moved to the correct location and rebuild confirmed working| | |
| Real model loaded into backend (replacing mock prediction)	 | Rana | ✅  | 	wired in the real smishing-detector-v3l2 model(transformers + torch, loaded from backend/model/final/). Tested via curl and inside the real app: is_mock correctly flips to false, scores are now real model confidence values (not random), and verified consistent across repeated calls with the same input ||
| AI model classes match agreed labels | Raseel, Hawra, Rana, Zainab| ✅ | Model is 3-class (ham/smishing/spam) as delivered. Zainab implemented the agreed mapping in Ai.py: smishing + spam scores are summed into one combined "scam" score, ham stays "safe" — matching the binary Safe/Scam contract in docs/decisions.md, without retraining the model. Verified with both scam and safe test messages | | | | | 
