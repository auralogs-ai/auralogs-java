package ai.auralogs.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ErrorCaptureGlobalMetadataTest {

  @AfterEach
  void cleanup() {
    ErrorCapture.uninstall();
  }

  @Test
  void uncaughtExceptionsCarryGlobalMetadata() throws InterruptedException {
    List<LogEntry> out = new ArrayList<>();
    Logger log = new Logger("test", out::add, null, () -> Map.of("userId", "session-user"));
    ErrorCapture.install(log);

    Thread worker =
        new Thread(
            () -> {
              throw new RuntimeException("kaboom");
            },
            "worker");
    worker.start();
    worker.join();

    assertThat(out).isNotEmpty();
    LogEntry entry = out.get(0);
    assertThat(entry.metadata()).containsEntry("userId", "session-user");
    // Per-call metadata from ErrorCapture (the thread name) is also preserved.
    assertThat(entry.metadata()).containsEntry("thread", "worker");
    assertThat(entry.stackTrace()).contains("kaboom");
  }
}
