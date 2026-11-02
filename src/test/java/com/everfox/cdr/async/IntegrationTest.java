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

import com.everfox.cdr.Api;
import com.everfox.cdr.MediaType;
import com.everfox.cdr.Region;
import com.everfox.cdr.Risk;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;

/**
 * Integration tests for the Everfox CDR Async API client.
 *
 * <p>These tests require a valid API key set in the CDR_ASYNC_API_KEY environment variable. To run: export
 * CDR_ASYNC_API_KEY=your-api-key-here &amp;&amp; mvn test -Dtest=com.everfox.cdr.async.IntegrationTest
 *
 * <p>The US_WEST_2 region is used by default. Set CDR_ASYNC_API_URL to a base URL (for example,
 * "https://eu-west-2.aws.async.cdr.everfox.com/v1"), or CDR_ASYNC_API_REGION to a region name (case-insensitive), to
 * use another. If both are set, the region takes precedence.
 *
 * <p>Tests are disabled by default if the environment variable is not set.
 */
class IntegrationTest {

    /** Environment variable name for the API key. */
    private static final String API_KEY_ENV = "CDR_ASYNC_API_KEY";
    /** Environment variable name for the API URL. */
    private static final String API_URL_ENV = "CDR_ASYNC_API_URL";
    /** Environment variable name for the API region. */
    private static final String API_REGION_ENV = "CDR_ASYNC_API_REGION";
    /** Simple PDF content for testing, shared with the Instant API tests. */
    private static final byte[] PDF_TEST_DATA = loadResource("/com/everfox/cdr/instant/test.pdf");
    /** Minimal valid JSON content for testing. */
    private static final byte[] JSON_TEST_DATA = "[]".getBytes(StandardCharsets.UTF_8);
    /** Temporary directory for file tests. */
    @TempDir
    Path tempDir;

    /** Default constructor. */
    IntegrationTest() {
        // Nothing to do
    }

    /**
     * Loads a test resource from the classpath.
     *
     * @param name the absolute name of the resource
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
     * Creates a client configured from environment variables.
     *
     * @return a configured Async API client
     */
    private AsyncApiClient createClient() {
        URI apiUri = Region.US_WEST_2.getBaseUrl(Api.ASYNC);
        String apiKey = System.getenv(API_KEY_ENV);
        assertNotNull(apiKey, "CDR_ASYNC_API_KEY environment variable must be set");

        String apiUrl = System.getenv(API_URL_ENV);
        if (apiUrl != null && !apiUrl.isEmpty()) {
            apiUri = assertDoesNotThrow(() -> new URI(apiUrl), "Invalid API URL");
        }
        String apiRegion = System.getenv(API_REGION_ENV);
        if (apiRegion != null && !apiRegion.isEmpty()) {
            apiUri = Region.fromString(apiRegion).getBaseUrl(Api.ASYNC);
        }
        return AsyncApiClient.create(AsyncApiConfig.create(apiKey, apiUri));
    }

    /**
     * Tests processing a PDF in one call.
     *
     * @throws Exception if processing fails
     */
    @Test
    @EnabledIfEnvironmentVariable(named = API_KEY_ENV, matches = ".+")
    void testProcessPdf() throws Exception {
        try (AsyncApiClient client = createClient()) {
            AsyncApiResponse response = client.process(AsyncApiRequest.create(MediaType.PDF), PDF_TEST_DATA);

            assertTrue(response.isSuccess());
            assertTrue(response.getStatus().isSuccess());
            assertTrue(response.getBody().length > 0);
        }
    }

    /**
     * Tests processing a PDF one step at a time, and that the result can only be downloaded once.
     *
     * @throws Exception if processing fails
     */
    @Test
    @EnabledIfEnvironmentVariable(named = API_KEY_ENV, matches = ".+")
    void testProcessPdfInSteps() throws Exception {
        RequestOptions options =
                RequestOptions.builder().userData("integration-test").build();
        try (AsyncApiClient client = createClient()) {
            AsyncApiStatus initiated = client.initiate(AsyncApiRequest.create(MediaType.PDF, options));
            assertFalse(initiated.isComplete());
            assertNotNull(initiated.getUploadLink());

            client.upload(initiated, PDF_TEST_DATA);
            AsyncApiStatus completed = client.waitForCompletion(initiated);
            assertTrue(completed.isSuccess(), () -> "Processing failed: " + completed.getState());
            assertEquals("integration-test", completed.getUserData());

            Path target = tempDir.resolve("out.pdf");
            long written = client.download(completed, target);
            assertEquals(Files.size(target), written);
            assertTrue(written > 0);

            AsyncApiException exception = assertThrows(AsyncApiException.class, () -> client.download(completed));
            assertEquals(410, exception.getHttpStatusCode());
        }
    }

    /** Tests that processing JSON without allowing its risks fails with the API's error. */
    @Test
    @EnabledIfEnvironmentVariable(named = API_KEY_ENV, matches = ".+")
    void testProcessJsonRisksNotAllowed() {
        try (AsyncApiClient client = createClient()) {
            AsyncApiException exception = assertThrows(
                    AsyncApiException.class,
                    () -> client.process(AsyncApiRequest.create(MediaType.JSON), JSON_TEST_DATA));

            assertEquals("RISK_NOT_ALLOWED", exception.getName());
        }
    }

    /**
     * Tests processing JSON with its risks allowed, and that the risks taken are reported.
     *
     * @throws Exception if processing fails
     */
    @Test
    @EnabledIfEnvironmentVariable(named = API_KEY_ENV, matches = ".+")
    void testProcessJsonRisksAllowed() throws Exception {
        RequestOptions options = RequestOptions.builder()
                .allowRisks(Risk.POLY_TEXT_JSON, Risk.STRUCTURED_NO_SCHEMA_JSON)
                .build();
        try (AsyncApiClient client = createClient()) {
            AsyncApiResponse response = client.process(AsyncApiRequest.create(MediaType.JSON, options), JSON_TEST_DATA);

            assertTrue(response.isSuccess());
            assertFalse(response.getRisksTaken().isEmpty());
        }
    }
}
