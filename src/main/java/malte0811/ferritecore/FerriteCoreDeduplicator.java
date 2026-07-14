package malte0811.ferritecore;

import it.unimi.dsi.fastutil.HashCommon;
import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet;
import it.unimi.dsi.fastutil.Hash;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.MultipartBakedModel;
import net.minecraft.resource.SynchronousResourceReloader;
import net.minecraft.state.property.Property;
import net.minecraft.util.math.Direction;

public final class FerriteCoreDeduplicator {
   private static final Map<String, String> VARIANTS = new ConcurrentHashMap<>();
   private static final Map<List<MultipartBakedModel.Selector>, MultipartBakedModel> MULTIPART_MODELS = new ConcurrentHashMap<>();
   private static final Map<List<Predicate<BlockState>>, Predicate<BlockState>> OR_PREDICATES = new ConcurrentHashMap<>();
   private static final Map<List<Predicate<BlockState>>, Predicate<BlockState>> AND_PREDICATES = new ConcurrentHashMap<>();
   private static final Map<PropertyValueKey, Predicate<BlockState>> PROPERTY_PREDICATES = new ConcurrentHashMap<>();
   private static final ObjectOpenCustomHashSet<int[]> QUAD_DATA = new ObjectOpenCustomHashSet<>(new Hash.Strategy<>() {
      @Override
      public int hashCode(int[] data) {
         int result = 0;
         for (int value : data) {
            result = 31 * result + HashCommon.murmurHash3(value);
         }
         return result;
      }

      @Override
      public boolean equals(int[] left, int[] right) {
         return Arrays.equals(left, right);
      }
   });
   private static final Map<Direction, List<BakedQuad>> EMPTY_SIDES;

   static {
      EnumMap<Direction, List<BakedQuad>> empty = new EnumMap<>(Direction.class);
      for (Direction direction : Direction.values()) {
         empty.put(direction, List.of());
      }
      EMPTY_SIDES = empty;
   }

   private FerriteCoreDeduplicator() {
   }

   public static String deduplicateVariant(String variant) {
      return VARIANTS.computeIfAbsent(variant, Function.identity());
   }

   public static int[] deduplicateVertexData(int[] vertexData) {
      synchronized (QUAD_DATA) {
         return QUAD_DATA.addOrGet(vertexData);
      }
   }

   public static MultipartBakedModel makeMultipartModel(List<MultipartBakedModel.Selector> selectors) {
      List<MultipartBakedModel.Selector> key = List.copyOf(selectors);
      return MULTIPART_MODELS.computeIfAbsent(key, MultipartBakedModel::new);
   }

   public static <T> List<Predicate<T>> canonicalize(List<Predicate<T>> predicates) {
      predicates.sort((left, right) -> Integer.compare(left.hashCode(), right.hashCode()));
      if (predicates instanceof ArrayList<Predicate<T>> arrayList) {
         arrayList.trimToSize();
      }
      return predicates;
   }

   public static Predicate<BlockState> or(List<Predicate<BlockState>> predicates) {
      List<Predicate<BlockState>> key = List.copyOf(canonicalize(predicates));
      return OR_PREDICATES.computeIfAbsent(key, values -> state -> {
         for (Predicate<BlockState> predicate : values) {
            if (predicate.test(state)) {
               return true;
            }
         }
         return false;
      });
   }

   public static Predicate<BlockState> and(List<Predicate<BlockState>> predicates) {
      List<Predicate<BlockState>> key = List.copyOf(canonicalize(predicates));
      return AND_PREDICATES.computeIfAbsent(key, values -> state -> {
         for (Predicate<BlockState> predicate : values) {
            if (!predicate.test(state)) {
               return false;
            }
         }
         return true;
      });
   }

   public static Predicate<BlockState> stateHas(Property<?> property, Comparable<?> value) {
      return PROPERTY_PREDICATES.computeIfAbsent(new PropertyValueKey(property, value), key -> state -> state.get(key.property).equals(key.value));
   }

   public static List<BakedQuad> minimizeQuads(List<BakedQuad> quads) {
      return List.copyOf(quads);
   }

   public static Map<Direction, List<BakedQuad>> minimizeSides(Map<Direction, List<BakedQuad>> quadsBySide) {
      if (quadsBySide.isEmpty()) {
         return quadsBySide;
      }
      EnumMap<Direction, List<BakedQuad>> minimized = new EnumMap<>(Direction.class);
      boolean allEmpty = true;
      for (Direction direction : Direction.values()) {
         List<BakedQuad> quads = List.copyOf(quadsBySide.get(direction));
         minimized.put(direction, quads);
         allEmpty &= quads.isEmpty();
      }
      return allEmpty ? EMPTY_SIDES : minimized;
   }

   public static SynchronousResourceReloader createReloadListener() {
      return manager -> clear();
   }

   public static void clear() {
      VARIANTS.clear();
      MULTIPART_MODELS.clear();
      OR_PREDICATES.clear();
      AND_PREDICATES.clear();
      PROPERTY_PREDICATES.clear();
      synchronized (QUAD_DATA) {
         QUAD_DATA.clear();
         QUAD_DATA.trim();
      }
   }

   private record PropertyValueKey(Property<?> property, Comparable<?> value) {
      @Override
      public boolean equals(Object other) {
         return other instanceof PropertyValueKey key && this.property == key.property && java.util.Objects.equals(this.value, key.value);
      }

      @Override
      public int hashCode() {
         return 31 * System.identityHashCode(this.property) + java.util.Objects.hashCode(this.value);
      }
   }
}
