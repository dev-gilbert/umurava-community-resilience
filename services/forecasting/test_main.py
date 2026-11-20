from datetime import datetime, timedelta, timezone
from fastapi.testclient import TestClient
from main import app

client = TestClient(app)

def payload():
    start = datetime(2026, 1, 1, tzinfo=timezone.utc)
    return {
        "community_id": "11111111-1111-4111-8111-111111111111",
        "resource_category": "WATER",
        "unit": "litres",
        "horizon_days": 3,
        "observations": [
            {"observed_at": (start + timedelta(days=i)).isoformat(), "value": 100 - i * 2}
            for i in range(6)
        ],
    }

def test_health():
    assert client.get("/health").json()["status"] == "UP"

def test_forecast_returns_bounded_horizon_and_disclaimer():
    response = client.post("/v1/forecast", json=payload())
    assert response.status_code == 200
    data = response.json()
    assert data["model"] == "LINEAR_TREND_BASELINE"
    assert len(data["forecasts"]) == 3
    assert data["forecasts"][0]["predicted_value"] >= 0
    assert "Experimental baseline" in data["warning"]

def test_rejects_unordered_observations():
    data = payload()
    data["observations"][2], data["observations"][3] = data["observations"][3], data["observations"][2]
    assert client.post("/v1/forecast", json=data).status_code == 422

def test_rejects_short_series():
    data = payload()
    data["observations"] = data["observations"][:4]
    assert client.post("/v1/forecast", json=data).status_code == 422
