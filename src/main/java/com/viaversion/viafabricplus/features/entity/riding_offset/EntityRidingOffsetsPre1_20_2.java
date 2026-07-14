package com.viaversion.viafabricplus.features.entity.riding_offset;

import com.viaversion.viafabricplus.protocoltranslator.ProtocolTranslator;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.AbstractPiglinEntity;
import net.minecraft.entity.mob.AbstractSkeletonEntity;
import net.minecraft.entity.mob.EndermiteEntity;
import net.minecraft.entity.mob.HoglinEntity;
import net.minecraft.entity.mob.PatrolEntity;
import net.minecraft.entity.mob.PhantomEntity;
import net.minecraft.entity.mob.PiglinEntity;
import net.minecraft.entity.mob.RavagerEntity;
import net.minecraft.entity.mob.ShulkerEntity;
import net.minecraft.entity.mob.SilverfishEntity;
import net.minecraft.entity.mob.SkeletonHorseEntity;
import net.minecraft.entity.mob.SpiderEntity;
import net.minecraft.entity.mob.VexEntity;
import net.minecraft.entity.mob.ZoglinEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.mob.ZombifiedPiglinEntity;
import net.minecraft.entity.passive.AbstractDonkeyEntity;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.passive.AllayEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.CamelEntity;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.entity.passive.LlamaEntity;
import net.minecraft.entity.passive.SnifferEntity;
import net.minecraft.entity.passive.StriderEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.AbstractBoatEntity;
import net.minecraft.entity.vehicle.AbstractChestBoatEntity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public final class EntityRidingOffsetsPre1_20_2 {
   public static Vec3d getMountedHeightOffset(Entity entity, Entity passenger) {
      double yOffset = entity.getHeight() * 0.75F;
      if (!(entity instanceof AbstractBoatEntity abstractBoatEntity)) {
         if (entity instanceof CamelEntity camelEntity) {
            if (!camelEntity.hasPassenger(passenger)) {
               return Vec3d.ZERO;
            }

            boolean firstPassenger = camelEntity.getPassengerList().indexOf(passenger) == 0;
            yOffset = camelEntity.getDimensions(camelEntity.isSitting() ? EntityPose.SITTING : EntityPose.STANDING).height()
               - (camelEntity.isBaby() ? 0.35F : 0.6F);
            if (camelEntity.isRemoved()) {
               yOffset = 0.01F;
            } else {
               yOffset = camelEntity.getPassengerAttachmentY(
                  firstPassenger, 0.0F, EntityDimensions.fixed(0.0F, (float)(0.375F * camelEntity.getScaleFactor() + yOffset)), camelEntity.getScaleFactor()
               );
            }

            double zOffset = 0.5;
            if (camelEntity.getPassengerList().size() > 1) {
               if (!firstPassenger) {
                  zOffset = -0.7F;
               }

               if (passenger instanceof AnimalEntity) {
                  zOffset += 0.2F;
               }
            }

            return new Vec3d(0.0, yOffset, zOffset);
         } else if (entity instanceof ChickenEntity chickenEntity) {
            return new Vec3d(0.0, chickenEntity.getBodyY(0.5) - chickenEntity.getY(), -0.1F);
         } else {
            if (entity instanceof EnderDragonEntity enderDragonEntity) {
               yOffset = enderDragonEntity.body.getHeight();
            } else if (entity instanceof HoglinEntity hoglinEntity) {
               yOffset = hoglinEntity.getHeight() - (hoglinEntity.isBaby() ? 0.2F : 0.15F);
            } else {
               if (entity instanceof LlamaEntity) {
                  return new Vec3d(0.0, entity.getHeight() * 0.6F, -0.3F);
               }

               if (entity instanceof PhantomEntity) {
                  yOffset = entity.getStandingEyeHeight();
               } else if (entity instanceof PiglinEntity) {
                  yOffset = entity.getHeight() * 0.92F;
               } else if (entity instanceof RavagerEntity) {
                  yOffset = 2.1F;
               } else if (entity instanceof SkeletonHorseEntity) {
                  yOffset -= 0.1875;
               } else if (entity instanceof SnifferEntity) {
                  yOffset = 1.8F;
               } else if (entity instanceof SpiderEntity) {
                  yOffset = entity.getHeight() * 0.5F;
               } else if (entity instanceof StriderEntity striderEntity) {
                  float speed = Math.min(0.25F, striderEntity.limbAnimator.getSpeed());
                  float pos = striderEntity.limbAnimator.getPos();
                  yOffset = striderEntity.getHeight() - 0.19F + 0.12F * MathHelper.cos(pos * 1.5F) * 2.0F * speed;
               } else if (entity instanceof ZoglinEntity zoglinEntity) {
                  yOffset = zoglinEntity.getHeight() - (zoglinEntity.isBaby() ? 0.2F : 0.15F);
               } else if (entity instanceof AbstractDonkeyEntity) {
                  yOffset -= 0.25;
               } else if (entity instanceof AbstractMinecartEntity) {
                  yOffset = 0.0;
               }
            }

            return entity instanceof AbstractHorseEntity abstractHorseEntity && abstractHorseEntity.lastAngryAnimationProgress > 0.0F
               ? new Vec3d(0.0, yOffset + 0.15F * abstractHorseEntity.lastAngryAnimationProgress, -0.7F * abstractHorseEntity.lastAngryAnimationProgress)
               : new Vec3d(0.0, yOffset, 0.0);
         }
      } else {
         if (!abstractBoatEntity.hasPassenger(passenger)) {
            return Vec3d.ZERO;
         }

         if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_8)) {
            yOffset = -0.3F;
            double xOffset = MathHelper.cos(abstractBoatEntity.getYaw() * (float) Math.PI / 180.0F);
            double zOffset = MathHelper.sin(abstractBoatEntity.getYaw() * (float) Math.PI / 180.0F);
            return new Vec3d(0.4F * xOffset, yOffset, 0.4F * zOffset);
         }

         if (abstractBoatEntity.isRemoved()) {
            yOffset = 0.01F;
         } else {
            yOffset = abstractBoatEntity.getType() != EntityType.BAMBOO_RAFT && abstractBoatEntity.getType() != EntityType.BAMBOO_CHEST_RAFT ? -0.1F : 0.25;
         }

         double xOffset = abstractBoatEntity instanceof AbstractChestBoatEntity ? 0.15F : 0.0;
         if (abstractBoatEntity.getPassengerList().size() > 1) {
            int idx = abstractBoatEntity.getPassengerList().indexOf(passenger);
            if (idx == 0) {
               xOffset = 0.2F;
            } else {
               xOffset = -0.6F;
            }

            if (passenger instanceof AnimalEntity) {
               xOffset += 0.2F;
            }
         }

         return new Vec3d(xOffset, yOffset, 0.0).rotateY((float) (-Math.PI / 2));
      }
   }

   public static double getHeightOffset(Entity entity) {
      if (entity instanceof AllayEntity || entity instanceof VexEntity) {
         return ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_19_1) ? 0.0 : 0.4;
      } else if (entity instanceof ArmorStandEntity armorStandEntity) {
         return armorStandEntity.isMarker() ? 0.0 : 0.1;
      } else if (entity instanceof EndermiteEntity) {
         return 0.1;
      } else if (entity instanceof ShulkerEntity shulkerEntity) {
         EntityType<?> vehicleType = shulkerEntity.getVehicle().getType();
         return !(shulkerEntity.getVehicle() instanceof BoatEntity) && vehicleType != EntityType.MINECART
            ? 0.0
            : 0.1875 - getMountedHeightOffset(shulkerEntity.getVehicle(), null).y;
      } else if (entity instanceof SilverfishEntity) {
         return 0.1;
      } else if (entity instanceof ZombifiedPiglinEntity zombifiedPiglinEntity) {
         return zombifiedPiglinEntity.isBaby() ? -0.05 : -0.45;
      } else if (entity instanceof ZombieEntity zombieEntity) {
         return zombieEntity.isBaby() ? 0.0 : -0.45;
      } else if (entity instanceof AnimalEntity) {
         return 0.14;
      } else if (entity instanceof PatrolEntity) {
         return -0.45;
      } else if (entity instanceof PlayerEntity) {
         return -0.35;
      } else if (entity instanceof AbstractPiglinEntity abstractPiglinEntity) {
         return abstractPiglinEntity.isBaby() ? -0.05 : -0.45;
      } else {
         return entity instanceof AbstractSkeletonEntity ? -0.6 : 0.0;
      }
   }
}
