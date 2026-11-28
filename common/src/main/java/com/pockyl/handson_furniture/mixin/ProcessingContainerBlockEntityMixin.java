package com.pockyl.handson_furniture.mixin;

import com.mrcrayfish.furniture.refurbished.blockentity.ProcessingContainerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
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
 *
 * <p>What a processed thing leaves (a water bucket's bucket) stays in its slot, as on the other lines: Refurbished 26.x looks
 * for it only once the last one is used up, on the emptied stack, and loses it.
 */
@Mixin(ProcessingContainerBlockEntity.class)
abstract class ProcessingContainerBlockEntityMixin {
    /** What each input slot's last thing leaves once processed, or null; read before Refurbished uses it up. */
    @Unique
    private ItemStack[] handson$leaves;

    @Shadow(remap = false)
    protected abstract int[] getInputSlots();

    @Inject(method = "serverTick", at = @At("HEAD"), remap = false)
    private static void handson$arrange(Level level, BlockPos pos, BlockState state, ProcessingContainerBlockEntity entity, CallbackInfo info) {
        ProcessingSlots.arrangeAndSync(entity);
    }

    @Inject(method = "onCompleteProcess", at = @At("HEAD"), remap = false)
    private void handson$leavings(CallbackInfo info) {
        ProcessingContainerBlockEntity entity = (ProcessingContainerBlockEntity) (Object) this;
        handson$leaves = null;
        if (Furniture.of(entity.getBlockState().getBlock()).flatMap(ProcessingSlots::of).isEmpty()) {
            return;
        }
        int[] inputs = getInputSlots();
        for (int i = 0; i < inputs.length; i++) {
            ItemStack stack = entity.getItem(inputs[i]);
            ItemStackTemplate remainder = stack.getCount() == 1 ? stack.getItem().getCraftingRemainder() : null;
            if (remainder != null) {
                if (handson$leaves == null) {
                    handson$leaves = new ItemStack[inputs.length];
                }
                handson$leaves[i] = remainder.create();
            }
        }
    }

    /** At every return: Refurbished 26.x leaves the method early once it processed its first input. */
    @Inject(method = "onCompleteProcess", at = @At("RETURN"), remap = false)
    private void handson$processed(CallbackInfo info) {
        ProcessingContainerBlockEntity entity = (ProcessingContainerBlockEntity) (Object) this;
        if (Furniture.of(entity.getBlockState().getBlock()).flatMap(ProcessingSlots::of).isEmpty()) {
            return;
        }
        if (handson$leaves != null) {
            int[] inputs = getInputSlots();
            for (int i = 0; i < inputs.length && i < handson$leaves.length; i++) {
                if (handson$leaves[i] != null && entity.getItem(inputs[i]).isEmpty()) {
                    entity.setItem(inputs[i], handson$leaves[i]);
                }
            }
            handson$leaves = null;
        }
        ProcessingSlots.sync(entity);
    }
}
