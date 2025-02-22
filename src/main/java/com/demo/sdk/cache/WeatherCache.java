package com.demo.sdk.cache;

import com.demo.sdk.exception.CacheInitializationException;
import com.demo.sdk.model.WeatherData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 * The {@code WeatherCache} class provides in-memory caching for weather data.
 * <p>
 * It maintains a cache for up to 10 cities, ensuring that:
 * <ul>
 *     <li>Entries expire after a configurable time (default 10 minutes).</li>
 *     <li>New entries push out the least recently used (LRU) entries once the cache size exceeds the limit.</li>
 * </ul>
 * This caching mechanism helps reduce unnecessary API calls while keeping weather data reasonably up-to-date.
 * <p>
 * The cache expiration time and maximum size can be configured via the {@code application.properties} file.
 */
public class WeatherCache {

    /**
     * The expiration time for cached weather data in seconds (default is 600 seconds or 10 minutes).
     * This value can be modified through the {@code application.properties} file.
     */
    private final long cacheExpirationTime;

    /**
     * The maximum number of cities that can be stored in the cache.
     * This value is fixed at 10 but can be adjusted by modifying the {@code MAX_CACHE_SIZE} constant.
     */
    private static final int MAX_CACHE_SIZE = 10;
    private static final String APP_PROPERTIES_FILE = "src/main/resources/application.properties";
    private static final String CACHE_EXPIRATION_NAME = "weather.cache.expiration.time";
    private static final String CACHE_EXPIRATION_VALUE = "600"; // same value as in the application.properties

    /**
     * A linked hash map implementing an LRU (Least Recently Used) cache eviction policy.
     * When the cache size exceeds the maximum limit, the oldest accessed entry is automatically removed.
     */
    private final Map<String, CacheEntry> cache;

    private static final Logger log = LoggerFactory.getLogger(WeatherCache.class);

    /**
     * Constructs a {@code WeatherCache} with a limited-size LRU eviction policy.
     * <p>
     * When the cache exceeds {@code MAX_CACHE_SIZE}, the oldest accessed entry is automatically removed.
     * The expiration time for cached entries is loaded from the {@code application.properties} file
     * (defaults to 600 seconds).
     * </p>
     */
    public WeatherCache() {
        Properties properties = new Properties();
        try (FileInputStream input = new FileInputStream(APP_PROPERTIES_FILE)) {
            properties.load(input);

            // Load expiration time from the properties file
            String expirationTime = properties.getProperty(CACHE_EXPIRATION_NAME, CACHE_EXPIRATION_VALUE);
            this.cacheExpirationTime = Long.parseLong(expirationTime);
        } catch (IOException e) {
            log.error("WeatherCache constructor failed, check the application.properties presence and content:", e);
            throw new CacheInitializationException("WeatherCache constructor failed: ", e);
        }

        this.cache = new LinkedHashMap<>(MAX_CACHE_SIZE, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, CacheEntry> eldest) {
                return size() > MAX_CACHE_SIZE;
            }
        };
    }

    /**
     * Retrieves weather data for a given city if it is still valid.
     * <p>
     * If the cached data has expired, it is removed from the cache and {@code null} is returned.
     * If the city is not found in the cache, {@code null} is also returned.
     * </p>
     *
     * @param cityName The name of the city whose weather data is requested.
     * @return The {@code WeatherData} object if available and valid, otherwise {@code null}.
     */
    public synchronized WeatherData get(String cityName) {
        CacheEntry entry = cache.get(cityName);
        if (entry != null && !isExpired(entry)) {
            return entry.weatherData;
        }
        cache.remove(cityName);
        return null;
    }

    /**
     * Stores weather data in the cache for a specified city.
     * <p>
     * If the cache exceeds its maximum size, the least recently used (LRU) entry is removed automatically.
     * </p>
     *
     * @param cityName The name of the city.
     * @param data     The {@code WeatherData} object to be cached.
     */
    public synchronized void put(String cityName, WeatherData data) {
        long currentTimeMillis = System.currentTimeMillis();
        cache.put(cityName, new CacheEntry(data, currentTimeMillis));
    }

    /**
     * Returns an iterable set of all currently stored city names in the cache.
     * This can be useful for inspecting or managing the cached entries.
     *
     * @return An {@code Iterable<String>} containing city names in the cache.
     */
    public synchronized Iterable<String> getStoredCities() {
        return cache.keySet();
    }

    /**
     * Checks whether a given cache entry has expired based on the cache expiration policy.
     * <p>
     * The expiration time is compared to the current system time. If the entry has expired, {@code true} is returned.
     * </p>
     *
     * @param entry The cache entry to check.
     * @return {@code true} if the entry is expired, {@code false} otherwise.
     */
    private boolean isExpired(CacheEntry entry) {
        long currentTimeMillis = System.currentTimeMillis();
        return currentTimeMillis >= entry.timestamp + cacheExpirationTime;
    }

    /**
     * A record representing a cached weather entry, containing weather data and a timestamp.
     * <p>
     * This is used to store both the weather data and the time at which the entry was added to the cache.
     * The timestamp helps determine when the data should expire.
     * </p>
     *
     * @param weatherData The weather data associated with this cache entry.
     * @param timestamp   The time (in milliseconds since the epoch) when this entry was added.
     */
    private record CacheEntry(
            WeatherData weatherData,
            long timestamp) {
    }
}