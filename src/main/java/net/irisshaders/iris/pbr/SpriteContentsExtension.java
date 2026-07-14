package net.irisshaders.iris.pbr;

import net.minecraft.client.texture.SpriteContents.AnimatorImpl;
import org.jetbrains.annotations.Nullable;

public interface SpriteContentsExtension {
   @Nullable
   AnimatorImpl getCreatedTicker();
}
