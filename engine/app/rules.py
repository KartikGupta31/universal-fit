"""Explainable sizing rules. These are heuristic defaults, kept in one place so they are easy to review and tune.

They are NOT brand data. Brand size charts always come from the request.
"""

from typing import Dict

from .models import Category, FitType
from .units import CIRCUMFERENCE_KEYS

# How much each body measurement matters for a category (higher = more important).
WEIGHTS: Dict[Category, Dict[str, float]] = {
    Category.T_SHIRT: {"chest": 3.0, "shoulder": 2.0, "waist": 1.0},
    Category.SHIRT: {"chest": 3.0, "shoulder": 2.0, "waist": 1.5, "neck": 1.0, "sleeve": 1.0},
    Category.JACKET: {"chest": 3.0, "shoulder": 2.0, "sleeve": 1.5, "waist": 1.0},
    Category.HOODIE: {"chest": 3.0, "shoulder": 1.5, "sleeve": 1.0, "waist": 1.0},
    Category.TOP: {"chest": 3.0, "shoulder": 1.5, "waist": 1.5},
    Category.JEANS: {"waist": 3.0, "hips": 2.0, "inseam": 1.5, "thigh": 1.0},
    Category.TROUSERS: {"waist": 3.0, "hips": 2.0, "inseam": 1.5, "thigh": 1.0},
    Category.DRESS: {"chest": 2.5, "waist": 2.0, "hips": 2.0, "shoulder": 1.0},
}

# At least one of these must be measurable, otherwise the engine refuses to guess.
PRIMARY: Dict[Category, frozenset] = {
    Category.T_SHIRT: frozenset({"chest"}),
    Category.SHIRT: frozenset({"chest"}),
    Category.JACKET: frozenset({"chest"}),
    Category.HOODIE: frozenset({"chest"}),
    Category.TOP: frozenset({"chest"}),
    Category.JEANS: frozenset({"waist", "hips"}),
    Category.TROUSERS: frozenset({"waist", "hips"}),
    Category.DRESS: frozenset({"chest", "waist", "hips"}),
}

_BOTTOMS = {Category.JEANS, Category.TROUSERS}

# Desired ease in cm: how much bigger than the body the garment should be, per fit preference.
_TOP_EASE = {
    "chest": {FitType.SLIM: 4, FitType.REGULAR: 10, FitType.RELAXED: 16, FitType.OVERSIZED: 24},
    "waist": {FitType.SLIM: 4, FitType.REGULAR: 8, FitType.RELAXED: 14, FitType.OVERSIZED: 20},
    "hips": {FitType.SLIM: 4, FitType.REGULAR: 8, FitType.RELAXED: 14, FitType.OVERSIZED: 20},
    "thigh": {FitType.SLIM: 4, FitType.REGULAR: 8, FitType.RELAXED: 12, FitType.OVERSIZED: 16},
    "neck": {FitType.SLIM: 1.5, FitType.REGULAR: 2, FitType.RELAXED: 3, FitType.OVERSIZED: 3},
    "shoulder": {FitType.SLIM: 0, FitType.REGULAR: 1, FitType.RELAXED: 2, FitType.OVERSIZED: 4},
}
_BOTTOM_EASE = {
    "waist": {FitType.SLIM: 0, FitType.REGULAR: 2, FitType.RELAXED: 4, FitType.OVERSIZED: 6},
    "hips": {FitType.SLIM: 4, FitType.REGULAR: 8, FitType.RELAXED: 12, FitType.OVERSIZED: 18},
    "thigh": {FitType.SLIM: 4, FitType.REGULAR: 8, FitType.RELAXED: 12, FitType.OVERSIZED: 16},
}
_DRESS_EASE = {
    "chest": {FitType.SLIM: 4, FitType.REGULAR: 8, FitType.RELAXED: 12, FitType.OVERSIZED: 18},
    "waist": {FitType.SLIM: 4, FitType.REGULAR: 8, FitType.RELAXED: 12, FitType.OVERSIZED: 18},
    "hips": {FitType.SLIM: 4, FitType.REGULAR: 8, FitType.RELAXED: 12, FitType.OVERSIZED: 18},
    "shoulder": {FitType.SLIM: 0, FitType.REGULAR: 1, FitType.RELAXED: 2, FitType.OVERSIZED: 3},
}

# Lengths (sleeve, inseam) are matched without ease.

# Tolerance in cm: deviations inside this band still count as a "Good match".
TOLERANCE_CM: Dict[str, float] = {
    "chest": 3.0, "waist": 3.0, "hips": 3.0, "thigh": 3.0,
    "neck": 1.5, "shoulder": 1.5, "sleeve": 2.5, "inseam": 2.5,
}

# Body charts: how far outside a size's range (cm) before its score reaches zero.
BODY_OUTSIDE_TOLERANCE_CM = {"circumference": 4.0, "length": 2.0}

# Body charts only shift circumferences when the preferred fit differs from the product's cut.
BODY_SHIFT_KEYS = frozenset({"chest", "waist", "hips", "thigh"})


def ease_cm(category: Category, key: str, fit: FitType) -> float:
    """Desired ease (cm) for a measurement under a fit preference. Lengths return 0."""
    if category in _BOTTOMS:
        table = _BOTTOM_EASE
    elif category == Category.DRESS:
        table = _DRESS_EASE
    else:
        table = _TOP_EASE
    return float(table.get(key, {}).get(fit, 0.0))


def is_circumference(key: str) -> bool:
    return key in CIRCUMFERENCE_KEYS
