package com.viaversion.viafabricplus.screen.impl.realms;

import com.viaversion.viafabricplus.ViaFabricPlusImpl;
import com.viaversion.viafabricplus.save.SaveManager;
import com.viaversion.viafabricplus.screen.VFPList;
import com.viaversion.viafabricplus.screen.VFPListEntry;
import com.viaversion.viafabricplus.screen.VFPScreen;
import com.viaversion.viafabricplus.util.ConnectionUtil;
import java.awt.Color;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.service.realms.BedrockRealmsService;
import net.raphimc.minecraftauth.service.realms.model.RealmsWorld;
import net.raphimc.minecraftauth.step.bedrock.session.StepFullBedrockSession.FullBedrockSession;
import net.raphimc.viabedrock.api.BedrockProtocolVersion;
import org.apache.logging.log4j.Level;

public final class BedrockRealmsScreen extends VFPScreen {
   public static final BedrockRealmsScreen INSTANCE = new BedrockRealmsScreen();
   private BedrockRealmsService service;
   private List<RealmsWorld> realmsWorlds;
   private BedrockRealmsScreen.SlotList slotList;
   private ButtonWidget joinButton;
   private ButtonWidget leaveButton;

   public BedrockRealmsScreen() {
      super(Text.translatable("screen.viafabricplus.bedrock_realms"), true);
   }

   @Override
   protected void init() {
      super.init();
      if (this.realmsWorlds != null) {
         this.createView();
      } else {
         this.setupSubtitle(Text.translatable("bedrock_realms.viafabricplus.availability_check"));
         CompletableFuture.runAsync(this::loadRealms);
      }
   }

   private void loadRealms() {
      FullBedrockSession account = SaveManager.INSTANCE.getAccountsSave().refreshAndGetBedrockAccount();
      if (account == null) {
         this.setupSubtitle(Text.translatable("bedrock_realms.viafabricplus.warning"));
      } else {
         this.service = new BedrockRealmsService(MinecraftAuth.createHttpClient(), "1.21.70", account.getRealmsXsts());
         this.service.isAvailable().thenAccept(state -> {
            if (state) {
               this.service.getWorlds().thenAccept(realmsWorlds -> {
                  this.realmsWorlds = (List<RealmsWorld>)realmsWorlds;
                  this.createView();
               }).exceptionally(throwable -> this.error("Failed to load realm worlds", throwable));
            } else {
               this.setupSubtitle(Text.translatable("bedrock_realms.viafabricplus.unavailable"));
            }
         }).exceptionally(throwable -> this.error("Failed to check realms availability", throwable));
      }
   }

   private Void error(String message, Throwable throwable) {
      this.setupSubtitle(Text.translatable("bedrock_realms.viafabricplus.error"));
      ViaFabricPlusImpl.INSTANCE.logger().log(Level.ERROR, message, throwable);
      return null;
   }

   private void createView() {
      if (!this.realmsWorlds.isEmpty()) {
         this.setupDefaultSubtitle();
      } else {
         this.setupSubtitle(Text.translatable("bedrock_realms.viafabricplus.no_worlds"));
      }

      this.addDrawableChild(this.slotList = new BedrockRealmsScreen.SlotList(this.client, this.width, this.height, 6 + (9 + 2) * 3, 30, (9 + 2) * 4));
      this.addRefreshButton(() -> this.realmsWorlds = null);
      int slotWidth = 356;
      int xPos = this.width / 2 - 178;
      this.addDrawableChild(
         this.joinButton = ButtonWidget.builder(
               Text.translatable("bedrock_realms.viafabricplus.join"),
               button -> {
                  BedrockRealmsScreen.SlotEntry entry = (BedrockRealmsScreen.SlotEntry)this.slotList.getFocused();
                  if (entry.realmsWorld.isExpired()) {
                     this.setupSubtitle(Text.translatable("bedrock_realms.viafabricplus.expired"));
                  } else if (!entry.realmsWorld.isCompatible()) {
                     this.setupSubtitle(Text.translatable("bedrock_realms.viafabricplus.incompatible"));
                  } else {
                     this.service
                        .joinWorld(entry.realmsWorld)
                        .thenAccept(address -> this.client.execute(() -> ConnectionUtil.connect(address, BedrockProtocolVersion.bedrockLatest)))
                        .exceptionally(throwable -> this.error("Failed to join realm", throwable));
                  }
               }
            )
            .position(xPos, this.height - 20 - 5)
            .size(115, 20)
            .build()
      );
      this.joinButton.active = false;
      xPos += 120;
      this.addDrawableChild(this.leaveButton = ButtonWidget.builder(Text.translatable("bedrock_realms.viafabricplus.leave"), button -> {
         BedrockRealmsScreen.SlotEntry entry = (BedrockRealmsScreen.SlotEntry)this.slotList.getFocused();
         this.service.leaveInvitedRealm(entry.realmsWorld).thenAccept(unused -> {
            this.realmsWorlds.remove(entry.realmsWorld);
            INSTANCE.open(this.prevScreen);
         }).exceptionally(throwable -> this.error("Failed to leave realm", throwable));
      }).position(xPos, this.height - 20 - 5).size(115, 20).build());
      this.leaveButton.active = false;
      xPos += 120;
      this.addDrawableChild(ButtonWidget.builder(Text.translatable("bedrock_realms.viafabricplus.invite"), button -> {
         AcceptInvitationCodeScreen screen = new AcceptInvitationCodeScreen(code -> this.service.acceptInvite(code).thenAccept(world -> {
            this.realmsWorlds.add(world);
            INSTANCE.open(this);
         }).exceptionally(throwable -> this.error("Failed to accept invite", throwable)));
         screen.open(this);
      }).position(xPos, this.height - 20 - 5).size(115, 20).build());
   }

   public void tick() {
      super.tick();
      if (this.slotList != null && this.joinButton != null && this.leaveButton != null) {
         this.joinButton.active = this.slotList.getFocused() instanceof BedrockRealmsScreen.SlotEntry;
         this.leaveButton.active = this.slotList.getFocused() instanceof BedrockRealmsScreen.SlotEntry;
      }
   }

   @Override
   protected boolean subtitleCentered() {
      return this.realmsWorlds == null;
   }

   public final class SlotEntry extends VFPListEntry {
      private final BedrockRealmsScreen.SlotList slotList;
      private final RealmsWorld realmsWorld;

      public SlotEntry(BedrockRealmsScreen.SlotList slotList, RealmsWorld realmsWorld) {
         this.slotList = slotList;
         this.realmsWorld = realmsWorld;
      }

      public Text getNarration() {
         return Text.of(this.realmsWorld.getName());
      }

      @Override
      public void mappedRender(
         DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta
      ) {
         TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
         String name = "";
         String ownerName = this.realmsWorld.getOwnerName();
         if (ownerName != null && !ownerName.trim().isEmpty()) {
            name = name + ownerName + " - ";
         }

         String worldName = this.realmsWorld.getName();
         if (worldName != null && !worldName.trim().isEmpty()) {
            name = name + worldName;
         }

         name = name + " (" + this.realmsWorld.getState() + ")";
         context.drawTextWithShadow(textRenderer, name, 3, 3, this.slotList.getFocused() == this ? Color.ORANGE.getRGB() : -1);
         String version = this.realmsWorld.getWorldType();
         String activeVersion = this.realmsWorld.getActiveVersion();
         if (activeVersion != null && !activeVersion.trim().isEmpty()) {
            version = version + " - " + activeVersion;
         }

         context.drawTextWithShadow(textRenderer, version, entryWidth - textRenderer.getWidth(version) - 4 - 3, 3, -1);
         String motd = this.realmsWorld.getMotd();
         if (motd != null) {
            this.renderScrollableText(Text.of(motd), entryHeight - 9 - 3, 6);
         }
      }
   }

   public final class SlotList extends VFPList {
      private static double scrollAmount;

      public SlotList(MinecraftClient minecraftClient, int width, int height, int top, int bottom, int entryHeight) {
         super(minecraftClient, width, height, top, bottom, entryHeight);

         for (RealmsWorld realmsWorld : BedrockRealmsScreen.this.realmsWorlds) {
            this.addEntry(BedrockRealmsScreen.this.new SlotEntry(this, realmsWorld));
         }

         this.initScrollY(scrollAmount);
      }

      @Override
      protected void updateSlotAmount(double amount) {
         scrollAmount = amount;
      }

      public int getRowWidth() {
         return super.getRowWidth() + 140;
      }
   }
}
