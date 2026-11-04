from fastapi.testclient import TestClient

from app.main import app

from .helpers import tee_request

client = TestClient(app)


def test_health():
    assert client.get("/health").json() == {"status": "ok"}


def test_recommend_returns_camel_case_contract():
    res = client.post("/v1/recommend", json=tee_request())
    assert res.status_code == 200
    data = res.json()
    for key in (
        "recommendedSize", "confidence", "fitType", "reason", "alternatives",
        "measurementAnalysis", "sizeScores", "missingMeasurements", "warnings", "dataQuality",
    ):
        assert key in data
    assert data["recommendedSize"] == "M"


def test_implausible_measurement_is_422():
    res = client.post("/v1/recommend", json=tee_request(profile={"chest": 40, "unit": "CM"}))
    assert res.status_code == 422


def test_unknown_chart_measurement_is_422():
    body = tee_request()
    body["sizes"][0]["measurements"]["biceps"] = {"min": 30}
    assert client.post("/v1/recommend", json=body).status_code == 422


def test_duplicate_size_labels_are_422():
    body = tee_request()
    body["sizes"][1]["label"] = "S"
    assert client.post("/v1/recommend", json=body).status_code == 422


def test_empty_sizes_is_422():
    assert client.post("/v1/recommend", json=tee_request(sizes=[])).status_code == 422


def test_api_key_enforced_when_configured(monkeypatch):
    monkeypatch.setenv("ENGINE_API_KEY", "secret")
    assert client.post("/v1/recommend", json=tee_request()).status_code == 401
    bad = client.post("/v1/recommend", json=tee_request(), headers={"X-API-Key": "nope"})
    assert bad.status_code == 401
    ok = client.post("/v1/recommend", json=tee_request(), headers={"X-API-Key": "secret"})
    assert ok.status_code == 200
