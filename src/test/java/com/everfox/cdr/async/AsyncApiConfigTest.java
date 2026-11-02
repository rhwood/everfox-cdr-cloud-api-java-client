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
import com.everfox.cdr.CdrApiConfig;
import com.everfox.cdr.Region;
import java.net.URI;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** Unit tests for {@link AsyncApiConfig}. The shared settings are also tested in {@code InstantApiConfigTest}. */
class AsyncApiConfigTest {

    /** A custom base URL for testing. */
    private static final URI BASE_URL = URI.create("https://example.com/v1");

    /** Default constructor for the test class. */
    AsyncApiConfigTest() {
        // Nothing to do here
    }

    /**
     * Tests that creating a configuration from a region uses the region's Async API host.
     *
     * @param region the region
     */
    @ParameterizedTest
    @EnumSource(Region.class)
    void testRegionBaseUrl(Region region) {
        assertEquals(
                region.getBaseUrl(Api.ASYNC),
                AsyncApiConfig.create("key", region).getBaseUrl());
        assertEquals(
                region.getBaseUrl(Api.ASYNC),
                AsyncApiConfig.builder("key", region).build().getBaseUrl());
    }

    /** Tests that the default timeouts and polling behavior are used when not specified. */
    @Test
    void testDefaults() {
        AsyncApiConfig config = AsyncApiConfig.builder("key", BASE_URL).build();

        assertEquals("key", config.getApiKey());
        assertEquals(BASE_URL, config.getBaseUrl());
        assertEquals(CdrApiConfig.DEFAULT_CONNECT_TIMEOUT, config.getConnectTimeout());
        assertEquals(CdrApiConfig.DEFAULT_REQUEST_TIMEOUT, config.getRequestTimeout());
        assertEquals(AsyncApiConfig.DEFAULT_POLL_INTERVAL, config.getPollInterval());
        assertEquals(AsyncApiConfig.DEFAULT_PROCESSING_TIMEOUT, config.getProcessingTimeout());
    }

    /** Tests that shared and async-only settings can be chained in any order and are used. */
    @Test
    void testCustomSettings() {
        AsyncApiConfig config = AsyncApiConfig.builder("key", Region.EU_WEST_2)
                .pollInterval(Duration.ofMillis(250))
                .connectTimeout(Duration.ofSeconds(1))
                .processingTimeout(Duration.ofMinutes(30))
                .requestTimeout(Duration.ofMinutes(5))
                .build();

        assertEquals(Duration.ofSeconds(1), config.getConnectTimeout());
        assertEquals(Duration.ofMinutes(5), config.getRequestTimeout());
        assertEquals(Duration.ofMillis(250), config.getPollInterval());
        assertEquals(Duration.ofMinutes(30), config.getProcessingTimeout());
    }

    /** Tests that required arguments and durations are validated. */
    @Test
    void testValidation() {
        assertThrows(NullPointerException.class, () -> AsyncApiConfig.create("key", null));
        assertThrows(NullPointerException.class, () -> AsyncApiConfig.builder(null, BASE_URL));
        assertThrows(NullPointerException.class, () -> AsyncApiConfig.builder("key", (URI) null));
        AsyncApiConfig.Builder builder = AsyncApiConfig.builder("key", BASE_URL);
        assertThrows(IllegalArgumentException.class, () -> builder.pollInterval(Duration.ZERO));
        assertThrows(IllegalArgumentException.class, () -> builder.processingTimeout(Duration.ofSeconds(-1)));
        assertThrows(NullPointerException.class, () -> builder.pollInterval(null));
    }
}
