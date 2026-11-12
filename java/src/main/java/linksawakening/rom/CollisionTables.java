package linksawakening.rom;
/** Immutable copies of CollisionTables ROM lookup data. */
// Direction arguments are collision-table selectors; retain their bounds and index semantics rather than treating them
// as Link facing.
public final class CollisionTables {
    private static final int ENTITY_FINE_COLLISION_SHAPES_FIRST_PHYSICS = 0x7C;
    private static final int ENTITY_FINE_COLLISION_SHAPES_LAST_PHYSICS = 0x8D;
    private static final int ENTITY_FINE_COLLISION_SHAPES_QUADRANTS = 4;
    private static final int LINK_FINE_COLLISION_SHAPES_FIRST_PHYSICS = 0x7C;
    private static final int LINK_FINE_COLLISION_SHAPES_LAST_PHYSICS = 0x8F;
    // EntityCollisionPointsX: original loader length.
    private static final int ENTITY_COLLISION_POINTS_X_LENGTH = 16;
    private final byte[] entityCollisionPointsX;
    // EntityCollisionPointsY: original loader length.
    private static final int ENTITY_COLLISION_POINTS_Y_LENGTH = 16;
    private final byte[] entityCollisionPointsY;
    // FineCollisionShapes: original loader length.
    private static final int ENTITY_FINE_COLLISION_SHAPES_LENGTH = 72;
    private final int[] entityFineCollisionShapes;
    // Data_002_49CA: original loader length.
    private static final int LINK_FINE_COLLISION_SHAPES_LENGTH = 80;
    private final int[] linkFineCollisionShapes;

    private CollisionTables(Rom rom) {
        entityCollisionPointsX = rom.bytes(RomSymbols.EntityCollisionPointsX, ENTITY_COLLISION_POINTS_X_LENGTH);
        entityCollisionPointsY = rom.bytes(RomSymbols.EntityCollisionPointsY, ENTITY_COLLISION_POINTS_Y_LENGTH);
        entityFineCollisionShapes = rom.u8Table(RomSymbols.FineCollisionShapes, ENTITY_FINE_COLLISION_SHAPES_LENGTH);
        linkFineCollisionShapes = rom.u8Table(RomSymbols.Data_002_49CA, LINK_FINE_COLLISION_SHAPES_LENGTH);
    }

    public static CollisionTables load(Rom rom) {
        return new CollisionTables(rom);
    }

    /** Signed X offset from EntityCollisionPointsX[box][direction]. */
    public int entityCollisionPointX(int collisionBoxType, int direction) {
        int index = collisionPointIndex(collisionBoxType, direction);
        return entityCollisionPointsX[index];
    }

    /** Signed Y offset from EntityCollisionPointsY[box][direction]. */
    public int entityCollisionPointY(int collisionBoxType, int direction) {
        int index = collisionPointIndex(collisionBoxType, direction);
        return entityCollisionPointsY[index];
    }

    /** Unsigned fine-collision shape byte for a physics flag and quadrant. */
    public int entityFineCollisionShape(int physicsFlag, int quadrant) {
        if (physicsFlag < ENTITY_FINE_COLLISION_SHAPES_FIRST_PHYSICS
                || physicsFlag > ENTITY_FINE_COLLISION_SHAPES_LAST_PHYSICS
                || quadrant < 0
                || quadrant >= ENTITY_FINE_COLLISION_SHAPES_QUADRANTS) {
            return 0;
        }
        return entityFineCollisionShapes[
                (physicsFlag - ENTITY_FINE_COLLISION_SHAPES_FIRST_PHYSICS) * ENTITY_FINE_COLLISION_SHAPES_QUADRANTS
                        + quadrant];
    }

    /** Unsigned Link collision byte from Data_002_49CA[physics-$7C][quadrant]. */
    public int linkFineCollisionShape(int physicsFlag, int quadrant) {
        if (physicsFlag < LINK_FINE_COLLISION_SHAPES_FIRST_PHYSICS
                || physicsFlag > LINK_FINE_COLLISION_SHAPES_LAST_PHYSICS
                || quadrant < 0
                || quadrant >= ENTITY_FINE_COLLISION_SHAPES_QUADRANTS) {
            return 0;
        }
        return linkFineCollisionShapes[
                (physicsFlag - LINK_FINE_COLLISION_SHAPES_FIRST_PHYSICS) * ENTITY_FINE_COLLISION_SHAPES_QUADRANTS
                        + quadrant];
    }

    private static int collisionPointIndex(int collisionBoxType, int direction) {
        if (collisionBoxType < 0 || collisionBoxType >= 4 || direction < 0 || direction >= 4) {
            return 0;
        }
        return collisionBoxType * 4 + direction;
    }
}
