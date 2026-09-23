from database.models import SessionLocal, Scan, URLCheck 
from services.url_service import check_url_safety as check_url

def process_and_save_url(target_url: str, user_id: int = None):
    # 1. Open a new database session
    db = SessionLocal()
    
    try:
        # 2. Call the URL check service to get status and details
        service_result = check_url(target_url)
        
        url_status = service_result.get("url_status", "unknown")
        is_mock = service_result.get("is_mock", True)
        
        # Determine final verdict based on URL status
        verdict = "Safe" if url_status == "no_known_threat" else "Suspicious/Threat"

        # 3. Create a main scan history record in the 'scans' table
        new_scan = Scan(
            user_id=user_id,
            input_type="url",
            input_text=target_url,
            verdict=verdict
        )
        db.add(new_scan)
        db.commit()      # Commit to generate the scan.id
        db.refresh(new_scan)

        # 4. Create a related record in the 'url_checks' table
        new_url_check = URLCheck(
            scan_id=new_scan.id,
            target_url=target_url,
            url_status=url_status,
            is_mock=is_mock
        )
        db.add(new_url_check)
        db.commit()      # Commit the URL check details

        print(f"Successfully scanned and saved URL: {target_url} with verdict: {verdict}")
        return new_scan.id

    except Exception as e:
        db.rollback()    # Rollback changes if any error occurs
        print(f"An error occurred: {e}")
        raise e
    finally:
        db.close()       # Always close the session safely

if __name__ == "__main__":
    # Test running a scan and saving it directly to SQLite
    test_url = "https://example.com"
    process_and_save_url(test_url) 