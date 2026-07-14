package net.irisshaders.iris.shaderpack.materialmap;

import com.google.common.collect.UnmodifiableIterator;
import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.platform.IrisPlatformHelpers;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList.Named;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Property;
import net.minecraft.util.Identifier;

public class BlockMaterialMapping {
   public static Object2IntMap<BlockState> createBlockStateIdMap(
      Int2ObjectLinkedOpenHashMap<List<BlockEntry>> blockPropertiesMap, Int2ObjectLinkedOpenHashMap<List<TagEntry>> tagPropertiesMap
   ) {
      Object2IntMap<BlockState> blockStateIds = new Object2IntLinkedOpenHashMap();
      blockStateIds.defaultReturnValue(-1);
      blockPropertiesMap.forEach((intId, entries) -> {
         for (BlockEntry entry : entries) {
            addBlockStates(entry, blockStateIds, intId);
         }
      });
      tagPropertiesMap.forEach((intId, entries) -> {
         for (TagEntry entry : entries) {
            addTag(entry, blockStateIds, intId);
         }
      });
      return blockStateIds;
   }

   private static void addTag(TagEntry tagEntry, Object2IntMap<BlockState> idMap, int intId) {
      List<Named<Block>> compatibleTags = Registries.BLOCK
         .streamTags()
         .filter(
            t -> t.getTag().id().getNamespace().equalsIgnoreCase(tagEntry.id().getNamespace())
               && t.getTag().id().getPath().equalsIgnoreCase(tagEntry.id().getName())
         )
         .toList();
      if (compatibleTags.isEmpty()) {
         if (IrisPlatformHelpers.getInstance().isDevelopmentEnvironment()) {
            Iris.logger.warn("Failed to find the tag " + tagEntry.id());
         }
      } else if (compatibleTags.size() > 1) {
         Iris.logger.fatal("You've broke the system; congrats. More than one tag matched " + tagEntry.id());
      } else {
         Registries.BLOCK
            .iterateEntries(compatibleTags.getFirst().getTag())
            .forEach(
               block -> {
                  Map<String, String> propertyPredicates = tagEntry.propertyPredicates();
                  if (!propertyPredicates.isEmpty()) {
                     Map<Property<?>, String> properties = new LinkedHashMap<>();
                     StateManager<Block, BlockState> stateManager = ((Block)block.value()).getStateManager();
                     propertyPredicates.forEach(
                        (key, value) -> {
                           Property<?> property = stateManager.getProperty(key);
                           if (property == null) {
                              Iris.logger.warn("Error while parsing the block ID map entry for tag \"block." + intId + "\":");
                              Iris.logger
                                 .warn(
                                    "- The block " + ((RegistryKey)block.getKey().get()).getValue() + " has no property with the name " + key + ", ignoring!"
                                 );
                           } else {
                              properties.put(property, value);
                           }
                        }
                     );
                     UnmodifiableIterator var7 = stateManager.getStates().iterator();

                     while (var7.hasNext()) {
                        BlockState state = (BlockState)var7.next();
                        if (checkState(state, properties)) {
                           idMap.putIfAbsent(state, intId);
                        }
                     }
                  } else {
                     UnmodifiableIterator properties = ((Block)block.value()).getStateManager().getStates().iterator();

                     while (properties.hasNext()) {
                        BlockState state = (BlockState)properties.next();
                        idMap.putIfAbsent(state, intId);
                     }
                  }
               }
            );
      }
   }

   public static Map<Block, BlockRenderType> createBlockTypeMap(Map<NamespacedId, BlockRenderType> blockPropertiesMap) {
      if (blockPropertiesMap.isEmpty()) {
         return Object2ObjectMaps.emptyMap();
      }

      Map<Block, BlockRenderType> blockTypeIds = new Reference2ReferenceOpenHashMap();
      blockPropertiesMap.forEach((id, blockType) -> {
         Identifier resourceLocation = Identifier.of(id.getNamespace(), id.getName());
         Block block = Registries.BLOCK.getEntry(resourceLocation).<Block>map(RegistryEntry::value).orElse(Blocks.AIR);
         blockTypeIds.put(block, blockType);
      });
      return blockTypeIds;
   }

   public static RenderLayer convertBlockToRenderType(BlockRenderType type) {
      if (type == null) {
         return null;
      }

      return switch (type) {
         case SOLID -> RenderLayer.getSolid();
         case CUTOUT -> RenderLayer.getCutout();
         case CUTOUT_MIPPED -> RenderLayer.getCutoutMipped();
         case TRANSLUCENT -> RenderLayer.getTranslucent();
      };
   }

   private static void addBlockStates(BlockEntry entry, Object2IntMap<BlockState> idMap, int intId) {
      NamespacedId id = entry.id();

      Identifier resourceLocation;
      try {
         resourceLocation = Identifier.of(id.getNamespace(), id.getName());
      } catch (Exception exception) {
         throw new IllegalStateException("Failed to get entry for " + intId, exception);
      }

      Block block = Registries.BLOCK.getEntry(resourceLocation).<Block>map(RegistryEntry::value).orElse(Blocks.AIR);
      if (block != Blocks.AIR) {
         Map<String, String> propertyPredicates = entry.propertyPredicates();
         if (!propertyPredicates.isEmpty()) {
            Map<Property<?>, String> properties = new LinkedHashMap<>();
            StateManager<Block, BlockState> stateManager = block.getStateManager();
            propertyPredicates.forEach((key, value) -> {
               Property<?> property = stateManager.getProperty(key);
               if (property == null) {
                  Iris.logger.warn("Error while parsing the block ID map entry for \"block." + intId + "\":");
                  Iris.logger.warn("- The block " + resourceLocation + " has no property with the name " + key + ", ignoring!");
               } else {
                  properties.put(property, value);
               }
            });
            UnmodifiableIterator var9 = stateManager.getStates().iterator();

            while (var9.hasNext()) {
               BlockState state = (BlockState)var9.next();
               if (checkState(state, properties)) {
                  idMap.putIfAbsent(state, intId);
               }
            }
         } else {
            UnmodifiableIterator properties = block.getStateManager().getStates().iterator();

            while (properties.hasNext()) {
               BlockState state = (BlockState)properties.next();
               idMap.putIfAbsent(state, intId);
            }
         }
      }
   }

   private static boolean checkState(BlockState state, Map<Property<?>, String> expectedValues) {
      for (java.util.Map.Entry<Property<?>, String> condition : expectedValues.entrySet()) {
         Property property = condition.getKey();
         String expectedValue = condition.getValue();
         String actualValue = property.name(state.get(property));
         if (!expectedValue.equals(actualValue)) {
            return false;
         }
      }

      return true;
   }
}
