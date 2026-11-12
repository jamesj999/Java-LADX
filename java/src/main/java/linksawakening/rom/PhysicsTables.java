package linksawakening.rom;
/** Immutable copies of PhysicsTables ROM lookup data. */
public final class PhysicsTables {
    private static final int PHYSICS_FLAGS_TABLE_SIZE = 0x100;
    public static final int PHYSICS_TABLE_OVERWORLD = 0;
    public static final int PHYSICS_TABLE_INDOORS1 = 1;
    public static final int PHYSICS_TABLE_INDOORS2 = 2;
    // OverworldObjectPhysicFlags: original loader length.
    private static final int PHYSICS_FLAGS_LENGTH = 256;
    private final int[][] physicsFlags;

    private PhysicsTables(Rom rom) {
        physicsFlags = new int[][] {
            rom.u8Table(RomSymbols.OverworldObjectPhysicFlags, PHYSICS_FLAGS_LENGTH),
            rom.u8Table(RomSymbols.OverworldObjectPhysicFlags.plus(PHYSICS_FLAGS_LENGTH), PHYSICS_FLAGS_LENGTH),
            rom.u8Table(RomSymbols.OverworldObjectPhysicFlags.plus(2 * PHYSICS_FLAGS_LENGTH), PHYSICS_FLAGS_LENGTH)
        };
    }

    public static PhysicsTables load(Rom rom) {
        return new PhysicsTables(rom);
    }

    /**
     * Physics flag for an object in the given context table. Pick
     * {@link #PHYSICS_TABLE_OVERWORLD} for overworld rooms and
     * {@link #PHYSICS_TABLE_INDOORS1} for regular indoor rooms (houses,
     * caves, dungeons). {@link #PHYSICS_TABLE_INDOORS2} covers Color Dungeon
     * with {@code wIsIndoor != 0}.
     */
    public int objectPhysicsFlag(int tableIndex, int objectId) {
        if (tableIndex < 0 || tableIndex >= physicsFlags.length) {
            return 0;
        }
        if (objectId < 0 || objectId >= PHYSICS_FLAGS_TABLE_SIZE) {
            return 0;
        }
        return physicsFlags[tableIndex][objectId];
    }

    /** Back-compat shim: overworld physics flag for an object id. */
    public int overworldPhysicsFlag(int objectId) {
        return objectPhysicsFlag(PHYSICS_TABLE_OVERWORLD, objectId);
    }
}
