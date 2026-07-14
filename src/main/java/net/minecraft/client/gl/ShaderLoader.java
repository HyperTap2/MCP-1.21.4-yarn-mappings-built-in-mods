package net.minecraft.client.gl;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import java.io.IOException;
import java.io.Reader;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.caffeinemc.mods.sodium.client.render.immediate.CloudRenderer;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.compat.SkipList;
import net.irisshaders.iris.mixinterface.ShaderInstanceInterface;
import net.irisshaders.iris.pathways.HandRenderer;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.pipeline.programs.ShaderAccess;
import net.irisshaders.iris.pipeline.programs.ShaderKey;
import net.irisshaders.iris.pipeline.programs.ShaderOverrides;
import net.irisshaders.iris.shadows.ShadowRenderingState;
import net.irisshaders.iris.vertices.ImmediateState;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceFactory;
import net.minecraft.resource.ResourceFinder;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.SinglePreparationResourceReloader;
import net.minecraft.util.Identifier;
import net.minecraft.util.InvalidIdentifierException;
import net.minecraft.util.PathUtil;
import net.minecraft.util.profiler.Profiler;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

public class ShaderLoader extends SinglePreparationResourceReloader<ShaderLoader.Definitions> implements AutoCloseable {
   static final Logger LOGGER = LogUtils.getLogger();
   public static final String SHADERS_PATH = "shaders";
   public static final String INCLUDE_PATH = "shaders/include/";
   private static final ResourceFinder SHADERS_FINDER = ResourceFinder.json("shaders");
   private static final ResourceFinder POST_EFFECT_FINDER = ResourceFinder.json("post_effect");
   public static final int field_53936 = 32768;
   final TextureManager textureManager;
   private final Consumer<Exception> onError;
   private ShaderLoader.Cache cache = new ShaderLoader.Cache(ShaderLoader.Definitions.EMPTY);
   private static final Function<IrisRenderingPipeline, ShaderKey> IRIS_FAKE_FUNCTION = pipeline -> null;
   private static final Map<ShaderProgramKey, Function<IrisRenderingPipeline, ShaderKey>> IRIS_CORE_SHADER_MAP = new Object2ObjectArrayMap<>();
   private static final Map<ShaderProgramKey, Function<IrisRenderingPipeline, ShaderKey>> IRIS_CORE_SHADER_MAP_SHADOW = new Object2ObjectArrayMap<>();
   private final Set<ShaderProgramKey> iris$missingShaders = new HashSet<>();

   public ShaderLoader(TextureManager textureManager, Consumer<Exception> onError) {
      this.textureManager = textureManager;
      this.onError = onError;
   }

   protected ShaderLoader.Definitions prepare(ResourceManager resourceManager, Profiler profiler) {
      Builder<Identifier, ShaderProgramDefinition> builder = ImmutableMap.builder();
      Builder<ShaderLoader.ShaderSourceKey, String> builder2 = ImmutableMap.builder();
      Map<Identifier, Resource> map = resourceManager.findResources("shaders", id -> isDefinition(id) || isShaderSource(id));

      for (Entry<Identifier, Resource> entry : map.entrySet()) {
         Identifier identifier = entry.getKey();
         CompiledShader.Type type = CompiledShader.Type.fromId(identifier);
         if (type != null) {
            loadShaderSource(identifier, entry.getValue(), type, map, builder2);
         } else if (isDefinition(identifier)) {
            loadDefinition(identifier, entry.getValue(), builder);
         }
      }

      Builder<Identifier, PostEffectPipeline> builder3 = ImmutableMap.builder();

      for (Entry<Identifier, Resource> entry2 : POST_EFFECT_FINDER.findResources(resourceManager).entrySet()) {
         loadPostEffect(entry2.getKey(), entry2.getValue(), builder3);
      }

      return new ShaderLoader.Definitions(builder.build(), builder2.build(), builder3.build());
   }

   private static void loadShaderSource(
      Identifier id, Resource resource, CompiledShader.Type type, Map<Identifier, Resource> allResources, Builder<ShaderLoader.ShaderSourceKey, String> builder
   ) {
      Identifier identifier = type.createFinder().toResourceId(id);
      GlImportProcessor glImportProcessor = createImportProcessor(allResources, id);

      try (Reader reader = resource.getReader()) {
         String string = IOUtils.toString(reader);
         builder.put(new ShaderLoader.ShaderSourceKey(identifier, type), String.join("", glImportProcessor.readSource(string)));
      } catch (IOException iOException) {
         LOGGER.error("Failed to load shader source at {}", id, iOException);
      }
   }

   private static GlImportProcessor createImportProcessor(Map<Identifier, Resource> allResources, Identifier id) {
      final Identifier identifier = id.withPath(PathUtil::getPosixFullPath);
      return new GlImportProcessor() {
         private final Set<Identifier> processed = new ObjectArraySet();

         @Override
         public String loadImport(boolean inline, String name) {
            Identifier identifierx;
            try {
               if (inline) {
                  identifierx = identifier.withPath(path -> PathUtil.normalizeToPosix(path + name));
               } else {
                  identifierx = Identifier.of(name).withPrefixedPath("shaders/include/");
               }
            } catch (InvalidIdentifierException invalidIdentifierException) {
               ShaderLoader.LOGGER.error("Malformed GLSL import {}: {}", name, invalidIdentifierException.getMessage());
               return "#error " + invalidIdentifierException.getMessage();
            }

            if (!this.processed.add(identifierx)) {
               return null;
            }

            try (Reader reader = allResources.get(identifierx).getReader()) {
               return IOUtils.toString(reader);
            } catch (IOException iOException) {
               ShaderLoader.LOGGER.error("Could not open GLSL import {}: {}", identifierx, iOException.getMessage());
               return "#error " + iOException.getMessage();
            }
         }
      };
   }

   private static void loadDefinition(Identifier id, Resource resource, Builder<Identifier, ShaderProgramDefinition> builder) {
      Identifier identifier = SHADERS_FINDER.toResourceId(id);

      try (Reader reader = resource.getReader()) {
         JsonElement jsonElement = JsonParser.parseReader(reader);
         ShaderProgramDefinition shaderProgramDefinition = (ShaderProgramDefinition)ShaderProgramDefinition.CODEC
            .parse(JsonOps.INSTANCE, jsonElement)
            .getOrThrow(JsonSyntaxException::new);
         builder.put(identifier, shaderProgramDefinition);
      } catch (IOException | JsonParseException exception) {
         LOGGER.error("Failed to parse shader config at {}", id, exception);
      }
   }

   private static void loadPostEffect(Identifier id, Resource resource, Builder<Identifier, PostEffectPipeline> builder) {
      Identifier identifier = POST_EFFECT_FINDER.toResourceId(id);

      try (Reader reader = resource.getReader()) {
         JsonElement jsonElement = JsonParser.parseReader(reader);
         builder.put(identifier, (PostEffectPipeline)PostEffectPipeline.CODEC.parse(JsonOps.INSTANCE, jsonElement).getOrThrow(JsonSyntaxException::new));
      } catch (IOException | JsonParseException exception) {
         LOGGER.error("Failed to parse post chain at {}", id, exception);
      }
   }

   private static boolean isDefinition(Identifier id) {
      return id.getPath().endsWith(".json");
   }

   private static boolean isShaderSource(Identifier id) {
      return CompiledShader.Type.fromId(id) != null || id.getPath().endsWith(".glsl");
   }

   protected void apply(ShaderLoader.Definitions definitions, ResourceManager resourceManager, Profiler profiler) {
      ShaderLoader.Cache cache = new ShaderLoader.Cache(definitions);
      Map<ShaderProgramKey, ShaderLoader.LoadException> map = new HashMap<>();
      Set<ShaderProgramKey> set = new HashSet<>(ShaderProgramKeys.getAll());

      for (PostEffectPipeline postEffectPipeline : definitions.postChains.values()) {
         for (PostEffectPipeline.Pass pass : postEffectPipeline.passes()) {
            set.add(pass.getShaderProgramKey());
         }
      }

      for (ShaderProgramKey shaderProgramKey : set) {
         try {
            cache.shaderPrograms.put(shaderProgramKey, Optional.of(cache.loadProgram(shaderProgramKey)));
         } catch (ShaderLoader.LoadException loadException) {
            map.put(shaderProgramKey, loadException);
         }
      }

      if (!map.isEmpty()) {
         cache.close();
         throw new RuntimeException(
            "Failed to load required shader programs:\n"
               + map.entrySet().stream().map(entry -> " - " + entry.getKey() + ": " + entry.getValue().getMessage()).collect(Collectors.joining("\n"))
         );
      }

      this.cache.close();
      this.cache = cache;
   }

   public String getName() {
      return "Shader Loader";
   }

   private void handleError(Exception exception) {
      if (!this.cache.errorHandled) {
         this.onError.accept(exception);
         this.cache.errorHandled = true;
      }
   }

   public void preload(ResourceFactory factory, ShaderProgramKey... keys) throws IOException, ShaderLoader.LoadException {
      for (ShaderProgramKey shaderProgramKey : keys) {
         Resource resource = factory.getResourceOrThrow(SHADERS_FINDER.toResourcePath(shaderProgramKey.configId()));

         try (Reader reader = resource.getReader()) {
            JsonElement jsonElement = JsonParser.parseReader(reader);
            ShaderProgramDefinition shaderProgramDefinition = (ShaderProgramDefinition)ShaderProgramDefinition.CODEC
               .parse(JsonOps.INSTANCE, jsonElement)
               .getOrThrow(JsonSyntaxException::new);
            Defines defines = shaderProgramDefinition.defines().withMerged(shaderProgramKey.defines());
            CompiledShader compiledShader = this.compileShader(factory, shaderProgramDefinition.vertex(), CompiledShader.Type.VERTEX, defines);
            CompiledShader compiledShader2 = this.compileShader(factory, shaderProgramDefinition.fragment(), CompiledShader.Type.FRAGMENT, defines);
            ShaderProgram shaderProgram = createProgram(shaderProgramKey, shaderProgramDefinition, compiledShader, compiledShader2);
            this.cache.shaderPrograms.put(shaderProgramKey, Optional.of(shaderProgram));
         }
      }
   }

   private CompiledShader compileShader(ResourceFactory factory, Identifier id, CompiledShader.Type type, Defines defines) throws IOException, ShaderLoader.LoadException {
      Identifier identifier = type.createFinder().toResourcePath(id);

      try (Reader reader = factory.getResourceOrThrow(identifier).getReader()) {
         String string = IOUtils.toString(reader);
         String string2 = GlImportProcessor.addDefines(string, defines);
         CompiledShader compiledShader = CompiledShader.compile(id, type, string2);
         this.cache.compiledShaders.put(new ShaderLoader.CompiledShaderKey(id, type, defines), compiledShader);
         return compiledShader;
      }
   }

   @Nullable
   public ShaderProgram getOrCreateProgram(ShaderProgramKey key) {
      ShaderProgram irisProgram = this.iris$getProgramOverride(key);
      if (irisProgram != null) {
         return irisProgram;
      }

      try {
         return this.cache.getOrLoadProgram(key);
      } catch (ShaderLoader.LoadException loadException) {
         LOGGER.error("Failed to load shader program: {}", key, loadException);
         this.cache.shaderPrograms.put(key, Optional.empty());
         this.handleError(loadException);
         return null;
      }
   }

   @Nullable
   private ShaderProgram iris$getProgramOverride(ShaderProgramKey shaderProgram) {
      if (Iris.getPipelineManager().getPipelineNullable() instanceof IrisRenderingPipeline irisPipeline
         && irisPipeline.shouldOverrideShaders()
         && !ImmediateState.bypass) {
         ShaderProgramKey newProgram = shaderProgram;
         if (newProgram == ShaderAccess.MEKANISM_FLAME) {
            ShaderProgram result = irisPipeline.getShaderMap()
               .getShader(ShadowRenderingState.areShadowsCurrentlyBeingRendered() ? ShaderKey.MEKANISM_FLAME_SHADOW : ShaderKey.MEKANISM_FLAME);
            if (result != null) {
               return result;
            }
         } else if (shaderProgram == ShaderAccess.MEKASUIT) {
            newProgram = ShaderProgramKeys.RENDERTYPE_ENTITY_CUTOUT;
         }

         ShaderProgram program = iris$override(irisPipeline, newProgram);
         if (program != null) {
            return program;
         }

         if (this.iris$missingShaders.add(shaderProgram)) {
            Iris.logger.error(
               "Missing program " + shaderProgram.configId() + " in override list. This is not a critical problem, but it could lead to weird rendering.",
               new Throwable()
            );
         }
      } else if (shaderProgram == ShaderAccess.MEKANISM_FLAME) {
         return this.getOrCreateProgram(ShaderProgramKeys.POSITION_TEX_COLOR);
      } else if (shaderProgram == ShaderAccess.MEKASUIT) {
         return this.getOrCreateProgram(ShaderProgramKeys.RENDERTYPE_ENTITY_CUTOUT);
      } else if (shaderProgram == ShaderAccess.TRANSLUCENT_PARTICLE_SHADER || shaderProgram == ShaderAccess.WEATHER_SHADER) {
         return this.getOrCreateProgram(ShaderProgramKeys.PARTICLE);
      }

      return null;
   }

   private static ShaderProgram iris$override(IrisRenderingPipeline pipeline, ShaderProgramKey shaderProgram) {
      ShaderKey shaderKey = iris$convertToShaderKey(pipeline, shaderProgram);
      return shaderKey == null ? null : pipeline.getShaderMap().getShader(shaderKey);
   }

   @Nullable
   private static ShaderKey iris$convertToShaderKey(IrisRenderingPipeline pipeline, ShaderProgramKey shaderProgram) {
      Function<IrisRenderingPipeline, ShaderKey> function = (ShadowRenderingState.areShadowsCurrentlyBeingRendered()
         ? IRIS_CORE_SHADER_MAP_SHADOW
         : IRIS_CORE_SHADER_MAP).getOrDefault(shaderProgram, IRIS_FAKE_FUNCTION);
      return function.apply(pipeline);
   }

   private static ShaderKey iris$getCutout(IrisRenderingPipeline pipeline) {
      if (HandRenderer.INSTANCE.isActive()) {
         return HandRenderer.INSTANCE.isRenderingSolid() ? ShaderKey.HAND_CUTOUT_DIFFUSE : ShaderKey.HAND_WATER_DIFFUSE;
      }

      return ShaderOverrides.isBlockEntities(pipeline) ? ShaderKey.BLOCK_ENTITY_DIFFUSE : ShaderKey.ENTITIES_CUTOUT_DIFFUSE;
   }

   private static ShaderKey iris$getSolid(IrisRenderingPipeline pipeline) {
      if (HandRenderer.INSTANCE.isActive()) {
         return HandRenderer.INSTANCE.isRenderingSolid() ? ShaderKey.HAND_CUTOUT : ShaderKey.HAND_TRANSLUCENT;
      }

      return ShaderOverrides.isBlockEntities(pipeline) ? ShaderKey.BLOCK_ENTITY : ShaderKey.ENTITIES_SOLID;
   }

   private static ShaderKey iris$getTranslucent(IrisRenderingPipeline pipeline) {
      if (HandRenderer.INSTANCE.isActive()) {
         return HandRenderer.INSTANCE.isRenderingSolid() ? ShaderKey.HAND_CUTOUT_DIFFUSE : ShaderKey.HAND_WATER_DIFFUSE;
      }

      return ShaderOverrides.isBlockEntities(pipeline) ? ShaderKey.BLOCK_ENTITY : ShaderKey.ENTITIES_TRANSLUCENT;
   }

   public ShaderProgram getProgramToLoad(ShaderProgramKey key) throws ShaderLoader.LoadException {
      ShaderProgram shaderProgram = this.cache.getOrLoadProgram(key);
      if (shaderProgram == null) {
         throw new ShaderLoader.LoadException("Shader '" + key + "' could not be found");
      } else {
         return shaderProgram;
      }
   }

   static ShaderProgram createProgram(ShaderProgramKey key, ShaderProgramDefinition definition, CompiledShader vertexShader, CompiledShader fragmentShader) throws ShaderLoader.LoadException {
      ShaderProgram shaderProgram = ShaderProgram.create(vertexShader, fragmentShader, key.vertexFormat());
      shaderProgram.set(definition.uniforms(), definition.samplers());
      MethodHandle shouldSkip = SkipList.shouldSkipList.computeIfAbsent(shaderProgram.getClass(), shaderClass -> {
         try {
            MethodHandle skipDraw = MethodHandles.lookup().findVirtual(shaderClass, "iris$skipDraw", MethodType.methodType(boolean.class));
            Iris.logger.warn("Class " + shaderClass.getName() + " has opted out of being rendered with shaders.");
            return skipDraw;
         } catch (NoSuchMethodException | IllegalAccessException exception) {
            return SkipList.NONE;
         }
      });
      if (Iris.getIrisConfig().shouldSkip(key.configId())) {
         shouldSkip = SkipList.ALWAYS;
      }

      ((ShaderInstanceInterface)shaderProgram).setShouldSkip(shouldSkip);
      return shaderProgram;
   }

   static {
      IRIS_CORE_SHADER_MAP.put(ShaderProgramKeys.POSITION, ShaderOverrides::getSkyShader);
      IRIS_CORE_SHADER_MAP.put(ShaderProgramKeys.POSITION_TEX, ShaderOverrides::getSkyTexShader);
      IRIS_CORE_SHADER_MAP.put(ShaderProgramKeys.POSITION_TEX_COLOR, ShaderOverrides::getSkyTexColorShader);
      IRIS_CORE_SHADER_MAP.put(ShaderProgramKeys.POSITION_COLOR, ShaderOverrides::getSkyColorShader);
      IRIS_CORE_SHADER_MAP.put(ShaderProgramKeys.PARTICLE, pipeline -> ShaderKey.PARTICLES);
      IRIS_CORE_SHADER_MAP.put(ShaderAccess.TRANSLUCENT_PARTICLE_SHADER, pipeline -> ShaderKey.PARTICLES_TRANS);
      IRIS_CORE_SHADER_MAP.put(ShaderAccess.WEATHER_SHADER, pipeline -> ShaderKey.WEATHER);
      IRIS_CORE_SHADER_MAP.put(ShaderProgramKeys.RENDERTYPE_ENTITY_CUTOUT, ShaderLoader::iris$getCutout);
      IRIS_CORE_SHADER_MAP.put(ShaderProgramKeys.RENDERTYPE_ENTITY_SOLID, ShaderLoader::iris$getSolid);
      IRIS_CORE_SHADER_MAP.put(ShaderProgramKeys.RENDERTYPE_ARMOR_CUTOUT_NO_CULL, ShaderLoader::iris$getCutout);
      IRIS_CORE_SHADER_MAP.put(ShaderProgramKeys.RENDERTYPE_ARMOR_TRANSLUCENT, ShaderLoader::iris$getTranslucent);
      IRIS_CORE_SHADER_MAP.put(ShaderProgramKeys.RENDERTYPE_GLINT, pipeline -> ShaderKey.GLINT);
      IRIS_CORE_SHADER_MAP.put(ShaderProgramKeys.RENDERTYPE_ENTITY_GLINT, pipeline -> ShaderKey.GLINT);
      IRIS_CORE_SHADER_MAP.put(ShaderProgramKeys.RENDERTYPE_GLINT_TRANSLUCENT, pipeline -> ShaderKey.GLINT);
      IRIS_CORE_SHADER_MAP.put(ShaderProgramKeys.RENDERTYPE_ARMOR_ENTITY_GLINT, pipeline -> ShaderKey.GLINT);
      IRIS_CORE_SHADER_MAP.put(ShaderProgramKeys.RENDERTYPE_ENTITY_CUTOUT_NO_CULL, ShaderLoader::iris$getCutout);
      IRIS_CORE_SHADER_MAP.put(ShaderProgramKeys.RENDERTYPE_ENTITY_CUTOUT_NO_CULL_Z_OFFSET, ShaderLoader::iris$getCutout);
      IRIS_CORE_SHADER_MAP.put(ShaderProgramKeys.RENDERTYPE_ENTITY_SHADOW, ShaderLoader::iris$getCutout);
      IRIS_CORE_SHADER_MAP.put(ShaderProgramKeys.RENDERTYPE_ENTITY_SMOOTH_CUTOUT, ShaderLoader::iris$getCutout);
      IRIS_CORE_SHADER_MAP.put(ShaderProgramKeys.RENDERTYPE_ENTITY_TRANSLUCENT, ShaderLoader::iris$getTranslucent);
      IRIS_CORE_SHADER_MAP.put(ShaderProgramKeys.RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE, pipeline -> ShaderKey.ENTITIES_EYES_TRANS);
      IRIS_CORE_SHADER_MAP.put(ShaderProgramKeys.RENDERTYPE_ENTITY_ALPHA, pipeline -> ShaderKey.ENTITIES_ALPHA);
      IRIS_CORE_SHADER_MAP.put(ShaderProgramKeys.RENDERTYPE_ENTITY_DECAL, pipeline -> ShaderKey.ENTITIES_CUTOUT_DIFFUSE);
      IRIS_CORE_SHADER_MAP.put(ShaderProgramKeys.RENDERTYPE_ITEM_ENTITY_TRANSLUCENT_CULL, ShaderLoader::iris$getTranslucent);
      IRIS_CORE_SHADER_MAP.put(
         ShaderProgramKeys.RENDERTYPE_SOLID,
         pipeline -> ShaderOverrides.isBlockEntities(pipeline) ? ShaderKey.MOVING_BLOCK : ShaderKey.TERRAIN_SOLID
      );
      IRIS_CORE_SHADER_MAP.put(
         ShaderProgramKeys.RENDERTYPE_CUTOUT,
         pipeline -> ShaderOverrides.isBlockEntities(pipeline) ? ShaderKey.MOVING_BLOCK : ShaderKey.TERRAIN_CUTOUT
      );
      IRIS_CORE_SHADER_MAP.put(
         ShaderProgramKeys.RENDERTYPE_CUTOUT_MIPPED,
         pipeline -> ShaderOverrides.isBlockEntities(pipeline) ? ShaderKey.MOVING_BLOCK : ShaderKey.TERRAIN_CUTOUT
      );
      IRIS_CORE_SHADER_MAP.put(
         ShaderProgramKeys.RENDERTYPE_TRANSLUCENT,
         pipeline -> ShaderOverrides.isBlockEntities(pipeline) ? ShaderKey.MOVING_BLOCK : ShaderKey.TERRAIN_TRANSLUCENT
      );
      IRIS_CORE_SHADER_MAP.put(ShaderProgramKeys.RENDERTYPE_TRIPWIRE, pipeline -> ShaderKey.TERRAIN_TRANSLUCENT);
      IRIS_CORE_SHADER_MAP.put(ShaderProgramKeys.RENDERTYPE_LINES, pipeline -> ShaderKey.LINES);
      IRIS_CORE_SHADER_MAP.put(ShaderProgramKeys.RENDERTYPE_TEXT_BACKGROUND, pipeline -> ShaderKey.TEXT_BG);
      IRIS_CORE_SHADER_MAP.put(ShaderProgramKeys.RENDERTYPE_TEXT_BACKGROUND_SEE_THROUGH, pipeline -> ShaderKey.TEXT_BG);
      IRIS_CORE_SHADER_MAP.put(
         ShaderProgramKeys.RENDERTYPE_TEXT,
         pipeline -> ShaderOverrides.isBlockEntities(pipeline) ? ShaderKey.TEXT_BE : ShaderKey.TEXT
      );
      IRIS_CORE_SHADER_MAP.put(
         ShaderProgramKeys.RENDERTYPE_TEXT_SEE_THROUGH,
         pipeline -> ShaderOverrides.isBlockEntities(pipeline) ? ShaderKey.TEXT_BE : ShaderKey.TEXT
      );
      IRIS_CORE_SHADER_MAP.put(
         ShaderProgramKeys.RENDERTYPE_TEXT_INTENSITY,
         pipeline -> ShaderOverrides.isBlockEntities(pipeline) ? ShaderKey.TEXT_INTENSITY_BE : ShaderKey.TEXT_INTENSITY
      );
      IRIS_CORE_SHADER_MAP.put(
         ShaderProgramKeys.RENDERTYPE_TEXT_INTENSITY_SEE_THROUGH,
         pipeline -> ShaderOverrides.isBlockEntities(pipeline) ? ShaderKey.TEXT_INTENSITY_BE : ShaderKey.TEXT_INTENSITY
      );
      IRIS_CORE_SHADER_MAP.put(ShaderProgramKeys.RENDERTYPE_EYES, pipeline -> ShaderKey.ENTITIES_EYES);
      IRIS_CORE_SHADER_MAP.put(ShaderProgramKeys.RENDERTYPE_ENTITY_NO_OUTLINE, ShaderLoader::iris$getTranslucent);
      IRIS_CORE_SHADER_MAP.put(ShaderProgramKeys.RENDERTYPE_BREEZE_WIND, ShaderLoader::iris$getTranslucent);
      IRIS_CORE_SHADER_MAP.put(ShaderProgramKeys.RENDERTYPE_ENERGY_SWIRL, pipeline -> ShaderKey.ENTITIES_CUTOUT);
      IRIS_CORE_SHADER_MAP.put(ShaderProgramKeys.RENDERTYPE_BEACON_BEAM, pipeline -> ShaderKey.BEACON);
      IRIS_CORE_SHADER_MAP.put(ShaderProgramKeys.RENDERTYPE_LIGHTNING, pipeline -> ShaderKey.LIGHTNING);
      IRIS_CORE_SHADER_MAP.put(ShaderProgramKeys.RENDERTYPE_END_PORTAL, ShaderLoader::iris$getCutout);
      IRIS_CORE_SHADER_MAP.put(ShaderProgramKeys.RENDERTYPE_END_GATEWAY, ShaderLoader::iris$getCutout);
      IRIS_CORE_SHADER_MAP.put(ShaderProgramKeys.RENDERTYPE_LEASH, pipeline -> ShaderKey.LEASH);
      IRIS_CORE_SHADER_MAP.put(ShaderProgramKeys.RENDERTYPE_WATER_MASK, pipeline -> ShaderKey.ENTITIES_SOLID);
      IRIS_CORE_SHADER_MAP.put(ShaderProgramKeys.RENDERTYPE_CLOUDS, pipeline -> ShaderKey.CLOUDS);
      IRIS_CORE_SHADER_MAP.put(ShaderProgramKeys.RENDERTYPE_CRUMBLING, pipeline -> ShaderKey.CRUMBLING);
      IRIS_CORE_SHADER_MAP.put(CloudRenderer.CLOUDS_SHADER, pipeline -> ShaderKey.CLOUDS_SODIUM);
      IRIS_CORE_SHADER_MAP.put(ShaderProgramKeys.RENDERTYPE_TRANSLUCENT_MOVING_BLOCK, pipeline -> ShaderKey.MOVING_BLOCK);

      IRIS_CORE_SHADER_MAP_SHADOW.put(ShaderProgramKeys.POSITION, pipeline -> ShaderKey.SHADOW_BASIC);
      IRIS_CORE_SHADER_MAP_SHADOW.put(ShaderProgramKeys.POSITION_TEX, pipeline -> ShaderKey.SHADOW_TEX);
      IRIS_CORE_SHADER_MAP_SHADOW.put(ShaderProgramKeys.POSITION_TEX_COLOR, pipeline -> ShaderKey.SHADOW_TEX_COLOR);
      IRIS_CORE_SHADER_MAP_SHADOW.put(ShaderProgramKeys.POSITION_COLOR, pipeline -> ShaderKey.SHADOW_BASIC_COLOR);
      IRIS_CORE_SHADER_MAP_SHADOW.put(ShaderProgramKeys.PARTICLE, pipeline -> ShaderKey.SHADOW_PARTICLES);
      IRIS_CORE_SHADER_MAP_SHADOW.put(ShaderAccess.TRANSLUCENT_PARTICLE_SHADER, pipeline -> ShaderKey.SHADOW_PARTICLES);
      IRIS_CORE_SHADER_MAP_SHADOW.put(ShaderAccess.WEATHER_SHADER, pipeline -> ShaderKey.SHADOW_PARTICLES);
      IRIS_CORE_SHADER_MAP_SHADOW.put(ShaderProgramKeys.RENDERTYPE_ENTITY_CUTOUT, pipeline -> ShaderKey.SHADOW_ENTITIES_CUTOUT);
      IRIS_CORE_SHADER_MAP_SHADOW.put(ShaderProgramKeys.RENDERTYPE_ENTITY_SOLID, pipeline -> ShaderKey.SHADOW_ENTITIES_CUTOUT);
      IRIS_CORE_SHADER_MAP_SHADOW.put(ShaderProgramKeys.RENDERTYPE_ARMOR_CUTOUT_NO_CULL, pipeline -> ShaderKey.SHADOW_ENTITIES_CUTOUT);
      IRIS_CORE_SHADER_MAP_SHADOW.put(ShaderProgramKeys.RENDERTYPE_GLINT, pipeline -> ShaderKey.SHADOW_ENTITIES_CUTOUT);
      IRIS_CORE_SHADER_MAP_SHADOW.put(ShaderProgramKeys.RENDERTYPE_ENTITY_GLINT, pipeline -> ShaderKey.SHADOW_ENTITIES_CUTOUT);
      IRIS_CORE_SHADER_MAP_SHADOW.put(ShaderProgramKeys.RENDERTYPE_GLINT_TRANSLUCENT, pipeline -> ShaderKey.SHADOW_ENTITIES_CUTOUT);
      IRIS_CORE_SHADER_MAP_SHADOW.put(ShaderProgramKeys.RENDERTYPE_ARMOR_ENTITY_GLINT, pipeline -> ShaderKey.SHADOW_ENTITIES_CUTOUT);
      IRIS_CORE_SHADER_MAP_SHADOW.put(ShaderProgramKeys.RENDERTYPE_ENTITY_CUTOUT_NO_CULL, pipeline -> ShaderKey.SHADOW_ENTITIES_CUTOUT);
      IRIS_CORE_SHADER_MAP_SHADOW.put(
         ShaderProgramKeys.RENDERTYPE_ENTITY_CUTOUT_NO_CULL_Z_OFFSET, pipeline -> ShaderKey.SHADOW_ENTITIES_CUTOUT
      );
      IRIS_CORE_SHADER_MAP_SHADOW.put(ShaderProgramKeys.RENDERTYPE_ENTITY_SMOOTH_CUTOUT, pipeline -> ShaderKey.SHADOW_ENTITIES_CUTOUT);
      IRIS_CORE_SHADER_MAP_SHADOW.put(ShaderProgramKeys.RENDERTYPE_ENTITY_SHADOW, pipeline -> ShaderKey.SHADOW_ENTITIES_CUTOUT);
      IRIS_CORE_SHADER_MAP_SHADOW.put(ShaderProgramKeys.RENDERTYPE_ENTITY_TRANSLUCENT, pipeline -> ShaderKey.SHADOW_ENTITIES_CUTOUT);
      IRIS_CORE_SHADER_MAP_SHADOW.put(
         ShaderProgramKeys.RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE, pipeline -> ShaderKey.SHADOW_ENTITIES_CUTOUT
      );
      IRIS_CORE_SHADER_MAP_SHADOW.put(ShaderProgramKeys.RENDERTYPE_ENTITY_DECAL, pipeline -> ShaderKey.SHADOW_ENTITIES_CUTOUT);
      IRIS_CORE_SHADER_MAP_SHADOW.put(ShaderProgramKeys.RENDERTYPE_ENTITY_ALPHA, pipeline -> ShaderKey.SHADOW_ENTITIES_CUTOUT);
      IRIS_CORE_SHADER_MAP_SHADOW.put(
         ShaderProgramKeys.RENDERTYPE_ITEM_ENTITY_TRANSLUCENT_CULL, pipeline -> ShaderKey.SHADOW_ENTITIES_CUTOUT
      );
      IRIS_CORE_SHADER_MAP_SHADOW.put(ShaderProgramKeys.RENDERTYPE_SOLID, pipeline -> ShaderKey.SHADOW_TERRAIN_CUTOUT);
      IRIS_CORE_SHADER_MAP_SHADOW.put(ShaderProgramKeys.RENDERTYPE_CUTOUT, pipeline -> ShaderKey.SHADOW_TERRAIN_CUTOUT);
      IRIS_CORE_SHADER_MAP_SHADOW.put(ShaderProgramKeys.RENDERTYPE_CUTOUT_MIPPED, pipeline -> ShaderKey.SHADOW_TERRAIN_CUTOUT);
      IRIS_CORE_SHADER_MAP_SHADOW.put(
         ShaderProgramKeys.RENDERTYPE_TRANSLUCENT,
         pipeline -> ShaderOverrides.isBlockEntities(pipeline) ? ShaderKey.SHADOW_ENTITIES_CUTOUT : ShaderKey.SHADOW_TERRAIN_CUTOUT
      );
      IRIS_CORE_SHADER_MAP_SHADOW.put(ShaderProgramKeys.RENDERTYPE_TRIPWIRE, pipeline -> ShaderKey.SHADOW_TERRAIN_CUTOUT);
      IRIS_CORE_SHADER_MAP_SHADOW.put(ShaderProgramKeys.RENDERTYPE_ARMOR_TRANSLUCENT, pipeline -> ShaderKey.SHADOW_ENTITIES_CUTOUT);
      IRIS_CORE_SHADER_MAP_SHADOW.put(ShaderProgramKeys.RENDERTYPE_LINES, pipeline -> ShaderKey.SHADOW_LINES);
      IRIS_CORE_SHADER_MAP_SHADOW.put(ShaderProgramKeys.RENDERTYPE_TEXT_BACKGROUND, pipeline -> ShaderKey.SHADOW_TEXT_BG);
      IRIS_CORE_SHADER_MAP_SHADOW.put(ShaderProgramKeys.RENDERTYPE_TEXT_BACKGROUND_SEE_THROUGH, pipeline -> ShaderKey.SHADOW_TEXT_BG);
      IRIS_CORE_SHADER_MAP_SHADOW.put(ShaderProgramKeys.RENDERTYPE_TEXT, pipeline -> ShaderKey.SHADOW_TEXT);
      IRIS_CORE_SHADER_MAP_SHADOW.put(ShaderProgramKeys.RENDERTYPE_TEXT_SEE_THROUGH, pipeline -> ShaderKey.SHADOW_TEXT);
      IRIS_CORE_SHADER_MAP_SHADOW.put(ShaderProgramKeys.RENDERTYPE_TEXT_INTENSITY, pipeline -> ShaderKey.SHADOW_TEXT_INTENSITY);
      IRIS_CORE_SHADER_MAP_SHADOW.put(
         ShaderProgramKeys.RENDERTYPE_TEXT_INTENSITY_SEE_THROUGH, pipeline -> ShaderKey.SHADOW_TEXT_INTENSITY
      );
      IRIS_CORE_SHADER_MAP_SHADOW.put(ShaderProgramKeys.RENDERTYPE_EYES, pipeline -> ShaderKey.SHADOW_ENTITIES_CUTOUT);
      IRIS_CORE_SHADER_MAP_SHADOW.put(ShaderProgramKeys.RENDERTYPE_ENTITY_NO_OUTLINE, pipeline -> ShaderKey.SHADOW_ENTITIES_CUTOUT);
      IRIS_CORE_SHADER_MAP_SHADOW.put(ShaderProgramKeys.RENDERTYPE_BREEZE_WIND, pipeline -> ShaderKey.SHADOW_ENTITIES_CUTOUT);
      IRIS_CORE_SHADER_MAP_SHADOW.put(ShaderProgramKeys.RENDERTYPE_ENERGY_SWIRL, pipeline -> ShaderKey.SHADOW_ENTITIES_CUTOUT);
      IRIS_CORE_SHADER_MAP_SHADOW.put(ShaderProgramKeys.RENDERTYPE_BEACON_BEAM, pipeline -> ShaderKey.SHADOW_BEACON_BEAM);
      IRIS_CORE_SHADER_MAP_SHADOW.put(ShaderProgramKeys.RENDERTYPE_LIGHTNING, pipeline -> ShaderKey.SHADOW_LIGHTNING);
      IRIS_CORE_SHADER_MAP_SHADOW.put(ShaderProgramKeys.RENDERTYPE_END_PORTAL, pipeline -> ShaderKey.SHADOW_ENTITIES_CUTOUT);
      IRIS_CORE_SHADER_MAP_SHADOW.put(ShaderProgramKeys.RENDERTYPE_END_GATEWAY, pipeline -> ShaderKey.SHADOW_ENTITIES_CUTOUT);
      IRIS_CORE_SHADER_MAP_SHADOW.put(ShaderProgramKeys.RENDERTYPE_LEASH, pipeline -> ShaderKey.SHADOW_LEASH);
      IRIS_CORE_SHADER_MAP_SHADOW.put(ShaderProgramKeys.RENDERTYPE_WATER_MASK, pipeline -> ShaderKey.SHADOW_ENTITIES_CUTOUT);
      IRIS_CORE_SHADER_MAP_SHADOW.put(ShaderProgramKeys.RENDERTYPE_CLOUDS, pipeline -> ShaderKey.SHADOW_CLOUDS);
      IRIS_CORE_SHADER_MAP_SHADOW.put(ShaderProgramKeys.RENDERTYPE_CRUMBLING, pipeline -> ShaderKey.SHADOW_BASIC);
      IRIS_CORE_SHADER_MAP_SHADOW.put(CloudRenderer.CLOUDS_SHADER, pipeline -> ShaderKey.SHADOW_CLOUDS);
      IRIS_CORE_SHADER_MAP_SHADOW.put(
         ShaderProgramKeys.RENDERTYPE_TRANSLUCENT_MOVING_BLOCK, pipeline -> ShaderKey.SHADOW_TERRAIN_CUTOUT
      );

      for (ShaderProgramKey program : ShaderProgramKeys.getAll()) {
         if (IRIS_CORE_SHADER_MAP.containsKey(program) && !IRIS_CORE_SHADER_MAP_SHADOW.containsKey(program)) {
            throw new IllegalStateException("Shader program " + program + " is not accounted for in the shadow list");
         }
      }
   }

   @Nullable
   public PostEffectProcessor loadPostEffect(Identifier id, Set<Identifier> availableExternalTargets) {
      try {
         return this.cache.getOrLoadProcessor(id, availableExternalTargets);
      } catch (ShaderLoader.LoadException loadException) {
         LOGGER.error("Failed to load post chain: {}", id, loadException);
         this.cache.postEffectProcessors.put(id, Optional.empty());
         this.handleError(loadException);
         return null;
      }
   }

   @Override
   public void close() {
      this.cache.close();
   }

   class Cache implements AutoCloseable {
      private final ShaderLoader.Definitions definitions;
      final Map<ShaderProgramKey, Optional<ShaderProgram>> shaderPrograms = new HashMap<>();
      final Map<ShaderLoader.CompiledShaderKey, CompiledShader> compiledShaders = new HashMap<>();
      final Map<Identifier, Optional<PostEffectProcessor>> postEffectProcessors = new HashMap<>();
      boolean errorHandled;

      Cache(final ShaderLoader.Definitions definitions) {
         this.definitions = definitions;
      }

      @Nullable
      public ShaderProgram getOrLoadProgram(ShaderProgramKey key) throws ShaderLoader.LoadException {
         Optional<ShaderProgram> optional = this.shaderPrograms.get(key);
         if (optional != null) {
            return optional.orElse(null);
         }

         ShaderProgram shaderProgram = this.loadProgram(key);
         this.shaderPrograms.put(key, Optional.of(shaderProgram));
         return shaderProgram;
      }

      ShaderProgram loadProgram(ShaderProgramKey key) throws ShaderLoader.LoadException {
         ShaderProgramDefinition shaderProgramDefinition = this.definitions.programs.get(key.configId());
         if (shaderProgramDefinition == null) {
            throw new ShaderLoader.LoadException("Could not find program with id: " + key.configId());
         }

         Defines defines = shaderProgramDefinition.defines().withMerged(key.defines());
         CompiledShader compiledShader = this.loadShader(shaderProgramDefinition.vertex(), CompiledShader.Type.VERTEX, defines);
         CompiledShader compiledShader2 = this.loadShader(shaderProgramDefinition.fragment(), CompiledShader.Type.FRAGMENT, defines);
         return ShaderLoader.createProgram(key, shaderProgramDefinition, compiledShader, compiledShader2);
      }

      private CompiledShader loadShader(Identifier id, CompiledShader.Type type, Defines defines) throws ShaderLoader.LoadException {
         ShaderLoader.CompiledShaderKey shaderKey = new ShaderLoader.CompiledShaderKey(id, type, defines);
         CompiledShader compiledShader = this.compiledShaders.get(shaderKey);
         if (compiledShader == null) {
            compiledShader = this.compileShader(shaderKey);
            this.compiledShaders.put(shaderKey, compiledShader);
         }

         return compiledShader;
      }

      private CompiledShader compileShader(ShaderLoader.CompiledShaderKey key) throws ShaderLoader.LoadException {
         String string = this.definitions.shaderSources.get(new ShaderLoader.ShaderSourceKey(key.id, key.type));
         if (string == null) {
            throw new ShaderLoader.LoadException("Could not find shader: " + key);
         }

         String string2 = GlImportProcessor.addDefines(string, key.defines);
         return CompiledShader.compile(key.id, key.type, string2);
      }

      @Nullable
      public PostEffectProcessor getOrLoadProcessor(Identifier id, Set<Identifier> availableExternalTargets) throws ShaderLoader.LoadException {
         Optional<PostEffectProcessor> optional = this.postEffectProcessors.get(id);
         if (optional != null) {
            return optional.orElse(null);
         }

         PostEffectProcessor postEffectProcessor = this.loadProcessor(id, availableExternalTargets);
         this.postEffectProcessors.put(id, Optional.of(postEffectProcessor));
         return postEffectProcessor;
      }

      private PostEffectProcessor loadProcessor(Identifier id, Set<Identifier> availableExternalTargets) throws ShaderLoader.LoadException {
         PostEffectPipeline postEffectPipeline = this.definitions.postChains.get(id);
         if (postEffectPipeline == null) {
            throw new ShaderLoader.LoadException("Could not find post chain with id: " + id);
         } else {
            return PostEffectProcessor.parseEffect(postEffectPipeline, ShaderLoader.this.textureManager, ShaderLoader.this, availableExternalTargets);
         }
      }

      @Override
      public void close() {
         RenderSystem.assertOnRenderThread();
         this.shaderPrograms.values().forEach(program -> program.ifPresent(ShaderProgram::close));
         this.compiledShaders.values().forEach(CompiledShader::close);
         this.shaderPrograms.clear();
         this.compiledShaders.clear();
         this.postEffectProcessors.clear();
      }
   }

   public record Definitions(
      Map<Identifier, ShaderProgramDefinition> programs,
      Map<ShaderLoader.ShaderSourceKey, String> shaderSources,
      Map<Identifier, PostEffectPipeline> postChains
   ) {
      public static final ShaderLoader.Definitions EMPTY = new ShaderLoader.Definitions(Map.of(), Map.of(), Map.of());
   }

   public static class LoadException extends Exception {
      public LoadException(String message) {
         super(message);
      }
   }

   record CompiledShaderKey(Identifier id, CompiledShader.Type type, Defines defines) {

      @Override
      public String toString() {
         String string = this.id + " (" + this.type + ")";
         return !this.defines.isEmpty() ? string + " with " + this.defines : string;
      }
   }

   record ShaderSourceKey(Identifier id, CompiledShader.Type type) {
      @Override
      public String toString() {
         return this.id + " (" + this.type + ")";
      }
   }
}
