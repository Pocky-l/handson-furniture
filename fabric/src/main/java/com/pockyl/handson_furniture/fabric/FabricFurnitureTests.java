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
}
