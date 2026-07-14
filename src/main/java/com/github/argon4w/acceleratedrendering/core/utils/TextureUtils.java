package com.github.argon4w.acceleratedrendering.core.utils;

import com.github.argon4w.acceleratedrendering.core.CoreFeature;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.SynchronousResourceReloader;
import net.minecraft.util.Identifier;
import org.lwjgl.system.MemoryStack;

import static org.lwjgl.opengl.GL46.*;

public class TextureUtils implements SynchronousResourceReloader {

    private static final TextureUtils INSTANCE = new TextureUtils();
    private static final Object2ObjectLinkedOpenHashMap<Identifier, NativeImage> IMAGE_CACHE = new Object2ObjectLinkedOpenHashMap<>();

    public static TextureUtils getInstance() {
        return INSTANCE;
    }

    public static NativeImage downloadTexture(RenderLayer renderType, int mipmapLevel) {
        var textureResourceLocation = RenderTypeUtils.getTextureLocation(renderType);

        if (textureResourceLocation == null) {
            return null;
        }

        var image = IMAGE_CACHE.getAndMoveToFirst(textureResourceLocation);

        if (image != null) {
            return image;
        }

        MinecraftClient
                .getInstance()
                .getTextureManager()
                .getTexture(textureResourceLocation)
                .bindTexture();

        try (var stack = MemoryStack.stackPush()) {
            var widthBuffer = stack.callocInt(1);
            var heightBuffer = stack.callocInt(1);

            glGetTexLevelParameteriv(
                    GL_TEXTURE_2D,
                    mipmapLevel,
                    GL_TEXTURE_WIDTH,
                    widthBuffer
            );

            glGetTexLevelParameteriv(
                    GL_TEXTURE_2D,
                    mipmapLevel,
                    GL_TEXTURE_HEIGHT,
                    heightBuffer
            );

            var width = widthBuffer.get(0);
            var height = heightBuffer.get(0);

            if (width == 0 || height == 0) {
                return null;
            }

            var nativeImage = new NativeImage(
                    width,
                    height,
                    false
            );

            nativeImage.loadFromTextureImage(mipmapLevel, false);
            IMAGE_CACHE.putAndMoveToFirst(textureResourceLocation, nativeImage);

            if (IMAGE_CACHE.size() > CoreFeature.getCachedImageSize()) {
                IMAGE_CACHE
                        .removeLast()
                        .close();
            }

            return nativeImage;
        }
    }

    @Override
    public void reload(ResourceManager resourceManager) {
        IMAGE_CACHE.clear();
    }
}
