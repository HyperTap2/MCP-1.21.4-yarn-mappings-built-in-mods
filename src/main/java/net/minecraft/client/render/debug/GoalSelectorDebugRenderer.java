package net.minecraft.client.render.debug;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.network.packet.s2c.custom.DebugGoalSelectorCustomPayload.Goal;
import net.minecraft.util.math.BlockPos;

public class GoalSelectorDebugRenderer implements DebugRenderer.Renderer {
   private static final int RANGE = 160;
   private final MinecraftClient client;
   private final Int2ObjectMap<GoalSelectorDebugRenderer.Entity> goalSelectors = new Int2ObjectOpenHashMap();

   @Override
   public void clear() {
      this.goalSelectors.clear();
   }

   public void setGoalSelectorList(int index, BlockPos pos, List<Goal> goals) {
      this.goalSelectors.put(index, new GoalSelectorDebugRenderer.Entity(pos, goals));
   }

   public void removeGoalSelectorList(int index) {
      this.goalSelectors.remove(index);
   }

   public GoalSelectorDebugRenderer(MinecraftClient client) {
      this.client = client;
   }

   @Override
   public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, double cameraX, double cameraY, double cameraZ) {
      Camera camera = this.client.gameRenderer.getCamera();
      BlockPos blockPos = BlockPos.ofFloored(camera.getPos().x, 0.0, camera.getPos().z);
      ObjectIterator var11 = this.goalSelectors.values().iterator();

      while (var11.hasNext()) {
         GoalSelectorDebugRenderer.Entity entity = (GoalSelectorDebugRenderer.Entity)var11.next();
         BlockPos blockPos2 = entity.entityPos;
         if (blockPos.isWithinDistance(blockPos2, 160.0)) {
            for (int i = 0; i < entity.goals.size(); i++) {
               Goal goal = entity.goals.get(i);
               double d = blockPos2.getX() + 0.5;
               double e = blockPos2.getY() + 2.0 + i * 0.25;
               double f = blockPos2.getZ() + 0.5;
               int j = goal.isRunning() ? -16711936 : -3355444;
               DebugRenderer.drawString(matrices, vertexConsumers, goal.name(), d, e, f, j);
            }
         }
      }
   }

   record Entity(BlockPos entityPos, List<Goal> goals) {
   }
}
