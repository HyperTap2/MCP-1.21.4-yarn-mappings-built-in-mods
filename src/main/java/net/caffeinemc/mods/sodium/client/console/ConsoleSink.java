package net.caffeinemc.mods.sodium.client.console;

import net.caffeinemc.mods.sodium.client.console.message.MessageLevel;
import org.jetbrains.annotations.NotNull;

public interface ConsoleSink {
   void logMessage(@NotNull MessageLevel var1, @NotNull String var2, boolean var3, double var4);
}
