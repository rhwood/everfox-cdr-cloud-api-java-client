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
import org.junit.jupiter.api.Test;

/** Unit tests for {@link InstantApiClient}. */
class InstantApiClientTest {

    /** Default constructor for the test class. */
    InstantApiClientTest() {
        // Nothing to do here
    }

    /** Tests that creating an InstantApiClient with a null configuration throws a NullPointerException. */
    @Test
    void testClientWithNullConfig() {
        assertThrows(NullPointerException.class, () -> {
            InstantApiClient.create(null);
        });
    }

    /** Tests that creating an InstantApiClient with a valid configuration succeeds. */
    @Test
    void testClientCreation() {
        InstantApiConfig config = InstantApiConfig.create("test-key", Region.US_WEST_2);
        InstantApiClient client = InstantApiClient.create(config);

        assertEquals(config, client.getConfig());
    }
}
