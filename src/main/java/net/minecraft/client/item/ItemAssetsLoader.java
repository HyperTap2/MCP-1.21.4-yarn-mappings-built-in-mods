package net.minecraft.client.item;

import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.resource.ResourceFinder;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

public class ItemAssetsLoader {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final ResourceFinder FINDER = ResourceFinder.json("items");

   public static CompletableFuture<ItemAssetsLoader.Result> load(ResourceManager resourceManager, Executor executor) {
      return CompletableFuture.supplyAsync(() -> FINDER.findResources(resourceManager), executor)
         .thenCompose(
            resources -> {
               List<CompletableFuture<ItemAssetsLoader.Definition>> list = new ArrayList<>(resources.size());
               resources.forEach(
                  (id, resource) -> list.add(
                     CompletableFuture.supplyAsync(
                        () -> {
                           Identifier identifier2 = FINDER.toResourceId(id);

                           try (Reader reader = resource.getReader()) {
                              ItemAsset itemAsset = ItemAsset.CODEC
                                 .parse(JsonOps.INSTANCE, JsonParser.parseReader(reader))
                                 .ifError(
                                    error -> LOGGER.error(
                                       "Couldn't parse item model '{}' from pack '{}': {}", new Object[]{identifier2, resource.getPackId(), error.message()}
                                    )
                                 )
                                 .result()
                                 .orElse(null);
                              return new ItemAssetsLoader.Definition(identifier2, itemAsset);
                           } catch (Exception exception) {
                              LOGGER.error("Failed to open item model {} from pack '{}'", new Object[]{id, resource.getPackId(), exception});
                              return new ItemAssetsLoader.Definition(identifier2, null);
                           }
                        },
                        executor
                     )
                  )
               );
               return Util.combineSafe(list).thenApply(definitions -> {
                  Map<Identifier, ItemAsset> map = new HashMap<>();

                  for (ItemAssetsLoader.Definition definition : definitions) {
                     if (definition.clientItemInfo != null) {
                        map.put(definition.id, definition.clientItemInfo);
                     }
                  }

                  return new ItemAssetsLoader.Result(map);
               });
            }
         );
   }

   record Definition(Identifier id, @Nullable ItemAsset clientItemInfo) {
   }

   public record Result(Map<Identifier, ItemAsset> contents) {
   }
}
