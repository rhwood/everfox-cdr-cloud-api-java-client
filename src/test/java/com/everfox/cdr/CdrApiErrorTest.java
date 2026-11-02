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

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link CdrApiError} and {@link CdrApiException}. */
class CdrApiErrorTest {

    /** A complete error response body. */
    private static final String VALID_BODY = "{\"error\":{\"code\":1000,\"message\":\"Error message\","
            + "\"name\":\"Error name\",\"type\":\"Error type\",\"subType\":\"Error subtype\"}}";

    /** Minimal concrete exception for testing the abstract {@link CdrApiException}. */
    private static final class TestException extends CdrApiException {

        /** The serialization version. */
        private static final long serialVersionUID = 1L;

        /**
         * Creates a new exception.
         *
         * @param httpStatusCode the HTTP status code
         * @param error the error
         */
        TestException(int httpStatusCode, CdrApiError error) {
            super(httpStatusCode, error);
        }
    }

    /** Default constructor for the test class. */
    CdrApiErrorTest() {
        // Nothing to do here
    }

    /**
     * Parses an error from a string.
     *
     * @param json the JSON, or null
     * @return the error
     */
    private static CdrApiError parse(String json) {
        return CdrApiError.fromJson(json != null ? json.getBytes(StandardCharsets.UTF_8) : null);
    }

    /**
     * Asserts that an error is the unknown error.
     *
     * @param error the error
     */
    private static void assertUnknown(CdrApiError error) {
        assertEquals(CdrApiError.UNKNOWN_CODE, error.getCode());
        assertEquals("Unknown error", error.getMessage());
        assertEquals("Unknown", error.getName());
        assertEquals("Unknown", error.getType());
        assertNull(error.getSubType());
    }

    /** Tests that a complete error is parsed. */
    @Test
    void testValidError() {
        CdrApiError error = parse(VALID_BODY);

        assertEquals(1000, error.getCode());
        assertEquals("Error message", error.getMessage());
        assertEquals("Error name", error.getName());
        assertEquals("Error type", error.getType());
        assertEquals("Error subtype", error.getSubType());
    }

    /** Tests that a missing or invalid body results in the unknown error. */
    @Test
    void testInvalidBodies() {
        assertUnknown(parse(null));
        assertUnknown(parse(""));
        assertUnknown(parse("[}"));
        assertUnknown(parse("[]"));
        assertUnknown(parse("{}"));
        assertUnknown(parse("{\"error\":\"oops\"}"));
        assertUnknown(parse("{\"error\":{\"code\":\"x\"}}"));
        assertUnknown(parse("<Error/>"));
        assertUnknown(CdrApiError.unknown());
    }

    /** Tests that an error with only a code uses defaults for the other fields. */
    @Test
    void testPartialError() {
        CdrApiError error = parse("{\"error\":{\"code\":6070,\"name\":7}}");

        assertEquals(6070, error.getCode());
        assertEquals("Unknown error", error.getMessage());
        assertEquals("Unknown", error.getName());
        assertEquals("Unknown", error.getType());
        assertNull(error.getSubType());
    }

    /** Tests that the exception exposes the error and formats its message. */
    @Test
    void testException() {
        CdrApiError error = parse(VALID_BODY);
        CdrApiException exception = new TestException(400, error);

        assertSame(error, exception.getError());
        assertEquals(400, exception.getHttpStatusCode());
        assertEquals(1000, exception.getApiStatusCode());
        assertEquals("Error message", exception.getApiMessage());
        assertEquals("Error name", exception.getName());
        assertEquals("Error type", exception.getType());
        assertEquals("Error subtype", exception.getSubType());
        assertEquals(
                "API Error: Error message (code: 1000, HTTP status: 400, type: Error type)", exception.getMessage());
    }
}
