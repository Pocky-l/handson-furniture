package com.pockyl.handson_furniture;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import com.pockyl.diegesis.api.Diegesis;
import com.pockyl.diegesis.api.state.ControlSpec;
import com.pockyl.diegesis.api.state.SlotRules;
import com.pockyl.diegesis.api.state.StationSchema;
import com.pockyl.diegesis.api.station.StationType;
import com.pockyl.handson_furniture.station.StorageLogic;

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
    KITCHEN_DRAWER("kitchen_drawer", "kitchen_drawer", 160, Woods.AND_COLOURS, List.of(Furniture.OPEN, Furniture.OPEN_LOWER));

    /** The slot group of every station: the furniture's own slots, in order. */
    public static final String ITEMS = "items";
    /** The control that says whether the door is open (or the drawer out): 1 open, 0 shut. Kept with the block. */
    public static final String OPEN = "open";
    /** The same for a second door or drawer under the first: the kitchen drawer's lower drawer. */
    public static final String OPEN_LOWER = "open_lower";
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
    public ResourceLocation blockId(String variant) {
        return ResourceLocation.fromNamespaceAndPath(REFURBISHED, variant + "_" + suffix);
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
        return schema.build();
    }

    /** The woods and dye colours Refurbished makes its kitchen furniture in. */
    private static final class Woods {
        static final List<String> AND_COLOURS = List.of("oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry",
                "crimson", "warped", "white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray", "cyan",
                "purple", "blue", "brown", "green", "red", "black");

        private Woods() {
        }
    }
}
