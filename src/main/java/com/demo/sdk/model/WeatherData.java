package com.demo.sdk.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents weather data for a specific location.
 * <p>
 * This class contains various details including weather conditions, temperature, visibility, wind speed,
 * datetime (timestamp of data retrieval), system information (e.g., sunrise and sunset times), timezone,
 * and the location name.
 * </p>
 *
 * @param weather   The weather information (e.g., main weather condition and description).
 * @param temperature The temperature data including current and "feels like" temperature.
 * @param visibility The visibility distance (in meters).
 * @param wind The wind data (e.g., wind speed).
 * @param datetime The timestamp of the weather data (in UTC seconds since epoch).
 * @param sys The system-related weather information, including sunrise and sunset times.
 * @param timezone The timezone offset in seconds from UTC.
 * @param name The name of the location (e.g., city name).
 */
public record WeatherData(
        @JsonProperty("weather") Weather weather,
        @JsonProperty("temperature") Temperature temperature,
        @JsonProperty("visibility") int visibility,
        @JsonProperty("wind") Wind wind,
        @JsonProperty("datetime") long datetime,
        @JsonProperty("sys") Sys sys,
        @JsonProperty("timezone") int timezone,
        @JsonProperty("name") String name) {
}