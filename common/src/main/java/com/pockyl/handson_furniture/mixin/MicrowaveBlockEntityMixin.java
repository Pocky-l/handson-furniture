package com.pockyl.handson_furniture.mixin;

import com.mrcrayfish.furniture.refurbished.blockentity.MicrowaveBlockEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.pockyl.handson_furniture.station.ProcessingSlots;

/**
 * Refurbished's microwave heats its first slot into its second, ticked by its power network rather than as the freezer is:
 * before each of its ticks the slots are set out so that what heats is in the first ({@link ProcessingSlots}).
 */
@Mixin(MicrowaveBlockEntity.class)
abstract class MicrowaveBlockEntityMixin {
    @Inject(method = "moduleTick", at = @At("HEAD"), remap = false)
    private void handson$arrange(Level level, CallbackInfo info) {
        if (!level.isClientSide()) {
            ProcessingSlots.arrangeAndSync((MicrowaveBlockEntity) (Object) this);
        }
    }
}
