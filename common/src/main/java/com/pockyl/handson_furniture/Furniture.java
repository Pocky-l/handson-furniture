package com.pockyl.handson_furniture;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import com.mrcrayfish.furniture.refurbished.item.WrenchItem;

import com.pockyl.diegesis.api.Diegesis;
import com.pockyl.diegesis.api.state.ControlSpec;
import com.pockyl.diegesis.api.state.SlotRules;
import com.pockyl.diegesis.api.state.StationSchema;
import com.pockyl.diegesis.api.station.StationType;
import com.pockyl.handson_furniture.station.StorageLogic;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The pieces of Refurbished furniture that open as stations. Each keeps its items in its own container: the station shows
 * them as things standing where the player put them, turned and sized as they were left.
 */
public enum Furniture {
    FRIDGE("fridge", "fridge", 250, List.of("light", "dark"), List.of(Furniture.OPEN)),
    /**
     * Under the fridge: Refurbished freezes what is in its first slot into its second, as its menu shows them; the rest is
     * cold storage.
     */
    FREEZER("freezer", "freezer", 250, List.of("light", "dark"), List.of(Furniture.OPEN)),
    KITCHEN_CABINET("kitchen_cabinet", "kitchen_storage_cabinet", 160, Woods.AND_COLOURS, List.of(Furniture.OPEN)),
    /** Two drawers, the upper one and the lower one Refurbished draws as a fixed panel, sharing its slots. */
    KITCHEN_DRAWER("kitchen_drawer", "kitchen_drawer", 160, Woods.AND_COLOURS, List.of(Furniture.OPEN, Furniture.OPEN_LOWER)),
    /** A box with a lid hinged at its back: what it holds lies on its floor, seen from above. */
    COOLER("cooler", "cooler", 160, Woods.COLOURS, List.of(Furniture.OPEN)),
    /** A wooden cabinet standing on the floor, its door hung on either side; one shelf. */
    STORAGE_CABINET("storage_cabinet", "storage_cabinet", 160, Woods.WOODS, List.of(Furniture.OPEN)),
    /**
     * A chest of drawers: two drawers, the upper one and the lower one Refurbished draws as a fixed panel, sharing its slots, as
     * the kitchen drawer's. Its top joins the tops of the ones beside it.
     */
    DRAWER("drawer", "drawer", 160, Woods.WOODS, List.of(Furniture.OPEN, Furniture.OPEN_LOWER)),
    /** Refurbished heats its first slot into its second while it is on and has power; what waits stands beside, on the plate. */
    MICROWAVE("microwave", "microwave", 64, List.of("light", "dark"), List.of(Furniture.OPEN)),
    /**
     * The oven of the stove: Refurbished bakes each of its first three slots into the one three further on while the stove is
     * on and has power; what waits stands beside, on the floor and the rack.
     */
    STOVE("stove", "stove", 160, List.of("light", "dark"), List.of(Furniture.OPEN)),
    /**
     * The electricity generator: a firebox behind the service panel on its side, fuel lying anywhere on its grate. Refurbished
     * burns its one slot; what waits lies beside, and goes in as the burning one runs out.
     */
    GENERATOR("generator", "electricity_generator", 64, List.of("light", "dark"), List.of(Furniture.OPEN)),
    /**
     * A mail box on its post: its front comes down, the mail lies on its floor. Its room is the most Refurbished's own may be
     * set to (six rows), so all its mail is in reach whatever the server's setting; the flag goes down as its front is opened.
     */
    MAIL_BOX("mail_box", "mail_box", 54, Woods.WOODS, List.of(Furniture.OPEN));

    /** The slot group of every station: the furniture's own slots, in order. */
    public static final String ITEMS = "items";
    /** The control that says whether the door is open (or the drawer out): 1 open, 0 shut. Kept with the block. */
    public static final String OPEN = "open";
    /** The same for a second door or drawer under the first: the kitchen drawer's lower drawer. */
    public static final String OPEN_LOWER = "open_lower";
    /**
     * The power switch of a freezer, a microwave, a stove or a generator, 1 on: Refurbished's own on and off (its menu's power button), kept
     * by the furniture itself; the station's control only shows it and turns it.
     */
    public static final String POWER = "power";
    public static final String REFURBISHED = "refurbished_furniture";

    private final String name;
    private final String suffix;
    private final int slots;
    private final List<String> variants;
    /** The controls of its doors or drawers, each opened on its own (one for a piece with a single door). */
    private final List<String> controls;
    private StationType type;

    Furniture(String name, String suffix, int slots, List<String> variants, List<String> controls) {
        this.name = name;
        this.suffix = suffix;
        this.slots = slots;
        this.variants = variants;
        this.controls = controls;
    }

    public List<String> controls() {
        return controls;
    }

    /** The station's name: its id, scene and models are named after it. */
    public String stationName() {
        return name;
    }

    /**
     * How many stacks it holds (at most 255: the slot's number is saved as a byte): more than ever fit on its shelves, so what is left over is room, not a number of slots. Its
     * container is made this large ({@code BasicLootBlockEntityMixin}); Refurbished's menu shows its own first slots of it.
     */
    public int slots() {
        return slots;
    }

    public List<String> variants() {
        return variants;
    }

    /** The id of Refurbished's block of a variant. */
    @SuppressWarnings("removal")
    public ResourceLocation blockId(String variant) {
        return new ResourceLocation(REFURBISHED, variant + "_" + suffix);
    }

    public StationType type() {
        return type;
    }

    /** The variant of a block of this furniture (its wood, colour or finish), or empty when the block is not one. */
    public Optional<String> variantOf(Block block) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
        String tail = "_" + suffix;
        if (!REFURBISHED.equals(id.getNamespace()) || !id.getPath().endsWith(tail)) {
            return Optional.empty();
        }
        String variant = id.getPath().substring(0, id.getPath().length() - tail.length());
        return variants.contains(variant) ? Optional.of(variant) : Optional.empty();
    }

    /** The furniture a block belongs to, if any. */
    public static Optional<Furniture> of(Block block) {
        for (Furniture furniture : values()) {
            if (furniture.variantOf(block).isPresent()) {
                return Optional.of(furniture);
            }
        }
        return Optional.empty();
    }

    void register() {
        StationType.Builder builder = Diegesis.station(HandsOnFurniture.id(name))
                // Refurbished's facing is the way its placer looked: the furniture's back.
                .orientation(StationType.Orientation.BLOCK_OPPOSITE)
                // The scene draws the furniture itself, so that its doors and drawers move and its insides show.
                .hidesBlock()
                .schema(schema())
                // Refurbished's wrench links the furniture to power: with it in hand the block is used as before.
                .openWhen((player, pos, state) -> StationType.OpenCondition.DEFAULT.test(player, pos, state) && !holdsWrench(player))
                .logic(StorageLogic::new);
        for (String variant : variants) {
            ResourceLocation id = blockId(variant);
            // Looked up once every block is registered; a block this Refurbished does not have is left as air, which is never used.
            builder.block(() -> BuiltInRegistries.BLOCK.getOptional(id).orElse(Blocks.AIR));
        }
        type = builder.register();
    }

    private StationSchema schema() {
        StationSchema.Builder schema = StationSchema.builder().slots(ITEMS, slots, SlotRules.DEFAULT.placed().kept());
        for (String control : controls) {
            schema.control(control, ControlSpec.toggle(false));
        }
        if (hasPower()) {
            // Refurbished makes them switched off: the power button of their menu switches them on. Off and on, whether the
            // scene shows a rocker or (the stove) a knob turned between its two steps.
            schema.control(POWER, ControlSpec.toggle(false));
        }
        return schema.build();
    }

    /**
     * Whether its door has a window: the microwave and the stove. What it holds shows through the glass with the door shut, so
     * the world shows the station's copy of it at all times, with its controls, not Refurbished's block with its painted pane.
     */
    public boolean hasWindow() {
        return this == MICROWAVE || this == STOVE;
    }

    /**
     * Whether the world shows the station's copy of it at all times, not Refurbished's block: a piece with a window, and one
     * whose outside has parts of the station's own (the generator's power key and the bar on its screen): the block looks the
     * same in the world as at its station, and no key or screen appears only once it is opened.
     */
    public boolean standsInWorld() {
        return hasWindow() || this == GENERATOR;
    }

    /**
     * Refurbished's sounds of its door, lid or drawer, {@code block.<name>.open} and {@code .close}: the cabinets share the
     * cabinet's. Null for the generator, whose panel Refurbished gives no sound, and the mail box, which has none of its own
     * ({@link com.pockyl.handson_furniture.station.MailDoor}).
     */
    public String doorSound() {
        return switch (this) {
            case KITCHEN_CABINET, STORAGE_CABINET -> "cabinet";
            case GENERATOR, MAIL_BOX -> null;
            default -> name;
        };
    }

    /** The id of Refurbished's open or close sound of its door, lid or drawer ({@link #doorSound}), or null when it has none. */
    @SuppressWarnings("removal")
    public ResourceLocation doorSoundId(boolean open) {
        String sound = doorSound();
        return sound == null ? null : new ResourceLocation(REFURBISHED, "block." + sound + (open ? ".open" : ".close"));
    }

    /**
     * Ticks from the click that shuts a door, lid or drawer to its close sound, so that the knock in the sound is heard as the
     * scene's part meets the frame: 0.8 of the part's shutting time (the client's {@code Motion}) less how far into the sound
     * its knock comes (fridge 80 ms, freezer 90, cabinet 10, cooler 15, drawer 235, kitchen drawer 205, microwave 70, oven 150,
     * a wooden trapdoor's click at once), to the nearest tick, a late one when it is a tie: the sound and the change that starts
     * the part moving reach every player in the same tick.
     */
    public int closeSoundTicks() {
        return switch (this) {
            case FRIDGE, FREEZER, KITCHEN_CABINET, STORAGE_CABINET -> 6;
            case COOLER -> 5;
            case DRAWER -> 1;
            case KITCHEN_DRAWER -> 2;
            case MICROWAVE -> 3;
            case STOVE, MAIL_BOX -> 4;
            case GENERATOR -> 0;
        };
    }

    /** Whether it has Refurbished's on and off: the freezer, the microwave, the stove and the generator. */
    public boolean hasPower() {
        return this == FREEZER || this == MICROWAVE || this == STOVE || this == GENERATOR;
    }

    private static boolean holdsWrench(Player player) {
        return player.getMainHandItem().getItem() instanceof WrenchItem || player.getOffhandItem().getItem() instanceof WrenchItem;
    }

    /** The woods and dye colours Refurbished makes its furniture in: kitchen furniture in both, coolers in the colours, storage in the woods. */
    private static final class Woods {
        static final List<String> COLOURS = List.of("white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
                "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black");
        static final List<String> WOODS = List.of("oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry",
                "crimson", "warped");
        static final List<String> AND_COLOURS = concat(WOODS, COLOURS);

        private static List<String> concat(List<String> first, List<String> second) {
            List<String> all = new ArrayList<>(first);
            all.addAll(second);
            return List.copyOf(all);
        }

        private Woods() {
        }
    }
}
