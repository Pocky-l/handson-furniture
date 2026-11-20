package com.pockyl.handson_furniture.mixin;

import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mrcrayfish.furniture.refurbished.block.FridgeBlock;
import com.mrcrayfish.furniture.refurbished.block.MicrowaveBlock;
import com.mrcrayfish.furniture.refurbished.block.StoveBlock;

import com.pockyl.handson_furniture.FurnitureLight;

/**
 * Refurbished's fridge (and the freezer, made the same way, which stays dark), microwave and stove get their lamps' light in
 * the world as they are made ({@link FurnitureLight}): a block's states are made with it, and set before any chunk holds them.
 * Done here rather than at the mod's setup, which on Fabric may come before Refurbished registers its blocks.
 */
@Mixin({FridgeBlock.class, MicrowaveBlock.class, StoveBlock.class})
abstract class RefurbishedLightMixin {
    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void handson$light(CallbackInfo info) {
        FurnitureLight.apply((Block) (Object) this);
    }
}
