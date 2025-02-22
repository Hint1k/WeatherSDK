package com.demo.sdk.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents system-related weather information, including sunrise and sunset times.
 * <p>
 * This class maps to the {@code sys} field in the OpenWeather API response.
 * </p>
 *
 * @param sunrise The timestamp of the sunrise time (in UTC seconds since epoch).
 * @param sunset  The timestamp of the sunset time (in UTC seconds since epoch).
 */
public record Sys(
        @JsonProperty("sunrise") long sunrise,
        @JsonProperty("sunset") long sunset) {
}