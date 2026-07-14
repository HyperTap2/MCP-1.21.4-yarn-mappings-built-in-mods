package net.caffeinemc.mods.lithium.common.ai.brain;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.sensor.Sensor;
import net.minecraft.entity.ai.brain.sensor.SensorType;
import net.minecraft.server.world.ServerWorld;

public final class SensorHelper {
   private SensorHelper() {
   }

   public static void disableSensor(LivingEntity entity, SensorType<?> sensorType) {
      if (entity.getWorld().isClient()) {
         return;
      }

      Sensor<?> sensor = entity.getBrain().lithium$getSensor(sensorType);
      if (sensor == null) {
         return;
      }

      long lastSenseTime = sensor.lithium$getLastSenseTime();
      int senseInterval = sensor.lithium$getSenseInterval();
      long disabledTime = Long.MAX_VALUE - Long.MAX_VALUE % senseInterval - senseInterval + lastSenseTime;
      sensor.lithium$setLastSenseTime(disabledTime);
   }

   public static <T extends LivingEntity, U extends Sensor<T>> void enableSensor(T entity, SensorType<U> sensorType) {
      enableSensor(entity, sensorType, false);
   }

   public static <T extends LivingEntity, U extends Sensor<T>> void enableSensor(T entity, SensorType<U> sensorType, boolean extraTick) {
      if (entity.getWorld().isClient()) {
         return;
      }

      Sensor<?> sensor = entity.getBrain().lithium$getSensor(sensorType);
      if (sensor == null) {
         return;
      }

      long lastSenseTime = sensor.lithium$getLastSenseTime();
      int senseInterval = sensor.lithium$getSenseInterval();
      if (lastSenseTime > senseInterval) {
         lastSenseTime %= senseInterval;
         if (extraTick) {
            sensor.lithium$setLastSenseTime(0L);
            ((Sensor<T>)sensor).tick((ServerWorld)entity.getWorld(), entity);
         }
      }

      sensor.lithium$setLastSenseTime(lastSenseTime);
   }
}
