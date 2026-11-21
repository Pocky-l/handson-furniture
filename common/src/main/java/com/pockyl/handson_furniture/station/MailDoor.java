package com.pockyl.handson_furniture.station;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import com.mrcrayfish.furniture.refurbished.block.MailboxBlock;
import com.mrcrayfish.furniture.refurbished.blockentity.MailboxBlockEntity;
import com.mrcrayfish.furniture.refurbished.mail.Mailbox;

/**
 * The front of a mail box, let down by hand. Opening it is checking the mail, as opening Refurbished's menu is: the flag goes
 * down and a mail box nobody has opened yet becomes the player's. Refurbished's mail box makes no sound of its own; its small
 * wooden front clicks as a wooden trapdoor does, quieter.
 */
public final class MailDoor {
    private static final float VOLUME = 0.5F;
    private static final float PITCH = 1.3F;

    private MailDoor() {
    }

    /** The player let the front down: the flag goes down, and the mail box is theirs if it had no owner. */
    public static void opened(MailboxBlockEntity mailbox, Level level, BlockPos pos, Player player) {
        BlockState state = level.getBlockState(pos);
        if (state.hasProperty(MailboxBlock.ENABLED) && state.getValue(MailboxBlock.ENABLED)) {
            level.setBlock(pos, state.setValue(MailboxBlock.ENABLED, false), Block.UPDATE_ALL);
        }
        Mailbox box = mailbox.getMailbox();
        if (box != null && !box.hasOwner()) {
            box.setOwner(player.getUUID());
        }
    }

    /** The front let down, or meeting its frame. */
    static void sound(Level level, BlockPos pos, boolean open) {
        level.playSound(null, pos, open ? SoundEvents.WOODEN_TRAPDOOR_OPEN : SoundEvents.WOODEN_TRAPDOOR_CLOSE, SoundSource.BLOCKS, VOLUME,
                PITCH);
    }
}
