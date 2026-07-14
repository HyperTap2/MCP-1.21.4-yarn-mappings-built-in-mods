package net.caffeinemc.mods.sodium.client.render.frapi.material;

import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.util.TriState;

public class RenderMaterialImpl extends MaterialViewImpl implements RenderMaterial {
   public static final int VALUE_COUNT = 1 << TOTAL_BIT_LENGTH;
   private static final RenderMaterialImpl[] BY_INDEX = new RenderMaterialImpl[VALUE_COUNT];

   private RenderMaterialImpl(int bits) {
      super(bits);
   }

   public int index() {
      return this.bits;
   }

   public static RenderMaterialImpl byIndex(int index) {
      return BY_INDEX[index];
   }

   public static RenderMaterialImpl setDisableDiffuse(RenderMaterialImpl material, boolean disable) {
      return material.disableDiffuse() != disable ? byIndex(disable ? material.bits | DIFFUSE_FLAG : material.bits & ~DIFFUSE_FLAG) : material;
   }

   public static RenderMaterialImpl setAmbientOcclusion(RenderMaterialImpl material, TriState mode) {
      return material.ambientOcclusion() != mode ? byIndex(material.bits & ~AO_MASK | mode.ordinal() << AO_BIT_OFFSET) : material;
   }

   static {
      for (int i = 0; i < VALUE_COUNT; i++) {
         if (areBitsValid(i)) {
            BY_INDEX[i] = new RenderMaterialImpl(i);
         }
      }
   }
}
