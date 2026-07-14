package com.viaversion.viafabricplus.features.block.shape;

import com.google.common.collect.UnmodifiableIterator;
import net.minecraft.block.AnvilBlock;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.BrewingStandBlock;
import net.minecraft.block.CarpetBlock;
import net.minecraft.block.CauldronBlock;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.CropBlock;
import net.minecraft.block.EndPortalBlock;
import net.minecraft.block.EndPortalFrameBlock;
import net.minecraft.block.EnderChestBlock;
import net.minecraft.block.FarmlandBlock;
import net.minecraft.block.FenceBlock;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.block.FlowerbedBlock;
import net.minecraft.block.HopperBlock;
import net.minecraft.block.LadderBlock;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.LilyPadBlock;
import net.minecraft.block.PaneBlock;
import net.minecraft.block.PistonBlock;
import net.minecraft.block.PistonHeadBlock;
import net.minecraft.block.SnowBlock;
import net.minecraft.block.WallBlock;
import net.minecraft.registry.Registries;

public final class CollisionShapes {
   public static void reloadBlockShapes() {
      for (Block block : Registries.BLOCK) {
         if (block instanceof AnvilBlock
            || block instanceof BedBlock
            || block instanceof BrewingStandBlock
            || block instanceof CarpetBlock
            || block instanceof CauldronBlock
            || block instanceof ChestBlock
            || block instanceof EnderChestBlock
            || block instanceof EndPortalBlock
            || block instanceof EndPortalFrameBlock
            || block instanceof FarmlandBlock
            || block instanceof FenceBlock
            || block instanceof FenceGateBlock
            || block instanceof HopperBlock
            || block instanceof LadderBlock
            || block instanceof LeavesBlock
            || block instanceof LilyPadBlock
            || block instanceof PaneBlock
            || block instanceof PistonBlock
            || block instanceof PistonHeadBlock
            || block instanceof SnowBlock
            || block instanceof WallBlock
            || block instanceof CropBlock
            || block instanceof FlowerbedBlock) {
            UnmodifiableIterator var2 = block.getStateManager().getStates().iterator();

            while (var2.hasNext()) {
               BlockState state = (BlockState)var2.next();
               state.initShapeCache();
            }
         }
      }
   }
}
