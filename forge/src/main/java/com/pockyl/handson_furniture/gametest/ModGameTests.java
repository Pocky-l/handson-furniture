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
    public static void everyDoorHasItsSounds(GameTestHelper helper) {
        FurnitureTests.everyDoorHasItsSounds(helper);
    }

    @GameTest(template = "empty")
    public static void theTwoDrawersOpenOnTheirOwn(GameTestHelper helper) {
        FurnitureTests.theTwoDrawersOpenOnTheirOwn(helper);
    }

    @GameTest(template = "empty")
    public static void aCoolersLidLiftsAndItHoldsMoreThanARow(GameTestHelper helper) {
        FurnitureTests.aCoolersLidLiftsAndItHoldsMoreThanARow(helper);
    }

    @GameTest(template = "empty")
    public static void theWrenchUsesTheBlockAsBefore(GameTestHelper helper) {
        FurnitureTests.theWrenchUsesTheBlockAsBefore(helper);
    }

    @GameTest(template = "empty")
    public static void whatFreezesGoesWhereTheFreezerFreezes(GameTestHelper helper) {
        FurnitureTests.whatFreezesGoesWhereTheFreezerFreezes(helper);
    }

    @GameTest(template = "empty")
    public static void theFreezersSwitchIsItsPower(GameTestHelper helper) {
        FurnitureTests.theFreezersSwitchIsItsPower(helper);
    }

    @GameTest(template = "empty")
    public static void whatHeatsGoesWhereTheMicrowaveHeats(GameTestHelper helper) {
        FurnitureTests.whatHeatsGoesWhereTheMicrowaveHeats(helper);
    }

    @GameTest(template = "empty")
    public static void threeThingsBakeAtOnceInTheOven(GameTestHelper helper) {
        FurnitureTests.threeThingsBakeAtOnceInTheOven(helper);
    }

    @GameTest(template = "empty")
    public static void theStovesKnobIsItsPower(GameTestHelper helper) {
        FurnitureTests.theStovesKnobIsItsPower(helper);
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

    @GameTest(template = "empty", timeoutTicks = 900)
    public static void aPoweredFreezerFreezesWhatStandsAnywhere(GameTestHelper helper) {
        FurnitureTests.aPoweredFreezerFreezesWhatStandsAnywhere(helper);
    }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void aPoweredMicrowaveHeatsWhatStandsOnThePlate(GameTestHelper helper) {
        FurnitureTests.aPoweredMicrowaveHeatsWhatStandsOnThePlate(helper);
    }

    @GameTest(template = "empty")
    public static void lampsLightTheRoom(GameTestHelper helper) {
        FurnitureTests.lampsLightTheRoom(helper);
    }

    @GameTest(template = "empty", timeoutTicks = 700)
    public static void aMicrowaveHeatsOnlyWithItsDoorShutAndLightsTheRoom(GameTestHelper helper) {
        FurnitureTests.aMicrowaveHeatsOnlyWithItsDoorShutAndLightsTheRoom(helper);
    }

    @GameTest(template = "empty", timeoutTicks = 700)
    public static void aMicrowaveSwitchesItselfOffWhenDone(GameTestHelper helper) {
        FurnitureTests.aMicrowaveSwitchesItselfOffWhenDone(helper);
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void aRightClickPutsOneDown(GameTestHelper helper) {
        FurnitureTests.aRightClickPutsOneDown(helper);
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void aGeneratorBurnsFuelLyingAnywhere(GameTestHelper helper) {
        FurnitureTests.aGeneratorBurnsFuelLyingAnywhere(helper);
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void aGeneratorMovesTheEmptyBucketAside(GameTestHelper helper) {
        FurnitureTests.aGeneratorMovesTheEmptyBucketAside(helper);
    }

    @GameTest(template = "empty")
    public static void aMailBoxsFrontChecksTheMail(GameTestHelper helper) {
        FurnitureTests.aMailBoxsFrontChecksTheMail(helper);
    }

    @GameTest(template = "empty")
    public static void menusAndComparatorsCountRefurbishedsSlots(GameTestHelper helper) {
        FurnitureTests.menusAndComparatorsCountRefurbishedsSlots(helper);
    }
}
