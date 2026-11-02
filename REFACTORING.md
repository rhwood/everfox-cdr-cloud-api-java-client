# Refactoring Opportunities

This document lists ways to reduce the duplicated effort of maintaining and using the `com.everfox.cdr`, `com.everfox.cdr.instant`, and `com.everfox.cdr.async` packages. It reflects the code as of the addition of `com.everfox.cdr.async`, which was written alongside `instant` without changing it, so the duplication is easy to see side by side.

The library is pre-release, so breaking changes are cheaper now than they will be after the first release. Most of the proposals below are breaking.

## Where the duplication is

Line counts include Javadoc and license headers. "Shared" is the number of lines that are identical (ignoring indentation) after replacing `Instant`/`Async` in class names.

| Instant | Async | Lines (instant / async) | Shared | Nature of the overlap |
|---|---|---|---|---|
| `RequestOptions` | `RequestOptions` | 759 / 865 | 661 | Same JSON model (now shared; see 1) |
| `InstantApiConfig` | `AsyncApiConfig` | 141 / 265 | 129 | Same fields; async adds polling |
| `InstantApiException` | `AsyncApiException` | 142 / 153 | 91 | Same error JSON (now shared; see 3) |
| `InstantApiResponse` | `AsyncApiResponse` | 130 / 127 | 95 | Status code, body, headers, `isSuccess()` |
| `InstantApiClient` | `AsyncApiClient` | 132 / 469 | 94 | `HttpClient` setup, factory, `close()` |

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

## 4. Share configuration, and stop multiplying `create` overloads

**Problem.** Each config class repeats the API key, base URL, connect timeout, request timeout, the `Region`-or-`URI` overload pair, and null checks. Every new setting doubles the overloads: `InstantApiConfig` has 4 `create` methods and `AsyncApiConfig` has 6. A seventh setting, such as a proxy or an executor, would make that 8. The timeouts are `int` seconds, which can't express sub-second polling and push the client tests to sleep for whole seconds. The async client tests take about 11 seconds for that reason.

**Proposal.**

- Extract the shared settings into an immutable `ConnectionSettings` in `com.everfox.cdr`: API key, base URL, and timeouts. Both configs either hold one or extend a small base class.
- Replace the overload matrix with a builder that matches the `RequestOptions` style. Keep `create(apiKey, region)` as the one-line shortcut:

  ```java
  AsyncApiConfig config = AsyncApiConfig.builder(apiKey, Region.US_WEST_2)
          .requestTimeout(Duration.ofMinutes(10))
          .pollInterval(Duration.ofMillis(500))
          .build();
  ```

- Use `java.time.Duration` for all timeouts and intervals.

## 5. Share the HTTP plumbing

**Problem.** Each client builds its own `HttpClient`, converts headers into a `Map`, and turns status >= 400 into an exception. The two have diverged:

| Behavior | Instant | Async |
|---|---|---|
| `close()` | No-op | Closes the `HttpClient` (`AutoCloseable` since Java 21) |
| `getHeaders()` keys | Case-sensitive copy, so `get("X-Risks-Taken")` returns `null` and only lowercase keys work | Case-insensitive |
| Redirects | `HttpClient` default (never followed) | Followed explicitly when downloading, without the link's credentials |

**Proposal.** Add an internal helper package such as `com.everfox.cdr.internal`:

- `HttpSupport.newClient(ConnectionSettings)`.
- `HttpSupport.headers(HttpResponse<?>)`, which returns a case-insensitive, unmodifiable map.
- `HttpSupport.throwIfError(HttpResponse<byte[]>, ExceptionFactory)`.
- A shared `ApiResponse` base for `InstantApiResponse` and `AsyncApiResponse` (status code, defensively copied body, headers, `isSuccess()`).

Settle each divergence once: `close()` closes the client, and header lookups are case-insensitive.

An internal package is public in a plain JAR, so add a `module-info.java` that exports only `com.everfox.cdr`, `com.everfox.cdr.instant`, and `com.everfox.cdr.async`. NiFi loads extensions from the classpath, which ignores the module boundary, so also name the package `internal` and state in its `package-info` that it isn't API.

## 6. Check `MediaType` and `Risk` against the published specs

**Problem.** `MediaType` is the union of the types the APIs accept, but each API accepts a different subset. For example, Async `/initiate` rejects `application/msword`, `application/vnd.ms-excel`, `application/rtf`, `text/csv`, `text/plain`, and the OpenDocument types. Those must be sent as `application/octet-stream` with `conversion.from`. `ALL` (`*/*`) is only meaningful as an Accept type. Nothing tells users which constants work where, and nothing catches drift when Everfox changes an API.

**Proposal.**

- Check in the OpenAPI documents under `src/test/resources`. Everfox doesn't publish them at a standalone URL. They're embedded in the documentation site's JavaScript bundle (`https://cdr.everfox.com/static/js/main.*.js`, as a `JSON.parse('[{"apiName":"instant",...},{"apiName":"async",...},{"apiName":"s3",...}]')` literal), which is how the Async client was written.
- Add a unit test that asserts `Risk` matches the specs' risk enums exactly (today it does), and reports `MediaType` constants that no API accepts.
- Either document each constant's support in Javadoc, or add `boolean isAcceptedBy(Api api)`, which the `/initiate` and `/upload` paths could then check before sending.

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
4. **Configuration (4)** and **HTTP plumbing (5)**: these depend on the shared settings type, so do them together, and add `module-info.java` with them.
5. **Spec checks (6)** and **test support (7)**: these can be done at any time, and they get cheaper once steps 1–5 have removed the per-package copies.
