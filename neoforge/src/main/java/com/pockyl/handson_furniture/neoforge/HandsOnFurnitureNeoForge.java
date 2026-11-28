package com.pockyl.handson_furniture.neoforge;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

import com.pockyl.handson_furniture.HandsOnFurniture;
import com.pockyl.handson_furniture.client.HandsOnFurnitureClient;
import com.pockyl.handson_furniture.gametest.NeoForgeGameTests;

/** NeoForge entry point. */
@Mod(HandsOnFurniture.MOD_ID)
public final class HandsOnFurnitureNeoForge {
    public HandsOnFurnitureNeoForge(IEventBus modBus, Dist dist) {
        HandsOnFurniture.init();
        NeoForgeGameTests.register(modBus);
        if (dist.isClient()) {
            HandsOnFurnitureClient.init();
        }
    }
}
