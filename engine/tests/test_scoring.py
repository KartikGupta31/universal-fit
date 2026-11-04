from copy import deepcopy

import pytest

from app.models import RecommendRequest
from app.scoring import MAX_CONFIDENCE, recommend, score_body_range, score_garment

from .helpers import JEANS_GARMENT, TEE_BODY, TEE_GARMENT, FULL_USER, r, tee_request


def run(body):
    return recommend(RecommendRequest.model_validate(body))


# ---------- scoring primitives ----------

def test_garment_inside_tolerance_is_good_match():
    score, label = score_garment(garment_cm=106, body_cm=96, key="chest", ease=10)
    assert score == 1.0 and label == "Good match"


def test_garment_smaller_than_body_is_too_tight_regardless_of_preference():
    score, label = score_garment(garment_cm=94, body_cm=96, key="chest", ease=-5)
    assert label == "Too tight" and score <= 0.2


def test_garment_loose_labels():
    assert score_garment(111, 96, "chest", 10)[1] == "Slightly loose"  # +5 cm vs target, tol 3
    assert score_garment(114, 96, "chest", 10)[1] == "Too loose"  # +8 cm vs target, > 2x tol
    assert score_garment(130, 96, "chest", 10)[1] == "Too loose"


def test_body_range_inside_scores_high_and_prefers_center():
    centered, _ = score_body_range(99.5, 96, 103, "chest")
    edge, _ = score_body_range(96, 96, 103, "chest")
    assert centered > edge >= 0.85


def test_body_range_outside_is_lower_and_labelled():
    score, label = score_body_range(105, 96, 103, "chest")
    assert score < 0.85 and label == "Slightly snug"
    assert score_body_range(120, 96, 103, "chest")[1] == "Too tight"
    assert score_body_range(80, 96, 103, "chest")[1] == "Too loose"


# ---------- garment-chart recommendations ----------

def test_garment_chart_picks_expected_size():
    result = run(tee_request())
    assert result.recommended_size == "M"
    assert result.measurement_analysis["chest"] == "Good match"
    assert result.measurements_used == ["chest", "shoulder", "waist"]
    assert result.missing_measurements == []


def test_relaxed_fit_moves_up_and_slim_moves_down():
    slim = run(tee_request(profile={**FULL_USER, "preferredFit": "SLIM"}))
    relaxed = run(tee_request(profile={**FULL_USER, "preferredFit": "RELAXED"}))
    order = ["S", "M", "L", "XL"]
    assert order.index(slim.recommended_size) < order.index("M") <= order.index(relaxed.recommended_size)
    assert slim.fit_type.value == "SLIM"


def test_flat_laid_garment_matches_full_circumference():
    flat = deepcopy(TEE_GARMENT)
    for size in flat:
        for key in ("chest", "waist"):
            half = size["measurements"][key]["min"] / 2
            size["measurements"][key] = r(half)
    full = run(tee_request())
    halved = run(tee_request(sizes=flat, garmentFlat=True))
    assert halved.recommended_size == full.recommended_size
    assert halved.size_scores == full.size_scores


def test_inches_profile_gives_same_result_as_cm():
    cm = run(tee_request())
    inches = run(tee_request(profile={
        "unit": "IN",
        "chest": 96 / 2.54, "waist": 92 / 2.54, "shoulder": 44 / 2.54,
    }))
    assert inches.recommended_size == cm.recommended_size
    assert inches.size_scores == pytest.approx(cm.size_scores, abs=0.001)


def test_chart_in_inches_is_normalised():
    sizes = deepcopy(TEE_GARMENT)
    for size in sizes:
        for key, rng in size["measurements"].items():
            rng["min"] = rng["max"] = rng["min"] / 2.54
    result = run(tee_request(sizes=sizes, unit="IN"))
    assert result.recommended_size == "M"


def test_tie_prefers_larger_size():
    sizes = [
        {"label": "A", "measurements": {"chest": r(106)}},
        {"label": "B", "measurements": {"chest": r(106)}},
    ]
    result = run(tee_request(profile={"chest": 96}, sizes=sizes))
    assert result.recommended_size == "B"


def test_alternatives_exclude_best_and_are_capped():
    result = run(tee_request(profile={"chest": 100, "waist": 92, "shoulder": 44}))
    assert result.recommended_size not in result.alternatives
    assert len(result.alternatives) <= 2


def test_bottoms_use_waist_hips_inseam():
    body = {
        "profile": {"waist": 84, "hips": 100, "inseam": 81},
        "category": "JEANS",
        "measurementBasis": "GARMENT",
        "chartVerified": True,
        "sizes": JEANS_GARMENT,
    }
    result = run(body)
    assert result.recommended_size == "32"
    assert set(result.measurements_used) == {"waist", "hips", "inseam"}
    assert "thigh" in result.missing_measurements


# ---------- body-chart recommendations ----------

def test_body_chart_picks_size_containing_user():
    result = run(tee_request(
        profile={"chest": 99, "waist": 85},
        measurementBasis="BODY", sizes=deepcopy(TEE_BODY),
    ))
    assert result.recommended_size == "M"
    assert result.measurement_analysis == {"chest": "Good match", "waist": "Good match"}


def test_body_chart_oversized_preference_sizes_up():
    base = dict(profile={"chest": 99, "waist": 85}, measurementBasis="BODY", sizes=deepcopy(TEE_BODY))
    regular = run(tee_request(**base))
    oversized = run(tee_request(
        profile={"chest": 99, "waist": 85, "preferredFit": "OVERSIZED"},
        measurementBasis="BODY", sizes=deepcopy(TEE_BODY),
    ))
    order = ["S", "M", "L", "XL"]
    assert order.index(oversized.recommended_size) > order.index(regular.recommended_size)


def test_body_chart_product_cut_offsets_preference():
    # A user who wants a relaxed fit buying an already-relaxed product needs no size shift.
    normal = run(tee_request(
        profile={"chest": 99, "waist": 85, "preferredFit": "RELAXED"},
        measurementBasis="BODY", sizes=deepcopy(TEE_BODY), productFitType="RELAXED",
    ))
    assert normal.recommended_size == "M"


# ---------- missing data, confidence, uncertainty ----------

def test_missing_measurements_still_recommend_with_lower_confidence():
    full = run(tee_request())
    partial = run(tee_request(profile={"chest": 96}))
    assert partial.recommended_size is not None
    assert set(partial.missing_measurements) == {"shoulder", "waist"}
    assert partial.confidence < full.confidence
    assert any("Missing measurements" in w for w in partial.warnings)


def test_no_primary_measurement_refuses_to_guess():
    result = run(tee_request(profile={"shoulder": 44, "waist": 92}))
    assert result.recommended_size is None
    assert result.confidence == 0.0 and result.uncertain is True
    assert "chest" in result.reason


def test_chart_missing_a_measurement_is_not_used_for_any_size():
    sizes = deepcopy(TEE_GARMENT)
    del sizes[2]["measurements"]["waist"]  # L has no waist
    result = run(tee_request(sizes=sizes))
    assert "waist" not in result.measurements_used
    assert "waist" in result.missing_measurements


def test_unverified_chart_lowers_confidence_and_warns():
    verified = run(tee_request(chartVerified=True))
    unverified = run(tee_request(chartVerified=False))
    assert unverified.confidence < verified.confidence
    assert any("unverified" in w.lower() for w in unverified.warnings)
    assert "unverified" in unverified.reason


def test_confidence_never_exceeds_cap():
    sizes = [{"label": "M", "measurements": {"chest": r(106), "shoulder": r(45), "waist": r(100)}}]
    result = run(tee_request(sizes=sizes))
    assert 0 < result.confidence <= MAX_CONFIDENCE


def test_user_far_outside_chart_is_uncertain_and_warned():
    result = run(tee_request(profile={"chest": 140, "waist": 130, "shoulder": 60}))
    assert result.recommended_size == "XL"
    assert result.uncertain is True
    assert any("outside" in w or "closely" in w for w in result.warnings)
    assert result.measurement_analysis["chest"] == "Too tight"


def test_single_size_chart_has_reduced_confidence():
    sizes = [{"label": "ONE", "measurements": {"chest": r(106)}}]
    multi = run(tee_request())
    single = run(tee_request(profile={"chest": 96}, sizes=sizes))
    assert single.confidence < multi.confidence


def test_size_scores_are_reported_for_every_size():
    result = run(tee_request())
    assert set(result.size_scores) == {"S", "M", "L", "XL"}
    assert all(0.0 <= v <= 1.0 for v in result.size_scores.values())


def test_same_label_is_not_assumed_equivalent_across_brands():
    # Same user, two different charts that both call their sizes "M": results follow each chart.
    big = deepcopy(TEE_GARMENT)
    for size in big:
        size["measurements"]["chest"] = r(size["measurements"]["chest"]["min"] + 12)
    a = run(tee_request())
    b = run(tee_request(sizes=big))
    assert a.recommended_size != b.recommended_size
