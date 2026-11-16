package com.pockyl.handson_furniture.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import com.pockyl.handson_furniture.HandsOnFurniture;

/** {@link FurnitureTests} on Forge. */
@GameTestHolder(HandsOnFurniture.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ModGameTests {
    private ModGameTests() {
    }

    @GameTest(template = "empty")
    public static void everyVariantIsAStation(GameTestHelper helper) {
        FurnitureTests.everyVariantIsAStation(helper);
    }

    @GameTest(template = "empty")
    public static void slotsAreTheFurnituresOwn(GameTestHelper helper) {
        FurnitureTests.slotsAreTheFurnituresOwn(helper);
    }

    @GameTest(template = "empty")
    public static void roomPastTheMenuIsKeptAndDropped(GameTestHelper helper) {
        FurnitureTests.roomPastTheMenuIsKeptAndDropped(helper);
    }

    @GameTest(template = "empty")
    public static void whatFallsOutGoesToTheInventoryElseTheFloor(GameTestHelper helper) {
        FurnitureTests.whatFallsOutGoesToTheInventoryElseTheFloor(helper);
    }

    @GameTest(template = "empty")
    public static void placesAreSavedWithTheBlock(GameTestHelper helper) {
        FurnitureTests.placesAreSavedWithTheBlock(helper);
    }

    @GameTest(template = "empty")
    public static void aClickOpensTheDoorAndItStaysOpen(GameTestHelper helper) {
        FurnitureTests.aClickOpensTheDoorAndItStaysOpen(helper);
    }

    @GameTest(template = "empty")
    public static void theTwoDrawersOpenOnTheirOwn(GameTestHelper helper) {
        FurnitureTests.theTwoDrawersOpenOnTheirOwn(helper);
    }

    @GameTest(template = "empty")
    public static void clientsAreToldOfAnOpenDoor(GameTestHelper helper) {
        FurnitureTests.clientsAreToldOfAnOpenDoor(helper);
    }

    @GameTest(template = "empty")
    public static void anEmptiedSlotForgetsItsPlace(GameTestHelper helper) {
        FurnitureTests.anEmptiedSlotForgetsItsPlace(helper);
    }

    @GameTest(template = "empty")
    public static void aShiftClickMergesThenFills(GameTestHelper helper) {
        FurnitureTests.aShiftClickMergesThenFills(helper);
    }

    @GameTest(template = "empty")
    public static void aFullDrawerLeavesTheRest(GameTestHelper helper) {
        FurnitureTests.aFullDrawerLeavesTheRest(helper);
    }
}
