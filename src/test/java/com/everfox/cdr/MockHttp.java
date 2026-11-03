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
package com.everfox.cdr;

import static org.mockito.Mockito.*;

import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/** Creates mock HTTP responses for unit tests of response and error handling. */
public final class MockHttp {

    /** Not instantiable. */
    private MockHttp() {}

    /**
     * Creates a mock HTTP response.
     *
     * @param statusCode the HTTP status code
     * @param body the response body, which may be null
     * @param headers the response headers
     * @return the mock response
     */
    public static HttpResponse<byte[]> response(int statusCode, byte[] body, Map<String, List<String>> headers) {
        @SuppressWarnings("unchecked") // unavoidable when mocking due to HttpResponse being generic
        HttpResponse<byte[]> httpResponse = mock(HttpResponse.class);
        when(httpResponse.statusCode()).thenReturn(statusCode);
        when(httpResponse.body()).thenReturn(body);
        when(httpResponse.headers()).thenReturn(HttpHeaders.of(headers, (name, value) -> true));
        return httpResponse;
    }

    /**
     * Creates a mock HTTP response with a text body.
     *
     * @param statusCode the HTTP status code
     * @param body the response body as UTF-8 text, which may be null
     * @param headers the response headers
     * @return the mock response
     */
    public static HttpResponse<byte[]> response(int statusCode, String body, Map<String, List<String>> headers) {
        return response(statusCode, body != null ? body.getBytes(StandardCharsets.UTF_8) : null, headers);
    }

    /**
     * Creates a mock HTTP response with a text body and no headers.
     *
     * @param statusCode the HTTP status code
     * @param body the response body as UTF-8 text, which may be null
     * @return the mock response
     */
    public static HttpResponse<byte[]> response(int statusCode, String body) {
        return response(statusCode, body, Map.of());
    }
}
