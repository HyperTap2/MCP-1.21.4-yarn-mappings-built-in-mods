package com.github.argon4w.acceleratedrendering.core;

import com.github.argon4w.acceleratedrendering.compat.iris.programs.IrisPrograms;
import com.github.argon4w.acceleratedrendering.core.programs.ComputeShaderPrograms;
import com.github.argon4w.acceleratedrendering.core.programs.LoadComputeShaderEvent;
import com.github.argon4w.acceleratedrendering.core.programs.culling.ICullingProgramSelector;
import com.github.argon4w.acceleratedrendering.core.programs.culling.LoadCullingProgramSelectorEvent;
import com.github.argon4w.acceleratedrendering.core.programs.processing.IPolygonProcessor;
import com.github.argon4w.acceleratedrendering.core.programs.processing.LoadPolygonProcessorEvent;
import com.github.argon4w.acceleratedrendering.features.culling.OrientationCullingPrograms;
import com.google.common.collect.ImmutableMap;
import net.minecraft.client.render.VertexFormat;

/** Builds the renderer's compute-program selections without a loader runtime. */
public final class AcceleratedRenderingRegistry {
   private AcceleratedRenderingRegistry() {
   }

   public static LoadComputeShaderEvent createShaderEvent() {
      LoadComputeShaderEvent event = new LoadComputeShaderEvent(ImmutableMap.builder());
      ComputeShaderPrograms.onLoadComputeShaders(event);
      OrientationCullingPrograms.onLoadComputeShaders(event);
      IrisPrograms.onLoadComputeShaders(event);
      return event;
   }

   public static ICullingProgramSelector createCullingSelector(VertexFormat format) {
      LoadCullingProgramSelectorEvent event = new LoadCullingProgramSelectorEvent(format);
      OrientationCullingPrograms.onLoadCullingPrograms(event);
      IrisPrograms.onLoadCullingPrograms(event);
      return event.getSelector();
   }

   public static IPolygonProcessor createPolygonProcessor(VertexFormat format) {
      LoadPolygonProcessorEvent event = new LoadPolygonProcessorEvent(format);
      IrisPrograms.onLoadPolygonProcessors(event);
      return event.getProcessor();
   }
}
