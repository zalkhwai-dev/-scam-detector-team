import os
import re
from transformers import AutoModelForSequenceClassification, AutoTokenizer, pipeline

from app.schemas import AnalysisResult, Highlight

# Path to the real fine-tuned model delivered by the AI team (Raseel/Hawra):
# backend/app/services/Ai.py -> backend/app -> backend -> backend/model/final
_MODEL_DIR = os.path.join(
    os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))),
    "model",
    "final",
)

# Loaded once when the server starts, not on every request.
_tokenizer = AutoTokenizer.from_pretrained(_MODEL_DIR)
_model = AutoModelForSequenceClassification.from_pretrained(_MODEL_DIR)

_classifier = pipeline(
    "text-classification",
    model=_model,
    tokenizer=_tokenizer,
    truncation=True,
    max_length=512,
    top_k=None,  # return scores for all 3 labels, not just the top one
)

# Common scam-indicator phrases, checked case-insensitively against the message text
_SUSPICIOUS_PATTERNS = [
    r"\burgent\b",
    r"\bverify (?:now|your account|immediately)\b",
    r"\bclick here\b",
    r"\bact now\b",
    r"\blimited time\b",
    r"\bcongratulations\b",
    r"\byou'?ve won\b",
    r"\bclaim your\b",
    r"\bsend (?:your )?(?:bank|card|account) details\b",
    r"\bbank details\b",
    r"\bsuspend(?:ed)? your account\b",
    r"\baccount (?:has been )?locked\b",
    r"\bconfirm your (?:identity|password|account)\b",
    r"bit\.ly/\S+",
    r"\bfree\b",
    r"\bprize\b",
    r"\blottery\b",
    r"\bwinner\b",
]


def find_highlights(text: str):
    """Finds suspicious phrases in the text and returns their positions as Highlight objects, merging any overlaps."""
    matches = []
    for pattern in _SUSPICIOUS_PATTERNS:
        for m in re.finditer(pattern, text, flags=re.IGNORECASE):
            matches.append((m.start(), m.end()))
    matches.sort()

    merged = []
    for start, end in matches:
        if merged and start <= merged[-1][1]:
            merged[-1] = (merged[-1][0], max(merged[-1][1], end))
        else:
            merged.append((start, end))

    return [Highlight(start=s, end=e) for s, e in merged]


def predict(text: str) -> AnalysisResult:
    raw_result = _classifier(text)

    # Depending on the transformers version, a single input can come back
    # either as a flat list of label scores, or nested one level deeper.
    if isinstance(raw_result[0], list):
        label_scores = raw_result[0]
    else:
        label_scores = raw_result

    scores_by_label = {item["label"]: item["score"] for item in label_scores}
    ham_score = scores_by_label.get("ham", 0.0)
    smishing_score = scores_by_label.get("smishing", 0.0)
    spam_score = scores_by_label.get("spam", 0.0)

    # The model has 3 classes (ham/smishing/spam), but the team agreed on a
    # binary Safe/Scam result, so smishing + spam are combined into one
    # "scam" score, per docs/decisions.md.
    scam_score = smishing_score + spam_score

    is_scam = scam_score > ham_score
    verdict_val = "scam" if is_scam else "safe"
    score_val = round(scam_score if is_scam else ham_score, 4)

    if is_scam:
        summary = "The model detected language patterns consistent with smishing or spam."
    else:
        summary = "No obvious scam patterns detected by the model."

    return AnalysisResult(
        scan_type="message",
        status="completed",
        verdict=verdict_val,
        model_label=verdict_val,
        model_score=score_val,
        model_version="smishing-detector-v3l2",
        is_mock=False,
        url_status=None,
        risk_score=None,
        summary=summary,
        highlights=find_highlights(text) if is_scam else []
    )
