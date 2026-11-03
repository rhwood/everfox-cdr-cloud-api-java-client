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

import com.everfox.cdr.Risk;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * Processing options accepted by every Everfox CDR Cloud API: report format, format conversion, image quality,
 * redactions, and risks.
 *
 * <p>Each API's {@code RequestOptions} extends this class with any options of its own, and is created with its
 * {@code builder()} method.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"report", "conversion", "images", "redactions", "risks"})
public abstract class ProcessingOptions {

    /** ObjectMapper for JSON serialization. */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Report format options. */
    private final ReportOptions report;
    /** Format conversion options. */
    private final ConversionOptions conversion;
    /** Image processing options. */
    private final ImagesOptions images;
    /** Redaction options. */
    private final RedactionOptions redactions;
    /** Allowed risks, in the order given. */
    private final Set<String> allowedRisks;
    /** Denied risks, in the order given. */
    private final Set<String> deniedRisks;

    /**
     * Creates the options held by a builder.
     *
     * @param builder the builder holding the options
     */
    protected ProcessingOptions(Builder<?> builder) {
        this.report = builder.reportFormat != null ? new ReportOptions(builder.reportFormat) : null;
        this.conversion = builder.conversionOptions;
        this.images = builder.imagesOptions;
        this.redactions = builder.redactionOptions;
        this.allowedRisks = new LinkedHashSet<>(builder.allowedRisks);
        this.deniedRisks = new LinkedHashSet<>(builder.deniedRisks);
    }

    /**
     * Returns the report format options.
     *
     * @return report format
     */
    public ReportOptions getReport() {
        return report;
    }

    /**
     * Returns the format conversion options.
     *
     * @return the format conversion options
     */
    public ConversionOptions getConversion() {
        return conversion;
    }

    /**
     * Returns the image options.
     *
     * @return the image options
     */
    public ImagesOptions getImages() {
        return images;
    }

    /**
     * Returns the redaction options.
     *
     * @return redaction options
     */
    public RedactionOptions getRedactions() {
        return redactions;
    }

    /**
     * Returns the allowed and denied risks as a map.
     *
     * @return a map with "allow" and/or "deny" keys, or null if no risks are specified
     */
    public Map<String, Set<String>> getRisks() {
        Map<String, Set<String>> risks = new LinkedHashMap<>();
        if (!allowedRisks.isEmpty()) {
            risks.put("allow", Collections.unmodifiableSet(allowedRisks));
        }
        if (!deniedRisks.isEmpty()) {
            risks.put("deny", Collections.unmodifiableSet(deniedRisks));
        }
        return risks.isEmpty() ? null : risks;
    }

    /**
     * Converts these options to JSON.
     *
     * @return JSON representation
     */
    public String toJson() {
        try {
            return MAPPER.writeValueAsString(this);
        } catch (JacksonException e) {
            throw new IllegalStateException("Failed to serialize options to JSON", e);
        }
    }

    /**
     * Ensures that two sets have no elements in common.
     *
     * @param first the first set
     * @param second the second set
     * @param message the exception message, to which the common elements are appended
     * @throws IllegalArgumentException if the sets have any elements in common
     */
    static void requireDisjoint(Set<String> first, Set<String> second, String message) {
        Set<String> common = new LinkedHashSet<>(first);
        common.retainAll(second);
        if (!common.isEmpty()) {
            throw new IllegalArgumentException(message + ": " + common);
        }
    }

    /**
     * Builder for the shared options. Each API's {@code RequestOptions.Builder} extends it, so the methods here return
     * that builder and calls can be chained in any order.
     *
     * @param <B> the type of the concrete builder
     */
    public abstract static class Builder<B extends Builder<B>> {

        /** Allowed risks, in the order given. */
        private final Set<String> allowedRisks = new LinkedHashSet<>();
        /** Denied risks, in the order given. */
        private final Set<String> deniedRisks = new LinkedHashSet<>();
        /** Report format. */
        private ReportFormat reportFormat;
        /** Format conversion options. */
        private ConversionOptions conversionOptions;
        /** Image options. */
        private ImagesOptions imagesOptions;
        /** Redaction options. */
        private RedactionOptions redactionOptions;

        /** Creates a new builder. */
        protected Builder() {}

        /**
         * Returns this builder as its concrete type.
         *
         * @return this builder
         */
        protected abstract B self();

        /**
         * Sets the report format.
         *
         * @param format the report format
         * @return the builder instance
         */
        public B reportFormat(ReportFormat format) {
            this.reportFormat = format;
            return self();
        }

        /**
         * Sets the format conversion options.
         *
         * @param conversionOptions the format conversion options
         * @return the builder instance
         */
        public B conversionOptions(ConversionOptions conversionOptions) {
            this.conversionOptions = conversionOptions;
            return self();
        }

        /**
         * Sets the image options.
         *
         * @param imagesOptions the image options
         * @return the builder instance
         */
        public B imagesOptions(ImagesOptions imagesOptions) {
            this.imagesOptions = imagesOptions;
            return self();
        }

        /**
         * Sets the redaction options.
         *
         * @param redactionOptions the redaction options
         * @return the builder instance
         */
        public B redactionOptions(RedactionOptions redactionOptions) {
            this.redactionOptions = redactionOptions;
            return self();
        }

        /**
         * Adds risks to allow. Repeated calls add to the risks already allowed.
         *
         * @param allowedRisks the risks to allow
         * @return the builder instance
         */
        public B allowRisks(Risk... allowedRisks) {
            Arrays.stream(allowedRisks).map(Risk::getRisk).forEach(this.allowedRisks::add);
            return self();
        }

        /**
         * Adds risks to deny. Repeated calls add to the risks already denied.
         *
         * @param deniedRisks the risks to deny
         * @return the builder instance
         */
        public B denyRisks(Risk... deniedRisks) {
            Arrays.stream(deniedRisks).map(Risk::getRisk).forEach(this.deniedRisks::add);
            return self();
        }

        /**
         * Validates the shared options. Call this from {@code build()} before creating the options.
         *
         * @throws IllegalArgumentException if any risk is both allowed and denied
         */
        protected void validate() {
            requireDisjoint(allowedRisks, deniedRisks, "Risks cannot be both allowed and denied");
        }
    }
}
