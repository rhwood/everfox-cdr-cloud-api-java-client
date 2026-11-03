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

/**
 * Formats that the live Instant and Async APIs reject as a file's media type, but convert to Office Open XML when sent
 * as {@link MediaType#OCTET_STREAM} with the format's media type as {@code conversion.from}.
 *
 * <p>This was confirmed against both production APIs on 2–3 November 2026. The published Instant API spec lists all of
 * these as valid {@code Content-Type} values, and has been reported to Everfox as inconsistent with the live API. If
 * these tests start failing, check whether Everfox has changed the APIs.
 */
public enum ConvertibleFormat {
    /** Word 97–2003 document. */
    DOC(MediaType.MSWORD.getMediaType(), "test.doc", Output.DOCX),
    /** Rich Text Format document, as {@code application/rtf}. */
    RTF(MediaType.RTF.getMediaType(), "test.rtf", Output.DOCX),
    /** Rich Text Format document, as {@code text/rtf}. */
    TEXT_RTF(MediaType.TEXT_RTF.getMediaType(), "test.rtf", Output.DOCX),
    /** OpenDocument text document. */
    ODT(MediaType.OASIS_OPENDOCUMENT_TEXT.getMediaType(), "test.odt", Output.DOCX),
    /** Excel 97–2003 workbook. */
    XLS(MediaType.MS_EXCEL.getMediaType(), "test.xls", Output.XLSX),
    /** Excel binary workbook, with the capitalization used by Microsoft. */
    XLSB("application/vnd.ms-excel.sheet.binary.macroEnabled.12", "test.xlsb", Output.XLSX),
    /** Excel binary workbook, in lower case as in {@link MediaType}. */
    XLSB_LOWER_CASE(MediaType.MS_EXCEL_SHEET_BINARY_MACROENABLED_12.getMediaType(), "test.xlsb", Output.XLSX),
    /** OpenDocument spreadsheet. */
    ODS(MediaType.OASIS_OPENDOCUMENT_SPREADSHEET.getMediaType(), "test.ods", Output.XLSX),
    /** Comma-separated values. */
    CSV(MediaType.TEXT_CSV.getMediaType(), "test.csv", Output.XLSX),
    /** PowerPoint 97–2003 presentation. */
    PPT(MediaType.MS_POWERPOINT.getMediaType(), "test.ppt", Output.PPTX),
    /** OpenDocument presentation. */
    ODP(MediaType.OASIS_OPENDOCUMENT_PRESENTATION.getMediaType(), "test.odp", Output.PPTX);

    /**
     * The Office Open XML formats the APIs convert to. Both APIs accept these as a file's media type and return them
     * unchanged in format.
     */
    public enum Output {
        /** Word document. */
        DOCX(MediaType.OPENXMLFORMATS_OFFICEDOCUMENT_WORDPROCESSINGML_DOCUMENT, "test.docx"),
        /** Excel workbook. */
        XLSX(MediaType.OPENXMLFORMATS_OFFICEDOCUMENT_SPREADSHEETML_SHEET, "test.xlsx"),
        /** PowerPoint presentation. */
        PPTX(MediaType.OPENXMLFORMATS_OFFICEDOCUMENT_PRESENTATIONML_PRESENTATION, "test.pptx");

        /** The media type of the format. */
        private final MediaType mediaType;
        /** The name of the sample file. */
        private final String fileName;

        /**
         * Creates an output format.
         *
         * @param mediaType the media type of the format
         * @param fileName the name of the sample file
         */
        Output(MediaType mediaType, String fileName) {
            this.mediaType = mediaType;
            this.fileName = fileName;
        }

        /**
         * Returns the contents of the sample file.
         *
         * @return the file contents
         */
        public byte[] load() {
            return TestResources.load(fileName);
        }

        /**
         * Returns the media type of the format.
         *
         * @return the media type
         */
        public MediaType getMediaType() {
            return mediaType;
        }
    }

    /** The media type of the format. */
    private final String mediaType;
    /** The name of the sample file. */
    private final String fileName;
    /** The format the APIs convert to. */
    private final Output output;

    /**
     * Creates a convertible format.
     *
     * @param mediaType the media type of the format
     * @param fileName the name of the sample file
     * @param output the format the APIs convert to
     */
    ConvertibleFormat(String mediaType, String fileName, Output output) {
        this.mediaType = mediaType;
        this.fileName = fileName;
        this.output = output;
    }

    /**
     * Returns the media type of the format.
     *
     * @return the media type
     */
    public String getMediaType() {
        return mediaType;
    }

    /**
     * Returns the contents of the sample file.
     *
     * @return the file contents
     */
    public byte[] load() {
        return TestResources.load(fileName);
    }

    /**
     * Returns the format the APIs convert to.
     *
     * @return the output format
     */
    public Output getOutput() {
        return output;
    }
}
