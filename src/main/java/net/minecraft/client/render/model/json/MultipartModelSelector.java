package net.minecraft.client.render.model.json;

import java.util.function.Predicate;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.StateManager;

@FunctionalInterface
public interface MultipartModelSelector {
   MultipartModelSelector TRUE = stateFactory -> state -> true;
   MultipartModelSelector FALSE = stateFactory -> state -> false;

   Predicate<BlockState> getPredicate(StateManager<Block, BlockState> stateFactory);
}
