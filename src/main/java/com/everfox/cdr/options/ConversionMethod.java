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

/** Format conversion methods (tools). See https://cdr.everfox.com/documentation/format-conversion for details. */
public enum ConversionMethod {
    /** The default set of conversion tools. Disable this to only use explicitly enabled tools. */
    DEFAULT("default"),
    /** Convert files using LibreOffice. */
    LIBREOFFICE("libreoffice"),
    /** Return the report instead of the transformed file. */
    REPORT("report"),
    /** Add the text extracted from supported documents to the report. */
    TEXT_EXTRACTION("text-extraction");

    /** The string representation of the conversion method. */
    private final String method;

    /**
     * Creates a new conversion method with the specified string representation.
     *
     * @param method the string representation of the conversion method
     */
    ConversionMethod(String method) {
        this.method = method;
    }

    /**
     * Returns the string representation of the conversion method.
     *
     * @return the conversion method as a string
     */
    public String getMethod() {
        return method;
    }

    @Override
    public String toString() {
        return getMethod();
    }
}
