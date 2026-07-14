package net.caffeinemc.mods.sodium.client.render.immediate.model;

import net.caffeinemc.mods.sodium.api.math.MatrixHelper;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.caffeinemc.mods.sodium.api.vertex.format.common.EntityVertex;
import net.minecraft.client.util.math.MatrixStack.Entry;
import net.minecraft.util.math.Direction;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;

public class EntityRenderer {
   private static final int NUM_CUBE_VERTICES = 8;
   private static final int NUM_CUBE_FACES = 6;
   private static final int NUM_FACE_VERTICES = 4;
   private static final int VERTEX_X1_Y1_Z1 = 0;
   private static final int VERTEX_X2_Y1_Z1 = 1;
   private static final int VERTEX_X2_Y2_Z1 = 2;
   private static final int VERTEX_X1_Y2_Z1 = 3;
   private static final int VERTEX_X1_Y1_Z2 = 4;
   private static final int VERTEX_X2_Y1_Z2 = 5;
   private static final int VERTEX_X2_Y2_Z2 = 6;
   private static final int VERTEX_X1_Y2_Z2 = 7;
   private static final Matrix3f lastMatrix = new Matrix3f();
   private static final int VERTEX_BUFFER_BYTES = 864;
   private static final Vector3f[] CUBE_CORNERS = new Vector3f[8];
   private static final int[][] CUBE_VERTICES = new int[6][];
   private static final Vector3f[][] VERTEX_POSITIONS = new Vector3f[6][4];
   private static final Vector3f[][] VERTEX_POSITIONS_MIRRORED = new Vector3f[6][4];
   private static final Vector2f[][] VERTEX_TEXTURES = new Vector2f[6][4];
   private static final Vector2f[][] VERTEX_TEXTURES_MIRRORED = new Vector2f[6][4];
   private static final int[] CUBE_NORMALS = new int[6];
   private static final int[] CUBE_NORMALS_MIRRORED = new int[6];

   public static void renderCuboid(Entry matrices, VertexBufferWriter writer, ModelCuboid cuboid, int light, int overlay, int color) {
      prepareNormalsIfChanged(matrices);
      prepareVertices(matrices, cuboid);
      MemoryStack stack = MemoryStack.stackPush();

      try {
         long vertexBuffer = stack.nmalloc(16, 864);
         int vertexCount = emitQuads(vertexBuffer, cuboid, color, overlay, light);
         if (vertexCount > 0) {
            writer.push(stack, vertexBuffer, vertexCount, EntityVertex.FORMAT);
         }
      } catch (Throwable var11) {
         if (stack != null) {
            try {
               stack.close();
            } catch (Throwable var10) {
               var11.addSuppressed(var10);
            }
         }

         throw var11;
      }

      if (stack != null) {
         stack.close();
      }
   }

   private static int emitQuads(long buffer, ModelCuboid cuboid, int color, int overlay, int light) {
      Vector3f[][] positions = cuboid.mirror ? VERTEX_POSITIONS_MIRRORED : VERTEX_POSITIONS;
      Vector2f[][] textures = cuboid.mirror ? VERTEX_TEXTURES_MIRRORED : VERTEX_TEXTURES;
      int[] normals = cuboid.mirror ? CUBE_NORMALS_MIRRORED : CUBE_NORMALS;
      int vertexCount = 0;
      long ptr = buffer;

      for (int quadIndex = 0; quadIndex < 6; quadIndex++) {
         if (cuboid.shouldDrawFace(quadIndex)) {
            emitVertex(ptr, positions[quadIndex][0], color, textures[quadIndex][0], overlay, light, normals[quadIndex]);
            ptr += 36L;
            emitVertex(ptr, positions[quadIndex][1], color, textures[quadIndex][1], overlay, light, normals[quadIndex]);
            ptr += 36L;
            emitVertex(ptr, positions[quadIndex][2], color, textures[quadIndex][2], overlay, light, normals[quadIndex]);
            ptr += 36L;
            emitVertex(ptr, positions[quadIndex][3], color, textures[quadIndex][3], overlay, light, normals[quadIndex]);
            ptr += 36L;
            vertexCount += 4;
         }
      }

      return vertexCount;
   }

   private static void emitVertex(long ptr, Vector3f pos, int color, Vector2f tex, int overlay, int light, int normal) {
      EntityVertex.write(ptr, pos.x, pos.y, pos.z, color, tex.x, tex.y, overlay, light, normal);
   }

   private static void prepareVertices(Entry matrices, ModelCuboid cuboid) {
      buildVertexPosition(CUBE_CORNERS[0], cuboid.x1, cuboid.y1, cuboid.z1, matrices.getPositionMatrix());
      buildVertexPosition(CUBE_CORNERS[1], cuboid.x2, cuboid.y1, cuboid.z1, matrices.getPositionMatrix());
      buildVertexPosition(CUBE_CORNERS[2], cuboid.x2, cuboid.y2, cuboid.z1, matrices.getPositionMatrix());
      buildVertexPosition(CUBE_CORNERS[3], cuboid.x1, cuboid.y2, cuboid.z1, matrices.getPositionMatrix());
      buildVertexPosition(CUBE_CORNERS[4], cuboid.x1, cuboid.y1, cuboid.z2, matrices.getPositionMatrix());
      buildVertexPosition(CUBE_CORNERS[5], cuboid.x2, cuboid.y1, cuboid.z2, matrices.getPositionMatrix());
      buildVertexPosition(CUBE_CORNERS[6], cuboid.x2, cuboid.y2, cuboid.z2, matrices.getPositionMatrix());
      buildVertexPosition(CUBE_CORNERS[7], cuboid.x1, cuboid.y2, cuboid.z2, matrices.getPositionMatrix());
      buildVertexTexCoord(VERTEX_TEXTURES[0], cuboid.u1, cuboid.v0, cuboid.u2, cuboid.v1);
      buildVertexTexCoord(VERTEX_TEXTURES[1], cuboid.u2, cuboid.v1, cuboid.u3, cuboid.v0);
      buildVertexTexCoord(VERTEX_TEXTURES[3], cuboid.u1, cuboid.v1, cuboid.u2, cuboid.v2);
      buildVertexTexCoord(VERTEX_TEXTURES[5], cuboid.u4, cuboid.v1, cuboid.u5, cuboid.v2);
      buildVertexTexCoord(VERTEX_TEXTURES[2], cuboid.u2, cuboid.v1, cuboid.u4, cuboid.v2);
      buildVertexTexCoord(VERTEX_TEXTURES[4], cuboid.u0, cuboid.v1, cuboid.u1, cuboid.v2);
   }

   public static void prepareNormalsIfChanged(Entry matrices) {
      if (!matrices.getNormalMatrix().equals(lastMatrix)) {
         lastMatrix.set(matrices.getNormalMatrix());
         CUBE_NORMALS[0] = MatrixHelper.transformNormal(matrices.getNormalMatrix(), matrices.canSkipNormalization, Direction.DOWN);
         CUBE_NORMALS[1] = MatrixHelper.transformNormal(matrices.getNormalMatrix(), matrices.canSkipNormalization, Direction.UP);
         CUBE_NORMALS[3] = MatrixHelper.transformNormal(matrices.getNormalMatrix(), matrices.canSkipNormalization, Direction.NORTH);
         CUBE_NORMALS[5] = MatrixHelper.transformNormal(matrices.getNormalMatrix(), matrices.canSkipNormalization, Direction.SOUTH);
         CUBE_NORMALS[4] = MatrixHelper.transformNormal(matrices.getNormalMatrix(), matrices.canSkipNormalization, Direction.WEST);
         CUBE_NORMALS[2] = MatrixHelper.transformNormal(matrices.getNormalMatrix(), matrices.canSkipNormalization, Direction.EAST);
         CUBE_NORMALS_MIRRORED[0] = CUBE_NORMALS[0];
         CUBE_NORMALS_MIRRORED[1] = CUBE_NORMALS[1];
         CUBE_NORMALS_MIRRORED[3] = CUBE_NORMALS[3];
         CUBE_NORMALS_MIRRORED[5] = CUBE_NORMALS[5];
         CUBE_NORMALS_MIRRORED[4] = CUBE_NORMALS[2];
         CUBE_NORMALS_MIRRORED[2] = CUBE_NORMALS[4];
      }
   }

   private static void buildVertexPosition(Vector3f vector, float x, float y, float z, Matrix4f matrix) {
      vector.x = MatrixHelper.transformPositionX(matrix, x, y, z);
      vector.y = MatrixHelper.transformPositionY(matrix, x, y, z);
      vector.z = MatrixHelper.transformPositionZ(matrix, x, y, z);
   }

   private static void buildVertexTexCoord(Vector2f[] uvs, float u1, float v1, float u2, float v2) {
      uvs[0].set(u2, v1);
      uvs[1].set(u1, v1);
      uvs[2].set(u1, v2);
      uvs[3].set(u2, v2);
   }

   static {
      CUBE_VERTICES[0] = new int[]{5, 4, 0, 1};
      CUBE_VERTICES[1] = new int[]{2, 3, 7, 6};
      CUBE_VERTICES[3] = new int[]{1, 0, 3, 2};
      CUBE_VERTICES[5] = new int[]{4, 5, 6, 7};
      CUBE_VERTICES[2] = new int[]{5, 1, 2, 6};
      CUBE_VERTICES[4] = new int[]{0, 4, 7, 3};

      for (int cornerIndex = 0; cornerIndex < 8; cornerIndex++) {
         CUBE_CORNERS[cornerIndex] = new Vector3f();
      }

      for (int quadIndex = 0; quadIndex < 6; quadIndex++) {
         for (int vertexIndex = 0; vertexIndex < 4; vertexIndex++) {
            VERTEX_TEXTURES[quadIndex][vertexIndex] = new Vector2f();
            VERTEX_POSITIONS[quadIndex][vertexIndex] = CUBE_CORNERS[CUBE_VERTICES[quadIndex][vertexIndex]];
         }
      }

      for (int quadIndex = 0; quadIndex < 6; quadIndex++) {
         for (int vertexIndex = 0; vertexIndex < 4; vertexIndex++) {
            VERTEX_TEXTURES_MIRRORED[quadIndex][vertexIndex] = VERTEX_TEXTURES[quadIndex][3 - vertexIndex];
            VERTEX_POSITIONS_MIRRORED[quadIndex][vertexIndex] = VERTEX_POSITIONS[quadIndex][3 - vertexIndex];
         }
      }
   }
}
