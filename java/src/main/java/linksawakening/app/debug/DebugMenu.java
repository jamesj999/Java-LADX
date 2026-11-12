package linksawakening.app.debug;

import static org.lwjgl.glfw.GLFW.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import linksawakening.state.PlayerState;
import linksawakening.world.room.RoomSession;
import linksawakening.world.transition.Warp;

/** Game-thread-only editor. No save I/O; changes use the ordinary save path. */
public final class DebugMenu {
    private static final String[] PAGES = {"TELEPORT", "INVENTORY", "RESOURCES", "QUEST", "DUNGEONS"};
    private static final String[] ITEMS = {
        "Empty",
        "Sword",
        "Bombs",
        "Bracelet",
        "Shield",
        "Bow",
        "Hookshot",
        "Magic rod",
        "Boots",
        "Ocarina",
        "Feather",
        "Shovel",
        "Powder",
        "Boomerang"
    };
    // TRADING_ITEM_* in LADX-Disassembly/src/constants/gameplay.asm.
    private static final String[] TRADES = {
        "None",
        "Yoshi doll",
        "Ribbon",
        "Dog food",
        "Bananas",
        "Stick",
        "Honeycomb",
        "Pineapple",
        "Hibiscus",
        "Letter",
        "Broom",
        "Fishing hook",
        "Necklace",
        "Scale",
        "Magnifying lens"
    };
    private final List<List<Entry>> pages = new ArrayList<>();
    private final Consumer<Warp> teleport;
    private boolean open, shift;
    private int page, selected, category, map, room, x, y, dungeon;
    private String message = "";

    public record Entry(
            String label,
            IntSupplier read,
            IntConsumer write,
            int min,
            int max,
            java.util.function.IntFunction<String> format) {
        public void change(int delta) {
            write.accept(Math.clamp(read.getAsInt() + delta, min, max));
        }

        public String text() {
            return label + ": " + format.apply(read.getAsInt());
        }
    }

    public DebugMenu(PlayerState p, RoomSession world, Consumer<Warp> teleport) {
        this.teleport = teleport;
        for (String ignored : PAGES) pages.add(new ArrayList<>());
        add(0, "Type", () -> category, v -> category = v, 0, 2, v -> new String[] {"Overworld", "Indoor", "Side view"}
                [v]);
        hex(0, "Map", () -> map, v -> map = v);
        hex(0, "Room", () -> room, v -> room = v);
        add(0, "Pixel X", () -> x, v -> x = v, 0, 144);
        add(0, "Pixel Y", () -> y, v -> y = v, 0, 112);
        add(0, "ENTER", () -> 0, v -> {}, 0, 0, v -> "Teleport");
        add(1, "A", p::itemA, p::setItemA, 0, 13, v -> ITEMS[v]);
        add(1, "B", p::itemB, p::setItemB, 0, 13, v -> ITEMS[v]);
        for (int i = 0; i < PlayerState.SUBSCREEN_SLOT_COUNT; i++) {
            final int slot = i;
            add(
                    1,
                    "Slot " + (i + 1),
                    () -> p.subscreenItem(slot),
                    v -> {
                        int[] items = new int[PlayerState.SUBSCREEN_SLOT_COUNT];
                        for (int j = 0; j < items.length; j++) items[j] = p.subscreenItem(j);
                        items[slot] = v;
                        p.setSubscreenItems(items);
                    },
                    0,
                    13,
                    v -> ITEMS[v]);
        }
        add(1, "Sword level", p::swordLevel, p::setSwordLevel, 0, 2);
        add(1, "Shield level", p::shieldLevel, p::setShieldLevel, 0, 2);
        add(1, "Bracelet level", p::powerBraceletLevel, p::setPowerBraceletLevel, 0, 2);
        add(
                2,
                "Rupees",
                p::rupees,
                v -> {
                    p.clearResourceBuffersForDebug();
                    p.setRupees(v);
                },
                0,
                999);
        add(
                2,
                "Hearts",
                p::maxHearts,
                v -> {
                    p.clearResourceBuffersForDebug();
                    p.setMaxHearts(v);
                },
                1,
                14);
        add(
                2,
                "HP (1/8 heart)",
                p::health,
                v -> {
                    p.clearResourceBuffersForDebug();
                    p.setHealth(v);
                },
                0,
                112);
        add(2, "Heart pieces", p::heartPieces, p::setHeartPieces, 0, 3);
        add(2, "Max arrows", p::maxArrows, p::setMaxArrows, 0, 99);
        add(2, "Arrows", p::arrowCount, p::setArrowCount, 0, 99);
        add(2, "Max bombs", p::maxBombs, p::setMaxBombs, 0, 99);
        add(2, "Bombs", p::bombCount, p::setBombCount, 0, 99);
        add(2, "Max powder", p::maxMagicPowder, p::setMaxMagicPowder, 0, 99);
        add(2, "Powder", p::magicPowderCount, p::setMagicPowderCount, 0, 99);
        add(2, "Seashells", p::seashells, p::setSeashells, 0, 99);
        add(3, "Flippers", () -> p.hasFlippers() ? 1 : 0, v -> p.setHasFlippers(v != 0), 0, 1);
        add(3, "Toadstool", () -> p.hasToadstool() ? 1 : 0, v -> p.setHasToadstool(v != 0), 0, 1);
        add(
                3,
                "Medicine",
                p::medicineCount,
                v -> {
                    p.setMedicineCount(v);
                    p.setHasMedicine(v != 0);
                },
                0,
                255);
        add(
                3,
                "Tail key",
                p::tailKeyCount,
                v -> {
                    p.setTailKeyCount(v);
                    world.setTailKeyOwned(v != 0);
                },
                0,
                1);
        add(3, "Angler key", p::anglerKeyCount, p::setAnglerKeyCount, 0, 1);
        add(3, "Face key", p::faceKeyCount, p::setFaceKeyCount, 0, 1);
        add(
                3,
                "Bird key",
                p::birdKeyCount,
                v -> {
                    p.setBirdKeyCount(v);
                    world.setBirdKeyOwned(v != 0);
                },
                0,
                1);
        add(3, "Leaves/key", p::goldenLeavesCount, p::setGoldenLeavesCount, 0, 6);
        add(
                3,
                "Trade",
                p::tradeSequenceItem,
                p::setTradeSequenceItem,
                0,
                14,
                v -> v < TRADES.length ? TRADES[v] : Integer.toString(v));
        add(3, "Song bits", p::ocarinaSongFlags, p::setOcarinaSongFlags, 0, 7);
        add(3, "Tunic", p::tunicType, p::setTunicType, 0, 2);
        add(3, "BowWow state", world::bowWowState, world::setBowWowState, 0, 255);
        add(3, "Richard flag", world::richardSpokenFlag, world::setRichardSpokenFlag, 0, 255);
        add(3, "Tarin flag", world::tarinFlag, world::setTarinFlag, 0, 255);
        add(4, "Dungeon", () -> dungeon, v -> dungeon = v, 0, 8, v -> v == 8 ? "Color" : Integer.toString(v + 1));
        String[] flags = {"Map", "Compass", "Stone beak", "Boss key", "Small keys"};
        for (int i = 0; i < flags.length; i++) {
            final int index = i;
            add(
                    4,
                    flags[i],
                    () -> Byte.toUnsignedInt(
                            dungeon == 8
                                    ? world.colorDungeonItemFlagsSnapshot()[index]
                                    : world.dungeonItemFlagsSnapshot()[dungeon * 5 + index]),
                    v -> world.setDungeonItemForDebug(dungeon == 8 ? 255 : dungeon, index, v),
                    0,
                    i == 4 ? 255 : 1);
        }
        for (int i = 0; i < 8; i++) {
            final int index = i;
            add(
                    4,
                    "Instrument " + (i + 1),
                    () -> Byte.toUnsignedInt(world.dungeonProgressFlagsSnapshot()[index]),
                    v -> {
                        byte[] flagsCopy = world.dungeonProgressFlagsSnapshot();
                        flagsCopy[index] = (byte) v;
                        world.restoreDungeonProgressFlags(flagsCopy);
                    },
                    0,
                    3);
        }
    }

    private void add(int page, String name, IntSupplier get, IntConsumer set, int min, int max) {
        add(page, name, get, set, min, max, Integer::toString);
    }

    private void add(
            int page,
            String name,
            IntSupplier get,
            IntConsumer set,
            int min,
            int max,
            java.util.function.IntFunction<String> format) {
        pages.get(page).add(new Entry(name, get, set, min, max, format));
    }

    private void hex(int page, String name, IntSupplier get, IntConsumer set) {
        add(page, name, get, set, 0, 255, v -> String.format("%02X", v));
    }

    public boolean isOpen() {
        return open;
    }

    public void open(int category, int map, int room, int x, int y) {
        this.category = category;
        this.map = map;
        this.room = room;
        this.x = Math.clamp(x, 0, 144);
        this.y = Math.clamp(y, 0, 112);
        message = "";
        shift = false;
        open = true;
    }

    public static boolean isToggle(int key, int action) {
        return key == GLFW_KEY_F4 && action == GLFW_PRESS;
    }

    public void key(int key, int action) {
        if (key == GLFW_KEY_LEFT_SHIFT || key == GLFW_KEY_RIGHT_SHIFT) {
            shift = action != GLFW_RELEASE;
            return;
        }
        key(key, action, shift);
    }

    public void key(int key, int action, boolean fast) {
        if (action != GLFW_PRESS && action != GLFW_REPEAT) return;
        if (key == GLFW_KEY_ESCAPE || key == GLFW_KEY_F4) {
            open = false;
            return;
        }
        if (key == GLFW_KEY_TAB || key == GLFW_KEY_PAGE_DOWN || key == GLFW_KEY_PAGE_UP) {
            page = Math.floorMod(page + (key == GLFW_KEY_PAGE_UP ? -1 : 1), pages.size());
            selected = 0;
        } else if (key == GLFW_KEY_UP || key == GLFW_KEY_DOWN) {
            selected = Math.floorMod(
                    selected + (key == GLFW_KEY_UP ? -1 : 1), entries().size());
        } else if (key == GLFW_KEY_LEFT || key == GLFW_KEY_RIGHT) {
            entries().get(selected).change((key == GLFW_KEY_LEFT ? -1 : 1) * (fast ? 16 : 1));
        } else if (key == GLFW_KEY_HOME || key == GLFW_KEY_END) {
            Entry entry = entries().get(selected);
            entry.write().accept(key == GLFW_KEY_HOME ? entry.min() : entry.max());
        } else if (key == GLFW_KEY_ENTER && page == 0) {
            try {
                teleport.accept(destination(category, map, room, x, y));
                message = "Teleported. F4 resumes.";
            } catch (IllegalArgumentException e) {
                message = "Invalid map/room selection";
            }
        }
    }

    public static Warp destination(int category, int map, int room, int x, int y) {
        if (category < 0
                || category > 2
                || map < 0
                || map > 255
                || room < 0
                || room > 255
                || x < 0
                || x > 144
                || y < 0
                || y > 112
                || (category != 0 && map > 0x1F && map != 0xFF)
                || (category != 0 && map == 0xFF && room > 0x15))
            throw new IllegalArgumentException("Invalid destination");
        return new Warp(category, category == 0 ? 0 : map, room, x + 8, y + 16, 0);
    }

    public List<Entry> entries() {
        return List.copyOf(pages.get(page));
    }

    public int selected() {
        return selected;
    }

    public String title() {
        return "DEBUG " + (page + 1) + "/5 " + PAGES[page];
    }

    public String message() {
        return message;
    }
}
