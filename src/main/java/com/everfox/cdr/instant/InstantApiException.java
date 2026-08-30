/**
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
package com.everfox.cdr.instant;

import java.util.Map;

import tools.jackson.databind.ObjectMapper;

/**
 * Exception thrown when the Instant API returns an error response.
 */
public class InstantApiException extends Exception {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * The API status code returned by the API. This is -1 is the API did not return a valid error response. 
     */
    private final int apiStatusCode;
    /**
     * The HTTP status code returned by the server.
     */
    private final int httpStatusCode;
    /**
     * The name of the error returned by the API. This is "Unknown" if the API did not return a valid error response.
     */
    private final String name;
    /**
     * The type of the error returned by the API. This is "Unknown" if the API did not return a valid error response.
     */
    private final String type;

    /**
     * Creates a new exception from the JSON error response returned by the API.
     * 
     * @param body the JSON error response body
     */
    // This is private because the constructor must call super() first, so we can't parse the body in the constructor
    private InstantApiException(int apiStatusCode, int httpStatusCode, String message, String name, String type) {
        super(message);
        this.apiStatusCode = apiStatusCode;
        this.httpStatusCode = httpStatusCode;
        this.name = name;
        this.type = type;
    }

    /**
     * Returns the API status code.
     *
     * @return the status code
     */
    public int getApiStatusCode() {
        return apiStatusCode;
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
     * Returns the name of the error.
     *
     * @return the error name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the type of the error.
     *
     * @return the error type
     */
    public String getType() {
        return type;
    }

    /**
     * Creates an InstantApiException from the HTTP status code and response body.
     *
     * @param httpStatusCode the HTTP status code
     * @param body the response body
     * @return an InstantApiException representing the error
     */
    // This is needed because the constructor must call super() first, so we can't parse the body in the constructor
    public static InstantApiException create(int httpStatusCode, byte[] body) {
        try {
            if (body != null && body.length > 0) {
                @SuppressWarnings("unchecked")
                Map<String, Object> bodyObj = MAPPER.readValue(body, Map.class);
                if (bodyObj.containsKey("error")) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> errorObj = (Map<String, Object>) bodyObj.get("error");
                    return new InstantApiException(
                            (int) errorObj.get("code"),
                            httpStatusCode,
                            (String) errorObj.get("message"),
                            (String) errorObj.get("name"),
                            (String) errorObj.get("type")
                    );
                }
            }
        } catch (Exception e) {
            // Fall through to default execption creation
        }
        return new InstantApiException(-1, httpStatusCode, "Unknown error", "Unknown", "Unknown");
    }
}
