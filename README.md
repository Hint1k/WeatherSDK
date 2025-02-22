# Weather SDK

## Introduction

The Weather SDK provides an easy-to-use interface for fetching weather data from the OpenWeather API. The SDK supports both on-demand and polling modes, allowing users to retrieve real-time weather data or automatically update cached weather information.

This README provides basic installation and usage instructions.

## Contents

- [Installation](#installation)
- [Configuration](#configuration)
- [Usage Example](#usage-example)

## Installation
### Downloading SDK
The Weather SDK is available as source code and can be downloaded and built into a JAR file using Gradle or Maven:
#### Gradle
```gradle
./gradlew build
```
#### Maven
```text
mvn clean package
```
### Adding SDK to Your Project

To use the Weather SDK in your project, you need to add it as a dependency in your build configuration:
#### Gradle
```gradle
implementation 'com.demo.sdk:weather-sdk:1.0-SNAPSHOT'
```

#### Maven
```xml
<dependency>
    <groupId>com.demo.sdk</groupId>
    <artifactId>weather-sdk</artifactId>
    <version>1.0-SNAPSHOT</version>
    <scope>system</scope>
    <systemPath>${project.basedir}/build/libs/sdk-1.0-SNAPSHOT.jar</systemPath>
</dependency>
```

### Using the SDK

Once the JAR file is built, you can add it to your project's classpath and start using the Weather SDK.

## Configuration

1. Obtain an API key from [OpenWeather](https://openweathermap.org/api).
2. Configure the SDK with the API key in `.env`:

```properties
API_KEY=your_secret_api_key_here
```

## Usage Example

### Fetch Weather Data On-Demand

```java
import com.demo.sdk.WeatherSDK;
import com.demo.sdk.enums.Mode;
import com.demo.sdk.exception.WeatherAPIException;
import com.fasterxml.jackson.databind.JsonNode;
import io.github.cdimascio.dotenv.Dotenv;

public class WeatherOnDemand {
    public static void main(String[] args) {
        try {
            String apiKey = ConfigLoader.getApiKey();
            WeatherSDK weatherSDK = WeatherSDK.getInstance(apiKey, Mode.ON_DEMAND);
            String city = "London";
            JsonNode weatherData = weatherSDK.getWeather(city);
            // Alternative: String jsonString = weatherSDK.getWeatherAsJsonString(city);
            System.out.println("Weather data for " + city + ": " + weatherData.toPrettyString());
        } catch (WeatherAPIException e) {
            e.printStackTrace();
        }
    }
    public static class ConfigLoader {
        public static String getApiKey() {
            Dotenv dotenv = Dotenv.load();
            return dotenv.get("API_KEY");
        }
    }
}
```

### Expected outcome
```json
{
  "weather": {
    "main": "Clouds",
    "description": "scattered clouds"
  },
  "temperature": {
    "temp": 284.37,
    "feels_like": 283.92
  },
  "visibility": 10000,
  "wind": {
    "speed": 3.09
  },
  "datetime": 1740224650,
  "sys": {
    "sunrise": 1740207632,
    "sunset": 1740245267
  },
  "timezone": 0,
  "name": "London"
}
```

### Fetch Weather Data for Multiple Cities with Polling

```java
import com.demo.sdk.WeatherSDK;
import com.demo.sdk.enums.Mode;
import com.demo.sdk.exception.WeatherAPIException;
import com.fasterxml.jackson.databind.JsonNode;
import io.github.cdimascio.dotenv.Dotenv;
import java.util.List;
import java.util.Arrays;

public class WeatherPolling {
    public static void main(String[] args) {
        String apiKey = ConfigLoader.getApiKey();
        WeatherSDK weatherSDK = WeatherSDK.getInstance(apiKey, Mode.POLLING);

        List<String> cities = Arrays.asList("New York", "Tokyo", "Paris", "Berlin", "London");

        for (String city : cities) {
            try {
                JsonNode weatherData = weatherSDK.getWeather(city);
                // Alternative: String jsonString = weatherSDK.getWeatherAsJsonString(city);
                System.out.println("Weather data for " + city + ": " + weatherData);
            } catch (WeatherAPIException e) {
                System.err.println("❌ Error fetching weather data for " + city + ": " + e.getMessage());
            }
        }

        // Schedule SDK shutdown after 25 minutes
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                System.out.println("\n⏳ 25 minutes elapsed. Shutting down polling mode...");
                weatherSDK.shutdown(); // Ensures clean shutdown in polling mode
                System.exit(0);
            }
        }, 25 * 60 * 1000); // 25 minutes in milliseconds

        System.out.println("\n✅ Application is running in polling mode for 25 minutes...");
    }

    public static class ConfigLoader {
        public static String getApiKey() {
            Dotenv dotenv = Dotenv.load();
            return dotenv.get("API_KEY");
        }
    }
}
```

### Multiple instances of SDK

```java
import com.demo.sdk.WeatherSDK;
import com.demo.sdk.enums.Mode;
import com.demo.sdk.exception.WeatherAPIException;
import com.fasterxml.jackson.databind.JsonNode;
import io.github.cdimascio.dotenv.Dotenv;

public class WeatherMultipleInstances {
    public static void main(String[] args) {
        try {
            // Load API keys from environment variables
            String apiKey1 = ConfigLoader.getApiKey("API_KEY_1");
            String apiKey2 = ConfigLoader.getApiKey("API_KEY_2");

            // Create SDK instances for different API keys
            WeatherSDK sdkInstance1 = WeatherSDK.getInstance(apiKey1, Mode.ON_DEMAND);
            WeatherSDK sdkInstance2 = WeatherSDK.getInstance(apiKey2, Mode.POLLING);

            String city = "London";

            // Fetch weather data using the first instance (ON_DEMAND)
            JsonNode weatherData1 = sdkInstance1.getWeather(city);
            System.out.println("Weather data (API Key 1) for " + city + ": " + weatherData1.toPrettyString());

            // Fetch weather data using the second instance (POLLING)
            JsonNode weatherData2 = sdkInstance2.getWeather(city);
            System.out.println("Weather data (API Key 2) for " + city + ": " + weatherData2.toPrettyString());

            // Wait for 5 seconds to allow polling to run before shutdown
            Thread.sleep(5000);  // Sleep for 5 seconds to allow polling to happen

            // Clean up: Shutdown the polling task manually before deleting the instance
            sdkInstance2.shutdown();  // Stop the polling task manually
            System.out.println("Polling task for API Key 2 stopped.");

            // Now delete the instance for apiKey2 (POLLING) after shutdown
            WeatherSDK.deleteInstance(apiKey2);
            System.out.println("Instance for API Key 2 deleted successfully.");

            // Delete the instance for apiKey1 (ON_DEMAND)
            WeatherSDK.deleteInstance(apiKey1);
            System.out.println("Instance for API Key 1 deleted successfully.");

        } catch (WeatherAPIException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Utility class for loading API keys from environment variables.
     */
    public static class ConfigLoader {
        public static String getApiKey(String keyName) {
            Dotenv dotenv = Dotenv.load();
            return dotenv.get(keyName);
        }
    }
}
```

## Additional Features

- **Caching**: Weather data is cached for up to 10 cities to minimize API requests.
- **Automatic Polling**: In polling mode, weather data is refreshed every 10 minutes.
- **SDK Version Publishing**: Upload new versions of the SDK to GitHub Packages using the provided script,
  publish-sdk.sh. This script automates the process of building the SDK JAR file and uploading it to the GitHub
  Package registry, ensuring a smooth version management experience. 
   
  To run the script:
    ```bash
    ./publish-sdk.sh 
    ```
  The script will prompt for the new version number, build the JAR file, and upload it to GitHub Packages.