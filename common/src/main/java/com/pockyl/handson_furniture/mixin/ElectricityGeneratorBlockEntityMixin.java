package com.pockyl.handson_furniture.mixin;

import com.mrcrayfish.furniture.refurbished.blockentity.ElectricityGeneratorBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.pockyl.handson_furniture.station.ProcessingSlots;
import com.pockyl.handson_furniture.station.QuietChanges;

/**
 * Refurbished's generator burns its one fuel slot, while a player of this mod lays fuel anywhere on its grate: before each of
 * its ticks a fuel goes into that slot ({@link ProcessingSlots}). It burns a fuel without telling clients unless the new one
 * burns longer or shorter: when its slot changed during the tick, clients are sent the block's data, so the pile on the grate
 * goes down as it burns. A burnt fuel that leaves something (a lava bucket its bucket) leaves it where it lay: Refurbished
 * asks the emptied stack for it, which has none, and the bucket was lost. Its energy going down, which marks it changed on every tick it runs, sends nothing
 * ({@link QuietChanges}): clients count it down themselves.
 */
@Mixin(ElectricityGeneratorBlockEntity.class)
abstract class ElectricityGeneratorBlockEntityMixin {
    /** The fuel slot before the tick: its item and count. */
    @Unique
    private int handson$item;
    @Unique
    private int handson$count;
    /** What the fuel in the slot leaves once burnt, or null. */
    @Unique
    private ItemStack handson$leaves = ItemStack.EMPTY;

    @Inject(method = "earlyNodeTick", at = @At("HEAD"), remap = false)
    private void handson$arrange(Level level, CallbackInfo info) {
        if (level.isClientSide()) {
            return;
        }
        ElectricityGeneratorBlockEntity generator = (ElectricityGeneratorBlockEntity) (Object) this;
        ProcessingSlots.arrangeAndSync(generator);
        ItemStack fuel = generator.getItem(0);
        handson$item = System.identityHashCode(fuel.getItem());
        handson$count = fuel.getCount();
        handson$leaves = fuel.isEmpty() ? ItemStack.EMPTY : fuel.getItem().getCraftingRemainder();
    }

    @Inject(method = "earlyNodeTick", at = @At("TAIL"), remap = false)
    private void handson$burnt(Level level, CallbackInfo info) {
        if (level.isClientSide()) {
            return;
        }
        ElectricityGeneratorBlockEntity generator = (ElectricityGeneratorBlockEntity) (Object) this;
        ItemStack fuel = generator.getItem(0);
        if (fuel.isEmpty() && handson$count == 1 && !handson$leaves.isEmpty()) {
            // In the burnt fuel's slot, so it keeps its place; it is moved off the burning slot on the next tick.
            generator.setItem(0, handson$leaves.copy());
            fuel = generator.getItem(0);
        }
        if (System.identityHashCode(fuel.getItem()) != handson$item || fuel.getCount() != handson$count) {
            ProcessingSlots.sync(generator);
        }
    }

    @Inject(method = "addEnergy", at = @At("HEAD"), remap = false)
    private void handson$quietStart(int energy, CallbackInfo info) {
        QuietChanges.begin();
    }

    @Inject(method = "addEnergy", at = @At("RETURN"), remap = false)
    private void handson$quietEnd(int energy, CallbackInfo info) {
        QuietChanges.end();
    }
}
