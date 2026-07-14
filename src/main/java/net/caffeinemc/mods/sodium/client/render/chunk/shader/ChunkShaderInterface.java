package net.caffeinemc.mods.sodium.client.render.chunk.shader;

import org.joml.Matrix4fc;

public interface ChunkShaderInterface {
   @Deprecated
   void setupState();

   @Deprecated
   void resetState();

   void setProjectionMatrix(Matrix4fc var1);

   void setModelViewMatrix(Matrix4fc var1);

   void setRegionOffset(float var1, float var2, float var3);
}
