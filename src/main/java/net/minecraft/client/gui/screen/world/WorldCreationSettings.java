package net.minecraft.client.gui.screen.world;

import net.minecraft.resource.DataConfiguration;
import net.minecraft.world.level.WorldGenSettings;

public record WorldCreationSettings(WorldGenSettings worldGenSettings, DataConfiguration dataConfiguration) {
}
