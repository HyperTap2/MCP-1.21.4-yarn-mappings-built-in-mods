package net.minecraft.entity.ai;

import com.google.common.annotations.VisibleForTesting;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.Nullable;

public class FuzzyPositions {
   private static final int GAUSS_RANGE = 10;

   public static BlockPos localFuzz(Random random, int horizontalRange, int verticalRange) {
      int i = random.nextInt(2 * horizontalRange + 1) - horizontalRange;
      int j = random.nextInt(2 * verticalRange + 1) - verticalRange;
      int k = random.nextInt(2 * horizontalRange + 1) - horizontalRange;
      return new BlockPos(i, j, k);
   }

   @Nullable
   public static BlockPos localFuzz(
      Random random, int horizontalRange, int verticalRange, int startHeight, double directionX, double directionZ, double angleRange
   ) {
      double d = MathHelper.atan2(directionZ, directionX) - (float) (Math.PI / 2);
      double e = d + (2.0F * random.nextFloat() - 1.0F) * angleRange;
      double f = Math.sqrt(random.nextDouble()) * MathHelper.SQUARE_ROOT_OF_TWO * horizontalRange;
      double g = -f * Math.sin(e);
      double h = f * Math.cos(e);
      if (!(Math.abs(g) > horizontalRange) && !(Math.abs(h) > horizontalRange)) {
         int i = random.nextInt(2 * verticalRange + 1) - verticalRange + startHeight;
         return BlockPos.ofFloored(g, i, h);
      } else {
         return null;
      }
   }

   @VisibleForTesting
   public static BlockPos upWhile(BlockPos pos, int maxY, Predicate<BlockPos> condition) {
      if (!condition.test(pos)) {
         return pos;
      }

      BlockPos.Mutable mutable = pos.mutableCopy().move(Direction.UP);

      while (mutable.getY() <= maxY && condition.test(mutable)) {
         mutable.move(Direction.UP);
      }

      return mutable.toImmutable();
   }

   @VisibleForTesting
   public static BlockPos upWhile(BlockPos pos, int extraAbove, int max, Predicate<BlockPos> condition) {
      if (extraAbove < 0) {
         throw new IllegalArgumentException("aboveSolidAmount was " + extraAbove + ", expected >= 0");
      }

      if (!condition.test(pos)) {
         return pos;
      }

      BlockPos.Mutable mutable = pos.mutableCopy().move(Direction.UP);

      while (mutable.getY() <= max && condition.test(mutable)) {
         mutable.move(Direction.UP);
      }

      int i = mutable.getY();

      while (mutable.getY() <= max && mutable.getY() - i < extraAbove) {
         mutable.move(Direction.UP);
         if (condition.test(mutable)) {
            mutable.move(Direction.DOWN);
            break;
         }
      }

      return mutable.toImmutable();
   }

   @Nullable
   public static Vec3d guessBestPathTarget(PathAwareEntity entity, Supplier<BlockPos> factory) {
      return guessBest(factory, entity::getPathfindingFavor);
   }

   @Nullable
   public static Vec3d guessBest(Supplier<BlockPos> factory, ToDoubleFunction<BlockPos> scorer) {
      double d = Double.NEGATIVE_INFINITY;
      BlockPos blockPos = null;

      for (int i = 0; i < 10; i++) {
         BlockPos blockPos2 = factory.get();
         if (blockPos2 != null) {
            double e = scorer.applyAsDouble(blockPos2);
            if (e > d) {
               d = e;
               blockPos = blockPos2;
            }
         }
      }

      return blockPos != null ? Vec3d.ofBottomCenter(blockPos) : null;
   }

   public static BlockPos towardTarget(PathAwareEntity entity, int horizontalRange, Random random, BlockPos fuzz) {
      int i = fuzz.getX();
      int j = fuzz.getZ();
      if (entity.hasPositionTarget() && horizontalRange > 1) {
         BlockPos blockPos = entity.getPositionTarget();
         if (entity.getX() > blockPos.getX()) {
            i -= random.nextInt(horizontalRange / 2);
         } else {
            i += random.nextInt(horizontalRange / 2);
         }

         if (entity.getZ() > blockPos.getZ()) {
            j -= random.nextInt(horizontalRange / 2);
         } else {
            j += random.nextInt(horizontalRange / 2);
         }
      }

      return BlockPos.ofFloored(i + entity.getX(), fuzz.getY() + entity.getY(), j + entity.getZ());
   }
}
