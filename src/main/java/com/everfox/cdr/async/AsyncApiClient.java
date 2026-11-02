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
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeoutException;

/**
 * Client for interacting with the Everfox CDR Async API.
 *
 * <p>Use {@link #process(AsyncApiRequest, byte[])} or {@link #process(AsyncApiRequest, Path, Path)} to process a file
 * in one call, or call {@link #initiate(AsyncApiRequest)}, {@code upload}, {@link #waitForCompletion(AsyncApiStatus)},
 * and {@code download} to control each step. Each step takes the status returned by the previous one, because the
 * status holds the links for the next step.
 */
public class AsyncApiClient implements AutoCloseable {

    /** Status codes to retry while polling: not modified, not found yet, and throttled. */
    private static final Set<Integer> POLL_RETRY_STATUS_CODES = Set.of(304, 404, 429);
    /** Headers that {@link HttpClient} does not allow callers to set. */
    private static final Set<String> RESTRICTED_HEADERS =
            Set.of("connection", "content-length", "expect", "host", "upgrade");
    /** The maximum number of redirects to follow when downloading. */
    private static final int MAX_REDIRECTS = 5;

    /** The API configuration. */
    private final AsyncApiConfig config;
    /** The HTTP client used for making requests. */
    private final HttpClient httpClient;

    /**
     * Creates a new client with the specified configuration.
     *
     * @param config the API configuration
     */
    // private to force use of the static factory method
    private AsyncApiClient(AsyncApiConfig config) {
        this.config = config;
        // Redirects are followed by download() so the link's credentials are not sent to the redirect target
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(config.getConnectTimeoutSeconds()))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    /**
     * Initiates a processing job. The returned status contains the link to upload the file to.
     *
     * @param request the request containing the file's media type and processing options
     * @return the status of the new job
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the operation is interrupted
     * @throws AsyncApiException if the API returns an error
     */
    public AsyncApiStatus initiate(AsyncApiRequest request)
            throws IOException, InterruptedException, AsyncApiException {
        Objects.requireNonNull(request, "request cannot be null");

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(config.getBaseUrl() + "/initiate"))
                .timeout(requestTimeout())
                .header("x-api-key", config.getApiKey())
                .header("Content-Type", MediaType.JSON.getMediaType())
                .header("Accept", MediaType.JSON.getMediaType())
                .POST(HttpRequest.BodyPublishers.ofString(request.toJson()))
                .build();
        return AsyncApiStatus.fromHttpResponse(httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofByteArray()));
    }

    /**
     * Uploads the file to the upload link of an initiated job.
     *
     * @param status the status returned by {@link #initiate(AsyncApiRequest)}
     * @param fileData the file data
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the operation is interrupted
     * @throws AsyncApiException if the upload is rejected
     * @throws IllegalStateException if the status has no upload link
     */
    public void upload(AsyncApiStatus status, byte[] fileData)
            throws IOException, InterruptedException, AsyncApiException {
        Objects.requireNonNull(fileData, "fileData cannot be null");
        upload(status, HttpRequest.BodyPublishers.ofByteArray(fileData));
    }

    /**
     * Uploads a file to the upload link of an initiated job. This streams the file, so use it for large files.
     *
     * @param status the status returned by {@link #initiate(AsyncApiRequest)}
     * @param file the file to upload
     * @throws IOException if an I/O error occurs or the file cannot be read
     * @throws InterruptedException if the operation is interrupted
     * @throws AsyncApiException if the upload is rejected
     * @throws IllegalStateException if the status has no upload link
     */
    public void upload(AsyncApiStatus status, Path file) throws IOException, InterruptedException, AsyncApiException {
        Objects.requireNonNull(file, "file cannot be null");
        upload(status, HttpRequest.BodyPublishers.ofFile(file));
    }

    /**
     * Uploads a file from an input stream to the upload link of an initiated job. The stream is read into memory
     * because the upload requires the content length.
     *
     * @param status the status returned by {@link #initiate(AsyncApiRequest)}
     * @param inputStream the input stream containing the file data
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the operation is interrupted
     * @throws AsyncApiException if the upload is rejected
     * @throws IllegalStateException if the status has no upload link
     */
    public void upload(AsyncApiStatus status, InputStream inputStream)
            throws IOException, InterruptedException, AsyncApiException {
        Objects.requireNonNull(inputStream, "inputStream cannot be null");
        upload(status, inputStream.readAllBytes());
    }

    /**
     * Uploads content to the upload link of an initiated job.
     *
     * @param status the status returned by {@link #initiate(AsyncApiRequest)}
     * @param body the content to upload
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the operation is interrupted
     * @throws AsyncApiException if the upload is rejected
     */
    private void upload(AsyncApiStatus status, HttpRequest.BodyPublisher body)
            throws IOException, InterruptedException, AsyncApiException {
        Objects.requireNonNull(status, "status cannot be null");
        AsyncApiLink link = requireLink(status.getUploadLink(), "upload");
        HttpResponse<byte[]> response =
                httpClient.send(linkRequest(link, body).build(), HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() >= 400) {
            throw AsyncApiException.create(response.statusCode(), response.body());
        }
    }

    /**
     * Requests the current status of a job once.
     *
     * @param status a previous status of the job
     * @return the current status
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the operation is interrupted
     * @throws AsyncApiException if the API returns an error
     * @throws IllegalStateException if the status has no status link, which is the case once processing is complete
     */
    public AsyncApiStatus getStatus(AsyncApiStatus status) throws IOException, InterruptedException, AsyncApiException {
        Objects.requireNonNull(status, "status cannot be null");
        return AsyncApiStatus.fromHttpResponse(sendStatusRequest(status, false));
    }

    /**
     * Polls the status of a job until processing is complete, waiting {@link AsyncApiConfig#getPollIntervalSeconds()}
     * between requests for up to {@link AsyncApiConfig#getProcessingTimeoutSeconds()}.
     *
     * <p>The returned status may report a failure; check {@link AsyncApiStatus#isSuccess()}, or call {@code download},
     * which throws an {@link AsyncApiException} if processing failed.
     *
     * @param status a previous status of the job, usually the one returned by {@link #initiate(AsyncApiRequest)}
     * @return the completed status
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the operation is interrupted
     * @throws AsyncApiException if the API returns an error
     * @throws TimeoutException if processing does not complete within the processing timeout
     * @throws IllegalStateException if an incomplete status has no status link
     */
    public AsyncApiStatus waitForCompletion(AsyncApiStatus status)
            throws IOException, InterruptedException, AsyncApiException, TimeoutException {
        Objects.requireNonNull(status, "status cannot be null");
        Duration pollInterval = Duration.ofSeconds(config.getPollIntervalSeconds());
        long deadline = System.nanoTime()
                + Duration.ofSeconds(config.getProcessingTimeoutSeconds()).toNanos();
        AsyncApiStatus current = status;
        while (!current.isComplete()) {
            if (System.nanoTime() - deadline >= 0) {
                throw new TimeoutException("Processing did not complete within " + config.getProcessingTimeoutSeconds()
                        + " seconds (state: " + current.getState() + ")");
            }
            Thread.sleep(pollInterval);
            HttpResponse<byte[]> response = sendStatusRequest(current, true);
            if (!POLL_RETRY_STATUS_CODES.contains(response.statusCode())) {
                current = AsyncApiStatus.fromHttpResponse(response);
            }
        }
        return current;
    }

    /**
     * Sends a request to the status link of a job.
     *
     * @param status a previous status of the job
     * @param conditional whether to send the status's ETag as {@code If-None-Match}
     * @return the HTTP response
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the operation is interrupted
     */
    private HttpResponse<byte[]> sendStatusRequest(AsyncApiStatus status, boolean conditional)
            throws IOException, InterruptedException {
        AsyncApiLink link = requireLink(status.getStatusLink(), "status");
        HttpRequest.Builder builder = linkRequest(link, HttpRequest.BodyPublishers.noBody())
                .setHeader("Accept", MediaType.JSON.getMediaType());
        if (conditional && status.getEtag() != null) {
            builder.setHeader("If-None-Match", status.getEtag());
        }
        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());
    }

    /**
     * Downloads the processed file of a completed job. The result can only be downloaded once.
     *
     * @param status the completed status returned by {@link #waitForCompletion(AsyncApiStatus)}
     * @return the response containing the processed file
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the operation is interrupted
     * @throws AsyncApiException if processing failed, or the API returns an error (HTTP 410 if already downloaded)
     * @throws IllegalStateException if processing is not complete
     */
    public AsyncApiResponse download(AsyncApiStatus status)
            throws IOException, InterruptedException, AsyncApiException {
        AsyncApiLink link = requireResultLink(status);
        return AsyncApiResponse.fromHttpResponse(
                sendFollowingRedirects(link, HttpResponse.BodyHandlers.ofByteArray()), status);
    }

    /**
     * Downloads the processed file of a completed job to a file, replacing it if it exists. This streams the file, so
     * use it for large files. The result can only be downloaded once.
     *
     * @param status the completed status returned by {@link #waitForCompletion(AsyncApiStatus)}
     * @param target the file to write the processed file to
     * @return the number of bytes written
     * @throws IOException if an I/O error occurs or the file cannot be written
     * @throws InterruptedException if the operation is interrupted
     * @throws AsyncApiException if processing failed, or the API returns an error (HTTP 410 if already downloaded)
     * @throws IllegalStateException if processing is not complete
     */
    public long download(AsyncApiStatus status, Path target)
            throws IOException, InterruptedException, AsyncApiException {
        Objects.requireNonNull(target, "target cannot be null");
        AsyncApiLink link = requireResultLink(status);
        HttpResponse<InputStream> response = sendFollowingRedirects(link, HttpResponse.BodyHandlers.ofInputStream());
        try (InputStream body = response.body()) {
            if (response.statusCode() >= 300) {
                throw AsyncApiException.create(response.statusCode(), body.readAllBytes());
            }
            return Files.copy(body, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * Returns the result link of a completed status.
     *
     * @param status the completed status
     * @return the result link
     * @throws AsyncApiException if processing failed
     * @throws IllegalStateException if processing is not complete or the status has no result link
     */
    private static AsyncApiLink requireResultLink(AsyncApiStatus status) throws AsyncApiException {
        Objects.requireNonNull(status, "status cannot be null");
        if (status.isComplete() && !status.isSuccess()) {
            throw AsyncApiException.create(
                    200, status.getError() != null ? status.getError() : AsyncApiError.unknown());
        }
        if (!status.isComplete()) {
            throw new IllegalStateException("Processing is not complete (state: " + status.getState() + ")");
        }
        return requireLink(status.getResultLink(), "result");
    }

    /**
     * Sends a GET request to a link, following redirects. The link's headers are not sent to redirect targets, which
     * are pre-signed URLs that reject additional credentials.
     *
     * @param <T> the response body type
     * @param link the link
     * @param handler the response body handler
     * @return the HTTP response
     * @throws IOException if an I/O error occurs or there are too many redirects
     * @throws InterruptedException if the operation is interrupted
     */
    private <T> HttpResponse<T> sendFollowingRedirects(AsyncApiLink link, HttpResponse.BodyHandler<T> handler)
            throws IOException, InterruptedException {
        HttpResponse.BodyHandler<T> redirectAware = info -> isRedirect(info.statusCode(), info.headers())
                ? HttpResponse.BodySubscribers.replacing(null)
                : handler.apply(info);
        HttpRequest request =
                linkRequest(link, HttpRequest.BodyPublishers.noBody()).build();
        for (int redirects = 0; ; redirects++) {
            HttpResponse<T> response = httpClient.send(request, redirectAware);
            if (!isRedirect(response.statusCode(), response.headers())) {
                return response;
            }
            if (redirects >= MAX_REDIRECTS) {
                throw new IOException("Too many redirects downloading the result");
            }
            URI location = request.uri()
                    .resolve(response.headers().firstValue("location").orElseThrow());
            request = HttpRequest.newBuilder(location)
                    .timeout(requestTimeout())
                    .GET()
                    .build();
        }
    }

    /**
     * Checks whether a response is a redirect that can be followed.
     *
     * @param statusCode the HTTP status code
     * @param headers the response headers
     * @return true if the response is a redirect with a location
     */
    private static boolean isRedirect(int statusCode, HttpHeaders headers) {
        Optional<String> location = headers.firstValue("location");
        return statusCode >= 300 && statusCode < 400 && statusCode != 304 && location.isPresent();
    }

    /**
     * Creates a request builder for a link, using the link's URL, method, and headers.
     *
     * @param link the link
     * @param body the request body
     * @return the request builder
     */
    private HttpRequest.Builder linkRequest(AsyncApiLink link, HttpRequest.BodyPublisher body) {
        HttpRequest.Builder builder =
                HttpRequest.newBuilder(link.getUrl()).timeout(requestTimeout()).method(link.getMethod(), body);
        link.getHeaders().forEach((name, value) -> {
            if (!RESTRICTED_HEADERS.contains(name.toLowerCase(Locale.ROOT))) {
                builder.header(name, value);
            }
        });
        return builder;
    }

    /**
     * Returns a link, or throws if it is missing.
     *
     * @param link the link
     * @param name the name of the link, for the exception message
     * @return the link
     * @throws IllegalStateException if the link is null
     */
    private static AsyncApiLink requireLink(AsyncApiLink link, String name) {
        if (link == null) {
            throw new IllegalStateException("Status has no " + name + " link");
        }
        return link;
    }

    /**
     * Returns the request timeout.
     *
     * @return the request timeout
     */
    private Duration requestTimeout() {
        return Duration.ofSeconds(config.getRequestTimeoutSeconds());
    }

    /**
     * Processes a file: initiates a job, uploads the file, waits for processing to complete, and downloads the result.
     *
     * @param request the request containing the file's media type and processing options
     * @param fileData the file data
     * @return the response containing the processed file
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the operation is interrupted
     * @throws AsyncApiException if processing failed or the API returns an error
     * @throws TimeoutException if processing does not complete within the processing timeout
     */
    public AsyncApiResponse process(AsyncApiRequest request, byte[] fileData)
            throws IOException, InterruptedException, AsyncApiException, TimeoutException {
        Objects.requireNonNull(fileData, "fileData cannot be null");
        AsyncApiStatus status = initiate(request);
        upload(status, fileData);
        return download(waitForCompletion(status));
    }

    /**
     * Processes a file from an input stream.
     *
     * @param inputStream the input stream containing the file data
     * @param contentType the media type of the file
     * @return the response containing the processed file
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the operation is interrupted
     * @throws AsyncApiException if processing failed or the API returns an error
     * @throws TimeoutException if processing does not complete within the processing timeout
     */
    public AsyncApiResponse process(InputStream inputStream, String contentType)
            throws IOException, InterruptedException, AsyncApiException, TimeoutException {
        Objects.requireNonNull(inputStream, "inputStream cannot be null");
        return process(AsyncApiRequest.create(contentType), inputStream.readAllBytes());
    }

    /**
     * Processes a file, streaming it from and to disk, so use it for large files.
     *
     * @param request the request containing the file's media type and processing options
     * @param source the file to process
     * @param target the file to write the processed file to, which is replaced if it exists
     * @return the completed status, which includes the risks taken
     * @throws IOException if an I/O error occurs or a file cannot be read or written
     * @throws InterruptedException if the operation is interrupted
     * @throws AsyncApiException if processing failed or the API returns an error
     * @throws TimeoutException if processing does not complete within the processing timeout
     */
    public AsyncApiStatus process(AsyncApiRequest request, Path source, Path target)
            throws IOException, InterruptedException, AsyncApiException, TimeoutException {
        Objects.requireNonNull(source, "source cannot be null");
        Objects.requireNonNull(target, "target cannot be null");
        AsyncApiStatus status = initiate(request);
        upload(status, source);
        status = waitForCompletion(status);
        download(status, target);
        return status;
    }

    /**
     * Returns the configuration used by this client.
     *
     * @return the API configuration
     */
    public AsyncApiConfig getConfig() {
        return config;
    }

    @Override
    public void close() {
        httpClient.close();
    }

    /**
     * Creates a new instance of the AsyncApiClient with the specified configuration.
     *
     * @param config the API configuration
     * @return a new instance of AsyncApiClient
     */
    public static AsyncApiClient create(AsyncApiConfig config) {
        Objects.requireNonNull(config, "config cannot be null");
        return new AsyncApiClient(config);
    }
}
