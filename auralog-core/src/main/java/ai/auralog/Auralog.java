package ai.auralog;

import ai.auralog.internal.ErrorCapture;
import ai.auralog.internal.Logger;
import ai.auralog.internal.Transport;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Static facade for the Auralog SDK.
 *
 * <p>Call {@link #init(AuralogConfig)} once at application startup. Emit logs via the {@code
 * debug/info/warn/error/fatal} methods. Shutdown happens automatically at JVM exit; call {@link
 * #shutdown()} explicitly for deterministic flush.
 *
 * <pre>{@code
 * Auralog.init(AuralogConfig.builder().apiKey("...").build());
 * Auralog.info("user signed in", Map.of("userId", "123"));
 * Auralog.error("payment failed", Map.of("orderId", "abc"), exception);
 * }</pre>
 */
public final class Auralog {
  private static volatile @Nullable Logger logger;
  private static volatile @Nullable Transport transport;
  private static volatile boolean shutdownHookRegistered = false;

  private Auralog() {}

  /**
   * Initialize the SDK. Idempotent — calling again replaces the prior configuration and flushes the
   * previous transport.
   */
  public static synchronized void init(AuralogConfig config) {
    shutdown();
    Transport t =
        new Transport(
            config.apiKey(), config.endpoint(), config.flushInterval(), config.maxQueueSize());
    Logger l = new Logger(config.environment(), t::send, config.traceId(), config.globalMetadata());
    transport = t;
    logger = l;
    if (config.captureErrors()) {
      ErrorCapture.install(l);
    }
    if (!shutdownHookRegistered) {
      Runtime.getRuntime().addShutdownHook(new Thread(Auralog::shutdown, "auralog-shutdown-hook"));
      shutdownHookRegistered = true;
    }
  }

  /** Flush pending logs and stop the background thread. Safe to call multiple times. */
  public static synchronized void shutdown() {
    ErrorCapture.uninstall();
    Transport t = transport;
    if (t != null) t.shutdown();
    transport = null;
    logger = null;
  }

  // ----- Trace ID API -----

  /** Returns the current trace ID. Auto-generated at init unless overridden. */
  public static String getTraceId() {
    return require().getTraceId();
  }

  /**
   * Replaces the current trace ID. Use this to propagate an incoming trace ID from another service.
   */
  public static void setTraceId(String id) {
    require().setTraceId(id);
  }

  // ----- Public logging API -----

  public static void debug(String message) {
    require().debug(message, null);
  }

  public static void debug(String message, Map<String, Object> metadata) {
    require().debug(message, metadata);
  }

  public static void info(String message) {
    require().info(message, null);
  }

  public static void info(String message, Map<String, Object> metadata) {
    require().info(message, metadata);
  }

  public static void warn(String message) {
    require().warn(message, null);
  }

  public static void warn(String message, Map<String, Object> metadata) {
    require().warn(message, metadata);
  }

  public static void error(String message) {
    require().error(message, null, null);
  }

  public static void error(String message, Map<String, Object> metadata) {
    require().error(message, metadata, null);
  }

  public static void error(String message, Throwable t) {
    require().error(message, null, t);
  }

  public static void error(String message, Map<String, Object> metadata, Throwable t) {
    require().error(message, metadata, t);
  }

  public static void fatal(String message) {
    require().fatal(message, null, null);
  }

  public static void fatal(String message, Map<String, Object> metadata) {
    require().fatal(message, metadata, null);
  }

  public static void fatal(String message, Throwable t) {
    require().fatal(message, null, t);
  }

  public static void fatal(String message, Map<String, Object> metadata, Throwable t) {
    require().fatal(message, metadata, t);
  }

  private static Logger require() {
    Logger l = logger;
    if (l == null) {
      throw new IllegalStateException("Auralog.init(config) must be called before logging");
    }
    return l;
  }
}
