package net.minecraft.client.render;

import net.minecraft.client.util.ClosableFactory;
import net.minecraft.client.util.Handle;

public interface RenderPass {
   <T> Handle<T> addRequiredResource(String name, ClosableFactory<T> factory);

   <T> void dependsOn(Handle<T> handle);

   <T> Handle<T> transfer(Handle<T> handle);

   void addRequired(RenderPass pass);

   void markToBeVisited();

   void setRenderer(Runnable renderer);
}
