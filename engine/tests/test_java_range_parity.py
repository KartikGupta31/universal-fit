"""Guards against the original class of bug: the Java backend accepting profile values the engine rejects.

The backend validates measurements when they are SAVED; the engine validates them when a recommendation is
REQUESTED. If the Java ranges are wider, a user can save a profile that always fails with HTTP 422 later.
This test parses the Java source so the two tables cannot drift apart unnoticed.
"""

import re
from pathlib import Path

from app.units import PLAUSIBLE_CM

JAVA = (
    Path(__file__).resolve().parents[2]
    / "backend/src/main/java/com/universalfit/controller/MeasurementController.java"
)


def _java_bounds():
    src = JAVA.read_text(encoding="utf-8")
    pairs = re.findall(r'"(\w+)",\s*new double\[\]\{\s*([\d.]+),\s*([\d.]+)\s*\}', src)
    return {k: (float(lo), float(hi)) for k, lo, hi in pairs}


def _java_weight_bounds():
    src = JAVA.read_text(encoding="utf-8")
    lo = float(re.search(r"MIN_WEIGHT_KG\s*=\s*([\d.]+)", src).group(1))
    hi = float(re.search(r"MAX_WEIGHT_KG\s*=\s*([\d.]+)", src).group(1))
    return lo, hi


def test_java_measurement_ranges_equal_engine_plausible_ranges():
    assert _java_bounds() == {k: tuple(v) for k, v in PLAUSIBLE_CM.items()}


def test_java_weight_range_equals_engine_weight_range():
    # models.MeasurementProfile rejects weight outside 20-300 kg
    assert _java_weight_bounds() == (20.0, 300.0)
