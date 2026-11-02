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

import java.util.Objects;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Exception thrown when the Async API returns an error response, or when processing completes unsuccessfully.
 *
 * <p>When processing fails, the HTTP status code is that of the status response that reported the failure, which is
 * always 200.
 */
public class AsyncApiException extends Exception {

    /** The serialization version. */
    private static final long serialVersionUID = 1L;

    /** The Jackson object mapper used to parse the error response. */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** The HTTP status code returned by the server. */
    private final int httpStatusCode;
    /** The error returned by the API. */
    private final AsyncApiError error;

    /**
     * Creates a new exception.
     *
     * @param httpStatusCode the HTTP status code
     * @param error the error returned by the API
     */
    // private to force use of the static factory methods
    private AsyncApiException(int httpStatusCode, AsyncApiError error) {
        super();
        this.httpStatusCode = httpStatusCode;
        this.error = error;
    }

    /**
     * Returns the API status code. This is {@link AsyncApiError#UNKNOWN_CODE} if the API did not return a valid error.
     *
     * @return the status code
     */
    public int getApiStatusCode() {
        return error.getCode();
    }

    /**
     * Returns the HTTP status code.
     *
     * @return the HTTP status code
     */
    public int getHttpStatusCode() {
        return httpStatusCode;
    }

    /**
     * Returns the API error message.
     *
     * @return the API error message
     */
    public String getApiMessage() {
        return error.getMessage();
    }

    /**
     * Returns the name of the error.
     *
     * @return the error name
     */
    public String getName() {
        return error.getName();
    }

    /**
     * Returns the type of the error.
     *
     * @return the error type
     */
    public String getType() {
        return error.getType();
    }

    /**
     * Returns the sub-type of the error.
     *
     * @return the error sub-type, or null if not present
     */
    public String getSubType() {
        return error.getSubType();
    }

    /**
     * Returns the error returned by the API.
     *
     * @return the error
     */
    public AsyncApiError getError() {
        return error;
    }

    @Override
    public String getMessage() {
        return String.format(
                "API Error: %s (code: %d, HTTP status: %d, type: %s)",
                error.getMessage(), error.getCode(), httpStatusCode, error.getType());
    }

    /**
     * Creates an AsyncApiException from the HTTP status code and response body.
     *
     * @param httpStatusCode the HTTP status code
     * @param body the response body
     * @return an AsyncApiException representing the error
     */
    public static AsyncApiException create(int httpStatusCode, byte[] body) {
        AsyncApiError error = AsyncApiError.unknown();
        if (body != null && body.length > 0) {
            try {
                JsonNode root = MAPPER.readTree(body);
                error = AsyncApiError.fromJson(root.get("error"));
            } catch (Exception e) {
                // Not JSON (for example, an XML error from the storage service), so the error stays unknown
            }
        }
        return new AsyncApiException(httpStatusCode, error);
    }

    /**
     * Creates an AsyncApiException from the HTTP status code and an error.
     *
     * @param httpStatusCode the HTTP status code
     * @param error the error
     * @return an AsyncApiException representing the error
     */
    public static AsyncApiException create(int httpStatusCode, AsyncApiError error) {
        Objects.requireNonNull(error, "error cannot be null");
        return new AsyncApiException(httpStatusCode, error);
    }
}
