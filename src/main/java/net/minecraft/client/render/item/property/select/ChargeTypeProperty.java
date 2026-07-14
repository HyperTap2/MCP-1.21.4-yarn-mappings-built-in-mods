package net.minecraft.client.render.item.property.select;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ChargedProjectilesComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.ModelTransformationMode;
import net.minecraft.item.CrossbowItem.ChargeType;
import org.jetbrains.annotations.Nullable;

public record ChargeTypeProperty() implements SelectProperty<ChargeType> {
   public static final SelectProperty.Type<ChargeTypeProperty, ChargeType> TYPE = SelectProperty.Type.create(
      MapCodec.unit(new ChargeTypeProperty()), ChargeType.CODEC
   );

   public ChargeType getValue(
      ItemStack itemStack, @Nullable ClientWorld clientWorld, @Nullable LivingEntity livingEntity, int i, ModelTransformationMode modelTransformationMode
   ) {
      ChargedProjectilesComponent chargedProjectilesComponent = (ChargedProjectilesComponent)itemStack.get(DataComponentTypes.CHARGED_PROJECTILES);
      if (chargedProjectilesComponent == null || chargedProjectilesComponent.isEmpty()) {
         return ChargeType.NONE;
      } else {
         return chargedProjectilesComponent.contains(Items.FIREWORK_ROCKET) ? ChargeType.ROCKET : ChargeType.ARROW;
      }
   }

   @Override
   public SelectProperty.Type<ChargeTypeProperty, ChargeType> getType() {
      return TYPE;
   }
}
