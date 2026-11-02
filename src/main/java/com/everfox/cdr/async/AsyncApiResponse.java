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

import com.everfox.cdr.CdrApiResponse;
import com.everfox.cdr.internal.HttpSupport;
import java.net.http.HttpResponse;
import java.util.List;

/** The processed file downloaded from the Async API, with the status of the job that produced it. */
public final class AsyncApiResponse extends CdrApiResponse {

    /** The status of the job that produced the file. */
    private final AsyncApiStatus status;

    /**
     * Creates a new response.
     *
     * <p>Note: This constructor is private to force the use of the static factory method
     * {@link #fromHttpResponse(HttpResponse, AsyncApiStatus)} so exceptions can be thrown without creating a new
     * instance.
     *
     * @param httpResponse the HTTP response
     * @param status the status of the job that produced the file
     */
    private AsyncApiResponse(HttpResponse<byte[]> httpResponse, AsyncApiStatus status) {
        super(httpResponse);
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
        HttpSupport.throwIfError(httpResponse, AsyncApiException::create);
        return new AsyncApiResponse(httpResponse, status);
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
}
