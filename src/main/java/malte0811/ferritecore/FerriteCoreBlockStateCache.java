package malte0811.ferritecore;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.util.math.Box;
import net.minecraft.util.shape.VoxelShape;

public final class FerriteCoreBlockStateCache {
   private static final Map<List<Box>, VoxelShape> COLLISION_SHAPES = new ConcurrentHashMap<>();
   private static final Map<BooleanArrayKey, boolean[]> SOLID_SIDES = new ConcurrentHashMap<>();

   private FerriteCoreBlockStateCache() {
   }

   public static VoxelShape deduplicateCollisionShape(VoxelShape shape) {
      List<Box> key = List.copyOf(shape.getBoundingBoxes());
      return COLLISION_SHAPES.computeIfAbsent(key, ignored -> shape);
   }

   public static boolean[] deduplicateSolidSides(boolean[] solidSides) {
      return SOLID_SIDES.computeIfAbsent(new BooleanArrayKey(solidSides), BooleanArrayKey::values);
   }

   private static final class BooleanArrayKey {
      private final boolean[] values;
      private final int hash;

      private BooleanArrayKey(boolean[] values) {
         this.values = values;
         this.hash = Arrays.hashCode(values);
      }

      private boolean[] values() {
         return this.values;
      }

      @Override
      public boolean equals(Object other) {
         return other instanceof BooleanArrayKey key && Arrays.equals(this.values, key.values);
      }

      @Override
      public int hashCode() {
         return this.hash;
      }
   }
}
