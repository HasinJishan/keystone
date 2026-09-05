package com.zidio.keystone.exception;

/** Thrown when a work-order status change is not permitted by the lifecycle rules. */
public class IllegalTransitionException extends RuntimeException {
    public IllegalTransitionException(String message) {
        super(message);
    }
}
