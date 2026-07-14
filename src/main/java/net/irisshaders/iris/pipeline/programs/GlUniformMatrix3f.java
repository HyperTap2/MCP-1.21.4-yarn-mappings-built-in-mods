package net.irisshaders.iris.pipeline.programs;

import com.mojang.blaze3d.platform.GlStateManager;
import java.nio.FloatBuffer;
import net.caffeinemc.mods.sodium.client.gl.shader.uniform.GlUniform;
import org.joml.Matrix3fc;
import org.lwjgl.system.MemoryStack;

public class GlUniformMatrix3f extends GlUniform<Matrix3fc> {
   public GlUniformMatrix3f(int index) {
      super(index);
   }

   public void set(Matrix3fc value) {
      MemoryStack stack = MemoryStack.stackPush();

      try {
         FloatBuffer buf = stack.callocFloat(9);
         value.get(buf);
         GlStateManager._glUniformMatrix3(this.index, false, buf);
      } catch (Throwable var6) {
         if (stack != null) {
            try {
               stack.close();
            } catch (Throwable var5) {
               var6.addSuppressed(var5);
            }
         }

         throw var6;
      }

      if (stack != null) {
         stack.close();
      }
   }
}
