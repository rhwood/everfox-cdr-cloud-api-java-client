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
import org.junit.jupiter.api.Test;

/** Unit tests for {@link AsyncApiRequest}. */
class AsyncApiRequestTest {

    /** Default constructor for the test class. */
    AsyncApiRequestTest() {
        // Nothing to do here
    }

    /** Tests that a request without options serializes only the media type. */
    @Test
    void testWithoutOptions() {
        AsyncApiRequest request = AsyncApiRequest.create(MediaType.PDF);

        assertEquals("application/pdf", request.getContentType());
        assertNull(request.getOptions());
        assertEquals("{\"mime-type\":\"application/pdf\"}", request.toJson());
        assertEquals(request.toJson(), AsyncApiRequest.create("application/pdf").toJson());
    }

    /** Tests that a request with options serializes them under the options key. */
    @Test
    void testWithOptions() {
        RequestOptions options = RequestOptions.builder()
                .reportFormat(RequestOptions.ReportFormat.NONE)
                .build();
        AsyncApiRequest request = AsyncApiRequest.create(MediaType.OCTET_STREAM, options);

        assertSame(options, request.getOptions());
        assertEquals(
                "{\"mime-type\":\"application/octet-stream\",\"options\":{\"report\":{\"format\":\"none\"}}}",
                request.toJson());
        assertEquals(
                request.toJson(),
                AsyncApiRequest.create("application/octet-stream", options).toJson());
    }

    /** Tests that a null content type throws a NullPointerException. */
    @Test
    void testNullContentType() {
        assertThrows(NullPointerException.class, () -> AsyncApiRequest.create((MediaType) null));
        assertThrows(NullPointerException.class, () -> AsyncApiRequest.create((String) null));
    }
}
