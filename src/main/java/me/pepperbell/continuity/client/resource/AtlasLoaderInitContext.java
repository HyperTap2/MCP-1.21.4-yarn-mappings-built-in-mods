package me.pepperbell.continuity.client.resource;

import java.util.Set;

import org.jetbrains.annotations.Nullable;

import net.minecraft.util.Identifier;

public interface AtlasLoaderInitContext {
	ThreadLocal<AtlasLoaderInitContext> THREAD_LOCAL = new ThreadLocal<>();

	@Nullable
	Set<Identifier> getExtraIds();
}
