package net.minecraft.client.render.entity.state;

import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

public class EndCrystalEntityRenderState extends EntityRenderState {
   public boolean baseVisible = true;
   @Nullable
   public Vec3d beamOffset;
}
