package linksawakening.world;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import linksawakening.entity.Link;
import linksawakening.rom.RomTables;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class OverworldBushInteractionTest {

    private static final int ROOM_OBJECTS_BASE = 0x11;
    private static final int ROOM_OBJECT_ROW_STRIDE = 0x10;
    private static final int ROOM_TILE_WIDTH = 20;
    private static final int ROOM_TILE_HEIGHT = 16;

    private static final int OBJECT_BUSH = 0x5C;
    private static final int OBJECT_BUSH_GROUND_STAIRS = 0xD3;
    private static final int OBJECT_TALL_GRASS = 0x0A;
    private static final int OBJECT_SHORT_GRASS = 0x04;
    private static final int OBJECT_DIRT = 0x03;
    private static final int OBJECT_GROUND_STAIRS = 0xC6;
    private static final int OBJECT_PIT = 0xE8;
    private static final int TARGET_LOCATION = 0x54;
    private static final int TARGET_LINK_X = 0x36;
    private static final int TARGET_LINK_Y = 0x4C;

    private static final byte[] ROM = loadRom();
    private static final RomTables ROM_TABLES = RomTables.loadFromRom(ROM);
    private static final OverworldBushInteraction INTERACTION = new OverworldBushInteraction(ROM, ROM_TABLES);

    @Test
    void plainBushRevealsShortGrassInNormalRoom() {
        assertPlainBushReveal(0x40, OBJECT_SHORT_GRASS);
    }

    @Test
    void plainBushRevealsDirtInExceptionRoom() {
        assertPlainBushReveal(0x00, OBJECT_DIRT);
    }

    @Test
    void bushCoveredStairsRevealStairsInKnownRooms() {
        for (int roomId : new int[] {0x75, 0x07, 0xAA, 0x4A}) {
            assertBushCoveredStairsReveal(roomId, OBJECT_GROUND_STAIRS);
        }
    }

    @Test
    void bushCoveredStairsRevealPitElsewhere() {
        assertBushCoveredStairsReveal(0x40, OBJECT_PIT);
    }

    @Test
    void tallGrassIsCutToShortGrass() {
        int roomId = 0x40;
        int location = hitLocation(Link.DIRECTION_RIGHT, TARGET_LINK_X, TARGET_LINK_Y);
        int[] roomObjectsArea = emptyRoomObjectsArea();
        int[] mutableBackupObjects = emptyRoomObjectsArea();
        int[] roomTileIds = emptyRoomTiles(-1);
        int[] roomTileAttrs = emptyRoomTiles(-1);
        int areaIndex = ROOM_OBJECTS_BASE + location;
        roomObjectsArea[areaIndex] = OBJECT_TALL_GRASS;
        mutableBackupObjects[areaIndex] = OBJECT_TALL_GRASS;

        boolean changed = INTERACTION.cutBush(
            true,
            Link.DIRECTION_RIGHT,
            TARGET_LINK_X,
            TARGET_LINK_Y,
            roomId,
            true,
            roomObjectsArea,
            mutableBackupObjects,
            null,
            roomTileIds,
            roomTileAttrs
        );

        assertTrue(changed);
        assertEquals(OBJECT_SHORT_GRASS, roomObjectsArea[areaIndex]);
        assertEquals(OBJECT_SHORT_GRASS, mutableBackupObjects[areaIndex]);
    }

    @Test
    void tallGrassCutRequestsLeafScatterEffect() {
        int roomId = 0x40;
        int location = hitLocation(Link.DIRECTION_RIGHT, TARGET_LINK_X, TARGET_LINK_Y);
        int[] roomObjectsArea = emptyRoomObjectsArea();
        int[] roomTileIds = emptyRoomTiles(-1);
        int[] roomTileAttrs = emptyRoomTiles(-1);
        roomObjectsArea[ROOM_OBJECTS_BASE + location] = OBJECT_TALL_GRASS;

        OverworldBushInteraction.CutResult result = INTERACTION.cutObjectAtLocation(
            location,
            roomId,
            true,
            roomObjectsArea,
            null,
            null,
            roomTileIds,
            roomTileAttrs
        );

        assertTrue(result.changed());
        assertTrue(result.bushLeavesVisible());
    }

    @Test
    void shortGrassIsNotCutAgain() {
        int roomId = 0x40;
        int location = hitLocation(Link.DIRECTION_RIGHT, TARGET_LINK_X, TARGET_LINK_Y);
        int[] roomObjectsArea = emptyRoomObjectsArea();
        int[] roomTileIds = emptyRoomTiles(-1);
        int[] roomTileAttrs = emptyRoomTiles(-1);
        int areaIndex = ROOM_OBJECTS_BASE + location;
        roomObjectsArea[areaIndex] = OBJECT_SHORT_GRASS;

        boolean changed = INTERACTION.cutBush(
            true,
            Link.DIRECTION_RIGHT,
            TARGET_LINK_X,
            TARGET_LINK_Y,
            roomId,
            true,
            roomObjectsArea,
            null,
            null,
            roomTileIds,
            roomTileAttrs
        );

        assertFalse(changed);
        assertEquals(OBJECT_SHORT_GRASS, roomObjectsArea[areaIndex]);
    }

    @Test
    void inactiveSwordDoesNotReportAHitLocation() {
        assertEquals(-1, INTERACTION.swordHitRoomObjectLocation(false, Link.DIRECTION_RIGHT, TARGET_LINK_X, TARGET_LINK_Y));
    }

    @Test
    void swordHitLocationMatchesStaticSwordCollisionMapInAllDirections() {
        assertEquals(0x54, INTERACTION.swordHitRoomObjectLocation(true, Link.DIRECTION_RIGHT, TARGET_LINK_X, TARGET_LINK_Y));
        assertEquals(0x53, INTERACTION.swordHitRoomObjectLocation(true, Link.DIRECTION_LEFT, TARGET_LINK_X, TARGET_LINK_Y));
        assertEquals(0x43, INTERACTION.swordHitRoomObjectLocation(true, Link.DIRECTION_UP, TARGET_LINK_X, TARGET_LINK_Y));
        assertEquals(0x63, INTERACTION.swordHitRoomObjectLocation(true, Link.DIRECTION_DOWN, TARGET_LINK_X, TARGET_LINK_Y));
    }

    @Test
    void rightFacingUsesStaticBushReachNotGroundItemReach() {
        assertEquals(0x54, INTERACTION.swordHitRoomObjectLocation(true, Link.DIRECTION_RIGHT, 0x2A, TARGET_LINK_Y));
    }

    @Test
    void spinAttackCollisionMapReachesDiagonalBushCells() {
        assertEquals(0x64,
            INTERACTION.swordHitRoomObjectLocationForCollisionIndex(true, 5, TARGET_LINK_X, TARGET_LINK_Y));
        assertEquals(0x44,
            INTERACTION.swordHitRoomObjectLocationForCollisionIndex(true, 11, TARGET_LINK_X, TARGET_LINK_Y));
    }

    @Test
    void bushSmashEffectOriginMatchesRomEntitySpawnAnchor() {
        assertEquals(0x48, INTERACTION.effectOriginXForLocation(0x54));
        assertEquals(0x60, INTERACTION.effectOriginYForLocation(0x54));
    }

    @Test
    void gbcMutableBackupObjectStaysSynchronized() {
        int roomId = 0x40;
        int location = hitLocation(Link.DIRECTION_RIGHT, TARGET_LINK_X, TARGET_LINK_Y);
        int[] roomObjectsArea = emptyRoomObjectsArea();
        int[] mutableBackupObjects = emptyRoomObjectsArea();
        int[] roomTileIds = emptyRoomTiles(-1);
        int[] roomTileAttrs = emptyRoomTiles(-1);
        int areaIndex = ROOM_OBJECTS_BASE + location;
        roomObjectsArea[areaIndex] = OBJECT_BUSH;
        mutableBackupObjects[areaIndex] = OBJECT_BUSH;

        boolean changed = INTERACTION.cutBush(
            true,
            Link.DIRECTION_RIGHT,
            TARGET_LINK_X,
            TARGET_LINK_Y,
            roomId,
            true,
            roomObjectsArea,
            mutableBackupObjects,
            null,
            roomTileIds,
            roomTileAttrs
        );

        assertTrue(changed);
        assertEquals(OBJECT_SHORT_GRASS, roomObjectsArea[areaIndex]);
        assertEquals(OBJECT_SHORT_GRASS, mutableBackupObjects[areaIndex]);
    }

    @Test
    void roomTileAndAttrOutputRefreshAfterRevealInsteadOfRemainingStale() {
        int roomId = 0x40;
        int location = hitLocation(Link.DIRECTION_RIGHT, TARGET_LINK_X, TARGET_LINK_Y);
        int[] roomObjectsArea = emptyRoomObjectsArea();
        int[] roomTileIds = emptyRoomTiles(-1);
        int[] roomTileAttrs = emptyRoomTiles(-1);
        int areaIndex = ROOM_OBJECTS_BASE + location;
        roomObjectsArea[areaIndex] = OBJECT_BUSH;

        INTERACTION.refreshRoomObjectCell(roomId, location, roomObjectsArea, null, null, roomTileIds, roomTileAttrs);
        int[] beforeTiles = roomTileIds.clone();
        int[] beforeAttrs = roomTileAttrs.clone();

        boolean changed = INTERACTION.cutBush(
            true,
            Link.DIRECTION_RIGHT,
            TARGET_LINK_X,
            TARGET_LINK_Y,
            roomId,
            true,
            roomObjectsArea,
            null,
            null,
            roomTileIds,
            roomTileAttrs
        );

        assertTrue(changed);
        assertEquals(OBJECT_SHORT_GRASS, roomObjectsArea[areaIndex]);

        int[] expectedTiles = emptyRoomTiles(-1);
        int[] expectedAttrs = emptyRoomTiles(-1);
        INTERACTION.refreshRoomObjectCell(roomId, location, roomObjectsArea, null, null, expectedTiles, expectedAttrs);

        assertTrue(!Arrays.equals(beforeTiles, roomTileIds) || !Arrays.equals(beforeAttrs, roomTileAttrs));
        assertArrayEquals(expectedTiles, roomTileIds);
        assertArrayEquals(expectedAttrs, roomTileAttrs);
    }

    private static void assertPlainBushReveal(int roomId, int expectedReveal) {
        int location = hitLocation(Link.DIRECTION_RIGHT, TARGET_LINK_X, TARGET_LINK_Y);
        int[] roomObjectsArea = emptyRoomObjectsArea();
        int[] mutableBackupObjects = emptyRoomObjectsArea();
        int[] roomTileIds = emptyRoomTiles(-1);
        int[] roomTileAttrs = emptyRoomTiles(-1);
        int areaIndex = ROOM_OBJECTS_BASE + location;
        roomObjectsArea[areaIndex] = OBJECT_BUSH;
        mutableBackupObjects[areaIndex] = OBJECT_BUSH;

        INTERACTION.refreshRoomObjectCell(roomId, location, roomObjectsArea, null, null, roomTileIds, roomTileAttrs);
        int[] beforeTiles = roomTileIds.clone();
        int[] beforeAttrs = roomTileAttrs.clone();

        boolean changed = INTERACTION.cutBush(
            true,
            Link.DIRECTION_RIGHT,
            TARGET_LINK_X,
            TARGET_LINK_Y,
            roomId,
            true,
            roomObjectsArea,
            mutableBackupObjects,
            null,
            roomTileIds,
            roomTileAttrs
        );

        assertTrue(changed);
        assertEquals(expectedReveal, roomObjectsArea[areaIndex]);
        assertEquals(expectedReveal, mutableBackupObjects[areaIndex]);

        int[] expectedTiles = emptyRoomTiles(-1);
        int[] expectedAttrs = emptyRoomTiles(-1);
        roomObjectsArea[areaIndex] = expectedReveal;
        INTERACTION.refreshRoomObjectCell(roomId, location, roomObjectsArea, null, null, expectedTiles, expectedAttrs);

        assertTrue(!Arrays.equals(beforeTiles, roomTileIds) || !Arrays.equals(beforeAttrs, roomTileAttrs));
        assertArrayEquals(expectedTiles, roomTileIds);
        assertArrayEquals(expectedAttrs, roomTileAttrs);
    }

    private static void assertBushCoveredStairsReveal(int roomId, int expectedReveal) {
        int location = hitLocation(Link.DIRECTION_RIGHT, TARGET_LINK_X, TARGET_LINK_Y);
        int[] roomObjectsArea = emptyRoomObjectsArea();
        int[] mutableBackupObjects = emptyRoomObjectsArea();
        int[] roomTileIds = emptyRoomTiles(-1);
        int[] roomTileAttrs = emptyRoomTiles(-1);
        int areaIndex = ROOM_OBJECTS_BASE + location;
        roomObjectsArea[areaIndex] = OBJECT_BUSH_GROUND_STAIRS;
        mutableBackupObjects[areaIndex] = OBJECT_BUSH_GROUND_STAIRS;

        boolean changed = INTERACTION.cutBush(
            true,
            Link.DIRECTION_RIGHT,
            TARGET_LINK_X,
            TARGET_LINK_Y,
            roomId,
            true,
            roomObjectsArea,
            mutableBackupObjects,
            null,
            roomTileIds,
            roomTileAttrs
        );

        assertTrue(changed);
        assertEquals(expectedReveal, roomObjectsArea[areaIndex]);
        assertEquals(expectedReveal, mutableBackupObjects[areaIndex]);
        if (expectedReveal == OBJECT_GROUND_STAIRS) {
            assertEquals(OBJECT_GROUND_STAIRS, roomObjectsArea[areaIndex]);
        } else {
            assertEquals(OBJECT_PIT, roomObjectsArea[areaIndex]);
        }
    }

    private static int hitLocation(int direction, int linkPixelX, int linkPixelY) {
        int hitLocation = INTERACTION.swordHitRoomObjectLocation(true, direction, linkPixelX, linkPixelY);
        assertEquals(TARGET_LOCATION, hitLocation);
        return hitLocation;
    }

    private static int[] emptyRoomObjectsArea() {
        int[] roomObjectsArea = new int[0x100];
        Arrays.fill(roomObjectsArea, 0xFF);
        return roomObjectsArea;
    }

    private static int[] emptyRoomTiles(int filler) {
        int[] tiles = new int[ROOM_TILE_WIDTH * ROOM_TILE_HEIGHT];
        Arrays.fill(tiles, filler);
        return tiles;
    }

    private static byte[] loadRom() {
        try (InputStream in = OverworldBushInteractionTest.class.getClassLoader().getResourceAsStream("rom/azle.gbc")) {
            if (in == null) {
                throw new IllegalStateException("Missing ROM resource: rom/azle.gbc");
            }
            return in.readAllBytes();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read ROM resource", e);
        }
    }
}
