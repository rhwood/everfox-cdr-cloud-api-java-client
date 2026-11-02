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
package com.everfox.cdr.instant;

import com.everfox.cdr.Api;
import com.everfox.cdr.CdrApiConfig;
import com.everfox.cdr.Region;
import java.net.URI;
import java.util.Objects;

/** Configuration for the Instant API client. The Instant API has only the {@linkplain CdrApiConfig shared settings}. */
public final class InstantApiConfig extends CdrApiConfig {

    /**
     * Creates the configuration held by a builder.
     *
     * @param builder the builder holding the configuration
     */
    // private to force use of the factory methods
    private InstantApiConfig(Builder builder) {
        super(builder);
    }

    /**
     * Creates a configuration with the default timeouts.
     *
     * @param apiKey the API key
     * @param region the region, whose Instant API endpoint is used
     * @return a new configuration
     */
    public static InstantApiConfig create(String apiKey, Region region) {
        return builder(apiKey, region).build();
    }

    /**
     * Creates a builder for a region's Instant API endpoint.
     *
     * @param apiKey the API key
     * @param region the region
     * @return a new builder
     */
    public static Builder builder(String apiKey, Region region) {
        Objects.requireNonNull(region, "region cannot be null");
        return builder(apiKey, region.getBaseUrl(Api.INSTANT));
    }

    /**
     * Creates a builder for a custom endpoint.
     *
     * @param apiKey the API key
     * @param baseUrl the base URL of the Instant API
     * @return a new builder
     */
    public static Builder builder(String apiKey, URI baseUrl) {
        Objects.requireNonNull(apiKey, "apiKey cannot be null");
        Objects.requireNonNull(baseUrl, "baseUrl cannot be null");
        return new Builder(apiKey, baseUrl);
    }

    /** Builder for creating {@link InstantApiConfig} instances. */
    public static final class Builder extends CdrApiConfig.Builder<Builder> {

        /**
         * Creates a new builder.
         *
         * @param apiKey the API key
         * @param baseUrl the base URL of the Instant API
         */
        private Builder(String apiKey, URI baseUrl) {
            super(apiKey, baseUrl);
        }

        @Override
        protected Builder self() {
            return this;
        }

        /**
         * Builds the configuration.
         *
         * @return a new configuration
         */
        public InstantApiConfig build() {
            return new InstantApiConfig(this);
        }
    }
}
