package com.pockyl.handson_furniture.fabric;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

import com.pockyl.handson_furniture.gametest.FurnitureTests;

/** {@link FurnitureTests} on Fabric: they run when the game starts with {@code -Dfabric-api.gametest}. */
public final class FabricFurnitureTests implements FabricGameTest {
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void everyVariantIsAStation(GameTestHelper helper) {
        FurnitureTests.everyVariantIsAStation(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void slotsAreTheFurnituresOwn(GameTestHelper helper) {
        FurnitureTests.slotsAreTheFurnituresOwn(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void roomPastTheMenuIsKeptAndDropped(GameTestHelper helper) {
        FurnitureTests.roomPastTheMenuIsKeptAndDropped(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void whatFallsOutGoesToTheInventoryElseTheFloor(GameTestHelper helper) {
        FurnitureTests.whatFallsOutGoesToTheInventoryElseTheFloor(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void placesAreSavedWithTheBlock(GameTestHelper helper) {
        FurnitureTests.placesAreSavedWithTheBlock(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void aClickOpensTheDoorAndItStaysOpen(GameTestHelper helper) {
        FurnitureTests.aClickOpensTheDoorAndItStaysOpen(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void everyDoorHasItsSounds(GameTestHelper helper) {
        FurnitureTests.everyDoorHasItsSounds(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void theTwoDrawersOpenOnTheirOwn(GameTestHelper helper) {
        FurnitureTests.theTwoDrawersOpenOnTheirOwn(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void aCoolersLidLiftsAndItHoldsMoreThanARow(GameTestHelper helper) {
        FurnitureTests.aCoolersLidLiftsAndItHoldsMoreThanARow(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void theWrenchUsesTheBlockAsBefore(GameTestHelper helper) {
        FurnitureTests.theWrenchUsesTheBlockAsBefore(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void whatFreezesGoesWhereTheFreezerFreezes(GameTestHelper helper) {
        FurnitureTests.whatFreezesGoesWhereTheFreezerFreezes(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void theFreezersSwitchIsItsPower(GameTestHelper helper) {
        FurnitureTests.theFreezersSwitchIsItsPower(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void whatHeatsGoesWhereTheMicrowaveHeats(GameTestHelper helper) {
        FurnitureTests.whatHeatsGoesWhereTheMicrowaveHeats(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void threeThingsBakeAtOnceInTheOven(GameTestHelper helper) {
        FurnitureTests.threeThingsBakeAtOnceInTheOven(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void theStovesKnobIsItsPower(GameTestHelper helper) {
        FurnitureTests.theStovesKnobIsItsPower(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void clientsAreToldOfAnOpenDoor(GameTestHelper helper) {
        FurnitureTests.clientsAreToldOfAnOpenDoor(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void anEmptiedSlotForgetsItsPlace(GameTestHelper helper) {
        FurnitureTests.anEmptiedSlotForgetsItsPlace(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void aShiftClickMergesThenFills(GameTestHelper helper) {
        FurnitureTests.aShiftClickMergesThenFills(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void aFullDrawerLeavesTheRest(GameTestHelper helper) {
        FurnitureTests.aFullDrawerLeavesTheRest(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 900)
    public void aPoweredFreezerFreezesWhatStandsAnywhere(GameTestHelper helper) {
        FurnitureTests.aPoweredFreezerFreezesWhatStandsAnywhere(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 400)
    public void aPoweredMicrowaveHeatsWhatStandsOnThePlate(GameTestHelper helper) {
        FurnitureTests.aPoweredMicrowaveHeatsWhatStandsOnThePlate(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void lampsLightTheRoom(GameTestHelper helper) {
        FurnitureTests.lampsLightTheRoom(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 700)
    public void aMicrowaveHeatsOnlyWithItsDoorShutAndLightsTheRoom(GameTestHelper helper) {
        FurnitureTests.aMicrowaveHeatsOnlyWithItsDoorShutAndLightsTheRoom(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 700)
    public void aMicrowaveSwitchesItselfOffWhenDone(GameTestHelper helper) {
        FurnitureTests.aMicrowaveSwitchesItselfOffWhenDone(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 100)
    public void aRightClickPutsOneDown(GameTestHelper helper) {
        FurnitureTests.aRightClickPutsOneDown(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 100)
    public void aGeneratorBurnsFuelLyingAnywhere(GameTestHelper helper) {
        FurnitureTests.aGeneratorBurnsFuelLyingAnywhere(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 100)
    public void aGeneratorMovesTheEmptyBucketAside(GameTestHelper helper) {
        FurnitureTests.aGeneratorMovesTheEmptyBucketAside(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void aMailBoxsFrontChecksTheMail(GameTestHelper helper) {
        FurnitureTests.aMailBoxsFrontChecksTheMail(helper);
    }

    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void menusAndComparatorsCountRefurbishedsSlots(GameTestHelper helper) {
        FurnitureTests.menusAndComparatorsCountRefurbishedsSlots(helper);
    }
}
