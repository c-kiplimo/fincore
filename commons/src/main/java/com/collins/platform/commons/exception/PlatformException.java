package com.collins.platform.commons.exception;

/**
 * Base class for every expected business or integration failure. {@code code} is a stable,
 * machine-readable string (e.g. "TRANSACTION_NOT_BALANCED"); {@code status} is the HTTP status.
 */
public class PlatformException extends RuntimeException {

    private final String code;
    private final int status;

    public PlatformException(String code, String message, int status) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public PlatformException(String code, String message, int status, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.status = status;
    }

    public String code() {
        return code;
    }

    public int status() {
        return status;
    }
}
