package com.demo.sdk.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents weather conditions for a given location.
 * <p>
 * This class includes the main weather condition and a brief description of the weather.
 * It is used to capture essential weather information such as the general condition (e.g., Clear, Rain)
 * and its detailed description (e.g., clear sky, light rain).
 * </p>
 *
 * @param main The main weather condition (e.g., "Clear", "Rain").
 * @param description A detailed description of the weather condition (e.g., "clear sky", "light rain").
 */
public record Weather(
        @JsonProperty("main") String main,
        @JsonProperty("description") String description) {
}