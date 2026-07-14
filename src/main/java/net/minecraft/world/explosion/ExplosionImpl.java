package net.minecraft.world.explosion;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.block.AbstractFireBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.caffeinemc.mods.lithium.common.util.Pos;
import net.caffeinemc.mods.lithium.common.world.ChunkView;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Util;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.profiler.Profilers;
import net.minecraft.world.GameRules;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.dimension.DimensionTypes;
import org.jetbrains.annotations.Nullable;

public class ExplosionImpl implements Explosion {
   private static final ExplosionBehavior DEFAULT_BEHAVIOR = new ExplosionBehavior();
   private static final int field_52618 = 16;
   private static final float field_52619 = 2.0F;
   private final boolean createFire;
   private final Explosion.DestructionType destructionType;
   private final ServerWorld world;
   private final Vec3d pos;
   @Nullable
   private final Entity entity;
   private final float power;
   private final DamageSource damageSource;
   private final ExplosionBehavior behavior;
   private final Map<PlayerEntity, Vec3d> knockbackByPlayer = new HashMap<>();
   private final BlockPos.Mutable lithium$cachedPos = new BlockPos.Mutable();
   private int lithium$previousChunkX = Integer.MIN_VALUE;
   private int lithium$previousChunkZ = Integer.MIN_VALUE;
   @Nullable
   private Chunk lithium$previousChunk;
   private final int lithium$bottomY;
   private final int lithium$topY;
   private final boolean lithium$explodeAirBlocks;

   public ExplosionImpl(
      ServerWorld world,
      @Nullable Entity entity,
      @Nullable DamageSource damageSource,
      @Nullable ExplosionBehavior behavior,
      Vec3d pos,
      float power,
      boolean createFire,
      Explosion.DestructionType destructionType
   ) {
      this.world = world;
      this.entity = entity;
      this.power = power;
      this.pos = pos;
      this.createFire = createFire;
      this.destructionType = destructionType;
      this.damageSource = damageSource == null ? world.getDamageSources().explosion(this) : damageSource;
      this.behavior = behavior == null ? this.makeBehavior(entity) : behavior;
      this.lithium$bottomY = world.getBottomY();
      this.lithium$topY = world.getTopYInclusive();
      int overestimatedRange = 8 + (int)(6.0F * power);
      this.lithium$explodeAirBlocks = createFire
         || world.getRegistryKey() == World.END
            && world.getDimensionEntry().matchesKey(DimensionTypes.THE_END)
            && overestimatedRange > Math.abs(pos.x)
            && overestimatedRange > Math.abs(pos.z);
   }

   private ExplosionBehavior makeBehavior(@Nullable Entity entity) {
      return entity == null ? DEFAULT_BEHAVIOR : new EntityExplosionBehavior(entity);
   }

   public static float calculateReceivedDamage(Vec3d pos, Entity entity) {
      Box box = entity.getBoundingBox();
      double d = 1.0 / ((box.maxX - box.minX) * 2.0 + 1.0);
      double e = 1.0 / ((box.maxY - box.minY) * 2.0 + 1.0);
      double f = 1.0 / ((box.maxZ - box.minZ) * 2.0 + 1.0);
      double g = (1.0 - Math.floor(1.0 / d) * d) / 2.0;
      double h = (1.0 - Math.floor(1.0 / f) * f) / 2.0;
      if (!(d < 0.0) && !(e < 0.0) && !(f < 0.0)) {
         int i = 0;
         int j = 0;
         LithiumExposureRaycaster raycaster = new LithiumExposureRaycaster(entity, pos);

         for (double k = 0.0; k <= 1.0; k += d) {
            for (double l = 0.0; l <= 1.0; l += e) {
               for (double m = 0.0; m <= 1.0; m += f) {
                  double n = MathHelper.lerp(k, box.minX, box.maxX);
                  double o = MathHelper.lerp(l, box.minY, box.maxY);
                  double p = MathHelper.lerp(m, box.minZ, box.maxZ);
                  Vec3d vec3d = new Vec3d(n + g, o, p + h);
                  if (raycaster.isUnobstructed(vec3d)) {
                     i++;
                  }

                  j++;
               }
            }
         }

         return (float)i / j;
      } else {
         return 0.0F;
      }
   }

   @Override
   public float getPower() {
      return this.power;
   }

   @Override
   public Vec3d getPosition() {
      return this.pos;
   }

   private List<BlockPos> getBlocksToDestroy() {
      LongOpenHashSet touched = new LongOpenHashSet();
      for (int rayX = 0; rayX < 16; rayX++) {
         boolean xPlane = rayX == 0 || rayX == 15;
         double x = rayX / 15.0F * 2.0F - 1.0F;
         for (int rayY = 0; rayY < 16; rayY++) {
            boolean yPlane = rayY == 0 || rayY == 15;
            double y = rayY / 15.0F * 2.0F - 1.0F;
            for (int rayZ = 0; rayZ < 16; rayZ++) {
               if (xPlane || yPlane || rayZ == 0 || rayZ == 15) {
                  this.lithium$performBlockRay(x, y, rayZ / 15.0F * 2.0F - 1.0F, touched);
               }
            }
         }
      }

      ObjectArrayList<BlockPos> result = new ObjectArrayList<>(touched.size());
      LongIterator iterator = touched.iterator();
      while (iterator.hasNext()) {
         result.add(BlockPos.fromLong(iterator.nextLong()));
      }
      return result;
   }

   private void lithium$performBlockRay(double x, double y, double z, LongOpenHashSet touched) {
      double length = Math.sqrt(x * x + y * y + z * z);
      double stepX = x / length * 0.3;
      double stepY = y / length * 0.3;
      double stepZ = z / length * 0.3;
      float strength = this.power * (0.7F + this.world.random.nextFloat() * 0.6F);
      double currentX = this.pos.x;
      double currentY = this.pos.y;
      double currentZ = this.pos.z;
      int previousX = Integer.MIN_VALUE;
      int previousY = Integer.MIN_VALUE;
      int previousZ = Integer.MIN_VALUE;
      float previousResistance = 0.0F;

      while (strength > 0.0F) {
         int blockX = MathHelper.floor(currentX);
         int blockY = MathHelper.floor(currentY);
         int blockZ = MathHelper.floor(currentZ);
         float resistance;
         if (blockX == previousX && blockY == previousY && blockZ == previousZ) {
            resistance = previousResistance;
         } else {
            if (blockY < this.lithium$bottomY || blockY > this.lithium$topY || blockX < -30000000 || blockZ < -30000000
               || blockX >= 30000000 || blockZ >= 30000000) {
               return;
            }
            resistance = this.lithium$traverseBlock(strength, blockX, blockY, blockZ, touched);
            previousX = blockX;
            previousY = blockY;
            previousZ = blockZ;
            previousResistance = resistance;
         }

         strength -= resistance + 0.22500001F;
         currentX += stepX;
         currentY += stepY;
         currentZ += stepZ;
      }
   }

   private float lithium$traverseBlock(float strength, int x, int y, int z, LongOpenHashSet touched) {
      BlockPos.Mutable pos = this.lithium$cachedPos.set(x, y, z);
      int chunkX = Pos.ChunkCoord.fromBlockCoord(x);
      int chunkZ = Pos.ChunkCoord.fromBlockCoord(z);
      if (chunkX != this.lithium$previousChunkX || chunkZ != this.lithium$previousChunkZ) {
         this.lithium$previousChunk = this.world.getChunk(chunkX, chunkZ);
         this.lithium$previousChunkX = chunkX;
         this.lithium$previousChunkZ = chunkZ;
      }

      BlockState state = Blocks.AIR.getDefaultState();
      FluidState fluid = state.getFluidState();
      Chunk chunk = this.lithium$previousChunk;
      if (chunk != null) {
         int index = Pos.SectionYIndex.fromBlockCoord(chunk, y);
         ChunkSection[] sections = chunk.getSectionArray();
         if (index >= 0 && index < sections.length) {
            ChunkSection section = sections[index];
            if (section != null && !section.isEmpty()) {
               state = section.getBlockState(x & 15, y & 15, z & 15);
               fluid = state.getFluidState();
            }
         }
      }

      float resistance = this.behavior.getBlastResistance(this, this.world, pos, state, fluid)
         .map(value -> (value + 0.3F) * 0.3F)
         .orElse(0.0F);
      float reducedStrength = strength - resistance;
      if (reducedStrength > 0.0F && (!state.isAir() || this.lithium$explodeAirBlocks)
         && this.behavior.canDestroyBlock(this, this.world, pos, state, reducedStrength)) {
         touched.add(pos.asLong());
      }
      return resistance;
   }

   private static final class LithiumExposureRaycaster {
      private final World world;
      private final ChunkView chunks;
      private final RaycastContext context;
      private int chunkX = Integer.MIN_VALUE;
      private int chunkZ = Integer.MIN_VALUE;
      @Nullable
      private Chunk chunk;

      LithiumExposureRaycaster(Entity entity, Vec3d end) {
         this.world = entity.getWorld();
         this.chunks = (ChunkView)this.world;
         this.context = new RaycastContext(Vec3d.ZERO, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, entity);
      }

      boolean isUnobstructed(Vec3d start) {
         this.context.lithium$setStart(start);
         return !BlockView.raycast(start, this.context.getEnd(), this.context, this::hitsBlock, ignored -> false);
      }

      private Boolean hitsBlock(RaycastContext context, BlockPos pos) {
         BlockState state = this.getBlockState(pos);
         return context.getBlockShape(state, this.world, pos).raycast(context.getStart(), context.getEnd(), pos) == null ? null : Boolean.TRUE;
      }

      private BlockState getBlockState(BlockPos pos) {
         if (this.world.isOutOfHeightLimit(pos)) {
            return Blocks.VOID_AIR.getDefaultState();
         }
         int nextX = Pos.ChunkCoord.fromBlockCoord(pos.getX());
         int nextZ = Pos.ChunkCoord.fromBlockCoord(pos.getZ());
         if (nextX != this.chunkX || nextZ != this.chunkZ) {
            this.chunk = this.chunks.lithium$getLoadedChunk(nextX, nextZ);
            this.chunkX = nextX;
            this.chunkZ = nextZ;
         }
         if (this.chunk != null) {
            int index = Pos.SectionYIndex.fromBlockCoord(this.chunk, pos.getY());
            ChunkSection[] sections = this.chunk.getSectionArray();
            if (index >= 0 && index < sections.length) {
               ChunkSection section = sections[index];
               if (section != null && !section.isEmpty()) {
                  return section.getBlockState(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15);
               }
            }
         }
         return Blocks.AIR.getDefaultState();
      }
   }

   private void damageEntities() {
      float f = this.power * 2.0F;
      int i = MathHelper.floor(this.pos.x - f - 1.0);
      int j = MathHelper.floor(this.pos.x + f + 1.0);
      int k = MathHelper.floor(this.pos.y - f - 1.0);
      int l = MathHelper.floor(this.pos.y + f + 1.0);
      int m = MathHelper.floor(this.pos.z - f - 1.0);
      int n = MathHelper.floor(this.pos.z + f + 1.0);

      for (Entity entity : this.world.getOtherEntities(this.entity, new Box(i, k, m, j, l, n))) {
         if (!entity.isImmuneToExplosion(this)) {
            double d = Math.sqrt(entity.squaredDistanceTo(this.pos)) / f;
            if (d <= 1.0) {
               double e = entity.getX() - this.pos.x;
               double g = (entity instanceof TntEntity ? entity.getY() : entity.getEyeY()) - this.pos.y;
               double h = entity.getZ() - this.pos.z;
               double o = Math.sqrt(e * e + g * g + h * h);
               if (o != 0.0) {
                  e /= o;
                  g /= o;
                  h /= o;
                  boolean bl = this.behavior.shouldDamage(this, entity);
                  float p = this.behavior.getKnockbackModifier(entity);
                  float q = !bl && p == 0.0F ? 0.0F : calculateReceivedDamage(this.pos, entity);
                  if (bl) {
                     entity.damage(this.world, this.damageSource, this.behavior.calculateDamage(this, entity, q));
                  }

                  double r = (1.0 - d) * q * p;
                  double s;
                  if (entity instanceof LivingEntity livingEntity) {
                     s = r * (1.0 - livingEntity.getAttributeValue(EntityAttributes.EXPLOSION_KNOCKBACK_RESISTANCE));
                  } else {
                     s = r;
                  }

                  e *= s;
                  g *= s;
                  h *= s;
                  Vec3d vec3d = new Vec3d(e, g, h);
                  entity.addVelocity(vec3d);
                  if (entity instanceof PlayerEntity playerEntity
                     && !playerEntity.isSpectator()
                     && (!playerEntity.isCreative() || !playerEntity.getAbilities().flying)) {
                     this.knockbackByPlayer.put(playerEntity, vec3d);
                  }

                  entity.onExplodedBy(this.entity);
               }
            }
         }
      }
   }

   private void destroyBlocks(List<BlockPos> positions) {
      List<ExplosionImpl.DroppedItem> list = new ArrayList<>();
      Util.shuffle(positions, this.world.random);

      for (BlockPos blockPos : positions) {
         this.world.getBlockState(blockPos).onExploded(this.world, blockPos, this, (item, pos) -> addDroppedItem(list, item, pos));
      }

      for (ExplosionImpl.DroppedItem droppedItem : list) {
         Block.dropStack(this.world, droppedItem.pos, droppedItem.item);
      }
   }

   private void createFire(List<BlockPos> positions) {
      for (BlockPos blockPos : positions) {
         if (this.world.random.nextInt(3) == 0 && this.world.getBlockState(blockPos).isAir() && this.world.getBlockState(blockPos.down()).isOpaqueFullCube()) {
            this.world.setBlockState(blockPos, AbstractFireBlock.getState(this.world, blockPos));
         }
      }
   }

   public void explode() {
      this.world.emitGameEvent(this.entity, GameEvent.EXPLODE, this.pos);
      List<BlockPos> list = this.getBlocksToDestroy();
      this.damageEntities();
      if (this.shouldDestroyBlocks()) {
         Profiler profiler = Profilers.get();
         profiler.push("explosion_blocks");
         this.destroyBlocks(list);
         profiler.pop();
      }

      if (this.createFire) {
         this.createFire(list);
      }
   }

   private static void addDroppedItem(List<ExplosionImpl.DroppedItem> droppedItemsOut, ItemStack item, BlockPos pos) {
      for (ExplosionImpl.DroppedItem droppedItem : droppedItemsOut) {
         droppedItem.merge(item);
         if (item.isEmpty()) {
            return;
         }
      }

      droppedItemsOut.add(new ExplosionImpl.DroppedItem(pos, item));
   }

   private boolean shouldDestroyBlocks() {
      return this.destructionType != Explosion.DestructionType.KEEP;
   }

   public Map<PlayerEntity, Vec3d> getKnockbackByPlayer() {
      return this.knockbackByPlayer;
   }

   @Override
   public ServerWorld getWorld() {
      return this.world;
   }

   @Nullable
   @Override
   public LivingEntity getCausingEntity() {
      return Explosion.getCausingEntity(this.entity);
   }

   @Nullable
   @Override
   public Entity getEntity() {
      return this.entity;
   }

   public DamageSource getDamageSource() {
      return this.damageSource;
   }

   @Override
   public Explosion.DestructionType getDestructionType() {
      return this.destructionType;
   }

   @Override
   public boolean canTriggerBlocks() {
      if (this.destructionType != Explosion.DestructionType.TRIGGER_BLOCK) {
         return false;
      } else {
         return this.entity != null && this.entity.getType() == EntityType.BREEZE_WIND_CHARGE
            ? this.world.getGameRules().getBoolean(GameRules.DO_MOB_GRIEFING)
            : true;
      }
   }

   @Override
   public boolean preservesDecorativeEntities() {
      boolean bl = this.world.getGameRules().getBoolean(GameRules.DO_MOB_GRIEFING);
      boolean bl2 = this.entity == null || !this.entity.isTouchingWater();
      boolean bl3 = this.entity == null || this.entity.getType() != EntityType.BREEZE_WIND_CHARGE && this.entity.getType() != EntityType.WIND_CHARGE;
      return bl ? bl2 && bl3 : this.destructionType.destroysBlocks() && bl2 && bl3;
   }

   public boolean isSmall() {
      return this.power < 2.0F || !this.shouldDestroyBlocks();
   }

   static class DroppedItem {
      final BlockPos pos;
      ItemStack item;

      DroppedItem(BlockPos pos, ItemStack item) {
         this.pos = pos;
         this.item = item;
      }

      public void merge(ItemStack other) {
         if (ItemEntity.canMerge(this.item, other)) {
            this.item = ItemEntity.merge(this.item, other, 16);
         }
      }
   }
}
