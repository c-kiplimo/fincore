package com.collins.platform.commons.exception;

/** A downstream service failed or rejected a call. Keeps the downstream error code and status. */
public class UpstreamException extends PlatformException {

    public UpstreamException(String code, String message, int status) {
        super(code, message, status);
    }

    public UpstreamException(String code, String message, int status, Throwable cause) {
        super(code, message, status, cause);
    }
}
