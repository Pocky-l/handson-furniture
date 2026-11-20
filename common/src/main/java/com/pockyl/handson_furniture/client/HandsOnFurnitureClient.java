package com.pockyl.handson_furniture.client;

import org.joml.Vector3f;

import com.pockyl.diegesis.api.client.DiegesisClient;
import com.pockyl.handson_furniture.Furniture;
import com.pockyl.handson_furniture.client.FurnitureBehavior.Motion;
import com.pockyl.handson_furniture.client.FurnitureBehavior.Part;
import com.pockyl.handson_furniture.client.FurnitureBehavior.Turntable;

import java.util.List;

/** Client setup: how each piece of furniture moves while it is open. */
public final class HandsOnFurnitureClient {
    /** How far the fridge's door swings, degrees: a little past square, as far as a hand opens it to reach in. */
    private static final float FRIDGE_DOOR = 100.0F;
    /** The fridge door's hinge, station pixels: where the door's back meets the fridge's front on the viewer's right. */
    private static final Vector3f FRIDGE_HINGE = new Vector3f(0.0F, 0.0F, 3.0F);
    /** How far a cabinet's door swings open, degrees: just past square, as a hand leaves it. */
    private static final float CABINET_DOOR = 92.0F;
    /**
     * How far a cooler's lid lifts, degrees about its back edge: a little past upright, so it leans back and stays up by
     * itself, still inside the block for a cooler standing against a wall.
     */
    private static final float COOLER_LID = -100.0F;
    /** How far a microwave's door swings, degrees about its hinge at the outer front corner: a little past square. */
    private static final float MICROWAVE_DOOR = -95.0F;
    /** The microwave's plate: the scene's only surface, turning about its middle once in twelve seconds while it heats. */
    private static final Turntable PLATE = new Turntable("plate", 0, new Vector3f(10.0F, 0.0F, 9.0F), 30.0F);
    /** How far the oven's door comes down, degrees about the line along its bottom: level, as Refurbished's open door lies. */
    private static final float OVEN_DOOR = 90.0F;
    /** How far the generator's service panel swings out, degrees about its back edge: a little past square. */
    private static final float GENERATOR_PANEL = -100.0F;
    /** How far a mail box's front comes down, degrees about the line along its bottom: level, its inside up, as a tray. */
    private static final float MAIL_BOX_FRONT = 90.0F;
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
    /** A cooler's light lid: its snug rim lets go, it swings up and rocks back onto its hinge; dropped, it claps shut. */
    private static final Motion COOLER = new Motion(0.55F, 0.32F, 0.03F, 0.12F, 0.8F, 0.03F);
    /** A microwave's door on its catch: it springs free, swings quickly and claps shut. */
    private static final Motion MICROWAVE = new Motion(0.45F, 0.3F, 0.04F, 0.08F, 0.9F, 0.03F);
    /** An oven's heavy door on its springs: it gives, sinks down and settles level; pushed up, it thuds shut. */
    private static final Motion OVEN = new Motion(0.7F, 0.45F, 0.02F, 0.12F, 0.4F, 0.015F);
    /** The generator's sheet-metal panel on its latch: it clicks free, swings out lightly and claps shut with a ring. */
    private static final Motion PANEL = new Motion(0.45F, 0.3F, 0.03F, 0.08F, 1.0F, 0.035F);
    /** A mail box's small wooden front on its catch: it gives, drops quickly onto its stop and rocks; lifted, it claps shut. */
    private static final Motion MAIL_BOX = new Motion(0.4F, 0.28F, 0.04F, 0.1F, 0.6F, 0.03F);
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
        behavior(Furniture.COOLER, COOLER, List.of(Part.lid("lid", "lid", COOLER_LID)));
        behavior(Furniture.STORAGE_CABINET, CABINET, List.of(Part.hinged("door_left", "door_left", -CABINET_DOOR, "left"),
                Part.hinged("door_right", "door_right", CABINET_DOOR, "right")));
        behavior(Furniture.DRAWER, DRAWER_RUN, List.of(Part.drawer("tray", "tray", DRAWER, Furniture.OPEN, FOLLOW_UPPER, 0),
                Part.drawer("tray_lower", "tray_lower", DRAWER, Furniture.OPEN_LOWER, FOLLOW_LOWER, 1)));
        behavior(Furniture.MICROWAVE, MICROWAVE, List.of(Part.door("door", "door", MICROWAVE_DOOR, new Vector3f())), PLATE);
        behavior(Furniture.STOVE, OVEN, List.of(Part.lid("door", "door", OVEN_DOOR)));
        behavior(Furniture.GENERATOR, PANEL, List.of(Part.door("panel", "panel", GENERATOR_PANEL, new Vector3f())));
        behavior(Furniture.MAIL_BOX, MAIL_BOX, List.of(Part.lid("door", "door", MAIL_BOX_FRONT)));
    }

    private static void behavior(Furniture furniture, Motion motion, List<Part> parts) {
        behavior(furniture, motion, parts, null);
    }

    private static void behavior(Furniture furniture, Motion motion, List<Part> parts, Turntable turntable) {
        DiegesisClient.registerBehavior(furniture.type().id(), view -> new FurnitureBehavior(furniture, parts, motion, turntable));
    }
}
