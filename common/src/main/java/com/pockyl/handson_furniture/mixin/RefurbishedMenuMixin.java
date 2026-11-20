package com.pockyl.handson_furniture.mixin;

import com.mrcrayfish.furniture.refurbished.inventory.ElectricityGeneratorMenu;
import com.mrcrayfish.furniture.refurbished.inventory.FreezerMenu;
import com.mrcrayfish.furniture.refurbished.inventory.MicrowaveMenu;
import com.mrcrayfish.furniture.refurbished.inventory.StoveMenu;
import net.minecraft.world.Container;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.pockyl.handson_furniture.station.RoomyContainer;

/**
 * The menus of Refurbished's generator, freezer, microwave and stove tell their own slots from the player's by the container's
 * size in a shift-click. Made larger here for the room ({@link RoomyContainer}), every slot counted as the furniture's and a
 * shift-click moved nothing, for a player without this mod who uses the menu: the size Refurbished made it with is given.
 */
@Mixin({ElectricityGeneratorMenu.class, FreezerMenu.class, MicrowaveMenu.class, StoveMenu.class})
abstract class RefurbishedMenuMixin {
    @Redirect(method = "quickMoveStack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/Container;getContainerSize()I"))
    private int handson$menuSize(Container container) {
        return container instanceof RoomyContainer roomy ? roomy.handson$ownSize() : container.getContainerSize();
    }
}
