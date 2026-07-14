package me.flashyreese.mods.sodiumextra.client.render.vertex.formats;

import net.caffeinemc.mods.sodium.api.math.MatrixHelper;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;

public class TextureColorVertex {
   public static final VertexFormat FORMAT = VertexFormats.POSITION_TEXTURE_COLOR;
   public static final int STRIDE = 24;
   private static final int OFFSET_POSITION = 0;
   private static final int OFFSET_TEXTURE = 12;
   private static final int OFFSET_COLOR = 20;

   public static void write(long ptr, Matrix4f matrix, float x, float y, float z, int color, float u, float v) {
      float xt = MatrixHelper.transformPositionX(matrix, x, y, z);
      float yt = MatrixHelper.transformPositionY(matrix, x, y, z);
      float zt = MatrixHelper.transformPositionZ(matrix, x, y, z);
      write(ptr, xt, yt, zt, color, u, v);
   }

   public static void write(long ptr, float x, float y, float z, int color, float u, float v) {
      MemoryUtil.memPutFloat(ptr + 0L, x);
      MemoryUtil.memPutFloat(ptr + 0L + 4L, y);
      MemoryUtil.memPutFloat(ptr + 0L + 8L, z);
      MemoryUtil.memPutFloat(ptr + 12L, u);
      MemoryUtil.memPutFloat(ptr + 12L + 4L, v);
      MemoryUtil.memPutInt(ptr + 20L, color);
   }
}
