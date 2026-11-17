package com.pockyl.handson_furniture.mixin;

import com.mrcrayfish.furniture.refurbished.blockentity.ProcessingContainerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.pockyl.handson_furniture.Furniture;
import com.pockyl.handson_furniture.station.ProcessingSlots;

/**
 * Refurbished's freezer freezes its first slot into its second, while a player of this mod puts things anywhere on its shelves:
 * before each of its ticks the slots are set out so that what freezes is in the first ({@link ProcessingSlots}). Clients are
 * told of the change, and of each thing frozen or heated (the microwave finishes the same way), as they would be of a thing
 * put in by hand: Refurbished tells them nothing.
 */
@Mixin(ProcessingContainerBlockEntity.class)
abstract class ProcessingContainerBlockEntityMixin {
    @Inject(method = "serverTick", at = @At("HEAD"), remap = false)
    private static void handson$arrange(Level level, BlockPos pos, BlockState state, ProcessingContainerBlockEntity entity, CallbackInfo info) {
        ProcessingSlots.arrangeAndSync(entity);
    }

    @Inject(method = "onCompleteProcess", at = @At("TAIL"), remap = false)
    private void handson$processed(CallbackInfo info) {
        ProcessingContainerBlockEntity entity = (ProcessingContainerBlockEntity) (Object) this;
        if (Furniture.of(entity.getBlockState().getBlock()).flatMap(ProcessingSlots::of).isPresent()) {
            ProcessingSlots.sync(entity);
        }
    }
}
