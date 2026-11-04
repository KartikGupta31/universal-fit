"""Test fixtures. All sizes here are synthetic test data, not real brand charts."""

from copy import deepcopy


def r(v, hi=None):
    return {"min": v, "max": hi if hi is not None else v}


# Synthetic T-shirt GARMENT chart (full circumference, cm)
TEE_GARMENT = [
    {"label": "S", "measurements": {"chest": r(98), "shoulder": r(43), "waist": r(94)}},
    {"label": "M", "measurements": {"chest": r(104), "shoulder": r(45), "waist": r(100)}},
    {"label": "L", "measurements": {"chest": r(110), "shoulder": r(47), "waist": r(106)}},
    {"label": "XL", "measurements": {"chest": r(116), "shoulder": r(49), "waist": r(112)}},
]

# Synthetic T-shirt BODY chart (body ranges, cm)
TEE_BODY = [
    {"label": "S", "measurements": {"chest": r(88, 95), "waist": r(74, 81)}},
    {"label": "M", "measurements": {"chest": r(96, 103), "waist": r(82, 89)}},
    {"label": "L", "measurements": {"chest": r(104, 111), "waist": r(90, 97)}},
    {"label": "XL", "measurements": {"chest": r(112, 119), "waist": r(98, 105)}},
]

# Synthetic jeans GARMENT chart (full circumference, cm; inseam length)
JEANS_GARMENT = [
    {"label": "30", "measurements": {"waist": r(80), "hips": r(104), "inseam": r(80)}},
    {"label": "32", "measurements": {"waist": r(85), "hips": r(109), "inseam": r(81)}},
    {"label": "34", "measurements": {"waist": r(90), "hips": r(114), "inseam": r(82)}},
]

FULL_USER = {"unit": "CM", "chest": 96, "waist": 92, "shoulder": 44}


def tee_request(**overrides):
    body = {
        "profile": deepcopy(FULL_USER),
        "category": "T_SHIRT",
        "measurementBasis": "GARMENT",
        "chartVerified": True,
        "sizes": deepcopy(TEE_GARMENT),
    }
    profile_over = overrides.pop("profile", None)
    if profile_over is not None:
        body["profile"] = profile_over
    body.update(overrides)
    return body
