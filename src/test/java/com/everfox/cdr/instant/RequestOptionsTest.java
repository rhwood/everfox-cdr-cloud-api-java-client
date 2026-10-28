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
        RequestOptions options = new RequestOptions(null, null, null, null, null);

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
        RequestOptions options = new RequestOptions(RequestOptions.ReportFormat.FULL, null, null, null, null);

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
                new RequestOptions.Builder().allowRisks(Risk.STEG_IMAGE_GIF).build();

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
                new RequestOptions.Builder().denyRisks(Risk.STEG_IMAGE_GIF).build();

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
        RequestOptions options = new RequestOptions(null, null, null, Set.of(Risk.STEG_IMAGE_GIF.getRisk()), null);

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
        RequestOptions options = new RequestOptions(null, null, null, null, Set.of(Risk.STEG_IMAGE_GIF.getRisk()));

        String json = options.toJson();
        assertNotNull(json);
        assertEquals("{\"risks\":{\"deny\":[\"steg/image/gif\"]}}", json);
    }

    /** Tests that the {@link RequestOptions#toJson()} method returns the correct JSON when no image options are set. */
    @Test
    void testRequestOptionsNoImagesOptions() {
        RequestOptions.ImagesOptions images = new RequestOptions.ImagesOptions(new String[0]);
        RequestOptions options = new RequestOptions(null, images, null, null, null);

        String json = options.toJson();
        assertNotNull(json);
        assertEquals("{\"images\":{}}", json);
    }

    /** Tests that the {@link RequestOptions#toJson()} method returns the correct JSON when redaction options MIME is a {@link MediaType}. */
    @Test
    void testRequestOptionsRedationOptionsMediaType() {
        RequestOptions options = new RequestOptions.Builder()
                .redactionOptions(new RequestOptions.RedactionOptions("txt", MediaType.TEXT_XML, "Redacted"))
                .build();

        String json = options.toJson();
        assertNotNull(json);
        assertEquals("{\"redactions\":{\"extension\":\"txt\",\"mime\":\"text/xml\",\"text\":\"Redacted\"}}", json);
    }

    /** Tests that the {@link RequestOptions#toJson()} method returns the correct JSON when redaction options MIME is a String. */
    @Test
    void testRequestOptionsRedationOptionsString() {
        RequestOptions options = new RequestOptions.Builder()
                .redactionOptions(new RequestOptions.RedactionOptions("txt", "text/plain", "Redacted"))
                .build();

        String json = options.toJson();
        assertNotNull(json);
        assertEquals("{\"redactions\":{\"extension\":\"txt\",\"mime\":\"text/plain\",\"text\":\"Redacted\"}}", json);
    }

    /**
     * Tests that the {@link RequestOptions#toJson()} method returns the correct JSON when the image quality is set to
     * preserve JPEG and PNG {@link MediaType}s.
     */
    @Test
    void testRequestOptionsPreserveJpegAndPngMediaTypes() {
        RequestOptions.ImagesOptions images =
                new RequestOptions.ImagesOptions(MediaType.IMAGE_JPEG, MediaType.IMAGE_PNG);
        RequestOptions options = new RequestOptions(null, images, null, null, null);

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
        RequestOptions options = new RequestOptions(null, images, null, null, null);

        String json = options.toJson();
        assertNotNull(json);
        assertEquals("{\"images\":{\"quality\":{\"preserve\":[\"image/png\",\"image/jpeg\"]}}}", json);
    }

    /**
     * Tests that the {@link RequestOptions#toJson()} method returns the correct JSON when a given risk is both allowed
     * and denied.
     */
    @Test
    void testRisksAreExclusive() {
        RequestOptions options = new RequestOptions.Builder()
                .allowRisks(Risk.EXE, Risk.POLY)
                .denyRisks(Risk.POLY, Risk.STEG)
                .build();

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
