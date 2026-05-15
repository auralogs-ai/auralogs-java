package ai.auralogs;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;

import com.github.tomakehurst.wiremock.WireMockServer;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.*;

class AuralogsTest {
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
    Auralogs.shutdown();
    wm.stop();
  }

  @Test
  void usingBeforeInitThrows() {
    Auralogs.shutdown();
    assertThatIllegalStateException()
        .isThrownBy(() -> Auralogs.info("hi"))
        .withMessageContaining("init");
  }

  @Test
  void endToEndErrorSend() {
    Auralogs.init(
        AuralogsConfig.builder()
            .apiKey("k")
            .environment("test")
            .endpoint("http://localhost:" + wm.port())
            .allowInsecureEndpoint(true)
            .flushInterval(Duration.ofMinutes(10))
            .captureErrors(false)
            .build());
    Auralogs.error("boom", Map.of("o", "abc"));
    await().atMost(Duration.ofSeconds(2)).until(() -> wm.getAllServeEvents().size() >= 1);
    assertThat(wm.getAllServeEvents()).isNotEmpty();
  }

  @Test
  void getTraceIdReturnsValueAfterInit() {
    Auralogs.init(
        AuralogsConfig.builder()
            .apiKey("k")
            .environment("test")
            .endpoint("http://localhost:" + wm.port())
            .allowInsecureEndpoint(true)
            .captureErrors(false)
            .build());
    assertThat(Auralogs.getTraceId()).isNotNull().isNotEmpty();
  }

  @Test
  void setTraceIdChangesTraceId() {
    Auralogs.init(
        AuralogsConfig.builder()
            .apiKey("k")
            .environment("test")
            .endpoint("http://localhost:" + wm.port())
            .allowInsecureEndpoint(true)
            .captureErrors(false)
            .build());
    Auralogs.setTraceId("custom-trace");
    assertThat(Auralogs.getTraceId()).isEqualTo("custom-trace");
  }

  @Test
  void getTraceIdThrowsBeforeInit() {
    Auralogs.shutdown();
    assertThatIllegalStateException().isThrownBy(Auralogs::getTraceId);
  }

  @Test
  void traceIdFromConfigIsUsed() {
    Auralogs.init(
        AuralogsConfig.builder()
            .apiKey("k")
            .environment("test")
            .endpoint("http://localhost:" + wm.port())
            .allowInsecureEndpoint(true)
            .traceId("config-trace-123")
            .captureErrors(false)
            .build());
    assertThat(Auralogs.getTraceId()).isEqualTo("config-trace-123");
  }

  @Test
  void infoBuffersThenFlushesOnShutdown() {
    Auralogs.init(
        AuralogsConfig.builder()
            .apiKey("k")
            .environment("test")
            .endpoint("http://localhost:" + wm.port())
            .allowInsecureEndpoint(true)
            .flushInterval(Duration.ofMinutes(10))
            .captureErrors(false)
            .build());
    Auralogs.info("hi", Map.of("u", "1"));
    assertThat(wm.getAllServeEvents()).isEmpty();
    Auralogs.shutdown();
    assertThat(wm.getAllServeEvents()).hasSize(1);
  }
}
