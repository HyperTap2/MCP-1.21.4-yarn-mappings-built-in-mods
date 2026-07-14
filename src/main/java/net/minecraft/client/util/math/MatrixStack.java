package net.minecraft.client.util.math;

import com.google.common.collect.Queues;
import java.util.Deque;
import net.minecraft.util.Util;
import net.minecraft.util.math.MatrixUtil;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class MatrixStack {
   private final Deque<MatrixStack.Entry> stack = Util.make(Queues.newArrayDeque(), stack -> {
      Matrix4f matrix4f = new Matrix4f();
      Matrix3f matrix3f = new Matrix3f();
      stack.add(new MatrixStack.Entry(matrix4f, matrix3f));
   });

   public void translate(double x, double y, double z) {
      this.translate((float)x, (float)y, (float)z);
   }

   public void translate(float x, float y, float z) {
      MatrixStack.Entry entry = this.stack.getLast();
      entry.positionMatrix.translate(x, y, z);
   }

   public void translate(Vec3d vec) {
      this.translate(vec.x, vec.y, vec.z);
   }

   public void scale(float x, float y, float z) {
      MatrixStack.Entry entry = this.stack.getLast();
      entry.positionMatrix.scale(x, y, z);
      if (Math.abs(x) == Math.abs(y) && Math.abs(y) == Math.abs(z)) {
         if (x < 0.0F || y < 0.0F || z < 0.0F) {
            entry.normalMatrix.scale(Math.signum(x), Math.signum(y), Math.signum(z));
         }
      } else {
         entry.normalMatrix.scale(1.0F / x, 1.0F / y, 1.0F / z);
         entry.canSkipNormalization = false;
      }
   }

   public void multiply(Quaternionf quaternion) {
      MatrixStack.Entry entry = this.stack.getLast();
      entry.positionMatrix.rotate(quaternion);
      entry.normalMatrix.rotate(quaternion);
   }

   public void multiply(Quaternionf quaternion, float originX, float originY, float originZ) {
      MatrixStack.Entry entry = this.stack.getLast();
      entry.positionMatrix.rotateAround(quaternion, originX, originY, originZ);
      entry.normalMatrix.rotate(quaternion);
   }

   public void push() {
      this.stack.addLast(new MatrixStack.Entry(this.stack.getLast()));
   }

   public void pop() {
      this.stack.removeLast();
   }

   public MatrixStack.Entry peek() {
      return this.stack.getLast();
   }

   public boolean isEmpty() {
      return this.stack.size() == 1;
   }

   public void loadIdentity() {
      MatrixStack.Entry entry = this.stack.getLast();
      entry.positionMatrix.identity();
      entry.normalMatrix.identity();
      entry.canSkipNormalization = true;
   }

   public void multiplyPositionMatrix(Matrix4f matrix) {
      MatrixStack.Entry entry = this.stack.getLast();
      entry.positionMatrix.mul(matrix);
      if (!MatrixUtil.isTranslation(matrix)) {
         if (MatrixUtil.isOrthonormal(matrix)) {
            entry.normalMatrix.mul(new Matrix3f(matrix));
         } else {
            entry.computeNormal();
         }
      }
   }

   public static final class Entry {
      final Matrix4f positionMatrix;
      final Matrix3f normalMatrix;
      public boolean canSkipNormalization = true;

      Entry(Matrix4f positionMatrix, Matrix3f normalMatrix) {
         this.positionMatrix = positionMatrix;
         this.normalMatrix = normalMatrix;
      }

      Entry(MatrixStack.Entry matrix) {
         this.positionMatrix = new Matrix4f(matrix.positionMatrix);
         this.normalMatrix = new Matrix3f(matrix.normalMatrix);
         this.canSkipNormalization = matrix.canSkipNormalization;
      }

      void computeNormal() {
         this.normalMatrix.set(this.positionMatrix).invert().transpose();
         this.canSkipNormalization = false;
      }

      public Matrix4f getPositionMatrix() {
         return this.positionMatrix;
      }

      public Matrix3f getNormalMatrix() {
         return this.normalMatrix;
      }

      public Vector3f transformNormal(Vector3f vec, Vector3f dest) {
         return this.transformNormal(vec.x, vec.y, vec.z, dest);
      }

      public Vector3f transformNormal(float x, float y, float z, Vector3f dest) {
         Vector3f vector3f = this.normalMatrix.transform(x, y, z, dest);
         return this.canSkipNormalization ? vector3f : vector3f.normalize();
      }

      public MatrixStack.Entry copy() {
         return new MatrixStack.Entry(this);
      }
   }
}
