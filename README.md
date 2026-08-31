# Everfox CDR Instant API Client

Java client library for the Everfox CDR (Content Disarm and Reconstruction) Instant API.

## Overview

This library provides a simple, type-safe interface for uploading files to the Everfox CDR service, which removes potential security threats and returns sanitized versions. It's designed for use with Apache NiFi 2.0.0+ but can be used in any Java 21+ application.

## Requirements

- Java 21 or higher
- Maven 3.6 or higher

## Installation

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
import com.everfox.cdr.instant.*;

// Configure the client
InstantApiConfig config = InstantApiConfig.builder()
    .apiKey("your-api-key")
    .region(InstantApiConfig.Region.US_WEST_2)
    .build();

// Create client
try (InstantApiClient client = new InstantApiClient(config)) {
    // Upload and process a file
    byte[] fileData = Files.readAllBytes(Path.of("document.pdf"));
    InstantApiRequest request = new InstantApiRequest(
        fileData,
        "application/pdf",
        "application/pdf"
    );

    InstantApiResponse response = client.upload(request);

    // Get sanitized file
    byte[] sanitizedData = response.getBody();

    // Check for risks and reports
    String risks = response.getRisksTaken();
    String report = response.getReport();

} catch (InstantApiException e) {
    System.err.println("API error: " + e.getMessage());
}
```

## Advanced Usage

### Custom Processing Options

```java
RequestOptions options = new RequestOptions();

// Configure risk handling
RequestOptions.RiskOptions risks = new RequestOptions.RiskOptions();
risks.setAllowMacros(false);
risks.setAllowExecutables(false);
options.setRisks(risks);

// Request full report
options.setReporting(RequestOptions.ReportFormat.FULL);

// Create request with options
InstantApiRequest request = new InstantApiRequest(
    fileData,
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    "application/pdf",
    options
);
```

### Using with Input Streams

```java
try (InputStream input = new FileInputStream("file.docx");
     InstantApiClient client = new InstantApiClient(config)) {

    InstantApiResponse response = client.upload(
        input,
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/pdf"
    );
}
```

## Supported File Types

The API supports 60+ file formats including:
- Documents: Word, Excel, PowerPoint, PDF, RTF, Visio, ODF formats
- Images: JPEG, PNG, GIF, TIFF, WebP, BMP, HEIC
- Audio: MP3, WAV, M4A
- Video: MP4
- Archives: ZIP
- Email: RFC822, Outlook MSG

## API Regions

Choose the region closest to your deployment:
- `Region.EU_WEST_1` - Europe (Ireland)
- `Region.EU_WEST_2` - Europe (London)
- `Region.US_WEST_2` - US West (Oregon)

## Building

```bash
# Build and run local tests
mvn clean test

# Package library
mvn clean package

# Install to local repository
mvn clean install
```

Integration tests can be run against a live server with an API key
```bash
# Build and run integration tests
export CDR_API_KEY=your-api-key-here
mvn clean test
```

## License

This software is provided "as is" under the Apache 2.0 License without warranty of any kind. This is a community contribution and is not officially supported by Everfox. For questions, please open a GitHub issue.

## Support

For API documentation and support, visit https://cdr.everfox.com
