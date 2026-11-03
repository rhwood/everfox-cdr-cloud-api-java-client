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
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves the live API endpoint and key for integration tests from environment variables.
 *
 * <p>For each API, {@code CDR_<API>_API_KEY} holds the API key, and the tests are skipped when it is not set. The
 * endpoint is {@link Region#US_WEST_2} unless {@code CDR_<API>_API_URL} is set to a base URL, or
 * {@code CDR_<API>_API_REGION} to a region name (case-insensitive). If both are set, the region takes precedence.
 */
public final class LiveApi {

    /** Environment variable holding the Instant API key. */
    public static final String INSTANT_KEY_ENV = "CDR_INSTANT_API_KEY";
    /** Environment variable holding the Async API key. */
    public static final String ASYNC_KEY_ENV = "CDR_ASYNC_API_KEY";

    /** A media type in single or double quotes. */
    private static final Pattern QUOTED_MEDIA_TYPE = Pattern.compile("['\"]([^'\"\\s]+/[^'\"\\s]+)['\"]");

    /** Not instantiable. */
    private LiveApi() {}

    /**
     * Returns the API key for an API.
     *
     * @param api the API
     * @return the API key
     * @throws IllegalStateException if the key is not set
     */
    public static String apiKey(Api api) {
        String apiKey = env(api, "KEY");
        if (apiKey == null) {
            throw new IllegalStateException(variable(api, "KEY") + " environment variable must be set");
        }
        return apiKey;
    }

    /**
     * Returns the base URL of an API.
     *
     * @param api the API
     * @return the base URL
     */
    public static URI baseUrl(Api api) {
        String region = env(api, "REGION");
        if (region != null) {
            return Region.fromString(region).getBaseUrl(api);
        }
        String url = env(api, "URL");
        return url != null ? URI.create(url) : Region.US_WEST_2.getBaseUrl(api);
    }

    /**
     * Returns the media types listed in an API error message after a marker, such as the allowed types in a rejection.
     * This depends on the wording of the API's error messages, so a change there fails the tests that use it.
     *
     * @param message the error message
     * @param marker the text that comes before the list
     * @return the quoted media types after the marker, as spelled in the message
     * @throws IllegalStateException if the marker is not in the message
     */
    public static Set<String> mediaTypesAfter(String message, String marker) {
        int start = message.indexOf(marker);
        if (start < 0) {
            throw new IllegalStateException("Marker '" + marker + "' not in message: " + message);
        }
        Set<String> mediaTypes = new LinkedHashSet<>();
        Matcher matcher = QUOTED_MEDIA_TYPE.matcher(message.substring(start + marker.length()));
        while (matcher.find()) {
            mediaTypes.add(matcher.group(1));
        }
        return mediaTypes;
    }

    /**
     * Returns the name of an API's environment variable.
     *
     * @param api the API
     * @param suffix the variable suffix, such as {@code KEY}
     * @return the variable name
     */
    private static String variable(Api api, String suffix) {
        return "CDR_" + api.name().toUpperCase(Locale.ROOT) + "_API_" + suffix;
    }

    /**
     * Returns the value of an API's environment variable.
     *
     * @param api the API
     * @param suffix the variable suffix, such as {@code KEY}
     * @return the value, or null if not set or empty
     */
    private static String env(Api api, String suffix) {
        String value = System.getenv(variable(api, suffix));
        return value == null || value.isEmpty() ? null : value;
    }
}
