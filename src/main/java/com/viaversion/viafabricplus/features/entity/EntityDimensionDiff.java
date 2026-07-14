package com.viaversion.viafabricplus.features.entity;

import com.viaversion.viafabricplus.api.events.ChangeProtocolVersionCallback;
import com.viaversion.viafabricplus.base.Events;
import com.viaversion.viafabricplus.util.MapUtil;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EntityAttachmentType;
import net.minecraft.entity.EntityAttachments;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EntityAttachments.Builder;
import net.raphimc.vialegacy.api.LegacyProtocolVersion;

public final class EntityDimensionDiff {
   private static final Map<EntityType<?>, Map<ProtocolVersion, EntityDimensions>> ENTITY_DIMENSIONS = MapUtil.linkedHashMap(
      EntityType.WITHER,
      MapUtil.linkedHashMap(
         ProtocolVersion.v1_7_6,
         EntityDimensionDiff.EntityDimensionsBuilder.create(EntityType.WITHER).withChangingDimensions(0.9F, 4.0F).build(),
         ProtocolVersion.v1_8,
         EntityType.WITHER.getDimensions()
      ),
      EntityType.SILVERFISH,
      MapUtil.linkedHashMap(
         ProtocolVersion.v1_7_6,
         EntityDimensionDiff.EntityDimensionsBuilder.create(EntityType.SILVERFISH).withChangingDimensions(0.3F, 0.7F).build(),
         ProtocolVersion.v1_8,
         EntityType.SILVERFISH.getDimensions()
      ),
      EntityType.SNOW_GOLEM,
      MapUtil.linkedHashMap(
         ProtocolVersion.v1_7_6,
         EntityDimensionDiff.EntityDimensionsBuilder.create(EntityType.SNOW_GOLEM).withChangingDimensions(0.4F, 1.8F).build(),
         ProtocolVersion.v1_8,
         EntityType.SNOW_GOLEM.getDimensions()
      ),
      EntityType.ZOMBIE,
      MapUtil.linkedHashMap(
         ProtocolVersion.v1_7_6,
         EntityDimensionDiff.EntityDimensionsBuilder.create(EntityType.ZOMBIE).withChangingDimensions(0.6F, 1.8F).build(),
         ProtocolVersion.v1_8,
         EntityDimensionDiff.EntityDimensionsBuilder.create(EntityType.ZOMBIE).withFixedDimensions(0.6F, 1.95F).build(),
         ProtocolVersion.v1_9,
         EntityType.ZOMBIE.getDimensions()
      ),
      EntityType.CHICKEN,
      MapUtil.linkedHashMap(
         LegacyProtocolVersion.b1_7tob1_7_3,
         EntityDimensionDiff.EntityDimensionsBuilder.create(EntityType.CHICKEN).withChangingDimensions(0.3F, 0.4F).build(),
         ProtocolVersion.v1_7_6,
         EntityDimensionDiff.EntityDimensionsBuilder.create(EntityType.CHICKEN).withChangingDimensions(0.3F, 0.7F).build(),
         ProtocolVersion.v1_8,
         EntityType.CHICKEN.getDimensions()
      ),
      EntityType.SHEEP,
      MapUtil.linkedHashMap(
         LegacyProtocolVersion.c0_28toc0_30,
         EntityDimensionDiff.EntityDimensionsBuilder.create(EntityType.SHEEP).withChangingDimensions(1.4F, 1.72F).build(),
         LegacyProtocolVersion.a1_0_15,
         EntityType.SHEEP.getDimensions()
      ),
      EntityType.OCELOT,
      MapUtil.linkedHashMap(
         ProtocolVersion.v1_7_6,
         EntityDimensionDiff.EntityDimensionsBuilder.create(EntityType.OCELOT).withChangingDimensions(0.6F, 0.8F).build(),
         ProtocolVersion.v1_8,
         EntityType.OCELOT.getDimensions()
      ),
      EntityType.OAK_BOAT,
      MapUtil.linkedHashMap(
         ProtocolVersion.v1_8,
         EntityDimensionDiff.EntityDimensionsBuilder.create(EntityType.OAK_BOAT).withChangingDimensions(1.5F, 0.6F).build(),
         ProtocolVersion.v1_9,
         EntityType.OAK_BOAT.getDimensions()
      ),
      EntityType.CREEPER,
      MapUtil.linkedHashMap(
         ProtocolVersion.v1_8,
         EntityDimensionDiff.EntityDimensionsBuilder.create(EntityType.CREEPER).withChangingDimensions(0.6F, 1.8F).build(),
         ProtocolVersion.v1_9,
         EntityType.CREEPER.getDimensions()
      ),
      EntityType.IRON_GOLEM,
      MapUtil.linkedHashMap(
         ProtocolVersion.v1_8,
         EntityDimensionDiff.EntityDimensionsBuilder.create(EntityType.IRON_GOLEM).withChangingDimensions(1.4F, 2.9F).build(),
         ProtocolVersion.v1_9,
         EntityType.IRON_GOLEM.getDimensions()
      ),
      EntityType.SKELETON,
      MapUtil.linkedHashMap(
         ProtocolVersion.v1_7_6,
         EntityDimensionDiff.EntityDimensionsBuilder.create(EntityType.SKELETON).withChangingDimensions(0.6F, 1.8F).build(),
         ProtocolVersion.v1_8,
         EntityDimensionDiff.EntityDimensionsBuilder.create(EntityType.SKELETON).withChangingDimensions(0.6F, 1.95F).build(),
         ProtocolVersion.v1_9,
         EntityType.SKELETON.getDimensions()
      ),
      EntityType.WITHER_SKELETON,
      MapUtil.linkedHashMap(
         LegacyProtocolVersion.r1_4_6tor1_4_7,
         EntityDimensionDiff.EntityDimensionsBuilder.create(EntityType.WITHER_SKELETON).withChangingDimensions(0.72F, 2.16F).build(),
         ProtocolVersion.v1_7_6,
         EntityDimensionDiff.EntityDimensionsBuilder.create(EntityType.WITHER_SKELETON).withChangingDimensions(0.72F, 2.34F).build(),
         ProtocolVersion.v1_8,
         EntityDimensionDiff.EntityDimensionsBuilder.create(EntityType.WITHER_SKELETON).withChangingDimensions(0.72F, 2.535F).build(),
         ProtocolVersion.v1_9,
         EntityType.WITHER_SKELETON.getDimensions()
      ),
      EntityType.COW,
      MapUtil.linkedHashMap(
         ProtocolVersion.v1_8,
         EntityDimensionDiff.EntityDimensionsBuilder.create(EntityType.COW).withChangingDimensions(0.9F, 1.3F).build(),
         ProtocolVersion.v1_9,
         EntityType.COW.getDimensions()
      ),
      EntityType.HORSE,
      MapUtil.linkedHashMap(
         ProtocolVersion.v1_8,
         EntityDimensionDiff.EntityDimensionsBuilder.create(EntityType.HORSE).withChangingDimensions(1.4F, 1.6F).build(),
         ProtocolVersion.v1_9,
         EntityType.HORSE.getDimensions()
      ),
      EntityType.MOOSHROOM,
      MapUtil.linkedHashMap(
         ProtocolVersion.v1_8,
         EntityDimensionDiff.EntityDimensionsBuilder.create(EntityType.MOOSHROOM).withChangingDimensions(0.9F, 1.3F).build(),
         ProtocolVersion.v1_9,
         EntityType.MOOSHROOM.getDimensions()
      ),
      EntityType.RABBIT,
      MapUtil.linkedHashMap(
         ProtocolVersion.v1_8,
         EntityDimensionDiff.EntityDimensionsBuilder.create(EntityType.RABBIT).withChangingDimensions(0.6F, 0.7F).build(),
         ProtocolVersion.v1_9,
         EntityType.RABBIT.getDimensions()
      ),
      EntityType.SQUID,
      MapUtil.linkedHashMap(
         ProtocolVersion.v1_8,
         EntityDimensionDiff.EntityDimensionsBuilder.create(EntityType.SQUID).withChangingDimensions(0.95F, 0.95F).build(),
         ProtocolVersion.v1_9,
         EntityType.SQUID.getDimensions()
      ),
      EntityType.VILLAGER,
      MapUtil.linkedHashMap(
         ProtocolVersion.v1_8,
         EntityDimensionDiff.EntityDimensionsBuilder.create(EntityType.VILLAGER).withChangingDimensions(0.6F, 1.8F).build(),
         ProtocolVersion.v1_9,
         EntityType.VILLAGER.getDimensions()
      ),
      EntityType.WOLF,
      MapUtil.linkedHashMap(
         LegacyProtocolVersion.r1_1,
         EntityDimensionDiff.EntityDimensionsBuilder.create(EntityType.WOLF).withChangingDimensions(0.8F, 0.8F).build(),
         ProtocolVersion.v1_8,
         EntityDimensionDiff.EntityDimensionsBuilder.create(EntityType.WOLF).withChangingDimensions(0.6F, 0.8F).build(),
         ProtocolVersion.v1_9,
         EntityType.WOLF.getDimensions()
      ),
      EntityType.DRAGON_FIREBALL,
      MapUtil.linkedHashMap(
         ProtocolVersion.v1_10,
         EntityDimensionDiff.EntityDimensionsBuilder.create(EntityType.DRAGON_FIREBALL).withChangingDimensions(0.3125F, 0.3125F).build(),
         ProtocolVersion.v1_11,
         EntityType.DRAGON_FIREBALL.getDimensions()
      ),
      EntityType.LEASH_KNOT,
      MapUtil.linkedHashMap(
         ProtocolVersion.v1_16_4,
         EntityDimensionDiff.EntityDimensionsBuilder.create(EntityType.LEASH_KNOT).withChangingDimensions(0.5F, 0.5F).build(),
         ProtocolVersion.v1_17,
         EntityType.LEASH_KNOT.getDimensions()
      ),
      EntityType.SLIME,
      MapUtil.linkedHashMap(
         ProtocolVersion.v1_13_2,
         EntityDimensionDiff.EntityDimensionsBuilder.create(EntityType.SLIME).withChangingDimensions(0.51F, 0.51F).build(),
         ProtocolVersion.v1_14,
         EntityDimensionDiff.EntityDimensionsBuilder.create(EntityType.SLIME).withChangingDimensions(0.52019995F, 0.52019995F).build(),
         ProtocolVersion.v1_20_5,
         EntityType.SLIME.getDimensions()
      ),
      EntityType.MAGMA_CUBE,
      MapUtil.linkedHashMap(
         ProtocolVersion.v1_13_2,
         EntityDimensionDiff.EntityDimensionsBuilder.create(EntityType.MAGMA_CUBE).withChangingDimensions(0.51F, 0.51F).build(),
         ProtocolVersion.v1_14,
         EntityDimensionDiff.EntityDimensionsBuilder.create(EntityType.MAGMA_CUBE).withChangingDimensions(0.52019995F, 0.52019995F).build(),
         ProtocolVersion.v1_20_5,
         EntityType.MAGMA_CUBE.getDimensions()
      ),
      EntityType.ARROW,
      MapUtil.linkedHashMap(
         LegacyProtocolVersion.c0_28toc0_30,
         EntityDimensionDiff.EntityDimensionsBuilder.create(EntityType.ARROW).withChangingDimensions(0.3F, 0.5F).build(),
         LegacyProtocolVersion.a1_0_15,
         EntityType.ARROW.getDimensions()
      )
   );

   public static void init() {
   }

   static {
      Events.CHANGE_PROTOCOL_VERSION
         .register(
            (ChangeProtocolVersionCallback)(oldVersion, newVersion) -> MinecraftClient.getInstance()
               .execute(() -> ENTITY_DIMENSIONS.forEach((entityType, dimensionMap) -> {
                  for (Entry<ProtocolVersion, EntityDimensions> entry : dimensionMap.entrySet()) {
                     ProtocolVersion version = entry.getKey();
                     EntityDimensions dimensions = entry.getValue();
                     if (oldVersion.newerThan(version) && newVersion.olderThanOrEqualTo(version)) {
                        entityType.dimensions = dimensions;
                        break;
                     }

                     if (newVersion.newerThanOrEqualTo(version) && oldVersion.olderThanOrEqualTo(version)) {
                        entityType.dimensions = dimensions;
                     }
                  }
               }))
         );
   }

   private static class EntityDimensionsBuilder {
      private EntityDimensions entityDimensions;
      private Builder attachments = EntityAttachments.builder();

      public static EntityDimensionDiff.EntityDimensionsBuilder create() {
         return new EntityDimensionDiff.EntityDimensionsBuilder();
      }

      public static EntityDimensionDiff.EntityDimensionsBuilder create(EntityType<?> template) {
         EntityDimensionDiff.EntityDimensionsBuilder entityDimensionsBuilder = new EntityDimensionDiff.EntityDimensionsBuilder();
         entityDimensionsBuilder.entityDimensions = template.getDimensions();
         return entityDimensionsBuilder;
      }

      public EntityDimensionDiff.EntityDimensionsBuilder withChangingDimensions(float width, float height) {
         this.entityDimensions = new EntityDimensions(width, height, this.entityDimensions.eyeHeight(), this.entityDimensions.attachments(), false);
         return this;
      }

      public EntityDimensionDiff.EntityDimensionsBuilder withFixedDimensions(float width, float height) {
         this.entityDimensions = new EntityDimensions(width, height, this.entityDimensions.eyeHeight(), this.entityDimensions.attachments(), true);
         return this;
      }

      public EntityDimensionDiff.EntityDimensionsBuilder withEyeHeight(float eyeHeight) {
         this.entityDimensions = this.entityDimensions.withEyeHeight(eyeHeight);
         return this;
      }

      public EntityDimensionDiff.EntityDimensionsBuilder withPassengerAttachments(float... offsetYs) {
         for (float f : offsetYs) {
            this.attachments = this.attachments.add(EntityAttachmentType.PASSENGER, 0.0F, f, 0.0F);
         }

         this.entityDimensions = this.entityDimensions.withAttachments(this.attachments);
         return this;
      }

      public EntityDimensions build() {
         return this.entityDimensions;
      }
   }
}
