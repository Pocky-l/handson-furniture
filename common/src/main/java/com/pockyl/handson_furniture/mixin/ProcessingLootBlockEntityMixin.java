package com.pockyl.handson_furniture.mixin;

import com.mrcrayfish.furniture.refurbished.blockentity.ElectricityModuleProcessingLootBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Refurbished's powered furniture (the freezer) tells clients only whether it has power. Loading that, a client's copy of
 * the block loses what it holds and the station's state (the door shows shut, the shelves empty): the update carries the
 * block's whole data instead, as the other furniture's does ({@link BasicLootBlockEntityMixin}).
 */
@Mixin(ElectricityModuleProcessingLootBlockEntity.class)
abstract class ProcessingLootBlockEntityMixin {
    @Inject(method = "getUpdateTag", at = @At("RETURN"))
    private void handson$whole(CallbackInfoReturnable<CompoundTag> info) {
        CompoundTag whole = ((BlockEntity) (Object) this).saveWithoutMetadata();
        whole.merge(info.getReturnValue());
        info.getReturnValue().merge(whole);
    }
}
