package net.irisshaders.batchedentityrendering.impl.wrappers;

import java.util.Objects;
import java.util.Optional;
import net.irisshaders.batchedentityrendering.impl.BlendingStateHolder;
import net.irisshaders.batchedentityrendering.impl.TransparencyType;
import net.irisshaders.batchedentityrendering.impl.WrappableRenderType;
import net.minecraft.client.render.RenderLayer;
import org.jetbrains.annotations.Nullable;

public class TaggingRenderTypeWrapper extends RenderLayer implements WrappableRenderType, BlendingStateHolder {
   private final int tag;
   private final RenderLayer wrapped;

   public TaggingRenderTypeWrapper(String name, RenderLayer wrapped, int tag) {
      super(
         name,
         wrapped.getVertexFormat(),
         wrapped.getDrawMode(),
         wrapped.getExpectedBufferSize(),
         wrapped.hasCrumbling(),
         shouldSortOnUpload(wrapped),
         wrapped::startDrawing,
         wrapped::endDrawing
      );
      this.tag = tag;
      this.wrapped = wrapped;
   }

   private static boolean shouldSortOnUpload(RenderLayer type) {
      return type.isTranslucent();
   }

   @Override
   public RenderLayer unwrap() {
      return this.wrapped;
   }

   public Optional<RenderLayer> getAffectedOutline() {
      return this.wrapped.getAffectedOutline();
   }

   public boolean isOutline() {
      return this.wrapped.isOutline();
   }

   @Override
   public boolean equals(@Nullable Object object) {
      if (object == null) {
         return false;
      }

      if (object.getClass() != this.getClass()) {
         return false;
      }

      TaggingRenderTypeWrapper other = (TaggingRenderTypeWrapper)object;
      return this.tag == other.tag && Objects.equals(this.wrapped, other.wrapped);
   }

   @Override
   public int hashCode() {
      return this.wrapped.hashCode() + this.tag + 1;
   }

   @Override
   public String toString() {
      return "tagged(" + this.tag + "):" + this.wrapped.toString();
   }

   @Override
   public TransparencyType getTransparencyType() {
      return ((BlendingStateHolder)this.wrapped).getTransparencyType();
   }

   @Override
   public void setTransparencyType(TransparencyType transparencyType) {
      ((BlendingStateHolder)this.wrapped).setTransparencyType(transparencyType);
   }
}
