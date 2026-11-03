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
package com.everfox.cdr;

import java.io.Serializable;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * An error reported by an Everfox CDR Cloud API, which is a JSON object of the form <code>
 * {"error": {"code": ..., "message": ..., "name": ..., "type": ..., "subType": ...}}</code>.
 *
 * <p>All the APIs return this in the body of a failed HTTP response. The Async API also returns it in the status of a
 * job that completes unsuccessfully. See the <a href="https://cdr.everfox.com/api/errors">error glossary</a> for the
 * codes.
 */
public class CdrApiError implements Serializable {

    /** The serialization version. */
    private static final long serialVersionUID = 1L;

    /** The code used when the API did not return a valid error. */
    public static final int UNKNOWN_CODE = -1;

    /** The Jackson object mapper used to parse errors. */
    private static final ObjectMapper MAPPER = new ObjectMapper();

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
    private CdrApiError(int code, String message, String name, String type, String subType) {
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
    public static CdrApiError unknown() {
        return new CdrApiError(UNKNOWN_CODE, "Unknown error", "Unknown", "Unknown", null);
    }

    /**
     * Creates an error from a response body or status containing an {@code error} property.
     *
     * @param json the JSON document, which may be null
     * @return the error, or {@link #unknown()} if the document is not JSON or has no valid {@code error} property
     */
    public static CdrApiError fromJson(byte[] json) {
        if (json == null || json.length == 0) {
            return unknown();
        }
        try {
            return fromErrorNode(MAPPER.readTree(json).get("error"));
        } catch (Exception e) {
            // Not JSON (for example, an XML error from a storage service), so the error is unknown
            return unknown();
        }
    }

    /**
     * Creates an error from the JSON error object, that is, the value of an {@code error} property.
     *
     * @param error the JSON error object
     * @return the error, or {@link #unknown()} if the object does not contain a numeric code
     */
    private static CdrApiError fromErrorNode(JsonNode error) {
        if (error == null || !error.path("code").isIntegralNumber()) {
            return unknown();
        }
        return new CdrApiError(
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
