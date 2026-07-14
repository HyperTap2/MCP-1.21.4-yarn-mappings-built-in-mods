package net.minecraft.client.render.entity.model;

import java.util.function.Function;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.util.Identifier;

public abstract class EntityModel<T extends EntityRenderState> extends Model {
   public static final float field_52908 = -1.501F;

   protected EntityModel(ModelPart root) {
      this(root, RenderLayer::getEntityCutoutNoCull);
   }

   protected EntityModel(ModelPart modelPart, Function<Identifier, RenderLayer> function) {
      super(modelPart, function);
   }

   public void setAngles(T state) {
      this.resetTransforms();
   }
}
