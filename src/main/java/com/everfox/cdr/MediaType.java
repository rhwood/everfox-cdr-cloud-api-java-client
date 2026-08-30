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
 * Available content types for file uploads and downloads.
 */
public enum MediaType {
    /**
     * Content type for Adobe Illustrator files.
     */
    EMF("application/emf"),
    /**
     * Content type for JSON files.
     */
    JSON("application/json"),
    /**
     * Content type for MP4 files.
     */
    MP4("application/mp4"),
    /**
     * Content type for Microsoft Word documents.
     */
    MSWORD("application/msword"),
    /**
     * Content type for PDF files.
     */
    PDF("application/pdf"),
    /**
     * Content type for RTF files.
     */
    RTF("application/rtf"),
    /**
     * Content type for Microsoft Excel addins with macros.
     */
    MS_EXCEL_ADDIN_MACROENABLED_12("application/vnd.ms-excel.addin.macroenabled.12"),
    /**
     * Content type for Microsoft Excel sheets with binary macros.
     */
    MS_EXCEL_SHEET_BINARY_MACROENABLED_12("application/vnd.ms-excel.sheet.binary.macroenabled.12"),
    /**
     * Content type for Microsoft Excel sheets with macros.
     */
    MS_EXCEL_SHEET_MACROENABLED_12("application/vnd.ms-excel.sheet.macroenabled.12"),
    /**
     * Content type for Microsoft Excel templates with macros.
     */
    MS_EXCEL_TEMPLATE_MACROENABLED_12("application/vnd.ms-excel.template.macroenabled.12"),
    /**
     * Content type for Microsoft Outlook files.
     */
    MS_OUTLOOK("application/vnd.ms-outlook"),
    /**
     * Content type for Microsoft PowerPoint files.
     */
    MS_POWERPOINT("application/vnd.ms-powerpoint"),
    /**
     * Content type for Microsoft PowerPoint addins with macros.
     */
    MS_POWERPOINT_ADDIN_MACROENABLED_12("application/vnd.ms-powerpoint.addin.macroenabled.12"),
    /**
     * Content type for Microsoft PowerPoint presentations with macros.
     */
    MS_POWERPOINT_PRESENTATION_MACROENABLED_12("application/vnd.ms-powerpoint.presentation.macroenabled.12"),
    /**
     * Content type for Microsoft PowerPoint slideshows with macros.
     */
    MS_POWERPOINT_SLIDESHOW_MACROENABLED_12("application/vnd.ms-powerpoint.slideshow.macroenabled.12"),
    /**
     * Content type for Microsoft PowerPoint templates with macros.
     */
    MS_POWERPOINT_TEMPLATE_MACROENABLED_12("application/vnd.ms-powerpoint.template.macroenabled.12"),
    /**
     * Content type for Microsoft Visio drawings.
     */
    MS_VISIO_DRAWING("application/vnd.ms-visio.drawing"),
    /**
     * Content type for Microsoft Visio viewers.
     */
    MS_VISIO_VIEWER("application/vnd.ms-visio.viewer"),
    /**
     * Content type for Microsoft Word documents with macros.
     */
    MS_WORD_DOCUMENT_MACROENABLED_12("application/vnd.ms-word.document.macroenabled.12"),
    /**
     * Content type for Microsoft Word templates with macros.
     */
    MS_WORD_TEMPLATE_MACROENABLED_12("application/vnd.ms-word.template.macroenabled.12"),
    /**
     * Content type for OASIS OpenDocument presentations.
     */
    OASIS_OPENDOCUMENT_PRESENTATION("application/vnd.oasis.opendocument.presentation"),
    /**
     * Content type for OASIS OpenDocument spreadsheets.
     */
    OASIS_OPENDOCUMENT_SPREADSHEET("application/vnd.oasis.opendocument.spreadsheet"),
    /**
     * Content type for OASIS OpenDocument text documents.
     */
    OASIS_OPENDOCUMENT_TEXT("application/vnd.oasis.opendocument.text"),
    /**
     * Content type for OpenXML Presentation files.
     */
    OPENXMLFORMATS_OFFICEDOCUMENT_PRESENTATIONML_PRESENTATION("application/vnd.openxmlformats-officedocument.presentationml.presentation"),
    /**
     * Content type for OpenXML Slideshow files.
     */
    OPENXMLFORMATS_OFFICEDOCUMENT_PRESENTATIONML_SLIDESHOW("application/vnd.openxmlformats-officedocument.presentationml.slideshow"),
    /**
     * Content type for OpenXML Presentation Template files.
     */
    OPENXMLFORMATS_OFFICEDOCUMENT_PRESENTATIONML_TEMPLATE("application/vnd.openxmlformats-officedocument.presentationml.template"),
    /**
     * Content type for OpenXML Spreadsheet files.
     */
    OPENXMLFORMATS_OFFICEDOCUMENT_SPREADSHEETML_SHEET("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
    /**
     * Content type for OpenXML Spreadsheet Template files.
     */
    OPENXMLFORMATS_OFFICEDOCUMENT_SPREADSHEETML_TEMPLATE("application/vnd.openxmlformats-officedocument.spreadsheetml.template"),
    /**
     * Content type for OpenXML Word Document files.
     */
    OPENXMLFORMATS_OFFICEDOCUMENT_WORDPROCESSINGML_DOCUMENT("application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
    /**
     * Content type for OpenXML Word Template files.
     */
    OPENXMLFORMATS_OFFICEDOCUMENT_WORDPROCESSINGML_TEMPLATE("application/vnd.openxmlformats-officedocument.wordprocessingml.template"),
    /**
     * Content type for Windows Metafile files.
     */
    WMF("application/wmf"),
    /**
     * Content type for X-compressed files.
     */
    X_COMPRESSED("application/x-compressed"),
    /**
     * Content type for X-ZIP compressed files.
     */
    X_ZIP_COMPRESSED("application/x-zip-compressed"),
    /**
     * Content type for XML files.
     */
    XML("application/xml"),
    /**
     * Content type for ZIP files.
     */
    ZIP("application/zip"),
    /**
     * Content type for MP4 audio files.
     */
    AUDIO_MP4("audio/mp4"),
    /**
     * Content type for MPEG audio files.
     */
    AUDIO_X_M4A("audio/x-m4a"),
    /**
     * Content type for WAV audio files.
     */
    AUDIO_X_WAV("audio/x-wav"),
    /**
     * Content type for BMP image files.
     */
    IMAGE_BMP("image/bmp"),
    /**
     * Content type for GIF image files.
     */
    IMAGE_GIF("image/gif"),
    /**
     * Content type for HEIC image files.
     */
    IMAGE_HEIC("image/heic"),
    /**
     * Content type for HEIF image files.
     */
    IMAGE_HEIF("image/heif"),
    /**
     * Content type for JP2 image files.
     */
    IMAGE_JP2("image/jp2"),
    /**
     * Content type for JPEG image files.
     */
    IMAGE_JPEG("image/jpeg"),
    /**
     * Content type for JPG image files.
     */
    IMAGE_JPG("image/jpg"),
    /**
     * Content type for JXR image files.
     */
    IMAGE_JXR("image/jxr"),
    /**
     * Content type for PNG image files.
     */
    IMAGE_PNG("image/png"),
    /**
     * Content type for TIFF image files.
     */
    IMAGE_TIFF("image/tiff"),
    /**
     * Content type for Microsoft Photo files.
     */
    IMAGE_MS_PHOTO("image/vnd.ms-photo"),
    /**
     * Content type for WebP image files.
     */
    IMAGE_WEBP("image/webp"),
    /**
     * Content type for X-Microsoft BMP files.
     */
    IMAGE_X_MS_BMP("image/x-ms-bmp"),
    /**
     * Content type for RFC822 messages.
     */
    MESSAGE_RFC822("message/rfc822"),
    /**
     * Content type for CSV text files.
     */
    TEXT_CSV("text/csv"),
    /**
     * Content type for RTF text files.
     */
    TEXT_RTF("text/rtf"),
    /**
     * Content type for vCard text files.
     */
    TEXT_VCARD("text/vcard"),
    /**
     * Content type for XML text files.
     */
    TEXT_XML("text/xml"),
    /**
     * Content type for MP4 video files.
     */
    VIDEO_MP4("video/mp4"),
    /**
     * Catch-all content type for all files.
     */
    ALL("*/*");

    private final String mediaType;

    MediaType(String mediaType) {
        this.mediaType = mediaType;
    }

    /**
     * Returns the string representation of the content type.
     *
     * @return the content type as a string
     */
    public String getMediaType() {
        return mediaType;
    }
}