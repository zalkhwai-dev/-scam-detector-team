from pydantic import BaseModel, Field, HttpUrl
from typing import List, Optional


class Response(BaseModel):
    status: str
    message: str


class MessageRequest(BaseModel):
    text: str = Field(
        ...,
        min_length=1,
        max_length=10000,
        description="Message text cannot be empty and has no practical character limit beyond a 10000-character safety cap. The real limit is the AI model's 512 tokens, checked in main.py."
    )


class URLRequest(BaseModel):
    url: HttpUrl = Field(
        ...,
        description="Must be a valid and well-formed URL."
    )


class Highlight(BaseModel):
    start: int
    end: int


class AnalysisResult(BaseModel):
    scan_type: str
    status: str
    verdict: Optional[str] = None
    model_label: Optional[str] = None
    model_score: Optional[float] = None
    model_version: Optional[str] = None
    is_mock: bool = True
    url_status: Optional[str] = None
    risk_score: Optional[int] = None
    summary: Optional[str] = None
    highlights: List[Highlight] = []
