package net.minecraft.client.session.telemetry;

import com.mojang.authlib.minecraft.TelemetryEvent;
import com.mojang.authlib.minecraft.TelemetrySession;
import com.mojang.serialization.Codec;

public record SentTelemetryEvent(TelemetryEventType type, PropertyMap properties) {
   public static final Codec<SentTelemetryEvent> CODEC = TelemetryEventType.CODEC.dispatchStable(SentTelemetryEvent::type, TelemetryEventType::getCodec);

   public SentTelemetryEvent {
      properties.keySet().forEach(property -> {
         if (!type.hasProperty((TelemetryEventProperty<?>)property)) {
            throw new IllegalArgumentException("Property '" + property.id() + "' not expected for event: '" + type.getId() + "'");
         }
      });
   }

   public TelemetryEvent createEvent(TelemetrySession session) {
      return this.type.createEvent(session, this.properties);
   }
}
