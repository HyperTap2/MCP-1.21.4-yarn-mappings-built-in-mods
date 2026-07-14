package net.minecraft.client.font;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mojang.logging.LogUtils;
import com.viaversion.viafabricplus.ViaFabricPlus;
import com.viaversion.viafabricplus.features.font.filter_non_existing_characters.RenderableGlyphDiff;
import com.viaversion.viafabricplus.features.font.replace_blank_glyph.BuiltinEmptyGlyph1_12_2;
import com.viaversion.viafabricplus.protocoltranslator.ProtocolTranslator;
import com.viaversion.viafabricplus.settings.impl.DebugSettings;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.IntFunction;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import org.slf4j.Logger;

public class FontStorage implements AutoCloseable {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Random RANDOM = Random.create();
   private static final float MAX_ADVANCE = 32.0F;
   private final TextureManager textureManager;
   private final Identifier id;
   private BakedGlyph blankBakedGlyph;
   private BakedGlyph blankBakedGlyph1_12_2;
   private BakedGlyph whiteRectangleBakedGlyph;
   private List<Font.FontFilterPair> allFonts = List.of();
   private List<Font> availableFonts = List.of();
   public final GlyphContainer<BakedGlyph> bakedGlyphCache = new GlyphContainer<>(BakedGlyph[]::new, BakedGlyph[][]::new);
   public final GlyphContainer<FontStorage.GlyphPair> glyphCache = new GlyphContainer<>(FontStorage.GlyphPair[]::new, FontStorage.GlyphPair[][]::new);
   private final Int2ObjectMap<IntList> charactersByWidth = new Int2ObjectOpenHashMap();
   private final List<GlyphAtlasTexture> glyphAtlases = Lists.newArrayList();
   private final IntFunction<FontStorage.GlyphPair> glyphFinder = this::findGlyph;
   private final IntFunction<BakedGlyph> glyphBaker = this::bake;

   public FontStorage(TextureManager textureManager, Identifier id) {
      this.textureManager = textureManager;
      this.id = id;
   }

   public void setFonts(List<Font.FontFilterPair> allFonts, Set<FontFilterType> activeFilters) {
      this.allFonts = allFonts;
      this.setActiveFilters(activeFilters);
   }

   public void setActiveFilters(Set<FontFilterType> activeFilters) {
      this.availableFonts = List.of();
      this.clear();
      this.availableFonts = this.applyFilters(this.allFonts, activeFilters);
   }

   private void clear() {
      this.closeGlyphAtlases();
      this.bakedGlyphCache.clear();
      this.glyphCache.clear();
      this.charactersByWidth.clear();
      this.blankBakedGlyph1_12_2 = BuiltinEmptyGlyph1_12_2.INSTANCE.bake(this::bake);
      this.blankBakedGlyph = BuiltinEmptyGlyph.MISSING.bake(this::bake);
      this.whiteRectangleBakedGlyph = BuiltinEmptyGlyph.WHITE.bake(this::bake);
   }

   private List<Font> applyFilters(List<Font.FontFilterPair> allFonts, Set<FontFilterType> activeFilters) {
      IntSet intSet = new IntOpenHashSet();
      List<Font> list = new ArrayList<>();

      for (Font.FontFilterPair fontFilterPair : allFonts) {
         if (fontFilterPair.filter().isAllowed(activeFilters)) {
            list.add(fontFilterPair.provider());
            intSet.addAll(fontFilterPair.provider().getProvidedGlyphs());
         }
      }

      Set<Font> set = Sets.newHashSet();
      intSet.forEach(codePoint -> {
         for (Font font : list) {
            Glyph glyph = font.getGlyph(codePoint);
            if (glyph != null) {
               set.add(font);
               if (glyph != BuiltinEmptyGlyph.MISSING) {
                  ((IntList)this.charactersByWidth.computeIfAbsent(MathHelper.ceil(glyph.getAdvance(false)), i -> new IntArrayList())).add(codePoint);
               }
               break;
            }
         }
      });
      return list.stream().filter(set::contains).toList();
   }

   @Override
   public void close() {
      this.closeGlyphAtlases();
   }

   private void closeGlyphAtlases() {
      for (GlyphAtlasTexture glyphAtlasTexture : this.glyphAtlases) {
         glyphAtlasTexture.close();
      }

      this.glyphAtlases.clear();
   }

   private static boolean isAdvanceInvalid(Glyph glyph) {
      float f = glyph.getAdvance(false);
      if (!(f < 0.0F) && !(f > 32.0F)) {
         float g = glyph.getAdvance(true);
         return g < 0.0F || g > 32.0F;
      } else {
         return true;
      }
   }

   private FontStorage.GlyphPair findGlyph(int codePoint) {
      Glyph glyph = null;

      for (Font font : this.availableFonts) {
         Glyph glyph2 = font.getGlyph(codePoint);
         if (glyph2 != null) {
            if (glyph == null) {
               glyph = glyph2;
            }

            if (!isAdvanceInvalid(glyph2)) {
               return this.applyProtocolGlyphRules(codePoint, new FontStorage.GlyphPair(glyph, glyph2));
            }
         }
      }

      FontStorage.GlyphPair glyphPair = glyph != null
         ? new FontStorage.GlyphPair(glyph, BuiltinEmptyGlyph.MISSING)
         : FontStorage.GlyphPair.MISSING;
      return this.applyProtocolGlyphRules(codePoint, glyphPair);
   }

   public Glyph getGlyph(int codePoint, boolean validateAdvance) {
      return this.glyphCache.computeIfAbsent(codePoint, this.glyphFinder).getGlyph(validateAdvance);
   }

   private BakedGlyph bake(int codePoint) {
      for (Font font : this.availableFonts) {
         Glyph glyph = font.getGlyph(codePoint);
         if (glyph != null) {
            BakedGlyph bakedGlyph = glyph.bake(this::bake);
            return this.shouldHideGlyph(codePoint) ? this.getProtocolBlankBakedGlyph() : bakedGlyph;
         }
      }

      LOGGER.warn("Couldn't find glyph for character {} (\\u{})", Character.toString(codePoint), String.format("%04x", codePoint));
      return this.getProtocolBlankBakedGlyph();
   }

   private FontStorage.GlyphPair applyProtocolGlyphRules(int codePoint, FontStorage.GlyphPair glyphPair) {
      if (this.shouldHideGlyph(codePoint)) {
         return this.getProtocolBlankGlyphPair();
      }

      if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_12_2)) {
         Glyph glyph = glyphPair.glyph();
         Glyph advanceValidatedGlyph = glyphPair.advanceValidatedGlyph();
         return new FontStorage.GlyphPair(
            glyph == BuiltinEmptyGlyph.MISSING ? BuiltinEmptyGlyph1_12_2.INSTANCE : glyph,
            advanceValidatedGlyph == BuiltinEmptyGlyph.MISSING ? BuiltinEmptyGlyph1_12_2.INSTANCE : advanceValidatedGlyph
         );
      }

      return glyphPair;
   }

   private boolean shouldHideGlyph(int codePoint) {
      return DebugSettings.INSTANCE.filterNonExistingGlyphs.getValue()
         && (this.id.equals(MinecraftClient.DEFAULT_FONT_ID) || this.id.equals(MinecraftClient.UNICODE_FONT_ID))
         && !RenderableGlyphDiff.isGlyphRenderable(codePoint);
   }

   private FontStorage.GlyphPair getProtocolBlankGlyphPair() {
      return ViaFabricPlus.getImpl().getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_21_2)
         ? new FontStorage.GlyphPair(BuiltinEmptyGlyph1_12_2.INSTANCE, BuiltinEmptyGlyph1_12_2.INSTANCE)
         : FontStorage.GlyphPair.MISSING;
   }

   private BakedGlyph getProtocolBlankBakedGlyph() {
      return ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_12_2)
         ? this.blankBakedGlyph1_12_2
         : this.blankBakedGlyph;
   }

   public BakedGlyph getBaked(int codePoint) {
      return this.bakedGlyphCache.computeIfAbsent(codePoint, this.glyphBaker);
   }

   private BakedGlyph bake(RenderableGlyph c) {
      for (GlyphAtlasTexture glyphAtlasTexture : this.glyphAtlases) {
         BakedGlyph bakedGlyph = glyphAtlasTexture.bake(c);
         if (bakedGlyph != null) {
            return bakedGlyph;
         }
      }

      Identifier identifier = this.id.withSuffixedPath("/" + this.glyphAtlases.size());
      boolean bl = c.hasColor();
      TextRenderLayerSet textRenderLayerSet = bl ? TextRenderLayerSet.of(identifier) : TextRenderLayerSet.ofIntensity(identifier);
      GlyphAtlasTexture glyphAtlasTexture2 = new GlyphAtlasTexture(textRenderLayerSet, bl);
      this.glyphAtlases.add(glyphAtlasTexture2);
      this.textureManager.registerTexture(identifier, glyphAtlasTexture2);
      BakedGlyph bakedGlyph2 = glyphAtlasTexture2.bake(c);
      return bakedGlyph2 == null ? this.blankBakedGlyph : bakedGlyph2;
   }

   public BakedGlyph getObfuscatedBakedGlyph(Glyph glyph) {
      IntList intList = (IntList)this.charactersByWidth.get(MathHelper.ceil(glyph.getAdvance(false)));
      return intList != null && !intList.isEmpty() ? this.getBaked(intList.getInt(RANDOM.nextInt(intList.size()))) : this.blankBakedGlyph;
   }

   public Identifier getId() {
      return this.id;
   }

   public BakedGlyph getRectangleBakedGlyph() {
      return this.whiteRectangleBakedGlyph;
   }

   record GlyphPair(Glyph glyph, Glyph advanceValidatedGlyph) {
      static final FontStorage.GlyphPair MISSING = new FontStorage.GlyphPair(BuiltinEmptyGlyph.MISSING, BuiltinEmptyGlyph.MISSING);

      Glyph getGlyph(boolean validateAdvance) {
         return validateAdvance ? this.advanceValidatedGlyph : this.glyph;
      }
   }
}
