package net.minecraft.item;

import com.viaversion.viafabricplus.protocoltranslator.ProtocolTranslator;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.raphimc.vialegacy.api.LegacyProtocolVersion;

public class SwordItem extends Item {
   public SwordItem(ToolMaterial material, float attackDamage, float attackSpeed, Item.Settings settings) {
      super(material.applySwordSettings(settings, attackDamage, attackSpeed));
   }

   @Override
   public ActionResult use(World world, PlayerEntity user, Hand hand) {
      if (ProtocolTranslator.getTargetVersion().betweenInclusive(LegacyProtocolVersion.b1_8tob1_8_1, ProtocolVersion.v1_8)) {
         user.setCurrentHand(hand);
         return ActionResult.SUCCESS;
      }

      return super.use(world, user, hand);
   }

   @Override
   public boolean canMine(BlockState state, World world, BlockPos pos, PlayerEntity miner) {
      return !miner.isCreative();
   }

   @Override
   public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
      return true;
   }

   @Override
   public void postDamageEntity(ItemStack stack, LivingEntity target, LivingEntity attacker) {
      stack.damage(1, attacker, EquipmentSlot.MAINHAND);
   }
}
