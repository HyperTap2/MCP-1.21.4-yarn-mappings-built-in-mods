package com.github.argon4w.acceleratedrendering.features.items;

import com.github.argon4w.acceleratedrendering.AcceleratedRendering;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

/** Caches deterministic item-model quad lookup without retaining reloaded models. */
public final class AcceleratedItemModelCache {
   private static final Map<BakedModel, Entry> CACHE = Collections.synchronizedMap(new WeakHashMap<>());

   private AcceleratedItemModelCache() {
   }

   public static List<BakedQuad> getQuads(BakedModel model) {
      if (!AcceleratedRendering.isAvailable()) {
         return null;
      }

      long generation = AcceleratedRendering.getResourceGeneration();
      Entry entry = CACHE.get(model);
      if (entry != null && entry.generation == generation) {
         return entry.quads;
      }

      Random random = Random.create();
      List<BakedQuad> quads = new ArrayList<>();
      for (Direction direction : Direction.values()) {
         random.setSeed(42L);
         quads.addAll(model.getQuads(null, direction, random));
      }
      random.setSeed(42L);
      quads.addAll(model.getQuads(null, null, random));
      List<BakedQuad> immutable = List.copyOf(quads);
      CACHE.put(model, new Entry(generation, immutable));
      return immutable;
   }

   private record Entry(long generation, List<BakedQuad> quads) {
   }
}
