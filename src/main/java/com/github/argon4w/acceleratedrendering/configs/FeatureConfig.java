package com.github.argon4w.acceleratedrendering.configs;

import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.pools.meshes.MeshInfoCacheType;
import com.github.argon4w.acceleratedrendering.core.meshes.MeshType;
import java.util.Locale;
import java.util.function.Function;

/** System-property-backed settings for the source-integrated renderer. */
public final class FeatureConfig {
   public static final FeatureConfig CONFIG = new FeatureConfig();

   public final IntValue corePooledBufferSetSize = integer("core.pooledBufferSetSize", 8, 1);
   public final IntValue corePooledElementBufferSize = integer("core.pooledElementBufferSize", 32, 1);
   public final IntValue coreCachedImageSize = integer("core.cachedImageSize", 32, 1);
   // Synchronous OpenGL debug output serializes command submission and is therefore
   // opt-in. In particular, it must not silently undo Sodium's NVIDIA performance
   // setting when no AcceleratedRendering system-property override was supplied.
   public final Value<FeatureStatus> coreDebugContextEnabled = feature("core.debugContext", FeatureStatus.DISABLED);
   public final Value<FeatureStatus> coreForceTranslucentAcceleration = feature("core.forceTranslucentAcceleration", FeatureStatus.DISABLED);
   public final Value<FeatureStatus> coreCacheIdenticalPose = feature("core.cacheIdenticalPose", FeatureStatus.ENABLED);
   public final Value<MeshInfoCacheType> coreMeshInfoCacheType = enumeration("core.meshInfoCacheType", MeshInfoCacheType.HANDLE, MeshInfoCacheType::valueOf);
   public final Value<FeatureStatus> acceleratedEntityRenderingFeatureStatus = feature("entities.enabled", FeatureStatus.ENABLED);
   // This source port does not yet provide the BASEVERTEX backend used by the
   // accelerated entity path. Keep it available as an explicit system-property opt-in.
   public final Value<PipelineSetting> acceleratedEntityRenderingDefaultPipeline = pipeline("entities.pipeline", PipelineSetting.VANILLA);
   public final Value<MeshType> acceleratedEntityRenderingMeshType = enumeration("entities.meshType", MeshType.SERVER, MeshType::valueOf);
   public final Value<FeatureStatus> acceleratedEntityRenderingGuiAcceleration = feature("entities.guiAcceleration", FeatureStatus.DISABLED);
   public final Value<FeatureStatus> acceleratedTextRenderingFeatureStatus = feature("text.enabled", FeatureStatus.ENABLED);
   public final Value<PipelineSetting> acceleratedTextRenderingDefaultPipeline = pipeline("text.pipeline", PipelineSetting.VANILLA);
   public final Value<MeshType> acceleratedTextRenderingMeshType = enumeration("text.meshType", MeshType.SERVER, MeshType::valueOf);
   public final Value<FeatureStatus> acceleratedItemRenderingFeatureStatus = feature("items.enabled", FeatureStatus.ENABLED);
   public final Value<FeatureStatus> acceleratedItemRenderingBakeMeshForQuads = feature("items.bakeMeshForQuads", FeatureStatus.ENABLED);
   public final Value<PipelineSetting> acceleratedItemRenderingDefaultPipeline = pipeline("items.pipeline", PipelineSetting.VANILLA);
   public final Value<MeshType> acceleratedItemRenderingMeshType = enumeration("items.meshType", MeshType.SERVER, MeshType::valueOf);
   public final Value<FeatureStatus> acceleratedItemRenderingHandAcceleration = feature("items.handAcceleration", FeatureStatus.ENABLED);
   public final Value<FeatureStatus> acceleratedItemRenderingGuiAcceleration = feature("items.guiAcceleration", FeatureStatus.ENABLED);
   public final Value<FeatureStatus> acceleratedItemRenderingGuiItemBatching = feature("items.guiItemBatching", FeatureStatus.DISABLED);
   public final Value<FeatureStatus> acceleratedItemRenderingMergeGuiItemBatches = feature("items.mergeGuiItemBatches", FeatureStatus.ENABLED);
   public final Value<FeatureStatus> orientationCullingFeatureStatus = feature("culling.enabled", FeatureStatus.ENABLED);
   public final Value<FeatureStatus> orientationCullingDefaultCulling = feature("culling.default", FeatureStatus.ENABLED);
   public final Value<FeatureStatus> orientationCullingIgnoreCullState = feature("culling.ignoreRenderLayerState", FeatureStatus.DISABLED);
   public final Value<FeatureStatus> irisCompatFeatureStatus = feature("iris.enabled", FeatureStatus.ENABLED);
   public final Value<FeatureStatus> irisCompatOrientationCullingCompat = feature("iris.orientationCulling", FeatureStatus.ENABLED);
   public final Value<FeatureStatus> irisCompatShadowCulling = feature("iris.shadowCulling", FeatureStatus.ENABLED);
   public final Value<FeatureStatus> irisCompatPolygonProcessing = feature("iris.polygonProcessing", FeatureStatus.ENABLED);

   private FeatureConfig() {
   }

   private static IntValue integer(String name, int fallback, int minimum) {
      int value = Integer.getInteger(key(name), fallback);
      return new IntValue(Math.max(value, minimum));
   }

   private static Value<FeatureStatus> feature(String name, FeatureStatus fallback) {
      return enumeration(name, fallback, FeatureStatus::valueOf);
   }

   private static Value<PipelineSetting> pipeline(String name, PipelineSetting fallback) {
      return enumeration(name, fallback, PipelineSetting::valueOf);
   }

   private static <T> Value<T> enumeration(String name, T fallback, Function<String, T> parser) {
      String property = System.getProperty(key(name));
      if (property == null) {
         return new Value<>(fallback);
      }

      try {
         return new Value<>(parser.apply(property.toUpperCase(Locale.ROOT)));
      } catch (IllegalArgumentException ignored) {
         return new Value<>(fallback);
      }
   }

   private static String key(String name) {
      return "acceleratedrendering." + name;
   }

   public record Value<T>(T get) {
      public T get() {
         return this.get;
      }
   }

   public record IntValue(int getAsInt) {
      public int getAsInt() {
         return this.getAsInt;
      }
   }
}
