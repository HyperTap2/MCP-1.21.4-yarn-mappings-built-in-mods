package com.viaversion.viafabricplus.protocoltranslator.impl.command.classic;

import com.viaversion.viafabricplus.injection.access.base.IExtensionProtocolMetadataStorage;
import com.viaversion.viafabricplus.protocoltranslator.impl.command.VFPViaSubCommand;
import com.viaversion.viaversion.api.command.ViaCommandSender;
import net.minecraft.util.Formatting;
import net.raphimc.vialegacy.api.LegacyProtocolVersion;
import net.raphimc.vialegacy.protocol.classic.c0_30cpetoc0_28_30.storage.ExtensionProtocolMetadataStorage;

public final class ListExtensionsCommand implements VFPViaSubCommand {
   public String name() {
      return "listextensions";
   }

   public String description() {
      return "Shows all classic extensions (only for " + LegacyProtocolVersion.c0_30cpe.getName() + ")";
   }

   public boolean execute(ViaCommandSender sender, String[] args) {
      if (this.getUser() != null && this.getUser().has(ExtensionProtocolMetadataStorage.class)) {
         ((IExtensionProtocolMetadataStorage)this.getUser().get(ExtensionProtocolMetadataStorage.class))
            .viaFabricPlus$getServerExtensions()
            .forEach((extension, version) -> this.sendMessage(sender, Formatting.GREEN + extension.getName() + Formatting.GOLD + " v" + version));
         return true;
      } else {
         this.sendMessage(sender, Formatting.RED + "Only for " + LegacyProtocolVersion.c0_30cpe.getName());
         return true;
      }
   }
}
