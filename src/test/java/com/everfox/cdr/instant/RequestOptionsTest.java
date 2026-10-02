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

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;

class RequestOptionsTest {

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
        RequestOptions options = new RequestOptions(null, null, Set.of(Risk.STEG_IMAGE_GIF), null);

        String json = options.toJson();
        assertNotNull(json);
        assertEquals("{\"risks\":{\"allow\":[\"steg/image/gif\"]}}", json);
    }

    @Test
    void testRequestOptionsNoImagesOptions() {
        RequestOptions options = new RequestOptions();
        RequestOptions.ImagesOptions images = new RequestOptions.ImagesOptions(new String[0]);
        options.setImages(images);

        String json = options.toJson();
        assertNotNull(json);
        assertEquals("{\"images\":{}}", json);
    }

    @Test
    void testRequestOptionsPreserveJpegAndPngMediaTypes() {
        RequestOptions options = new RequestOptions();
        RequestOptions.ImagesOptions images = new RequestOptions.ImagesOptions(MediaType.IMAGE_JPEG, MediaType.IMAGE_PNG);
        options.setImages(images);

        String json = options.toJson();
        assertNotNull(json);
        assertEquals("{\"images\":{\"quality\":{\"preserve\":[\"image/png\",\"image/jpeg\"]}}}", json);
    }

    @Test
    void testRequestOptionsPreserveJpegAndPngStrings() {
        RequestOptions options = new RequestOptions();
        RequestOptions.ImagesOptions images = new RequestOptions.ImagesOptions("image/jpeg", "image/png");
        options.setImages(images);

        String json = options.toJson();
        assertNotNull(json);
        assertEquals("{\"images\":{\"quality\":{\"preserve\":[\"image/png\",\"image/jpeg\"]}}}", json);
    }
}
