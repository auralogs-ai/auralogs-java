package ai.auralogs;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

/**
 * Immutable configuration for the Auralogs SDK. Build via {@link #builder()}.
 *
 * <pre>{@code
 * AuralogsConfig config = AuralogsConfig.builder()
 *     .apiKey(System.getenv("AURALOG_API_KEY"))
 *     .environment("production")
 *     .globalMetadata(() -> Map.of("userId", currentUserId()))
 *     .build();
 * Auralogs.init(config);
 * }</pre>
 */
public final class AuralogsConfig {
  private static final String DEFAULT_ENDPOINT = "https://ingest.auralogs.ai";
  private static final Duration DEFAULT_FLUSH_INTERVAL = Duration.ofSeconds(5);
  private static final String DEFAULT_ENVIRONMENT = "production";
  private static final int DEFAULT_MAX_QUEUE_SIZE = 1000;

  private final String apiKey;
  private final String environment;
  private final String endpoint;
  private final Duration flushInterval;
  private final boolean captureErrors;
  private final int maxQueueSize;
  private final @Nullable String traceId;
  private final @Nullable Supplier<Map<String, Object>> globalMetadata;

  private AuralogsConfig(Builder builder) {
    this.apiKey = Objects.requireNonNull(builder.apiKey, "apiKey");
    this.environment = builder.environment;
    this.endpoint = builder.endpoint;
    this.flushInterval = builder.flushInterval;
    this.captureErrors = builder.captureErrors;
    this.maxQueueSize = builder.maxQueueSize;
    this.traceId = builder.traceId;
    this.globalMetadata = builder.globalMetadata;
  }

  public String apiKey() {
    return apiKey;
  }

  public String environment() {
    return environment;
  }

  public String endpoint() {
    return endpoint;
  }

  public Duration flushInterval() {
    return flushInterval;
  }

  public boolean captureErrors() {
    return captureErrors;
  }

  /**
   * Maximum number of buffered log entries before the in-memory queue starts dropping the oldest
   * entries to keep memory bounded. Default is {@value #DEFAULT_MAX_QUEUE_SIZE}.
   */
  public int maxQueueSize() {
    return maxQueueSize;
  }

  public @Nullable String traceId() {
    return traceId;
  }

  /**
   * Returns the configured global-metadata supplier, or {@code null} if none was set. The supplier
   * is invoked at every emission — see {@link Builder#globalMetadata(Supplier)} for semantics.
   */
  public @Nullable Supplier<Map<String, Object>> globalMetadata() {
    return globalMetadata;
  }

  public static Builder builder() {
    return new Builder();
  }

  /** Fluent builder for {@link AuralogsConfig}. Every setter returns {@code this}. */
  public static final class Builder {
    private String apiKey;
    private String environment = DEFAULT_ENVIRONMENT;
    private String endpoint = DEFAULT_ENDPOINT;
    private Duration flushInterval = DEFAULT_FLUSH_INTERVAL;
    private boolean captureErrors = true;
    private int maxQueueSize = DEFAULT_MAX_QUEUE_SIZE;
    private boolean allowInsecureEndpoint = false;
    private @Nullable String traceId;
    private @Nullable Supplier<Map<String, Object>> globalMetadata;

    public Builder apiKey(String value) {
      this.apiKey = value;
      return this;
    }

    public Builder environment(String value) {
      this.environment = value;
      return this;
    }

    public Builder endpoint(String value) {
      this.endpoint = value;
      return this;
    }

    public Builder flushInterval(Duration value) {
      this.flushInterval = value;
      return this;
    }

    public Builder captureErrors(boolean value) {
      this.captureErrors = value;
      return this;
    }

    /**
     * Cap the number of buffered log entries held in memory between flushes. When the cap is
     * reached, the oldest entries are dropped first. Defaults to {@value #DEFAULT_MAX_QUEUE_SIZE}.
     * Must be {@code >= 1}.
     *
     * <p>This bound prevents the JVM heap from growing without limit when the ingest endpoint is
     * unreachable for an extended period.
     */
    public Builder maxQueueSize(int value) {
      if (value < 1) {
        throw new IllegalArgumentException("maxQueueSize must be >= 1, got " + value);
      }
      this.maxQueueSize = value;
      return this;
    }

    /**
     * Permit non-{@code https} endpoint URIs (typically {@code http://} for local testing). The SDK
     * rejects non-{@code https} endpoints by default to prevent silently downgrading every POST to
     * plaintext if the endpoint is misconfigured.
     */
    public Builder allowInsecureEndpoint(boolean value) {
      this.allowInsecureEndpoint = value;
      return this;
    }

    public Builder traceId(String value) {
      this.traceId = value;
      return this;
    }

    /**
     * Set a supplier that returns metadata to merge into every emitted log entry.
     *
     * <p>The supplier is invoked at every log emission — never pre-resolved at init — so it
     * naturally late-binds to mutable host state (current user, request scope, feature flags). Keep
     * it cheap: it runs on the hot path.
     *
     * <p>Per-call metadata wins on key collision. Merge is shallow.
     *
     * <p>If the supplier throws, returns a {@link java.util.concurrent.CompletionStage} (or any
     * other async-looking type), or returns a value that the SDK's JSON encoder cannot serialize,
     * the entry is emitted with only the per-call metadata and a one-time warning is logged via
     * {@link System.Logger}. Subsequent failures from the same SDK instance are silent.
     */
    public Builder globalMetadata(@Nullable Supplier<Map<String, Object>> value) {
      this.globalMetadata = value;
      return this;
    }

    /**
     * Convenience overload that wraps a static {@link Map} as a {@link Supplier}. Equivalent to
     * {@code globalMetadata(() -> map)}. The {@link Supplier} form is the load-bearing API; this is
     * for the trivial static-map case.
     */
    public Builder globalMetadata(@Nullable Map<String, Object> value) {
      this.globalMetadata = (value == null) ? null : () -> value;
      return this;
    }

    public AuralogsConfig build() {
      validate();
      return new AuralogsConfig(this);
    }

    private void validate() {
      if (endpoint == null || endpoint.isBlank()) {
        throw new IllegalArgumentException("endpoint must be a non-blank URI");
      }
      String scheme;
      try {
        scheme = new URI(endpoint).getScheme();
      } catch (URISyntaxException syntaxFailure) {
        throw new IllegalArgumentException(
            "endpoint is not a valid URI: " + endpoint, syntaxFailure);
      }
      if (scheme == null) {
        throw new IllegalArgumentException("endpoint must include a scheme: " + endpoint);
      }
      String normalizedScheme = scheme.toLowerCase(Locale.ROOT);
      if (!"https".equals(normalizedScheme) && !allowInsecureEndpoint) {
        throw new IllegalArgumentException(
            "endpoint scheme must be 'https' (got '"
                + scheme
                + "'); call allowInsecureEndpoint(true) to opt in to plaintext (typically only for"
                + " local testing)");
      }
    }
  }
}
