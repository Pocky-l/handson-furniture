package com.pockyl.handson_furniture.mixin;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.BlockLightEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.pockyl.handson_furniture.FurnitureLight;

/**
 * A heating microwave lights the room through its glass door, though Refurbished's block has no state for it: the block light
 * engine takes its light from {@link FurnitureLight#heatingLight} where the block itself gives none.
 */
@Mixin(BlockLightEngine.class)
abstract class BlockLightEngineMixin {
    @Inject(method = "getEmission", at = @At("RETURN"), cancellable = true)
    private void handson$heating(long pos, BlockState state, CallbackInfoReturnable<Integer> info) {
        if (info.getReturnValueI() == 0) {
            int light = FurnitureLight.heatingLight(((LightEngineAccess) this).handson$chunkSource(), pos, state);
            if (light > 0) {
                info.setReturnValue(light);
            }
        }
    }
}
