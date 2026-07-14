package net.caffeinemc.mods.sodium.client.gl.shader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.util.Identifier;

public class ShaderParser {
   private static final Pattern IMPORT_PATTERN = Pattern.compile("#import <(?<namespace>.*):(?<path>.*)>");

   public static String parseShader(String src, ShaderConstants constants) {
      List<String> lines = parseShader(src);
      lines.addAll(1, constants.getDefineStrings());
      return String.join("\n", lines);
   }

   public static List<String> parseShader(String src) {
      List<String> builder = new LinkedList<>();

      try {
         String line;
         try (BufferedReader reader = new BufferedReader(new StringReader(src))) {
            while ((line = reader.readLine()) != null) {
               if (line.startsWith("#import")) {
                  builder.addAll(resolveImport(line));
               } else {
                  builder.add(line);
               }
            }
         }

         return builder;
      } catch (IOException var8) {
         throw new RuntimeException("Failed to read shader sources", var8);
      }
   }

   private static List<String> resolveImport(String line) {
      Matcher matcher = IMPORT_PATTERN.matcher(line);
      if (!matcher.matches()) {
         throw new IllegalArgumentException("Malformed import statement (expected format: " + IMPORT_PATTERN + ")");
      } else {
         String namespace = matcher.group("namespace");
         String path = matcher.group("path");
         Identifier name = Identifier.of(namespace, path);
         String source = ShaderLoader.getShaderSource(name);
         return parseShader(source);
      }
   }
}
