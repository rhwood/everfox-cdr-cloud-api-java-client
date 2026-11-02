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

import com.everfox.cdr.MediaType;
import com.everfox.cdr.Risk;
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
import java.util.Objects;
import java.util.Set;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/** Options for customizing file processing behavior, sent as the {@code options} object of the initiate request. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RequestOptions {

    /** The maximum length of the user data. */
    public static final int MAX_USER_DATA_LENGTH = 16384;

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
    /** Set of allowed risks. */
    private final Set<String> allowedRisks;
    /** Set of denied risks. */
    private final Set<String> deniedRisks;
    /** Quota usage options. */
    private final CostsOptions costs;
    /** Status notification options. */
    private final StatusOptions status;

    /**
     * Creates a new instance of {@link RequestOptions}.
     *
     * @param builder the builder holding the options
     */
    // private to force use of the builder
    private RequestOptions(Builder builder) {
        this.report = builder.reportFormat != null ? new ReportOptions(builder.reportFormat) : null;
        this.conversion = builder.conversionOptions;
        this.images = builder.imagesOptions;
        this.redactions = builder.redactionOptions;
        this.allowedRisks = new LinkedHashSet<>(builder.allowedRisks);
        this.deniedRisks = new LinkedHashSet<>(builder.deniedRisks);
        this.costs = builder.largeFileEnvironment != null ? new CostsOptions(builder.largeFileEnvironment) : null;
        this.status = builder.userData != null || builder.snsTopicArn != null
                ? new StatusOptions(builder.snsTopicArn, builder.snsRoleArn, builder.userData)
                : null;
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
     * Returns the report format options.
     *
     * @return report format
     */
    public ReportOptions getReport() {
        return report;
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
     * Returns the quota usage options.
     *
     * @return the quota usage options
     */
    public CostsOptions getCosts() {
        return costs;
    }

    /**
     * Returns the status notification options.
     *
     * @return the status options
     */
    public StatusOptions getStatus() {
        return status;
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

    /** Report format options. See https://cdr.everfox.com/documentation/report for details. */
    public enum ReportFormat {
        /** Reports on data that has had a notable change. */
        CHANGED("changed"),
        /** The current default behavior is {@link #CHANGED}. */
        DEFAULT("default"),
        /**
         * Reports everything about the data, including information on data that hasn't been transformed. These reports
         * can be big!
         */
        FULL("full"),
        /** A report will not be generated. Use this if you don't need the report. */
        NONE("none");

        /** The string representation of the report format. */
        private final String format;

        /**
         * Creates a new report format with the specified string representation.
         *
         * @param format the string representation of the report format
         */
        ReportFormat(String format) {
            this.format = format;
        }

        /**
         * Returns the string representation of the report format.
         *
         * @return the report format as a string
         */
        public String getFormat() {
            return format;
        }

        @Override
        public String toString() {
            return getFormat();
        }
    }

    /** Format conversion methods (tools). See https://cdr.everfox.com/documentation/format-conversion for details. */
    public enum ConversionMethod {
        /** The default set of conversion tools. Disable this to only use explicitly enabled tools. */
        DEFAULT("default"),
        /** Convert files using LibreOffice. */
        LIBREOFFICE("libreoffice"),
        /** Return the report as the result instead of the transformed file. */
        REPORT("report"),
        /** Add the text extracted from supported documents to the report. */
        TEXT_EXTRACTION("text-extraction");

        /** The string representation of the conversion method. */
        private final String method;

        /**
         * Creates a new conversion method with the specified string representation.
         *
         * @param method the string representation of the conversion method
         */
        ConversionMethod(String method) {
            this.method = method;
        }

        /**
         * Returns the string representation of the conversion method.
         *
         * @return the conversion method as a string
         */
        public String getMethod() {
            return method;
        }

        @Override
        public String toString() {
            return getMethod();
        }
    }

    /** Options for handling images. */
    public static class ImagesOptions {

        /** The set of media types to preserve image quality for. */
        private final Set<String> preservationFormats;

        /**
         * Create a set of image options with the specified preservation formats.
         *
         * @param preservationFormats the media types to preserve
         */
        public ImagesOptions(MediaType... preservationFormats) {
            this(Arrays.stream(preservationFormats).map(MediaType::getMediaType).toArray(String[]::new));
        }

        /**
         * Create a set of image options with the specified preservation formats.
         *
         * @param preservationFormats the media types to preserve
         */
        public ImagesOptions(String... preservationFormats) {
            this.preservationFormats = new LinkedHashSet<>(Arrays.asList(preservationFormats));
        }

        /**
         * Returns the preservation formats as a map for JSON serialization.
         *
         * @return a map with "preserve" key and the set of preservation formats, or null if no formats are specified
         */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public Map<String, Set<String>> getQuality() {
            return preservationFormats.isEmpty()
                    ? null
                    : Map.of("preserve", Collections.unmodifiableSet(preservationFormats));
        }
    }

    /** Report options. */
    // public for JSON serialization
    public static class ReportOptions {
        /** The preferred format of the report. */
        private final ReportFormat format;

        /**
         * Create a set of report options.
         *
         * @param format the preferred format of the report
         */
        public ReportOptions(ReportFormat format) {
            this.format = format;
        }

        /**
         * The preferred format of the report.
         *
         * @return the format
         */
        public ReportFormat getFormat() {
            return format;
        }
    }

    /**
     * Redaction options.
     *
     * <p>Redaction options specify how to redact files in archives when those would otherwise prevent the archive from
     * being processed by the CDR. For example, if a ZIP file contains an executable, the executable can be redacted to
     * allow the ZIP file to be processed.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class RedactionOptions {
        /** The file extension to replace redacted files with. */
        private final String extension;
        /** The media type to use for the redacted file replacement. */
        private final String mime;
        /** The text to use for the redacted file replacement. */
        private final String text;

        /**
         * Create a set of redaction options.
         *
         * @param extension the file extension to replace redacted files with, including the leading dot (e.g.,
         *     {@code ".txt"})
         * @param mime the media type to use for the redacted file replacement
         * @param text the text to use for the redacted file replacement
         */
        public RedactionOptions(String extension, MediaType mime, String text) {
            this(extension, mime.getMediaType(), text);
        }

        /**
         * Create a set of redaction options.
         *
         * @param extension the file extension to replace redacted files with, including the leading dot (e.g.,
         *     {@code ".txt"})
         * @param mime the media type to use for the redacted file replacement
         * @param text the text to use for the redacted file replacement
         */
        public RedactionOptions(String extension, String mime, String text) {
            this.extension = extension;
            this.mime = mime;
            this.text = text;
        }

        /**
         * Returns the file extension used when redacting a file.
         *
         * @return the file extension
         */
        public String getExtension() {
            return extension;
        }

        /**
         * Returns the media type for the redaction.
         *
         * @return the media type
         */
        public String getMime() {
            return mime;
        }

        /**
         * Returns the text used in the redaction.
         *
         * @return the text
         */
        public String getText() {
            return text;
        }
    }

    /**
     * Format conversion options.
     *
     * <p>Format conversion options specify the original format of a file to convert from, which conversion tools may be
     * used, and which media type conversions are allowed. See https://cdr.everfox.com/documentation/format-conversion
     * for details.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ConversionOptions {
        /** The media type of the original file. */
        private final String fromMediaType;
        /** The conversion methods to enable. */
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
            return enabledAndDisabled(enabledMethods, disabledMethods);
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
        public static class Builder {
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
                requireDisjoint(
                        enabledMethods, disabledMethods, "Conversion methods cannot be both enabled and disabled");
                return new ConversionOptions(this);
            }
        }
    }

    /** A conversion from one media type to another. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class MediaTypeConversion {
        /** The media type to convert from. */
        private final String from;
        /** The media type to convert to. */
        private final String to;

        /**
         * Create a media type conversion.
         *
         * @param from the media type to convert from
         * @param to the media type to convert to
         */
        public MediaTypeConversion(String from, String to) {
            this.from = from;
            this.to = to;
        }

        /**
         * Returns the media type to convert from.
         *
         * @return the media type to convert from
         */
        public String getFrom() {
            return from;
        }

        /**
         * Returns the media type to convert to.
         *
         * @return the media type to convert to
         */
        public String getTo() {
            return to;
        }
    }

    /** Options that affect quota usage. */
    // public for JSON serialization
    public static class CostsOptions {
        /** Whether to use the large file environment for files over 250 MB. */
        private final boolean largeFileEnvironment;

        /**
         * Create a set of quota usage options.
         *
         * @param largeFileEnvironment whether to use the large file environment for files over 250 MB
         */
        public CostsOptions(boolean largeFileEnvironment) {
            this.largeFileEnvironment = largeFileEnvironment;
        }

        /**
         * Returns the file processing environment options as a map for JSON serialization.
         *
         * @return a map with a "large" key holding the large file environment options
         */
        public Map<String, Map<String, Boolean>> getEnvironment() {
            return Map.of("large", Map.of("enabled", largeFileEnvironment));
        }
    }

    /** Status notification options. */
    // public for JSON serialization
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class StatusOptions {
        /** The ARN of the SNS topic to publish status updates to. */
        private final String snsTopicArn;
        /** The ARN of the role to assume to publish to the SNS topic. */
        private final String snsRoleArn;
        /** Data returned in the status, for the caller's use. */
        private final String userData;

        /**
         * Create a set of status options.
         *
         * @param snsTopicArn the ARN of the SNS topic to publish status updates to, or null
         * @param snsRoleArn the ARN of the role to assume to publish to the SNS topic, or null
         * @param userData data returned in the status, or null
         */
        public StatusOptions(String snsTopicArn, String snsRoleArn, String userData) {
            this.snsTopicArn = snsTopicArn;
            this.snsRoleArn = snsRoleArn;
            this.userData = userData;
        }

        /**
         * Returns the SNS options as a map for JSON serialization.
         *
         * @return a map with "topicArn" and "roleArn" keys, or null if no SNS topic is specified
         */
        public Map<String, String> getSns() {
            if (snsTopicArn == null) {
                return null;
            }
            Map<String, String> sns = new LinkedHashMap<>();
            sns.put("topicArn", snsTopicArn);
            sns.put("roleArn", snsRoleArn);
            return sns;
        }

        /**
         * Returns the user data.
         *
         * @return the user data
         */
        public String getUserData() {
            return userData;
        }
    }

    /** Builder for creating {@link RequestOptions} instances. */
    public static class Builder {

        /** Set of allowed risks. */
        private final Set<String> allowedRisks = new LinkedHashSet<>();
        /** Set of denied risks. */
        private final Set<String> deniedRisks = new LinkedHashSet<>();
        /** Report format for the request options. */
        private ReportFormat reportFormat;
        /** Format conversion options for the request options. */
        private ConversionOptions conversionOptions;
        /** Image options for the request options. */
        private ImagesOptions imagesOptions;
        /** Redaction options for the request options. */
        private RedactionOptions redactionOptions;
        /** Whether to use the large file environment, or null to use the API default. */
        private Boolean largeFileEnvironment;
        /** The ARN of the SNS topic to publish status updates to. */
        private String snsTopicArn;
        /** The ARN of the role to assume to publish to the SNS topic. */
        private String snsRoleArn;
        /** Data returned in the status. */
        private String userData;

        /** Creates a new instance of the Builder. */
        public Builder() {}

        /**
         * Sets the report format for the request options.
         *
         * @param format the report format
         * @return the builder instance
         */
        public Builder reportFormat(ReportFormat format) {
            this.reportFormat = format;
            return this;
        }

        /**
         * Sets the format conversion options for the request options.
         *
         * @param conversionOptions the format conversion options
         * @return the builder instance
         */
        public Builder conversionOptions(ConversionOptions conversionOptions) {
            this.conversionOptions = conversionOptions;
            return this;
        }

        /**
         * Sets the image options for the request options.
         *
         * @param imagesOptions the image options
         * @return the builder instance
         */
        public Builder imagesOptions(ImagesOptions imagesOptions) {
            this.imagesOptions = imagesOptions;
            return this;
        }

        /**
         * Sets the redaction options for the request options.
         *
         * @param redactionOptions the redaction options
         * @return the builder instance
         */
        public Builder redactionOptions(RedactionOptions redactionOptions) {
            this.redactionOptions = redactionOptions;
            return this;
        }

        /**
         * Adds allowed risks to the request options.
         *
         * @param allowedRisks the risks to allow
         * @return the builder instance
         */
        public Builder allowRisks(Risk... allowedRisks) {
            Arrays.stream(allowedRisks).map(Risk::getRisk).forEach(this.allowedRisks::add);
            return this;
        }

        /**
         * Adds denied risks to the request options.
         *
         * @param deniedRisks the risks to deny
         * @return the builder instance
         */
        public Builder denyRisks(Risk... deniedRisks) {
            Arrays.stream(deniedRisks).map(Risk::getRisk).forEach(this.deniedRisks::add);
            return this;
        }

        /**
         * Sets whether to use the large file environment automatically for files over 250 MB. This increases the quota
         * usage rate.
         *
         * @param enabled true to use the large file environment
         * @return the builder instance
         */
        public Builder largeFileEnvironment(boolean enabled) {
            this.largeFileEnvironment = enabled;
            return this;
        }

        /**
         * Sets the AWS SNS topic to publish status updates to, as an alternative to polling for status.
         *
         * @param topicArn the ARN of the SNS topic
         * @param roleArn the ARN of a role in the topic's account that the API assumes to publish to the topic
         * @return the builder instance
         */
        public Builder snsStatus(String topicArn, String roleArn) {
            Objects.requireNonNull(topicArn, "topicArn cannot be null");
            Objects.requireNonNull(roleArn, "roleArn cannot be null");
            this.snsTopicArn = topicArn;
            this.snsRoleArn = roleArn;
            return this;
        }

        /**
         * Sets data that is returned in every status for this job, for example to correlate SNS notifications.
         *
         * @param userData the user data, at most {@value RequestOptions#MAX_USER_DATA_LENGTH} characters
         * @return the builder instance
         * @throws IllegalArgumentException if the user data is too long
         */
        public Builder userData(String userData) {
            if (userData != null && userData.length() > MAX_USER_DATA_LENGTH) {
                throw new IllegalArgumentException(
                        "userData cannot be longer than " + MAX_USER_DATA_LENGTH + " characters");
            }
            this.userData = userData;
            return this;
        }

        /**
         * Builds the {@link RequestOptions} instance with the specified configurations.
         *
         * @return a new instance of RequestOptions
         * @throws IllegalArgumentException if any risk is both allowed and denied
         */
        public RequestOptions build() {
            requireDisjoint(allowedRisks, deniedRisks, "Risks cannot be both allowed and denied");
            return new RequestOptions(this);
        }
    }

    /**
     * Creates a new instance of the Builder.
     *
     * @return a new instance of the Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Ensures that two sets have no elements in common.
     *
     * @param first the first set
     * @param second the second set
     * @param message the exception message, to which the common elements are appended
     * @throws IllegalArgumentException if the sets have any elements in common
     */
    private static void requireDisjoint(Set<String> first, Set<String> second, String message) {
        Set<String> common = new LinkedHashSet<>(first);
        common.retainAll(second);
        if (!common.isEmpty()) {
            throw new IllegalArgumentException(message + ": " + common);
        }
    }
}
