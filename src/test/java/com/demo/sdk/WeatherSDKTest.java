package com.demo.sdk;

import com.demo.sdk.client.WeatherClient;
import com.demo.sdk.cache.WeatherCache;
import com.demo.sdk.data.TestData;
import com.demo.sdk.enums.Mode;
import com.demo.sdk.exception.WeatherAPIException;
import com.demo.sdk.model.WeatherData;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.lang.reflect.Field;
import java.util.concurrent.ScheduledExecutorService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WeatherSDKTest {

    private static final Logger log = LoggerFactory.getLogger(WeatherSDKTest.class);
    private final String apiKey = "weatherApiKey";
    private WeatherSDK weatherSDK;

    @Mock
    private WeatherClient weatherClient;

    @Mock
    private WeatherCache weatherCache;

    @BeforeEach
    void setup() {
        // Using the updated constructor for testing that accepts mocks
        weatherSDK = new WeatherSDK(apiKey, Mode.ON_DEMAND, weatherClient, weatherCache);
    }

    @Test
    void testGetWeather_CachedDataAvailable() {
        try {
            // Given: Cached weather data is present
            String cityName = "London";
            WeatherData mockWeatherData = new TestData().getTestData(cityName);
            Mockito.when(weatherCache.get(cityName)).thenReturn(mockWeatherData);

            // When: getWeather is called
            JsonNode result = weatherSDK.getWeather(cityName);

            // Then: Cached data should be returned as JSON
            assertNotNull(result, "Result should not be null");
            assertEquals(mockWeatherData.weather().main(), result.get("weather").get("main").asText());
            assertEquals(mockWeatherData.weather().description(), result.get("weather").get("description").asText());
            assertEquals(mockWeatherData.temperature().temp(), result.get("temperature").get("temp").asDouble());

            // Verify cache was accessed
            Mockito.verify(weatherCache, times(1)).get(cityName);
            // Ensure the real weatherClient was not called (cache hit)
            verify(weatherClient, never()).fetchWeather(anyString());
        } catch (WeatherAPIException e) {
            log.error("testGetWeather_CachedDataAvailable() failed: {}", e.getMessage());
            fail("Test failed due to exception: {}" + e.getMessage());
        }
    }

    @Test
    void testGetWeather_Success_CacheMiss() {
        try {
            // Given: No cached data available for a city
            String cityName = "London";
            when(weatherCache.get(cityName)).thenReturn(null);
            WeatherData freshWeatherData = new TestData().getTestData(cityName);
            when(weatherClient.fetchWeather(cityName)).thenReturn(freshWeatherData);

            // When: Calling getWeather for that city
            JsonNode result = weatherSDK.getWeather(cityName);

            // Then: It should fetch fresh data and cache it
            assertNotNull(result, "Result should not be null");
            assertEquals(freshWeatherData.name(), result.get("name").asText());
            assertEquals(freshWeatherData.weather().main(), result.get("weather").get("main").asText());
            assertEquals(freshWeatherData.weather().description(), result.get("weather").get("description").asText());
            assertEquals(freshWeatherData.temperature().temp(), result.get("temperature").get("temp").asDouble());

            // Verify that fresh data was fetched and cached
            verify(weatherClient, times(1)).fetchWeather(cityName);
            verify(weatherCache, times(1)).put(cityName, freshWeatherData);
        } catch (WeatherAPIException e) {
            log.error("testGetWeather_Success_CacheMiss() failed: {}", e.getMessage());
            fail("Test failed due to exception: {}" + e.getMessage());
        }
    }

    @Test
    void testGetWeather_Success_FreshData() {
        try {
            // Given: No cached data available for the city, fresh data is needed
            String cityName = "London";
            WeatherData freshWeatherData = new TestData().getTestData(cityName);
            when(weatherCache.get(cityName)).thenReturn(null);
            when(weatherClient.fetchWeather(cityName)).thenReturn(freshWeatherData);

            // When: Calling getWeather for the city
            JsonNode result = weatherSDK.getWeather(cityName);

            // Then: It should fetch fresh data and cache it
            assertNotNull(result, "Result should not be null");
            assertEquals(freshWeatherData.name(), result.get("name").asText());
            assertEquals(freshWeatherData.weather().main(), result.get("weather").get("main").asText());
            assertEquals(freshWeatherData.weather().description(), result.get("weather").get("description").asText());
            assertEquals(freshWeatherData.temperature().temp(), result.get("temperature").get("temp").asDouble());

            // Verify that fresh data was fetched and then put into the cache
            verify(weatherClient, times(1)).fetchWeather(cityName);
            verify(weatherCache, times(1)).put(cityName, freshWeatherData);
        } catch (WeatherAPIException e) {
            log.error("testGetWeather_Success_FreshData() failed: {}", e.getMessage());
            fail("Test failed due to exception: {}" + e.getMessage());
        }
    }

    @Test
    void testGetWeatherAsJsonString_Success() {
        try {
            // Given
            String cityName = "London";
            WeatherData mockWeatherData = new TestData().getTestData(cityName);
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode expectedJsonNode = objectMapper.valueToTree(mockWeatherData);
            String expectedJsonString = objectMapper.writeValueAsString(expectedJsonNode);

            // Stub the cache to return our mockWeatherData so that getWeather() uses it.
            when(weatherCache.get(cityName)).thenReturn(mockWeatherData);

            // When: Calling getWeatherAsJsonString, which internally calls getWeather()
            String result = weatherSDK.getWeatherAsJsonString(cityName);

            // Then: Verify the result is as expected
            assertNotNull(result, "JSON result should not be null");
            assertEquals(expectedJsonString, result, "JSON string should match the expected JsonNode output");

            // Verify that getWeather() was indirectly called (via weatherCache.get) and no API call was made.
            verify(weatherCache, times(1)).get(cityName);
            verify(weatherClient, never()).fetchWeather(anyString());
        } catch (WeatherAPIException | JsonProcessingException e) {
            log.error("testGetWeatherAsJsonString_Success() failed: {}", e.getMessage());
            fail("Test failed due to exception: " + e.getMessage());
        }
    }

    @Test
    void testGetWeather_Failure_InvalidCityName() {
        // Given: An invalid city name is provided
        String invalidCityName = "Invalid_City_123";

        // When: Calling getWeather with the invalid city name
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
                weatherSDK.getWeather(invalidCityName));

        // Then: It should throw an IllegalArgumentException with an appropriate message
        assertTrue(exception.getMessage().contains("City name contains invalid characters"));
    }

    @Test
    void testGetWeather_Failure_InvalidApiKey() {
        // Given: A WeatherSDK instance with an invalid API key
        String invalidApiKey = "InvalidKey#123";

        // When: Creating the WeatherSDK instance with the invalid API key
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> new WeatherSDK(invalidApiKey, Mode.ON_DEMAND, weatherClient, weatherCache));

        // Then: It should throw an IllegalArgumentException with an appropriate message
        assertTrue(exception.getMessage().contains("API Key contains invalid characters"));
    }

    @Test
    void testValidMode_ON_DEMAND() {
        // Given: A valid mode (ON_DEMAND)
        Mode validMode = Mode.ON_DEMAND;

        // When: Creating a WeatherSDK instance with the valid mode
        // Then: No exception should be thrown
        assertDoesNotThrow(() -> new WeatherSDK(apiKey, validMode, weatherClient, weatherCache));
    }

    @Test
    void testValidMode_POLLING() {
        // Given: A valid mode (POLLING)
        Mode validMode = Mode.POLLING;

        // When: Creating a WeatherSDK instance with the valid mode
        // Then: No exception should be thrown
        assertDoesNotThrow(() -> new WeatherSDK(apiKey, validMode, weatherClient, weatherCache));
    }

    @Test
    void testInvalidMode_NullMode() {
        // Given: Null mode
        Mode invalidMode = null;

        // When: Trying to create a WeatherSDK instance with null mode
        // Then: IllegalArgumentException should be thrown
        assertThrows(IllegalArgumentException.class,
                () -> new WeatherSDK(apiKey, invalidMode, weatherClient, weatherCache));
    }

    @Test
    void testInvalidMode_InvalidMode() {
        // Given: An invalid mode (not ON_DEMAND or POLLING)
        // When: Trying to create a WeatherSDK instance with an invalid mode
        // Then: IllegalArgumentException should be thrown
        assertThrows(IllegalArgumentException.class,
                () -> new WeatherSDK(apiKey, Mode.valueOf("INVALID_MODE"), weatherClient, weatherCache));
    }

    @Test
    void testDeleteInstance_Success() {
        try {
            // Given: A WeatherSDK instance has been created
            WeatherSDK weatherSDKInstance = WeatherSDK.getInstance(apiKey, Mode.ON_DEMAND);

            // When: The instance is deleted in one thread
            Thread deleteThread = new Thread(() -> WeatherSDK.deleteInstance(apiKey));
            deleteThread.start();

            // Wait for the delete thread to finish
            deleteThread.join();

            // Then: A new instance should be created after deletion, not null
            WeatherSDK instanceAfterDeletion = WeatherSDK.getInstance(apiKey, Mode.ON_DEMAND);

            // Assert: The instance should not be null, but a new instance should be created
            assertNotNull(instanceAfterDeletion);
            assertNotEquals(weatherSDKInstance, instanceAfterDeletion);  // Ensure it's a new instance
        } catch (InterruptedException e) {
            log.error("testDeleteInstance_Success() failed: {}", e.getMessage());
            fail("Test failed due to exception: {}" + e.getMessage());
        }
    }

    @Test
    void testShutdown() throws Exception {
        // Given
        ScheduledExecutorService schedulerMock = mock(ScheduledExecutorService.class);
        Field schedulerField = WeatherSDK.class.getDeclaredField("scheduler");
        schedulerField.setAccessible(true);
        schedulerField.set(weatherSDK, schedulerMock);

        // When
        weatherSDK.shutdown();

        // Then
        verify(schedulerMock, times(1)).shutdown();

        // Verify a subsequent getWeather call throws exception
        Exception exception = assertThrows(WeatherAPIException.class, () -> weatherSDK.getWeather("London"));
        assertTrue(exception.getMessage().contains("SDK is shut down"),
                "Exception should indicate that the SDK is shut down");
    }
}