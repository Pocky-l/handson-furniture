package com.pockyl.handson_furniture.station;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import com.mrcrayfish.furniture.refurbished.blockentity.BasicLootBlockEntity;
import com.mrcrayfish.furniture.refurbished.blockentity.IHomeControlDevice;
import com.mrcrayfish.furniture.refurbished.blockentity.StoveBlockEntity;

import com.pockyl.diegesis.api.state.SlotRef;
import com.pockyl.diegesis.api.state.SlotStorage;
import com.pockyl.diegesis.api.station.StationContext;
import com.pockyl.diegesis.api.station.StationLogic;
import com.pockyl.handson_furniture.Furniture;

/**
 * The rules of a piece of storage furniture. Its items stay in the furniture's own container, so hoppers, breaking it and the
 * menu of a player without the mod all go on working; the station only keeps where each item stands. Its door (or drawer) is
 * opened and shut by a click on it, with the furniture's own sound and look, and stays as it was left: a door left open is
 * open for everyone, also once the player has gone.
 */
public final class StorageLogic implements StationLogic {
    /** A shift-click on an inventory stack: it goes in as the furniture's menu would put it, and stands where there is room. */
    public static final String STORE = "store";
    public static final String SLOT = "slot";
    /** A click on the door or drawer: it opens, or shuts. */
    public static final String DOOR = "door";
    /** Which door or drawer, by its control ({@link Furniture#OPEN} without it). */
    public static final String CONTROL = "control";

    private final int size;

    public StorageLogic(StationContext station) {
        if (station.blockEntity() instanceof Container container) {
            size = Math.min(container.getContainerSize(), station.state().schema().slotGroups().get(Furniture.ITEMS).size());
            station.state().bindSlots(Furniture.ITEMS, SlotStorage.of(container, 0, size));
            // A slot a hopper emptied while nobody was here forgets its place: what comes into it next is set out anew.
            for (int i = 0; i < size; i++) {
                SlotRef ref = SlotRef.of(Furniture.ITEMS, i);
                if (station.state().getItem(ref).isEmpty() && station.state().getPose(ref) != null) {
                    station.state().setPose(ref, null);
                }
            }
        } else {
            size = 0;
        }
        showPower(station);
    }

    /** The power switch follows the furniture: it may have been switched elsewhere (Refurbished's home computer). */
    @Override
    public void tick(StationContext station) {
        showPower(station);
    }

    /**
     * The player flips the power switch: the furniture itself is switched, as its menu's power button does. The stove's own
     * button also lights its hob, so it is pressed as that button.
     */
    @Override
    public void onControlChanged(StationContext station, Player player, String control, float value) {
        if (Furniture.POWER.equals(control) && station.blockEntity() instanceof IHomeControlDevice device) {
            boolean on = value > 0.5F;
            if (device.isDeviceEnabled() != on) {
                if (device instanceof StoveBlockEntity stove) {
                    stove.togglePower();
                } else {
                    device.setDeviceState(on);
                }
            }
        }
    }

    private static void showPower(StationContext station) {
        if (station.state().schema().controls().containsKey(Furniture.POWER) && station.blockEntity() instanceof IHomeControlDevice device) {
            float shown = device.isDeviceEnabled() ? 1.0F : 0.0F;
            if (station.state().getControl(Furniture.POWER) != shown) {
                station.state().setControl(Furniture.POWER, shown);
            }
        }
    }

    @Override
    public void onCustomAction(StationContext station, Player player, String action, CompoundTag data) {
        if (DOOR.equals(action)) {
            String control = data.contains(CONTROL) ? data.getString(CONTROL) : Furniture.OPEN;
            if (!Furniture.POWER.equals(control) && station.state().schema().controls().containsKey(control)) {
                toggleDoor(station, control);
            }
            return;
        }
        if (!STORE.equals(action) || !data.contains(SLOT)) {
            return;
        }
        int from = data.getInt(SLOT);
        Inventory inventory = player.getInventory();
        if (from < 0 || from >= inventory.items.size()) {
            return;
        }
        ItemStack stack = inventory.items.get(from);
        if (stack.isEmpty()) {
            return;
        }
        ItemStack left = store(station, stack.copy());
        if (left.getCount() != stack.getCount()) {
            inventory.items.set(from, left);
            inventory.setChanged();
        }
    }

    /**
     * Opens a door or drawer, or shuts it, without counting a player as having it open, so it stays as it is left. The
     * furniture's own open and shut (its sound, and the open model the world shows where the station's scene is not drawn)
     * come with the first that opens and the last that shuts.
     */
    private static void toggleDoor(StationContext station, String control) {
        boolean wasOpen = anyOpen(station);
        boolean open = station.state().getControl(control) < 0.5F;
        station.state().setControl(control, open ? 1.0F : 0.0F);
        boolean isOpen = anyOpen(station);
        if (wasOpen != isOpen && station.blockEntity() instanceof BasicLootBlockEntity furniture) {
            BlockState state = station.level().getBlockState(station.pos());
            if (isOpen) {
                furniture.onOpen(station.level(), station.pos(), state);
            } else {
                furniture.onClose(station.level(), station.pos(), state);
            }
        }
    }

    private static boolean anyOpen(StationContext station) {
        for (String control : station.state().schema().controls().keySet()) {
            if (!Furniture.POWER.equals(control) && station.state().getControl(control) > 0.5F) {
                return true;
            }
        }
        return false;
    }

    /** Puts {@code stack} into the furniture: onto stacks of the same item first, then into empty slots. Returns what did not fit. */
    private ItemStack store(StationContext station, ItemStack stack) {
        for (int pass = 0; pass < 2 && !stack.isEmpty(); pass++) {
            for (int i = 0; i < size && !stack.isEmpty(); i++) {
                SlotRef ref = SlotRef.of(Furniture.ITEMS, i);
                ItemStack there = station.state().getItem(ref);
                boolean merge = pass == 0 && !there.isEmpty() && ItemStack.isSameItemSameTags(there, stack);
                boolean fill = pass == 1 && there.isEmpty();
                if (!merge && !fill) {
                    continue;
                }
                int room = stack.getMaxStackSize() - there.getCount();
                int moved = Math.min(room, stack.getCount());
                if (moved <= 0) {
                    continue;
                }
                station.state().setItem(ref, stack.copyWithCount(there.getCount() + moved));
                stack.shrink(moved);
            }
        }
        return stack;
    }
}
