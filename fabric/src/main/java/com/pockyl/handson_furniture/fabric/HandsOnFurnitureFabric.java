package com.pockyl.handson_furniture.fabric;

import net.fabricmc.api.ModInitializer;

import com.pockyl.handson_furniture.HandsOnFurniture;

/** Fabric entry point, on both sides. */
public final class HandsOnFurnitureFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        HandsOnFurniture.init();
    }
}
