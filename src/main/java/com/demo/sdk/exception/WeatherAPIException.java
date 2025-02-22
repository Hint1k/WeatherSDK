package com.demo.sdk.exception;

/**
 * Custom exception for handling errors related to the OpenWeather API.
 */
public class WeatherAPIException extends Exception {

    /**
     * Constructs a new WeatherAPIException with the specified detail message.
     *
     * @param message The error message describing the cause of the exception.
     */
    public WeatherAPIException(String message) {
        super(message);
    }
}