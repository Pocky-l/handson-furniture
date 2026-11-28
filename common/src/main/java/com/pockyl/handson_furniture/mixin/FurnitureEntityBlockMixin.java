package com.pockyl.handson_furniture.mixin;

import com.mrcrayfish.furniture.refurbished.block.FurnitureEntityBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.pockyl.handson_furniture.station.RoomyContainer;

/**
 * A comparator reads Refurbished's furniture as full when its menu's slots are full, as before this mod: the room added for
 * things to stand in ({@link RoomyContainer}) would make a full fridge read nearly empty (its 27 slots of 250). What stands in
 * the room counts too, up to the strongest signal.
 */
@Mixin(FurnitureEntityBlock.class)
abstract class FurnitureEntityBlockMixin {
    @Inject(method = "getAnalogOutputSignal", at = @At("HEAD"), cancellable = true)
    private void handson$signal(BlockState state, Level level, BlockPos pos, Direction direction, CallbackInfoReturnable<Integer> info) {
        if (!(level.getBlockEntity(pos) instanceof RoomyContainer roomy) || !(roomy instanceof Container container)) {
            return;
        }
        int own = roomy.handson$ownSize();
        if (own <= 0 || own >= container.getContainerSize()) {
            return;
        }
        // As the game's getRedstoneSignalFromContainer, over the slots Refurbished made.
        float full = 0.0F;
        boolean any = false;
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty()) {
                full += (float) stack.getCount() / Math.min(container.getMaxStackSize(), stack.getMaxStackSize());
                any = true;
            }
        }
        info.setReturnValue(Math.min(15, Mth.floor(full / own * 14.0F) + (any ? 1 : 0)));
    }
}
