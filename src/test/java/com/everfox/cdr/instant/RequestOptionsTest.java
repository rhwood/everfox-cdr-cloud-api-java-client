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

import static org.junit.jupiter.api.Assertions.*;

import com.everfox.cdr.MediaType;
import com.everfox.cdr.Risk;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** Unit tests for the {@link RequestOptions} class. */
class RequestOptionsTest {

    /** Object mapper for parsing JSON. */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Default constructor. */
    RequestOptionsTest() {
        // Nothing to do
    }

    /** Tests that the {@link RequestOptions#toJson()} method returns an empty JSON object when no options are set. */
    @Test
    void testNoRequestOptions() {
        RequestOptions options = new RequestOptions(null, null, null, null, null, null);

        String json = options.toJson();
        assertNotNull(json);
        assertEquals("{}", json);
    }

    /**
     * Tests that the {@link RequestOptions#toJson()} method returns the correct JSON when the report format is set to
     * FULL.
     */
    @Test
    void testRequestOptionsFullReport() {
        RequestOptions options = RequestOptions.builder()
                .reportFormat(RequestOptions.ReportFormat.FULL)
                .build();

        String json = options.toJson();
        assertNotNull(json);
        assertEquals("{\"report\":{\"format\":\"full\"}}", json);
    }

    /**
     * Tests that the {@link RequestOptions#toJson()} method returns the correct JSON when the GIF image stenography
     * risk is allowed using Builder.
     */
    @Test
    void testRequestOptionsBuilderAllowGifStenography() {
        RequestOptions options =
                RequestOptions.builder().allowRisks(Risk.STEG_IMAGE_GIF).build();

        String json = options.toJson();
        assertNotNull(json);
        assertEquals("{\"risks\":{\"allow\":[\"steg/image/gif\"]}}", json);
    }

    /**
     * Tests that the {@link RequestOptions#toJson()} method returns the correct JSON when the GIF image stenography
     * risk is denied using Builder.
     */
    @Test
    void testRequestOptionsBuilderDenyGifStenography() {
        RequestOptions options =
                RequestOptions.builder().denyRisks(Risk.STEG_IMAGE_GIF).build();

        String json = options.toJson();
        assertNotNull(json);
        assertEquals("{\"risks\":{\"deny\":[\"steg/image/gif\"]}}", json);
    }

    /**
     * Tests that the {@link RequestOptions#toJson()} method returns the correct JSON when the GIF image stenography
     * risk is allowed using constructor.
     */
    @Test
    void testRequestOptionsConstructorAllowGifStenography() {
        RequestOptions options =
                new RequestOptions(null, null, null, null, Set.of(Risk.STEG_IMAGE_GIF.getRisk()), null);

        String json = options.toJson();
        assertNotNull(json);
        assertEquals("{\"risks\":{\"allow\":[\"steg/image/gif\"]}}", json);
    }

    /**
     * Tests that the {@link RequestOptions#toJson()} method returns the correct JSON when the GIF image stenography
     * risk is denied using constructor.
     */
    @Test
    void testRequestOptionsConstructorDenyGifStenography() {
        RequestOptions options =
                new RequestOptions(null, null, null, null, null, Set.of(Risk.STEG_IMAGE_GIF.getRisk()));

        String json = options.toJson();
        assertNotNull(json);
        assertEquals("{\"risks\":{\"deny\":[\"steg/image/gif\"]}}", json);
    }

    /** Tests that the {@link RequestOptions#toJson()} method returns the correct JSON when no image options are set. */
    @Test
    void testRequestOptionsNoImagesOptions() {
        RequestOptions.ImagesOptions images = new RequestOptions.ImagesOptions(new String[0]);
        RequestOptions options = RequestOptions.builder().imagesOptions(images).build();

        String json = options.toJson();
        assertNotNull(json);
        assertEquals("{\"images\":{}}", json);
    }

    /**
     * Tests that the {@link RequestOptions#toJson()} method returns the correct JSON when redaction options MIME is a
     * {@link MediaType}.
     */
    @Test
    void testRequestOptionsRedationOptionsMediaType() {
        RequestOptions options = RequestOptions.builder()
                .redactionOptions(new RequestOptions.RedactionOptions(".txt", MediaType.TEXT_XML, "Redacted"))
                .build();

        String json = options.toJson();
        assertNotNull(json);
        assertEquals("{\"redactions\":{\"extension\":\".txt\",\"mime\":\"text/xml\",\"text\":\"Redacted\"}}", json);
    }

    /**
     * Tests that the {@link RequestOptions#toJson()} method returns the correct JSON when redaction options MIME is a
     * String.
     */
    @Test
    void testRequestOptionsRedationOptionsString() {
        RequestOptions options = RequestOptions.builder()
                .redactionOptions(new RequestOptions.RedactionOptions(".txt", "text/plain", "Redacted"))
                .build();

        String json = options.toJson();
        assertNotNull(json);
        assertEquals("{\"redactions\":{\"extension\":\".txt\",\"mime\":\"text/plain\",\"text\":\"Redacted\"}}", json);
    }

    /** Tests that the {@link RequestOptions#toJson()} method omits the redaction options extension when it is null. */
    @Test
    void testRequestOptionsRedationOptionsNullExtension() {
        RequestOptions options = RequestOptions.builder()
                .redactionOptions(new RequestOptions.RedactionOptions(null, "text/plain", "Redacted"))
                .build();

        String json = options.toJson();
        assertNotNull(json);
        assertEquals("{\"redactions\":{\"mime\":\"text/plain\",\"text\":\"Redacted\"}}", json);
    }

    /**
     * Tests that the {@link RequestOptions#toJson()} method returns the correct JSON when the image quality is set to
     * preserve JPEG and PNG {@link MediaType}s.
     */
    @Test
    void testRequestOptionsPreserveJpegAndPngMediaTypes() {
        RequestOptions.ImagesOptions images =
                new RequestOptions.ImagesOptions(MediaType.IMAGE_JPEG, MediaType.IMAGE_PNG);
        RequestOptions options = new RequestOptions(null, null, images, null, null, null);

        String json = options.toJson();
        assertNotNull(json);
        assertEquals("{\"images\":{\"quality\":{\"preserve\":[\"image/png\",\"image/jpeg\"]}}}", json);
    }

    /**
     * Tests that the {@link RequestOptions#toJson()} method returns the correct JSON when the image quality is set to
     * preserve JPEG and PNG strings.
     */
    @Test
    void testRequestOptionsPreserveJpegAndPngStrings() {
        RequestOptions.ImagesOptions images = new RequestOptions.ImagesOptions("image/jpeg", "image/png");
        RequestOptions options = new RequestOptions(null, null, images, null, null, null);

        String json = options.toJson();
        assertNotNull(json);
        assertEquals("{\"images\":{\"quality\":{\"preserve\":[\"image/png\",\"image/jpeg\"]}}}", json);
    }

    /**
     * Tests that the {@link RequestOptions#toJson()} method returns the correct JSON when an empty set of format
     * conversion options is set.
     */
    @Test
    void testRequestOptionsEmptyConversionOptions() {
        RequestOptions options = RequestOptions.builder()
                .conversionOptions(RequestOptions.ConversionOptions.builder().build())
                .build();

        String json = options.toJson();
        assertNotNull(json);
        assertEquals("{\"conversion\":{}}", json);
    }

    /**
     * Tests that the {@link RequestOptions#toJson()} method returns the correct JSON when format conversion options are
     * constructed with null values.
     */
    @Test
    void testRequestOptionsConversionOptionsConstructorNulls() {
        RequestOptions.ConversionOptions conversion =
                new RequestOptions.ConversionOptions(null, null, null, null, null);
        RequestOptions options = new RequestOptions(null, conversion, null, null, null, null);

        String json = options.toJson();
        assertNotNull(json);
        assertEquals("{\"conversion\":{}}", json);
    }

    /**
     * Tests that the {@link RequestOptions#toJson()} method returns the correct JSON when the original file format for
     * conversion is a {@link MediaType}.
     */
    @Test
    void testRequestOptionsConversionFromMediaType() {
        RequestOptions options = RequestOptions.builder()
                .conversionOptions(RequestOptions.ConversionOptions.builder()
                        .from(MediaType.MSWORD)
                        .build())
                .build();

        String json = options.toJson();
        assertNotNull(json);
        assertEquals("{\"conversion\":{\"from\":{\"mime-type\":\"application/msword\"}}}", json);
    }

    /**
     * Tests that the {@link RequestOptions#toJson()} method returns the correct JSON when the original file format for
     * conversion is a String.
     */
    @Test
    void testRequestOptionsConversionFromString() {
        RequestOptions options = RequestOptions.builder()
                .conversionOptions(RequestOptions.ConversionOptions.builder()
                        .from("application/vnd.ms-excel")
                        .build())
                .build();

        String json = options.toJson();
        assertNotNull(json);
        assertEquals("{\"conversion\":{\"from\":{\"mime-type\":\"application/vnd.ms-excel\"}}}", json);
    }

    /**
     * Tests that the {@link RequestOptions#toJson()} method returns the correct JSON when conversion methods are
     * enabled, preserving the order of priority.
     */
    @Test
    void testRequestOptionsConversionEnableMethods() {
        RequestOptions options = RequestOptions.builder()
                .conversionOptions(RequestOptions.ConversionOptions.builder()
                        .enableMethods(
                                RequestOptions.ConversionMethod.TEXT_EXTRACTION, RequestOptions.ConversionMethod.REPORT)
                        .build())
                .build();

        String json = options.toJson();
        assertNotNull(json);
        assertEquals("{\"conversion\":{\"methods\":{\"enabled\":[\"text-extraction\",\"report\"]}}}", json);
    }

    /**
     * Tests that the {@link RequestOptions#toJson()} method returns the correct JSON when conversion methods are both
     * enabled and disabled.
     */
    @Test
    void testRequestOptionsConversionEnableAndDisableMethods() {
        RequestOptions options = RequestOptions.builder()
                .conversionOptions(RequestOptions.ConversionOptions.builder()
                        .disableMethods(RequestOptions.ConversionMethod.DEFAULT)
                        .enableMethods(RequestOptions.ConversionMethod.LIBREOFFICE)
                        .build())
                .build();

        String json = options.toJson();
        assertNotNull(json);
        assertEquals("{\"conversion\":{\"methods\":{\"enabled\":[\"libreoffice\"],\"disabled\":[\"default\"]}}}", json);
    }

    /**
     * Tests that the {@link RequestOptions#toJson()} method returns the correct JSON when media type conversions are
     * enabled using {@link MediaType}s.
     */
    @Test
    void testRequestOptionsConversionEnableMediaTypeConversionMediaTypes() {
        RequestOptions options = RequestOptions.builder()
                .conversionOptions(RequestOptions.ConversionOptions.builder()
                        .enableMediaTypeConversion(MediaType.IMAGE_BMP, MediaType.IMAGE_PNG)
                        .enableMediaTypeConversion(MediaType.IMAGE_TIFF, MediaType.IMAGE_PNG)
                        .build())
                .build();

        String json = options.toJson();
        assertNotNull(json);
        assertEquals(
                "{\"conversion\":{\"mime_types\":{\"enabled\":[{\"from\":\"image/bmp\",\"to\":\"image/png\"},{\"from\":\"image/tiff\",\"to\":\"image/png\"}]}}}",
                json);
    }

    /**
     * Tests that the {@link RequestOptions#toJson()} method returns the correct JSON when media type conversions are
     * disabled using Strings.
     */
    @Test
    void testRequestOptionsConversionDisableMediaTypeConversionStrings() {
        RequestOptions options = RequestOptions.builder()
                .conversionOptions(RequestOptions.ConversionOptions.builder()
                        .disableMediaTypeConversion("application/msword", "application/pdf")
                        .build())
                .build();

        String json = options.toJson();
        assertNotNull(json);
        assertEquals(
                "{\"conversion\":{\"mime_types\":{\"disabled\":[{\"from\":\"application/msword\",\"to\":\"application/pdf\"}]}}}",
                json);
    }

    /** Tests that the {@link RequestOptions.ConversionMethod} string representation matches the API value. */
    @Test
    void testConversionMethodToString() {
        assertEquals("text-extraction", RequestOptions.ConversionMethod.TEXT_EXTRACTION.toString());
    }

    /**
     * Tests that the {@link RequestOptions.ConversionOptions.Builder#build()} method throws an exception when a given
     * conversion method is both enabled and disabled.
     */
    @Test
    void testConversionOptionsBuilderRejectsEnabledAndDisabledMethod() {
        RequestOptions.ConversionOptions.Builder builder = RequestOptions.ConversionOptions.builder()
                .enableMethods(RequestOptions.ConversionMethod.TEXT_EXTRACTION, RequestOptions.ConversionMethod.REPORT)
                .disableMethods(RequestOptions.ConversionMethod.REPORT);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);

        assertEquals("Conversion methods cannot be both enabled and disabled: [report]", exception.getMessage());
    }

    /**
     * Tests that the {@link RequestOptions.ConversionOptions} constructor gives precedence to disabled methods when a
     * given conversion method is both enabled and disabled.
     */
    @Test
    void testConversionOptionsConstructorDisabledMethodsTakePrecedence() {
        RequestOptions.ConversionOptions conversion = new RequestOptions.ConversionOptions(
                null, Set.of("text-extraction", "report"), Set.of("report"), null, null);
        RequestOptions options = new RequestOptions(null, conversion, null, null, null, null);

        String json = options.toJson();
        assertNotNull(json);
        assertEquals(
                "{\"conversion\":{\"methods\":{\"enabled\":[\"text-extraction\"],\"disabled\":[\"report\"]}}}", json);
    }

    /**
     * Tests that the {@link RequestOptions.Builder#build()} method throws an exception when a given risk is both
     * allowed and denied.
     */
    @Test
    void testRequestOptionsBuilderRejectsAllowedAndDeniedRisk() {
        RequestOptions.Builder builder =
                RequestOptions.builder().allowRisks(Risk.EXE, Risk.POLY).denyRisks(Risk.POLY, Risk.STEG);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);

        assertEquals("Risks cannot be both allowed and denied: [poly]", exception.getMessage());
    }

    /**
     * Tests that the {@link RequestOptions.Builder#build()} method only rejects risks that are both allowed and denied
     * in the final state of the builder, since {@link RequestOptions.Builder#allowRisks(Risk...)} replaces previously
     * allowed risks.
     */
    @Test
    void testRequestOptionsBuilderAcceptsReplacedAllowedRisk() {
        RequestOptions options = RequestOptions.builder()
                .allowRisks(Risk.POLY)
                .denyRisks(Risk.POLY)
                .allowRisks(Risk.EXE)
                .build();

        String json = options.toJson();
        assertNotNull(json);
        assertEquals(MAPPER.readTree("{\"risks\":{\"allow\":[\"exe\"],\"deny\":[\"poly\"]}}"), MAPPER.readTree(json));
    }

    /**
     * Tests that the {@link RequestOptions#toJson()} method returns the correct JSON when a given risk is both allowed
     * and denied, bypassing the builder.
     */
    @Test
    void testRisksAreExclusive() {
        RequestOptions options =
                new RequestOptions(null, null, null, null, Set.of("exe", "poly"), Set.of("poly", "steg"));

        JsonNode json = MAPPER.readTree(options.toJson());

        assertNotNull(json);
        JsonNode risks = json.get("risks");
        assertTrue(risks.get("allow").isArray());
        assertTrue(risks.get("deny").isArray());
        List<String> allow = MAPPER.readerForListOf(String.class).readValue(risks.get("allow"));
        List<String> deny = MAPPER.readerForListOf(String.class).readValue(risks.get("deny"));
        assertTrue(allow.contains("exe"));
        assertFalse(allow.contains("poly"));
        assertFalse(allow.contains("steg"));
        assertFalse(deny.contains("exe"));
        assertTrue(deny.contains("poly"));
        assertTrue(deny.contains("steg"));
    }
}
