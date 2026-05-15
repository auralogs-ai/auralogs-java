package ai.auralogs.internal;

import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

public final class LogEntry {
  private final LogLevel level;
  private final String message;
  private final String environment;
  private final String timestamp;
  private final @Nullable Map<String, Object> metadata;
  private final @Nullable String stackTrace;
  private final @Nullable String traceId;

  public LogEntry(
      LogLevel level,
      String message,
      String environment,
      String timestamp,
      @Nullable Map<String, Object> metadata,
      @Nullable String stackTrace,
      @Nullable String traceId) {
    this.level = Objects.requireNonNull(level, "level");
    this.message = Objects.requireNonNull(message, "message");
    this.environment = Objects.requireNonNull(environment, "environment");
    this.timestamp = Objects.requireNonNull(timestamp, "timestamp");
    this.metadata = metadata;
    this.stackTrace = stackTrace;
    this.traceId = traceId;
  }

  public LogLevel level() {
    return level;
  }

  public String message() {
    return message;
  }

  public String environment() {
    return environment;
  }

  public String timestamp() {
    return timestamp;
  }

  public @Nullable Map<String, Object> metadata() {
    return metadata;
  }

  public @Nullable String stackTrace() {
    return stackTrace;
  }

  public @Nullable String traceId() {
    return traceId;
  }
}
