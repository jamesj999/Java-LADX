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
        withoutFlippers.position().setRoomEntryPixelPosition(0x20, 0x20);
        for (int i = 0; i < 20; i++) {
            withoutFlippers.motion().update();
        }

        PlayerState playerState = new PlayerState();
        playerState.progression().setHasFlippers(true);
        Link withFlippers =
                linkInRoom(inputConfig, romTables, roomObjects, playerState, RomTables.PHYSICS_TABLE_OVERWORLD);
        withFlippers.position().setPixelPosition(0x20, 0x20);
        for (int i = 0; i < 20 && !withFlippers.motion().isSwimming(); i++) {
            withFlippers.motion().update();
        }

        assertEquals(Link.LINK_MOTION_RECOVER, withoutFlippers.airborne().romMotionState());
        assertTrue(withoutFlippers.position().pixelX() > 0x24);
        assertTrue(withFlippers.motion().isSwimming());
        assertEquals(0x01, withFlippers.airborne().romMotionState());
        while (withoutFlippers.airborne().romMotionState() == Link.LINK_MOTION_RECOVER)
            withoutFlippers.motion().update();
        assertEquals(0x20, withoutFlippers.position().pixelX());
        assertEquals(0x20, withoutFlippers.position().pixelY());
        assertEquals(0x40, withoutFlippers.linkServices.playerState.invincibilityCounter());
    }

    @Test
    void waterEntryQueuesRomPositionSplashForBothBranchesAndGatesRecovery() throws Exception {
        RomTables tables = RomTables.loadFromRom(loadRom());
        for (boolean flippers : new boolean[] {false, true}) {
            PlayerState player = new PlayerState();
            player.progression().setHasFlippers(flippers);
            int[] objects = emptyRoomObjectsArea();
            objects[ROOM_OBJECTS_BASE + 2 * ROOM_OBJECT_ROW_STRIDE + 2] = OBJECT_DEEP_WATER;
            Link link = linkInRoom(
                    new InputConfig(1, 2, 3, 4, 5, 6, 7), tables, objects, player, RomTables.PHYSICS_TABLE_OVERWORLD);
            link.position().setRoomEntryPixelPosition(16, 16);
            link.position().setPixelPosition(32, 32);
            assertTrue(link.linkSwimming.enterSwimmingIfNeeded());
            Link.WaterSplash splash = link.status().consumeWaterSplash();
            assertNotNull(splash);
            assertEquals(40, splash.romX());
            assertEquals(46, splash.romY());
            assertEquals(null, link.status().consumeWaterSplash());
            assertEquals(!flippers, link.status().recoveryBlocksInput());
            if (!flippers) {
                for (int i = 0; i < 79; i++) {
                    link.motion().update();
                    assertTrue(link.status().recoveryBlocksInput());
                }
                link.motion().update();
                assertFalse(link.status().recoveryBlocksInput());
            }
        }
    }

    @Test
    void raftAndSlowWaterCurrentBypassDrowningEntry() throws Exception {
        RomTables tables = RomTables.loadFromRom(loadRom());
        int[] objects = emptyRoomObjectsArea();
        objects[ROOM_OBJECTS_BASE + 2 * ROOM_OBJECT_ROW_STRIDE + 2] = OBJECT_DEEP_WATER;
        Link link = linkInRoom(new InputConfig(1, 2, 3, 4, 5, 6, 7), tables, objects);
        link.position().setPixelPosition(0x20, 0x20);
        link.linkStateMachine.itemUsageContext = 0x80;
        assertFalse(link.linkSwimming.enterSwimmingIfNeeded());
        assertEquals(Link.LINK_MOTION_DEFAULT, link.airborne().romMotionState());
        link.linkStateMachine.itemUsageContext = 0;
        link.linkStateMachine.slowWalkingSpeed = 1;
        link.linkStateMachine.physicsRoomId = 0x3E;
        assertTrue(link.linkSwimming.enterSwimmingIfNeeded());
        assertEquals(Link.LINK_MOTION_DEFAULT, link.airborne().romMotionState());
        assertEquals(0xFA, link.position().romSpeedX());
        assertTrue(link.position().pixelX() < 0x20);
    }

    @Test
    void swimmingUsesRomMotionAnimationAndReturnsToDefaultOnNormalGround() throws Exception {
        byte[] rom = loadRom();
        RomTables romTables = RomTables.loadFromRom(rom);
        InputConfig inputConfig = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        int[] roomObjects = emptyRoomObjectsArea();
        roomObjects[ROOM_OBJECTS_BASE + 2 * ROOM_OBJECT_ROW_STRIDE + 3] = OBJECT_DEEP_WATER;
        PlayerState playerState = new PlayerState();
        playerState.progression().setHasFlippers(true);
        Link link = linkInRoom(inputConfig, romTables, roomObjects, playerState, RomTables.PHYSICS_TABLE_OVERWORLD);
        link.position().setPixelPosition(0x20, 0x20);

        int guard = 0;
        while (!link.motion().isSwimming() && guard++ < 32) {
            link.motion().update();
        }
        assertTrue(link.motion().isSwimming());
        assertEquals(0x47, resolvedAnimationState(link));
        assertEquals(0x08, (byte) link.position().romSpeedX());

        // The room cell immediately beyond the water is empty, so continued
        // rightward motion must run the source's default-ground exit path.
        while (link.motion().isSwimming() && guard++ < 128) {
            link.motion().update();
        }

        assertFalse(link.motion().isSwimming());
        assertEquals(0x00, link.airborne().romMotionState());
    }

    @Test
    void newlyPressedBEntersTheRomDivingSwimmingAnimation() throws Exception {
        byte[] rom = loadRom();
        RomTables romTables = RomTables.loadFromRom(rom);
        InputConfig inputConfig = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        InputState inputState = new InputState();
        PlayerState playerState = new PlayerState();
        playerState.progression().setHasFlippers(true);
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
        link.position().setPixelPosition(0x20, 0x20);
        link.motion().update();

        inputState.tickEdges();
        inputState.onKeyEvent(inputConfig.bKey(), GLFW_PRESS);
        link.motion().update();

        assertTrue(link.motion().isSwimming());
        assertTrue(link.motion().isDiving());
        assertEquals(0x4E, resolvedAnimationState(link));
    }
}
