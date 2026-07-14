package net.irisshaders.iris.gl.sampler;

import java.util.function.IntSupplier;
import net.irisshaders.iris.gl.state.ValueUpdateNotifier;
import net.irisshaders.iris.gl.texture.TextureType;

public interface SamplerHolder {
   void addExternalSampler(int var1, String... var2);

   boolean hasSampler(String var1);

   default boolean addDefaultSampler(IntSupplier sampler, String... names) {
      return this.addDefaultSampler(TextureType.TEXTURE_2D, sampler, null, null, names);
   }

   boolean addDefaultSampler(TextureType var1, IntSupplier var2, ValueUpdateNotifier var3, GlSampler var4, String... var5);

   default boolean addDynamicSampler(IntSupplier texture, String... names) {
      return this.addDynamicSampler(TextureType.TEXTURE_2D, texture, null, names);
   }

   boolean addDynamicSampler(TextureType var1, IntSupplier var2, GlSampler var3, String... var4);

   default boolean addDynamicSampler(IntSupplier texture, ValueUpdateNotifier notifier, String... names) {
      return this.addDynamicSampler(TextureType.TEXTURE_2D, texture, notifier, null, names);
   }

   boolean addDynamicSampler(TextureType var1, IntSupplier var2, ValueUpdateNotifier var3, GlSampler var4, String... var5);
}
