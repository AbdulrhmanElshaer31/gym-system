package com.gym.exception;

/**
 * Exception thrown when subscription operations fail
 */
public class SubscriptionException extends RuntimeException {
    
    public SubscriptionException(String message) {
        super(message);
    }
    
    public SubscriptionException(String message, Throwable cause) {
        super(message, cause);
    }
}
