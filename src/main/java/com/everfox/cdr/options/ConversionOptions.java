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
package com.everfox.cdr.options;

import com.everfox.cdr.MediaType;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Format conversion options.
 *
 * <p>Format conversion options specify the original format of a file to convert from, which conversion tools may be
 * used, and which media type conversions are allowed. See https://cdr.everfox.com/documentation/format-conversion for
 * details. Create them with {@link #builder()}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class ConversionOptions {
    /** The media type of the original file. */
    private final String fromMediaType;
    /** The conversion methods to enable, in order of priority. */
    private final Set<String> enabledMethods;
    /** The conversion methods to disable. */
    private final Set<String> disabledMethods;
    /** The media type conversions to enable. */
    private final List<MediaTypeConversion> enabledMediaTypes;
    /** The media type conversions to disable. */
    private final List<MediaTypeConversion> disabledMediaTypes;

    /**
     * Create a set of format conversion options.
     *
     * @param builder the builder holding the options
     */
    // private to force use of the builder
    private ConversionOptions(Builder builder) {
        this.fromMediaType = builder.fromMediaType;
        this.enabledMethods = new LinkedHashSet<>(builder.enabledMethods);
        this.disabledMethods = new LinkedHashSet<>(builder.disabledMethods);
        this.enabledMediaTypes = List.copyOf(builder.enabledMediaTypes);
        this.disabledMediaTypes = List.copyOf(builder.disabledMediaTypes);
    }

    /**
     * Returns the original file format as a map for JSON serialization.
     *
     * @return a map with "mime-type" key and the original media type, or null if no media type is specified
     */
    public Map<String, String> getFrom() {
        return fromMediaType != null ? Map.of("mime-type", fromMediaType) : null;
    }

    /**
     * Returns the enabled and disabled conversion methods as a map for JSON serialization.
     *
     * @return a map with "enabled" and/or "disabled" keys, or null if no methods are specified
     */
    public Map<String, Set<String>> getMethods() {
        return enabledAndDisabled(
                Collections.unmodifiableSet(enabledMethods), Collections.unmodifiableSet(disabledMethods));
    }

    /**
     * Returns the enabled and disabled media type conversions as a map for JSON serialization.
     *
     * @return a map with "enabled" and/or "disabled" keys, or null if no media type conversions are specified
     */
    @JsonProperty("mime_types")
    public Map<String, List<MediaTypeConversion>> getMediaTypes() {
        return enabledAndDisabled(enabledMediaTypes, disabledMediaTypes);
    }

    /**
     * Combines enabled and disabled collections into a map, omitting empty collections.
     *
     * @param <T> the collection type
     * @param enabled the enabled collection
     * @param disabled the disabled collection
     * @return a map with "enabled" and/or "disabled" keys, or null if both collections are empty
     */
    private static <T extends Collection<?>> Map<String, T> enabledAndDisabled(T enabled, T disabled) {
        Map<String, T> map = new LinkedHashMap<>();
        if (!enabled.isEmpty()) {
            map.put("enabled", enabled);
        }
        if (!disabled.isEmpty()) {
            map.put("disabled", disabled);
        }
        return map.isEmpty() ? null : map;
    }

    /**
     * Creates a new instance of the Builder.
     *
     * @return a new instance of the Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /** Builder for creating {@link ConversionOptions} instances. */
    public static final class Builder {
        /** The media type of the original file. */
        private String fromMediaType;
        /** The conversion methods to enable. */
        private final Set<String> enabledMethods = new LinkedHashSet<>();
        /** The conversion methods to disable. */
        private final Set<String> disabledMethods = new LinkedHashSet<>();
        /** The media type conversions to enable. */
        private final List<MediaTypeConversion> enabledMediaTypes = new ArrayList<>();
        /** The media type conversions to disable. */
        private final List<MediaTypeConversion> disabledMediaTypes = new ArrayList<>();

        /** Creates a new instance of the Builder. */
        public Builder() {}

        /**
         * Sets the media type of the original file to convert from.
         *
         * @param mediaType the media type of the original file
         * @return the builder instance
         */
        public Builder from(MediaType mediaType) {
            return from(mediaType.getMediaType());
        }

        /**
         * Sets the media type of the original file to convert from.
         *
         * @param mediaType the media type of the original file
         * @return the builder instance
         */
        public Builder from(String mediaType) {
            this.fromMediaType = mediaType;
            return this;
        }

        /**
         * Adds conversion methods to enable, in order of priority.
         *
         * @param methods the conversion methods to enable
         * @return the builder instance
         */
        public Builder enableMethods(ConversionMethod... methods) {
            Arrays.stream(methods).map(ConversionMethod::getMethod).forEach(enabledMethods::add);
            return this;
        }

        /**
         * Adds conversion methods to disable.
         *
         * @param methods the conversion methods to disable
         * @return the builder instance
         */
        public Builder disableMethods(ConversionMethod... methods) {
            Arrays.stream(methods).map(ConversionMethod::getMethod).forEach(disabledMethods::add);
            return this;
        }

        /**
         * Adds a media type conversion to enable.
         *
         * @param from the media type to convert from
         * @param to the media type to convert to
         * @return the builder instance
         */
        public Builder enableMediaTypeConversion(MediaType from, MediaType to) {
            return enableMediaTypeConversion(from.getMediaType(), to.getMediaType());
        }

        /**
         * Adds a media type conversion to enable.
         *
         * @param from the media type to convert from
         * @param to the media type to convert to
         * @return the builder instance
         */
        public Builder enableMediaTypeConversion(String from, String to) {
            enabledMediaTypes.add(new MediaTypeConversion(from, to));
            return this;
        }

        /**
         * Adds a media type conversion to disable.
         *
         * @param from the media type to convert from
         * @param to the media type to convert to
         * @return the builder instance
         */
        public Builder disableMediaTypeConversion(MediaType from, MediaType to) {
            return disableMediaTypeConversion(from.getMediaType(), to.getMediaType());
        }

        /**
         * Adds a media type conversion to disable.
         *
         * @param from the media type to convert from
         * @param to the media type to convert to
         * @return the builder instance
         */
        public Builder disableMediaTypeConversion(String from, String to) {
            disabledMediaTypes.add(new MediaTypeConversion(from, to));
            return this;
        }

        /**
         * Builds the {@link ConversionOptions} instance with the specified configurations.
         *
         * @return a new instance of ConversionOptions
         * @throws IllegalArgumentException if any conversion method is both enabled and disabled
         */
        public ConversionOptions build() {
            ProcessingOptions.requireDisjoint(
                    enabledMethods, disabledMethods, "Conversion methods cannot be both enabled and disabled");
            return new ConversionOptions(this);
        }
    }
}
