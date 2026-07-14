package net.irisshaders.iris.uniforms;

import java.util.Objects;
import net.irisshaders.iris.gl.uniform.UniformHolder;
import net.irisshaders.iris.gl.uniform.UniformUpdateFrequency;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import org.joml.Vector4f;

public final class CelestialUniforms {
   private final float sunPathRotation;

   public CelestialUniforms(float sunPathRotation) {
      this.sunPathRotation = sunPathRotation;
   }

   public static float getSunAngle() {
      float skyAngle = getSkyAngle();
      return skyAngle < 0.75F ? skyAngle + 0.25F : skyAngle - 0.75F;
   }

   private static float getShadowAngle() {
      float shadowAngle = getSunAngle();
      if (!isDay()) {
         shadowAngle -= 0.5F;
      }

      return shadowAngle;
   }

   private static Vector4f getUpPosition() {
      Vector4f upVector = new Vector4f(0.0F, 100.0F, 0.0F, 0.0F);
      Matrix4f preCelestial = new Matrix4f(CapturedRenderingState.INSTANCE.getGbufferModelView());
      preCelestial.rotate(RotationAxis.POSITIVE_Y.rotationDegrees(-90.0F));
      return preCelestial.transform(upVector);
   }

   public static boolean isDay() {
      return getSunAngle() <= 0.5;
   }

   private static ClientWorld getWorld() {
      return Objects.requireNonNull(MinecraftClient.getInstance().world);
   }

   private static float getSkyAngle() {
      return getWorld().getSkyAngle(CapturedRenderingState.INSTANCE.getTickDelta());
   }

   public void addCelestialUniforms(UniformHolder uniforms) {
      uniforms.uniform1f(UniformUpdateFrequency.PER_FRAME, "sunAngle", CelestialUniforms::getSunAngle)
         .uniformTruncated3f(UniformUpdateFrequency.PER_FRAME, "sunPosition", this::getSunPosition)
         .uniformTruncated3f(UniformUpdateFrequency.PER_FRAME, "moonPosition", this::getMoonPosition)
         .uniform1f(UniformUpdateFrequency.PER_FRAME, "shadowAngle", CelestialUniforms::getShadowAngle)
         .uniformTruncated3f(UniformUpdateFrequency.PER_FRAME, "shadowLightPosition", this::getShadowLightPosition)
         .uniformTruncated3f(UniformUpdateFrequency.PER_FRAME, "upPosition", CelestialUniforms::getUpPosition);
   }

   private Vector4f getSunPosition() {
      return this.getCelestialPosition(100.0F);
   }

   private Vector4f getMoonPosition() {
      return this.getCelestialPosition(-100.0F);
   }

   public Vector4f getShadowLightPosition() {
      return isDay() ? this.getSunPosition() : this.getMoonPosition();
   }

   public Vector4f getShadowLightPositionInWorldSpace() {
      return isDay() ? this.getCelestialPositionInWorldSpace(100.0F) : this.getCelestialPositionInWorldSpace(-100.0F);
   }

   private Vector4f getCelestialPositionInWorldSpace(float y) {
      Vector4f position = new Vector4f(0.0F, y, 0.0F, 0.0F);
      Matrix4f celestial = new Matrix4f();
      celestial.identity();
      celestial.rotate(RotationAxis.POSITIVE_Y.rotationDegrees(-90.0F));
      celestial.rotate(RotationAxis.POSITIVE_Z.rotationDegrees(this.sunPathRotation));
      celestial.rotate(RotationAxis.POSITIVE_X.rotationDegrees(getSkyAngle() * 360.0F));
      celestial.transform(position);
      return position;
   }

   private Vector4f getCelestialPosition(float y) {
      Vector4f position = new Vector4f(0.0F, y, 0.0F, 0.0F);
      Matrix4f celestial = new Matrix4f(CapturedRenderingState.INSTANCE.getGbufferModelView());
      celestial.rotate(RotationAxis.POSITIVE_Y.rotationDegrees(-90.0F));
      celestial.rotate(RotationAxis.POSITIVE_Z.rotationDegrees(this.sunPathRotation));
      celestial.rotate(RotationAxis.POSITIVE_X.rotationDegrees(getSkyAngle() * 360.0F));
      return celestial.transform(position);
   }
}
