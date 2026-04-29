package linksawakening.physics;

import linksawakening.rom.RomTables;

/**
 * Queries against the active overworld room's 16x16 object grid. Matches the
 * disassembly's {@code GetObjectPhysicsFlags} path: object id at a room cell ->
 * physics flag from {@code OverworldObjectPhysicFlags}.
 */
public final class OverworldCollision {

    private static final int CELL_SIZE = 16;
    private static final int ROOM_OBJECTS_BASE = 0x11;
    private static final int ROOM_OBJECT_ROW_STRIDE = 0x10;
    private static final int OBJECTS_PER_ROW = 10;
    private static final int OBJECTS_PER_COLUMN = 8;

    private final RomTables romTables;
    private int[] roomObjectsArea;
    private int[] gbcOverlay;
    private int physicsTableIndex = RomTables.PHYSICS_TABLE_OVERWORLD;

    public OverworldCollision(RomTables romTables) {
        this.romTables = romTables;
    }

    public void setRoom(int[] roomObjectsArea) {
        this.roomObjectsArea = roomObjectsArea;
    }

    /**
     * Provide the GBC overlay (80-byte, row-major, {@code row * 10 + col})
     * that the renderer uses for tile IDs. Collision queries both the raw
     * stream object and the overlay value and blocks if either would —
     * matches the visual, so Link can't walk through "edge tree" cells
     * where the stream places grass but the overlay draws a tree half.
     */
    public void setGbcOverlay(int[] gbcOverlay) {
        this.gbcOverlay = gbcOverlay;
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

    /**
     * Whether the 8x8 cell containing {@code (pixelX, pixelY)} blocks
     * walking. Mirrors the point-based collision the disassembly does at
     * bank2.asm:6302 via {@code LinkCollisionPointsX/Y}: LA checks just
     * two points on the leading edge of movement (top two points for UP,
     * bottom two for DOWN, etc.), not a full bounding box.
     */
    public boolean pointBlocked(int pixelX, int pixelY) {
        if (roomObjectsArea == null) {
            return false;
        }
        int cellX = cellCoordinate(pixelX);
        int cellY = cellCoordinate(pixelY);
        return isCellBlocking(cellX, cellY);
    }

    /**
     * Whether Link's current foot cell applies the ROM's slow-ground motion
     * gate. Mirrors {@code GetObjectUnderLink}: hLinkPositionX is the sprite
     * center and hLinkPositionY is the sprite bottom, then Y is sampled four
     * pixels above the bottom edge.
     */
    public boolean linkOnSlowGround(int linkPixelX, int linkPixelY) {
        int objectId = objectUnderLinkFeet(linkPixelX, linkPixelY);
        int flag = romTables.objectPhysicsFlag(physicsTableIndex, objectId);
        return PhysicsFlags.slowsWalking(flag);
    }

    public int objectUnderLinkFeet(int linkPixelX, int linkPixelY) {
        int cellX = cellCoordinate(linkPixelX + 8);
        int cellY = cellCoordinate(linkPixelY + 12);
        return objectIdAtCell(cellX, cellY);
    }

    private boolean isCellBlocking(int cellX, int cellY) {
        if (cellX < 0 || cellX >= OBJECTS_PER_ROW || cellY < 0 || cellY >= OBJECTS_PER_COLUMN) {
            // Off-room cells are the caller's problem - they trigger room scroll,
            // not a collision block. Treat as passable here.
            return false;
        }
        int areaIndex = ROOM_OBJECTS_BASE + cellY * ROOM_OBJECT_ROW_STRIDE + cellX;
        if (areaIndex < 0 || areaIndex >= roomObjectsArea.length) {
            return false;
        }
        int rawId = roomObjectsArea[areaIndex];

        // Screen-edge trees: the stream often places walkable grass (e.g.
        // $04) at a room's leftmost/rightmost column while the GBC overlay
        // draws the right/left half of a tree from the neighbouring room
        // (object ids $25-$2A or $82/$83). Without looking at the overlay,
        // Link walks through the visible tree. Block only for these
        // specifically-tree overlay ids — other overlay differences (like
        // $FB/$FE decorative markers) are just render hints and must not
        // affect collision.
        if (gbcOverlay != null) {
            int overlayIdx = cellY * OBJECTS_PER_ROW + cellX;
            if (overlayIdx >= 0 && overlayIdx < gbcOverlay.length) {
                int overlayId = gbcOverlay[overlayIdx];
                if (isTreeOverlayId(overlayId)) {
                    return true;
                }
            }
        }

        return idBlocks(rawId);
    }

    private int objectIdAtCell(int cellX, int cellY) {
        if (roomObjectsArea == null
            || cellX < 0 || cellX >= OBJECTS_PER_ROW
            || cellY < 0 || cellY >= OBJECTS_PER_COLUMN) {
            return 0xFF;
        }
        int areaIndex = ROOM_OBJECTS_BASE + cellY * ROOM_OBJECT_ROW_STRIDE + cellX;
        if (areaIndex < 0 || areaIndex >= roomObjectsArea.length) {
            return 0xFF;
        }
        return roomObjectsArea[areaIndex];
    }

    private static int cellCoordinate(int pixel) {
        return pixel < 0
            ? pixel / CELL_SIZE - (pixel % CELL_SIZE == 0 ? 0 : 1)
            : pixel / CELL_SIZE;
    }

    private static boolean isTreeOverlayId(int id) {
        return (id >= 0x25 && id <= 0x2A) || id == 0x82 || id == 0x83;
    }

    /**
     * Per-object-id collision decision, with overrides for cases where the
     * raw physics-table flag doesn't reflect in-game behaviour:
     *
     * <ul>
     *   <li>Door / warp-trigger types ({@code $C2/$C5/$C6/$E1/$E2/$E3/
     *       $BA/$CB/$61}) are walkable so {@code maybeTriggerWarpTransition}
     *       sees Link on the tile and the warp fires.</li>
     *   <li>Tree-over-bush variants ($82/$83), emitted by
     *       {@code TreeMacroHandler} when a tree's bottom row overlaps a
     *       bush cell, block despite having walkable overworld physics
     *       flags ($02 STAIRS, $03 DOOR) — those slots are shared with
     *       unrelated objects.</li>
     * </ul>
     */
    private boolean idBlocks(int id) {
        if (id < 0 || id > 0xFF) {
            return false;
        }
        switch (id) {
            case 0xC2:
            case 0xC5:
            case 0xC6:
            case 0xE1:
            case 0xE2:
            case 0xE3:
            case 0xBA:
            case 0xCB:
            case 0x61:
                return false;
            case 0x82:
            case 0x83:
                return true;
        }
        return PhysicsFlags.blocksWalking(romTables.objectPhysicsFlag(physicsTableIndex, id));
    }
}
