package net.minecraft.block;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.objects.Object2ByteLinkedOpenHashMap;
import net.caffeinemc.mods.lithium.common.util.collections.Object2BooleanCacheTable;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.SharedConstants;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.PiglinBrain;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootWorldContext;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.stat.Stats;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Property;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.collection.IdList;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.intprovider.IntProvider;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.explosion.Explosion;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

public class Block extends AbstractBlock implements ItemConvertible {
   public static final MapCodec<Block> CODEC = createCodec(Block::new);
   private static final Logger LOGGER = LogUtils.getLogger();
   private final RegistryEntry.Reference<Block> registryEntry = Registries.BLOCK.createEntry(this);
   public static final IdList<BlockState> STATE_IDS = new IdList<>();
   private static final Object2BooleanCacheTable<VoxelShape> FULL_CUBE_SHAPE_CACHE = new Object2BooleanCacheTable<>(
      512, shape -> !VoxelShapes.matchesAnywhere(VoxelShapes.fullCube(), shape, BooleanBiFunction.NOT_SAME)
   );
   public static final int NOTIFY_NEIGHBORS = 1;
   public static final int NOTIFY_LISTENERS = 2;
   public static final int NO_REDRAW = 4;
   public static final int REDRAW_ON_MAIN_THREAD = 8;
   public static final int FORCE_STATE = 16;
   public static final int SKIP_DROPS = 32;
   public static final int MOVED = 64;
   public static final int SKIP_REDSTONE_WIRE_STATE_REPLACEMENT = 128;
   public static final int field_31035 = 4;
   public static final int NOTIFY_ALL = 3;
   public static final int NOTIFY_ALL_AND_REDRAW = 11;
   public static final float field_31023 = -1.0F;
   public static final float field_31024 = 0.0F;
   public static final int field_31025 = 512;
   protected final StateManager<Block, BlockState> stateManager;
   private BlockState defaultState;
   @Nullable
   private Item cachedItem;
   private static final int FACE_CULL_MAP_SIZE = 256;
   private static final ThreadLocal<Object2ByteLinkedOpenHashMap<Block.VoxelShapePair>> FACE_CULL_MAP = ThreadLocal.withInitial(() -> {
      Object2ByteLinkedOpenHashMap<Block.VoxelShapePair> object2ByteLinkedOpenHashMap = new Object2ByteLinkedOpenHashMap<Block.VoxelShapePair>(256, 0.25F) {
         protected void rehash(int newN) {
         }
      };
      object2ByteLinkedOpenHashMap.defaultReturnValue((byte)127);
      return object2ByteLinkedOpenHashMap;
   });

   @Override
   protected MapCodec<? extends Block> getCodec() {
      return CODEC;
   }

   public static int getRawIdFromState(@Nullable BlockState state) {
      if (state == null) {
         return 0;
      }

      int i = STATE_IDS.getRawId(state);
      return i == -1 ? 0 : i;
   }

   public static BlockState getStateFromRawId(int stateId) {
      BlockState blockState = STATE_IDS.get(stateId);
      return blockState == null ? Blocks.AIR.getDefaultState() : blockState;
   }

   public static Block getBlockFromItem(@Nullable Item item) {
      return item instanceof BlockItem ? ((BlockItem)item).getBlock() : Blocks.AIR;
   }

   public static BlockState pushEntitiesUpBeforeBlockChange(BlockState from, BlockState to, WorldAccess world, BlockPos pos) {
      VoxelShape voxelShape = VoxelShapes.combine(from.getCollisionShape(world, pos), to.getCollisionShape(world, pos), BooleanBiFunction.ONLY_SECOND)
         .offset(pos.getX(), pos.getY(), pos.getZ());
      if (voxelShape.isEmpty()) {
         return to;
      }

      for (Entity entity : world.getOtherEntities(null, voxelShape.getBoundingBox())) {
         double d = VoxelShapes.calculateMaxOffset(Direction.Axis.Y, entity.getBoundingBox().offset(0.0, 1.0, 0.0), List.of(voxelShape), -1.0);
         entity.requestTeleportOffset(0.0, 1.0 + d, 0.0);
      }

      return to;
   }

   public static VoxelShape createCuboidShape(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
      return VoxelShapes.cuboid(minX / 16.0, minY / 16.0, minZ / 16.0, maxX / 16.0, maxY / 16.0, maxZ / 16.0);
   }

   public static BlockState postProcessState(BlockState state, WorldAccess world, BlockPos pos) {
      BlockState blockState = state;
      BlockPos.Mutable mutable = new BlockPos.Mutable();

      for (Direction direction : DIRECTIONS) {
         mutable.set(pos, direction);
         blockState = blockState.getStateForNeighborUpdate(world, world, pos, direction, mutable, world.getBlockState(mutable), world.getRandom());
      }

      return blockState;
   }

   public static void replace(BlockState state, BlockState newState, WorldAccess world, BlockPos pos, int flags) {
      replace(state, newState, world, pos, flags, 512);
   }

   public static void replace(BlockState state, BlockState newState, WorldAccess world, BlockPos pos, int flags, int maxUpdateDepth) {
      if (newState != state) {
         if (newState.isAir()) {
            if (!world.isClient()) {
               world.breakBlock(pos, (flags & 32) == 0, null, maxUpdateDepth);
            }
         } else {
            world.setBlockState(pos, newState, flags & -33, maxUpdateDepth);
         }
      }
   }

   public Block(AbstractBlock.Settings settings) {
      super(settings);
      StateManager.Builder<Block, BlockState> builder = new StateManager.Builder<>(this);
      this.appendProperties(builder);
      this.stateManager = builder.build(Block::getDefaultState, BlockState::new);
      this.setDefaultState(this.stateManager.getDefaultState());
      if (SharedConstants.isDevelopment) {
         String string = this.getClass().getSimpleName();
         if (!string.endsWith("Block")) {
            LOGGER.error("Block classes should end with Block and {} doesn't.", string);
         }
      }
   }

   public static boolean cannotConnect(BlockState state) {
      return state.getBlock() instanceof LeavesBlock
         || state.isOf(Blocks.BARRIER)
         || state.isOf(Blocks.CARVED_PUMPKIN)
         || state.isOf(Blocks.JACK_O_LANTERN)
         || state.isOf(Blocks.MELON)
         || state.isOf(Blocks.PUMPKIN)
         || state.isIn(BlockTags.SHULKER_BOXES);
   }

   public static boolean shouldDrawSide(BlockState state, BlockState otherState, Direction side) {
      VoxelShape voxelShape = otherState.getCullingFace(side.getOpposite());
      if (voxelShape == VoxelShapes.fullCube()) {
         return false;
      }

      if (state.isSideInvisible(otherState, side)) {
         return false;
      }

      if (voxelShape == VoxelShapes.empty()) {
         return true;
      }

      VoxelShape voxelShape2 = state.getCullingFace(side);
      if (voxelShape2 == VoxelShapes.empty()) {
         return true;
      }

      Block.VoxelShapePair voxelShapePair = new Block.VoxelShapePair(voxelShape2, voxelShape);
      Object2ByteLinkedOpenHashMap<Block.VoxelShapePair> object2ByteLinkedOpenHashMap = FACE_CULL_MAP.get();
      byte b = object2ByteLinkedOpenHashMap.getAndMoveToFirst(voxelShapePair);
      if (b != 127) {
         return b != 0;
      }

      boolean bl = VoxelShapes.matchesAnywhere(voxelShape2, voxelShape, BooleanBiFunction.ONLY_FIRST);
      if (object2ByteLinkedOpenHashMap.size() == 256) {
         object2ByteLinkedOpenHashMap.removeLastByte();
      }

      object2ByteLinkedOpenHashMap.putAndMoveToFirst(voxelShapePair, (byte)(bl ? 1 : 0));
      return bl;
   }

   public static boolean hasTopRim(BlockView world, BlockPos pos) {
      return world.getBlockState(pos).isSideSolid(world, pos, Direction.UP, SideShapeType.RIGID);
   }

   public static boolean sideCoversSmallSquare(WorldView world, BlockPos pos, Direction side) {
      BlockState blockState = world.getBlockState(pos);
      return side == Direction.DOWN && blockState.isIn(BlockTags.UNSTABLE_BOTTOM_CENTER)
         ? false
         : blockState.isSideSolid(world, pos, side, SideShapeType.CENTER);
   }

   public static boolean isFaceFullSquare(VoxelShape shape, Direction side) {
      VoxelShape voxelShape = shape.getFace(side);
      return isShapeFullCube(voxelShape);
   }

   public static boolean isShapeFullCube(VoxelShape shape) {
      return FULL_CUBE_SHAPE_CACHE.get(shape);
   }

   public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
   }

   public void onBroken(WorldAccess world, BlockPos pos, BlockState state) {
   }

   public static List<ItemStack> getDroppedStacks(BlockState state, ServerWorld world, BlockPos pos, @Nullable BlockEntity blockEntity) {
      LootWorldContext.Builder builder = new LootWorldContext.Builder(world)
         .add(LootContextParameters.ORIGIN, Vec3d.ofCenter(pos))
         .add(LootContextParameters.TOOL, ItemStack.EMPTY)
         .addOptional(LootContextParameters.BLOCK_ENTITY, blockEntity);
      return state.getDroppedStacks(builder);
   }

   public static List<ItemStack> getDroppedStacks(
      BlockState state, ServerWorld world, BlockPos pos, @Nullable BlockEntity blockEntity, @Nullable Entity entity, ItemStack stack
   ) {
      LootWorldContext.Builder builder = new LootWorldContext.Builder(world)
         .add(LootContextParameters.ORIGIN, Vec3d.ofCenter(pos))
         .add(LootContextParameters.TOOL, stack)
         .addOptional(LootContextParameters.THIS_ENTITY, entity)
         .addOptional(LootContextParameters.BLOCK_ENTITY, blockEntity);
      return state.getDroppedStacks(builder);
   }

   public static void dropStacks(BlockState state, World world, BlockPos pos) {
      if (world instanceof ServerWorld) {
         getDroppedStacks(state, (ServerWorld)world, pos, null).forEach(stack -> dropStack(world, pos, stack));
         state.onStacksDropped((ServerWorld)world, pos, ItemStack.EMPTY, true);
      }
   }

   public static void dropStacks(BlockState state, WorldAccess world, BlockPos pos, @Nullable BlockEntity blockEntity) {
      if (world instanceof ServerWorld) {
         getDroppedStacks(state, (ServerWorld)world, pos, blockEntity).forEach(stack -> dropStack((ServerWorld)world, pos, stack));
         state.onStacksDropped((ServerWorld)world, pos, ItemStack.EMPTY, true);
      }
   }

   public static void dropStacks(BlockState state, World world, BlockPos pos, @Nullable BlockEntity blockEntity, @Nullable Entity entity, ItemStack tool) {
      if (world instanceof ServerWorld) {
         getDroppedStacks(state, (ServerWorld)world, pos, blockEntity, entity, tool).forEach(stack -> dropStack(world, pos, stack));
         state.onStacksDropped((ServerWorld)world, pos, tool, true);
      }
   }

   public static void dropStack(World world, BlockPos pos, ItemStack stack) {
      double d = EntityType.ITEM.getHeight() / 2.0;
      double e = pos.getX() + 0.5 + MathHelper.nextDouble(world.random, -0.25, 0.25);
      double f = pos.getY() + 0.5 + MathHelper.nextDouble(world.random, -0.25, 0.25) - d;
      double g = pos.getZ() + 0.5 + MathHelper.nextDouble(world.random, -0.25, 0.25);
      dropStack(world, () -> new ItemEntity(world, e, f, g, stack), stack);
   }

   public static void dropStack(World world, BlockPos pos, Direction direction, ItemStack stack) {
      int i = direction.getOffsetX();
      int j = direction.getOffsetY();
      int k = direction.getOffsetZ();
      double d = EntityType.ITEM.getWidth() / 2.0;
      double e = EntityType.ITEM.getHeight() / 2.0;
      double f = pos.getX() + 0.5 + (i == 0 ? MathHelper.nextDouble(world.random, -0.25, 0.25) : i * (0.5 + d));
      double g = pos.getY() + 0.5 + (j == 0 ? MathHelper.nextDouble(world.random, -0.25, 0.25) : j * (0.5 + e)) - e;
      double h = pos.getZ() + 0.5 + (k == 0 ? MathHelper.nextDouble(world.random, -0.25, 0.25) : k * (0.5 + d));
      double l = i == 0 ? MathHelper.nextDouble(world.random, -0.1, 0.1) : i * 0.1;
      double m = j == 0 ? MathHelper.nextDouble(world.random, 0.0, 0.1) : j * 0.1 + 0.1;
      double n = k == 0 ? MathHelper.nextDouble(world.random, -0.1, 0.1) : k * 0.1;
      dropStack(world, () -> new ItemEntity(world, f, g, h, stack, l, m, n), stack);
   }

   private static void dropStack(World world, Supplier<ItemEntity> itemEntitySupplier, ItemStack stack) {
      if (world instanceof ServerWorld serverWorld && !stack.isEmpty() && serverWorld.getGameRules().getBoolean(GameRules.DO_TILE_DROPS)) {
         ItemEntity itemEntity = itemEntitySupplier.get();
         itemEntity.setToDefaultPickupDelay();
         world.spawnEntity(itemEntity);
      }
   }

   protected void dropExperience(ServerWorld world, BlockPos pos, int size) {
      if (world.getGameRules().getBoolean(GameRules.DO_TILE_DROPS)) {
         ExperienceOrbEntity.spawn(world, Vec3d.ofCenter(pos), size);
      }
   }

   public float getBlastResistance() {
      return this.resistance;
   }

   public void onDestroyedByExplosion(ServerWorld world, BlockPos pos, Explosion explosion) {
   }

   public void onSteppedOn(World world, BlockPos pos, BlockState state, Entity entity) {
   }

   @Nullable
   public BlockState getPlacementState(ItemPlacementContext ctx) {
      return this.getDefaultState();
   }

   public void afterBreak(World world, PlayerEntity player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, ItemStack tool) {
      player.incrementStat(Stats.MINED.getOrCreateStat(this));
      player.addExhaustion(0.005F);
      dropStacks(state, world, pos, blockEntity, player, tool);
   }

   public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
   }

   public boolean canMobSpawnInside(BlockState state) {
      return !state.isSolid() && !state.isLiquid();
   }

   public MutableText getName() {
      return Text.translatable(this.getTranslationKey());
   }

   public void onLandedUpon(World world, BlockState state, BlockPos pos, Entity entity, float fallDistance) {
      entity.handleFallDamage(fallDistance, 1.0F, entity.getDamageSources().fall());
   }

   public void onEntityLand(BlockView world, Entity entity) {
      entity.setVelocity(entity.getVelocity().multiply(1.0, 0.0, 1.0));
   }

   public float getSlipperiness() {
      return this.slipperiness;
   }

   public float getVelocityMultiplier() {
      return this.velocityMultiplier;
   }

   public float getJumpVelocityMultiplier() {
      return this.jumpVelocityMultiplier;
   }

   protected void spawnBreakParticles(World world, PlayerEntity player, BlockPos pos, BlockState state) {
      world.syncWorldEvent(player, 2001, pos, getRawIdFromState(state));
   }

   public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
      this.spawnBreakParticles(world, player, pos, state);
      if (state.isIn(BlockTags.GUARDED_BY_PIGLINS) && world instanceof ServerWorld serverWorld) {
         PiglinBrain.onGuardedBlockInteracted(serverWorld, player, false);
      }

      world.emitGameEvent(GameEvent.BLOCK_DESTROY, pos, GameEvent.Emitter.of(player, state));
      return state;
   }

   public void precipitationTick(BlockState state, World world, BlockPos pos, Biome.Precipitation precipitation) {
   }

   public boolean shouldDropItemsOnExplosion(Explosion explosion) {
      return true;
   }

   protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
   }

   public StateManager<Block, BlockState> getStateManager() {
      return this.stateManager;
   }

   protected final void setDefaultState(BlockState state) {
      this.defaultState = state;
   }

   public final BlockState getDefaultState() {
      return this.defaultState;
   }

   public final BlockState getStateWithProperties(BlockState state) {
      BlockState blockState = this.getDefaultState();

      for (Property<?> property : state.getBlock().getStateManager().getProperties()) {
         if (blockState.contains(property)) {
            blockState = copyProperty(state, blockState, property);
         }
      }

      return blockState;
   }

   private static <T extends Comparable<T>> BlockState copyProperty(BlockState source, BlockState target, Property<T> property) {
      return target.with(property, source.get(property));
   }

   @Override
   public Item asItem() {
      if (this.cachedItem == null) {
         this.cachedItem = Item.fromBlock(this);
      }

      return this.cachedItem;
   }

   public boolean hasDynamicBounds() {
      return this.dynamicBounds;
   }

   @Override
   public String toString() {
      return "Block{" + Registries.BLOCK.getEntry(this).getIdAsString() + "}";
   }

   public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType options) {
   }

   @Override
   protected Block asBlock() {
      return this;
   }

   protected ImmutableMap<BlockState, VoxelShape> getShapesForStates(Function<BlockState, VoxelShape> stateToShape) {
      return this.stateManager.getStates().stream().collect(ImmutableMap.toImmutableMap(Function.identity(), stateToShape));
   }

   @Deprecated
   public RegistryEntry.Reference<Block> getRegistryEntry() {
      return this.registryEntry;
   }

   protected void dropExperienceWhenMined(ServerWorld world, BlockPos pos, ItemStack tool, IntProvider experience) {
      int i = EnchantmentHelper.getBlockExperience(world, tool, experience.get(world.getRandom()));
      if (i > 0) {
         this.dropExperience(world, pos, i);
      }
   }

   record VoxelShapePair(VoxelShape first, VoxelShape second) {
      @Override
      public boolean equals(Object o) {
         return o instanceof Block.VoxelShapePair voxelShapePair && this.first == voxelShapePair.first && this.second == voxelShapePair.second;
      }

      @Override
      public int hashCode() {
         return System.identityHashCode(this.first) * 31 + System.identityHashCode(this.second);
      }
   }
}
