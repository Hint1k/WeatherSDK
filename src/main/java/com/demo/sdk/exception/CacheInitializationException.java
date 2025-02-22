package com.demo.sdk.exception;

/**
 * Custom exception for handling errors related to the initialization of the weather cache.
 * <p>
 * This exception is thrown when the cache cannot be properly initialized due to issues such as
 * missing or invalid properties or other configuration errors.
 * </p>
 */
public class CacheInitializationException extends RuntimeException {

    /**
     * Constructs a new CacheInitializationException with the specified detail message and cause.
     *
     * @param message The error message describing the cause of the exception.
     * @param cause   The underlying cause of the exception (e.g., an IOException).
     */
    public CacheInitializationException(String message, Throwable cause) {
        super(message, cause);
    }
}