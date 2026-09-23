<<<<<<< HEAD
from fastapi import FastAPI, Depends, HTTPException
from pydantic import BaseModel
from sqlalchemy.orm import Session

# Import database session, models, and initialization function
from database.models import SessionLocal, Scan, URLCheck, init_db
from services.url_service import check_url_safety as check_url

# Initialize FastAPI app
app = FastAPI(
    title="Scam Detector API",
    description="API for detecting scam URLs and text threats",
    version="1.0"
)

# Automatically create database tables when the application starts
@app.on_event("startup")
def startup_event():
    init_db()

# Dependency to get database session
def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()

# Pydantic model for request body validation
class URLRequest(BaseModel):
    url: str
    user_id: int = None

@app.post("/api/scan/url")
def scan_and_save_url(request: URLRequest, db: Session = Depends(get_db)):
    """
    Endpoint to receive a URL, scan it via Google Safe Browsing service,
    and persist the results in the SQLite database.
    """
    try:
        # 1. Call the URL safety check service
        service_result = check_url(request.url)
        
        url_status = service_result.get("url_status", "unknown")
        
        # 2. Determine verdict based on status
        verdict = "Safe" if url_status == "no_known_threat" else "Suspicious/Threat"
        
        # 3. Create and save scan record
        new_scan = Scan(
            user_id=request.user_id,
            input_type="url",
            input_text=request.url,
            verdict=verdict
        )
        db.add(new_scan)
        db.commit()
        db.refresh(new_scan)
        
        # 4. Create and save URL check details record
        new_url_check = URLCheck(
            scan_id=new_scan.id,
            target_url=request.url,
            url_status=url_status,
            is_mock=service_result.get("is_mock", True)
        )
        db.add(new_url_check)
        db.commit()
        
        return {
            "success": True,
            "scan_id": new_scan.id,
            "url": request.url,
            "verdict": verdict,
            "url_status": url_status,
            "is_mock": service_result.get("is_mock", True)
        }
        
    except Exception as e:
        db.rollback()
        raise HTTPException(status_code=500, detail=str(e))

if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host="127.0.0.1", port=8000, reload=True) 
=======
from fastapi import FastAPI,Request,status
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from app.schemas import (
    Response,
    MessageRequest,
    URLRequest,
    AnalysisResult,
)

#Project Information
app = FastAPI(  
    title="AI-Based Scam Message and Suspicious Link Detection System",
    version ="1.0.0",
    docs_url="/docs",
    redoc_url="/redoc"
)

@app.get("/health",response_model= Response ,tags=["Health"])
def health_check():     #to verify backend service availability.
    return Response(
    status="running",
    message="Backend service is running"
  )

@app.post("/api/v1/analyze-message" , response_model=AnalysisResult, tags=["Analysis"])
def analyze_message(payload:MessageRequest):
    return AnalysisResult(
        scan_type= "message",
         status="completed",
         verdict="scam",
         model_label="scam",
         model_score=0.87,
         model_version= None,
         is_mock=True,
         url_status=None,
         risk_score=None,
         summary=None,
         highlights=[]
   )





@app.post("/api/v1/check-url",response_model=AnalysisResult, tags=["Analysis"])
def check_url(payload:URLRequest):
  return AnalysisResult(
     scan_type= "url",
              status="completed",
              verdict=None,
              model_label=None,
              model_score=None,
              model_version= None,
              is_mock=True,
              url_status="no_known_threat",
              risk_score=None,
              summary=None,
              highlights=[]
  )

@app.exception_handler(RequestValidationError)
async def validation_exception_handler(request :Request ,exc:RequestValidationError):
     error =[]
     for err in exc.errors():
        field = err.get("loc",[])[-1] 
        msg= err.get("msg")
        error.append({"field":field , "message":msg})


     return JSONResponse(
          status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
          content={
           "error":{
             "code":422,
             "message": "Validation Error: Invalid input data provided.",
             "details": error
           }  
          }
       )


>>>>>>> e597fdf5fa716b8869188570cca01b765da23b50
