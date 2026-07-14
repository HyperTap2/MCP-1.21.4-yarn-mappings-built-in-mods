package net.caffeinemc.mods.sodium.client.render.frapi;

import java.util.HashMap;
import net.caffeinemc.mods.sodium.client.render.frapi.material.MaterialFinderImpl;
import net.caffeinemc.mods.sodium.client.render.frapi.material.RenderMaterialImpl;
import net.caffeinemc.mods.sodium.client.render.frapi.mesh.MutableMeshImpl;
import net.fabricmc.fabric.api.renderer.v1.Renderer;
import net.fabricmc.fabric.api.renderer.v1.material.MaterialFinder;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableMesh;
import net.minecraft.util.Identifier;

public class SodiumRenderer implements Renderer {
   public static final SodiumRenderer INSTANCE = new SodiumRenderer();
   public static final RenderMaterial STANDARD_MATERIAL = INSTANCE.materialFinder().find();
   private final HashMap<Identifier, RenderMaterial> materialMap = new HashMap<>();

   private SodiumRenderer() {
   }

   public MutableMesh mutableMesh() {
      return new MutableMeshImpl();
   }

   public MaterialFinder materialFinder() {
      return new MaterialFinderImpl();
   }

   public RenderMaterial materialById(Identifier id) {
      return this.materialMap.get(id);
   }

   public boolean registerMaterial(Identifier id, RenderMaterial material) {
      if (this.materialMap.containsKey(id)) {
         return false;
      } else {
         this.materialMap.put(id, (RenderMaterialImpl)material);
         return true;
      }
   }

   static {
      INSTANCE.registerMaterial(RenderMaterial.STANDARD_ID, STANDARD_MATERIAL);
   }
}
