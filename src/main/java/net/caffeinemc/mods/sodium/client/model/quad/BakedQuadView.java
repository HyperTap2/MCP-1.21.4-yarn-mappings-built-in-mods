package net.caffeinemc.mods.sodium.client.model.quad;

import net.caffeinemc.mods.sodium.client.model.quad.properties.ModelQuadFacing;

public interface BakedQuadView extends ModelQuadView {
   ModelQuadFacing getNormalFace();

   @Override
   int getFaceNormal();

   boolean hasShade();

   boolean hasAO();
}
