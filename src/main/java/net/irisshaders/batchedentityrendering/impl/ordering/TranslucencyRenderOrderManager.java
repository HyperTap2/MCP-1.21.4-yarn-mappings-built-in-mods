package net.irisshaders.batchedentityrendering.impl.ordering;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import net.irisshaders.batchedentityrendering.impl.BlendingStateHolder;
import net.irisshaders.batchedentityrendering.impl.TransparencyType;
import net.irisshaders.batchedentityrendering.impl.WrappableRenderType;
import net.minecraft.client.render.RenderLayer;

public class TranslucencyRenderOrderManager implements RenderOrderManager {
   private final EnumMap<TransparencyType, LinkedHashSet<RenderLayer>> renderTypes = new EnumMap<>(TransparencyType.class);

   public TranslucencyRenderOrderManager() {
      for (TransparencyType type : TransparencyType.values()) {
         this.renderTypes.put(type, new LinkedHashSet<>());
      }
   }

   private static TransparencyType getTransparencyType(RenderLayer type) {
      while (type instanceof WrappableRenderType) {
         type = ((WrappableRenderType)type).unwrap();
      }

      return type instanceof BlendingStateHolder ? ((BlendingStateHolder)type).getTransparencyType() : TransparencyType.GENERAL_TRANSPARENT;
   }

   @Override
   public void begin(RenderLayer type) {
      this.renderTypes.get(getTransparencyType(type)).add(type);
   }

   @Override
   public void startGroup() {
   }

   @Override
   public boolean maybeStartGroup() {
      return false;
   }

   @Override
   public boolean isInGroup() {
      return false;
   }

   public int getGroupId() {
      return 0;
   }

   @Override
   public void endGroup() {
   }

   @Override
   public void reset() {
      this.renderTypes.forEach((type, set) -> set.clear());
   }

   @Override
   public void resetType(TransparencyType type) {
      this.renderTypes.get(type).clear();
   }

   @Override
   public List<RenderLayer> getRenderOrder() {
      int layerCount = 0;

      for (LinkedHashSet<RenderLayer> set : this.renderTypes.values()) {
         layerCount += set.size();
      }

      List<RenderLayer> allRenderTypes = new ArrayList<>(layerCount);

      for (LinkedHashSet<RenderLayer> set : this.renderTypes.values()) {
         allRenderTypes.addAll(set);
      }

      return allRenderTypes;
   }
}
