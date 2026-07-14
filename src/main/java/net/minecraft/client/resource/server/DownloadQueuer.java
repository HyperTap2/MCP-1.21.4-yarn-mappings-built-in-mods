package net.minecraft.client.resource.server;

import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.util.Downloader.DownloadEntry;
import net.minecraft.util.Downloader.DownloadResult;

public interface DownloadQueuer {
   void enqueue(Map<UUID, DownloadEntry> entries, Consumer<DownloadResult> callback);
}
