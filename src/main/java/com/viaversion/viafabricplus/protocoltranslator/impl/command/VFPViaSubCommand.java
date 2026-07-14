package com.viaversion.viafabricplus.protocoltranslator.impl.command;

import com.viaversion.viafabricplus.protocoltranslator.ProtocolTranslator;
import com.viaversion.viafabricplus.util.ChatUtil;
import com.viaversion.viaversion.api.command.ViaCommandSender;
import com.viaversion.viaversion.api.command.ViaSubCommand;
import com.viaversion.viaversion.api.connection.UserConnection;

public interface VFPViaSubCommand extends ViaSubCommand {
   default void sendMessage(ViaCommandSender sender, String message) {
      ViaSubCommand.super.sendMessage(sender, ChatUtil.PREFIX + " " + message, new Object[0]);
   }

   default UserConnection getUser() {
      return ProtocolTranslator.getPlayNetworkUserConnection();
   }
}
