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

import com.everfox.cdr.CdrApiError;
import com.everfox.cdr.CdrApiException;
import java.util.Objects;

/**
 * Exception thrown when the Async API returns an error response, or when processing completes unsuccessfully.
 *
 * <p>When processing fails, the HTTP status code is that of the status response that reported the failure, which is
 * always 200.
 */
public class AsyncApiException extends CdrApiException {

    /** The serialization version. */
    private static final long serialVersionUID = 1L;

    /**
     * Creates a new exception.
     *
     * @param httpStatusCode the HTTP status code
     * @param error the error returned by the API
     */
    // private to force use of the static factory methods
    private AsyncApiException(int httpStatusCode, CdrApiError error) {
        super(httpStatusCode, error);
    }

    /**
     * Creates an AsyncApiException from the HTTP status code and response body.
     *
     * @param httpStatusCode the HTTP status code
     * @param body the response body
     * @return an AsyncApiException representing the error
     */
    public static AsyncApiException create(int httpStatusCode, byte[] body) {
        return new AsyncApiException(httpStatusCode, CdrApiError.fromJson(body));
    }

    /**
     * Creates an AsyncApiException from the HTTP status code and an error.
     *
     * @param httpStatusCode the HTTP status code
     * @param error the error
     * @return an AsyncApiException representing the error
     */
    public static AsyncApiException create(int httpStatusCode, CdrApiError error) {
        Objects.requireNonNull(error, "error cannot be null");
        return new AsyncApiException(httpStatusCode, error);
    }
}
