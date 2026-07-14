package net.minecraft.client;

import java.io.File;
import java.time.Duration;
import net.minecraft.server.dedicated.DedicatedServerWatchdog;
import net.minecraft.util.crash.CrashReport;

public class ClientWatchdog {
   private static final Duration timeout = Duration.ofSeconds(15L);

   public static void shutdownClient(File runDir, long threadId) {
      Thread thread = new Thread(() -> {
         try {
            Thread.sleep(timeout);
         } catch (InterruptedException interruptedException) {
            return;
         }

         CrashReport crashReport = DedicatedServerWatchdog.createCrashReport("Client shutdown", threadId);
         MinecraftClient.saveCrashReport(runDir, crashReport);
      });
      thread.setDaemon(true);
      thread.setName("Client shutdown watchdog");
      thread.start();
   }
}
