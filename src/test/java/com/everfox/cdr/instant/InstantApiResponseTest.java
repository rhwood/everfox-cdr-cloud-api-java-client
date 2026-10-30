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
import static org.mockito.Mockito.*;

import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link InstantApiResponse}. */
class InstantApiResponseTest {

    /** Default constructor for the test class. */
    InstantApiResponseTest() {
        // Nothing to do here
    }

    /**
     * Tests that creating an InstantApiResponse with HTTP code 2xx is successful.
     *
     * @throws InstantApiException if the response is unexpectedly treated as an error
     */
    @Test
    void testResponseIsSuccess() throws InstantApiException {
        @SuppressWarnings("unchecked") // unavoidable when mocking due to HttpResponse being generic
        HttpResponse<byte[]> httpResponse = mock(HttpResponse.class);
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.headers())
                .thenReturn(HttpHeaders.of(
                        Map.of(
                                "Content-Type", List.of("application/json"),
                                "X-Custom-Header", List.of("value1", "value2")),
                        (k, v) -> true));
        InstantApiResponse apiResponse = InstantApiResponse.fromHttpResponse(httpResponse);
        assertEquals(200, apiResponse.getStatusCode());
        assertTrue(apiResponse.isSuccess());
        when(httpResponse.statusCode()).thenReturn(100);
        apiResponse = InstantApiResponse.fromHttpResponse(httpResponse);
        assertEquals(100, apiResponse.getStatusCode());
        assertFalse(apiResponse.isSuccess());
    }

    /**
     * Tests that creating an InstantApiResponse with HTTP code 3xx is neither successful nor an exception.
     *
     * @throws InstantApiException if the response is unexpectedly treated as an error
     */
    @Test
    void testResponseHttpRedirectionIsNotSuccess() throws InstantApiException {
        @SuppressWarnings("unchecked") // unavoidable when mocking due to HttpResponse being generic
        HttpResponse<byte[]> httpResponse = mock(HttpResponse.class);
        when(httpResponse.statusCode()).thenReturn(300);
        when(httpResponse.headers())
                .thenReturn(HttpHeaders.of(
                        Map.of(
                                "Content-Type", List.of("application/json"),
                                "X-Custom-Header", List.of("value1", "value2")),
                        (k, v) -> true));
        InstantApiResponse apiResponse = InstantApiResponse.fromHttpResponse(httpResponse);
        assertEquals(300, apiResponse.getStatusCode());
        assertFalse(apiResponse.isSuccess());
    }

    /**
     * Tests that creating an InstantApiResponse with HTTP code 1xx is neither successful nor an exception.
     *
     * @throws InstantApiException if the response is unexpectedly treated as an error
     */
    @Test
    void testResponseHttpInformationalIsNotSuccess() throws InstantApiException {
        @SuppressWarnings("unchecked") // unavoidable when mocking due to HttpResponse being generic
        HttpResponse<byte[]> httpResponse = mock(HttpResponse.class);
        when(httpResponse.statusCode()).thenReturn(100);
        when(httpResponse.headers())
                .thenReturn(HttpHeaders.of(
                        Map.of(
                                "Content-Type", List.of("application/json"),
                                "X-Custom-Header", List.of("value1", "value2")),
                        (k, v) -> true));
        InstantApiResponse apiResponse = InstantApiResponse.fromHttpResponse(httpResponse);
        apiResponse = InstantApiResponse.fromHttpResponse(httpResponse);
        assertEquals(100, apiResponse.getStatusCode());
        assertFalse(apiResponse.isSuccess());
    }
}
