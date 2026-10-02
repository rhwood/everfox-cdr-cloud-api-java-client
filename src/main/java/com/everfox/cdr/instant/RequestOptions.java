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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.everfox.cdr.MediaType;
import com.everfox.cdr.Risk;
import com.fasterxml.jackson.annotation.JsonInclude;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * Options for customizing file processing behavior.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RequestOptions {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ReportOptions report;
    // private ConversionOptions conversion; // not yet implemented
    private final ImagesOptions images;
    // private RedactionOptions redactions; // not yet implemented
    private final Set<String> allowedRisks;
    private final Set<String> deniedRisks;

    /**
     * Creates a new instance of {@link RequestOptions}.
     *
     * Note that allowed risks and denied risks are mutually exclusive.
     * If a risk is in both, the denied risks will take precedence.
     * 
     * @param format the report format
     * @param images the image options
     * @param allowedRisks the allowed risks
     * @param deniedRisks the denied risks
     */
    public RequestOptions(ReportFormat format, ImagesOptions images, Set<String> allowedRisks, Set<String> deniedRisks) {
        if (deniedRisks != null && allowedRisks != null) {
            for (String risk : deniedRisks) {
                allowedRisks.remove(risk);
            }
        }
        this.report = format != null ? new ReportOptions(format) : null;
        this.images = images;
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
                    "deny", deniedRisks
            );
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

    /**
     * Report format options. See https://cdr.everfox.com/documentation/report for details.
     */
    public enum ReportFormat {
        /**
         * Reports on data that has had a notable change.
         */
        CHANGED("changed"),
        /**
         * The current default behavior is @{@link #CHANGED}.
         */
        DEFAULT("default"),
        /**
         * Reports everything about the data, including information on data that hasn't been transformed. These reports can be big!
         */
        FULL("full"),
        /**
         * A report will not be generated. Use this if you don't need the report.
         */
        NONE("none");

        private final String format;

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

    /**
     * Options for handling images.
     */
    public static class ImagesOptions {

        // this is a set and not an arrat to avoid duplicates and to make it easier to check for existence
        private final Set<String> preservationFormats;

        /**
         * Create a set of image options with the specified preservation formats.
         *
         * @param preservationFormats the media types to preserve
         */
        public ImagesOptions(MediaType... preservationFormats) {
            this.preservationFormats = new HashSet<>(Arrays
                .stream(preservationFormats)
                .map(MediaType::toString)
                .toList());
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
        @JsonInclude (JsonInclude.Include.NON_NULL)
        public Map<String, Set<String>> getQuality() {
            if (preservationFormats.isEmpty()) {
                return null;
            } else {
                return Map.of("preserve", preservationFormats);
            }
        }

    }

    /**
     * Report options.
     */
    // public for JSON serialization
    public static class ReportOptions {
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

    public static class Builder {

        private Set<String> allowedRisks = new HashSet<>();
        private Set<String> deniedRisks = new HashSet<>();
        private ReportFormat reportFormat;
        private ImagesOptions imagesOptions;

        public Builder() {
        }

        public Builder reportFormat(ReportFormat format) {
            this.reportFormat = format;
            return this;
        }

        public Builder imagesOptions(ImagesOptions imagesOptions) {
            this.imagesOptions = imagesOptions;
            return this;
        }

        public Builder allowRisks(Risk... allowedRisks) {
            this.allowedRisks = Arrays.stream(allowedRisks).map(Risk::getRisk).collect(Collectors.toSet());
            return this;
        }

        public Builder denyRisks(Risk... deniedRisks) {
            this.deniedRisks = Arrays.stream(deniedRisks).map(Risk::getRisk).collect(Collectors.toSet());
            return this;
        }

        public RequestOptions build() {
            return new RequestOptions(reportFormat, imagesOptions, allowedRisks, deniedRisks);
        }
    }
}
