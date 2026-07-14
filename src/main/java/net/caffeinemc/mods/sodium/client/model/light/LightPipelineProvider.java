package net.caffeinemc.mods.sodium.client.model.light;

import java.util.EnumMap;
import net.caffeinemc.mods.sodium.client.model.light.data.LightDataAccess;
import net.caffeinemc.mods.sodium.client.model.light.flat.FlatLightPipeline;
import net.caffeinemc.mods.sodium.client.model.light.smooth.SmoothLightPipeline;

public class LightPipelineProvider {
   private final EnumMap<LightMode, LightPipeline> lighters = new EnumMap<>(LightMode.class);

   public LightPipelineProvider(LightDataAccess cache) {
      this.lighters.put(LightMode.SMOOTH, new SmoothLightPipeline(cache));
      this.lighters.put(LightMode.FLAT, new FlatLightPipeline(cache));
   }

   public LightPipeline getLighter(LightMode type) {
      LightPipeline pipeline = this.lighters.get(type);
      if (pipeline == null) {
         throw new NullPointerException("No lighter exists for mode: " + type.name());
      } else {
         return pipeline;
      }
   }
}
