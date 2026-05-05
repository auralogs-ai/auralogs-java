package ai.auralog;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;

import com.github.tomakehurst.wiremock.WireMockServer;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.*;

class AuralogTest {
  private WireMockServer wm;

  @BeforeEach
  void setUp() {
    wm = new WireMockServer(0);
    wm.start();
    wm.stubFor(post("/v1/logs").willReturn(ok()));
    wm.stubFor(post("/v1/logs/single").willReturn(ok()));
  }

  @AfterEach
  void tearDown() {
    Auralog.shutdown();
    wm.stop();
  }

  @Test
  void usingBeforeInitThrows() {
    Auralog.shutdown();
    assertThatIllegalStateException()
        .isThrownBy(() -> Auralog.info("hi"))
        .withMessageContaining("init");
  }

  @Test
  void endToEndErrorSend() {
    Auralog.init(
        AuralogConfig.builder()
            .apiKey("k")
            .environment("test")
            .endpoint("http://localhost:" + wm.port())
            .allowInsecureEndpoint(true)
            .flushInterval(Duration.ofMinutes(10))
            .captureErrors(false)
            .build());
    Auralog.error("boom", Map.of("o", "abc"));
    await().atMost(Duration.ofSeconds(2)).until(() -> wm.getAllServeEvents().size() >= 1);
    assertThat(wm.getAllServeEvents()).isNotEmpty();
  }

  @Test
  void getTraceIdReturnsValueAfterInit() {
    Auralog.init(
        AuralogConfig.builder()
            .apiKey("k")
            .environment("test")
            .endpoint("http://localhost:" + wm.port())
            .allowInsecureEndpoint(true)
            .captureErrors(false)
            .build());
    assertThat(Auralog.getTraceId()).isNotNull().isNotEmpty();
  }

  @Test
  void setTraceIdChangesTraceId() {
    Auralog.init(
        AuralogConfig.builder()
            .apiKey("k")
            .environment("test")
            .endpoint("http://localhost:" + wm.port())
            .allowInsecureEndpoint(true)
            .captureErrors(false)
            .build());
    Auralog.setTraceId("custom-trace");
    assertThat(Auralog.getTraceId()).isEqualTo("custom-trace");
  }

  @Test
  void getTraceIdThrowsBeforeInit() {
    Auralog.shutdown();
    assertThatIllegalStateException().isThrownBy(Auralog::getTraceId);
  }

  @Test
  void traceIdFromConfigIsUsed() {
    Auralog.init(
        AuralogConfig.builder()
            .apiKey("k")
            .environment("test")
            .endpoint("http://localhost:" + wm.port())
            .allowInsecureEndpoint(true)
            .traceId("config-trace-123")
            .captureErrors(false)
            .build());
    assertThat(Auralog.getTraceId()).isEqualTo("config-trace-123");
  }

  @Test
  void infoBuffersThenFlushesOnShutdown() {
    Auralog.init(
        AuralogConfig.builder()
            .apiKey("k")
            .environment("test")
            .endpoint("http://localhost:" + wm.port())
            .allowInsecureEndpoint(true)
            .flushInterval(Duration.ofMinutes(10))
            .captureErrors(false)
            .build());
    Auralog.info("hi", Map.of("u", "1"));
    assertThat(wm.getAllServeEvents()).isEmpty();
    Auralog.shutdown();
    assertThat(wm.getAllServeEvents()).hasSize(1);
  }
}
