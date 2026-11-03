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

/** The Everfox CDR Cloud APIs, each of which has its own endpoint in every {@link Region}. */
public enum Api {
    /** The Instant API, which processes a file in a single request. */
    INSTANT("instant"),
    /** The Async API, which processes a file in a job that is polled for completion. */
    ASYNC("async"),
    /** The S3 API, which processes files in Amazon S3. This library does not have a client for it yet. */
    S3("s3");

    /** The subdomain that identifies the API in its host names. */
    private final String subdomain;

    /**
     * Constructs an Api enum constant with the specified subdomain.
     *
     * @param subdomain the subdomain that identifies the API in its host names
     */
    Api(String subdomain) {
        this.subdomain = subdomain;
    }

    /**
     * Returns the subdomain that identifies the API in its host names, for example {@code async} in
     * {@code us-west-2.aws.async.cdr.everfox.com}.
     *
     * @return the subdomain
     */
    public String getSubdomain() {
        return subdomain;
    }
}
