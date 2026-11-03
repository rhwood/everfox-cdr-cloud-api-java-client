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
package com.everfox.cdr.internal;

import static org.junit.jupiter.api.Assertions.*;

import com.everfox.cdr.MockHttp;
import com.everfox.cdr.instant.InstantApiConfig;
import com.everfox.cdr.instant.InstantApiException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link HttpSupport}. */
class HttpSupportTest {

    /** The configuration used in tests. */
    private static final InstantApiConfig CONFIG = InstantApiConfig.builder("key", URI.create("https://example.com"))
            .connectTimeout(Duration.ofSeconds(3))
            .requestTimeout(Duration.ofSeconds(7))
            .build();

    /** Default constructor for the test class. */
    HttpSupportTest() {
        // Nothing to do here
    }

    /**
     * Creates a mock HTTP response.
     *
     * @param statusCode the HTTP status code
     * @return the mock response
     */
    private static HttpResponse<byte[]> response(int statusCode) {
        return MockHttp.response(statusCode, "{\"error\":{\"code\":42}}");
    }

    /** Tests that the client uses the connection timeout and never follows redirects. */
    @Test
    void testNewClient() {
        try (HttpClient client = HttpSupport.newClient(CONFIG)) {
            assertEquals(Duration.ofSeconds(3), client.connectTimeout().orElseThrow());
            assertEquals(HttpClient.Redirect.NEVER, client.followRedirects());
        }
    }

    /** Tests that requests use the request timeout. */
    @Test
    void testNewRequest() {
        HttpRequest request = HttpSupport.newRequest(URI.create("https://example.com/x"), CONFIG)
                .build();

        assertEquals(Duration.ofSeconds(7), request.timeout().orElseThrow());
        assertEquals(URI.create("https://example.com/x"), request.uri());
    }

    /**
     * Tests that only status codes of 400 and above throw.
     *
     * @throws InstantApiException if the response is unexpectedly treated as an error
     */
    @Test
    void testThrowIfError() throws InstantApiException {
        HttpSupport.throwIfError(response(200), InstantApiException::create);
        HttpSupport.throwIfError(response(399), InstantApiException::create);

        InstantApiException exception = assertThrows(
                InstantApiException.class, () -> HttpSupport.throwIfError(response(400), InstantApiException::create));
        assertEquals(400, exception.getHttpStatusCode());
        assertEquals(42, exception.getApiStatusCode());
    }
}
