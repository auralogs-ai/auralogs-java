package ai.auralogs.internal;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import java.time.Duration;
import org.junit.jupiter.api.*;

class TransportTest {
  private WireMockServer wm;

  @BeforeEach
  void setUp() {
    wm = new WireMockServer(0);
    wm.start();
    WireMock.configureFor("localhost", wm.port());
  }

  @AfterEach
  void tearDown() {
    wm.stop();
  }

  private String url() {
    return "http://localhost:" + wm.port();
  }

  private LogEntry entry(LogLevel level, String msg) {
    return new LogEntry(level, msg, "test", "2026-04-20T00:00:00Z", null, null, null);
  }

  @Test
  void sendBelowErrorIsBuffered() {
    stubFor(post("/v1/logs").willReturn(ok()));
    Transport t = new Transport("k", url(), Duration.ofMinutes(10));
    t.send(entry(LogLevel.INFO, "hi"));
    assertThat(wm.getAllServeEvents()).isEmpty();
    t.flush();
    assertThat(wm.getAllServeEvents()).hasSize(1);
    t.shutdown();
  }

  @Test
  void errorLevelSendsImmediately() {
    stubFor(post("/v1/logs/single").willReturn(ok()));
    Transport t = new Transport("k", url(), Duration.ofMinutes(10));
    t.send(entry(LogLevel.ERROR, "boom"));
    await().atMost(Duration.ofSeconds(2)).until(() -> wm.getAllServeEvents().size() >= 1);
    t.shutdown();
  }

  @Test
  void networkErrorSwallowed() {
    // No stub → server returns 404; transport must not throw.
    Transport t = new Transport("k", url(), Duration.ofMinutes(10));
    t.send(entry(LogLevel.INFO, "hi"));
    t.flush();
    t.shutdown();
  }

  @Test
  void concurrentSendsDontCorrupt() throws InterruptedException {
    stubFor(post("/v1/logs").willReturn(ok()));
    Transport t = new Transport("k", url(), Duration.ofMinutes(10));
    Thread[] ts = new Thread[10];
    for (int i = 0; i < 10; i++) {
      ts[i] =
          new Thread(
              () -> {
                for (int j = 0; j < 200; j++) t.send(entry(LogLevel.INFO, "x"));
              });
      ts[i].start();
    }
    for (Thread th : ts) th.join();
    t.flush();
    assertThat(wm.getAllServeEvents()).hasSize(1);
    t.shutdown();
  }

  @Test
  void shutdownFlushesPending() {
    stubFor(post("/v1/logs").willReturn(ok()));
    Transport t = new Transport("k", url(), Duration.ofMinutes(10));
    t.send(entry(LogLevel.INFO, "hi"));
    t.shutdown();
    assertThat(wm.getAllServeEvents()).hasSize(1);
  }

  @Test
  void endpointTrailingSlashStripped() {
    stubFor(post("/v1/logs/single").willReturn(ok()));
    Transport t = new Transport("k", url() + "/", Duration.ofMinutes(10));
    t.send(entry(LogLevel.ERROR, "boom"));
    await().atMost(Duration.ofSeconds(2)).until(() -> wm.getAllServeEvents().size() >= 1);
    t.shutdown();
  }

  @Test
  void bufferDropsOldestWhenCapExceeded() {
    stubFor(post("/v1/logs").willReturn(ok()));
    Transport t = new Transport("k", url(), Duration.ofMinutes(10), 3);

    t.send(entry(LogLevel.INFO, "first"));
    t.send(entry(LogLevel.INFO, "second"));
    t.send(entry(LogLevel.INFO, "third"));
    // Two more pushes — the oldest two ("first" and "second") must be evicted.
    t.send(entry(LogLevel.INFO, "fourth"));
    t.send(entry(LogLevel.INFO, "fifth"));

    t.flush();
    assertThat(wm.getAllServeEvents()).hasSize(1);
    String body = wm.getAllServeEvents().get(0).getRequest().getBodyAsString();
    assertThat(body).contains("\"third\"").contains("\"fourth\"").contains("\"fifth\"");
    assertThat(body).doesNotContain("\"first\"").doesNotContain("\"second\"");
    t.shutdown();
  }

  @Test
  void maxQueueSizeMustBePositive() {
    org.assertj.core.api.Assertions.assertThatIllegalArgumentException()
        .isThrownBy(() -> new Transport("k", url(), Duration.ofMinutes(10), 0));
  }
}
