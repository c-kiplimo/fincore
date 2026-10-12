package com.collins.platform.commons.exception;

public class ConflictException extends PlatformException {

    public ConflictException(String code, String message) {
        super(code, message, 409);
    }
}
