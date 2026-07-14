package net.minecraft.item;

import com.viaversion.viafabricplus.protocoltranslator.ProtocolTranslator;
import com.viaversion.viafabricplus.protocoltranslator.impl.ViaFabricPlusMappingDataLoader;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class ItemPlacementContext extends ItemUsageContext {
   private final BlockPos placementPos;
   protected boolean canReplaceExisting = true;

   public ItemPlacementContext(PlayerEntity player, Hand hand, ItemStack stack, BlockHitResult hitResult) {
      this(player.getWorld(), player, hand, stack, hitResult);
   }

   public ItemPlacementContext(ItemUsageContext context) {
      this(context.getWorld(), context.getPlayer(), context.getHand(), context.getStack(), context.getHitResult());
   }

   public ItemPlacementContext(World world, @Nullable PlayerEntity playerEntity, Hand hand, ItemStack itemStack, BlockHitResult blockHitResult) {
      super(world, playerEntity, hand, itemStack, blockHitResult);
      this.placementPos = blockHitResult.getBlockPos().offset(blockHitResult.getSide());
      this.canReplaceExisting = world.getBlockState(blockHitResult.getBlockPos()).canReplace(this);
   }

   public static ItemPlacementContext offset(ItemPlacementContext context, BlockPos pos, Direction side) {
      return new ItemPlacementContext(
         context.getWorld(),
         context.getPlayer(),
         context.getHand(),
         context.getStack(),
         new BlockHitResult(
            new Vec3d(pos.getX() + 0.5 + side.getOffsetX() * 0.5, pos.getY() + 0.5 + side.getOffsetY() * 0.5, pos.getZ() + 0.5 + side.getOffsetZ() * 0.5),
            side,
            pos,
            false
         )
      );
   }

   @Override
   public BlockPos getBlockPos() {
      return this.canReplaceExisting ? super.getBlockPos() : this.placementPos;
   }

   public boolean canPlace() {
      boolean canPlace = this.canReplaceExisting || this.getWorld().getBlockState(this.getBlockPos()).canReplace(this);
      if (!canPlace && ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_12_2)) {
         return ViaFabricPlusMappingDataLoader.getBlockMaterial(this.getWorld().getBlockState(this.getBlockPos()).getBlock()).equals("decoration")
            && Block.getBlockFromItem(this.getStack().getItem()).equals(Blocks.ANVIL);
      }

      return canPlace;
   }

   public boolean canReplaceExisting() {
      return this.canReplaceExisting;
   }

   public Direction getPlayerLookDirection() {
      if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_12_2)) {
         PlayerEntity player = this.getPlayer();
         BlockPos placementPos = this.getBlockPos();
         double centerOffset = ProtocolTranslator.getTargetVersion().newerThan(ProtocolVersion.v1_10) ? 0.5 : 0.0;
         if (Math.abs(player.getX() - (placementPos.getX() + centerOffset)) < 2.0
            && Math.abs(player.getZ() - (placementPos.getZ() + centerOffset)) < 2.0) {
            double eyeY = player.getY() + player.getEyeHeight(player.getPose());
            if (eyeY - placementPos.getY() > 2.0) {
               return Direction.DOWN;
            }

            if (placementPos.getY() - eyeY > 0.0) {
               return Direction.UP;
            }
         }

         return player.getHorizontalFacing();
      }

      return Direction.getEntityFacingOrder(this.getPlayer())[0];
   }

   public Direction getVerticalPlayerLookDirection() {
      return Direction.getLookDirectionForAxis(this.getPlayer(), Direction.Axis.Y);
   }

   public Direction[] getPlacementDirections() {
      Direction[] directions = Direction.getEntityFacingOrder(this.getPlayer());
      if (this.canReplaceExisting) {
         return directions;
      }

      Direction direction = this.getSide();
      int i = 0;

      while (i < directions.length && directions[i] != direction.getOpposite()) {
         i++;
      }

      if (i > 0) {
         System.arraycopy(directions, 0, directions, 1, i);
         directions[0] = direction.getOpposite();
      }

      return directions;
   }
}
