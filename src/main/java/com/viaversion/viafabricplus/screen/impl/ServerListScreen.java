package com.viaversion.viafabricplus.screen.impl;

import com.viaversion.viafabricplus.save.SaveManager;
import com.viaversion.viafabricplus.screen.VFPScreen;
import com.viaversion.viafabricplus.screen.impl.classic4j.BetaCraftScreen;
import com.viaversion.viafabricplus.screen.impl.classic4j.ClassiCubeLoginScreen;
import com.viaversion.viafabricplus.screen.impl.classic4j.ClassiCubeServerListScreen;
import com.viaversion.viafabricplus.screen.impl.realms.BedrockRealmsScreen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ButtonWidget.Builder;
import net.minecraft.text.Text;

public final class ServerListScreen extends VFPScreen {
   public static final ServerListScreen INSTANCE = new ServerListScreen();

   public ServerListScreen() {
      super(Text.translatable("screen.viafabricplus.server_list"), true);
   }

   @Override
   protected void init() {
      super.init();
      this.setupDefaultSubtitle();
      boolean loggedIn = SaveManager.INSTANCE.getAccountsSave().getClassicubeAccount() != null;
      Builder classiCubeBuilder = ButtonWidget.builder(ClassiCubeServerListScreen.INSTANCE.getTitle(), button -> {
         if (!loggedIn) {
            ClassiCubeLoginScreen.INSTANCE.open(this);
         } else {
            ClassiCubeServerListScreen.INSTANCE.open(this);
         }
      }).position(this.width / 2 - 100, this.height / 2 - 25).size(200, 20);
      if (!loggedIn) {
         classiCubeBuilder.tooltip(Tooltip.of(Text.translatable("classicube.viafabricplus.warning")));
      }

      this.addDrawableChild(classiCubeBuilder.build());
      Builder betaCraftBuilder = ButtonWidget.builder(BetaCraftScreen.INSTANCE.getTitle(), button -> BetaCraftScreen.INSTANCE.open(this))
         .position(this.width / 2 - 100, this.height / 2 - 25 + 20 + 3)
         .size(200, 20);
      if (BetaCraftScreen.SERVER_LIST == null) {
         betaCraftBuilder.tooltip(Tooltip.of(Text.translatable("betacraft.viafabricplus.warning")));
      }

      this.addDrawableChild(betaCraftBuilder.build());
      Builder bedrockRealmsBuilder = ButtonWidget.builder(BedrockRealmsScreen.INSTANCE.getTitle(), button -> BedrockRealmsScreen.INSTANCE.open(this))
         .position(this.width / 2 - 100, this.height / 2 - 25 + 40 + 6)
         .size(200, 20);
      boolean missingAccount = SaveManager.INSTANCE.getAccountsSave().getBedrockAccount() == null;
      if (missingAccount) {
         bedrockRealmsBuilder.tooltip(Tooltip.of(Text.translatable("bedrock_realms.viafabricplus.warning")));
      }

      ButtonWidget bedrockRealmsButton = bedrockRealmsBuilder.build();
      this.addDrawableChild(bedrockRealmsButton);
      if (missingAccount) {
         bedrockRealmsButton.active = false;
      }
   }
}
