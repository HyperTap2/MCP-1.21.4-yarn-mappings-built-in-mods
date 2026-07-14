package net.minecraft.client.gui.screen;

import com.viaversion.viafabricplus.injection.access.networking.downloading_terrain.IDownloadingTerrainScreen;
import com.viaversion.viafabricplus.protocoltranslator.ProtocolTranslator;
import com.viaversion.viafabricplus.settings.impl.GeneralSettings;
import com.viaversion.viafabricplus.util.ChatUtil;
import com.viaversion.viafabricplus.visuals.settings.VisualSettings;
import com.viaversion.viaversion.api.connection.UserConnection;
import java.util.function.BooleanSupplier;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.NarratorManager;
import net.minecraft.network.packet.c2s.common.KeepAliveC2SPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;
import net.raphimc.vialegacy.protocol.classic.c0_28_30toa1_0_15.storage.ClassicProgressStorage;

public class DownloadingTerrainScreen extends Screen implements IDownloadingTerrainScreen {
   private static final Text TEXT = Text.translatable("multiplayer.downloadingTerrain");
   private static final long MIN_LOAD_TIME_MS = 30000L;
   private final long loadStartTime;
   private final BooleanSupplier shouldClose;
   private final DownloadingTerrainScreen.WorldEntryReason worldEntryReason;
   private int viaFabricPlus$tickCounter;
   private boolean viaFabricPlus$ready;
   private boolean viaFabricPlus$closeOnNextTick;
   @Nullable
   private Sprite backgroundSprite;

   public DownloadingTerrainScreen(BooleanSupplier shouldClose, DownloadingTerrainScreen.WorldEntryReason worldEntryReason) {
      super(NarratorManager.EMPTY);
      this.shouldClose = shouldClose;
      this.worldEntryReason = worldEntryReason;
      this.loadStartTime = Util.getMeasuringTimeMs();
   }

   @Override
   public boolean shouldCloseOnEsc() {
      return false;
   }

   @Override
   protected boolean hasUsageText() {
      return false;
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      super.render(context, mouseX, mouseY, delta);
      context.drawCenteredTextWithShadow(this.textRenderer, TEXT, this.width / 2, this.height / 2 - 50, -1);
      if ((Boolean)GeneralSettings.INSTANCE.showClassicLoadingProgressInConnectScreen.getValue()) {
         UserConnection connection = ProtocolTranslator.getPlayNetworkUserConnection();
         if (connection != null) {
            ClassicProgressStorage progress = connection.get(ClassicProgressStorage.class);
            if (progress != null) {
               context.drawCenteredTextWithShadow(
                  this.textRenderer, ChatUtil.prefixText(progress.status), this.width / 2, this.height / 2 - 30, -1
               );
            }
         }
      }
   }

   @Override
   public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
      DownloadingTerrainScreen.WorldEntryReason worldEntryReason = VisualSettings.INSTANCE.hideDownloadTerrainScreenTransitionEffects.isEnabled()
         ? DownloadingTerrainScreen.WorldEntryReason.OTHER
         : this.worldEntryReason;
      switch (worldEntryReason) {
         case NETHER_PORTAL:
            context.drawSpriteStretched(
               RenderLayer::getGuiOpaqueTexturedBackground, this.getBackgroundSprite(), 0, 0, context.getScaledWindowWidth(), context.getScaledWindowHeight()
            );
            break;
         case END_PORTAL:
            context.fillWithLayer(RenderLayer.getEndPortal(), 0, 0, this.width, this.height, 0);
            break;
         case OTHER:
            this.renderPanoramaBackground(context, delta);
            this.applyBlur();
            this.renderDarkening(context);
      }
   }

   private Sprite getBackgroundSprite() {
      if (this.backgroundSprite != null) {
         return this.backgroundSprite;
      }

      this.backgroundSprite = this.client.getBlockRenderManager().getModels().getModelParticleSprite(Blocks.NETHER_PORTAL.getDefaultState());
      return this.backgroundSprite;
   }

   @Override
   public void tick() {
      if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(com.viaversion.viaversion.api.protocol.version.ProtocolVersion.v1_20_2)) {
         if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(com.viaversion.viaversion.api.protocol.version.ProtocolVersion.v1_18)) {
            if (this.viaFabricPlus$ready) {
               this.close();
            }
            if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(com.viaversion.viaversion.api.protocol.version.ProtocolVersion.v1_12_1)) {
               this.viaFabricPlus$tickCounter++;
               if (this.viaFabricPlus$tickCounter % 20 == 0 && this.client.getNetworkHandler() != null) {
                  this.client.getNetworkHandler().sendPacket(new KeepAliveC2SPacket(0L));
               }
            }
         } else if (System.currentTimeMillis() > this.loadStartTime + 30000L) {
            this.close();
         } else if (this.viaFabricPlus$closeOnNextTick) {
            if (this.client.player == null) {
               return;
            }
            BlockPos blockPos = this.client.player.getBlockPos();
            boolean outsideWorld = this.client.world != null && this.client.world.isOutOfHeightLimit(blockPos.getY());
            if (outsideWorld
               || this.client.worldRenderer.isRenderingReady(blockPos)
               || this.client.player.isSpectator()
               || !this.client.player.isAlive()) {
               this.close();
            }
         } else if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(com.viaversion.viaversion.api.protocol.version.ProtocolVersion.v1_19_1)) {
            this.viaFabricPlus$closeOnNextTick = this.viaFabricPlus$ready || System.currentTimeMillis() > this.loadStartTime + 2000L;
         } else {
            this.viaFabricPlus$closeOnNextTick = this.viaFabricPlus$ready;
         }
         return;
      }
      if (this.shouldClose.getAsBoolean() || Util.getMeasuringTimeMs() > this.loadStartTime + 30000L) {
         this.close();
      }
   }

   @Override
   public void viaFabricPlus$setReady() {
      this.viaFabricPlus$ready = true;
   }

   @Override
   public void close() {
      this.client.getNarratorManager().narrate(Text.translatable("narrator.ready_to_play"));
      super.close();
   }

   @Override
   public boolean shouldPause() {
      return false;
   }

   public enum WorldEntryReason {
      NETHER_PORTAL,
      END_PORTAL,
      OTHER;
   }
}
