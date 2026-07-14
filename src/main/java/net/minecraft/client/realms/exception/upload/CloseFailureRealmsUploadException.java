package net.minecraft.client.realms.exception.upload;

import net.minecraft.client.realms.exception.RealmsUploadException;
import net.minecraft.text.Text;

public class CloseFailureRealmsUploadException extends RealmsUploadException {
   @Override
   public Text getStatus() {
      return Text.translatable("mco.upload.close.failure");
   }
}
