package com.viaversion.viafabricplus.features.interaction.replace_block_placement_logic;

import net.minecraft.util.ActionResult;

public final class ActionResultException1_12_2 extends RuntimeException {
   private final ActionResult actionResult;

   public ActionResultException1_12_2(ActionResult actionResult) {
      this.actionResult = actionResult;
   }

   public ActionResult getActionResult() {
      return this.actionResult;
   }

   @Override
   public synchronized Throwable fillInStackTrace() {
      return this;
   }
}
