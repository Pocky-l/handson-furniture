package com.pockyl.handson_furniture.forge;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;

import com.pockyl.handson_furniture.HandsOnFurniture;
import com.pockyl.handson_furniture.client.HandsOnFurnitureClient;

/** Forge entry point. */
@Mod(HandsOnFurniture.MOD_ID)
public final class HandsOnFurnitureForge {
    public HandsOnFurnitureForge() {
        HandsOnFurniture.init();
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> HandsOnFurnitureClient::init);
    }
}
