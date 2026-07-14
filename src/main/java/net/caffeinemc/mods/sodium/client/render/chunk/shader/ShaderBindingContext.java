package net.caffeinemc.mods.sodium.client.render.chunk.shader;

import java.util.function.IntFunction;
import net.caffeinemc.mods.sodium.client.gl.shader.uniform.GlUniform;
import net.caffeinemc.mods.sodium.client.gl.shader.uniform.GlUniformBlock;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ShaderBindingContext {
   @NotNull
   <U extends GlUniform<?>> U bindUniform(String var1, IntFunction<U> var2);

   @Nullable
   <U extends GlUniform<?>> U bindUniformOptional(String var1, IntFunction<U> var2);

   @NotNull
   GlUniformBlock bindUniformBlock(String var1, int var2);

   @Nullable
   GlUniformBlock bindUniformBlockOptional(String var1, int var2);
}
