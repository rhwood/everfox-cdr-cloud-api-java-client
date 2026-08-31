/**
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

/**
 * Represents the risks allowed to be taken by the CDR Cloud API when processing
 * a file. These risks are used to determine how a file is processed. See
 * <a href="https://cdr.everfox.com/documentation/risks">Managing Risks</a> for
 * more information.
 */
public enum Risk {
    /**
     * Represents a risk that a file could be an executable.
     */
    EXE("exe"),
    /**
     * Represents a risk that a file contains executable macros.
     */
    EXE_MACRO("exe/macro"),
    /**
     * Represents a risk that a file contains executable macros for Microsoft Office
     * applications.
     */
    EXE_MACRO_MS("exe/macro/ms"),
    /**
     * Represents a risk that a file contains executable macros for Microsoft Excel.
     */
    EXE_MACRO_MS_EXCEL("exe/macro/ms/excel"),
    /**
     * Represents a risk that a file contains executable macros for Microsoft PowerPoint.
     */
    EXE_MACRO_MS_POWERPOINT("exe/macro/ms/powerpoint"),
    /**
     * Represents a risk that a file contains executable macros for Microsoft Word.
     */
    EXE_MACRO_MS_WORD("exe/macro/ms/word"),
    /**
     * Represents a risk that a file is polyglot.
     */
    POLY("poly"),
    /**
     * Represents a risk that a file is a polyglot text file.
     */
    POLY_TEXT("poly/text"),
    /**
     * Represents a risk that a file is a polyglot JSON file.
     */
    POLY_TEXT_JSON("poly/text/json"),
    /**
     * Represents a risk that a file is a polyglot XML file.
     */
    POLY_TEXT_XML("poly/text/xml"),
    /**
     * Represents a risk that a file contains steganography.
     */
    STEG("steg"),
    /**
     * Represents a risk that a file contains steganography in an image.
     */
    STEG_IMAGE("steg/image"),
    /**
     * Represents a risk that a file contains steganography in a BMP image.
     */
    STEG_IMAGE_BMP("steg/image/bmp"),
    /**
     * Represents a risk that a file contains steganography in a GIF image.
     */
    STEG_IMAGE_GIF("steg/image/gif"),
    /**
     * Represents a risk that a file contains steganography in a JPEG image.
     */
    STEG_IMAGE_JPEG("steg/image/jpeg"),
    /**
     * Represents a risk that a file contains steganography in a JPEG2000 image.
     */
    STEG_IMAGE_JPEG2K("steg/image/jpeg2k"),
    /**
     * Represents a risk that a file contains steganography in a JPEG-XR image.
     */
    STEG_IMAGE_JXR("steg/image/jxr"),
    /**
     * Represents a risk that a file contains steganography in a PDF image.
     */
    STEG_IMAGE_PDF("steg/image/pdf"),
    /**
     * Represents a risk that a file contains steganography in a PNG image.
     */
    STEG_IMAGE_PNG("steg/image/png"),
    /**
     * Represents a risk that a file contains steganography in a WEBP image.
     */
    STEG_IMAGE_WEBP("steg/image/webp"),
    /**
     * Represents a risk that a file is structured data that can't be transformed.
     */
    STRUCTURED("structured"),
    /**
     * Represents a risk that a file is structured data that does not have a schema.
     */
    STRUCTURED_NO_SCHEMA("structured/no-schema"),
    /**
     * Represents a risk that a file is JSON and does not have a schema.
     */
    STRUCTURED_NO_SCHEMA_JSON("structured/no-schema/json"),
    /**
     * Represents a risk that a file is XML and does not have a schema.
     */
    STRUCTURED_NO_SCHEMA_XML("structured/no-schema/xml");

    private final String risk;

    /**
     * Constructs a Risk enum with the specified risk string.
     *
     * @param risk the risk string
     */
    Risk(String risk) {
        this.risk = risk;
    }

    /**
     * Returns the risk string associated with this Risk enum.
     *
     * @return the risk string
     */
    public String getRisk() {
        return risk;
    }

    /**
     * Returns the risk string associated with this Risk enum.
     *
     * @return the risk string
     */
    public String toString() {
        return getRisk();
    }
}
