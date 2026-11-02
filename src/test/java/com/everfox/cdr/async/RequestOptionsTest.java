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

import static org.junit.jupiter.api.Assertions.*;

import com.everfox.cdr.MediaType;
import com.everfox.cdr.Risk;
import org.junit.jupiter.api.Test;

/** Unit tests for the async {@link RequestOptions} class. */
class RequestOptionsTest {

    /** Default constructor. */
    RequestOptionsTest() {
        // Nothing to do
    }

    /** Tests that no options serialize to an empty JSON object. */
    @Test
    void testNoOptions() {
        assertEquals("{}", RequestOptions.builder().build().toJson());
    }

    /** Tests the report format. */
    @Test
    void testReportFormat() {
        RequestOptions options = RequestOptions.builder()
                .reportFormat(RequestOptions.ReportFormat.FULL)
                .build();

        assertEquals("{\"report\":{\"format\":\"full\"}}", options.toJson());
        assertEquals("full", RequestOptions.ReportFormat.FULL.toString());
    }

    /** Tests that repeated risk calls add to the lists and keep their order. */
    @Test
    void testRisks() {
        RequestOptions options = RequestOptions.builder()
                .allowRisks(Risk.POLY_TEXT_JSON)
                .allowRisks(Risk.STRUCTURED_NO_SCHEMA_JSON)
                .denyRisks(Risk.EXE)
                .build();

        assertEquals(
                "{\"risks\":{\"allow\":[\"poly/text/json\",\"structured/no-schema/json\"],\"deny\":[\"exe\"]}}",
                options.toJson());
        assertEquals(
                "{\"risks\":{\"deny\":[\"exe\"]}}",
                RequestOptions.builder().denyRisks(Risk.EXE).build().toJson());
    }

    /** Tests that a risk that is both allowed and denied is rejected. */
    @Test
    void testConflictingRisks() {
        RequestOptions.Builder builder =
                RequestOptions.builder().allowRisks(Risk.EXE).denyRisks(Risk.EXE);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);
        assertEquals("Risks cannot be both allowed and denied: [exe]", exception.getMessage());
    }

    /** Tests the image options. */
    @Test
    void testImages() {
        assertEquals(
                "{\"images\":{\"quality\":{\"preserve\":[\"image/png\",\"image/gif\"]}}}",
                RequestOptions.builder()
                        .imagesOptions(new RequestOptions.ImagesOptions(MediaType.IMAGE_PNG, MediaType.IMAGE_GIF))
                        .build()
                        .toJson());
        assertEquals(
                "{\"images\":{}}",
                RequestOptions.builder()
                        .imagesOptions(new RequestOptions.ImagesOptions(new String[0]))
                        .build()
                        .toJson());
    }

    /** Tests the redaction options. */
    @Test
    void testRedactions() {
        assertEquals(
                "{\"redactions\":{\"extension\":\".txt\",\"mime\":\"text/plain\",\"text\":\"Removed\"}}",
                RequestOptions.builder()
                        .redactionOptions(new RequestOptions.RedactionOptions(".txt", MediaType.TEXT_PLAIN, "Removed"))
                        .build()
                        .toJson());
        assertEquals(
                "{\"redactions\":{\"text\":\"Removed\"}}",
                RequestOptions.builder()
                        .redactionOptions(new RequestOptions.RedactionOptions(null, (String) null, "Removed"))
                        .build()
                        .toJson());
    }

    /** Tests the format conversion options. */
    @Test
    void testConversion() {
        RequestOptions.ConversionOptions conversion = RequestOptions.ConversionOptions.builder()
                .from(MediaType.MSWORD)
                .enableMethods(RequestOptions.ConversionMethod.LIBREOFFICE)
                .disableMethods(RequestOptions.ConversionMethod.DEFAULT)
                .enableMediaTypeConversion(MediaType.MSWORD, MediaType.PDF)
                .disableMediaTypeConversion(MediaType.MS_EXCEL, MediaType.PDF)
                .build();

        assertEquals(
                "{\"conversion\":{\"from\":{\"mime-type\":\"application/msword\"},"
                        + "\"methods\":{\"enabled\":[\"libreoffice\"],\"disabled\":[\"default\"]},"
                        + "\"mime_types\":{\"enabled\":[{\"from\":\"application/msword\",\"to\":\"application/pdf\"}],"
                        + "\"disabled\":[{\"from\":\"application/vnd.ms-excel\",\"to\":\"application/pdf\"}]}}}",
                RequestOptions.builder().conversionOptions(conversion).build().toJson());
        assertEquals(
                "{\"conversion\":{}}",
                RequestOptions.builder()
                        .conversionOptions(
                                RequestOptions.ConversionOptions.builder().build())
                        .build()
                        .toJson());
        assertEquals("text-extraction", RequestOptions.ConversionMethod.TEXT_EXTRACTION.toString());
    }

    /** Tests that a conversion method that is both enabled and disabled is rejected. */
    @Test
    void testConflictingConversionMethods() {
        RequestOptions.ConversionOptions.Builder builder = RequestOptions.ConversionOptions.builder()
                .enableMethods(RequestOptions.ConversionMethod.REPORT)
                .disableMethods(RequestOptions.ConversionMethod.REPORT);

        assertThrows(IllegalArgumentException.class, builder::build);
    }

    /** Tests the large file environment option. */
    @Test
    void testLargeFileEnvironment() {
        assertEquals(
                "{\"costs\":{\"environment\":{\"large\":{\"enabled\":true}}}}",
                RequestOptions.builder().largeFileEnvironment(true).build().toJson());
        assertEquals(
                "{\"costs\":{\"environment\":{\"large\":{\"enabled\":false}}}}",
                RequestOptions.builder().largeFileEnvironment(false).build().toJson());
    }

    /** Tests the status options. */
    @Test
    void testStatus() {
        assertEquals(
                "{\"status\":{\"sns\":{\"topicArn\":\"topic\",\"roleArn\":\"role\"},\"userData\":\"mine\"}}",
                RequestOptions.builder()
                        .snsStatus("topic", "role")
                        .userData("mine")
                        .build()
                        .toJson());
        assertEquals(
                "{\"status\":{\"userData\":\"mine\"}}",
                RequestOptions.builder().userData("mine").build().toJson());
        assertEquals(
                "{\"status\":{\"sns\":{\"topicArn\":\"topic\",\"roleArn\":\"role\"}}}",
                RequestOptions.builder().snsStatus("topic", "role").build().toJson());
    }

    /** Tests that the status options are validated. */
    @Test
    void testStatusValidation() {
        RequestOptions.Builder builder = RequestOptions.builder();
        assertThrows(NullPointerException.class, () -> builder.snsStatus(null, "role"));
        assertThrows(NullPointerException.class, () -> builder.snsStatus("topic", null));
        assertThrows(
                IllegalArgumentException.class,
                () -> builder.userData("x".repeat(RequestOptions.MAX_USER_DATA_LENGTH + 1)));
        builder.userData("x".repeat(RequestOptions.MAX_USER_DATA_LENGTH));
        builder.userData(null);
        assertEquals("{}", builder.build().toJson());
    }
}
