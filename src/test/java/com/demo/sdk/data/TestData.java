package com.demo.sdk.data;

import com.demo.sdk.model.*;

/**
 * The {@code TestData} class provides helper methods to generate predefined test data for weather-related objects.
 * This class is primarily used to create and return a {@code WeatherData} object with predefined values,
 * which can be used in unit tests for the SDK.
 * <p>
 * The {@code getTestData} method allows for generating a {@code WeatherData} object with mock data for
 * a specific city, which can be used to test the behavior of the SDK without needing to make actual API calls.
 * </p>
 */
public class TestData {

    /**
     * Generates a predefined {@code WeatherData} object with mock weather information for a given city.
     * <p>
     * This method creates a {@code WeatherData} object containing mock weather conditions (e.g., "Clear" weather,
     * temperature values, wind speed, and system information), which is useful for unit testing.
     * </p>
     *
     * @param city The name of the city for which the weather data is generated.
     * @return A {@code WeatherData} object populated with mock data for the specified city.
     */
    public WeatherData getTestData(String city) {
        Weather weather = new Weather("Clear", "clear sky");
        Temperature temperature = new Temperature(28.28, 29.29);
        Wind wind = new Wind(5.5);
        Sys sys = new Sys(1L, 2L);

        return new WeatherData(weather, temperature, 1000, wind, 10L,
                sys, 1, city);
    }
}