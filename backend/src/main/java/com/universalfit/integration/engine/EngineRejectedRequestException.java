package com.universalfit.integration.engine;

/**
 * The engine understood the call but rejected the request payload (HTTP 400 or 422).
 * Retrying the same request will not help; the measurements or sizing data must change.
 */
public class EngineRejectedRequestException extends EngineResponseException {
    public EngineRejectedRequestException(String message) {
        super(message);
    }
}
