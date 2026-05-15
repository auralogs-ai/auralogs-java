package ai.auralogs.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JsonTest {
  @Test
  void primitives() {
    assertThat(Json.encode("hello")).isEqualTo("\"hello\"");
    assertThat(Json.encode(42)).isEqualTo("42");
    assertThat(Json.encode(true)).isEqualTo("true");
    assertThat(Json.encode(null)).isEqualTo("null");
  }

  @Test
  void stringEscaping() {
    assertThat(Json.encode("a\"b\nc")).isEqualTo("\"a\\\"b\\nc\"");
  }

  @Test
  void mapAndArray() {
    assertThat(Json.encode(Map.of("k", "v"))).isEqualTo("{\"k\":\"v\"}");
    assertThat(Json.encode(List.of(1, 2, 3))).isEqualTo("[1,2,3]");
  }

  @Test
  void nested() {
    String out = Json.encode(Map.of("logs", List.of(Map.of("level", "info"))));
    assertThat(out).isEqualTo("{\"logs\":[{\"level\":\"info\"}]}");
  }
}
