package com.pockyl.handson_furniture;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;

/** Constants and setup of the mod, the same on the client and the server. */
public final class HandsOnFurniture {
    public static final String MOD_ID = "handson_furniture";
    public static final Logger LOGGER = LogUtils.getLogger();

    private HandsOnFurniture() {
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    /** Registers the stations; called during mod setup on both sides. */
    public static void init() {
        for (Furniture furniture : Furniture.values()) {
            furniture.register();
        }
    }
}
