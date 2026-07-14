package net.minecraft.client.render.entity.state;

import net.minecraft.entity.passive.RabbitEntity.RabbitType;

public class RabbitEntityRenderState extends LivingEntityRenderState {
   public float jumpProgress;
   public boolean isToast;
   public RabbitType type = RabbitType.BROWN;
}
