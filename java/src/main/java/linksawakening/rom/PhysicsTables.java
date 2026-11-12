package linksawakening.rom;
/** Immutable copies of PhysicsTables ROM lookup data. */
public final class PhysicsTables {
    private static final int PHYSICS_FLAGS_TABLE_SIZE = 0x100;
    private static final int PHYSICS_TABLE_OVERWORLD = 0;
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

    public int objectPhysicsFlag(int tableIndex, int objectId) {
        if (tableIndex < 0 || tableIndex >= physicsFlags.length) {
            return 0;
        }
        if (objectId < 0 || objectId >= PHYSICS_FLAGS_TABLE_SIZE) {
            return 0;
        }
        return physicsFlags[tableIndex][objectId];
    }

    public int overworldPhysicsFlag(int objectId) {
        return objectPhysicsFlag(PHYSICS_TABLE_OVERWORLD, objectId);
    }
}
