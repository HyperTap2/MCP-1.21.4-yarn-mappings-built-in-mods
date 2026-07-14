package com.viaversion.viafabricplus;

import com.viaversion.viafabricplus.api.ViaFabricPlusBase;
import com.viaversion.viafabricplus.api.events.ChangeProtocolVersionCallback;
import com.viaversion.viafabricplus.api.events.LoadingCycleCallback;
import com.viaversion.viafabricplus.api.events.LoadingCycleCallback.LoadingCycle;
import com.viaversion.viafabricplus.api.settings.SettingGroup;
import com.viaversion.viafabricplus.base.Events;
import com.viaversion.viafabricplus.base.overriding_jars.ClassLoaderPriorityUtil;
import com.viaversion.viafabricplus.base.sync_tasks.SyncTasks;
import com.viaversion.viafabricplus.features.FeaturesLoading;
import com.viaversion.viafabricplus.features.item.filter_creative_tabs.ItemRegistryDiff;
import com.viaversion.viafabricplus.features.item.negative_item_count.NegativeItemUtil;
import com.viaversion.viafabricplus.features.limitation.max_chat_length.MaxChatLength;
import com.viaversion.viafabricplus.injection.access.base.IClientConnection;
import com.viaversion.viafabricplus.injection.access.base.IServerInfo;
import com.viaversion.viafabricplus.protocoltranslator.ProtocolTranslator;
import com.viaversion.viafabricplus.protocoltranslator.translator.ItemTranslator;
import com.viaversion.viafabricplus.save.SaveManager;
import com.viaversion.viafabricplus.screen.impl.ProtocolSelectionScreen;
import com.viaversion.viafabricplus.screen.impl.settings.SettingsScreen;
import com.viaversion.viafabricplus.settings.SettingsManager;
import com.viaversion.viafabricplus.util.ChatUtil;
import com.viaversion.viafabricplus.visuals.ViaFabricPlusVisuals;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import io.netty.channel.Channel;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.item.ItemStack;
import net.minecraft.network.ClientConnection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

public final class ViaFabricPlusImpl implements ViaFabricPlusBase {
   public static final ViaFabricPlusImpl INSTANCE = new ViaFabricPlusImpl();
   private final Logger logger = LogManager.getLogger("ViaFabricPlus");
   private final Path path = Path.of("config", "viafabricplus");
   private String version;
   private String implVersion;
   private CompletableFuture<Void> loadingFuture;

   public void init() {
      ViaFabricPlus.init(INSTANCE);
      ViaFabricPlusVisuals.INSTANCE.init(this);
      this.version = "4.0.5-BACKPORT";
      this.implVersion = "git-viafabricplus-4.0.5-BACKPORT:ca43e3e5";
      if (!Files.exists(this.path)) {
         try {
            Files.createDirectories(this.path);
         } catch (IOException e) {
            this.logger.error("Failed to create ViaFabricPlus directory", e);
         }
      }

      ClassLoaderPriorityUtil.loadOverridingJars(this.path, this.logger);
      SettingsManager.INSTANCE.init();
      SaveManager.INSTANCE.init();
      SyncTasks.init();
      FeaturesLoading.init();
      this.loadingFuture = ProtocolTranslator.init(this.path);
      this.registerLoadingCycleCallback(cycle -> {
         if (cycle == LoadingCycle.POST_GAME_LOAD) {
            this.loadingFuture.join();
            SaveManager.INSTANCE.postInit();
         }
      });
      ((LoadingCycleCallback)Events.LOADING_CYCLE.invoker()).onLoadCycle(LoadingCycle.FINAL_LOAD);
   }

   public String getVersion() {
      return this.version;
   }

   public String getImplVersion() {
      return this.implVersion;
   }

   public Path rootPath() {
      return this.path;
   }

   @Nullable
   public ProtocolVersion getTargetVersion() {
      return ProtocolTranslator.getTargetVersion();
   }

   @Nullable
   public ProtocolVersion getTargetVersion(Channel channel) {
      return ProtocolTranslator.getTargetVersion(channel);
   }

   @Nullable
   public ProtocolVersion getTargetVersion(ClientConnection connection) {
      return ((IClientConnection)connection).viaFabricPlus$getTargetVersion();
   }

   public void setTargetVersion(ProtocolVersion newVersion) {
      ProtocolTranslator.setTargetVersion(newVersion);
   }

   public void setTargetVersion(ProtocolVersion newVersion, boolean revertOnDisconnect) {
      ProtocolTranslator.setTargetVersion(newVersion, revertOnDisconnect);
   }

   @Nullable
   public UserConnection getPlayNetworkUserConnection() {
      return ProtocolTranslator.getPlayNetworkUserConnection();
   }

   @Nullable
   public UserConnection getUserConnection(ClientConnection connection) {
      return ((IClientConnection)connection).viaFabricPlus$getUserConnection();
   }

   @Nullable
   public ProtocolVersion getServerVersion(ServerInfo serverInfo) {
      return ((IServerInfo)serverInfo).viaFabricPlus$forcedVersion();
   }

   public void registerOnChangeProtocolVersionCallback(ChangeProtocolVersionCallback callback) {
      Events.CHANGE_PROTOCOL_VERSION.register(callback);
   }

   public void registerLoadingCycleCallback(LoadingCycleCallback callback) {
      Events.LOADING_CYCLE.register(callback);
   }

   public int getMaxChatLength(ProtocolVersion version) {
      return MaxChatLength.getChatLength();
   }

   public List<SettingGroup> settingGroups() {
      return SettingsManager.INSTANCE.getGroups();
   }

   public void addSettingGroup(SettingGroup group) {
      SettingsManager.INSTANCE.addGroup(group);
   }

   @Nullable
   public SettingGroup getSettingGroup(String translationKey) {
      for (SettingGroup group : SettingsManager.INSTANCE.getGroups()) {
         if (ChatUtil.uncoverTranslationKey(group.getName()).equals(translationKey)) {
            return group;
         }
      }

      return null;
   }

   public void openProtocolSelectionScreen(Screen parent) {
      ProtocolSelectionScreen.INSTANCE.open(parent);
   }

   public void openSettingsScreen(Screen parent) {
      SettingsScreen.INSTANCE.open(parent);
   }

   @Nullable
   public Item translateItem(ItemStack stack, ProtocolVersion targetVersion) {
      return ItemTranslator.mcToVia(stack, targetVersion);
   }

   @Nullable
   public ItemStack translateItem(Item item, ProtocolVersion sourceVersion) {
      return ItemTranslator.viaToMc(item, sourceVersion);
   }

   public boolean itemExists(net.minecraft.item.Item item, ProtocolVersion version) {
      return ItemRegistryDiff.contains(item, version);
   }

   public boolean itemExistsInConnection(net.minecraft.item.Item item) {
      return ItemRegistryDiff.keepItem(item);
   }

   public int getStackCount(ItemStack stack) {
      return NegativeItemUtil.getCount(stack);
   }

   public Logger logger() {
      return this.logger;
   }
}
