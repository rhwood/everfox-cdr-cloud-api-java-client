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

import tools.jackson.databind.ObjectMapper;

import java.net.http.HttpResponse;
import java.util.Map;

/**
 * Response from the Instant API.
 */
public class InstantApiResponse {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final int statusCode;
    private final byte[] body;
    private final Map<String, String> headers;
    private final String risksTaken;
    private final String report;

    private InstantApiResponse(int statusCode, byte[] body, Map<String, String> headers,
                               String risksTaken, String report) {
        this.statusCode = statusCode;
        this.body = body;
        this.headers = headers;
        this.risksTaken = risksTaken;
        this.report = report;
    }

    /**
     * Creates a response from an HTTP response.
     *
     * @param httpResponse the HTTP response
     * @return the API response
     * @throws InstantApiException if the response indicates an error
     */
    static InstantApiResponse fromHttpResponse(HttpResponse<byte[]> httpResponse) throws InstantApiException {
        int statusCode = httpResponse.statusCode();
        byte[] body = httpResponse.body();
        Map<String, String> headers = httpResponse.headers().map().entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        e -> String.join(", ", e.getValue())
                ));

        String risksTaken = headers.get("X-Risks-Taken");
        String report = headers.get("X-Report");

        if (statusCode >= 400) {
            String errorMessage = parseErrorMessage(body, statusCode);
            throw new InstantApiException(statusCode, errorMessage);
        }

        return new InstantApiResponse(statusCode, body, headers, risksTaken, report);
    }

    private static String parseErrorMessage(byte[] body, int statusCode) {
        try {
            if (body != null && body.length > 0) {
                @SuppressWarnings("unchecked")
                Map<String, Object> errorObj = MAPPER.readValue(body, Map.class);
                if (errorObj.containsKey("error")) {
                    return errorObj.get("error").toString();
                }
            }
        } catch (Exception e) {
            // Fall through to default message
        }
        return "HTTP " + statusCode + " error";
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
        return body;
    }

    /**
     * Returns all response headers.
     *
     * @return the headers map
     */
    public Map<String, String> getHeaders() {
        return headers;
    }

    /**
     * Returns the X-Risks-Taken header value if present.
     *
     * @return the risks taken, or null if not present
     */
    public String getRisksTaken() {
        return risksTaken;
    }

    /**
     * Returns the X-Report header value if present.
     *
     * @return the processing report, or null if not present
     */
    public String getReport() {
        return report;
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
