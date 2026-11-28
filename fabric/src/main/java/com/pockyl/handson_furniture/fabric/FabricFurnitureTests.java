package com.pockyl.handson_furniture.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

import com.pockyl.handson_furniture.HandsOnFurniture;
import com.pockyl.handson_furniture.gametest.FurnitureTests;

/**
 * {@link FurnitureTests} on Fabric, only on the game test server ({@code -Dfabric-api.gametest}). Fabric API's {@code @GameTest}
 * cannot be read on an obfuscated production server (its default rotation names an intermediary enum constant), so the functions
 * are registered here and their test instances come from a built-in data pack ({@code resourcepacks/gametests}, one file per
 * test, with its time limit).
 */
public final class FabricFurnitureTests implements ModInitializer {
    /** Fabric API's switch for the game test server. */
    private static final String ENABLED = "fabric-api.gametest";

    @Override
    public void onInitialize() {
        if (System.getProperty(ENABLED) == null) {
            return;
        }
        for (FurnitureTests.Test test : FurnitureTests.TESTS) {
            Registry.register(BuiltInRegistries.TEST_FUNCTION, HandsOnFurniture.id(test.name()), test.function());
        }
        FabricLoader.getInstance().getModContainer(HandsOnFurniture.MOD_ID).ifPresent(mod ->
                ResourceLoader.registerBuiltinPack(HandsOnFurniture.id("gametests"), mod, PackActivationType.ALWAYS_ENABLED));
    }
}
