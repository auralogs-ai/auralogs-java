package ai.auralog.slf4j;

import ai.auralog.Auralog;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Marker;
import org.slf4j.event.Level;
import org.slf4j.helpers.AbstractLogger;
import org.slf4j.helpers.MessageFormatter;

/**
 * SLF4J {@code Logger} implementation that forwards records to the Auralog core facade. Returned by
 * {@link AuralogSlf4jLoggerFactory}; not constructed directly by users.
 *
 * <p>A configurable level threshold (default {@link Level#INFO}) controls which calls are
 * forwarded. {@code isXxxEnabled()} returns {@code true} only when the requested level is at or
 * above the threshold; SLF4J's {@link AbstractLogger} short-circuits below-threshold calls so they
 * never reach the Auralog buffer. This prevents low-severity log floods from filling the in-memory
 * queue in shared logging infrastructure.
 */
public final class AuralogSlf4jLogger extends AbstractLogger {
  private final Level threshold;

  AuralogSlf4jLogger(String name) {
    this(name, Level.INFO);
  }

  AuralogSlf4jLogger(String name, Level threshold) {
    this.name = name;
    this.threshold = Objects.requireNonNull(threshold, "threshold");
  }

  /** Returns true when {@code level} is at or above the configured threshold. */
  private boolean isEnabled(Level level) {
    // Higher numeric severity wins. Level.toInt(): TRACE=0, DEBUG=10, INFO=20, WARN=30, ERROR=40.
    return level.toInt() >= threshold.toInt();
  }

  @Override
  public boolean isTraceEnabled() {
    return isEnabled(Level.TRACE);
  }

  @Override
  public boolean isTraceEnabled(Marker marker) {
    return isTraceEnabled();
  }

  @Override
  public boolean isDebugEnabled() {
    return isEnabled(Level.DEBUG);
  }

  @Override
  public boolean isDebugEnabled(Marker marker) {
    return isDebugEnabled();
  }

  @Override
  public boolean isInfoEnabled() {
    return isEnabled(Level.INFO);
  }

  @Override
  public boolean isInfoEnabled(Marker marker) {
    return isInfoEnabled();
  }

  @Override
  public boolean isWarnEnabled() {
    return isEnabled(Level.WARN);
  }

  @Override
  public boolean isWarnEnabled(Marker marker) {
    return isWarnEnabled();
  }

  @Override
  public boolean isErrorEnabled() {
    return isEnabled(Level.ERROR);
  }

  @Override
  public boolean isErrorEnabled(Marker marker) {
    return isErrorEnabled();
  }

  @Override
  protected String getFullyQualifiedCallerName() {
    return AuralogSlf4jLogger.class.getName();
  }

  @Override
  protected void handleNormalizedLoggingCall(
      Level level, Marker marker, String messagePattern, Object[] arguments, Throwable throwable) {
    String message = MessageFormatter.basicArrayFormat(messagePattern, arguments);
    Map<String, Object> metadata = Collections.singletonMap("logger", name);
    switch (level) {
      case TRACE:
      case DEBUG:
        Auralog.debug(message, metadata);
        break;
      case INFO:
        Auralog.info(message, metadata);
        break;
      case WARN:
        Auralog.warn(message, metadata);
        break;
      case ERROR:
        if (throwable != null) Auralog.error(message, metadata, throwable);
        else Auralog.error(message, metadata);
        break;
      default:
        Auralog.info(message, metadata);
    }
  }
}
