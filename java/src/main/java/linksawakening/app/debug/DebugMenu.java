package linksawakening.app.debug;

import static linksawakening.app.input.ApplicationKeys.*;
import static linksawakening.input.KeyCodes.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;
import linksawakening.state.PlayerState;
import linksawakening.world.room.RoomSession;
import linksawakening.world.transition.Warp;

/** Game-thread-only editor. No save I/O; changes use the ordinary save path. */
public final class DebugMenu {
    private static final String[] PAGES = {"TELEPORT", "INVENTORY", "RESOURCES", "QUEST", "DUNGEONS"};
    private final List<List<Entry>> pages = new ArrayList<>();
    private final TeleportTarget teleport;
    private boolean open, shift;
    private int page, selected, category, map, room, x, y, dungeon;
    private String message = "";

    /** A debug editor binding; reads and writes a field without session navigation callbacks. */
    public record Entry(
            String label, IntSupplier read, IntConsumer write, int min, int max, IntFunction<String> format) {
        public void change(int delta) {
            write.accept(Math.clamp(read.getAsInt() + delta, min, max));
        }

        public String text() {
            return label + ": " + format.apply(read.getAsInt());
        }
    }

    public DebugMenu(PlayerState p, RoomSession world, TeleportTarget teleport) {
        this.teleport = teleport;
        for (String ignored : PAGES) pages.add(new ArrayList<>());
        add(0, "Type", () -> category, v -> category = v, 0, 2, v -> new String[] {"Overworld", "Indoor", "Side view"}
                [v]);
        hex(0, "Map", () -> map, v -> map = v);
        hex(0, "Room", () -> room, v -> room = v);
        add(0, "Pixel X", () -> x, v -> x = v, 0, 144);
        add(0, "Pixel Y", () -> y, v -> y = v, 0, 112);
        add(0, "ENTER", () -> 0, v -> {}, 0, 0, v -> "Teleport");
        DebugMenuPages.inventory(this, p);
        DebugMenuPages.resources(this, p);
        DebugMenuPages.questItems(this, p, world);
        DebugMenuPages.questProgress(this, p, world);
        buildDungeons(world);
    }

    private void buildDungeons(RoomSession world) {
        add(4, "Dungeon", () -> dungeon, v -> dungeon = v, 0, 8, v -> v == 8 ? "Color" : Integer.toString(v + 1));
        String[] flags = {"Map", "Compass", "Stone beak", "Boss key", "Small keys"};
        for (int i = 0; i < flags.length; i++) {
            final int index = i;
            add(
                    4,
                    flags[i],
                    () -> Byte.toUnsignedInt(
                            dungeon == 8
                                    ? world.dungeon().colorDungeonItemFlagsSnapshot()[index]
                                    : world.dungeon().dungeonItemFlagsSnapshot()[dungeon * 5 + index]),
                    v -> world.dungeon().setDungeonItemForDebug(dungeon == 8 ? 255 : dungeon, index, v),
                    0,
                    i == 4 ? 255 : 1);
        }
        for (int i = 0; i < 8; i++) {
            final int index = i;
            add(
                    4,
                    "Instrument " + (i + 1),
                    () -> Byte.toUnsignedInt(world.dungeon().dungeonProgressFlagsSnapshot()[index]),
                    v -> {
                        byte[] flagsCopy = world.dungeon().dungeonProgressFlagsSnapshot();
                        flagsCopy[index] = (byte) v;
                        world.dungeon().restoreDungeonProgressFlags(flagsCopy);
                    },
                    0,
                    3);
        }
    }

    void add(int page, String name, IntSupplier get, IntConsumer set, int min, int max) {
        add(page, name, get, set, min, max, Integer::toString);
    }

    void add(int page, String name, IntSupplier get, IntConsumer set, int min, int max, IntFunction<String> format) {
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
        return key == KEY_F4 && action == PRESS;
    }

    public void key(int key, int action) {
        if (key == KEY_LEFT_SHIFT || key == KEY_RIGHT_SHIFT) {
            shift = action != RELEASE;
            return;
        }
        key(key, action, shift);
    }

    public void key(int key, int action, boolean fast) {
        if (action != PRESS && action != REPEAT) return;
        if (key == KEY_ESCAPE || key == KEY_F4) {
            open = false;
            return;
        }
        if (key == KEY_TAB || key == KEY_PAGE_DOWN || key == KEY_PAGE_UP) {
            page = Math.floorMod(page + (key == KEY_PAGE_UP ? -1 : 1), pages.size());
            selected = 0;
        } else if (key == KEY_UP || key == KEY_DOWN) {
            selected =
                    Math.floorMod(selected + (key == KEY_UP ? -1 : 1), entries().size());
        } else if (key == KEY_LEFT || key == KEY_RIGHT) {
            entries().get(selected).change((key == KEY_LEFT ? -1 : 1) * (fast ? 16 : 1));
        } else if (key == KEY_HOME || key == KEY_END) {
            Entry entry = entries().get(selected);
            entry.write().accept(key == KEY_HOME ? entry.min() : entry.max());
        } else if (key == KEY_ENTER && page == 0) {
            try {
                teleport.teleport(destination(category, map, room, x, y));
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
