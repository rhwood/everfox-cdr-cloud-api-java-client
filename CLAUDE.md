# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Java client library for the Everfox CDR (Content Disarm and Reconstruction) Instant API. Callers upload a file; the service strips potential threats and returns a sanitized version.

- **Target platform:** Apache NiFi 2.0.0+, so the library targets **Java 21** (`maven.compiler.release=21`). CI builds on Java 21 and 25.
- **Only runtime dependency:** Jackson 3 (`tools.jackson.core:jackson-databind`). Keep the dependency footprint minimal.

## Build Commands

```bash
mvn clean verify                 # full build as CI runs it: compile, format check, tests, SpotBugs, javadoc
mvn test                         # run unit tests
mvn test -Dtest=RequestOptionsTest            # single test class
mvn test -Dtest=RequestOptionsTest#testName   # single test method
mvn spotless:apply               # fix formatting
mvn clean install                # install to local repo
```

**Formatting is enforced at compile time.** `spotless:check` is bound to the `compile` phase (Palantir Java Format, with Javadoc formatting enabled), so badly formatted code fails `mvn compile`. Run `mvn spotless:apply` after editing Java or the POM.

**SpotBugs runs in `verify`.** Watch for exposed mutable state. Defensive copies (`byte[].clone()`, `Map.copyOf`) are used throughout for this reason.

## Architecture

Two packages:

### `com.everfox.cdr`: shared enums
- `Region`: regional base URLs (`EU_WEST_1`, `EU_WEST_2`, `US_WEST_2`); `fromString` is case-insensitive.
- `MediaType`: MIME types the API accepts. `ALL` is `*/*`.
- `Risk`: hierarchical risk identifiers (for example `exe/macro/ms/word`, `steg/image/png`) used in allow/deny lists.

### `com.everfox.cdr.instant`: Instant API client
- `InstantApiClient`: wraps `java.net.http.HttpClient`. `upload(...)` POSTs to `{baseUrl}/upload` with the `x-api-key`, `Content-Type`, and `Accept` headers, plus `X-Options` when options are set. It always adds `application/json` to `Accept` so error bodies can be parsed. Implements `AutoCloseable`; `close()` is a no-op.
- `InstantApiConfig`: API key, base URL, connect/request timeouts (defaults 10s/60s). Built with static `create(...)` overloads that take either a `Region` or a `URI`.
- `InstantApiRequest`: file bytes, content type, accept type, and optional `RequestOptions`. Built with static `create(...)` overloads that take either `MediaType` or `String`. **The client does not enforce the 4.5 MB API limit.**
- `InstantApiResponse`: built by `fromHttpResponse` (package-private). Throws `InstantApiException` for status >= 400. Exposes the `x-risks-taken` and `x-report` headers.
- `InstantApiException`: parses the API's `{"error": {code, message, name, type}}` body. If parsing fails, it falls back to code `-1` / `"Unknown"`.
- `RequestOptions`: serialized by Jackson into the `X-Options` header JSON. Use `RequestOptions.Builder`. Supports a report format, image quality preservation (`ImagesOptions`), and risk allow/deny lists (a denied risk is removed from the allowed set). **Conversion and redaction options are not yet implemented** (they are stubbed as comments).

### Conventions
- Constructors are private and objects are created through static `create(...)` factory methods. `RequestOptions` is the exception and uses a `Builder`.
- Objects are immutable after construction, and mutable inputs and outputs are defensively copied.
- Validate required arguments with `Objects.requireNonNull(x, "x cannot be null")`.
- Jackson 3 packages: `tools.jackson.*` for core and databind, but annotations stay in `com.fasterxml.jackson.annotation`. Serialization depends on public getters (`getRisks`, `getImages`, `getReport`) with `@JsonInclude(NON_NULL)`. Returning `null` from a getter omits that key.
- Every source file has the Apache 2.0 license header (`Copyright 2026 Everfox`).
- Document all public APIs, and test classes and their fields, with Javadoc.

## Testing

- JUnit 6 (Jupiter) and Mockito; JaCoCo generates coverage reports during `test`.
- Unit tests sit next to each class (`*Test.java`) and focus on validation, edge cases, and JSON output for options.
- `IntegrationTest` calls the real API. It runs only when `CDR_INSTANT_API_KEY` is set, and it also reads `CDR_INSTANT_API_URL` or `CDR_INSTANT_API_REGION`:
  ```bash
  export CDR_INSTANT_API_KEY=your-api-key-here
  export CDR_INSTANT_API_REGION=EU_WEST_2
  mvn test -Dtest=IntegrationTest
  ```
  Never commit API keys.

## NiFi Usage Notes

When this library is used inside NiFi processors: keep API keys in Controller Services or sensitive properties, route FlowFiles over 4.5 MB away before upload, tune request timeouts to file size, route `InstantApiException` to failure, and copy `X-Risks-Taken`/`X-Report` into FlowFile attributes.
