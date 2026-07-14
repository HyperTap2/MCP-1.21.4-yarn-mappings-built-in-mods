package net.minecraft.client.render.model;

import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.builders.IBufferGraph;
import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.builders.IAcceleratedVertexConsumer;
import com.github.argon4w.acceleratedrendering.core.meshes.IMesh;
import com.github.argon4w.acceleratedrendering.core.meshes.collectors.CulledMeshCollector;
import com.github.argon4w.acceleratedrendering.features.items.AcceleratedItemRenderingFeature;
import com.github.argon4w.acceleratedrendering.features.items.IAcceleratedBakedQuad;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.Map;
import java.util.Collections;
import java.util.WeakHashMap;
import malte0811.ferritecore.FerriteCoreDeduplicator;
import net.caffeinemc.mods.sodium.client.model.quad.BakedQuadView;
import net.caffeinemc.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import net.caffeinemc.mods.sodium.client.model.quad.properties.ModelQuadFlags;
import net.caffeinemc.mods.sodium.client.util.ModelQuadUtil;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.ColorHelper;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class BakedQuad implements BakedQuadView, IAcceleratedBakedQuad {
   private static final Map<int[], Map<IBufferGraph, IMesh>> ACCELERATED_MESHES = Collections.synchronizedMap(new WeakHashMap<>());
   protected final int[] vertexData;
   protected final int tintIndex;
   protected final Direction face;
   protected final Sprite sprite;
   private final boolean shade;
   private final int lightEmission;
   private final int sodium$flags;
   private final int sodium$normal;
   private final ModelQuadFacing sodium$normalFace;

   public BakedQuad(int[] vertexData, int tintIndex, Direction face, Sprite sprite, boolean shade, int lightEmission) {
      this.vertexData = FerriteCoreDeduplicator.deduplicateVertexData(vertexData);
      this.tintIndex = tintIndex;
      this.face = face;
      this.sprite = sprite;
      this.shade = shade;
      this.lightEmission = lightEmission;
      this.sodium$normal = this.calculateNormal();
      this.sodium$normalFace = ModelQuadFacing.fromPackedNormal(this.sodium$normal);
      this.sodium$flags = ModelQuadFlags.getQuadFlags(this, face);
   }

   public Sprite getSprite() {
      return this.sprite;
   }

   public int[] getVertexData() {
      return this.vertexData;
   }

   public boolean hasTint() {
      return this.tintIndex != -1;
   }

   public int getTintIndex() {
      return this.tintIndex;
   }

   public Direction getFace() {
      return this.face;
   }

   public boolean hasShade() {
      return this.shade;
   }

   public int getLightEmission() {
      return this.lightEmission;
   }

   @Override
   public float getX(int index) {
      return Float.intBitsToFloat(this.vertexData[ModelQuadUtil.vertexOffset(index)]);
   }

   @Override
   public float getY(int index) {
      return Float.intBitsToFloat(this.vertexData[ModelQuadUtil.vertexOffset(index) + 1]);
   }

   @Override
   public float getZ(int index) {
      return Float.intBitsToFloat(this.vertexData[ModelQuadUtil.vertexOffset(index) + 2]);
   }

   @Override
   public int getColor(int index) {
      return this.vertexData[ModelQuadUtil.vertexOffset(index) + 3];
   }

   @Override
   public float getTexU(int index) {
      return Float.intBitsToFloat(this.vertexData[ModelQuadUtil.vertexOffset(index) + 4]);
   }

   @Override
   public float getTexV(int index) {
      return Float.intBitsToFloat(this.vertexData[ModelQuadUtil.vertexOffset(index) + 5]);
   }

   @Override
   public int getLight(int index) {
      return this.vertexData[ModelQuadUtil.vertexOffset(index) + 6];
   }

   @Override
   public int getVertexNormal(int index) {
      return this.vertexData[ModelQuadUtil.vertexOffset(index) + 7];
   }

   @Override
   public int getFlags() {
      return this.sodium$flags;
   }

   @Override
   public int getFaceNormal() {
      return this.sodium$normal;
   }

   @Override
   public ModelQuadFacing getNormalFace() {
      return this.sodium$normalFace;
   }

   @Override
   public Direction getLightFace() {
      return this.face;
   }

   @Override
   public int getMaxLightQuad(int index) {
      return LightmapTextureManager.applyEmission(this.getLight(index), this.lightEmission);
   }

   @Override
   public boolean hasAO() {
      return true;
   }

   @Override
   public void renderFast(
      Matrix4f transform,
      Matrix3f normal,
      IAcceleratedVertexConsumer consumer,
      int light,
      int overlay,
      int color
   ) {
      Map<IBufferGraph, IMesh> meshes = ACCELERATED_MESHES.computeIfAbsent(this.vertexData, ignored -> new Object2ObjectOpenHashMap<>());
      IMesh mesh = meshes.get(consumer);
      if (mesh == null) {
         CulledMeshCollector collector = new CulledMeshCollector(
            consumer.getRenderType(), consumer.getBufferSet().getBufferEnvironment().getLayout()
         );
         net.minecraft.client.render.VertexConsumer builder = consumer.decorate(collector);

         for (int vertex = 0; vertex < this.vertexData.length / 8; vertex++) {
            int offset = vertex * 8;
            int packedNormal = this.vertexData[offset + 7];
            builder.vertex(
               Float.intBitsToFloat(this.vertexData[offset]),
               Float.intBitsToFloat(this.vertexData[offset + 1]),
               Float.intBitsToFloat(this.vertexData[offset + 2]),
               ColorHelper.fromAbgr(this.vertexData[offset + 3]),
               Float.intBitsToFloat(this.vertexData[offset + 4]),
               Float.intBitsToFloat(this.vertexData[offset + 5]),
               overlay,
               this.vertexData[offset + 6],
               (byte)(packedNormal & 0xFF) / 127.0F,
               (byte)(packedNormal >> 8 & 0xFF) / 127.0F,
               (byte)(packedNormal >> 16 & 0xFF) / 127.0F
            );
         }

         collector.flush();
         mesh = AcceleratedItemRenderingFeature.getMeshType().getBuilder().build(collector);
         meshes.put(consumer, mesh);
      }

      mesh.write(consumer, this.getCustomColor(color), light, overlay);
   }

   @Override
   public int getCustomColor(int color) {
      return this.hasTint() ? color : -1;
   }

   public static void clearAcceleratedMeshes() {
      synchronized (ACCELERATED_MESHES) {
         ACCELERATED_MESHES.clear();
      }
   }
}
