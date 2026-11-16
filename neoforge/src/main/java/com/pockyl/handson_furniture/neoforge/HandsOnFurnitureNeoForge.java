package com.pockyl.handson_furniture.neoforge;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;

import com.pockyl.handson_furniture.HandsOnFurniture;
import com.pockyl.handson_furniture.client.HandsOnFurnitureClient;

/** NeoForge entry point. */
@Mod(HandsOnFurniture.MOD_ID)
public final class HandsOnFurnitureNeoForge {
    public HandsOnFurnitureNeoForge(Dist dist) {
        HandsOnFurniture.init();
        if (dist.isClient()) {
            HandsOnFurnitureClient.init();
        }
    }
}
