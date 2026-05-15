package ai.auralogs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class AuralogsConfigTest {
  @Test
  void defaults() {
    AuralogsConfig cfg = AuralogsConfig.builder().apiKey("k").build();
    assertThat(cfg.apiKey()).isEqualTo("k");
    assertThat(cfg.environment()).isEqualTo("production");
    assertThat(cfg.endpoint()).isEqualTo("https://ingest.auralogs.ai");
    assertThat(cfg.flushInterval()).isEqualTo(Duration.ofSeconds(5));
    assertThat(cfg.captureErrors()).isTrue();
    assertThat(cfg.traceId()).isNull();
  }

  @Test
  void apiKeyRequired() {
    assertThatNullPointerException().isThrownBy(() -> AuralogsConfig.builder().build());
  }

  @Test
  void overrides() {
    AuralogsConfig cfg =
        AuralogsConfig.builder()
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
    AuralogsConfig cfg = AuralogsConfig.builder().apiKey("k").traceId("my-trace").build();
    assertThat(cfg.traceId()).isEqualTo("my-trace");
  }

  @Test
  void httpsEndpointAccepted() {
    AuralogsConfig cfg =
        AuralogsConfig.builder().apiKey("k").endpoint("https://example.com").build();
    assertThat(cfg.endpoint()).isEqualTo("https://example.com");
  }

  @Test
  void httpEndpointRejectedByDefault() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> AuralogsConfig.builder().apiKey("k").endpoint("http://insecure").build())
        .withMessageContaining("https");
  }

  @Test
  void httpEndpointAllowedWhenInsecureOptIn() {
    AuralogsConfig cfg =
        AuralogsConfig.builder()
            .apiKey("k")
            .endpoint("http://insecure")
            .allowInsecureEndpoint(true)
            .build();
    assertThat(cfg.endpoint()).isEqualTo("http://insecure");
  }

  @Test
  void blankEndpointRejected() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> AuralogsConfig.builder().apiKey("k").endpoint("   ").build());
  }

  @Test
  void endpointWithoutSchemeRejected() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> AuralogsConfig.builder().apiKey("k").endpoint("ingest.example").build());
  }

  @Test
  void uppercaseHttpsAccepted() {
    AuralogsConfig cfg =
        AuralogsConfig.builder().apiKey("k").endpoint("HTTPS://example.com").build();
    assertThat(cfg.endpoint()).isEqualTo("HTTPS://example.com");
  }

  @Test
  void maxQueueSizeDefaults() {
    AuralogsConfig cfg = AuralogsConfig.builder().apiKey("k").build();
    assertThat(cfg.maxQueueSize()).isEqualTo(1000);
  }

  @Test
  void maxQueueSizeOverride() {
    AuralogsConfig cfg = AuralogsConfig.builder().apiKey("k").maxQueueSize(50).build();
    assertThat(cfg.maxQueueSize()).isEqualTo(50);
  }

  @Test
  void maxQueueSizeMustBePositive() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> AuralogsConfig.builder().apiKey("k").maxQueueSize(0).build());
  }
}
