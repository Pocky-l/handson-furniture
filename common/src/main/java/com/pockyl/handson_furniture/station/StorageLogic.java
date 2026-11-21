package com.pockyl.handson_furniture.station;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;

import com.mrcrayfish.furniture.refurbished.blockentity.BasicLootBlockEntity;
import com.mrcrayfish.furniture.refurbished.blockentity.ElectricityGeneratorBlockEntity;
import com.mrcrayfish.furniture.refurbished.blockentity.IHomeControlDevice;
import com.mrcrayfish.furniture.refurbished.blockentity.MailboxBlockEntity;
import com.mrcrayfish.furniture.refurbished.blockentity.MicrowaveBlockEntity;
import com.mrcrayfish.furniture.refurbished.blockentity.StoveBlockEntity;
import com.mrcrayfish.furniture.refurbished.electricity.IModuleNode;

import com.pockyl.diegesis.api.state.SlotRef;
import com.pockyl.diegesis.api.state.SlotStorage;
import com.pockyl.diegesis.api.state.StationHost;
import com.pockyl.diegesis.api.station.StationContext;
import com.pockyl.diegesis.api.station.StationLogic;
import com.pockyl.handson_furniture.Furniture;
import com.pockyl.handson_furniture.mixin.GeneratorAccess;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * The rules of a piece of storage furniture. Its items stay in the furniture's own container, so hoppers, breaking it and the
 * menu of a player without the mod all go on working; the station only keeps where each item stands. Its door (or drawer) is
 * opened and shut by a click on it, with the furniture's own sounds and look, and stays as it was left: a door left open is
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
    /**
     * Ticks the furniture's own shut waits after the click that shuts the last door: until then the scene's door is still on
     * its way to the frame (the slowest, the fridge's, takes half a second). Shut at once, the furniture turned solid and dark
     * inside while the player still saw it open.
     */
    public static final int SHUT_TICKS = 10;
    /**
     * The line a player at a freezer, microwave or stove switched on without power is shown: Refurbished runs them only on
     * electricity, and its menu's sign of missing power is not there at the station.
     */
    public static final String NO_POWER = "message.handson_furniture.no_power";
    /** The line for a microwave switched on with nothing in it that heats: it does not start (Refurbished heats few things). */
    public static final String NOTHING_TO_HEAT = "message.handson_furniture.nothing_to_heat";
    /** The line for a generator switched on with nothing on its grate that burns: it gives no power (its menu shows an empty bar). */
    public static final String NO_FUEL = "message.handson_furniture.no_fuel";

    /** The name of Refurbished's block property that says its door is open (or its drawer out, its lid up). */
    private static final String OPEN_PROPERTY = "open";

    private final int size;
    /** The game time the furniture's own shut is due at, or -1 when none waits. */
    private long shutAt = -1L;
    /** Per door or drawer on its way shut, the game time its close sound is due at. */
    private final Map<String, Long> closeSoundAt = new HashMap<>();

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
        knock(station, false);
        if (shutAt >= 0L && station.level().getGameTime() >= shutAt) {
            shut(station);
        }
    }

    /** A player leaving does not wait for the door: a close sound or shut still due comes now, as the station may end with them. */
    @Override
    public void onClose(StationContext station, Player player) {
        knock(station, true);
        if (shutAt >= 0L) {
            shut(station);
        }
    }

    /**
     * The player flips the power switch: the furniture itself is switched, as its menu's power button does. The stove's own
     * button also lights its hob, so it is pressed as that button; the generator has only that button.
     */
    @Override
    public void onControlChanged(StationContext station, Player player, String control, float value) {
        if (Furniture.POWER.equals(control) && station.blockEntity() instanceof ElectricityGeneratorBlockEntity generator) {
            // Switched on with too much linked to it, Refurbished leaves it off: the switch follows on the next tick.
            if (isSwitchedOn(generator) != value > 0.5F) {
                generator.togglePower();
            }
            return;
        }
        if (Furniture.POWER.equals(control) && station.blockEntity() instanceof IHomeControlDevice device) {
            boolean on = value > 0.5F;
            if (device.isDeviceEnabled() != on) {
                if (device instanceof StoveBlockEntity stove) {
                    stove.togglePower();
                } else {
                    device.setDeviceState(on);
                }
            }
            // A microwave with power and nothing in it that heats does not start: its switch goes back, with a low beep.
            if (on && device instanceof MicrowaveBlockEntity microwave && !lacksPower(microwave)
                    && !ProcessingSlots.MICROWAVE.hasWork(microwave)) {
                device.setDeviceState(false);
                station.state().setControl(Furniture.POWER, 0.0F);
                MicrowaveCycle.refuse(station.level(), station.pos());
            }
        }
    }

    /**
     * Whether the furniture runs on electricity and gets none: Refurbished's freezer, microwave and stove do nothing, switched on
     * or not, until a generator is linked to them. Its power is known on both sides (Refurbished sends it to clients).
     */
    public static boolean lacksPower(BlockEntity furniture) {
        return furniture instanceof IModuleNode node && !node.isNodePowered();
    }

    /** Whether the furniture's door is open by hand at its station, as everyone sees it (its menu opens none). */
    public static boolean isDoorOpen(BlockEntity furniture) {
        return furniture instanceof StationHost host && host.stationState().schema().controls().containsKey(Furniture.OPEN)
                && host.stationState().getControl(Furniture.OPEN) > 0.5F;
    }

    /**
     * Whether the furniture is switched on by its power switch (Refurbished's own on and off), whether or not it runs: a generator
     * may have no fuel, an appliance no power. False for furniture without one. Known on either side.
     */
    public static boolean isSwitchedOn(BlockEntity furniture) {
        if (furniture instanceof GeneratorAccess generator) {
            return generator.handson$isEnabled();
        }
        return furniture instanceof IHomeControlDevice device && device.isDeviceEnabled();
    }

    private static void showPower(StationContext station) {
        if (station.state().schema().controls().containsKey(Furniture.POWER)) {
            float shown = isSwitchedOn(station.blockEntity()) ? 1.0F : 0.0F;
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
                toggleDoor(station, player, control);
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
     * Opens a door or drawer, or shuts it, without counting a player as having it open, so it stays as it is left. Each part
     * sounds on its own: Refurbished's open sound with the click, its close sound as the part meets the frame
     * ({@link Furniture#closeSoundTicks}). The furniture's own open and shut (the open model the world shows where the station's
     * scene is not drawn, and the light inside) come with the first that opens and the last that shuts, once that one has met
     * its frame ({@link #SHUT_TICKS}). Opened again before then, the furniture never shut and stays open. A mail box's front let
     * down is its mail checked.
     */
    private void toggleDoor(StationContext station, Player player, String control) {
        boolean wasOpen = anyOpen(station);
        boolean open = station.state().getControl(control) < 0.5F;
        station.state().setControl(control, open ? 1.0F : 0.0F);
        if (open && station.blockEntity() instanceof MailboxBlockEntity mailbox) {
            MailDoor.opened(mailbox, station.level(), station.pos(), player);
        }
        Furniture furniture = furniture(station);
        if (open) {
            // Pulled open again on its way shut, it never met the frame.
            closeSoundAt.remove(control);
            if (furniture != null) {
                DoorSounds.play(furniture, station.level(), station.pos(), true);
            }
        } else if (furniture != null) {
            closeSoundAt.put(control, station.level().getGameTime() + furniture.closeSoundTicks());
        }
        boolean isOpen = anyOpen(station);
        if (wasOpen == isOpen) {
            return;
        }
        if (!isOpen) {
            shutAt = station.level().getGameTime() + SHUT_TICKS;
        } else if (shutAt >= 0L) {
            shutAt = -1L;
        } else {
            showOpen(station, furniture, true);
        }
    }

    /** The close sounds due by now: each part's as it meets the frame; all that wait with {@code all}. */
    private void knock(StationContext station, boolean all) {
        if (closeSoundAt.isEmpty()) {
            return;
        }
        Furniture furniture = furniture(station);
        long now = station.level().getGameTime();
        Iterator<Map.Entry<String, Long>> due = closeSoundAt.entrySet().iterator();
        while (due.hasNext()) {
            Map.Entry<String, Long> entry = due.next();
            if (all || now >= entry.getValue()) {
                due.remove();
                if (furniture != null) {
                    DoorSounds.play(furniture, station.level(), station.pos(), false);
                }
            }
        }
    }

    /** The furniture's own shut, its shut model, unless a door was opened again meanwhile. */
    private void shut(StationContext station) {
        shutAt = -1L;
        if (!anyOpen(station)) {
            showOpen(station, furniture(station), false);
        }
    }

    /**
     * Refurbished's open or shut look of the furniture, without its sound: the parts make their own. Refurbished's open and close
     * play the sound and set the block's {@code open}; a piece without a sound of Refurbished's (the generator, the mail box) has
     * them called as before.
     */
    private static void showOpen(StationContext station, Furniture furniture, boolean open) {
        BlockState state = station.level().getBlockState(station.pos());
        if (furniture != null && furniture.doorSound() != null) {
            for (Property<?> property : state.getProperties()) {
                if (property instanceof BooleanProperty flag && OPEN_PROPERTY.equals(flag.getName())) {
                    if (state.getValue(flag) != open) {
                        station.level().setBlock(station.pos(), state.setValue(flag, open), Block.UPDATE_ALL);
                    }
                    return;
                }
            }
        }
        if (station.blockEntity() instanceof BasicLootBlockEntity loot) {
            if (open) {
                loot.onOpen(station.level(), station.pos(), state);
            } else {
                loot.onClose(station.level(), station.pos(), state);
            }
        }
    }

    private static Furniture furniture(StationContext station) {
        return Furniture.of(station.level().getBlockState(station.pos()).getBlock()).orElse(null);
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
