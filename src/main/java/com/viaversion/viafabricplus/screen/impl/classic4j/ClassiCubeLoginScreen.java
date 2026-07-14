package com.viaversion.viafabricplus.screen.impl.classic4j;

import com.viaversion.viafabricplus.ViaFabricPlusImpl;
import com.viaversion.viafabricplus.injection.access.base.ITextFieldWidget;
import com.viaversion.viafabricplus.save.SaveManager;
import com.viaversion.viafabricplus.save.impl.AccountsSave;
import com.viaversion.viafabricplus.screen.VFPScreen;
import de.florianmichael.classic4j.ClassiCubeHandler;
import de.florianmichael.classic4j.api.LoginProcessHandler;
import de.florianmichael.classic4j.model.classicube.account.CCAccount;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ConfirmLinkScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public final class ClassiCubeLoginScreen extends VFPScreen {
   public static final ClassiCubeLoginScreen INSTANCE = new ClassiCubeLoginScreen();
   private TextFieldWidget nameField;
   private TextFieldWidget passwordField;

   public ClassiCubeLoginScreen() {
      super(Text.translatable("screen.viafabricplus.classicube_login"), true);
   }

   @Override
   protected void init() {
      super.init();
      this.setupSubtitle(
         Text.translatable("classicube.viafabricplus.account"), ConfirmLinkScreen.opening(this, ClassiCubeHandler.CLASSICUBE_ROOT_URI.toString())
      );
      this.addDrawableChild(this.nameField = new TextFieldWidget(this.textRenderer, this.width / 2 - 150, 80, 300, 20, Text.empty()));
      this.addDrawableChild(
         this.passwordField = new TextFieldWidget(this.textRenderer, this.width / 2 - 150, this.nameField.getY() + 20 + 5, 300, 20, Text.empty())
      );
      this.passwordField.setRenderTextProvider((s, integer) -> Text.of("*".repeat(s.length())).asOrderedText());
      this.nameField.setPlaceholder(Text.translatable("base.viafabricplus.name"));
      this.passwordField.setPlaceholder(Text.translatable("base.viafabricplus.password"));
      this.nameField.setMaxLength(Integer.MAX_VALUE);
      this.passwordField.setMaxLength(Integer.MAX_VALUE);
      ((ITextFieldWidget)this.nameField).viaFabricPlus$unlockForbiddenCharacters();
      ((ITextFieldWidget)this.passwordField).viaFabricPlus$unlockForbiddenCharacters();
      AccountsSave accountsSave = SaveManager.INSTANCE.getAccountsSave();
      if (accountsSave.getClassicubeAccount() != null) {
         this.nameField.setText(accountsSave.getClassicubeAccount().username());
         this.passwordField.setText(accountsSave.getClassicubeAccount().username());
      }

      this.addDrawableChild(ButtonWidget.builder(Text.translatable("base.viafabricplus.login"), button -> {
         accountsSave.setClassicubeAccount(new CCAccount(this.nameField.getText(), this.passwordField.getText()));
         this.setupSubtitle(Text.translatable("classicube.viafabricplus.loading"));
         ClassiCubeHandler.requestAuthentication(accountsSave.getClassicubeAccount(), null, new LoginProcessHandler() {
            public void handleMfa(CCAccount account) {
               ClassiCubeMFAScreen.INSTANCE.open(ClassiCubeLoginScreen.this.prevScreen);
            }

            public void handleSuccessfulLogin(CCAccount account) {
               ClassiCubeServerListScreen.open(ClassiCubeLoginScreen.this.prevScreen, this);
            }

            public void handleException(Throwable throwable) {
               ViaFabricPlusImpl.INSTANCE.logger().error("Error while logging in to ClassiCube!", throwable);
               ClassiCubeLoginScreen.this.setupSubtitle(Text.of(throwable.getMessage()));
            }
         });
      }).position(this.width / 2 - 75, this.passwordField.getY() + 80 + 5).size(150, 20).build());
   }

   @Override
   public void close() {
      SaveManager.INSTANCE.getAccountsSave().setClassicubeAccount(null);
      super.close();
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      super.render(context, mouseX, mouseY, delta);
      this.renderScreenTitle(context);
   }
}
