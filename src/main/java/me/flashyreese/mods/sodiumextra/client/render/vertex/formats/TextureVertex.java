package me.flashyreese.mods.sodiumextra.client.render.vertex.formats;

import net.caffeinemc.mods.sodium.api.math.MatrixHelper;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;

public class TextureVertex {
   public static final VertexFormat FORMAT = VertexFormats.POSITION_TEXTURE;
   public static final int STRIDE = 20;
   private static final int OFFSET_POSITION = 0;
   private static final int OFFSET_TEXTURE = 12;

   public static void write(long ptr, Matrix4f matrix, float x, float y, float z, float u, float v) {
      float xt = MatrixHelper.transformPositionX(matrix, x, y, z);
      float yt = MatrixHelper.transformPositionY(matrix, x, y, z);
      float zt = MatrixHelper.transformPositionZ(matrix, x, y, z);
      write(ptr, xt, yt, zt, u, v);
   }

   public static void write(long ptr, float x, float y, float z, float u, float v) {
      MemoryUtil.memPutFloat(ptr + 0L + 0L, x);
      MemoryUtil.memPutFloat(ptr + 0L + 4L, y);
      MemoryUtil.memPutFloat(ptr + 0L + 8L, z);
      MemoryUtil.memPutFloat(ptr + 12L + 0L, u);
      MemoryUtil.memPutFloat(ptr + 12L + 4L, v);
   }
}
