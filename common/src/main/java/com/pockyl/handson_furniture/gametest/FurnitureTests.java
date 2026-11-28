package com.pockyl.handson_furniture.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import org.joml.Quaternionf;

import com.mrcrayfish.furniture.refurbished.blockentity.IHomeControlDevice;
import com.mrcrayfish.furniture.refurbished.blockentity.MailboxBlockEntity;
import com.mrcrayfish.furniture.refurbished.blockentity.ProcessingContainerBlockEntity;
import com.mrcrayfish.furniture.refurbished.electricity.IModuleNode;
import com.mrcrayfish.furniture.refurbished.inventory.ElectricityGeneratorMenu;
import com.mrcrayfish.furniture.refurbished.mail.Mailbox;

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
import com.pockyl.handson_furniture.FurnitureLight;
import com.pockyl.handson_furniture.station.ProcessingSlots;
import com.pockyl.handson_furniture.station.RoomyContainer;
import com.pockyl.handson_furniture.station.StorageLogic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

/**
 * The furniture as stations on the server: every variant Refurbished makes opens one, the station's slots are the furniture's
 * own, where each item stands is saved with the block, and a shift-click stores as the furniture's menu would. The same on
 * every loader: each registers {@link #TESTS} in its own way ({@code NeoForgeGameTests} on NeoForge, {@code FabricFurnitureTests}
 * on Fabric). Tests are registry entries since Minecraft 1.21.5: a function and a test instance on vanilla's empty structure each.
 */
public final class FurnitureTests {
    private static final BlockPos AT = new BlockPos(1, 1, 1);

    /** A test: its name (snake case, as test instances are named), how many ticks it may take, and what it does. */
    public record Test(String name, int timeout, Consumer<GameTestHelper> function) {
    }

    /** Every test, for the loaders to register. */
    public static final List<Test> TESTS = List.of(
            new Test("every_variant_is_a_station", 100, FurnitureTests::everyVariantIsAStation),
            new Test("slots_are_the_furnitures_own", 100, FurnitureTests::slotsAreTheFurnituresOwn),
            new Test("room_past_the_menu_is_kept_and_dropped", 100, FurnitureTests::roomPastTheMenuIsKeptAndDropped),
            new Test("what_falls_out_goes_to_the_inventory_else_the_floor", 100, FurnitureTests::whatFallsOutGoesToTheInventoryElseTheFloor),
            new Test("places_are_saved_with_the_block", 100, FurnitureTests::placesAreSavedWithTheBlock),
            new Test("a_click_opens_the_door_and_it_stays_open", 100, FurnitureTests::aClickOpensTheDoorAndItStaysOpen),
            new Test("every_door_has_its_sounds", 100, FurnitureTests::everyDoorHasItsSounds),
            new Test("the_two_drawers_open_on_their_own", 100, FurnitureTests::theTwoDrawersOpenOnTheirOwn),
            new Test("a_coolers_lid_lifts_and_it_holds_more_than_a_row", 100, FurnitureTests::aCoolersLidLiftsAndItHoldsMoreThanARow),
            new Test("the_wrench_uses_the_block_as_before", 100, FurnitureTests::theWrenchUsesTheBlockAsBefore),
            new Test("what_freezes_goes_where_the_freezer_freezes", 100, FurnitureTests::whatFreezesGoesWhereTheFreezerFreezes),
            new Test("the_freezers_switch_is_its_power", 100, FurnitureTests::theFreezersSwitchIsItsPower),
            new Test("what_heats_goes_where_the_microwave_heats", 100, FurnitureTests::whatHeatsGoesWhereTheMicrowaveHeats),
            new Test("three_things_bake_at_once_in_the_oven", 100, FurnitureTests::threeThingsBakeAtOnceInTheOven),
            new Test("the_stoves_knob_is_its_power", 100, FurnitureTests::theStovesKnobIsItsPower),
            new Test("clients_are_told_of_an_open_door", 100, FurnitureTests::clientsAreToldOfAnOpenDoor),
            new Test("an_emptied_slot_forgets_its_place", 100, FurnitureTests::anEmptiedSlotForgetsItsPlace),
            new Test("a_shift_click_merges_then_fills", 100, FurnitureTests::aShiftClickMergesThenFills),
            new Test("a_full_drawer_leaves_the_rest", 100, FurnitureTests::aFullDrawerLeavesTheRest),
            new Test("a_powered_freezer_freezes_what_stands_anywhere", 900, FurnitureTests::aPoweredFreezerFreezesWhatStandsAnywhere),
            new Test("a_powered_microwave_heats_what_stands_on_the_plate", 400, FurnitureTests::aPoweredMicrowaveHeatsWhatStandsOnThePlate),
            new Test("lamps_light_the_room", 100, FurnitureTests::lampsLightTheRoom),
            new Test("a_microwave_heats_only_with_its_door_shut_and_lights_the_room", 700, FurnitureTests::aMicrowaveHeatsOnlyWithItsDoorShutAndLightsTheRoom),
            new Test("a_microwave_switches_itself_off_when_done", 700, FurnitureTests::aMicrowaveSwitchesItselfOffWhenDone),
            new Test("a_right_click_puts_one_down", 100, FurnitureTests::aRightClickPutsOneDown),
            new Test("a_generator_burns_fuel_lying_anywhere", 100, FurnitureTests::aGeneratorBurnsFuelLyingAnywhere),
            new Test("a_generator_moves_the_empty_bucket_aside", 100, FurnitureTests::aGeneratorMovesTheEmptyBucketAside),
            new Test("a_mail_boxs_front_checks_the_mail", 100, FurnitureTests::aMailBoxsFrontChecksTheMail),
            new Test("menus_and_comparators_count_refurbisheds_slots", 100, FurnitureTests::menusAndComparatorsCountRefurbishedsSlots));

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
        BlockEntity loaded = BlockEntity.loadStatic(fridge.getBlockPos(), fridge.getBlockState(),
                fridge.saveWithFullMetadata(registries(helper)), registries(helper));
        helper.assertTrue(loaded instanceof Container container && container.getContainerSize() == Furniture.FRIDGE.slots()
                && container.getItem(far).is(Items.CARROT) && container.getItem(far).getCount() == 5, "a stack past the menu's slots is saved");
        helper.destroyBlock(AT);
        helper.assertItemEntityCountIs(Items.CARROT, AT, 2.0, 5);
        helper.succeed();
    }

    public static void whatFallsOutGoesToTheInventoryElseTheFloor(GameTestHelper helper) {
        TestStation station = place(helper, Furniture.FRIDGE);
        StorageLogic logic = new StorageLogic(station);
        Player player = helper.makeMockPlayer(GameType.CREATIVE);
        SlotRef apple = SlotRef.of(Furniture.ITEMS, 40);
        station.state().setItem(apple, new ItemStack(Items.APPLE, 3));
        ActionHandler.apply(station, logic, player, new StationAction.Click(new StationAction.Target.Station(apple), 0,
                StationAction.ClickKind.SPILL));
        helper.assertTrue(station.state().getItem(apple).isEmpty() && player.getInventory().countItem(Items.APPLE) == 3,
                "with room, it falls into the inventory");
        for (int i = 0; i < player.getInventory().getNonEquipmentItems().size(); i++) {
            player.getInventory().getNonEquipmentItems().set(i, new ItemStack(Items.STONE, 64));
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
        CompoundTag saved = fridge.saveWithFullMetadata(registries(helper));
        BlockEntity loaded = BlockEntity.loadStatic(fridge.getBlockPos(), fridge.getBlockState(), saved, registries(helper));
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
        Player player = helper.makeMockPlayer(GameType.CREATIVE);
        helper.assertTrue(station.state().getControl(Furniture.OPEN) == 0.0F, "a new fridge is shut");
        logic.onCustomAction(station, player, StorageLogic.DOOR, new CompoundTag());
        helper.assertTrue(station.state().getControl(Furniture.OPEN) == 1.0F, "a click opens it");
        helper.assertTrue(isOpenBlock(station), "as the fridge itself shows it: " + station.level().getBlockState(station.pos()));
        BlockEntity fridge = station.blockEntity();
        BlockEntity loaded = BlockEntity.loadStatic(fridge.getBlockPos(), fridge.getBlockState(),
                fridge.saveWithFullMetadata(registries(helper)), registries(helper));
        helper.assertTrue(((StationHost) loaded).stationState().getControl(Furniture.OPEN) == 1.0F, "left open, it is kept open");
        logic.onCustomAction(station, player, StorageLogic.DOOR, new CompoundTag());
        helper.assertTrue(station.state().getControl(Furniture.OPEN) == 0.0F && isOpenBlock(station),
                "another click shuts the door; the fridge waits for it to meet the frame");
        helper.runAfterDelay(StorageLogic.SHUT_TICKS + 1, () -> {
            logic.tick(station);
            helper.assertTrue(!isOpenBlock(station), "then the fridge itself is shut");
            helper.succeed();
        });
    }

    /**
     * Every piece with a sound of Refurbished's has it under the id its doors play (Refurbished's own open and close no longer
     * sound at a station), and its close sound comes before the furniture's own shut.
     */
    public static void everyDoorHasItsSounds(GameTestHelper helper) {
        for (Furniture furniture : Furniture.values()) {
            for (boolean open : new boolean[] {true, false}) {
                Identifier id = furniture.doorSoundId(open);
                helper.assertTrue(id == null || BuiltInRegistries.SOUND_EVENT.containsKey(id), furniture + ": no sound " + id);
            }
            helper.assertTrue(furniture.closeSoundTicks() < StorageLogic.SHUT_TICKS, furniture + ": the knock comes before the shut");
        }
        helper.assertTrue(Furniture.DRAWER.doorSoundId(false) != null && Furniture.GENERATOR.doorSoundId(true) == null,
                "the chest of drawers sounds, the generator's panel has no sound of Refurbished's");
        helper.succeed();
    }

    /** A kitchen drawer's and a chest of drawers' two drawers open on their own; the block shows open while either is. */
    public static void theTwoDrawersOpenOnTheirOwn(GameTestHelper helper) {
        for (Furniture furniture : List.of(Furniture.KITCHEN_DRAWER, Furniture.DRAWER)) {
            TestStation station = place(helper, furniture);
            StorageLogic logic = new StorageLogic(station);
            Player player = helper.makeMockPlayer(GameType.CREATIVE);
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
            logic.onClose(station, player);
            helper.assertTrue(!isOpenBlock(station), "both in, it is shut, at once when the player leaves");
        }
        helper.succeed();
    }

    /** A cooler's lid lifts and drops as a door does, and the cooler holds more than its menu's one row. */
    public static void aCoolersLidLiftsAndItHoldsMoreThanARow(GameTestHelper helper) {
        TestStation station = place(helper, Furniture.COOLER);
        StorageLogic logic = new StorageLogic(station);
        Player player = helper.makeMockPlayer(GameType.CREATIVE);
        logic.onCustomAction(station, player, StorageLogic.DOOR, new CompoundTag());
        helper.assertTrue(station.state().getControl(Furniture.OPEN) == 1.0F && isOpenBlock(station), "a click lifts the lid");
        Container cooler = (Container) station.blockEntity();
        for (int i = 0; i < 12; i++) {
            player.getInventory().getNonEquipmentItems().set(0, new ItemStack(i % 2 == 0 ? Items.MELON_SLICE : Items.COOKIE, 64));
            CompoundTag data = new CompoundTag();
            data.putInt(StorageLogic.SLOT, 0);
            logic.onCustomAction(station, player, StorageLogic.STORE, data);
        }
        helper.assertTrue(cooler.getItem(11).getCount() == 64, "twelve stacks go in, past the menu's nine slots");
        logic.onCustomAction(station, player, StorageLogic.DOOR, new CompoundTag());
        logic.onClose(station, player);
        helper.assertTrue(station.state().getControl(Furniture.OPEN) == 0.0F && !isOpenBlock(station), "another click drops it shut");
        helper.succeed();
    }

    /** Refurbished's wrench links the furniture to power: with it in hand, using the block does not open the station. */
    public static void theWrenchUsesTheBlockAsBefore(GameTestHelper helper) {
        TestStation station = place(helper, Furniture.FREEZER);
        Player player = helper.makeMockPlayer(GameType.CREATIVE);
        StationType.OpenCondition condition = Furniture.FREEZER.type().openCondition();
        helper.assertTrue(condition.test(player, station.pos(), station.blockState()), "an empty hand opens the freezer");
        Item wrench = BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath(Furniture.REFURBISHED, "wrench"));
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
        Player player = helper.makeMockPlayer(GameType.CREATIVE);
        station.state().setControl(Furniture.POWER, 1.0F);
        logic.onControlChanged(station, player, Furniture.POWER, 1.0F);
        helper.assertTrue(freezer.isDeviceEnabled(), "the switch turns it on");
        helper.assertTrue(!isOpenBlock(station) && station.state().getControl(Furniture.OPEN) == 0.0F, "and opens no door");
        freezer.setDeviceState(false);
        logic.tick(station);
        helper.assertTrue(station.state().getControl(Furniture.POWER) == 0.0F, "turned off elsewhere, the switch shows it");
        helper.succeed();
    }

    /** An open fridge, an open microwave and a lit oven light the room; shut or off they are dark, and the freezer always. */
    public static void lampsLightTheRoom(GameTestHelper helper) {
        for (Furniture furniture : List.of(Furniture.FRIDGE, Furniture.FREEZER, Furniture.MICROWAVE, Furniture.STOVE)) {
            for (String variant : furniture.variants()) {
                Block block = refurbished(helper, furniture, variant);
                for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                    int expected = switch (furniture) {
                        case FRIDGE -> flag(state, "open") ? FurnitureLight.FRIDGE_OPEN : 0;
                        case MICROWAVE -> flag(state, "open") ? FurnitureLight.MICROWAVE : 0;
                        case STOVE -> flag(state, "lit") ? FurnitureLight.OVEN_LIT : 0;
                        default -> 0;
                    };
                    helper.assertTrue(state.getLightEmission() == expected, state + " gives light " + expected + ", not " + state.getLightEmission());
                }
            }
        }
        helper.succeed();
    }

    private static boolean flag(BlockState state, String name) {
        for (net.minecraft.world.level.block.state.properties.Property<?> property : state.getProperties()) {
            if (name.equals(property.getName()) && state.getValue(property) instanceof Boolean value) {
                return value;
            }
        }
        return false;
    }

    /**
     * Linked to power and switched on, the freezer really freezes: a bucket of water standing anywhere on its shelves comes out
     * as ice, its bucket left beside it, both still in the freezer. Refurbished's ticks do it, as in a world.
     */
    public static void aPoweredFreezerFreezesWhatStandsAnywhere(GameTestHelper helper) {
        TestStation station = place(helper, Furniture.FREEZER);
        new StorageLogic(station);
        BlockEntity freezer = station.blockEntity();
        helper.assertTrue(StorageLogic.lacksPower(freezer), "a freezer nobody linked to a generator has no power");
        SlotRef far = SlotRef.of(Furniture.ITEMS, 30);
        station.state().setItem(far, new ItemStack(Items.WATER_BUCKET));
        station.state().setPose(far, new ItemPose(9.0F, 11.0F, 8.0F, 0.0F, new Quaternionf(), 1.0F, -1));
        ((IHomeControlDevice) freezer).setDeviceState(true);
        helper.onEachTick(() -> power(freezer));
        helper.succeedWhen(() -> {
            helper.assertTrue(!StorageLogic.lacksPower(freezer), "powered");
            helper.assertTrue(holds(freezer, Items.ICE) && holds(freezer, Items.BUCKET), "the water froze into ice, the bucket stays");
        });
    }

    /** Linked to power and switched on, the microwave bakes a potato standing anywhere on its plate. */
    public static void aPoweredMicrowaveHeatsWhatStandsOnThePlate(GameTestHelper helper) {
        TestStation station = place(helper, Furniture.MICROWAVE);
        new StorageLogic(station);
        BlockEntity microwave = station.blockEntity();
        SlotRef far = SlotRef.of(Furniture.ITEMS, 12);
        station.state().setItem(far, new ItemStack(Items.POTATO));
        station.state().setPose(far, new ItemPose(10.0F, 2.0F, 9.0F, 0.0F, new Quaternionf(), 1.0F, -1));
        ((IHomeControlDevice) microwave).setDeviceState(true);
        helper.onEachTick(() -> power(microwave));
        helper.succeedWhen(() -> helper.assertTrue(holds(microwave, Items.BAKED_POTATO) && !holds(microwave, Items.POTATO),
                "the potato is baked"));
    }

    /**
     * With its door open the microwave does not heat, as a real one: the potato waits, and bakes once the door is shut. While
     * it heats behind the shut glass door it lights the room.
     */
    public static void aMicrowaveHeatsOnlyWithItsDoorShutAndLightsTheRoom(GameTestHelper helper) {
        TestStation station = place(helper, Furniture.MICROWAVE);
        StorageLogic logic = new StorageLogic(station);
        BlockEntity microwave = station.blockEntity();
        SlotRef far = SlotRef.of(Furniture.ITEMS, 12);
        station.state().setItem(far, new ItemStack(Items.POTATO));
        station.state().setPose(far, new ItemPose(10.0F, 2.0F, 9.0F, 0.0F, new Quaternionf(), 1.0F, -1));
        ((IHomeControlDevice) microwave).setDeviceState(true);
        Player player = helper.makeMockPlayer(GameType.CREATIVE);
        logic.onCustomAction(station, player, StorageLogic.DOOR, new CompoundTag());
        helper.onEachTick(() -> power(microwave));
        boolean[] lit = new boolean[1];
        int[] shutFor = {-1};
        helper.runAtTickTime(260, () -> {
            helper.assertTrue(holds(microwave, Items.POTATO) && !holds(microwave, Items.BAKED_POTATO),
                    "with the door open, longer than a potato takes, it is not baked");
            logic.onCustomAction(station, player, StorageLogic.DOOR, new CompoundTag());
            // The furniture's own shut, which the station's tick would bring a moment later: its open door's light goes.
            logic.onClose(station, player);
            shutFor[0] = 0;
        });
        helper.onEachTick(() -> {
            if (shutFor[0] >= 0 && shutFor[0]++ > 20 && !holds(microwave, Items.BAKED_POTATO)) {
                lit[0] |= helper.getLevel().getBrightness(net.minecraft.world.level.LightLayer.BLOCK, station.pos().above())
                        >= FurnitureLight.MICROWAVE - 1;
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(station.state().getControl(Furniture.OPEN) == 0.0F, "the door was shut");
            helper.assertTrue(holds(microwave, Items.BAKED_POTATO), "shut, it bakes");
            helper.assertTrue(lit[0], "and lit the room while it heated");
        });
    }

    /**
     * When everything in it is heated, the microwave switches itself off. Switched on with nothing in it that heats, it does not
     * start: its switch goes back.
     */
    public static void aMicrowaveSwitchesItselfOffWhenDone(GameTestHelper helper) {
        TestStation station = place(helper, Furniture.MICROWAVE);
        StorageLogic logic = new StorageLogic(station);
        BlockEntity microwave = station.blockEntity();
        IHomeControlDevice device = (IHomeControlDevice) microwave;
        helper.onEachTick(() -> power(microwave));
        SlotRef bread = SlotRef.of(Furniture.ITEMS, 5);
        station.state().setItem(bread, new ItemStack(Items.BREAD));
        boolean[] refused = new boolean[1];
        helper.runAtTickTime(5, () -> {
            station.state().setControl(Furniture.POWER, 1.0F);
            logic.onControlChanged(station, helper.makeMockPlayer(GameType.CREATIVE), Furniture.POWER, 1.0F);
            refused[0] = !device.isDeviceEnabled() && station.state().getControl(Furniture.POWER) == 0.0F;
            station.state().setItem(SlotRef.of(Furniture.ITEMS, 9), new ItemStack(Items.POTATO, 2));
            station.state().setControl(Furniture.POWER, 1.0F);
            logic.onControlChanged(station, helper.makeMockPlayer(GameType.CREATIVE), Furniture.POWER, 1.0F);
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(refused[0], "with only bread in it, it would not start");
            helper.assertTrue(count(microwave, Items.BAKED_POTATO) == 2, "both potatoes baked");
            helper.assertTrue(!device.isDeviceEnabled(), "and it switched itself off");
        });
    }

    private static int count(BlockEntity furniture, Item item) {
        Container container = (Container) furniture;
        int count = 0;
        for (int i = 0; i < container.getContainerSize(); i++) {
            if (container.getItem(i).is(item)) {
                count += container.getItem(i).getCount();
            }
        }
        return count;
    }

    /** A right click puts one of the stack in the hand down, the rest stays in the hand. */
    public static void aRightClickPutsOneDown(GameTestHelper helper) {
        TestStation station = place(helper, Furniture.FRIDGE);
        StorageLogic logic = new StorageLogic(station);
        Player player = helper.makeMockPlayer(GameType.CREATIVE);
        player.inventoryMenu.setCarried(new ItemStack(Items.APPLE, 5));
        SlotRef slot = SlotRef.of(Furniture.ITEMS, 7);
        ItemPose shelf = new ItemPose(8.0F, 6.0F, 10.0F, 0.0F, new Quaternionf(), 1.0F, -1);
        helper.assertTrue(ActionHandler.apply(station, logic, player, new StationAction.Place(slot, shelf, 1)), "put down");
        helper.assertTrue(station.state().getItem(slot).getCount() == 1 && player.inventoryMenu.getCarried().getCount() == 4,
                "one on the shelf, four in the hand");
        SlotRef next = SlotRef.of(Furniture.ITEMS, 8);
        helper.assertTrue(ActionHandler.apply(station, logic, player, new StationAction.Place(next, shelf, 0)), "a left click");
        helper.assertTrue(station.state().getItem(next).getCount() == 4 && player.inventoryMenu.getCarried().isEmpty(),
                "puts the rest down at once");
        helper.succeed();
    }

    /**
     * What a generator linked by the wrench does each tick: the furniture receives power (Refurbished's ticker reads it before
     * the furniture's own tick).
     */
    private static void power(BlockEntity furniture) {
        IModuleNode node = (IModuleNode) furniture;
        node.setNodeReceivingPower(true);
        if (!node.isNodePowered()) {
            node.setNodePowered(true);
        }
    }

    private static boolean holds(BlockEntity furniture, Item item) {
        Container container = (Container) furniture;
        for (int i = 0; i < container.getContainerSize(); i++) {
            if (container.getItem(i).is(item)) {
                return true;
            }
        }
        return false;
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
        logic.onControlChanged(station, helper.makeMockPlayer(GameType.CREATIVE), Furniture.POWER, 1.0F);
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
        Player player = helper.makeMockPlayer(GameType.CREATIVE);
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

    /**
     * The generator burns its one slot while fuel lies anywhere on its grate: a fuel goes into that slot keeping its place, and
     * what does not burn leaves it. Its key is Refurbished's own on and off, and switched on it burns what lies on the grate.
     */
    public static void aGeneratorBurnsFuelLyingAnywhere(GameTestHelper helper) {
        TestStation station = place(helper, Furniture.GENERATOR);
        StorageLogic logic = new StorageLogic(station);
        BlockEntity generator = station.blockEntity();
        SlotRef first = SlotRef.of(Furniture.ITEMS, ProcessingSlots.GENERATOR.input(0));
        SlotRef far = SlotRef.of(Furniture.ITEMS, 20);
        ItemPose grate = new ItemPose(9.0F, 7.0F, 6.0F, 0.0F, new Quaternionf(), 1.0F, -1);
        station.state().setItem(first, new ItemStack(Items.DIRT));
        station.state().setItem(far, new ItemStack(Items.COAL, 4));
        station.state().setPose(far, grate);
        helper.assertTrue(ProcessingSlots.GENERATOR.arrange(generator), "the slots are set out");
        helper.assertTrue(station.state().getItem(first).is(Items.COAL) && grate.equals(station.state().getPose(first)),
                "the coal is in the slot that burns, where it lay");
        helper.assertTrue(holds(generator, Items.DIRT), "the dirt stays in the generator");
        helper.assertTrue(!StorageLogic.isSwitchedOn(generator) && station.state().getControl(Furniture.POWER) == 0.0F,
                "a new generator is off, and its key shows it");
        Player player = helper.makeMockPlayer(GameType.CREATIVE);
        station.state().setControl(Furniture.POWER, 1.0F);
        logic.onControlChanged(station, player, Furniture.POWER, 1.0F);
        helper.assertTrue(StorageLogic.isSwitchedOn(generator), "the key switches it on");
        helper.assertTrue(station.state().getControl(Furniture.OPEN) == 0.0F, "and opens no panel");
        helper.succeedWhen(() -> {
            helper.assertTrue(count(generator, Items.COAL) == 3, "it burns one coal");
            helper.assertTrue(flag(helper.getLevel().getBlockState(station.pos()), "powered"), "and runs");
        });
    }

    /**
     * A lava bucket burns as fuel and leaves its bucket where it lay (Refurbished lost it), moved off the slot that burns; the
     * coal beside it goes in next.
     */
    public static void aGeneratorMovesTheEmptyBucketAside(GameTestHelper helper) {
        TestStation station = place(helper, Furniture.GENERATOR);
        StorageLogic logic = new StorageLogic(station);
        BlockEntity generator = station.blockEntity();
        station.state().setItem(SlotRef.of(Furniture.ITEMS, 0), new ItemStack(Items.LAVA_BUCKET));
        station.state().setItem(SlotRef.of(Furniture.ITEMS, 5), new ItemStack(Items.COAL, 2));
        station.state().setControl(Furniture.POWER, 1.0F);
        logic.onControlChanged(station, helper.makeMockPlayer(GameType.CREATIVE), Furniture.POWER, 1.0F);
        helper.succeedWhen(() -> {
            helper.assertTrue(holds(generator, Items.BUCKET), "the lava burnt, its bucket stays in the generator");
            helper.assertTrue(((Container) generator).getItem(0).is(Items.COAL), "and the coal waits in the slot that burns");
        });
    }

    /**
     * Mail comes into a mail box past its menu's row, into the room, and puts its flag up; letting its front down checks the
     * mail as Refurbished's menu does: the flag goes down and the box is the player's. Its front shuts as a door does.
     */
    public static void aMailBoxsFrontChecksTheMail(GameTestHelper helper) {
        TestStation station = place(helper, Furniture.MAIL_BOX);
        StorageLogic logic = new StorageLogic(station);
        MailboxBlockEntity mailbox = (MailboxBlockEntity) station.blockEntity();
        int own = ((RoomyContainer) mailbox).handson$ownSize();
        for (int i = 0; i < own; i++) {
            mailbox.setItem(i, new ItemStack(Items.PAPER, 64));
        }
        helper.assertTrue(mailbox.deliverItem(new ItemStack(Items.WRITABLE_BOOK)), "mail comes in past the menu's " + own + " slots");
        helper.assertTrue(holds(mailbox, Items.WRITABLE_BOOK), "and stays in the mail box");
        helper.assertTrue(flag(station.blockState(), "enabled"), "its flag goes up");
        Player player = helper.makeMockPlayer(GameType.CREATIVE);
        logic.onCustomAction(station, player, StorageLogic.DOOR, new CompoundTag());
        helper.assertTrue(station.state().getControl(Furniture.OPEN) == 1.0F, "a click lets the front down");
        helper.assertTrue(!flag(station.blockState(), "enabled"), "and the flag goes down");
        Mailbox box = mailbox.getMailbox().orElse(null);
        helper.assertTrue(box == null || box.hasOwner(), "the mail box is the player's");
        logic.onCustomAction(station, player, StorageLogic.DOOR, new CompoundTag());
        helper.assertTrue(station.state().getControl(Furniture.OPEN) == 0.0F, "another click shuts it");
        helper.succeed();
    }

    /**
     * For a player without this mod, Refurbished's menus work as before the room was added: a shift-click puts fuel into the
     * generator's slot (it moved nothing while every slot of the room counted as the menu's), and a comparator reads a piece
     * whose menu slots are full as full.
     */
    public static void menusAndComparatorsCountRefurbishedsSlots(GameTestHelper helper) {
        TestStation generator = place(helper, Furniture.GENERATOR);
        Container fuel = (Container) generator.blockEntity();
        Player player = helper.makeMockPlayer(GameType.CREATIVE);
        player.getInventory().getNonEquipmentItems().set(0, new ItemStack(Items.COAL, 5));
        ElectricityGeneratorMenu menu = new ElectricityGeneratorMenu(0, player.getInventory(), fuel, new SimpleContainerData(6));
        // The menu's slots: the generator's one, then the player's 27 and their hotbar.
        menu.quickMoveStack(player, 1 + 27);
        helper.assertTrue(fuel.getItem(0).is(Items.COAL) && fuel.getItem(0).getCount() == 5, "a shift-click puts the coal in");
        helper.assertTrue(player.getInventory().getNonEquipmentItems().get(0).isEmpty(), "out of the hotbar");

        TestStation fridge = place(helper, Furniture.FRIDGE);
        Container shelves = (Container) fridge.blockEntity();
        int own = ((RoomyContainer) shelves).handson$ownSize();
        helper.assertTrue(own > 0 && own < shelves.getContainerSize(), "the fridge has room past its menu's " + own + " slots");
        for (int i = 0; i < own; i++) {
            shelves.setItem(i, new ItemStack(Items.APPLE, 64));
        }
        BlockState state = helper.getLevel().getBlockState(fridge.pos());
        helper.assertTrue(state.getAnalogOutputSignal(helper.getLevel(), fridge.pos(), Direction.NORTH) == 15, "full menu slots read full");
        for (int i = 0; i < own; i++) {
            shelves.setItem(i, ItemStack.EMPTY);
        }
        shelves.setItem(own + 3, new ItemStack(Items.APPLE));
        helper.assertTrue(state.getAnalogOutputSignal(helper.getLevel(), fridge.pos(), Direction.NORTH) == 1, "an apple in the room reads as something");
        helper.succeed();
    }

    /** A player who joins later learns from the block's update that its door was left open, and what stands behind it. */
    public static void clientsAreToldOfAnOpenDoor(GameTestHelper helper) {
        TestStation station = place(helper, Furniture.FRIDGE);
        StorageLogic logic = new StorageLogic(station);
        SlotRef egg = SlotRef.of(Furniture.ITEMS, 2);
        station.state().setItem(egg, new ItemStack(Items.EGG, 3));
        station.state().setPose(egg, new ItemPose(4.0F, 6.0F, 5.0F, 1.0F, new Quaternionf(), 1.0F, -1));
        logic.onCustomAction(station, helper.makeMockPlayer(GameType.CREATIVE), StorageLogic.DOOR, new CompoundTag());
        BlockEntity fridge = station.blockEntity();
        helper.assertTrue(fridge.getUpdatePacket() != null, "the fridge sends its data to clients");
        // As a client takes it: into the block entity it already has there.
        BlockEntity seen = fridge.getType().create(fridge.getBlockPos(), fridge.getBlockState());
        seen.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, registries(helper), fridge.getUpdateTag(registries(helper))));
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
        Player player = helper.makeMockPlayer(GameType.CREATIVE);
        player.getInventory().getNonEquipmentItems().set(7, new ItemStack(Items.BREAD, 10));
        CompoundTag data = new CompoundTag();
        data.putInt(StorageLogic.SLOT, 7);
        logic.onCustomAction(station, player, StorageLogic.STORE, data);
        helper.assertTrue(drawer.getItem(5).getCount() == 64, "the bread joins the bread first");
        helper.assertTrue(drawer.getItem(1).is(Items.BREAD) && drawer.getItem(1).getCount() == 6, "the rest takes the first free slot");
        helper.assertTrue(player.getInventory().getNonEquipmentItems().get(7).isEmpty(), "and leaves the inventory");
        helper.succeed();
    }

    public static void aFullDrawerLeavesTheRest(GameTestHelper helper) {
        TestStation station = place(helper, Furniture.KITCHEN_DRAWER);
        StorageLogic logic = new StorageLogic(station);
        Container drawer = (Container) station.blockEntity();
        for (int i = 0; i < drawer.getContainerSize(); i++) {
            drawer.setItem(i, new ItemStack(Items.STONE, i == 3 ? 62 : 64));
        }
        Player player = helper.makeMockPlayer(GameType.CREATIVE);
        player.getInventory().getNonEquipmentItems().set(0, new ItemStack(Items.STONE, 5));
        CompoundTag data = new CompoundTag();
        data.putInt(StorageLogic.SLOT, 0);
        logic.onCustomAction(station, player, StorageLogic.STORE, data);
        helper.assertTrue(player.getInventory().getNonEquipmentItems().get(0).getCount() == 3, "two fit, three stay in the hand's inventory");
        helper.succeed();
    }

    private static HolderLookup.Provider registries(GameTestHelper helper) {
        return helper.getLevel().registryAccess();
    }

    private static Block refurbished(GameTestHelper helper, Furniture furniture, String variant) {
        Identifier id = furniture.blockId(variant);
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
        public void effect(Identifier effect, String node, CompoundTag data) {
        }
    }
}
