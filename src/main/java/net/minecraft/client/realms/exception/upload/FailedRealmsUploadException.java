package net.minecraft.client.realms.exception.upload;

import net.minecraft.client.realms.exception.RealmsUploadException;
import net.minecraft.text.Text;

public class FailedRealmsUploadException extends RealmsUploadException {
   private final Text errorMessage;

   public FailedRealmsUploadException(Text errorMessage) {
      this.errorMessage = errorMessage;
   }

   public FailedRealmsUploadException(String errorMessage) {
      this(Text.literal(errorMessage));
   }

   @Override
   public Text getStatus() {
      return Text.translatable("mco.upload.failed", new Object[]{this.errorMessage});
   }
}
