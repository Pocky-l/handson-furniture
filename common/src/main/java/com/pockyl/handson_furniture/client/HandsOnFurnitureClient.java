package com.pockyl.handson_furniture.client;

import org.joml.Vector3f;

import com.pockyl.diegesis.api.client.DiegesisClient;
import com.pockyl.handson_furniture.Furniture;
import com.pockyl.handson_furniture.client.FurnitureBehavior.Motion;
import com.pockyl.handson_furniture.client.FurnitureBehavior.Part;

import java.util.List;

/** Client setup: how each piece of furniture moves while it is open. */
public final class HandsOnFurnitureClient {
    /** How far the fridge's door swings, degrees: a little past square, as far as a hand opens it to reach in. */
    private static final float FRIDGE_DOOR = 100.0F;
    /** The fridge door's hinge, station pixels: where the door's back meets the fridge's front on the viewer's right. */
    private static final Vector3f FRIDGE_HINGE = new Vector3f(0.0F, 0.0F, 3.0F);
    /** How far a cabinet's door swings open, degrees: just past square, as a hand leaves it. */
    private static final float CABINET_DOOR = 92.0F;
    /** How far a drawer slides out, pixels (towards the front, -z in the scene). */
    private static final float DRAWER = -11.0F;
    /** How far the camera goes along with a drawer pulled all the way out: over the middle of its floor, the lower one's lower. */
    private static final Vector3f FOLLOW_UPPER = new Vector3f(0.0F, 1.0F, -4.5F);
    private static final Vector3f FOLLOW_LOWER = new Vector3f(0.0F, -6.0F, -4.5F);

    /**
     * The fridge's heavy door: its seal holds it for a moment, it swings round and rocks a little against its stop; shut, the
     * seal pulls it in with a small bounce.
     */
    private static final Motion FRIDGE = new Motion(0.75F, 0.5F, 0.035F, 0.16F, 0.7F, 0.012F);
    /** A cabinet's light door on a latch: a short give, a quick swing, a soft knock on the frame. */
    private static final Motion CABINET = new Motion(0.5F, 0.38F, 0.025F, 0.1F, 0.9F, 0.02F);
    /** A drawer on runners: it glides out, recoils a little at the end stop, and knocks shut. */
    private static final Motion DRAWER_RUN = new Motion(0.45F, 0.35F, 0.0F, 0.0F, 1.1F, 0.025F);

    private HandsOnFurnitureClient() {
    }

    public static void init() {
        // The bins of the fridge's and the freezer's doors are the scenes' surfaces 3 to 5, the drawers' floors 0 and 1.
        behavior(Furniture.FRIDGE, FRIDGE, List.of(Part.door("door", "door", FRIDGE_DOOR, FRIDGE_HINGE, 3, 4, 5)));
        behavior(Furniture.FREEZER, FRIDGE, List.of(Part.door("door", "door", FRIDGE_DOOR, FRIDGE_HINGE, 3, 4, 5)));
        behavior(Furniture.KITCHEN_CABINET, CABINET, List.of(Part.hinged("door_left", "door_left", -CABINET_DOOR, "left"),
                Part.hinged("door_right", "door_right", CABINET_DOOR, "right")));
        behavior(Furniture.KITCHEN_DRAWER, DRAWER_RUN, List.of(Part.drawer("tray", "tray", DRAWER, Furniture.OPEN, FOLLOW_UPPER, 0),
                Part.drawer("tray_lower", "tray_lower", DRAWER, Furniture.OPEN_LOWER, FOLLOW_LOWER, 1)));
    }

    private static void behavior(Furniture furniture, Motion motion, List<Part> parts) {
        DiegesisClient.registerBehavior(furniture.type().id(), view -> new FurnitureBehavior(furniture, parts, motion));
    }
}
