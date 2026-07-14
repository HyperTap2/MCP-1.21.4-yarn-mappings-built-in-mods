package net.minecraft.client.render.entity.feature;

import it.unimi.dsi.fastutil.objects.Object2IntFunction;
import net.irisshaders.iris.shaderpack.materialmap.NamespacedId;
import net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings;
import net.irisshaders.iris.uniforms.CapturedRenderingState;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.equipment.EquipmentModel;
import net.minecraft.client.render.entity.equipment.EquipmentModelLoader;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.LoadedEntityModels;
import net.minecraft.client.render.entity.model.PlayerCapeModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.equipment.EquipmentAsset;
import net.minecraft.registry.RegistryKey;
import dev.tr7zw.waveycapes.CapeSimulation;
import dev.tr7zw.waveycapes.WaveyCapesConfig;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.Map;

public class CapeFeatureRenderer extends FeatureRenderer<PlayerEntityRenderState, PlayerEntityModel> {
   private static final NamespacedId IRIS_PLAYER_CAPE_ID = new NamespacedId("minecraft", "player_cape");
   private final BipedEntityModel<PlayerEntityRenderState> model;
   private final EquipmentModelLoader equipmentModelLoader;
   private final Map<Integer, CapeSimulation> waveyCapes$simulations = new Int2ObjectOpenHashMap<>();

   public CapeFeatureRenderer(
      FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel> context, LoadedEntityModels modelLoader, EquipmentModelLoader equipmentModelLoader
   ) {
      super(context);
      this.model = new PlayerCapeModel<>(modelLoader.getModelPart(EntityModelLayers.PLAYER_CAPE));
      this.equipmentModelLoader = equipmentModelLoader;
   }

   private boolean hasCustomModelForLayer(ItemStack stack, EquipmentModel.LayerType layerType) {
      EquippableComponent equippableComponent = (EquippableComponent)stack.get(DataComponentTypes.EQUIPPABLE);
      if (equippableComponent != null && !equippableComponent.assetId().isEmpty()) {
         EquipmentModel equipmentModel = this.equipmentModelLoader.get((RegistryKey<EquipmentAsset>)equippableComponent.assetId().get());
         return !equipmentModel.getLayers(layerType).isEmpty();
      } else {
         return false;
      }
   }

   public void render(
      MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, PlayerEntityRenderState playerEntityRenderState, float f, float g
   ) {
      if (!playerEntityRenderState.invisible && playerEntityRenderState.capeVisible) {
         SkinTextures skinTextures = playerEntityRenderState.skinTextures;
         if (skinTextures.capeTexture() != null) {
            if (!this.hasCustomModelForLayer(playerEntityRenderState.equippedChestStack, EquipmentModel.LayerType.WINGS)) {
               int previousItem = CapturedRenderingState.INSTANCE.getCurrentRenderedItem();
               try {
                  Object2IntFunction<NamespacedId> itemIds = WorldRenderingSettings.INSTANCE.getItemIds();
                  if (itemIds != null) {
                     CapturedRenderingState.INSTANCE.setCurrentRenderedItem(itemIds.applyAsInt(IRIS_PLAYER_CAPE_ID));
                  }

                  matrixStack.push();
                  try {
                     if (this.hasCustomModelForLayer(playerEntityRenderState.equippedChestStack, EquipmentModel.LayerType.HUMANOID)) {
                        matrixStack.translate(0.0F, -0.053125F, 0.06875F);
                     }

                     VertexConsumer vertexConsumer = vertexConsumerProvider.getBuffer(RenderLayer.getEntityTranslucent(skinTextures.capeTexture()));
                     WaveyCapesConfig config = WaveyCapesConfig.get();
                     if (config.enabled) {
                        this.waveyCapes$render(matrixStack, vertexConsumer, i, playerEntityRenderState, config);
                     } else {
                        this.getContextModel().copyTransforms(this.model);
                        this.model.setAngles(playerEntityRenderState);
                        this.model.render(matrixStack, vertexConsumer, i, OverlayTexture.DEFAULT_UV);
                     }
                  } finally {
                     matrixStack.pop();
                  }
               } finally {
                  CapturedRenderingState.INSTANCE.setCurrentRenderedItem(previousItem);
               }
            }
         }
      }
   }

   private void waveyCapes$render(
      MatrixStack matrices, VertexConsumer vertices, int light, PlayerEntityRenderState state, WaveyCapesConfig config
   ) {
      CapeSimulation simulation = this.waveyCapes$simulations.computeIfAbsent(state.id, ignored -> new CapeSimulation());
      float baseAngle = 6.0F + state.field_53537 * 0.5F + state.field_53536;
      float[] angles = simulation.update(baseAngle, state.age, config);
      matrices.translate(0.0F, 0.0F, 0.125F);
      float halfWidth = 5.0F / 16.0F;
      float segmentLength = 1.0F / 16.0F;
      float y = 0.0F;
      float z = 0.0F;
      for (int segment = 0; segment < CapeSimulation.SEGMENTS; segment++) {
         float angle = angles[segment];
         if (config.capeStyle == WaveyCapesConfig.CapeStyle.BLOCKY) angle = Math.round(angle / 6.0F) * 6.0F;
         float radians = angle * (float)(Math.PI / 180.0);
         float nextY = y + (float)Math.cos(radians) * segmentLength;
         float nextZ = z + (float)Math.sin(radians) * segmentLength;
         float v0 = (1.0F + segment) / 32.0F;
         float v1 = (2.0F + segment) / 32.0F;
         MatrixStack.Entry entry = matrices.peek();
         this.waveyCapes$quad(vertices, entry, -halfWidth, y, z, halfWidth, nextY, nextZ, v0, v1, light, false);
         this.waveyCapes$quad(vertices, entry, halfWidth, y, z - 0.0125F, -halfWidth, nextY, nextZ - 0.0125F, v0, v1, light, true);
         y = nextY;
         z = nextZ;
      }
   }

   private void waveyCapes$quad(
      VertexConsumer vertices, MatrixStack.Entry entry, float x0, float y0, float z0, float x1, float y1, float z1,
      float v0, float v1, int light, boolean back
   ) {
      float u0 = back ? 12.0F / 64.0F : 1.0F / 64.0F;
      float u1 = back ? 22.0F / 64.0F : 11.0F / 64.0F;
      float normal = back ? -1.0F : 1.0F;
      vertices.vertex(entry, x0, y0, z0).color(-1).texture(u0, v0).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(entry, 0.0F, 0.0F, normal);
      vertices.vertex(entry, x0, y1, z1).color(-1).texture(u0, v1).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(entry, 0.0F, 0.0F, normal);
      vertices.vertex(entry, x1, y1, z1).color(-1).texture(u1, v1).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(entry, 0.0F, 0.0F, normal);
      vertices.vertex(entry, x1, y0, z0).color(-1).texture(u1, v0).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(entry, 0.0F, 0.0F, normal);
   }
}
