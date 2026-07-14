package com.viaversion.viafabricplus.base;

import com.viaversion.viafabricplus.api.events.ChangeProtocolVersionCallback;
import com.viaversion.viafabricplus.api.events.LoadingCycleCallback;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

public final class Events {
   public static final CallbackEvent<LoadingCycleCallback> LOADING_CYCLE = new CallbackEvent<>(listeners -> state -> {
      for (LoadingCycleCallback listener : listeners) {
         listener.onLoadCycle(state);
      }
   });
   public static final CallbackEvent<ChangeProtocolVersionCallback> CHANGE_PROTOCOL_VERSION = new CallbackEvent<>(
      listeners -> (oldVersion, newVersion) -> {
         for (ChangeProtocolVersionCallback listener : listeners) {
            listener.onChangeProtocolVersion(oldVersion, newVersion);
         }
      }
   );

   private Events() {
   }

   public static final class CallbackEvent<T> {
      private final List<T> listeners = new CopyOnWriteArrayList<>();
      private final Function<List<T>, T> invokerFactory;

      private CallbackEvent(Function<List<T>, T> invokerFactory) {
         this.invokerFactory = invokerFactory;
      }

      public void register(T listener) {
         this.listeners.add(listener);
      }

      public T invoker() {
         return this.invokerFactory.apply(List.copyOf(this.listeners));
      }
   }
}
