package net.minecraft.client.render.entity.equipment;

import it.unimi.dsi.fastutil.objects.Object2IntFunction;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import net.irisshaders.iris.shaderpack.materialmap.NamespacedId;
import net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings;
import net.irisshaders.iris.uniforms.CapturedRenderingState;
import net.minecraft.client.model.Model;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.DyedColorComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.equipment.EquipmentAsset;
import net.minecraft.item.equipment.trim.ArmorTrim;
import net.minecraft.item.equipment.trim.ArmorTrimMaterial;
import net.minecraft.item.equipment.trim.ArmorTrimPattern;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.ColorHelper;
import org.jetbrains.annotations.Nullable;

public class EquipmentRenderer {
   private static final int field_54178 = 0;
   private final EquipmentModelLoader equipmentModelLoader;
   private final Function<EquipmentRenderer.LayerTextureKey, Identifier> layerTextures;
   private final Function<EquipmentRenderer.TrimSpriteKey, Sprite> trimSprites;

   public EquipmentRenderer(EquipmentModelLoader equipmentModelLoader, SpriteAtlasTexture armorTrimsAtlas) {
      this.equipmentModelLoader = equipmentModelLoader;
      this.layerTextures = Util.memoize(key -> key.layer.getFullTextureId(key.layerType));
      this.trimSprites = Util.memoize(key -> armorTrimsAtlas.getSprite(key.getTexture()));
   }

   public void render(
      EquipmentModel.LayerType layerType,
      RegistryKey<EquipmentAsset> assetKey,
      Model model,
      ItemStack stack,
      MatrixStack matrices,
      VertexConsumerProvider vertexConsumers,
      int light
   ) {
      this.render(layerType, assetKey, model, stack, matrices, vertexConsumers, light, null);
   }

   public void render(
      EquipmentModel.LayerType layerType,
      RegistryKey<EquipmentAsset> assetKey,
      Model model,
      ItemStack stack,
      MatrixStack matrices,
      VertexConsumerProvider vertexConsumers,
      int light,
      @Nullable Identifier texture
   ) {
      int previousItem = CapturedRenderingState.INSTANCE.getCurrentRenderedItem();
      try {
         Object2IntFunction<NamespacedId> itemIds = WorldRenderingSettings.INSTANCE.getItemIds();
         if (itemIds != null) {
            Identifier identifier = (Identifier)stack.get(DataComponentTypes.ITEM_MODEL);
            if (identifier == null) {
               identifier = Registries.ITEM.getId(stack.getItem());
            }

            CapturedRenderingState.INSTANCE
               .setCurrentRenderedItem(itemIds.applyAsInt(new NamespacedId(identifier.getNamespace(), identifier.getPath())));
         }

         List<EquipmentModel.Layer> list = this.equipmentModelLoader.get(assetKey).getLayers(layerType);
         if (!list.isEmpty()) {
            int i = stack.isIn(ItemTags.DYEABLE) ? DyedColorComponent.getColor(stack, 0) : 0;
            boolean bl = stack.hasGlint();

            for (EquipmentModel.Layer layer : list) {
               int j = getDyeColor(layer, i);
               if (j != 0) {
                  Identifier identifier = layer.usePlayerTexture() && texture != null
                     ? texture
                     : this.layerTextures.apply(new EquipmentRenderer.LayerTextureKey(layerType, layer));
                  VertexConsumer vertexConsumer = ItemRenderer.getArmorGlintConsumer(vertexConsumers, RenderLayer.getArmorCutoutNoCull(identifier), bl);
                  model.render(matrices, vertexConsumer, light, OverlayTexture.DEFAULT_UV, j);
                  bl = false;
               }
            }

            ArmorTrim armorTrim = (ArmorTrim)stack.get(DataComponentTypes.TRIM);
            if (armorTrim != null) {
               int equipmentItem = CapturedRenderingState.INSTANCE.getCurrentRenderedItem();
               try {
                  if (itemIds != null) {
                     NamespacedId trimId = new NamespacedId("minecraft", "trim_" + armorTrim.material().value().assetName());
                     CapturedRenderingState.INSTANCE.setCurrentRenderedItem(itemIds.applyAsInt(trimId));
                  }

                  Sprite sprite = this.trimSprites.apply(new EquipmentRenderer.TrimSpriteKey(armorTrim, layerType, assetKey));
                  VertexConsumer vertexConsumer2 = sprite.getTextureSpecificVertexConsumer(
                     vertexConsumers.getBuffer(TexturedRenderLayers.getArmorTrims(((ArmorTrimPattern)armorTrim.pattern().value()).decal()))
                  );
                  model.render(matrices, vertexConsumer2, light, OverlayTexture.DEFAULT_UV);
               } finally {
                  CapturedRenderingState.INSTANCE.setCurrentRenderedItem(equipmentItem);
               }
            }
         }
      } finally {
         CapturedRenderingState.INSTANCE.setCurrentRenderedItem(previousItem);
      }
   }

   private static int getDyeColor(EquipmentModel.Layer layer, int dyeColor) {
      Optional<EquipmentModel.Dyeable> optional = layer.dyeable();
      if (optional.isPresent()) {
         int i = optional.get().colorWhenUndyed().<Integer>map(ColorHelper::fullAlpha).orElse(0);
         return dyeColor != 0 ? dyeColor : i;
      } else {
         return -1;
      }
   }

   record LayerTextureKey(EquipmentModel.LayerType layerType, EquipmentModel.Layer layer) {
   }

   record TrimSpriteKey(ArmorTrim trim, EquipmentModel.LayerType layerType, RegistryKey<EquipmentAsset> equipmentAssetId) {
      private static String getAssetName(RegistryEntry<ArmorTrimMaterial> material, RegistryKey<EquipmentAsset> assetKey) {
         String string = (String)((ArmorTrimMaterial)material.value()).overrideArmorAssets().get(assetKey);
         return string != null ? string : ((ArmorTrimMaterial)material.value()).assetName();
      }

      public Identifier getTexture() {
         Identifier identifier = ((ArmorTrimPattern)this.trim.pattern().value()).assetId();
         String string = getAssetName(this.trim.material(), this.equipmentAssetId);
         return identifier.withPath(path -> "trims/entity/" + this.layerType.asString() + "/" + path + "_" + string);
      }
   }
}
