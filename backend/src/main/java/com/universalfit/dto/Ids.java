package com.universalfit.dto;

import org.bson.types.ObjectId;

/** Helper for turning MongoDB ObjectIds into the plain 24-character hex strings the API exposes. */
final class Ids {
    private Ids() {}

    static String hex(ObjectId id) {
        return id == null ? null : id.toHexString();
    }
}
