/**
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

/**
 * Available API regions.
 */
public enum Region {
    /**
     * EU West (Ireland) region.
     */
    EU_WEST_1("https://eu-west-1.aws.instant.cdr.everfox.com/v1"),
    /**
     * EU West (London) region.
     */
    EU_WEST_2("https://eu-west-2.aws.instant.cdr.everfox.com/v1"),
    /**
     * US West (Oregon) region.
     */
    US_WEST_2("https://us-west-2.aws.instant.cdr.everfox.com/v1");

    private final URI baseUrl;

    Region(String baseUrl) {
        this.baseUrl = URI.create(baseUrl);
    }

    /**
     * Returns the base URL for the region.
     *
     * @return the base URL
     */
    public URI getBaseUrl() {
        return baseUrl;
    }
}