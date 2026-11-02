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
package com.everfox.cdr.async;

import java.net.URI;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import tools.jackson.databind.JsonNode;

/**
 * An action to take next, as returned in the {@code links} property of a status.
 *
 * <p>Use the URL, method, and headers as given; they may change over time. Links expire (after a few minutes) and
 * contain the credentials needed to access the resource, so do not log or expose them.
 */
public class AsyncApiLink {

    /** The location for the request. */
    private final URI url;
    /** The HTTP method for the request. */
    private final String method;
    /** The HTTP headers to send with the request. */
    private final Map<String, String> headers;
    /** When the link expires. */
    private final Instant expiry;
    /** The size in bytes of the processed file. */
    private final Long fileSize;

    /**
     * Creates a new link.
     *
     * @param url the location for the request
     * @param method the HTTP method for the request
     * @param headers the HTTP headers to send with the request
     * @param expiry when the link expires
     * @param fileSize the size in bytes of the processed file
     */
    // private to force use of the static factory method
    private AsyncApiLink(URI url, String method, Map<String, String> headers, Instant expiry, Long fileSize) {
        this.url = url;
        this.method = method;
        this.headers = headers;
        this.expiry = expiry;
        this.fileSize = fileSize;
    }

    /**
     * Returns the location for the request.
     *
     * @return the URL
     */
    public URI getUrl() {
        return url;
    }

    /**
     * Returns the HTTP method for the request.
     *
     * @return the HTTP method
     */
    public String getMethod() {
        return method;
    }

    /**
     * Returns the HTTP headers to send with the request.
     *
     * @return the headers
     */
    public Map<String, String> getHeaders() {
        return Map.copyOf(headers);
    }

    /**
     * Returns when the link expires.
     *
     * @return the expiry, or null if not present
     */
    public Instant getExpiry() {
        return expiry;
    }

    /**
     * Returns the size in bytes of the processed file. Only result links have a file size.
     *
     * @return the file size, or null if not present
     */
    public Long getFileSize() {
        return fileSize;
    }

    /**
     * Creates a link from its JSON representation.
     *
     * @param link the JSON link object
     * @return the link, or null if the object is missing or has no URL
     */
    static AsyncApiLink fromJson(JsonNode link) {
        if (link == null || !link.path("url").isString()) {
            return null;
        }
        URI url = URI.create(link.path("url").stringValue());
        String method = link.path("method").isString() ? link.path("method").stringValue() : "GET";
        Map<String, String> headers = new LinkedHashMap<>();
        link.path("headers").properties().forEach(e -> {
            if (e.getValue().isString()) {
                headers.put(e.getKey(), e.getValue().stringValue());
            }
        });
        Instant expiry = null;
        if (link.path("expiry").isString()) {
            try {
                expiry = Instant.parse(link.path("expiry").stringValue());
            } catch (DateTimeParseException e) {
                // Leave the expiry unknown rather than reject an otherwise usable link
            }
        }
        Long fileSize =
                link.path("fileSize").isIntegralNumber() ? link.path("fileSize").asLong() : null;
        return new AsyncApiLink(url, method, headers, expiry, fileSize);
    }

    /**
     * Creates a link.
     *
     * @param url the location for the request
     * @param method the HTTP method for the request
     * @param headers the HTTP headers to send with the request
     * @return the link
     */
    public static AsyncApiLink create(URI url, String method, Map<String, String> headers) {
        Objects.requireNonNull(url, "url cannot be null");
        Objects.requireNonNull(method, "method cannot be null");
        Objects.requireNonNull(headers, "headers cannot be null");
        return new AsyncApiLink(url, method, new LinkedHashMap<>(headers), null, null);
    }
}
