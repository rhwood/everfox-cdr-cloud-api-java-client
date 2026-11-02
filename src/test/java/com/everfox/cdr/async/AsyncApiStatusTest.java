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
import static org.mockito.Mockito.*;

import com.everfox.cdr.CdrApiError;
import java.net.URI;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link AsyncApiStatus} and {@link AsyncApiLink}. */
class AsyncApiStatusTest {

    /** A status as returned after initiating a job. */
    private static final String INITIATED = "{\"id\":\"data/1.pdf\",\"userData\":\"mine\","
            + "\"status\":{\"state\":\"WAITING_FOR_UPLOAD\",\"complete\":false},"
            + "\"links\":{\"upload\":{\"url\":\"https://upload.example.com/1\",\"method\":\"PUT\","
            + "\"headers\":{\"Content-Type\":\"application/pdf\",\"X-Number\":1},"
            + "\"expiry\":\"2018-03-04T12:35:56Z\"},"
            + "\"status\":{\"url\":\"https://status.example.com/1\",\"method\":\"GET\",\"headers\":{}}}}";
    /** A status of a successfully completed job. */
    private static final String SUCCEEDED = "{\"id\":\"data/1.pdf\","
            + "\"status\":{\"state\":\"COMPLETE\",\"complete\":true,\"success\":true},"
            + "\"risks\":{\"taken\":[\"exe/macro/ms/word\",7]},"
            + "\"links\":{\"result\":{\"url\":\"https://result.example.com/1\",\"method\":\"GET\","
            + "\"fileSize\":123456,\"expiry\":\"not a date\"}}}";
    /** A status of an unsuccessfully completed job. */
    private static final String FAILED = "{\"status\":{\"state\":\"COMPLETE\",\"complete\":true,\"success\":false},"
            + "\"error\":{\"code\":3020,\"name\":\"PROCESSING_NOT_RECOGNISED\",\"message\":\"Not a PDF\","
            + "\"type\":\"PROCESSING\",\"subType\":\"Parsing/NotRecognised\"}}";

    /** Default constructor for the test class. */
    AsyncApiStatusTest() {
        // Nothing to do here
    }

    /**
     * Creates a mock HTTP response.
     *
     * @param statusCode the HTTP status code
     * @param body the response body
     * @param headers the response headers
     * @return the mock response
     */
    private static HttpResponse<byte[]> response(int statusCode, String body, Map<String, List<String>> headers) {
        @SuppressWarnings("unchecked") // unavoidable when mocking due to HttpResponse being generic
        HttpResponse<byte[]> httpResponse = mock(HttpResponse.class);
        when(httpResponse.statusCode()).thenReturn(statusCode);
        when(httpResponse.body()).thenReturn(body != null ? body.getBytes(StandardCharsets.UTF_8) : null);
        when(httpResponse.headers()).thenReturn(HttpHeaders.of(headers, (k, v) -> true));
        return httpResponse;
    }

    /** Tests parsing an initiated status and its links. */
    @Test
    void testInitiated() {
        AsyncApiStatus status = AsyncApiStatus.fromJson(INITIATED);

        assertEquals("data/1.pdf", status.getId());
        assertEquals("WAITING_FOR_UPLOAD", status.getState());
        assertEquals("mine", status.getUserData());
        assertFalse(status.isComplete());
        assertFalse(status.isSuccess());
        assertNull(status.getError());
        assertNull(status.getEtag());
        assertNull(status.getResultLink());
        assertEquals(List.of(), status.getRisksTaken());
        assertEquals(INITIATED, status.toJson());

        AsyncApiLink upload = status.getUploadLink();
        assertEquals(URI.create("https://upload.example.com/1"), upload.getUrl());
        assertEquals("PUT", upload.getMethod());
        // Non-string header values are ignored
        assertEquals(Map.of("Content-Type", "application/pdf"), upload.getHeaders());
        assertEquals(Instant.parse("2018-03-04T12:35:56Z"), upload.getExpiry());
        assertNull(upload.getFileSize());
        assertEquals("GET", status.getStatusLink().getMethod());
    }

    /** Tests parsing a successful status and its result link. */
    @Test
    void testSucceeded() {
        AsyncApiStatus status = AsyncApiStatus.fromJson(SUCCEEDED);

        assertTrue(status.isComplete());
        assertTrue(status.isSuccess());
        // Non-string risks are ignored
        assertEquals(List.of("exe/macro/ms/word"), status.getRisksTaken());
        assertNull(status.getUploadLink());
        assertNull(status.getStatusLink());
        assertEquals(123456L, status.getResultLink().getFileSize());
        // An unparseable expiry is ignored
        assertNull(status.getResultLink().getExpiry());
    }

    /** Tests parsing a failed status and its error. */
    @Test
    void testFailed() {
        AsyncApiStatus status = AsyncApiStatus.fromJson(FAILED);

        assertTrue(status.isComplete());
        assertFalse(status.isSuccess());
        CdrApiError error = status.getError();
        assertEquals(3020, error.getCode());
        assertEquals("PROCESSING_NOT_RECOGNISED", error.getName());
        assertEquals("Not a PDF", error.getMessage());
        assertEquals("PROCESSING", error.getType());
        assertEquals("Parsing/NotRecognised", error.getSubType());
    }

    /** Tests that a complete status without a success flag is not successful. */
    @Test
    void testCompleteWithoutSuccess() {
        AsyncApiStatus status = AsyncApiStatus.fromJson("{\"status\":{\"complete\":true}}");

        assertTrue(status.isComplete());
        assertFalse(status.isSuccess());
        assertNull(status.getId());
        assertNull(status.getState());
    }

    /** Tests that a success flag is ignored until the status is complete. */
    @Test
    void testSuccessBeforeComplete() {
        assertFalse(AsyncApiStatus.fromJson("{\"status\":{\"complete\":false,\"success\":true}}")
                .isSuccess());
    }

    /** Tests that invalid JSON is rejected. */
    @Test
    void testInvalidJson() {
        assertThrows(NullPointerException.class, () -> AsyncApiStatus.fromJson(null));
        assertThrows(IllegalArgumentException.class, () -> AsyncApiStatus.fromJson("[}"));
        assertThrows(IllegalArgumentException.class, () -> AsyncApiStatus.fromJson("[]"));
        assertThrows(IllegalArgumentException.class, () -> AsyncApiStatus.fromJson(""));
    }

    /** Tests that links without a URL are ignored and links without a method default to GET. */
    @Test
    void testLinkDefaults() {
        AsyncApiStatus status = AsyncApiStatus.fromJson(
                "{\"links\":{\"upload\":{\"method\":\"PUT\"},\"status\":{\"url\":\"https://example.com\"}}}");

        assertNull(status.getUploadLink());
        assertEquals("GET", status.getStatusLink().getMethod());
        assertEquals(Map.of(), status.getStatusLink().getHeaders());
    }

    /**
     * Tests creating a status from an HTTP response, including the ETag.
     *
     * @throws AsyncApiException if the response is unexpectedly treated as an error
     */
    @Test
    void testFromHttpResponse() throws AsyncApiException {
        AsyncApiStatus status =
                AsyncApiStatus.fromHttpResponse(response(200, SUCCEEDED, Map.of("ETag", List.of("\"abc\""))));

        assertTrue(status.isSuccess());
        assertEquals("\"abc\"", status.getEtag());
    }

    /** Tests that an HTTP error response is thrown as an {@link AsyncApiException}. */
    @Test
    void testFromHttpResponseError() {
        AsyncApiException exception = assertThrows(
                AsyncApiException.class,
                () -> AsyncApiStatus.fromHttpResponse(
                        response(403, "{\"error\":{\"code\":6070,\"message\":\"Forbidden\"}}", Map.of())));

        assertEquals(403, exception.getHttpStatusCode());
        assertEquals(6070, exception.getApiStatusCode());
    }

    /** Tests that a successful HTTP response without a status is thrown as an {@link AsyncApiException}. */
    @Test
    void testFromHttpResponseNotStatus() {
        AsyncApiException empty = assertThrows(
                AsyncApiException.class, () -> AsyncApiStatus.fromHttpResponse(response(200, null, Map.of())));
        AsyncApiException html = assertThrows(
                AsyncApiException.class,
                () -> AsyncApiStatus.fromHttpResponse(response(200, "<html></html>", Map.of())));

        assertEquals(200, empty.getHttpStatusCode());
        assertEquals(CdrApiError.UNKNOWN_CODE, html.getApiStatusCode());
    }

    /** Tests creating a link directly. */
    @Test
    void testCreateLink() {
        URI url = URI.create("https://example.com");
        AsyncApiLink link = AsyncApiLink.create(url, "POST", Map.of("A", "B"));

        assertEquals(url, link.getUrl());
        assertEquals("POST", link.getMethod());
        assertEquals(Map.of("A", "B"), link.getHeaders());
        assertNull(link.getExpiry());
        assertNull(link.getFileSize());
        assertThrows(NullPointerException.class, () -> AsyncApiLink.create(null, "GET", Map.of()));
        assertThrows(NullPointerException.class, () -> AsyncApiLink.create(url, null, Map.of()));
        assertThrows(NullPointerException.class, () -> AsyncApiLink.create(url, "GET", null));
    }

    /** Tests that a null link object is ignored. */
    @Test
    void testNullLinkJson() {
        assertNull(AsyncApiLink.fromJson(null));
    }
}
