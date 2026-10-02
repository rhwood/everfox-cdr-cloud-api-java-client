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

import org.junit.jupiter.api.Test;

import com.everfox.cdr.MediaType;
import com.everfox.cdr.Risk;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Set;

class RequestOptionsTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void testNoRequestOptions() {
        RequestOptions options = new RequestOptions(null, null, null, null);

        String json = options.toJson();
        assertNotNull(json);
        assertEquals("{}", json);
    }

    @Test
    void testRequestOptionsFullReport() {
        RequestOptions options = new RequestOptions(RequestOptions.ReportFormat.FULL, null, null, null);

        String json = options.toJson();
        assertNotNull(json);
        assertEquals("{\"report\":{\"format\":\"full\"}}", json);
    }

    @Test
    void testRequestOptionsAllowGifStenography() {
        RequestOptions options = new RequestOptions.Builder()
            .allowRisks(Risk.STEG_IMAGE_GIF)
            .build();

        String json = options.toJson();
        assertNotNull(json);
        assertEquals("{\"risks\":{\"allow\":[\"steg/image/gif\"]}}", json);
    }

    @Test
    void testRequestOptionsNoImagesOptions() {
        RequestOptions.ImagesOptions images = new RequestOptions.ImagesOptions(new String[0]);
        RequestOptions options = new RequestOptions(null, images, null, null);

        String json = options.toJson();
        assertNotNull(json);
        assertEquals("{\"images\":{}}", json);
    }

    @Test
    void testRequestOptionsPreserveJpegAndPngMediaTypes() {
        RequestOptions.ImagesOptions images = new RequestOptions.ImagesOptions(MediaType.IMAGE_JPEG, MediaType.IMAGE_PNG);
        RequestOptions options = new RequestOptions(null, images, null, null);

        String json = options.toJson();
        assertNotNull(json);
        assertEquals("{\"images\":{\"quality\":{\"preserve\":[\"image/png\",\"image/jpeg\"]}}}", json);
    }

    @Test
    void testRequestOptionsPreserveJpegAndPngStrings() {
        RequestOptions.ImagesOptions images = new RequestOptions.ImagesOptions("image/jpeg", "image/png");
        RequestOptions options = new RequestOptions(null, images, null, null);

        String json = options.toJson();
        assertNotNull(json);
        assertEquals("{\"images\":{\"quality\":{\"preserve\":[\"image/png\",\"image/jpeg\"]}}}", json);
    }

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
