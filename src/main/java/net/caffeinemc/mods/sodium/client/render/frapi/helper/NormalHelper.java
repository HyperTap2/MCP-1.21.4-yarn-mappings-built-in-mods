package net.caffeinemc.mods.sodium.client.render.frapi.helper;

import net.fabricmc.fabric.api.renderer.v1.mesh.QuadView;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

public abstract class NormalHelper {
   private NormalHelper() {
   }

   public static void computeFaceNormal(@NotNull Vector3f saveTo, QuadView q) {
      Direction nominalFace = q.nominalFace();
      if (nominalFace != null && GeometryHelper.isQuadParallelToFace(nominalFace, q)) {
         Vec3i vec = nominalFace.getVector();
         saveTo.set(vec.getX(), vec.getY(), vec.getZ());
      } else {
         float x0 = q.x(0);
         float y0 = q.y(0);
         float z0 = q.z(0);
         float x1 = q.x(1);
         float y1 = q.y(1);
         float z1 = q.z(1);
         float x2 = q.x(2);
         float y2 = q.y(2);
         float z2 = q.z(2);
         float x3 = q.x(3);
         float y3 = q.y(3);
         float z3 = q.z(3);
         float dx0 = x2 - x0;
         float dy0 = y2 - y0;
         float dz0 = z2 - z0;
         float dx1 = x3 - x1;
         float dy1 = y3 - y1;
         float dz1 = z3 - z1;
         float normX = dy0 * dz1 - dz0 * dy1;
         float normY = dz0 * dx1 - dx0 * dz1;
         float normZ = dx0 * dy1 - dy0 * dx1;
         float l = (float)Math.sqrt(normX * normX + normY * normY + normZ * normZ);
         if (l != 0.0F) {
            normX /= l;
            normY /= l;
            normZ /= l;
         }

         saveTo.set(normX, normY, normZ);
      }
   }
}
