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
import com.everfox.cdr.internal.HttpSupport;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * The status of an Async API processing job, as returned by the initiate and status requests or published to an SNS
 * topic.
 *
 * <p>The status contains the links needed for the next step: an upload link after initiating, a status link until
 * processing is complete, and a result link once processing has succeeded.
 */
public class AsyncApiStatus {

    /** The Jackson object mapper used to parse the status. */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** The job identifier. */
    private final String id;
    /** The text description of what is currently happening. */
    private final String state;
    /** Whether processing is complete. */
    private final boolean complete;
    /** Whether processing succeeded, or null if not complete. */
    private final Boolean success;
    /** The user data given in the request options. */
    private final String userData;
    /** The error if processing completed unsuccessfully. */
    private final CdrApiError error;
    /** The risks taken during processing. */
    private final List<String> risksTaken;
    /** The link to upload the file to. */
    private final AsyncApiLink uploadLink;
    /** The link to request the status from. */
    private final AsyncApiLink statusLink;
    /** The link to download the processed file from. */
    private final AsyncApiLink resultLink;
    /** The ETag header of the status response. */
    private final String etag;
    /** The status JSON. */
    private final String json;

    /**
     * Creates a new status.
     *
     * @param root the parsed status JSON
     * @param json the status JSON
     * @param etag the ETag header of the status response
     */
    // private to force use of the static factory methods
    private AsyncApiStatus(JsonNode root, String json, String etag) {
        JsonNode status = root.path("status");
        JsonNode links = root.path("links");
        this.id = text(root, "id");
        this.state = text(status, "state");
        this.complete = status.path("complete").asBoolean(false);
        this.success =
                status.path("success").isBoolean() ? status.path("success").booleanValue() : null;
        this.userData = text(root, "userData");
        this.error = root.path("error").isObject() ? CdrApiError.fromJson(json.getBytes(StandardCharsets.UTF_8)) : null;
        List<String> taken = new ArrayList<>();
        root.path("risks").path("taken").values().forEach(risk -> {
            if (risk.isString()) {
                taken.add(risk.stringValue());
            }
        });
        this.risksTaken = List.copyOf(taken);
        this.uploadLink = AsyncApiLink.fromJson(links.get("upload"));
        this.statusLink = AsyncApiLink.fromJson(links.get("status"));
        this.resultLink = AsyncApiLink.fromJson(links.get("result"));
        this.etag = etag;
        this.json = json;
    }

    /**
     * Returns the job identifier.
     *
     * @return the identifier, or null if not present
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the text description of what is currently happening, for example {@code WAITING_FOR_UPLOAD},
     * {@code PROCESSING}, or {@code COMPLETE}.
     *
     * @return the state, or null if not present
     */
    public String getState() {
        return state;
    }

    /**
     * Returns whether processing is complete, successfully or not.
     *
     * @return true if complete
     */
    public boolean isComplete() {
        return complete;
    }

    /**
     * Returns whether processing completed successfully.
     *
     * @return true if complete and successful
     */
    public boolean isSuccess() {
        return complete && Boolean.TRUE.equals(success);
    }

    /**
     * Returns the user data given in the request options.
     *
     * @return the user data, or null if not present
     */
    public String getUserData() {
        return userData;
    }

    /**
     * Returns the error if processing completed unsuccessfully.
     *
     * @return the error, or null if not present
     */
    public CdrApiError getError() {
        return error;
    }

    /**
     * Returns the risks taken during processing.
     *
     * @return the risks taken, which is empty if none were taken or processing is not complete
     */
    public List<String> getRisksTaken() {
        return risksTaken;
    }

    /**
     * Returns the link to upload the file to.
     *
     * @return the upload link, or null if not present
     */
    public AsyncApiLink getUploadLink() {
        return uploadLink;
    }

    /**
     * Returns the link to request the status from. This is present until processing is complete.
     *
     * @return the status link, or null if not present
     */
    public AsyncApiLink getStatusLink() {
        return statusLink;
    }

    /**
     * Returns the link to download the processed file from. This is present once processing has succeeded.
     *
     * @return the result link, or null if not present
     */
    public AsyncApiLink getResultLink() {
        return resultLink;
    }

    /**
     * Returns the ETag header of the status response, which is sent as {@code If-None-Match} when polling.
     *
     * @return the ETag, or null if not present
     */
    public String getEtag() {
        return etag;
    }

    /**
     * Returns the status JSON, which includes any properties this class does not expose. Treat it as sensitive because
     * the links it contains grant access to the job.
     *
     * @return the status JSON
     */
    public String toJson() {
        return json;
    }

    /**
     * Creates a status from its JSON representation, for example from an SNS notification.
     *
     * @param json the status JSON
     * @return the status
     * @throws IllegalArgumentException if the JSON is not a JSON object
     */
    public static AsyncApiStatus fromJson(String json) {
        Objects.requireNonNull(json, "json cannot be null");
        return fromJson(json, null);
    }

    /**
     * Creates a status from its JSON representation.
     *
     * @param json the status JSON
     * @param etag the ETag header of the status response
     * @return the status
     * @throws IllegalArgumentException if the JSON is not a JSON object
     */
    private static AsyncApiStatus fromJson(String json, String etag) {
        JsonNode root;
        try {
            root = MAPPER.readTree(json);
        } catch (JacksonException e) {
            throw new IllegalArgumentException("Status is not valid JSON", e);
        }
        // readTree never returns null; empty input gives a MissingNode
        if (!root.isObject()) {
            throw new IllegalArgumentException("Status is not a JSON object");
        }
        return new AsyncApiStatus(root, json, etag);
    }

    /**
     * Creates a status from an HTTP response.
     *
     * @param httpResponse the HTTP response
     * @return the status
     * @throws AsyncApiException if the response indicates an error or does not contain a status
     */
    static AsyncApiStatus fromHttpResponse(HttpResponse<byte[]> httpResponse) throws AsyncApiException {
        HttpSupport.throwIfError(httpResponse, AsyncApiException::create);
        int statusCode = httpResponse.statusCode();
        byte[] body = httpResponse.body();
        try {
            return fromJson(
                    new String(body != null ? body : new byte[0], StandardCharsets.UTF_8),
                    httpResponse.headers().firstValue("etag").orElse(null));
        } catch (IllegalArgumentException e) {
            throw AsyncApiException.create(statusCode, body);
        }
    }

    /**
     * Returns the string value of a property.
     *
     * @param node the JSON object
     * @param property the property name
     * @return the property value, or null if missing or not a string
     */
    private static String text(JsonNode node, String property) {
        JsonNode value = node.path(property);
        return value.isString() ? value.stringValue() : null;
    }
}
