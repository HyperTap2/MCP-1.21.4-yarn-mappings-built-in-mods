package net.caffeinemc.mods.sodium.client.util;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

public class FlawlessFrames {
   private static final Set<Object> ACTIVE = Collections.newSetFromMap(new ConcurrentHashMap<>());
   private static final Function<String, Consumer<Boolean>> PROVIDER = name -> {
      Object token = new Object();
      return active -> {
         if (active) {
            ACTIVE.add(token);
         } else {
            ACTIVE.remove(token);
         }
      };
   };

   public static Function<String, Consumer<Boolean>> getProvider() {
      return PROVIDER;
   }

   public static boolean isActive() {
      return !ACTIVE.isEmpty();
   }
}
