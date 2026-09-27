import random
from app.schemas import AnalysisResult

def predict(text: str) -> AnalysisResult:
    is_scam = "http" in text.lower() or "bit.ly" in text.lower() or len(text) > 100
    verdict_val = "scam" if is_scam else "safe"
    score_val = round(random.uniform(0.75, 0.98), 2) if is_scam else round(random.uniform(0.01, 0.20), 2)

    return AnalysisResult(
        scan_type="message",
        status="completed",
        verdict=verdict_val,
        model_label=verdict_val,
        model_score=score_val,
        model_version=None,
        is_mock=True,
        url_status=None,
        risk_score=None,
        summary="Suspicious pattern or link detected in message." if is_scam else "No obvious scam patterns detected.",
        highlights=[]
    )