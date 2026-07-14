package dev.tr7zw.entityculling;

import com.logisticscraft.occlusionculling.OcclusionCullingInstance;
import com.logisticscraft.occlusionculling.util.Vec3d;
import java.util.List;
import java.util.Map;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

final class EntityCullingTask implements Runnable {
   private final EntityCullingManager manager;
   private final OcclusionCullingInstance culling;
   private final Vec3d camera = new Vec3d(0.0, 0.0, 0.0);
   private final Vec3d minimum = new Vec3d(0.0, 0.0, 0.0);
   private final Vec3d maximum = new Vec3d(0.0, 0.0, 0.0);
   private volatile boolean requested;
   private volatile boolean inGame;
   private volatile List<Entity> entities = List.of();
   private volatile Map<BlockPos, BlockEntity> blockEntities = Map.of();
   private volatile net.minecraft.util.math.Vec3d cameraPosition = net.minecraft.util.math.Vec3d.ZERO;

   EntityCullingTask(EntityCullingManager manager, OcclusionCullingInstance culling) {
      this.manager = manager;
      this.culling = culling;
   }

   void update(List<Entity> entities, Map<BlockPos, BlockEntity> blockEntities, net.minecraft.util.math.Vec3d cameraPosition) {
      this.entities = entities;
      this.blockEntities = blockEntities;
      this.cameraPosition = cameraPosition;
      this.inGame = true;
      this.requested = true;
   }

   void clear() {
      this.inGame = false;
      this.entities = List.of();
      this.blockEntities = Map.of();
   }

   @Override
   public void run() {
      while (this.manager.isRunning()) {
         try {
            Thread.sleep(Math.max(1, this.manager.getConfig().sleepDelay));
            if (!this.inGame || !this.manager.isEnabled() || !this.requested) {
               continue;
            }

            this.requested = false;
            net.minecraft.util.math.Vec3d cameraPos = this.cameraPosition;
            this.camera.set(cameraPos.x, cameraPos.y, cameraPos.z);
            this.culling.resetCache();
            if (!this.manager.getConfig().skipBlockEntityCulling) {
               this.cullBlockEntities(cameraPos);
            }
            if (!this.manager.getConfig().skipEntityCulling) {
               this.cullEntities(cameraPos);
            }
         } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return;
         } catch (Throwable throwable) {
            EntityCullingManager.LOGGER.warn("Entity culling pass failed; keeping affected objects visible", throwable);
         }
      }
   }

   private void cullEntities(net.minecraft.util.math.Vec3d cameraPos) {
      EntityCullingConfig config = this.manager.getConfig();
      for (Entity entity : this.entities) {
         if (!(entity instanceof Cullable cullable) || entity.isRemoved() || this.manager.isEntityWhitelisted(entity)) {
            continue;
         }
         if (cullable.entityCulling$isForcedVisible()) {
            continue;
         }
         if (this.manager.isEntityAlwaysVisible(entity) || !entity.getPos().isInRange(cameraPos, config.tracingDistance)) {
            cullable.entityCulling$setCulled(false);
            continue;
         }

         Box box = this.manager.getCullingBox(entity);
         if (box == null || box.getLengthX() > config.hitboxLimit || box.getLengthY() > config.hitboxLimit || box.getLengthZ() > config.hitboxLimit) {
            cullable.entityCulling$setCulled(false);
            continue;
         }

         this.minimum.set(box.minX, box.minY, box.minZ);
         this.maximum.set(box.maxX, box.maxY, box.maxZ);
         cullable.entityCulling$setCulled(!this.culling.isAABBVisible(this.minimum, this.maximum, this.camera));
      }
   }

   private void cullBlockEntities(net.minecraft.util.math.Vec3d cameraPos) {
      EntityCullingConfig config = this.manager.getConfig();
      for (Map.Entry<BlockPos, BlockEntity> entry : this.blockEntities.entrySet()) {
         BlockEntity blockEntity = entry.getValue();
         if (!(blockEntity instanceof Cullable cullable) || blockEntity.isRemoved() || this.manager.isBlockEntityWhitelisted(blockEntity)) {
            continue;
         }
         if (cullable.entityCulling$isForcedVisible()) {
            continue;
         }
         BlockPos pos = entry.getKey();
         if (!net.minecraft.util.math.Vec3d.ofCenter(pos).isInRange(cameraPos, 64.0)) {
            cullable.entityCulling$setCulled(false);
            continue;
         }

         Box box = this.manager.getBlockEntityCullingBox(blockEntity);
         if (box.getLengthX() > config.hitboxLimit || box.getLengthY() > config.hitboxLimit || box.getLengthZ() > config.hitboxLimit) {
            cullable.entityCulling$setCulled(false);
            continue;
         }
         this.minimum.set(box.minX, box.minY, box.minZ);
         this.maximum.set(box.maxX, box.maxY, box.maxZ);
         cullable.entityCulling$setCulled(!this.culling.isAABBVisible(this.minimum, this.maximum, this.camera));
      }
   }
}
