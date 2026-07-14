package net.minecraft.client.gui.screen.world;

import java.util.Set;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.GameRules.BooleanRule;
import net.minecraft.world.GameRules.Key;
import net.minecraft.world.gen.FlatLevelGeneratorPreset;
import org.jetbrains.annotations.Nullable;

public record InitialWorldOptions(
   WorldCreator.Mode selectedGameMode, Set<Key<BooleanRule>> disabledGameRules, @Nullable RegistryKey<FlatLevelGeneratorPreset> flatLevelPreset
) {
}
