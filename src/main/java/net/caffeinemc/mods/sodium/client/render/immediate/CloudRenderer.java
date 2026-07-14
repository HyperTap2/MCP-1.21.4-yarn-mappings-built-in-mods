package net.caffeinemc.mods.sodium.client.render.immediate;

import com.mojang.blaze3d.platform.GlStateManager.DstFactor;
import com.mojang.blaze3d.platform.GlStateManager.SrcFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import java.io.InputStream;
import java.util.Objects;
import me.flashyreese.mods.sodiumextra.client.SodiumExtraClientMod;
import net.caffeinemc.mods.sodium.api.util.ColorABGR;
import net.caffeinemc.mods.sodium.api.util.ColorARGB;
import net.caffeinemc.mods.sodium.api.util.ColorU8;
import net.caffeinemc.mods.sodium.api.util.NormI8;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.caffeinemc.mods.sodium.api.vertex.format.common.ColorVertex;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.api.v0.IrisApi;
import net.irisshaders.iris.pipeline.WorldRenderingPhase;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.irisshaders.iris.vertices.IrisVertexFormats;
import net.irisshaders.iris.vertices.sodium.CloudVertex;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.GlUsage;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Fog;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.BackgroundRenderer.FogType;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceFactory;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CloudRenderer {
   private static final Logger LOGGER = LoggerFactory.getLogger("Sodium-CloudRenderer");
   public static final ShaderProgramKey CLOUDS_SHADER = new ShaderProgramKey(
      Identifier.of("sodium", "clouds"), VertexFormats.POSITION_COLOR, Defines.builder().build()
   );
   private static final int[] IRIS_NORMALS = new int[]{
      NormI8.pack(0.0F, -1.0F, 0.0F),
      NormI8.pack(0.0F, 1.0F, 0.0F),
      NormI8.pack(-1.0F, 0.0F, 0.0F),
      NormI8.pack(1.0F, 0.0F, 0.0F),
      NormI8.pack(0.0F, 0.0F, -1.0F),
      NormI8.pack(0.0F, 0.0F, 1.0F)
   };
   private static int iris$computedNormal;
   private static boolean iris$hadShadersOn;
   private static final Identifier CLOUDS_TEXTURE_ID = Identifier.ofVanilla("textures/environment/clouds.png");
   private static final float CLOUD_HEIGHT = 4.0F;
   private static final float CLOUD_WIDTH = 12.0F;
   private static final int FACE_MASK_NEG_Y = 1;
   private static final int FACE_MASK_POS_Y = 2;
   private static final int FACE_MASK_NEG_X = 4;
   private static final int FACE_MASK_POS_X = 8;
   private static final int FACE_MASK_NEG_Z = 16;
   private static final int FACE_MASK_POS_Z = 32;
   private static final int BRIGHTNESS_POS_Y = ColorU8.normalizedFloatToByte(1.0F);
   private static final int BRIGHTNESS_NEG_Y = ColorU8.normalizedFloatToByte(0.7F);
   private static final int BRIGHTNESS_X_AXIS = ColorU8.normalizedFloatToByte(0.9F);
   private static final int BRIGHTNESS_Z_AXIS = ColorU8.normalizedFloatToByte(0.8F);
   @Nullable
   private CloudRenderer.CloudTextureData textureData;
   @Nullable
   private CloudRenderer.CloudGeometry builtGeometry;

   public CloudRenderer(ResourceFactory resourceProvider) {
      this.reload(resourceProvider);
   }

   public void render(Camera camera, ClientWorld level, Matrix4f projectionMatrix, Matrix4f modelView, float ticks, float tickDelta, int color) {
      WorldRenderingPipeline pipeline = Iris.getPipelineManager().getPipelineNullable();
      if (pipeline != null) {
         pipeline.setPhase(WorldRenderingPhase.CLOUDS);
      }

      try {
         this.renderInternal(camera, level, projectionMatrix, modelView, ticks, tickDelta, color);
      } finally {
         if (pipeline != null) {
            pipeline.setPhase(WorldRenderingPhase.NONE);
         }
      }
   }

   private void renderInternal(Camera camera, ClientWorld level, Matrix4f projectionMatrix, Matrix4f modelView, float ticks, float tickDelta, int color) {
      float height = level.getDimensionEffects().getCloudsHeight() + 0.33F;
      if (!Float.isNaN(height)) {
         if (this.textureData != null) {
            Vec3d cameraPos = camera.getPos();
            int renderDistance = getCloudRenderDistance();
            CloudRenderMode renderMode = MinecraftClient.getInstance().options.getCloudRenderModeValue();
            double worldX = cameraPos.x + (ticks + tickDelta) * 0.03;
            double worldZ = cameraPos.z + 3.96;
            double textureWidth = this.textureData.width * 12.0F;
            double textureHeight = this.textureData.height * 12.0F;
            worldX -= MathHelper.floor(worldX / textureWidth) * textureWidth;
            worldZ -= MathHelper.floor(worldZ / textureHeight) * textureHeight;
            int cellX = MathHelper.floor(worldX / 12.0);
            int cellZ = MathHelper.floor(worldZ / 12.0);
            CloudRenderer.ViewOrientation orientation;
            if (renderMode == CloudRenderMode.FANCY) {
               orientation = CloudRenderer.ViewOrientation.getOrientation(cameraPos, height, height + 4.0F);
            } else {
               orientation = null;
            }

            CloudRenderer.CloudGeometryParameters parameters = new CloudRenderer.CloudGeometryParameters(cellX, cellZ, renderDistance, orientation, renderMode);
            CloudRenderer.CloudGeometry geometry = this.builtGeometry;
            boolean shaders = Iris.isPackInUseQuick();
            if (geometry == null || iris$hadShadersOn != shaders || !Objects.equals(geometry.params(), parameters)) {
               this.builtGeometry = geometry = rebuildGeometry(geometry, parameters, this.textureData);
            }

            VertexBuffer vertexBuffer = geometry.vertexBuffer();
            if (vertexBuffer != null) {
               float viewPosX = (float)(worldX - cellX * 12.0F);
               float viewPosY = (float)cameraPos.getY() - height;
               float viewPosZ = (float)(worldZ - cellZ * 12.0F);
               Matrix4f modelViewMatrix = new Matrix4f(modelView);
               modelViewMatrix.translate(-viewPosX, -viewPosY, -viewPosZ);
               Fog prevFogParameters = copyShaderFogParameters(RenderSystem.getShaderFog());
               boolean flat = geometry.params().renderMode() == CloudRenderMode.FAST;
               Fog fogParameters = BackgroundRenderer.applyFog(
                  camera,
                  FogType.FOG_TERRAIN,
                  new Vector4f(prevFogParameters.red(), prevFogParameters.green(), prevFogParameters.blue(), prevFogParameters.alpha()),
                  renderDistance * 8,
                  shouldUseWorldFog(level, cameraPos),
                  tickDelta
               );
               RenderSystem.setShaderColor(ColorHelper.getRedFloat(color), ColorHelper.getGreenFloat(color), ColorHelper.getBlueFloat(color), 0.8F);
               RenderSystem.setShaderFog(fogParameters);
               Framebuffer renderTarget = MinecraftClient.getInstance().worldRenderer.getCloudsFramebuffer();
               if (renderTarget != null) {
                  renderTarget.beginWrite(false);
               } else {
                  MinecraftClient.getInstance().getFramebuffer().beginWrite(false);
               }

               RenderSystem.enableBlend();
               RenderSystem.blendFuncSeparate(SrcFactor.SRC_ALPHA, DstFactor.ONE_MINUS_SRC_ALPHA, SrcFactor.ONE, DstFactor.ONE_MINUS_SRC_ALPHA);
               RenderSystem.setShader(CLOUDS_SHADER);
               if (flat) {
                  RenderSystem.disableCull();
               }

               RenderSystem.enableDepthTest();
               RenderSystem.depthFunc(513);
               vertexBuffer.bind();
               vertexBuffer.draw(modelViewMatrix, projectionMatrix, RenderSystem.getShader());
               VertexBuffer.unbind();
               RenderSystem.depthFunc(515);
               RenderSystem.disableDepthTest();
               if (flat) {
                  RenderSystem.enableCull();
               }

               RenderSystem.defaultBlendFunc();
               RenderSystem.disableBlend();
               if (renderTarget != null) {
                  MinecraftClient.getInstance().getFramebuffer().beginWrite(false);
               }

               RenderSystem.setShaderFog(prevFogParameters);
               RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            }
         }
      }
   }

   @NotNull
   private static CloudRenderer.CloudGeometry rebuildGeometry(
      @Nullable CloudRenderer.CloudGeometry existingGeometry, CloudRenderer.CloudGeometryParameters parameters, CloudRenderer.CloudTextureData textureData
   ) {
      iris$hadShadersOn = IrisApi.getInstance().isShaderPackInUse();
      VertexFormat vertexFormat = iris$hadShadersOn ? IrisVertexFormats.CLOUDS : VertexFormats.POSITION_COLOR;
      BufferBuilder bufferBuilder = Tessellator.getInstance().begin(DrawMode.QUADS, vertexFormat);
      VertexBufferWriter writer = VertexBufferWriter.of(bufferBuilder);
      int radius = parameters.radius();
      CloudRenderer.ViewOrientation orientation = parameters.orientation();
      boolean flat = parameters.renderMode() == CloudRenderMode.FAST;
      CloudRenderer.CloudTextureData.Slice slice = textureData.slice(parameters.originX(), parameters.originZ(), radius);
      addCellGeometryToBuffer(writer, slice, 0, 0, orientation, flat);

      for (int layer = 1; layer <= radius; layer++) {
         for (int z = -layer; z < layer; z++) {
            int x = Math.abs(z) - layer;
            addCellGeometryToBuffer(writer, slice, x, z, orientation, flat);
         }

         for (int z = layer; z > -layer; z--) {
            int x = layer - Math.abs(z);
            addCellGeometryToBuffer(writer, slice, x, z, orientation, flat);
         }
      }

      for (int layer = radius + 1; layer <= 2 * radius; layer++) {
         int l = layer - radius;

         for (int z = -radius; z <= -l; z++) {
            int x = -z - layer;
            addCellGeometryToBuffer(writer, slice, x, z, orientation, flat);
         }

         for (int z = l; z <= radius; z++) {
            int x = z - layer;
            addCellGeometryToBuffer(writer, slice, x, z, orientation, flat);
         }

         for (int z = radius; z >= l; z--) {
            int x = layer - z;
            addCellGeometryToBuffer(writer, slice, x, z, orientation, flat);
         }

         for (int z = -l; z >= -radius; z--) {
            int x = layer + z;
            addCellGeometryToBuffer(writer, slice, x, z, orientation, flat);
         }
      }

      BuiltBuffer meshData = bufferBuilder.endNullable();
      VertexBuffer vertexBuffer = null;
      if (existingGeometry != null) {
         vertexBuffer = existingGeometry.vertexBuffer();
      }

      if (meshData != null) {
         if (vertexBuffer == null) {
            vertexBuffer = new VertexBuffer(GlUsage.DYNAMIC_WRITE);
         }

         uploadToVertexBuffer(vertexBuffer, meshData);
      } else if (vertexBuffer != null) {
         vertexBuffer.close();
         vertexBuffer = null;
      }

      Tessellator.getInstance().clear();
      return new CloudRenderer.CloudGeometry(vertexBuffer, parameters);
   }

   private static void addCellGeometryToBuffer(
      VertexBufferWriter writer,
      CloudRenderer.CloudTextureData.Slice textureData,
      int x,
      int z,
      @Nullable CloudRenderer.ViewOrientation orientation,
      boolean flat
   ) {
      int index = textureData.getCellIndex(x, z);
      int faces = textureData.getCellFaces(index) & getVisibleFaces(x, z, orientation);
      if (faces != 0) {
         int color = textureData.getCellColor(index);
         if (!isTransparent(color)) {
            if (flat) {
               emitCellGeometryFlat(writer, color, x, z);
            } else {
               emitCellGeometryExterior(writer, faces, color, x, z);
               if (taxicabDistance(x, z) <= 1) {
                  emitCellGeometryInterior(writer, color, x, z);
               }
            }
         }
      }
   }

   private static int getVisibleFaces(int x, int z, CloudRenderer.ViewOrientation orientation) {
      int faces = 0;
      if (x <= 0) {
         faces |= 8;
      }

      if (z <= 0) {
         faces |= 32;
      }

      if (x >= 0) {
         faces |= 4;
      }

      if (z >= 0) {
         faces |= 16;
      }

      if (orientation != CloudRenderer.ViewOrientation.BELOW_CLOUDS) {
         faces |= 2;
      }

      if (orientation != CloudRenderer.ViewOrientation.ABOVE_CLOUDS) {
         faces |= 1;
      }

      return faces;
   }

   private static void emitCellGeometryFlat(VertexBufferWriter writer, int texel, int x, int z) {
      MemoryStack stack = MemoryStack.stackPush();

      try {
         long vertexBuffer = stack.nmalloc(iris$hadShadersOn ? 80 : 64);
         float x0 = x * 12.0F;
         float x1 = x0 + 12.0F;
         float z0 = z * 12.0F;
         float z1 = z0 + 12.0F;
         int color = ColorABGR.mulRGB(texel, BRIGHTNESS_POS_Y);
         iris$computedNormal = IRIS_NORMALS[0];
         long ptr = writeVertex(vertexBuffer, x1, 0.0F, z1, color);
         ptr = writeVertex(ptr, x0, 0.0F, z1, color);
         ptr = writeVertex(ptr, x0, 0.0F, z0, color);
         ptr = writeVertex(ptr, x1, 0.0F, z0, color);
         writer.push(stack, vertexBuffer, 4, iris$hadShadersOn ? IrisVertexFormats.CLOUDS : ColorVertex.FORMAT);
      } catch (Throwable var15) {
         if (stack != null) {
            try {
               stack.close();
            } catch (Throwable var14) {
               var15.addSuppressed(var14);
            }
         }

         throw var15;
      }

      if (stack != null) {
         stack.close();
      }
   }

   private static void emitCellGeometryExterior(VertexBufferWriter writer, int cellFaces, int cellColor, int cellX, int cellZ) {
      MemoryStack stack = MemoryStack.stackPush();

      try {
         long vertexBuffer = stack.nmalloc(iris$hadShadersOn ? 480 : 384);
         int vertexCount = 0;
         long ptr = vertexBuffer;
         float x0 = cellX * 12.0F;
         float y0 = 0.0F;
         float z0 = cellZ * 12.0F;
         float x1 = x0 + 12.0F;
         float y1 = 4.0F;
         float z1 = z0 + 12.0F;
         if ((cellFaces & 1) != 0) {
            int vertexColor = ColorABGR.mulRGB(cellColor, BRIGHTNESS_NEG_Y);
            iris$computedNormal = IRIS_NORMALS[0];
            ptr = writeVertex(vertexBuffer, x1, 0.0F, z1, vertexColor);
            ptr = writeVertex(ptr, x0, 0.0F, z1, vertexColor);
            ptr = writeVertex(ptr, x0, 0.0F, z0, vertexColor);
            ptr = writeVertex(ptr, x1, 0.0F, z0, vertexColor);
            vertexCount += 4;
         }

         if ((cellFaces & 2) != 0) {
            int vertexColor = ColorABGR.mulRGB(cellColor, BRIGHTNESS_POS_Y);
            iris$computedNormal = IRIS_NORMALS[1];
            ptr = writeVertex(ptr, x0, 4.0F, z1, vertexColor);
            ptr = writeVertex(ptr, x1, 4.0F, z1, vertexColor);
            ptr = writeVertex(ptr, x1, 4.0F, z0, vertexColor);
            ptr = writeVertex(ptr, x0, 4.0F, z0, vertexColor);
            vertexCount += 4;
         }

         if ((cellFaces & 12) != 0) {
            int vertexColor = ColorABGR.mulRGB(cellColor, BRIGHTNESS_X_AXIS);
            if ((cellFaces & 4) != 0) {
               iris$computedNormal = IRIS_NORMALS[2];
               ptr = writeVertex(ptr, x0, 0.0F, z1, vertexColor);
               ptr = writeVertex(ptr, x0, 4.0F, z1, vertexColor);
               ptr = writeVertex(ptr, x0, 4.0F, z0, vertexColor);
               ptr = writeVertex(ptr, x0, 0.0F, z0, vertexColor);
               vertexCount += 4;
            }

            if ((cellFaces & 8) != 0) {
               iris$computedNormal = IRIS_NORMALS[3];
               ptr = writeVertex(ptr, x1, 4.0F, z1, vertexColor);
               ptr = writeVertex(ptr, x1, 0.0F, z1, vertexColor);
               ptr = writeVertex(ptr, x1, 0.0F, z0, vertexColor);
               ptr = writeVertex(ptr, x1, 4.0F, z0, vertexColor);
               vertexCount += 4;
            }
         }

         if ((cellFaces & 48) != 0) {
            int vertexColorx = ColorABGR.mulRGB(cellColor, BRIGHTNESS_Z_AXIS);
            if ((cellFaces & 16) != 0) {
               iris$computedNormal = IRIS_NORMALS[4];
               ptr = writeVertex(ptr, x1, 4.0F, z0, vertexColorx);
               ptr = writeVertex(ptr, x1, 0.0F, z0, vertexColorx);
               ptr = writeVertex(ptr, x0, 0.0F, z0, vertexColorx);
               ptr = writeVertex(ptr, x0, 4.0F, z0, vertexColorx);
               vertexCount += 4;
            }

            if ((cellFaces & 32) != 0) {
               iris$computedNormal = IRIS_NORMALS[5];
               ptr = writeVertex(ptr, x1, 0.0F, z1, vertexColorx);
               ptr = writeVertex(ptr, x1, 4.0F, z1, vertexColorx);
               ptr = writeVertex(ptr, x0, 4.0F, z1, vertexColorx);
               ptr = writeVertex(ptr, x0, 0.0F, z1, vertexColorx);
               vertexCount += 4;
            }
         }

         writer.push(stack, vertexBuffer, vertexCount, iris$hadShadersOn ? IrisVertexFormats.CLOUDS : ColorVertex.FORMAT);
      } catch (Throwable var19) {
         if (stack != null) {
            try {
               stack.close();
            } catch (Throwable var18) {
               var19.addSuppressed(var18);
            }
         }

         throw var19;
      }

      if (stack != null) {
         stack.close();
      }
   }

   private static void emitCellGeometryInterior(VertexBufferWriter writer, int baseColor, int cellX, int cellZ) {
      MemoryStack stack = MemoryStack.stackPush();

      try {
         long vertexBuffer = stack.nmalloc(iris$hadShadersOn ? 480 : 384);
         float x0 = cellX * 12.0F;
         float y0 = 0.0F;
         float z0 = cellZ * 12.0F;
         float x1 = x0 + 12.0F;
         float y1 = 4.0F;
         float z1 = z0 + 12.0F;
         int vertexColor = ColorABGR.mulRGB(baseColor, BRIGHTNESS_NEG_Y);
         long ptr = writeVertex(vertexBuffer, x1, 0.0F, z0, vertexColor);
         ptr = writeVertex(ptr, x0, 0.0F, z0, vertexColor);
         ptr = writeVertex(ptr, x0, 0.0F, z1, vertexColor);
         ptr = writeVertex(ptr, x1, 0.0F, z1, vertexColor);
         vertexColor = ColorABGR.mulRGB(baseColor, BRIGHTNESS_POS_Y);
         ptr = writeVertex(ptr, x0, 4.0F, z0, vertexColor);
         ptr = writeVertex(ptr, x1, 4.0F, z0, vertexColor);
         ptr = writeVertex(ptr, x1, 4.0F, z1, vertexColor);
         ptr = writeVertex(ptr, x0, 4.0F, z1, vertexColor);
         vertexColor = ColorABGR.mulRGB(baseColor, BRIGHTNESS_X_AXIS);
         ptr = writeVertex(ptr, x0, 0.0F, z0, vertexColor);
         ptr = writeVertex(ptr, x0, 4.0F, z0, vertexColor);
         ptr = writeVertex(ptr, x0, 4.0F, z1, vertexColor);
         ptr = writeVertex(ptr, x0, 0.0F, z1, vertexColor);
         ptr = writeVertex(ptr, x1, 4.0F, z0, vertexColor);
         ptr = writeVertex(ptr, x1, 0.0F, z0, vertexColor);
         ptr = writeVertex(ptr, x1, 0.0F, z1, vertexColor);
         ptr = writeVertex(ptr, x1, 4.0F, z1, vertexColor);
         vertexColor = ColorABGR.mulRGB(baseColor, BRIGHTNESS_Z_AXIS);
         ptr = writeVertex(ptr, x0, 4.0F, z0, vertexColor);
         ptr = writeVertex(ptr, x0, 0.0F, z0, vertexColor);
         ptr = writeVertex(ptr, x1, 0.0F, z0, vertexColor);
         ptr = writeVertex(ptr, x1, 4.0F, z0, vertexColor);
         ptr = writeVertex(ptr, x0, 0.0F, z1, vertexColor);
         ptr = writeVertex(ptr, x0, 4.0F, z1, vertexColor);
         ptr = writeVertex(ptr, x1, 4.0F, z1, vertexColor);
         ptr = writeVertex(ptr, x1, 0.0F, z1, vertexColor);
         writer.push(stack, vertexBuffer, 24, iris$hadShadersOn ? IrisVertexFormats.CLOUDS : ColorVertex.FORMAT);
      } catch (Throwable var17) {
         if (stack != null) {
            try {
               stack.close();
            } catch (Throwable var16) {
               var17.addSuppressed(var16);
            }
         }

         throw var17;
      }

      if (stack != null) {
         stack.close();
      }
   }

   private static long writeVertex(long buffer, float x, float y, float z, int color) {
      if (iris$hadShadersOn) {
         CloudVertex.put(buffer, x, y, z, color, iris$computedNormal);
         return buffer + 20L;
      }

      ColorVertex.put(buffer, x, y, z, color);
      return buffer + 16L;
   }

   private static void uploadToVertexBuffer(VertexBuffer vertexBuffer, BuiltBuffer builtBuffer) {
      vertexBuffer.bind();
      vertexBuffer.upload(builtBuffer);
      VertexBuffer.unbind();
   }

   public void reload(ResourceFactory resourceProvider) {
      this.destroy();
      this.textureData = loadTextureData(resourceProvider);
   }

   public void destroy() {
      if (this.builtGeometry != null) {
         VertexBuffer vertexBuffer = this.builtGeometry.vertexBuffer();
         if (vertexBuffer != null) {
            vertexBuffer.close();
         }

         this.builtGeometry = null;
      }
   }

   @Nullable
   private static CloudRenderer.CloudTextureData loadTextureData(ResourceFactory resourceProvider) {
      Resource resource = resourceProvider.getResource(CLOUDS_TEXTURE_ID).orElseThrow();

      try {
         CloudRenderer.CloudTextureData var4;
         try (
            InputStream inputStream = resource.getInputStream();
            NativeImage nativeImage = NativeImage.read(inputStream);
         ) {
            var4 = CloudRenderer.CloudTextureData.load(nativeImage);
         }

         return var4;
      } catch (Throwable var10) {
         LOGGER.error(
            "Failed to load texture '{}'. The rendering of clouds in the skybox will be disabled. This may be caused by an incompatible resource pack.",
            CLOUDS_TEXTURE_ID,
            var10
         );
         return null;
      }
   }

   private static boolean shouldUseWorldFog(ClientWorld level, Vec3d pos) {
      return level.getDimensionEffects().useThickFog(MathHelper.floor(pos.getX()), MathHelper.floor(pos.getZ()))
         || MinecraftClient.getInstance().inGameHud.getBossBarHud().shouldThickenFog();
   }

   private static int getCloudRenderDistance() {
      int viewDistance = MinecraftClient.getInstance().options.getClampedViewDistance();
      int distancePercent = SodiumExtraClientMod.isMixinEnabled("sodium.cloud.MixinCloudRenderer")
         ? SodiumExtraClientMod.options().extraSettings.cloudDistance
         : 100;
      return Math.max(32, viewDistance * distancePercent / 100 * 2 + 9);
   }

   private static boolean isTransparent(int argb) {
      return ColorARGB.unpackAlpha(argb) < 10;
   }

   private static int taxicabDistance(int x, int z) {
      return Math.abs(x) + Math.abs(z);
   }

   private static Fog copyShaderFogParameters(Fog shaderFog) {
      return new Fog(shaderFog.start(), shaderFog.end(), shaderFog.shape(), shaderFog.red(), shaderFog.green(), shaderFog.blue(), shaderFog.alpha());
   }

   public record CloudGeometry(@Nullable VertexBuffer vertexBuffer, CloudRenderer.CloudGeometryParameters params) {
   }

   public record CloudGeometryParameters(int originX, int originZ, int radius, @Nullable CloudRenderer.ViewOrientation orientation, CloudRenderMode renderMode) {
   }

   private static class CloudTextureData {
      private final byte[] faces;
      private final int[] colors;
      private final int width;
      private final int height;

      private CloudTextureData(int width, int height) {
         this.faces = new byte[width * height];
         this.colors = new int[width * height];
         this.width = width;
         this.height = height;
      }

      public CloudRenderer.CloudTextureData.Slice slice(int originX, int originY, int radius) {
         CloudRenderer.CloudTextureData src = this;
         CloudRenderer.CloudTextureData.Slice dst = new CloudRenderer.CloudTextureData.Slice(radius);

         for (int dstY = 0; dstY < dst.height; dstY++) {
            int srcX = Math.floorMod(originX - radius, this.width);
            int srcY = Math.floorMod(originY - radius + dstY, this.height);
            int dstX = 0;

            while (dstX < dst.width) {
               int length = Math.min(src.width - srcX, dst.width - dstX);
               int srcPos = getCellIndex(srcX, srcY, src.width);
               int dstPos = getCellIndex(dstX, dstY, dst.width);
               System.arraycopy(this.faces, srcPos, dst.faces, dstPos, length);
               System.arraycopy(this.colors, srcPos, dst.colors, dstPos, length);
               srcX = 0;
               dstX += length;
            }
         }

         return dst;
      }

      @Nullable
      public static CloudRenderer.CloudTextureData load(NativeImage image) {
         int width = image.getWidth();
         int height = image.getHeight();
         CloudRenderer.CloudTextureData data = new CloudRenderer.CloudTextureData(width, height);
         return !data.loadTextureData(image, width, height) ? null : data;
      }

      private boolean loadTextureData(NativeImage texture, int width, int height) {
         Validate.isTrue(this.width == width);
         Validate.isTrue(this.height == height);
         boolean containsData = false;

         for (int x = 0; x < width; x++) {
            for (int z = 0; z < height; z++) {
               int color = texture.getColorArgb(x, z);
               if (!CloudRenderer.isTransparent(color)) {
                  int index = getCellIndex(x, z, width);
                  this.colors[index] = color;
                  this.faces[index] = (byte)getOpenFaces(texture, color, x, z);
                  containsData = true;
               }
            }
         }

         return containsData;
      }

      private static int getOpenFaces(NativeImage image, int color, int x, int z) {
         int faces = 3;
         int neighbor = getNeighborTexel(image, x - 1, z);
         if (color != neighbor) {
            faces |= 4;
         }

         neighbor = getNeighborTexel(image, x + 1, z);
         if (color != neighbor) {
            faces |= 8;
         }

         neighbor = getNeighborTexel(image, x, z - 1);
         if (color != neighbor) {
            faces |= 16;
         }

         neighbor = getNeighborTexel(image, x, z + 1);
         if (color != neighbor) {
            faces |= 32;
         }

         return faces;
      }

      private static int getNeighborTexel(NativeImage image, int x, int z) {
         x = wrapTexelCoord(x, 0, image.getWidth() - 1);
         z = wrapTexelCoord(z, 0, image.getHeight() - 1);
         return image.getColorArgb(x, z);
      }

      private static int wrapTexelCoord(int coord, int min, int max) {
         if (coord < min) {
            coord = max;
         }

         if (coord > max) {
            coord = min;
         }

         return coord;
      }

      private static int getCellIndex(int x, int z, int pitch) {
         return z * pitch + x;
      }

      public static class Slice {
         private final int width;
         private final int height;
         private final int radius;
         private final byte[] faces;
         private final int[] colors;

         public Slice(int radius) {
            this.width = 1 + radius * 2;
            this.height = 1 + radius * 2;
            this.radius = radius;
            this.faces = new byte[this.width * this.height];
            this.colors = new int[this.width * this.height];
         }

         public int getCellIndex(int x, int z) {
            return CloudRenderer.CloudTextureData.getCellIndex(x + this.radius, z + this.radius, this.width);
         }

         public int getCellFaces(int index) {
            return Byte.toUnsignedInt(this.faces[index]);
         }

         public int getCellColor(int index) {
            return this.colors[index];
         }
      }
   }

   private static enum ViewOrientation {
      BELOW_CLOUDS,
      INSIDE_CLOUDS,
      ABOVE_CLOUDS;

      @NotNull
      public static CloudRenderer.ViewOrientation getOrientation(Vec3d camera, float minY, float maxY) {
         if (camera.getY() <= minY + 0.125F) {
            return BELOW_CLOUDS;
         } else {
            return camera.getY() >= maxY - 0.125F ? ABOVE_CLOUDS : INSIDE_CLOUDS;
         }
      }
   }
}
