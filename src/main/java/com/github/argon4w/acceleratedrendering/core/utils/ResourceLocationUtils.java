package com.github.argon4w.acceleratedrendering.core.utils;

import com.github.argon4w.acceleratedrendering.AcceleratedRendering;
import net.minecraft.util.Identifier;

public class ResourceLocationUtils {

    public static Identifier create(String path) {
        return Identifier.of(AcceleratedRendering.MOD_ID, path);
    }
}
