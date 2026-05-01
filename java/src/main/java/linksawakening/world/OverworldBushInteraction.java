package linksawakening.world;

import linksawakening.entity.Link;
import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.rom.RomTables;

/**
 * ROM-backed overworld sword interaction for breakable static objects.
 *
 * <p>Matches the disassembly split between {@code CheckStaticSwordCollision}
 * (bank0.asm:15AF) and {@code RevealObjectUnderObject} (bank14.asm:5526):
 * compute the sword-hit room-object cell from Link position and direction,
 * then mutate the active room object and refresh the 2x2 rendered output for
 * that single cell.
 */
public final class OverworldBushInteraction {

    private static final int BANK_SIZE = 0x4000;

    private static final int ROOM_OBJECTS_BASE = 0x11;
    private static final int ROOM_OBJECT_ROW_STRIDE = 0x10;
    private static final int OBJECTS_PER_ROW = 10;
    private static final int OBJECTS_PER_COLUMN = 8;
    private static final int ROOM_TILE_WIDTH = 20;

    private static final int OBJECT_BUSH = 0x5C;
    private static final int OBJECT_BUSH_GROUND_STAIRS = 0xD3;
    private static final int OBJECT_TALL_GRASS = 0x0A;
    private static final int OBJECT_SHORT_GRASS = 0x04;
    private static final int OBJECT_DIRT = 0x03;
    private static final int OBJECT_GROUND_STAIRS = 0xC6;
    private static final int OBJECT_PIT = 0xE8;

    private static final int OVERWORLD_TILEMAP_BANK = 0x1A;
    private static final int OVERWORLD_TILEMAP_ADDR = 0x6B1D;
    private static final int OVERWORLD_OBJ_ATTR_BANKS_BANK = 0x1A;
    private static final int OVERWORLD_OBJ_ATTR_BANKS_ADDR = 0x6476;
    private static final int OVERWORLD_OBJ_ATTR_PTRS_BANK = 0x1A;
    private static final int OVERWORLD_OBJ_ATTR_PTRS_ADDR = 0x5E76;

    private final byte[] romData;
    private final RomTables romTables;

    public record CutResult(boolean changed, int originalObjectId, int revealedObjectId,
                            boolean bushLeavesVisible, GameplaySoundEvent soundEvent) {
        public static CutResult unchanged(int originalObjectId) {
            return new CutResult(false, originalObjectId, originalObjectId, false, null);
        }
    }

    private record StaticObjectRule(int objectId, boolean bushLeavesVisible,
                                    RevealedObjectResolver revealedObjectResolver) {
        private CutResult cut(OverworldBushInteraction interaction, int roomId, boolean isGbcOverworld) {
            int revealed = revealedObjectResolver.resolve(interaction, roomId, isGbcOverworld);
            return new CutResult(true, objectId, revealed, bushLeavesVisible, GameplaySoundEvent.CUT_GRASS);
        }
    }

    @FunctionalInterface
    private interface RevealedObjectResolver {
        int resolve(OverworldBushInteraction interaction, int roomId, boolean isGbcOverworld);
    }

    private static final StaticObjectRule[] CUTTABLE_OBJECT_RULES = {
        new StaticObjectRule(OBJECT_BUSH, true, OverworldBushInteraction::revealGroundAfterCut),
        new StaticObjectRule(OBJECT_BUSH_GROUND_STAIRS, true,
            (interaction, roomId, isGbcOverworld) -> interaction.revealBushCoveredStairs(roomId)),
        new StaticObjectRule(OBJECT_TALL_GRASS, true, OverworldBushInteraction::revealGroundAfterCut),
    };

    public OverworldBushInteraction(byte[] romData, RomTables romTables) {
        this.romData = romData;
        this.romTables = romTables;
    }

    /**
     * Compute the YX-packed room-object location intersected by Link's sword.
     * Returns {@code -1} when static-object sword collision is inactive or the
     * hit point falls outside the active 10x8 room area.
     */
    public int swordHitRoomObjectLocation(boolean swordActive, int javaDirection,
                                          int linkPixelX, int linkPixelY) {
        return swordHitRoomObjectLocationForCollisionIndex(
            swordActive, toRomDirection(javaDirection), linkPixelX, linkPixelY);
    }

    /**
     * Compute the YX-packed room-object location intersected by the sword for
     * the ROM collision-map index:
     * 0-3 = cardinal swing, 4-11 = spin-attack angles.
     */
    public int swordHitRoomObjectLocationForCollisionIndex(boolean swordActive, int collisionIndex,
                                                           int linkPixelX, int linkPixelY) {
        if (!swordActive) {
            return -1;
        }
        // The ROM tables are relative to hLinkPositionX/Y, whose origin differs
        // from Java's Link.pixelX/Y():
        // - hLinkPositionX is Link's sprite-center X  => Java left + 8
        // - hLinkPositionY is Link's sprite-bottom Y  => Java top + 16
        // The ROM then subtracts 8 / 16 respectively, so in Java top-left
        // space the net expression is just pixel + tableOffset.
        int hitLeft = (linkPixelX + romTables.staticSwordCollisionOffsetX(collisionIndex)) & 0xF0;
        int hitTop = (linkPixelY + romTables.staticSwordCollisionOffsetY(collisionIndex)) & 0xF0;
        int cellX = hitLeft >> 4;
        int cellY = hitTop >> 4;
        if (cellX < 0 || cellX >= OBJECTS_PER_ROW || cellY < 0 || cellY >= OBJECTS_PER_COLUMN) {
            return -1;
        }
        return (cellY << 4) | cellX;
    }

    /**
     * Apply the overworld bush reveal logic for the current sword-hit cell.
     * Returns {@code true} if a supported bush object was changed.
     */
    public boolean cutBush(boolean swordActive, int javaDirection,
                           int linkPixelX, int linkPixelY,
                           int roomId, boolean isGbcOverworld,
                           int[] roomObjectsArea,
                           int[] mutableRenderValues,
                           int[] fallbackOverlay,
                           int[] roomTileIds,
                           int[] roomTileAttrs) {
        int location = swordHitRoomObjectLocation(swordActive, javaDirection, linkPixelX, linkPixelY);
        if (location < 0) {
            return false;
        }
        return cutBushAtLocation(location, roomId, isGbcOverworld, roomObjectsArea,
            mutableRenderValues, fallbackOverlay, roomTileIds, roomTileAttrs);
    }

    /**
     * Apply bush reveal logic at a specific YX-packed room-object location.
     */
    public boolean cutBushAtLocation(int location, int roomId, boolean isGbcOverworld,
                                     int[] roomObjectsArea,
                                     int[] mutableRenderValues,
                                     int[] fallbackOverlay,
                                     int[] roomTileIds,
                                     int[] roomTileAttrs) {
        return cutObjectAtLocation(location, roomId, isGbcOverworld, roomObjectsArea,
            mutableRenderValues, fallbackOverlay, roomTileIds, roomTileAttrs).changed();
    }

    public CutResult cutObjectAtLocation(int location, int roomId, boolean isGbcOverworld,
                                         int[] roomObjectsArea,
                                         int[] mutableRenderValues,
                                         int[] fallbackOverlay,
                                         int[] roomTileIds,
                                         int[] roomTileAttrs) {
        int areaIndex = ROOM_OBJECTS_BASE + location;
        if (roomObjectsArea == null || areaIndex < 0 || areaIndex >= roomObjectsArea.length) {
            return CutResult.unchanged(0xFF);
        }

        int objectId = roomObjectsArea[areaIndex];
        StaticObjectRule rule = cuttableRuleFor(objectId);
        if (rule == null) {
            return CutResult.unchanged(objectId);
        }
        CutResult result = rule.cut(this, roomId, isGbcOverworld);

        roomObjectsArea[areaIndex] = result.revealedObjectId();
        if (mutableRenderValues != null && areaIndex < mutableRenderValues.length) {
            mutableRenderValues[areaIndex] = result.revealedObjectId();
        }

        refreshRoomObjectCell(roomId, location, roomObjectsArea, mutableRenderValues,
            fallbackOverlay, roomTileIds, roomTileAttrs);
        return result;
    }

    /**
     * Refresh the 2x2 tile ids and attrs for one room-object cell from the
     * authoritative object/render-value tables.
     */
    public void refreshRoomObjectCell(int roomId, int location,
                                      int[] roomObjectsArea,
                                      int[] mutableRenderValues,
                                      int[] fallbackOverlay,
                                      int[] roomTileIds,
                                      int[] roomTileAttrs) {
        if (roomObjectsArea == null || roomTileIds == null || roomTileAttrs == null) {
            return;
        }

        int cellX = location & 0x0F;
        int cellY = (location >> 4) & 0x0F;
        if (cellX < 0 || cellX >= OBJECTS_PER_ROW || cellY < 0 || cellY >= OBJECTS_PER_COLUMN) {
            return;
        }

        int areaIndex = ROOM_OBJECTS_BASE + location;
        if (areaIndex < 0 || areaIndex >= roomObjectsArea.length) {
            return;
        }

        int objectId = roomObjectsArea[areaIndex];
        int renderValue = objectId;
        if (mutableRenderValues != null && areaIndex < mutableRenderValues.length
            && mutableRenderValues[areaIndex] >= 0 && mutableRenderValues[areaIndex] <= 0xFF) {
            renderValue = mutableRenderValues[areaIndex];
        } else if (fallbackOverlay != null) {
            int overlayIndex = cellY * OBJECTS_PER_ROW + cellX;
            if (overlayIndex >= 0 && overlayIndex < fallbackOverlay.length) {
                renderValue = fallbackOverlay[overlayIndex];
            }
        }

        int objectTilemapOffset = romOffset(OVERWORLD_TILEMAP_BANK, OVERWORLD_TILEMAP_ADDR);
        int tileTableOffset = objectTilemapOffset + (renderValue & 0xFF) * 4;

        int attrBankOffset = romOffset(OVERWORLD_OBJ_ATTR_BANKS_BANK, OVERWORLD_OBJ_ATTR_BANKS_ADDR + roomId);
        int attrBank = Byte.toUnsignedInt(romData[attrBankOffset]) | 0x20;
        int attrPtrsOffset = romOffset(OVERWORLD_OBJ_ATTR_PTRS_BANK, OVERWORLD_OBJ_ATTR_PTRS_ADDR + roomId * 2);
        int attrPtrLo = Byte.toUnsignedInt(romData[attrPtrsOffset]);
        int attrPtrHi = Byte.toUnsignedInt(romData[attrPtrsOffset + 1]);
        int attrTableOffset = romOffset(attrBank, (attrPtrHi << 8) | attrPtrLo) + (renderValue & 0xFF) * 4;

        for (int ty = 0; ty < 2; ty++) {
            for (int tx = 0; tx < 2; tx++) {
                int tileByteIndex = ty * 2 + tx;
                int mapX = cellX * 2 + tx;
                int mapY = cellY * 2 + ty;
                int mapIndex = mapY * ROOM_TILE_WIDTH + mapX;
                roomTileIds[mapIndex] = Byte.toUnsignedInt(romData[tileTableOffset + tileByteIndex]);
                roomTileAttrs[mapIndex] = Byte.toUnsignedInt(romData[attrTableOffset + tileByteIndex]);
            }
        }
    }

    public boolean roomRevealsStairs(int roomId) {
        return roomId == 0x75 || roomId == 0x07 || roomId == 0xAA || roomId == 0x4A;
    }

    /**
     * ROM-equivalent X anchor for the bush-smash visual spawned from the hit
     * room-object cell. Matches the entity spawn path after
     * {@code CheckStaticSwordCollision}: {@code hIntersectedObjectLeft + 8}.
     */
    public int effectOriginXForLocation(int location) {
        return ((location & 0x0F) << 4) + 0x08;
    }

    /**
     * ROM-equivalent Y anchor for the bush-smash visual spawned from the hit
     * room-object cell. Matches the entity spawn path after
     * {@code CheckStaticSwordCollision}: {@code hIntersectedObjectTop + 16}.
     */
    public int effectOriginYForLocation(int location) {
        return (((location >> 4) & 0x0F) << 4) + 0x10;
    }

    private static StaticObjectRule cuttableRuleFor(int objectId) {
        for (StaticObjectRule rule : CUTTABLE_OBJECT_RULES) {
            if (rule.objectId() == objectId) {
                return rule;
            }
        }
        return null;
    }

    private int revealGroundAfterCut(int roomId, boolean isGbcOverworld) {
        if (!isGbcOverworld) {
            return OBJECT_SHORT_GRASS;
        }
        if (roomId < 0x20 || roomId == 0xE0 || roomId == 0xE1 || roomId == 0xE3 || roomId == 0xE4) {
            return OBJECT_DIRT;
        }
        if (roomId == 0xFF) {
            return OBJECT_DIRT;
        }
        return OBJECT_SHORT_GRASS;
    }

    private int revealBushCoveredStairs(int roomId) {
        return roomRevealsStairs(roomId) ? OBJECT_GROUND_STAIRS : OBJECT_PIT;
    }

    private static int toRomDirection(int javaDirection) {
        switch (javaDirection) {
            case Link.DIRECTION_RIGHT:
                return 0;
            case Link.DIRECTION_LEFT:
                return 1;
            case Link.DIRECTION_UP:
                return 2;
            case Link.DIRECTION_DOWN:
            default:
                return 3;
        }
    }

    private static int romOffset(int bank, int address) {
        if (bank <= 0) {
            return address;
        }
        return bank * BANK_SIZE + (address - 0x4000);
    }
}
