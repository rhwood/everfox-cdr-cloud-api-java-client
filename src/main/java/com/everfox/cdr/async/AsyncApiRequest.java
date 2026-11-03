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

import com.everfox.cdr.MediaType;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import tools.jackson.databind.ObjectMapper;

/**
 * Represents a request to initiate an Async API processing job.
 *
 * <p>The file itself is not part of the request; it is uploaded afterwards to the link returned by
 * {@link AsyncApiClient#initiate(AsyncApiRequest)}.
 */
public class AsyncApiRequest {

    /** ObjectMapper for JSON serialization. */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** The media type of the file to be uploaded. */
    private final String contentType;
    /** Additional processing options for the request. */
    private final RequestOptions options;

    /**
     * Creates a new request with the specified content type and options.
     *
     * @param contentType the media type of the file
     * @param options additional processing options
     */
    // private to force use of the static factory methods
    private AsyncApiRequest(String contentType, RequestOptions options) {
        this.contentType = contentType;
        this.options = options;
    }

    /**
     * Returns the media type of the file.
     *
     * @return the media type
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * Returns the additional processing options.
     *
     * @return the processing options, or null if not set
     */
    public RequestOptions getOptions() {
        return options;
    }

    /**
     * Converts this request to the JSON body of the initiate request.
     *
     * @return JSON representation
     */
    public String toJson() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("mime-type", contentType);
        if (options != null) {
            body.put("options", options);
        }
        // Can't fail: the body holds only a string and RequestOptions, which is final and has plain getters
        return MAPPER.writeValueAsString(body);
    }

    /**
     * Creates a new request with the specified content type.
     *
     * @param contentType the media type of the file
     * @return a new AsyncApiRequest instance
     */
    public static AsyncApiRequest create(MediaType contentType) {
        return AsyncApiRequest.create(contentType, null);
    }

    /**
     * Creates a new request with the specified content type.
     *
     * @param contentType the media type of the file
     * @return a new AsyncApiRequest instance
     */
    public static AsyncApiRequest create(String contentType) {
        return AsyncApiRequest.create(contentType, null);
    }

    /**
     * Creates a new request with the specified content type and options.
     *
     * @param contentType the media type of the file
     * @param options additional processing options
     * @return a new AsyncApiRequest instance
     */
    public static AsyncApiRequest create(MediaType contentType, RequestOptions options) {
        Objects.requireNonNull(contentType, "contentType cannot be null");
        return AsyncApiRequest.create(contentType.getMediaType(), options);
    }

    /**
     * Creates a new request with the specified content type and options.
     *
     * @param contentType the media type of the file
     * @param options additional processing options
     * @return a new AsyncApiRequest instance
     */
    public static AsyncApiRequest create(String contentType, RequestOptions options) {
        Objects.requireNonNull(contentType, "contentType cannot be null");
        return new AsyncApiRequest(contentType, options);
    }
}
