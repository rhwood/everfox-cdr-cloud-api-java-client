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

import com.everfox.cdr.MediaType;
import com.everfox.cdr.Region;
import com.everfox.cdr.Risk;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link InstantApiClient}. */
class InstantApiClientTest {

    /** Local HTTP server standing in for the Instant API. */
    private HttpServer server;
    /** Headers of the most recent request received by the server. */
    private final AtomicReference<Headers> requestHeaders = new AtomicReference<>();
    /** Body of the most recent request received by the server. */
    private final AtomicReference<byte[]> requestBody = new AtomicReference<>();
    /** Path of the most recent request received by the server. */
    private final AtomicReference<String> requestPath = new AtomicReference<>();
    /** Method of the most recent request received by the server. */
    private final AtomicReference<String> requestMethod = new AtomicReference<>();
    /** HTTP status the server responds with. */
    private int responseStatus;
    /** Body the server responds with. */
    private byte[] responseBody;

    /** Default constructor for the test class. */
    InstantApiClientTest() {
        // Nothing to do here
    }

    /**
     * Starts a local HTTP server that records requests and replies with {@link #responseStatus} and
     * {@link #responseBody}.
     *
     * @throws IOException if the server cannot be started
     */
    @BeforeEach
    void startServer() throws IOException {
        responseStatus = 200;
        responseBody = "sanitized".getBytes(StandardCharsets.UTF_8);
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/", exchange -> {
            requestMethod.set(exchange.getRequestMethod());
            requestPath.set(exchange.getRequestURI().getPath());
            requestHeaders.set(exchange.getRequestHeaders());
            requestBody.set(exchange.getRequestBody().readAllBytes());
            exchange.getResponseHeaders().add("X-Risks-Taken", "exe");
            exchange.getResponseHeaders().add("X-Report", "report");
            exchange.sendResponseHeaders(responseStatus, responseBody.length);
            exchange.getResponseBody().write(responseBody);
            exchange.close();
        });
        server.start();
    }

    /** Stops the local HTTP server. */
    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    /**
     * Creates a client pointed at the local HTTP server.
     *
     * @return the client
     */
    private InstantApiClient createLocalClient() {
        URI baseUrl = URI.create("http://" + server.getAddress().getHostString() + ":"
                + server.getAddress().getPort());
        return InstantApiClient.create(InstantApiConfig.create("test-key", baseUrl));
    }

    /**
     * Returns the request's Accept header split into its individual media types.
     *
     * @return the accepted media types
     */
    private Set<String> acceptedTypes() {
        return new HashSet<>(
                Arrays.asList(requestHeaders.get().getFirst("Accept").split(",\\s*")));
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

            assertEquals("POST", requestMethod.get());
            assertEquals("/upload", requestPath.get());
            assertEquals("test-key", requestHeaders.get().getFirst("x-api-key"));
            assertEquals("application/pdf", requestHeaders.get().getFirst("Content-Type"));
            assertEquals(Set.of("application/pdf", "application/json"), acceptedTypes());
            assertNull(requestHeaders.get().getFirst("X-Options"));
            assertArrayEquals(data, requestBody.get());

            assertEquals(200, response.getStatusCode());
            assertTrue(response.isSuccess());
            assertArrayEquals(responseBody, response.getBody());
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

            assertEquals(options.toJson(), requestHeaders.get().getFirst("X-Options"));
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

            assertArrayEquals(data, requestBody.get());
            assertEquals("text/plain", requestHeaders.get().getFirst("Content-Type"));
            assertEquals(Set.of("text/plain", "application/json"), acceptedTypes());
            assertTrue(response.isSuccess());
        }
    }

    /** Tests that an error response from the API is thrown as an {@link InstantApiException}. */
    @Test
    void testUploadErrorResponse() {
        responseStatus = 400;
        responseBody = "{\"error\":{\"code\":1001,\"message\":\"Bad file\",\"name\":\"BadFile\",\"type\":\"client\"}}"
                .getBytes(StandardCharsets.UTF_8);
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
