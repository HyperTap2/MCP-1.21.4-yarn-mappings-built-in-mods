package com.viaversion.viafabricplus.base.sync_tasks;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;

public final class SyncTasks {
   private static final Map<String, Consumer<RegistryByteBuf>> PENDING_EXECUTION_TASKS = new ConcurrentHashMap<>();
   public static final String PACKET_SYNC_IDENTIFIER = UUID.randomUUID() + ":" + UUID.randomUUID();

   public static void init() {
   }

   public static String executeSyncTask(Consumer<RegistryByteBuf> task) {
      String uuid = UUID.randomUUID().toString();
      PENDING_EXECUTION_TASKS.put(uuid, task);
      return uuid;
   }

   public static void handleSyncTask(PacketByteBuf buf) {
      String uuid = buf.readString();
      if (PENDING_EXECUTION_TASKS.containsKey(uuid)) {
         MinecraftClient.getInstance().execute(() -> {
            Consumer<RegistryByteBuf> task = PENDING_EXECUTION_TASKS.remove(uuid);
            task.accept(new RegistryByteBuf(buf, MinecraftClient.getInstance().getNetworkHandler().getRegistryManager()));
         });
      }
   }

   static {
      DataCustomPayload.init();
   }
}
