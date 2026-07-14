package net.minecraft.client.realms.exception;


public class RealmsHttpException extends RuntimeException {
   public RealmsHttpException(String s, Exception e) {
      super(s, e);
   }
}
