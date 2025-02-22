package com.demo.sdk.client;

import com.demo.sdk.exception.WeatherAPIException;
import com.demo.sdk.model.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

/**
 * The {@code WeatherClient} class is responsible for communicating with the OpenWeather API
 * to fetch weather data for a given city. It handles network requests, response parsing,
 * error handling, and validates the received JSON response to ensure all required fields are present.
 * <p>
 * If any required field is missing in the API response, this class logs the malformed JSON response
 * for debugging purposes
 * and throws a {@code WeatherAPIException} with a detailed error message.
 * </p>
 */
public class WeatherClient {

    /**
     * The base URL for the OpenWeather API, which can be customized for testing with mock servers.
     */
    private final String baseUrl;

    /**
     * The API key used to authenticate requests to the OpenWeather API.
     */
    private final String apiKey;

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String DEFAULT_BASE_URL = "https://api.openweathermap.org/data/2.5";
    private static final String WEATHER_URL = "/weather?q="; // segment of url before city name
    private static final String APPID = "&appid="; // segment of url after city name
    private static final Logger log = LoggerFactory.getLogger(WeatherClient.class);
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    /**
     * Constructs a {@code WeatherClient} with the provided API key and the default OpenWeather API base URL.
     *
     * @param apiKey The OpenWeather API key used for authentication.
     */
    public WeatherClient(String apiKey) {
        this(DEFAULT_BASE_URL, apiKey); // call to a 2nd constructor
    }

    /**
     * Constructs a {@code WeatherClient} with a custom base URL (for testing purposes) and an API key.
     * This constructor allows using a mock server like WireMock for integration tests.
     *
     * @param baseUrl The base URL of the OpenWeather API or a mock server.
     * @param apiKey  The OpenWeather API key used for authentication.
     */
    public WeatherClient(String baseUrl, String apiKey) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
    }

    /**
     * Fetches the raw JSON response from the OpenWeather API for a given city.
     * <p>
     * This method sends a GET request to the OpenWeather API to fetch weather data for the provided city name,
     * encoding the city name to ensure special characters are handled properly. It handles various HTTP errors,
     * including unauthorized access (invalid API key), unexpected status codes, and empty responses.
     * </p>
     *
     * @param cityName The name of the city for which weather data is requested.
     * @return A JSON string containing the raw response from the OpenWeather API.
     * @throws WeatherAPIException If an error occurs during the API request or if the response is invalid.
     */
    public String fetchRawWeatherJson(String cityName) throws WeatherAPIException {
        try {
            // Encode the city name to properly format spaces and special characters
            String encodedCity = URLEncoder.encode(cityName, StandardCharsets.UTF_8);

            // Construct the full API request URL
            URI uri = URI.create(baseUrl + WEATHER_URL + encodedCity + APPID + apiKey);

            // Build the HTTP request
            HttpRequest request = HttpRequest.newBuilder().uri(uri).GET().build();

            // Send the request and receive the response
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // Handle unauthorized API key error
            if (response.statusCode() == HttpURLConnection.HTTP_UNAUTHORIZED) {
                log.warn("Unauthorized access");
                throw new WeatherAPIException("Invalid API key");
            }

            // Handle other non-OK HTTP responses
            if (response.statusCode() != HttpURLConnection.HTTP_OK) {
                log.warn("Unexpected response code: {}", response.statusCode());
                throw new WeatherAPIException("Failed to fetch weather data: " + response.body());
            }

            // Retrieve the response body
            String responseBody = response.body();
            if (responseBody == null || responseBody.isEmpty()) {
                log.warn("Response body is empty");
                throw new WeatherAPIException("Received empty response from API.");
            }

            return responseBody;
        } catch (IOException | InterruptedException e) {
            // Restore interrupted state in case of thread interruption
            Thread.currentThread().interrupt();
            log.error("Error communicating with the weather API: {}", e.getMessage());
            throw new WeatherAPIException("Error communicating with the weather API: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error occurred: {}", e.getMessage());
            throw new WeatherAPIException("Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Fetches structured weather data for a given city by making a request to the OpenWeather API
     * and parsing the response into a {@code WeatherData} object.
     * <p>
     * This method not only fetches the raw JSON response but also validates that all necessary fields are present
     * and throws a {@code WeatherAPIException} if any of the required fields are missing or malformed.
     * </p>
     *
     * @param cityName The name of the city for which weather data is requested.
     * @return A {@code WeatherData} object containing structured weather details.
     * @throws WeatherAPIException If an error occurs while fetching or parsing the data.
     */
    public WeatherData fetchWeather(String cityName) throws WeatherAPIException {
        try {
            // Fetch the raw JSON response from the API
            String rawJson = fetchRawWeatherJson(cityName);

            // Parse the JSON response into a tree structure
            JsonNode rootNode = objectMapper.readTree(rawJson);

            // Validate the required fields in the JSON response
            validateWeatherDataFields(rootNode);

            // Convert JSON data into a structured WeatherData object
            return createWeatherDataFromJson(rootNode);

        } catch (IOException e) {
            log.error("Failed to parse weather data due to JSON issue: {}", e.getMessage());
            throw new WeatherAPIException("Failed to parse weather data due to JSON issue: " + e.getMessage());
        } catch (Exception e) {
            log.error("Failed to parse weather data: {}", e.getMessage());
            throw new WeatherAPIException("Failed to parse weather data: " + e.getMessage());
        }
    }

    /**
     * Parses the given JSON node and creates a {@code WeatherData} object.
     * <p>
     * This method extracts relevant weather data from the JSON response, such as weather conditions, temperature,
     * wind speed, and system information (like sunrise and sunset times). It then populates a {@code WeatherData}
     * object with this data.
     * </p>
     *
     * @param rootNode The root node of the parsed JSON response from the OpenWeather API.
     * @return A {@code WeatherData} object populated with data extracted from the JSON.
     */
    private WeatherData createWeatherDataFromJson(JsonNode rootNode) {
        Weather weather = new Weather(
                rootNode.get("weather").get(0).get("main").asText(),
                rootNode.get("weather").get(0).get("description").asText()
        );

        Temperature temperature = new Temperature(
                rootNode.get("main").get("temp").asDouble(),
                rootNode.get("main").get("feels_like").asDouble()
        );

        Wind wind = new Wind(
                rootNode.get("wind").get("speed").asDouble()
        );

        Sys sys = new Sys(
                rootNode.get("sys").get("sunrise").asLong(),
                rootNode.get("sys").get("sunset").asLong()
        );

        return new WeatherData(
                weather,
                temperature,
                rootNode.has("visibility") ? rootNode.get("visibility").asInt() : 0,
                wind,
                rootNode.has("dt") ? rootNode.get("dt").asLong() : 0,
                sys,
                rootNode.has("timezone") ? rootNode.get("timezone").asInt() : 0,
                rootNode.has("name") ? rootNode.get("name").asText() : "Unknown"
        );
    }

    /**
     * Validates that the necessary fields are present and not null in the API response.
     * <p>
     * This method checks for the presence and non-nullity of critical fields in the JSON response, such as weather
     * conditions, temperature, wind speed, and system information (e.g., sunrise and sunset times). If any required
     * field is missing or invalid, a {@code WeatherAPIException} is thrown with a detailed error message.
     * </p>
     *
     * @param rootNode The root node of the parsed JSON response from the OpenWeather API.
     * @throws WeatherAPIException If any required field is missing or null.
     */
    private void validateWeatherDataFields(JsonNode rootNode) throws WeatherAPIException {
        // Map of required fields and their paths
        Map<String, String> requiredFields = Map.of(
                "weather[0].main", "Missing 'main' in 'weather' field",
                "weather[0].description", "Missing 'description' in 'weather' field",
                "main.temp", "Missing 'temp' in 'main' field",
                "main.feels_like", "Missing 'feels_like' in 'main' field",
                "wind.speed", "Missing 'speed' in 'wind' field",
                "sys.sunrise", "Missing 'sunrise' in 'sys' field",
                "sys.sunset", "Missing 'sunset' in 'sys' field",
                "name", "Missing 'name' field"
        );

        // Iterate over the map and validate each field
        for (Map.Entry<String, String> entry : requiredFields.entrySet()) {
            JsonNode node = getNodeByPath(rootNode, entry.getKey());
            if (node == null || node.asText().isEmpty()) {
                log.error("{}: {}", entry.getValue(), rootNode);
                throw new WeatherAPIException(entry.getValue());
            }
        }
    }

    /**
     * Retrieves the node from the JSON object based on a dot-separated path.
     * <p>
     * This method supports both simple object fields and array elements (e.g., "weather[0].main").
     * It traverses the JSON structure according to the provided path, which may contain both object field names
     * and array indices. If the specified path does not exist, it returns {@code null}.
     * </p>
     *
     * @param rootNode The root node of the parsed JSON response.
     * @param path     The dot-separated path of the field to retrieve (e.g., "weather[0].main").
     * @return The JsonNode for the requested field, or {@code null} if not found.
     */
    private JsonNode getNodeByPath(JsonNode rootNode, String path) {
        String[] parts = path.split("\\.");
        JsonNode currentNode = rootNode;

        for (String part : parts) {
            if (currentNode == null) return null;
            if (part.contains("[")) {
                // Handle array access like 'weather[0]'
                String arrayName = part.substring(0, part.indexOf("["));
                int index = Integer.parseInt(part.substring(part.indexOf("[") + 1, part.indexOf("]")));
                currentNode = currentNode.get(arrayName);
                if (currentNode == null || !currentNode.isArray() || currentNode.size() <= index) {
                    return null; // Return null if the array or index doesn't exist
                }
                currentNode = currentNode.get(index);  // Get the element at the specified index
            } else {
                currentNode = currentNode.get(part);  // Handle normal object fields
            }
        }
        return currentNode;
    }
}