package net.minecraft.entity.ai.brain;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Const;
import com.mojang.datafixers.kinds.IdF;
import com.mojang.datafixers.kinds.K1;
import com.mojang.datafixers.kinds.OptionalBox;
import com.mojang.datafixers.kinds.Const.Mu;
import com.mojang.datafixers.util.Unit;
import org.jetbrains.annotations.Nullable;

public interface MemoryQuery<F extends K1, Value> {
   MemoryModuleType<Value> memory();

   MemoryModuleState getState();

   @Nullable
   MemoryQueryResult<F, Value> toQueryResult(Brain<?> brain, java.util.Optional<Value> value);

   record Absent<Value>(MemoryModuleType<Value> memory) implements MemoryQuery<Mu<Unit>, Value> {
      @Override
      public MemoryModuleState getState() {
         return MemoryModuleState.VALUE_ABSENT;
      }

      @Override
      public MemoryQueryResult<Mu<Unit>, Value> toQueryResult(Brain<?> brain, java.util.Optional<Value> value) {
         return value.isPresent() ? null : new MemoryQueryResult<>(brain, this.memory, (App<Mu<Unit>, Value>)(App)Const.create(Unit.INSTANCE));
      }
   }

   record Optional<Value>(MemoryModuleType<Value> memory) implements MemoryQuery<com.mojang.datafixers.kinds.OptionalBox.Mu, Value> {
      @Override
      public MemoryModuleState getState() {
         return MemoryModuleState.REGISTERED;
      }

      @Override
      public MemoryQueryResult<com.mojang.datafixers.kinds.OptionalBox.Mu, Value> toQueryResult(Brain<?> brain, java.util.Optional<Value> value) {
         return new MemoryQueryResult<>(brain, this.memory, (App<com.mojang.datafixers.kinds.OptionalBox.Mu, Value>)(App)OptionalBox.create(value));
      }
   }

   record Present<Value>(MemoryModuleType<Value> memory) implements MemoryQuery<com.mojang.datafixers.kinds.IdF.Mu, Value> {
      @Override
      public MemoryModuleState getState() {
         return MemoryModuleState.VALUE_PRESENT;
      }

      @Override
      public MemoryQueryResult<com.mojang.datafixers.kinds.IdF.Mu, Value> toQueryResult(Brain<?> brain, java.util.Optional<Value> value) {
         return value.isEmpty() ? null : new MemoryQueryResult<>(brain, this.memory, (App<com.mojang.datafixers.kinds.IdF.Mu, Value>)(App)IdF.create(value.get()));
      }
   }
}
