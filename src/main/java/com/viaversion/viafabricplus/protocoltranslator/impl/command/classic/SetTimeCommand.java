package com.viaversion.viafabricplus.protocoltranslator.impl.command.classic;

import com.viaversion.viafabricplus.protocoltranslator.impl.command.VFPViaSubCommand;
import com.viaversion.viaversion.api.command.ViaCommandSender;
import net.minecraft.util.Formatting;
import net.raphimc.vialegacy.api.LegacyProtocolVersion;
import net.raphimc.vialegacy.protocol.alpha.a1_0_16_2toa1_0_17_1_0_17_4.storage.TimeLockStorage;

public final class SetTimeCommand implements VFPViaSubCommand {
   public String name() {
      return "settime";
   }

   public String description() {
      return "Changes the time (Only for <= " + LegacyProtocolVersion.a1_0_16toa1_0_16_2.getName() + ")";
   }

   public String usage() {
      return this.name() + " <Time (Long)>";
   }

   public boolean execute(ViaCommandSender sender, String[] args) {
      if (this.getUser() != null && this.getUser().has(TimeLockStorage.class)) {
         try {
            if (args.length == 1) {
               long time = Long.parseLong(args[0]) % 24000L;
               ((TimeLockStorage)this.getUser().get(TimeLockStorage.class)).setTime(time);
               this.sendMessage(sender, Formatting.GREEN + "Time has been set to " + Formatting.GOLD + time);
               return true;
            } else {
               return false;
            }
         } catch (Throwable ignored) {
            return false;
         }
      } else {
         this.sendMessage(sender, Formatting.RED + "Only for <= " + LegacyProtocolVersion.a1_0_16toa1_0_16_2.getName());
         return true;
      }
   }
}
