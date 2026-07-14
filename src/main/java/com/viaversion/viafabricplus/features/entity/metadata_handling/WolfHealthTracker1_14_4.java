package com.viaversion.viafabricplus.features.entity.metadata_handling;

import com.viaversion.viafabricplus.protocoltranslator.ProtocolTranslator;
import com.viaversion.viaversion.api.connection.StorableObject;
import it.unimi.dsi.fastutil.ints.Int2FloatMap;
import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;
import net.minecraft.entity.LivingEntity;

public final class WolfHealthTracker1_14_4 implements StorableObject {
   private final Int2FloatMap healthDataMap = new Int2FloatOpenHashMap();

   public static float getWolfHealth(LivingEntity entity) {
      WolfHealthTracker1_14_4 tracker = (WolfHealthTracker1_14_4)ProtocolTranslator.getPlayNetworkUserConnection().get(WolfHealthTracker1_14_4.class);
      return tracker != null ? tracker.getWolfHealth(entity.getId(), entity.getHealth()) : entity.getHealth();
   }

   public float getWolfHealth(int entityId, float fallback) {
      return this.healthDataMap.getOrDefault(entityId, fallback);
   }

   public void setWolfHealth(int entityId, float wolfHealth) {
      this.healthDataMap.put(entityId, wolfHealth);
   }
}
