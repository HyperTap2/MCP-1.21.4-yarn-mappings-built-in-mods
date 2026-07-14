package net.minecraft.client.resource.server;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

public interface ReloadScheduler {
   void scheduleReload(ReloadScheduler.ReloadContext context);

   record PackInfo(UUID id, Path path) {
   }

   interface ReloadContext {
      void onSuccess();

      void onFailure(boolean force);

      List<ReloadScheduler.PackInfo> getPacks();
   }
}
