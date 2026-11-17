package com.pockyl.handson_furniture.mixin;

import com.mrcrayfish.furniture.refurbished.blockentity.BasicLootBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.Container;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.pockyl.diegesis.api.state.SlotStorage;
import com.pockyl.diegesis.api.state.StationHost;
import com.pockyl.diegesis.api.state.StationSchema;
import com.pockyl.diegesis.api.state.StationState;
import com.pockyl.handson_furniture.Furniture;
import com.pockyl.handson_furniture.HandsOnFurniture;

/**
 * Refurbished's storage furniture keeps its station's state: where each of its items stands, turned and sized. The items
 * themselves stay in the furniture's own container; only their places are saved, beside it in the block entity's data.
 *
 * <p>Clients are sent that data too (Refurbished sends none): a door left open, and what stands behind it, are seen by players
 * who join or come near later, while nobody uses the furniture.
 *
 * <p>Whatever changes what it holds tells them too: a hopper, a command or another mod, not only a hand at the station.
 *
 * <p>Its container holds as many stacks as {@link Furniture#slots()} says, more than its menu has: what fits inside is a matter
 * of room on its shelves, not of slots. Hoppers reach the slots they always did; breaking it drops everything; a player without
 * the mod sees the menu's own slots.
 */
@Mixin(BasicLootBlockEntity.class)
abstract class BasicLootBlockEntityMixin extends BlockEntity implements StationHost {
    @Unique
    private static final String handson$KEY = HandsOnFurniture.MOD_ID;

    @Shadow(remap = false)
    @Final
    @Mutable
    protected int containerSize;
    @Shadow(remap = false)
    protected NonNullList<ItemStack> items;

    @Unique
    private StationState handson$station;
    /** What was read before the state was first asked for: the block entity loads before its block state is known. */
    @Unique
    private CompoundTag handson$read;
    /** The registries it was read with, for that first load: a block entity loads before it has a level. */
    @Unique
    private HolderLookup.Provider handson$registries;

    private BasicLootBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /** Made as large as the station's room: {@code loadAdditional} makes its list as large as the size kept here. */
    @Inject(method = "<init>", at = @At("RETURN"))
    private void handson$roomy(BlockEntityType<?> type, BlockPos pos, BlockState state, int size, CallbackInfo info) {
        Furniture.of(state.getBlock()).ifPresent(furniture -> {
            if (furniture.slots() > containerSize) {
                containerSize = furniture.slots();
                items = NonNullList.withSize(containerSize, ItemStack.EMPTY);
            }
        });
    }

    @Override
    public StationState stationState() {
        if (handson$station == null) {
            StationSchema schema = Furniture.of(getBlockState().getBlock()).map(furniture -> furniture.type().schema())
                    .orElse(StationSchema.EMPTY);
            handson$station = new StationState(schema);
            // The items are the furniture's own, on both sides: a client's copy shows what the block's update brought into the
            // container, and loading the places never empties them.
            StationSchema.SlotGroup items = schema.slotGroups().get(Furniture.ITEMS);
            Container container = (Container) this;
            if (items != null && container.getContainerSize() >= items.size()) {
                handson$station.bindSlots(Furniture.ITEMS, SlotStorage.of(container, 0, items.size()));
            }
            if (handson$read != null) {
                handson$station.load(handson$read, level != null ? level.registryAccess() : handson$registries);
            }
        }
        return handson$station;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /**
     * Clients see what the furniture holds, whoever changed it: a hopper, a command or another mod reach them as a hand at the
     * station does. The game sends a block's update at most once a tick however often it is asked, so a hopper or a burst of
     * changes costs no more than one packet a tick.
     */
    @Override
    public void setChanged() {
        super.setChanged();
        if (level != null && !level.isClientSide() && Furniture.of(getBlockState().getBlock()).isPresent()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void handson$save(CompoundTag tag, HolderLookup.Provider registries, CallbackInfo info) {
        CompoundTag saved = handson$station != null ? handson$station.save(registries) : handson$read;
        if (saved != null && !saved.isEmpty()) {
            tag.put(handson$KEY, saved);
        }
    }

    @Inject(method = "loadAdditional", at = @At("TAIL"))
    private void handson$load(CompoundTag tag, HolderLookup.Provider registries, CallbackInfo info) {
        handson$read = tag.contains(handson$KEY) ? tag.getCompound(handson$KEY) : null;
        handson$registries = registries;
        if (handson$station != null) {
            handson$station.load(handson$read != null ? handson$read : new CompoundTag(), registries);
            handson$station.pollStorages();
        }
    }
}
