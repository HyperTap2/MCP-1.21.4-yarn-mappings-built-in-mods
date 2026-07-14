package com.viaversion.viafabricplus.screen.impl.classic4j;

import com.viaversion.viafabricplus.protocoltranslator.impl.provider.vialegacy.ViaFabricPlusClassicMPPassProvider;
import com.viaversion.viafabricplus.save.SaveManager;
import com.viaversion.viafabricplus.screen.VFPList;
import com.viaversion.viafabricplus.screen.VFPListEntry;
import com.viaversion.viafabricplus.screen.VFPScreen;
import com.viaversion.viafabricplus.settings.impl.AuthenticationSettings;
import com.viaversion.viafabricplus.util.ConnectionUtil;
import de.florianmichael.classic4j.ClassiCubeHandler;
import de.florianmichael.classic4j.api.LoginProcessHandler;
import de.florianmichael.classic4j.model.classicube.account.CCAccount;
import de.florianmichael.classic4j.model.classicube.server.CCServerInfo;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.raphimc.vialegacy.api.LegacyProtocolVersion;

public final class ClassiCubeServerListScreen extends VFPScreen {
   public static final ClassiCubeServerListScreen INSTANCE = new ClassiCubeServerListScreen();
   public static final List<CCServerInfo> SERVER_LIST = new ArrayList<>();
   private static final String CLASSICUBE_SERVER_LIST_URL = "https://www.classicube.net/server/list/";

   public static void open(Screen prevScreen, LoginProcessHandler loginProcessHandler) {
      CCAccount account = SaveManager.INSTANCE.getAccountsSave().getClassicubeAccount();
      ClassiCubeHandler.requestServerList(account, serverList -> {
         SERVER_LIST.addAll(serverList.servers());
         INSTANCE.open(prevScreen);
      }, loginProcessHandler::handleException);
   }

   public ClassiCubeServerListScreen() {
      super("ClassiCube", true);
   }

   @Override
   protected void init() {
      CCAccount account = SaveManager.INSTANCE.getAccountsSave().getClassicubeAccount();
      if (account != null) {
         this.setupUrlSubtitle("https://www.classicube.net/server/list/");
      }

      this.addDrawableChild(new ClassiCubeServerListScreen.SlotList(this.client, this.width, this.height, 6 + (9 + 2) * 3, -5, (9 + 4) * 3));
      this.addDrawableChild(ButtonWidget.builder(Text.translatable("base.viafabricplus.logout"), button -> {
         SaveManager.INSTANCE.getAccountsSave().setClassicubeAccount(null);
         SERVER_LIST.clear();
         this.close();
      }).position(this.width - 60 - 5, 5).size(60, 20).build());
      super.init();
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      super.render(context, mouseX, mouseY, delta);
      CCAccount account = SaveManager.INSTANCE.getAccountsSave().getClassicubeAccount();
      if (account != null) {
         context.drawTextWithShadow(this.textRenderer, Text.translatable("classicube.viafabricplus.profile"), 32, 6, -1);
         context.drawTextWithShadow(this.textRenderer, Text.of(account.username()), 32, 16, -1);
      }
   }

   public static class ServerSlot extends VFPListEntry {
      private final CCServerInfo classiCubeServerInfo;

      public ServerSlot(CCServerInfo classiCubeServerInfo) {
         this.classiCubeServerInfo = classiCubeServerInfo;
      }

      public Text getNarration() {
         return Text.of(this.classiCubeServerInfo.name());
      }

      @Override
      public void mappedMouseClicked(double mouseX, double mouseY, int button) {
         boolean selectCPE = (Boolean)AuthenticationSettings.INSTANCE.automaticallySelectCPEInClassiCubeServerList.getValue();
         ViaFabricPlusClassicMPPassProvider.classicubeMPPass = this.classiCubeServerInfo.mpPass();
         ConnectionUtil.connect(
            this.classiCubeServerInfo.name(),
            this.classiCubeServerInfo.ip() + ":" + this.classiCubeServerInfo.port(),
            selectCPE ? LegacyProtocolVersion.c0_30cpe : null
         );
      }

      @Override
      public void mappedRender(
         DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta
      ) {
         TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
         context.drawCenteredTextWithShadow(textRenderer, this.classiCubeServerInfo.name(), entryWidth / 2, entryHeight / 2 - 9 / 2, -1);
         context.drawTextWithShadow(textRenderer, this.classiCubeServerInfo.software().replace('&', '§'), 1, 1, -1);
         String playerText = this.classiCubeServerInfo.players() + "/" + this.classiCubeServerInfo.maxPlayers();
         context.drawTextWithShadow(textRenderer, playerText, entryWidth - textRenderer.getWidth(playerText) - 4 - 1, 1, -1);
      }
   }

   public static class SlotList extends VFPList {
      private static double scrollAmount;

      public SlotList(MinecraftClient minecraftClient, int width, int height, int top, int bottom, int entryHeight) {
         super(minecraftClient, width, height, top, bottom, entryHeight);
         ClassiCubeServerListScreen.SERVER_LIST.forEach(serverInfo -> this.addEntry(new ClassiCubeServerListScreen.ServerSlot(serverInfo)));
         this.initScrollY(scrollAmount);
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
