package com.viaversion.viafabricplus.settings.impl;

import com.viaversion.viafabricplus.api.settings.SettingGroup;
import com.viaversion.viafabricplus.api.settings.type.BooleanSetting;
import com.viaversion.viafabricplus.api.settings.type.ButtonSetting;
import com.viaversion.viafabricplus.injection.access.base.bedrock.IConfirmScreen;
import com.viaversion.viafabricplus.save.SaveManager;
import com.viaversion.viafabricplus.save.impl.AccountsSave;
import com.viaversion.viafabricplus.screen.VFPScreen;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import java.util.Locale;
import java.util.Objects;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.step.AbstractStep;
import net.raphimc.minecraftauth.step.bedrock.session.StepFullBedrockSession.FullBedrockSession;
import net.raphimc.minecraftauth.step.msa.StepMsaDeviceCodeMsaCode;
import net.raphimc.minecraftauth.step.msa.StepMsaDeviceCode.MsaDeviceCodeCallback;
import net.raphimc.minecraftauth.util.logging.ILogger;
import net.raphimc.minecraftauth.util.logging.Slf4jConsoleLogger;
import net.raphimc.viabedrock.api.BedrockProtocolVersion;

public final class BedrockSettings extends SettingGroup {
   private static final Text TITLE = Text.of("Microsoft Bedrock login");
   public static final BedrockSettings INSTANCE = new BedrockSettings();
   public static final AbstractStep<?, FullBedrockSession> BEDROCK_DEVICE_CODE_LOGIN = MinecraftAuth.builder()
      .withClientId("0000000048183522")
      .withScope("service::user.auth.xboxlive.com::MBI_SSL")
      .deviceCode()
      .withDeviceToken("Android")
      .sisuTitleAuthentication("https://multiplayer.minecraft.net/")
      .buildMinecraftBedrockChainStep(true, true);
   private Thread thread;
   private final ButtonSetting clickToSetBedrockAccount = new ButtonSetting(
      this, Text.translatable("bedrock_settings.viafabricplus.click_to_set_bedrock_account"), () -> {
         this.thread = new Thread(this::openBedrockAccountLogin);
         this.thread.start();
      }
   ) {
      public MutableText displayValue() {
         FullBedrockSession account = SaveManager.INSTANCE.getAccountsSave().getBedrockAccount();
         return account != null
            ? Text.translatable("click_to_set_bedrock_account.viafabricplus.display", new Object[]{account.getMcChain().getDisplayName()})
            : super.displayValue();
      }
   };
   public final BooleanSetting replaceDefaultPort = new BooleanSetting(this, Text.translatable("bedrock_settings.viafabricplus.replace_default_port"), true);
   private final ILogger GUI_LOGGER = new Slf4jConsoleLogger() {
      public void info(AbstractStep<?, ?> step, String message) {
         super.info(step, message);
         if (!(step instanceof StepMsaDeviceCodeMsaCode)) {
            MinecraftClient.getInstance()
               .execute(
                  () -> {
                     if (MinecraftClient.getInstance().currentScreen instanceof ConfirmScreen confirmScreen) {
                        ((IConfirmScreen)confirmScreen)
                           .viaFabricPlus$setMessage(Text.translatable("minecraftauth_library.viafabricplus." + step.name.toLowerCase(Locale.ROOT)));
                     }
                  }
               );
         }
      }
   };

   public BedrockSettings() {
      super(Text.translatable("setting_group_name.viafabricplus.bedrock"));
   }

   private void openBedrockAccountLogin() {
      AccountsSave accountsSave = SaveManager.INSTANCE.getAccountsSave();
      MinecraftClient client = MinecraftClient.getInstance();
      Screen prevScreen = client.currentScreen;

      try {
         accountsSave.setBedrockAccount(
            (FullBedrockSession)BEDROCK_DEVICE_CODE_LOGIN.getFromInput(
               this.GUI_LOGGER,
               MinecraftAuth.createHttpClient(),
               new MsaDeviceCodeCallback(
                  msaDeviceCode -> {
                     VFPScreen.setScreen(
                        new ConfirmScreen(
                           copyUrl -> {
                              if (copyUrl) {
                                 client.keyboard.setClipboard(msaDeviceCode.getDirectVerificationUri());
                              } else {
                                 client.setScreen(prevScreen);
                                 this.thread.interrupt();
                              }
                           },
                           TITLE,
                           Text.translatable("click_to_set_bedrock_account.viafabricplus.notice"),
                           Text.translatable("base.viafabricplus.copy_link"),
                           Text.translatable("base.viafabricplus.cancel")
                        )
                     );
                     Util.getOperatingSystem().open(msaDeviceCode.getDirectVerificationUri());
                  }
               )
            )
         );
         VFPScreen.setScreen(prevScreen);
      } catch (Exception e) {
         if (e instanceof InterruptedException) {
            return;
         }

         this.thread.interrupt();
         VFPScreen.showErrorScreen(TITLE, e, prevScreen);
      }
   }

   public static String replaceDefaultPort(String address, ProtocolVersion version) {
      return INSTANCE.replaceDefaultPort.getValue() && Objects.equals(version, BedrockProtocolVersion.bedrockLatest) && !address.contains(":")
         ? address + ":19132"
         : address;
   }
}
