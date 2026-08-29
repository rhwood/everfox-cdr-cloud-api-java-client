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

    public RiskOptions getRisks() {
        return risks;
    }

    public void setRisks(RiskOptions risks) {
        this.risks = risks;
    }

    public ReportFormat getReporting() {
        return reporting;
    }

    public void setReporting(ReportFormat reporting) {
        this.reporting = reporting;
    }

    public ConversionOptions getConversion() {
        return conversion;
    }

    public void setConversion(ConversionOptions conversion) {
        this.conversion = conversion;
    }

    public ImageQualityOptions getImageQuality() {
        return imageQuality;
    }

    public void setImageQuality(ImageQualityOptions imageQuality) {
        this.imageQuality = imageQuality;
    }

    public RedactionOptions getRedactions() {
        return redactions;
    }

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

        public Boolean getAllowMacros() {
            return allowMacros;
        }

        public void setAllowMacros(Boolean allowMacros) {
            this.allowMacros = allowMacros;
        }

        public Boolean getAllowExecutables() {
            return allowExecutables;
        }

        public void setAllowExecutables(Boolean allowExecutables) {
            this.allowExecutables = allowExecutables;
        }

        public Boolean getAllowSteganography() {
            return allowSteganography;
        }

        public void setAllowSteganography(Boolean allowSteganography) {
            this.allowSteganography = allowSteganography;
        }

        public Boolean getAllowPolymorphicContent() {
            return allowPolymorphicContent;
        }

        public void setAllowPolymorphicContent(Boolean allowPolymorphicContent) {
            this.allowPolymorphicContent = allowPolymorphicContent;
        }
    }

    /**
     * Report format options.
     */
    public enum ReportFormat {
        CHANGED,
        DEFAULT,
        FULL,
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

        public String getSourceMimeType() {
            return sourceMimeType;
        }

        public void setSourceMimeType(String sourceMimeType) {
            this.sourceMimeType = sourceMimeType;
        }

        public Boolean getEnableLibreOffice() {
            return enableLibreOffice;
        }

        public void setEnableLibreOffice(Boolean enableLibreOffice) {
            this.enableLibreOffice = enableLibreOffice;
        }

        public Boolean getEnableTextExtraction() {
            return enableTextExtraction;
        }

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

        public Boolean getPreserveJpeg() {
            return preserveJpeg;
        }

        public void setPreserveJpeg(Boolean preserveJpeg) {
            this.preserveJpeg = preserveJpeg;
        }

        public Boolean getPreservePng() {
            return preservePng;
        }

        public void setPreservePng(Boolean preservePng) {
            this.preservePng = preservePng;
        }

        public Boolean getPreserveGif() {
            return preserveGif;
        }

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

        public String getReplacementText() {
            return replacementText;
        }

        public void setReplacementText(String replacementText) {
            this.replacementText = replacementText;
        }
    }
}
