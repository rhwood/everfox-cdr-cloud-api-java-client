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

import java.util.Objects;

/**
 * Configuration for the Instant API client.
 */
public class InstantApiConfig {

    /**
     * Available API regions.
     */
    public enum Region {
        EU_WEST_1("https://eu-west-1.aws.instant.cdr.everfox.com/v1"),
        EU_WEST_2("https://eu-west-2.aws.instant.cdr.everfox.com/v1"),
        US_WEST_2("https://us-west-2.aws.instant.cdr.everfox.com/v1");

        private final String baseUrl;

        Region(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getBaseUrl() {
            return baseUrl;
        }
    }

    private final String apiKey;
    private final String baseUrl;
    private final int connectTimeoutSeconds;
    private final int requestTimeoutSeconds;

    private InstantApiConfig(Builder builder) {
        this.apiKey = Objects.requireNonNull(builder.apiKey, "apiKey cannot be null");
        this.baseUrl = Objects.requireNonNull(builder.baseUrl, "baseUrl cannot be null");
        this.connectTimeoutSeconds = builder.connectTimeoutSeconds;
        this.requestTimeoutSeconds = builder.requestTimeoutSeconds;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public int getConnectTimeoutSeconds() {
        return connectTimeoutSeconds;
    }

    public int getRequestTimeoutSeconds() {
        return requestTimeoutSeconds;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for InstantApiConfig.
     */
    public static class Builder {
        private String apiKey;
        private String baseUrl;
        private int connectTimeoutSeconds = 10;
        private int requestTimeoutSeconds = 60;

        /**
         * Sets the API key for authentication.
         *
         * @param apiKey the API key
         * @return this builder
         */
        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        /**
         * Sets the base URL for the API.
         *
         * @param baseUrl the base URL
         * @return this builder
         */
        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        /**
         * Sets the base URL using a predefined region.
         *
         * @param region the region
         * @return this builder
         */
        public Builder region(Region region) {
            this.baseUrl = region.getBaseUrl();
            return this;
        }

        /**
         * Sets the connection timeout in seconds.
         *
         * @param seconds the timeout in seconds
         * @return this builder
         */
        public Builder connectTimeoutSeconds(int seconds) {
            this.connectTimeoutSeconds = seconds;
            return this;
        }

        /**
         * Sets the request timeout in seconds.
         *
         * @param seconds the timeout in seconds
         * @return this builder
         */
        public Builder requestTimeoutSeconds(int seconds) {
            this.requestTimeoutSeconds = seconds;
            return this;
        }

        /**
         * Builds the configuration.
         *
         * @return the configuration
         */
        public InstantApiConfig build() {
            return new InstantApiConfig(this);
        }
    }
}
