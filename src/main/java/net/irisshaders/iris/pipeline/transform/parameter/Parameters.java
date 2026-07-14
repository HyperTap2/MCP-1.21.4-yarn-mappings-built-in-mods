package net.irisshaders.iris.pipeline.transform.parameter;

import io.github.douira.glsl_transformer.ast.transform.JobParameters;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import net.irisshaders.iris.gl.blending.AlphaTest;
import net.irisshaders.iris.gl.texture.TextureType;
import net.irisshaders.iris.helpers.Tri;
import net.irisshaders.iris.pipeline.transform.Patch;
import net.irisshaders.iris.pipeline.transform.PatchShaderType;
import net.irisshaders.iris.shaderpack.texture.TextureStage;

public abstract class Parameters implements JobParameters {
   public final Patch patch;
   private final Object2ObjectMap<Tri<String, TextureType, TextureStage>, String> textureMap;
   public PatchShaderType type;
   public String name;

   public Parameters(Patch patch, Object2ObjectMap<Tri<String, TextureType, TextureStage>, String> textureMap) {
      this.patch = patch;
      this.textureMap = textureMap;
   }

   public AlphaTest getAlphaTest() {
      return AlphaTest.ALWAYS;
   }

   public abstract TextureStage getTextureStage();

   public Object2ObjectMap<Tri<String, TextureType, TextureStage>, String> getTextureMap() {
      return this.textureMap;
   }

   @Override
   public int hashCode() {
      int prime = 31;
      int result = 1;
      result = 31 * result + (this.patch == null ? 0 : this.patch.hashCode());
      return 31 * result + (this.textureMap == null ? 0 : this.textureMap.hashCode());
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) {
         return true;
      } else if (obj == null) {
         return false;
      } else if (this.getClass() != obj.getClass()) {
         return false;
      } else {
         Parameters other = (Parameters)obj;
         if (this.patch != other.patch) {
            return false;
         } else {
            return this.textureMap == null ? other.textureMap == null : this.textureMap.equals(other.textureMap);
         }
      }
   }
}
