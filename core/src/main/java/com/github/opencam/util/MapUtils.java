package com.github.opencam.util;

import java.util.Map;

public class MapUtils {
  public static <K, V> V getRequired(final Map<K, V> map, final K key) {
    if (map == null) {
      throw new RuntimeException("Cannot find " + key + " in a null map");
    }

    final V out = map.get(key);

    if (out == null) {
      throw new RuntimeException(key + " is not in the map");
    }

    return out;
  }
}
