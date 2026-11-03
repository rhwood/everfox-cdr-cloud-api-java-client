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

import com.everfox.cdr.MockHttp;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link AsyncApiResponse}. */
class AsyncApiResponseTest {

    /** A completed status. */
    private static final AsyncApiStatus STATUS = AsyncApiStatus.fromJson(
            "{\"status\":{\"complete\":true,\"success\":true},\"risks\":{\"taken\":[\"exe\"]}}");

    /** Default constructor for the test class. */
    AsyncApiResponseTest() {
        // Nothing to do here
    }

    /**
     * Creates a mock HTTP response.
     *
     * @param statusCode the HTTP status code
     * @param body the response body
     * @return the mock response
     */
    private static HttpResponse<byte[]> response(int statusCode, byte[] body) {
        return MockHttp.response(statusCode, body, Map.of("X-Custom-Header", List.of("value1", "value2")));
    }

    /**
     * Tests that a 2xx response is successful and exposes its body, headers, and status.
     *
     * @throws AsyncApiException if the response is unexpectedly treated as an error
     */
    @Test
    void testSuccess() throws AsyncApiException {
        byte[] body = {1, 2, 3};
        AsyncApiResponse response = AsyncApiResponse.fromHttpResponse(response(200, body), STATUS);

        assertEquals(200, response.getStatusCode());
        assertTrue(response.isSuccess());
        assertArrayEquals(body, response.getBody());
        assertNotSame(response.getBody(), response.getBody());
        assertEquals("value1, value2", response.getHeaders().get("x-custom-header"));
        assertSame(STATUS, response.getStatus());
        assertEquals(List.of("exe"), response.getRisksTaken());
    }

    /**
     * Tests that 1xx and 3xx responses are neither successful nor errors, and a null body is empty.
     *
     * @throws AsyncApiException if the response is unexpectedly treated as an error
     */
    @Test
    void testNotSuccess() throws AsyncApiException {
        assertFalse(
                AsyncApiResponse.fromHttpResponse(response(100, null), STATUS).isSuccess());
        AsyncApiResponse redirect = AsyncApiResponse.fromHttpResponse(response(302, null), STATUS);
        assertFalse(redirect.isSuccess());
        assertArrayEquals(new byte[0], redirect.getBody());
    }

    /** Tests that an error response is thrown as an {@link AsyncApiException}. */
    @Test
    void testError() {
        AsyncApiException exception = assertThrows(
                AsyncApiException.class, () -> AsyncApiResponse.fromHttpResponse(response(410, null), STATUS));

        assertEquals(410, exception.getHttpStatusCode());
    }
}
