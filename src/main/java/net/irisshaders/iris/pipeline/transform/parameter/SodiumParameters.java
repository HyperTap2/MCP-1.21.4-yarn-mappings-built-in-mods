package net.irisshaders.iris.pipeline.transform.parameter;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import net.irisshaders.iris.gl.blending.AlphaTest;
import net.irisshaders.iris.gl.texture.TextureType;
import net.irisshaders.iris.helpers.Tri;
import net.irisshaders.iris.pipeline.transform.Patch;
import net.irisshaders.iris.shaderpack.texture.TextureStage;

public class SodiumParameters extends Parameters {
   public final AlphaTest alpha;

   public SodiumParameters(Patch patch, Object2ObjectMap<Tri<String, TextureType, TextureStage>, String> textureMap, AlphaTest alpha) {
      super(patch, textureMap);
      this.alpha = alpha;
   }

   @Override
   public AlphaTest getAlphaTest() {
      return this.alpha;
   }

   @Override
   public TextureStage getTextureStage() {
      return TextureStage.GBUFFERS_AND_SHADOW;
   }

   @Override
   public int hashCode() {
      int prime = 31;
      int result = super.hashCode();
      return 31 * result + (this.alpha == null ? 0 : this.alpha.hashCode());
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) {
         return true;
      }

      if (!super.equals(obj)) {
         return false;
      }

      if (this.getClass() != obj.getClass()) {
         return false;
      }

      SodiumParameters other = (SodiumParameters)obj;
      return this.alpha == null ? other.alpha == null : this.alpha.equals(other.alpha);
   }
}
