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

import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/** A processed file returned by an Everfox CDR Cloud API, with its HTTP status code and headers. */
public abstract class CdrApiResponse {

    /** The HTTP status code of the response. */
    private final int statusCode;
    /** The response body (processed file data). */
    private final byte[] body;
    /** The response headers, with case-insensitive names. */
    private final Map<String, String> headers;

    /**
     * Creates a response from an HTTP response.
     *
     * @param httpResponse the HTTP response
     */
    protected CdrApiResponse(HttpResponse<byte[]> httpResponse) {
        this.statusCode = httpResponse.statusCode();
        this.body = httpResponse.body() != null ? httpResponse.body() : new byte[0];
        // Header names are case-insensitive, so look them up that way
        Map<String, String> map = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        httpResponse.headers().map().forEach((name, values) -> map.put(name, String.join(", ", values)));
        this.headers = map;
    }

    /**
     * Returns the HTTP status code.
     *
     * @return the status code
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * Returns the response body (processed file data).
     *
     * @return a copy of the body bytes
     */
    public byte[] getBody() {
        return body.clone();
    }

    /**
     * Returns all response headers. Header names are case-insensitive, and a header with several values has them joined
     * with a comma and a space.
     *
     * @return the unmodifiable headers map
     */
    public Map<String, String> getHeaders() {
        return Collections.unmodifiableMap(headers);
    }

    /**
     * Returns a response header.
     *
     * @param name the header name (case-insensitive)
     * @return the header value, or null if not present
     */
    public String getHeader(String name) {
        return headers.get(name);
    }

    /**
     * Checks if the response is successful (status 200-299).
     *
     * @return true if successful
     */
    public boolean isSuccess() {
        return statusCode >= 200 && statusCode < 300;
    }
}
