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
package com.everfox.cdr.options;

import com.everfox.cdr.MediaType;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** Options for handling images. */
public class ImagesOptions {

    /** The media types to preserve image quality for, in the order given. */
    private final Set<String> preservationFormats;

    /**
     * Create a set of image options with the specified preservation formats.
     *
     * @param preservationFormats the media types to preserve
     */
    public ImagesOptions(MediaType... preservationFormats) {
        this(Arrays.stream(preservationFormats).map(MediaType::getMediaType).toArray(String[]::new));
    }

    /**
     * Create a set of image options with the specified preservation formats.
     *
     * @param preservationFormats the media types to preserve
     */
    public ImagesOptions(String... preservationFormats) {
        this.preservationFormats = new LinkedHashSet<>(Arrays.asList(preservationFormats));
    }

    /**
     * Returns the preservation formats as a map for JSON serialization.
     *
     * @return a map with "preserve" key and the set of preservation formats, or null if no formats are specified
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Map<String, Set<String>> getQuality() {
        return preservationFormats.isEmpty()
                ? null
                : Map.of("preserve", Collections.unmodifiableSet(preservationFormats));
    }
}
