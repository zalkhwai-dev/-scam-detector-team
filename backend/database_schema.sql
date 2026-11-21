
CREATE TABLE users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    full_name TEXT NOT NULL,
    email TEXT NOT NULL,
    password_hash TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);


CREATE TABLE scans (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER,
    input_type TEXT NOT NULL,
    input_text TEXT NOT NULL,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);


CREATE TABLE results (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    scan_id INTEGER,
    verdict TEXT,
    risk_score REAL,
    summary TEXT,
    highlighted_phrases TEXT,
    FOREIGN KEY (scan_id) REFERENCES scans(id)
);


CREATE TABLE ai_model_logs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    result_id INTEGER,
    model_label TEXT,
    model_score REAL,
    model_version TEXT,
    is_mock BOOLEAN,
    processing_time REAL,
    FOREIGN KEY (result_id) REFERENCES results(id)
);


CREATE TABLE url_checks (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    result_id INTEGER,
    url TEXT NOT NULL,
    url_status TEXT,
    FOREIGN KEY (result_id) REFERENCES results(id)
);
