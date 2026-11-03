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
import com.everfox.cdr.CdrApiConfig;
import com.everfox.cdr.Region;
import java.net.URI;
import java.time.Duration;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link InstantApiConfig} and the shared {@link CdrApiConfig} settings. */
class InstantApiConfigTest {

    /** A custom base URL for testing. */
    private static final URI BASE_URL = URI.create("https://custom.example.com");

    /** Default constructor for the test class. */
    InstantApiConfigTest() {
        // Nothing to do here
    }

    /** Tests that creating a configuration with a region uses its Instant endpoint and the default timeouts. */
    @Test
    void testCreateWithRegion() {
        InstantApiConfig config = InstantApiConfig.create("test-key", Region.US_WEST_2);

        assertEquals("test-key", config.getApiKey());
        assertEquals(Region.US_WEST_2.getBaseUrl(Api.INSTANT), config.getBaseUrl());
        assertEquals(CdrApiConfig.DEFAULT_CONNECT_TIMEOUT, config.getConnectTimeout());
        assertEquals(CdrApiConfig.DEFAULT_REQUEST_TIMEOUT, config.getRequestTimeout());
    }

    /** Tests that builder settings can be chained in any order and are used. */
    @Test
    void testBuilderWithCustomUrlAndTimeouts() {
        InstantApiConfig config = InstantApiConfig.builder("test-key", BASE_URL)
                .requestTimeout(Duration.ofSeconds(30))
                .connectTimeout(Duration.ofMillis(500))
                .build();

        assertEquals(BASE_URL, config.getBaseUrl());
        assertEquals(Duration.ofMillis(500), config.getConnectTimeout());
        assertEquals(Duration.ofSeconds(30), config.getRequestTimeout());
        assertEquals(
                Region.EU_WEST_1.getBaseUrl(Api.INSTANT),
                InstantApiConfig.builder("test-key", Region.EU_WEST_1).build().getBaseUrl());
    }

    /** Tests that required arguments and timeouts are validated. */
    @Test
    void testValidation() {
        assertThrows(NullPointerException.class, () -> InstantApiConfig.create(null, Region.US_WEST_2));
        assertThrows(NullPointerException.class, () -> InstantApiConfig.create("key", null));
        assertThrows(NullPointerException.class, () -> InstantApiConfig.builder("key", (URI) null));
        InstantApiConfig.Builder builder = InstantApiConfig.builder("key", BASE_URL);
        assertThrows(NullPointerException.class, () -> builder.connectTimeout(null));
        assertThrows(IllegalArgumentException.class, () -> builder.connectTimeout(Duration.ZERO));
        assertThrows(IllegalArgumentException.class, () -> builder.requestTimeout(Duration.ofSeconds(-1)));
    }
}
