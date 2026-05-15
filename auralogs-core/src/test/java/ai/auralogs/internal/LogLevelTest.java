package ai.auralogs.internal;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LogLevelTest {
  @Test
  void ordering() {
    assertThat(LogLevel.ERROR.isAtOrAbove(LogLevel.WARN)).isTrue();
    assertThat(LogLevel.FATAL.isAtOrAbove(LogLevel.ERROR)).isTrue();
    assertThat(LogLevel.DEBUG.isAtOrAbove(LogLevel.INFO)).isFalse();
    assertThat(LogLevel.INFO.isAtOrAbove(LogLevel.INFO)).isTrue();
  }

  @Test
  void wireNames() {
    assertThat(LogLevel.ERROR.wireName()).isEqualTo("error");
    assertThat(LogLevel.DEBUG.wireName()).isEqualTo("debug");
  }
}
