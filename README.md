# auralogs-java

Java SDK for [Auralogs](https://auralogs.ai) — agentic logging and application awareness.

Auralogs acts as an on-call engineer — powered by your choice of model (Claude, OpenAI, or any MCP-compatible LLM) — monitoring your logs and errors, alerting you when something's wrong, and opening fix PRs automatically.

[![Maven Central](https://img.shields.io/maven-central/v/ai.auralogs/auralogs-core.svg?label=maven-central&color=blue)](https://central.sonatype.com/artifact/ai.auralogs/auralogs-core)
[![provenance verified](https://img.shields.io/badge/provenance-verified-2dba4e?logo=sigstore&logoColor=white)](https://central.sonatype.com/artifact/ai.auralogs/auralogs-core)
[![Java](https://img.shields.io/badge/java-11%20%7C%2017%20%7C%2021%20%7C%2025-blue.svg)](https://central.sonatype.com/artifact/ai.auralogs/auralogs-core)
[![license](https://img.shields.io/badge/license-MIT-blue.svg)](./LICENSE)

## Install

### Gradle (Kotlin DSL)

```kotlin
dependencies {
    implementation("ai.auralogs:auralogs-core:1.0.0")
    // Optional: stdlib SLF4J bridge — captures logs from Logback/Log4j2/libraries
    implementation("ai.auralogs:auralogs-slf4j:1.0.0")
}
```

### Maven

```xml
<dependency>
    <groupId>ai.auralogs</groupId>
    <artifactId>auralogs-core</artifactId>
    <version>1.0.0</version>
</dependency>
```

Java 11 or later.

## Quick start

```java
import ai.auralogs.Auralogs;
import ai.auralogs.AuralogsConfig;
import java.util.Map;

Auralogs.init(AuralogsConfig.builder()
    .apiKey(System.getenv("AURALOG_API_KEY"))
    .environment("production")
    .build());

Auralogs.info("user signed in", Map.of("userId", "123"));
Auralogs.error("payment failed", Map.of("orderId", "abc"));
```

## SLF4J bridge (recommended for existing codebases)

Drop `ai.auralogs:auralogs-slf4j` on your classpath and any SLF4J call — including from third-party libraries (Logback, Log4j2, Spring, Hibernate, etc.) — flows to Auralogs automatically:

```java
import ai.auralogs.Auralogs;
import ai.auralogs.AuralogsConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

Auralogs.init(AuralogsConfig.builder().apiKey("...").environment("production").build());

Logger log = LoggerFactory.getLogger(MyClass.class);
log.info("user signed in {}", userId);
log.error("payment failed", exception);
```

The bridge defaults to forwarding `INFO` and above. Calls below the threshold short-circuit inside SLF4J and never reach the Auralogs buffer. Override the threshold via the `auralogs.slf4j.level` system property — accepted values are `TRACE`, `DEBUG`, `INFO`, `WARN`, `ERROR` (case-insensitive):

```bash
java -Dauralogs.slf4j.level=WARN -jar app.jar
```

## Configuration

| Option | Type | Default | Description |
|---|---|---|---|
| `apiKey` | `String` | _required_ | Your Auralogs project API key |
| `environment` | `String` | `"production"` | e.g. `"production"`, `"staging"`, `"dev"` |
| `endpoint` | `String` | `https://ingest.auralogs.ai` | Ingest endpoint override. Must be `https://`; non-`https` URIs are rejected unless `allowInsecureEndpoint(true)` is set. |
| `allowInsecureEndpoint` | `boolean` | `false` | Permit non-`https` endpoint URIs. The SDK rejects plaintext endpoints by default so a misconfigured `endpoint` cannot silently downgrade every POST. Typically only used for local testing. |
| `flushInterval` | `Duration` | `Duration.ofSeconds(5)` | Time between batched flushes (errors flush immediately) |
| `captureErrors` | `boolean` | `true` | Capture uncaught exceptions via `Thread.UncaughtExceptionHandler` |
| `maxQueueSize` | `int` | `1000` | Maximum number of buffered log entries between flushes. When the cap is reached the oldest entries are dropped first, keeping memory bounded if the ingest endpoint is unreachable for an extended period. |
| `traceId` | `String` | _auto-generated_ | Custom trace ID for distributed tracing |
| `globalMetadata` | `Supplier<Map<String, Object>>` _or_ `Map<String, Object>` | _none_ | Fields merged into every emitted log entry (direct API, SLF4J bridge, uncaught-error capture). Per-call metadata wins on key collision; merge is shallow. The supplier runs on every emit — keep it cheap. |

## Attaching session-scoped fields with `globalMetadata`

The canonical recipe — attach `userId` to every log without threading it through every call site:

```java
import ai.auralogs.Auralogs;
import ai.auralogs.AuralogsConfig;
import java.util.Map;

Auralogs.init(AuralogsConfig.builder()
    .apiKey(System.getenv("AURALOG_API_KEY"))
    .environment("production")
    .globalMetadata(() -> Map.of(
        "userId", CurrentUser.id(),         // late-bound: re-read on every emit
        "orgId",  CurrentUser.orgId()
    ))
    .build());

Auralogs.info("checkout started");                          // carries userId + orgId
Auralogs.info("special", Map.of("userId", "impersonated")); // per-call wins on collision
```

The supplier form is the load-bearing API — it re-evaluates on every emission, so you can read mutable host state (current user, request scope, feature-flag snapshot) without restarting the SDK. For static, init-time-known fields, the `Map` overload is a convenience:

```java
.globalMetadata(Map.of("service", "checkout", "region", "us-east-1"))
```

**Performance caveat.** The supplier runs on every log emission, including from the SLF4J bridge and uncaught-error capture. Keep it O(1) — read from a thread-local or a pre-computed snapshot rather than doing real work inside it.

**Failure modes.** If the supplier throws, returns a `CompletionStage` / `CompletableFuture` (the SDK is sync-only and will not await), or returns a value that the SDK's JSON encoder cannot serialize, the entry is still emitted — with just per-call metadata — and a one-time warning is logged via `System.Logger` (`ai.auralogs.internal`, `WARNING`). Subsequent failures from the same SDK instance are silent.

## Attaching exceptions

```java
try {
    risky();
} catch (Exception e) {
    Auralogs.error("task crashed", Map.of("task", "ingest"), e);
}
```

## Graceful shutdown

`Auralogs.init()` registers a JVM shutdown hook that flushes pending logs on process exit. For deterministic flush (short-lived CLI apps, serverless):

```java
Auralogs.shutdown();
```

## Thread and async safety

- **Multi-threaded apps** (Tomcat, Jetty, Spring Boot) are supported out of the box — the transport is `ReentrantLock`-guarded.
- **Background flushing** runs on a named daemon thread (`auralogs-flush`); won't prevent JVM shutdown.
- **Network failures are swallowed** — a single ingest blip never crashes the host app.

## GraalVM native-image

The `auralogs-core` artifact ships [reachability metadata](https://docs.oracle.com/en/graalvm/jdk/21/docs/reference-manual/native-image/metadata/) under `META-INF/native-image/ai.auralogs/auralogs-core/`, so Spring Boot 3 + GraalVM users work with zero extra configuration.

## Verify this package

Every release is published with sigstore provenance attestations via GitHub Actions OIDC. The attestation proves the artifact was built from a specific commit in this repository.

Inspect on [central.sonatype.com/artifact/ai.auralogs/auralogs-core](https://central.sonatype.com/artifact/ai.auralogs/auralogs-core).

## Documentation

Full docs at [docs.auralogs.ai/java-sdk/installation](https://docs.auralogs.ai/java-sdk/installation/).

## Security

Found a vulnerability? See [SECURITY.md](./SECURITY.md) for how to report it.

## License

[MIT](./LICENSE) © James Thomas
