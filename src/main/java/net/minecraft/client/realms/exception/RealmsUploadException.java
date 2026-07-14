package net.minecraft.client.realms.exception;

import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

public abstract class RealmsUploadException extends RuntimeException {
   @Nullable
   public Text getStatus() {
      return null;
   }

   @Nullable
   public Text[] getStatusTexts() {
      return null;
   }
}
