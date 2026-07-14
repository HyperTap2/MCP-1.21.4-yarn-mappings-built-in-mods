package net.caffeinemc.mods.sodium.client.model.light;

import net.caffeinemc.mods.sodium.client.model.light.data.QuadLightData;
import net.caffeinemc.mods.sodium.client.model.quad.ModelQuadView;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public interface LightPipeline {
   void calculate(ModelQuadView var1, BlockPos var2, QuadLightData var3, Direction var4, Direction var5, boolean var6, boolean var7);
}
