import pytest
from pydantic import ValidationError

from app.models import MeasurementProfile
from app.units import CM_PER_INCH, to_cm, weight_to_kg


def test_inches_to_cm():
    assert to_cm(10, "IN") == pytest.approx(25.4)
    assert to_cm(10, "CM") == 10


def test_weight_lb_to_kg():
    assert weight_to_kg(220, "IN") == pytest.approx(99.79, abs=0.01)
    assert weight_to_kg(70, "CM") == 70


def test_profile_converts_inches():
    p = MeasurementProfile(unit="IN", chest=40, waist=32)
    cm = p.clothing_cm()
    assert cm["chest"] == pytest.approx(40 * CM_PER_INCH)
    assert cm["waist"] == pytest.approx(32 * CM_PER_INCH)


def test_profile_rejects_inches_entered_as_cm():
    with pytest.raises(ValidationError, match="check the unit"):
        MeasurementProfile(unit="CM", chest=40)


def test_profile_rejects_cm_entered_as_inches():
    with pytest.raises(ValidationError, match="check the unit"):
        MeasurementProfile(unit="IN", chest=100)


def test_weight_is_optional_and_never_needed():
    p = MeasurementProfile(chest=100)
    assert p.weight is None
    assert "weight" not in p.clothing_cm()


def test_rejects_non_positive_values():
    with pytest.raises(ValidationError):
        MeasurementProfile(chest=-5)
