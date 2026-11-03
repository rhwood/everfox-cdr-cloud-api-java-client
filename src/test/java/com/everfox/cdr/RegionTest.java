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

import java.net.URI;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

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

    /**
     * Tests that {@link Region#getBaseUrl(Api)} returns the host of each API in each region.
     *
     * @param region the region
     */
    @ParameterizedTest
    @EnumSource(Region.class)
    void testGetBaseUrl(Region region) {
        String awsRegion = region.name().toLowerCase(Locale.ROOT).replace('_', '-');
        for (Api api : Api.values()) {
            assertEquals(
                    URI.create("https://" + awsRegion + ".aws." + api.getSubdomain() + ".cdr.everfox.com/v1"),
                    region.getBaseUrl(api));
        }
        assertEquals(
                "https://eu-west-1.aws.async.cdr.everfox.com/v1",
                Region.EU_WEST_1.getBaseUrl(Api.ASYNC).toString());
    }

    /** Tests that {@link Region#getBaseUrl(Api)} rejects a null API. */
    @Test
    void testGetBaseUrlNullApi() {
        assertThrows(NullPointerException.class, () -> Region.US_WEST_2.getBaseUrl(null));
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
