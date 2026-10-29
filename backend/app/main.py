from fastapi import FastAPI, Request, status
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse

from app.services.url_service import check_url_safety

from app.services.Ai import predict, count_tokens, MAX_MODEL_TOKENS

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
    # Characters are capped at 2000 in the schema; tokens are what the model
    # actually limits. Reject anything over 512 tokens with a clear message
    # instead of silently truncating it and judging only part of the text.
    token_count = count_tokens(payload.text)
    if token_count > MAX_MODEL_TOKENS:
        return JSONResponse(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            content={
                "error": {
                    "code": 422,
                    "message": (
                        f"Message is too long for the AI model ({token_count} tokens, "
                        f"maximum {MAX_MODEL_TOKENS}). Please shorten it and try again."
                    ),
                }
            },
        )
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
    top_message = "Validation Error: Invalid input data provided."
    for err in exc.errors():
        field = err.get("loc", [])[-1]
        msg = err.get("msg")
        error.append({"field": field, "message": msg})
        # Give a clear, specific message when a message is simply too long,
        # since the app shows error.message to the user.
        if err.get("type") == "string_too_long":
            max_chars = err.get("ctx", {}).get("max_length", 10000)
            top_message = (
                f"Message is too long (maximum {max_chars} characters). "
                "Please shorten it and try again."
            )

    return JSONResponse(
        status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
        content={
            "error": {
                "code": 422,
                "message": top_message,
                "details": error
            }
        }
    )


if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host="127.0.0.1", port=8000, reload=True)
