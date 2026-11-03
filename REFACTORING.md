# Refactoring Opportunities

This document lists ways to reduce the duplicated effort of maintaining and using the `com.everfox.cdr`, `com.everfox.cdr.instant`, and `com.everfox.cdr.async` packages. It reflects the code as of the addition of `com.everfox.cdr.async`, which was written alongside `instant` without changing it, so the duplication is easy to see side by side.

The library is pre-release, so breaking changes are cheaper now than they will be after the first release. Most of the proposals below are breaking.

## Where the duplication is

Line counts include Javadoc and license headers. "Shared" is the number of lines that are identical (ignoring indentation) after replacing `Instant`/`Async` in class names.

| Instant | Async | Lines (instant / async) | Shared | Nature of the overlap |
|---|---|---|---|---|
| `RequestOptions` | `RequestOptions` | 759 / 865 | 661 | Same JSON model (now shared; see 1) |
| `InstantApiConfig` | `AsyncApiConfig` | 141 / 265 | 129 | Same fields (now shared; see 4) |
| `InstantApiException` | `AsyncApiException` | 142 / 153 | 91 | Same error JSON (now shared; see 3) |
| `InstantApiResponse` | `AsyncApiResponse` | 130 / 127 | 95 | Status code, body, headers, `isSuccess()` (now shared; see 5) |
| `InstantApiClient` | `AsyncApiClient` | 132 / 469 | 94 | `HttpClient` setup, factory, `close()` (now shared; see 5) |

The tests repeat the same pattern: two `IntegrationTest` classes with the same environment variable handling, two local `HttpServer` fixtures, and two sets of mocked `HttpResponse` helpers.

The Everfox S3 API (`/s3`, `/validate`) takes the same `options` object, plus `emails` and `costs.environment.large.force`. A third client would make every item below a three-way copy.

## 1. Share one options model (done)

The shared options now live in `com.everfox.cdr.options`: the abstract `ProcessingOptions`, its self-typed `Builder`, and the value types `ReportFormat`, `ConversionMethod`, `ConversionOptions`, `MediaTypeConversion`, `ImagesOptions`, `RedactionOptions`, and `ReportOptions`. `instant.RequestOptions` adds nothing to it; `async.RequestOptions` adds `costs` and `status`. One `ProcessingOptionsTest` covers the shared JSON.

The divergences were settled on the Async behavior, which changes the Instant API:

- Repeated `allowRisks(...)` / `denyRisks(...)` calls now add to the risks already given instead of replacing them.
- Risks, image formats, and top-level keys are serialized in a fixed or insertion order, instead of an unpredictable one.
- The public `RequestOptions` and `ConversionOptions` constructors were removed; use the builders. The constructors' silent "denied wins" handling of a risk that is both allowed and denied went with them, so `build()` always rejects it.
- The nested types moved, so `RequestOptions.ReportFormat` is now `com.everfox.cdr.options.ReportFormat`, and the same applies to the other nested types.

## 2. Make `Region` know about every API (done)

`Region.getBaseUrl(Api)` now builds each API's endpoint (`https://{region}.aws.{api}.cdr.everfox.com/v1`), with a new `Api` enum (`INSTANT`, `ASYNC`, `S3`). Both config classes use it, and the `AsyncApiConfig.getBaseUrl(Region)` stopgap has been removed. The no-argument `Region.getBaseUrl()`, which always returned the Instant URL, has been removed.

## 3. Share the error model and give errors a common supertype (done)

`AsyncApiError` moved to `com.everfox.cdr` as `CdrApiError`, whose `fromJson(byte[])` is the only parser of the `{"error": {...}}` body and the only place that has the `-1` / `"Unknown"` fallback. `InstantApiException` and `AsyncApiException` now extend the abstract `com.everfox.cdr.CdrApiException`, which has the shared getters, including `getSubType()` and `getError()`, and the message format. Instant errors now include `subType`.

One behavior changed for Instant: an error with a code but no message, name, or type now reports `"Unknown error"` / `"Unknown"` for the missing fields instead of `null`, as Async already did.

## 4. Share configuration, and stop multiplying `create` overloads (done)

The shared settings live in the abstract `com.everfox.cdr.CdrApiConfig`: API key, base URL, and `Duration` connect and request timeouts. It has a self-typed builder, like `ProcessingOptions`, rather than the separate `ConnectionSettings` holder proposed here, so the getters stay on the config. `InstantApiConfig` adds nothing to it; `AsyncApiConfig` adds `Duration` poll interval and processing timeout. Each config has `create(apiKey, region)` and `builder(apiKey, region | uri)`, so a new setting is one builder method instead of a doubling of overloads.

All the `*Seconds` getters, `DEFAULT_*_SECONDS` constants, and multi-argument `create` overloads were removed. Sub-second polling brought the async client tests down from about 11 seconds to under 1.

## 5. Share the HTTP plumbing (done)

- `com.everfox.cdr.internal.HttpSupport` has `newClient(config)` (never follows redirects), `newRequest(uri, config)` (applies the request timeout), and `throwIfError(response, ErrorFactory)`. Both clients and `AsyncApiStatus` use them.
- The shared response base is `com.everfox.cdr.CdrApiResponse`, not an internal class: module users can only call inherited methods if the superclass is in an exported package. It takes the `HttpResponse` directly, so a large body isn't copied on construction, and it normalizes headers case-insensitively. That made the proposed `HttpSupport.headers()` unnecessary. `getHeader(name)` was added.
- `InstantApiClient.close()` now closes its `HttpClient`, and Instant header lookups are case-insensitive.
- `src/main/java/module-info.java` declares the module `com.everfox.cdr`. It exports `com.everfox.cdr`, `.instant`, `.async`, and `.options` (not `.internal`), and `requires transitive java.net.http` because `CdrApiResponse` takes an `HttpResponse`. Tests run inside the module, so the POM adds `jdk.httpserver` to test compilation and surefire.

## 6. Check `MediaType` and `Risk` against the live APIs, not just the specs

**Problem.** `MediaType` lists types that the live APIs reject, and the published Instant spec can't be used to catch that, because it is wrong. Probes against both production APIs (us-west-2, 2 November 2026) found the following:

- **Both live APIs accept the same 61 primary media types**: the Instant `Content-Type` header and the Async `/initiate` `mime-type`. The Async spec matches that list. The Instant spec doesn't: it lists 11 types the live `/upload` rejects (`1000 CONTENT_TYPE_UNSUPPORTED`), and omits `application/octet-stream`, `application/ms-tnef`, and `text/css`, which it accepts.
- **Ten `MediaType` constants are rejected by both APIs as the primary type**, but accepted as `conversion.from` with `application/octet-stream` as the primary type: `MSWORD`, `MS_EXCEL`, `MS_EXCEL_SHEET_BINARY_MACROENABLED_12`, `MS_POWERPOINT`, `RTF`, `TEXT_RTF`, `TEXT_CSV`, and the three `OASIS_OPENDOCUMENT_*` types. Through that route, both APIs return the same output: .doc, .rtf, and .odt become .docx, and .csv becomes .xlsx. So `InstantApiRequest.create(data, MediaType.MSWORD, ...)` fails against the live API, although the Instant spec says it is valid.
- **`TEXT_PLAIN` is accepted by neither API as a file's type**, either as the primary type or as `conversion.from`. It is still valid as the media type of a redaction replacement (`RedactionOptions`), which the Instant integration tests use. `ALL` (`*/*`) is only meaningful as an Accept type.
- **Every type the live APIs accept has a `MediaType` constant.**
- **`Risk` matches both specs' risk enums exactly.**

The probes also found differences between the two APIs that have been reported to Everfox, and that client code shouldn't depend on until Everfox answers:

- The same unsupported type returns `1000 CONTENT_TYPE_UNSUPPORTED` from Instant but `6010 APIGATEWAY_BAD_REQUEST_BODY` (subType `ValidationFailure`) from Async.
- Instant accepts `conversion.from: text/css`; Async rejects it. Neither spec lists it.
- `3020 PROCESSING_NOT_RECOGNISED` has type `BadRequest` from Instant, but type `PROCESSING` and subType `Invalid data` from Async.

Nothing in the build catches drift when Everfox changes an API, and nothing tells users which constants work where.

**Proposal.**

- **Check in the OpenAPI documents** under `src/test/resources`. Everfox publishes them at `https://cdr.everfox.com/static/everfox-cdr-{instant,async,s3}-api-OpenAPI.json`. Treat the Async spec as the reference for media types until Everfox fixes the Instant one.
- **Add unit tests against the checked-in specs.**
  - Assert that `Risk` matches the specs' risk enums exactly.
  - Report `MediaType` constants that no API accepts as a primary type or as `conversion.from`, using the Async enums.
- **Add an opt-in integration test**, gated on the API keys like the existing ones, that sends an unsupported type to each API. It should parse the list of allowed types from the error message and compare it with the checked-in spec. This is how the Instant spec error was found, and it catches the same kind of drift.
- **Document each constant's support.**
  - Javadoc each of the ten conversion-only constants with "use as `conversion.from` with `OCTET_STREAM`".
  - Javadoc `TEXT_PLAIN` and `ALL` with where they are valid: redaction replacements and the Accept header, respectively.
  - Alternatively, add `boolean isAcceptedAsContentType()` and `boolean isAcceptedAsConversionSource()`, which the `/upload` and `/initiate` paths could check before sending.
- **Error codes:** while Everfox hasn't confirmed whether they're meant to differ, branch on `CdrApiException.getName()` and `getType()` only per API, not across the two.

## 7. Share test support

**Problem.** The two `IntegrationTest` classes repeat the logic that resolves the API key, URL, and region from environment variables, along with `loadResource`. The two client tests each build a local `HttpServer` fixture, and the response tests each mock `HttpResponse` the same way.

**Proposal.** Add a test-only package `com.everfox.cdr.testing` with:

- `LiveApi.config(String prefix)`, which reads `CDR_<PREFIX>_API_KEY`, `_URL`, and `_REGION`.
- `TestResources.load(name)`.
- `MockHttp.response(status, body, headers)`.
- A small `FakeServer` that records requests and replies from a queue.

Move the shared test files (`test.pdf` and the others) to a neutral location such as `src/test/resources/com/everfox/cdr/`. The async integration test currently loads them from the `instant` resource folder.

## 8. Rename the artifact (done)

The artifact is now `everfox-cdr-cloud-api-client`, named "Everfox CDR Cloud API Client". Anyone who installed the old `everfox-cdr-instant-client` snapshot locally needs to update their dependency.

## Not recommended: merging the clients

Merging the clients into a single class isn't worth it. The Instant API is one synchronous call; the Async API is a four-step workflow driven by links in the job status. One class would carry methods that make no sense for half of its users. The sharing belongs below the clients (options, errors, config, HTTP plumbing), and the client classes should stay separate.

## Suggested order

Each step stands on its own and keeps `mvn clean verify` green:

1. ~~**Region endpoints (2)** and **artifact rename (8)**~~: done.
2. ~~**Error model (3)**~~: done.
3. ~~**Options model (1)**~~: done.
4. ~~**Configuration (4)** and **HTTP plumbing (5)**~~: done, including `module-info.java`.
5. **Spec checks (6)** and **test support (7)**: these can be done at any time. Do the `MediaType` fixes in 6 first, because ten constants fail against both live APIs today.
