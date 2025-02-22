package com.demo.sdk;

import com.demo.sdk.cache.WeatherCache;
import com.demo.sdk.data.TestData;
import com.demo.sdk.model.WeatherData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class WeatherCacheTest {

    @InjectMocks
    private WeatherCache weatherCache;

    private static final Logger log = LoggerFactory.getLogger(WeatherCacheTest.class);

    @Test
    void testPutAndGet_Success() {
        // Given: A city and weather data to cache
        String cityName = "London";
        WeatherData weatherData = new TestData().getTestData(cityName);

        // When: Putting the weather data in the cache
        weatherCache.put(cityName, weatherData);

        // Then: The weather data should be retrievable from the cache
        WeatherData cachedData = weatherCache.get(cityName);
        assertNotNull(cachedData);
        assertEquals(cityName, cachedData.name());
        assertEquals("clear sky", cachedData.weather().description());
        assertEquals(28.28, cachedData.temperature().temp());
    }

    @Test
    void testGet_CacheMiss() {
        // Given: No data cached for a city
        String cityName = "Paris";

        // When: Attempting to retrieve weather data for that city
        WeatherData cachedData = weatherCache.get(cityName);

        // Then: It should return null as the city is not cached
        assertNull(cachedData);
    }

    @Test
    void testPut_ExceedsCacheSize() {
        // Given: 10 different cities with weather data
        for (int i = 1; i <= 10; i++) {
            String cityName = "London" + i;
            WeatherData weatherData = new TestData().getTestData(cityName);
            weatherCache.put(cityName, weatherData);
        }

        // When: Adding another city, pushing out the least recently used (LRU) city
        String newCityName = "Paris";
        WeatherData newWeatherData = new TestData().getTestData(newCityName);
        weatherCache.put(newCityName, newWeatherData);

        // Then: The first city (City1) should be evicted, and the new city should be present
        assertNull(weatherCache.get("London1")); // City1 should be evicted
        WeatherData cachedData = weatherCache.get(newCityName);
        assertNotNull(cachedData); // The new city should be in the cache
        assertEquals(newCityName, cachedData.name());
    }

    @Test
    void testGetStoredCities() {
        // Given: Several cities stored in the cache
        for (int i = 1; i <= 5; i++) {
            String cityName = "London" + i;
            WeatherData weatherData = new TestData().getTestData(cityName);
            weatherCache.put(cityName, weatherData);
        }

        // When: Getting the stored cities from the cache
        Iterable<String> storedCities = weatherCache.getStoredCities();

        // Then: It should return all the cities currently in the cache
        assertEquals(5, storedCities.spliterator().getExactSizeIfKnown());
    }

    @Test
    void testGet_MultipleCacheEntriesWithDifferentExpirationTimes() {
        try {
            // Given: Two cities with different cache expiration times
            String cityName1 = "Paris";  // Short expiration time (0.1 second)
            String cityName2 = "London"; // Long expiration time (1 second)

            WeatherData weatherData1 = new TestData().getTestData(cityName1);
            WeatherData weatherData2 = new TestData().getTestData(cityName2);

            // Create WeatherCache instance (will load properties from file)
            WeatherCache weatherCache;

            // Set a short expiration time for Paris (0.1 second)
            updateExpirationTimeInPropertiesFile("100");  // Set 0.1 second expiration in the properties file

            // Create a new WeatherCache instance after modifying the properties file
            weatherCache = new WeatherCache(); // This will load the updated expiration time (0.1 second)

            // Put the first weather data entry into the cache (Paris)
            weatherCache.put(cityName1, weatherData1);

            // Simulate cache expiration by waiting for some time (ensuring the first entry expires)
            try {
                Thread.sleep(750); // Sleep for 0.75 seconds to ensure Paris entry expires
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // Set a long expiration time for London (1 second)
            updateExpirationTimeInPropertiesFile("1000");  // Set 1 second expiration in the properties file

            // Create a new WeatherCache instance after modifying the properties file
            weatherCache = new WeatherCache(); // This will load the updated expiration time (1 second)

            // Put the second weather data entry into the cache (London)
            weatherCache.put(cityName2, weatherData2);

            // When: Trying to get both weather data entries after some time
            WeatherData cachedData1 = weatherCache.get(cityName1); // Should be expired
            WeatherData cachedData2 = weatherCache.get(cityName2); // Should still be present

            // Then: The first entry (Paris) should be null as it expired
            assertNull(cachedData1, "Cache entry for Paris should have expired and be null.");

            // And: The second entry (London) should still be present as it hasn't expired yet
            assertNotNull(cachedData2, "Cache entry for London should still be present.");
        } catch (IOException e) {
            log.error("testGet_MultipleCacheEntriesWithDifferentExpirationTimes() failed: {}, " +
                    "check the test-application.properties file presence and content", e.getMessage());
            fail("Test failed due to exception: {}" + e.getMessage());
        }
    }

    // helper method
    private void updateExpirationTimeInPropertiesFile(String expirationTime) throws IOException {
        String pathToFile = "src/test/resources/test-application.properties";

        // Load the properties file
        Properties properties = new Properties();
        FileInputStream input = new FileInputStream(pathToFile);
        properties.load(input);

        // Set the new expiration time in the properties
        properties.setProperty("weather.cache.expiration.time", expirationTime);

        // Save the updated properties back to the file
        FileOutputStream output = new FileOutputStream(pathToFile);
        properties.store(output, null);
    }
}