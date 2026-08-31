/**
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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import com.everfox.cdr.MediaType;
import com.everfox.cdr.Region;
import com.everfox.cdr.Risk;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the Everfox CDR Instant API client.
 *
 * These tests require a valid API key set in the CDR_INSTANT_API_KEY environment variable.
 * To run: export CDR_INSTANT_API_KEY=your-api-key-here && mvn test -Dtest=IntegrationTest
 *
 * Tests are disabled by default if the environment variable is not set.
 *
 * NOTE: The API has specific requirements for Accept headers that may vary by content type.
 * Using "application/pdf" for both Content-Type and Accept typically works well for testing.
 */
class IntegrationTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String API_KEY_ENV = "CDR_INSTANT_API_KEY";
    // Simple PDF content for testing (minimal valid PDF)
    private static final byte[] PDF_TEST_DATA = ("%PDF-1.4\n" +
            "1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj\n" +
            "2 0 obj<</Type/Pages/Count 1/Kids[3 0 R]>>endobj\n" +
            "3 0 obj<</Type/Page/Parent 2 0 R/MediaBox[0 0 612 792]/Contents 4 0 R>>endobj\n" +
            "4 0 obj<</Length 44>>stream\n" +
            "BT /F1 12 Tf 100 700 Td (Test Document) Tj ET\n" +
            "endstream endobj\n" +
            "xref\n0 5\n0000000000 65535 f\n0000000009 00000 n\n0000000056 00000 n\n" +
            "0000000115 00000 n\n0000000214 00000 n\ntrailer<</Size 5/Root 1 0 R>>\n" +
            "startxref\n307\n%%EOF").getBytes();
    // Minimal valid JSON content for testing
    private static final byte[] JSON_TEST_DATA = "[]".getBytes();
    // Minimal invalid JSON content for testing
    private static final byte[] INVALID_JSON_TEST_DATA = "[}".getBytes();

    /**
     * Creates a client configured for testing.
     */
    private InstantApiClient createClient() {
        String apiKey = System.getenv(API_KEY_ENV);
        assertNotNull(apiKey, "CDR_INSTANT_API_KEY environment variable must be set");

        InstantApiConfig config = new InstantApiConfig(apiKey, Region.US_WEST_2);

        return new InstantApiClient(config);
    }

    @Test
    @EnabledIfEnvironmentVariable(named = API_KEY_ENV, matches = ".+")
    void testUploadPdfFile() throws IOException, InterruptedException, InstantApiException {
        try (InstantApiClient client = createClient()) {
            InstantApiRequest request = new InstantApiRequest(
                    PDF_TEST_DATA,
                    "application/pdf",
                    "application/pdf"
            );

            InstantApiResponse response = client.upload(request);

            assertNotNull(response);
            assertTrue(response.isSuccess());
            assertEquals(200, response.getStatusCode());
            assertNotNull(response.getBody());
            assertTrue(response.getBody().length > 0);
        }
    }

    @Test
    @EnabledIfEnvironmentVariable(named = API_KEY_ENV, matches = ".+")
    void testUploadJsonFileNoOptions() throws IOException, InterruptedException, InstantApiException {
        try (InstantApiClient client = createClient()) {
            InstantApiRequest request = new InstantApiRequest(
                    JSON_TEST_DATA,
                    MediaType.JSON,
                    MediaType.JSON
            );

            InstantApiException exception = assertThrows(
                    InstantApiException.class,
                    () -> client.upload(request),
                    "Should throw InstantApiException for invalid risk options"
            );

            assertEquals(400, exception.getHttpStatusCode());
            assertEquals(1110, exception.getApiStatusCode());
            assertEquals("The following risks are associated with this file and need to be explicitly allowed:- poly/text/json, structured/no-schema/json", exception.getMessage());
            assertEquals("RISK_NOT_ALLOWED", exception.getName());
            assertEquals("BadRequest", exception.getType());
        }
    }

    @Test
    @EnabledIfEnvironmentVariable(named = API_KEY_ENV, matches = ".+")
    void testUploadJsonFilePartialRisks() throws IOException, InterruptedException, InstantApiException {
        try (InstantApiClient client = createClient()) {
            RequestOptions options = new RequestOptions();
            options.allowRisks(Risk.POLY_TEXT_JSON);
            InstantApiRequest request = new InstantApiRequest(
                    JSON_TEST_DATA,
                    MediaType.JSON,
                    MediaType.JSON,
                    options
            );

            InstantApiException exception = assertThrows(
                    InstantApiException.class,
                    () -> client.upload(request),
                    "Should throw InstantApiException for invalid risk options"
            );

            assertEquals(400, exception.getHttpStatusCode());
            assertEquals(1110, exception.getApiStatusCode());
            assertEquals("The following risks are associated with this file and need to be explicitly allowed:- structured/no-schema/json", exception.getMessage());
            assertEquals("RISK_NOT_ALLOWED", exception.getName());
            assertEquals("BadRequest", exception.getType());
        }
    }

    @Test
    @EnabledIfEnvironmentVariable(named = API_KEY_ENV, matches = ".+")
    void testUploadJsonFileRequiredRisks() throws IOException, InterruptedException, InstantApiException {
        try (InstantApiClient client = createClient()) {
            RequestOptions options = new RequestOptions();
            options.allowRisks(Risk.POLY_TEXT_JSON, Risk.STRUCTURED_NO_SCHEMA_JSON);
            InstantApiRequest request = new InstantApiRequest(
                    JSON_TEST_DATA,
                    MediaType.JSON,
                    MediaType.JSON,
                    options
            );

            InstantApiResponse response = client.upload(request);

            assertNotNull(response);
            assertTrue(response.isSuccess());
            assertEquals(200, response.getStatusCode());
            assertNotNull(response.getBody());
            assertTrue(response.getBody().length > 0);
        }
    }

    @Test
    @EnabledIfEnvironmentVariable(named = API_KEY_ENV, matches = ".+")
    void testUploadInvalidJsonFile() throws IOException, InterruptedException, InstantApiException {
        try (InstantApiClient client = createClient()) {
            RequestOptions options = new RequestOptions();
            options.allowRisks(Risk.POLY_TEXT_JSON, Risk.STRUCTURED_NO_SCHEMA_JSON);
            InstantApiRequest request = new InstantApiRequest(
                    INVALID_JSON_TEST_DATA,
                    MediaType.JSON,
                    MediaType.JSON,
                    options
            );

            InstantApiException exception = assertThrows(
                    InstantApiException.class,
                    () -> client.upload(request),
                    "Should throw InstantApiException for invalid JSON"
            );

            assertEquals(400, exception.getHttpStatusCode());
            assertEquals(3020, exception.getApiStatusCode());
            assertEquals("This file could not be processed: the file content isn't recognised as 'application/json'", exception.getMessage());
            assertEquals("PROCESSING_NOT_RECOGNISED", exception.getName());
            assertEquals("BadRequest", exception.getType());
        }
    }

    @Test
    @EnabledIfEnvironmentVariable(named = API_KEY_ENV, matches = ".+")
    void testUploadWithInputStream() throws IOException, InterruptedException, InstantApiException {
        try (InstantApiClient client = createClient()) {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(PDF_TEST_DATA);

            InstantApiResponse response = client.upload(inputStream, "application/pdf", "application/pdf");

            assertNotNull(response);
            assertTrue(response.isSuccess());
            assertEquals(200, response.getStatusCode());
            assertNotNull(response.getBody());
        }
    }

    @Test
    @EnabledIfEnvironmentVariable(named = API_KEY_ENV, matches = ".+")
    void testUploadWithOptions() throws IOException, InterruptedException, InstantApiException {
        try (InstantApiClient client = createClient()) {
            RequestOptions options = new RequestOptions();
            options.setReport(RequestOptions.ReportFormat.FULL);

            InstantApiRequest request = new InstantApiRequest(
                    PDF_TEST_DATA,
                    "application/pdf",
                    "application/pdf",
                    options
            );

            InstantApiResponse response = client.upload(request);

            assertNotNull(response);
            assertTrue(response.isSuccess());
            assertEquals(200, response.getStatusCode());

            // Verify response headers
            assertNotNull(response.getHeaders());

            // Report header may be present depending on the file content
            if (response.getReport() != null) {
                assertFalse(response.getReport().isEmpty());
            }
        }
    }

    @Test
    @EnabledIfEnvironmentVariable(named = API_KEY_ENV, matches = ".+")
    void testUploadToRegion() throws IOException, InterruptedException, InstantApiException {
        String apiKey = System.getenv(API_KEY_ENV);
        InstantApiConfig config = new InstantApiConfig(apiKey, Region.US_WEST_2);

        try (InstantApiClient client = new InstantApiClient(config)) {
            InstantApiRequest request = new InstantApiRequest(
                    PDF_TEST_DATA,
                    "application/pdf",
                    "application/pdf"
            );

            InstantApiResponse response = client.upload(request);
            assertNotNull(response, "Response should not be null");
            assertTrue(response.isSuccess(), "Upload should succeed");
        }
    }

    @Test
    @EnabledIfEnvironmentVariable(named = API_KEY_ENV, matches = ".+")
    void testInvalidApiKey() {
        InstantApiConfig config = new InstantApiConfig("invalid-api-key-12345", Region.US_WEST_2);

        try (InstantApiClient client = new InstantApiClient(config)) {
            InstantApiRequest request = new InstantApiRequest(
                    PDF_TEST_DATA,
                    "application/pdf",
                    "application/pdf"
            );

            InstantApiException exception = assertThrows(
                    InstantApiException.class,
                    () -> client.upload(request),
                    "Should throw InstantApiException for invalid API key"
            );

            assertEquals(403, exception.getHttpStatusCode());
            assertEquals(6070, exception.getApiStatusCode());
            assertEquals("Forbidden", exception.getMessage());
            assertEquals("APIGATEWAY_INVALID_API_KEY", exception.getName());
            assertEquals("BadRequest", exception.getType());
        }
    }

    @Test
    @EnabledIfEnvironmentVariable(named = API_KEY_ENV, matches = ".+")
    void testFileSizeLimit() {
        try (InstantApiClient client = createClient()) {
            // Create a file larger than 4.5 MB
            byte[] largeFile = new byte[5 * 1024 * 1024]; // 5 MB
            InstantApiRequest request = new InstantApiRequest(
                    largeFile,
                    "application/octet-stream",
                    "application/octet-stream"
            );

            InstantApiException exception = assertThrows(
                    InstantApiException.class,
                    () -> client.upload(request),
                    "Should throw InstantApiException for oversized file"
            );

            assertEquals(413, exception.getHttpStatusCode());
            assertEquals(6050, exception.getApiStatusCode());
            assertEquals("Request Too Long", exception.getMessage());
            assertEquals("APIGATEWAY_INTEGRATION_FAILURE", exception.getName());
            assertEquals("InternalServerError", exception.getType());
        }
    }

    @Test
    @EnabledIfEnvironmentVariable(named = API_KEY_ENV, matches = ".+")
    void testResponseHeaders() throws IOException, InterruptedException, InstantApiException {
        try (InstantApiClient client = createClient()) {
            InstantApiRequest request = new InstantApiRequest(
                    PDF_TEST_DATA,
                    "application/pdf",
                    "application/pdf"
            );

            InstantApiResponse response = client.upload(request);

            assertNotNull(response);
            assertTrue(response.isSuccess());

            // Verify headers are captured
            assertNotNull(response.getHeaders());
            assertFalse(response.getHeaders().isEmpty());

            // Content-Type should be present in response headers (case-insensitive check)
            boolean hasContentType = response.getHeaders().keySet().stream()
                    .anyMatch(key -> key.equalsIgnoreCase("content-type"));
            assertTrue(hasContentType);
        }
    }

    @Test
    @EnabledIfEnvironmentVariable(named = API_KEY_ENV, matches = ".+")
    void testCustomTimeouts() throws IOException, InterruptedException, InstantApiException {
        String apiKey = System.getenv(API_KEY_ENV);
        InstantApiConfig config = new InstantApiConfig(apiKey, Region.US_WEST_2, 5, 120); // Custom timeouts

        try (InstantApiClient client = new InstantApiClient(config)) {
            InstantApiRequest request = new InstantApiRequest(
                    PDF_TEST_DATA,
                    "application/pdf",
                    "application/pdf"
            );

            InstantApiResponse response = client.upload(request);

            assertNotNull(response);
            assertTrue(response.isSuccess());
        }
    }

    @Test
    @EnabledIfEnvironmentVariable(named = API_KEY_ENV, matches = ".+")
    void testAllRequestOptions() throws IOException, InterruptedException, InstantApiException {
        try (InstantApiClient client = createClient()) {
            RequestOptions options = new RequestOptions();
            options.allowRisks(Risk.EXE, Risk.EXE_MACRO, Risk.STEG, Risk.POLY);

            // Configure reporting
            options.setReport(RequestOptions.ReportFormat.FULL);

            // Configure image quality
            RequestOptions.ImageQualityOptions imageQuality = new RequestOptions.ImageQualityOptions();
            imageQuality.setPreserveJpeg(true);
            imageQuality.setPreservePng(true);
            options.setImageQuality(imageQuality);

            InstantApiRequest request = new InstantApiRequest(
                    PDF_TEST_DATA,
                    "application/pdf",
                    "application/pdf",
                    options
            );

            InstantApiResponse response = client.upload(request);

            assertNotNull(response);
            assertTrue(response.isSuccess());

            assertNotNull(response.getReport());
            JsonNode report = MAPPER.readTree(response.getReport());
            assertEquals(2, report.size());
            assertEquals(RequestOptions.ReportFormat.FULL.getFormat(), report.get("settings").get("format").asString());
            JsonNode structure = report.get("structure").asArray();
            assertEquals(1, structure.size());
            assertEquals(MediaType.PDF.getMediaType(), structure.get(0).get("details").get("type").asString());
        }
    }
}
