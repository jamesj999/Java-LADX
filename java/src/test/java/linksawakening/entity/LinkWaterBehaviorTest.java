package linksawakening.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;

import linksawakening.equipment.ItemRegistry;
import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
import linksawakening.physics.OverworldCollision;
import linksawakening.rom.RomTables;
import linksawakening.state.PlayerState;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class LinkWaterBehaviorTest extends LinkFixture {

    @Test
    void deepWaterStartsRecoveryWithoutFlippersAndSwimmingWithFlippers() throws Exception {
        byte[] rom = loadRom();
        RomTables romTables = RomTables.loadFromRom(rom);
        InputConfig inputConfig = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        int[] roomObjects = emptyRoomObjectsArea();
        roomObjects[ROOM_OBJECTS_BASE + 2 * ROOM_OBJECT_ROW_STRIDE + 3] = OBJECT_DEEP_WATER;

        Link withoutFlippers = linkInRoom(inputConfig, romTables, roomObjects);
        withoutFlippers.setRoomEntryPixelPosition(0x20, 0x20);
        for (int i = 0; i < 20; i++) {
            withoutFlippers.update();
        }

        PlayerState playerState = new PlayerState();
        playerState.setHasFlippers(true);
        Link withFlippers =
                linkInRoom(inputConfig, romTables, roomObjects, playerState, RomTables.PHYSICS_TABLE_OVERWORLD);
        withFlippers.setPixelPosition(0x20, 0x20);
        for (int i = 0; i < 20 && !withFlippers.isSwimming(); i++) {
            withFlippers.update();
        }

        assertEquals(Link.LINK_MOTION_RECOVER, withoutFlippers.romMotionState());
        assertTrue(withoutFlippers.pixelX() > 0x24);
        assertTrue(withFlippers.isSwimming());
        assertEquals(0x01, withFlippers.romMotionState());
        while (withoutFlippers.romMotionState() == Link.LINK_MOTION_RECOVER) withoutFlippers.update();
        assertEquals(0x20, withoutFlippers.pixelX());
        assertEquals(0x20, withoutFlippers.pixelY());
        assertEquals(0x40, withoutFlippers.linkServices.playerState.invincibilityCounter());
    }

    @Test
    void waterEntryQueuesRomPositionSplashForBothBranchesAndGatesRecovery() throws Exception {
        RomTables tables = RomTables.loadFromRom(loadRom());
        for (boolean flippers : new boolean[] {false, true}) {
            PlayerState player = new PlayerState();
            player.setHasFlippers(flippers);
            int[] objects = emptyRoomObjectsArea();
            objects[ROOM_OBJECTS_BASE + 2 * ROOM_OBJECT_ROW_STRIDE + 2] = OBJECT_DEEP_WATER;
            Link link = linkInRoom(
                    new InputConfig(1, 2, 3, 4, 5, 6, 7), tables, objects, player, RomTables.PHYSICS_TABLE_OVERWORLD);
            link.setRoomEntryPixelPosition(16, 16);
            link.setPixelPosition(32, 32);
            assertTrue(link.linkSwimming.enterSwimmingIfNeeded());
            Link.WaterSplash splash = link.consumeWaterSplash();
            assertNotNull(splash);
            assertEquals(40, splash.romX());
            assertEquals(46, splash.romY());
            assertEquals(null, link.consumeWaterSplash());
            assertEquals(!flippers, link.recoveryBlocksInput());
            if (!flippers) {
                for (int i = 0; i < 79; i++) {
                    link.update();
                    assertTrue(link.recoveryBlocksInput());
                }
                link.update();
                assertFalse(link.recoveryBlocksInput());
            }
        }
    }

    @Test
    void raftAndSlowWaterCurrentBypassDrowningEntry() throws Exception {
        RomTables tables = RomTables.loadFromRom(loadRom());
        int[] objects = emptyRoomObjectsArea();
        objects[ROOM_OBJECTS_BASE + 2 * ROOM_OBJECT_ROW_STRIDE + 2] = OBJECT_DEEP_WATER;
        Link link = linkInRoom(new InputConfig(1, 2, 3, 4, 5, 6, 7), tables, objects);
        link.setPixelPosition(0x20, 0x20);
        link.linkStateMachine.itemUsageContext = 0x80;
        assertFalse(link.linkSwimming.enterSwimmingIfNeeded());
        assertEquals(Link.LINK_MOTION_DEFAULT, link.romMotionState());
        link.linkStateMachine.itemUsageContext = 0;
        link.linkStateMachine.slowWalkingSpeed = 1;
        link.linkStateMachine.physicsRoomId = 0x3E;
        assertTrue(link.linkSwimming.enterSwimmingIfNeeded());
        assertEquals(Link.LINK_MOTION_DEFAULT, link.romMotionState());
        assertEquals(0xFA, link.romSpeedX());
        assertTrue(link.pixelX() < 0x20);
    }

    @Test
    void swimmingUsesRomMotionAnimationAndReturnsToDefaultOnNormalGround() throws Exception {
        byte[] rom = loadRom();
        RomTables romTables = RomTables.loadFromRom(rom);
        InputConfig inputConfig = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        int[] roomObjects = emptyRoomObjectsArea();
        roomObjects[ROOM_OBJECTS_BASE + 2 * ROOM_OBJECT_ROW_STRIDE + 3] = OBJECT_DEEP_WATER;
        PlayerState playerState = new PlayerState();
        playerState.setHasFlippers(true);
        Link link = linkInRoom(inputConfig, romTables, roomObjects, playerState, RomTables.PHYSICS_TABLE_OVERWORLD);
        link.setPixelPosition(0x20, 0x20);

        int guard = 0;
        while (!link.isSwimming() && guard++ < 32) {
            link.update();
        }
        assertTrue(link.isSwimming());
        assertEquals(0x47, resolvedAnimationState(link));
        assertEquals(0x08, (byte) link.romSpeedX());

        // The room cell immediately beyond the water is empty, so continued
        // rightward motion must run the source's default-ground exit path.
        while (link.isSwimming() && guard++ < 128) {
            link.update();
        }

        assertFalse(link.isSwimming());
        assertEquals(0x00, link.romMotionState());
    }

    @Test
    void newlyPressedBEntersTheRomDivingSwimmingAnimation() throws Exception {
        byte[] rom = loadRom();
        RomTables romTables = RomTables.loadFromRom(rom);
        InputConfig inputConfig = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        InputState inputState = new InputState();
        PlayerState playerState = new PlayerState();
        playerState.setHasFlippers(true);
        int[] roomObjects = emptyRoomObjectsArea();
        for (int row = 0; row < 8; row++) {
            for (int column = 0; column < 10; column++) {
                roomObjects[ROOM_OBJECTS_BASE + row * ROOM_OBJECT_ROW_STRIDE + column] = OBJECT_DEEP_WATER;
            }
        }
        OverworldCollision collision = new OverworldCollision(romTables);
        collision.setRoom(roomObjects);
        Link link = new LinkTestBuilder()
                .input(inputState)
                .config(inputConfig)
                .tables(romTables)
                .collision(collision)
                .sprites(null)
                .player(playerState)
                .items(new ItemRegistry())
                .build();
        link.setPixelPosition(0x20, 0x20);
        link.update();

        inputState.tickEdges();
        inputState.onKeyEvent(inputConfig.bKey(), GLFW_PRESS);
        link.update();

        assertTrue(link.isSwimming());
        assertTrue(link.isDiving());
        assertEquals(0x4E, resolvedAnimationState(link));
    }
}
