package com.viaversion.viafabricplus.screen.impl.classic4j;

import com.viaversion.viafabricplus.screen.VFPList;
import com.viaversion.viafabricplus.screen.VFPListEntry;
import com.viaversion.viafabricplus.screen.VFPScreen;
import com.viaversion.viafabricplus.screen.impl.settings.TitleRenderer;
import com.viaversion.viafabricplus.util.ConnectionUtil;
import de.florianmichael.classic4j.BetaCraftHandler;
import de.florianmichael.classic4j.model.betacraft.BCServerInfoSpec;
import de.florianmichael.classic4j.model.betacraft.BCServerList;
import de.florianmichael.classic4j.model.betacraft.BCVersionCategory;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ConfirmLinkScreen;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class BetaCraftScreen extends VFPScreen {
   public static final BetaCraftScreen INSTANCE = new BetaCraftScreen();
   public static BCServerList SERVER_LIST;
   private static final String BETA_CRAFT_SERVER_LIST_URL = "https://betacraft.uk/serverlist/";

   private BetaCraftScreen() {
      super("BetaCraft", true);
   }

   @Override
   protected void init() {
      super.init();
      if (SERVER_LIST != null) {
         this.createView();
      } else {
         this.setupSubtitle(Text.translatable("betacraft.viafabricplus.loading"));
         BetaCraftHandler.requestV2ServerList(serverList -> {
            SERVER_LIST = serverList;
            this.createView();
         }, throwable -> showErrorScreen(INSTANCE.getTitle(), throwable, this));
      }
   }

   private void createView() {
      this.setupSubtitle(Text.of("https://betacraft.uk/serverlist/"), ConfirmLinkScreen.opening(this, "https://betacraft.uk/serverlist/"));
      this.addDrawableChild(new BetaCraftScreen.SlotList(this.client, this.width, this.height, 6 + (9 + 2) * 3, -5, (9 + 2) * 3));
      this.addRefreshButton(() -> SERVER_LIST = null);
   }

   @Override
   protected boolean subtitleCentered() {
      return SERVER_LIST == null;
   }

   public static class ServerSlot extends VFPListEntry {
      private final BCServerInfoSpec server;

      public ServerSlot(BCServerInfoSpec server) {
         this.server = server;
      }

      public Text getNarration() {
         return Text.of(this.server.name());
      }

      @Override
      public void mappedMouseClicked(double mouseX, double mouseY, int button) {
         ConnectionUtil.connect(this.server.name(), this.server.socket());
         super.mappedMouseClicked(mouseX, mouseY, button);
      }

      @Override
      public void mappedRender(
         DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta
      ) {
         TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
         context.drawCenteredTextWithShadow(
            textRenderer, this.server.name() + Formatting.DARK_GRAY + " [" + this.server.gameVersion() + "]", entryWidth / 2, entryHeight / 2 - 9 / 2, -1
         );
         if (this.server.onlineMode()) {
            context.drawTextWithShadow(textRenderer, Text.translatable("base.viafabricplus.online_mode").formatted(Formatting.GREEN), 1, 1, -1);
         }

         String playerText = this.server.playerCount() + "/" + this.server.playerLimit();
         context.drawTextWithShadow(textRenderer, playerText, entryWidth - textRenderer.getWidth(playerText) - 4 - 1, 1, -1);
      }
   }

   public static class SlotList extends VFPList {
      private static double scrollAmount;

      public SlotList(MinecraftClient minecraftClient, int width, int height, int top, int bottom, int entryHeight) {
         super(minecraftClient, width, height, top, bottom, entryHeight);
         if (BetaCraftScreen.SERVER_LIST != null) {
            for (BCVersionCategory value : BCVersionCategory.values()) {
               List<BCServerInfoSpec> servers = BetaCraftScreen.SERVER_LIST.serversOfVersionCategory(value);
               if (!servers.isEmpty()) {
                  this.addEntry(new TitleRenderer(Text.of(value.name())));

                  for (BCServerInfoSpec server : servers) {
                     this.addEntry(new BetaCraftScreen.ServerSlot(server));
                  }
               }
            }

            this.initScrollY(scrollAmount);
         }
      }

      public int getRowWidth() {
         return super.getRowWidth() + 140;
      }

      @Override
      protected void updateSlotAmount(double amount) {
         scrollAmount = amount;
      }
   }
}
