package com.collins.platform.commons.exception;

/** A request that is well-formed but breaks a business rule (HTTP 422). */
public class BusinessRuleException extends PlatformException {

    public BusinessRuleException(String code, String message) {
        super(code, message, 422);
    }
}
