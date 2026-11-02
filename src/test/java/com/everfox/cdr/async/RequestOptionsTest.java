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

import static org.junit.jupiter.api.Assertions.*;

import com.everfox.cdr.Risk;
import com.everfox.cdr.options.ReportFormat;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the async {@link RequestOptions}: the async-only options, and that they combine with the shared ones.
 * The shared options are tested in {@code ProcessingOptionsTest}.
 */
class RequestOptionsTest {

    /** Default constructor. */
    RequestOptionsTest() {
        // Nothing to do
    }

    /** Tests that shared and async-only builder methods chain in any order, and serialize after the shared options. */
    @Test
    void testBuilderChaining() {
        RequestOptions options = RequestOptions.builder()
                .userData("mine")
                .reportFormat(ReportFormat.NONE)
                .largeFileEnvironment(true)
                .allowRisks(Risk.EXE)
                .build();

        assertEquals(
                "{\"report\":{\"format\":\"none\"},\"risks\":{\"allow\":[\"exe\"]},"
                        + "\"costs\":{\"environment\":{\"large\":{\"enabled\":true}}},"
                        + "\"status\":{\"userData\":\"mine\"}}",
                options.toJson());
        assertEquals("{}", new RequestOptions.Builder().build().toJson());
    }

    /** Tests that the shared validation runs when building async options. */
    @Test
    void testBuilderValidates() {
        RequestOptions.Builder builder =
                RequestOptions.builder().allowRisks(Risk.EXE).denyRisks(Risk.EXE);

        assertThrows(IllegalArgumentException.class, builder::build);
    }

    /** Tests the large file environment option. */
    @Test
    void testLargeFileEnvironment() {
        assertEquals(
                "{\"costs\":{\"environment\":{\"large\":{\"enabled\":true}}}}",
                RequestOptions.builder().largeFileEnvironment(true).build().toJson());
        assertEquals(
                "{\"costs\":{\"environment\":{\"large\":{\"enabled\":false}}}}",
                RequestOptions.builder().largeFileEnvironment(false).build().toJson());
    }

    /** Tests the status options. */
    @Test
    void testStatus() {
        assertEquals(
                "{\"status\":{\"sns\":{\"topicArn\":\"topic\",\"roleArn\":\"role\"},\"userData\":\"mine\"}}",
                RequestOptions.builder()
                        .snsStatus("topic", "role")
                        .userData("mine")
                        .build()
                        .toJson());
        assertEquals(
                "{\"status\":{\"sns\":{\"topicArn\":\"topic\",\"roleArn\":\"role\"}}}",
                RequestOptions.builder().snsStatus("topic", "role").build().toJson());
    }

    /** Tests that the status options are validated. */
    @Test
    void testStatusValidation() {
        RequestOptions.Builder builder = RequestOptions.builder();
        assertThrows(NullPointerException.class, () -> builder.snsStatus(null, "role"));
        assertThrows(NullPointerException.class, () -> builder.snsStatus("topic", null));
        assertThrows(
                IllegalArgumentException.class,
                () -> builder.userData("x".repeat(RequestOptions.MAX_USER_DATA_LENGTH + 1)));
        builder.userData("x".repeat(RequestOptions.MAX_USER_DATA_LENGTH));
        builder.userData(null);
        assertEquals("{}", builder.build().toJson());
    }
}
