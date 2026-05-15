package ai.auralogs.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ErrorCaptureTest {
  @AfterEach
  void cleanup() {
    ErrorCapture.uninstall();
  }

  @Test
  void installCapturesUncaughtFromThread() throws InterruptedException {
    List<LogEntry> out = new ArrayList<>();
    Logger log = new Logger("test", out::add, null);
    ErrorCapture.install(log);

    Thread t =
        new Thread(
            () -> {
              throw new RuntimeException("background-boom");
            },
            "worker");
    t.start();
    t.join();

    assertThat(out).isNotEmpty();
    assertThat(out.get(0).stackTrace()).contains("background-boom");
  }

  @Test
  void uninstallRestoresOriginal() {
    Thread.UncaughtExceptionHandler before = Thread.getDefaultUncaughtExceptionHandler();
    ErrorCapture.install(new Logger("t", e -> {}, null));
    ErrorCapture.uninstall();
    assertThat(Thread.getDefaultUncaughtExceptionHandler()).isSameAs(before);
  }

  @Test
  void doubleInstallIsIdempotent() {
    Thread.UncaughtExceptionHandler before = Thread.getDefaultUncaughtExceptionHandler();
    Logger log = new Logger("t", e -> {}, null);
    ErrorCapture.install(log);
    ErrorCapture.install(log);
    ErrorCapture.uninstall();
    assertThat(Thread.getDefaultUncaughtExceptionHandler()).isSameAs(before);
  }

  @Test
  void handlerItselfCrashingDoesNotPropagate() throws InterruptedException {
    AtomicBoolean sinkCalled = new AtomicBoolean(false);
    Logger crashingLog =
        new Logger(
            "t",
            e -> {
              sinkCalled.set(true);
              throw new RuntimeException("sink failed");
            },
            null);
    ErrorCapture.install(crashingLog);
    Thread t =
        new Thread(
            () -> {
              throw new RuntimeException("x");
            });
    t.start();
    t.join();
    assertThat(sinkCalled).isTrue();
  }
}
