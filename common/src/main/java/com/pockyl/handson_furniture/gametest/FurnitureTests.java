package com.pockyl.handson_furniture.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Quaternionf;

import com.mrcrayfish.furniture.refurbished.blockentity.IHomeControlDevice;
import com.mrcrayfish.furniture.refurbished.blockentity.ProcessingContainerBlockEntity;

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
import com.pockyl.handson_furniture.station.ProcessingSlots;
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

    /** A kitchen drawer's and a chest of drawers' two drawers open on their own; the block shows open while either is. */
    public static void theTwoDrawersOpenOnTheirOwn(GameTestHelper helper) {
        for (Furniture furniture : List.of(Furniture.KITCHEN_DRAWER, Furniture.DRAWER)) {
            TestStation station = place(helper, furniture);
            StorageLogic logic = new StorageLogic(station);
            Player player = helper.makeMockPlayer();
            CompoundTag lower = new CompoundTag();
            lower.putString(StorageLogic.CONTROL, Furniture.OPEN_LOWER);
            logic.onCustomAction(station, player, StorageLogic.DOOR, lower);
            helper.assertTrue(station.state().getControl(Furniture.OPEN_LOWER) == 1.0F && station.state().getControl(Furniture.OPEN) == 0.0F,
                    furniture + ": a click on the lower drawer pulls out that one only");
            helper.assertTrue(isOpenBlock(station), "and the drawer shows open");
            logic.onCustomAction(station, player, StorageLogic.DOOR, new CompoundTag());
            logic.onCustomAction(station, player, StorageLogic.DOOR, lower);
            helper.assertTrue(station.state().getControl(Furniture.OPEN) == 1.0F && isOpenBlock(station), "the upper one out, it stays open");
            logic.onCustomAction(station, player, StorageLogic.DOOR, new CompoundTag());
            helper.assertTrue(!isOpenBlock(station), "both in, it is shut");
        }
        helper.succeed();
    }

    /** A cooler's lid lifts and drops as a door does, and the cooler holds more than its menu's one row. */
    public static void aCoolersLidLiftsAndItHoldsMoreThanARow(GameTestHelper helper) {
        TestStation station = place(helper, Furniture.COOLER);
        StorageLogic logic = new StorageLogic(station);
        Player player = helper.makeMockPlayer();
        logic.onCustomAction(station, player, StorageLogic.DOOR, new CompoundTag());
        helper.assertTrue(station.state().getControl(Furniture.OPEN) == 1.0F && isOpenBlock(station), "a click lifts the lid");
        Container cooler = (Container) station.blockEntity();
        for (int i = 0; i < 12; i++) {
            player.getInventory().items.set(0, new ItemStack(i % 2 == 0 ? Items.MELON_SLICE : Items.COOKIE, 64));
            CompoundTag data = new CompoundTag();
            data.putInt(StorageLogic.SLOT, 0);
            logic.onCustomAction(station, player, StorageLogic.STORE, data);
        }
        helper.assertTrue(cooler.getItem(11).getCount() == 64, "twelve stacks go in, past the menu's nine slots");
        logic.onCustomAction(station, player, StorageLogic.DOOR, new CompoundTag());
        helper.assertTrue(station.state().getControl(Furniture.OPEN) == 0.0F && !isOpenBlock(station), "another click drops it shut");
        helper.succeed();
    }

    /** Refurbished's wrench links the furniture to power: with it in hand, using the block does not open the station. */
    public static void theWrenchUsesTheBlockAsBefore(GameTestHelper helper) {
        TestStation station = place(helper, Furniture.FREEZER);
        Player player = helper.makeMockPlayer();
        StationType.OpenCondition condition = Furniture.FREEZER.type().openCondition();
        helper.assertTrue(condition.test(player, station.pos(), station.blockState()), "an empty hand opens the freezer");
        Item wrench = BuiltInRegistries.ITEM.get(new ResourceLocation(Furniture.REFURBISHED, "wrench"));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(wrench));
        helper.assertTrue(!condition.test(player, station.pos(), station.blockState()), "the wrench is left to link it");
        helper.succeed();
    }

    /**
     * Refurbished freezes the freezer's first slot into its second: what freezes is swapped into the first wherever it stands,
     * keeping its place, and the second is cleared of what the result could not join.
     */
    public static void whatFreezesGoesWhereTheFreezerFreezes(GameTestHelper helper) {
        TestStation station = place(helper, Furniture.FREEZER);
        new StorageLogic(station);
        ProcessingContainerBlockEntity freezer = (ProcessingContainerBlockEntity) station.blockEntity();
        SlotRef first = SlotRef.of(Furniture.ITEMS, ProcessingSlots.FREEZER.input(0));
        SlotRef second = SlotRef.of(Furniture.ITEMS, ProcessingSlots.FREEZER.output(0));
        SlotRef far = SlotRef.of(Furniture.ITEMS, 30);
        ItemPose shelf = new ItemPose(9.0F, 11.0F, 8.0F, 0.0F, new Quaternionf(), 1.0F, -1);
        ItemPose floor = new ItemPose(3.0F, 1.0F, 5.0F, 0.0F, new Quaternionf(), 1.0F, -1);
        station.state().setItem(first, new ItemStack(Items.APPLE, 4));
        station.state().setPose(first, floor);
        station.state().setItem(second, new ItemStack(Items.BREAD, 2));
        station.state().setItem(far, new ItemStack(Items.ICE, 3));
        station.state().setPose(far, shelf);
        helper.assertTrue(ProcessingSlots.FREEZER.arrange(freezer), "the slots are set out");
        helper.assertTrue(station.state().getItem(first).is(Items.ICE) && shelf.equals(station.state().getPose(first)),
                "the ice is in the slot that freezes, still on the shelf");
        helper.assertTrue(station.state().getItem(far).is(Items.APPLE) && floor.equals(station.state().getPose(far)),
                "the apples took its slot and stayed on the floor");
        helper.assertTrue(station.state().getItem(second).isEmpty(), "the bread left the slot the packed ice comes out into");
        boolean breadKept = false;
        for (int i = 2; i < Furniture.FREEZER.slots(); i++) {
            breadKept |= station.state().getItem(SlotRef.of(Furniture.ITEMS, i)).is(Items.BREAD);
        }
        helper.assertTrue(breadKept, "and stays in the freezer");
        station.state().setItem(second, new ItemStack(Items.PACKED_ICE, 2));
        helper.assertTrue(!ProcessingSlots.FREEZER.arrange(freezer), "packed ice already out stays for more to join it, and for hoppers");
        helper.succeed();
    }

    /** The freezer's switch is Refurbished's own on and off: flipping it switches the freezer, and it shows what the freezer is. */
    public static void theFreezersSwitchIsItsPower(GameTestHelper helper) {
        TestStation station = place(helper, Furniture.FREEZER);
        StorageLogic logic = new StorageLogic(station);
        IHomeControlDevice freezer = (IHomeControlDevice) station.blockEntity();
        helper.assertTrue(!freezer.isDeviceEnabled() && station.state().getControl(Furniture.POWER) == 0.0F,
                "a new freezer is off, as Refurbished makes it, and its switch shows it");
        Player player = helper.makeMockPlayer();
        station.state().setControl(Furniture.POWER, 1.0F);
        logic.onControlChanged(station, player, Furniture.POWER, 1.0F);
        helper.assertTrue(freezer.isDeviceEnabled(), "the switch turns it on");
        helper.assertTrue(!isOpenBlock(station) && station.state().getControl(Furniture.OPEN) == 0.0F, "and opens no door");
        freezer.setDeviceState(false);
        logic.tick(station);
        helper.assertTrue(station.state().getControl(Furniture.POWER) == 0.0F, "turned off elsewhere, the switch shows it");
        helper.succeed();
    }

    /**
     * The microwave heats its first slot into its second, as the freezer freezes: a potato standing anywhere on the plate goes
     * into the slot that heats, keeping its place, and its switch is Refurbished's own on and off.
     */
    public static void whatHeatsGoesWhereTheMicrowaveHeats(GameTestHelper helper) {
        TestStation station = place(helper, Furniture.MICROWAVE);
        StorageLogic logic = new StorageLogic(station);
        SlotRef first = SlotRef.of(Furniture.ITEMS, ProcessingSlots.MICROWAVE.input(0));
        SlotRef far = SlotRef.of(Furniture.ITEMS, 9);
        ItemPose plate = new ItemPose(11.0F, 2.0F, 8.0F, 0.0F, new Quaternionf(), 1.0F, -1);
        station.state().setItem(first, new ItemStack(Items.APPLE));
        station.state().setItem(far, new ItemStack(Items.POTATO, 2));
        station.state().setPose(far, plate);
        helper.assertTrue(ProcessingSlots.MICROWAVE.arrange(station.blockEntity()), "the slots are set out");
        helper.assertTrue(station.state().getItem(first).is(Items.POTATO) && plate.equals(station.state().getPose(first)),
                "the potato is in the slot that heats, where it stood");
        helper.assertTrue(station.state().getItem(far).is(Items.APPLE), "and the apple waits in its slot");
        IHomeControlDevice microwave = (IHomeControlDevice) station.blockEntity();
        helper.assertTrue(!microwave.isDeviceEnabled(), "a new microwave is off");
        station.state().setControl(Furniture.POWER, 1.0F);
        logic.onControlChanged(station, helper.makeMockPlayer(), Furniture.POWER, 1.0F);
        helper.assertTrue(microwave.isDeviceEnabled() && station.state().getControl(Furniture.OPEN) == 0.0F,
                "the switch turns it on and opens no door");
        helper.succeed();
    }

    /**
     * The oven bakes three things at once, each of its first three slots into the one three further on: what bakes goes into
     * them wherever it stands, and an output is cleared of what its bake could not join.
     */
    public static void threeThingsBakeAtOnceInTheOven(GameTestHelper helper) {
        TestStation station = place(helper, Furniture.STOVE);
        new StorageLogic(station);
        for (int i = 0; i < 3; i++) {
            SlotRef waiting = SlotRef.of(Furniture.ITEMS, 20 + i);
            station.state().setItem(waiting, new ItemStack(Items.POTATO));
            station.state().setPose(waiting, new ItemPose(3.0F + 4.0F * i, 7.0F, 9.0F, 0.0F, new Quaternionf(), 1.0F, -1));
        }
        SlotRef output = SlotRef.of(Furniture.ITEMS, ProcessingSlots.OVEN.output(1));
        station.state().setItem(output, new ItemStack(Items.BREAD, 3));
        helper.assertTrue(ProcessingSlots.OVEN.arrange(station.blockEntity()), "the slots are set out");
        Container stove = (Container) station.blockEntity();
        for (int i = 0; i < 3; i++) {
            helper.assertTrue(stove.getItem(ProcessingSlots.OVEN.input(i)).is(Items.POTATO), "a potato bakes in space " + i);
        }
        helper.assertTrue(station.state().getItem(output).isEmpty(), "the bread left the slot a baked potato comes out into");
        helper.assertTrue(!ProcessingSlots.OVEN.arrange(station.blockEntity()), "set out, the slots stay as they are");
        helper.succeed();
    }

    /** The stove's knob is Refurbished's own on and off, as its menu's power button: the stove is switched with it. */
    public static void theStovesKnobIsItsPower(GameTestHelper helper) {
        TestStation station = place(helper, Furniture.STOVE);
        StorageLogic logic = new StorageLogic(station);
        IHomeControlDevice stove = (IHomeControlDevice) station.blockEntity();
        helper.assertTrue(!stove.isDeviceEnabled() && station.state().getControl(Furniture.POWER) == 0.0F, "a new stove is off");
        Player player = helper.makeMockPlayer();
        station.state().setControl(Furniture.POWER, 1.0F);
        logic.onControlChanged(station, player, Furniture.POWER, 1.0F);
        helper.assertTrue(stove.isDeviceEnabled(), "the knob turns it on");
        station.state().setControl(Furniture.POWER, 0.0F);
        logic.onControlChanged(station, player, Furniture.POWER, 0.0F);
        helper.assertTrue(!stove.isDeviceEnabled(), "and off again");
        logic.onCustomAction(station, player, StorageLogic.DOOR, new CompoundTag());
        helper.assertTrue(station.state().getControl(Furniture.OPEN) == 1.0F && isOpenBlock(station), "a click lets the oven door down");
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
