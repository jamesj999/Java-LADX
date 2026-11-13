package linksawakening.world.entity;

import java.util.function.IntSupplier;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.rom.Rom;
import linksawakening.rom.RomTables;
import linksawakening.world.room.ChestContentsTable;

/** Named runtime construction inputs. Validation occurs after all inputs are supplied. */
public final class RoomEntityRuntimeBuilder {
    // Builder input; validated or resolved to an explicit source by build().
    private final RoomEntitySnapshot initial;
    private boolean indoorRoom;
    // Builder input; validated or resolved to an explicit source by build().
    private IntSupplier randomByteSupplier;
    // Omitted builder input; build() selects the ROM-free policy.
    private EntitySpriteHandlerCatalog spriteHandlers;
    private RomEnemyCombatTables enemyCombatTables;
    // Omitted builder input; build() selects the ROM-free policy.
    private ChestContentsTable chestContentsTable;
    // Omitted builder input; build() selects the ROM-free policy.
    private RomTables romTables;
    // Omitted builder input; build() selects the ROM-free policy.
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

    EntitySpriteViews spriteHandlers() {
        return new EntitySpriteViews(spriteHandlers);
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

    public RoomEntityRuntimeBuilder enemyCombatTables(RomEnemyCombatTables value) {
        enemyCombatTables = value;
        enemyCombatTablesSupplied = true;
        return this;
    }

    EntityChestContents chestContentsTable() {
        return EntityChestContents.from(chestContentsTable);
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

    boolean hasRom() {
        return romDataSupplied;
    }

    java.util.function.IntUnaryOperator entityOptionsLookup() {
        return romTablesSupplied ? romTables.entityPropertyTables()::entityOptions1 : ignored -> 0;
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
