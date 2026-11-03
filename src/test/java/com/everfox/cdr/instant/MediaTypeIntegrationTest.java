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

import com.everfox.cdr.Api;
import com.everfox.cdr.ApiSpec;
import com.everfox.cdr.ConvertibleFormat;
import com.everfox.cdr.LiveApi;
import com.everfox.cdr.MediaType;
import com.everfox.cdr.TestResources;
import com.everfox.cdr.options.ConversionOptions;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Integration tests that pin down how the live Instant API handles media types, as confirmed on 2–3 November 2026.
 *
 * <p>Several of these behaviors contradict the published Instant API spec, or differ from the Async API, and have been
 * reported to Everfox; see REFACTORING.md section 6. If a test here starts failing, check whether Everfox has changed
 * the API before changing the client. These tests run only when {@code CDR_INSTANT_API_KEY} is set; see
 * {@link LiveApi}.
 */
@EnabledIfEnvironmentVariable(named = LiveApi.INSTANT_KEY_ENV, matches = ".+")
class MediaTypeIntegrationTest {

    /** A valid PDF, used where only the declared media type matters. */
    private static final byte[] PDF = TestResources.load("test.pdf");
    /** A valid style sheet. */
    private static final byte[] CSS = "body { color: black; }\n".getBytes(StandardCharsets.UTF_8);

    /** The client for the live API. */
    private InstantApiClient client;

    /** Default constructor. */
    MediaTypeIntegrationTest() {
        // Nothing to do
    }

    /** Creates the client for the live API. */
    @BeforeEach
    void createClient() {
        client = InstantApiClient.create(
                InstantApiConfig.builder(LiveApi.apiKey(Api.INSTANT), LiveApi.baseUrl(Api.INSTANT))
                        .build());
    }

    /** Closes the client. */
    @AfterEach
    void closeClient() {
        client.close();
    }

    /**
     * Creates options that convert from a media type.
     *
     * @param mediaType the media type to convert from
     * @return the options
     */
    private static RequestOptions convertFrom(String mediaType) {
        return RequestOptions.builder()
                .conversionOptions(ConversionOptions.builder().from(mediaType).build())
                .build();
    }

    /**
     * Asserts that a response is an Office Open XML file (a ZIP archive) of the expected media type.
     *
     * @param response the response
     * @param expected the expected media type
     */
    private static void assertOfficeOpenXml(InstantApiResponse response, MediaType expected) {
        assertTrue(response.isSuccess());
        assertEquals(expected.getMediaType(), response.getHeader("Content-Type"));
        assertEquals("PK", new String(response.getBody(), 0, 2, StandardCharsets.US_ASCII));
    }

    /**
     * Tests that a genuine file of a convertible format is rejected when sent as its own media type, although the
     * Instant spec lists the type as a valid {@code Content-Type}. The request accepts {@code *}{@code /*} so that only
     * the {@code Content-Type} is checked; see {@link #testAcceptComparedCaseSensitively()}.
     *
     * @param format the format
     */
    @ParameterizedTest
    @EnumSource(ConvertibleFormat.class)
    void testRejectedAsContentType(ConvertibleFormat format) {
        InstantApiException exception = assertThrows(
                InstantApiException.class,
                () -> client.upload(
                        InstantApiRequest.create(format.load(), format.getMediaType(), MediaType.ALL.getMediaType())));

        assertEquals(400, exception.getHttpStatusCode());
        assertEquals(1000, exception.getApiStatusCode());
        assertEquals("CONTENT_TYPE_UNSUPPORTED", exception.getName());
    }

    /**
     * Tests that the allowed {@code Content-Type} values listed in the rejection message are exactly the Async spec's
     * {@code mime-type} list, and not the Instant spec's own {@code Content-Type} list.
     */
    @Test
    void testAllowedContentTypesMatchAsyncSpec() {
        InstantApiException exception = assertThrows(
                InstantApiException.class,
                () -> client.upload(InstantApiRequest.create(
                        ConvertibleFormat.DOC.load(),
                        ConvertibleFormat.DOC.getMediaType(),
                        MediaType.ALL.getMediaType())));

        assertEquals(
                ApiSpec.ASYNC.contentTypes(), LiveApi.mediaTypesAfter(exception.getApiMessage(), "must be one of"));
    }

    /**
     * Tests that the API matches the {@code Content-Type} case-insensitively but the {@code Accept} header
     * case-sensitively: it reads the lower-case {@code .xlsb} type as {@code ...macroEnabled.12}, then rejects an
     * {@code Accept} of the same lower-case type before checking whether the type is supported.
     */
    @Test
    void testAcceptComparedCaseSensitively() {
        String lowerCase = ConvertibleFormat.XLSB_LOWER_CASE.getMediaType();
        InstantApiException exception = assertThrows(
                InstantApiException.class,
                () -> client.upload(
                        InstantApiRequest.create(ConvertibleFormat.XLSB_LOWER_CASE.load(), lowerCase, lowerCase)));

        assertEquals(400, exception.getHttpStatusCode());
        assertEquals(1020, exception.getApiStatusCode());
        assertEquals("ACCEPT_HEADER_INVALID", exception.getName());
        assertTrue(exception.getApiMessage().contains(ConvertibleFormat.XLSB.getMediaType()));
    }

    /**
     * Tests that a convertible format is converted to Office Open XML when sent as {@code application/octet-stream}
     * with the format as {@code conversion.from}, although the Instant spec does not list
     * {@code application/octet-stream} as a valid {@code Content-Type}.
     *
     * @param format the format
     * @throws Exception if the upload fails
     */
    @ParameterizedTest
    @EnumSource(ConvertibleFormat.class)
    void testConvertedFromOctetStream(ConvertibleFormat format) throws Exception {
        MediaType output = format.getOutput().getMediaType();
        InstantApiResponse response = client.upload(InstantApiRequest.create(
                format.load(), MediaType.OCTET_STREAM, output, convertFrom(format.getMediaType())));

        assertOfficeOpenXml(response, output);
    }

    /**
     * Tests that an Office Open XML file is accepted as its own media type and returned in the same format.
     *
     * @param format the format
     * @throws Exception if the upload fails
     */
    @ParameterizedTest
    @EnumSource(ConvertibleFormat.Output.class)
    void testOfficeOpenXmlAcceptedAsContentType(ConvertibleFormat.Output format) throws Exception {
        MediaType mediaType = format.getMediaType();
        InstantApiResponse response = client.upload(InstantApiRequest.create(format.load(), mediaType, mediaType));

        assertOfficeOpenXml(response, mediaType);
    }

    /** Tests that {@code application/octet-stream} without {@code conversion.from} is rejected. */
    @Test
    void testOctetStreamRequiresConversionFrom() {
        InstantApiException exception = assertThrows(
                InstantApiException.class,
                () -> client.upload(InstantApiRequest.create(PDF, MediaType.OCTET_STREAM, MediaType.ALL)));

        assertEquals(400, exception.getHttpStatusCode());
        assertEquals(1090, exception.getApiStatusCode());
        assertEquals("CONVERSION_UNSUPPORTED", exception.getName());
    }

    /**
     * Tests that, without {@code conversion.from}, {@code application/octet-stream} must be accepted, and that the
     * {@code Accept} header is checked before the missing {@code conversion.from}.
     */
    @Test
    void testOctetStreamWithoutConversionFromChecksAcceptFirst() {
        InstantApiException exception = assertThrows(
                InstantApiException.class,
                () -> client.upload(InstantApiRequest.create(PDF, MediaType.OCTET_STREAM, MediaType.PDF)));

        assertEquals(400, exception.getHttpStatusCode());
        assertEquals(1020, exception.getApiStatusCode());
        assertEquals("ACCEPT_HEADER_INVALID", exception.getName());
    }

    /**
     * Tests that {@code application/ms-tnef} is recognized but only available in preview mode, although the Instant
     * spec does not list it.
     */
    @Test
    void testTnefIsPreviewOnly() {
        InstantApiException exception = assertThrows(
                InstantApiException.class,
                () -> client.upload(InstantApiRequest.create(PDF, MediaType.MS_TNEF, MediaType.MS_TNEF)));

        assertEquals(400, exception.getHttpStatusCode());
        assertEquals(1010, exception.getApiStatusCode());
        assertEquals("PREVIEW_MODE_ONLY", exception.getName());
    }

    /**
     * Tests that {@code text/css} is accepted as {@code conversion.from}, although neither spec lists it there and the
     * Async API rejects it.
     *
     * @throws Exception if the upload fails
     */
    @Test
    void testCssAcceptedAsConversionFrom() throws Exception {
        InstantApiResponse response = client.upload(InstantApiRequest.create(
                CSS, MediaType.OCTET_STREAM, MediaType.TEXT_CSS, convertFrom(MediaType.TEXT_CSS.getMediaType())));

        assertTrue(response.isSuccess());
        assertEquals(MediaType.TEXT_CSS.getMediaType(), response.getHeader("Content-Type"));
    }

    /**
     * Tests that content that doesn't match its declared media type is rejected after processing starts, as a
     * {@code BadRequest} with no sub-type (the Async API reports it as {@code PROCESSING} / {@code Invalid data}).
     */
    @Test
    void testMismatchedContentNotRecognised() {
        MediaType xlsx = ConvertibleFormat.Output.XLSX.getMediaType();
        InstantApiException exception =
                assertThrows(InstantApiException.class, () -> client.upload(InstantApiRequest.create(PDF, xlsx, xlsx)));

        assertEquals(400, exception.getHttpStatusCode());
        assertEquals(3020, exception.getApiStatusCode());
        assertEquals("PROCESSING_NOT_RECOGNISED", exception.getName());
        assertEquals("BadRequest", exception.getType());
        assertNull(exception.getSubType());
    }
}
