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

    /**
     * Available API regions.
     */
    public enum Region {
        /**
         * EU West (Ireland) region.
         */
        EU_WEST_1("https://eu-west-1.aws.instant.cdr.everfox.com/v1"),
        /**
         * EU West (London) region.
         */
        EU_WEST_2("https://eu-west-2.aws.instant.cdr.everfox.com/v1"),
        /**
         * US West (Oregon) region.
         */
        US_WEST_2("https://us-west-2.aws.instant.cdr.everfox.com/v1");

        private final URI baseUrl;

        Region(String baseUrl) {
            this.baseUrl = URI.create(baseUrl);
        }

        /**
         * Returns the base URL for the region.
         *
         * @return the base URL
         */
        public URI getBaseUrl() {
            return baseUrl;
        }
    }

    private final String apiKey;
    private final URI baseUrl;
    private final int connectTimeoutSeconds;
    private final int requestTimeoutSeconds;

    /**
     * Creates a new configuration with the specified API key and region.
     *
     * @param apiKey the API key
     * @param region the API region
     */
    public InstantApiConfig(String apiKey, Region region) {
        this(apiKey, region.getBaseUrl());
    }

    /**
     * Creates a new configuration with the specified API key, region, and timeouts.
     *
     * @param apiKey the API key
     * @param region the API region
     * @param connectTimeoutSeconds the connection timeout in seconds
     * @param requestTimeoutSeconds the request timeout in seconds
     */
    public InstantApiConfig(String apiKey, Region region, int connectTimeoutSeconds, int requestTimeoutSeconds) {
        this(apiKey, region.getBaseUrl(), connectTimeoutSeconds, requestTimeoutSeconds);
    }

    /**
     * Creates a new configuration with the specified API key and base URL.
     *
     * @param apiKey the API key
     * @param baseUrl the base URL of the Instant API
     */
    public InstantApiConfig(String apiKey, URI baseUrl) {
        this(apiKey, baseUrl, DEFAULT_CONNECT_TIMEOUT_SECONDS, DEFAULT_REQUEST_TIMEOUT_SECONDS);
    }

    /**
     * Creates a new configuration with the specified API key, base URL, and timeouts.
     *
     * @param apiKey the API key
     * @param baseUrl the base URL of the Instant API
     * @param connectTimeoutSeconds the connection timeout in seconds
     * @param requestTimeoutSeconds the request timeout in seconds
     */
    public InstantApiConfig(String apiKey, URI baseUrl, int connectTimeoutSeconds, int requestTimeoutSeconds) {
        this.apiKey = Objects.requireNonNull(apiKey, "apiKey cannot be null");
        this.baseUrl = Objects.requireNonNull(baseUrl, "baseUrl cannot be null");
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

}
