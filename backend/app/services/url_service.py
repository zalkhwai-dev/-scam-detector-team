import os
import requests
from dotenv import load_dotenv

# Load environment variables from .env file
load_dotenv()
API_KEY = os.getenv("GOOGLE_SAFE_BROWSING_API_KEY")


def check_url_safety(target_url: str) -> dict:
  """Check URL safety using Google Safe Browsing or return a mock status if the key is missing."""

  # 1. Check if the API key exists or is still at its default value
  if not API_KEY or API_KEY == "your_api_key_here":
    # These are mock fixtures requested by the project plan so the frontend can work smoothly
    return {
        "url": target_url,
        "url_status": "no_known_threat",  # Default for testing
        "is_mock": True,
        "message": (
            "Mock result returned because the real Google Safe Browsing API key"
            " is missing."
        ),
    }

  # 2. Real Google Safe Browsing API integration structure (when the key is provided)
  endpoint = (
      "https://safebrowsing.googleapis.com/v4/threatMatches:find?key=" + API_KEY
  )

  body = {
      "client": {"clientId": "scam-detector-app", "clientVersion": "1.0"},
      "threatInfo": {
          "threatTypes": [
              "MALWARE",
              "SOCIAL_ENGINEERING",
              "UNWANTED_SOFTWARE",
              "POTENTIALLY_HARMFUL_APPLICATION",
          ],
          "platformTypes": ["ANY_PLATFORM"],
          "threatEntryTypes": ["URL"],
          "threatEntries": [{"url": target_url}],
      },
  }

  try:
    response = requests.post(endpoint, json=body, timeout=5)
    result_data = response.json()

    # If Google's API returned an error, don't treat it as "safe"
    if "error" in result_data:
      return {
          "url": target_url,
          "url_status": "unavailable",
          "is_mock": False,
      }

    # If Google detects a threat in the URL
    if "matches" in result_data and result_data["matches"]:
      return {"url": target_url, "url_status": "threat_match", "is_mock": False}
    else:
      return {
          "url": target_url,
          "url_status": "no_known_threat",
          "is_mock": False,
      }

  except requests.exceptions.RequestException:
    # Handle connection errors or network timeout
    return {"url": target_url, "url_status": "unavailable", "is_mock": False}
