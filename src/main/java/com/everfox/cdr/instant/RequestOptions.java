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

import com.everfox.cdr.MediaType;
import com.everfox.cdr.Risk;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/** Options for customizing file processing behavior. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RequestOptions {

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

    /**
     * Creates a new instance of {@link RequestOptions}.
     *
     * <p>Note that allowed risks and denied risks are mutually exclusive. If a risk is in both, the denied risks will
     * take precedence.
     *
     * @param format the report format
     * @param conversion the format conversion options
     * @param images the image options
     * @param redactions the redaction options
     * @param allowedRisks the allowed risks
     * @param deniedRisks the denied risks
     */
    public RequestOptions(
            ReportFormat format,
            ConversionOptions conversion,
            ImagesOptions images,
            RedactionOptions redactions,
            Set<String> allowedRisks,
            Set<String> deniedRisks) {
        this.report = format != null ? new ReportOptions(format) : null;
        this.conversion = conversion;
        this.images = images;
        this.redactions = redactions;
        this.allowedRisks = allowedRisks != null ? new HashSet<>(allowedRisks) : new HashSet<>();
        this.deniedRisks = deniedRisks != null ? new HashSet<>(deniedRisks) : new HashSet<>();
        this.allowedRisks.removeAll(this.deniedRisks);
    }

    /**
     * Returns the format conversion options.
     *
     * @return the format conversion options
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public ConversionOptions getConversion() {
        return conversion;
    }

    /**
     * Returns the image options.
     *
     * @return the image options
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public ImagesOptions getImages() {
        return images;
    }

    /**
     * Returns the allowed and denied risks as a map.
     *
     * @return a map with "allow" and/or "deny" keys, or null if no risks are specified
     */
    public Map<String, Set<String>> getRisks() {
        if (allowedRisks.isEmpty() && deniedRisks.isEmpty()) {
            return null;
        } else if (deniedRisks.isEmpty()) {
            return Map.of("allow", allowedRisks);
        } else if (allowedRisks.isEmpty()) {
            return Map.of("deny", deniedRisks);
        } else {
            return Map.of(
                    "allow", allowedRisks,
                    "deny", deniedRisks);
        }
    }

    /**
     * Returns the report format options.
     *
     * @return report format
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public ReportOptions getReport() {
        return report;
    }

    /**
     * Returns the redaction options.
     *
     * @return redaction options
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public RedactionOptions getRedactions() {
        return redactions;
    }

    /**
     * Converts these options to a JSON string for the X-Options header.
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
        /** The current default behavior is @{@link #CHANGED}. */
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
        /** Return the report in the response body instead of the transformed file. */
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

        /**
         * The set of media types to preserve image quality for.
         *
         * <p>This is a set and not an array to avoid duplicates and to make it easier to check for existence.
         */
        private final Set<String> preservationFormats;

        /**
         * Create a set of image options with the specified preservation formats.
         *
         * @param preservationFormats the media types to preserve
         */
        public ImagesOptions(MediaType... preservationFormats) {
            this.preservationFormats = new HashSet<>(
                    Arrays.stream(preservationFormats).map(MediaType::toString).toList());
        }

        /**
         * Create a set of image options with the specified preservation formats.
         *
         * @param preservationFormats the media types to preserve
         */
        public ImagesOptions(String... preservationFormats) {
            this.preservationFormats = new HashSet<>(Arrays.asList(preservationFormats));
        }

        /**
         * Returns the preservation formats as a map for JSON serialization.
         *
         * @return a map with "preserve" key and the set of preservation formats, or null if no formats are specified
         */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public Map<String, Set<String>> getQuality() {
            if (preservationFormats.isEmpty()) {
                return null;
            } else {
                return Map.of("preserve", preservationFormats);
            }
        }
    }

    /** Report options. */
    // public for JSON serialization
    public static class ReportOptions {
        /** The preferred format of the report. */
        private final ReportFormat format;

        /**
         * Create a set of repport options.
         *
         * @param format the preferred format of the report
         */
        public ReportOptions(ReportFormat format) {
            this.format = format;
        }

        /**
         * The preferred format of the report
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
            this(extension, mime.toString(), text);
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
         * Returns the file extension used when redating a file.
         *
         * @return the file extension
         */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public String getExtension() {
            return extension;
        }

        /**
         * Returns the media type for the redaction.
         *
         * @return the media type
         */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public String getMime() {
            return mime;
        }

        /**
         * Returns the text used in the redaction.
         *
         * @return the text
         */
        @JsonInclude(JsonInclude.Include.NON_NULL)
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
         * <p>Note that enabled methods and disabled methods are mutually exclusive. If a method is in both, the
         * disabled methods will take precedence.
         *
         * @param fromMediaType the media type of the original file
         * @param enabledMethods the conversion methods to enable
         * @param disabledMethods the conversion methods to disable
         * @param enabledMediaTypes the media type conversions to enable
         * @param disabledMediaTypes the media type conversions to disable
         */
        public ConversionOptions(
                String fromMediaType,
                Set<String> enabledMethods,
                Set<String> disabledMethods,
                List<MediaTypeConversion> enabledMediaTypes,
                List<MediaTypeConversion> disabledMediaTypes) {
            this.fromMediaType = fromMediaType;
            this.enabledMethods = enabledMethods != null ? new LinkedHashSet<>(enabledMethods) : new LinkedHashSet<>();
            this.disabledMethods =
                    disabledMethods != null ? new LinkedHashSet<>(disabledMethods) : new LinkedHashSet<>();
            this.enabledMethods.removeAll(this.disabledMethods);
            this.enabledMediaTypes = enabledMediaTypes != null ? List.copyOf(enabledMediaTypes) : List.of();
            this.disabledMediaTypes = disabledMediaTypes != null ? List.copyOf(disabledMediaTypes) : List.of();
        }

        /**
         * Returns the original file format as a map for JSON serialization.
         *
         * @return a map with "mime-type" key and the original media type, or null if no media type is specified
         */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public Map<String, String> getFrom() {
            return fromMediaType != null ? Map.of("mime-type", fromMediaType) : null;
        }

        /**
         * Returns the enabled and disabled conversion methods as a map for JSON serialization.
         *
         * @return a map with "enabled" and/or "disabled" keys, or null if no methods are specified
         */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public Map<String, Set<String>> getMethods() {
            return enabledAndDisabled(enabledMethods, disabledMethods);
        }

        /**
         * Returns the enabled and disabled media type conversions as a map for JSON serialization.
         *
         * @return a map with "enabled" and/or "disabled" keys, or null if no media type conversions are specified
         */
        @JsonInclude(JsonInclude.Include.NON_NULL)
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
                return new ConversionOptions(
                        fromMediaType, enabledMethods, disabledMethods, enabledMediaTypes, disabledMediaTypes);
            }
        }
    }

    /** A conversion from one media type to another. */
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
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public String getFrom() {
            return from;
        }

        /**
         * Returns the media type to convert to.
         *
         * @return the media type to convert to
         */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public String getTo() {
            return to;
        }
    }

    /** Builder for creating {@link RequestOptions} instances. */
    public static class Builder {

        /** Set of allowed risks. */
        private Set<String> allowedRisks = new HashSet<>();
        /** Set of denied risks. */
        private Set<String> deniedRisks = new HashSet<>();
        /** Report format for the request options. */
        private ReportFormat reportFormat;
        /** Format conversion options for the request options. */
        private ConversionOptions conversionOptions;
        /** Image options for the request options. */
        private ImagesOptions imagesOptions;
        /** Redaction options for the request options. */
        private RedactionOptions redactionOptions;

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
            this.allowedRisks = Arrays.stream(allowedRisks).map(Risk::getRisk).collect(Collectors.toSet());
            return this;
        }

        /**
         * Adds denied risks to the request options.
         *
         * @param deniedRisks the risks to deny
         * @return the builder instance
         */
        public Builder denyRisks(Risk... deniedRisks) {
            this.deniedRisks = Arrays.stream(deniedRisks).map(Risk::getRisk).collect(Collectors.toSet());
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
            return new RequestOptions(
                    reportFormat, conversionOptions, imagesOptions, redactionOptions, allowedRisks, deniedRisks);
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
