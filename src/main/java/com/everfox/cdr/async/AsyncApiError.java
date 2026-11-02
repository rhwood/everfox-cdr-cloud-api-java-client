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

import java.io.Serializable;
import tools.jackson.databind.JsonNode;

/**
 * An error reported by the Async API.
 *
 * <p>The API reports errors in two places: in the body of a failed HTTP response, and in the {@code error} property of
 * a status when processing completes unsuccessfully. Both have the same shape. See the <a
 * href="https://cdr.everfox.com/api/errors">error glossary</a> for the codes.
 */
public class AsyncApiError implements Serializable {

    /** The serialization version. */
    private static final long serialVersionUID = 1L;

    /** The code used when the API did not return a valid error. */
    public static final int UNKNOWN_CODE = -1;

    /** The error code. */
    private final int code;
    /** The human-readable description of the error. */
    private final String message;
    /** The name of the error. */
    private final String name;
    /** The category of the error. */
    private final String type;
    /** The sub-category of the error. */
    private final String subType;

    /**
     * Creates a new error.
     *
     * @param code the error code
     * @param message the human-readable description of the error
     * @param name the name of the error
     * @param type the category of the error
     * @param subType the sub-category of the error
     */
    // private to force use of the static factory methods
    private AsyncApiError(int code, String message, String name, String type, String subType) {
        this.code = code;
        this.message = message;
        this.name = name;
        this.type = type;
        this.subType = subType;
    }

    /**
     * Returns the error code. This is {@link #UNKNOWN_CODE} if the API did not return a valid error.
     *
     * @return the error code
     */
    public int getCode() {
        return code;
    }

    /**
     * Returns the human-readable description of the error. This is "Unknown error" if the API did not return a valid
     * error.
     *
     * @return the error message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Returns the name of the error, for example {@code PROCESSING_NOT_RECOGNISED}. This is "Unknown" if the API did
     * not return a valid error.
     *
     * @return the error name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the category of the error, for example {@code BadRequest}. This is "Unknown" if the API did not return a
     * valid error.
     *
     * @return the error type
     */
    public String getType() {
        return type;
    }

    /**
     * Returns the sub-category of the error, for example {@code Parsing/NotRecognised}.
     *
     * @return the error sub-type, or null if not present
     */
    public String getSubType() {
        return subType;
    }

    /**
     * Returns the error used when the API did not return a valid error.
     *
     * @return the unknown error
     */
    public static AsyncApiError unknown() {
        return new AsyncApiError(UNKNOWN_CODE, "Unknown error", "Unknown", "Unknown", null);
    }

    /**
     * Creates an error from the JSON error object, that is, the value of an {@code error} property.
     *
     * @param error the JSON error object
     * @return the error, or {@link #unknown()} if the object does not contain a numeric code
     */
    static AsyncApiError fromJson(JsonNode error) {
        if (error == null || !error.path("code").isIntegralNumber()) {
            return unknown();
        }
        return new AsyncApiError(
                error.path("code").asInt(),
                text(error, "message", "Unknown error"),
                text(error, "name", "Unknown"),
                text(error, "type", "Unknown"),
                text(error, "subType", null));
    }

    /**
     * Returns the string value of a property.
     *
     * @param node the JSON object
     * @param property the property name
     * @param defaultValue the value to return if the property is missing or not a string
     * @return the property value or the default value
     */
    private static String text(JsonNode node, String property, String defaultValue) {
        JsonNode value = node.path(property);
        return value.isString() ? value.stringValue() : defaultValue;
    }
}
