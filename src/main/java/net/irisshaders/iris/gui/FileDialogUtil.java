package net.irisshaders.iris.gui;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

public final class FileDialogUtil {
   private static final ExecutorService FILE_DIALOG_EXECUTOR = Executors.newSingleThreadExecutor();

   private FileDialogUtil() {
   }

   public static CompletableFuture<Optional<Path>> fileSelectDialog(
      FileDialogUtil.DialogType dialog, String title, @Nullable Path origin, @Nullable String filterLabel, String... filters
   ) {
      CompletableFuture<Optional<Path>> future = new CompletableFuture<>();
      FILE_DIALOG_EXECUTOR.submit(() -> {
         String result = null;
         MemoryStack stack = MemoryStack.stackPush();

         try {
            PointerBuffer filterBuffer = stack.mallocPointer(filters.length);

            for (String filter : filters) {
               filterBuffer.put(stack.UTF8(filter));
            }

            filterBuffer.flip();
            String path = origin != null ? origin.toAbsolutePath().toString() : null;
            if (dialog == FileDialogUtil.DialogType.SAVE) {
               result = TinyFileDialogs.tinyfd_saveFileDialog(title, path, filterBuffer, filterLabel);
            } else if (dialog == FileDialogUtil.DialogType.OPEN) {
               result = TinyFileDialogs.tinyfd_openFileDialog(title, path, filterBuffer, filterLabel, false);
            }
         } catch (Throwable var14) {
            if (stack != null) {
               try {
                  stack.close();
               } catch (Throwable var13) {
                  var14.addSuppressed(var13);
               }
            }

            throw var14;
         }

         if (stack != null) {
            stack.close();
         }

         future.complete(Optional.ofNullable(result).map(x$0 -> Paths.get(x$0)));
      });
      return future;
   }

   public enum DialogType {
      SAVE,
      OPEN;
   }
}
