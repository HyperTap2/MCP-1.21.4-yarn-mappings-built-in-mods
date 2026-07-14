package com.github.argon4w.acceleratedrendering.features.items;

import lombok.Getter;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.random.Random;

@Getter
public class AcceleratedItemRenderContext {

    private final ItemStack itemStack;
    private final BakedModel bakedModel;
    private final Random random;
    private final int[] tints;

    public AcceleratedItemRenderContext(
            ItemStack itemStack,
            BakedModel bakedModel,
            Random random,
            int[] tints
    ) {
        this.itemStack = itemStack;
        this.bakedModel = bakedModel;
        this.random = random;
        this.tints = tints;
    }

    public int getTint(int index) {
        return index >= 0 && index < tints.length ? tints[index] : -1;
    }

    public AcceleratedItemRenderContext withBakedModel(BakedModel model) {
        return new AcceleratedItemRenderContext(this.itemStack, model, this.random, this.tints);
    }
}
