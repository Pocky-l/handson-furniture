package com.pockyl.handson_furniture.mixin;

import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Sets a block state's own light. A state keeps it from when it is made; Refurbished's fridge, microwave and stove make theirs
 * dark in every state ({@link com.pockyl.handson_furniture.FurnitureLight}).
 */
@Mixin(BlockBehaviour.BlockStateBase.class)
public interface BlockStateLightAccess {
    @Mutable
    @Accessor("lightEmission")
    void handson$setLightEmission(int light);
}
