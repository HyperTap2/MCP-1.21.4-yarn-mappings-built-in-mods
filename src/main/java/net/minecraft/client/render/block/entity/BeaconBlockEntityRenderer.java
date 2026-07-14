package net.minecraft.client.render.block.entity;

import java.util.List;
import java.util.Objects;
import me.flashyreese.mods.sodiumextra.client.SodiumExtraClientMod;
import me.flashyreese.mods.sodiumextra.compat.IrisCompat;
import net.caffeinemc.mods.sodium.api.math.MatrixHelper;
import net.caffeinemc.mods.sodium.api.util.ColorARGB;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.caffeinemc.mods.sodium.api.vertex.format.common.EntityVertex;
import net.minecraft.block.entity.BeaconBlockEntity;
import net.minecraft.block.entity.BeaconBlockEntity.BeamSegment;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Box;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;

public class BeaconBlockEntityRenderer implements BlockEntityRenderer<BeaconBlockEntity> {
   public static final Identifier BEAM_TEXTURE = Identifier.ofVanilla("textures/entity/beacon_beam.png");
   public static final int MAX_BEAM_HEIGHT = 1024;

   public BeaconBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {
   }

   public void render(BeaconBlockEntity beaconBlockEntity, float f, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, int j) {
      boolean renderOptionsEnabled = SodiumExtraClientMod.isMixinEnabled("render.block.entity.MixinBeaconRenderer");
      if (renderOptionsEnabled && !SodiumExtraClientMod.options().renderSettings.beaconBeam) {
         return;
      }

      Frustum frustum = SodiumExtraClientMod.isMixinEnabled("optimizations.beacon_beam_rendering.MixinBeaconRenderer")
         ? MinecraftClient.getInstance().worldRenderer.getCullingFrustum()
         : null;
      if (frustum != null) {
         Box box = new Box(
            beaconBlockEntity.getPos().getX() - 1.0,
            beaconBlockEntity.getPos().getY() - 1.0,
            beaconBlockEntity.getPos().getZ() - 1.0,
            beaconBlockEntity.getPos().getX() + 1.0,
            beaconBlockEntity.getPos().getY() + (beaconBlockEntity.getBeamSegments().isEmpty() ? 1.0 : 1024.0),
            beaconBlockEntity.getPos().getZ() + 1.0
         );
         if (!frustum.isVisible(box)) {
            return;
         }
      }

      long l = beaconBlockEntity.getWorld().getTime();
      List<BeamSegment> list = beaconBlockEntity.getBeamSegments();
      int k = 0;

      for (int m = 0; m < list.size(); m++) {
         BeamSegment beamSegment = list.get(m);
         int maxY = m == list.size() - 1 ? 1024 : beamSegment.getHeight();
         if (renderOptionsEnabled && maxY == 1024 && SodiumExtraClientMod.options().renderSettings.limitBeaconBeamHeight) {
            int lastSegment = beaconBlockEntity.getPos().getY() + k;
            maxY = Objects.requireNonNull(beaconBlockEntity.getWorld()).getTopYInclusive() - lastSegment;
         }

         renderBeam(matrixStack, vertexConsumerProvider, f, l, k, maxY, beamSegment.getColor());
         k += beamSegment.getHeight();
      }
   }

   private static void renderBeam(
      MatrixStack matrices, VertexConsumerProvider vertexConsumers, float tickDelta, long worldTime, int yOffset, int maxY, int color
   ) {
      renderBeam(matrices, vertexConsumers, BEAM_TEXTURE, tickDelta, 1.0F, worldTime, yOffset, maxY, color, 0.2F, 0.25F);
   }

   public static void renderBeam(
      MatrixStack matrices,
      VertexConsumerProvider vertexConsumers,
      Identifier textureId,
      float tickDelta,
      float heightScale,
      long worldTime,
      int yOffset,
      int maxY,
      int color,
      float innerRadius,
      float outerRadius
   ) {
      if (SodiumExtraClientMod.isMixinEnabled("optimizations.beacon_beam_rendering.MixinBeaconRenderer")) {
         if (!IrisCompat.isIrisPresent() || !IrisCompat.isRenderingShadowPass()) {
            renderBeamOptimized(
               matrices, vertexConsumers, textureId, tickDelta, heightScale, worldTime, yOffset, maxY, color, innerRadius, outerRadius
            );
         }
         return;
      }

      int i = yOffset + maxY;
      matrices.push();
      matrices.translate(0.5, 0.0, 0.5);
      float f = Math.floorMod(worldTime, 40) + tickDelta;
      float g = maxY < 0 ? f : -f;
      float h = MathHelper.fractionalPart(g * 0.2F - MathHelper.floor(g * 0.1F));
      matrices.push();
      matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(f * 2.25F - 45.0F));
      float j = 0.0F;
      float k = innerRadius;
      float l = innerRadius;
      float m = 0.0F;
      float n = -innerRadius;
      float o = 0.0F;
      float p = 0.0F;
      float q = -innerRadius;
      float r = 0.0F;
      float s = 1.0F;
      float t = -1.0F + h;
      float u = maxY * heightScale * (0.5F / innerRadius) + t;
      renderBeamLayer(
         matrices,
         vertexConsumers.getBuffer(RenderLayer.getBeaconBeam(textureId, false)),
         color,
         yOffset,
         i,
         0.0F,
         k,
         l,
         0.0F,
         n,
         0.0F,
         0.0F,
         q,
         0.0F,
         1.0F,
         u,
         t
      );
      matrices.pop();
      j = -outerRadius;
      k = -outerRadius;
      l = outerRadius;
      m = -outerRadius;
      n = -outerRadius;
      o = outerRadius;
      p = outerRadius;
      q = outerRadius;
      r = 0.0F;
      s = 1.0F;
      t = -1.0F + h;
      u = maxY * heightScale + t;
      renderBeamLayer(
         matrices,
         vertexConsumers.getBuffer(RenderLayer.getBeaconBeam(textureId, true)),
         ColorHelper.withAlpha(32, color),
         yOffset,
         i,
         j,
         k,
         l,
         m,
         n,
         o,
         p,
         q,
         0.0F,
         1.0F,
         u,
         t
      );
      matrices.pop();
   }

   private static void renderBeamOptimized(
      MatrixStack matrices,
      VertexConsumerProvider vertexConsumers,
      Identifier textureId,
      float tickDelta,
      float heightScale,
      long worldTime,
      int yOffset,
      int maxY,
      int color,
      float innerRadius,
      float outerRadius
   ) {
      int height = yOffset + maxY;
      matrices.push();
      matrices.translate(0.5, 0.0, 0.5);
      float time = Math.floorMod(worldTime, 40) + tickDelta;
      float negativeTime = maxY < 0 ? time : -time;
      float fractionalPart = MathHelper.fractionalPart(negativeTime * 0.2F - MathHelper.floor(negativeTime * 0.1F));
      matrices.push();
      matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(time * 2.25F - 45.0F));
      float innerV2 = -1.0F + fractionalPart;
      float innerV1 = maxY * heightScale * (0.5F / innerRadius) + innerV2;
      try (MemoryStack stack = MemoryStack.stackPush()) {
         long buffer = stack.nmalloc(1152);
         long ptr = writeBeamLayerVertices(
            buffer,
            matrices,
            ColorARGB.toABGR(color),
            yOffset,
            height,
            0.0F,
            innerRadius,
            innerRadius,
            0.0F,
            -innerRadius,
            0.0F,
            0.0F,
            -innerRadius,
            innerV1,
            innerV2
         );
         VertexBufferWriter.of(vertexConsumers.getBuffer(RenderLayer.getBeaconBeam(textureId, false)))
            .push(stack, buffer, 16, EntityVertex.FORMAT);
         matrices.pop();
         innerV2 = -1.0F + fractionalPart;
         innerV1 = maxY * heightScale + innerV2;
         long outerBuffer = ptr;
         writeBeamLayerVertices(
            outerBuffer,
            matrices,
            ColorARGB.toABGR(color, 32),
            yOffset,
            height,
            -outerRadius,
            -outerRadius,
            outerRadius,
            -outerRadius,
            -outerRadius,
            outerRadius,
            outerRadius,
            outerRadius,
            innerV1,
            innerV2
         );
         VertexBufferWriter.of(vertexConsumers.getBuffer(RenderLayer.getBeaconBeam(textureId, true)))
            .push(stack, outerBuffer, 16, EntityVertex.FORMAT);
      }

      matrices.pop();
   }

   private static long writeBeamLayerVertices(
      long ptr,
      MatrixStack matrices,
      int color,
      int yOffset,
      int height,
      float x1,
      float z1,
      float x2,
      float z2,
      float x3,
      float z3,
      float x4,
      float z4,
      float v1,
      float v2
   ) {
      MatrixStack.Entry entry = matrices.peek();
      Matrix4f positionMatrix = entry.getPositionMatrix();
      Matrix3f normalMatrix = entry.getNormalMatrix();
      int normal = MatrixHelper.transformNormal(normalMatrix, false, 0.0F, 1.0F, 0.0F);
      ptr = writeBeamVertex(ptr, positionMatrix, x1, height, z1, color, 1.0F, v1, normal);
      ptr = writeBeamVertex(ptr, positionMatrix, x1, yOffset, z1, color, 1.0F, v2, normal);
      ptr = writeBeamVertex(ptr, positionMatrix, x2, yOffset, z2, color, 0.0F, v2, normal);
      ptr = writeBeamVertex(ptr, positionMatrix, x2, height, z2, color, 0.0F, v1, normal);
      ptr = writeBeamVertex(ptr, positionMatrix, x4, height, z4, color, 1.0F, v1, normal);
      ptr = writeBeamVertex(ptr, positionMatrix, x4, yOffset, z4, color, 1.0F, v2, normal);
      ptr = writeBeamVertex(ptr, positionMatrix, x3, yOffset, z3, color, 0.0F, v2, normal);
      ptr = writeBeamVertex(ptr, positionMatrix, x3, height, z3, color, 0.0F, v1, normal);
      ptr = writeBeamVertex(ptr, positionMatrix, x2, height, z2, color, 1.0F, v1, normal);
      ptr = writeBeamVertex(ptr, positionMatrix, x2, yOffset, z2, color, 1.0F, v2, normal);
      ptr = writeBeamVertex(ptr, positionMatrix, x4, yOffset, z4, color, 0.0F, v2, normal);
      ptr = writeBeamVertex(ptr, positionMatrix, x4, height, z4, color, 0.0F, v1, normal);
      ptr = writeBeamVertex(ptr, positionMatrix, x3, height, z3, color, 1.0F, v1, normal);
      ptr = writeBeamVertex(ptr, positionMatrix, x3, yOffset, z3, color, 1.0F, v2, normal);
      ptr = writeBeamVertex(ptr, positionMatrix, x1, yOffset, z1, color, 0.0F, v2, normal);
      return writeBeamVertex(ptr, positionMatrix, x1, height, z1, color, 0.0F, v1, normal);
   }

   private static long writeBeamVertex(long ptr, Matrix4f matrix, float x, float y, float z, int color, float u, float v, int normal) {
      EntityVertex.write(
         ptr,
         MatrixHelper.transformPositionX(matrix, x, y, z),
         MatrixHelper.transformPositionY(matrix, x, y, z),
         MatrixHelper.transformPositionZ(matrix, x, y, z),
         color,
         u,
         v,
         15728880,
         OverlayTexture.DEFAULT_UV,
         normal
      );
      return ptr + 36L;
   }

   private static void renderBeamLayer(
      MatrixStack matrices,
      VertexConsumer vertices,
      int color,
      int yOffset,
      int height,
      float x1,
      float z1,
      float x2,
      float z2,
      float x3,
      float z3,
      float x4,
      float z4,
      float u1,
      float u2,
      float v1,
      float v2
   ) {
      MatrixStack.Entry entry = matrices.peek();
      renderBeamFace(entry, vertices, color, yOffset, height, x1, z1, x2, z2, u1, u2, v1, v2);
      renderBeamFace(entry, vertices, color, yOffset, height, x4, z4, x3, z3, u1, u2, v1, v2);
      renderBeamFace(entry, vertices, color, yOffset, height, x2, z2, x4, z4, u1, u2, v1, v2);
      renderBeamFace(entry, vertices, color, yOffset, height, x3, z3, x1, z1, u1, u2, v1, v2);
   }

   private static void renderBeamFace(
      MatrixStack.Entry matrix,
      VertexConsumer vertices,
      int color,
      int yOffset,
      int height,
      float x1,
      float z1,
      float x2,
      float z2,
      float u1,
      float u2,
      float v1,
      float v2
   ) {
      renderBeamVertex(matrix, vertices, color, height, x1, z1, u2, v1);
      renderBeamVertex(matrix, vertices, color, yOffset, x1, z1, u2, v2);
      renderBeamVertex(matrix, vertices, color, yOffset, x2, z2, u1, v2);
      renderBeamVertex(matrix, vertices, color, height, x2, z2, u1, v1);
   }

   private static void renderBeamVertex(MatrixStack.Entry matrix, VertexConsumer vertices, int color, int y, float x, float z, float u, float v) {
      vertices.vertex(matrix, x, y, z).color(color).texture(u, v).overlay(OverlayTexture.DEFAULT_UV).light(15728880).normal(matrix, 0.0F, 1.0F, 0.0F);
   }

   public boolean rendersOutsideBoundingBox(BeaconBlockEntity beaconBlockEntity) {
      return true;
   }

   @Override
   public int getRenderDistance() {
      return 256;
   }

   public boolean isInRenderDistance(BeaconBlockEntity beaconBlockEntity, Vec3d vec3d) {
      return Vec3d.ofCenter(beaconBlockEntity.getPos()).multiply(1.0, 0.0, 1.0).isInRange(vec3d.multiply(1.0, 0.0, 1.0), this.getRenderDistance());
   }
}
