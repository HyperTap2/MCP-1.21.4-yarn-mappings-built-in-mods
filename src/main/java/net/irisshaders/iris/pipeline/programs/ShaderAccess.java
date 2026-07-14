package net.irisshaders.iris.pipeline.programs;

import net.irisshaders.iris.Iris;
import net.irisshaders.iris.pipeline.ShaderRenderingPipeline;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.irisshaders.iris.shadows.ShadowRenderingState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormatElement;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;

public class ShaderAccess {
   public static final VertexFormat IE_FORMAT = VertexFormat.builder()
      .add("Position", VertexFormatElement.POSITION)
      .add("Color", VertexFormatElement.COLOR)
      .add("UV0", VertexFormatElement.UV_0)
      .add("Normal", VertexFormatElement.NORMAL)
      .skip(1)
      .build();
   public static final ShaderProgramKey TRANSLUCENT_PARTICLE_SHADER = new ShaderProgramKey(
      Identifier.of("iris", "translucent_particle"), VertexFormats.POSITION_TEXTURE_COLOR_LIGHT, Defines.EMPTY
   );
   public static final ShaderProgramKey WEATHER_SHADER = new ShaderProgramKey(
      Identifier.of("iris", "weather"), VertexFormats.POSITION_TEXTURE_COLOR_LIGHT, Defines.EMPTY
   );
   public static ShaderProgramKey MEKANISM_FLAME = new ShaderProgramKey(
      Identifier.of("iris", "mekanism_flame"), VertexFormats.POSITION_TEXTURE_COLOR, Defines.EMPTY
   );
   public static ShaderProgramKey MEKASUIT = new ShaderProgramKey(
      Identifier.of("iris", "mekasuit"), VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, Defines.EMPTY
   );
   public static ShaderProgramKey IE_COMPAT = new ShaderProgramKey(Identifier.of("iris", "ie_vbo"), IE_FORMAT, Defines.EMPTY);

   public static ShaderProgram getParticleTranslucentShader() {
      WorldRenderingPipeline pipeline = Iris.getPipelineManager().getPipelineNullable();
      if (pipeline instanceof ShaderRenderingPipeline) {
         ShaderProgram override = ((ShaderRenderingPipeline)pipeline).getShaderMap().getShader(ShaderKey.PARTICLES_TRANS);
         if (override != null) {
            return override;
         }
      }

      return MinecraftClient.getInstance().getShaderLoader().getOrCreateProgram(ShaderProgramKeys.PARTICLE);
   }

   public static ShaderProgram getIEVBOShader() {
      WorldRenderingPipeline pipeline = Iris.getPipelineManager().getPipelineNullable();
      return pipeline instanceof ShaderRenderingPipeline
         ? ((ShaderRenderingPipeline)pipeline)
            .getShaderMap()
            .getShader(ShadowRenderingState.areShadowsCurrentlyBeingRendered() ? ShaderKey.IE_COMPAT_SHADOW : ShaderKey.IE_COMPAT)
         : null;
   }
}
