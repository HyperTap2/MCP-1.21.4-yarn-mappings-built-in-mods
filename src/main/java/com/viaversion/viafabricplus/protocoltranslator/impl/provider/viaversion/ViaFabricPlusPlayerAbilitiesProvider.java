package com.viaversion.viafabricplus.protocoltranslator.impl.provider.viaversion;

import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.protocols.v1_15_2to1_16.provider.PlayerAbilitiesProvider;
import net.minecraft.client.MinecraftClient;

public final class ViaFabricPlusPlayerAbilitiesProvider extends PlayerAbilitiesProvider {
   public float getFlyingSpeed(UserConnection connection) {
      return MinecraftClient.getInstance().player.getAbilities().getFlySpeed();
   }

   public float getWalkingSpeed(UserConnection connection) {
      return MinecraftClient.getInstance().player.getAbilities().getWalkSpeed();
   }
}
