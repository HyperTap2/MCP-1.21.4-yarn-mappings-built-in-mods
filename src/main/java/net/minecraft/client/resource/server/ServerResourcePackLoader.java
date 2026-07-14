package net.minecraft.client.resource.server;

import com.viaversion.viafabricplus.features.networking.resource_pack_header.ResourcePackHeaderDiff;
import com.viaversion.viafabricplus.protocoltranslator.ProtocolTranslator;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.google.common.collect.Lists;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.mojang.logging.LogUtils;
import com.mojang.util.UndashedUuid;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.Proxy;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import net.minecraft.GameVersion;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.RunArgs;
import net.minecraft.client.realms.SizeUnit;
import net.minecraft.client.session.Session;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.c2s.common.ResourcePackStatusC2SPacket;
import net.minecraft.network.packet.c2s.common.ResourcePackStatusC2SPacket.Status;
import net.minecraft.resource.ResourcePackInfo;
import net.minecraft.resource.ResourcePackPosition;
import net.minecraft.resource.ResourcePackProfile;
import net.minecraft.resource.ResourcePackProvider;
import net.minecraft.resource.ResourcePackSource;
import net.minecraft.resource.ResourceType;
import net.minecraft.resource.ResourcePackProfile.InsertionPosition;
import net.minecraft.resource.ResourcePackProfile.Metadata;
import net.minecraft.resource.ResourcePackProfile.PackFactory;
import net.minecraft.resource.ZipResourcePack.ZipBackedFactory;
import net.minecraft.text.Text;
import net.minecraft.util.Downloader;
import net.minecraft.util.Downloader.Config;
import net.minecraft.util.Downloader.DownloadEntry;
import net.minecraft.util.Downloader.DownloadResult;
import net.minecraft.util.NetworkUtils.DownloadListener;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

public class ServerResourcePackLoader implements AutoCloseable {
   private static final Text SERVER_NAME_TEXT = Text.translatable("resourcePack.server.name");
   private static final Pattern SHA1_PATTERN = Pattern.compile("^[a-fA-F0-9]{40}$");
   static final Logger LOGGER = LogUtils.getLogger();
   private static final ResourcePackProvider NOOP_PROVIDER = profileAdder -> {};
   private static final ResourcePackPosition POSITION = new ResourcePackPosition(true, InsertionPosition.TOP, true);
   private static final PackStateChangeCallback DEBUG_PACK_STATE_CHANGE_CALLBACK = new PackStateChangeCallback() {
      @Override
      public void onStateChanged(UUID id, PackStateChangeCallback.State state) {
         ServerResourcePackLoader.LOGGER.debug("Downloaded pack {} changed state to {}", id, state);
      }

      @Override
      public void onFinish(UUID id, PackStateChangeCallback.FinishState state) {
         ServerResourcePackLoader.LOGGER.debug("Downloaded pack {} finished with state {}", id, state);
      }
   };
   final MinecraftClient client;
   private ResourcePackProvider packProvider = NOOP_PROVIDER;
   @Nullable
   private ReloadScheduler.ReloadContext reloadContext;
   final ServerResourcePackManager manager;
   private final Downloader downloader;
   private ResourcePackSource packSource = ResourcePackSource.SERVER;
   PackStateChangeCallback packStateChangeCallback = DEBUG_PACK_STATE_CHANGE_CALLBACK;
   private int packIndex;

   public ServerResourcePackLoader(MinecraftClient client, Path downloadsDirectory, RunArgs.Network runArgs) {
      this.client = client;

      try {
         this.downloader = new Downloader(downloadsDirectory);
      } catch (IOException iOException) {
         throw new UncheckedIOException("Failed to open download queue in directory " + downloadsDirectory, iOException);
      }

      Executor executor = client::send;
      this.manager = new ServerResourcePackManager(
         this.createDownloadQueuer(this.downloader, executor, runArgs.session, runArgs.netProxy), new PackStateChangeCallback() {
            @Override
            public void onStateChanged(UUID id, PackStateChangeCallback.State state) {
               ServerResourcePackLoader.this.packStateChangeCallback.onStateChanged(id, state);
            }

            @Override
            public void onFinish(UUID id, PackStateChangeCallback.FinishState state) {
               ServerResourcePackLoader.this.packStateChangeCallback.onFinish(id, state);
            }
         }, this.getReloadScheduler(), this.createPackChangeCallback(executor), ServerResourcePackManager.AcceptanceStatus.PENDING
      );
   }

   DownloadListener createListener(int entryCount) {
      return new DownloadListener() {
         private final SystemToast.Type toastType = new SystemToast.Type();
         private Text toastTitle = Text.empty();
         @Nullable
         private Text toastDescription = null;
         private int current;
         private int failureCount;
         private OptionalLong contentLength = OptionalLong.empty();

         private void showToast() {
            ServerResourcePackLoader.this.client
               .execute(() -> SystemToast.show(ServerResourcePackLoader.this.client.getToastManager(), this.toastType, this.toastTitle, this.toastDescription));
         }

         private void showProgress(long writtenBytes) {
            if (this.contentLength.isPresent()) {
               this.toastDescription = Text.translatable("download.pack.progress.percent", new Object[]{writtenBytes * 100L / this.contentLength.getAsLong()});
            } else {
               this.toastDescription = Text.translatable("download.pack.progress.bytes", new Object[]{SizeUnit.getUserFriendlyString(writtenBytes)});
            }

            this.showToast();
         }

         public void onStart() {
            this.current++;
            this.toastTitle = Text.translatable("download.pack.title", new Object[]{this.current, entryCount});
            this.showToast();
            ServerResourcePackLoader.LOGGER.debug("Starting pack {}/{} download", this.current, entryCount);
         }

         public void onContentLength(OptionalLong contentLength) {
            ServerResourcePackLoader.LOGGER.debug("File size = {} bytes", contentLength);
            this.contentLength = contentLength;
            this.showProgress(0L);
         }

         public void onProgress(long writtenBytes) {
            ServerResourcePackLoader.LOGGER.debug("Progress for pack {}: {} bytes", this.current, writtenBytes);
            this.showProgress(writtenBytes);
         }

         public void onFinish(boolean success) {
            if (!success) {
               ServerResourcePackLoader.LOGGER.info("Pack {} failed to download", this.current);
               this.failureCount++;
            } else {
               ServerResourcePackLoader.LOGGER.debug("Download ended for pack {}", this.current);
            }

            if (this.current == entryCount) {
               if (this.failureCount > 0) {
                  this.toastTitle = Text.translatable("download.pack.failed", new Object[]{this.failureCount, entryCount});
                  this.toastDescription = null;
                  this.showToast();
               } else {
                  SystemToast.hide(ServerResourcePackLoader.this.client.getToastManager(), this.toastType);
               }
            }
         }
      };
   }

   private DownloadQueuer createDownloadQueuer(Downloader downloader, Executor executor, Session session, Proxy proxy) {
      return new DownloadQueuer() {
         private static final int MAX_BYTES = 262144000;
         private static final HashFunction SHA1 = Hashing.sha1();

         private Map<String, String> getHeaders() {
            GameVersion gameVersion = ResourcePackHeaderDiff.get(ProtocolTranslator.getTargetVersion());
            Map<String, String> headers = new LinkedHashMap<>();
            headers.put("X-Minecraft-Username", session.getUsername());
            headers.put("X-Minecraft-UUID", UndashedUuid.toString(session.getUuidOrNull()));
            headers.put("X-Minecraft-Version", gameVersion.getName());
            headers.put("X-Minecraft-Version-ID", gameVersion.getId());
            headers.put("X-Minecraft-Pack-Format", String.valueOf(gameVersion.getResourceVersion(ResourceType.CLIENT_RESOURCES)));
            headers.put("User-Agent", "Minecraft Java/" + gameVersion.getName());
            if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_14_3)) {
               headers.remove("X-Minecraft-Version-ID");
               if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_12_2)) {
                  headers.remove("X-Minecraft-Pack-Format");
                  headers.remove("User-Agent");
               }
            }
            return headers;
         }

         @Override
         public void enqueue(Map<UUID, DownloadEntry> entries, Consumer<DownloadResult> callback) {
            downloader.downloadAsync(
                  new Config(SHA1, 262144000, this.getHeaders(), proxy, ServerResourcePackLoader.this.createListener(entries.size())), entries
               )
               .thenAcceptAsync(callback, executor);
         }
      };
   }

   private Runnable createPackChangeCallback(Executor executor) {
      return new Runnable() {
         private boolean currentlyRunning;
         private boolean shouldKeepRunning;

         @Override
         public void run() {
            this.shouldKeepRunning = true;
            if (!this.currentlyRunning) {
               this.currentlyRunning = true;
               executor.execute(this::runOnExecutor);
            }
         }

         private void runOnExecutor() {
            while (this.shouldKeepRunning) {
               this.shouldKeepRunning = false;
               ServerResourcePackLoader.this.manager.update();
            }

            this.currentlyRunning = false;
         }
      };
   }

   private ReloadScheduler getReloadScheduler() {
      return this::reload;
   }

   @Nullable
   private List<ResourcePackProfile> toProfiles(List<ReloadScheduler.PackInfo> packs) {
      List<ResourcePackProfile> list = new ArrayList<>(packs.size());

      for (ReloadScheduler.PackInfo packInfo : Lists.reverse(packs)) {
         String string = String.format(Locale.ROOT, "server/%08X/%s", this.packIndex++, packInfo.id());
         Path path = packInfo.path();
         ResourcePackInfo resourcePackInfo = new ResourcePackInfo(string, SERVER_NAME_TEXT, this.packSource, Optional.empty());
         PackFactory packFactory = new ZipBackedFactory(path);
         int i = SharedConstants.getGameVersion().getResourceVersion(ResourceType.CLIENT_RESOURCES);
         Metadata metadata = ResourcePackProfile.loadMetadata(resourcePackInfo, packFactory, i);
         if (metadata == null) {
            LOGGER.warn("Invalid pack metadata in {}, ignoring all", path);
            return null;
         }

         list.add(new ResourcePackProfile(resourcePackInfo, packFactory, metadata, POSITION));
      }

      return list;
   }

   public ResourcePackProvider getPassthroughPackProvider() {
      return packAdder -> this.packProvider.register(packAdder);
   }

   private static ResourcePackProvider getPackProvider(List<ResourcePackProfile> serverPacks) {
      return serverPacks.isEmpty() ? NOOP_PROVIDER : serverPacks::forEach;
   }

   private void reload(ReloadScheduler.ReloadContext context) {
      this.reloadContext = context;
      List<ReloadScheduler.PackInfo> list = context.getPacks();
      List<ResourcePackProfile> list2 = this.toProfiles(list);
      if (list2 == null) {
         context.onFailure(false);
         List<ReloadScheduler.PackInfo> list3 = context.getPacks();
         list2 = this.toProfiles(list3);
         if (list2 == null) {
            LOGGER.warn("Double failure in loading server packs");
            list2 = List.of();
         }
      }

      this.packProvider = getPackProvider(list2);
      this.client.reloadResources();
   }

   public void onReloadFailure() {
      if (this.reloadContext != null) {
         this.reloadContext.onFailure(false);
         List<ResourcePackProfile> list = this.toProfiles(this.reloadContext.getPacks());
         if (list == null) {
            LOGGER.warn("Double failure in loading server packs");
            list = List.of();
         }

         this.packProvider = getPackProvider(list);
      }
   }

   public void onForcedReloadFailure() {
      if (this.reloadContext != null) {
         this.reloadContext.onFailure(true);
         this.reloadContext = null;
         this.packProvider = NOOP_PROVIDER;
      }
   }

   public void onReloadSuccess() {
      if (this.reloadContext != null) {
         this.reloadContext.onSuccess();
         this.reloadContext = null;
      }
   }

   @Nullable
   private static HashCode toHashCode(@Nullable String hash) {
      return hash != null && SHA1_PATTERN.matcher(hash).matches() ? HashCode.fromString(hash.toLowerCase(Locale.ROOT)) : null;
   }

   public void addResourcePack(UUID id, URL url, @Nullable String hash) {
      HashCode hashCode = toHashCode(hash);
      this.manager.addResourcePack(id, url, hashCode);
   }

   public void addResourcePack(UUID id, Path path) {
      this.manager.addResourcePack(id, path);
   }

   public void remove(UUID id) {
      this.manager.remove(id);
   }

   public void removeAll() {
      this.manager.removeAll();
   }

   private static PackStateChangeCallback getStateChangeCallback(ClientConnection connection) {
      return new PackStateChangeCallback() {
         @Override
         public void onStateChanged(UUID id, PackStateChangeCallback.State state) {
            ServerResourcePackLoader.LOGGER.debug("Pack {} changed status to {}", id, state);

            Status status = switch (state) {
               case ACCEPTED -> Status.ACCEPTED;
               case DOWNLOADED -> Status.DOWNLOADED;
            };
            connection.send(new ResourcePackStatusC2SPacket(id, status));
         }

         @Override
         public void onFinish(UUID id, PackStateChangeCallback.FinishState state) {
            ServerResourcePackLoader.LOGGER.debug("Pack {} changed status to {}", id, state);

            Status status = switch (state) {
               case APPLIED -> Status.SUCCESSFULLY_LOADED;
               case DOWNLOAD_FAILED -> Status.FAILED_DOWNLOAD;
               case DECLINED -> Status.DECLINED;
               case DISCARDED -> Status.DISCARDED;
               case ACTIVATION_FAILED -> Status.FAILED_RELOAD;
            };
            connection.send(new ResourcePackStatusC2SPacket(id, status));
         }
      };
   }

   public void init(ClientConnection connection, ServerResourcePackManager.AcceptanceStatus acceptanceStatus) {
      this.packSource = ResourcePackSource.SERVER;
      this.packStateChangeCallback = getStateChangeCallback(connection);
      switch (acceptanceStatus) {
         case ALLOWED:
            this.manager.acceptAll();
            break;
         case DECLINED:
            this.manager.declineAll();
            break;
         case PENDING:
            this.manager.resetAcceptanceStatus();
      }
   }

   public void initWorldPack() {
      this.packSource = ResourcePackSource.WORLD;
      this.packStateChangeCallback = DEBUG_PACK_STATE_CHANGE_CALLBACK;
      this.manager.acceptAll();
   }

   public void acceptAll() {
      this.manager.acceptAll();
   }

   public void declineAll() {
      this.manager.declineAll();
   }

   public CompletableFuture<Void> getPackLoadFuture(UUID requestedId) {
      final CompletableFuture<Void> completableFuture = new CompletableFuture<>();
      final PackStateChangeCallback packStateChangeCallback = this.packStateChangeCallback;
      this.packStateChangeCallback = new PackStateChangeCallback() {
         @Override
         public void onStateChanged(UUID id, PackStateChangeCallback.State state) {
            packStateChangeCallback.onStateChanged(id, state);
         }

         @Override
         public void onFinish(UUID id, PackStateChangeCallback.FinishState state) {
            if (requestedId.equals(id)) {
               ServerResourcePackLoader.this.packStateChangeCallback = packStateChangeCallback;
               if (state == PackStateChangeCallback.FinishState.APPLIED) {
                  completableFuture.complete(null);
               } else {
                  completableFuture.completeExceptionally(new IllegalStateException("Failed to apply pack " + id + ", reason: " + state));
               }
            }

            packStateChangeCallback.onFinish(id, state);
         }
      };
      return completableFuture;
   }

   public void clear() {
      this.manager.removeAll();
      this.packStateChangeCallback = DEBUG_PACK_STATE_CHANGE_CALLBACK;
      this.manager.resetAcceptanceStatus();
   }

   @Override
   public void close() throws IOException {
      this.downloader.close();
   }
}
