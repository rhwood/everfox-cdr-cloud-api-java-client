# Everfox CDR Instant API Client

Java client library for the Everfox CDR (Content Disarm and Reconstruction) Instant API.

## Overview

This library provides a simple, type-safe interface for uploading files to the Everfox CDR service, which removes potential security threats and returns sanitized versions. It's designed for use with Apache NiFi 2.0.0+ but can be used in any Java 21+ application.

## Requirements

- Java 21 or higher
- Maven 3.6 or higher

## Installation

⚠️ Publication location is TBD ⚠️

Add to your `pom.xml`:

```xml
<dependency>
    <groupId>com.everfox.cdr</groupId>
    <artifactId>everfox-cdr-instant-client</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

## Quick Start

```java
import com.everfox.cdr.MediaType;
import com.everfox.cdr.Region;
import com.everfox.cdr.instant.InstantApiClient;
import com.everfox.cdr.instant.InstantApiConfig;
import com.everfox.cdr.instant.InstantApiException;
import com.everfox.cdr.instant.InstantApiRequest;
import com.everfox.cdr.instant.InstantApiResponse;
import java.nio.file.Files;
import java.nio.file.Path;

InstantApiConfig config = InstantApiConfig.create("your-api-key", Region.US_WEST_2);

try (InstantApiClient client = InstantApiClient.create(config)) {
    byte[] fileData = Files.readAllBytes(Path.of("document.pdf"));
    InstantApiRequest request = InstantApiRequest.create(fileData, MediaType.PDF, MediaType.PDF);

    InstantApiResponse response = client.upload(request);

    byte[] sanitizedData = response.getBody();
    String risksTaken = response.getRisksTaken(); // X-Risks-Taken header, or null
    String report = response.getReport(); // X-Report header (JSON), or null
} catch (InstantApiException e) {
    System.err.println("API error: " + e.getMessage());
}
```

`upload` also throws `IOException` and `InterruptedException` for transport failures.

## Library Structure

| Class | Package | Purpose |
|---|---|---|
| `InstantApiConfig` | `com.everfox.cdr.instant` | API key, endpoint, and timeouts. Created with `InstantApiConfig.create(...)`. |
| `InstantApiClient` | `com.everfox.cdr.instant` | Uploads files. Created with `InstantApiClient.create(config)`; implements `AutoCloseable`. |
| `InstantApiRequest` | `com.everfox.cdr.instant` | File data, content type, accept type, and options. Created with `InstantApiRequest.create(...)`. |
| `InstantApiResponse` | `com.everfox.cdr.instant` | Processed file, status code, headers, risks taken, and report. |
| `InstantApiException` | `com.everfox.cdr.instant` | Error returned by the API (HTTP 4xx/5xx). |
| `RequestOptions` | `com.everfox.cdr.instant` | Processing options sent in the `X-Options` header. Created with `RequestOptions.builder()`. |
| `MediaType` | `com.everfox.cdr` | Media types supported by the API. |
| `Region` | `com.everfox.cdr` | Regional API endpoints. |
| `Risk` | `com.everfox.cdr` | Risks that can be allowed or denied. |

## Configuration

### Regions

Choose the region closest to your deployment:

- `Region.EU_WEST_1` - Europe (Ireland)
- `Region.EU_WEST_2` - Europe (London)
- `Region.US_WEST_2` - US West (Oregon)

```java
InstantApiConfig config = InstantApiConfig.create(apiKey, Region.EU_WEST_2);

// Region names can also be parsed (case-insensitive), e.g. from configuration
Region region = Region.fromString("eu_west_2");
```

### Custom Endpoint

```java
InstantApiConfig config = InstantApiConfig.create(apiKey, URI.create("https://us-west-2.aws.instant.cdr.everfox.com/v1"));
```

### Timeouts

The connection timeout defaults to 10 seconds and the request timeout to 60 seconds. Either can be overridden with a `Region` or `URI`:

```java
// 5 second connection timeout, 120 second request timeout
InstantApiConfig config = InstantApiConfig.create(apiKey, Region.US_WEST_2, 5, 120);
```

## Making Requests

Requests can be created with `MediaType` constants or media type strings. Use strings for media types without a `MediaType` constant, or for a comma-separated list of accept types:

```java
InstantApiRequest request = InstantApiRequest.create(fileData, MediaType.AUDIO_MPEG, MediaType.AUDIO_MPEG);
```

The client always adds `application/json` to the `Accept` header so that API errors can be read. Files are limited to 4.5 MB.

### Input Streams

```java
try (InputStream input = Files.newInputStream(Path.of("document.docx"));
        InstantApiClient client = InstantApiClient.create(config)) {
    InstantApiResponse response = client.upload(
            input,
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
}
```

## Handling Errors

API errors are thrown as `InstantApiException`, which exposes the details from the API's JSON error response:

```java
try {
    InstantApiResponse response = client.upload(request);
} catch (InstantApiException e) {
    int httpStatus = e.getHttpStatusCode(); // e.g. 400
    int apiCode = e.getApiStatusCode(); // e.g. 1110
    String name = e.getName(); // e.g. "RISK_NOT_ALLOWED"
    String type = e.getType(); // e.g. "BadRequest"
    String message = e.getApiMessage(); // human readable description
}
```

## Request Options

Processing options are built with `RequestOptions.builder()` and passed when creating the request:

```java
RequestOptions options = RequestOptions.builder()
        .reportFormat(RequestOptions.ReportFormat.FULL)
        .allowRisks(Risk.POLY_TEXT_JSON, Risk.STRUCTURED_NO_SCHEMA_JSON)
        .build();

InstantApiRequest request = InstantApiRequest.create(fileData, MediaType.JSON, MediaType.JSON, options);
```

See the [Everfox CDR documentation](https://cdr.everfox.com/documentation/risks) for details on each option.

### Report Format

`ReportFormat.CHANGED` (the default), `FULL`, or `NONE` controls the detail of the report returned in the `X-Report` header:

```java
RequestOptions options = RequestOptions.builder()
        .reportFormat(RequestOptions.ReportFormat.FULL)
        .build();
```

### Risks

Some content is only processed if the associated risk is explicitly allowed. Risks can also be explicitly denied:

```java
RequestOptions options = RequestOptions.builder()
        .allowRisks(Risk.STEG_IMAGE_JPEG)
        .denyRisks(Risk.EXE_MACRO)
        .build();
```

`build()` throws `IllegalArgumentException` if a risk is both allowed and denied.

### Image Quality

Preserve image quality for specific image types:

```java
RequestOptions options = RequestOptions.builder()
        .imagesOptions(new RequestOptions.ImagesOptions(MediaType.IMAGE_JPEG, MediaType.IMAGE_PNG))
        .allowRisks(Risk.STEG_IMAGE_JPEG, Risk.STEG_IMAGE_PNG)
        .build();
```

### Redactions

Archive contents that can't be processed are replaced with a redacted file. Customize the replacement file's extension (including the leading dot), media type, and text:

```java
RequestOptions options = RequestOptions.builder()
        .redactionOptions(new RequestOptions.RedactionOptions(".txt", MediaType.TEXT_PLAIN, "Redacted"))
        .build();
```

With these options, an unsupported `minimal.json` in a ZIP archive is replaced with `minimal.json.txt` containing `Redacted`.

### Format Conversion

Conversion options are built with `RequestOptions.ConversionOptions.builder()`. Use `from(...)` to give the original format of a file to convert, for example to convert a Word 97 document to `.docx`:

```java
RequestOptions options = RequestOptions.builder()
        .conversionOptions(RequestOptions.ConversionOptions.builder()
                .from(MediaType.MSWORD)
                .build())
        .build();

InstantApiRequest request = InstantApiRequest.create(
        fileData,
        MediaType.OCTET_STREAM,
        MediaType.OPENXMLFORMATS_OFFICEDOCUMENT_WORDPROCESSINGML_DOCUMENT,
        options);
```

Conversion methods (`DEFAULT`, `LIBREOFFICE`, `REPORT`, `TEXT_EXTRACTION`) can be enabled, in order of priority, or disabled. For example, to return only the report, including the text extracted from a document, instead of the processed file:

```java
RequestOptions options = RequestOptions.builder()
        .conversionOptions(RequestOptions.ConversionOptions.builder()
                .enableMethods(RequestOptions.ConversionMethod.TEXT_EXTRACTION, RequestOptions.ConversionMethod.REPORT)
                .build())
        .build();
```

The response body is then the JSON report, with the extracted text under `structure[].previous.textExtraction`. Text extraction supports Office, OpenDocument, RTF, and Visio documents, but not PDF.

Media type conversions can also be explicitly enabled or disabled with `enableMediaTypeConversion(from, to)` and `disableMediaTypeConversion(from, to)`. `build()` throws `IllegalArgumentException` if a conversion method is both enabled and disabled.

See the [format conversion documentation](https://cdr.everfox.com/documentation/format-conversion) for supported conversions.

## Supported File Types

The API supports 60+ file formats including:

- Documents: Word, Excel, PowerPoint, PDF, RTF, Visio, ODF formats
- Images: JPEG, PNG, GIF, TIFF, WebP, BMP, HEIC
- Audio: MP3, WAV, M4A
- Video: MP4
- Archives: ZIP
- Email: RFC822, Outlook MSG

## Building

```bash
# Build and run unit tests
mvn clean test

# Package library (JAR with sources and Javadoc)
mvn clean package

# Install to local repository
mvn clean install
```

Integration tests run against the live API when an API key is set, and are skipped otherwise:

```bash
export CDR_INSTANT_API_KEY=your-api-key-here
mvn test -Dtest=IntegrationTest
```

Integration tests use `Region.US_WEST_2` by default. Set `CDR_INSTANT_API_REGION` to a region name (e.g. `eu_west_1`) or `CDR_INSTANT_API_URL` to a base URL to use a different endpoint. If both are set, the region takes precedence.

## License

This software is provided "as is" under the Apache 2.0 License without warranty of any kind. This is a community contribution and is not officially supported by Everfox. For questions, please open a GitHub issue.

## Support

For API documentation and support, visit https://cdr.everfox.com
