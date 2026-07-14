package dev.tr7zw.entityculling;

import java.util.HashSet;
import java.util.Set;

public final class EntityCullingConfig {
   static final int CURRENT_VERSION = 9;
   static final Set<String> DEFAULT_TICK_CULLING_WHITELIST = Set.of(
      "minecraft:acacia_boat",
      "minecraft:acacia_chest_boat",
      "minecraft:bamboo_chest_raft",
      "minecraft:bamboo_raft",
      "minecraft:birch_boat",
      "minecraft:birch_chest_boat",
      "minecraft:block_display",
      "minecraft:cherry_boat",
      "minecraft:cherry_chest_boat",
      "minecraft:dark_oak_boat",
      "minecraft:dark_oak_chest_boat",
      "minecraft:firework_rocket",
      "minecraft:item_display",
      "minecraft:jungle_boat",
      "minecraft:jungle_chest_boat",
      "minecraft:mangrove_boat",
      "minecraft:mangrove_chest_boat",
      "minecraft:oak_boat",
      "minecraft:oak_chest_boat",
      "minecraft:pale_oak_boat",
      "minecraft:pale_oak_chest_boat",
      "minecraft:spruce_boat",
      "minecraft:spruce_chest_boat",
      "minecraft:text_display"
   );

   public int configVersion = CURRENT_VERSION;
   public boolean enabled = true;
   public boolean renderNametagsThroughWalls = true;
   public boolean tickCulling = true;
   public boolean skipEntityCulling;
   public boolean skipBlockEntityCulling;
   public boolean blockEntityFrustumCulling = true;
   public boolean solidLeaves;
   public int tracingDistance = 128;
   public int sleepDelay = 10;
   public int hitboxLimit = 50;
   public int captureRate = 5;
   public int chunkRadius = 8;
   public Set<String> blockEntityWhitelist = new HashSet<>(Set.of("minecraft:beacon"));
   public Set<String> entityWhitelist = new HashSet<>();
   public Set<String> tickCullingWhitelist = new HashSet<>(DEFAULT_TICK_CULLING_WHITELIST);
}
