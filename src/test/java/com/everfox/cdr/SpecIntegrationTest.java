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

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Checks that the OpenAPI specs checked in as test resources ({@link ApiSpec}) still match the specs Everfox publishes.
 *
 * <p>The specs are public, so this test needs no API key, but it needs network access. It is skipped if a spec can't be
 * downloaded, and fails if the download returns an error status or a spec has changed. If it fails, download the spec
 * from the URL in the failure message, replace the checked-in copy, and run {@code SpecConformanceTest}, which shows
 * whether {@code MediaType} or {@code Risk} need updating.
 *
 * <p>The name ends in {@code IntegrationTest} so that {@code -Dtest='*IntegrationTest'} includes it and
 * {@code -Dtest='!*IntegrationTest'} excludes it, with the other tests that need the network.
 */
class SpecIntegrationTest {

    /** Object mapper for parsing the published specs. */
    private static final ObjectMapper MAPPER = new ObjectMapper();
    /** The most differences to list in a failure message. */
    private static final int MAX_DIFFERENCES = 20;

    /** Default constructor. */
    SpecIntegrationTest() {
        // Nothing to do
    }

    /**
     * Tests that the checked-in spec is the same JSON as the published spec. The comparison is of the parsed JSON, so
     * changes in formatting alone don't fail it.
     *
     * @param spec the spec
     * @throws InterruptedException if the download is interrupted
     */
    @ParameterizedTest
    @EnumSource(ApiSpec.class)
    void testCheckedInSpecMatchesPublishedSpec(ApiSpec spec) throws InterruptedException {
        HttpResponse<byte[]> response;
        try (HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build()) {
            response = client.send(
                    HttpRequest.newBuilder(spec.publishedUrl())
                            .timeout(Duration.ofSeconds(30))
                            .GET()
                            .build(),
                    HttpResponse.BodyHandlers.ofByteArray());
        } catch (IOException e) {
            Assumptions.abort("Can't download " + spec.publishedUrl() + ": " + e);
            return;
        }
        assertEquals(200, response.statusCode(), "HTTP status for " + spec.publishedUrl());

        JsonNode published = MAPPER.readTree(response.body());
        List<String> differences = new ArrayList<>();
        diff("", spec.json(), published, differences);

        assertTrue(
                differences.isEmpty(),
                () -> "The checked-in " + spec + " spec differs from " + spec.publishedUrl() + " at:\n  "
                        + String.join("\n  ", differences));
    }

    /**
     * Collects the JSON pointers at which two JSON values differ, up to {@link #MAX_DIFFERENCES}.
     *
     * @param pointer the JSON pointer of the values
     * @param checkedIn the checked-in value, or null if missing
     * @param published the published value, or null if missing
     * @param differences the differences found so far
     */
    private static void diff(String pointer, JsonNode checkedIn, JsonNode published, List<String> differences) {
        if (differences.size() >= MAX_DIFFERENCES || (checkedIn != null && checkedIn.equals(published))) {
            return;
        }
        String at = pointer.isEmpty() ? "/" : pointer;
        if (checkedIn == null || published == null) {
            differences.add(at + (checkedIn == null ? " (only published)" : " (only checked in)"));
        } else if (checkedIn.isObject() && published.isObject()) {
            Set<String> names = new HashSet<>();
            checkedIn.properties().stream().map(Map.Entry::getKey).forEach(names::add);
            published.properties().stream().map(Map.Entry::getKey).forEach(names::add);
            names.stream()
                    .sorted()
                    .forEach(name -> diff(
                            pointer + "/" + name.replace("~", "~0").replace("/", "~1"),
                            checkedIn.get(name),
                            published.get(name),
                            differences));
        } else if (checkedIn.isArray() && published.isArray() && checkedIn.size() == published.size()) {
            for (int i = 0; i < checkedIn.size(); i++) {
                diff(pointer + "/" + i, checkedIn.get(i), published.get(i), differences);
            }
        } else {
            differences.add(at);
        }
    }
}
