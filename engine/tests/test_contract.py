import json
import logging
from copy import deepcopy
from pathlib import Path

import pytest
from fastapi.testclient import TestClient

from app.main import app

client = TestClient(app)

FIXTURE = (
    Path(__file__).resolve().parents[2]
    / "contracts"
    / "recommend-request.demo-tshirt.json"
)


@pytest.fixture
def demo():
    return deepcopy(json.loads(FIXTURE.read_text(encoding="utf-8")))


def _errors(res):
    assert res.status_code == 422
    return res.json()["detail"]


def test_java_demo_tshirt_payload_is_accepted(demo):
    res = client.post("/v1/recommend", json=demo)
    assert res.status_code == 200, res.text

    body = res.json()
    assert body["recommendedSize"] in {"S", "M", "L", "XL"}
    assert set(body["sizeScores"]) == {"S", "M", "L", "XL"}


def test_explicit_nulls_for_absent_profile_fields_are_accepted(demo):
    assert demo["profile"]["hips"] is None
    assert client.post("/v1/recommend", json=demo).status_code == 200


@pytest.mark.parametrize("field", ["profile", "category", "measurementBasis", "sizes"])
def test_missing_required_field_is_422_and_names_the_field(demo, field):
    del demo[field]

    errs = _errors(client.post("/v1/recommend", json=demo))

    assert any(
        e["loc"][:2] == ["body", field] and e["type"] == "missing"
        for e in errs
    )


def test_invalid_measurement_key_is_422_without_exposing_input(demo):
    demo["sizes"][0]["measurements"]["biceps"] = {
        "min": 30.0,
        "max": 30.0,
    }

    res = client.post("/v1/recommend", json=demo)
    errs = _errors(res)

    assert any(e["type"] == "value_error" for e in errs)
    assert all(e["msg"] == "Invalid request data" for e in errs)
    assert "biceps" not in res.text


@pytest.mark.parametrize(
    "path, bad",
    [
        (("category",), "SOCKS"),
        (("measurementBasis",), "BODYISH"),
        (("unit",), "METERS"),
        (("productFitType",), "TIGHT"),
        (("profile", "preferredFit"), "BAGGY"),
        (("profile", "unit"), "FEET"),
    ],
)
def test_invalid_enum_value_is_422_at_the_right_location(demo, path, bad):
    target = demo

    for part in path[:-1]:
        target = target[part]

    target[path[-1]] = bad

    errs = _errors(client.post("/v1/recommend", json=demo))

    assert any(
        e["loc"] == ["body", *path] and e["type"] == "enum"
        for e in errs
    )


def test_implausible_profile_value_is_rejected_without_exposing_value(demo):
    demo["profile"]["chest"] = 40.0

    res = client.post("/v1/recommend", json=demo)
    errs = _errors(res)

    assert any(
        e["loc"] == ["body", "profile"] and e["type"] == "value_error"
        for e in errs
    )
    assert all(e["msg"] == "Invalid request data" for e in errs)
    assert "40.0" not in res.text


def test_validation_response_has_details_but_does_not_echo_input(demo):
    demo["profile"]["chest"] = 40.0

    res = client.post("/v1/recommend", json=demo)

    assert res.status_code == 422

    for err in res.json()["detail"]:
        assert set(err) == {"loc", "msg", "type"}
        assert err["msg"] == "Invalid request data"

    assert "40.0" not in res.text


def test_validation_failure_is_logged_without_secrets_or_measurements(
    demo, monkeypatch, caplog
):
    monkeypatch.setenv("ENGINE_API_KEY", "super-secret-key")

    demo["sizes"][0]["measurements"]["biceps"] = {
        "min": 30.0,
        "max": 30.0,
    }

    with caplog.at_level(logging.WARNING, logger="universalfit.engine"):
        res = client.post(
            "/v1/recommend",
            json=demo,
            headers={"X-API-Key": "super-secret-key"},
        )

    assert res.status_code == 422

    log = caplog.text

    assert "Request validation failed" in log
    assert "value_error" in log
    assert "super-secret-key" not in log
    assert "30.0" not in log
    assert "biceps" not in log

    assert "super-secret-key" not in res.text
    assert "30.0" not in res.text


def test_api_key_is_checked_before_body_validation(demo, monkeypatch):
    monkeypatch.setenv("ENGINE_API_KEY", "k")

    demo.pop("category")

    res = client.post("/v1/recommend", json=demo)

    assert res.status_code == 401
    assert "category" not in res.text
