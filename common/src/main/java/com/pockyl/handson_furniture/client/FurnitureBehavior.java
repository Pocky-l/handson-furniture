package com.pockyl.handson_furniture.client;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import com.mrcrayfish.furniture.refurbished.blockentity.ElectricityGeneratorBlockEntity;
import com.mrcrayfish.furniture.refurbished.blockentity.MicrowaveBlockEntity;

import com.pockyl.diegesis.api.client.MeshShape;
import com.pockyl.diegesis.api.client.NodeView;
import com.pockyl.diegesis.api.client.StationBehavior;
import com.pockyl.diegesis.api.client.StationView;
import com.pockyl.diegesis.api.event.SceneEvent;
import com.pockyl.diegesis.api.scene.Visual;
import com.pockyl.diegesis.api.state.SlotRef;
import com.pockyl.handson_furniture.Furniture;
import com.pockyl.handson_furniture.HandsOnFurniture;
import com.pockyl.handson_furniture.mixin.GeneratorAccess;
import com.pockyl.handson_furniture.station.ProcessingSlots;
import com.pockyl.handson_furniture.station.StorageLogic;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A piece of furniture opened by hand. While the player works at it the block is swapped for the scene's copy, made of the
 * furniture's own textures: a hollow body with real shelves, and the parts that move, a door swinging open or a drawer sliding
 * out as the player comes, and back as they leave. The copy stands in for the block until the part is shut again.
 */
final class FurnitureBehavior implements StationBehavior {
    /**
     * How a piece's door or drawer moves, as a hand moves it: a door held by its seal or latch first gives a little
     * ({@code crack}, the share of the way, over the first {@code crackTime} of the opening), then swings and comes to rest a
     * touch past open ({@code overshoot}, as {@code easeOutBack}'s strength) as it meets its stop; shutting, it gathers speed and
     * bounces back off the frame by {@code bounce} of the way before it settles.
     */
    record Motion(float openSeconds, float closeSeconds, float crack, float crackTime, float overshoot, float bounce) {
        /** The share of the shutting spent before the part meets the frame; the rest is its bounce. */
        private static final float MEETS = 0.8F;

        /** Where the part is, 0 to 1 of the way it is going, at {@code u} (0 to 1) of the time; {@code fromRest}: it started still. */
        float opening(float u, boolean fromRest) {
            if (!fromRest || crack <= 0.0F) {
                return easeOutBack(u, overshoot);
            }
            if (u < crackTime) {
                return crack * smooth(u / crackTime);
            }
            return crack + (1.0F - crack) * easeOutBack((u - crackTime) / (1.0F - crackTime), overshoot);
        }

        float shutting(float u) {
            if (u < MEETS) {
                float w = u / MEETS;
                // Pushed: it moves off at once and gathers speed, fastest as it meets the frame.
                return w * (0.35F + 0.65F * w);
            }
            return 1.0F - bounce * Mth.sin((u - MEETS) / (1.0F - MEETS) * Mth.PI);
        }

        private static float smooth(float t) {
            return t * t * (3.0F - 2.0F * t);
        }

        private static float easeOutBack(float t, float strength) {
            float back = t - 1.0F;
            return 1.0F + (strength + 1.0F) * back * back * back + strength * back * back;
        }
    }

    /** How a part moves: a door swings about its upright hinge, a lid lifts about the hinge along its back, a drawer slides. */
    enum Way {
        SWING, LIFT, SLIDE
    }

    /**
     * A part that moves: the scene node, the model of each variant ({@code <variant>_<model>}), and how far it goes when open,
     * degrees about its hinge for a door or a lid, pixels along z for a drawer. With {@code hinge} it shows only on blocks whose
     * {@code hinge} property has that value. {@code surfaces} are the places of the scene's {@code placing} that are on the
     * part: what stands there goes along. They are written where the part stands open, or with {@code writtenShut} where it is
     * shut (a door that stops part way open turns its shelves askew, and a surface is written square). {@code pivot} is the
     * hinge in station pixels. {@code control} opens it ({@link Furniture#OPEN} unless the piece has more than one); {@code follow}
     * is how far the camera goes along while it is open, station pixels.
     */
    record Part(String node, String model, float open, Way way, String hinge, int[] surfaces, Vector3f pivot, boolean writtenShut,
            String control, Vector3f follow) {
        /** A door whose shelves the scene writes where the shut door has them. */
        static Part door(String node, String model, float degrees, Vector3f pivot, int... surfaces) {
            return new Part(node, model, degrees, Way.SWING, null, surfaces, pivot, true, Furniture.OPEN, new Vector3f());
        }

        static Part hinged(String node, String model, float degrees, String hinge) {
            return new Part(node, model, degrees, Way.SWING, hinge, new int[0], new Vector3f(), true, Furniture.OPEN, new Vector3f());
        }

        /** A lid lifted by its front edge, {@code degrees} up about the hinge along its back. */
        static Part lid(String node, String model, float degrees) {
            return new Part(node, model, degrees, Way.LIFT, null, new int[0], new Vector3f(), true, Furniture.OPEN, new Vector3f());
        }

        /** A drawer; the camera goes along with it by {@code follow}. */
        static Part drawer(String node, String model, float pixels, String control, Vector3f follow, int... surfaces) {
            return new Part(node, model, pixels, Way.SLIDE, null, surfaces, new Vector3f(), false, control, follow);
        }

        /** How what stands on the part moves from where the scene writes it to where the part is now. */
        Matrix4f move(float eased) {
            float left = writtenShut ? open * eased : open * (eased - 1.0F);
            if (way == Way.SLIDE) {
                return new Matrix4f().translation(0.0F, 0.0F, left);
            }
            Matrix4f turned = new Matrix4f().translation(pivot);
            float radians = (float) Math.toRadians(left);
            if (way == Way.LIFT) {
                turned.rotateX(radians);
            } else {
                turned.rotateY(radians);
            }
            return turned.translate(-pivot.x(), -pivot.y(), -pivot.z());
        }
    }

    /**
     * A microwave's turning plate: the scene node, the surface of the scene's {@code placing} that lies on it (what stands
     * there turns along), the middle it turns about in station pixels, and how fast it turns while the microwave heats.
     */
    record Turntable(String node, int surface, Vector3f center, float degreesPerSecond) {
    }

    /** The freezer's switch plate: it stays still while the key on it rocks. */
    private static final String POWER_PLATE = "power_plate";
    /** The microwave's green lamp: it glows while the microwave is switched on. */
    private static final String LAMP = "lamp";
    /** The oven's heating element: dark iron while the stove is off, glowing while it is lit. */
    private static final String ELEMENT = "element";
    /** The element unlit: its red darkened to the iron of a cold coil. */
    private static final int COLD = 0xFF4A4448;
    /** The generator's bed of embers under its grate: glowing while it runs, dark otherwise. */
    private static final String EMBERS = "embers";
    /** Embers gone out: the magma darkened to cold slag. */
    private static final int ASH = 0xFF3A3230;
    /** The generator's screen: a bar of what is left of the fuel burning now. */
    private static final String SCREEN = "screen";
    /** The screen's bar: half its length, its height and how far it stands off the screen, pixels. */
    private static final float BAR_HALF = 3.0F;
    private static final float BAR_HIGH = 1.0F;
    private static final float BAR_OFF = -0.03F;
    /** One texel of lime concrete, stretched along the bar: an even green. */
    @SuppressWarnings("removal")
    private static final ResourceLocation BAR = new ResourceLocation("minecraft", "block/lime_concrete");

    /**
     * The light inside: the fridge's strip down its side wall, the microwave's cover in its cavity wall, the oven's on its side
     * wall. Each node shines on what stands near it while it is on (the scene's {@code light}), and the one with a model glows;
     * the fridge's strip has one for each shelf's room, as a light does not reach through a shelf.
     */
    private static final List<String> LIGHTS = List.of("light", "light_middle", "light_low");
    /**
     * How far a door is off its frame when its lamp switch lets go, of the way open: the fridge's and the microwave's lamps come
     * on as the door cracks open, and go out just before the door meets the frame, as a door switch does.
     */
    private static final float DOOR_SWITCH = 0.04F;
    /** How long a lamp takes to come on, seconds: a short warm-up, not a flash; it goes out quicker. */
    private static final float LAMP_ON_SECONDS = 0.08F;
    private static final float LAMP_OFF_SECONDS = 0.05F;
    /** The lamp's cover while it is out: a pale grey, lit as the block around it. */
    private static final int LAMP_OFF = 0xFFA7ACB2;
    /** How long the line about missing power stays, seconds. */
    private static final float NO_POWER_SECONDS = 4.0F;

    /** The middle of the block across, scene pixels: the plane the camera is mirrored across. */
    private static final float MIDDLE = 8.0F;

    /**
     * Where one door or drawer is, 0 shut to 1 open (a touch past it at the stop), whether it is opening, and its current move:
     * from where, seconds into it, how long it takes, and whether it started from rest.
     */
    private static final class Swing {
        private float open;
        private boolean opening;
        private float from;
        private float elapsed;
        private float duration;
        private boolean fromRest;
        private boolean moving;

        /** Stands where the state has it, without moving there: found so (left open by someone, or while this player was away). */
        void settle(boolean isOpen) {
            opening = isOpen;
            open = isOpen ? 1.0F : 0.0F;
            moving = false;
        }

        /** Starts the move towards open or shut from wherever it is; turning round midway takes the rest of the way only. */
        void start(boolean toOpen, Motion motion) {
            opening = toOpen;
            from = open;
            float way = toOpen ? 1.0F - from : from;
            fromRest = !moving && from <= 0.0F;
            elapsed = 0.0F;
            duration = Math.max(0.05F, (toOpen ? motion.openSeconds() : motion.closeSeconds()) * Mth.clamp(way, 0.35F, 1.0F));
            moving = true;
        }

        void advance(float dt, Motion motion) {
            if (!moving) {
                return;
            }
            elapsed += dt;
            float u = Math.min(1.0F, elapsed / duration);
            open = opening ? from + (1.0F - from) * motion.opening(u, fromRest) : from * (1.0F - motion.shutting(u));
            if (u >= 1.0F) {
                moving = false;
                open = opening ? 1.0F : 0.0F;
            }
        }

        /** Its inside is seen: it is open, or on its way. */
        boolean showsInside() {
            return open > 0.0F || moving;
        }

        float eased() {
            return Math.max(0.0F, open);
        }
    }

    private final Furniture furniture;
    private final List<Part> parts;
    private final Motion motion;
    /** The microwave's plate, null for the rest. */
    private final Turntable turntable;
    /** Where the plate stands, degrees: it stops where it is when the heating stops. */
    private float plateAngle;
    /** Whether the oven's element (the generator's embers) was last drawn glowing; null before it is first drawn. */
    private Boolean elementLit;
    /** The generator's energy as last sent, the game time it was seen, and the share of the bar last drawn (-1 none). */
    private int energySeen = -1;
    private long energySeenAt;
    private float barDrawn = -2.0F;
    /** How bright the lamp inside is, 0 off to 1 on, and how bright it was last drawn (NaN before it is first drawn). */
    private float lamp;
    private float lampDrawn = Float.NaN;
    /** Whether the player was told the appliance has no power, since it last had some or was switched off. */
    private boolean toldNoPower;
    /** Whether the power switch was on in the last frame: switching it on with nothing to heat is told once. */
    private boolean powerWasOn;
    /** Per control, how its door or drawer moves (a cabinet's two hinged doors are one: only one of them shows). */
    private final Map<String, Swing> swings = new LinkedHashMap<>();
    /** The control whose door or drawer the camera goes with: the one opened last. */
    private String followed;
    private String variant;
    private BlockState shownFor;

    FurnitureBehavior(Furniture furniture, List<Part> parts, Motion motion, Turntable turntable) {
        this.furniture = furniture;
        this.parts = parts;
        this.motion = motion;
        this.turntable = turntable;
        for (Part part : parts) {
            swings.computeIfAbsent(part.control(), control -> new Swing());
        }
        followed = parts.get(0).control();
    }

    @Override
    public void onSceneReady(StationView view) {
        dress(view);
        powerWasOn = furniture.hasPower() && isOpen(view, Furniture.POWER);
        for (Map.Entry<String, Swing> entry : swings.entrySet()) {
            boolean open = isOpen(view, entry.getKey());
            entry.getValue().settle(open);
            if (open) {
                followed = entry.getKey();
            }
        }
    }

    /** A player who comes to the appliance switched on without power is told so again. */
    @Override
    public void onOpen(StationView view) {
        toldNoPower = false;
    }

    /** Whether a door is open (a drawer out), as the station's state keeps it for everyone. */
    private static boolean isOpen(StationView view, String control) {
        return view.state().getControl(control) > 0.5F;
    }

    /** A click on a door or a drawer opens it, or shuts it. */
    @Override
    public void onEvent(StationView view, SceneEvent event) {
        if (!(event instanceof SceneEvent.Click click) || click.button() != 0) {
            return;
        }
        for (Part part : parts) {
            if (part.node().equals(click.node())) {
                CompoundTag data = new CompoundTag();
                data.putString(StorageLogic.CONTROL, part.control());
                view.sendAction(StorageLogic.DOOR, data);
                return;
            }
        }
    }

    /**
     * The scene stands in for the block while a door is open, or still on the way back: a door left open stays open. A piece
     * with a window or controls of its own outside always ({@link Furniture#standsInWorld}): what it holds is seen through the
     * glass, its switch, knob or key and its screen stand on it.
     */
    @Override
    public boolean holdsBlock(StationView view) {
        if (view.isInUse()) {
            return false;
        }
        if (furniture.standsInWorld() && variant != null) {
            return true;
        }
        for (Map.Entry<String, Swing> entry : swings.entrySet()) {
            if (entry.getValue().showsInside() || isOpen(view, entry.getKey())) {
                return true;
            }
        }
        return false;
    }

    /**
     * A shift-click in the inventory puts the stack in, merged with what is there, as the furniture's own menu would. A stack
     * that joins none goes in only where there is room for it on the shelves; with none left, nothing happens, as in a full menu.
     */
    @Override
    public boolean onInventoryShiftClick(StationView view, int slot, ItemStack stack) {
        if (!joinsAStack(view, stack) && !view.hasRoomFor(stack)) {
            return true;
        }
        CompoundTag data = new CompoundTag();
        data.putInt(StorageLogic.SLOT, slot);
        view.sendAction(StorageLogic.STORE, data);
        return true;
    }

    /** Whether a stack of the same item inside has room left for some of {@code stack}. */
    private static boolean joinsAStack(StationView view, ItemStack stack) {
        int size = view.state().schema().slotGroups().get(Furniture.ITEMS).size();
        for (int i = 0; i < size; i++) {
            ItemStack there = view.state().getItem(SlotRef.of(Furniture.ITEMS, i));
            if (!there.isEmpty() && there.getCount() < there.getMaxStackSize() && ItemStack.isSameItemSameTags(there, stack)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void frame(StationView view, float dt) {
        BlockState state = view.level().getBlockState(view.pos());
        if (state != shownFor) {
            dress(view);
        }
        boolean anyInside = false;
        for (Map.Entry<String, Swing> entry : swings.entrySet()) {
            Swing swing = entry.getValue();
            boolean wanted = isOpen(view, entry.getKey());
            if (wanted != swing.opening) {
                swing.start(wanted, motion);
                if (wanted) {
                    followed = entry.getKey();
                }
            }
            swing.advance(dt, motion);
            anyInside |= swing.showsInside();
        }
        boolean shown = view.isBlockHidden() && variant != null;
        view.node("body").ifPresent(node -> node.setVisible(shown));
        // What stands inside is seen, and can be reached, only while a door is open or on its way (behind a window also while
        // it is shut: the shut door is in the way of the hand); each drawer's own only while that drawer is.
        view.setPlacingVisible(shown && (anyInside || furniture.hasWindow()));
        view.setPlacingReachable(anyInside);
        // The freezer's power switch is inside, on its wall; the microwave's and the stove's are outside, on their fronts.
        boolean powerShown = furniture == Furniture.FREEZER ? shown && anyInside : shown;
        view.node(Furniture.POWER).ifPresent(node -> node.setVisible(powerShown));
        view.node(POWER_PLATE).ifPresent(node -> node.setVisible(powerShown));
        view.node(ELEMENT).ifPresent(node -> node.setVisible(shown));
        view.node(EMBERS).ifPresent(node -> node.setVisible(shown));
        screen(view, state, shown);
        // The microwave's green lamp shows it is on, and only with power: an appliance without power is dark.
        boolean unpowered = furniture.hasPower() && StorageLogic.lacksPower(view.level().getBlockEntity(view.pos()));
        view.node(LAMP).ifPresent(node -> node.setVisible(shown && isOpen(view, Furniture.POWER) && !unpowered));
        tellNoPower(view, unpowered);
        if (turntable != null) {
            turn(view, dt, shown);
        }
        light(view, dt, state, shown);
        Vector3f camera = new Vector3f();
        for (Part part : parts) {
            Swing swing = swings.get(part.control());
            float eased = swing.eased();
            boolean fits = fits(state, part);
            view.node(part.node()).ifPresent(node -> {
                node.setVisible(shown && fits);
                switch (part.way()) {
                    case SLIDE -> node.moveTo(0.0F, 0.0F, part.open() * eased, 0.0F);
                    case LIFT -> node.rotateTo(part.open() * eased, 0.0F, 0.0F, 0.0F);
                    case SWING -> node.rotateTo(0.0F, part.open() * eased, 0.0F, 0.0F);
                }
            });
            if (part.surfaces().length > 0) {
                Matrix4f move = part.move(eased);
                for (int surface : part.surfaces()) {
                    view.setSurfaceTransform(surface, move);
                    view.setSurfaceVisible(surface, swing.showsInside());
                }
            }
            if (fits && part.control().equals(followed)) {
                camera.set(part.follow()).mul(eased);
            }
        }
        // The camera goes along with the drawer pulled out, to look into it.
        view.moveCamera(camera.x, camera.y, camera.z);
    }

    /**
     * Switched on with no power, Refurbished's appliance does nothing, and nothing on it says why (its menu does): the player at
     * it is told once, as it is switched on or as they come to it.
     */
    private void tellNoPower(StationView view, boolean unpowered) {
        if (!furniture.hasPower()) {
            return;
        }
        if (furniture == Furniture.GENERATOR) {
            // Switched on with nothing left that burns, it gives no power: its lamp says so, the player is told why once.
            boolean empty = isOpen(view, Furniture.POWER) && !flag(view.level().getBlockState(view.pos()), "powered")
                    && !ProcessingSlots.GENERATOR.hasWork(view.level().getBlockEntity(view.pos()));
            if (empty && !toldNoPower) {
                view.showCaption(Component.translatable(StorageLogic.NO_FUEL), NO_POWER_SECONDS);
            }
            toldNoPower = empty;
            return;
        }
        boolean on = isOpen(view, Furniture.POWER);
        if (furniture == Furniture.MICROWAVE && on && !powerWasOn && !unpowered
                && !ProcessingSlots.MICROWAVE.hasWork(view.level().getBlockEntity(view.pos()))) {
            // Switched on with nothing in it that heats: the server puts the switch back; the player is told why.
            view.showCaption(Component.translatable(StorageLogic.NOTHING_TO_HEAT), NO_POWER_SECONDS);
        }
        powerWasOn = on;
        boolean waiting = unpowered && on;
        if (waiting && !toldNoPower) {
            view.showCaption(Component.translatable(StorageLogic.NO_POWER), NO_POWER_SECONDS);
        }
        toldNoPower = waiting;
    }

    /**
     * The lamp inside: the fridge's comes on as its door leaves the seal and goes out as it comes back; the microwave's the same
     * way, and while it heats (on, with power, something in it that heats); the oven's while the stove is lit. On, its cover glows
     * and it lights what stands inside. The room around gets the block's own light by the same states ({@code FurnitureLight}).
     */
    private void light(StationView view, float dt, BlockState state, boolean shown) {
        if (view.node(LIGHTS.get(0)).isEmpty()) {
            return;
        }
        boolean on = switch (furniture) {
            case FRIDGE -> swings.get(Furniture.OPEN).open > DOOR_SWITCH;
            case MICROWAVE -> heating(view) || swings.get(Furniture.OPEN).open > DOOR_SWITCH;
            case STOVE -> flag(state, "lit");
            case GENERATOR -> flag(state, "powered");
            default -> false;
        };
        float step = dt / (on ? LAMP_ON_SECONDS : LAMP_OFF_SECONDS);
        lamp = on ? Math.min(1.0F, lamp + step) : Math.max(0.0F, lamp - step);
        boolean changed = lamp != lampDrawn;
        lampDrawn = lamp;
        for (String id : LIGHTS) {
            view.node(id).ifPresent(node -> {
                node.setVisible(shown);
                if (changed) {
                    int level = node.definition().light().map(light -> light.level()).orElse(0);
                    node.setLight(Math.round(level * lamp));
                    node.definition().visual().ifPresent(drawn -> node.setVisual(new Visual(drawn.model(), drawn.mesh(), drawn.texture(),
                            drawn.item(), drawn.block(), drawn.anchor(), lerpColor(LAMP_OFF, drawn.tint(), lamp), false, lamp > 0.0F, false,
                            0.0F, lamp <= 0.0F)));
                }
            });
        }
    }

    private static int lerpColor(int from, int to, float t) {
        int color = 0xFF000000;
        for (int shift = 0; shift < 24; shift += 8) {
            int a = from >> shift & 0xFF;
            int b = to >> shift & 0xFF;
            color |= Math.round(a + (b - a) * t) << shift;
        }
        return color;
    }

    /** Whether the microwave heats now: on, with power, with something in it that heats, as Refurbished hums while it does. */
    private static boolean heating(StationView view) {
        return view.level().getBlockEntity(view.pos()) instanceof MicrowaveBlockEntity microwave && microwave.canPlayAudio();
    }

    /**
     * Turns the microwave's plate while it heats (on, with power, something in it that heats), and what stands on the plate with
     * it, as Refurbished's microwave hums while it heats.
     */
    private void turn(StationView view, float dt, boolean shown) {
        if (heating(view)) {
            plateAngle = (plateAngle + dt * turntable.degreesPerSecond()) % 360.0F;
        }
        view.node(turntable.node()).ifPresent(node -> {
            node.setVisible(shown);
            node.rotateTo(0.0F, plateAngle, 0.0F, 0.0F);
        });
        Vector3f center = turntable.center();
        view.setSurfaceTransform(turntable.surface(), new Matrix4f().translation(center).rotateY((float) Math.toRadians(plateAngle))
                .translate(-center.x(), -center.y(), -center.z()));
    }

    /**
     * The oven's element glows while the stove is lit (switched on, with power), the generator's embers while it runs (switched
     * on, burning), as Refurbished's own states say.
     */
    private void dressElement(StationView view, BlockState state) {
        boolean generator = furniture == Furniture.GENERATOR;
        boolean lit = flag(state, generator ? "powered" : "lit");
        if (elementLit != null && elementLit == lit) {
            return;
        }
        elementLit = lit;
        int dark = generator ? ASH : COLD;
        view.node(generator ? EMBERS : ELEMENT).ifPresent(node -> node.definition().visual().ifPresent(drawn -> node.setVisual(
                new Visual(drawn.model(), drawn.mesh(), drawn.texture(), drawn.item(), drawn.block(), drawn.anchor(),
                        lit ? Visual.WHITE : dark, false, lit, false, 0.0F, !lit))));
    }

    /**
     * The generator's screen: a green bar of what is left of the fuel burning now, as its menu shows it. The server sends the
     * energy as each fuel goes in; while it runs it goes down by one a tick, counted here.
     */
    private void screen(StationView view, BlockState state, boolean shown) {
        if (view.node(SCREEN).isEmpty()) {
            return;
        }
        float share = 0.0F;
        if (view.level().getBlockEntity(view.pos()) instanceof ElectricityGeneratorBlockEntity generator) {
            int energy = generator.getEnergy();
            long now = view.level().getGameTime();
            if (energy != energySeen) {
                energySeen = energy;
                energySeenAt = now;
            }
            float left = flag(state, "powered") ? energySeen - (now - energySeenAt) : energySeen;
            int total = ((GeneratorAccess) generator).handson$totalEnergy();
            share = total > 0 ? Mth.clamp(left / total, 0.0F, 1.0F) : 0.0F;
        }
        // In steps of an eighth of a pixel: the bar moves as the fuel burns, it is not built anew every frame.
        float drawn = shown ? Math.round(share * 48.0F) / 48.0F : -1.0F;
        if (drawn == barDrawn) {
            return;
        }
        barDrawn = drawn;
        view.node(SCREEN).ifPresent(node -> {
            node.setVisible(drawn > 0.0F);
            if (drawn > 0.0F) {
                // Full from the viewer's left (+x here), shrinking towards it.
                float right = BAR_HALF - 2.0F * BAR_HALF * drawn;
                float u0 = 7.25F / 16.0F;
                float u1 = 7.75F / 16.0F;
                MeshShape.Builder bar = MeshShape.builder();
                // Wound to face the front (-z).
                bar.corner(right, BAR_HIGH, BAR_OFF, u1, u0, 0.0F, 0.0F, -1.0F);
                bar.corner(BAR_HALF, BAR_HIGH, BAR_OFF, u0, u0, 0.0F, 0.0F, -1.0F);
                bar.corner(BAR_HALF, 0.0F, BAR_OFF, u0, u1, 0.0F, 0.0F, -1.0F);
                bar.corner(right, 0.0F, BAR_OFF, u1, u1, 0.0F, 0.0F, -1.0F);
                node.setShape(bar.build(), BAR, true);
            }
        });
    }

    /** Gives the body and the moving parts the models of the block's variant (its wood, colour or finish). */
    private void dress(StationView view) {
        BlockState state = view.level().getBlockState(view.pos());
        shownFor = state;
        Optional<String> found = furniture.variantOf(state.getBlock());
        variant = found.orElse(null);
        if (variant == null) {
            return;
        }
        // A body whose door hangs on either side has its sides painted to match: one model per hinge.
        String hinge = hinge(state);
        // The scene's camera looks in past a door hung on the left; a door on the right has it from the other side.
        view.mirrorCamera("right".equals(hinge) ? MIDDLE : Float.NaN);
        // The generator's body by Refurbished's lamp on its front: green while it runs, amber while it does not. A mail box's with
        // its flag up while its mail is not checked.
        String body = switch (furniture) {
            case GENERATOR -> variant + "_body_" + (flag(state, "powered") ? "on" : "off");
            case MAIL_BOX -> variant + "_body" + (flag(state, "enabled") ? "_unchecked" : "");
            default -> variant + "_body" + (hinge != null ? "_" + hinge : row(state));
        };
        view.node("body").ifPresent(node -> node.setVisual(model(body, node)));
        for (Part part : parts) {
            view.node(part.node()).ifPresent(node -> node.setVisual(model(variant + "_" + part.model(), node)));
        }
        // The switch or knob in the finish of the piece, light or dark.
        view.node(Furniture.POWER).ifPresent(node -> node.setVisual(model(variant + "_power", node)));
        view.node(POWER_PLATE).ifPresent(node -> node.setVisual(model(variant + "_power_plate", node)));
        dressElement(view, state);
    }

    private static boolean fits(BlockState state, Part part) {
        if (part.hinge() == null) {
            return true;
        }
        String hinge = hinge(state);
        return part.hinge().equals(hinge != null ? hinge : "left");
    }

    /**
     * Where a piece that joins its neighbours (a chest of drawers) stands in its row, as Refurbished names its tops: {@code _left}
     * with one beside it on the left only, {@code _right} on the right only, {@code _middle} on both; empty when alone or for
     * blocks without the {@code left} and {@code right} properties.
     */
    private static String row(BlockState state) {
        boolean left = flag(state, "left");
        boolean right = flag(state, "right");
        return left && right ? "_middle" : left ? "_left" : right ? "_right" : "";
    }

    private static boolean flag(BlockState state, String name) {
        for (Property<?> property : state.getProperties()) {
            if (name.equals(property.getName()) && state.getValue(property) instanceof Boolean value) {
                return value;
            }
        }
        return false;
    }

    /** The side a door hangs on ({@code left} or {@code right}), for blocks that have it; null otherwise. */
    private static String hinge(BlockState state) {
        for (Property<?> property : state.getProperties()) {
            if ("hinge".equals(property.getName())) {
                return valueName(state, property);
            }
        }
        return null;
    }

    private static <T extends Comparable<T>> String valueName(BlockState state, Property<T> property) {
        return property.getName(state.getValue(property));
    }

    /**
     * The scene's copy of a part in the variant's model, standing on the node as the scene has it (a door on its hinge), lit as
     * the world lights the block so that the two look the same as they swap.
     */
    private Visual model(String name, NodeView node) {
        ResourceLocation id = HandsOnFurniture.id("diegesis/" + furniture.stationName() + "/" + name);
        Vector3fc anchor = node.definition().visual().map(Visual::anchor).orElse(Visual.DEFAULT_ANCHOR);
        return new Visual(Optional.of(id), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), anchor,
                Visual.WHITE, false, false, false, 0.0F, true);
    }
}
