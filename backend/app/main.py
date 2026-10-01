from fastapi import FastAPI, Request, status
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse

from services.url_service import check_url_safety

from app.services.Ai import predict

from app.schemas import (
    Response,
    MessageRequest,
    URLRequest,
    AnalysisResult,
)

# Project Information
app = FastAPI(
    title="AI-Based Scam Message and Suspicious Link Detection System",
    version="1.0.0",
    docs_url="/docs",
    redoc_url="/redoc"
)


@app.get("/health", response_model=Response, tags=["Health"])
def health_check():  # to verify backend service availability.
    return Response(
        status="running",
        message="Backend service is running"
    )


@app.post("/api/v1/analyze-message", response_model=AnalysisResult, tags=["Analysis"])
def analyze_message(payload: MessageRequest):
    return predict(payload.text)


@app.post("/api/v1/check-url", response_model=AnalysisResult, tags=["Analysis"])
def check_url(payload: URLRequest):
    result = check_url_safety(str(payload.url))

    return AnalysisResult(
        scan_type="url",
        status="completed",
        verdict=None,
        model_label=None,
        model_score=None,
        model_version=None,
        is_mock=result.get("is_mock", True),
        url_status=result.get("url_status"),
        risk_score=None,
        summary=result.get("message"),
        highlights=[]
    )


@app.exception_handler(RequestValidationError)
async def validation_exception_handler(request: Request, exc: RequestValidationError):
    error = []
    for err in exc.errors():
        field = err.get("loc", [])[-1]
        msg = err.get("msg")
        error.append({"field": field, "message": msg})

    return JSONResponse(
        status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
        content={
            "error": {
                "code": 422,
                "message": "Validation Error: Invalid input data provided.",
                "details": error
            }
        }
    )


if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host="127.0.0.1", port=8000, reload=True)
