package com.gym.exception;

/**
 * Exception thrown when member creation fails
 */
public class MemberCreationException extends RuntimeException {
    
    public MemberCreationException(String message) {
        super(message);
    }
    
    public MemberCreationException(String message, Throwable cause) {
        super(message, cause);
    }
}
