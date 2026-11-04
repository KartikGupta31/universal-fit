package com.universalfit.domain.model;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** One size label (e.g. "M" or "32") and its measurements keyed by chest, waist, hips, ... */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class SizeEntry {
    private String label;
    private Map<String, MeasurementRange> measurements = new LinkedHashMap<>();
}
