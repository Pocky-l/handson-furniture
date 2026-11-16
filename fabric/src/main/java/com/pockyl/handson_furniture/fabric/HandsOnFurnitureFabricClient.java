package com.pockyl.handson_furniture.fabric;

import net.fabricmc.api.ClientModInitializer;

import com.pockyl.handson_furniture.client.HandsOnFurnitureClient;

/** Fabric client entry point. */
public final class HandsOnFurnitureFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        HandsOnFurnitureClient.init();
    }
}
