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

import java.util.Objects;

/**
 * Represents a request to upload and process a file through the Instant API.
 */
public class InstantApiRequest {

    private final byte[] fileData;
    private final String contentType;
    private final String acceptType;
    private final RequestOptions options;

    /**
     * Creates a new request with the specified file data and content types.
     *
     * @param fileData the file data (max 4.5 MB)
     * @param contentType the MIME type of the file
     * @param acceptType the desired MIME type of the processed file
     */
    public InstantApiRequest(byte[] fileData, String contentType, String acceptType) {
        this(fileData, contentType, acceptType, null);
    }

    /**
     * Creates a new request with the specified file data, content types, and options.
     *
     * @param fileData the file data (max 4.5 MB)
     * @param contentType the MIME type of the file
     * @param acceptType the desired MIME type of the processed file
     * @param options additional processing options
     */
    public InstantApiRequest(byte[] fileData, String contentType, String acceptType, RequestOptions options) {
        this.fileData = Objects.requireNonNull(fileData, "fileData cannot be null");
        this.contentType = Objects.requireNonNull(contentType, "contentType cannot be null");
        this.acceptType = Objects.requireNonNull(acceptType, "acceptType cannot be null");
        this.options = options;
    }

    /**
     * Returns the file data.
     *
     * @return the file data
     */
    public byte[] getFileData() {
        return fileData;
    }

    /**
     * Returns the MIME type of the file.
     *
     * @return the MIME type
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * Returns the desired MIME type of the processed file.
     *
     * @return the desired MIME type
     */
    public String getAcceptType() {
        return acceptType;
    }

    /**
     * Returns the additional processing options.
     *
     * @return the processing options
     */
    public RequestOptions getOptions() {
        return options;
    }
}
