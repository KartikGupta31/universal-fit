"""Request and response models for the recommendation engine (camelCase on the wire)."""

from enum import Enum
from typing import Dict, List, Optional

from pydantic import BaseModel, ConfigDict, Field, model_validator
from pydantic.alias_generators import to_camel

from .units import (
    CIRCUMFERENCE_KEYS,
    CLOTHING_KEYS,
    check_plausible,
    to_cm,
    weight_to_kg,
)


class CamelModel(BaseModel):
    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True)


class Unit(str, Enum):
    CM = "CM"
    IN = "IN"


class FitType(str, Enum):
    SLIM = "SLIM"
    REGULAR = "REGULAR"
    RELAXED = "RELAXED"
    OVERSIZED = "OVERSIZED"


class Category(str, Enum):
    T_SHIRT = "T_SHIRT"
    SHIRT = "SHIRT"
    JEANS = "JEANS"
    TROUSERS = "TROUSERS"
    JACKET = "JACKET"
    HOODIE = "HOODIE"
    DRESS = "DRESS"
    TOP = "TOP"


class MeasurementBasis(str, Enum):
    """Whether a size chart lists body measurements or garment measurements."""

    BODY = "BODY"
    GARMENT = "GARMENT"


class MeasurementProfile(CamelModel):
    """A user's body measurements. Every measurement is optional; weight is never required."""

    unit: Unit = Unit.CM
    preferred_fit: FitType = FitType.REGULAR
    height: Optional[float] = Field(default=None, gt=0)
    weight: Optional[float] = Field(default=None, gt=0)
    chest: Optional[float] = Field(default=None, gt=0)
    waist: Optional[float] = Field(default=None, gt=0)
    hips: Optional[float] = Field(default=None, gt=0)
    shoulder: Optional[float] = Field(default=None, gt=0)
    neck: Optional[float] = Field(default=None, gt=0)
    sleeve: Optional[float] = Field(default=None, gt=0)
    inseam: Optional[float] = Field(default=None, gt=0)
    thigh: Optional[float] = Field(default=None, gt=0)
    foot_length: Optional[float] = Field(default=None, gt=0)

    @model_validator(mode="after")
    def _check_plausible(self) -> "MeasurementProfile":
        for key in (*CLOTHING_KEYS, "height", "footLength"):
            attr = "foot_length" if key == "footLength" else key
            value = getattr(self, attr)
            if value is not None:
                check_plausible(key, to_cm(value, self.unit.value), value, self.unit.value)
        if self.weight is not None:
            kg = weight_to_kg(self.weight, self.unit.value)
            if not (20.0 <= kg <= 300.0):
                raise ValueError("weight is outside the plausible range; please check the unit.")
        return self

    def clothing_cm(self) -> Dict[str, float]:
        """Clothing-relevant body measurements in cm. Missing ones are simply absent."""
        out: Dict[str, float] = {}
        for key in CLOTHING_KEYS:
            value = getattr(self, key)
            if value is not None:
                out[key] = to_cm(value, self.unit.value)
        return out


class Range(CamelModel):
    """A single value (garment measurement) or a min-max range (body range)."""

    min: float = Field(gt=0)
    max: Optional[float] = Field(default=None, gt=0)

    @model_validator(mode="after")
    def _order(self) -> "Range":
        if self.max is None:
            self.max = self.min
        if self.max < self.min:
            raise ValueError("range max must be >= min")
        return self


class SizeSpec(CamelModel):
    label: str = Field(min_length=1, max_length=20)
    measurements: Dict[str, Range]


class RecommendRequest(CamelModel):
    profile: MeasurementProfile
    category: Category
    product_fit_type: FitType = FitType.REGULAR
    measurement_basis: MeasurementBasis
    unit: Unit = Unit.CM  # unit used by the size chart values
    garment_flat: bool = False  # garment circumferences are flat-laid halves (doubled by the engine)
    chart_verified: bool = False
    brand_name: Optional[str] = None
    sizes: List[SizeSpec] = Field(min_length=1, max_length=40)

    @model_validator(mode="after")
    def _validate_sizes(self) -> "RecommendRequest":
        labels = [s.label for s in self.sizes]
        if len(set(labels)) != len(labels):
            raise ValueError("size labels must be unique")
        self.sizes_cm()  # raises ValueError on unknown keys / implausible values
        return self

    def sizes_cm(self) -> List[tuple]:
        """Return [(label, {key: (lo_cm, hi_cm)})] with flat-laid garments expanded to full circumference."""
        result = []
        for size in self.sizes:
            converted: Dict[str, tuple] = {}
            for key, rng in size.measurements.items():
                if key not in CLOTHING_KEYS:
                    raise ValueError(f"unknown measurement '{key}' in size {size.label}")
                factor = 2.0 if (
                    self.garment_flat
                    and self.measurement_basis == MeasurementBasis.GARMENT
                    and key in CIRCUMFERENCE_KEYS
                ) else 1.0
                lo = to_cm(rng.min, self.unit.value) * factor
                hi = to_cm(rng.max, self.unit.value) * factor
                check_plausible(key, lo, rng.min, self.unit.value)
                check_plausible(key, hi, rng.max, self.unit.value)
                converted[key] = (lo, hi)
            result.append((size.label, converted))
        return result


class Recommendation(CamelModel):
    recommended_size: Optional[str]
    confidence: float
    uncertain: bool
    fit_type: FitType  # the fit preference that was applied
    product_fit_type: FitType
    reason: str
    alternatives: List[str]
    measurement_analysis: Dict[str, str]
    size_scores: Dict[str, float]
    measurements_used: List[str]
    missing_measurements: List[str]
    warnings: List[str]
    data_quality: Dict[str, object]
