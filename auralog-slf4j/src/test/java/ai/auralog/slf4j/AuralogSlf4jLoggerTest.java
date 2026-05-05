package ai.auralog.slf4j;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import ai.auralog.Auralog;
import ai.auralog.AuralogConfig;
import com.github.tomakehurst.wiremock.WireMockServer;
import java.time.Duration;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class AuralogSlf4jLoggerTest {
  private WireMockServer wm;

  @BeforeEach
  void setUp() {
    wm = new WireMockServer(0);
    wm.start();
    wm.stubFor(post("/v1/logs").willReturn(ok()));
    wm.stubFor(post("/v1/logs/single").willReturn(ok()));

    Auralog.init(
        AuralogConfig.builder()
            .apiKey("k")
            .environment("test")
            .endpoint("http://localhost:" + wm.port())
            .allowInsecureEndpoint(true)
            .flushInterval(Duration.ofMinutes(10))
            .captureErrors(false)
            .build());
  }

  @AfterEach
  void tearDown() {
    Auralog.shutdown();
    wm.stop();
  }

  @Test
  void loggerFactoryReturnsAuralogSlf4jLogger() {
    Logger log = LoggerFactory.getLogger("test");
    assertThat(log).isInstanceOf(AuralogSlf4jLogger.class);
  }

  @Test
  void errorLevelGoesToSingleEndpoint() {
    Logger log = LoggerFactory.getLogger("test");
    log.error("boom");
    await().atMost(Duration.ofSeconds(2)).until(() -> wm.getAllServeEvents().size() >= 1);
    assertThat(wm.getAllServeEvents().get(0).getRequest().getUrl()).isEqualTo("/v1/logs/single");
  }

  @Test
  void errorWithThrowable() {
    Logger log = LoggerFactory.getLogger("test");
    log.error("crashed", new RuntimeException("x"));
    await().atMost(Duration.ofSeconds(2)).until(() -> wm.getAllServeEvents().size() >= 1);
    String body = wm.getAllServeEvents().get(0).getRequest().getBodyAsString();
    assertThat(body).contains("RuntimeException");
  }

  @Test
  void defaultThresholdIsInfoSoDebugIsDisabled() {
    Logger log = LoggerFactory.getLogger("test");
    assertThat(log.isErrorEnabled()).isTrue();
    assertThat(log.isWarnEnabled()).isTrue();
    assertThat(log.isInfoEnabled()).isTrue();
    assertThat(log.isDebugEnabled()).isFalse();
    assertThat(log.isTraceEnabled()).isFalse();
  }

  @Test
  void debugBelowThresholdDoesNotReachAuralog() {
    // Default INFO threshold — debug() should be filtered out by AbstractLogger before reaching
    // Auralog.debug(...). This is the load-bearing DoS-prevention behavior.
    Logger log = LoggerFactory.getLogger("test");
    log.debug("noisy");
    Auralog.shutdown(); // forces a flush
    assertThat(wm.getAllServeEvents()).isEmpty();
  }

  @Test
  void warnThresholdDisablesInfoAndBelow() {
    AuralogSlf4jLogger log = new AuralogSlf4jLogger("test", org.slf4j.event.Level.WARN);
    assertThat(log.isErrorEnabled()).isTrue();
    assertThat(log.isWarnEnabled()).isTrue();
    assertThat(log.isInfoEnabled()).isFalse();
    assertThat(log.isDebugEnabled()).isFalse();
    assertThat(log.isTraceEnabled()).isFalse();
  }

  @Test
  void traceThresholdEnablesEverything() {
    AuralogSlf4jLogger log = new AuralogSlf4jLogger("test", org.slf4j.event.Level.TRACE);
    assertThat(log.isTraceEnabled()).isTrue();
    assertThat(log.isDebugEnabled()).isTrue();
    assertThat(log.isInfoEnabled()).isTrue();
    assertThat(log.isWarnEnabled()).isTrue();
    assertThat(log.isErrorEnabled()).isTrue();
  }

  @Test
  void infoBuffersThenFlushesOnShutdown() {
    Logger log = LoggerFactory.getLogger("test");
    log.info("user signed in {}", "alice");
    assertThat(wm.getAllServeEvents()).isEmpty();
    Auralog.shutdown();
    assertThat(wm.getAllServeEvents()).hasSize(1);
    String body = wm.getAllServeEvents().get(0).getRequest().getBodyAsString();
    assertThat(body).contains("user signed in alice");
  }
}
