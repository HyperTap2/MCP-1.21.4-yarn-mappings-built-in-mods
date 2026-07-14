package com.viaversion.viafabricplus.protocoltranslator.impl.provider.vialegacy;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.HttpAuthenticationService;
import com.mojang.authlib.ProfileLookupCallback;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.yggdrasil.ProfileNotFoundException;
import com.mojang.authlib.yggdrasil.ProfileResult;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import java.net.Proxy;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import net.raphimc.vialegacy.protocol.release.r1_7_6_10tor1_8.provider.GameProfileFetcher;

public final class ViaFabricPlusGameProfileFetcher extends GameProfileFetcher {
   private static final HttpAuthenticationService AUTHENTICATION_SERVICE = new YggdrasilAuthenticationService(Proxy.NO_PROXY);
   private static final MinecraftSessionService SESSION_SERVICE = AUTHENTICATION_SERVICE.createMinecraftSessionService();
   private static final GameProfileRepository GAME_PROFILE_REPOSITORY = AUTHENTICATION_SERVICE.createProfileRepository();

   public UUID loadMojangUUID(String playerName) throws Exception {
      final CompletableFuture<GameProfile> future = new CompletableFuture<>();
      GAME_PROFILE_REPOSITORY.findProfilesByNames(new String[]{playerName}, new ProfileLookupCallback() {
         public void onProfileLookupSucceeded(GameProfile profile) {
            future.complete(profile);
         }

         public void onProfileLookupFailed(String profileName, Exception exception) {
            future.completeExceptionally(exception);
         }
      });
      if (!future.isDone()) {
         future.completeExceptionally(new ProfileNotFoundException());
      }

      return future.get().getId();
   }

   public net.raphimc.vialegacy.protocol.release.r1_7_6_10tor1_8.model.GameProfile loadGameProfile(UUID uuid) {
      ProfileResult result = SESSION_SERVICE.fetchProfile(uuid, true);
      if (result == null) {
         throw new ProfileNotFoundException();
      }

      GameProfile authLibProfile = result.profile();
      net.raphimc.vialegacy.protocol.release.r1_7_6_10tor1_8.model.GameProfile mcProfile = new net.raphimc.vialegacy.protocol.release.r1_7_6_10tor1_8.model.GameProfile(
         authLibProfile.getName(), authLibProfile.getId()
      );

      for (Entry<String, Property> entry : authLibProfile.getProperties().entries()) {
         mcProfile.addProperty(
            new net.raphimc.vialegacy.protocol.release.r1_7_6_10tor1_8.model.GameProfile.Property(
               entry.getValue().name(), entry.getValue().value(), entry.getValue().signature()
            )
         );
      }

      return mcProfile;
   }
}
