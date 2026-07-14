package com.viaversion.viafabricplus.features;

import com.viaversion.viaaprilfools.api.AprilFoolsProtocolVersion;
import com.viaversion.viafabricplus.api.events.ChangeProtocolVersionCallback;
import com.viaversion.viafabricplus.api.events.LoadingCycleCallback;
import com.viaversion.viafabricplus.api.events.LoadingCycleCallback.LoadingCycle;
import com.viaversion.viafabricplus.base.Events;
import com.viaversion.viafabricplus.features.block.shape.CollisionShapes;
import com.viaversion.viafabricplus.features.classic.cpe_extension.CPEAdditions;
import com.viaversion.viafabricplus.features.emulation.armor_hud.ArmorHudEmulation1_8;
import com.viaversion.viafabricplus.features.emulation.recipe.Recipes1_11_2;
import com.viaversion.viafabricplus.features.entity.EntityDimensionDiff;
import com.viaversion.viafabricplus.features.entity.attribute.EnchantmentAttributesEmulation1_20_6;
import com.viaversion.viafabricplus.features.font.replace_blank_glyph.FontCacheReload;
import com.viaversion.viafabricplus.features.footstep_particle.FootStepParticle1_12_2;
import com.viaversion.viafabricplus.features.networking.resource_pack_header.ResourcePackHeaderDiff;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import net.minecraft.client.MinecraftClient;

public final class FeaturesLoading {
   public static void init() {
   }

   static {
      ResourcePackHeaderDiff.init();
      CPEAdditions.init();
      FootStepParticle1_12_2.init();
      Events.LOADING_CYCLE.register((LoadingCycleCallback)cycle -> {
         if (cycle == LoadingCycle.POST_GAME_LOAD) {
            EnchantmentAttributesEmulation1_20_6.init();
            EntityDimensionDiff.init();
            ArmorHudEmulation1_8.init();
         }
      });
      Events.CHANGE_PROTOCOL_VERSION.register((ChangeProtocolVersionCallback)(oldVersion, newVersion) -> MinecraftClient.getInstance().execute(() -> {
         CollisionShapes.reloadBlockShapes();
         FontCacheReload.reload();
         if (newVersion.olderThanOrEqualTo(ProtocolVersion.v1_11_1)) {
            Recipes1_11_2.reset();
         }

         if (oldVersion.equals(AprilFoolsProtocolVersion.s3d_shareware) || newVersion.equals(AprilFoolsProtocolVersion.s3d_shareware)) {
            MinecraftClient.getInstance().getSoundManager().reloadSounds();
         }
      }));
   }
}
