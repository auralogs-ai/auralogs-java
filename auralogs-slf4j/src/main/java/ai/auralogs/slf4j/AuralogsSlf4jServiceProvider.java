package ai.auralogs.slf4j;

import org.slf4j.ILoggerFactory;
import org.slf4j.IMarkerFactory;
import org.slf4j.event.Level;
import org.slf4j.helpers.BasicMarkerFactory;
import org.slf4j.helpers.NOPMDCAdapter;
import org.slf4j.spi.MDCAdapter;
import org.slf4j.spi.SLF4JServiceProvider;

/**
 * Registered as the {@code org.slf4j.spi.SLF4JServiceProvider} for SLF4J 2.0+. SLF4J picks this up
 * automatically via the {@code ServiceLoader} SPI when the {@code auralogs-slf4j} artifact is on
 * the classpath.
 *
 * <p>The forwarding level threshold is read from the {@code auralogs.slf4j.level} system property
 * at SLF4J initialization time and defaults to {@code INFO}. Calls below the threshold are dropped
 * by SLF4J's {@code AbstractLogger} short-circuit and never reach the Auralogs buffer.
 */
public final class AuralogsSlf4jServiceProvider implements SLF4JServiceProvider {
  public static final String REQUESTED_API_VERSION = "2.0.99";

  /**
   * System property that controls the minimum level forwarded to Auralogs. Accepted values are the
   * SLF4J event-level names ({@code TRACE}, {@code DEBUG}, {@code INFO}, {@code WARN}, {@code
   * ERROR}); case-insensitive. Unrecognized values fall back to {@code INFO}.
   */
  public static final String LEVEL_PROPERTY = "auralogs.slf4j.level";

  private ILoggerFactory loggerFactory;
  private IMarkerFactory markerFactory;
  private MDCAdapter mdcAdapter;

  @Override
  public void initialize() {
    loggerFactory = new AuralogsSlf4jLoggerFactory(resolveThreshold());
    markerFactory = new BasicMarkerFactory();
    mdcAdapter = new NOPMDCAdapter();
  }

  private static Level resolveThreshold() {
    String raw = System.getProperty(LEVEL_PROPERTY);
    if (raw == null || raw.isBlank()) return Level.INFO;
    try {
      return Level.valueOf(raw.trim().toUpperCase(java.util.Locale.ROOT));
    } catch (IllegalArgumentException unknownLevel) {
      return Level.INFO;
    }
  }

  @Override
  public ILoggerFactory getLoggerFactory() {
    return loggerFactory;
  }

  @Override
  public IMarkerFactory getMarkerFactory() {
    return markerFactory;
  }

  @Override
  public MDCAdapter getMDCAdapter() {
    return mdcAdapter;
  }

  @Override
  public String getRequestedApiVersion() {
    return REQUESTED_API_VERSION;
  }
}
