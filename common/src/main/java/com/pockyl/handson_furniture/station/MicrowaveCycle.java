package com.pockyl.handson_furniture.station;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;

/**
 * The beeps of a microwave's run, as a real one gives them: one as it starts heating, three when it is done and switches itself
 * off, and a low one when it is switched on with nothing in it that heats. The electronic note of the note block's "bit", played
 * on the server for everyone near. Refurbished's own fan hums in between.
 */
public final class MicrowaveCycle {
    private static final float VOLUME = 0.45F;
    /** The beep's pitch: the bit an octave up, the thin high tone of a microwave's buzzer. */
    private static final float BEEP = 2.0F;
    /** The refusal: the same note an octave lower, shorter in the ear. */
    private static final float REFUSE = 1.0F;
    private static final int DONE_BEEPS = 3;
    /** Ticks between the beeps at the end. */
    private static final int GAP = 5;

    private int beepsLeft;
    private long nextBeep;

    /** Plays a beep of the end when one is due; called every server tick of the microwave. */
    public void tick(Level level, BlockPos pos) {
        if (beepsLeft > 0 && level.getGameTime() >= nextBeep) {
            beep(level, pos, BEEP);
            beepsLeft--;
            nextBeep = level.getGameTime() + GAP;
        }
    }

    /** It starts heating. */
    public void started(Level level, BlockPos pos) {
        beepsLeft = 0;
        beep(level, pos, BEEP);
    }

    /** Everything is heated: three beeps, from the next tick on. */
    public void done(Level level) {
        beepsLeft = DONE_BEEPS;
        nextBeep = level.getGameTime() + 1;
    }

    /** Switched on with nothing in it that heats: it will not start. */
    public static void refuse(Level level, BlockPos pos) {
        beep(level, pos, REFUSE);
    }

    private static void beep(Level level, BlockPos pos, float pitch) {
        level.playSound(null, pos, SoundEvents.NOTE_BLOCK_BIT.value(), SoundSource.BLOCKS, VOLUME, pitch);
    }
}
