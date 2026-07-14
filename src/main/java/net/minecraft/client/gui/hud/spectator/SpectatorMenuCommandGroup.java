package net.minecraft.client.gui.hud.spectator;

import java.util.List;
import net.minecraft.text.Text;

public interface SpectatorMenuCommandGroup {
   List<SpectatorMenuCommand> getCommands();

   Text getPrompt();
}
