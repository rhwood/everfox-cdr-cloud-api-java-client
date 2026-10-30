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

import org.junit.jupiter.api.Test;

/** Unit tests for {@link InstantApiException}. */
class InstantApiExceptionTest {

    /** Minimal valid JSON content for testing */
    private static final byte[] VALID_RESPONSE_BODY =
            "{\"error\": { \"code\": 1000, \"message\": \"Error message\", \"name\": \"Error name\", \"type\": \"Error type\" }}"
                    .getBytes();
    /** Minimal invalid JSON content for testing */
    private static final byte[] NO_ERROR_RESPONSE_BODY = "{}".getBytes();
    /** Minimal invalid JSON object for testing */
    private static final byte[] INVALID_JSON_RESPONSE_BODY = "[}".getBytes();

    /** Default constructor for the test class. */
    InstantApiExceptionTest() {
        // Nothing to do here
    }

    /** Tests that creating an InstantApiException with a null body throws expected unknown error status. */
    @Test
    void testExceptionWithValidBody() {
        InstantApiException exception = InstantApiException.create(1000, VALID_RESPONSE_BODY);
        assertEquals(1000, exception.getApiStatusCode());
        assertEquals(1000, exception.getHttpStatusCode());
        assertEquals("Error message", exception.getApiMessage());
        assertEquals("Error name", exception.getName());
        assertEquals("Error type", exception.getType());
    }

    /** Tests that creating an InstantApiException with a null body throws expected unknown error status. */
    @Test
    void testExceptionWithNullBody() {
        InstantApiException exception = InstantApiException.create(1000, null);
        assertEquals(-1, exception.getApiStatusCode());
        assertEquals(1000, exception.getHttpStatusCode());
        assertEquals("Unknown error", exception.getApiMessage());
        assertEquals("Unknown", exception.getName());
        assertEquals("Unknown", exception.getType());
    }

    /** Tests that creating an InstantApiException with an empty body throws expected unknown error status. */
    @Test
    void testExceptionWithEmptyBody() {
        InstantApiException exception = InstantApiException.create(1000, new byte[0]);
        assertEquals(-1, exception.getApiStatusCode());
        assertEquals(1000, exception.getHttpStatusCode());
        assertEquals("Unknown error", exception.getApiMessage());
        assertEquals("Unknown", exception.getName());
        assertEquals("Unknown", exception.getType());
    }

    /** Tests that creating an InstantApiException with no JSON error object throws expected unknown error status. */
    @Test
    void testExceptionWithNoErrorBody() {
        InstantApiException exception = InstantApiException.create(1000, NO_ERROR_RESPONSE_BODY);
        assertEquals(-1, exception.getApiStatusCode());
        assertEquals(1000, exception.getHttpStatusCode());
        assertEquals("Unknown error", exception.getApiMessage());
        assertEquals("Unknown", exception.getName());
        assertEquals("Unknown", exception.getType());
    }

    /** Tests that creating an InstantApiException with invalid JSON throws expected unknown error status. */
    @Test
    void testExceptionWithInvalidJsonBody() {
        InstantApiException exception = InstantApiException.create(1000, INVALID_JSON_RESPONSE_BODY);
        assertEquals(-1, exception.getApiStatusCode());
        assertEquals(1000, exception.getHttpStatusCode());
        assertEquals("Unknown error", exception.getApiMessage());
        assertEquals("Unknown", exception.getName());
        assertEquals("Unknown", exception.getType());
    }
}
