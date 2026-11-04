package com.universalfit.domain.enums;

/**
 * How trustworthy a brand/size chart/product sizing record is.
 * Seed data is always DEMO_UNVERIFIED. Only an admin may mark a record VERIFIED, with a source.
 */
public enum VerificationStatus {
    DEMO_UNVERIFIED,
    UNVERIFIED,
    VERIFIED
}
