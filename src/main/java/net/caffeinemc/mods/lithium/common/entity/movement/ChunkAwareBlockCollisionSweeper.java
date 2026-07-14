package net.caffeinemc.mods.lithium.common.entity.movement;

import com.google.common.collect.AbstractIterator;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import net.caffeinemc.mods.lithium.common.block.BlockCountingSection;
import net.caffeinemc.mods.lithium.common.block.BlockStateFlags;
import net.caffeinemc.mods.lithium.common.shapes.VoxelShapeCaster;
import net.caffeinemc.mods.lithium.common.util.Pos;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.Entity;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ChunkStatus;
import org.jetbrains.annotations.Nullable;

public class ChunkAwareBlockCollisionSweeper extends AbstractIterator<VoxelShape> {
   private static final double EPSILON = 1.0E-7;
   private final BlockPos.Mutable pos = new BlockPos.Mutable();
   private final Box box;
   private final VoxelShape shape;
   private final World world;
   private final ShapeContext context;
   private final int minX;
   private final int minY;
   private final int minZ;
   private final int maxX;
   private final int maxY;
   private final int maxZ;
   private int chunkX;
   private int chunkYIndex;
   private int chunkZ;
   private int sectionStartX;
   private int sectionStartZ;
   private int sectionEndX;
   private int sectionEndZ;
   private int currentX;
   private int currentY;
   private int currentZ;
   private int maxHitX = Integer.MIN_VALUE;
   private int maxHitY = Integer.MIN_VALUE;
   private int maxHitZ = Integer.MIN_VALUE;
   private VoxelShape maxShape;
   private final boolean hideLastCollision;
   private int sectionSize;
   private int sectionIndex;
   private boolean sectionHasOversizedBlocks;
   @Nullable
   private Chunk cachedChunk;
   @Nullable
   private ChunkSection cachedSection;

   public ChunkAwareBlockCollisionSweeper(World world, @Nullable Entity entity, Box box) {
      this(world, entity, box, false);
   }

   public ChunkAwareBlockCollisionSweeper(World world, @Nullable Entity entity, Box box, boolean hideLastCollision) {
      this.box = box;
      this.shape = VoxelShapes.cuboid(box);
      this.context = entity == null ? ShapeContext.absent() : ShapeContext.of(entity);
      this.world = world;
      this.minX = MathHelper.floor(box.minX - EPSILON);
      this.maxX = MathHelper.floor(box.maxX + EPSILON);
      this.minY = MathHelper.clamp(MathHelper.floor(box.minY - EPSILON), world.getBottomY(), world.getTopYInclusive());
      this.maxY = MathHelper.clamp(MathHelper.floor(box.maxY + EPSILON), world.getBottomY(), world.getTopYInclusive());
      this.minZ = MathHelper.floor(box.minZ - EPSILON);
      this.maxZ = MathHelper.floor(box.maxZ + EPSILON);
      this.chunkX = Pos.ChunkCoord.fromBlockCoord(this.minX - 1) - 1;
      this.chunkZ = Pos.ChunkCoord.fromBlockCoord(this.minZ - 1);
      this.hideLastCollision = hideLastCollision;
   }

   @Nullable
   public VoxelShape getLastCollision() {
      return this.maxShape;
   }

   public Iterator<VoxelShape> getLastCollisionIterator() {
      return new Iterator<>() {
         @Override
         public boolean hasNext() {
            return ChunkAwareBlockCollisionSweeper.this.hideLastCollision && ChunkAwareBlockCollisionSweeper.this.maxShape != null;
         }

         @Override
         public VoxelShape next() {
            if (!this.hasNext()) {
               throw new NoSuchElementException();
            }
            VoxelShape result = ChunkAwareBlockCollisionSweeper.this.maxShape;
            ChunkAwareBlockCollisionSweeper.this.maxShape = null;
            return result;
         }
      };
   }

   private boolean nextSection() {
      while (true) {
         if (this.cachedChunk != null
            && this.chunkYIndex < Pos.SectionYIndex.getMaxYSectionIndexInclusive(this.world)
            && this.chunkYIndex < Pos.SectionYIndex.fromBlockCoord(this.world, this.maxY + 1)) {
            this.chunkYIndex++;
            this.cachedSection = this.cachedChunk.getSectionArray()[this.chunkYIndex];
         } else {
            if (this.chunkX < Pos.ChunkCoord.fromBlockCoord(this.maxX + 1)) {
               this.chunkX++;
            } else {
               if (this.chunkZ >= Pos.ChunkCoord.fromBlockCoord(this.maxZ + 1)) {
                  return false;
               }
               this.chunkX = Pos.ChunkCoord.fromBlockCoord(this.minX - 1);
               this.chunkZ++;
            }

            this.cachedChunk = this.world.getChunk(this.chunkX, this.chunkZ, ChunkStatus.FULL, false);
            if (this.cachedChunk != null) {
               this.chunkYIndex = MathHelper.clamp(
                  Pos.SectionYIndex.fromBlockCoord(this.world, this.minY - 1),
                  Pos.SectionYIndex.getMinYSectionIndex(this.world),
                  Pos.SectionYIndex.getMaxYSectionIndexInclusive(this.world)
               );
               this.cachedSection = this.cachedChunk.getSectionArray()[this.chunkYIndex];
            } else {
               this.cachedSection = null;
            }
         }

         if (this.cachedSection != null && !this.cachedSection.isEmpty()) {
            this.sectionHasOversizedBlocks = this.hasOversizedBlocks(this.cachedSection);
            int extension = this.sectionHasOversizedBlocks ? 1 : 0;
            this.sectionEndX = Math.min(this.maxX + extension, Pos.BlockCoord.getMaxInSectionCoord(this.chunkX));
            int endY = Math.min(this.maxY + extension, Pos.BlockCoord.getMaxYInSectionIndex(this.world, this.chunkYIndex));
            this.sectionEndZ = Math.min(this.maxZ + extension, Pos.BlockCoord.getMaxInSectionCoord(this.chunkZ));
            this.sectionStartX = Math.max(this.minX - extension, Pos.BlockCoord.getMinInSectionCoord(this.chunkX));
            int startY = Math.max(this.minY - extension, Pos.BlockCoord.getMinYInSectionIndex(this.world, this.chunkYIndex));
            this.sectionStartZ = Math.max(this.minZ - extension, Pos.BlockCoord.getMinInSectionCoord(this.chunkZ));
            this.currentX = this.sectionStartX;
            this.currentY = startY;
            this.currentZ = this.sectionStartZ;
            this.sectionSize = (this.sectionEndX - this.sectionStartX + 1)
               * (endY - startY + 1)
               * (this.sectionEndZ - this.sectionStartZ + 1);
            if (this.sectionSize > 0) {
               this.sectionIndex = 0;
               return true;
            }
         }
      }
   }

   @Override
   public VoxelShape computeNext() {
      while (this.sectionIndex < this.sectionSize || this.nextSection()) {
         this.sectionIndex++;
         int x = this.currentX;
         int y = this.currentY;
         int z = this.currentZ;
         if (this.currentX < this.sectionEndX) {
            this.currentX++;
         } else if (this.currentZ < this.sectionEndZ) {
            this.currentX = this.sectionStartX;
            this.currentZ++;
         } else {
            this.currentX = this.sectionStartX;
            this.currentZ = this.sectionStartZ;
            this.currentY++;
         }

         int edgesHit = this.sectionHasOversizedBlocks
            ? (x >= this.minX && x <= this.maxX ? 0 : 1)
               + (y >= this.minY && y <= this.maxY ? 0 : 1)
               + (z >= this.minZ && z <= this.maxZ ? 0 : 1)
            : 0;
         if (edgesHit == 3) {
            continue;
         }
         BlockState state = this.cachedSection.getBlockState(x & 15, y & 15, z & 15);
         if (!canInteractWithBlock(state, edgesHit)) {
            continue;
         }
         this.pos.set(x, y, z);
         VoxelShape collisionShape = this.context.getCollisionShape(state, this.world, this.pos);
         if (collisionShape == null || collisionShape == VoxelShapes.empty()) {
            continue;
         }
         VoxelShape collidedShape = getCollidedShape(this.box, this.shape, collisionShape, x, y, z);
         if (collidedShape == null) {
            continue;
         }
         if (z >= this.maxHitZ && (z > this.maxHitZ || y >= this.maxHitY && (y > this.maxHitY || x > this.maxHitX))) {
            this.maxHitX = x;
            this.maxHitY = y;
            this.maxHitZ = z;
            VoxelShape previous = this.maxShape;
            this.maxShape = collidedShape;
            if (previous == null) {
               continue;
            }
            return previous;
         }
         return collidedShape;
      }

      if (!this.hideLastCollision && this.maxShape != null) {
         VoxelShape result = this.maxShape;
         this.maxShape = null;
         return result;
      }
      return this.endOfData();
   }

   private boolean hasOversizedBlocks(ChunkSection section) {
      return !BlockStateFlags.ENABLED || ((BlockCountingSection)section).lithium$mayContainAny(BlockStateFlags.OVERSIZED_SHAPE);
   }

   private static boolean canInteractWithBlock(BlockState state, int edgesHit) {
      return (edgesHit != 1 || state.exceedsCube()) && (edgesHit != 2 || state.isOf(Blocks.MOVING_PISTON));
   }

   @Nullable
   private static VoxelShape getCollidedShape(Box entityBox, VoxelShape entityShape, VoxelShape shape, int x, int y, int z) {
      if (shape == VoxelShapes.fullCube()) {
         return entityBox.intersects(x, y, z, x + 1.0, y + 1.0, z + 1.0) ? shape.offset(x, y, z) : null;
      }
      if (shape instanceof VoxelShapeCaster caster) {
         return caster.intersects(entityBox, x, y, z) ? shape.offset(x, y, z) : null;
      }
      VoxelShape offsetShape = shape.offset(x, y, z);
      return VoxelShapes.matchesAnywhere(offsetShape, entityShape, BooleanBiFunction.AND) ? offsetShape : null;
   }

   public List<VoxelShape> collectAll() {
      ArrayList<VoxelShape> collisions = new ArrayList<>();
      while (this.hasNext()) {
         collisions.add(this.next());
      }
      return collisions;
   }
}
