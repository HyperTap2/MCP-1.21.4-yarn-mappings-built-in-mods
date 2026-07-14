package net.irisshaders.iris.uniforms;

import java.util.Objects;
import java.util.stream.StreamSupport;
import net.irisshaders.iris.gl.uniform.UniformHolder;
import net.irisshaders.iris.gl.uniform.UniformUpdateFrequency;
import net.irisshaders.iris.gui.option.IrisVideoSettings;
import net.irisshaders.iris.helpers.JomlConversions;
import net.irisshaders.iris.mixin.GameRendererAccessor;
import net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import org.joml.Math;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class IrisExclusiveUniforms {
   private static final Vector3d ZERO = new Vector3d(0.0);

   public static void addIrisExclusiveUniforms(UniformHolder uniforms) {
      IrisExclusiveUniforms.WorldInfoUniforms.addWorldInfoUniforms(uniforms);
      uniforms.uniform1i(UniformUpdateFrequency.PER_TICK, "currentColorSpace", () -> IrisVideoSettings.colorSpace.ordinal());
      uniforms.uniform1f(UniformUpdateFrequency.PER_FRAME, "thunderStrength", IrisExclusiveUniforms::getThunderStrength);
      uniforms.uniform1f(UniformUpdateFrequency.PER_TICK, "currentPlayerHealth", IrisExclusiveUniforms::getCurrentHealth);
      uniforms.uniform1f(UniformUpdateFrequency.PER_TICK, "maxPlayerHealth", IrisExclusiveUniforms::getMaxHealth);
      uniforms.uniform1f(UniformUpdateFrequency.PER_TICK, "currentPlayerHunger", IrisExclusiveUniforms::getCurrentHunger);
      uniforms.uniform1f(UniformUpdateFrequency.PER_TICK, "maxPlayerHunger", () -> 20);
      uniforms.uniform1f(UniformUpdateFrequency.PER_TICK, "currentPlayerArmor", IrisExclusiveUniforms::getCurrentArmor);
      uniforms.uniform1f(UniformUpdateFrequency.PER_TICK, "maxPlayerArmor", () -> 50);
      uniforms.uniform1f(UniformUpdateFrequency.PER_TICK, "currentPlayerAir", IrisExclusiveUniforms::getCurrentAir);
      uniforms.uniform1f(UniformUpdateFrequency.PER_TICK, "maxPlayerAir", IrisExclusiveUniforms::getMaxAir);
      uniforms.uniform1b(UniformUpdateFrequency.PER_FRAME, "firstPersonCamera", IrisExclusiveUniforms::isFirstPersonCamera);
      uniforms.uniform1b(UniformUpdateFrequency.PER_TICK, "isSpectator", IrisExclusiveUniforms::isSpectator);
      uniforms.uniform1i(UniformUpdateFrequency.PER_FRAME, "currentSelectedBlockId", IrisExclusiveUniforms::getCurrentSelectedBlockId);
      uniforms.uniform3f(UniformUpdateFrequency.PER_FRAME, "currentSelectedBlockPos", IrisExclusiveUniforms::getCurrentSelectedBlockPos);
      uniforms.uniform3d(UniformUpdateFrequency.PER_FRAME, "eyePosition", IrisExclusiveUniforms::getEyePosition);
      uniforms.uniform1f(UniformUpdateFrequency.PER_TICK, "cloudTime", CapturedRenderingState.INSTANCE::getCloudTime);
      uniforms.uniform3d(UniformUpdateFrequency.PER_FRAME, "relativeEyePosition", () -> CameraUniforms.getUnshiftedCameraPosition().sub(getEyePosition()));
      uniforms.uniform3d(
         UniformUpdateFrequency.PER_FRAME,
         "playerLookVector",
         () -> MinecraftClient.getInstance().cameraEntity instanceof LivingEntity livingEntity
            ? JomlConversions.fromVec3(livingEntity.getRotationVec(CapturedRenderingState.INSTANCE.getTickDelta()))
            : ZERO
      );
      uniforms.uniform3d(
         UniformUpdateFrequency.PER_FRAME,
         "playerBodyVector",
         () -> JomlConversions.fromVec3(MinecraftClient.getInstance().getCameraEntity().getRotationVecClient())
      );
      Vector4f zero = new Vector4f(0.0F, 0.0F, 0.0F, 0.0F);
      uniforms.uniform4f(
         UniformUpdateFrequency.PER_TICK,
         "lightningBoltPosition",
         () -> MinecraftClient.getInstance().world != null
            ? StreamSupport.<Entity>stream(MinecraftClient.getInstance().world.getEntities().spliterator(), false)
               .filter(bolt -> bolt instanceof LightningEntity)
               .findAny()
               .map(
                  bolt -> {
                     Vector3d unshiftedCameraPosition = CameraUniforms.getUnshiftedCameraPosition();
                     Vec3d vec3 = bolt.getLerpedPos(MinecraftClient.getInstance().getRenderTickCounter().getTickDelta(true));
                     return new Vector4f(
                        (float)(vec3.x - unshiftedCameraPosition.x),
                        (float)(vec3.y - unshiftedCameraPosition.y),
                        (float)(vec3.z - unshiftedCameraPosition.z),
                        1.0F
                     );
                  }
               )
               .orElse(zero)
            : zero
      );
   }

   private static int getCurrentSelectedBlockId() {
      HitResult hitResult = MinecraftClient.getInstance().crosshairTarget;
      if (MinecraftClient.getInstance().world != null
         && ((GameRendererAccessor)MinecraftClient.getInstance().gameRenderer).shouldRenderBlockOutlineA()
         && hitResult != null
         && hitResult.getType() == Type.BLOCK) {
         BlockPos blockPos4 = ((BlockHitResult)hitResult).getBlockPos();
         BlockState blockState = MinecraftClient.getInstance().world.getBlockState(blockPos4);
         if (!blockState.isAir() && MinecraftClient.getInstance().world.getWorldBorder().contains(blockPos4)) {
            return WorldRenderingSettings.INSTANCE.getBlockStateIds().getInt(blockState);
         }
      }

      return 0;
   }

   private static Vector3f getCurrentSelectedBlockPos() {
      HitResult hitResult = MinecraftClient.getInstance().crosshairTarget;
      if (MinecraftClient.getInstance().world != null
         && ((GameRendererAccessor)MinecraftClient.getInstance().gameRenderer).shouldRenderBlockOutlineA()
         && hitResult != null
         && hitResult.getType() == Type.BLOCK) {
         BlockPos blockPos4 = ((BlockHitResult)hitResult).getBlockPos();
         return blockPos4.toCenterPos().subtract(MinecraftClient.getInstance().gameRenderer.getCamera().getPos()).toVector3f();
      } else {
         return new Vector3f(-256.0F);
      }
   }

   private static float getThunderStrength() {
      return Math.clamp(0.0F, 1.0F, MinecraftClient.getInstance().world.getThunderGradient(CapturedRenderingState.INSTANCE.getTickDelta()));
   }

   private static float getCurrentHealth() {
      return MinecraftClient.getInstance().player != null && MinecraftClient.getInstance().interactionManager.getCurrentGameMode().isSurvivalLike()
         ? MinecraftClient.getInstance().player.getHealth() / MinecraftClient.getInstance().player.getMaxHealth()
         : -1.0F;
   }

   private static float getCurrentHunger() {
      return MinecraftClient.getInstance().player != null && MinecraftClient.getInstance().interactionManager.getCurrentGameMode().isSurvivalLike()
         ? MinecraftClient.getInstance().player.getHungerManager().getFoodLevel() / 20.0F
         : -1.0F;
   }

   private static float getCurrentAir() {
      return MinecraftClient.getInstance().player != null && MinecraftClient.getInstance().interactionManager.getCurrentGameMode().isSurvivalLike()
         ? (float)MinecraftClient.getInstance().player.getAir() / MinecraftClient.getInstance().player.getMaxAir()
         : -1.0F;
   }

   private static float getCurrentArmor() {
      return MinecraftClient.getInstance().player != null && MinecraftClient.getInstance().interactionManager.getCurrentGameMode().isSurvivalLike()
         ? MinecraftClient.getInstance().player.getArmor() / 50.0F
         : -1.0F;
   }

   private static float getMaxAir() {
      return MinecraftClient.getInstance().player != null && MinecraftClient.getInstance().interactionManager.getCurrentGameMode().isSurvivalLike()
         ? MinecraftClient.getInstance().player.getMaxAir()
         : -1.0F;
   }

   private static float getMaxHealth() {
      return MinecraftClient.getInstance().player != null && MinecraftClient.getInstance().interactionManager.getCurrentGameMode().isSurvivalLike()
         ? MinecraftClient.getInstance().player.getMaxHealth()
         : -1.0F;
   }

   private static boolean isFirstPersonCamera() {
      return switch (MinecraftClient.getInstance().options.getPerspective()) {
         case THIRD_PERSON_BACK, THIRD_PERSON_FRONT -> false;
         default -> true;
      };
   }

   private static boolean isSpectator() {
      return MinecraftClient.getInstance().interactionManager.getCurrentGameMode() == GameMode.SPECTATOR;
   }

   private static Vector3d getEyePosition() {
      Objects.requireNonNull(MinecraftClient.getInstance().getCameraEntity());
      Vec3d pos = MinecraftClient.getInstance().getCameraEntity().getCameraPosVec(CapturedRenderingState.INSTANCE.getTickDelta());
      return new Vector3d(pos.x, pos.y, pos.z);
   }

   public static class WorldInfoUniforms {
      public static void addWorldInfoUniforms(UniformHolder uniforms) {
         ClientWorld level = MinecraftClient.getInstance().world;
         uniforms.uniform1i(UniformUpdateFrequency.PER_FRAME, "bedrockLevel", () -> level != null ? level.getDimension().minY() : 0);
         uniforms.uniform1f(UniformUpdateFrequency.PER_FRAME, "cloudHeight", () -> level != null ? level.getDimensionEffects().getCloudsHeight() : 192.0);
         uniforms.uniform1i(UniformUpdateFrequency.PER_FRAME, "heightLimit", () -> level != null ? level.getDimension().height() : 256);
         uniforms.uniform1i(UniformUpdateFrequency.PER_FRAME, "logicalHeightLimit", () -> level != null ? level.getDimension().logicalHeight() : 256);
         uniforms.uniform1b(UniformUpdateFrequency.PER_FRAME, "hasCeiling", () -> level != null ? level.getDimension().hasCeiling() : false);
         uniforms.uniform1b(UniformUpdateFrequency.PER_FRAME, "hasSkylight", () -> level != null ? level.getDimension().hasSkyLight() : true);
         uniforms.uniform1f(UniformUpdateFrequency.PER_FRAME, "ambientLight", () -> level != null ? level.getDimension().ambientLight() : 0.0F);
      }
   }
}
