package com.demo.sdk;

import com.demo.sdk.client.WeatherClient;
import com.demo.sdk.exception.WeatherAPIException;
import com.demo.sdk.model.WeatherData;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ExtendWith(MockitoExtension.class)
class WeatherClientTest {

    private WeatherClient weatherClient;
    private final String apiKey = "weatherApiKey";
    private static final Logger log = LoggerFactory.getLogger(WeatherClientTest.class);

    @RegisterExtension
    static WireMockExtension wireMockExtension = WireMockExtension
            .newInstance()
            .options(wireMockConfig().dynamicPort()
                    .usingFilesUnderClasspath("wiremock"))
            .build();

    @BeforeEach
    void setup() {
        wireMockExtension.resetAll();
        String baseUrl = wireMockExtension.baseUrl();
        weatherClient = new WeatherClient(baseUrl, apiKey);
    }

    @Test
    void testFetchWeather_Success() {
        try {
            // Given: Correct URL format for a valid city
            String urlString = "/weather?q=London&appid=" + apiKey;

            // WireMock Stubbing: Simulate a successful response with weather data
            wireMockExtension.stubFor(get(urlEqualTo(urlString))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBodyFile("weather.json")));

            // When: Calling fetchWeather with a valid city (London)
            WeatherData weatherData = weatherClient.fetchWeather("London");

            // Then: Verify that weather data is returned correctly
            assertNotNull(weatherData, "Weather data should not be null");
            assertEquals("overcast clouds", weatherData.weather().description(),
                    "Weather description should match the expected value");
            assertEquals(0.30, weatherData.temperature().temp(),
                    "Temperature should match the expected value");

            // Verify WireMock received the request
            wireMockExtension.verify(exactly(1), getRequestedFor(urlEqualTo(urlString)));
        } catch (WeatherAPIException e) {
            log.error("testFetchWeather_Success() failed: {}", e.getMessage());
            fail("Test failed due to exception: {}" + e.getMessage());
        }
    }

    @Test
    void testFetchWeather_InvalidCity() {
        String invalidCity = "InvalidCity";
        String urlString = "/weather?q=" + invalidCity + "&appid=" + apiKey;

        // Given: WireMock stub for a 404 response
        wireMockExtension.stubFor(get(urlEqualTo(urlString))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"cod\":\"404\",\"message\":\"city not found\"}")));

        // When: Calling fetchWeather with an invalid city
        WeatherAPIException exception = assertThrows(WeatherAPIException.class,
                () -> weatherClient.fetchWeather(invalidCity));

        // Then: Verify exception message
        assertNotNull(exception, "Exception should not be null");
        assertTrue(exception.getMessage().contains("city not found"),
                "The exception message should indicate 'city not found'.");

        // Verify WireMock received the request
        wireMockExtension.verify(exactly(1), getRequestedFor(urlEqualTo(urlString)));
    }

    @Test
    void testFetchWeather_InvalidApiKey() {
        String invalidApiKey = "invalidApiKey";
        String city = "London";
        String urlString = "/weather?q=" + city + "&appid=" + invalidApiKey;

        // Given: WireMock stub for an invalid API key response
        wireMockExtension.stubFor(get(urlEqualTo(urlString))
                .willReturn(aResponse()
                        .withStatus(401)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"cod\":401, \"message\":\"Invalid API key\"}")));

        // Create a WeatherClient with WireMock base URL and an invalid API key
        WeatherClient weatherClient = new WeatherClient(wireMockExtension.baseUrl(), invalidApiKey);

        // When: Attempt to fetch weather data with an invalid API key
        WeatherAPIException thrown = assertThrows(WeatherAPIException.class, () -> weatherClient.fetchWeather(city));

        // Then: Verify that the exception message indicates an invalid API key
        assertNotNull(thrown, "Exception should not be null");
        assertTrue(thrown.getMessage().contains("Invalid API key") ||
                        thrown.getMessage().contains("Failed to fetch weather data"),
                "The exception message should indicate an authentication failure.");

        // Verify WireMock received the request
        wireMockExtension.verify(exactly(1), getRequestedFor(urlEqualTo(urlString)));
    }

    @Test
    void testFetchWeather_MissingRequiredField() {
        // Given: Missing 'temp' field in 'main' section of the response
        String urlString = "/weather?q=London&appid=" + apiKey;

        // WireMock Stubbing: Simulate a response with a missing required 'temp' field
        wireMockExtension.stubFor(get(urlEqualTo(urlString))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{ \"weather\": [{ \"main\": \"Clear\", \"description\": \"clear sky\" }], "
                                + "\"main\": { \"feels_like\": 20.0 }, "
                                + "\"wind\": { \"speed\": 5.0 }, "
                                + "\"sys\": { \"sunrise\": 1600000000, \"sunset\": 1600020000 }, "
                                + "\"name\": \"London\" }")));

        // When: Calling fetchWeather with missing 'temp' field
        WeatherAPIException exception = assertThrows(WeatherAPIException.class,
                () -> weatherClient.fetchWeather("London"));

        // Then: Verify exception message
        assertNotNull(exception, "Exception should not be null");
        assertTrue(exception.getMessage().contains("Missing 'temp' in 'main' field"),
                "The exception message should indicate missing 'temp' field.");

        // Verify WireMock received the request
        wireMockExtension.verify(exactly(1), getRequestedFor(urlEqualTo(urlString)));
    }

    @Test
    void testFetchWeather_InvalidTextInRequiredField() {
        // Given: Invalid 'description' field in 'weather' section of the response
        String urlString = "/weather?q=London&appid=" + apiKey;

        // WireMock Stubbing: Simulate a response with invalid 'description' (empty string)
        wireMockExtension.stubFor(get(urlEqualTo(urlString))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{ \"weather\": [{ \"main\": \"Clear\", \"description\": \"\" }], "
                                + "\"main\": { \"temp\": 22.0, \"feels_like\": 20.0 }, "
                                + "\"wind\": { \"speed\": 5.0 }, "
                                + "\"sys\": { \"sunrise\": 1600000000, \"sunset\": 1600020000 }, "
                                + "\"name\": \"London\" }")));

        // When: Calling fetchWeather with invalid 'description' field
        WeatherAPIException exception = assertThrows(WeatherAPIException.class,
                () -> weatherClient.fetchWeather("London"));

        // Then: Verify exception message
        assertNotNull(exception, "Exception should not be null");
        assertTrue(exception.getMessage().contains("Missing 'description' in 'weather' field"),
                "The exception message should indicate missing 'description' field.");

        // Verify WireMock received the request
        wireMockExtension.verify(exactly(1), getRequestedFor(urlEqualTo(urlString)));
    }
}