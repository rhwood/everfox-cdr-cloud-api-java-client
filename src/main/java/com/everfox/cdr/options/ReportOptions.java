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
package com.everfox.cdr.options;

/**
 * Report options, as serialized in the {@code report} property. Set the format with
 * {@link ProcessingOptions.Builder#reportFormat(ReportFormat)}.
 */
// public for JSON serialization
public final class ReportOptions {
    /** The preferred format of the report. */
    private final ReportFormat format;

    /**
     * Create a set of report options.
     *
     * @param format the preferred format of the report
     */
    ReportOptions(ReportFormat format) {
        this.format = format;
    }

    /**
     * The preferred format of the report.
     *
     * @return the format
     */
    public ReportFormat getFormat() {
        return format;
    }
}
