package net.minecraft.client.realms.exception;

import net.minecraft.client.realms.RealmsError;

public class RetryCallException extends RealmsServiceException {
   public static final int DEFAULT_DELAY_SECONDS = 5;
   public final int delaySeconds;

   public RetryCallException(int delaySeconds, int httpResultCode) {
      super(RealmsError.SimpleHttpError.retryable(httpResultCode));
      if (delaySeconds >= 0 && delaySeconds <= 120) {
         this.delaySeconds = delaySeconds;
      } else {
         this.delaySeconds = 5;
      }
   }
}
