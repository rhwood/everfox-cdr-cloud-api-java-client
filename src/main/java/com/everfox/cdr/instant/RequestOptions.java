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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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

    private ReportOptions report;
    private ConversionOptions conversion;
    private ImageQualityOptions imageQuality;
    private RedactionOptions redactions;
    private Set<String> allowedRisks = new HashSet<>();
    private Set<String> deniedRisks = new HashSet<>();

    /**
     * Default constructor.
     */
    public RequestOptions() {
    }

    /**
     * Allow the specified risks.
     *
     * @param risks the risks to allow
     */
    public void allowRisks(Risk... risks) {
        for (Risk risk : risks) {
            allowRisk(risk);
        }
    }

    /**
     * Deny the specified risks.
     *
     * @param risks the risks to deny
     */
    public void denyRisks(Risk... risks) {
        for (Risk risk : risks) {
            denyRisk(risk);
        }
    }

    /**
     * Allow the specified risk.
     *
     * @param risk the risk to allow
     */
    public void allowRisk(Risk risk) {
        allowRisk(risk.getRisk());
    }

    /**
     * Deny the specified risk.
     *
     * @param risk the risk to deny
     */
    public void denyRisk(Risk risk) {
        denyRisk(risk.getRisk());
    }

    /**
     * Allow the specified risk.
     *
     * @param risk the risk to allow
     */
    public void allowRisk(String risk) {
        allowedRisks.add(risk);
        deniedRisks.remove(risk);
    }

    /**
     * Deny the specified risk.
     *
     * @param risk the risk to deny
     */
    public void denyRisk(String risk) {
        deniedRisks.add(risk);
        allowedRisks.remove(risk);
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
    public ReportOptions getReport() {
        return report;
    }

    /**
     * Set the report options.
     *
     * @param format report format
     */
    public void setReport(ReportFormat format) {
        this.report = new ReportOptions(format);
    }

    /**
     * Returns the conversion options.
     *
     * @return conversion options
     */
    public ConversionOptions getConversion() {
        return conversion;
    }

    /**
     * Sets the conversion options.
     *
     * @param conversion conversion options
     */
    public void setConversion(ConversionOptions conversion) {
        this.conversion = conversion;
    }

    /**
     * Returns the image quality options.
     *
     * @return image quality options
     */
    public ImageQualityOptions getImageQuality() {
        return imageQuality;
    }

    /**
     * Sets the image quality options.
     *
     * @param imageQuality image quality options
     */
    public void setImageQuality(ImageQualityOptions imageQuality) {
        this.imageQuality = imageQuality;
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
     * Sets the redaction options.
     *
     * @param redactions redaction options
     */
    public void setRedactions(RedactionOptions redactions) {
        this.redactions = redactions;
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
     * Conversion options.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ConversionOptions {
        private String sourceMimeType;
        private Boolean enableLibreOffice;
        private Boolean enableTextExtraction;

        /**
         * Default constructor.
         */
        public ConversionOptions() {
        }

        /**
         * Returns the source media type for conversion.
         *
         * @return source media type
         */
        public String getSourceMimeType() {
            return sourceMimeType;
        }

        /**
         * Sets the source media type for conversion.
         *
         * @param sourceMimeType source media type
         */
        public void setSourceMimeType(MediaType sourceMimeType) {
            setSourceMimeType(sourceMimeType.getMediaType());
        }

        /**
         * Sets the source media type for conversion.
         *
         * @param sourceMimeType source media type
         */
        public void setSourceMimeType(String sourceMimeType) {
            this.sourceMimeType = sourceMimeType;
        }

        /**
         * Returns whether LibreOffice conversion is enabled.
         *
         * @return true if enabled, false if disabled, null if not specified
         */
        public Boolean getEnableLibreOffice() {
            return enableLibreOffice;
        }

        /**
         * Sets whether LibreOffice conversion is enabled.
         *
         * @param enableLibreOffice true to enable, false to disable, null to not specify
         */
        public void setEnableLibreOffice(Boolean enableLibreOffice) {
            this.enableLibreOffice = enableLibreOffice;
        }

        /**
         * Returns whether text extraction is enabled.
         *
         * @return true if enabled, false if disabled, null if not specified
         */
        public Boolean getEnableTextExtraction() {
            return enableTextExtraction;
        }

        /**
         * Sets whether text extraction is enabled.
         *
         * @param enableTextExtraction true to enable, false to disable, null to not specify
         */
        public void setEnableTextExtraction(Boolean enableTextExtraction) {
            this.enableTextExtraction = enableTextExtraction;
        }
    }

    /**
     * Image quality preservation options.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ImageQualityOptions {
        private Boolean preserveJpeg;
        private Boolean preservePng;
        private Boolean preserveGif;

        /**
         * Default constructor.
         */
        public ImageQualityOptions() {
        }

        /**
         * Returns whether JPEG images are preserved.
         *
         * @return true if preserved, false if not, null if not specified
         */
        public Boolean getPreserveJpeg() {
            return preserveJpeg;
        }

        /**
         * Sets whether JPEG images are preserved.
         *
         * @param preserveJpeg true to preserve, false to not preserve, null to not specify
         */
        public void setPreserveJpeg(Boolean preserveJpeg) {
            this.preserveJpeg = preserveJpeg;
        }

        /**
         * Returns whether PNG images are preserved.
         *
         * @return true if preserved, false if not, null if not specified
         */
        public Boolean getPreservePng() {
            return preservePng;
        }

        /**
         * Sets whether PNG images are preserved.
         *
         * @param preservePng true to preserve, false to not preserve, null to not specify
         */
        public void setPreservePng(Boolean preservePng) {
            this.preservePng = preservePng;
        }

        /**
         * Returns whether GIF images are preserved.
         *
         * @return true if preserved, false if not, null if not specified
         */
        public Boolean getPreserveGif() {
            return preserveGif;
        }

        /**
         * Sets whether GIF images are preserved.
         *
         * @param preserveGif true to preserve, false to not preserve, null to not specify
         */
        public void setPreserveGif(Boolean preserveGif) {
            this.preserveGif = preserveGif;
        }
    }

    /**
     * Redaction options.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class RedactionOptions {
        private String replacementText;

        /**
         * Default constructor.
         */
        public RedactionOptions() {
        }

        /**
         * Returns the text to use as a replacement for redacted content.
         *
         * @return the replacement text
         */
        public String getReplacementText() {
            return replacementText;
        }

        /**
         * Sets the text to use as a replacement for redacted content.
         *
         * @param replacementText the replacement text
         */
        public void setReplacementText(String replacementText) {
            this.replacementText = replacementText;
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

}
