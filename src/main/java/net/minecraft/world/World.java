package net.minecraft.world;

import net.caffeinemc.mods.lithium.common.shapes.VoxelShapeHelper;
import net.caffeinemc.mods.lithium.common.entity.LithiumEntityCollisions;
import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.caffeinemc.mods.lithium.common.world.LithiumData;
import net.caffeinemc.mods.lithium.common.world.blockentity.BlockEntityGetter;
import net.minecraft.block.AbstractFireBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.type.FireworkExplosionComponent;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.boss.dragon.EnderDragonPart;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageSources;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.FuelRegistry;
import net.minecraft.item.ItemStack;
import net.minecraft.item.map.MapState;
import net.minecraft.network.packet.Packet;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.recipe.BrewingRecipeRegistry;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ChunkLevelType;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.TypeFilter;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import net.minecraft.util.function.LazyIterationConsumer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.profiler.Profilers;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.block.ChainRestrictedNeighborUpdater;
import net.minecraft.world.block.NeighborUpdater;
import net.minecraft.world.block.WireOrientation;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.chunk.BlockEntityTickInvoker;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.light.LightingProvider;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.entity.EntityLookup;
import net.minecraft.world.entity.SectionedEntityCache;
import net.minecraft.world.entity.SimpleEntityLookup;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.explosion.Explosion;
import net.minecraft.world.explosion.ExplosionBehavior;
import net.minecraft.world.tick.TickManager;
import org.jetbrains.annotations.Nullable;

public abstract class World implements WorldAccess, AutoCloseable, LithiumData, BlockEntityGetter {
   public static final Codec<RegistryKey<World>> CODEC = RegistryKey.createCodec(RegistryKeys.WORLD);
   public static final RegistryKey<World> OVERWORLD = RegistryKey.of(RegistryKeys.WORLD, Identifier.ofVanilla("overworld"));
   public static final RegistryKey<World> NETHER = RegistryKey.of(RegistryKeys.WORLD, Identifier.ofVanilla("the_nether"));
   public static final RegistryKey<World> END = RegistryKey.of(RegistryKeys.WORLD, Identifier.ofVanilla("the_end"));
   public static final int HORIZONTAL_LIMIT = 30000000;
   public static final int MAX_UPDATE_DEPTH = 512;
   public static final int field_30967 = 32;
   public static final int field_30968 = 15;
   public static final int field_30969 = 24000;
   public static final int MAX_Y = 20000000;
   public static final int MIN_Y = -20000000;
   protected final List<BlockEntityTickInvoker> blockEntityTickers = Lists.newArrayList();
   protected final NeighborUpdater neighborUpdater;
   private final List<BlockEntityTickInvoker> pendingBlockEntityTickers = Lists.newArrayList();
   private boolean iteratingTickingBlockEntities;
   private final Thread thread;
   private final boolean debugWorld;
   private int ambientDarkness;
   protected int lcgBlockSeed = Random.create().nextInt();
   protected final int lcgBlockSeedIncrement = 1013904223;
   protected float rainGradientPrev;
   protected float rainGradient;
   protected float thunderGradientPrev;
   protected float thunderGradient;
   public final Random random = Random.create();
   @Deprecated
   private final Random threadSafeRandom = Random.createThreadSafe();
   private final RegistryEntry<DimensionType> dimensionEntry;
   protected final MutableWorldProperties properties;
   public final boolean isClient;
   private final WorldBorder border;
   private final BiomeAccess biomeAccess;
   private final RegistryKey<World> registryKey;
   private final DynamicRegistryManager registryManager;
   private final DamageSources damageSources;
   private long tickOrder;
   private final LithiumData.Data lithium$data = new LithiumData.Data();

   protected World(
      MutableWorldProperties properties,
      RegistryKey<World> registryRef,
      DynamicRegistryManager registryManager,
      RegistryEntry<DimensionType> dimensionEntry,
      boolean isClient,
      boolean debugWorld,
      long seed,
      int maxChainedNeighborUpdates
   ) {
      this.properties = properties;
      this.dimensionEntry = dimensionEntry;
      final DimensionType dimensionType = dimensionEntry.value();
      this.registryKey = registryRef;
      this.isClient = isClient;
      if (dimensionType.coordinateScale() != 1.0) {
         this.border = new WorldBorder() {
            @Override
            public double getCenterX() {
               return super.getCenterX() / dimensionType.coordinateScale();
            }

            @Override
            public double getCenterZ() {
               return super.getCenterZ() / dimensionType.coordinateScale();
            }
         };
      } else {
         this.border = new WorldBorder();
      }

      this.thread = Thread.currentThread();
      this.biomeAccess = new BiomeAccess(this, seed);
      this.debugWorld = debugWorld;
      this.neighborUpdater = new ChainRestrictedNeighborUpdater(this, maxChainedNeighborUpdates);
      this.registryManager = registryManager;
      this.damageSources = new DamageSources(registryManager);
   }

   @Override
   public boolean isClient() {
      return this.isClient;
   }

   @Override
   public LithiumData.Data lithium$getData() {
      return this.lithium$data;
   }

   @Nullable
   @Override
   public MinecraftServer getServer() {
      return null;
   }

   public boolean isInBuildLimit(BlockPos pos) {
      return !this.isOutOfHeightLimit(pos) && isValidHorizontally(pos);
   }

   public static boolean isValid(BlockPos pos) {
      return !isInvalidVertically(pos.getY()) && isValidHorizontally(pos);
   }

   private static boolean isValidHorizontally(BlockPos pos) {
      return pos.getX() >= -30000000 && pos.getZ() >= -30000000 && pos.getX() < 30000000 && pos.getZ() < 30000000;
   }

   private static boolean isInvalidVertically(int y) {
      return y < -20000000 || y >= 20000000;
   }

   public WorldChunk getWorldChunk(BlockPos pos) {
      return this.getChunk(ChunkSectionPos.getSectionCoord(pos.getX()), ChunkSectionPos.getSectionCoord(pos.getZ()));
   }

   @Override
   public Chunk getChunk(BlockPos pos) {
      return this.getChunkManager().getChunk(
         ChunkSectionPos.getSectionCoord(pos.getX()), ChunkSectionPos.getSectionCoord(pos.getZ()), ChunkStatus.FULL, true
      );
   }

   public WorldChunk getChunk(int i, int j) {
      return (WorldChunk)this.getChunk(i, j, ChunkStatus.FULL);
   }

   @Override
   public Chunk getChunk(int chunkX, int chunkZ, ChunkStatus status) {
      return this.getChunkManager().getChunk(chunkX, chunkZ, status, true);
   }

   @Nullable
   @Override
   public Chunk getChunk(int chunkX, int chunkZ, ChunkStatus leastStatus, boolean create) {
      Chunk chunk = this.getChunkManager().getChunk(chunkX, chunkZ, leastStatus, create);
      if (chunk == null && create) {
         throw new IllegalStateException("Should always be able to create a chunk!");
      } else {
         return chunk;
      }
   }

   @Override
   public boolean setBlockState(BlockPos pos, BlockState state, int flags) {
      return this.setBlockState(pos, state, flags, 512);
   }

   @Override
   public boolean setBlockState(BlockPos pos, BlockState state, int flags, int maxUpdateDepth) {
      if (this.isOutOfHeightLimit(pos)) {
         return false;
      }

      if (!this.isClient && this.isDebugWorld()) {
         return false;
      }

      WorldChunk worldChunk = this.getWorldChunk(pos);
      Block block = state.getBlock();
      BlockState blockState = worldChunk.setBlockState(pos, state, (flags & 64) != 0);
      if (blockState == null) {
         return false;
      }

      BlockState blockState2 = this.getBlockState(pos);
      if (blockState2 == state) {
         if (blockState != blockState2) {
            this.scheduleBlockRerenderIfNeeded(pos, blockState, blockState2);
         }

         if ((flags & 2) != 0
            && (!this.isClient || (flags & 4) == 0)
            && (this.isClient || worldChunk.getLevelType() != null && worldChunk.getLevelType().isAfter(ChunkLevelType.BLOCK_TICKING))) {
            this.updateListeners(pos, blockState, state, flags);
         }

         if ((flags & 1) != 0) {
            this.updateNeighbors(pos, blockState.getBlock());
            if (!this.isClient && state.hasComparatorOutput()) {
               this.updateComparators(pos, block);
            }
         }

         if ((flags & 16) == 0 && maxUpdateDepth > 0) {
            int i = flags & -34;
            blockState.prepare(this, pos, i, maxUpdateDepth - 1);
            state.updateNeighbors(this, pos, i, maxUpdateDepth - 1);
            state.prepare(this, pos, i, maxUpdateDepth - 1);
         }

         this.onBlockChanged(pos, blockState, blockState2);
      }

      return true;
   }

   public void onBlockChanged(BlockPos pos, BlockState oldBlock, BlockState newBlock) {
   }

   @Override
   public boolean removeBlock(BlockPos pos, boolean move) {
      FluidState fluidState = this.getFluidState(pos);
      return this.setBlockState(pos, fluidState.getBlockState(), 3 | (move ? 64 : 0));
   }

   @Override
   public boolean breakBlock(BlockPos pos, boolean drop, @Nullable Entity breakingEntity, int maxUpdateDepth) {
      BlockState blockState = this.getBlockState(pos);
      if (blockState.isAir()) {
         return false;
      }

      FluidState fluidState = this.getFluidState(pos);
      if (!(blockState.getBlock() instanceof AbstractFireBlock)) {
         this.syncWorldEvent(2001, pos, Block.getRawIdFromState(blockState));
      }

      if (drop) {
         BlockEntity blockEntity = blockState.hasBlockEntity() ? this.getBlockEntity(pos) : null;
         Block.dropStacks(blockState, this, pos, blockEntity, breakingEntity, ItemStack.EMPTY);
      }

      boolean bl = this.setBlockState(pos, fluidState.getBlockState(), 3, maxUpdateDepth);
      if (bl) {
         this.emitGameEvent(GameEvent.BLOCK_DESTROY, pos, GameEvent.Emitter.of(breakingEntity, blockState));
      }

      return bl;
   }

   public void addBlockBreakParticles(BlockPos pos, BlockState state) {
   }

   public boolean setBlockState(BlockPos pos, BlockState state) {
      return this.setBlockState(pos, state, 3);
   }

   public abstract void updateListeners(BlockPos pos, BlockState oldState, BlockState newState, int flags);

   public void scheduleBlockRerenderIfNeeded(BlockPos pos, BlockState old, BlockState updated) {
   }

   public void updateNeighborsAlways(BlockPos pos, Block block) {
   }

   public void updateNeighborsAlways(BlockPos pos, Block sourceBlock, @Nullable WireOrientation orientation) {
   }

   public void updateNeighborsExcept(BlockPos pos, Block sourceBlock, Direction direction, @Nullable WireOrientation orientation) {
   }

   public void updateNeighbor(BlockPos pos, Block sourceBlock, @Nullable WireOrientation orientation) {
   }

   public void updateNeighbor(BlockState state, BlockPos pos, Block sourceBlock, @Nullable WireOrientation orientation, boolean notify) {
   }

   @Override
   public void replaceWithStateForNeighborUpdate(
      Direction direction, BlockPos pos, BlockPos neighborPos, BlockState neighborState, int flags, int maxUpdateDepth
   ) {
      this.neighborUpdater.replaceWithStateForNeighborUpdate(direction, neighborState, pos, neighborPos, flags, maxUpdateDepth);
   }

   @Override
   public int getTopY(Heightmap.Type heightmap, int x, int z) {
      int i;
      if (x >= -30000000 && z >= -30000000 && x < 30000000 && z < 30000000) {
         if (this.isChunkLoaded(ChunkSectionPos.getSectionCoord(x), ChunkSectionPos.getSectionCoord(z))) {
            i = this.getChunk(ChunkSectionPos.getSectionCoord(x), ChunkSectionPos.getSectionCoord(z)).sampleHeightmap(heightmap, x & 15, z & 15) + 1;
         } else {
            i = this.getBottomY();
         }
      } else {
         i = this.getSeaLevel() + 1;
      }

      return i;
   }

   @Override
   public LightingProvider getLightingProvider() {
      return this.getChunkManager().getLightingProvider();
   }

   @Override
   public BlockState getBlockState(BlockPos pos) {
      if (this.isOutOfHeightLimit(pos)) {
         return Blocks.VOID_AIR.getDefaultState();
      }

      WorldChunk worldChunk = this.getChunk(ChunkSectionPos.getSectionCoord(pos.getX()), ChunkSectionPos.getSectionCoord(pos.getZ()));
      if (this.isDebugWorld()) {
         return worldChunk.getBlockState(pos);
      }
      ChunkSection[] sections = worldChunk.getSectionArray();
      int sectionIndex = this.getSectionIndex(pos.getY());
      if (sectionIndex < 0 || sectionIndex >= sections.length || worldChunk.isEmpty()) {
         return Blocks.VOID_AIR.getDefaultState();
      }
      ChunkSection section = sections[sectionIndex];
      return section != null && !section.isEmpty()
         ? section.getBlockState(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15)
         : Blocks.AIR.getDefaultState();
   }

   @Override
   public FluidState getFluidState(BlockPos pos) {
      if (this.isOutOfHeightLimit(pos)) {
         return Fluids.EMPTY.getDefaultState();
      }

      WorldChunk worldChunk = this.getWorldChunk(pos);
      return worldChunk.getFluidState(pos);
   }

   public boolean isDay() {
      return !this.getDimension().hasFixedTime() && this.ambientDarkness < 4;
   }

   public boolean isNight() {
      return !this.getDimension().hasFixedTime() && !this.isDay();
   }

   public void playSound(@Nullable Entity source, BlockPos pos, SoundEvent sound, SoundCategory category, float volume, float pitch) {
      this.playSound(source instanceof PlayerEntity playerEntity ? playerEntity : null, pos, sound, category, volume, pitch);
   }

   @Override
   public void playSound(@Nullable PlayerEntity source, BlockPos pos, SoundEvent sound, SoundCategory category, float volume, float pitch) {
      this.playSound(source, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, sound, category, volume, pitch);
   }

   public abstract void playSound(
      @Nullable PlayerEntity source,
      double x,
      double y,
      double z,
      RegistryEntry<SoundEvent> sound,
      SoundCategory category,
      float volume,
      float pitch,
      long seed
   );

   public void playSound(
      @Nullable PlayerEntity source, double x, double y, double z, SoundEvent sound, SoundCategory category, float volume, float pitch, long seed
   ) {
      this.playSound(source, x, y, z, Registries.SOUND_EVENT.getEntry(sound), category, volume, pitch, seed);
   }

   public abstract void playSoundFromEntity(
      @Nullable PlayerEntity source, Entity entity, RegistryEntry<SoundEvent> sound, SoundCategory category, float volume, float pitch, long seed
   );

   public void playSound(@Nullable PlayerEntity source, double x, double y, double z, SoundEvent sound, SoundCategory category) {
      this.playSound(source, x, y, z, sound, category, 1.0F, 1.0F);
   }

   public void playSound(@Nullable PlayerEntity source, double x, double y, double z, SoundEvent sound, SoundCategory category, float volume, float pitch) {
      this.playSound(source, x, y, z, sound, category, volume, pitch, this.threadSafeRandom.nextLong());
   }

   public void playSound(
      @Nullable PlayerEntity source, double x, double y, double z, RegistryEntry<SoundEvent> sound, SoundCategory category, float volume, float pitch
   ) {
      this.playSound(source, x, y, z, sound, category, volume, pitch, this.threadSafeRandom.nextLong());
   }

   public void playSoundFromEntity(@Nullable PlayerEntity source, Entity entity, SoundEvent sound, SoundCategory category, float volume, float pitch) {
      this.playSoundFromEntity(source, entity, Registries.SOUND_EVENT.getEntry(sound), category, volume, pitch, this.threadSafeRandom.nextLong());
   }

   public void playSoundAtBlockCenter(BlockPos pos, SoundEvent sound, SoundCategory category, float volume, float pitch, boolean useDistance) {
      this.playSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, sound, category, volume, pitch, useDistance);
   }

   public void playSoundFromEntity(Entity entity, SoundEvent sound, SoundCategory category, float volume, float pitch) {
   }

   public void playSound(double x, double y, double z, SoundEvent sound, SoundCategory category, float volume, float pitch, boolean useDistance) {
   }

   @Override
   public void addParticle(ParticleEffect parameters, double x, double y, double z, double velocityX, double velocityY, double velocityZ) {
   }

   public void addParticle(
      ParticleEffect parameters, boolean force, boolean canSpawnOnMinimal, double x, double y, double z, double velocityX, double velocityY, double velocityZ
   ) {
   }

   public void addImportantParticle(ParticleEffect parameters, double x, double y, double z, double velocityX, double velocityY, double velocityZ) {
   }

   public void addImportantParticle(
      ParticleEffect parameters, boolean force, double x, double y, double z, double velocityX, double velocityY, double velocityZ
   ) {
   }

   public float getSkyAngleRadians(float tickDelta) {
      float f = this.getSkyAngle(tickDelta);
      return f * (float) (Math.PI * 2);
   }

   public void addBlockEntityTicker(BlockEntityTickInvoker ticker) {
      (this.iteratingTickingBlockEntities ? this.pendingBlockEntityTickers : this.blockEntityTickers).add(ticker);
   }

   protected void tickBlockEntities() {
      Profiler profiler = Profilers.get();
      profiler.push("blockEntities");
      this.iteratingTickingBlockEntities = true;
      if (!this.pendingBlockEntityTickers.isEmpty()) {
         this.blockEntityTickers.addAll(this.pendingBlockEntityTickers);
         this.pendingBlockEntityTickers.clear();
      }

      Iterator<BlockEntityTickInvoker> iterator = this.blockEntityTickers.iterator();
      boolean bl = this.getTickManager().shouldTick();
      long lastTickableChunk = Long.MIN_VALUE;

      while (iterator.hasNext()) {
         BlockEntityTickInvoker blockEntityTickInvoker = iterator.next();
         if (blockEntityTickInvoker.isRemoved()) {
            iterator.remove();
         } else if (bl) {
            BlockPos pos = blockEntityTickInvoker.getPos();
            if (pos != null) {
               long chunkPos = ChunkPos.toLong(pos);
               if (chunkPos == lastTickableChunk || this.shouldTickBlocksInChunk(chunkPos)) {
                  lastTickableChunk = chunkPos;
                  blockEntityTickInvoker.tick();
               }
            }
         }
      }

      this.iteratingTickingBlockEntities = false;
      profiler.pop();
   }

   public <T extends Entity> void tickEntity(Consumer<T> tickConsumer, T entity) {
      try {
         tickConsumer.accept(entity);
      } catch (Throwable throwable) {
         CrashReport crashReport = CrashReport.create(throwable, "Ticking entity");
         CrashReportSection crashReportSection = crashReport.addElement("Entity being ticked");
         entity.populateCrashReport(crashReportSection);
         throw new CrashException(crashReport);
      }
   }

   public boolean shouldUpdatePostDeath(Entity entity) {
      return true;
   }

   public boolean shouldTickBlocksInChunk(long chunkPos) {
      return true;
   }

   public boolean shouldTickBlockPos(BlockPos pos) {
      return this.shouldTickBlocksInChunk(ChunkPos.toLong(pos));
   }

   public void createExplosion(@Nullable Entity entity, double x, double y, double z, float power, World.ExplosionSourceType explosionSourceType) {
      this.createExplosion(
         entity,
         Explosion.createDamageSource(this, entity),
         null,
         x,
         y,
         z,
         power,
         false,
         explosionSourceType,
         ParticleTypes.EXPLOSION,
         ParticleTypes.EXPLOSION_EMITTER,
         SoundEvents.ENTITY_GENERIC_EXPLODE
      );
   }

   public void createExplosion(
      @Nullable Entity entity, double x, double y, double z, float power, boolean createFire, World.ExplosionSourceType explosionSourceType
   ) {
      this.createExplosion(
         entity,
         Explosion.createDamageSource(this, entity),
         null,
         x,
         y,
         z,
         power,
         createFire,
         explosionSourceType,
         ParticleTypes.EXPLOSION,
         ParticleTypes.EXPLOSION_EMITTER,
         SoundEvents.ENTITY_GENERIC_EXPLODE
      );
   }

   public void createExplosion(
      @Nullable Entity entity,
      @Nullable DamageSource damageSource,
      @Nullable ExplosionBehavior behavior,
      Vec3d pos,
      float power,
      boolean createFire,
      World.ExplosionSourceType explosionSourceType
   ) {
      this.createExplosion(
         entity,
         damageSource,
         behavior,
         pos.getX(),
         pos.getY(),
         pos.getZ(),
         power,
         createFire,
         explosionSourceType,
         ParticleTypes.EXPLOSION,
         ParticleTypes.EXPLOSION_EMITTER,
         SoundEvents.ENTITY_GENERIC_EXPLODE
      );
   }

   public void createExplosion(
      @Nullable Entity entity,
      @Nullable DamageSource damageSource,
      @Nullable ExplosionBehavior behavior,
      double x,
      double y,
      double z,
      float power,
      boolean createFire,
      World.ExplosionSourceType explosionSourceType
   ) {
      this.createExplosion(
         entity,
         damageSource,
         behavior,
         x,
         y,
         z,
         power,
         createFire,
         explosionSourceType,
         ParticleTypes.EXPLOSION,
         ParticleTypes.EXPLOSION_EMITTER,
         SoundEvents.ENTITY_GENERIC_EXPLODE
      );
   }

   public abstract void createExplosion(
      @Nullable Entity entity,
      @Nullable DamageSource damageSource,
      @Nullable ExplosionBehavior behavior,
      double x,
      double y,
      double z,
      float power,
      boolean createFire,
      World.ExplosionSourceType explosionSourceType,
      ParticleEffect smallParticle,
      ParticleEffect largeParticle,
      RegistryEntry<SoundEvent> soundEvent
   );

   public abstract String asString();

   @Nullable
   @Override
   public BlockEntity getBlockEntity(BlockPos pos) {
      if (this.isOutOfHeightLimit(pos)) {
         return null;
      } else {
         return !this.isClient && Thread.currentThread() != this.thread ? null : this.getWorldChunk(pos).getBlockEntity(pos, WorldChunk.CreationType.IMMEDIATE);
      }
   }

   @Nullable
   @Override
   public BlockEntity lithium$getLoadedExistingBlockEntity(BlockPos pos) {
      if (this.isOutOfHeightLimit(pos) || !this.isClient && Thread.currentThread() != this.thread) {
         return null;
      }
      Chunk chunk = this.getChunk(
         ChunkSectionPos.getSectionCoord(pos.getX()), ChunkSectionPos.getSectionCoord(pos.getZ()), ChunkStatus.FULL, false
      );
      return chunk == null ? null : chunk.getBlockEntity(pos);
   }

   public void addBlockEntity(BlockEntity blockEntity) {
      BlockPos blockPos = blockEntity.getPos();
      if (!this.isOutOfHeightLimit(blockPos)) {
         this.getWorldChunk(blockPos).addBlockEntity(blockEntity);
      }
   }

   public void removeBlockEntity(BlockPos pos) {
      if (!this.isOutOfHeightLimit(pos)) {
         this.getWorldChunk(pos).removeBlockEntity(pos);
      }
   }

   public boolean isPosLoaded(BlockPos pos) {
      return this.isOutOfHeightLimit(pos)
         ? false
         : this.getChunkManager().isChunkLoaded(ChunkSectionPos.getSectionCoord(pos.getX()), ChunkSectionPos.getSectionCoord(pos.getZ()));
   }

   public boolean isDirectionSolid(BlockPos pos, Entity entity, Direction direction) {
      if (this.isOutOfHeightLimit(pos)) {
         return false;
      }

      Chunk chunk = this.getChunk(ChunkSectionPos.getSectionCoord(pos.getX()), ChunkSectionPos.getSectionCoord(pos.getZ()), ChunkStatus.FULL, false);
      return chunk == null ? false : chunk.getBlockState(pos).isSolidSurface(this, pos, entity, direction);
   }

   public boolean isTopSolid(BlockPos pos, Entity entity) {
      return this.isDirectionSolid(pos, entity, Direction.UP);
   }

   public void calculateAmbientDarkness() {
      double d = 1.0 - this.getRainGradient(1.0F) * 5.0F / 16.0;
      double e = 1.0 - this.getThunderGradient(1.0F) * 5.0F / 16.0;
      double f = 0.5 + 2.0 * MathHelper.clamp(MathHelper.cos(this.getSkyAngle(1.0F) * (float) (Math.PI * 2)), -0.25, 0.25);
      this.ambientDarkness = (int)((1.0 - f * d * e) * 11.0);
   }

   public void setMobSpawnOptions(boolean spawnMonsters) {
      this.getChunkManager().setMobSpawnOptions(spawnMonsters);
   }

   public BlockPos getSpawnPos() {
      BlockPos blockPos = this.properties.getSpawnPos();
      if (!this.getWorldBorder().contains(blockPos)) {
         blockPos = this.getTopPosition(
            Heightmap.Type.MOTION_BLOCKING, BlockPos.ofFloored(this.getWorldBorder().getCenterX(), 0.0, this.getWorldBorder().getCenterZ())
         );
      }

      return blockPos;
   }

   public float getSpawnAngle() {
      return this.properties.getSpawnAngle();
   }

   protected void initWeatherGradients() {
      if (this.properties.isRaining()) {
         this.rainGradient = 1.0F;
         if (this.properties.isThundering()) {
            this.thunderGradient = 1.0F;
         }
      }
   }

   @Override
   public void close() throws IOException {
      this.getChunkManager().close();
   }

   @Nullable
   @Override
   public BlockView getChunkAsView(int chunkX, int chunkZ) {
      return this.getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);
   }

   @Override
   public List<Entity> getOtherEntities(@Nullable Entity except, Box box, Predicate<? super Entity> predicate) {
      Profilers.get().visit("getEntities");
      List<Entity> list = Lists.newArrayList();
      this.getEntityLookup().forEachIntersects(box, entity -> {
         if (entity != except && predicate.test(entity)) {
            list.add(entity);
         }
      });

      for (EnderDragonPart enderDragonPart : this.getEnderDragonParts()) {
         if (enderDragonPart != except
            && enderDragonPart.owner != except
            && predicate.test(enderDragonPart)
            && box.intersects(enderDragonPart.getBoundingBox())) {
            list.add(enderDragonPart);
         }
      }

      return list;
   }

   @Override
   public boolean isSpaceEmpty(@Nullable Entity entity, Box box) {
      if (LithiumEntityCollisions.doesBoxCollideWithBlocks(this, entity, box)) {
         return false;
      }
      if (LithiumEntityCollisions.doesBoxCollideWithHardEntities(this, entity, box)) {
         return false;
      }
      return entity == null || !LithiumEntityCollisions.doesBoxCollideWithWorldBorder(this, entity, box);
   }

   @Override
   public Optional<Vec3d> findClosestCollision(@Nullable Entity entity, VoxelShape shape, Vec3d target, double x, double y, double z) {
      if (shape.isEmpty()) {
         return Optional.empty();
      }

      Box searchBox = shape.getBoundingBox().expand(x, y, z);
      List<VoxelShape> blockCollisions = new ArrayList<>();
      this.getBlockCollisions(entity, searchBox).forEach(blockCollisions::add);
      if (blockCollisions.isEmpty()) {
         return shape.getClosestPointTo(target);
      }

      WorldBorder border = this.getWorldBorder();
      if (border != null) {
         double sideLength = Math.max(searchBox.getLengthX(), searchBox.getLengthZ());
         double centerX = MathHelper.lerp(0.5, searchBox.minX, searchBox.maxX);
         double centerZ = MathHelper.lerp(0.5, searchBox.minZ, searchBox.maxZ);
         if (2.0 + 2.0 * sideLength >= border.getDistanceInsideBorder(centerX, centerZ)) {
            blockCollisions.removeIf(collision -> !border.contains(collision.getBoundingBox()));
         }
      }

      List<Box> collisionBoxes = new ArrayList<>();
      for (VoxelShape collision : blockCollisions) {
         for (Box box : collision.getBoundingBoxes()) {
            collisionBoxes.add(box.expand(x / 2.0, y / 2.0, z / 2.0));
         }
      }
      return VoxelShapeHelper.getClosestPointTo(target, shape, collisionBoxes);
   }

   @Override
   public <T extends Entity> List<T> getEntitiesByType(TypeFilter<Entity, T> filter, Box box, Predicate<? super T> predicate) {
      List<T> list = Lists.newArrayList();
      this.collectEntitiesByType(filter, box, predicate, list);
      return list;
   }

   public <T extends Entity> void collectEntitiesByType(TypeFilter<Entity, T> filter, Box box, Predicate<? super T> predicate, List<? super T> result) {
      this.collectEntitiesByType(filter, box, predicate, result, Integer.MAX_VALUE);
   }

   public <T extends Entity> void collectEntitiesByType(
      TypeFilter<Entity, T> filter, Box box, Predicate<? super T> predicate, List<? super T> result, int limit
   ) {
      Profilers.get().visit("getEntities");
      this.getEntityLookup().forEachIntersects(filter, box, entity -> {
         if (predicate.test(entity)) {
            result.add(entity);
            if (result.size() >= limit) {
               return LazyIterationConsumer.NextIteration.ABORT;
            }
         }

         if (entity instanceof EnderDragonEntity enderDragonEntity) {
            for (EnderDragonPart enderDragonPart : enderDragonEntity.getBodyParts()) {
               T entity2 = filter.downcast(enderDragonPart);
               if (entity2 != null && predicate.test(entity2)) {
                  result.add(entity2);
                  if (result.size() >= limit) {
                     return LazyIterationConsumer.NextIteration.ABORT;
                  }
               }
            }
         }

         return LazyIterationConsumer.NextIteration.CONTINUE;
      });
   }

   @Nullable
   public abstract Entity getEntityById(int id);

   public abstract Collection<EnderDragonPart> getEnderDragonParts();

   public void markDirty(BlockPos pos) {
      if (this.isChunkLoaded(pos)) {
         this.getWorldChunk(pos).markNeedsSaving();
      }
   }

   public void disconnect() {
   }

   public long getTime() {
      return this.properties.getTime();
   }

   public long getTimeOfDay() {
      return this.properties.getTimeOfDay();
   }

   public boolean canPlayerModifyAt(PlayerEntity player, BlockPos pos) {
      return true;
   }

   public void sendEntityStatus(Entity entity, byte status) {
   }

   public void sendEntityDamage(Entity entity, DamageSource damageSource) {
   }

   public void addSyncedBlockEvent(BlockPos pos, Block block, int type, int data) {
      this.getBlockState(pos).onSyncedBlockEvent(this, pos, type, data);
   }

   @Override
   public WorldProperties getLevelProperties() {
      return this.properties;
   }

   public abstract TickManager getTickManager();

   public float getThunderGradient(float delta) {
      return MathHelper.lerp(delta, this.thunderGradientPrev, this.thunderGradient) * this.getRainGradient(delta);
   }

   public void setThunderGradient(float thunderGradient) {
      float f = MathHelper.clamp(thunderGradient, 0.0F, 1.0F);
      this.thunderGradientPrev = f;
      this.thunderGradient = f;
   }

   public float getRainGradient(float delta) {
      return MathHelper.lerp(delta, this.rainGradientPrev, this.rainGradient);
   }

   public void setRainGradient(float rainGradient) {
      float f = MathHelper.clamp(rainGradient, 0.0F, 1.0F);
      this.rainGradientPrev = f;
      this.rainGradient = f;
   }

   private boolean canHaveWeather() {
      return this.getDimension().hasSkyLight() && !this.getDimension().hasCeiling();
   }

   public boolean isThundering() {
      return this.canHaveWeather() && this.getThunderGradient(1.0F) > 0.9;
   }

   public boolean isRaining() {
      return this.canHaveWeather() && this.getRainGradient(1.0F) > 0.2;
   }

   public boolean hasRain(BlockPos pos) {
      if (!this.isRaining()) {
         return false;
      }

      if (!this.isSkyVisible(pos)) {
         return false;
      }

      if (this.getTopPosition(Heightmap.Type.MOTION_BLOCKING, pos).getY() > pos.getY()) {
         return false;
      }

      Biome biome = this.getBiome(pos).value();
      return biome.getPrecipitation(pos, this.getSeaLevel()) == Biome.Precipitation.RAIN;
   }

   @Nullable
   public abstract MapState getMapState(MapIdComponent id);

   public abstract void putMapState(MapIdComponent id, MapState state);

   public abstract MapIdComponent increaseAndGetMapId();

   public void syncGlobalEvent(int eventId, BlockPos pos, int data) {
   }

   public CrashReportSection addDetailsToCrashReport(CrashReport report) {
      CrashReportSection crashReportSection = report.addElement("Affected level", 1);
      crashReportSection.add("All players", () -> this.getPlayers().size() + " total; " + this.getPlayers());
      crashReportSection.add("Chunk stats", this.getChunkManager()::getDebugString);
      crashReportSection.add("Level dimension", () -> this.getRegistryKey().getValue().toString());

      try {
         this.properties.populateCrashReport(crashReportSection, this);
      } catch (Throwable throwable) {
         crashReportSection.add("Level Data Unobtainable", throwable);
      }

      return crashReportSection;
   }

   public abstract void setBlockBreakingInfo(int entityId, BlockPos pos, int progress);

   public void addFireworkParticle(
      double x, double y, double z, double velocityX, double velocityY, double velocityZ, List<FireworkExplosionComponent> explosions
   ) {
   }

   public abstract Scoreboard getScoreboard();

   public void updateComparators(BlockPos pos, Block block) {
      for (Direction direction : Direction.Type.HORIZONTAL) {
         BlockPos blockPos = pos.offset(direction);
         if (this.isChunkLoaded(blockPos)) {
            BlockState blockState = this.getBlockState(blockPos);
            if (blockState.isOf(Blocks.COMPARATOR)) {
               this.updateNeighbor(blockState, blockPos, block, null, false);
            } else if (blockState.isSolidBlock(this, blockPos)) {
               blockPos = blockPos.offset(direction);
               blockState = this.getBlockState(blockPos);
               if (blockState.isOf(Blocks.COMPARATOR)) {
                  this.updateNeighbor(blockState, blockPos, block, null, false);
               }
            }
         }
      }
   }

   @Override
   public LocalDifficulty getLocalDifficulty(BlockPos pos) {
      long l = 0L;
      float f = 0.0F;
      if (this.isChunkLoaded(pos)) {
         f = this.getMoonSize();
         l = this.getWorldChunk(pos).getInhabitedTime();
      }

      return new LocalDifficulty(this.getDifficulty(), this.getTimeOfDay(), l, f);
   }

   @Override
   public int getAmbientDarkness() {
      return this.ambientDarkness;
   }

   public void setLightningTicksLeft(int lightningTicksLeft) {
   }

   @Override
   public WorldBorder getWorldBorder() {
      return this.border;
   }

   public void sendPacket(Packet<?> packet) {
      throw new UnsupportedOperationException("Can't send packets to server unless you're on the client.");
   }

   @Override
   public DimensionType getDimension() {
      return this.dimensionEntry.value();
   }

   public RegistryEntry<DimensionType> getDimensionEntry() {
      return this.dimensionEntry;
   }

   public RegistryKey<World> getRegistryKey() {
      return this.registryKey;
   }

   @Override
   public Random getRandom() {
      return this.random;
   }

   @Override
   public boolean testBlockState(BlockPos pos, Predicate<BlockState> state) {
      return state.test(this.getBlockState(pos));
   }

   @Override
   public boolean testFluidState(BlockPos pos, Predicate<FluidState> state) {
      return state.test(this.getFluidState(pos));
   }

   public abstract RecipeManager getRecipeManager();

   public BlockPos getRandomPosInChunk(int x, int y, int z, int i) {
      BlockPos.Mutable mutable = new BlockPos.Mutable();
      this.lithium$getRandomPosInChunk(x, y, z, i, mutable);
      return mutable.toImmutable();
   }

   public void lithium$getRandomPosInChunk(int x, int y, int z, int mask, BlockPos.Mutable out) {
      this.lcgBlockSeed = this.lcgBlockSeed * 3 + 1013904223;
      int j = this.lcgBlockSeed >> 2;
      out.set(x + (j & 15), y + (j >> 16 & mask), z + (j >> 8 & 15));
   }

   public boolean isSavingDisabled() {
      return false;
   }

   @Override
   public BiomeAccess getBiomeAccess() {
      return this.biomeAccess;
   }

   public final boolean isDebugWorld() {
      return this.debugWorld;
   }

   protected abstract EntityLookup<Entity> getEntityLookup();

   @Nullable
   public SectionedEntityCache<Entity> lithium$getEntityCache() {
      EntityLookup<Entity> lookup = this.getEntityLookup();
      return lookup instanceof SimpleEntityLookup<?> simpleLookup
         ? (SectionedEntityCache<Entity>)(SectionedEntityCache<?>)simpleLookup.lithium$getCache()
         : null;
   }

   @Override
   public long getTickOrder() {
      return this.tickOrder++;
   }

   @Override
   public DynamicRegistryManager getRegistryManager() {
      return this.registryManager;
   }

   public DamageSources getDamageSources() {
      return this.damageSources;
   }

   public abstract BrewingRecipeRegistry getBrewingRecipeRegistry();

   public abstract FuelRegistry getFuelRegistry();

   public enum ExplosionSourceType implements StringIdentifiable {
      NONE("none"),
      BLOCK("block"),
      MOB("mob"),
      TNT("tnt"),
      TRIGGER("trigger");

      public static final Codec<World.ExplosionSourceType> CODEC = StringIdentifiable.createCodec(World.ExplosionSourceType::values);
      private final String id;

      ExplosionSourceType(final String id) {
         this.id = id;
      }

      @Override
      public String asString() {
         return this.id;
      }
   }
}
