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

/** Report format options. See https://cdr.everfox.com/documentation/report for details. */
public enum ReportFormat {
    /** Reports on data that has had a notable change. */
    CHANGED("changed"),
    /** The current default behavior is {@link #CHANGED}. */
    DEFAULT("default"),
    /**
     * Reports everything about the data, including information on data that hasn't been transformed. These reports can
     * be big!
     */
    FULL("full"),
    /** A report will not be generated. Use this if you don't need the report. */
    NONE("none");

    /** The string representation of the report format. */
    private final String format;

    /**
     * Creates a new report format with the specified string representation.
     *
     * @param format the string representation of the report format
     */
    ReportFormat(String format) {
        this.format = format;
    }

    /**
     * Returns the string representation of the report format.
     *
     * @return the report format as a string
     */
    public String getFormat() {
        return format;
    }

    @Override
    public String toString() {
        return getFormat();
    }
}
