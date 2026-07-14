package com.viaversion.viafabricplus.protocoltranslator.impl.command;

import com.viaversion.viafabricplus.protocoltranslator.impl.command.classic.ListExtensionsCommand;
import com.viaversion.viafabricplus.protocoltranslator.impl.command.classic.SetTimeCommand;
import com.viaversion.vialoader.impl.viaversion.VLCommandHandler;

public final class ViaFabricPlusVLCommandHandler extends VLCommandHandler {
   public ViaFabricPlusVLCommandHandler() {
      this.removeSubCommand("list");
      this.removeSubCommand("player");
      this.removeSubCommand("pps");
      this.removeSubCommand("dump");
      this.registerSubCommand(new ListExtensionsCommand());
      this.registerSubCommand(new SetTimeCommand());
   }
}
