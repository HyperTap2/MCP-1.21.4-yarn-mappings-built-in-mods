package me.pepperbell.continuity.api.client;

import java.util.Collection;
import java.util.Properties;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourcePack;
import net.minecraft.util.Identifier;

public interface CtmProperties extends Comparable<CtmProperties> {
	Collection<SpriteIdentifier> getTextureDependencies();

	interface Factory<T extends CtmProperties> {
		@Nullable
		T createProperties(Properties properties, Identifier resourceId, ResourcePack pack, int packPriority, ResourceManager resourceManager, String method);
	}
}
