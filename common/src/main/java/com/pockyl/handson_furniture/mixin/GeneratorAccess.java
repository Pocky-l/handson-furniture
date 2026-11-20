package com.pockyl.handson_furniture.mixin;

import com.mrcrayfish.furniture.refurbished.blockentity.ElectricityGeneratorBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Refurbished's generator: whether it is switched on (its menu's power button flips it, {@code togglePower}, and nothing
 * else reads it) and what its fuel gave. Sent to clients with the block's data.
 */
@Mixin(ElectricityGeneratorBlockEntity.class)
public interface GeneratorAccess {
    @Accessor(value = "enabled", remap = false)
    boolean handson$isEnabled();

    /** How much energy the fuel burning now gave when it went in: the full bar of its screen. */
    @Accessor(value = "totalEnergy", remap = false)
    int handson$totalEnergy();
}
