package net.minecraft.client.render;

import me.flashyreese.mods.sodiumextra.client.SodiumExtraClientMod;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import net.irisshaders.iris.mixin.LevelRendererAccessor;
import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.dimension.DimensionTypes;

public abstract class DimensionEffects {
   private static final Object2ObjectMap<Identifier, DimensionEffects> BY_IDENTIFIER = (Object2ObjectMap<Identifier, DimensionEffects>)Util.make(
      new Object2ObjectArrayMap(), map -> {
         DimensionEffects.Overworld overworld = new DimensionEffects.Overworld();
         map.defaultReturnValue(overworld);
         map.put(DimensionTypes.OVERWORLD_ID, overworld);
         map.put(DimensionTypes.THE_NETHER_ID, new DimensionEffects.Nether());
         map.put(DimensionTypes.THE_END_ID, new DimensionEffects.End());
      }
   );
   private final float cloudsHeight;
   private final boolean alternateSkyColor;
   private final DimensionEffects.SkyType skyType;
   private final boolean brightenLighting;
   private final boolean darkened;

   public DimensionEffects(float cloudsHeight, boolean alternateSkyColor, DimensionEffects.SkyType skyType, boolean brightenLighting, boolean darkened) {
      this.cloudsHeight = cloudsHeight;
      this.alternateSkyColor = alternateSkyColor;
      this.skyType = skyType;
      this.brightenLighting = brightenLighting;
      this.darkened = darkened;
   }

   public static DimensionEffects byDimensionType(DimensionType dimensionType) {
      return (DimensionEffects)BY_IDENTIFIER.get(dimensionType.effects());
   }

   public boolean isSunRisingOrSetting(float skyAngle) {
      return false;
   }

   public int getSkyColor(float skyAngle) {
      return 0;
   }

   public float getCloudsHeight() {
      return this.cloudsHeight;
   }

   public boolean isAlternateSkyColor() {
      return this.alternateSkyColor;
   }

   public abstract Vec3d adjustFogColor(Vec3d color, float sunHeight);

   public abstract boolean useThickFog(int camX, int camY);

   public DimensionEffects.SkyType getSkyType() {
      return this.skyType;
   }

   public boolean shouldBrightenLighting() {
      return this.brightenLighting;
   }

   public boolean isDarkened() {
      return this.darkened;
   }

   public static class End extends DimensionEffects {
      public End() {
         super(Float.NaN, false, DimensionEffects.SkyType.END, true, false);
      }

      @Override
      public Vec3d adjustFogColor(Vec3d color, float sunHeight) {
         return color.multiply(0.15F);
      }

      @Override
      public boolean useThickFog(int camX, int camY) {
         return false;
      }
   }

   public static class Nether extends DimensionEffects {
      public Nether() {
         super(Float.NaN, true, DimensionEffects.SkyType.NONE, false, true);
      }

      @Override
      public Vec3d adjustFogColor(Vec3d color, float sunHeight) {
         return color;
      }

      @Override
      public boolean useThickFog(int camX, int camY) {
         return true;
      }
   }

   public static class Overworld extends DimensionEffects {
      public static final int CLOUDS_HEIGHT = 192;
      private static final float field_53064 = 0.4F;

      public Overworld() {
         super(192.0F, true, DimensionEffects.SkyType.NORMAL, false, false);
      }

      @Override
      public float getCloudsHeight() {
         return SodiumExtraClientMod.isMixinEnabled("cloud.MixinDimensionEffectsOverworld")
            ? SodiumExtraClientMod.options().extraSettings.cloudHeight
            : 192.0F;
      }

      @Override
      public boolean isSunRisingOrSetting(float skyAngle) {
         float f = MathHelper.cos(skyAngle * (float) (Math.PI * 2));
         return f >= -0.4F && f <= 0.4F;
      }

      @Override
      public int getSkyColor(float skyAngle) {
         MinecraftClient client = MinecraftClient.getInstance();
         if (((LevelRendererAccessor)client.worldRenderer).invokeMethod_43788(client.gameRenderer.getCamera())
            || client.gameRenderer.getCamera().getSubmersionType() != CameraSubmersionType.NONE) {
            return 0;
         }

         float f = MathHelper.cos(skyAngle * (float) (Math.PI * 2));
         float g = f / 0.4F * 0.5F + 0.5F;
         float h = MathHelper.square(1.0F - (1.0F - MathHelper.sin(g * (float) Math.PI)) * 0.99F);
         return ColorHelper.fromFloats(h, g * 0.3F + 0.7F, g * g * 0.7F + 0.2F, 0.2F);
      }

      @Override
      public Vec3d adjustFogColor(Vec3d color, float sunHeight) {
         return color.multiply(sunHeight * 0.94F + 0.06F, sunHeight * 0.94F + 0.06F, sunHeight * 0.91F + 0.09F);
      }

      @Override
      public boolean useThickFog(int camX, int camY) {
         return false;
      }
   }

   public enum SkyType {
      NONE,
      NORMAL,
      END;
   }
}
