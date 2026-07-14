package net.irisshaders.iris.pipeline;

import com.mojang.blaze3d.platform.GlStateManager;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.shaderpack.materialmap.NamespacedId;
import net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings;
import net.irisshaders.iris.uniforms.SystemTimeUniforms;
import net.minecraft.client.MinecraftClient;
import org.jetbrains.annotations.Nullable;

public class PipelineManager {
   private final Function<NamespacedId, WorldRenderingPipeline> pipelineFactory;
   private final Map<NamespacedId, WorldRenderingPipeline> pipelinesPerDimension = new HashMap<>();
   private WorldRenderingPipeline pipeline = new VanillaRenderingPipeline();
   private int versionCounterForSodiumShaderReload = 0;

   public PipelineManager(Function<NamespacedId, WorldRenderingPipeline> pipelineFactory) {
      this.pipelineFactory = pipelineFactory;
   }

   public WorldRenderingPipeline preparePipeline(NamespacedId currentDimension) {
      if (!this.pipelinesPerDimension.containsKey(currentDimension)) {
         SystemTimeUniforms.COUNTER.reset();
         SystemTimeUniforms.TIMER.reset();
         Iris.logger.info("Creating pipeline for dimension {}", currentDimension);
         this.pipeline = this.pipelineFactory.apply(currentDimension);
         this.pipelinesPerDimension.put(currentDimension, this.pipeline);
         if (WorldRenderingSettings.INSTANCE.isReloadRequired()) {
            if (MinecraftClient.getInstance().worldRenderer != null) {
               MinecraftClient.getInstance().worldRenderer.reload();
            }

            WorldRenderingSettings.INSTANCE.clearReloadRequired();
         }
      } else {
         this.pipeline = this.pipelinesPerDimension.get(currentDimension);
      }

      return this.pipeline;
   }

   @Nullable
   public WorldRenderingPipeline getPipelineNullable() {
      return this.pipeline;
   }

   public Optional<WorldRenderingPipeline> getPipeline() {
      return Optional.ofNullable(this.pipeline);
   }

   public int getVersionCounterForSodiumShaderReload() {
      return this.versionCounterForSodiumShaderReload;
   }

   public void destroyPipeline() {
      this.pipelinesPerDimension.forEach((dimensionId, pipeline) -> {
         Iris.logger.info("Destroying pipeline {}", dimensionId);
         this.resetTextureState();
         pipeline.destroy();
      });
      this.pipelinesPerDimension.clear();
      this.pipeline = null;
      this.versionCounterForSodiumShaderReload++;
   }

   private void resetTextureState() {
      for (int i = 0; i < 16; i++) {
         GlStateManager.glActiveTexture(33984 + i);
         GlStateManager._bindTexture(0);
      }

      GlStateManager.glActiveTexture(33984);
   }
}
