package com.demo.sdk.enums;

/**
 * Defines the available modes of operation for the Weather SDK.
 * <p>
 * The SDK can operate in one of the following modes:
 * <ul>
 *     <li>{@link #ON_DEMAND} - Fetches weather data only when explicitly requested.</li>
 *     <li>{@link #POLLING} - Automatically fetches weather data at regular intervals for cached cities.</li>
 * </ul>
 * </p>
 */
public enum Mode {
    /** Fetches weather data only when explicitly requested. */
    ON_DEMAND,

    /** Periodically fetches weather data for stored cities at scheduled intervals. */
    POLLING;
}