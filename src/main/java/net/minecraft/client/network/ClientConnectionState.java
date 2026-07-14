package net.minecraft.client.network;

import com.mojang.authlib.GameProfile;
import java.util.Map;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.session.telemetry.WorldSession;
import net.minecraft.registry.DynamicRegistryManager.Immutable;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.server.ServerLinks;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

public record ClientConnectionState(
   GameProfile localGameProfile,
   WorldSession worldSession,
   Immutable receivedRegistries,
   FeatureSet enabledFeatures,
   @Nullable String serverBrand,
   @Nullable ServerInfo serverInfo,
   @Nullable Screen postDisconnectScreen,
   Map<Identifier, byte[]> serverCookies,
   @Nullable ChatHud.ChatState chatState,
   Map<String, String> customReportDetails,
   ServerLinks serverLinks
) {
}
