package net.minecraft.client.texture.atlas;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.client.texture.MissingSprite;
import net.minecraft.client.texture.SpriteContents;
import net.minecraft.client.texture.SpriteOpener;
import me.pepperbell.continuity.client.resource.AtlasLoaderInitContext;
import me.pepperbell.continuity.client.resource.AtlasLoaderLoadContext;
import me.pepperbell.continuity.client.resource.EmissiveSuffixLoader;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceFinder;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;

public class AtlasLoader {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final ResourceFinder FINDER = new ResourceFinder("atlases", ".json");
   private final List<AtlasSource> sources;

   private AtlasLoader(List<AtlasSource> sources) {
      AtlasLoaderInitContext context = AtlasLoaderInitContext.THREAD_LOCAL.get();
      Set<Identifier> extraIds = context == null ? null : context.getExtraIds();
      if (extraIds == null || extraIds.isEmpty()) {
         this.sources = sources;
      } else {
         List<AtlasSource> expanded = new ArrayList<>(extraIds.size() + sources.size());
         for (Identifier extraId : extraIds) {
            expanded.add(new SingleAtlasSource(extraId, Optional.empty()));
         }
         expanded.addAll(sources);
         this.sources = expanded;
      }
   }

   public List<Function<SpriteOpener, SpriteContents>> loadSources(ResourceManager resourceManager) {
      final Map<Identifier, AtlasSource.SpriteRegion> map = new HashMap<>();
      AtlasSource.SpriteRegions spriteRegions = new AtlasSource.SpriteRegions() {
         @Override
         public void add(Identifier arg, AtlasSource.SpriteRegion region) {
            AtlasSource.SpriteRegion spriteRegion = map.put(arg, region);
            if (spriteRegion != null) {
               spriteRegion.close();
            }
         }

         @Override
         public void removeIf(Predicate<Identifier> predicate) {
            Iterator<Entry<Identifier, AtlasSource.SpriteRegion>> iterator = map.entrySet().iterator();

            while (iterator.hasNext()) {
               Entry<Identifier, AtlasSource.SpriteRegion> entry = iterator.next();
               if (predicate.test(entry.getKey())) {
                  entry.getValue().close();
                  iterator.remove();
               }
            }
         }
      };
      this.sources.forEach(source -> source.load(resourceManager, spriteRegions));
      AtlasLoaderLoadContext continuityContext = AtlasLoaderLoadContext.THREAD_LOCAL.get();
      String emissiveSuffix = EmissiveSuffixLoader.getEmissiveSuffix();
      if (continuityContext != null && emissiveSuffix != null) {
         Map<Identifier, AtlasSource.SpriteRegion> emissiveRegions = new HashMap<>();
         Map<Identifier, Identifier> emissiveIds = new HashMap<>();
         map.forEach((id, region) -> {
            if (!id.getPath().endsWith(emissiveSuffix)) {
               Identifier emissiveId = id.withPath(id.getPath() + emissiveSuffix);
               if (map.containsKey(emissiveId)) {
                  emissiveIds.put(id, emissiveId);
               } else {
                  Identifier location = emissiveId.withPath("textures/" + emissiveId.getPath() + ".png");
                  resourceManager.getResource(location).ifPresent(resource -> {
                     emissiveRegions.put(emissiveId, opener -> opener.loadSprite(emissiveId, resource));
                     emissiveIds.put(id, emissiveId);
                  });
               }
            }
         });
         map.putAll(emissiveRegions);
         if (!emissiveIds.isEmpty()) {
            continuityContext.setEmissiveIdMap(emissiveIds);
         }
      }
      Builder<Function<SpriteOpener, SpriteContents>> builder = ImmutableList.builder();
      builder.add((Function<SpriteOpener, SpriteContents>)opener -> MissingSprite.createSpriteContents());
      builder.addAll(map.values());
      return builder.build();
   }

   public static AtlasLoader of(ResourceManager resourceManager, Identifier id) {
      Identifier identifier = FINDER.toResourcePath(id);
      List<AtlasSource> list = new ArrayList<>();

      for (Resource resource : resourceManager.getAllResources(identifier)) {
         try (BufferedReader bufferedReader = resource.getReader()) {
            Dynamic<JsonElement> dynamic = new Dynamic(JsonOps.INSTANCE, JsonParser.parseReader(bufferedReader));
            list.addAll((Collection<? extends AtlasSource>)AtlasSourceManager.LIST_CODEC.parse(dynamic).getOrThrow());
         } catch (Exception exception) {
            LOGGER.error("Failed to parse atlas definition {} in pack {}", new Object[]{identifier, resource.getPackId(), exception});
         }
      }

      return new AtlasLoader(list);
   }
}
