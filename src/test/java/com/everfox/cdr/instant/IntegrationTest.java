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
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Integration tests for the Everfox CDR Instant API client.
 *
 * <p>These tests require a valid API key set in the CDR_INSTANT_API_KEY environment variable. To run: export
 * CDR_INSTANT_API_KEY=your-api-key-here &amp;&amp; mvn test -Dtest=IntegrationTest
 *
 * <p>The US_WEST_2 region is used for testing, but you can change the region by setting the CDR_INSTANT_API_URL
 * environment variable. Note that the API URL must be a valid URI, e.g.,
 * "https://us-west-2.aws.instant.cdr.everfox.com/v1" and not the name of a region.
 *
 * <p>If you want to use a different region, you can set the CDR_INSTANT_API_REGION environment variable to one of the
 * supported region names (case-insensitive), e.g., "eu_west_1" or "us_west_2". If both CDR_INSTANT_API_URL and
 * CDR_INSTANT_API_REGION are set, the region will take precedence.
 *
 * <p>Tests are disabled by default if the environment variable is not set.
 *
 * <p>NOTE: The API has specific requirements for Accept headers that may vary by content type. Using "application/pdf"
 * for both Content-Type and Accept typically works well for testing.
 */
class IntegrationTest {

    /** JSON object mapper for parsing API responses. */
    private static final ObjectMapper MAPPER = new ObjectMapper();
    /** Environment variable name for the API key. */
    private static final String API_KEY_ENV = "CDR_INSTANT_API_KEY";
    /** Environment variable name for the API URL. */
    private static final String API_URL_ENV = "CDR_INSTANT_API_URL";
    /** Environment variable name for the API region. */
    private static final String API_REGION_ENV = "CDR_INSTANT_API_REGION";
    /** Simple PDF content for testing (minimal valid PDF) */
    private static final byte[] PDF_TEST_DATA = loadResource("test.pdf");
    /** Simple DOCX content for testing (minimal valid Word document containing "This is a test.") */
    private static final byte[] DOCX_TEST_DATA = loadResource("test.docx");
    /** Simple JPEG content for testing (minimal valid JPEG) */
    private static final byte[] JPEG_TEST_DATA = loadResource("test.jpg");
    /** ZIP archive for testing that cannot be processed unless unsupported content is redacted */
    private static final byte[] ZIP_TEST_DATA = loadResource("test.zip");
    /** Minimal valid JSON content for testing */
    private static final byte[] JSON_TEST_DATA = "[]".getBytes();
    /** Minimal invalid JSON content for testing */
    private static final byte[] INVALID_JSON_TEST_DATA = "[}".getBytes();

    /** Default constructor. */
    IntegrationTest() {
        // Nothing to do
    }

    /**
     * Loads a test resource from the classpath.
     *
     * @param name the name of the resource, relative to this class's package
     * @return the contents of the resource
     * @throws IllegalStateException if the resource is not found
     * @throws UncheckedIOException if the resource cannot be read
     */
    private static byte[] loadResource(String name) {
        try (InputStream in = IntegrationTest.class.getResourceAsStream(name)) {
            if (in == null) {
                throw new IllegalStateException("Test resource not found: " + name);
            }
            return in.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to read test resource: " + name, e);
        }
    }

    /**
     * Creates a client configured for testing using the API key and optional API URL from environment variables.
     *
     * @return a configured Instant API client
     */
    private InstantApiClient createClient() {
        URI apiUri = Region.US_WEST_2.getBaseUrl();
        String apiKey = System.getenv(API_KEY_ENV);
        assertNotNull(apiKey, "CDR_INSTANT_API_KEY environment variable must be set");

        String apiUrl = System.getenv(API_URL_ENV);
        if (apiUrl != null && !apiUrl.isEmpty()) {
            apiUri = assertDoesNotThrow(() -> new URI(apiUrl), "Invalid API URL");
        }
        String apiRegion = System.getenv(API_REGION_ENV);
        if (apiRegion != null && !apiRegion.isEmpty()) {
            Region region = Region.fromString(apiRegion);
            apiUri = region.getBaseUrl();
        }
        InstantApiConfig config = InstantApiConfig.create(apiKey, apiUri);

        return InstantApiClient.create(config);
    }

    /**
     * Tests uploading a PDF file to the Instant API.
     *
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the operation is interrupted
     * @throws InstantApiException if the API returns an error response
     */
    @Test
    @EnabledIfEnvironmentVariable(named = API_KEY_ENV, matches = ".+")
    void testUploadPdfFile() throws IOException, InterruptedException, InstantApiException {
        try (InstantApiClient client = createClient()) {
            InstantApiRequest request = InstantApiRequest.create(PDF_TEST_DATA, "application/pdf", "application/pdf");

            InstantApiResponse response = client.upload(request);

            assertNotNull(response);
            assertTrue(response.isSuccess());
            assertEquals(200, response.getStatusCode());
            assertNotNull(response.getBody());
            assertTrue(response.getBody().length > 0);
        }
    }

    /** Tests uploading a JSON file to the Instant API without specifying any risk options. */
    @Test
    @EnabledIfEnvironmentVariable(named = API_KEY_ENV, matches = ".+")
    void testUploadJsonFileNoOptions() {
        try (InstantApiClient client = createClient()) {
            InstantApiRequest request = InstantApiRequest.create(JSON_TEST_DATA, MediaType.JSON, MediaType.JSON);

            InstantApiException exception = assertThrows(
                    InstantApiException.class,
                    () -> client.upload(request),
                    "Should throw InstantApiException for invalid risk options");

            assertEquals(400, exception.getHttpStatusCode());
            assertEquals(1110, exception.getApiStatusCode());
            assertEquals(
                    "The following risks are associated with this file and need to be explicitly allowed:- poly/text/json, structured/no-schema/json",
                    exception.getApiMessage());
            assertEquals("RISK_NOT_ALLOWED", exception.getName());
            assertEquals("BadRequest", exception.getType());
        }
    }

    /** Tests uploading a JSON file to the Instant API with only one of the required risk options specified. */
    @Test
    @EnabledIfEnvironmentVariable(named = API_KEY_ENV, matches = ".+")
    void testUploadJsonFilePartialRisks() {
        try (InstantApiClient client = createClient()) {
            RequestOptions options =
                    RequestOptions.builder().allowRisks(Risk.POLY_TEXT_JSON).build();
            InstantApiRequest request =
                    InstantApiRequest.create(JSON_TEST_DATA, MediaType.JSON, MediaType.JSON, options);

            InstantApiException exception = assertThrows(
                    InstantApiException.class,
                    () -> client.upload(request),
                    "Should throw InstantApiException for invalid risk options");

            assertEquals(400, exception.getHttpStatusCode());
            assertEquals(1110, exception.getApiStatusCode());
            assertEquals(
                    "The following risks are associated with this file and need to be explicitly allowed:- structured/no-schema/json",
                    exception.getApiMessage());
            assertEquals("RISK_NOT_ALLOWED", exception.getName());
            assertEquals("BadRequest", exception.getType());
        }
    }

    /**
     * Tests uploading a JSON file to the Instant API with all required risk options specified.
     *
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the operation is interrupted
     * @throws InstantApiException if the API returns an error response
     */
    @Test
    @EnabledIfEnvironmentVariable(named = API_KEY_ENV, matches = ".+")
    void testUploadJsonFileRequiredRisks() throws IOException, InterruptedException, InstantApiException {
        try (InstantApiClient client = createClient()) {
            RequestOptions options = RequestOptions.builder()
                    .allowRisks(Risk.POLY_TEXT_JSON, Risk.STRUCTURED_NO_SCHEMA_JSON)
                    .build();
            InstantApiRequest request =
                    InstantApiRequest.create(JSON_TEST_DATA, MediaType.JSON, MediaType.JSON, options);

            InstantApiResponse response = client.upload(request);

            assertNotNull(response);
            assertTrue(response.isSuccess());
            assertEquals(200, response.getStatusCode());
            assertNotNull(response.getBody());
            assertTrue(response.getBody().length > 0);
        }
    }

    /** Tests uploading an invalid JSON file to the Instant API with all required risk options specified. */
    @Test
    @EnabledIfEnvironmentVariable(named = API_KEY_ENV, matches = ".+")
    void testUploadInvalidJsonFile() {
        try (InstantApiClient client = createClient()) {
            RequestOptions options = RequestOptions.builder()
                    .allowRisks(Risk.POLY_TEXT_JSON, Risk.STRUCTURED_NO_SCHEMA_JSON)
                    .build();
            InstantApiRequest request =
                    InstantApiRequest.create(INVALID_JSON_TEST_DATA, MediaType.JSON, MediaType.JSON, options);

            InstantApiException exception = assertThrows(
                    InstantApiException.class,
                    () -> client.upload(request),
                    "Should throw InstantApiException for invalid JSON");

            assertEquals(400, exception.getHttpStatusCode());
            assertEquals(3020, exception.getApiStatusCode());
            assertEquals(
                    "This file could not be processed: the file content isn't recognised as 'application/json'",
                    exception.getApiMessage());
            assertEquals("PROCESSING_NOT_RECOGNISED", exception.getName());
            assertEquals("BadRequest", exception.getType());
        }
    }

    /**
     * Tests uploading a PDF file to the Instant API using an InputStream.
     *
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the operation is interrupted
     * @throws InstantApiException if the API returns an error response
     */
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

    /**
     * Tests uploading a PDF file to the Instant API with additional request options.
     *
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the operation is interrupted
     * @throws InstantApiException if the API returns an error response
     */
    @Test
    @EnabledIfEnvironmentVariable(named = API_KEY_ENV, matches = ".+")
    void testUploadWithOptions() throws IOException, InterruptedException, InstantApiException {
        try (InstantApiClient client = createClient()) {
            RequestOptions options = new RequestOptions(RequestOptions.ReportFormat.FULL, null, null, null, null, null);

            InstantApiRequest request =
                    InstantApiRequest.create(PDF_TEST_DATA, "application/pdf", "application/pdf", options);

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

    /**
     * Tests uploading a JPEG image to the Instant API with image quality preservation options.
     *
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the operation is interrupted
     * @throws InstantApiException if the API returns an error response
     */
    @Test
    @EnabledIfEnvironmentVariable(named = API_KEY_ENV, matches = ".+")
    void testUploadWithImageQualityPreserveJpeg() throws IOException, InterruptedException, InstantApiException {
        try (InstantApiClient client = createClient()) {
            RequestOptions.ImagesOptions imagesOptions = new RequestOptions.ImagesOptions(MediaType.IMAGE_JPEG);
            RequestOptions options = RequestOptions.builder()
                    .imagesOptions(imagesOptions)
                    .allowRisks(Risk.STEG_IMAGE_JPEG)
                    .build();

            InstantApiRequest request =
                    InstantApiRequest.create(JPEG_TEST_DATA, MediaType.IMAGE_JPEG, MediaType.IMAGE_JPEG, options);

            InstantApiResponse response = client.upload(request);

            assertNotNull(response);
            assertTrue(response.isSuccess());
            assertEquals(200, response.getStatusCode());
            assertNotNull(response.getBody());
        }
    }

    /**
     * Tests uploading a JPEG image to the Instant API with an empty array of image quality preservation options.
     *
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the operation is interrupted
     * @throws InstantApiException if the API returns an error response
     */
    @Test
    @EnabledIfEnvironmentVariable(named = API_KEY_ENV, matches = ".+")
    void testUploadWithImageQualityPreserveEmptyArray() throws IOException, InterruptedException, InstantApiException {
        try (InstantApiClient client = createClient()) {
            RequestOptions.ImagesOptions imagesOptions = new RequestOptions.ImagesOptions(new String[0]);
            RequestOptions options = new RequestOptions(null, null, imagesOptions, null, null, null);

            InstantApiRequest request =
                    InstantApiRequest.create(JPEG_TEST_DATA, MediaType.IMAGE_JPEG, MediaType.IMAGE_JPEG, options);

            InstantApiResponse response = client.upload(request);

            assertNotNull(response);
            assertTrue(response.isSuccess());
            assertEquals(200, response.getStatusCode());
            assertNotNull(response.getBody());
        }
    }

    /**
     * Tests uploading a file to the Instant API using the configured region and API key.
     *
     * <p>Note that this test ignores the API URL environment variable and uses the {@link Region#EU_WEST_2} region for
     * testing unless the {@code CDR_INSTANT_API_REGION} environment variable is set.
     *
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the operation is interrupted
     * @throws InstantApiException if the API returns an error response
     */
    @Test
    @EnabledIfEnvironmentVariable(named = API_KEY_ENV, matches = ".+")
    void testUploadToRegion() throws IOException, InterruptedException, InstantApiException {
        Region region = Region.US_WEST_2;
        String apiKey = System.getenv(API_KEY_ENV);
        String regionEnv = System.getenv(API_REGION_ENV);
        if (regionEnv != null && !regionEnv.isEmpty()) {
            region = Region.fromString(regionEnv);
        }
        InstantApiConfig config = InstantApiConfig.create(apiKey, region);

        try (InstantApiClient client = InstantApiClient.create(config)) {
            InstantApiRequest request = InstantApiRequest.create(PDF_TEST_DATA, "application/pdf", "application/pdf");

            InstantApiResponse response = client.upload(request);
            assertNotNull(response, "Response should not be null");
            assertTrue(response.isSuccess(), "Upload should succeed");
        }
    }

    /** Tests uploading a file to the Instant API with an invalid API key. */
    @Test
    @EnabledIfEnvironmentVariable(named = API_KEY_ENV, matches = ".+")
    void testInvalidApiKey() {
        InstantApiConfig config = InstantApiConfig.create(
                "invalid-api-key-12345", this.createClient().getConfig().getBaseUrl());

        try (InstantApiClient client = InstantApiClient.create(config)) {
            InstantApiRequest request = InstantApiRequest.create(PDF_TEST_DATA, "application/pdf", "application/pdf");

            InstantApiException exception = assertThrows(
                    InstantApiException.class,
                    () -> client.upload(request),
                    "Should throw InstantApiException for invalid API key");

            assertEquals(403, exception.getHttpStatusCode());
            assertEquals(6070, exception.getApiStatusCode());
            assertEquals("Forbidden", exception.getApiMessage());
            assertEquals("APIGATEWAY_INVALID_API_KEY", exception.getName());
            assertEquals("BadRequest", exception.getType());
        }
    }

    /** Tests uploading a file that exceeds the size limit of the Instant API. */
    @Test
    @EnabledIfEnvironmentVariable(named = API_KEY_ENV, matches = ".+")
    void testFileSizeLimit() {
        try (InstantApiClient client = createClient()) {
            // Create a file larger than 4.5 MB
            byte[] largeFile = new byte[5 * 1024 * 1024]; // 5 MB
            InstantApiRequest request =
                    InstantApiRequest.create(largeFile, MediaType.OCTET_STREAM, MediaType.OCTET_STREAM);

            InstantApiException exception = assertThrows(
                    InstantApiException.class,
                    () -> client.upload(request),
                    "Should throw InstantApiException for oversized file");

            assertEquals(413, exception.getHttpStatusCode());
            assertEquals(6050, exception.getApiStatusCode());
            assertEquals("Request Too Long", exception.getApiMessage());
            assertEquals("APIGATEWAY_INTEGRATION_FAILURE", exception.getName());
            assertEquals("InternalServerError", exception.getType());
        }
    }

    /**
     * Tests that the response headers from the Instant API are captured correctly in the response object.
     *
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the operation is interrupted
     * @throws InstantApiException if the API returns an error response
     */
    @Test
    @EnabledIfEnvironmentVariable(named = API_KEY_ENV, matches = ".+")
    void testResponseHeaders() throws IOException, InterruptedException, InstantApiException {
        try (InstantApiClient client = createClient()) {
            InstantApiRequest request = InstantApiRequest.create(PDF_TEST_DATA, "application/pdf", "application/pdf");

            InstantApiResponse response = client.upload(request);

            assertNotNull(response);
            assertTrue(response.isSuccess());

            // Verify headers are captured
            assertNotNull(response.getHeaders());
            assertFalse(response.getHeaders().isEmpty());

            // Content-Type should be present in response headers (case-insensitive check)
            boolean hasContentType =
                    response.getHeaders().keySet().stream().anyMatch(key -> key.equalsIgnoreCase("content-type"));
            assertTrue(hasContentType);
        }
    }

    /**
     * Tests that custom timeouts can be set in the Instant API client configuration.
     *
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the operation is interrupted
     * @throws InstantApiException if the API returns an error response
     */
    @Test
    @EnabledIfEnvironmentVariable(named = API_KEY_ENV, matches = ".+")
    void testCustomTimeouts() throws IOException, InterruptedException, InstantApiException {
        String apiKey = System.getenv(API_KEY_ENV);
        InstantApiConfig config = InstantApiConfig.create(apiKey, Region.US_WEST_2, 5, 120); // Custom timeouts

        try (InstantApiClient client = InstantApiClient.create(config)) {
            InstantApiRequest request = InstantApiRequest.create(PDF_TEST_DATA, "application/pdf", "application/pdf");

            InstantApiResponse response = client.upload(request);

            assertNotNull(response);
            assertTrue(response.isSuccess());
        }
    }

    /**
     * Tests uploading a PDF file to the Instant API with all request options configured.
     *
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the operation is interrupted
     * @throws InstantApiException if the API returns an error response
     */
    @Test
    @EnabledIfEnvironmentVariable(named = API_KEY_ENV, matches = ".+")
    void testAllRequestOptions() throws IOException, InterruptedException, InstantApiException {
        try (InstantApiClient client = createClient()) {
            RequestOptions.ImagesOptions images =
                    new RequestOptions.ImagesOptions(MediaType.IMAGE_JPEG, MediaType.IMAGE_PNG);
            RequestOptions options = RequestOptions.builder()
                    .allowRisks(Risk.EXE, Risk.EXE_MACRO, Risk.STEG, Risk.POLY)
                    // Configure reporting
                    .reportFormat(RequestOptions.ReportFormat.FULL)
                    // Configure image quality
                    .imagesOptions(images)
                    .build();

            InstantApiRequest request =
                    InstantApiRequest.create(PDF_TEST_DATA, "application/pdf", "application/pdf", options);

            InstantApiResponse response = client.upload(request);

            assertNotNull(response);
            assertTrue(response.isSuccess());

            assertNotNull(response.getReport());
            JsonNode report = MAPPER.readTree(response.getReport());
            assertEquals(2, report.size());
            assertEquals(
                    RequestOptions.ReportFormat.FULL.getFormat(),
                    report.get("settings").get("format").asString());
            JsonNode structure = report.get("structure").asArray();
            assertEquals(1, structure.size());
            assertEquals(
                    MediaType.PDF.getMediaType(),
                    structure.get(0).get("details").get("type").asString());
        }
    }

    /**
     * Tests uploading a ZIP file to the Instant API without redaction options, which redacts unsupported content using
     * the API's default replacement file.
     *
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the operation is interrupted
     * @throws InstantApiException if the API returns an error response
     */
    @Test
    @EnabledIfEnvironmentVariable(named = API_KEY_ENV, matches = ".+")
    void testUploadZipFileWithoutRedaction() throws IOException, InterruptedException, InstantApiException {
        try (InstantApiClient client = createClient()) {
            InstantApiRequest request = InstantApiRequest.create(ZIP_TEST_DATA, MediaType.ZIP, MediaType.ZIP);

            InstantApiResponse response = client.upload(request);

            assertNotNull(response);
            assertTrue(response.isSuccess());
            assertEquals(200, response.getStatusCode());
            assertEquals(
                    Map.of(
                            "minimal.json.removed.txt",
                            "The contents of this file were removed for potential security concerns."),
                    readZipEntries(response.getBody()));
        }
    }

    /**
     * Tests uploading a ZIP file to the Instant API with redaction options, which redacts unsupported content using the
     * specified replacement file.
     *
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the operation is interrupted
     * @throws InstantApiException if the API returns an error response
     */
    @Test
    @EnabledIfEnvironmentVariable(named = API_KEY_ENV, matches = ".+")
    void testUploadZipFileWithRedaction() throws IOException, InterruptedException, InstantApiException {
        try (InstantApiClient client = createClient()) {
            RequestOptions options = RequestOptions.builder()
                    .redactionOptions(new RequestOptions.RedactionOptions(".txt", MediaType.TEXT_PLAIN, "Redacted"))
                    .build();
            InstantApiRequest request = InstantApiRequest.create(ZIP_TEST_DATA, MediaType.ZIP, MediaType.ZIP, options);

            InstantApiResponse response = client.upload(request);

            assertNotNull(response);
            assertTrue(response.isSuccess());
            assertEquals(200, response.getStatusCode());
            assertEquals(Map.of("minimal.json.txt", "Redacted"), readZipEntries(response.getBody()));
        }
    }

    /**
     * Tests uploading a PDF file to the Instant API using automatic format conversion, which returns a PDF file since
     * PDF is a supported format.
     *
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the operation is interrupted
     * @throws InstantApiException if the API returns an error response
     */
    @Test
    @EnabledIfEnvironmentVariable(named = API_KEY_ENV, matches = ".+")
    void testUploadPdfFileWithAutomaticConversion() throws IOException, InterruptedException, InstantApiException {
        try (InstantApiClient client = createClient()) {
            RequestOptions options = RequestOptions.builder()
                    .conversionOptions(RequestOptions.ConversionOptions.builder()
                            .from(MediaType.PDF)
                            .build())
                    .build();
            InstantApiRequest request =
                    InstantApiRequest.create(PDF_TEST_DATA, MediaType.OCTET_STREAM, MediaType.PDF, options);

            InstantApiResponse response = client.upload(request);

            assertNotNull(response);
            assertTrue(response.isSuccess());
            assertEquals(200, response.getStatusCode());
            assertEquals("%PDF-", new String(response.getBody(), 0, 5, StandardCharsets.US_ASCII));
            assertEquals(
                    MediaType.PDF.getMediaType(),
                    MAPPER.readTree(response.getReport())
                            .get("structure")
                            .get(0)
                            .get("details")
                            .get("type")
                            .asString());
        }
    }

    /**
     * Tests uploading a DOCX file to the Instant API with the text extraction and report conversion methods, which
     * returns the report containing the document text instead of the transformed file.
     *
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the operation is interrupted
     * @throws InstantApiException if the API returns an error response
     */
    @Test
    @EnabledIfEnvironmentVariable(named = API_KEY_ENV, matches = ".+")
    void testUploadDocxFileWithTextExtraction() throws IOException, InterruptedException, InstantApiException {
        try (InstantApiClient client = createClient()) {
            RequestOptions options = RequestOptions.builder()
                    .conversionOptions(RequestOptions.ConversionOptions.builder()
                            .enableMethods(
                                    RequestOptions.ConversionMethod.TEXT_EXTRACTION,
                                    RequestOptions.ConversionMethod.REPORT)
                            .build())
                    .build();
            InstantApiRequest request = InstantApiRequest.create(
                    DOCX_TEST_DATA,
                    MediaType.OPENXMLFORMATS_OFFICEDOCUMENT_WORDPROCESSINGML_DOCUMENT,
                    MediaType.OPENXMLFORMATS_OFFICEDOCUMENT_WORDPROCESSINGML_DOCUMENT,
                    options);

            InstantApiResponse response = client.upload(request);

            assertNotNull(response);
            assertTrue(response.isSuccess());
            assertEquals(200, response.getStatusCode());
            JsonNode parts = MAPPER.readTree(response.getBody())
                    .get("structure")
                    .get(0)
                    .get("previous")
                    .get("textExtraction")
                    .get("parts");
            List<String> strings = parts.valueStream()
                    .flatMap(part -> part.get("strings").valueStream())
                    .map(JsonNode::asString)
                    .toList();
            assertEquals(List.of("This is a test."), strings);
        }
    }

    /**
     * Reads the entries of a ZIP archive.
     *
     * @param zipData the ZIP archive data
     * @return a map of entry names to entry contents decoded as UTF-8
     * @throws IOException if the archive cannot be read
     */
    private static Map<String, String> readZipEntries(byte[] zipData) throws IOException {
        Map<String, String> entries = new HashMap<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(zipData))) {
            for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                entries.put(entry.getName(), new String(zip.readAllBytes(), StandardCharsets.UTF_8));
            }
        }
        return entries;
    }
}
