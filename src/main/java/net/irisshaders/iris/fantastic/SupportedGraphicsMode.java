package net.irisshaders.iris.fantastic;

import net.irisshaders.iris.Iris;
import net.minecraft.client.option.GraphicsMode;
import net.minecraft.client.option.SimpleOption;

public enum SupportedGraphicsMode {
   FAST,
   FANCY;

   public static SupportedGraphicsMode fromVanilla(SimpleOption<GraphicsMode> status) {
      return switch ((GraphicsMode)status.getValue()) {
         case FAST -> FAST;
         case FANCY -> FANCY;
         case FABULOUS -> {
            Iris.logger.warn("Detected Fabulous Graphics being used somehow, changing to Fancy!");
            status.setValue(GraphicsMode.FANCY);
            yield FANCY;
         }
         default -> throw new MatchException(null, null);
      };
   }

   public GraphicsMode toVanilla() {
      return switch (this) {
         case FAST -> GraphicsMode.FAST;
         case FANCY -> GraphicsMode.FANCY;
      };
   }
}
