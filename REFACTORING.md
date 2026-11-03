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

The tests repeated the same pattern, which is now shared; see 7.

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

## 6. Check `MediaType` and `Risk` against the live APIs, not just the specs (done, pending Everfox)

**Problem.** `MediaType` lists types that the live APIs reject, and the published Instant spec can't be used to catch that, because it is wrong. Probes against both production APIs (us-west-2, 2–3 November 2026), using a genuine file for every format, found the following:

- **Both live APIs accept the same 61 primary media types**: the Instant `Content-Type` header and the Async `/initiate` `mime-type`. The Async spec matches that list. The Instant spec doesn't: it lists 11 types the live `/upload` rejects (`1000 CONTENT_TYPE_UNSUPPORTED`), and omits `application/octet-stream`, `application/ms-tnef`, and `text/css`, which it accepts. So, read strictly, the Instant spec has no valid way to send the 11 types.
- **Ten `MediaType` constants are rejected by both APIs as the primary type**, but converted to Office Open XML when sent as `OCTET_STREAM` with the type as `conversion.from`. Both APIs produce the same output:

  | Constants | Output |
  |---|---|
  | `MSWORD`, `RTF`, `TEXT_RTF`, `OASIS_OPENDOCUMENT_TEXT` | .docx |
  | `MS_EXCEL`, `MS_EXCEL_SHEET_BINARY_MACROENABLED_12` (and the `macroEnabled` spelling), `OASIS_OPENDOCUMENT_SPREADSHEET`, `TEXT_CSV` | .xlsx |
  | `MS_POWERPOINT`, `OASIS_OPENDOCUMENT_PRESENTATION` | .pptx |

  So `InstantApiRequest.create(data, MediaType.MSWORD, ...)` fails against the live API, although the Instant spec says it is valid. Office Open XML files sent as their own type come back in the same format.
- **`TEXT_PLAIN` is accepted by neither API as a file's type**, either as the primary type or as `conversion.from`. It is still valid as the media type of a redaction replacement (`RedactionOptions`), which the Instant integration tests use. `ALL` (`*/*`) is only meaningful as an Accept type.
- **The Instant API checks the `Accept` header before most other validation.**
  - `OCTET_STREAM` without `conversion.from` returns `1020 ACCEPT_HEADER_INVALID`, unless the `Accept` header includes `application/octet-stream` or `*/*`. With one of those, it returns `1090 CONVERSION_UNSUPPORTED`, as Async does.
  - The API matches the `Content-Type` case-insensitively, but compares the `Accept` header case-sensitively. A lower-case `.xlsb` `Content-Type` is read as `...macroEnabled.12`, and an `Accept` with the same lower-case type is then rejected with `1020`. `MediaType.MS_EXCEL_SHEET_BINARY_MACROENABLED_12` is lower case, so it can't be used as an Instant Accept type.
- **Every type the live APIs accept has a `MediaType` constant.**
- **`Risk` matches both specs' risk enums exactly.**
- **Converted output isn't byte-for-byte reproducible.** Converting the same file twice gives output that differs by a few bytes, on either API. For example, the same `.ppt` gave 7,494 and then 7,491 bytes from Instant. So tests should check the output's media type and format, not its size or checksum.
- **A valid `.ppt` can fail conversion on both APIs.** A copy of `test.ppt` re-saved in PowerPoint for Mac failed with `3060 PROCESSING_CONVERSION_FAILED` on both APIs, although the original save of the same presentation converts. That copy isn't in the repository. It's worth sending to Everfox if `.ppt` conversion failures turn up in practice.

The probes also found differences between the two APIs, which have been reported to Everfox. Client code shouldn't depend on them until Everfox answers:

- The same unsupported type returns `1000 CONTENT_TYPE_UNSUPPORTED` from Instant but `6010 APIGATEWAY_BAD_REQUEST_BODY` (subType `ValidationFailure`) from Async.
- Instant accepts `conversion.from: text/css`; Async rejects it. Neither spec lists it.
- `3020 PROCESSING_NOT_RECOGNISED` has type `BadRequest` from Instant, but type `PROCESSING` and subType `Invalid data` from Async.

**Done.**

- **Live behavior tests:** `instant.MediaTypeIntegrationTest` and `async.MediaTypeIntegrationTest` cover every confirmed behavior above against the live APIs. They are driven by the shared `com.everfox.cdr.ConvertibleFormat` table: each format's media type, sample file, and Office Open XML output. They run whenever the API keys are set, and they assert the current error codes. If Everfox changes an API, they fail and point to the change.
- **Live allowed lists:** the same tests parse the list of allowed types from each API's rejection message, and compare it with the checked-in Async spec. That covers the Instant `Content-Type`, the Async `mime-type`, and the Async `conversion.from`. These depend on the wording of the error messages (`LiveApi.mediaTypesAfter`), so a change in wording fails them too.
- **Checked-in specs:** the Instant and Async specs are in `src/test/resources/com/everfox/cdr/openapi/`, as published, and are read through `com.everfox.cdr.ApiSpec`.
- **`SpecConformanceTest`** (unit tests, no API keys needed):
  - `Risk` matches both specs' `allow` and `deny` lists exactly.
  - Every `MediaType` constant's flags agree with the specs.
  - Every type the Async spec accepts has a constant.
  - Only `ALL` and `TEXT_PLAIN` are accepted nowhere.
  - The Instant spec's `Content-Type` list still differs from the Async spec in exactly the known way: 11 extra types, and 3 missing. That test fails when Everfox changes the Instant spec.
- **`MediaType` documents its support.** `isAcceptedAsContentType()` and `isAcceptedAsConversionSource()` record where each constant can be used, following the Async spec. Javadoc on the ten conversion-only constants, `OCTET_STREAM`, `MS_TNEF`, `TEXT_CSS`, `TEXT_PLAIN`, and `ALL` says how to use them. `MS_EXCEL_SHEET_BINARY_MACROENABLED_12` notes that the Instant `Accept` header needs the `macroEnabled` capitalization. The clients don't check the flags before sending, because the APIs validate anyway and requests can also be made with media type strings.

**Remaining.**

- **Error codes:** while Everfox hasn't confirmed whether they're meant to differ, branch on `CdrApiException.getName()` and `getType()` only per API, not across the two. This is guidance for client code, not a change to make.
- **When Everfox replies:** update the `MediaType` flags, `SpecConformanceTest`, the `MediaTypeIntegrationTest` assertions, and this section to match.

## 7. Share test support (done)

**Done.**

- The sample files are in `src/test/resources/com/everfox/cdr/`, and both APIs' tests load them with `com.everfox.cdr.TestResources.load(name)`.
- The sample files have been checked for personal metadata (author names and absolute file paths), and the name fields scrubbed where needed. Check any new Office file the same way before committing it, and search the decompressed ZIP entries as well as the raw bytes. Office for Mac can't always remove the author on save. Scrubbing `test.doc`, `test.ppt`, `test.pptx`, and `test.odp` didn't change any API result.
- `com.everfox.cdr.LiveApi` resolves each API's key (`apiKey(Api)`) and endpoint (`baseUrl(Api)`) from `CDR_<API>_API_KEY`, `_URL`, and `_REGION`. Both `IntegrationTest` classes and both `MediaTypeIntegrationTest` classes use it.
- The helpers are in `com.everfox.cdr`, not a separate `testing` package, so they sit next to the shared main classes.

- `com.everfox.cdr.MockHttp.response(...)` builds the mock `HttpResponse`s used by the response, status, and `HttpSupport` tests.
- `com.everfox.cdr.FakeServer` replaced both client tests' `HttpServer` fixtures. It routes by the longest matching path prefix, plays queued replies in order (repeating the last), and records every request.

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
5. ~~**Spec checks (6)** and **test support (7)**~~: done. What's left in 6 depends on Everfox's answer.
