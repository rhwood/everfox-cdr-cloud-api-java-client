/*
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
package com.everfox.cdr.async;

import com.everfox.cdr.Api;
import com.everfox.cdr.Region;
import java.net.URI;
import java.util.Objects;

/** Configuration for the Async API client. */
public class AsyncApiConfig {

    /** Default connection timeout in seconds. */
    public static final int DEFAULT_CONNECT_TIMEOUT_SECONDS = 10;
    /** Default timeout in seconds for each HTTP request, including uploads and downloads. */
    public static final int DEFAULT_REQUEST_TIMEOUT_SECONDS = 60;
    /** Default interval in seconds between status requests while waiting for processing to complete. */
    public static final int DEFAULT_POLL_INTERVAL_SECONDS = 1;
    /**
     * Default time in seconds to wait for processing to complete. The API processes a file for up to five minutes, so
     * this allows for queueing as well.
     */
    public static final int DEFAULT_PROCESSING_TIMEOUT_SECONDS = 600;

    /** The API key for authenticating with the Async API. */
    private final String apiKey;
    /** The base URL of the Async API. */
    private final URI baseUrl;
    /** The connection timeout in seconds. */
    private final int connectTimeoutSeconds;
    /** The request timeout in seconds. */
    private final int requestTimeoutSeconds;
    /** The interval in seconds between status requests. */
    private final int pollIntervalSeconds;
    /** The time in seconds to wait for processing to complete. */
    private final int processingTimeoutSeconds;

    /**
     * Creates a new configuration.
     *
     * @param apiKey the API key
     * @param baseUrl the base URL of the Async API
     * @param connectTimeoutSeconds the connection timeout in seconds
     * @param requestTimeoutSeconds the request timeout in seconds
     * @param pollIntervalSeconds the interval in seconds between status requests
     * @param processingTimeoutSeconds the time in seconds to wait for processing to complete
     */
    // private to force use of the static factory method
    private AsyncApiConfig(
            String apiKey,
            URI baseUrl,
            int connectTimeoutSeconds,
            int requestTimeoutSeconds,
            int pollIntervalSeconds,
            int processingTimeoutSeconds) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.connectTimeoutSeconds = connectTimeoutSeconds;
        this.requestTimeoutSeconds = requestTimeoutSeconds;
        this.pollIntervalSeconds = pollIntervalSeconds;
        this.processingTimeoutSeconds = processingTimeoutSeconds;
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
     * Returns the base URL of the Async API.
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
     * Returns the timeout in seconds for each HTTP request. This applies to uploads and downloads, so increase it for
     * large files.
     *
     * @return the request timeout
     */
    public int getRequestTimeoutSeconds() {
        return requestTimeoutSeconds;
    }

    /**
     * Returns the interval in seconds between status requests while waiting for processing to complete.
     *
     * @return the poll interval
     */
    public int getPollIntervalSeconds() {
        return pollIntervalSeconds;
    }

    /**
     * Returns the time in seconds to wait for processing to complete.
     *
     * @return the processing timeout
     */
    public int getProcessingTimeoutSeconds() {
        return processingTimeoutSeconds;
    }

    /**
     * Creates a new configuration with the specified API key and region.
     *
     * @param apiKey the API key
     * @param region the region for the Async API
     * @return a new configuration
     */
    public static AsyncApiConfig create(String apiKey, Region region) {
        return AsyncApiConfig.create(apiKey, region.getBaseUrl(Api.ASYNC));
    }

    /**
     * Creates a new configuration with the specified API key and base URL.
     *
     * @param apiKey the API key
     * @param baseUrl the base URL of the Async API
     * @return a new configuration
     */
    public static AsyncApiConfig create(String apiKey, URI baseUrl) {
        return AsyncApiConfig.create(apiKey, baseUrl, DEFAULT_CONNECT_TIMEOUT_SECONDS, DEFAULT_REQUEST_TIMEOUT_SECONDS);
    }

    /**
     * Creates a new configuration with the specified API key, region, and HTTP timeouts.
     *
     * @param apiKey the API key
     * @param region the region for the Async API
     * @param connectTimeoutSeconds the connection timeout in seconds
     * @param requestTimeoutSeconds the request timeout in seconds
     * @return a new configuration
     */
    public static AsyncApiConfig create(
            String apiKey, Region region, int connectTimeoutSeconds, int requestTimeoutSeconds) {
        return AsyncApiConfig.create(
                apiKey, region.getBaseUrl(Api.ASYNC), connectTimeoutSeconds, requestTimeoutSeconds);
    }

    /**
     * Creates a new configuration with the specified API key, base URL, and HTTP timeouts.
     *
     * @param apiKey the API key
     * @param baseUrl the base URL of the Async API
     * @param connectTimeoutSeconds the connection timeout in seconds
     * @param requestTimeoutSeconds the request timeout in seconds
     * @return a new configuration
     */
    public static AsyncApiConfig create(
            String apiKey, URI baseUrl, int connectTimeoutSeconds, int requestTimeoutSeconds) {
        return AsyncApiConfig.create(
                apiKey,
                baseUrl,
                connectTimeoutSeconds,
                requestTimeoutSeconds,
                DEFAULT_POLL_INTERVAL_SECONDS,
                DEFAULT_PROCESSING_TIMEOUT_SECONDS);
    }

    /**
     * Creates a new configuration with the specified API key, region, HTTP timeouts, and polling behavior.
     *
     * @param apiKey the API key
     * @param region the region for the Async API
     * @param connectTimeoutSeconds the connection timeout in seconds
     * @param requestTimeoutSeconds the request timeout in seconds
     * @param pollIntervalSeconds the interval in seconds between status requests
     * @param processingTimeoutSeconds the time in seconds to wait for processing to complete
     * @return a new configuration
     * @throws IllegalArgumentException if the poll interval or processing timeout is not positive
     */
    public static AsyncApiConfig create(
            String apiKey,
            Region region,
            int connectTimeoutSeconds,
            int requestTimeoutSeconds,
            int pollIntervalSeconds,
            int processingTimeoutSeconds) {
        return AsyncApiConfig.create(
                apiKey,
                region.getBaseUrl(Api.ASYNC),
                connectTimeoutSeconds,
                requestTimeoutSeconds,
                pollIntervalSeconds,
                processingTimeoutSeconds);
    }

    /**
     * Creates a new configuration with the specified API key, base URL, HTTP timeouts, and polling behavior.
     *
     * @param apiKey the API key
     * @param baseUrl the base URL of the Async API
     * @param connectTimeoutSeconds the connection timeout in seconds
     * @param requestTimeoutSeconds the request timeout in seconds
     * @param pollIntervalSeconds the interval in seconds between status requests
     * @param processingTimeoutSeconds the time in seconds to wait for processing to complete
     * @return a new configuration
     * @throws IllegalArgumentException if the poll interval or processing timeout is not positive
     */
    public static AsyncApiConfig create(
            String apiKey,
            URI baseUrl,
            int connectTimeoutSeconds,
            int requestTimeoutSeconds,
            int pollIntervalSeconds,
            int processingTimeoutSeconds) {
        Objects.requireNonNull(apiKey, "apiKey cannot be null");
        Objects.requireNonNull(baseUrl, "baseUrl cannot be null");
        if (pollIntervalSeconds <= 0) {
            throw new IllegalArgumentException("pollIntervalSeconds must be positive");
        }
        if (processingTimeoutSeconds <= 0) {
            throw new IllegalArgumentException("processingTimeoutSeconds must be positive");
        }
        return new AsyncApiConfig(
                apiKey,
                baseUrl,
                connectTimeoutSeconds,
                requestTimeoutSeconds,
                pollIntervalSeconds,
                processingTimeoutSeconds);
    }
}
