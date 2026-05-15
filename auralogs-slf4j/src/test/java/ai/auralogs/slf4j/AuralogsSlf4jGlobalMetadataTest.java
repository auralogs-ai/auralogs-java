package ai.auralogs.slf4j;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import ai.auralogs.Auralogs;
import ai.auralogs.AuralogsConfig;
import com.github.tomakehurst.wiremock.WireMockServer;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class AuralogsSlf4jGlobalMetadataTest {
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
  void slf4jBridgeEntriesCarryGlobalMetadataFromSupplier() {
    AtomicInteger callCount = new AtomicInteger();
    Auralogs.init(
        AuralogsConfig.builder()
            .apiKey("k")
            .environment("test")
            .endpoint("http://localhost:" + wm.port())
            .allowInsecureEndpoint(true)
            .flushInterval(Duration.ofMinutes(10))
            .captureErrors(false)
            .globalMetadata(
                () -> {
                  callCount.incrementAndGet();
                  return Map.of("userId", "alice");
                })
            .build());

    Logger log = LoggerFactory.getLogger("test");
    log.error("boom"); // ERROR routes immediately to /v1/logs/single

    await().atMost(Duration.ofSeconds(2)).until(() -> wm.getAllServeEvents().size() >= 1);
    String body = wm.getAllServeEvents().get(0).getRequest().getBodyAsString();
    // Both globalMetadata key and the SLF4J-attached "logger" key should be present.
    assertThat(body).contains("\"userId\":\"alice\"");
    assertThat(body).contains("\"logger\":\"test\"");
    assertThat(callCount.get()).isGreaterThanOrEqualTo(1);
  }

  @Test
  void slf4jPerCallLoggerKeyDoesNotCollideWithGlobalMetadata() {
    Auralogs.init(
        AuralogsConfig.builder()
            .apiKey("k")
            .environment("test")
            .endpoint("http://localhost:" + wm.port())
            .allowInsecureEndpoint(true)
            .flushInterval(Duration.ofMinutes(10))
            .captureErrors(false)
            .globalMetadata(Map.of("userId", "u1", "feature", "checkout"))
            .build());

    Logger log = LoggerFactory.getLogger("a.b.c.MyClass");
    log.error("crashed", new RuntimeException("x"));

    await().atMost(Duration.ofSeconds(2)).until(() -> wm.getAllServeEvents().size() >= 1);
    String body = wm.getAllServeEvents().get(0).getRequest().getBodyAsString();
    assertThat(body).contains("\"userId\":\"u1\"");
    assertThat(body).contains("\"feature\":\"checkout\"");
    assertThat(body).contains("\"logger\":\"a.b.c.MyClass\"");
  }
}
