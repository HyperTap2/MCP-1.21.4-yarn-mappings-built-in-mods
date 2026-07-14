package com.viaversion.viafabricplus.features.networking.legacy_chat_signature;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

public final class LegacyKeySignatureStorage {
   private static final Map<Object, byte[]> SIGNATURES = Collections.synchronizedMap(new WeakHashMap<>());

   private LegacyKeySignatureStorage() {
   }

   public static void put(Object owner, byte[] signature) {
      if (owner != null && signature != null) {
         SIGNATURES.put(owner, signature);
      }
   }

   public static byte[] get(Object owner) {
      return owner == null ? null : SIGNATURES.get(owner);
   }
}
