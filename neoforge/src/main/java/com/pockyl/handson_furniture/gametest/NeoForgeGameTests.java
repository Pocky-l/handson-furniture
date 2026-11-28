package com.pockyl.handson_furniture.gametest;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.gametest.GameTestHooks;
import net.neoforged.neoforge.registries.DeferredRegister;

import com.pockyl.handson_furniture.HandsOnFurniture;

import java.util.List;
import java.util.function.Consumer;

/** {@link FurnitureTests} on NeoForge: each a function and a test instance on vanilla's empty structure. */
public final class NeoForgeGameTests {
    private static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION,
            HandsOnFurniture.MOD_ID);

    private NeoForgeGameTests() {
    }

    /** Registers the tests when the game test framework is on (the game test server and dev runs that enable it). */
    public static void register(IEventBus modBus) {
        if (!GameTestHooks.isGametestEnabled()) {
            return;
        }
        for (FurnitureTests.Test test : FurnitureTests.TESTS) {
            FUNCTIONS.register(test.name(), test::function);
        }
        FUNCTIONS.register(modBus);
        modBus.addListener(NeoForgeGameTests::registerTests);
    }

    private static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition> environment = event.registerEnvironment(HandsOnFurniture.id("default"),
                new TestEnvironmentDefinition.AllOf(List.of()));
        for (FurnitureTests.Test test : FurnitureTests.TESTS) {
            Identifier id = HandsOnFurniture.id(test.name());
            TestData<Holder<TestEnvironmentDefinition>> data = new TestData<>(environment, Identifier.withDefaultNamespace("empty"),
                    test.timeout(), 0, true, Rotation.NONE, false, 1, 1, false);
            event.registerTest(id, new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, id), data));
        }
    }
}
