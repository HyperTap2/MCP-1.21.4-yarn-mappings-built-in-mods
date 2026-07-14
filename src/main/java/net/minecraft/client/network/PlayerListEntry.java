package net.minecraft.client.network;

import com.google.common.base.Suppliers;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.yggdrasil.ProfileResult;
import com.viaversion.viafabricplus.protocoltranslator.ProtocolTranslator;
import com.viaversion.viafabricplus.visuals.features.r1_7_tab_list_style.LegacyTabList;
import com.viaversion.viafabricplus.visuals.injection.access.r1_7_tab_list_tyle.IPlayerListEntry;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.PlayerSkinProvider;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.network.encryption.PlayerPublicKey;
import net.minecraft.network.encryption.PublicPlayerSession;
import net.minecraft.network.message.MessageVerifier;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.world.GameMode;
import org.jetbrains.annotations.Nullable;

public class PlayerListEntry implements IPlayerListEntry {
   private final GameProfile profile;
   private final Supplier<SkinTextures> texturesSupplier;
   private GameMode gameMode = GameMode.DEFAULT;
   private int latency;
   @Nullable
   private Text displayName;
   private boolean showHat = true;
   @Nullable
   private PublicPlayerSession session;
   private MessageVerifier messageVerifier;
   private int listOrder;
   private final int viaFabricPlusVisuals$index = LegacyTabList.globalTablistIndex++;

   public PlayerListEntry(GameProfile profile, boolean secureChatEnforced) {
      this.profile = profile;
      this.messageVerifier = getInitialVerifier(secureChatEnforced);
      Supplier<Supplier<SkinTextures>> supplier = Suppliers.memoize(() -> texturesSupplier(profile));
      this.texturesSupplier = () -> supplier.get().get();
   }

   private static Supplier<SkinTextures> texturesSupplier(GameProfile profile) {
      MinecraftClient minecraftClient = MinecraftClient.getInstance();
      PlayerSkinProvider playerSkinProvider = minecraftClient.getSkinProvider();
      CompletableFuture<Optional<SkinTextures>> completableFuture;
      if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_20) && !profile.getProperties().containsKey("textures")) {
         completableFuture = CompletableFuture.supplyAsync(() -> {
            ProfileResult profileResult = minecraftClient.getSessionService().fetchProfile(profile.getId(), true);
            return profileResult == null ? profile : profileResult.profile();
         }, Util.getMainWorkerExecutor()).thenCompose(playerSkinProvider::fetchSkinTextures);
      } else {
         completableFuture = playerSkinProvider.fetchSkinTextures(profile);
      }
      boolean bl = !minecraftClient.uuidEquals(profile.getId());
      SkinTextures skinTextures = DefaultSkinHelper.getSkinTextures(profile);
      return () -> {
         SkinTextures skinTextures2 = completableFuture.getNow(Optional.empty()).orElse(skinTextures);
         return bl && !skinTextures2.secure() ? skinTextures : skinTextures2;
      };
   }

   public GameProfile getProfile() {
      return this.profile;
   }

   @Nullable
   public PublicPlayerSession getSession() {
      return this.session;
   }

   public MessageVerifier getMessageVerifier() {
      return this.messageVerifier;
   }

   public boolean hasPublicKey() {
      return this.session != null;
   }

   protected void setSession(PublicPlayerSession session) {
      this.session = session;
      this.messageVerifier = session.createVerifier(PlayerPublicKey.EXPIRATION_GRACE_PERIOD);
   }

   protected void resetSession(boolean secureChatEnforced) {
      this.session = null;
      this.messageVerifier = getInitialVerifier(secureChatEnforced);
   }

   private static MessageVerifier getInitialVerifier(boolean secureChatEnforced) {
      return secureChatEnforced ? MessageVerifier.UNVERIFIED : MessageVerifier.NO_SIGNATURE;
   }

   public GameMode getGameMode() {
      return this.gameMode;
   }

   protected void setGameMode(GameMode gameMode) {
      this.gameMode = gameMode;
   }

   public int getLatency() {
      return this.latency;
   }

   protected void setLatency(int latency) {
      this.latency = latency;
   }

   public SkinTextures getSkinTextures() {
      return this.texturesSupplier.get();
   }

   @Nullable
   public Team getScoreboardTeam() {
      return MinecraftClient.getInstance().world.getScoreboard().getScoreHolderTeam(this.getProfile().getName());
   }

   public void setDisplayName(@Nullable Text displayName) {
      this.displayName = displayName;
   }

   @Nullable
   public Text getDisplayName() {
      return this.displayName;
   }

   public void setShowHat(boolean showHat) {
      this.showHat = showHat;
   }

   public boolean shouldShowHat() {
      return this.showHat;
   }

   public void setListOrder(int listOrder) {
      this.listOrder = listOrder;
   }

   public int getListOrder() {
      return this.listOrder;
   }

   @Override
   public int viaFabricPlusVisuals$getIndex() {
      return this.viaFabricPlusVisuals$index;
   }
}
