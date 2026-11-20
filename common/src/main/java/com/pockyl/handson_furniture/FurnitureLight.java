package com.pockyl.handson_furniture;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import com.mrcrayfish.furniture.refurbished.block.FreezerBlock;
import com.mrcrayfish.furniture.refurbished.block.FridgeBlock;
import com.mrcrayfish.furniture.refurbished.block.MicrowaveBlock;
import com.mrcrayfish.furniture.refurbished.block.StoveBlock;

import com.pockyl.handson_furniture.mixin.BlockStateLightAccess;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The light the furniture's lamps give the room: an open fridge, an open microwave and a lit oven shine on the blocks around
 * them, as their lamps inside do on what they hold. It is the block's own light, by the states Refurbished already has (open,
 * lit), so every light engine and shader sees it and nothing is sent of it; Refurbished gives these blocks none.
 *
 * <p>A heating microwave shines through its glass door too, and Refurbished has no state for that: each side keeps the
 * microwaves that heat now, and the game's block light engine asks here for their light ({@code BlockLightEngineMixin}).
 * A light engine of another mod (Starlight) does not ask: there a heating microwave lights only its inside.
 */
public final class FurnitureLight {
    /** An open fridge: its strip lights the kitchen in front of it, as a fridge opened in the dark does. */
    public static final int FRIDGE_OPEN = 12;
    /** A microwave with its lamp on: its door open, or heating behind the glass. */
    public static final int MICROWAVE = 9;
    /** The microwaves heating now, by the chunk source of their level (the server's and the client's are apart). */
    private static final Map<Object, Set<BlockPos>> HEATING = Collections.synchronizedMap(new WeakHashMap<>());
    /** A lit oven: its lamp and glowing element through the window. */
    public static final int OVEN_LIT = 10;

    private FurnitureLight() {
    }

    /** Gives each state of {@code block} its light; called as Refurbished makes the block, so no state is ever without it. */
    public static void apply(Block block) {
        for (BlockState state : block.getStateDefinition().getPossibleStates()) {
            int light = lightOf(block, state);
            if (light > state.getLightEmission()) {
                ((BlockStateLightAccess) state).handson$setLightEmission(light);
            }
        }
    }

    /** How bright the block is in this state: 0 for a shut fridge or microwave, an oven that is off, and the freezer. */
    public static int lightOf(Block block, BlockState state) {
        if (block instanceof FreezerBlock) {
            return 0;
        }
        if (block instanceof FridgeBlock) {
            return flag(state, "open") ? FRIDGE_OPEN : 0;
        }
        if (block instanceof MicrowaveBlock) {
            return flag(state, "open") ? MICROWAVE : 0;
        }
        if (block instanceof StoveBlock) {
            return flag(state, "lit") ? OVEN_LIT : 0;
        }
        return 0;
    }

    /** Notes whether the microwave at {@code pos} heats now; on a change the light around it is worked out again. */
    public static void heating(Level level, BlockPos pos, boolean heating) {
        Object source = level.getChunkSource();
        Set<BlockPos> microwaves = heating ? HEATING.computeIfAbsent(source, key -> ConcurrentHashMap.newKeySet()) : HEATING.get(source);
        if (microwaves == null) {
            return;
        }
        boolean changed = heating ? microwaves.add(pos.immutable()) : microwaves.remove(pos);
        if (changed) {
            level.getChunkSource().getLightEngine().checkBlock(pos);
        }
    }

    /**
     * The light of a heating microwave at {@code pos} of the level whose chunk source is {@code source}, or 0. Asked by the light
     * engine, on its own thread, for the blocks it lights.
     */
    public static int heatingLight(Object source, long pos, BlockState state) {
        if (!(state.getBlock() instanceof MicrowaveBlock)) {
            return 0;
        }
        Set<BlockPos> microwaves = HEATING.get(source);
        return microwaves != null && microwaves.contains(BlockPos.of(pos)) ? MICROWAVE : 0;
    }

    private static boolean flag(BlockState state, String name) {
        for (Property<?> property : state.getProperties()) {
            if (name.equals(property.getName()) && state.getValue(property) instanceof Boolean value) {
                return value;
            }
        }
        return false;
    }
}
