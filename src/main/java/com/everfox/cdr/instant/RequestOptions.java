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

import com.everfox.cdr.options.ProcessingOptions;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Options for customizing file processing behavior, sent as JSON in the {@code X-Options} header. The Instant API
 * accepts only the {@linkplain ProcessingOptions shared options}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class RequestOptions extends ProcessingOptions {

    /**
     * Creates the options held by a builder.
     *
     * @param builder the builder holding the options
     */
    // private to force use of the builder
    private RequestOptions(Builder builder) {
        super(builder);
    }

    /**
     * Creates a new instance of the Builder.
     *
     * @return a new instance of the Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /** Builder for creating {@link RequestOptions} instances. */
    public static final class Builder extends ProcessingOptions.Builder<Builder> {

        /** Creates a new instance of the Builder. */
        public Builder() {}

        @Override
        protected Builder self() {
            return this;
        }

        /**
         * Builds the {@link RequestOptions} instance with the specified configurations.
         *
         * @return a new instance of RequestOptions
         * @throws IllegalArgumentException if any risk is both allowed and denied
         */
        public RequestOptions build() {
            validate();
            return new RequestOptions(this);
        }
    }
}
