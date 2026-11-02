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
package com.everfox.cdr.instant;

import static org.junit.jupiter.api.Assertions.*;

import com.everfox.cdr.Risk;
import com.everfox.cdr.options.ReportFormat;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the Instant {@link RequestOptions}. The shared options are tested in {@code ProcessingOptionsTest}.
 */
class RequestOptionsTest {

    /** Default constructor. */
    RequestOptionsTest() {
        // Nothing to do
    }

    /** Tests that the shared builder methods chain and build Instant options. */
    @Test
    void testBuilder() {
        RequestOptions options = RequestOptions.builder()
                .reportFormat(ReportFormat.FULL)
                .allowRisks(Risk.EXE)
                .build();

        assertEquals("{\"report\":{\"format\":\"full\"},\"risks\":{\"allow\":[\"exe\"]}}", options.toJson());
        assertEquals("{}", new RequestOptions.Builder().build().toJson());
    }

    /** Tests that the shared validation runs when building Instant options. */
    @Test
    void testBuilderValidates() {
        RequestOptions.Builder builder =
                RequestOptions.builder().allowRisks(Risk.EXE).denyRisks(Risk.EXE);

        assertThrows(IllegalArgumentException.class, builder::build);
    }
}
