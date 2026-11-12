package linksawakening.world.entity;

import java.util.function.IntSupplier;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.rom.Rom;
import linksawakening.rom.RomTables;
import linksawakening.world.room.ChestContentsTable;

/** Named runtime construction inputs. Validation occurs after all inputs are supplied. */
public final class RoomEntityRuntimeBuilder {
    private RoomEntitySnapshot initial;
    private boolean indoorRoom;
    private IntSupplier randomByteSupplier;
    private EntitySpriteHandlerCatalog spriteHandlers;
    private RomEnemyCombatTables enemyCombatTables;
    private ChestContentsTable chestContentsTable;
    private RomTables romTables;
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

    public RoomEntityRuntimeBuilder spriteHandlers(EntitySpriteHandlerCatalog value) {
        spriteHandlers = value;
        return this;
    }

    RomEnemyCombatTables enemyCombatTables() {
        return enemyCombatTables;
    }

    public RoomEntityRuntimeBuilder enemyCombatTables(RomEnemyCombatTables value) {
        enemyCombatTables = value;
        enemyCombatTablesSupplied = true;
        return this;
    }

    ChestContentsTable chestContentsTable() {
        return chestContentsTable;
    }

    public RoomEntityRuntimeBuilder chestContentsTable(ChestContentsTable value) {
        chestContentsTable = value;
        chestContentsTableSupplied = true;
        return this;
    }

    RomTables romTables() {
        return romTables;
    }

    public RoomEntityRuntimeBuilder romTables(RomTables value) {
        romTables = value;
        romTablesSupplied = true;
        return this;
    }

    Rom rom() {
        return romData;
    }

    public RoomEntityRuntimeBuilder rom(Rom value) {
        romData = value;
        romDataSupplied = true;
        return this;
    }

    /** @deprecated External callers: world.room.RoomActiveRoomInitializer and RoomFollowerSynchronizer. Use rom(Rom). */
    @Deprecated(forRemoval = true)
    public RoomEntityRuntimeBuilder romData(byte[] value) {
        return rom(value == null ? null : new Rom(value));
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
