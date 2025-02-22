package com.demo.sdk.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents wind data for a given location.
 * <p>
 * This class contains information about the wind speed at the location.
 * </p>
 *
 * @param speed The wind speed in meters per second.
 */
public record Wind(
        @JsonProperty("speed") double speed) {
}