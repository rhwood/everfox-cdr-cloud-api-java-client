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

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Objects;

import com.everfox.cdr.MediaType;

import java.util.Arrays;
import java.util.HashSet;

/**
 * Client for interacting with the Everfox CDR Instant API.
 *
 * This client provides methods to upload and process files through the CDR service,
 * removing potential threats and converting files to safe formats.
 */
public class InstantApiClient implements AutoCloseable {

    private final InstantApiConfig config;
    private final HttpClient httpClient;

    /**
     * Creates a new client with the specified configuration.
     *
     * @param config the API configuration
     */
    public InstantApiClient(InstantApiConfig config) {
        this.config = Objects.requireNonNull(config, "config cannot be null");
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(config.getConnectTimeoutSeconds()))
                .build();
    }

    /**
     * Uploads and processes a file through the CDR service.
     *
     * @param request the upload request containing file data and options
     * @return the response containing the processed file or error information
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the operation is interrupted
     * @throws InstantApiException if the API returns an error
     */
    public InstantApiResponse upload(InstantApiRequest request) throws IOException, InterruptedException, InstantApiException {
        Objects.requireNonNull(request, "request cannot be null");

        String endpoint = config.getBaseUrl() + "/upload";
        HashSet<String> acceptTypes = new HashSet<>(Arrays.asList(request.getAcceptType().split(",")));
        acceptTypes.add(MediaType.JSON.getMediaType());

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(Duration.ofSeconds(config.getRequestTimeoutSeconds()))
                .header("x-api-key", config.getApiKey())
                .header("Content-Type", request.getContentType())
                .header("Accept", String.join(", ", acceptTypes))
                .POST(HttpRequest.BodyPublishers.ofByteArray(request.getFileData()));

        if (request.getOptions() != null) {
            requestBuilder.header("X-Options", request.getOptions().toJson());
        }

        HttpRequest httpRequest = requestBuilder.build();
        HttpResponse<byte[]> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofByteArray());

        return InstantApiResponse.fromHttpResponse(httpResponse);
    }

    /**
     * Uploads and processes a file from an input stream.
     *
     * @param inputStream the input stream containing the file data
     * @param contentType the media type of the file
     * @param acceptType the desired media type of the processed file
     * @return the response containing the processed file
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the operation is interrupted
     * @throws InstantApiException if the API returns an error
     */
    public InstantApiResponse upload(InputStream inputStream, String contentType, String acceptType)
            throws IOException, InterruptedException, InstantApiException {
        byte[] fileData = inputStream.readAllBytes();
        InstantApiRequest request = new InstantApiRequest(fileData, contentType, acceptType);
        return upload(request);
    }

    @Override
    public void close() {
        // HttpClient does not require explicit cleanup
    }
}
