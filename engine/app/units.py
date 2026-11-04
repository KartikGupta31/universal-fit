"""Unit conversion and plausibility checks. Everything is normalised to centimetres."""

CM_PER_INCH = 2.54
KG_PER_LB = 0.45359237

# Garment-relevant measurements the engine understands (all lengths, in cm).
CLOTHING_KEYS = ("chest", "waist", "hips", "shoulder", "neck", "sleeve", "inseam", "thigh")

# Measurements that are circumferences (they get "ease" and can be flat-laid garment halves).
CIRCUMFERENCE_KEYS = frozenset({"chest", "waist", "hips", "thigh", "neck"})

# Wide adult bounds in cm. They exist to catch unit mix-ups (e.g. 40 entered as cm when it was inches),
# not to judge anybody's body.
PLAUSIBLE_CM = {
    "height": (100.0, 250.0),
    "chest": (50.0, 200.0),
    "waist": (40.0, 200.0),
    "hips": (50.0, 220.0),
    "shoulder": (25.0, 80.0),
    "neck": (20.0, 70.0),
    "sleeve": (40.0, 100.0),
    "inseam": (40.0, 120.0),
    "thigh": (25.0, 100.0),
    "footLength": (15.0, 40.0),
}


def to_cm(value: float, unit: str) -> float:
    """Convert a length to centimetres. `unit` is 'CM' or 'IN'."""
    return value * CM_PER_INCH if unit == "IN" else value


def weight_to_kg(value: float, unit: str) -> float:
    """Weight follows the unit system: kilograms for CM, pounds for IN."""
    return value * KG_PER_LB if unit == "IN" else value


def check_plausible(key: str, cm_value: float, original: float, unit: str) -> None:
    bounds = PLAUSIBLE_CM.get(key)
    if bounds is None:
        return
    lo, hi = bounds
    if not (lo <= cm_value <= hi):
        raise ValueError(
            f"{key}={original} {unit.lower()} is outside the plausible adult range "
            f"({lo:g}-{hi:g} cm); please check the unit."
        )
