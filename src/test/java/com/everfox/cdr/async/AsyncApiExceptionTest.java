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

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link AsyncApiException} and {@link AsyncApiError}. */
class AsyncApiExceptionTest {

    /** Valid error response body. */
    private static final byte[] VALID_RESPONSE_BODY =
            ("{\"error\":{\"code\":1000,\"message\":\"Error message\",\"name\":\"Error name\","
                            + "\"type\":\"Error type\",\"subType\":\"Error subtype\"}}")
                    .getBytes(StandardCharsets.UTF_8);

    /** Default constructor for the test class. */
    AsyncApiExceptionTest() {
        // Nothing to do here
    }

    /**
     * Asserts that an exception holds the unknown error.
     *
     * @param exception the exception
     */
    private static void assertUnknown(AsyncApiException exception) {
        assertEquals(AsyncApiError.UNKNOWN_CODE, exception.getApiStatusCode());
        assertEquals("Unknown error", exception.getApiMessage());
        assertEquals("Unknown", exception.getName());
        assertEquals("Unknown", exception.getType());
        assertNull(exception.getSubType());
    }

    /** Tests that a valid error body is parsed. */
    @Test
    void testExceptionWithValidBody() {
        AsyncApiException exception = AsyncApiException.create(400, VALID_RESPONSE_BODY);

        assertEquals(1000, exception.getApiStatusCode());
        assertEquals(400, exception.getHttpStatusCode());
        assertEquals("Error message", exception.getApiMessage());
        assertEquals("Error name", exception.getName());
        assertEquals("Error type", exception.getType());
        assertEquals("Error subtype", exception.getSubType());
        assertEquals(1000, exception.getError().getCode());
        assertEquals(
                "API Error: Error message (code: 1000, HTTP status: 400, type: Error type)", exception.getMessage());
    }

    /** Tests that a missing or invalid body results in the unknown error. */
    @Test
    void testExceptionWithInvalidBodies() {
        assertUnknown(AsyncApiException.create(500, (byte[]) null));
        assertUnknown(AsyncApiException.create(500, new byte[0]));
        assertUnknown(AsyncApiException.create(500, "[}".getBytes(StandardCharsets.UTF_8)));
        assertUnknown(AsyncApiException.create(500, "{}".getBytes(StandardCharsets.UTF_8)));
        assertUnknown(AsyncApiException.create(500, "{\"error\":{\"code\":\"x\"}}".getBytes(StandardCharsets.UTF_8)));
        assertUnknown(AsyncApiException.create(500, "<Error/>".getBytes(StandardCharsets.UTF_8)));
    }

    /** Tests that an error with only a code uses defaults for the other fields. */
    @Test
    void testExceptionWithPartialError() {
        AsyncApiException exception =
                AsyncApiException.create(403, "{\"error\":{\"code\":6070}}".getBytes(StandardCharsets.UTF_8));

        assertEquals(6070, exception.getApiStatusCode());
        assertEquals("Unknown error", exception.getApiMessage());
        assertEquals("Unknown", exception.getName());
        assertEquals("Unknown", exception.getType());
    }

    /** Tests creating an exception from an error. */
    @Test
    void testExceptionFromError() {
        AsyncApiError error = AsyncApiError.unknown();
        AsyncApiException exception = AsyncApiException.create(200, error);

        assertSame(error, exception.getError());
        assertEquals(200, exception.getHttpStatusCode());
        assertThrows(NullPointerException.class, () -> AsyncApiException.create(200, (AsyncApiError) null));
    }
}
