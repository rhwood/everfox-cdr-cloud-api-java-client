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
import java.util.Arrays;
import java.util.HashSet;
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
    // private ConversionOptions conversion; // not yet implemented
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
     * @param images the image options
     * @param redactions the redaction options
     * @param allowedRisks the allowed risks
     * @param deniedRisks the denied risks
     */
    public RequestOptions(
            ReportFormat format,
            ImagesOptions images,
            RedactionOptions redactions,
            Set<String> allowedRisks,
            Set<String> deniedRisks) {
        if (deniedRisks != null && allowedRisks != null) {
            for (String risk : deniedRisks) {
                allowedRisks.remove(risk);
            }
        }
        this.report = format != null ? new ReportOptions(format) : null;
        this.images = images;
        this.redactions = redactions;
        this.allowedRisks = allowedRisks != null ? new HashSet<>(allowedRisks) : new HashSet<>();
        this.deniedRisks = deniedRisks != null ? new HashSet<>(deniedRisks) : new HashSet<>();
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

    /** Builder for creating {@link RequestOptions} instances. */
    public static class Builder {

        /** Set of allowed risks. */
        private Set<String> allowedRisks = new HashSet<>();
        /** Set of denied risks. */
        private Set<String> deniedRisks = new HashSet<>();
        /** Report format for the request options. */
        private ReportFormat reportFormat;
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
         */
        public RequestOptions build() {
            return new RequestOptions(reportFormat, imagesOptions, redactionOptions, allowedRisks, deniedRisks);
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
}
