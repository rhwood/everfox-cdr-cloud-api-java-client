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

import com.everfox.cdr.Region;
import java.net.URI;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link InstantApiConfig}. */
class InstantApiConfigTest {

    /** Default constructor for the test class. */
    InstantApiConfigTest() {
        // Nothing to do here
    }

    /** Tests that creating an InstantApiConfig with a region sets the correct base URL. */
    @Test
    void testConfigWithRegion() {
        InstantApiConfig config = InstantApiConfig.create("test-key", Region.US_WEST_2);

        assertEquals("test-key", config.getApiKey());
        assertEquals(
                "https://us-west-2.aws.instant.cdr.everfox.com/v1",
                config.getBaseUrl().toString());
        assertEquals(10, config.getConnectTimeoutSeconds());
        assertEquals(60, config.getRequestTimeoutSeconds());
    }

    /**
     * Tests that creating an InstantApiConfig with a region and custom timeouts sets the correct base URL and timeouts.
     */
    @Test
    void testConfigWithRegionCustomTimeouts() {
        InstantApiConfig config = InstantApiConfig.create("test-key", Region.US_WEST_2, 5, 30);

        assertEquals("test-key", config.getApiKey());
        assertEquals(
                "https://us-west-2.aws.instant.cdr.everfox.com/v1",
                config.getBaseUrl().toString());
        assertEquals(5, config.getConnectTimeoutSeconds());
        assertEquals(30, config.getRequestTimeoutSeconds());
    }

    /** Tests that creating an InstantApiConfig with a custom URL sets the correct base URL. */
    @Test
    void testConfigWithCustomUrl() {
        InstantApiConfig config = InstantApiConfig.create("test-key", URI.create("https://custom.example.com"));

        assertEquals("test-key", config.getApiKey());
        assertEquals("https://custom.example.com", config.getBaseUrl().toString());
        assertEquals(10, config.getConnectTimeoutSeconds());
        assertEquals(60, config.getRequestTimeoutSeconds());
    }
    /**
     * Tests that creating an InstantApiConfig with a custom URL and custom timeouts sets the correct base URL and
     * timeouts.
     */
    @Test
    void testConfigWithCustomUrlCustomTimeouts() {
        InstantApiConfig config = InstantApiConfig.create("test-key", URI.create("https://custom.example.com"), 5, 30);

        assertEquals("test-key", config.getApiKey());
        assertEquals("https://custom.example.com", config.getBaseUrl().toString());
        assertEquals(5, config.getConnectTimeoutSeconds());
        assertEquals(30, config.getRequestTimeoutSeconds());
    }
}
