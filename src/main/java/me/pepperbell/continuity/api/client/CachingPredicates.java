package me.pepperbell.continuity.api.client;

import java.util.function.Function;

import net.minecraft.block.BlockState;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.SpriteIdentifier;

public interface CachingPredicates {
	boolean affectsSprites();

	boolean affectsSprite(Sprite sprite);

	boolean affectsBlockStates();

	boolean affectsBlockState(BlockState state);

	boolean isValidForMultipass();

	interface Factory<T extends CtmProperties> {
		CachingPredicates createPredicates(T properties, Function<SpriteIdentifier, Sprite> textureGetter);
	}
}
