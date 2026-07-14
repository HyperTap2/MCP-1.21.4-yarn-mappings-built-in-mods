package com.viaversion.viafabricplus.protocoltranslator.protocol.storage;

import com.viaversion.viaversion.api.connection.StorableObject;

public final class BedrockJoinGameTracker implements StorableObject {
   private long seed;
   private String levelId;
   private long enchantmentSeed;

   public long getSeed() {
      return this.seed;
   }

   public void setSeed(long seed) {
      this.seed = seed;
   }

   public String getLevelId() {
      return this.levelId;
   }

   public void setLevelId(String levelId) {
      this.levelId = levelId;
   }

   public long getEnchantmentSeed() {
      return this.enchantmentSeed;
   }

   public void setEnchantmentSeed(long enchantmentSeed) {
      this.enchantmentSeed = enchantmentSeed;
   }
}
