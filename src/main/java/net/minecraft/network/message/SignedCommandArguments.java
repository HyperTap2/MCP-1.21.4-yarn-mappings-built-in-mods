package net.minecraft.network.message;

import java.util.Map;
import org.jetbrains.annotations.Nullable;

public interface SignedCommandArguments {
   SignedCommandArguments EMPTY = new SignedCommandArguments() {
      @Nullable
      @Override
      public SignedMessage getMessage(String argumentName) {
         return null;
      }
   };

   @Nullable
   SignedMessage getMessage(String argumentName);

   record Impl(Map<String, SignedMessage> arguments) implements SignedCommandArguments {
      @Nullable
      @Override
      public SignedMessage getMessage(String argumentName) {
         return this.arguments.get(argumentName);
      }
   }
}
