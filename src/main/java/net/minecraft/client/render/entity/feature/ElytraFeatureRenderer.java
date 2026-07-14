package net.minecraft.client.render.entity.feature;

import it.unimi.dsi.fastutil.objects.Object2IntFunction;
import net.irisshaders.iris.shaderpack.materialmap.NamespacedId;
import net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings;
import net.irisshaders.iris.uniforms.CapturedRenderingState;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.equipment.EquipmentModel;
import net.minecraft.client.render.entity.equipment.EquipmentRenderer;
import net.minecraft.client.render.entity.model.ElytraEntityModel;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.LoadedEntityModels;
import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.equipment.EquipmentAsset;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

public class ElytraFeatureRenderer<S extends BipedEntityRenderState, M extends EntityModel<S>> extends FeatureRenderer<S, M> {
   private static final NamespacedId IRIS_ELYTRA_WITH_CAPE_ID = new NamespacedId("minecraft", "elytra_with_cape");
   private final ElytraEntityModel model;
   private final ElytraEntityModel babyModel;
   private final EquipmentRenderer equipmentRenderer;

   public ElytraFeatureRenderer(FeatureRendererContext<S, M> context, LoadedEntityModels loader, EquipmentRenderer equipmentRenderer) {
      super(context);
      this.model = new ElytraEntityModel(loader.getModelPart(EntityModelLayers.ELYTRA));
      this.babyModel = new ElytraEntityModel(loader.getModelPart(EntityModelLayers.ELYTRA_BABY));
      this.equipmentRenderer = equipmentRenderer;
   }

   public void render(MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, S bipedEntityRenderState, float f, float g) {
      ItemStack itemStack = bipedEntityRenderState.equippedChestStack;
      EquippableComponent equippableComponent = (EquippableComponent)itemStack.get(DataComponentTypes.EQUIPPABLE);
      if (equippableComponent != null && !equippableComponent.assetId().isEmpty()) {
         int previousItem = CapturedRenderingState.INSTANCE.getCurrentRenderedItem();
         try {
            this.iris$setElytraContextId(bipedEntityRenderState);
            Identifier identifier = getTexture(bipedEntityRenderState);
            ElytraEntityModel elytraEntityModel = bipedEntityRenderState.baby ? this.babyModel : this.model;
            matrixStack.push();
            try {
               matrixStack.translate(0.0F, 0.0F, 0.125F);
               elytraEntityModel.setAngles(bipedEntityRenderState);
               this.equipmentRenderer
                  .render(
                     EquipmentModel.LayerType.WINGS,
                     (RegistryKey<EquipmentAsset>)equippableComponent.assetId().get(),
                     elytraEntityModel,
                     itemStack,
                     matrixStack,
                     vertexConsumerProvider,
                     i,
                     identifier
                  );
            } finally {
               matrixStack.pop();
            }
         } finally {
            CapturedRenderingState.INSTANCE.setCurrentRenderedItem(previousItem);
         }
      }
   }

   private void iris$setElytraContextId(S state) {
      Object2IntFunction<NamespacedId> itemIds = WorldRenderingSettings.INSTANCE.getItemIds();
      if (itemIds == null) {
         return;
      }

      if (state instanceof PlayerEntityRenderState playerState
         && playerState.skinTextures.capeTexture() != null
         && playerState.capeVisible) {
         CapturedRenderingState.INSTANCE.setCurrentRenderedItem(itemIds.applyAsInt(IRIS_ELYTRA_WITH_CAPE_ID));
         return;
      }

      Identifier identifier = Registries.ITEM.getId(Items.ELYTRA);
      CapturedRenderingState.INSTANCE
         .setCurrentRenderedItem(itemIds.applyAsInt(new NamespacedId(identifier.getNamespace(), identifier.getPath())));
   }

   @Nullable
   private static Identifier getTexture(BipedEntityRenderState state) {
      if (state instanceof PlayerEntityRenderState playerEntityRenderState) {
         SkinTextures skinTextures = playerEntityRenderState.skinTextures;
         if (skinTextures.elytraTexture() != null) {
            return skinTextures.elytraTexture();
         }

         if (skinTextures.capeTexture() != null && playerEntityRenderState.capeVisible) {
            return skinTextures.capeTexture();
         }
      }

      return null;
   }
}
