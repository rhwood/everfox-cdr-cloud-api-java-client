# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Java client library for the Everfox CDR (Content Disarm and Reconstruction) Instant API. The library provides a simple interface for uploading files to the CDR service, which removes potential security threats and returns sanitized versions.

**Target Platform:** Apache NiFi 2.0.0+ (requires Java 21)

## Build Commands

```bash
# Compile the project
mvn clean compile

# Run tests
mvn test

# Run a specific test class
mvn test -Dtest=InstantApiClientTest

# Run a specific test method
mvn test -Dtest=InstantApiClientTest#testConfigBuilder

# Package the library (creates JAR with sources and javadocs)
mvn clean package

# Install to local Maven repository
mvn clean install

# Skip tests during build
mvn clean package -DskipTests
```

## Architecture

### Core Components

**InstantApiClient** - Main entry point for interacting with the API
- Uses Java 21's `java.net.http.HttpClient` for HTTP requests
- Implements `AutoCloseable` for resource management
- Thread-safe for concurrent use

**InstantApiConfig** - Configuration container with builder pattern
- Supports three regional endpoints (EU West 1/2, US West 2)
- Configurable timeouts for connection and requests
- API key authentication

**InstantApiRequest** - Request model for file uploads
- Encapsulates file data (max 4.5 MB), content type, and accept type
- Optional `RequestOptions` for advanced processing control

**InstantApiResponse** - Response wrapper
- Provides access to processed file data
- Exposes `X-Risks-Taken` and `X-Report` headers
- Includes status code and error handling

**RequestOptions** - Advanced processing configuration
- Risk management (macros, executables, steganography, polymorphic content)
- Reporting format (changed, default, full, none)
- Conversion options (LibreOffice, text extraction)
- Image quality preservation
- Custom redaction text
- Serializes to JSON for `X-Options` header

**InstantApiException** - Exception for API errors
- Wraps HTTP error responses (4xx, 5xx)
- Includes status code and error message

### Design Patterns

- **Builder Pattern**: Used in `InstantApiConfig` for flexible configuration
- **Immutability**: Configuration and request objects are immutable after construction
- **Resource Management**: Client implements `AutoCloseable` for try-with-resources

### API Integration

The library integrates with the Everfox CDR Instant API which:
- Accepts files up to 4.5 MB via POST /upload
- Requires `x-api-key` header for authentication
- Returns sanitized file data or JSON error responses
- Supports 60+ file formats (Office docs, PDFs, images, audio, video, archives, email)
- Provides threat analysis via response headers

### NiFi Integration Considerations

When using this library in Apache NiFi 2.0.0+ custom processors:

1. **Configuration**: Store API keys in NiFi Controller Services or encrypted properties
2. **FlowFile Size**: Ensure files don't exceed 4.5 MB limit (use RouteOnAttribute or similar)
3. **Timeouts**: Adjust request timeouts based on expected file sizes and processing times
4. **Error Handling**: Catch `InstantApiException` and route to failure relationships
5. **Response Headers**: Extract `X-Risks-Taken` and `X-Report` to FlowFile attributes for routing decisions
6. **Regional Selection**: Choose nearest region for optimal latency

### Dependencies

- **Java 21**: Required for NiFi 2.0.0 compatibility
- **Jackson 2.18.2**: JSON serialization for request options and error parsing
- **JUnit 5.11.4**: Unit testing framework
- **Mockito 5.14.2**: Mocking framework for tests

## Testing Strategy

- Unit tests focus on configuration, request building, and option serialization
- Integration tests should use mock HTTP responses to avoid API calls
- For real API testing, use environment variables for API keys:
  ```bash
  export CDR_API_KEY=your-api-key-here
  mvn test -Dtest=IntegrationTest
  ```

## Code Style

- Follow standard Java conventions
- Use builder pattern for complex object construction
- Prefer immutability for data transfer objects
- Document public APIs with Javadoc
- Keep classes focused on single responsibilities
