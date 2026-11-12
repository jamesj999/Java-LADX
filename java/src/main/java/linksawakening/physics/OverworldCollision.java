package linksawakening.physics;

import linksawakening.rom.CollisionTables;
import linksawakening.rom.PhysicsTables;
import linksawakening.rom.RomTables;
import linksawakening.world.object.SwitchBlockLinkInteraction;

/**
 * Queries against the active overworld room's 16x16 object grid. Matches the
 * disassembly's {@code GetObjectPhysicsFlags} path: object id at a room cell ->
 * physics flag from {@code OverworldObjectPhysicFlags}.
 */
public final class OverworldCollision {
    private static final GroundInteractionSample EMPTY_SAMPLE =
            new GroundInteractionSample(0xFF, PhysicsFlags.NONE, 0, 0);
    // Immutable samples may be retained by callers. Refresh only the changed cell value.
    private final GroundInteractionSample[] groundSamples = new GroundInteractionSample[256];

    private static final int CELL_SIZE = 16;
    private static final int ROOM_OBJECTS_BASE = 0x11;
    private static final int ROOM_OBJECT_ROW_STRIDE = 0x10;
    private static final int OBJECTS_PER_ROW = 10;
    private static final int OBJECTS_PER_COLUMN = 8;

    private final PhysicsTables physicsTables;
    private final CollisionTables collisionTables;
    private int[] roomObjectsArea;
    private int physicsTableIndex = RomTables.PHYSICS_TABLE_OVERWORLD;
    private int switchBlocksState;
    private boolean linkStandingOnSwitchBlock;

    /**
     * Java top-left sprite position that aligns Link's ROM-style center/bottom
     * sample point with the pit cell's center/bottom target.
     */
    public record PitCell(int targetTopLeftX, int targetTopLeftY, int physicsFlag) {}

    /** The complete padded-buffer sample used by entity ground interaction. */
    public record GroundInteractionSample(int objectId, int physicsFlag, int objectLeft, int objectTop) {}

    public OverworldCollision(RomTables romTables) {
        this.physicsTables = romTables == null ? null : romTables.physicsTables();
        this.collisionTables = romTables == null ? null : romTables.collisionTables();
    }

    public void setRoom(int[] roomObjectsArea) {
        this.roomObjectsArea = roomObjectsArea;
        linkStandingOnSwitchBlock = false;
    }

    /** Mirrors the unsigned WRAM {@code wSwitchBlocksState} value. */
    public void setSwitchBlocksState(int value) {
        if (value != 0x00 && value != 0x02) {
            throw new IllegalArgumentException("Switch-block state must be $00 or $02: " + value);
        }
        switchBlocksState = value;
    }

    /** Mirrors {@code wLinkStandingOnSwitchBlock}. */
    public boolean linkStandingOnSwitchBlock() {
        return linkStandingOnSwitchBlock;
    }

    /**
     * Accepts the renderer's GBC overlay during room binding. The collision
     * path intentionally does not retain or query it: the ROM reads physics
     * from bank-0 {@code wRoomObjects}, while the overlay occupies WRAM bank 2
     * and is used only by {@code WriteOverworldObjectToBG}.
     */
    public void setGbcOverlay(int[] gbcOverlay) {
        // Rendering-only data; retained in ActiveRoom, not collision state.
    }

    /**
     * Select which of the three physics-flag tables to query. Mirrors the
     * wIsIndoor → table-offset dispatch in {@code GetObjectPhysicsFlags}
     * (bank0.asm:4513): {@code add hl, de} with {@code d = wIsIndoor}
     * advances the base pointer by {@code wIsIndoor × 256}, so overworld
     * rooms read the Overworld table and indoor rooms read Indoors1.
     */
    public void setPhysicsTable(int tableIndex) {
        this.physicsTableIndex = tableIndex;
    }

    /** Whether GetObjectPhysicsFlags is currently using an indoor table. */
    public boolean usesIndoorPhysicsTable() {
        return physicsTableIndex != RomTables.PHYSICS_TABLE_OVERWORLD;
    }

    /**
     * Whether the 8x8 cell containing {@code (pixelX, pixelY)} blocks
     * walking. Mirrors the point-based collision the disassembly does at
     * bank2.asm:6302 via {@code LinkCollisionPointsX/Y}: LA checks just
     * two points on the leading edge of movement (top two points for UP,
     * bottom two for DOWN, etc.), not a full bounding box.
     */
    public boolean pointBlocked(int pixelX, int pixelY) {
        return pointBlockedForLink(pixelX, pixelY, false);
    }

    /**
     * Link's leading-edge collision query. Water, lava and pits are passable;
     * ground physics handles their consequences ($02:7461).
     */
    public boolean pointBlockedForLink(int pixelX, int pixelY, boolean hasFlippers) {
        if (roomObjectsArea == null) {
            return false;
        }
        int cellX = cellCoordinate(pixelX);
        int cellY = cellCoordinate(pixelY);
        return isCellBlocking(cellX, cellY, pixelX, pixelY, hasFlippers);
    }

    public boolean pointNormalPit(int pixelX, int pixelY) {
        if (roomObjectsArea == null) {
            return false;
        }
        int cellX = cellCoordinate(pixelX);
        int cellY = cellCoordinate(pixelY);
        return isCellNormalPit(cellX, cellY);
    }

    /**
     * Returns the ROM physics byte for the cell sampled at a background
     * collision point. The padded room-object border contains object $FF.
     */
    public int objectPhysicsFlagAtPoint(int pixelX, int pixelY) {
        if (roomObjectsArea == null) {
            return PhysicsFlags.NONE;
        }
        int cellX = cellCoordinate(pixelX);
        int cellY = cellCoordinate(pixelY);
        if (cellX < 0 || cellX >= OBJECTS_PER_ROW || cellY < 0 || cellY >= OBJECTS_PER_COLUMN) {
            return physicsTables.objectPhysicsFlag(physicsTableIndex, 0xFF);
        }
        int objectId = objectIdAtCell(cellX, cellY);
        return physicsTables.objectPhysicsFlag(physicsTableIndex, objectId);
    }

    /** Returns the raw room object sampled at an entity collision point. */
    public int objectIdAtPoint(int pixelX, int pixelY) {
        if (roomObjectsArea == null) {
            return 0xFF;
        }
        int cellX = cellCoordinate(pixelX);
        int cellY = cellCoordinate(pixelY);
        return objectIdAtCell(cellX, cellY);
    }

    /**
     * Whether Link's current foot cell applies the ROM's slow-ground motion
     * gate. Mirrors {@code GetObjectUnderLink}: hLinkPositionX is the sprite
     * center and hLinkPositionY is the sprite bottom, then Y is sampled four
     * pixels above the bottom edge.
     */
    public boolean linkOnSlowGround(int linkPixelX, int linkPixelY) {
        int objectId = objectUnderLinkFeet(linkPixelX, linkPixelY);
        int flag = physicsTables.objectPhysicsFlag(physicsTableIndex, objectId);
        return PhysicsFlags.slowsWalking(flag);
    }

    public boolean linkOnNormalPit(int linkPixelX, int linkPixelY) {
        int objectId = objectUnderLinkFeet(linkPixelX, linkPixelY);
        int flag = physicsTables.objectPhysicsFlag(physicsTableIndex, objectId);
        return PhysicsFlags.isNormalPit(flag);
    }

    public boolean linkOnDeepWater(int linkPixelX, int linkPixelY) {
        int objectId = objectUnderLinkFeet(linkPixelX, linkPixelY);
        int flag = physicsTables.objectPhysicsFlag(physicsTableIndex, objectId);
        return flag == PhysicsFlags.DEEP_WATER;
    }

    public boolean linkOnPit(int x, int y) {
        int flag = objectPhysicsFlagAtPoint(x + 8, y + 12);
        return flag == 0x50 || flag == 0x51;
    }

    public PitCell normalPitUnderLink(int linkPixelX, int linkPixelY) {
        int cellX = cellCoordinate(linkPixelX + 8);
        int cellY = cellCoordinate(linkPixelY + 12);
        if (cellX < 0 || cellX >= OBJECTS_PER_ROW || cellY < 0 || cellY >= OBJECTS_PER_COLUMN) {
            return null;
        }
        int objectId = objectIdAtCell(cellX, cellY);
        int flag = physicsTables.objectPhysicsFlag(physicsTableIndex, objectId);
        if (flag != 0x50 && flag != 0x51) {
            return null;
        }
        return new PitCell(cellX * CELL_SIZE, cellY * CELL_SIZE, flag);
    }

    public int objectUnderLinkFeet(int linkPixelX, int linkPixelY) {
        int cellX = cellCoordinate(linkPixelX + 8);
        int cellY = cellCoordinate(linkPixelY + 12);
        return objectIdAtCell(cellX, cellY);
    }

    /**
     * Refreshes the transient switch-block footing flag from Link's current
     * ground sample. This is the bank-$02 ground-physics branch that runs
     * before the next leading-edge collision probe.
     */
    public boolean refreshLinkGroundInteraction(int linkPixelX, int linkPixelY) {
        int objectId = objectUnderLinkFeet(linkPixelX, linkPixelY);
        int physicsFlag = physicsTables.objectPhysicsFlag(physicsTableIndex, objectId);
        linkStandingOnSwitchBlock = SwitchBlockLinkInteraction.marksStanding(objectId, physicsFlag, switchBlocksState);
        return linkStandingOnSwitchBlock;
    }

    /**
     * Reads the object physics byte used by bank-$03's
     * {@code func_003_7E0E} before {@code ApplyEntityInteractionWithBackground}.
     * The helper samples {@code entityX - 1}, {@code entityY - 7} in the
     * padded room-object buffer, rather than one of the entity's directional
     * wall-collision points.
     */
    public int objectPhysicsFlagAtGroundInteraction(int entityX, int entityY) {
        return groundInteractionSample(entityX, entityY).physicsFlag();
    }

    /**
     * Reads the raw object and aligned coordinates returned by
     * {@code func_003_7E0E}. The caller can therefore follow the same sample
     * for status, splash, conveyor, and (later) pit handling.
     */
    public GroundInteractionSample groundInteractionSample(int entityX, int entityY) {
        if (roomObjectsArea == null) {
            return EMPTY_SAMPLE;
        }
        int sampleX = (entityX - 1) & 0xFF;
        int sampleY = (entityY - 7) & 0xFF;
        int objectLeft = sampleX & 0xF0;
        int objectTop = sampleY & 0xF0;
        return sampleAt(objectLeft, objectTop);
    }

    /**
     * Reads the object physics byte at the coordinate sampled by bank-$03's
     * {@code ApplySwordIntersectionWithObjects}: X is aligned directly and Y
     * is aligned after subtracting eight pixels. This intentionally uses the
     * padded room buffer rather than the flattened active 10x8 grid.
     */
    public int objectPhysicsFlagAtEntityPosition(int entityX, int entityY) {
        return entityInteractionSample(entityX, entityY).physicsFlag();
    }

    /** Complete padded-buffer sample used by ApplySwordIntersectionWithObjects. */
    public GroundInteractionSample entityInteractionSample(int entityX, int entityY) {
        if (roomObjectsArea == null) {
            return EMPTY_SAMPLE;
        }
        int x = entityX & 0xFF;
        int y = (entityY - 0x08) & 0xFF;
        int objectLeft = x & 0xF0;
        int objectTop = y & 0xF0;
        return sampleAt(objectLeft, objectTop);
    }

    private GroundInteractionSample sampleAt(int objectLeft, int objectTop) {
        int cell = objectTop + (objectLeft >>> 4);
        int areaIndex = ROOM_OBJECTS_BASE + cell;
        boolean inBounds = areaIndex >= 0 && areaIndex < roomObjectsArea.length;
        int objectId = inBounds ? roomObjectsArea[areaIndex] & 0xFF : 0xFF;
        int physicsFlag = inBounds ? physicsTables.objectPhysicsFlag(physicsTableIndex, objectId) : PhysicsFlags.NONE;
        GroundInteractionSample sample = groundSamples[cell];
        if (sample == null || sample.objectId() != objectId || sample.physicsFlag() != physicsFlag) {
            sample = new GroundInteractionSample(objectId, physicsFlag, objectLeft, objectTop);
            groundSamples[cell] = sample;
        }
        return sample;
    }

    private boolean isCellBlocking(int cellX, int cellY, int pixelX, int pixelY, boolean hasFlippers) {
        if (cellX < 0 || cellX >= OBJECTS_PER_ROW || cellY < 0 || cellY >= OBJECTS_PER_COLUMN) {
            // Off-room cells are the caller's problem - they trigger room scroll,
            // not a collision block. Treat as passable here.
            return false;
        }
        int areaIndex = ROOM_OBJECTS_BASE + cellY * ROOM_OBJECT_ROW_STRIDE + cellX;
        if (areaIndex < 0 || areaIndex >= roomObjectsArea.length) {
            return false;
        }
        int rawId = roomObjectsArea[areaIndex] & 0xFF;
        int physicsFlag = physicsTables.objectPhysicsFlag(physicsTableIndex, rawId);

        // Link's source path handles $7C..$8F through Data_002_49CA: bit 3
        // of each collision-point coordinate selects one of the object's four
        // 8x8 quadrants, and the corresponding shape byte decides solidity.
        // Read Link's own contiguous table because its source range includes
        // $8E/$8F beyond the entity FineCollisionShapes label.
        if (physicsFlag >= 0x7C && physicsFlag < 0x90) {
            int quadrant = ((pixelX >>> 3) & 0x01) | ((pixelY >>> 2) & 0x02);
            return collisionTables.linkFineCollisionShape(physicsFlag, quadrant) != 0;
        }

        if (physicsFlag == PhysicsFlags.DEEP_WATER
                || physicsFlag == PhysicsFlags.LAVA
                || PhysicsFlags.isPitCategory(physicsFlag)) {
            return false;
        }

        if (physicsFlag == SwitchBlockLinkInteraction.PHYSICS_OCEAN_SWITCH_BLOCK
                && SwitchBlockLinkInteraction.isSwitchBlock(rawId)) {
            return SwitchBlockLinkInteraction.blocks(rawId, physicsFlag, switchBlocksState, linkStandingOnSwitchBlock);
        }

        return idBlocks(rawId);
    }

    private boolean isCellNormalPit(int cellX, int cellY) {
        if (cellX < 0 || cellX >= OBJECTS_PER_ROW || cellY < 0 || cellY >= OBJECTS_PER_COLUMN) {
            return false;
        }
        int objectId = objectIdAtCell(cellX, cellY);
        int flag = physicsTables.objectPhysicsFlag(physicsTableIndex, objectId);
        return PhysicsFlags.isNormalPit(flag);
    }

    private int objectIdAtCell(int cellX, int cellY) {
        if (roomObjectsArea == null
                || cellX < 0
                || cellX >= OBJECTS_PER_ROW
                || cellY < 0
                || cellY >= OBJECTS_PER_COLUMN) {
            return 0xFF;
        }
        int areaIndex = ROOM_OBJECTS_BASE + cellY * ROOM_OBJECT_ROW_STRIDE + cellX;
        if (areaIndex < 0 || areaIndex >= roomObjectsArea.length) {
            return 0xFF;
        }
        return roomObjectsArea[areaIndex];
    }

    private static int cellCoordinate(int pixel) {
        return pixel < 0 ? pixel / CELL_SIZE - (pixel % CELL_SIZE == 0 ? 0 : 1) : pixel / CELL_SIZE;
    }

    /** Uses the active ROM physics table; object IDs have different meanings indoors. */
    private boolean idBlocks(int id) {
        if (id < 0 || id > 0xFF) {
            return false;
        }
        return PhysicsFlags.blocksWalking(physicsTables.objectPhysicsFlag(physicsTableIndex, id));
    }
}
