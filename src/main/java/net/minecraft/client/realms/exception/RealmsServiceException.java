package net.minecraft.client.realms.exception;

import net.minecraft.client.realms.RealmsError;

public class RealmsServiceException extends Exception {
   public final RealmsError error;

   public RealmsServiceException(RealmsError error) {
      this.error = error;
   }

   @Override
   public String getMessage() {
      return this.error.getErrorMessage();
   }
}
