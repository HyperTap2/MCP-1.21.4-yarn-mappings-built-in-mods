package net.minecraft.client.render.model;

import net.minecraft.block.BlockState;

public interface GroupableModel extends ResolvableModel {
   BakedModel bake(Baker baker);

   Object getEqualityGroup(BlockState state);
}
