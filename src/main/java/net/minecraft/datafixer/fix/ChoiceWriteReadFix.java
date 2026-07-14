package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.DSL.TypeReference;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import net.minecraft.datafixer.FixUtil;
import net.minecraft.util.Util;

public abstract class ChoiceWriteReadFix extends DataFix {
   private final String name;
   private final String choiceName;
   private final TypeReference type;

   public ChoiceWriteReadFix(Schema outputSchema, boolean changesType, String name, TypeReference type, String choiceName) {
      super(outputSchema, changesType);
      this.name = name;
      this.type = type;
      this.choiceName = choiceName;
   }

   public TypeRewriteRule makeRule() {
      Type<?> type = this.getInputSchema().getType(this.type);
      Type<?> type2 = this.getInputSchema().getChoiceType(this.type, this.choiceName);
      Type<?> type3 = this.getOutputSchema().getType(this.type);
      Type<?> type4 = this.getOutputSchema().getChoiceType(this.type, this.choiceName);
      OpticFinder<?> opticFinder = DSL.namedChoice(this.choiceName, type2);
      Type<?> type5 = FixUtil.withTypeChanged(type2, type, type3);
      return this.makeRule(type, type3, opticFinder, type4, type5);
   }

   @SuppressWarnings("unchecked")
    private <A, B> TypeRewriteRule makeRule(
      Type<?> inputType, Type<?> outputType, OpticFinder<A> opticFinder, Type<B> outputSubtype, Type<?> rewrittenType
   ) {
      return this.fixTypeEverywhere(this.name, (Type)inputType, (Type)outputType, dynamicOps -> input -> {
         Typed typed = new Typed(inputType, dynamicOps, input);
         return typed.update(opticFinder, outputSubtype, object -> {
            @SuppressWarnings("unchecked")
             Typed<A> typedx = new Typed((Type<A>)rewrittenType, dynamicOps, object);
            return Util.apply(typedx, outputSubtype, ChoiceWriteReadFix.this::transform).getValue();
         }).getValue();
      });
   }

   protected abstract <T> Dynamic<T> transform(Dynamic<T> data);
}
