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

/**
 * Exception thrown when an Everfox CDR Cloud API reports an error.
 *
 * <p>Each API's client throws its own subclass. Catch this class to handle errors from any of them the same way.
 *
 * <p>The APIs don't always report the same error with the same code, name, or type. For example, an unsupported media
 * type is {@code 1000 CONTENT_TYPE_UNSUPPORTED} from the Instant API but {@code 6010 APIGATEWAY_BAD_REQUEST_BODY} from
 * the Async API. Branch on {@link #getApiStatusCode()}, {@link #getName()}, or {@link #getType()} separately for each
 * API.
 */
public abstract class CdrApiException extends Exception {

    /** The serialization version. */
    private static final long serialVersionUID = 1L;

    /** The HTTP status code returned by the server. */
    private final int httpStatusCode;
    /** The error returned by the API. */
    private final CdrApiError error;

    /**
     * Creates a new exception.
     *
     * @param httpStatusCode the HTTP status code
     * @param error the error returned by the API, which subclass factories must check is not null
     */
    protected CdrApiException(int httpStatusCode, CdrApiError error) {
        super();
        this.httpStatusCode = httpStatusCode;
        this.error = error;
    }

    /**
     * Returns the API status code. This is {@link CdrApiError#UNKNOWN_CODE} if the API did not return a valid error.
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
     * Returns the API error message. This is "Unknown error" if the API did not return a valid error.
     *
     * @return the API error message
     */
    public String getApiMessage() {
        return error.getMessage();
    }

    /**
     * Returns the name of the error. This is "Unknown" if the API did not return a valid error.
     *
     * @return the error name
     */
    public String getName() {
        return error.getName();
    }

    /**
     * Returns the type of the error. This is "Unknown" if the API did not return a valid error.
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
    public CdrApiError getError() {
        return error;
    }

    @Override
    public String getMessage() {
        return String.format(
                "API Error: %s (code: %d, HTTP status: %d, type: %s)",
                error.getMessage(), error.getCode(), httpStatusCode, error.getType());
    }
}
