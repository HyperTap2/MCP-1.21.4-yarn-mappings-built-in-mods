package net.caffeinemc.mods.lithium.common.block;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.caffeinemc.mods.lithium.common.ai.pathing.PathNodeCache;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.Entity;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.World;

public final class BlockStateFlags {
   private static final Map<Class<?>, Boolean> ENTITY_TOUCHABLE_CACHE = new ConcurrentHashMap<>();
   public static final boolean ENABLED = BlockCountingSection.class.isAssignableFrom(ChunkSection.class);
   public static final TrackedBlockStatePredicate OVERSIZED_SHAPE;
   public static final TrackedBlockStatePredicate WATER;
   public static final TrackedBlockStatePredicate LAVA;
   public static final TrackedBlockStatePredicate PATH_NOT_OPEN;
   public static final int NUM_TRACKED_FLAGS;
   public static final TrackedBlockStatePredicate[] TRACKED_FLAGS;
   public static final TrackedBlockStatePredicate ENTITY_TOUCHABLE;
   public static final TrackedBlockStatePredicate[] FLAGS;

   static {
      ArrayList<TrackedBlockStatePredicate> countingFlags = new ArrayList<>();
      OVERSIZED_SHAPE = new TrackedBlockStatePredicate(countingFlags.size()) {
         @Override
         public boolean test(BlockState state) {
            return state.exceedsCube();
         }
      };
      countingFlags.add(OVERSIZED_SHAPE);
      WATER = new TrackedBlockStatePredicate(countingFlags.size()) {
         @Override
         public boolean test(BlockState state) {
            return state.getFluidState().isIn(FluidTags.WATER);
         }
      };
      countingFlags.add(WATER);
      LAVA = new TrackedBlockStatePredicate(countingFlags.size()) {
         @Override
         public boolean test(BlockState state) {
            return state.getFluidState().isIn(FluidTags.LAVA);
         }
      };
      countingFlags.add(LAVA);
      PATH_NOT_OPEN = new TrackedBlockStatePredicate(countingFlags.size()) {
         @Override
         public boolean test(BlockState state) {
            return PathNodeCache.getNeighborPathNodeType(state) != PathNodeType.OPEN;
         }
      };
      countingFlags.add(PATH_NOT_OPEN);
      NUM_TRACKED_FLAGS = countingFlags.size();
      TRACKED_FLAGS = countingFlags.toArray(TrackedBlockStatePredicate[]::new);

      ENTITY_TOUCHABLE = new TrackedBlockStatePredicate(NUM_TRACKED_FLAGS) {
         @Override
         public boolean test(BlockState state) {
            return isEntityTouchable(state) || state.isOf(Blocks.LAVA) || state.isIn(BlockTags.FIRE);
         }
      };
      ArrayList<TrackedBlockStatePredicate> flags = new ArrayList<>(countingFlags);
      flags.add(ENTITY_TOUCHABLE);
      FLAGS = flags.toArray(TrackedBlockStatePredicate[]::new);
   }

   private BlockStateFlags() {
   }

   private static boolean isEntityTouchable(BlockState state) {
      return ENTITY_TOUCHABLE_CACHE.computeIfAbsent(state.getBlock().getClass(), BlockStateFlags::overridesEntityCollision);
   }

   private static boolean overridesEntityCollision(Class<?> blockClass) {
      Class<?> current = blockClass;
      while (current != null && current != AbstractBlock.class && AbstractBlock.class.isAssignableFrom(current)) {
         try {
            Method ignored = current.getDeclaredMethod("onEntityCollision", BlockState.class, World.class, BlockPos.class, Entity.class);
            return true;
         } catch (NoSuchMethodException exception) {
            current = current.getSuperclass();
         } catch (LinkageError | SecurityException exception) {
            return true;
         }
      }

      return false;
   }
}
