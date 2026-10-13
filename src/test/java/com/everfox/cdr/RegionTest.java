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

import org.junit.jupiter.api.Test;

/** Unit tests for the {@link Region} enum. */
class RegionTest {

    /** Default constructor. */
    RegionTest() {
        // Nothing to do
    }

    /** Tests that the {@link Region#toString()} method returns the correct string representation of the region. */
    @Test
    void testToString() {
        Region region = Region.EU_WEST_1;
        assertEquals("EU_WEST_1", region.toString());
    }

    /** Tests that the {@link Region#getBaseUrl()} method returns the correct base URL for the region. */
    @Test
    void testGetBaseUrl() {
        Region region = Region.EU_WEST_1;
        assertEquals(
                "https://eu-west-1.aws.instant.cdr.everfox.com/v1",
                region.getBaseUrl().toString());
    }

    /** Tests that the {@link Region#fromString(String)} method returns the correct region for a valid string. */
    @Test
    void testFromString() {
        Region region = Region.fromString("eu_west_1");
        assertEquals(Region.EU_WEST_1, region);
    }

    /** Tests that the {@link Region#fromString(String)} method throws an exception for an invalid string. */
    @Test
    void testFromStringInvalid() {
        assertThrows(IllegalArgumentException.class, () -> {
            Region.fromString("invalid_region");
        });
    }
}
