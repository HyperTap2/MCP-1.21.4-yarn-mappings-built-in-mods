package net.caffeinemc.mods.sodium.client.gl.shader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import net.minecraft.util.Identifier;
import org.apache.commons.io.IOUtils;

public class ShaderLoader {
   public static GlShader loadShader(ShaderType type, Identifier name, ShaderConstants constants) {
      return new GlShader(type, name, ShaderParser.parseShader(getShaderSource(name), constants));
   }

   public static String getShaderSource(Identifier name) {
      String path = String.format("/assets/%s/shaders/%s", name.getNamespace(), name.getPath());

      try {
         String var3;
         try (InputStream in = ShaderLoader.class.getResourceAsStream(path)) {
            if (in == null) {
               throw new RuntimeException("Shader not found: " + path);
            }

            var3 = IOUtils.toString(in, StandardCharsets.UTF_8);
         }

         return var3;
      } catch (IOException var7) {
         throw new RuntimeException("Failed to read shader source for " + path, var7);
      }
   }
}
