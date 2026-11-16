package com.pockyl.handson_furniture.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Quaternionf;

import com.pockyl.diegesis.api.Diegesis;
import com.pockyl.diegesis.api.state.ItemPose;
import com.pockyl.diegesis.api.state.SlotRef;
import com.pockyl.diegesis.api.state.StationHost;
import com.pockyl.diegesis.api.state.StationState;
import com.pockyl.diegesis.api.station.StationContext;
import com.pockyl.diegesis.api.station.StationType;
import com.pockyl.diegesis.impl.action.ActionHandler;
import com.pockyl.diegesis.impl.action.StationAction;
import com.pockyl.handson_furniture.Furniture;
import com.pockyl.handson_furniture.station.StorageLogic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * The furniture as stations on the server: every variant Refurbished makes opens one, the station's slots are the furniture's
 * own, where each item stands is saved with the block, and a shift-click stores as the furniture's menu would. The same on
 * every loader: each registers them in its own way ({@code ModGameTests} on Forge, {@code FabricFurnitureTests} on Fabric).
 */
public final class FurnitureTests {
    private static final BlockPos AT = new BlockPos(1, 1, 1);

    private FurnitureTests() {
    }

    public static void everyVariantIsAStation(GameTestHelper helper) {
        for (Furniture furniture : Furniture.values()) {
            for (String variant : furniture.variants()) {
                Block block = refurbished(helper, furniture, variant);
                helper.assertTrue(Diegesis.stationFor(block.defaultBlockState()).filter(furniture.type()::equals).isPresent(),
                        block + " opens the " + furniture.stationName());
                helper.assertTrue(furniture.variantOf(block).filter(variant::equals).isPresent(), block + " is " + variant);
            }
        }
        helper.succeed();
    }

    public static void slotsAreTheFurnituresOwn(GameTestHelper helper) {
        for (Furniture furniture : Furniture.values()) {
            TestStation station = place(helper, furniture);
            new StorageLogic(station);
            Container container = (Container) station.blockEntity();
            helper.assertTrue(container.getContainerSize() == furniture.slots(), furniture + " has " + furniture.slots() + " slots");
            station.state().setItem(SlotRef.of(Furniture.ITEMS, furniture.slots() - 1), new ItemStack(Items.APPLE, 3));
            helper.assertTrue(container.getItem(furniture.slots() - 1).is(Items.APPLE), furniture + ": the station writes into it");
            container.setItem(0, new ItemStack(Items.MILK_BUCKET));
            helper.assertTrue(station.state().getItem(SlotRef.of(Furniture.ITEMS, 0)).is(Items.MILK_BUCKET), furniture + ": and reads it");
        }
        helper.succeed();
    }

    public static void roomPastTheMenuIsKeptAndDropped(GameTestHelper helper) {
        TestStation station = place(helper, Furniture.FRIDGE);
        new StorageLogic(station);
        BlockEntity fridge = station.blockEntity();
        int far = Furniture.FRIDGE.slots() - 1;
        ((Container) fridge).setItem(far, new ItemStack(Items.CARROT, 5));
        BlockEntity loaded = BlockEntity.loadStatic(fridge.getBlockPos(), fridge.getBlockState(), fridge.saveWithFullMetadata());
        helper.assertTrue(loaded instanceof Container container && container.getContainerSize() == Furniture.FRIDGE.slots()
                && container.getItem(far).is(Items.CARROT) && container.getItem(far).getCount() == 5, "a stack past the menu's slots is saved");
        helper.destroyBlock(AT);
        helper.assertItemEntityCountIs(Items.CARROT, AT, 2.0, 5);
        helper.succeed();
    }

    public static void whatFallsOutGoesToTheInventoryElseTheFloor(GameTestHelper helper) {
        TestStation station = place(helper, Furniture.FRIDGE);
        StorageLogic logic = new StorageLogic(station);
        Player player = helper.makeMockPlayer();
        SlotRef apple = SlotRef.of(Furniture.ITEMS, 40);
        station.state().setItem(apple, new ItemStack(Items.APPLE, 3));
        ActionHandler.apply(station, logic, player, new StationAction.Click(new StationAction.Target.Station(apple), 0,
                StationAction.ClickKind.SPILL));
        helper.assertTrue(station.state().getItem(apple).isEmpty() && player.getInventory().countItem(Items.APPLE) == 3,
                "with room, it falls into the inventory");
        for (int i = 0; i < player.getInventory().items.size(); i++) {
            player.getInventory().items.set(i, new ItemStack(Items.STONE, 64));
        }
        station.state().setItem(apple, new ItemStack(Items.APPLE, 2));
        ActionHandler.apply(station, logic, player, new StationAction.Click(new StationAction.Target.Station(apple), 0,
                StationAction.ClickKind.SPILL));
        helper.assertTrue(station.state().getItem(apple).isEmpty(), "with none, it still leaves the fridge");
        // Its front faces north here: it lands on the ground there.
        helper.assertItemEntityCountIs(Items.APPLE, AT.north(), 1.0, 2);
        helper.succeed();
    }

    public static void placesAreSavedWithTheBlock(GameTestHelper helper) {
        TestStation station = place(helper, Furniture.FRIDGE);
        new StorageLogic(station);
        SlotRef egg = SlotRef.of(Furniture.ITEMS, 4);
        ItemPose pose = new ItemPose(5.0F, 6.0F, 7.0F, 0.0F, new Quaternionf().rotateY(0.7F), 1.3F, -1);
        station.state().setItem(egg, new ItemStack(Items.EGG, 6));
        station.state().setPose(egg, pose);
        BlockEntity fridge = station.blockEntity();
        CompoundTag saved = fridge.saveWithFullMetadata();
        BlockEntity loaded = BlockEntity.loadStatic(fridge.getBlockPos(), fridge.getBlockState(), saved);
        helper.assertTrue(loaded instanceof StationHost, "the fridge keeps a station state");
        StationState state = ((StationHost) loaded).stationState();
        helper.assertTrue(state.schema() == Furniture.FRIDGE.type().schema(), "of the fridge's station");
        helper.assertTrue(pose.equals(state.getPose(egg)), "with the egg where it was left: " + state.getPose(egg));
        helper.assertTrue(((Container) loaded).getItem(4).is(Items.EGG), "and the egg itself in the fridge's own slot");
        helper.succeed();
    }

    public static void aClickOpensTheDoorAndItStaysOpen(GameTestHelper helper) {
        TestStation station = place(helper, Furniture.FRIDGE);
        StorageLogic logic = new StorageLogic(station);
        Player player = helper.makeMockPlayer();
        helper.assertTrue(station.state().getControl(Furniture.OPEN) == 0.0F, "a new fridge is shut");
        logic.onCustomAction(station, player, StorageLogic.DOOR, new CompoundTag());
        helper.assertTrue(station.state().getControl(Furniture.OPEN) == 1.0F, "a click opens it");
        helper.assertTrue(isOpenBlock(station), "as the fridge itself shows it: " + station.level().getBlockState(station.pos()));
        BlockEntity fridge = station.blockEntity();
        BlockEntity loaded = BlockEntity.loadStatic(fridge.getBlockPos(), fridge.getBlockState(), fridge.saveWithFullMetadata());
        helper.assertTrue(((StationHost) loaded).stationState().getControl(Furniture.OPEN) == 1.0F, "left open, it is kept open");
        logic.onCustomAction(station, player, StorageLogic.DOOR, new CompoundTag());
        helper.assertTrue(station.state().getControl(Furniture.OPEN) == 0.0F && !isOpenBlock(station), "another click shuts it");
        helper.succeed();
    }

    /** The kitchen drawer's two drawers open on their own; the block shows open while either is. */
    public static void theTwoDrawersOpenOnTheirOwn(GameTestHelper helper) {
        TestStation station = place(helper, Furniture.KITCHEN_DRAWER);
        StorageLogic logic = new StorageLogic(station);
        Player player = helper.makeMockPlayer();
        CompoundTag lower = new CompoundTag();
        lower.putString(StorageLogic.CONTROL, Furniture.OPEN_LOWER);
        logic.onCustomAction(station, player, StorageLogic.DOOR, lower);
        helper.assertTrue(station.state().getControl(Furniture.OPEN_LOWER) == 1.0F && station.state().getControl(Furniture.OPEN) == 0.0F,
                "a click on the lower drawer pulls out that one only");
        helper.assertTrue(isOpenBlock(station), "and the drawer shows open");
        logic.onCustomAction(station, player, StorageLogic.DOOR, new CompoundTag());
        logic.onCustomAction(station, player, StorageLogic.DOOR, lower);
        helper.assertTrue(station.state().getControl(Furniture.OPEN) == 1.0F && isOpenBlock(station), "the upper one out, it stays open");
        logic.onCustomAction(station, player, StorageLogic.DOOR, new CompoundTag());
        helper.assertTrue(!isOpenBlock(station), "both in, it is shut");
        helper.succeed();
    }

    /** A player who joins later learns from the block's update that its door was left open, and what stands behind it. */
    public static void clientsAreToldOfAnOpenDoor(GameTestHelper helper) {
        TestStation station = place(helper, Furniture.FRIDGE);
        StorageLogic logic = new StorageLogic(station);
        SlotRef egg = SlotRef.of(Furniture.ITEMS, 2);
        station.state().setItem(egg, new ItemStack(Items.EGG, 3));
        station.state().setPose(egg, new ItemPose(4.0F, 6.0F, 5.0F, 1.0F, new Quaternionf(), 1.0F, -1));
        logic.onCustomAction(station, helper.makeMockPlayer(), StorageLogic.DOOR, new CompoundTag());
        BlockEntity fridge = station.blockEntity();
        helper.assertTrue(fridge.getUpdatePacket() != null, "the fridge sends its data to clients");
        // As a client takes it: into the block entity it already has there.
        BlockEntity seen = fridge.getType().create(fridge.getBlockPos(), fridge.getBlockState());
        seen.load(fridge.getUpdateTag());
        helper.assertTrue(((StationHost) seen).stationState().getControl(Furniture.OPEN) == 1.0F, "with its door open");
        helper.assertTrue(((StationHost) seen).stationState().getPose(egg) != null && ((Container) seen).getItem(2).is(Items.EGG),
                "and the egg where it stands");
        helper.succeed();
    }

    /** Whether Refurbished's block shows its door open (its own boolean property of the open door). */
    private static boolean isOpenBlock(TestStation station) {
        net.minecraft.world.level.block.state.BlockState state = station.level().getBlockState(station.pos());
        for (net.minecraft.world.level.block.state.properties.Property<?> property : state.getProperties()) {
            if (property.getName().equals("open") && state.getValue(property) instanceof Boolean open) {
                return open;
            }
        }
        return false;
    }

    public static void anEmptiedSlotForgetsItsPlace(GameTestHelper helper) {
        TestStation station = place(helper, Furniture.KITCHEN_CABINET);
        new StorageLogic(station);
        SlotRef cup = SlotRef.of(Furniture.ITEMS, 2);
        station.state().setItem(cup, new ItemStack(Items.GLASS_BOTTLE));
        station.state().setPose(cup, new ItemPose(3.0F, 1.0F, 4.0F, 0.0F, new Quaternionf(), 1.0F, -1));
        // A hopper takes it while nobody is at the cabinet.
        ((Container) station.blockEntity()).setItem(2, ItemStack.EMPTY);
        new StorageLogic(station);
        helper.assertTrue(station.state().getPose(cup) == null, "what comes in next is set out anew");
        helper.succeed();
    }

    public static void aShiftClickMergesThenFills(GameTestHelper helper) {
        TestStation station = place(helper, Furniture.KITCHEN_DRAWER);
        StorageLogic logic = new StorageLogic(station);
        Container drawer = (Container) station.blockEntity();
        drawer.setItem(0, new ItemStack(Items.STICK));
        drawer.setItem(5, new ItemStack(Items.BREAD, 60));
        Player player = helper.makeMockPlayer();
        player.getInventory().items.set(7, new ItemStack(Items.BREAD, 10));
        CompoundTag data = new CompoundTag();
        data.putInt(StorageLogic.SLOT, 7);
        logic.onCustomAction(station, player, StorageLogic.STORE, data);
        helper.assertTrue(drawer.getItem(5).getCount() == 64, "the bread joins the bread first");
        helper.assertTrue(drawer.getItem(1).is(Items.BREAD) && drawer.getItem(1).getCount() == 6, "the rest takes the first free slot");
        helper.assertTrue(player.getInventory().items.get(7).isEmpty(), "and leaves the inventory");
        helper.succeed();
    }

    public static void aFullDrawerLeavesTheRest(GameTestHelper helper) {
        TestStation station = place(helper, Furniture.KITCHEN_DRAWER);
        StorageLogic logic = new StorageLogic(station);
        Container drawer = (Container) station.blockEntity();
        for (int i = 0; i < drawer.getContainerSize(); i++) {
            drawer.setItem(i, new ItemStack(Items.STONE, i == 3 ? 62 : 64));
        }
        Player player = helper.makeMockPlayer();
        player.getInventory().items.set(0, new ItemStack(Items.STONE, 5));
        CompoundTag data = new CompoundTag();
        data.putInt(StorageLogic.SLOT, 0);
        logic.onCustomAction(station, player, StorageLogic.STORE, data);
        helper.assertTrue(player.getInventory().items.get(0).getCount() == 3, "two fit, three stay in the hand's inventory");
        helper.succeed();
    }

    private static Block refurbished(GameTestHelper helper, Furniture furniture, String variant) {
        ResourceLocation id = furniture.blockId(variant);
        Block block = BuiltInRegistries.BLOCK.getOptional(id).orElse(null);
        if (block == null) {
            throw new IllegalStateException("Refurbished has no " + id);
        }
        return block;
    }

    /** The furniture's first variant at {@link #AT}, and a station on it that shares the block entity's kept state. */
    private static TestStation place(GameTestHelper helper, Furniture furniture) {
        helper.setBlock(AT, refurbished(helper, furniture, furniture.variants().get(0)));
        BlockPos pos = helper.absolutePos(AT);
        return new TestStation(helper.getLevel(), pos, furniture.type());
    }

    /** What the library gives a station's logic, at a real block: its state is the block entity's own. */
    private static final class TestStation implements StationContext {
        private final Level level;
        private final BlockPos pos;
        private final StationType type;
        private final List<Player> users = new ArrayList<>();

        TestStation(Level level, BlockPos pos, StationType type) {
            this.level = level;
            this.pos = pos;
            this.type = type;
        }

        @Override
        public StationType type() {
            return type;
        }

        @Override
        public Level level() {
            return level;
        }

        @Override
        public BlockPos pos() {
            return pos;
        }

        @Override
        public BlockState blockState() {
            return level.getBlockState(pos);
        }

        @Override
        public Direction facing() {
            return Direction.NORTH;
        }

        @Override
        public StationState state() {
            return ((StationHost) level.getBlockEntity(pos)).stationState();
        }

        @Override
        public Collection<? extends Player> users() {
            return users;
        }

        @Override
        public void effect(ResourceLocation effect, String node, CompoundTag data) {
        }
    }
}
