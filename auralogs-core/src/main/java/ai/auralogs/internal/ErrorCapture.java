package ai.auralogs.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public final class ErrorCapture {
  private static final AtomicReference<Thread.UncaughtExceptionHandler> original =
      new AtomicReference<>();
  private static volatile boolean installed = false;

  private ErrorCapture() {}

  public static synchronized void install(Logger logger) {
    if (!installed) {
      // Capture the real original exactly once so uninstall restores the true default.
      original.set(Thread.getDefaultUncaughtExceptionHandler());
      installed = true;
    }

    Thread.setDefaultUncaughtExceptionHandler(
        (thread, throwable) -> {
          try {
            Map<String, Object> md = new HashMap<>();
            md.put("thread", thread.getName());
            logger.error(
                "Unhandled exception in thread " + thread.getName() + ": " + throwable,
                md,
                throwable);
          } catch (Throwable ignored) {
            // Our handler itself must never crash.
          }
          Thread.UncaughtExceptionHandler o = original.get();
          if (o != null) o.uncaughtException(thread, throwable);
        });
  }

  public static synchronized void uninstall() {
    if (!installed) return;
    Thread.setDefaultUncaughtExceptionHandler(original.get());
    original.set(null);
    installed = false;
  }
}
