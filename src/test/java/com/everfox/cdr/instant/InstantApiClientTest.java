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

import com.everfox.cdr.FakeServer;
import com.everfox.cdr.MediaType;
import com.everfox.cdr.Region;
import com.everfox.cdr.Risk;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link InstantApiClient}. */
class InstantApiClientTest {

    /** The body the local server replies with by default. */
    private static final String RESPONSE_BODY = "sanitized";

    /** Local HTTP server standing in for the Instant API. */
    private FakeServer server;

    /** Default constructor for the test class. */
    InstantApiClientTest() {
        // Nothing to do here
    }

    /**
     * Starts a local HTTP server that records requests and replies with {@link #RESPONSE_BODY} and the CDR headers.
     *
     * @throws IOException if the server cannot be started
     */
    @BeforeEach
    void startServer() throws IOException {
        server = FakeServer.start()
                .route(
                        "/",
                        FakeServer.Reply.of(200, RESPONSE_BODY)
                                .withHeader("X-Risks-Taken", "exe")
                                .withHeader("X-Report", "report"));
    }

    /** Stops the local HTTP server. */
    @AfterEach
    void stopServer() {
        server.close();
    }

    /**
     * Creates a client pointed at the local HTTP server.
     *
     * @return the client
     */
    private InstantApiClient createLocalClient() {
        return InstantApiClient.create(
                InstantApiConfig.builder("test-key", server.baseUrl()).build());
    }

    /**
     * Returns the request's Accept header split into its individual media types.
     *
     * @return the accepted media types
     */
    private Set<String> acceptedTypes() {
        return new HashSet<>(
                Arrays.asList(server.lastRequest().headers().getFirst("Accept").split(",\\s*")));
    }

    /** Tests that creating an InstantApiClient with a null configuration throws a NullPointerException. */
    @Test
    void testClientWithNullConfig() {
        assertThrows(NullPointerException.class, () -> {
            InstantApiClient.create(null);
        });
    }

    /** Tests that creating an InstantApiClient with a valid configuration succeeds. */
    @Test
    void testClientCreation() {
        InstantApiConfig config = InstantApiConfig.create("test-key", Region.US_WEST_2);
        InstantApiClient client = InstantApiClient.create(config);

        assertEquals(config, client.getConfig());
    }

    /** Tests that uploading a null request throws a NullPointerException. */
    @Test
    void testUploadNullRequest() {
        try (InstantApiClient client = createLocalClient()) {
            assertThrows(NullPointerException.class, () -> client.upload(null));
        }
    }

    /**
     * Tests that uploading a request without options sends the expected request and returns the response.
     *
     * @throws Exception if the upload fails
     */
    @Test
    void testUploadWithoutOptions() throws Exception {
        byte[] data = "test data".getBytes(StandardCharsets.UTF_8);
        try (InstantApiClient client = createLocalClient()) {
            InstantApiResponse response = client.upload(InstantApiRequest.create(data, MediaType.PDF, MediaType.PDF));

            assertEquals("POST", server.lastRequest().method());
            assertEquals("/upload", server.lastRequest().path());
            assertEquals("test-key", server.lastRequest().headers().getFirst("x-api-key"));
            assertEquals("application/pdf", server.lastRequest().headers().getFirst("Content-Type"));
            assertEquals(Set.of("application/pdf", "application/json"), acceptedTypes());
            assertNull(server.lastRequest().headers().getFirst("X-Options"));
            assertArrayEquals(data, server.lastRequest().body());

            assertEquals(200, response.getStatusCode());
            assertTrue(response.isSuccess());
            assertArrayEquals(RESPONSE_BODY.getBytes(StandardCharsets.UTF_8), response.getBody());
            assertEquals("exe", response.getRisksTaken());
            assertEquals("report", response.getReport());
            assertEquals("exe", response.getHeaders().get("x-risks-taken"));
        }
    }

    /**
     * Tests that uploading a request with options sends the X-Options header.
     *
     * @throws Exception if the upload fails
     */
    @Test
    void testUploadWithOptions() throws Exception {
        RequestOptions options = RequestOptions.builder().denyRisks(Risk.EXE).build();
        try (InstantApiClient client = createLocalClient()) {
            client.upload(InstantApiRequest.create(new byte[] {1, 2, 3}, MediaType.PDF, MediaType.PDF, options));

            assertEquals(options.toJson(), server.lastRequest().headers().getFirst("X-Options"));
        }
    }

    /**
     * Tests that a comma-separated accept type is split and merged with application/json without duplicates.
     *
     * @throws Exception if the upload fails
     */
    @Test
    void testUploadWithMultipleAcceptTypes() throws Exception {
        try (InstantApiClient client = createLocalClient()) {
            client.upload(InstantApiRequest.create(new byte[] {1}, "image/png", "image/png,application/json"));

            assertEquals(Set.of("image/png", "application/json"), acceptedTypes());
        }
    }

    /**
     * Tests that uploading from an input stream sends the stream contents.
     *
     * @throws Exception if the upload fails
     */
    @Test
    void testUploadInputStream() throws Exception {
        byte[] data = "stream data".getBytes(StandardCharsets.UTF_8);
        try (InstantApiClient client = createLocalClient()) {
            InstantApiResponse response = client.upload(new ByteArrayInputStream(data), "text/plain", "text/plain");

            assertArrayEquals(data, server.lastRequest().body());
            assertEquals("text/plain", server.lastRequest().headers().getFirst("Content-Type"));
            assertEquals(Set.of("text/plain", "application/json"), acceptedTypes());
            assertTrue(response.isSuccess());
        }
    }

    /** Tests that an error response from the API is thrown as an {@link InstantApiException}. */
    @Test
    void testUploadErrorResponse() {
        server.route(
                "/",
                FakeServer.Reply.of(
                        400,
                        "{\"error\":{\"code\":1001,\"message\":\"Bad file\",\"name\":\"BadFile\",\"type\":\"client\"}}"));
        try (InstantApiClient client = createLocalClient()) {
            InstantApiException exception = assertThrows(
                    InstantApiException.class,
                    () -> client.upload(InstantApiRequest.create(new byte[] {1}, MediaType.PDF, MediaType.PDF)));

            assertEquals(400, exception.getHttpStatusCode());
            assertEquals(1001, exception.getApiStatusCode());
            assertEquals("Bad file", exception.getApiMessage());
        }
    }
}
