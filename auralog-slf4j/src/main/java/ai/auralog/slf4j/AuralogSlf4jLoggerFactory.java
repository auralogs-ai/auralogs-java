package ai.auralog.slf4j;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.event.Level;

final class AuralogSlf4jLoggerFactory implements ILoggerFactory {
  private final ConcurrentHashMap<String, Logger> loggers = new ConcurrentHashMap<>();
  private final Level threshold;

  AuralogSlf4jLoggerFactory() {
    this(Level.INFO);
  }

  AuralogSlf4jLoggerFactory(Level threshold) {
    this.threshold = Objects.requireNonNull(threshold, "threshold");
  }

  @Override
  public Logger getLogger(String name) {
    return loggers.computeIfAbsent(name, key -> new AuralogSlf4jLogger(key, threshold));
  }
}
