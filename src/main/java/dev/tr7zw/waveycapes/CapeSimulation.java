package dev.tr7zw.waveycapes;

public final class CapeSimulation {
   public static final int SEGMENTS = 16;
   private final float[] angles = new float[SEGMENTS];
   private final float[] velocities = new float[SEGMENTS];
   private long lastNanos = System.nanoTime();

   public float[] update(float targetAngle, float age, WaveyCapesConfig config) {
      long now = System.nanoTime();
      float dt = Math.min((now - this.lastNanos) / 1_000_000_000.0F, 0.05F);
      this.lastNanos = now;
      if (dt <= 0.0F) return this.angles;
      for (int i = 0; i < SEGMENTS; i++) {
         float chainTarget = i == 0 ? targetAngle : this.angles[i - 1];
         float gravityBend = config.gravity * (i / (float)(SEGMENTS - 1)) * 0.08F;
         float wind = config.windMode == WaveyCapesConfig.WindMode.WAVES
            ? (float)Math.sin(age * 0.12F + i * 0.55F) * config.windStrength * (i / (float)SEGMENTS)
            : 0.0F;
         float acceleration = (chainTarget + gravityBend + wind - this.angles[i]) * config.stiffness - this.velocities[i] * config.damping;
         this.velocities[i] += acceleration * dt;
         this.angles[i] += this.velocities[i] * dt;
         this.angles[i] = Math.clamp(this.angles[i], -25.0F, 165.0F);
      }
      return this.angles;
   }
}
