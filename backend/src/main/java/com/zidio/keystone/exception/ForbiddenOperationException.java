package com.zidio.keystone.exception;

/** Thrown when an authenticated user tries to act outside what their role/ownership allows. */
public class ForbiddenOperationException extends RuntimeException {
    public ForbiddenOperationException(String message) {
        super(message);
    }
}
