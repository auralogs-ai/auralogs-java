package ai.auralog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class AuralogConfigTest {
  @Test
  void defaults() {
    AuralogConfig cfg = AuralogConfig.builder().apiKey("k").build();
    assertThat(cfg.apiKey()).isEqualTo("k");
    assertThat(cfg.environment()).isEqualTo("production");
    assertThat(cfg.endpoint()).isEqualTo("https://ingest.auralog.ai");
    assertThat(cfg.flushInterval()).isEqualTo(Duration.ofSeconds(5));
    assertThat(cfg.captureErrors()).isTrue();
    assertThat(cfg.traceId()).isNull();
  }

  @Test
  void apiKeyRequired() {
    assertThatNullPointerException().isThrownBy(() -> AuralogConfig.builder().build());
  }

  @Test
  void overrides() {
    AuralogConfig cfg =
        AuralogConfig.builder()
            .apiKey("k")
            .environment("staging")
            .endpoint("http://localhost:8787")
            .allowInsecureEndpoint(true)
            .flushInterval(Duration.ofSeconds(1))
            .captureErrors(false)
            .build();
    assertThat(cfg.environment()).isEqualTo("staging");
    assertThat(cfg.endpoint()).isEqualTo("http://localhost:8787");
    assertThat(cfg.flushInterval()).isEqualTo(Duration.ofSeconds(1));
    assertThat(cfg.captureErrors()).isFalse();
  }

  @Test
  void traceIdOverride() {
    AuralogConfig cfg = AuralogConfig.builder().apiKey("k").traceId("my-trace").build();
    assertThat(cfg.traceId()).isEqualTo("my-trace");
  }

  @Test
  void httpsEndpointAccepted() {
    AuralogConfig cfg = AuralogConfig.builder().apiKey("k").endpoint("https://example.com").build();
    assertThat(cfg.endpoint()).isEqualTo("https://example.com");
  }

  @Test
  void httpEndpointRejectedByDefault() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> AuralogConfig.builder().apiKey("k").endpoint("http://insecure").build())
        .withMessageContaining("https");
  }

  @Test
  void httpEndpointAllowedWhenInsecureOptIn() {
    AuralogConfig cfg =
        AuralogConfig.builder()
            .apiKey("k")
            .endpoint("http://insecure")
            .allowInsecureEndpoint(true)
            .build();
    assertThat(cfg.endpoint()).isEqualTo("http://insecure");
  }

  @Test
  void blankEndpointRejected() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> AuralogConfig.builder().apiKey("k").endpoint("   ").build());
  }

  @Test
  void endpointWithoutSchemeRejected() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> AuralogConfig.builder().apiKey("k").endpoint("ingest.example").build());
  }

  @Test
  void uppercaseHttpsAccepted() {
    AuralogConfig cfg = AuralogConfig.builder().apiKey("k").endpoint("HTTPS://example.com").build();
    assertThat(cfg.endpoint()).isEqualTo("HTTPS://example.com");
  }

  @Test
  void maxQueueSizeDefaults() {
    AuralogConfig cfg = AuralogConfig.builder().apiKey("k").build();
    assertThat(cfg.maxQueueSize()).isEqualTo(1000);
  }

  @Test
  void maxQueueSizeOverride() {
    AuralogConfig cfg = AuralogConfig.builder().apiKey("k").maxQueueSize(50).build();
    assertThat(cfg.maxQueueSize()).isEqualTo(50);
  }

  @Test
  void maxQueueSizeMustBePositive() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> AuralogConfig.builder().apiKey("k").maxQueueSize(0).build());
  }
}
