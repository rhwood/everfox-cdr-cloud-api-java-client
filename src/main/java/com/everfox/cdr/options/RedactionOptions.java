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

/**
 * Redaction options.
 *
 * <p>Redaction options specify how to redact files in archives when those would otherwise prevent the archive from
 * being processed by the CDR. For example, if a ZIP file contains an executable, the executable can be redacted to
 * allow the ZIP file to be processed.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RedactionOptions {
    /** The file extension to replace redacted files with. */
    private final String extension;
    /** The media type to use for the redacted file replacement. */
    private final String mime;
    /** The text to use for the redacted file replacement. */
    private final String text;

    /**
     * Create a set of redaction options.
     *
     * @param extension the file extension to replace redacted files with, including the leading dot (e.g.,
     *     {@code ".txt"})
     * @param mime the media type to use for the redacted file replacement
     * @param text the text to use for the redacted file replacement
     */
    public RedactionOptions(String extension, MediaType mime, String text) {
        this(extension, mime.getMediaType(), text);
    }

    /**
     * Create a set of redaction options.
     *
     * @param extension the file extension to replace redacted files with, including the leading dot (e.g.,
     *     {@code ".txt"})
     * @param mime the media type to use for the redacted file replacement
     * @param text the text to use for the redacted file replacement
     */
    public RedactionOptions(String extension, String mime, String text) {
        this.extension = extension;
        this.mime = mime;
        this.text = text;
    }

    /**
     * Returns the file extension used when redacting a file.
     *
     * @return the file extension
     */
    public String getExtension() {
        return extension;
    }

    /**
     * Returns the media type for the redaction.
     *
     * @return the media type
     */
    public String getMime() {
        return mime;
    }

    /**
     * Returns the text used in the redaction.
     *
     * @return the text
     */
    public String getText() {
        return text;
    }
}
