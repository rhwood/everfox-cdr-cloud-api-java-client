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
package com.everfox.cdr.internal;

import com.everfox.cdr.CdrApiConfig;
import com.everfox.cdr.CdrApiException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/** HTTP helpers shared by the API clients. Not part of the library's API. */
public final class HttpSupport {

    /**
     * Creates an exception for an HTTP error response.
     *
     * @param <E> the exception type
     */
    @FunctionalInterface
    public interface ErrorFactory<E extends CdrApiException> {
        /**
         * Creates the exception.
         *
         * @param httpStatusCode the HTTP status code
         * @param body the response body
         * @return the exception
         */
        E create(int httpStatusCode, byte[] body);
    }

    /** Not instantiable. */
    private HttpSupport() {}

    /**
     * Creates an HTTP client with the configuration's connection timeout. Redirects are never followed, so a client
     * that needs them follows them explicitly.
     *
     * @param config the configuration
     * @return the HTTP client
     */
    public static HttpClient newClient(CdrApiConfig config) {
        return HttpClient.newBuilder()
                .connectTimeout(config.getConnectTimeout())
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    /**
     * Creates a request builder with the configuration's request timeout.
     *
     * @param uri the request URI
     * @param config the configuration
     * @return the request builder
     */
    public static HttpRequest.Builder newRequest(URI uri, CdrApiConfig config) {
        return HttpRequest.newBuilder(uri).timeout(config.getRequestTimeout());
    }

    /**
     * Throws an exception if a response has an HTTP error status (400 or above).
     *
     * @param <E> the exception type
     * @param response the HTTP response
     * @param factory creates the exception from the status code and body
     * @throws E if the response has an error status
     */
    public static <E extends CdrApiException> void throwIfError(HttpResponse<byte[]> response, ErrorFactory<E> factory)
            throws E {
        if (response.statusCode() >= 400) {
            throw factory.create(response.statusCode(), response.body());
        }
    }
}
