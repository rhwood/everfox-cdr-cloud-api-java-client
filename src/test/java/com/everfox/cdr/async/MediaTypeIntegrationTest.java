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
import com.everfox.cdr.ApiSpec;
import com.everfox.cdr.ConvertibleFormat;
import com.everfox.cdr.LiveApi;
import com.everfox.cdr.MediaType;
import com.everfox.cdr.TestResources;
import com.everfox.cdr.options.ConversionOptions;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Integration tests that pin down how the live Async API handles media types, as confirmed on 2–3 November 2026.
 *
 * <p>The Async API matches its published spec for media types, but differs from the Instant API in error codes and in
 * {@code conversion.from} validation. Those differences have been reported to Everfox; see "Known API Behavior" in
 * README.md. If a test here starts failing, check whether Everfox has changed the API before changing the client. These
 * tests run only when {@code CDR_ASYNC_API_KEY} is set; see {@link LiveApi}.
 */
@EnabledIfEnvironmentVariable(named = LiveApi.ASYNC_KEY_ENV, matches = ".+")
class MediaTypeIntegrationTest {

    /** A valid PDF, used where only the declared media type matters. */
    private static final byte[] PDF = TestResources.load("test.pdf");

    /** The client for the live API. */
    private AsyncApiClient client;

    /** Default constructor. */
    MediaTypeIntegrationTest() {
        // Nothing to do
    }

    /** Creates the client for the live API, polling more often than the default because the files are small. */
    @BeforeEach
    void createClient() {
        client = AsyncApiClient.create(AsyncApiConfig.builder(LiveApi.apiKey(Api.ASYNC), LiveApi.baseUrl(Api.ASYNC))
                .pollInterval(Duration.ofMillis(250))
                .processingTimeout(Duration.ofMinutes(5))
                .build());
    }

    /** Closes the client. */
    @AfterEach
    void closeClient() {
        client.close();
    }

    /**
     * Creates a request for {@code application/octet-stream} that converts from a media type.
     *
     * @param mediaType the media type to convert from
     * @return the request
     */
    private static AsyncApiRequest convertFrom(String mediaType) {
        return AsyncApiRequest.create(
                MediaType.OCTET_STREAM,
                RequestOptions.builder()
                        .conversionOptions(
                                ConversionOptions.builder().from(mediaType).build())
                        .build());
    }

    /**
     * Asserts that an exception is the API gateway's rejection of a request body that doesn't match the spec.
     *
     * @param exception the exception
     */
    private static void assertValidationFailure(AsyncApiException exception) {
        assertEquals(400, exception.getHttpStatusCode());
        assertEquals(6010, exception.getApiStatusCode());
        assertEquals("APIGATEWAY_BAD_REQUEST_BODY", exception.getName());
        assertEquals("ValidationFailure", exception.getSubType());
    }

    /**
     * Asserts that a response is an Office Open XML file (a ZIP archive) of the expected media type.
     *
     * @param response the response
     * @param expected the expected media type
     */
    private static void assertOfficeOpenXml(AsyncApiResponse response, MediaType expected) {
        assertTrue(response.isSuccess());
        assertEquals(expected.getMediaType(), response.getHeader("Content-Type"));
        assertEquals("PK", new String(response.getBody(), 0, 2, StandardCharsets.US_ASCII));
    }

    /**
     * Tests that a convertible format is rejected when initiating with it as the {@code mime-type}, as the Async spec
     * says. The Instant API rejects it too, but with error 1000.
     *
     * @param format the format
     */
    @ParameterizedTest
    @EnumSource(ConvertibleFormat.class)
    void testRejectedAsMimeType(ConvertibleFormat format) {
        assertValidationFailure(assertThrows(
                AsyncApiException.class, () -> client.initiate(AsyncApiRequest.create(format.getMediaType()))));
    }

    /** Tests that the allowed {@code mime-type} values listed in the rejection message are exactly the spec's. */
    @Test
    void testAllowedMimeTypesMatchSpec() {
        AsyncApiException exception = assertThrows(
                AsyncApiException.class,
                () -> client.initiate(AsyncApiRequest.create(ConvertibleFormat.DOC.getMediaType())));

        assertEquals(
                ApiSpec.ASYNC.contentTypes(), LiveApi.mediaTypesAfter(exception.getApiMessage(), "possible values:"));
    }

    /** Tests that the allowed {@code conversion.from} values listed in the rejection message are exactly the spec's. */
    @Test
    void testAllowedConversionSourcesMatchSpec() {
        AsyncApiException exception = assertThrows(
                AsyncApiException.class, () -> client.initiate(convertFrom(MediaType.TEXT_CSS.getMediaType())));

        assertEquals(
                ApiSpec.ASYNC.conversionSources(),
                LiveApi.mediaTypesAfter(exception.getApiMessage(), "possible values:"));
    }

    /**
     * Tests that a convertible format is converted to Office Open XML when sent as {@code application/octet-stream}
     * with the format as {@code conversion.from}.
     *
     * @param format the format
     * @throws Exception if processing fails
     */
    @ParameterizedTest
    @EnumSource(ConvertibleFormat.class)
    void testConvertedFromOctetStream(ConvertibleFormat format) throws Exception {
        AsyncApiResponse response = client.process(convertFrom(format.getMediaType()), format.load());

        assertOfficeOpenXml(response, format.getOutput().getMediaType());
    }

    /**
     * Tests that an Office Open XML file is accepted as its own media type and returned in the same format.
     *
     * @param format the format
     * @throws Exception if processing fails
     */
    @ParameterizedTest
    @EnumSource(ConvertibleFormat.Output.class)
    void testOfficeOpenXmlAcceptedAsMimeType(ConvertibleFormat.Output format) throws Exception {
        AsyncApiResponse response = client.process(AsyncApiRequest.create(format.getMediaType()), format.load());

        assertOfficeOpenXml(response, format.getMediaType());
    }

    /** Tests that {@code application/octet-stream} without {@code conversion.from} is rejected. */
    @Test
    void testOctetStreamRequiresConversionFrom() {
        AsyncApiException exception = assertThrows(
                AsyncApiException.class, () -> client.initiate(AsyncApiRequest.create(MediaType.OCTET_STREAM)));

        assertEquals(400, exception.getHttpStatusCode());
        assertEquals(1090, exception.getApiStatusCode());
        assertEquals("CONVERSION_UNSUPPORTED", exception.getName());
    }

    /** Tests that {@code application/ms-tnef} is recognized but only available in preview mode. */
    @Test
    void testTnefIsPreviewOnly() {
        AsyncApiException exception =
                assertThrows(AsyncApiException.class, () -> client.initiate(AsyncApiRequest.create(MediaType.MS_TNEF)));

        assertEquals(400, exception.getHttpStatusCode());
        assertEquals(1010, exception.getApiStatusCode());
        assertEquals("PREVIEW_MODE_ONLY", exception.getName());
    }

    /**
     * Tests that {@code text/css} is rejected as {@code conversion.from}, as the Async spec says. The Instant API
     * accepts it.
     */
    @Test
    void testCssRejectedAsConversionFrom() {
        assertValidationFailure(assertThrows(
                AsyncApiException.class, () -> client.initiate(convertFrom(MediaType.TEXT_CSS.getMediaType()))));
    }

    /**
     * Tests that content that doesn't match its declared media type fails processing, reported in the job status as
     * {@code PROCESSING} / {@code Invalid data} (the Instant API reports it as {@code BadRequest} with no sub-type).
     */
    @Test
    void testMismatchedContentNotRecognised() {
        AsyncApiRequest request = AsyncApiRequest.create(ConvertibleFormat.Output.XLSX.getMediaType());
        AsyncApiException exception = assertThrows(AsyncApiException.class, () -> client.process(request, PDF));

        assertEquals(200, exception.getHttpStatusCode());
        assertEquals(3020, exception.getApiStatusCode());
        assertEquals("PROCESSING_NOT_RECOGNISED", exception.getName());
        assertEquals("PROCESSING", exception.getType());
        assertEquals("Invalid data", exception.getSubType());
    }
}
