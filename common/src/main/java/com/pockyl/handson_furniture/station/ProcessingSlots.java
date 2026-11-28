package com.pockyl.handson_furniture.station;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipePropertySet;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

import com.mrcrayfish.furniture.refurbished.core.ModRecipePropertySets;

import com.pockyl.diegesis.api.state.ItemPose;
import com.pockyl.diegesis.api.state.SlotRef;
import com.pockyl.diegesis.api.state.StationHost;
import com.pockyl.diegesis.api.state.StationState;
import com.pockyl.handson_furniture.Furniture;

import java.util.Optional;

/**
 * Keeps Refurbished's processing working while things stand anywhere inside. Refurbished works on fixed slots: the freezer
 * freezes its first slot into its second, the microwave heats its first into its second, the oven bakes each of its first three
 * into the one three further on, the generator burns its first. Here a player puts things anywhere, so before each tick the slots are set out: something
 * the furniture can process goes into a free input, and an output is cleared of whatever its input's result could not join.
 * A swap takes the place along, so nothing moves where it stands. Hoppers and the menu of a player without the mod see the
 * furniture as Refurbished made it.
 */
public enum ProcessingSlots {
    /** Refurbished's freezer: what freezes, and what came out frozen. */
    FREEZER("freezer_solidifying", ModRecipePropertySets.FREEZER_INPUT, new int[] {0}, new int[] {1}),
    /** Refurbished's microwave: what heats, and what came out heated. */
    MICROWAVE("microwave_heating", ModRecipePropertySets.MICROWAVE_INPUT, new int[] {0}, new int[] {1}),
    /** Refurbished's oven: three things bake at once, each into the slot three further on. */
    OVEN("oven_baking", ModRecipePropertySets.OVEN_INPUT, new int[] {0, 1, 2}, new int[] {3, 4, 5}),
    /** Refurbished's generator: the fuel that burns; what it leaves (a lava bucket's bucket) is moved off like a thing that does not burn. */
    GENERATOR(null, null, new int[] {0}, new int[0]);

    /** The recipes it processes by, or null for the generator, which burns whatever burns in a furnace. */
    private final Identifier recipeType;
    /** What its recipes take, which clients know too (they have no recipes since 1.21.2). */
    private final ResourceKey<RecipePropertySet> takes;
    private final int[] inputs;
    private final int[] outputs;
    /** The first slot past the furniture's own: the room where things wait. */
    private final int room;

    ProcessingSlots(String recipeType, ResourceKey<RecipePropertySet> takes, int[] inputs, int[] outputs) {
        this.recipeType = recipeType == null ? null : Identifier.fromNamespaceAndPath(Furniture.REFURBISHED, recipeType);
        this.takes = takes;
        this.inputs = inputs;
        this.outputs = outputs;
        int[] own = outputs.length > 0 ? outputs : inputs;
        this.room = own[own.length - 1] + 1;
    }

    public int input(int pair) {
        return inputs[pair];
    }

    public int output(int pair) {
        return outputs[pair];
    }

    /** The processing of a piece of furniture, if it does any. */
    public static Optional<ProcessingSlots> of(Furniture furniture) {
        return switch (furniture) {
            case FREEZER -> Optional.of(FREEZER);
            case MICROWAVE -> Optional.of(MICROWAVE);
            case STOVE -> Optional.of(OVEN);
            case GENERATOR -> Optional.of(GENERATOR);
            default -> Optional.empty();
        };
    }

    /** Sets the slots of {@code furniture} (a block entity that is a container and a station host) out for its next tick. Returns whether anything moved. */
    public boolean arrange(BlockEntity furniture) {
        Level level = furniture.getLevel();
        if (level == null || !(furniture instanceof Container container) || !(furniture instanceof StationHost host)) {
            return false;
        }
        StationState state = host.stationState();
        int size = Math.min(container.getContainerSize(), state.schema().slotGroups().get(Furniture.ITEMS).size());
        boolean moved = false;
        for (int pair = 0; pair < inputs.length; pair++) {
            int in = inputs[pair];
            ItemStack input = container.getItem(in);
            if (input.isEmpty() || !processes(level, input)) {
                int found = find(container, size, level);
                if (found >= 0) {
                    swap(state, in, found);
                    moved = true;
                } else if (!input.isEmpty()) {
                    // Nothing to process: the input is left empty, for a hopper to fill.
                    moved |= moveAway(state, container, size, in);
                }
            }
            if (pair >= outputs.length) {
                continue;
            }
            input = container.getItem(in);
            ItemStack output = container.getItem(outputs[pair]);
            if (!input.isEmpty() && !output.isEmpty() && !joins(output, resultOf(level, input))) {
                moved |= moveAway(state, container, size, outputs[pair]);
            }
        }
        return moved;
    }

    /**
     * Sets the slots of the furniture at {@code entity} out, if it processes anything, and tells clients when something
     * moved: Refurbished tells them nothing of it.
     */
    public static void arrangeAndSync(BlockEntity entity) {
        Furniture.of(entity.getBlockState().getBlock()).flatMap(ProcessingSlots::of).ifPresent(slots -> {
            if (slots.arrange(entity)) {
                sync(entity);
            }
        });
    }

    /**
     * Whether anything in the furniture still has to be processed: a thing waiting or in an input that its recipes turn into
     * something. Works on either side (the client knows the recipes too).
     */
    public boolean hasWork(BlockEntity furniture) {
        Level level = furniture.getLevel();
        if (level == null || !(furniture instanceof Container container)) {
            return false;
        }
        for (int i = 0; i < container.getContainerSize(); i++) {
            if (isOutput(i)) {
                continue;
            }
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty() && processes(level, stack)) {
                return true;
            }
        }
        return false;
    }

    private boolean isOutput(int slot) {
        for (int output : outputs) {
            if (output == slot) {
                return true;
            }
        }
        return false;
    }

    /** Sends the block's data to the clients that see it, as for a thing put in by hand. */
    public static void sync(BlockEntity entity) {
        Level level = entity.getLevel();
        if (level != null && !level.isClientSide()) {
            entity.setChanged();
            level.sendBlockUpdated(entity.getBlockPos(), entity.getBlockState(), entity.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    /** The first slot of the room whose stack can be processed, or -1. */
    private int find(Container container, int size, Level level) {
        for (int i = room; i < size; i++) {
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty() && processes(level, stack)) {
                return i;
            }
        }
        return -1;
    }

    /** Whether the furniture does something with {@code stack}: it has a recipe for it, or (the generator) it burns. */
    public boolean processes(Level level, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (recipeType == null) {
            return level.fuelValues().isFuel(stack);
        }
        if (level instanceof ServerLevel server) {
            return !resultOf(server, stack).isEmpty();
        }
        return level.recipeAccess().propertySet(takes).test(stack);
    }

    /** What processing {@code input} gives, or empty (always for the generator, whose fuel gives power). */
    @SuppressWarnings("unchecked")
    public ItemStack resultOf(Level level, ItemStack input) {
        if (recipeType == null) {
            return ItemStack.EMPTY;
        }
        RecipeType<Recipe<SingleRecipeInput>> type = (RecipeType<Recipe<SingleRecipeInput>>) BuiltInRegistries.RECIPE_TYPE
                .getOptional(recipeType).orElse(null);
        if (type == null || input.isEmpty() || !(level instanceof ServerLevel server)) {
            return ItemStack.EMPTY;
        }
        SingleRecipeInput recipeInput = new SingleRecipeInput(input);
        return server.recipeAccess().getRecipeFor(type, recipeInput, server)
                .map(holder -> holder.value().assemble(recipeInput)).orElse(ItemStack.EMPTY);
    }

    /** Whether {@code result} can be added to {@code output}, as Refurbished adds it. */
    private static boolean joins(ItemStack output, ItemStack result) {
        return !result.isEmpty() && ItemStack.isSameItemSameComponents(output, result)
                && output.getCount() + result.getCount() <= output.getMaxStackSize();
    }

    /**
     * Takes the stack out of {@code slot} into the room: onto stacks of the same thing first, the rest into an empty slot
     * with its place. Returns whether anything moved.
     */
    private boolean moveAway(StationState state, Container container, int size, int slot) {
        SlotRef from = SlotRef.of(Furniture.ITEMS, slot);
        ItemStack stack = container.getItem(slot).copy();
        boolean moved = false;
        for (int i = room; i < size && !stack.isEmpty(); i++) {
            ItemStack there = container.getItem(i);
            if (!there.isEmpty() && ItemStack.isSameItemSameComponents(there, stack) && there.getCount() < there.getMaxStackSize()) {
                int count = Math.min(stack.getCount(), there.getMaxStackSize() - there.getCount());
                state.setItem(SlotRef.of(Furniture.ITEMS, i), there.copyWithCount(there.getCount() + count));
                stack.shrink(count);
                moved = true;
            }
        }
        if (stack.isEmpty()) {
            state.setItem(from, ItemStack.EMPTY);
            state.setPose(from, null);
            return true;
        }
        for (int i = room; i < size; i++) {
            if (container.getItem(i).isEmpty()) {
                state.setItem(from, stack);
                swap(state, slot, i);
                return true;
            }
        }
        if (moved) {
            state.setItem(from, stack);
        }
        return moved;
    }

    /** Swaps two slots with their places: what stands where stays standing there. */
    private static void swap(StationState state, int a, int b) {
        SlotRef first = SlotRef.of(Furniture.ITEMS, a);
        SlotRef second = SlotRef.of(Furniture.ITEMS, b);
        ItemStack firstStack = state.getItem(first);
        ItemStack secondStack = state.getItem(second);
        ItemPose firstPose = state.getPose(first);
        ItemPose secondPose = state.getPose(second);
        state.setItem(first, secondStack);
        state.setItem(second, firstStack);
        state.setPose(first, secondPose);
        state.setPose(second, firstPose);
    }
}
