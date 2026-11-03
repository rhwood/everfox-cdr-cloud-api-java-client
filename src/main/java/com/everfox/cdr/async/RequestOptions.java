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

import com.everfox.cdr.options.ProcessingOptions;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Options for customizing file processing behavior, sent as the {@code options} object of the initiate request. These
 * are the {@linkplain ProcessingOptions shared options} plus quota usage and status notification options.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class RequestOptions extends ProcessingOptions {

    /** The maximum length of the user data. */
    public static final int MAX_USER_DATA_LENGTH = 16384;

    /** Quota usage options. */
    private final CostsOptions costs;
    /** Status notification options. */
    private final StatusOptions status;

    /**
     * Creates the options held by a builder.
     *
     * @param builder the builder holding the options
     */
    // private to force use of the builder
    private RequestOptions(Builder builder) {
        super(builder);
        this.costs = builder.largeFileEnvironment != null ? new CostsOptions(builder.largeFileEnvironment) : null;
        this.status = builder.userData != null || builder.snsTopicArn != null
                ? new StatusOptions(builder.snsTopicArn, builder.snsRoleArn, builder.userData)
                : null;
    }

    /**
     * Returns the quota usage options.
     *
     * @return the quota usage options
     */
    public CostsOptions getCosts() {
        return costs;
    }

    /**
     * Returns the status notification options.
     *
     * @return the status options
     */
    public StatusOptions getStatus() {
        return status;
    }

    /** Options that affect quota usage. */
    // public for JSON serialization
    public static final class CostsOptions {
        /** Whether to use the large file environment for files over 250 MB. */
        private final boolean largeFileEnvironment;

        /**
         * Create a set of quota usage options.
         *
         * @param largeFileEnvironment whether to use the large file environment for files over 250 MB
         */
        private CostsOptions(boolean largeFileEnvironment) {
            this.largeFileEnvironment = largeFileEnvironment;
        }

        /**
         * Returns the file processing environment options as a map for JSON serialization.
         *
         * @return a map with a "large" key holding the large file environment options
         */
        public Map<String, Map<String, Boolean>> getEnvironment() {
            return Map.of("large", Map.of("enabled", largeFileEnvironment));
        }
    }

    /** Status notification options. */
    // public for JSON serialization
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static final class StatusOptions {
        /** The ARN of the SNS topic to publish status updates to. */
        private final String snsTopicArn;
        /** The ARN of the role to assume to publish to the SNS topic. */
        private final String snsRoleArn;
        /** Data returned in the status, for the caller's use. */
        private final String userData;

        /**
         * Create a set of status options.
         *
         * @param snsTopicArn the ARN of the SNS topic to publish status updates to, or null
         * @param snsRoleArn the ARN of the role to assume to publish to the SNS topic, or null
         * @param userData data returned in the status, or null
         */
        private StatusOptions(String snsTopicArn, String snsRoleArn, String userData) {
            this.snsTopicArn = snsTopicArn;
            this.snsRoleArn = snsRoleArn;
            this.userData = userData;
        }

        /**
         * Returns the SNS options as a map for JSON serialization.
         *
         * @return a map with "topicArn" and "roleArn" keys, or null if no SNS topic is specified
         */
        public Map<String, String> getSns() {
            if (snsTopicArn == null) {
                return null;
            }
            Map<String, String> sns = new LinkedHashMap<>();
            sns.put("topicArn", snsTopicArn);
            sns.put("roleArn", snsRoleArn);
            return sns;
        }

        /**
         * Returns the user data.
         *
         * @return the user data
         */
        public String getUserData() {
            return userData;
        }
    }

    /**
     * Creates a new instance of the Builder.
     *
     * @return a new instance of the Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /** Builder for creating {@link RequestOptions} instances. */
    public static final class Builder extends ProcessingOptions.Builder<Builder> {

        /** Whether to use the large file environment, or null to use the API default. */
        private Boolean largeFileEnvironment;
        /** The ARN of the SNS topic to publish status updates to. */
        private String snsTopicArn;
        /** The ARN of the role to assume to publish to the SNS topic. */
        private String snsRoleArn;
        /** Data returned in the status. */
        private String userData;

        /** Creates a new instance of the Builder. */
        public Builder() {}

        @Override
        protected Builder self() {
            return this;
        }

        /**
         * Sets whether to use the large file environment automatically for files over 250 MB. This increases the quota
         * usage rate.
         *
         * @param enabled true to use the large file environment
         * @return the builder instance
         */
        public Builder largeFileEnvironment(boolean enabled) {
            this.largeFileEnvironment = enabled;
            return this;
        }

        /**
         * Sets the AWS SNS topic to publish status updates to, as an alternative to polling for status.
         *
         * @param topicArn the ARN of the SNS topic
         * @param roleArn the ARN of a role in the topic's account that the API assumes to publish to the topic
         * @return the builder instance
         */
        public Builder snsStatus(String topicArn, String roleArn) {
            Objects.requireNonNull(topicArn, "topicArn cannot be null");
            Objects.requireNonNull(roleArn, "roleArn cannot be null");
            this.snsTopicArn = topicArn;
            this.snsRoleArn = roleArn;
            return this;
        }

        /**
         * Sets data that is returned in every status for this job, for example to correlate SNS notifications.
         *
         * @param userData the user data, at most {@value RequestOptions#MAX_USER_DATA_LENGTH} characters
         * @return the builder instance
         * @throws IllegalArgumentException if the user data is too long
         */
        public Builder userData(String userData) {
            if (userData != null && userData.length() > MAX_USER_DATA_LENGTH) {
                throw new IllegalArgumentException(
                        "userData cannot be longer than " + MAX_USER_DATA_LENGTH + " characters");
            }
            this.userData = userData;
            return this;
        }

        /**
         * Builds the {@link RequestOptions} instance with the specified configurations.
         *
         * @return a new instance of RequestOptions
         * @throws IllegalArgumentException if any risk is both allowed and denied
         */
        public RequestOptions build() {
            validate();
            return new RequestOptions(this);
        }
    }
}
