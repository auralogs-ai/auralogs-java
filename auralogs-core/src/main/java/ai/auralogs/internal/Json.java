package ai.auralogs.internal;

import java.util.IdentityHashMap;
import java.util.Map;

public final class Json {
  private Json() {}

  public static String encode(Object value) {
    StringBuilder sb = new StringBuilder();
    writeValue(sb, value);
    return sb.toString();
  }

  /**
   * Walk {@code value} and throw {@link IllegalArgumentException} if the SDK's encoder cannot
   * faithfully represent it as JSON. Catches:
   *
   * <ul>
   *   <li>Circular references (a container that contains itself, directly or transitively).
   *   <li>Values that are neither primitive, {@code Map}, nor {@code Iterable}, and whose {@code
   *       toString()} would be the only fallback. We allow such values in {@link #encode(Object)}
   *       for backward compatibility, but the merge layer uses this stricter check to decide
   *       whether {@code globalMetadata} is shippable.
   * </ul>
   *
   * <p>Numbers, booleans, null, strings, {@link Map}, and {@link Iterable} are accepted.
   */
  static void assertSerializable(Object value) {
    walkStrict(value, new IdentityHashMap<>());
  }

  private static void walkStrict(Object value, IdentityHashMap<Object, Boolean> seen) {
    if (value == null) return;
    if (value instanceof String || value instanceof Number || value instanceof Boolean) return;
    if (value instanceof Map) {
      if (seen.put(value, Boolean.TRUE) != null) {
        throw new IllegalArgumentException("circular reference in metadata");
      }
      try {
        for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
          walkStrict(entry.getValue(), seen);
        }
      } finally {
        seen.remove(value);
      }
      return;
    }
    if (value instanceof Iterable) {
      if (seen.put(value, Boolean.TRUE) != null) {
        throw new IllegalArgumentException("circular reference in metadata");
      }
      try {
        for (Object element : (Iterable<?>) value) {
          walkStrict(element, seen);
        }
      } finally {
        seen.remove(value);
      }
      return;
    }
    throw new IllegalArgumentException(
        "non-serializable metadata value of type " + value.getClass().getName());
  }

  private static void writeValue(StringBuilder sb, Object value) {
    if (value == null) {
      sb.append("null");
      return;
    }
    if (value instanceof String) {
      writeString(sb, (String) value);
      return;
    }
    if (value instanceof Number || value instanceof Boolean) {
      sb.append(value);
      return;
    }
    if (value instanceof Map) {
      writeMap(sb, (Map<?, ?>) value);
      return;
    }
    if (value instanceof Iterable) {
      writeArray(sb, (Iterable<?>) value);
      return;
    }
    writeString(sb, value.toString());
  }

  private static void writeMap(StringBuilder sb, Map<?, ?> map) {
    sb.append('{');
    boolean first = true;
    for (Map.Entry<?, ?> entry : map.entrySet()) {
      if (!first) sb.append(',');
      writeString(sb, String.valueOf(entry.getKey()));
      sb.append(':');
      writeValue(sb, entry.getValue());
      first = false;
    }
    sb.append('}');
  }

  private static void writeArray(StringBuilder sb, Iterable<?> values) {
    sb.append('[');
    boolean first = true;
    for (Object element : values) {
      if (!first) sb.append(',');
      writeValue(sb, element);
      first = false;
    }
    sb.append(']');
  }

  private static void writeString(StringBuilder sb, String value) {
    sb.append('"');
    for (int index = 0; index < value.length(); index++) {
      char ch = value.charAt(index);
      switch (ch) {
        case '"':
          sb.append("\\\"");
          break;
        case '\\':
          sb.append("\\\\");
          break;
        case '\n':
          sb.append("\\n");
          break;
        case '\r':
          sb.append("\\r");
          break;
        case '\t':
          sb.append("\\t");
          break;
        default:
          if (ch < 0x20) sb.append(String.format("\\u%04x", (int) ch));
          else sb.append(ch);
          break;
      }
    }
    sb.append('"');
  }
}
