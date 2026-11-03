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
import com.everfox.cdr.CdrApiConfig;
import com.everfox.cdr.Region;
import java.net.URI;
import java.time.Duration;
import java.util.Objects;

/**
 * Configuration for the Async API client: the {@linkplain CdrApiConfig shared settings} plus how to poll for
 * completion.
 */
public final class AsyncApiConfig extends CdrApiConfig {

    /** Default interval between status requests while waiting for processing to complete. */
    public static final Duration DEFAULT_POLL_INTERVAL = Duration.ofSeconds(1);
    /**
     * Default time to wait for processing to complete. The API processes a file for up to five minutes, so this allows
     * for queueing as well.
     */
    public static final Duration DEFAULT_PROCESSING_TIMEOUT = Duration.ofMinutes(10);

    /** The interval between status requests. */
    private final Duration pollInterval;
    /** The time to wait for processing to complete. */
    private final Duration processingTimeout;

    /**
     * Creates the configuration held by a builder.
     *
     * @param builder the builder holding the configuration
     */
    // private to force use of the factory methods
    private AsyncApiConfig(Builder builder) {
        super(builder);
        this.pollInterval = builder.pollInterval;
        this.processingTimeout = builder.processingTimeout;
    }

    /**
     * Returns the interval between status requests while waiting for processing to complete.
     *
     * @return the poll interval
     */
    public Duration getPollInterval() {
        return pollInterval;
    }

    /**
     * Returns the time to wait for processing to complete.
     *
     * @return the processing timeout
     */
    public Duration getProcessingTimeout() {
        return processingTimeout;
    }

    /**
     * Creates a configuration with the default timeouts and polling.
     *
     * @param apiKey the API key
     * @param region the region, whose Async API endpoint is used
     * @return a new configuration
     */
    public static AsyncApiConfig create(String apiKey, Region region) {
        return builder(apiKey, region).build();
    }

    /**
     * Creates a builder for a region's Async API endpoint.
     *
     * @param apiKey the API key
     * @param region the region
     * @return a new builder
     */
    public static Builder builder(String apiKey, Region region) {
        Objects.requireNonNull(region, "region cannot be null");
        return builder(apiKey, region.getBaseUrl(Api.ASYNC));
    }

    /**
     * Creates a builder for a custom endpoint.
     *
     * @param apiKey the API key
     * @param baseUrl the base URL of the Async API
     * @return a new builder
     */
    public static Builder builder(String apiKey, URI baseUrl) {
        Objects.requireNonNull(apiKey, "apiKey cannot be null");
        Objects.requireNonNull(baseUrl, "baseUrl cannot be null");
        return new Builder(apiKey, baseUrl);
    }

    /** Builder for creating {@link AsyncApiConfig} instances. */
    public static final class Builder extends CdrApiConfig.Builder<Builder> {

        /** The interval between status requests. */
        private Duration pollInterval = DEFAULT_POLL_INTERVAL;
        /** The time to wait for processing to complete. */
        private Duration processingTimeout = DEFAULT_PROCESSING_TIMEOUT;

        /**
         * Creates a new builder.
         *
         * @param apiKey the API key
         * @param baseUrl the base URL of the Async API
         */
        private Builder(String apiKey, URI baseUrl) {
            super(apiKey, baseUrl);
        }

        @Override
        protected Builder self() {
            return this;
        }

        /**
         * Sets the interval between status requests. The default is {@link AsyncApiConfig#DEFAULT_POLL_INTERVAL}.
         *
         * @param pollInterval the poll interval
         * @return the builder instance
         * @throws IllegalArgumentException if the interval is not positive
         */
        public Builder pollInterval(Duration pollInterval) {
            this.pollInterval = requirePositive(pollInterval, "pollInterval");
            return this;
        }

        /**
         * Sets the time to wait for processing to complete. The default is
         * {@link AsyncApiConfig#DEFAULT_PROCESSING_TIMEOUT}.
         *
         * @param processingTimeout the processing timeout
         * @return the builder instance
         * @throws IllegalArgumentException if the timeout is not positive
         */
        public Builder processingTimeout(Duration processingTimeout) {
            this.processingTimeout = requirePositive(processingTimeout, "processingTimeout");
            return this;
        }

        /**
         * Builds the configuration.
         *
         * @return a new configuration
         */
        public AsyncApiConfig build() {
            return new AsyncApiConfig(this);
        }
    }
}
