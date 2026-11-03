# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Java client library for the Everfox CDR (Content Disarm and Reconstruction) Instant and Async APIs. Callers upload a file; the service strips potential threats and returns a sanitized version.

- **Target platform:** Apache NiFi 2.0.0+, so the library targets **Java 21** (`maven.compiler.release=21`). CI builds on Java 21 and 25.
- **API specs (OpenAPI 3.0.3):** `https://cdr.everfox.com/static/everfox-cdr-{instant,async,s3}-api-OpenAPI.json`. The documentation pages are a JavaScript app and can't be fetched as text, so use these.
- **Only runtime dependency:** Jackson 3 (`tools.jackson.core:jackson-databind`). Keep the dependency footprint minimal.

## Build Commands

```bash
mvn clean verify                 # full build as CI runs it: compile, format check, tests, SpotBugs, javadoc
mvn test                         # run unit tests
mvn test -Dtest=RequestOptionsTest            # single test class
mvn test -Dtest=RequestOptionsTest#testName   # single test method
mvn spotless:apply               # fix formatting
mvn clean install                # install to local repo
mvn site                         # build the project site (README.md becomes the index page) in target/site
```

**Formatting is enforced at compile time.** `spotless:check` is bound to the `compile` phase (Palantir Java Format, with Javadoc formatting enabled), so badly formatted code fails `mvn compile`. Run `mvn spotless:apply` after editing Java or the POM.

**SpotBugs runs in `verify`.** Watch for exposed mutable state. Defensive copies (`byte[].clone()`, `Map.copyOf`) are used throughout for this reason.

## Architecture

Four packages (see REFACTORING.md for the remaining duplication between `instant` and `async`):

### `com.everfox.cdr`: shared enums
- `Region`: `EU_WEST_1`, `EU_WEST_2`, `US_WEST_2`. `getBaseUrl(Api)` builds `https://{region}.aws.{api}.cdr.everfox.com/v1`. `fromString` is case-insensitive.
- `Api`: `INSTANT`, `ASYNC`, `S3` (no S3 client yet); supplies the host subdomain.
- `CdrApiError`: the shared `{"error": {code, message, name, type, subType}}` model. `fromJson(byte[])` is the one parser and holds the `-1` / `"Unknown"` fallback. It takes bytes so Jackson types stay out of the public API.
- `CdrApiConfig`: abstract supertype of each client's config. It holds the API key, base URL, and `Duration` connect/request timeouts (defaults 10 s/60 s), and has a self-typed `Builder` like `ProcessingOptions`. Subclass `builder(...)` factories null-check the key and URL, because the base builder's constructor doesn't validate (SpotBugs `CT_CONSTRUCTOR_THROW`).
- `CdrApiResponse`: abstract supertype of each client's response, with the status code, body, case-insensitive headers (`getHeader`), and `isSuccess()`. Its constructor takes the `HttpResponse` so the body isn't copied.
- `CdrApiException`: abstract supertype of each client's exception, with the shared getters and message format. Its constructor doesn't validate (SpotBugs `CT_CONSTRUCTOR_THROW`), so subclass factories must pass a non-null error.
- `MediaType`: MIME types the API accepts. `ALL` is `*/*`.
- `Risk`: hierarchical risk identifiers (for example `exe/macro/ms/word`, `steg/image/png`) used in allow/deny lists.

### `com.everfox.cdr.options`: shared processing options
- `ProcessingOptions`: abstract base of each API's `RequestOptions`. It holds report, conversion, images, redactions, and risks, and `toJson()`. Its self-typed `Builder<B extends Builder<B>>` lets subclass builders chain shared and API-specific methods in any order. A subclass builder implements `self()` and its `build()` calls `validate()` (which rejects risks that are both allowed and denied). `@JsonPropertyOrder` fixes the shared keys first, then subclass properties.
- Value types: `ReportFormat`, `ConversionMethod`, `ConversionOptions` (builder-only; it rejects methods that are both enabled and disabled), `MediaTypeConversion`, `ImagesOptions`, `RedactionOptions`, and `ReportOptions` (serialization only).
- Everything is builder-only and insertion-ordered (`LinkedHashSet`/`LinkedHashMap`), and repeated `allowRisks`/`denyRisks` calls accumulate.
- Tests: `ProcessingOptionsTest` covers the shared JSON through a test subclass. Each `RequestOptionsTest` covers only builder chaining and API-specific fields.

### `com.everfox.cdr.instant`: Instant API client
- `InstantApiClient`: wraps `java.net.http.HttpClient`. `upload(...)` POSTs to `{baseUrl}/upload` with the `x-api-key`, `Content-Type`, and `Accept` headers, plus `X-Options` when options are set. It always adds `application/json` to `Accept` so error bodies can be parsed. Implements `AutoCloseable`; `close()` closes the `HttpClient`.
- `InstantApiConfig`: a `CdrApiConfig` with no extra settings. Created with `create(apiKey, region)` or `builder(apiKey, region | uri)`.
- `InstantApiRequest`: file bytes, content type, accept type, and optional `RequestOptions`. Built with static `create(...)` overloads that take either `MediaType` or `String`. **The client does not enforce the 4.5 MB API limit.**
- `InstantApiResponse`: a `CdrApiResponse` built by `fromHttpResponse` (package-private), which throws `InstantApiException` for status >= 400. Exposes the `X-Risks-Taken` and `X-Report` headers.
- `InstantApiException`: a `CdrApiException` built from the response body by `CdrApiError.fromJson`.
- `RequestOptions`: a `ProcessingOptions` with no extra options, serialized into the `X-Options` header JSON.

### `com.everfox.cdr.async`: Async API client
- `AsyncApiClient`: four HATEOAS steps, each taking the previous `AsyncApiStatus` because it holds the next link. `initiate` POSTs `{"mime-type", "options"}` JSON to `{baseUrl}/initiate` with `x-api-key`. `upload`, `getStatus`, and `download` use the link's URL, method, and headers as given (dropping headers `HttpClient` restricts). `waitForCompletion` polls with `If-None-Match` and retries 304/404/429. `download` follows the 302 itself without the link headers, because the target is a pre-signed URL. `process(...)` runs all steps. `close()` closes the `HttpClient`.
- `AsyncApiConfig`: a `CdrApiConfig` plus poll interval (default 1 s) and processing timeout (default 10 min). Same factories as `InstantApiConfig`.
- `AsyncApiStatus` / `AsyncApiLink`: parsed from JSON trees (not data-bound). `AsyncApiStatus.fromJson` is public so SNS messages can be parsed.
- `AsyncApiException`: a `CdrApiException`, used both for HTTP errors and for failed jobs (HTTP status 200). The status's `error` is a `CdrApiError`.
- `RequestOptions`: a `ProcessingOptions` plus `costs` (large-file environment) and `status` (SNS and `userData`), serialized as the `options` object of the initiate body.

### Conventions
- Pre-release: when refactoring, remove replaced public methods outright rather than deprecating them. This changes after the first release.
- Constructors are private and objects are created through static `create(...)` factory methods. Configs and options (`InstantApiConfig`, `AsyncApiConfig`, `RequestOptions`, `ConversionOptions`) use a `Builder` instead; the configs also have a `create(apiKey, region)` shortcut.
- Timeouts and intervals are `java.time.Duration`.
- The JAR is the module `com.everfox.cdr` (`src/main/java/module-info.java`). It exports every package except `com.everfox.cdr.internal`, which holds `HttpSupport` (client factory, request builder, `throwIfError`). A new API package must be added to the exports.
- Objects are immutable after construction, and mutable inputs and outputs are defensively copied.
- Validate required arguments with `Objects.requireNonNull(x, "x cannot be null")`.
- Jackson 3 packages: `tools.jackson.*` for core and databind, but annotations stay in `com.fasterxml.jackson.annotation`. Serialization depends on public getters (`getRisks`, `getImages`, `getReport`) with `@JsonInclude(NON_NULL)`. Returning `null` from a getter omits that key.
- Every source file has the Apache 2.0 license header (`Copyright 2026 Everfox`).
- Document all public APIs, and test classes and their fields, with Javadoc.

## Testing

- JUnit 6 (Jupiter) and Mockito; JaCoCo generates coverage reports during `test`.
- Tests run inside the `com.everfox.cdr` module. The client tests use the JDK's `com.sun.net.httpserver`, so the POM passes `--add-modules=jdk.httpserver --add-reads=com.everfox.cdr=jdk.httpserver` to both test compilation and surefire; extend those flags if tests need another module.
- Unit tests sit next to each class (`*Test.java`) and focus on validation, edge cases, and JSON output for options.
- Integration tests call the real APIs: `IntegrationTest` and `MediaTypeIntegrationTest`, in both `instant` and `async`. Each API's tests run only when `CDR_<API>_API_KEY` is set. They use US_WEST_2 unless `CDR_<API>_API_URL` or `CDR_<API>_API_REGION` is set; `com.everfox.cdr.LiveApi` resolves these, and the region takes precedence:
  ```bash
  export CDR_INSTANT_API_KEY=your-instant-key
  export CDR_ASYNC_API_KEY=your-async-key
  mvn test -Dtest='*IntegrationTest'
  ```
  With both keys set, `mvn verify` runs them too, which adds about two and a half minutes (mostly Async jobs) and is billed per MB. Never commit API keys.
- `MediaTypeIntegrationTest` pins the media type behavior confirmed against the live APIs, including where it contradicts the Instant spec (REFACTORING.md section 6). Its formats come from `com.everfox.cdr.ConvertibleFormat`. If it starts failing, check whether Everfox changed the API before changing the client.
- Sample files shared by all tests are in `src/test/resources/com/everfox/cdr/`; load them with `com.everfox.cdr.TestResources.load(name)`. Most were saved from Microsoft Office; `test.odt` was generated with macOS `textutil`.

## NiFi Usage Notes

When this library is used inside NiFi processors: keep API keys in Controller Services or sensitive properties, route FlowFiles over 4.5 MB away before upload, tune request timeouts to file size, route `InstantApiException` to failure, and copy `X-Risks-Taken`/`X-Report` into FlowFile attributes.
