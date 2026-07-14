package com.viaversion.viafabricplus.screen.impl;

import com.viaversion.viafabricplus.screen.VFPList;
import com.viaversion.viafabricplus.screen.VFPListEntry;
import com.viaversion.viafabricplus.screen.VFPScreen;
import com.viaversion.vialoader.util.ProtocolVersionList;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import java.awt.Color;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class PerServerVersionScreen extends VFPScreen {
   private final Consumer<ProtocolVersion> selectionConsumer;
   private final Supplier<ProtocolVersion> selectionSupplier;

   public PerServerVersionScreen(Screen prevScreen, Consumer<ProtocolVersion> selectionConsumer, Supplier<ProtocolVersion> selectionSupplier) {
      super(Text.translatable("screen.viafabricplus.force_version"), false);
      this.prevScreen = prevScreen;
      this.selectionConsumer = selectionConsumer;
      this.selectionSupplier = selectionSupplier;
      this.setupSubtitle(Text.translatable("force_version.viafabricplus.title"));
   }

   @Override
   protected void init() {
      super.init();
      this.addDrawableChild(new PerServerVersionScreen.SlotList(this.client, this.width, this.height, 6 + (9 + 2) * 3, -5, 9 + 4));
   }

   public final class ProtocolSlot extends PerServerVersionScreen.SharedSlot {
      private final ProtocolVersion protocolVersion;

      public ProtocolSlot(final ProtocolVersion protocolVersion) {
         this.protocolVersion = protocolVersion;
      }

      public Text getNarration() {
         return Text.of(this.protocolVersion.getName());
      }

      @Override
      public boolean mouseClicked(double mouseX, double mouseY, int button) {
         PerServerVersionScreen.this.selectionConsumer.accept(this.protocolVersion);
         return super.mouseClicked(mouseX, mouseY, button);
      }

      @Override
      public void render(
         DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta
      ) {
         boolean isSelected = this.protocolVersion.equals(PerServerVersionScreen.this.selectionSupplier.get());
         TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
         context.drawCenteredTextWithShadow(
            textRenderer, this.protocolVersion.getName(), x + entryWidth / 2, y - 1 + entryHeight / 2 - 9 / 2, isSelected ? Color.GREEN.getRGB() : -1
         );
      }
   }

   public final class ResetSlot extends PerServerVersionScreen.SharedSlot {
      public Text getNarration() {
         return Text.translatable("base.viafabricplus.cancel_and_reset");
      }

      @Override
      public boolean mouseClicked(double mouseX, double mouseY, int button) {
         PerServerVersionScreen.this.selectionConsumer.accept(null);
         return super.mouseClicked(mouseX, mouseY, button);
      }

      @Override
      public void render(
         DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta
      ) {
         TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
         context.drawCenteredTextWithShadow(
            textRenderer, ((MutableText)this.getNarration()).formatted(Formatting.GOLD), x + entryWidth / 2, y + entryHeight / 2 - 9 / 2, -1
         );
      }
   }

   public abstract class SharedSlot extends VFPListEntry {
      @Override
      public void mappedMouseClicked(double mouseX, double mouseY, int button) {
         PerServerVersionScreen.this.close();
      }
   }

   public final class SlotList extends VFPList {
      public SlotList(MinecraftClient minecraftClient, int width, int height, int top, int bottom, int entryHeight) {
         super(minecraftClient, width, height, top, bottom, entryHeight);
         this.addEntry(PerServerVersionScreen.this.new ResetSlot());
         ProtocolVersionList.getProtocolsNewToOld().stream().map(x$0 -> PerServerVersionScreen.this.new ProtocolSlot(x$0)).forEach(x$0 -> this.addEntry(x$0));
      }
   }
}
