package ai.auralogs.internal;

public enum LogLevel {
  DEBUG(10, "debug"),
  INFO(20, "info"),
  WARN(30, "warn"),
  ERROR(40, "error"),
  FATAL(50, "fatal");

  private final int severity;
  private final String wire;

  LogLevel(int severity, String wire) {
    this.severity = severity;
    this.wire = wire;
  }

  public boolean isAtOrAbove(LogLevel other) {
    return severity >= other.severity;
  }

  public String wireName() {
    return wire;
  }
}
