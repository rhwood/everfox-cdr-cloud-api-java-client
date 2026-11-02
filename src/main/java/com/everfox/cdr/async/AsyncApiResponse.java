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

import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** The processed file downloaded from the Async API. */
public class AsyncApiResponse {

    /** The HTTP status code of the response. */
    private final int statusCode;
    /** The response body (processed file data). */
    private final byte[] body;
    /** The response headers. */
    private final Map<String, String> headers;
    /** The status of the job that produced the file. */
    private final AsyncApiStatus status;

    /**
     * Creates a new response.
     *
     * <p>Note: This constructor is private to force the use of the static factory method
     * {@link #fromHttpResponse(HttpResponse, AsyncApiStatus)} so exceptions can be thrown without creating a new
     * instance.
     *
     * @param statusCode the HTTP status code
     * @param body the response body (processed file data)
     * @param headers the response headers
     * @param status the status of the job that produced the file
     */
    private AsyncApiResponse(int statusCode, byte[] body, Map<String, String> headers, AsyncApiStatus status) {
        this.statusCode = statusCode;
        this.body = body;
        this.headers = headers;
        this.status = status;
    }

    /**
     * Creates a response from an HTTP response.
     *
     * @param httpResponse the HTTP response
     * @param status the status of the job that produced the file
     * @return the API response
     * @throws AsyncApiException if the response indicates an error
     */
    static AsyncApiResponse fromHttpResponse(HttpResponse<byte[]> httpResponse, AsyncApiStatus status)
            throws AsyncApiException {
        int statusCode = httpResponse.statusCode();
        byte[] body = httpResponse.body() != null ? httpResponse.body() : new byte[0];
        if (statusCode >= 400) {
            throw AsyncApiException.create(statusCode, body);
        }
        // Header names are case-insensitive, so look them up that way
        Map<String, String> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        httpResponse.headers().map().forEach((name, values) -> headers.put(name, String.join(", ", values)));
        return new AsyncApiResponse(statusCode, body, headers, status);
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
     * @return the body bytes
     */
    public byte[] getBody() {
        return body.clone();
    }

    /**
     * Returns all response headers. Header names are case-insensitive.
     *
     * @return the unmodifiable headers map
     */
    public Map<String, String> getHeaders() {
        return Collections.unmodifiableMap(headers);
    }

    /**
     * Returns the status of the job that produced the file.
     *
     * @return the status
     */
    public AsyncApiStatus getStatus() {
        return status;
    }

    /**
     * Returns the risks taken during processing.
     *
     * @return the risks taken
     */
    public List<String> getRisksTaken() {
        return status.getRisksTaken();
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
