package com.viaversion.viafabricplus.screen.impl.classic4j;

import com.viaversion.viafabricplus.save.SaveManager;
import com.viaversion.viafabricplus.screen.VFPScreen;
import com.viaversion.viafabricplus.screen.impl.ProtocolSelectionScreen;
import de.florianmichael.classic4j.ClassiCubeHandler;
import de.florianmichael.classic4j.api.LoginProcessHandler;
import de.florianmichael.classic4j.model.classicube.account.CCAccount;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public final class ClassiCubeMFAScreen extends VFPScreen {
   public static final ClassiCubeMFAScreen INSTANCE = new ClassiCubeMFAScreen();
   private TextFieldWidget mfaField;

   public ClassiCubeMFAScreen() {
      super(Text.translatable("screen.viafabricplus.classicube_mfa"), false);
   }

   @Override
   protected void init() {
      super.init();
      this.setupSubtitle(Text.translatable("classic4j_library.viafabricplus.error.logincode"));
      this.addDrawableChild(this.mfaField = new TextFieldWidget(this.textRenderer, this.width / 2 - 150, 80, 300, 20, Text.empty()));
      this.mfaField.setPlaceholder(Text.of("MFA"));
      this.addDrawableChild(ButtonWidget.builder(Text.translatable("base.viafabricplus.login"), button -> {
         this.setupSubtitle(Text.translatable("classicube.viafabricplus.loading"));
         CCAccount account = SaveManager.INSTANCE.getAccountsSave().getClassicubeAccount();
         ClassiCubeHandler.requestAuthentication(account, this.mfaField.getText(), new LoginProcessHandler() {
            public void handleMfa(CCAccount account) {
            }

            public void handleSuccessfulLogin(CCAccount account) {
               ClassiCubeServerListScreen.open(ClassiCubeMFAScreen.this.prevScreen, this);
            }

            public void handleException(Throwable throwable) {
               ClassiCubeMFAScreen.this.setupSubtitle(Text.of(throwable.getMessage()));
            }
         });
      }).position(this.width / 2 - 75, this.mfaField.getY() + 80 + 5).size(150, 20).build());
   }

   @Override
   public void close() {
      SaveManager.INSTANCE.getAccountsSave().setClassicubeAccount(null);
      ProtocolSelectionScreen.INSTANCE.open(this.prevScreen);
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      super.render(context, mouseX, mouseY, delta);
      this.renderScreenTitle(context);
   }
}
