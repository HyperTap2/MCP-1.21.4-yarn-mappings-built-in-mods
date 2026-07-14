package net.caffeinemc.mods.lithium.common.entity;

import com.google.common.collect.AbstractIterator;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.caffeinemc.mods.lithium.common.entity.movement.ChunkAwareBlockCollisionSweeper;
import net.caffeinemc.mods.lithium.common.util.Pos;
import net.caffeinemc.mods.lithium.common.world.WorldHelper;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.Entity;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.CollisionView;
import net.minecraft.world.EntityView;
import net.minecraft.world.World;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ChunkStatus;
import org.jetbrains.annotations.Nullable;

public final class LithiumEntityCollisions {
   public static final double EPSILON = 1.0E-7;

   private LithiumEntityCollisions() {
   }

   public static List<VoxelShape> getBlockCollisions(World world, @Nullable Entity entity, Box box) {
      return new ChunkAwareBlockCollisionSweeper(world, entity, box).collectAll();
   }

   public static boolean doesBoxCollideWithBlocks(World world, @Nullable Entity entity, Box box) {
      return new ChunkAwareBlockCollisionSweeper(world, entity, box).hasNext();
   }

   public static boolean doesBoxCollideWithHardEntities(EntityView view, @Nullable Entity entity, Box box) {
      return !isBoxEmpty(box) && getEntityWorldBorderCollisionIterable(view, entity, box.expand(EPSILON), false).iterator().hasNext();
   }

   public static void appendEntityCollisions(List<VoxelShape> collisions, World world, @Nullable Entity entity, Box box) {
      if (isBoxEmpty(box)) {
         return;
      }
      for (Entity other : WorldHelper.getEntitiesForCollision(world, box.expand(EPSILON), entity)) {
         if (entity == null ? other.isCollidable() : entity.collidesWith(other)) {
            collisions.add(VoxelShapes.cuboid(other.getBoundingBox()));
         }
      }
   }

   public static void appendWorldBorderCollision(List<VoxelShape> collisions, Entity entity, Box box) {
      WorldBorder border = entity.getWorld().getWorldBorder();
      if (!isWithinWorldBorder(border, box) && isWithinWorldBorder(border, entity.getBoundingBox())) {
         collisions.add(border.asVoxelShape());
      }
   }

   public static Iterable<VoxelShape> getEntityWorldBorderCollisionIterable(
      EntityView view, @Nullable Entity entity, Box box, boolean includeWorldBorder
   ) {
      return () -> new AbstractIterator<>() {
         private List<Entity> entities;
         private int index;
         private boolean consumedWorldBorder;

         @Override
         protected VoxelShape computeNext() {
            if (this.entities == null) {
               this.entities = WorldHelper.getEntitiesForCollision(view, box, entity);
            }
            while (this.index < this.entities.size()) {
               Entity other = this.entities.get(this.index++);
               if (entity == null ? other.isCollidable() : entity.collidesWith(other)) {
                  return VoxelShapes.cuboid(other.getBoundingBox());
               }
            }
            if (includeWorldBorder && !this.consumedWorldBorder && entity != null) {
               this.consumedWorldBorder = true;
               WorldBorder border = entity.getWorld().getWorldBorder();
               if (!isWithinWorldBorder(border, box) && isWithinWorldBorder(border, entity.getBoundingBox())) {
                  return border.asVoxelShape();
               }
            }
            return this.endOfData();
         }
      };
   }

   public static boolean isWithinWorldBorder(WorldBorder border, Box box) {
      double minX = Math.floor(border.getBoundWest());
      double minZ = Math.floor(border.getBoundNorth());
      double maxX = Math.ceil(border.getBoundEast());
      double maxZ = Math.ceil(border.getBoundSouth());
      return box.minX >= minX && box.minX <= maxX
         && box.minZ >= minZ && box.minZ <= maxZ
         && box.maxX >= minX && box.maxX <= maxX
         && box.maxZ >= minZ && box.maxZ <= maxZ;
   }

   private static boolean isBoxEmpty(Box box) {
      return box.getAverageSideLength() <= EPSILON;
   }

   public static boolean doesBoxCollideWithWorldBorder(CollisionView view, Entity entity, Box box) {
      if (isWithinWorldBorder(view.getWorldBorder(), box)) {
         return false;
      }
      VoxelShape borderShape = getWorldBorderCollision(view, entity, box);
      return borderShape != null
         && VoxelShapes.matchesAnywhere(borderShape, VoxelShapes.cuboid(box), BooleanBiFunction.AND);
   }

   @Nullable
   public static VoxelShape getWorldBorderCollision(CollisionView view, Entity entity, Box box) {
      WorldBorder border = view.getWorldBorder();
      return border.canCollide(entity, box) ? border.asVoxelShape() : null;
   }

   @Nullable
   public static VoxelShape getSupportingCollisionForEntity(World world, @Nullable Entity entity, Box entityBox) {
      if (entity instanceof SupportingBlockCollisionShapeProvider provider) {
         VoxelShape cached = provider.lithium$getCollisionShapeBelow();
         if (cached != null) {
            return cached;
         }
      }
      int x = MathHelper.floor((entityBox.minX + entityBox.maxX) * 0.5);
      int y = MathHelper.floor(entityBox.minY);
      int z = MathHelper.floor((entityBox.minZ + entityBox.maxZ) * 0.5);
      if (world.isOutOfHeightLimit(y)) {
         return null;
      }
      Chunk chunk = world.getChunk(
         Pos.ChunkCoord.fromBlockCoord(x), Pos.ChunkCoord.fromBlockCoord(z), ChunkStatus.FULL, false
      );
      if (chunk == null) {
         return null;
      }
      int sectionIndex = Pos.SectionYIndex.fromBlockCoord(world, y);
      ChunkSection[] sections = chunk.getSectionArray();
      if (sectionIndex < 0 || sectionIndex >= sections.length) {
         return null;
      }
      ChunkSection section = sections[sectionIndex];
      if (section == null || section.isEmpty()) {
         return null;
      }
      BlockPos pos = new BlockPos(x, y, z);
      return section.getBlockState(x & 15, y & 15, z & 15)
         .getCollisionShape(world, pos, entity == null ? ShapeContext.absent() : ShapeContext.of(entity));
   }

   public static boolean addLastBlockCollisionIfRequired(
      boolean required, ChunkAwareBlockCollisionSweeper sweeper, List<VoxelShape> collisions
   ) {
      if (required && sweeper.getLastCollision() != null) {
         collisions.add(sweeper.getLastCollision());
      }
      return false;
   }

   public static Box getSmallerBoxForSingleAxisMovement(Vec3d movement, Box box, double y, double x, double z) {
      double minX = box.minX;
      double minY = box.minY;
      double minZ = box.minZ;
      double maxX = box.maxX;
      double maxY = box.maxY;
      double maxZ = box.maxZ;
      if (y > 0.0) {
         minY = maxY;
         maxY += y;
      } else if (y < 0.0) {
         maxY = minY;
         minY += y;
      } else if (x > 0.0) {
         minX = maxX;
         maxX += x;
      } else if (x < 0.0) {
         maxX = minX;
         minX += x;
      } else if (z > 0.0) {
         minZ = maxZ;
         maxZ += z;
      } else if (z < 0.0) {
         maxZ = minZ;
         minZ += z;
      } else {
         return box.stretch(movement);
      }
      return new Box(minX, minY, minZ, maxX, maxY, maxZ);
   }

   public static boolean addEntityCollisionsIfRequired(
      boolean required, @Nullable Entity entity, World world, List<VoxelShape> collisions, Box movementSpace
   ) {
      if (required) {
         appendEntityCollisions(collisions, world, entity, movementSpace);
      }
      return false;
   }

   public static boolean addWorldBorderCollisionIfRequired(
      boolean required, @Nullable Entity entity, List<VoxelShape> collisions, Box movementSpace
   ) {
      if (required && entity != null) {
         appendWorldBorderCollision(collisions, entity, movementSpace);
      }
      return false;
   }

   public interface SupportingBlockCollisionShapeProvider {
      @Nullable
      VoxelShape lithium$getCollisionShapeBelow();
   }
}
