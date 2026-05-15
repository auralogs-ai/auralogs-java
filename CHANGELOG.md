# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2026-05-15

### Changed

- **BREAKING: Renamed Maven group + artifacts**:
  ```diff
  - implementation("ai.auralog:auralog-core:0.2.0")
  - implementation("ai.auralog:auralog-slf4j:0.2.0")
  + implementation("ai.auralogs:auralogs-core:1.0.0")
  + implementation("ai.auralogs:auralogs-slf4j:1.0.0")
  ```
- **BREAKING: Renamed Java package** `ai.auralog` → `ai.auralogs`. Update imports:
  ```diff
  - import ai.auralog.Auralog;
  - import ai.auralog.AuralogConfig;
  + import ai.auralogs.Auralogs;
  + import ai.auralogs.AuralogsConfig;
  ```
- **BREAKING: Renamed `Auralog` class** → `Auralogs`. Update call sites:
  ```diff
  - Auralog.init(AuralogConfig.builder()...);
  - Auralog.info("...");
  + Auralogs.init(AuralogsConfig.builder()...);
  + Auralogs.info("...");
  ```
- Default ingest endpoint updated `https://ingest.auralog.ai` → `https://ingest.auralogs.ai`.
- Repository moved to https://github.com/auralogs-ai/auralogs-java.

### Migration

Replace the Gradle/Maven coordinates + the imports + the class references. Behavior is identical apart from the renamed types.

The previous artifacts `ai.auralog:auralog-core:0.2.0` and `ai.auralog:auralog-slf4j:0.2.0` continue to work but are now deprecated and will not receive updates.

## [0.2.0] - 2026-04-25

### Added
- `AuralogsConfig.Builder.globalMetadata(Supplier<Map<String, Object>>)` — attach session-scoped fields (e.g. `userId`, organisation id, feature-flag snapshot) to every emitted log entry. The supplier is invoked at every emission, never pre-resolved at init, so it late-binds to mutable host state.
- `AuralogsConfig.Builder.globalMetadata(Map<String, Object>)` — convenience overload that wraps a static map as `() -> map`. The supplier form remains the load-bearing API.
- Capture-path entries — both the `auralogs-slf4j` SLF4J bridge and uncaught-exception captures (`Thread.UncaughtExceptionHandler`) now flow through the same merge choke-point as direct API calls and therefore carry `globalMetadata`. Previously these entries shipped without any session attribution.

### Behaviour
- Per-call metadata wins on key collision with `globalMetadata`. Merge is shallow.
- The supplier is treated as failed when it throws, returns a `CompletionStage` / `CompletableFuture` (the SDK does not await async suppliers), or returns a value the SDK's JSON encoder cannot serialize. In every failure case the entry is still emitted, with only the per-call metadata, and a one-time warning is logged via `System.Logger` at `WARNING` (logger name `ai.auralogs.internal`). Subsequent failures from the same SDK instance are silent.
- Backward compatible: absent `globalMetadata`, behaviour is unchanged.

## [0.1.0] - 2026-04-20

### Added
- Initial release.
- `ai.auralogs:auralogs-core` — static `Auralogs` facade, builder config, thread-safe batched HTTP transport, error capture via `Thread.UncaughtExceptionHandler`, automatic JVM shutdown hook.
- `ai.auralogs:auralogs-slf4j` — SLF4J 2.0+ service provider routing all `org.slf4j` calls to the core facade.
- Full JPMS modules (`ai.auralogs.core`, `ai.auralogs.slf4j`).
- GraalVM reachability metadata.
