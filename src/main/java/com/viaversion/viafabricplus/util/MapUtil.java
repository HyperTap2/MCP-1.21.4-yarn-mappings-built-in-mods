package com.viaversion.viafabricplus.util;

import java.util.LinkedHashMap;
import java.util.Map;

public final class MapUtil {
   public static <K, V> Map<K, V> linkedHashMap(Object... objects) {
      if (objects.length % 2 != 0) {
         throw new IllegalArgumentException("Uneven object count");
      }

      Map<K, V> map = new LinkedHashMap<>();

      for (int i = 0; i < objects.length; i += 2) {
         map.put((K)objects[i], (V)objects[i + 1]);
      }

      return map;
   }
}
