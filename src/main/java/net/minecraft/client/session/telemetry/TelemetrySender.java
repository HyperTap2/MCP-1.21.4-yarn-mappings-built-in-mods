package net.minecraft.client.session.telemetry;

import java.util.function.Consumer;

@FunctionalInterface
public interface TelemetrySender {
   TelemetrySender NOOP = (eventType, propertyAdder) -> {};

   default TelemetrySender decorate(Consumer<PropertyMap.Builder> decorationAdder) {
      return (eventType, propertyAdder) -> this.send(eventType, builder -> {
         propertyAdder.accept(builder);
         decorationAdder.accept(builder);
      });
   }

   void send(TelemetryEventType eventType, Consumer<PropertyMap.Builder> propertyAdder);
}
