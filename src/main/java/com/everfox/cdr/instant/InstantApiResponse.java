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
package com.everfox.cdr.instant;

import com.everfox.cdr.CdrApiResponse;
import com.everfox.cdr.internal.HttpSupport;
import java.net.http.HttpResponse;

/** Response from the Instant API: the processed file, and the risks taken and report from the response headers. */
public final class InstantApiResponse extends CdrApiResponse {

    /**
     * Creates a response from an HTTP response.
     *
     * <p>Note: This constructor is private to force the use of the static factory method
     * {@link #fromHttpResponse(HttpResponse)} so exceptions can be thrown without creating a new instance.
     *
     * @param httpResponse the HTTP response
     */
    private InstantApiResponse(HttpResponse<byte[]> httpResponse) {
        super(httpResponse);
    }

    /**
     * Creates a response from an HTTP response.
     *
     * @param httpResponse the HTTP response
     * @return the API response
     * @throws InstantApiException if the response indicates an error
     */
    static InstantApiResponse fromHttpResponse(HttpResponse<byte[]> httpResponse) throws InstantApiException {
        HttpSupport.throwIfError(httpResponse, InstantApiException::create);
        return new InstantApiResponse(httpResponse);
    }

    /**
     * Returns the X-Risks-Taken header value if present.
     *
     * @return the risks taken, or null if not present
     */
    public String getRisksTaken() {
        return getHeader("x-risks-taken");
    }

    /**
     * Returns the X-Report header value if present.
     *
     * @return the processing report, or null if not present
     */
    public String getReport() {
        return getHeader("x-report");
    }
}
