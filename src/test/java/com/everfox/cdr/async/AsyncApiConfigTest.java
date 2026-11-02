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
import com.everfox.cdr.Region;
import java.net.URI;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** Unit tests for {@link AsyncApiConfig}. */
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
                AsyncApiConfig.create("key", region, 1, 1, 1, 1).getBaseUrl());
    }

    /** Tests that a null region throws a NullPointerException. */
    @Test
    void testNullRegion() {
        assertThrows(NullPointerException.class, () -> AsyncApiConfig.create("key", (Region) null));
    }

    /** Tests that the default timeouts and polling behavior are used when not specified. */
    @Test
    void testDefaults() {
        AsyncApiConfig config = AsyncApiConfig.create("key", BASE_URL);

        assertEquals("key", config.getApiKey());
        assertEquals(BASE_URL, config.getBaseUrl());
        assertEquals(AsyncApiConfig.DEFAULT_CONNECT_TIMEOUT_SECONDS, config.getConnectTimeoutSeconds());
        assertEquals(AsyncApiConfig.DEFAULT_REQUEST_TIMEOUT_SECONDS, config.getRequestTimeoutSeconds());
        assertEquals(AsyncApiConfig.DEFAULT_POLL_INTERVAL_SECONDS, config.getPollIntervalSeconds());
        assertEquals(AsyncApiConfig.DEFAULT_PROCESSING_TIMEOUT_SECONDS, config.getProcessingTimeoutSeconds());
    }

    /** Tests that custom timeouts are used. */
    @Test
    void testCustomTimeouts() {
        AsyncApiConfig config = AsyncApiConfig.create("key", Region.EU_WEST_1, 1, 2);

        assertEquals(1, config.getConnectTimeoutSeconds());
        assertEquals(2, config.getRequestTimeoutSeconds());
        assertEquals(AsyncApiConfig.DEFAULT_POLL_INTERVAL_SECONDS, config.getPollIntervalSeconds());
    }

    /** Tests that custom polling behavior is used. */
    @Test
    void testCustomPolling() {
        AsyncApiConfig config = AsyncApiConfig.create("key", Region.EU_WEST_2, 1, 2, 3, 4);

        assertEquals(1, config.getConnectTimeoutSeconds());
        assertEquals(2, config.getRequestTimeoutSeconds());
        assertEquals(3, config.getPollIntervalSeconds());
        assertEquals(4, config.getProcessingTimeoutSeconds());
    }

    /** Tests that required arguments are validated. */
    @Test
    void testValidation() {
        assertThrows(NullPointerException.class, () -> AsyncApiConfig.create(null, BASE_URL));
        assertThrows(NullPointerException.class, () -> AsyncApiConfig.create("key", (URI) null));
        assertThrows(IllegalArgumentException.class, () -> AsyncApiConfig.create("key", BASE_URL, 1, 1, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> AsyncApiConfig.create("key", BASE_URL, 1, 1, 1, 0));
    }
}
