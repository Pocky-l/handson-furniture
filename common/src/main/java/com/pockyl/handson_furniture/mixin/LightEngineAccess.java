package com.pockyl.handson_furniture.mixin;

import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.lighting.LightEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The chunk source a light engine lights: it tells the server's light from the client's. */
@Mixin(LightEngine.class)
public interface LightEngineAccess {
    @Accessor("chunkSource")
    LightChunkGetter handson$chunkSource();
}
