package com.viaversion.viafabricplus.protocoltranslator.impl;

import com.viaversion.viafabricplus.protocoltranslator.ProtocolTranslator;
import com.viaversion.viaversion.api.data.MappingDataLoader;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.libs.gson.JsonElement;
import com.viaversion.viaversion.libs.gson.JsonObject;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.block.Block;
import net.minecraft.registry.Registries;

public final class ViaFabricPlusMappingDataLoader extends MappingDataLoader {
   public static final Map<String, ViaFabricPlusMappingDataLoader.Material> MATERIALS = new HashMap<>();
   public static final Map<String, Map<ProtocolVersion, String>> BLOCK_MATERIALS = new HashMap<>();
   public static final ViaFabricPlusMappingDataLoader INSTANCE = new ViaFabricPlusMappingDataLoader();

   private ViaFabricPlusMappingDataLoader() {
      super(ViaFabricPlusMappingDataLoader.class, "assets/viafabricplus/data/");
      JsonObject materialsData = this.loadData("materials-1.19.4.json");

      for (Entry<String, JsonElement> entry : materialsData.getAsJsonObject("materials").entrySet()) {
         JsonObject materialData = entry.getValue().getAsJsonObject();
         MATERIALS.put(
            entry.getKey(),
            new ViaFabricPlusMappingDataLoader.Material(
               materialData.get("blocksMovement").getAsBoolean(),
               materialData.get("burnable").getAsBoolean(),
               materialData.get("liquid").getAsBoolean(),
               materialData.get("blocksLight").getAsBoolean(),
               materialData.get("replaceable").getAsBoolean(),
               materialData.get("solid").getAsBoolean()
            )
         );
      }

      for (Entry<String, JsonElement> blockEntry : materialsData.getAsJsonObject("blocks").entrySet()) {
         Map<ProtocolVersion, String> blockMaterials = new HashMap<>();

         for (Entry<String, JsonElement> entry : blockEntry.getValue().getAsJsonObject().entrySet()) {
            blockMaterials.put(ProtocolVersion.getClosest(entry.getKey()), entry.getValue().getAsString());
         }

         BLOCK_MATERIALS.put(blockEntry.getKey(), blockMaterials);
      }
   }

   public static String getBlockMaterial(Block block) {
      return getBlockMaterial(block, ProtocolTranslator.getTargetVersion());
   }

   public static String getBlockMaterial(Block block, ProtocolVersion version) {
      if (version.newerThan(ProtocolVersion.v1_19_4)) {
         version = ProtocolVersion.v1_19_4;
      }

      Map<ProtocolVersion, String> materials = BLOCK_MATERIALS.get(Registries.BLOCK.getId(block).toString());
      if (materials == null) {
         return null;
      }

      for (Entry<ProtocolVersion, String> materialEntry : materials.entrySet()) {
         if (version.olderThanOrEqualTo(materialEntry.getKey())) {
            return materialEntry.getValue();
         }
      }

      return null;
   }

   public record Material(boolean blocksMovement, boolean burnable, boolean liquid, boolean blocksLight, boolean replaceable, boolean solid) {
   }
}
