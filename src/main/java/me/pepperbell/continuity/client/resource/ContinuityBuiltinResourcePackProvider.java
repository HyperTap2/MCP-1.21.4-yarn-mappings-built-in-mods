package me.pepperbell.continuity.client.resource;

import java.net.URI;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import me.pepperbell.continuity.client.ContinuityClient;
import net.minecraft.resource.DirectoryResourcePack;
import net.minecraft.resource.ResourcePackInfo;
import net.minecraft.resource.ResourcePackPosition;
import net.minecraft.resource.ResourcePackProfile;
import net.minecraft.resource.ResourcePackProvider;
import net.minecraft.resource.ResourcePackSource;
import net.minecraft.resource.ResourceType;
import net.minecraft.text.Text;

public final class ContinuityBuiltinResourcePackProvider implements ResourcePackProvider {
   private static final ResourcePackPosition POSITION = new ResourcePackPosition(
      false, ResourcePackProfile.InsertionPosition.TOP, false
   );
   private static final List<FileSystem> OPEN_FILE_SYSTEMS = new ArrayList<>();

   @Override
   public void register(Consumer<ResourcePackProfile> profileAdder) {
      this.register(profileAdder, "default", "resourcePack.continuity.default.name");
      this.register(profileAdder, "glass_pane_culling_fix", "resourcePack.continuity.glass_pane_culling_fix.name");
   }

   private void register(Consumer<ResourcePackProfile> profileAdder, String path, String titleKey) {
      Path root = locatePack(path);
      if (root == null) {
         ContinuityClient.LOGGER.warn("Could not locate bundled Continuity resource pack '{}'", path);
         return;
      }

      ResourcePackInfo info = new ResourcePackInfo(
         "continuity/" + path, Text.translatable(titleKey), ResourcePackSource.BUILTIN, Optional.empty()
      );
      ResourcePackProfile profile = ResourcePackProfile.create(
         info, new DirectoryResourcePack.DirectoryBackedFactory(root), ResourceType.CLIENT_RESOURCES, POSITION
      );
      if (profile != null) {
         profileAdder.accept(profile);
      }
   }

   private static Path locatePack(String path) {
      try {
         URL url = ContinuityClient.class.getResource("/resourcepacks/" + path + "/pack.mcmeta");
         if (url == null) {
            return null;
         }

         URI uri = url.toURI();
         try {
            return Paths.get(uri).getParent();
         } catch (FileSystemNotFoundException ignored) {
            synchronized (OPEN_FILE_SYSTEMS) {
               try {
                  FileSystem fileSystem = FileSystems.newFileSystem(uri, Map.of());
                  OPEN_FILE_SYSTEMS.add(fileSystem);
               } catch (java.nio.file.FileSystemAlreadyExistsException alreadyExists) {
                  FileSystems.getFileSystem(uri);
               }
            }
            return Paths.get(uri).getParent();
         }
      } catch (Exception exception) {
         ContinuityClient.LOGGER.warn("Failed to open bundled Continuity resource pack '{}'", path, exception);
         return null;
      }
   }
}
