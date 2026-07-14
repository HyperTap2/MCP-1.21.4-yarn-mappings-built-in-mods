package com.viaversion.viafabricplus.screen.impl;

import com.viaversion.viafabricplus.protocoltranslator.ProtocolTranslator;
import com.viaversion.viafabricplus.screen.VFPList;
import com.viaversion.viafabricplus.screen.VFPListEntry;
import com.viaversion.viafabricplus.screen.VFPScreen;
import com.viaversion.viafabricplus.screen.impl.settings.SettingsScreen;
import com.viaversion.vialoader.util.ProtocolVersionList;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import java.awt.Color;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

public final class ProtocolSelectionScreen extends VFPScreen {
   public static final ProtocolSelectionScreen INSTANCE = new ProtocolSelectionScreen();

   private ProtocolSelectionScreen() {
      super("ViaFabricPlus", true);
   }

   @Override
   protected void init() {
      this.setupDefaultSubtitle();
      this.addDrawableChild(new ProtocolSelectionScreen.SlotList(this.client, this.width, this.height, 6 + (9 + 2) * 3, 30, 9 + 4));
      this.addDrawableChild(
         ButtonWidget.builder(Text.translatable("base.viafabricplus.settings"), button -> SettingsScreen.INSTANCE.open(this))
            .position(this.width - 98 - 5, 5)
            .size(98, 20)
            .build()
      );
      ButtonWidget serverList = (ButtonWidget)this.addDrawableChild(
         ButtonWidget.builder(ServerListScreen.INSTANCE.getTitle(), button -> ServerListScreen.INSTANCE.open(this))
            .position(5, this.height - 25)
            .size(98, 20)
            .build()
      );
      serverList.active = MinecraftClient.getInstance().getNetworkHandler() == null;
      this.addDrawableChild(
         ButtonWidget.builder(Text.translatable("report.viafabricplus.button"), button -> ReportIssuesScreen.INSTANCE.open(this))
            .position(this.width - 98 - 5, this.height - 25)
            .size(98, 20)
            .build()
      );
      super.init();
   }

   public static class ProtocolSlot extends VFPListEntry {
      private final ProtocolVersion protocolVersion;

      public ProtocolSlot(ProtocolVersion protocolVersion) {
         this.protocolVersion = protocolVersion;
      }

      public Text getNarration() {
         return Text.of(this.protocolVersion.getName());
      }

      @Override
      public void mappedMouseClicked(double mouseX, double mouseY, int button) {
         if (MinecraftClient.getInstance().getNetworkHandler() == null) {
            ProtocolTranslator.setTargetVersion(this.protocolVersion);
         }
      }

      @Override
      public void render(
         DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta
      ) {
         boolean isSelected = ProtocolTranslator.getTargetVersion().equals(this.protocolVersion);
         MatrixStack matrices = context.getMatrices();
         matrices.push();
         matrices.translate(x, y - 1, 0.0F);
         Color color = isSelected ? Color.GREEN : Color.RED;
         if (MinecraftClient.getInstance().getNetworkHandler() != null) {
            color = color.darker();
         }

         TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
         context.drawCenteredTextWithShadow(textRenderer, this.protocolVersion.getName(), entryWidth / 2, entryHeight / 2 - 9 / 2, color.getRGB());
         matrices.pop();
      }
   }

   public static class SlotList extends VFPList {
      private static double scrollAmount;

      public SlotList(MinecraftClient minecraftClient, int width, int height, int top, int bottom, int entryHeight) {
         super(minecraftClient, width, height, top, bottom, entryHeight);
         ProtocolVersionList.getProtocolsNewToOld().stream().map(ProtocolSelectionScreen.ProtocolSlot::new).forEach(x$0 -> this.addEntry(x$0));
         this.initScrollY(scrollAmount);
      }

      @Override
      protected void updateSlotAmount(double amount) {
         scrollAmount = amount;
      }
   }
}
