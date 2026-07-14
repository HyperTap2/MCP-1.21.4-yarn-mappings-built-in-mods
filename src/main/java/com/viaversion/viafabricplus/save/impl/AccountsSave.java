package com.viaversion.viafabricplus.save.impl;

import com.google.gson.JsonObject;
import com.viaversion.viafabricplus.ViaFabricPlusImpl;
import com.viaversion.viafabricplus.save.AbstractSave;
import com.viaversion.viafabricplus.settings.impl.BedrockSettings;
import de.florianmichael.classic4j.model.classicube.account.CCAccount;
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.step.bedrock.session.StepFullBedrockSession.FullBedrockSession;
import net.raphimc.minecraftauth.step.msa.StepMsaToken.RefreshToken;
import net.raphimc.minecraftauth.step.xbl.session.StepInitialXblSession.InitialXblSession;

public final class AccountsSave extends AbstractSave {
   private FullBedrockSession bedrockAccount;
   private CCAccount classicubeAccount;

   public AccountsSave() {
      super("accounts");
   }

   @Override
   public void write(JsonObject object) {
      if (this.bedrockAccount != null) {
         object.add("bedrockV2", BedrockSettings.BEDROCK_DEVICE_CODE_LOGIN.toJson(this.bedrockAccount));
      }

      if (this.classicubeAccount != null) {
         object.add("classicube", this.classicubeAccount.asJson());
      }
   }

   @Override
   public void read(JsonObject object) {
      this.handleAccount("bedrock", object, account -> {
         FullBedrockSession oldSession = (FullBedrockSession)MinecraftAuth.BEDROCK_DEVICE_CODE_LOGIN.fromJson(account);
         InitialXblSession xblSession = oldSession.getMcChain().getXblXsts().getInitialXblSession();
         RefreshToken refreshToken = new RefreshToken(xblSession.getMsaToken().getRefreshToken());
         this.bedrockAccount = (FullBedrockSession)BedrockSettings.BEDROCK_DEVICE_CODE_LOGIN.getFromInput(MinecraftAuth.createHttpClient(), refreshToken);
      });
      this.handleAccount("bedrockV2", object, account -> this.bedrockAccount = (FullBedrockSession)BedrockSettings.BEDROCK_DEVICE_CODE_LOGIN.fromJson(account));
      this.handleAccount("classicube", object, account -> this.classicubeAccount = CCAccount.fromJson(account));
   }

   private void handleAccount(String name, JsonObject object, AccountsSave.AccountConsumer output) {
      if (object.has(name)) {
         try {
            output.accept(object.get(name).getAsJsonObject());
         } catch (Exception e) {
            ViaFabricPlusImpl.INSTANCE.logger().error("Failed to read {} account!", name, e);
         }
      }
   }

   public FullBedrockSession refreshAndGetBedrockAccount() {
      if (this.bedrockAccount == null) {
         return null;
      }

      try {
         this.bedrockAccount = (FullBedrockSession)BedrockSettings.BEDROCK_DEVICE_CODE_LOGIN.refresh(MinecraftAuth.createHttpClient(), this.bedrockAccount);
      } catch (Throwable t) {
         throw new RuntimeException("Failed to refresh Bedrock chain data. Please re-login to Bedrock!", t);
      }

      return this.bedrockAccount;
   }

   public FullBedrockSession getBedrockAccount() {
      return this.bedrockAccount;
   }

   public void setBedrockAccount(FullBedrockSession bedrockAccount) {
      this.bedrockAccount = bedrockAccount;
   }

   public CCAccount getClassicubeAccount() {
      return this.classicubeAccount;
   }

   public void setClassicubeAccount(CCAccount classicubeAccount) {
      this.classicubeAccount = classicubeAccount;
   }

   @FunctionalInterface
   interface AccountConsumer {
      void accept(JsonObject var1) throws Exception;
   }
}
