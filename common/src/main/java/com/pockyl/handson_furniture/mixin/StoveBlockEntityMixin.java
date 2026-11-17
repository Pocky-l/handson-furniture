package com.pockyl.handson_furniture.mixin;

import com.mrcrayfish.furniture.refurbished.blockentity.StoveBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.pockyl.handson_furniture.station.ProcessingSlots;

/**
 * Refurbished's oven bakes each of its first three slots into the one three further on: before each tick of the stove the
 * slots are set out so that what bakes is in the first three ({@link ProcessingSlots}). Its bakes finish without telling
 * clients: when one of its six slots changed during the tick, clients are sent the block's data, as for a thing put in by hand.
 */
@Mixin(StoveBlockEntity.class)
abstract class StoveBlockEntityMixin {
    /** The stove's six slots before its tick, as items and counts. */
    @Unique
    private static final ThreadLocal<int[]> handson$before = ThreadLocal.withInitial(() -> new int[12]);

    @Inject(method = "serverTick", at = @At("HEAD"), remap = false)
    private static void handson$arrange(Level level, BlockPos pos, BlockState state, StoveBlockEntity stove, CallbackInfo info) {
        ProcessingSlots.arrangeAndSync(stove);
        handson$snapshot(stove, handson$before.get());
    }

    @Inject(method = "serverTick", at = @At("TAIL"), remap = false)
    private static void handson$baked(Level level, BlockPos pos, BlockState state, StoveBlockEntity stove, CallbackInfo info) {
        int[] after = new int[12];
        handson$snapshot(stove, after);
        if (!java.util.Arrays.equals(after, handson$before.get())) {
            ProcessingSlots.sync(stove);
        }
    }

    @Unique
    private static void handson$snapshot(StoveBlockEntity stove, int[] into) {
        for (int i = 0; i < 6; i++) {
            ItemStack stack = stove.getItem(i);
            into[i * 2] = System.identityHashCode(stack.getItem());
            into[i * 2 + 1] = stack.getCount();
        }
    }
}
