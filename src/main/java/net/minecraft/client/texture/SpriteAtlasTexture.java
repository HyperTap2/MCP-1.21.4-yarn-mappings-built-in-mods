package net.minecraft.client.texture;

import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Supplier;
import me.flashyreese.mods.sodiumextra.client.SodiumExtraClientMod;
import net.fabricmc.fabric.impl.renderer.SpriteFinderImpl;
import net.irisshaders.iris.pbr.texture.PBRAtlasHolder;
import net.irisshaders.iris.pbr.texture.TextureAtlasExtension;
import net.minecraft.util.Identifier;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

public class SpriteAtlasTexture extends AbstractTexture implements DynamicTexture, TextureTickListener, TextureAtlasExtension, SpriteFinderImpl.SpriteFinderAccess {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Map<Supplier<Boolean>, List<Identifier>> ANIMATION_SETTINGS = Map.of(
      () -> SodiumExtraClientMod.options().animationSettings.water,
      List.of(Identifier.ofVanilla("block/water_still"), Identifier.ofVanilla("block/water_flow")),
      () -> SodiumExtraClientMod.options().animationSettings.lava,
      List.of(Identifier.ofVanilla("block/lava_still"), Identifier.ofVanilla("block/lava_flow")),
      () -> SodiumExtraClientMod.options().animationSettings.portal,
      List.of(Identifier.ofVanilla("block/nether_portal")),
      () -> SodiumExtraClientMod.options().animationSettings.fire,
      List.of(
         Identifier.ofVanilla("block/fire_0"), Identifier.ofVanilla("block/fire_1"), Identifier.ofVanilla("block/soul_fire_0"),
         Identifier.ofVanilla("block/soul_fire_1"), Identifier.ofVanilla("block/campfire_fire"), Identifier.ofVanilla("block/campfire_log_lit"),
         Identifier.ofVanilla("block/soul_campfire_fire"), Identifier.ofVanilla("block/soul_campfire_log_lit")
      ),
      () -> SodiumExtraClientMod.options().animationSettings.blockAnimations,
      List.of(
         Identifier.ofVanilla("block/magma"), Identifier.ofVanilla("block/lantern"), Identifier.ofVanilla("block/sea_lantern"),
         Identifier.ofVanilla("block/soul_lantern"), Identifier.ofVanilla("block/kelp"), Identifier.ofVanilla("block/kelp_plant"),
         Identifier.ofVanilla("block/seagrass"), Identifier.ofVanilla("block/tall_seagrass_top"), Identifier.ofVanilla("block/tall_seagrass_bottom"),
         Identifier.ofVanilla("block/warped_stem"), Identifier.ofVanilla("block/crimson_stem"), Identifier.ofVanilla("block/blast_furnace_front_on"),
         Identifier.ofVanilla("block/smoker_front_on"), Identifier.ofVanilla("block/stonecutter_saw"), Identifier.ofVanilla("block/prismarine"),
         Identifier.ofVanilla("block/respawn_anchor_top"), Identifier.ofVanilla("entity/conduit/wind"), Identifier.ofVanilla("entity/conduit/wind_vertical")
      ),
      () -> SodiumExtraClientMod.options().animationSettings.sculkSensor,
      List.of(
         Identifier.ofVanilla("block/sculk"), Identifier.ofVanilla("block/sculk_catalyst_top_bloom"), Identifier.ofVanilla("block/sculk_catalyst_side_bloom"),
         Identifier.ofVanilla("block/sculk_shrieker_inner_top"), Identifier.ofVanilla("block/sculk_vein"),
         Identifier.ofVanilla("block/sculk_shrieker_can_summon_inner_top"), Identifier.ofVanilla("block/sculk_sensor_tendril_inactive"),
         Identifier.ofVanilla("block/sculk_sensor_tendril_active"), Identifier.ofVanilla("vibration")
      )
   );
   @Deprecated
   public static final Identifier BLOCK_ATLAS_TEXTURE = Identifier.ofVanilla("textures/atlas/blocks.png");
   @Deprecated
   public static final Identifier PARTICLE_ATLAS_TEXTURE = Identifier.ofVanilla("textures/atlas/particles.png");
   private List<SpriteContents> spritesToLoad = List.of();
   private List<Sprite.TickableAnimation> animatedSprites = List.of();
   private Map<Identifier, Sprite> sprites = Map.of();
   @Nullable
   private Sprite missingSprite;
   private final Identifier id;
   private final int maxTextureSize;
   private int width;
   private int height;
   private int mipLevel;
   @Nullable
   private PBRAtlasHolder iris$pbrHolder;
   @Nullable
   private SpriteFinderImpl fabric_spriteFinder;

   public SpriteAtlasTexture(Identifier id) {
      this.id = id;
      this.maxTextureSize = RenderSystem.maxSupportedTextureSize();
   }

   public void upload(SpriteLoader.StitchResult stitchResult) {
      LOGGER.info("Created: {}x{}x{} {}-atlas", new Object[]{stitchResult.width(), stitchResult.height(), stitchResult.mipLevel(), this.id});
      TextureUtil.prepareImage(this.getGlId(), stitchResult.mipLevel(), stitchResult.width(), stitchResult.height());
      this.width = stitchResult.width();
      this.height = stitchResult.height();
      this.mipLevel = stitchResult.mipLevel();
      this.clear();
      this.setFilter(false, this.mipLevel > 1);
      this.sprites = Map.copyOf(stitchResult.regions());
      this.missingSprite = this.sprites.get(MissingSprite.getMissingSpriteId());
      if (this.missingSprite == null) {
         throw new IllegalStateException("Atlas '" + this.id + "' (" + this.sprites.size() + " sprites) has no missing texture sprite");
      }

      List<SpriteContents> list = new ArrayList<>();
      List<Sprite.TickableAnimation> list2 = new ArrayList<>();

      for (Sprite sprite : stitchResult.regions().values()) {
         list.add(sprite.getContents());

         try {
            sprite.upload();
         } catch (Throwable throwable) {
            CrashReport crashReport = CrashReport.create(throwable, "Stitching texture atlas");
            CrashReportSection crashReportSection = crashReport.addElement("Texture being stitched together");
            crashReportSection.add("Atlas path", this.id);
            crashReportSection.add("Sprite", sprite);
            throw new CrashException(crashReport);
         }

         Sprite.TickableAnimation tickableAnimation = this.createAnimation(sprite);
         if (tickableAnimation != null) {
            list2.add(tickableAnimation);
         }
      }

      this.spritesToLoad = List.copyOf(list);
      this.animatedSprites = List.copyOf(list2);
      this.fabric_spriteFinder = null;
   }

   @Nullable
   private Sprite.TickableAnimation createAnimation(Sprite sprite) {
      Sprite.TickableAnimation animation = sprite.createAnimation();
      if (animation == null || SodiumExtraClientMod.isMixinEnabled("animation.MixinSpriteAtlasTexture")
         && !SodiumExtraClientMod.options().animationSettings.animation) {
         return null;
      }

      if (!SodiumExtraClientMod.isMixinEnabled("animation.MixinSpriteAtlasTexture")) {
         return animation;
      }

      Identifier id = sprite.getContents().getId();
      for (Entry<Supplier<Boolean>, List<Identifier>> setting : ANIMATION_SETTINGS.entrySet()) {
         if (setting.getValue().contains(id)) {
            return setting.getKey().get() ? animation : null;
         }
      }

      return animation;
   }

   @Override
   public void save(Identifier id, Path path) throws IOException {
      String string = id.toUnderscoreSeparatedString();
      TextureUtil.writeAsPNG(path, string, this.getGlId(), this.mipLevel, this.width, this.height);
      dumpAtlasInfos(path, string, this.sprites);
   }

   private static void dumpAtlasInfos(Path path, String id, Map<Identifier, Sprite> sprites) {
      Path path2 = path.resolve(id + ".txt");

      try (Writer writer = Files.newBufferedWriter(path2)) {
         for (Entry<Identifier, Sprite> entry : sprites.entrySet().stream().sorted(Entry.comparingByKey()).toList()) {
            Sprite sprite = entry.getValue();
            writer.write(
               String.format(
                  Locale.ROOT,
                  "%s\tx=%d\ty=%d\tw=%d\th=%d%n",
                  entry.getKey(),
                  sprite.getX(),
                  sprite.getY(),
                  sprite.getContents().getWidth(),
                  sprite.getContents().getHeight()
               )
            );
         }
      } catch (IOException iOException) {
         LOGGER.warn("Failed to write file {}", path2, iOException);
      }
   }

   public void tickAnimatedSprites() {
      this.bindTexture();

      for (Sprite.TickableAnimation tickableAnimation : this.animatedSprites) {
         tickableAnimation.tick();
      }

      if (this.iris$pbrHolder != null) {
         this.iris$pbrHolder.cycleAnimationFrames();
      }
   }

   @Override
   public void tick() {
      this.tickAnimatedSprites();
   }

   public Sprite getSprite(Identifier id) {
      Sprite sprite = this.sprites.getOrDefault(id, this.missingSprite);
      if (sprite == null) {
         throw new IllegalStateException("Tried to lookup sprite, but atlas is not initialized");
      } else {
         return sprite;
      }
   }

   public void clear() {
      this.spritesToLoad.forEach(SpriteContents::close);
      this.animatedSprites.forEach(Sprite.TickableAnimation::close);
      this.spritesToLoad = List.of();
      this.animatedSprites = List.of();
      this.sprites = Map.of();
      this.missingSprite = null;
   }

   public Identifier getId() {
      return this.id;
   }

   public int getMaxTextureSize() {
      return this.maxTextureSize;
   }

   public int getWidth() {
      return this.width;
   }

   public int getHeight() {
      return this.height;
   }

   public Map<Identifier, Sprite> getTexturesByName() {
      return this.sprites;
   }

   public int getMipLevel() {
      return this.mipLevel;
   }

   @Override
   public SpriteFinderImpl fabric_spriteFinder() {
      SpriteFinderImpl spriteFinder = this.fabric_spriteFinder;
      if (spriteFinder == null) {
         spriteFinder = new SpriteFinderImpl(this.sprites, this);
         this.fabric_spriteFinder = spriteFinder;
      }
      return spriteFinder;
   }

   @Nullable
   @Override
   public PBRAtlasHolder getPBRHolder() {
      return this.iris$pbrHolder;
   }

   @Override
   public PBRAtlasHolder getOrCreatePBRHolder() {
      if (this.iris$pbrHolder == null) {
         this.iris$pbrHolder = new PBRAtlasHolder();
      }

      return this.iris$pbrHolder;
   }
}
