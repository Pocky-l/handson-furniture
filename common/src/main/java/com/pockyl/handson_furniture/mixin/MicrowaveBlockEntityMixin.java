package com.pockyl.handson_furniture.mixin;

import com.mrcrayfish.furniture.refurbished.blockentity.IHomeControlDevice;
import com.mrcrayfish.furniture.refurbished.blockentity.MicrowaveBlockEntity;
import com.mrcrayfish.furniture.refurbished.electricity.IModuleNode;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.pockyl.handson_furniture.FurnitureLight;
import com.pockyl.handson_furniture.station.MicrowaveCycle;
import com.pockyl.handson_furniture.station.ProcessingSlots;
import com.pockyl.handson_furniture.station.StorageLogic;

/**
 * Refurbished's microwave heats its first slot into its second, ticked by its power network rather than as the freezer is:
 * before each of its ticks the slots are set out so that what heats is in the first ({@link ProcessingSlots}).
 *
 * <p>It runs as a real microwave ({@link MicrowaveCycle}): with its door open by hand it does not heat, and goes on from where
 * it was once the door is shut (Refurbished would heat with it open, and start over after any pause); it beeps as it starts,
 * and when nothing is left to heat it beeps three times and switches itself off. Its menu, for a player without this mod,
 * opens no door here. While it heats, its light shines through the glass ({@link FurnitureLight#heating}).
 */
@Mixin(MicrowaveBlockEntity.class)
abstract class MicrowaveBlockEntityMixin {
    @Shadow(remap = false)
    protected boolean processing;

    /** Whether it heated at the end of its last tick, on the server. */
    @Unique
    private boolean handson$wasHeating;
    @Unique
    private final MicrowaveCycle handson$cycle = new MicrowaveCycle();

    @Inject(method = "moduleTick", at = @At("HEAD"), cancellable = true, remap = false)
    private void handson$arrange(Level level, CallbackInfo info) {
        if (level.isClientSide()) {
            return;
        }
        MicrowaveBlockEntity microwave = (MicrowaveBlockEntity) (Object) this;
        handson$cycle.tick(level, microwave.getBlockPos());
        ProcessingSlots.arrangeAndSync(microwave);
        if (StorageLogic.isDoorOpen(microwave)) {
            // Its power is still followed, as its own tick does; the heating is skipped, so the time it heated for stays.
            IModuleNode node = (IModuleNode) microwave;
            node.updateNodePoweredState();
            node.setNodeReceivingPower(false);
            if (processing) {
                processing = false;
                microwave.syncDataToTrackingClients();
            }
            // Shut again, it starts with its beep.
            handson$wasHeating = false;
            FurnitureLight.heating(level, microwave.getBlockPos(), false);
            info.cancel();
        }
    }

    @Inject(method = "moduleTick", at = @At("TAIL"), remap = false)
    private void handson$light(Level level, CallbackInfo info) {
        MicrowaveBlockEntity microwave = (MicrowaveBlockEntity) (Object) this;
        FurnitureLight.heating(level, microwave.getBlockPos(), microwave.canPlayAudio());
        if (level.isClientSide()) {
            return;
        }
        boolean heating = processing;
        if (heating && !handson$wasHeating) {
            handson$cycle.started(level, microwave.getBlockPos());
        } else if (!heating && handson$wasHeating && microwave.isNodePowered() && ((IHomeControlDevice) microwave).isDeviceEnabled()
                && !ProcessingSlots.MICROWAVE.hasWork(microwave)) {
            // Done: everything in it is heated. It switches itself off, as a microwave does at the end of its time.
            ((IHomeControlDevice) microwave).setDeviceState(false);
            handson$cycle.done(level);
        }
        handson$wasHeating = heating;
    }
}
