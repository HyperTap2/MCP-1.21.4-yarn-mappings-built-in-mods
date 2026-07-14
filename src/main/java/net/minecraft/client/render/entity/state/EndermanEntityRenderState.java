package net.minecraft.client.render.entity.state;

import net.minecraft.block.BlockState;
import org.jetbrains.annotations.Nullable;

public class EndermanEntityRenderState extends BipedEntityRenderState {
   public boolean angry;
   @Nullable
   public BlockState carriedBlock;
}
