# Everfox CDR Cloud API Client

Java client library for the [Everfox CDR (Content Disarm and Reconstruction) Cloud API](https://cdr.everfox.com).

> ⚠️ **Pre-release software.** This library is under active development and has not had a stable release. The API may change incompatibly between versions, and no artifacts have been published yet. Don't rely on it in production.

## Overview

This library provides a simple, type-safe interface for uploading files to the Everfox CDR Cloud API service, which removes potential security threats and returns sanitized versions. It's designed for use with Apache NiFi 2.0.0+ but can be used in any Java 21+ application.

## Requirements

- Java 21 or higher
- Maven 3.6 or higher

The JAR is a Java module named `com.everfox.cdr`, and it can also be used from the classpath (as Apache NiFi does).

## Installation

The library hasn't been published to a Maven repository yet. Build and install it locally with `mvn clean install`, then add it to your `pom.xml`:

```xml
<dependency>
    <groupId>com.everfox.cdr</groupId>
    <artifactId>everfox-cdr-cloud-api-client</artifactId>
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
| `InstantApiConfig` | `com.everfox.cdr.instant` | API key, endpoint, and timeouts. Created with `InstantApiConfig.create(...)` or `InstantApiConfig.builder(...)`. |
| `InstantApiClient` | `com.everfox.cdr.instant` | Uploads files. Created with `InstantApiClient.create(config)`; implements `AutoCloseable`. |
| `InstantApiRequest` | `com.everfox.cdr.instant` | File data, content type, accept type, and options. Created with `InstantApiRequest.create(...)`. |
| `InstantApiResponse` | `com.everfox.cdr.instant` | Processed file, status code, headers, risks taken, and report. |
| `InstantApiException` | `com.everfox.cdr.instant` | Error returned by the API (HTTP 4xx/5xx). |
| `RequestOptions` | `com.everfox.cdr.instant` | Processing options sent in the `X-Options` header. Created with `RequestOptions.builder()`. |
| `AsyncApiConfig` | `com.everfox.cdr.async` | API key, endpoint, timeouts, and polling interval and limit. Created with `AsyncApiConfig.create(...)` or `AsyncApiConfig.builder(...)`. |
| `AsyncApiClient` | `com.everfox.cdr.async` | Initiates, uploads, polls, and downloads. Created with `AsyncApiClient.create(config)`; implements `AutoCloseable`. |
| `AsyncApiRequest` | `com.everfox.cdr.async` | Content type and options for initiating a job. Created with `AsyncApiRequest.create(...)`. |
| `AsyncApiStatus` | `com.everfox.cdr.async` | Job state, error, risks taken, and the links for the next step. |
| `AsyncApiResponse` | `com.everfox.cdr.async` | Downloaded file, status code, headers, and the job's final status. |
| `AsyncApiException` | `com.everfox.cdr.async` | Error returned by the API, or the error of a failed job. |
| `RequestOptions` | `com.everfox.cdr.async` | Processing options sent in the initiate request. Created with `RequestOptions.builder()`. |
| `CdrApiConfig` | `com.everfox.cdr` | Supertype of `InstantApiConfig` and `AsyncApiConfig`: API key, base URL, and timeouts. |
| `CdrApiResponse` | `com.everfox.cdr` | Supertype of `InstantApiResponse` and `AsyncApiResponse`: status code, body, and case-insensitive headers. |
| `CdrApiException` | `com.everfox.cdr` | Supertype of `InstantApiException` and `AsyncApiException`. |
| `CdrApiError` | `com.everfox.cdr` | An error reported by an API: code, message, name, type, and sub-type. |
| `ProcessingOptions` | `com.everfox.cdr.options` | Options shared by every API, extended by each `RequestOptions`. The option value types are in this package too. |
| `MediaType` | `com.everfox.cdr` | Media types supported by the API. |
| `Region` | `com.everfox.cdr` | Regions, and the endpoint of each API in them. |
| `Api` | `com.everfox.cdr` | The Everfox CDR Cloud APIs, used to look up a region's endpoint. |
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

// Each API has its own endpoint in every region
URI asyncUrl = region.getBaseUrl(Api.ASYNC); // https://eu-west-2.aws.async.cdr.everfox.com/v1
```

### Custom Endpoint

```java
InstantApiConfig config = InstantApiConfig.builder(apiKey, URI.create("https://us-west-2.aws.instant.cdr.everfox.com/v1"))
        .build();
```

### Timeouts

The connection timeout defaults to 10 seconds and the request timeout to 60 seconds. Override either with the builder, which takes a `Region` or a `URI`:

```java
InstantApiConfig config = InstantApiConfig.builder(apiKey, Region.US_WEST_2)
        .connectTimeout(Duration.ofSeconds(5))
        .requestTimeout(Duration.ofMinutes(2))
        .build();
```

Response headers can be looked up in any case, for example `response.getHeader("X-Risks-Taken")`. Close clients when you're done with them (they're `AutoCloseable`), which releases their HTTP connections.

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

API errors are thrown as `InstantApiException` (or `AsyncApiException` from the Async client), which exposes the details from the API's JSON error response:

```java
try {
    InstantApiResponse response = client.upload(request);
} catch (InstantApiException e) {
    int httpStatus = e.getHttpStatusCode(); // e.g. 400
    int apiCode = e.getApiStatusCode(); // e.g. 1110
    String name = e.getName(); // e.g. "RISK_NOT_ALLOWED"
    String type = e.getType(); // e.g. "BadRequest"
    String message = e.getApiMessage(); // human readable description
    String subType = e.getSubType(); // e.g. "ValidationFailure", or null
}
```

Both exceptions extend `com.everfox.cdr.CdrApiException`, so code that uses either client can catch errors the same way. If the API doesn't return a valid error (for example, a proxy returns HTML), the code is `CdrApiError.UNKNOWN_CODE` (`-1`) and the message, name, and type are `"Unknown error"`, `"Unknown"`, and `"Unknown"`.

## Request Options

Processing options are built with `RequestOptions.builder()` and passed when creating the request. The option types (`ReportFormat`, `ConversionOptions`, `ConversionMethod`, `ImagesOptions`, and `RedactionOptions`) are in `com.everfox.cdr.options` and are shared by both APIs. Repeated `allowRisks` and `denyRisks` calls add to the risks already given, and options are sent in the order given:

```java
RequestOptions options = RequestOptions.builder()
        .reportFormat(ReportFormat.FULL)
        .allowRisks(Risk.POLY_TEXT_JSON, Risk.STRUCTURED_NO_SCHEMA_JSON)
        .build();

InstantApiRequest request = InstantApiRequest.create(fileData, MediaType.JSON, MediaType.JSON, options);
```

See the [Everfox CDR documentation](https://cdr.everfox.com/documentation/risks) for details on each option.

### Report Format

`ReportFormat.CHANGED` (the default), `FULL`, or `NONE` controls the detail of the report returned in the `X-Report` header:

```java
RequestOptions options = RequestOptions.builder()
        .reportFormat(ReportFormat.FULL)
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
        .imagesOptions(new ImagesOptions(MediaType.IMAGE_JPEG, MediaType.IMAGE_PNG))
        .allowRisks(Risk.STEG_IMAGE_JPEG, Risk.STEG_IMAGE_PNG)
        .build();
```

### Redactions

Archive contents that can't be processed are replaced with a redacted file. Customize the replacement file's extension (including the leading dot), media type, and text:

```java
RequestOptions options = RequestOptions.builder()
        .redactionOptions(new RedactionOptions(".txt", MediaType.TEXT_PLAIN, "Redacted"))
        .build();
```

With these options, an unsupported `minimal.json` in a ZIP archive is replaced with `minimal.json.txt` containing `Redacted`.

### Format Conversion

Conversion options are built with `ConversionOptions.builder()`. Use `from(...)` to give the original format of a file to convert, for example to convert a Word 97 document to `.docx`:

```java
RequestOptions options = RequestOptions.builder()
        .conversionOptions(ConversionOptions.builder()
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
        .conversionOptions(ConversionOptions.builder()
                .enableMethods(ConversionMethod.TEXT_EXTRACTION, ConversionMethod.REPORT)
                .build())
        .build();
```

The response body is then the JSON report, with the extracted text under `structure[].previous.textExtraction`. Text extraction supports Office, OpenDocument, RTF, and Visio documents, but not PDF.

Media type conversions can also be explicitly enabled or disabled with `enableMediaTypeConversion(from, to)` and `disableMediaTypeConversion(from, to)`. `build()` throws `IllegalArgumentException` if a conversion method is both enabled and disabled.

See the [format conversion documentation](https://cdr.everfox.com/documentation/format-conversion) for supported conversions.

## Async API

The [Async API](https://cdr.everfox.com/api/async) handles files up to about 1.25 GB and complex files that take longer to process. A job runs in four steps (initiate, upload, poll status, download), and each step uses the links in the status returned by the step before it. `process(...)` runs all four steps:

```java
import com.everfox.cdr.MediaType;
import com.everfox.cdr.Region;
import com.everfox.cdr.async.AsyncApiClient;
import com.everfox.cdr.async.AsyncApiConfig;
import com.everfox.cdr.async.AsyncApiRequest;
import com.everfox.cdr.async.AsyncApiResponse;

AsyncApiConfig config = AsyncApiConfig.create("your-api-key", Region.US_WEST_2);

try (AsyncApiClient client = AsyncApiClient.create(config)) {
    AsyncApiResponse response = client.process(AsyncApiRequest.create(MediaType.PDF), fileData);
    byte[] sanitizedData = response.getBody();
    List<String> risksTaken = response.getRisksTaken();
}
```

`AsyncApiConfig.create(apiKey, region)` uses the region's Async API host, `region.getBaseUrl(Api.ASYNC)`.

For large files, stream from and to disk with `process(request, sourcePath, targetPath)`, which returns the final `AsyncApiStatus`. To control each step yourself, for example to store the status between steps:

```java
AsyncApiStatus status = client.initiate(AsyncApiRequest.create(MediaType.PDF, options));
client.upload(status, Path.of("large.pdf"));
status = client.waitForCompletion(status); // polls until complete
client.download(status, Path.of("large-safe.pdf")); // a result can only be downloaded once
```

`waitForCompletion` polls every second for up to 10 minutes by default, and throws `java.util.concurrent.TimeoutException` if processing doesn't finish in time. Change these limits with the builder, for example `AsyncApiConfig.builder(apiKey, region).pollInterval(Duration.ofMillis(500)).processingTimeout(Duration.ofMinutes(20)).build()`. The request timeout applies to each upload and download, so increase it for large files.

If processing fails, `download` and `process` throw an `AsyncApiException` that holds the job's error (for example `PROCESSING_NOT_RECOGNISED`), with HTTP status 200. Upload, status, and result links expire after a few minutes and contain credentials, so don't log them.

The async `RequestOptions` accept the same options as the Instant API, plus:

```java
RequestOptions options = RequestOptions.builder()
        .largeFileEnvironment(true) // automatically use the large file environment for files over 250 MB
        .snsStatus(topicArn, roleArn) // publish status updates to an AWS SNS topic
        .userData("correlation-id") // returned in every status
        .build();
```

A status received from SNS can be parsed with `AsyncApiStatus.fromJson(message)` and passed to `download`.

## Supported File Types

The API supports 60+ file formats including:

- Documents: Word, Excel, PowerPoint, PDF, RTF, Visio, ODF formats
- Images: JPEG, PNG, GIF, TIFF, WebP, BMP, HEIC
- Audio: MP3, WAV, M4A
- Video: MP4
- Archives: ZIP
- Email: RFC822, Outlook MSG

Some formats can't be sent as their own media type, and must be sent as `MediaType.OCTET_STREAM` with the format as `conversion.from` (see [Format Conversion](#format-conversion)). These are `.doc`, `.xls`, `.xlsb`, `.ppt`, `.rtf`, `.csv`, and the OpenDocument formats. `MediaType.isAcceptedAsContentType()` and `isAcceptedAsConversionSource()` say where each constant can be used:

```java
MediaType.MSWORD.isAcceptedAsContentType();     // false: send as OCTET_STREAM with conversion.from
MediaType.MSWORD.isAcceptedAsConversionSource(); // true
```

## Building

```bash
# Build and run unit tests
mvn clean test

# Package library (JAR with sources and Javadoc)
mvn clean package

# Install to local repository
mvn clean install
```

Integration tests run against the live APIs when an API key is set, and are skipped otherwise. Each API has its own key:

```bash
export CDR_INSTANT_API_KEY=your-instant-key
export CDR_ASYNC_API_KEY=your-async-key
mvn test -Dtest='*IntegrationTest'
```

Integration tests use `Region.US_WEST_2` by default. Set `CDR_INSTANT_API_REGION` (or `CDR_ASYNC_API_REGION`) to a region name (e.g. `eu_west_1`), or `CDR_INSTANT_API_URL` (or `CDR_ASYNC_API_URL`) to a base URL, to use a different endpoint. If both are set, the region takes precedence.

`SpecIntegrationTest` checks that the OpenAPI specs checked in as test resources still match the ones Everfox publishes. It needs network access but no API key, and it's skipped if the specs can't be downloaded. To run only the tests that don't use the network, exclude all the integration tests:

```bash
mvn test -Dtest='!*IntegrationTest'
```

The `MediaTypeIntegrationTest` classes check how each API handles legacy formats such as `.doc`, `.xls`, and `.odt`. Both APIs reject these as a file's own media type. Send them as `MediaType.OCTET_STREAM` with the format as `conversion.from`, and they are returned as `.docx`, `.xlsx`, or `.pptx`.

## License

This software is provided "as is" under the Apache 2.0 License without warranty of any kind. This is a community contribution and is not officially supported by Everfox. For questions, please open a GitHub issue.

## Support

Visit https://cdr.everfox.com for Everfox CDR Cloud API documentation and support. This SDK is not officially supported by Everfox. 
