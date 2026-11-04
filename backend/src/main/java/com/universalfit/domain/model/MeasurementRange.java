package com.universalfit.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A single garment value (min == max) or a body range (min..max). Units come from the owning chart. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class MeasurementRange {
    private double min;
    private double max;

    public static MeasurementRange of(double value) {
        return new MeasurementRange(value, value);
    }

    public static MeasurementRange of(double min, double max) {
        return new MeasurementRange(min, max);
    }
}
