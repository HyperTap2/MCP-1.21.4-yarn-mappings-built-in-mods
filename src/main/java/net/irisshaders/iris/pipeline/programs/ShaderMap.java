package net.irisshaders.iris.pipeline.programs;

import com.mojang.blaze3d.platform.GlStateManager;
import java.util.function.Consumer;
import java.util.function.Function;
import net.irisshaders.iris.gl.shader.ShaderCompileException;
import net.minecraft.client.gl.ShaderProgram;

public class ShaderMap {
   private final ShaderProgram[] shaders;

   public ShaderMap(ShaderLoadingMap loadingMap, Function<ShaderSupplier, Boolean> deletionFunction, Consumer<ShaderProgram> programConsumer) {
      ShaderKey[] ids = ShaderKey.values();
      this.shaders = new ShaderProgram[ids.length];
      loadingMap.forAllShaders((key, shader) -> {
         if (shader != null) {
            if (deletionFunction.apply(shader)) {
               GlStateManager.glDeleteProgram(shader.id());
               return;
            }

            this.checkLinkingState(key, shader);
            ShaderProgram shaderProgram = shader.shader().get();
            this.shaders[key.ordinal()] = shaderProgram;
            programConsumer.accept(shaderProgram);
         }
      });
   }

   private void checkLinkingState(ShaderKey key, ShaderSupplier shader) {
      int i = shader.id();
      int j = GlStateManager.glGetProgrami(i, 35714);
      if (j == 0) {
         String string = GlStateManager.glGetProgramInfoLog(i, 32768);
         throw new ShaderCompileException(key.name(), string);
      }
   }

   public ShaderProgram getShader(ShaderKey id) {
      return this.shaders[id.ordinal()];
   }
}
