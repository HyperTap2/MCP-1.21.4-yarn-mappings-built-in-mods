package net.irisshaders.iris.uniforms;

import it.unimi.dsi.fastutil.objects.Object2IntFunction;
import net.irisshaders.iris.api.v0.item.IrisItemLightProvider;
import net.irisshaders.iris.gl.uniform.UniformHolder;
import net.irisshaders.iris.gl.uniform.UniformUpdateFrequency;
import net.irisshaders.iris.shaderpack.IdMap;
import net.irisshaders.iris.shaderpack.materialmap.NamespacedId;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

public final class IdMapUniforms {
   private IdMapUniforms() {
   }

   public static void addIdMapUniforms(FrameUpdateNotifier notifier, UniformHolder uniforms, IdMap idMap, boolean isOldHandLight) {
      IdMapUniforms.HeldItemSupplier mainHandSupplier = new IdMapUniforms.HeldItemSupplier(Hand.MAIN_HAND, idMap.getItemIdMap(), isOldHandLight);
      IdMapUniforms.HeldItemSupplier offHandSupplier = new IdMapUniforms.HeldItemSupplier(Hand.OFF_HAND, idMap.getItemIdMap(), false);
      notifier.addListener(mainHandSupplier::update);
      notifier.addListener(offHandSupplier::update);
      uniforms.uniform1i(UniformUpdateFrequency.PER_FRAME, "heldItemId", mainHandSupplier::getIntID)
         .uniform1i(UniformUpdateFrequency.PER_FRAME, "heldItemId2", offHandSupplier::getIntID)
         .uniform1i(UniformUpdateFrequency.PER_FRAME, "heldBlockLightValue", mainHandSupplier::getLightValue)
         .uniform1i(UniformUpdateFrequency.PER_FRAME, "heldBlockLightValue2", offHandSupplier::getLightValue)
         .uniform3f(UniformUpdateFrequency.PER_FRAME, "heldBlockLightColor", mainHandSupplier::getLightColor)
         .uniform3f(UniformUpdateFrequency.PER_FRAME, "heldBlockLightColor2", offHandSupplier::getLightColor);
   }

   private static class HeldItemSupplier {
      private final Hand hand;
      private final Object2IntFunction<NamespacedId> itemIdMap;
      private final boolean applyOldHandLight;
      private int intID;
      private int lightValue;
      private Vector3f lightColor;

      HeldItemSupplier(Hand hand, Object2IntFunction<NamespacedId> itemIdMap, boolean shouldApplyOldHandLight) {
         this.hand = hand;
         this.itemIdMap = itemIdMap;
         this.applyOldHandLight = shouldApplyOldHandLight && hand == Hand.MAIN_HAND;
      }

      private void invalidate() {
         this.intID = -1;
         this.lightValue = 0;
         this.lightColor = IrisItemLightProvider.DEFAULT_LIGHT_COLOR;
      }

      public void update() {
         ClientPlayerEntity player = MinecraftClient.getInstance().player;
         if (player == null) {
            this.invalidate();
         } else {
            ItemStack heldStack = player.getStackInHand(this.hand);
            if (heldStack == null) {
               this.invalidate();
            } else {
               Item heldItem = heldStack.getItem();
               if (heldItem == null) {
                  this.invalidate();
               } else {
                  Identifier heldItemId = (Identifier)heldStack.get(DataComponentTypes.ITEM_MODEL);
                  if (heldItemId == null) {
                     heldItemId = Registries.ITEM.getId(heldItem);
                  }

                  this.intID = this.itemIdMap.applyAsInt(new NamespacedId(heldItemId.getNamespace(), heldItemId.getPath()));
                  IrisItemLightProvider lightProvider = (IrisItemLightProvider)heldItem;
                  this.lightValue = lightProvider.getLightEmission(MinecraftClient.getInstance().player, heldStack);
                  if (this.applyOldHandLight) {
                     lightProvider = this.applyOldHandLighting(player, lightProvider);
                  }

                  this.lightColor = lightProvider.getLightColor(MinecraftClient.getInstance().player, heldStack);
               }
            }
         }
      }

      private IrisItemLightProvider applyOldHandLighting(@NotNull ClientPlayerEntity player, IrisItemLightProvider existing) {
         ItemStack offHandStack = player.getStackInHand(Hand.OFF_HAND);
         if (offHandStack == null) {
            return existing;
         } else {
            Item offHandItem = offHandStack.getItem();
            if (offHandItem == null) {
               return existing;
            } else {
               IrisItemLightProvider lightProvider = (IrisItemLightProvider)offHandItem;
               int newEmission = lightProvider.getLightEmission(MinecraftClient.getInstance().player, offHandStack);
               if (this.lightValue < newEmission) {
                  this.lightValue = newEmission;
                  return lightProvider;
               } else {
                  return existing;
               }
            }
         }
      }

      public int getIntID() {
         return this.intID;
      }

      public int getLightValue() {
         return this.lightValue;
      }

      public Vector3f getLightColor() {
         return this.lightColor;
      }
   }
}
