package net.irisshaders.iris.api.v0;

import java.nio.ByteBuffer;
import net.minecraft.client.render.VertexFormat;

public interface IrisTextVertexSink {
   VertexFormat getUnderlyingVertexFormat();

   ByteBuffer getUnderlyingByteBuffer();

   void quad(float var1, float var2, float var3, float var4, float var5, int var6, float var7, float var8, float var9, float var10, int var11);
}
