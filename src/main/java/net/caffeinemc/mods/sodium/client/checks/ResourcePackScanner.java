package net.caffeinemc.mods.sodium.client.checks;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import net.caffeinemc.mods.sodium.client.console.Console;
import net.caffeinemc.mods.sodium.client.console.message.MessageLevel;
import net.minecraft.resource.DirectoryResourcePack;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourcePack;
import net.minecraft.resource.ResourceType;
import net.minecraft.resource.ZipResourcePack;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResourcePackScanner {
   private static final Logger LOGGER = LoggerFactory.getLogger("Sodium-ResourcePackScanner");
   private static final Set<String> SHADER_PROGRAM_BLACKLIST = Set.of(
      "rendertype_solid.vsh",
      "rendertype_solid.fsh",
      "rendertype_solid.json",
      "rendertype_cutout_mipped.vsh",
      "rendertype_cutout_mipped.fsh",
      "rendertype_cutout_mipped.json",
      "rendertype_cutout.vsh",
      "rendertype_cutout.fsh",
      "rendertype_cutout.json",
      "rendertype_translucent.vsh",
      "rendertype_translucent.fsh",
      "rendertype_translucent.json",
      "rendertype_tripwire.vsh",
      "rendertype_tripwire.fsh",
      "rendertype_tripwire.json",
      "rendertype_clouds.vsh",
      "rendertype_clouds.fsh",
      "rendertype_clouds.json"
   );
   private static final Set<String> SHADER_INCLUDE_BLACKLIST = Set.of("light.glsl", "fog.glsl");

   public static void checkIfCoreShaderLoaded(ResourceManager manager) {
      List<ResourcePackScanner.ScannedResourcePack> outputs = manager.streamResourcePacks()
         .filter(ResourcePackScanner::isExternalResourcePack)
         .map(ResourcePackScanner::scanResources)
         .toList();
      printToasts(outputs);
      printCompatibilityReport(outputs);
   }

   private static void printToasts(Collection<ResourcePackScanner.ScannedResourcePack> resourcePacks) {
      List<ResourcePackScanner.ScannedResourcePack> incompatibleResourcePacks = resourcePacks.stream().filter(pack -> !pack.shaderPrograms.isEmpty()).toList();
      List<ResourcePackScanner.ScannedResourcePack> likelyIncompatibleResourcePacks = resourcePacks.stream()
         .filter(pack -> !pack.shaderIncludes.isEmpty())
         .filter(pack -> !incompatibleResourcePacks.contains(pack))
         .toList();
      boolean shown = false;
      if (!incompatibleResourcePacks.isEmpty()) {
         showConsoleMessage("sodium.console.core_shaders_error", true, MessageLevel.SEVERE);

         for (ResourcePackScanner.ScannedResourcePack entry : incompatibleResourcePacks) {
            showConsoleMessage(getResourcePackName(entry.resourcePack), false, MessageLevel.SEVERE);
         }

         shown = true;
      }

      if (!likelyIncompatibleResourcePacks.isEmpty()) {
         showConsoleMessage("sodium.console.core_shaders_warn", true, MessageLevel.WARN);

         for (ResourcePackScanner.ScannedResourcePack entry : likelyIncompatibleResourcePacks) {
            showConsoleMessage(getResourcePackName(entry.resourcePack), false, MessageLevel.WARN);
         }

         shown = true;
      }

      if (shown) {
         showConsoleMessage("sodium.console.core_shaders_info", true, MessageLevel.INFO);
      }
   }

   private static void printCompatibilityReport(Collection<ResourcePackScanner.ScannedResourcePack> scanResults) {
      StringBuilder builder = new StringBuilder();

      for (ResourcePackScanner.ScannedResourcePack entry : scanResults) {
         if (!entry.shaderPrograms.isEmpty() || !entry.shaderIncludes.isEmpty()) {
            builder.append("- Resource pack: ").append(getResourcePackName(entry.resourcePack)).append("\n");
            if (!entry.shaderPrograms.isEmpty()) {
               emitProblem(
                  builder,
                  "The resource pack replaces terrain shaders, which are not supported",
                  "https://github.com/CaffeineMC/sodium/wiki/Resource-Packs",
                  entry.shaderPrograms
               );
            }

            if (!entry.shaderIncludes.isEmpty()) {
               emitProblem(
                  builder,
                  "The resource pack modifies shader include files, which are not fully supported",
                  "https://github.com/CaffeineMC/sodium/wiki/Resource-Packs",
                  entry.shaderIncludes
               );
            }
         }
      }

      if (!builder.isEmpty()) {
         LOGGER.error("The following compatibility issues were found with installed resource packs:\n{}", builder);
      }
   }

   private static void emitProblem(StringBuilder builder, String description, String url, List<String> resources) {
      builder.append("\t- Problem found: ").append("\n");
      builder.append("\t\t- Description:\n\t\t\t").append(description).append("\n");
      builder.append("\t\t- More information: ").append(url).append("\n");
      builder.append("\t\t- Files: ").append("\n");

      for (String resource : resources) {
         builder.append("\t\t\t- ").append(resource).append("\n");
      }
   }

   @NotNull
   private static ResourcePackScanner.ScannedResourcePack scanResources(ResourcePack resourcePack) {
      List<String> ignoredShaders = determineIgnoredShaders(resourcePack);
      if (!ignoredShaders.isEmpty()) {
         LOGGER.warn(
            "Resource pack '{}' indicates the following shaders should be ignored: {}", getResourcePackName(resourcePack), String.join(", ", ignoredShaders)
         );
      }

      ArrayList<String> unsupportedShaderPrograms = new ArrayList<>();
      ArrayList<String> unsupportedShaderIncludes = new ArrayList<>();
      resourcePack.findResources(ResourceType.CLIENT_RESOURCES, "minecraft", "shaders", (identifier, supplier) -> {
         String path = identifier.getPath();
         String name = path.substring(path.lastIndexOf(47) + 1);
         if (!ignoredShaders.contains(name)) {
            if (SHADER_PROGRAM_BLACKLIST.contains(name)) {
               unsupportedShaderPrograms.add(path);
            } else if (SHADER_INCLUDE_BLACKLIST.contains(name)) {
               unsupportedShaderIncludes.add(path);
            }
         }
      });
      return new ResourcePackScanner.ScannedResourcePack(resourcePack, unsupportedShaderPrograms, unsupportedShaderIncludes);
   }

   private static boolean isExternalResourcePack(ResourcePack pack) {
      return pack instanceof DirectoryResourcePack || pack instanceof ZipResourcePack;
   }

   private static String getResourcePackName(ResourcePack pack) {
      String path = pack.getId();
      return path.startsWith("file/") ? path.substring(5) : path;
   }

   private static List<String> determineIgnoredShaders(ResourcePack resourcePack) {
      ArrayList<String> ignoredShaders = new ArrayList<>();

      try {
         SodiumResourcePackMetadata meta = resourcePack.parseMetadata(SodiumResourcePackMetadata.SERIALIZER);
         if (meta != null) {
            ignoredShaders.addAll(meta.ignoredShaders());
         }
      } catch (IOException var3) {
         LOGGER.error("Failed to load pack.mcmeta file for resource pack '{}'", resourcePack.getId());
      }

      return ignoredShaders;
   }

   private static void showConsoleMessage(String message, boolean translatable, MessageLevel messageLevel) {
      Console.instance().logMessage(messageLevel, message, translatable, 12.5);
   }

   private record ScannedResourcePack(ResourcePack resourcePack, ArrayList<String> shaderPrograms, ArrayList<String> shaderIncludes) {
   }
}
