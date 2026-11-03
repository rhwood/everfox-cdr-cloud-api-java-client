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
package com.everfox.cdr;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;

/**
 * Configuration shared by every Everfox CDR Cloud API client: API key, base URL, and HTTP timeouts.
 *
 * <p>Each API's config class extends this class with any settings of its own, and is created with its
 * {@code builder(...)} or {@code create(...)} methods.
 */
public abstract class CdrApiConfig {

    /** Default connection timeout. */
    public static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(10);
    /** Default timeout for each HTTP request. */
    public static final Duration DEFAULT_REQUEST_TIMEOUT = Duration.ofSeconds(60);

    /** The API key. */
    private final String apiKey;
    /** The base URL of the API. */
    private final URI baseUrl;
    /** The connection timeout. */
    private final Duration connectTimeout;
    /** The timeout for each HTTP request. */
    private final Duration requestTimeout;

    /**
     * Creates the configuration held by a builder.
     *
     * @param builder the builder holding the configuration
     */
    protected CdrApiConfig(Builder<?> builder) {
        this.apiKey = builder.apiKey;
        this.baseUrl = builder.baseUrl;
        this.connectTimeout = builder.connectTimeout;
        this.requestTimeout = builder.requestTimeout;
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
     * Returns the base URL of the API.
     *
     * @return the base URL
     */
    public URI getBaseUrl() {
        return baseUrl;
    }

    /**
     * Returns the connection timeout.
     *
     * @return the connection timeout
     */
    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    /**
     * Returns the timeout for each HTTP request. Requests that upload or download files are included, so increase it
     * for large files.
     *
     * @return the request timeout
     */
    public Duration getRequestTimeout() {
        return requestTimeout;
    }

    /**
     * Builder for the shared configuration. Each API's config builder extends it, so the methods here return that
     * builder and calls can be chained in any order.
     *
     * @param <B> the type of the concrete builder
     */
    public abstract static class Builder<B extends Builder<B>> {

        /** The API key. */
        private final String apiKey;
        /** The base URL of the API. */
        private final URI baseUrl;
        /** The connection timeout. */
        private Duration connectTimeout = DEFAULT_CONNECT_TIMEOUT;
        /** The timeout for each HTTP request. */
        private Duration requestTimeout = DEFAULT_REQUEST_TIMEOUT;

        /**
         * Creates a new builder. Subclass factory methods must check that the arguments are not null.
         *
         * @param apiKey the API key
         * @param baseUrl the base URL of the API
         */
        protected Builder(String apiKey, URI baseUrl) {
            this.apiKey = apiKey;
            this.baseUrl = baseUrl;
        }

        /**
         * Returns this builder as its concrete type.
         *
         * @return this builder
         */
        protected abstract B self();

        /**
         * Sets the connection timeout. The default is {@link CdrApiConfig#DEFAULT_CONNECT_TIMEOUT}.
         *
         * @param connectTimeout the connection timeout
         * @return the builder instance
         * @throws IllegalArgumentException if the timeout is not positive
         */
        public B connectTimeout(Duration connectTimeout) {
            this.connectTimeout = requirePositive(connectTimeout, "connectTimeout");
            return self();
        }

        /**
         * Sets the timeout for each HTTP request. The default is {@link CdrApiConfig#DEFAULT_REQUEST_TIMEOUT}.
         *
         * @param requestTimeout the request timeout
         * @return the builder instance
         * @throws IllegalArgumentException if the timeout is not positive
         */
        public B requestTimeout(Duration requestTimeout) {
            this.requestTimeout = requirePositive(requestTimeout, "requestTimeout");
            return self();
        }

        /**
         * Checks that a duration is positive.
         *
         * @param duration the duration
         * @param name the name of the setting, for the exception message
         * @return the duration
         * @throws IllegalArgumentException if the duration is not positive
         */
        protected static Duration requirePositive(Duration duration, String name) {
            Objects.requireNonNull(duration, name + " cannot be null");
            if (duration.isNegative() || duration.isZero()) {
                throw new IllegalArgumentException(name + " must be positive");
            }
            return duration;
        }
    }
}
