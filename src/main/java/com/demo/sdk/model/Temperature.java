package com.demo.sdk.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents temperature data for a given location.
 * <p>
 * This class includes both the actual temperature and the "feels like" temperature,
 * which accounts for factors like humidity and wind to provide a perceived temperature.
 * </p>
 *
 * @param temp      The actual temperature in Kelvin.
 * @param feelsLike The perceived temperature in Kelvin, considering weather factors like humidity and wind.
 */
public record Temperature(
        @JsonProperty("temp") double temp,
        @JsonProperty("feels_like") double feelsLike) {
}