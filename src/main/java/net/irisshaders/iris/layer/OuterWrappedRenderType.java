package net.irisshaders.iris.layer;

import java.util.Objects;
import java.util.Optional;
import net.irisshaders.batchedentityrendering.impl.BlendingStateHolder;
import net.irisshaders.batchedentityrendering.impl.TransparencyType;
import net.irisshaders.batchedentityrendering.impl.WrappableRenderType;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import org.jetbrains.annotations.Nullable;

public class OuterWrappedRenderType extends RenderLayer implements WrappableRenderType, BlendingStateHolder {
   private final RenderPhase extra;
   private final RenderLayer wrapped;

   public OuterWrappedRenderType(String name, RenderLayer wrapped, RenderPhase extra) {
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
      this.extra = extra;
      this.wrapped = wrapped;
   }

   public static OuterWrappedRenderType wrapExactlyOnce(String name, RenderLayer wrapped, RenderPhase extra) {
      if (wrapped instanceof OuterWrappedRenderType) {
         wrapped = ((OuterWrappedRenderType)wrapped).unwrap();
      }

      return new OuterWrappedRenderType(name, wrapped, extra);
   }

   private static boolean shouldSortOnUpload(RenderLayer type) {
      return type.isTranslucent();
   }

   public void startDrawing() {
      this.extra.startDrawing();
      super.startDrawing();
   }

   public void endDrawing() {
      super.endDrawing();
      this.extra.endDrawing();
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

      OuterWrappedRenderType other = (OuterWrappedRenderType)object;
      return Objects.equals(this.wrapped, other.wrapped) && Objects.equals(this.extra, other.extra);
   }

   @Override
   public int hashCode() {
      return this.wrapped.hashCode() + 1;
   }

   @Override
   public String toString() {
      return "iris_wrapped:" + this.wrapped.toString();
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
