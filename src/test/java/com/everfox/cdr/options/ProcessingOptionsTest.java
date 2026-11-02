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

import static org.junit.jupiter.api.Assertions.*;

import com.everfox.cdr.MediaType;
import com.everfox.cdr.Risk;
import org.junit.jupiter.api.Test;

/** Unit tests for the shared {@link ProcessingOptions} and its value types. */
class ProcessingOptionsTest {

    /** Minimal concrete options for testing the abstract {@link ProcessingOptions}. */
    private static class TestOptions extends ProcessingOptions {

        /**
         * Creates the options held by a builder.
         *
         * @param builder the builder
         */
        TestOptions(Builder builder) {
            super(builder);
        }

        /** Builder for {@link TestOptions}. */
        static final class Builder extends ProcessingOptions.Builder<Builder> {

            @Override
            protected Builder self() {
                return this;
            }

            /**
             * Builds the options.
             *
             * @return the options
             */
            TestOptions build() {
                validate();
                return new TestOptions(this);
            }
        }
    }

    /** Default constructor. */
    ProcessingOptionsTest() {
        // Nothing to do
    }

    /**
     * Creates a new builder.
     *
     * @return the builder
     */
    private static TestOptions.Builder builder() {
        return new TestOptions.Builder();
    }

    /** Tests that no options serialize to an empty JSON object. */
    @Test
    void testNoOptions() {
        assertEquals("{}", builder().build().toJson());
    }

    /** Tests the report format. */
    @Test
    void testReportFormat() {
        assertEquals(
                "{\"report\":{\"format\":\"full\"}}",
                builder().reportFormat(ReportFormat.FULL).build().toJson());
        assertEquals("full", ReportFormat.FULL.toString());
        assertEquals(
                ReportFormat.NONE,
                builder().reportFormat(ReportFormat.NONE).build().getReport().getFormat());
    }

    /** Tests allowing and denying risks. */
    @Test
    void testRisks() {
        assertEquals(
                "{\"risks\":{\"allow\":[\"steg/image/gif\"]}}",
                builder().allowRisks(Risk.STEG_IMAGE_GIF).build().toJson());
        assertEquals(
                "{\"risks\":{\"deny\":[\"steg/image/gif\"]}}",
                builder().denyRisks(Risk.STEG_IMAGE_GIF).build().toJson());
    }

    /** Tests that repeated risk calls add to the lists and keep their order. */
    @Test
    void testRisksAccumulateInOrder() {
        ProcessingOptions options = builder()
                .allowRisks(Risk.POLY_TEXT_JSON)
                .allowRisks(Risk.STRUCTURED_NO_SCHEMA_JSON, Risk.POLY_TEXT_JSON)
                .denyRisks(Risk.EXE)
                .build();

        assertEquals(
                "{\"risks\":{\"allow\":[\"poly/text/json\",\"structured/no-schema/json\"],\"deny\":[\"exe\"]}}",
                options.toJson());
    }

    /** Tests that a risk that is both allowed and denied is rejected, even when added in separate calls. */
    @Test
    void testConflictingRisks() {
        TestOptions.Builder builder =
                builder().allowRisks(Risk.EXE).denyRisks(Risk.POLY, Risk.STEG).allowRisks(Risk.POLY);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);
        assertEquals("Risks cannot be both allowed and denied: [poly]", exception.getMessage());
    }

    /** Tests the image options with media types, strings, and no formats. */
    @Test
    void testImages() {
        assertEquals(
                "{\"images\":{\"quality\":{\"preserve\":[\"image/jpeg\",\"image/png\"]}}}",
                builder()
                        .imagesOptions(new ImagesOptions(MediaType.IMAGE_JPEG, MediaType.IMAGE_PNG))
                        .build()
                        .toJson());
        assertEquals(
                "{\"images\":{\"quality\":{\"preserve\":[\"image/png\",\"image/jpeg\"]}}}",
                builder()
                        .imagesOptions(new ImagesOptions("image/png", "image/jpeg"))
                        .build()
                        .toJson());
        assertEquals(
                "{\"images\":{}}",
                builder()
                        .imagesOptions(new ImagesOptions(new String[0]))
                        .build()
                        .toJson());
    }

    /** Tests the redaction options, omitting null values. */
    @Test
    void testRedactions() {
        assertEquals(
                "{\"redactions\":{\"extension\":\".txt\",\"mime\":\"text/xml\",\"text\":\"Redacted\"}}",
                builder()
                        .redactionOptions(new RedactionOptions(".txt", MediaType.TEXT_XML, "Redacted"))
                        .build()
                        .toJson());
        assertEquals(
                "{\"redactions\":{\"mime\":\"text/plain\",\"text\":\"Redacted\"}}",
                builder()
                        .redactionOptions(new RedactionOptions(null, "text/plain", "Redacted"))
                        .build()
                        .toJson());
    }

    /** Tests empty conversion options. */
    @Test
    void testEmptyConversion() {
        assertEquals(
                "{\"conversion\":{}}",
                builder()
                        .conversionOptions(ConversionOptions.builder().build())
                        .build()
                        .toJson());
    }

    /** Tests the original format to convert from, as a media type and as a string. */
    @Test
    void testConversionFrom() {
        assertEquals(
                "{\"conversion\":{\"from\":{\"mime-type\":\"application/msword\"}}}",
                builder()
                        .conversionOptions(ConversionOptions.builder()
                                .from(MediaType.MSWORD)
                                .build())
                        .build()
                        .toJson());
        assertEquals(
                "{\"conversion\":{\"from\":{\"mime-type\":\"application/vnd.ms-excel\"}}}",
                builder()
                        .conversionOptions(ConversionOptions.builder()
                                .from("application/vnd.ms-excel")
                                .build())
                        .build()
                        .toJson());
    }

    /** Tests that enabled conversion methods keep their order of priority alongside disabled methods. */
    @Test
    void testConversionMethods() {
        assertEquals(
                "{\"conversion\":{\"methods\":{\"enabled\":[\"text-extraction\",\"report\"]}}}",
                builder()
                        .conversionOptions(ConversionOptions.builder()
                                .enableMethods(ConversionMethod.TEXT_EXTRACTION, ConversionMethod.REPORT)
                                .build())
                        .build()
                        .toJson());
        assertEquals(
                "{\"conversion\":{\"methods\":{\"enabled\":[\"libreoffice\"],\"disabled\":[\"default\"]}}}",
                builder()
                        .conversionOptions(ConversionOptions.builder()
                                .disableMethods(ConversionMethod.DEFAULT)
                                .enableMethods(ConversionMethod.LIBREOFFICE)
                                .build())
                        .build()
                        .toJson());
        assertEquals("text-extraction", ConversionMethod.TEXT_EXTRACTION.toString());
    }

    /** Tests enabling and disabling media type conversions, with media types and strings. */
    @Test
    void testMediaTypeConversions() {
        assertEquals(
                "{\"conversion\":{\"mime_types\":{\"enabled\":[{\"from\":\"image/bmp\",\"to\":\"image/png\"},"
                        + "{\"from\":\"image/tiff\",\"to\":\"image/png\"}]}}}",
                builder()
                        .conversionOptions(ConversionOptions.builder()
                                .enableMediaTypeConversion(MediaType.IMAGE_BMP, MediaType.IMAGE_PNG)
                                .enableMediaTypeConversion(MediaType.IMAGE_TIFF, MediaType.IMAGE_PNG)
                                .build())
                        .build()
                        .toJson());
        assertEquals(
                "{\"conversion\":{\"mime_types\":{\"disabled\":[{\"from\":\"image/bmp\",\"to\":\"image/png\"},"
                        + "{\"from\":\"application/msword\",\"to\":\"application/pdf\"}]}}}",
                builder()
                        .conversionOptions(ConversionOptions.builder()
                                .disableMediaTypeConversion(MediaType.IMAGE_BMP, MediaType.IMAGE_PNG)
                                .disableMediaTypeConversion("application/msword", "application/pdf")
                                .build())
                        .build()
                        .toJson());
    }

    /** Tests that a conversion method that is both enabled and disabled is rejected. */
    @Test
    void testConflictingConversionMethods() {
        ConversionOptions.Builder builder = ConversionOptions.builder()
                .enableMethods(ConversionMethod.TEXT_EXTRACTION, ConversionMethod.REPORT)
                .disableMethods(ConversionMethod.REPORT);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);
        assertEquals("Conversion methods cannot be both enabled and disabled: [report]", exception.getMessage());
    }

    /** Tests that the shared options are serialized in a fixed order. */
    @Test
    void testPropertyOrder() {
        ProcessingOptions options = builder()
                .allowRisks(Risk.EXE)
                .redactionOptions(new RedactionOptions(null, (String) null, "R"))
                .imagesOptions(new ImagesOptions(MediaType.IMAGE_PNG))
                .conversionOptions(ConversionOptions.builder().build())
                .reportFormat(ReportFormat.NONE)
                .build();

        assertEquals(
                "{\"report\":{\"format\":\"none\"},\"conversion\":{},"
                        + "\"images\":{\"quality\":{\"preserve\":[\"image/png\"]}},"
                        + "\"redactions\":{\"text\":\"R\"},\"risks\":{\"allow\":[\"exe\"]}}",
                options.toJson());
    }

    /** Tests that serialization failures are wrapped. */
    @Test
    void testToJsonSerializationFailure() {
        ProcessingOptions options = new TestOptions(builder()) {
            @Override
            public ImagesOptions getImages() {
                throw new IllegalArgumentException("boom");
            }
        };

        IllegalStateException exception = assertThrows(IllegalStateException.class, options::toJson);
        assertEquals("Failed to serialize options to JSON", exception.getMessage());
    }
}
