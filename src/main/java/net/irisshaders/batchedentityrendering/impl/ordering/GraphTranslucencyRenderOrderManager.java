package net.irisshaders.batchedentityrendering.impl.ordering;

import de.odysseus.ithaka.digraph.Digraph;
import de.odysseus.ithaka.digraph.Digraphs;
import de.odysseus.ithaka.digraph.MapDigraph;
import de.odysseus.ithaka.digraph.util.fas.FeedbackArcSet;
import de.odysseus.ithaka.digraph.util.fas.FeedbackArcSetPolicy;
import de.odysseus.ithaka.digraph.util.fas.FeedbackArcSetProvider;
import de.odysseus.ithaka.digraph.util.fas.SimpleFeedbackArcSetProvider;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import net.irisshaders.batchedentityrendering.impl.BlendingStateHolder;
import net.irisshaders.batchedentityrendering.impl.TransparencyType;
import net.irisshaders.batchedentityrendering.impl.WrappableRenderType;
import net.minecraft.client.render.RenderLayer;

public class GraphTranslucencyRenderOrderManager implements RenderOrderManager {
   private final FeedbackArcSetProvider feedbackArcSetProvider;
   private final EnumMap<TransparencyType, Digraph<RenderLayer>> types;
   private final EnumMap<TransparencyType, RenderLayer> currentTypes;
   private boolean inGroup = false;

   public GraphTranslucencyRenderOrderManager() {
      this.feedbackArcSetProvider = new SimpleFeedbackArcSetProvider();
      this.types = new EnumMap<>(TransparencyType.class);
      this.currentTypes = new EnumMap<>(TransparencyType.class);

      for (TransparencyType type : TransparencyType.values()) {
         this.types.put(type, new MapDigraph<>());
      }
   }

   private static TransparencyType getTransparencyType(RenderLayer type) {
      while (type instanceof WrappableRenderType) {
         type = ((WrappableRenderType)type).unwrap();
      }

      return type instanceof BlendingStateHolder ? ((BlendingStateHolder)type).getTransparencyType() : TransparencyType.GENERAL_TRANSPARENT;
   }

   @Override
   public void begin(RenderLayer renderType) {
      TransparencyType transparencyType = getTransparencyType(renderType);
      Digraph<RenderLayer> graph = this.types.get(transparencyType);
      graph.add(renderType);
      if (this.inGroup) {
         RenderLayer previous = this.currentTypes.put(transparencyType, renderType);
         if (previous == null) {
            return;
         }

         int weight = graph.get(previous, renderType).orElse(0);
         graph.put(previous, renderType, ++weight);
      }
   }

   @Override
   public void startGroup() {
      if (this.inGroup) {
         throw new IllegalStateException("Already in a group");
      }

      this.currentTypes.clear();
      this.inGroup = true;
   }

   @Override
   public boolean maybeStartGroup() {
      if (this.inGroup) {
         return false;
      }

      this.currentTypes.clear();
      this.inGroup = true;
      return true;
   }

   @Override
   public boolean isInGroup() {
      return this.inGroup;
   }

   @Override
   public void endGroup() {
      if (!this.inGroup) {
         throw new IllegalStateException("Not in a group");
      }

      this.currentTypes.clear();
      this.inGroup = false;
   }

   @Override
   public void reset() {
      this.types.clear();

      for (TransparencyType type : TransparencyType.values()) {
         this.types.put(type, new MapDigraph<>());
      }
   }

   @Override
   public void resetType(TransparencyType type) {
      this.types.put(type, new MapDigraph<>());
   }

   @Override
   public List<RenderLayer> getRenderOrder() {
      int layerCount = 0;

      for (Digraph<RenderLayer> graph : this.types.values()) {
         layerCount += graph.getVertexCount();
      }

      List<RenderLayer> allLayers = new ArrayList<>(layerCount);

      for (Digraph<RenderLayer> graph : this.types.values()) {
         FeedbackArcSet<RenderLayer> arcSet = this.feedbackArcSetProvider.getFeedbackArcSet(graph, graph, FeedbackArcSetPolicy.MIN_WEIGHT);
         if (arcSet.getEdgeCount() > 0) {
            for (RenderLayer source : arcSet.vertices()) {
               for (RenderLayer target : arcSet.targets(source)) {
                  graph.remove(source, target);
               }
            }
         }

         allLayers.addAll(Digraphs.toposort(graph, false));
      }

      return allLayers;
   }
}
