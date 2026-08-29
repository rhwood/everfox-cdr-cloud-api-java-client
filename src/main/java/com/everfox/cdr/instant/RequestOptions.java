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

import com.fasterxml.jackson.annotation.JsonInclude;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * Options for customizing file processing behavior.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RequestOptions {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private RiskOptions risks;
    private ReportFormat reporting;
    private ConversionOptions conversion;
    private ImageQualityOptions imageQuality;
    private RedactionOptions redactions;

    /**
     * Default constructor.
     */
    public RequestOptions() {
    }

    /**
     * Returns the risk management options.
     *
     * @return risk options
     */
    public RiskOptions getRisks() {
        return risks;
    }

    /**
     * Sets the risk management options.
     *
     * @param risks risk options
     */
    public void setRisks(RiskOptions risks) {
        this.risks = risks;
    }

    /**
     * Returns the report format options.
     *
     * @return report format
     */
    public ReportFormat getReporting() {
        return reporting;
    }

    /**
     * Sets the report format options.
     *
     * @param reporting report format
     */
    public void setReporting(ReportFormat reporting) {
        this.reporting = reporting;
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
     * Risk management options for allowing or denying specific threat categories.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class RiskOptions {
        private Boolean allowMacros;
        private Boolean allowExecutables;
        private Boolean allowSteganography;
        private Boolean allowPolymorphicContent;

        /**
         * Default constructor.
         */
        public RiskOptions() {
        }

        /**
         * Returns whether macros are allowed.
         *
         * @return true if allowed, false if denied, null if not specified
         */
        public Boolean getAllowMacros() {
            return allowMacros;
        }

        /**
         * Sets whether macros are allowed.
         *
         * @param allowMacros true to allow, false to deny, null to not specify
         */
        public void setAllowMacros(Boolean allowMacros) {
            this.allowMacros = allowMacros;
        }

        /**
         * Returns whether executables are allowed.
         *
         * @return true if allowed, false if denied, null if not specified
         */
        public Boolean getAllowExecutables() {
            return allowExecutables;
        }

        /**
         * Sets whether executables are allowed.
         *
         * @param allowExecutables true to allow, false to deny, null to not specify
         */
        public void setAllowExecutables(Boolean allowExecutables) {
            this.allowExecutables = allowExecutables;
        }

        /**
         * Returns whether steganography is allowed.
         *
         * @return true if allowed, false if denied, null if not specified
         */
        public Boolean getAllowSteganography() {
            return allowSteganography;
        }

        /**
         * Sets whether steganography is allowed.
         *
         * @param allowSteganography true to allow, false to deny, null to not specify
         */
        public void setAllowSteganography(Boolean allowSteganography) {
            this.allowSteganography = allowSteganography;
        }

        /**
         * Returns whether polymorphic content is allowed.
         *
         * @return true if allowed, false if denied, null if not specified
         */
        public Boolean getAllowPolymorphicContent() {
            return allowPolymorphicContent;
        }

        /**
         * Sets whether polymorphic content is allowed.
         *
         * @param allowPolymorphicContent true to allow, false to deny, null to not specify
         */
        public void setAllowPolymorphicContent(Boolean allowPolymorphicContent) {
            this.allowPolymorphicContent = allowPolymorphicContent;
        }
    }

    /**
     * Report format options. See https://cdr.everfox.com/documentation/report for details.
     */
    public enum ReportFormat {
        /**
         * Reports on data that has had a notable change.
         */
        CHANGED,
        /**
         * The current default behavior is @{@link #CHANGED}.
         */
        DEFAULT,
        /**
         * Reports everything about the data, including information on data that hasn't been transformed. These reports can be big!
         */
        FULL,
        /**
         * A report will not be generated. Use this if you don't need the report.
         */
        NONE
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
         * Returns the source MIME type for conversion.
         *
         * @return source MIME type
         */
        public String getSourceMimeType() {
            return sourceMimeType;
        }

        /**
         * Sets the source MIME type for conversion.
         *
         * @param sourceMimeType source MIME type
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
}
