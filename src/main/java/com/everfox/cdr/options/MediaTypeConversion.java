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

import com.fasterxml.jackson.annotation.JsonInclude;

/** A conversion from one media type to another. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MediaTypeConversion {
    /** The media type to convert from. */
    private final String from;
    /** The media type to convert to. */
    private final String to;

    /**
     * Create a media type conversion.
     *
     * @param from the media type to convert from
     * @param to the media type to convert to
     */
    public MediaTypeConversion(String from, String to) {
        this.from = from;
        this.to = to;
    }

    /**
     * Returns the media type to convert from.
     *
     * @return the media type to convert from
     */
    public String getFrom() {
        return from;
    }

    /**
     * Returns the media type to convert to.
     *
     * @return the media type to convert to
     */
    public String getTo() {
        return to;
    }
}
