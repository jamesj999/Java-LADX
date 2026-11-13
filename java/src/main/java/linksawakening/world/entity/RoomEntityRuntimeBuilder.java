package linksawakening.world.entity;

import java.util.function.IntSupplier;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.rom.Rom;
import linksawakening.rom.RomTables;
import linksawakening.world.room.ChestContentsTable;

/** Named runtime construction inputs. Validation occurs after all inputs are supplied. */
public final class RoomEntityRuntimeBuilder {
    // Absent before initialization or while this reusable execution scratch is inactive.
    private RoomEntitySnapshot initial;
    private boolean indoorRoom;
    // Absent before initialization or while this reusable execution scratch is inactive.
    private IntSupplier randomByteSupplier;
    // Optional ROM-backed data; absent in ROM-free or unconfigured presentation state.
    private EntitySpriteHandlerCatalog spriteHandlers;
    // Optional ROM-backed data; absent in ROM-free or unconfigured presentation state.
    private RomEnemyCombatTables enemyCombatTables;
    // Optional ROM-backed data; absent in ROM-free or unconfigured presentation state.
    private ChestContentsTable chestContentsTable;
    // Optional ROM-backed data; absent in ROM-free or unconfigured presentation state.
    private RomTables romTables;
    // Optional ROM-backed data; absent in ROM-free or unconfigured presentation state.
    private Rom romData;
    private boolean enemyCombatTablesSupplied;
    private boolean chestContentsTableSupplied;
    private boolean romTablesSupplied;
    private boolean romDataSupplied;

    RoomEntityRuntimeBuilder(RoomEntitySnapshot initial) {
        this.initial = initial;
    }

    RoomEntitySnapshot initial() {
        return initial;
    }

    boolean indoorRoom() {
        return indoorRoom;
    }

    public RoomEntityRuntimeBuilder indoorRoom(boolean value) {
        indoorRoom = value;
        return this;
    }

    IntSupplier randomByteSupplier() {
        return randomByteSupplier;
    }

    public RoomEntityRuntimeBuilder randomByteSupplier(IntSupplier value) {
        randomByteSupplier = value;
        return this;
    }

    EntitySpriteHandlerCatalog spriteHandlers() {
        return spriteHandlers;
    }

    public RoomEntityRuntimeBuilder spriteHandlers(
            /* Optional ROM-backed data; absent in ROM-free or unconfigured presentation state. */ EntitySpriteHandlerCatalog
                    value) {
        spriteHandlers = value;
        return this;
    }

    RomEnemyCombatTables enemyCombatTables() {
        return java.util.Objects.requireNonNullElse(enemyCombatTables, RomEnemyCombatTables.DEFAULTS);
    }

    public RoomEntityRuntimeBuilder enemyCombatTables(
            /* Optional ROM-backed data; absent in ROM-free or unconfigured presentation state. */ RomEnemyCombatTables
                    value) {
        enemyCombatTables = value;
        enemyCombatTablesSupplied = true;
        return this;
    }

    ChestContentsTable chestContentsTable() {
        return chestContentsTable;
    }

    public RoomEntityRuntimeBuilder chestContentsTable(
            /* Optional ROM-backed data; absent in ROM-free or unconfigured presentation state. */ ChestContentsTable
                    value) {
        chestContentsTable = value;
        chestContentsTableSupplied = true;
        return this;
    }

    RomTables romTables() {
        return romTables;
    }

    public RoomEntityRuntimeBuilder romTables(
            /* Optional ROM-backed data; absent in ROM-free or unconfigured presentation state. */ RomTables value) {
        romTables = value;
        romTablesSupplied = true;
        return this;
    }

    Rom rom() {
        return romData;
    }

    public RoomEntityRuntimeBuilder rom(
            /* Optional ROM-backed data; absent in ROM-free or unconfigured presentation state. */ Rom value) {
        romData = value;
        romDataSupplied = true;
        return this;
    }

    public RoomEntityRuntime build() {
        if (romTablesSupplied || romDataSupplied) {
            if (initial == null
                    || enemyCombatTables == null
                    || chestContentsTable == null
                    || romTables == null
                    || (romDataSupplied && romData == null)) {
                throw new IllegalArgumentException("ROM room runtime inputs cannot be null");
            }
        } else {
            if (initial == null) throw new IllegalArgumentException("Initial entity snapshot cannot be null");
            if ((enemyCombatTablesSupplied || chestContentsTableSupplied) && enemyCombatTables == null)
                throw new IllegalArgumentException("ROM enemy combat tables cannot be null");
            if (chestContentsTableSupplied && chestContentsTable == null)
                throw new IllegalArgumentException("ROM chest contents table cannot be null");
        }
        return new RoomEntityRuntime(this);
    }
}
