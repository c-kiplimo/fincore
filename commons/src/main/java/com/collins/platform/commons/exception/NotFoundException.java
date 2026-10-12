package com.collins.platform.commons.exception;

public class NotFoundException extends PlatformException {

    public NotFoundException(String what, Object id) {
        super("NOT_FOUND", what + " '" + id + "' was not found", 404);
    }
}
