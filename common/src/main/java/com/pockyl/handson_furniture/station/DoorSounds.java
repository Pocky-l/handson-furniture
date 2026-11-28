package com.pockyl.handson_furniture.station;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;

import com.pockyl.handson_furniture.Furniture;

/**
 * The sounds of a door, lid or drawer opened and shut at its station: Refurbished's own, each part on its own (a chest of
 * drawers' second drawer is heard too), the close sound when the part meets the frame ({@link Furniture#closeSoundTicks}).
 * Played on the server for everyone near, players without this mod too.
 */
final class DoorSounds {
    /** As Refurbished plays them: full volume, a pitch a little lower and varied. */
    private static final float VOLUME = 1.0F;
    private static final float PITCH = 0.9F;
    private static final float PITCH_SPREAD = 0.1F;

    private DoorSounds() {
    }

    static void play(Furniture furniture, Level level, BlockPos pos, boolean open) {
        if (furniture == Furniture.MAIL_BOX) {
            MailDoor.sound(level, pos, open);
            return;
        }
        Identifier id = furniture.doorSoundId(open);
        if (id == null) {
            return;
        }
        BuiltInRegistries.SOUND_EVENT.getOptional(id).ifPresent(sound -> level.playSound(null, pos, sound, SoundSource.BLOCKS, VOLUME,
                PITCH + PITCH_SPREAD * level.random.nextFloat()));
    }
}
