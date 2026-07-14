package net.irisshaders.iris.uniforms;

import com.mojang.blaze3d.platform.GlStateManager.BlendFuncState;
import com.mojang.blaze3d.systems.RenderSystem;
import net.irisshaders.iris.compat.dh.DHCompat;
import net.irisshaders.iris.gl.state.FogMode;
import net.irisshaders.iris.gl.state.StateUpdateNotifiers;
import net.irisshaders.iris.gl.uniform.DynamicUniformHolder;
import net.irisshaders.iris.gl.uniform.UniformHolder;
import net.irisshaders.iris.gl.uniform.UniformUpdateFrequency;
import net.irisshaders.iris.layer.GbufferPrograms;
import net.irisshaders.iris.mixin.GlStateManagerAccessor;
import net.irisshaders.iris.mixin.statelisteners.BooleanStateAccessor;
import net.irisshaders.iris.mixinterface.LocalPlayerInterface;
import net.irisshaders.iris.pbr.TextureInfoCache;
import net.irisshaders.iris.pbr.TextureTracker;
import net.irisshaders.iris.shaderpack.IdMap;
import net.irisshaders.iris.shaderpack.properties.PackDirectives;
import net.irisshaders.iris.uniforms.transforms.SmoothedFloat;
import net.irisshaders.iris.uniforms.transforms.SmoothedVec2f;
import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.Arm;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LightType;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.joml.Vector3d;
import org.joml.Vector4f;
import org.joml.Vector4i;

public final class CommonUniforms {
   private static final MinecraftClient client = MinecraftClient.getInstance();
   private static final Vector2i ZERO_VECTOR_2i = new Vector2i();
   private static final Vector4i ZERO_VECTOR_4i = new Vector4i(0, 0, 0, 0);
   private static final Vector3d ZERO_VECTOR_3d = new Vector3d();

   private CommonUniforms() {
   }

   public static void addDynamicUniforms(DynamicUniformHolder uniforms, FogMode fogMode) {
      ExternallyManagedUniforms.addExternallyManagedUniforms117(uniforms);
      FogUniforms.addFogUniforms(uniforms, fogMode);
      IrisInternalUniforms.addFogUniforms(uniforms, fogMode);
      uniforms.uniform1i("entityId", CapturedRenderingState.INSTANCE::getCurrentRenderedEntity, StateUpdateNotifiers.fallbackEntityNotifier);
      uniforms.uniform2i("atlasSize", () -> {
         int glId = RenderSystem.getShaderTexture(0);
         if (TextureTracker.INSTANCE.getTexture(glId) instanceof SpriteAtlasTexture atlas) {
            return new Vector2i(atlas.getWidth(), atlas.getHeight());
         } else {
            return ZERO_VECTOR_2i;
         }
      }, listener -> {});
      uniforms.uniform2i("gtextureSize", () -> {
         int glId = GlStateManagerAccessor.getTEXTURES()[0].boundTexture;
         TextureInfoCache.TextureInfo info = TextureInfoCache.INSTANCE.getInfo(glId);
         return new Vector2i(info.getWidth(), info.getHeight());
      }, StateUpdateNotifiers.bindTextureNotifier);
      uniforms.uniform4i(
         "blendFunc",
         () -> {
            BlendFuncState blend = GlStateManagerAccessor.getBLEND();
            return ((BooleanStateAccessor)blend.capState).isEnabled()
               ? new Vector4i(blend.srcFactorRGB, blend.dstFactorRGB, blend.srcFactorAlpha, blend.dstFactorAlpha)
               : ZERO_VECTOR_4i;
         },
         StateUpdateNotifiers.blendFuncNotifier
      );
      uniforms.uniform1i("renderStage", () -> GbufferPrograms.getCurrentPhase().ordinal(), StateUpdateNotifiers.phaseChangeNotifier);
   }

   public static void addCommonUniforms(
      DynamicUniformHolder uniforms, IdMap idMap, PackDirectives directives, FrameUpdateNotifier updateNotifier, FogMode fogMode
   ) {
      addNonDynamicUniforms(uniforms, idMap, directives, updateNotifier);
      addDynamicUniforms(uniforms, fogMode);
   }

   public static void addNonDynamicUniforms(UniformHolder uniforms, IdMap idMap, PackDirectives directives, FrameUpdateNotifier updateNotifier) {
      CameraUniforms.addCameraUniforms(uniforms, updateNotifier);
      ViewportUniforms.addViewportUniforms(uniforms);
      WorldTimeUniforms.addWorldTimeUniforms(uniforms);
      SystemTimeUniforms.addSystemTimeUniforms(uniforms);
      BiomeUniforms.addBiomeUniforms(uniforms);
      new CelestialUniforms(directives.getSunPathRotation()).addCelestialUniforms(uniforms);
      IrisExclusiveUniforms.addIrisExclusiveUniforms(uniforms);
      IrisTimeUniforms.addTimeUniforms(uniforms);
      MatrixUniforms.addMatrixUniforms(uniforms, directives);
      IdMapUniforms.addIdMapUniforms(updateNotifier, uniforms, idMap, directives.isOldHandLight());
      generalCommonUniforms(uniforms, updateNotifier, directives);
   }

   public static void generalCommonUniforms(UniformHolder uniforms, FrameUpdateNotifier updateNotifier, PackDirectives directives) {
      ExternallyManagedUniforms.addExternallyManagedUniforms117(uniforms);
      SmoothedVec2f eyeBrightnessSmooth = new SmoothedVec2f(
         directives.getEyeBrightnessHalfLife(), directives.getEyeBrightnessHalfLife(), CommonUniforms::getEyeBrightness, updateNotifier
      );
      uniforms.uniform1b(UniformUpdateFrequency.PER_FRAME, "hideGUI", () -> client.options.hudHidden)
         .uniform1b(UniformUpdateFrequency.PER_FRAME, "isRightHanded", () -> client.options.getMainArm().getValue() == Arm.RIGHT)
         .uniform1i(UniformUpdateFrequency.PER_FRAME, "isEyeInWater", CommonUniforms::isEyeInWater)
         .uniform1f(UniformUpdateFrequency.PER_FRAME, "blindness", CommonUniforms::getBlindness)
         .uniform1f(UniformUpdateFrequency.PER_FRAME, "darknessFactor", CommonUniforms::getDarknessFactor)
         .uniform1f(UniformUpdateFrequency.PER_FRAME, "darknessLightFactor", CapturedRenderingState.INSTANCE::getDarknessLightFactor)
         .uniform1f(UniformUpdateFrequency.PER_FRAME, "nightVision", CommonUniforms::getNightVision)
         .uniform1b(UniformUpdateFrequency.PER_FRAME, "is_sneaking", CommonUniforms::isSneaking)
         .uniform1b(UniformUpdateFrequency.PER_FRAME, "is_sprinting", CommonUniforms::isSprinting)
         .uniform1b(UniformUpdateFrequency.PER_FRAME, "is_hurt", CommonUniforms::isHurt)
         .uniform1b(UniformUpdateFrequency.PER_FRAME, "is_invisible", CommonUniforms::isInvisible)
         .uniform1b(UniformUpdateFrequency.PER_FRAME, "is_burning", CommonUniforms::isBurning)
         .uniform1b(UniformUpdateFrequency.PER_FRAME, "is_on_ground", CommonUniforms::isOnGround)
         .uniform1f(UniformUpdateFrequency.PER_FRAME, "screenBrightness", () -> (Double)client.options.getGamma().getValue())
         .uniform4f(UniformUpdateFrequency.ONCE, "entityColor", () -> new Vector4f(0.0F, 0.0F, 0.0F, 0.0F))
         .uniform1i(UniformUpdateFrequency.ONCE, "blockEntityId", () -> -1)
         .uniform1i(UniformUpdateFrequency.ONCE, "currentRenderedItemId", () -> -1)
         .uniform1f(UniformUpdateFrequency.ONCE, "pi", () -> Math.PI)
         .uniform1f(UniformUpdateFrequency.PER_TICK, "playerMood", CommonUniforms::getPlayerMood)
         .uniform1f(UniformUpdateFrequency.PER_TICK, "constantMood", CommonUniforms::getConstantMood)
         .uniform2i(UniformUpdateFrequency.PER_FRAME, "eyeBrightness", CommonUniforms::getEyeBrightness)
         .uniform2i(UniformUpdateFrequency.PER_FRAME, "eyeBrightnessSmooth", () -> {
            Vector2f smoothed = eyeBrightnessSmooth.get();
            return new Vector2i((int)smoothed.x(), (int)smoothed.y());
         })
         .uniform1f(UniformUpdateFrequency.PER_TICK, "rainStrength", CommonUniforms::getRainStrength)
         .uniform1f(
            UniformUpdateFrequency.PER_TICK,
            "wetness",
            new SmoothedFloat(directives.getWetnessHalfLife(), directives.getDrynessHalfLife(), CommonUniforms::getRainStrength, updateNotifier)
         )
         .uniform3d(UniformUpdateFrequency.PER_FRAME, "skyColor", CommonUniforms::getSkyColor)
         .uniform1f(UniformUpdateFrequency.PER_FRAME, "dhFarPlane", DHCompat::getFarPlane)
         .uniform1f(UniformUpdateFrequency.PER_FRAME, "dhNearPlane", DHCompat::getNearPlane)
         .uniform1i(UniformUpdateFrequency.PER_FRAME, "dhRenderDistance", DHCompat::getRenderDistance);
   }

   private static boolean isOnGround() {
      return client.player != null && client.player.isOnGround();
   }

   private static boolean isHurt() {
      return client.player != null ? client.player.hurtTime > 0 : false;
   }

   private static boolean isInvisible() {
      return client.player != null ? client.player.isInvisible() : false;
   }

   private static boolean isBurning() {
      return client.player != null ? client.player.isOnFire() : false;
   }

   private static boolean isSneaking() {
      return client.player != null ? client.player.isInSneakingPose() : false;
   }

   private static boolean isSprinting() {
      return client.player != null ? client.player.isSprinting() : false;
   }

   private static Vector3d getSkyColor() {
      if (client.world != null && client.cameraEntity != null) {
         int skyColor = client.world.getSkyColor(client.cameraEntity.getPos(), CapturedRenderingState.INSTANCE.getTickDelta());
         return new Vector3d(ColorHelper.getRedFloat(skyColor), ColorHelper.getGreenFloat(skyColor), ColorHelper.getBlueFloat(skyColor));
      } else {
         return ZERO_VECTOR_3d;
      }
   }

   static float getBlindness() {
      Entity cameraEntity = client.getCameraEntity();
      if (cameraEntity instanceof LivingEntity) {
         StatusEffectInstance blindness = ((LivingEntity)cameraEntity).getStatusEffect(StatusEffects.BLINDNESS);
         if (blindness != null) {
            if (blindness.isInfinite()) {
               return 1.0F;
            }

            return org.joml.Math.clamp(0.0F, 1.0F, blindness.getDuration() / 20.0F);
         }
      }

      return 0.0F;
   }

   static float getDarknessFactor() {
      Entity cameraEntity = client.getCameraEntity();
      if (cameraEntity instanceof LivingEntity) {
         StatusEffectInstance darkness = ((LivingEntity)cameraEntity).getStatusEffect(StatusEffects.DARKNESS);
         if (darkness != null) {
            return darkness.getFadeFactor((LivingEntity)cameraEntity, CapturedRenderingState.INSTANCE.getTickDelta());
         }
      }

      return 0.0F;
   }

   private static float getPlayerMood() {
      return !(client.cameraEntity instanceof ClientPlayerEntity)
         ? 0.0F
         : org.joml.Math.clamp(0.0F, 1.0F, ((ClientPlayerEntity)client.cameraEntity).getMoodPercentage());
   }

   private static float getConstantMood() {
      return !(client.cameraEntity instanceof ClientPlayerEntity)
         ? 0.0F
         : org.joml.Math.clamp(0.0F, 1.0F, ((LocalPlayerInterface)client.cameraEntity).getCurrentConstantMood());
   }

   static float getRainStrength() {
      return client.world == null ? 0.0F : org.joml.Math.clamp(0.0F, 1.0F, client.world.getRainGradient(CapturedRenderingState.INSTANCE.getTickDelta()));
   }

   private static Vector2i getEyeBrightness() {
      if (client.cameraEntity != null && client.world != null) {
         Vec3d feet = client.cameraEntity.getPos();
         Vec3d eyes = new Vec3d(feet.x, client.cameraEntity.getEyeY(), feet.z);
         BlockPos eyeBlockPos = BlockPos.ofFloored(eyes);
         int blockLight = client.world.getLightLevel(LightType.BLOCK, eyeBlockPos);
         int skyLight = client.world.getLightLevel(LightType.SKY, eyeBlockPos);
         return new Vector2i(blockLight * 16, skyLight * 16);
      } else {
         return ZERO_VECTOR_2i;
      }
   }

   private static float getNightVision() {
      if (client.getCameraEntity() instanceof LivingEntity livingEntity) {
         try {
            float nightVisionStrength = GameRenderer.getNightVisionStrength(livingEntity, CapturedRenderingState.INSTANCE.getTickDelta());
            if (nightVisionStrength > 0.0F) {
               return org.joml.Math.clamp(0.0F, 1.0F, nightVisionStrength);
            }
         } catch (NullPointerException e) {
            return 0.0F;
         }
      }

      if (client.player != null && client.player.hasStatusEffect(StatusEffects.CONDUIT_POWER)) {
         float underwaterVisibility = client.player.getUnderwaterVisibility();
         if (underwaterVisibility > 0.0F) {
            return org.joml.Math.clamp(0.0F, 1.0F, underwaterVisibility);
         }
      }

      return 0.0F;
   }

   static int isEyeInWater() {
      CameraSubmersionType submersionType = client.gameRenderer.getCamera().getSubmersionType();
      if (submersionType == CameraSubmersionType.WATER) {
         return 1;
      } else if (submersionType == CameraSubmersionType.LAVA) {
         return 2;
      } else {
         return submersionType == CameraSubmersionType.POWDER_SNOW ? 3 : 0;
      }
   }

   static {
      GbufferPrograms.init();
   }
}
