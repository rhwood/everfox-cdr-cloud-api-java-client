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

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * The published OpenAPI spec of an API, checked in under {@code src/test/resources/com/everfox/cdr/openapi/}.
 *
 * <p>To refresh a spec, download it again from
 * {@code https://cdr.everfox.com/static/everfox-cdr-{instant,async}-api-OpenAPI.json} and run the tests.
 */
public enum ApiSpec {
    /** The Instant API spec. */
    INSTANT(
            "everfox-cdr-instant-api-OpenAPI.json",
            "/components/schemas/OptionsHeader/properties",
            "/paths/~1upload/post/parameters"),
    /** The Async API spec. */
    ASYNC(
            "everfox-cdr-async-api-OpenAPI.json",
            "/components/schemas/CreateUploadUrlsRequest/properties/options/properties",
            "/components/schemas/CreateUploadUrlsRequest/properties/mime-type/enum");

    /** The parsed spec. */
    private final JsonNode spec;
    /** JSON pointer to the properties of the processing options. */
    private final String optionsPointer;
    /** JSON pointer to the file's media type: the Instant upload parameters, or the Async {@code mime-type} enum. */
    private final String contentTypesPointer;

    /**
     * Loads a spec.
     *
     * @param fileName the spec file name
     * @param optionsPointer JSON pointer to the properties of the processing options
     * @param contentTypesPointer JSON pointer to the file's media type
     */
    ApiSpec(String fileName, String optionsPointer, String contentTypesPointer) {
        this.spec = new ObjectMapper().readTree(TestResources.load("openapi/" + fileName));
        this.optionsPointer = optionsPointer;
        this.contentTypesPointer = contentTypesPointer;
    }

    /**
     * Returns the media types the spec allows for a file: the Instant {@code Content-Type} header, or the Async
     * {@code mime-type}.
     *
     * @return the media types, as spelled in the spec
     */
    public Set<String> contentTypes() {
        JsonNode node = spec.at(contentTypesPointer);
        if (this == INSTANT) {
            node = node.values().stream()
                    .filter(p -> "content-type".equalsIgnoreCase(p.path("name").asString()))
                    .findFirst()
                    .orElseThrow()
                    .path("schema")
                    .path("enum");
        }
        return strings(node);
    }

    /**
     * Returns the media types the spec allows as {@code conversion.from}.
     *
     * @return the media types, as spelled in the spec
     */
    public Set<String> conversionSources() {
        return strings(spec.at(optionsPointer + "/conversion/properties/from/properties/mime-type/enum"));
    }

    /**
     * Returns the risks the spec allows in a risks list.
     *
     * @param list {@code allow} or {@code deny}
     * @return the risks
     */
    public Set<String> risks(String list) {
        return strings(spec.at(optionsPointer + "/risks/properties/" + list + "/items/enum"));
    }

    /**
     * Converts media types to lower case, because media types are case-insensitive.
     *
     * @param mediaTypes the media types
     * @return the lower-case media types
     */
    public static Set<String> lowerCase(Set<String> mediaTypes) {
        return mediaTypes.stream().map(t -> t.toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
    }

    /**
     * Returns the strings in a JSON array.
     *
     * @param array the array
     * @return the strings, in order
     * @throws IllegalStateException if the node is not a non-empty array, which means the spec's layout changed
     */
    private static Set<String> strings(JsonNode array) {
        if (!array.isArray() || array.isEmpty()) {
            throw new IllegalStateException("Expected a non-empty enum in the spec; has its layout changed?");
        }
        Set<String> values = new LinkedHashSet<>();
        array.values().forEach(v -> values.add(v.asString()));
        return values;
    }
}
