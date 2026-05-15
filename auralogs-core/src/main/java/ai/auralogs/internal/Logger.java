package ai.auralogs.internal;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

public final class Logger {
  private static final System.Logger INTERNAL_LOG = System.getLogger("ai.auralogs.internal");

  private final String environment;
  private final Consumer<LogEntry> sink;
  private final @Nullable Supplier<Map<String, Object>> globalMetadata;

  // Each warn-once flag covers a distinct failure mode so a first occurrence on one path is never
  // silently swallowed because the other path already tripped its flag.
  private final AtomicBoolean globalSupplierWarned = new AtomicBoolean(false);
  private final AtomicBoolean perCallCycleWarned = new AtomicBoolean(false);
  private volatile String traceId;

  /** Backward-compatible constructor without {@code globalMetadata}. Used by existing tests. */
  public Logger(String environment, Consumer<LogEntry> sink, @Nullable String traceId) {
    this(environment, sink, traceId, null);
  }

  public Logger(
      String environment,
      Consumer<LogEntry> sink,
      @Nullable String traceId,
      @Nullable Supplier<Map<String, Object>> globalMetadata) {
    this.environment = environment;
    this.sink = sink;
    this.traceId = traceId != null ? traceId : UUID.randomUUID().toString();
    this.globalMetadata = globalMetadata;
  }

  public String getTraceId() {
    return traceId;
  }

  public void setTraceId(String id) {
    this.traceId = id;
  }

  public void debug(String message, @Nullable Map<String, Object> metadata) {
    emit(LogLevel.DEBUG, message, metadata, null);
  }

  public void info(String message, @Nullable Map<String, Object> metadata) {
    emit(LogLevel.INFO, message, metadata, null);
  }

  public void warn(String message, @Nullable Map<String, Object> metadata) {
    emit(LogLevel.WARN, message, metadata, null);
  }

  public void error(String message, @Nullable Map<String, Object> metadata, @Nullable Throwable t) {
    emit(LogLevel.ERROR, message, metadata, t);
  }

  public void fatal(String message, @Nullable Map<String, Object> metadata, @Nullable Throwable t) {
    emit(LogLevel.FATAL, message, metadata, t);
  }

  private void emit(
      LogLevel level,
      String message,
      @Nullable Map<String, Object> metadata,
      @Nullable Throwable throwable) {
    String stack = null;
    if (throwable != null) {
      StringWriter writer = new StringWriter();
      throwable.printStackTrace(new PrintWriter(writer));
      stack = writer.toString();
    }

    Map<String, Object> mergedMetadata = mergeMetadata(metadata);

    String entryTraceId = this.traceId;
    if (mergedMetadata != null && mergedMetadata.containsKey("traceId")) {
      entryTraceId = String.valueOf(mergedMetadata.get("traceId"));
      mergedMetadata = new LinkedHashMap<>(mergedMetadata);
      mergedMetadata.remove("traceId");
      if (mergedMetadata.isEmpty()) mergedMetadata = null;
    }

    sink.accept(
        new LogEntry(
            level,
            message,
            environment,
            Instant.now().toString(),
            mergedMetadata,
            stack,
            entryTraceId));
  }

  /**
   * Choke-point: every emitted entry routes through this merge. Resolves the configured
   * globalMetadata supplier, applies serialization defense, and shallow-merges per-call metadata
   * over global keys.
   *
   * <p>Returns {@code null} when both sides are absent so {@link Transport} omits {@code metadata}
   * from the wire payload.
   */
  private @Nullable Map<String, Object> mergeMetadata(@Nullable Map<String, Object> perCall) {
    Map<String, Object> resolved = resolveGlobalMetadata();
    if (resolved == null || resolved.isEmpty()) {
      // Defend the encode path against cycles / non-serializable values in per-call metadata.
      // assertSerializable's IdentityHashMap walk catches circular references that would otherwise
      // StackOverflowError inside Json.writeMap / writeArray on the flush thread, silently losing
      // all subsequent telemetry.
      if (perCall == null || perCall.isEmpty()) return null;
      try {
        Json.assertSerializable(perCall);
      } catch (Exception serializationFailure) {
        warnPerCallCycleOnce(
            "per-call metadata is not serializable by the Auralogs JSON encoder; dropping metadata"
                + " for this entry",
            serializationFailure);
        return null;
      }
      return perCall;
    }

    LinkedHashMap<String, Object> merged = new LinkedHashMap<>(resolved);
    if (perCall != null) merged.putAll(perCall);

    // Serialization defense: if the merged metadata isn't shippable, drop globalMetadata for this
    // entry and warn once. The merged walk also covers per-call cycles since the per-call map is
    // reachable from `merged`.
    try {
      Json.assertSerializable(merged);
    } catch (Exception serializationFailure) {
      warnGlobalSupplierOnce(
          "globalMetadata produced a value the Auralogs JSON encoder cannot serialize; dropping"
              + " globalMetadata for this entry",
          serializationFailure);
      // Fall back to per-call metadata only — but re-check it independently in case the cycle is
      // on the per-call side.
      if (perCall == null || perCall.isEmpty()) return null;
      try {
        Json.assertSerializable(perCall);
      } catch (Exception perCallFailure) {
        warnPerCallCycleOnce(
            "per-call metadata is not serializable by the Auralogs JSON encoder; dropping metadata"
                + " for this entry",
            perCallFailure);
        return null;
      }
      return perCall;
    }

    return merged;
  }

  /**
   * Invoke the supplier with full failure containment. Returns {@code null} when the supplier is
   * absent, throws, or returns an async/promise-like value.
   */
  private @Nullable Map<String, Object> resolveGlobalMetadata() {
    Supplier<Map<String, Object>> supplier = this.globalMetadata;
    if (supplier == null) return null;

    Map<String, Object> resolved;
    try {
      Object raw = supplier.get();
      if (raw == null) return null;
      if (raw instanceof CompletionStage) {
        warnGlobalSupplierOnce(
            "globalMetadata supplier returned a CompletionStage / CompletableFuture; the SDK"
                + " requires synchronous suppliers and will not await. Cache async state on the sync"
                + " side (e.g. via a thread-local).",
            null);
        return null;
      }
      if (!(raw instanceof Map)) {
        warnGlobalSupplierOnce(
            "globalMetadata supplier returned a non-Map value of type "
                + raw.getClass().getName()
                + "; expected Map<String, Object>",
            null);
        return null;
      }
      @SuppressWarnings("unchecked")
      Map<String, Object> casted = (Map<String, Object>) raw;
      resolved = casted;
    } catch (Throwable supplierFailure) {
      warnGlobalSupplierOnce(
          "globalMetadata supplier threw; emitting entry without globalMetadata", supplierFailure);
      return null;
    }
    return resolved;
  }

  private void warnGlobalSupplierOnce(String message, @Nullable Throwable cause) {
    warnOnce(globalSupplierWarned, message, cause);
  }

  private void warnPerCallCycleOnce(String message, @Nullable Throwable cause) {
    warnOnce(perCallCycleWarned, message, cause);
  }

  private static void warnOnce(AtomicBoolean flag, String message, @Nullable Throwable cause) {
    if (flag.compareAndSet(false, true)) {
      if (cause != null) {
        INTERNAL_LOG.log(System.Logger.Level.WARNING, message, cause);
      } else {
        INTERNAL_LOG.log(System.Logger.Level.WARNING, message);
      }
    }
  }

  // Visible for tests in the same package.
  boolean hasWarnedAboutGlobalMetadata() {
    return globalSupplierWarned.get();
  }

  // Visible for tests in the same package.
  boolean hasWarnedAboutPerCallCycle() {
    return perCallCycleWarned.get();
  }
}
