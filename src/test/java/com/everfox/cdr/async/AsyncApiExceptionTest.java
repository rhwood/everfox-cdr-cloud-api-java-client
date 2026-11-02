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

import com.everfox.cdr.CdrApiError;
import com.everfox.cdr.CdrApiException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link AsyncApiException}. Error parsing is tested in {@code CdrApiErrorTest}. */
class AsyncApiExceptionTest {

    /** Default constructor for the test class. */
    AsyncApiExceptionTest() {
        // Nothing to do here
    }

    /** Tests creating an exception from a response body. */
    @Test
    void testExceptionFromBody() {
        AsyncApiException exception = AsyncApiException.create(
                400,
                "{\"error\":{\"code\":1000,\"message\":\"Error message\",\"type\":\"Error type\"}}"
                        .getBytes(StandardCharsets.UTF_8));

        assertInstanceOf(CdrApiException.class, exception);
        assertEquals(400, exception.getHttpStatusCode());
        assertEquals(1000, exception.getApiStatusCode());
        assertEquals(
                "API Error: Error message (code: 1000, HTTP status: 400, type: Error type)", exception.getMessage());
        assertEquals(
                CdrApiError.UNKNOWN_CODE,
                AsyncApiException.create(500, (byte[]) null).getApiStatusCode());
    }

    /** Tests creating an exception from an error. */
    @Test
    void testExceptionFromError() {
        CdrApiError error = CdrApiError.unknown();
        AsyncApiException exception = AsyncApiException.create(200, error);

        assertSame(error, exception.getError());
        assertEquals(200, exception.getHttpStatusCode());
        assertThrows(NullPointerException.class, () -> AsyncApiException.create(200, (CdrApiError) null));
    }
}
