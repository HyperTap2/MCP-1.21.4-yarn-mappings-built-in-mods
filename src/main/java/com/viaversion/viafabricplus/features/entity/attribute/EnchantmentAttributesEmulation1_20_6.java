package com.viaversion.viafabricplus.features.entity.attribute;

import com.viaversion.viafabricplus.protocoltranslator.ProtocolTranslator;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import java.util.Optional;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry.Reference;
import net.minecraft.registry.tag.BlockTags;

public final class EnchantmentAttributesEmulation1_20_6 {
   public static void init() {
   }

   public static void tick(ClientWorld world) {
      if (!ProtocolTranslator.getTargetVersion().newerThan(ProtocolVersion.v1_20_5)) {
         for (Entity entity : world.getEntities()) {
            if (entity.isLogicalSideForUpdatingMovement() && entity instanceof LivingEntity livingEntity) {
               livingEntity.getAttributeInstance(EntityAttributes.WATER_MOVEMENT_EFFICIENCY)
                  .setBaseValue(getEquipmentLevel(Enchantments.DEPTH_STRIDER, livingEntity) / 3.0F);
               setGenericMovementEfficiencyAttribute(livingEntity);
            }
         }

         for (PlayerEntity player : world.getPlayers()) {
            if (player.isLogicalSideForUpdatingMovement()) {
               int efficiencyLevel = getEquipmentLevel(Enchantments.EFFICIENCY, player);
               player.getAttributeInstance(EntityAttributes.MINING_EFFICIENCY)
                  .setBaseValue(efficiencyLevel > 0 ? efficiencyLevel * efficiencyLevel + 1 : 0.0);
               player.getAttributeInstance(EntityAttributes.SNEAKING_SPEED)
                  .setBaseValue(0.3F + getEquipmentLevel(Enchantments.SWIFT_SNEAK, player) * 0.15F);
               player.getAttributeInstance(EntityAttributes.SUBMERGED_MINING_SPEED)
                  .setBaseValue(getEquipmentLevel(Enchantments.AQUA_AFFINITY, player) <= 0 ? 0.2F : 1.0);
            }
         }
      }
   }

   public static void setGenericMovementEfficiencyAttribute(LivingEntity entity) {
      boolean isOnSoulSpeedBlock = entity.getWorld().getBlockState(entity.getVelocityAffectingPos()).isIn(BlockTags.SOUL_SPEED_BLOCKS);
      if (isOnSoulSpeedBlock && getEquipmentLevel(Enchantments.SOUL_SPEED, entity) > 0) {
         entity.getAttributeInstance(EntityAttributes.MOVEMENT_EFFICIENCY).setBaseValue(1.0);
      } else {
         entity.getAttributeInstance(EntityAttributes.MOVEMENT_EFFICIENCY).setBaseValue(0.0);
      }
   }

   private static int getEquipmentLevel(RegistryKey<Enchantment> enchantment, LivingEntity entity) {
      Optional<Reference<Enchantment>> enchantmentRef = entity.getWorld().getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT).getOptional(enchantment);
      return enchantmentRef.<Integer>map(e -> EnchantmentHelper.getEquipmentLevel(e, entity)).orElse(0);
   }

}
