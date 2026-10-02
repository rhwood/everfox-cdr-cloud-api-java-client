/**
 * Copyright 2026 Everfox
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.everfox.cdr.instant;

import java.net.URI;
import java.util.Objects;

import com.everfox.cdr.Region;

/**
 * Configuration for the Instant API client.
 */
public class InstantApiConfig {

    /**
     * Default connection timeout in seconds.
     */
    public static final int DEFAULT_CONNECT_TIMEOUT_SECONDS = 10;
    /**
     * Default request timeout in seconds.
     */
    public static final int DEFAULT_REQUEST_TIMEOUT_SECONDS = 60;

    private final String apiKey;
    private final URI baseUrl;
    private final int connectTimeoutSeconds;
    private final int requestTimeoutSeconds;

    /**
     * Creates a new configuration with the specified API key, base URL, and timeouts.
     *
     * @param apiKey the API key
     * @param baseUrl the base URL of the Instant API
     * @param connectTimeoutSeconds the connection timeout in seconds
     * @param requestTimeoutSeconds the request timeout in seconds
     */
    // private to force use of the static factory method
    private InstantApiConfig(String apiKey, URI baseUrl, int connectTimeoutSeconds, int requestTimeoutSeconds) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.connectTimeoutSeconds = connectTimeoutSeconds;
        this.requestTimeoutSeconds = requestTimeoutSeconds;
    }

    /**
     * Returns the API key.
     *
     * @return the API key
     */
    public String getApiKey() {
        return apiKey;
    }

    /**
     * Returns the base URL of the Instant API.
     *
     * @return the base URL
     */
    public URI getBaseUrl() {
        return baseUrl;
    }

    /**
     * Returns the connection timeout in seconds.
     *
     * @return the connection timeout
     */
    public int getConnectTimeoutSeconds() {
        return connectTimeoutSeconds;
    }

    /**
     * Returns the request timeout in seconds.
     *
     * @return the request timeout
     */
    public int getRequestTimeoutSeconds() {
        return requestTimeoutSeconds;
    }

    /**
     * Creates a new configuration with the specified API key and region.
     *
     * @param apiKey the API key
     * @param region the region for the Instant API
     * @return a new configuration
     */
    public static InstantApiConfig create(String apiKey, Region region) {
        return InstantApiConfig.create(apiKey, region.getBaseUrl());
    }

    /**
     * Creates a new configuration with the specified API key and base URL.
     *
     * @param apiKey the API key
     * @param baseUrl the base URL of the Instant API
     * @return a new configuration
     */
    public static InstantApiConfig create(String apiKey, URI baseUrl) {
        return InstantApiConfig.create(apiKey, baseUrl, DEFAULT_CONNECT_TIMEOUT_SECONDS, DEFAULT_REQUEST_TIMEOUT_SECONDS);
    }

    /**
     * Creates a new configuration with the specified API key, base URL, and timeouts.
     *
     * @param apiKey the API key
     * @param region the region for the Instant API
     * @param connectTimeoutSeconds the connection timeout in seconds
     * @param requestTimeoutSeconds the request timeout in seconds
     * @return a new configuration
     */
    public static InstantApiConfig create(String apiKey, Region region, int connectTimeoutSeconds, int requestTimeoutSeconds) {
        return InstantApiConfig.create(apiKey, region.getBaseUrl(), connectTimeoutSeconds, requestTimeoutSeconds);
    }

    /**
     * Creates a new configuration with the specified API key, base URL, and timeouts.
     *
     * @param apiKey the API key
     * @param baseUrl the base URL of the Instant API
     * @param connectTimeoutSeconds the connection timeout in seconds
     * @param requestTimeoutSeconds the request timeout in seconds
     * @return a new configuration
     */
    public static InstantApiConfig create(String apiKey, URI baseUrl, int connectTimeoutSeconds, int requestTimeoutSeconds) {
        Objects.requireNonNull(apiKey, "apiKey cannot be null");
        Objects.requireNonNull(baseUrl, "baseUrl cannot be null");
        return new InstantApiConfig(apiKey, baseUrl, connectTimeoutSeconds, requestTimeoutSeconds);
    }
}
