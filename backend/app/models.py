from datetime import datetime
from sqlalchemy import Column, DateTime, Float, ForeignKey, Integer, String, Text, Boolean
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import relationship, sessionmaker
from sqlalchemy import create_engine

# 1. Setup SQLite database connection & session setup
SQLALCHEMY_DATABASE_URL = "sqlite:///./scam_detector.db"

engine = create_engine(
    SQLALCHEMY_DATABASE_URL, connect_args={"check_same_thread": False}, echo=True
)
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)
Base = declarative_base()


# 2. User Entity 
class User(Base):
    __tablename__ = "users"

    id = Column(Integer, primary_key=True, index=True)
    username = Column(String(50), unique=True, index=True, nullable=False)
    email = Column(String(100), unique=True, index=True, nullable=False)
    password_hash = Column(String(255), nullable=False)  
    created_at = Column(DateTime, default=datetime.utcnow)

    scans = relationship("Scan", back_populates="user")


# 3. Core Scan Entity 
class Scan(Base):
    __tablename__ = "scans"

    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=True)
    input_type = Column(String(50), nullable=False)  
    input_text = Column(Text, nullable=False)        
    verdict = Column(String(50), nullable=False)     
    scanned_at = Column(DateTime, default=datetime.utcnow)

    user = relationship("User", back_populates="scans")
    results = relationship("Result", back_populates="scan", cascade="all, delete-orphan")
    url_checks = relationship("URLCheck", back_populates="scan", cascade="all, delete-orphan")
    ai_logs = relationship("AIModelLog", back_populates="scan", cascade="all, delete-orphan")


# 4. Result Entity 
class Result(Base):
    __tablename__ = "results"

    id = Column(Integer, primary_key=True, index=True)
    scan_id = Column(Integer, ForeignKey("scans.id"), nullable=False)
    details = Column(Text, nullable=True)            
    risk_level = Column(String(20), nullable=True)   

    scan = relationship("Scan", back_populates="results")


# 5. URL Check Entity 
class URLCheck(Base):
    __tablename__ = "url_checks"

    id = Column(Integer, primary_key=True, index=True)
    scan_id = Column(Integer, ForeignKey("scans.id"), nullable=False)
    target_url = Column(String(500), nullable=False)
    url_status = Column(String(50), nullable=False)  
    is_mock = Column(Boolean, default=False)         
    checked_at = Column(DateTime, default=datetime.utcnow)

    scan = relationship("Scan", back_populates="url_checks")


class AIModelLog(Base):
    __tablename__ = "ai_model_logs"

    id = Column(Integer, primary_key=True, index=True)
    scan_id = Column(Integer, ForeignKey("scans.id"), nullable=False)
    model_version = Column(String(50), nullable=False) 
    model_score = Column(Float, nullable=False)      
    logged_at = Column(DateTime, default=datetime.utcnow)

    scan = relationship("Scan", back_populates="ai_logs")


# Function to initialize and create all tables within the SQLite database
def init_db():
    Base.metadata.create_all(bind=engine)
    print("Database tables created successfully using SQLite and SQLAlchemy!")

if __name__ == "__main__":
    init_db()