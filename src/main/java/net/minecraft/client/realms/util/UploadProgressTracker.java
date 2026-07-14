package net.minecraft.client.realms.util;


public interface UploadProgressTracker {
   UploadProgress getUploadProgress();

   void updateProgressDisplay();

   static UploadProgressTracker create() {
      return new UploadProgressTracker() {
         private final UploadProgress progress = new UploadProgress();

         @Override
         public UploadProgress getUploadProgress() {
            return this.progress;
         }

         @Override
         public void updateProgressDisplay() {
         }
      };
   }
}
