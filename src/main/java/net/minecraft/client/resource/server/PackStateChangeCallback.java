package net.minecraft.client.resource.server;

import java.util.UUID;

public interface PackStateChangeCallback {
   void onStateChanged(UUID id, PackStateChangeCallback.State state);

   void onFinish(UUID id, PackStateChangeCallback.FinishState state);

   enum FinishState {
      DECLINED,
      APPLIED,
      DISCARDED,
      DOWNLOAD_FAILED,
      ACTIVATION_FAILED;
   }

   enum State {
      ACCEPTED,
      DOWNLOADED;
   }
}
