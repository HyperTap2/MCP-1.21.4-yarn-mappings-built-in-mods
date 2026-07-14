package net.minecraft.client.render.model.json;

import com.google.common.collect.Streams;
import java.util.function.Predicate;
import java.util.ArrayList;
import malte0811.ferritecore.FerriteCoreDeduplicator;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.StateManager;
import net.minecraft.util.Util;

public class AndMultipartModelSelector implements MultipartModelSelector {
   public static final String KEY = "AND";
   private final Iterable<? extends MultipartModelSelector> selectors;

   public AndMultipartModelSelector(Iterable<? extends MultipartModelSelector> selectors) {
      this.selectors = selectors;
   }

   @Override
   public Predicate<BlockState> getPredicate(StateManager<Block, BlockState> stateManager) {
      return FerriteCoreDeduplicator.and(
         Streams.stream(this.selectors).map(selector -> selector.getPredicate(stateManager)).collect(java.util.stream.Collectors.toCollection(ArrayList::new))
      );
   }
}
