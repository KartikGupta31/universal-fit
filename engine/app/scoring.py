"""Deterministic size recommendation. Every step is explainable and unit-testable."""

from typing import Dict, List, Tuple

from .models import (
    FitType,
    MeasurementBasis,
    RecommendRequest,
    Recommendation,
)
from .rules import (
    BODY_OUTSIDE_TOLERANCE_CM,
    BODY_SHIFT_KEYS,
    PRIMARY,
    TOLERANCE_CM,
    WEIGHTS,
    ease_cm,
    is_circumference,
)

MAX_CONFIDENCE = 0.95  # we never claim a guaranteed fit
UNCERTAIN_BELOW = 0.5


def score_body_range(user_cm: float, lo: float, hi: float, key: str) -> Tuple[float, str]:
    """Score a user's (fit-adjusted) body value against a chart's body range."""
    if lo <= user_cm <= hi:
        half = (hi - lo) / 2
        off_center = abs(user_cm - (lo + hi) / 2) / half if half > 0 else 0.0
        return 1.0 - 0.15 * off_center, "Good match"
    tol = BODY_OUTSIDE_TOLERANCE_CM["circumference" if is_circumference(key) else "length"]
    dist = (lo - user_cm) if user_cm < lo else (user_cm - hi)
    score = 0.85 * max(0.0, 1.0 - dist / tol)
    if user_cm > hi:
        label = "Slightly snug" if dist <= tol / 2 else "Too tight"
    else:
        label = "Slightly loose" if dist <= tol / 2 else "Too loose"
    return score, label


def score_garment(garment_cm: float, body_cm: float, key: str, ease: float) -> Tuple[float, str]:
    """Score a garment measurement against body + desired ease."""
    tol = TOLERANCE_CM[key]
    deviation = garment_cm - (body_cm + ease)
    over = max(0.0, abs(deviation) - tol)
    score = max(0.0, 1.0 - over / (2 * tol))

    if is_circumference(key) and garment_cm < body_cm:
        # Smaller than the body: not wearable without stretch, whatever the fit preference.
        return min(score, 0.2), "Too tight"
    if abs(deviation) <= tol:
        return score, "Good match"
    if deviation < 0:
        return score, "Slightly snug" if abs(deviation) <= 2 * tol else "Too tight"
    return score, "Slightly loose" if deviation <= 2 * tol else "Too loose"


def _human_list(items: List[str]) -> str:
    if len(items) <= 1:
        return "".join(items)
    return ", ".join(items[:-1]) + " and " + items[-1]


def _insufficient(req: RecommendRequest, weights: Dict[str, float], missing: List[str]) -> Recommendation:
    primary = sorted(PRIMARY[req.category])
    return Recommendation(
        recommended_size=None,
        confidence=0.0,
        uncertain=True,
        fit_type=req.profile.preferred_fit,
        product_fit_type=req.product_fit_type,
        reason=(
            f"Not enough data to recommend a size. Add at least one of: {_human_list(primary)} "
            "(and make sure the size chart lists it)."
        ),
        alternatives=[],
        measurement_analysis={},
        size_scores={},
        measurements_used=[],
        missing_measurements=missing,
        warnings=["Insufficient measurements for this category."],
        data_quality={
            "coverage": 0.0,
            "chartVerified": req.chart_verified,
            "basis": req.measurement_basis.value,
        },
    )


def recommend(req: RecommendRequest) -> Recommendation:
    weights = WEIGHTS[req.category]
    user = req.profile.clothing_cm()
    sizes = req.sizes_cm()
    fit = req.profile.preferred_fit

    # Only compare on measurements the user has AND every size in the chart lists,
    # so that every size is scored on the same footing.
    usable = [
        k for k in weights
        if k in user and all(k in measures for _, measures in sizes)
    ]
    missing = [k for k in weights if k not in usable]

    if not set(usable) & PRIMARY[req.category]:
        return _insufficient(req, weights, missing)

    per_size_scores: Dict[str, float] = {}
    per_size_labels: Dict[str, Dict[str, str]] = {}

    for label, measures in sizes:
        total_w = 0.0
        weighted = 0.0
        labels: Dict[str, str] = {}
        for key in usable:
            lo, hi = measures[key]
            if req.measurement_basis == MeasurementBasis.GARMENT:
                score, text = score_garment(
                    (lo + hi) / 2, user[key], key, ease_cm(req.category, key, fit)
                )
            else:
                shift = 0.0
                if key in BODY_SHIFT_KEYS:
                    shift = ease_cm(req.category, key, fit) - ease_cm(
                        req.category, key, req.product_fit_type
                    )
                score, text = score_body_range(user[key] + shift, lo, hi, key)
            weighted += weights[key] * score
            total_w += weights[key]
            labels[key] = text
        per_size_scores[label] = weighted / total_w
        per_size_labels[label] = labels

    order = [label for label, _ in sizes]
    # Highest score wins; on an exact tie the larger size (later in the chart) wins.
    ranked = sorted(order, key=lambda s: (per_size_scores[s], order.index(s)), reverse=True)
    best = ranked[0]
    best_score = per_size_scores[best]

    alternatives = [
        s for s in ranked[1:]
        if per_size_scores[s] >= 0.5 and best_score - per_size_scores[s] <= 0.15
    ][:2]

    all_weight = sum(weights.values())
    coverage = sum(weights[k] for k in usable) / all_weight
    coverage_factor = 0.5 + 0.5 * coverage
    if len(ranked) > 1:
        gap = best_score - per_size_scores[ranked[1]]
        separation = 0.75 + 0.25 * min(1.0, gap / 0.25)
    else:
        separation = 0.85  # nothing to compare against
    source_factor = 1.0 if req.chart_verified else 0.85

    confidence = min(MAX_CONFIDENCE, best_score * coverage_factor * separation * source_factor)
    if coverage < 0.5:
        confidence = min(confidence, 0.5)
    confidence = round(confidence, 2)

    warnings: List[str] = []
    if not req.chart_verified:
        warnings.append("Size chart is unverified (demo or unconfirmed data).")
    if missing:
        warnings.append(f"Missing measurements reduced confidence: {_human_list(missing)}.")
    if best_score < UNCERTAIN_BELOW:
        warnings.append("No size in this chart closely matches your measurements.")
    elif best_score < 0.8 and best in (order[0], order[-1]):
        warnings.append("You may fall outside this chart's size range.")

    uncertain = confidence < UNCERTAIN_BELOW
    basis_text = "garment" if req.measurement_basis == MeasurementBasis.GARMENT else "body"
    reason = (
        f"Based on your {_human_list(usable)} and your {fit.value.lower()} fit preference, "
        f"compared with {req.brand_name + ' ' if req.brand_name else ''}{basis_text} measurements"
        f"{'' if req.chart_verified else ' (unverified chart)'}."
    )

    return Recommendation(
        recommended_size=best,
        confidence=confidence,
        uncertain=uncertain,
        fit_type=fit,
        product_fit_type=req.product_fit_type,
        reason=reason,
        alternatives=alternatives,
        measurement_analysis=per_size_labels[best],
        size_scores={s: round(v, 3) for s, v in per_size_scores.items()},
        measurements_used=usable,
        missing_measurements=missing,
        warnings=warnings,
        data_quality={
            "coverage": round(coverage, 2),
            "chartVerified": req.chart_verified,
            "basis": req.measurement_basis.value,
        },
    )
