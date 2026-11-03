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
package com.everfox.cdr;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Checks {@link Risk} and {@link MediaType} against the checked-in OpenAPI specs ({@link ApiSpec}).
 *
 * <p>Media types are checked against the Async spec, which matches the live APIs. The Instant spec's
 * {@code Content-Type} list is known to be wrong (see "Known API Behavior" in README.md), and
 * {@link #testInstantSpecContentTypesStillDiffer()} fails when Everfox changes it. If a test here fails after
 * refreshing a spec, update {@code MediaType} or {@code Risk} to match.
 */
class SpecConformanceTest {

    /** Default constructor. */
    SpecConformanceTest() {
        // Nothing to do
    }

    /**
     * Returns the lower-case media type of a constant.
     *
     * @param mediaType the constant
     * @return the lower-case media type
     */
    private static String lowerCase(MediaType mediaType) {
        return mediaType.getMediaType().toLowerCase(Locale.ROOT);
    }

    /**
     * Tests that {@link Risk} has exactly the risks each spec allows in each risks list.
     *
     * @param list the risks list
     */
    @ParameterizedTest
    @ValueSource(strings = {"allow", "deny"})
    void testRiskMatchesSpecs(String list) {
        Set<String> risks = Arrays.stream(Risk.values()).map(Risk::getRisk).collect(Collectors.toSet());

        for (ApiSpec spec : ApiSpec.values()) {
            assertEquals(spec.risks(list), risks, spec + " " + list);
        }
    }

    /**
     * Tests that {@link MediaType#isAcceptedAsContentType()} agrees with the Async spec's {@code mime-type} list.
     *
     * @param mediaType the constant
     */
    @ParameterizedTest
    @EnumSource(MediaType.class)
    void testContentTypeFlagMatchesSpec(MediaType mediaType) {
        Set<String> accepted = ApiSpec.lowerCase(ApiSpec.ASYNC.contentTypes());

        assertEquals(accepted.contains(lowerCase(mediaType)), mediaType.isAcceptedAsContentType(), mediaType.name());
    }

    /**
     * Tests that {@link MediaType#isAcceptedAsConversionSource()} agrees with both specs' {@code conversion.from}
     * lists, which are identical.
     *
     * @param mediaType the constant
     */
    @ParameterizedTest
    @EnumSource(MediaType.class)
    void testConversionSourceFlagMatchesSpecs(MediaType mediaType) {
        for (ApiSpec spec : ApiSpec.values()) {
            Set<String> accepted = ApiSpec.lowerCase(spec.conversionSources());

            assertEquals(
                    accepted.contains(lowerCase(mediaType)),
                    mediaType.isAcceptedAsConversionSource(),
                    spec + " " + mediaType.name());
        }
    }

    /** Tests that every media type the Async spec accepts, in either place, has a {@link MediaType} constant. */
    @Test
    void testEveryAcceptedTypeHasConstant() {
        Set<String> constants = Arrays.stream(MediaType.values())
                .map(SpecConformanceTest::lowerCase)
                .collect(Collectors.toSet());
        Set<String> accepted = new HashSet<>(ApiSpec.lowerCase(ApiSpec.ASYNC.contentTypes()));
        accepted.addAll(ApiSpec.lowerCase(ApiSpec.ASYNC.conversionSources()));

        accepted.removeAll(constants);
        assertEquals(Set.of(), accepted, "media types without a MediaType constant");
    }

    /** Tests that only {@link MediaType#ALL} and {@link MediaType#TEXT_PLAIN} are accepted nowhere. */
    @Test
    void testOnlyAllAndTextPlainAreAcceptedNowhere() {
        Set<MediaType> nowhere = Arrays.stream(MediaType.values())
                .filter(t -> !t.isAcceptedAsContentType() && !t.isAcceptedAsConversionSource())
                .collect(Collectors.toSet());

        assertEquals(Set.of(MediaType.ALL, MediaType.TEXT_PLAIN), nowhere);
    }

    /**
     * Tests that the Instant spec's {@code Content-Type} list still differs from the live APIs (and the Async spec) in
     * the known way. When this fails, Everfox has changed the Instant spec: check whether it now matches the Async
     * spec, and update this test and "Known API Behavior" in README.md.
     */
    @Test
    void testInstantSpecContentTypesStillDiffer() {
        Set<String> instant = ApiSpec.INSTANT.contentTypes();
        Set<String> async = ApiSpec.ASYNC.contentTypes();
        Set<String> onlyInstant = new HashSet<>(instant);
        onlyInstant.removeAll(async);
        Set<String> onlyAsync = new HashSet<>(async);
        onlyAsync.removeAll(instant);

        assertEquals(
                Set.of(
                        "application/msword",
                        "application/rtf",
                        "text/rtf",
                        "application/vnd.ms-excel",
                        "application/vnd.ms-excel.sheet.binary.macroEnabled.12",
                        "application/vnd.ms-excel.sheet.binary.macroenabled.12",
                        "application/vnd.ms-powerpoint",
                        "application/vnd.oasis.opendocument.presentation",
                        "application/vnd.oasis.opendocument.spreadsheet",
                        "application/vnd.oasis.opendocument.text",
                        "text/csv"),
                onlyInstant,
                "listed only by the Instant spec (rejected by the live Instant API)");
        assertEquals(
                Set.of("application/octet-stream", "application/ms-tnef", "text/css"),
                onlyAsync,
                "listed only by the Async spec (accepted by the live Instant API)");
    }
}
