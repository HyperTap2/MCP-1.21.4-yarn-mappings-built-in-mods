package net.minecraft.client.realms.exception.upload;

import net.minecraft.client.realms.exception.RealmsUploadException;
import net.minecraft.text.Text;

public class CancelledRealmsUploadException extends RealmsUploadException {
   private static final Text STATUS_TEXT = Text.translatable("mco.upload.cancelled");

   @Override
   public Text getStatus() {
      return STATUS_TEXT;
   }
}
