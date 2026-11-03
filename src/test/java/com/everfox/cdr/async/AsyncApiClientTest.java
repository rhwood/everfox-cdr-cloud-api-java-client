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
import com.everfox.cdr.FakeServer;
import com.everfox.cdr.MediaType;
import com.everfox.cdr.Region;
import com.everfox.cdr.Risk;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** Unit tests for {@link AsyncApiClient}. */
class AsyncApiClientTest {

    /** Object mapper for parsing request bodies. */
    private static final ObjectMapper MAPPER = new ObjectMapper();
    /** The processed file returned by the simulated storage service. */
    private static final byte[] RESULT = "sanitized".getBytes(StandardCharsets.UTF_8);
    /** The file uploaded in tests. */
    private static final byte[] FILE = "unsafe".getBytes(StandardCharsets.UTF_8);

    /** Local HTTP server standing in for the Async API and its storage service. */
    private FakeServer server;
    /** Temporary directory for file tests. */
    @TempDir
    Path tempDir;

    /** Default constructor for the test class. */
    AsyncApiClientTest() {
        // Nothing to do here
    }

    /**
     * Starts a local HTTP server that simulates the Async API.
     *
     * @throws IOException if the server cannot be started
     */
    @BeforeEach
    void startServer() throws IOException {
        server = FakeServer.start();
        server.route("/v1/initiate", reply(202, waitingForUpload(), "e0"))
                .route("/upload/", reply(200, "", null))
                .route("/status/", reply(200, complete(true), "e1"))
                .route("/download/", reply(302, "", null).withHeader("Location", "/storage/1?X-Amz-Signature=abc"))
                .route("/storage/", new FakeServer.Reply(200, RESULT, Map.of()))
                .route("/loop/", reply(302, "", null).withHeader("Location", "/loop/1"));
    }

    /** Stops the local HTTP server. */
    @AfterEach
    void stopServer() {
        server.close();
    }

    /**
     * Creates a reply.
     *
     * @param status the HTTP status
     * @param body the response body
     * @param etag the ETag header, or null
     * @return the reply
     */
    private static FakeServer.Reply reply(int status, String body, String etag) {
        FakeServer.Reply reply = FakeServer.Reply.of(status, body);
        return etag != null ? reply.withHeader("ETag", etag) : reply;
    }

    /**
     * Returns the base URL of the local server.
     *
     * @return the base URL
     */
    private String base() {
        return server.baseUrl().toString();
    }

    /**
     * Returns the JSON for a link to the local server.
     *
     * @param path the path
     * @param method the HTTP method
     * @return the link JSON
     */
    private String link(String path, String method) {
        return "{\"url\":\"" + base() + path + "\",\"method\":\"" + method
                + "\",\"headers\":{\"Authorization\":\"sigv4\",\"Host\":\"ignored\"}}";
    }

    /**
     * Returns the status JSON of a job waiting for upload.
     *
     * @return the status JSON
     */
    private String waitingForUpload() {
        return "{\"id\":\"data/1.pdf\",\"status\":{\"state\":\"WAITING_FOR_UPLOAD\",\"complete\":false},"
                + "\"links\":{\"upload\":{\"url\":\"" + base() + "/upload/1\",\"method\":\"PUT\","
                + "\"headers\":{\"Content-Type\":\"application/pdf\",\"Content-Length\":\"6\"}},"
                + "\"status\":" + link("/status/1", "GET") + "}}";
    }

    /**
     * Returns the status JSON of a job being processed.
     *
     * @return the status JSON
     */
    private String processing() {
        return "{\"id\":\"data/1.pdf\",\"status\":{\"state\":\"PROCESSING\",\"complete\":false},"
                + "\"links\":{\"status\":" + link("/status/1", "GET") + "}}";
    }

    /**
     * Returns the status JSON of a completed job.
     *
     * @param success whether processing succeeded
     * @return the status JSON
     */
    private String complete(boolean success) {
        if (success) {
            return "{\"id\":\"data/1.pdf\",\"status\":{\"state\":\"COMPLETE\",\"complete\":true,\"success\":true},"
                    + "\"risks\":{\"taken\":[\"exe/macro/ms/word\"]},"
                    + "\"links\":{\"result\":" + link("/download/1", "GET") + "}}";
        }
        return "{\"id\":\"data/1.pdf\",\"status\":{\"state\":\"COMPLETE\",\"complete\":true,\"success\":false},"
                + "\"error\":{\"code\":3020,\"name\":\"PROCESSING_NOT_RECOGNISED\",\"message\":\"Not a PDF\","
                + "\"type\":\"PROCESSING\",\"subType\":\"Parsing/NotRecognised\"},\"links\":{}}";
    }

    /**
     * Creates a client pointed at the local server.
     *
     * @return the client
     */
    private AsyncApiClient createLocalClient() {
        return createLocalClient(AsyncApiConfig.DEFAULT_PROCESSING_TIMEOUT);
    }

    /**
     * Creates a client pointed at the local server.
     *
     * <p>The client polls every 10 ms so that tests don't wait for the default poll interval.
     *
     * @param processingTimeout the processing timeout
     * @return the client
     */
    private AsyncApiClient createLocalClient(Duration processingTimeout) {
        return AsyncApiClient.create(AsyncApiConfig.builder("test-key", URI.create(base() + "/v1"))
                .connectTimeout(Duration.ofSeconds(5))
                .requestTimeout(Duration.ofSeconds(5))
                .pollInterval(Duration.ofMillis(10))
                .processingTimeout(processingTimeout)
                .build());
    }

    /**
     * Returns the requests received for a path prefix.
     *
     * @param prefix the path prefix
     * @return the matching requests
     */
    private List<FakeServer.Request> receivedFor(String prefix) {
        return server.requests(prefix);
    }

    /** Tests that creating a client with a null configuration throws a NullPointerException. */
    @Test
    void testClientWithNullConfig() {
        assertThrows(NullPointerException.class, () -> AsyncApiClient.create(null));
    }

    /** Tests that the client exposes its configuration. */
    @Test
    void testClientCreation() {
        AsyncApiConfig config = AsyncApiConfig.create("test-key", Region.US_WEST_2);
        try (AsyncApiClient client = AsyncApiClient.create(config)) {
            assertEquals(config, client.getConfig());
        }
    }

    /** Tests that null arguments throw a NullPointerException. */
    @Test
    void testNullArguments() {
        try (AsyncApiClient client = createLocalClient()) {
            AsyncApiStatus status = AsyncApiStatus.fromJson(waitingForUpload());
            assertThrows(NullPointerException.class, () -> client.initiate(null));
            assertThrows(NullPointerException.class, () -> client.upload(null, FILE));
            assertThrows(NullPointerException.class, () -> client.upload(status, (byte[]) null));
            assertThrows(NullPointerException.class, () -> client.upload(status, (Path) null));
            assertThrows(NullPointerException.class, () -> client.upload(status, (InputStream) null));
            assertThrows(NullPointerException.class, () -> client.getStatus(null));
            assertThrows(NullPointerException.class, () -> client.waitForCompletion(null));
            assertThrows(NullPointerException.class, () -> client.download(null));
            assertThrows(NullPointerException.class, () -> client.download(status, null));
            assertThrows(NullPointerException.class, () -> client.process(AsyncApiRequest.create("a/b"), null));
            assertThrows(NullPointerException.class, () -> client.process(null, "a/b"));
            assertThrows(
                    NullPointerException.class, () -> client.process(AsyncApiRequest.create("a/b"), null, tempDir));
            assertThrows(
                    NullPointerException.class, () -> client.process(AsyncApiRequest.create("a/b"), tempDir, null));
        }
    }

    /**
     * Tests that initiating sends the API key and JSON body and returns the parsed status.
     *
     * @throws Exception if the request fails
     */
    @Test
    void testInitiate() throws Exception {
        RequestOptions options = RequestOptions.builder().allowRisks(Risk.EXE).build();
        try (AsyncApiClient client = createLocalClient()) {
            AsyncApiStatus status = client.initiate(AsyncApiRequest.create(MediaType.PDF, options));

            FakeServer.Request request = server.requests().get(0);
            assertEquals("POST", request.method());
            assertEquals("test-key", request.headers().getFirst("x-api-key"));
            assertEquals("application/json", request.headers().getFirst("Content-Type"));
            assertEquals("application/json", request.headers().getFirst("Accept"));
            JsonNode body = MAPPER.readTree(request.body());
            assertEquals("application/pdf", body.get("mime-type").stringValue());
            assertEquals("exe", body.at("/options/risks/allow/0").stringValue());

            assertEquals("WAITING_FOR_UPLOAD", status.getState());
            assertEquals("e0", status.getEtag());
            assertNotNull(status.getUploadLink());
        }
    }

    /** Tests that an initiate error is thrown as an {@link AsyncApiException}. */
    @Test
    void testInitiateError() {
        server.route(
                "/v1/initiate",
                reply(
                        400,
                        "{\"error\":{\"code\":6010,\"message\":\"Bad type\",\"name\":\"APIGATEWAY_BAD_REQUEST_BODY\","
                                + "\"type\":\"BadRequest\",\"subType\":\"ValidationFailure\"}}",
                        null));
        try (AsyncApiClient client = createLocalClient()) {
            AsyncApiException exception =
                    assertThrows(AsyncApiException.class, () -> client.initiate(AsyncApiRequest.create("bad/type")));

            assertEquals(400, exception.getHttpStatusCode());
            assertEquals(6010, exception.getApiStatusCode());
            assertEquals("ValidationFailure", exception.getSubType());
        }
    }

    /**
     * Tests that uploading uses the link's method and headers, except those the HTTP client may not set.
     *
     * @throws Exception if the request fails
     */
    @Test
    void testUploadBytes() throws Exception {
        try (AsyncApiClient client = createLocalClient()) {
            client.upload(AsyncApiStatus.fromJson(waitingForUpload()), FILE);

            FakeServer.Request request = receivedFor("/upload/").get(0);
            assertEquals("PUT", request.method());
            assertEquals("application/pdf", request.headers().getFirst("Content-Type"));
            assertEquals(String.valueOf(FILE.length), request.headers().getFirst("Content-Length"));
            assertArrayEquals(FILE, request.body());
        }
    }

    /**
     * Tests uploading from a file and from an input stream.
     *
     * @throws Exception if the request fails
     */
    @Test
    void testUploadFileAndStream() throws Exception {
        Path file = Files.write(tempDir.resolve("in.pdf"), FILE);
        try (AsyncApiClient client = createLocalClient()) {
            AsyncApiStatus status = AsyncApiStatus.fromJson(waitingForUpload());
            client.upload(status, file);
            client.upload(status, new ByteArrayInputStream(FILE));

            List<FakeServer.Request> uploads = receivedFor("/upload/");
            assertEquals(2, uploads.size());
            assertArrayEquals(FILE, uploads.get(0).body());
            assertArrayEquals(FILE, uploads.get(1).body());
        }
    }

    /** Tests that a rejected upload with a non-JSON body is thrown with an unknown error. */
    @Test
    void testUploadError() {
        server.route("/upload/", reply(403, "<Error><Code>AccessDenied</Code></Error>", null));
        try (AsyncApiClient client = createLocalClient()) {
            AsyncApiException exception = assertThrows(
                    AsyncApiException.class, () -> client.upload(AsyncApiStatus.fromJson(waitingForUpload()), FILE));

            assertEquals(403, exception.getHttpStatusCode());
            assertEquals(CdrApiError.UNKNOWN_CODE, exception.getApiStatusCode());
        }
    }

    /** Tests that steps throw an IllegalStateException when the status lacks the link they need. */
    @Test
    void testMissingLinks() {
        try (AsyncApiClient client = createLocalClient()) {
            AsyncApiStatus noLinks = AsyncApiStatus.fromJson("{\"status\":{\"state\":\"PROCESSING\"}}");
            AsyncApiStatus noResult =
                    AsyncApiStatus.fromJson("{\"status\":{\"state\":\"COMPLETE\",\"complete\":true,\"success\":true}}");
            assertThrows(IllegalStateException.class, () -> client.upload(noLinks, FILE));
            assertThrows(IllegalStateException.class, () -> client.getStatus(noLinks));
            assertThrows(IllegalStateException.class, () -> client.waitForCompletion(noLinks));
            assertThrows(IllegalStateException.class, () -> client.download(noLinks));
            assertThrows(IllegalStateException.class, () -> client.download(noResult));
        }
    }

    /**
     * Tests that getting the status sends an unconditional request to the status link.
     *
     * @throws Exception if the request fails
     */
    @Test
    void testGetStatus() throws Exception {
        try (AsyncApiClient client = createLocalClient()) {
            AsyncApiStatus status = client.getStatus(AsyncApiStatus.fromJson(waitingForUpload()));

            FakeServer.Request request = receivedFor("/status/").get(0);
            assertEquals("GET", request.method());
            assertEquals("application/json", request.headers().getFirst("Accept"));
            assertEquals("sigv4", request.headers().getFirst("Authorization"));
            assertNotEquals("ignored", request.headers().getFirst("Host"));
            assertNull(request.headers().getFirst("If-None-Match"));
            assertTrue(status.isSuccess());
            assertEquals("e1", status.getEtag());
        }
    }

    /**
     * Tests that waiting for a completed status returns it without any requests.
     *
     * @throws Exception if the request fails
     */
    @Test
    void testWaitForCompletionAlreadyComplete() throws Exception {
        try (AsyncApiClient client = createLocalClient()) {
            AsyncApiStatus status = AsyncApiStatus.fromJson(complete(true));

            assertSame(status, client.waitForCompletion(status));
            assertTrue(server.requests().isEmpty());
        }
    }

    /**
     * Tests that polling retries throttled and unmodified responses and sends the latest ETag.
     *
     * @throws Exception if the request fails
     */
    @Test
    void testWaitForCompletionPolls() throws Exception {
        server.route(
                "/status/",
                reply(429, "", null),
                reply(200, processing(), "e2"),
                reply(304, "", null),
                reply(200, complete(true), "e3"));
        try (AsyncApiClient client = createLocalClient()) {
            AsyncApiStatus initiated = AsyncApiStatus.fromJson(waitingForUpload());
            AsyncApiStatus status = client.waitForCompletion(initiated);

            assertTrue(status.isSuccess());
            List<FakeServer.Request> polls = receivedFor("/status/");
            assertEquals(4, polls.size());
            // The initiated status was parsed without an ETag, so the first poll is unconditional
            assertNull(polls.get(0).headers().getFirst("If-None-Match"));
            assertNull(polls.get(1).headers().getFirst("If-None-Match"));
            assertEquals("e2", polls.get(2).headers().getFirst("If-None-Match"));
            assertEquals("e2", polls.get(3).headers().getFirst("If-None-Match"));
        }
    }

    /** Tests that polling throws a TimeoutException if processing does not complete in time. */
    @Test
    void testWaitForCompletionTimeout() {
        server.route("/status/", reply(200, processing(), null));
        try (AsyncApiClient client = createLocalClient(Duration.ofMillis(200))) {
            TimeoutException exception = assertThrows(
                    TimeoutException.class, () -> client.waitForCompletion(AsyncApiStatus.fromJson(processing())));

            assertTrue(exception.getMessage().contains("PROCESSING"));
        }
    }

    /** Tests that a status error while polling is thrown as an {@link AsyncApiException}. */
    @Test
    void testWaitForCompletionError() {
        server.route("/status/", reply(500, "{\"error\":{\"code\":5000,\"message\":\"Oops\"}}", null));
        try (AsyncApiClient client = createLocalClient()) {
            AsyncApiException exception = assertThrows(
                    AsyncApiException.class, () -> client.waitForCompletion(AsyncApiStatus.fromJson(processing())));

            assertEquals(500, exception.getHttpStatusCode());
            assertEquals(5000, exception.getApiStatusCode());
        }
    }

    /**
     * Tests that downloading follows the redirect without sending the link's credentials to the redirect target.
     *
     * @throws Exception if the request fails
     */
    @Test
    void testDownload() throws Exception {
        try (AsyncApiClient client = createLocalClient()) {
            AsyncApiResponse response = client.download(AsyncApiStatus.fromJson(complete(true)));

            assertEquals("sigv4", receivedFor("/download/").get(0).headers().getFirst("Authorization"));
            assertNull(receivedFor("/storage/").get(0).headers().getFirst("Authorization"));
            assertEquals(200, response.getStatusCode());
            assertTrue(response.isSuccess());
            assertArrayEquals(RESULT, response.getBody());
            assertEquals(List.of("exe/macro/ms/word"), response.getRisksTaken());
        }
    }

    /**
     * Tests downloading to a file.
     *
     * @throws Exception if the request fails
     */
    @Test
    void testDownloadToFile() throws Exception {
        Path target = tempDir.resolve("out.pdf");
        try (AsyncApiClient client = createLocalClient()) {
            long written = client.download(AsyncApiStatus.fromJson(complete(true)), target);

            assertEquals(RESULT.length, written);
            assertArrayEquals(RESULT, Files.readAllBytes(target));
        }
    }

    /** Tests that downloading a result twice is thrown as an {@link AsyncApiException}. */
    @Test
    void testDownloadAlreadyDownloaded() {
        server.route(
                "/download/", reply(410, "{\"error\":{\"code\":4010,\"message\":\"Gone\",\"type\":\"Gone\"}}", null));
        try (AsyncApiClient client = createLocalClient()) {
            AsyncApiStatus status = AsyncApiStatus.fromJson(complete(true));
            AsyncApiException bytes = assertThrows(AsyncApiException.class, () -> client.download(status));
            AsyncApiException file =
                    assertThrows(AsyncApiException.class, () -> client.download(status, tempDir.resolve("out")));

            assertEquals(410, bytes.getHttpStatusCode());
            assertEquals(4010, bytes.getApiStatusCode());
            assertEquals(410, file.getHttpStatusCode());
            assertFalse(Files.exists(tempDir.resolve("out")));
        }
    }

    /** Tests that downloading the result of failed processing throws the processing error. */
    @Test
    void testDownloadFailedProcessing() {
        try (AsyncApiClient client = createLocalClient()) {
            AsyncApiException exception = assertThrows(
                    AsyncApiException.class, () -> client.download(AsyncApiStatus.fromJson(complete(false))));
            AsyncApiException unknown = assertThrows(
                    AsyncApiException.class,
                    () -> client.download(AsyncApiStatus.fromJson(
                            "{\"status\":{\"state\":\"COMPLETE\",\"complete\":true,\"success\":false}}")));

            assertEquals(200, exception.getHttpStatusCode());
            assertEquals(3020, exception.getApiStatusCode());
            assertEquals("PROCESSING_NOT_RECOGNISED", exception.getName());
            assertEquals(CdrApiError.UNKNOWN_CODE, unknown.getApiStatusCode());
            assertTrue(server.requests().isEmpty());
        }
    }

    /** Tests that an endless redirect loop is reported as an IOException. */
    @Test
    void testDownloadTooManyRedirects() {
        String status = "{\"status\":{\"state\":\"COMPLETE\",\"complete\":true,\"success\":true},"
                + "\"links\":{\"result\":" + link("/loop/1", "GET") + "}}";
        try (AsyncApiClient client = createLocalClient()) {
            assertThrows(IOException.class, () -> client.download(AsyncApiStatus.fromJson(status)));
        }
    }

    /**
     * Tests processing a file in one call.
     *
     * @throws Exception if the request fails
     */
    @Test
    void testProcess() throws Exception {
        try (AsyncApiClient client = createLocalClient()) {
            AsyncApiResponse response = client.process(AsyncApiRequest.create(MediaType.PDF), FILE);

            assertArrayEquals(RESULT, response.getBody());
            assertArrayEquals(FILE, receivedFor("/upload/").get(0).body());
            assertEquals(
                    List.of("/v1/initiate", "/upload/1", "/status/1", "/download/1", "/storage/1"),
                    server.requests().stream().map(FakeServer.Request::path).toList());
        }
    }

    /**
     * Tests processing a file from an input stream.
     *
     * @throws Exception if the request fails
     */
    @Test
    void testProcessInputStream() throws Exception {
        try (AsyncApiClient client = createLocalClient()) {
            AsyncApiResponse response = client.process(new ByteArrayInputStream(FILE), "application/pdf");

            assertArrayEquals(RESULT, response.getBody());
            assertEquals(
                    "application/pdf",
                    MAPPER.readTree(server.requests().get(0).body())
                            .get("mime-type")
                            .stringValue());
        }
    }

    /**
     * Tests processing a file from and to disk.
     *
     * @throws Exception if the request fails
     */
    @Test
    void testProcessFiles() throws Exception {
        Path source = Files.write(tempDir.resolve("in.pdf"), FILE);
        Path target = tempDir.resolve("out.pdf");
        try (AsyncApiClient client = createLocalClient()) {
            AsyncApiStatus status = client.process(AsyncApiRequest.create(MediaType.PDF), source, target);

            assertTrue(status.isSuccess());
            assertEquals(List.of("exe/macro/ms/word"), status.getRisksTaken());
            assertArrayEquals(RESULT, Files.readAllBytes(target));
        }
    }

    /** Tests that processing throws the processing error when processing fails. */
    @Test
    void testProcessFailure() {
        server.route("/status/", reply(200, complete(false), null));
        try (AsyncApiClient client = createLocalClient()) {
            AsyncApiException exception = assertThrows(
                    AsyncApiException.class, () -> client.process(AsyncApiRequest.create(MediaType.PDF), FILE));

            assertEquals(3020, exception.getApiStatusCode());
            assertTrue(receivedFor("/download/").isEmpty());
        }
    }
}
