package com.demo.sdk;

import com.demo.sdk.client.WeatherClient;
import com.demo.sdk.cache.WeatherCache;
import com.demo.sdk.enums.Mode;
import com.demo.sdk.exception.WeatherAPIException;
import com.demo.sdk.model.WeatherData;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.*;

/**
 * The {@code WeatherSDK} class serves as the main entry point for the Weather SDK.
 * It allows users to fetch weather data from the OpenWeather API using different operational modes:
 * <ul>
 *     <li>{@code ON_DEMAND} - Fetches weather data on request.</li>
 *     <li>{@code POLLING} - Automatically refreshes cached data at regular intervals.</li>
 * </ul>
 * This class ensures that only one instance of the SDK exists per API key at a time.
 * <p>
 * The SDK validates the inputs and throws exceptions for invalid values. Invalid API keys, city names,
 * or mode selections will result in appropriate exceptions.
 * </p>
 * <p>
 * The SDK also provides a testing constructor that allows the injection of mocked {@code WeatherClient} and
 * {@code WeatherCache} objects for unit tests, bypassing real API calls and cache storage.
 * </p>
 */
public class WeatherSDK {

    private static final Logger log = LoggerFactory.getLogger(WeatherSDK.class);
    private static final Map<String, WeatherSDK> instances = new ConcurrentHashMap<>();
    private final String apiKey;
    private final Mode mode;
    private final WeatherClient weatherClient;
    private final WeatherCache cache;
    private ScheduledExecutorService scheduler;
    private boolean isShutdown = false;

    /**
     * Private constructor to enforce controlled instance creation.
     * Initializes the weather client and cache storage.
     * If {@code POLLING} mode is selected, starts the polling scheduler.
     *
     * @param apiKey The API key used to authenticate requests to the OpenWeather API.
     * @param mode   The mode of operation ({@code ON_DEMAND} or {@code POLLING}).
     */
    private WeatherSDK(String apiKey, Mode mode) {
        validateApiKey(apiKey);
        validateMode(mode);

        this.apiKey = apiKey;
        this.mode = mode;
        this.weatherClient = new WeatherClient(apiKey);
        this.cache = new WeatherCache();

        if (mode == Mode.POLLING) {
            startPolling();
        }
    }

    /**
     * Overloaded constructor for testing purposes.
     * This constructor allows the injection of mocked {@code WeatherClient} and {@code WeatherCache}
     * objects to bypass real API calls and cache storage during unit tests.
     *
     * @param apiKey        The API key used for authentication (same validation as the main constructor).
     * @param mode          The operational mode of the SDK (either {@link Mode#ON_DEMAND} or {@link Mode#POLLING}).
     * @param weatherClient A mocked {@code WeatherClient} instance used to simulate weather data retrieval.
     * @param weatherCache  A mocked {@code WeatherCache} instance used to simulate cache lookups and storage.
     */
    WeatherSDK(String apiKey, Mode mode, WeatherClient weatherClient, WeatherCache weatherCache) { // Package-private
        validateApiKey(apiKey);
        validateMode(mode);

        this.apiKey = apiKey;
        this.mode = mode;
        this.weatherClient = weatherClient;
        this.cache = weatherCache;

        if (mode == Mode.POLLING) {
            startPolling();
        }
    }

    /**
     * Retrieves an existing or creates a new {@code WeatherSDK} instance for the given API key
     * Ensures only one instance exists per API key, creating a new instance if one does not already exist.
     *
     * @param apiKey The API key used for authentication.
     * @param mode   The mode of operation ({@code ON_DEMAND} or {@code POLLING}).
     * @return A {@code WeatherSDK} instance associated with the specified API key.
     */
    public static synchronized WeatherSDK getInstance(String apiKey, Mode mode) {
        return instances.computeIfAbsent(apiKey, key -> new WeatherSDK(key, mode));
    }

    /**
     * Deletes the {@code WeatherSDK} instance for the specified API key and stops any polling tasks if running.
     * If polling mode was enabled, it stops any scheduled polling tasks before removal.
     *
     * @param apiKey The API key of the instance to be removed.
     */
    public static synchronized void deleteInstance(String apiKey) {
        WeatherSDK instance = instances.remove(apiKey);
        if (instance != null) {
            instance.shutdown();
        }
    }

    /**
     * Retrieves the cached weather data for the specified city,
     * or fetches fresh data from the OpenWeather API if not cached.
     * The data is then converted into a JSON format before being returned.
     * <p>
     * The method first checks the cache for the city's weather data. If the data is not found, it fetches fresh data
     * from the API and stores it in the cache for future use.
     * </p>
     *
     * @param cityName The name of the city for which the weather data is to be retrieved.
     * @return A {@link JsonNode} containing the weather data for the specified city in JSON format.
     * @throws WeatherAPIException If there is an issue fetching weather data from the API.
     */
    public JsonNode getWeather(String cityName) throws WeatherAPIException {
        if (isShutdown) {
            throw new WeatherAPIException("SDK is shut down");
        }
        validateCityName(cityName);

        // Check cache
        WeatherData weatherData = cache.get(cityName);
        if (weatherData == null) {
            // Fetch fresh data from API
            weatherData = weatherClient.fetchWeather(cityName);
            cache.put(cityName, weatherData);
        }

        // Convert WeatherData to JSON (not a string)
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.valueToTree(weatherData);
    }

    /**
     * Retrieves the cached weather data for the specified city,
     * or fetches fresh data from the OpenWeather API if not cached.
     * The data is then converted into a JSON string before being returned.
     * <p>
     * This method serves as a helper to provide the weather data as a JSON string,
     * without requiring additional dependencies to be installed when the SDK is used.
     * </p>
     *
     * @param cityName The name of the city for which the weather data is to be retrieved.
     * @return A JSON string containing the weather data for the specified city.
     * @throws WeatherAPIException If there is an issue fetching weather data from the API or serializing it to string.
     */
    public String getWeatherAsJsonString(String cityName) throws WeatherAPIException {
        JsonNode jsonNode = getWeather(cityName);
        // Convert JsonNode to a JSON string and return it
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writeValueAsString(jsonNode);
        } catch (JsonProcessingException e) {
            log.error("Error while parsing json object", e);
            throw new WeatherAPIException("Error while parsing json object: " + e);
        }
    }

    /**
     * Starts a scheduled task to automatically refresh cached weather data for all stored cities every 10 minutes.
     * <p>
     * The method iterates through all stored cities in the cache and fetches updated weather data.
     * If an update fails, the error is logged but does not interrupt the process.
     * </p>
     */
    private void startPolling() {
        scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> {
            for (String city : cache.getStoredCities()) {
                try {
                    // Fetch and update weather data for the stored city
                    WeatherData updatedData = weatherClient.fetchWeather(city);
                    cache.put(city, updatedData);
                    log.info("Updating weather data for city: {}", city);
                } catch (WeatherAPIException e) {
                    log.error("Failed to update weather for {}: {}", city, e.getMessage());
                }
            }
        }, 0, 10, TimeUnit.MINUTES);
    }

    /**
     * Shuts down the polling scheduler if the SDK instance is operating in polling mode.
     * Has no effect if polling was not enabled.
     * <p>
     * This method is called automatically when the SDK instance is deleted.
     * If polling was never enabled, calling this method has no effect.
     * </p>
     */
    public void shutdown() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
        isShutdown = true;
    }

    /**
     * Validates the provided API key.
     * <p>
     * The API key must be a non-null, non-empty string containing only alphanumeric characters,
     * hyphens (-), and underscores (_). Leading and trailing spaces are trimmed before validation.
     * </p>
     *
     * @param apiKey The API key to validate.
     * @throws IllegalArgumentException If the API key is null, empty, or contains invalid characters.
     */
    private void validateApiKey(String apiKey) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalArgumentException("API Key cannot be null or empty");
        }
        String trimmedApiKey = apiKey.trim();
        if (!trimmedApiKey.matches("^[a-zA-Z0-9-_]+$")) {
            throw new IllegalArgumentException("API Key contains invalid characters");
        }
    }

    /**
     * Validates the provided city name.
     * <p>
     * The city name must be a non-null, non-empty string containing only letters, spaces,
     * periods (.), apostrophes ('), and hyphens (-). Leading and trailing spaces
     * are trimmed before validation.
     * </p>
     *
     * @param cityName The city name to validate.
     * @throws IllegalArgumentException If the city name is null, empty, or contains invalid characters.
     */
    private void validateCityName(String cityName) {
        if (cityName == null || cityName.trim().isEmpty()) {
            throw new IllegalArgumentException("City name cannot be null or empty");
        }
        String trimmedCityName = cityName.trim();
        if (!trimmedCityName.matches("^[a-zA-Z\\s.'-]+$")) {
            throw new IllegalArgumentException("City name contains invalid characters");
        }
    }

    /**
     * Validates the provided mode.
     * <p>
     * The mode must be either {@link Mode#ON_DEMAND} or {@link Mode#POLLING}.
     * </p>
     *
     * @param mode The mode to validate.
     * @throws IllegalArgumentException If the mode is null or not one of the valid modes.
     */
    private void validateMode(Mode mode) {
        if (mode == null) {
            throw new IllegalArgumentException("Mode cannot be null");
        }
        if (!(mode == Mode.ON_DEMAND || mode == Mode.POLLING)) {
            throw new IllegalArgumentException("Invalid mode. Must be either ON_DEMAND or POLLING.");
        }
    }
}