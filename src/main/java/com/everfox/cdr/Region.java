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

import java.net.URI;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/** Available API regions. */
public enum Region {
    /** EU West (Ireland) region. */
    EU_WEST_1("eu-west-1"),
    /** EU West (London) region. */
    EU_WEST_2("eu-west-2"),
    /** US West (Oregon) region. */
    US_WEST_2("us-west-2");

    /** The base URL of each API in the region. */
    private final Map<Api, URI> baseUrls = new EnumMap<>(Api.class);

    /**
     * Constructs a Region enum constant with the specified AWS region name.
     *
     * @param awsRegion the AWS region name, which identifies the region in API host names
     */
    Region(String awsRegion) {
        for (Api api : Api.values()) {
            baseUrls.put(
                    api, URI.create("https://" + awsRegion + ".aws." + api.getSubdomain() + ".cdr.everfox.com/v1"));
        }
    }

    /**
     * Returns the base URL of an API in the region.
     *
     * @param api the API
     * @return the base URL
     */
    public URI getBaseUrl(Api api) {
        Objects.requireNonNull(api, "api cannot be null");
        return baseUrls.get(api);
    }

    /**
     * Returns the Region enum corresponding to the given string.
     *
     * @param region the region string (case-insensitive)
     * @return the corresponding Region enum
     * @throws IllegalArgumentException if the region string does not match any known region
     */
    public static Region fromString(String region) {
        for (Region r : Region.values()) {
            if (r.name().equalsIgnoreCase(region)) {
                return r;
            }
        }
        throw new IllegalArgumentException("Unknown region: " + region);
    }
}
