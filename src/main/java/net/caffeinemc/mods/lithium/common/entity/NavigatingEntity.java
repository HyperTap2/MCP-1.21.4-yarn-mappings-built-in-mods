package net.caffeinemc.mods.lithium.common.entity;

import net.minecraft.entity.ai.pathing.EntityNavigation;
import org.jetbrains.annotations.Nullable;

public interface NavigatingEntity {
   boolean lithium$isRegisteredToWorld();

   void lithium$setRegisteredToWorld(@Nullable EntityNavigation navigation);

   @Nullable
   EntityNavigation lithium$getRegisteredNavigation();

   void lithium$updateNavigationRegistration();
}
