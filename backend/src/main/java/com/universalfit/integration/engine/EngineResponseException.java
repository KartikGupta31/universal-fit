package com.universalfit.integration.engine;

/**
 * Indicates that the recommendation engine returned a client-side or malformed response.
 */
public class EngineResponseException extends RuntimeException {
    public EngineResponseException(String message) {
        super(message);
    }
}
