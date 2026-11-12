package linksawakening.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;

import java.util.List;
import linksawakening.equipment.Hookshot;
import linksawakening.equipment.ItemRegistry;
import linksawakening.equipment.Sword;
import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
import linksawakening.physics.OverworldCollision;
import linksawakening.physics.PhysicsFlags;
import linksawakening.rom.RomTables;
import linksawakening.state.PlayerState;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class LinkPitBehaviorTest extends LinkFixture {

    @Test
    void startingPitFallPlaysLinkFallWaveEffectOnce() throws Exception {
        byte[] rom = loadRom();
        RomTables romTables = RomTables.loadFromRom(rom);
        InputConfig inputConfig = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        InputState inputState = new InputState();
        inputState.onKeyEvent(inputConfig.rightKey(), GLFW_PRESS);
        int[] roomObjects = emptyRoomObjectsArea();
        roomObjects[ROOM_OBJECTS_BASE + 2 * ROOM_OBJECT_ROW_STRIDE + 2] = OBJECT_PIT;
        OverworldCollision collision = new OverworldCollision(romTables);
        collision.setRoom(roomObjects);
        RecordingGameplaySoundSink soundSink = new RecordingGameplaySoundSink();
        Link link = new LinkTestBuilder()
                .input(inputState)
                .config(inputConfig)
                .tables(romTables)
                .collision(collision)
                .sprites(null)
                .player(new PlayerState())
                .items(new ItemRegistry())
                .sounds(soundSink)
                .build();
        link.setPixelPosition(0x20, 0x20);

        runUntilPitFallStarts(link);
        for (int i = 0; i < 8; i++) {
            link.update();
        }

        assertEquals(List.of(GameplaySoundEvent.PIT_FALL), soundSink.events);
    }

    @Test
    void fallingIntoPitUsesRomAnimationStatesAndBlocksMovement() throws Exception {
        byte[] rom = loadRom();
        RomTables romTables = RomTables.loadFromRom(rom);
        InputConfig inputConfig = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        InputState inputState = new InputState();
        inputState.onKeyEvent(inputConfig.rightKey(), GLFW_PRESS);
        int[] roomObjects = emptyRoomObjectsArea();
        roomObjects[ROOM_OBJECTS_BASE + 2 * ROOM_OBJECT_ROW_STRIDE + 2] = OBJECT_PIT;
        OverworldCollision collision = new OverworldCollision(romTables);
        collision.setRoom(roomObjects);
        Link link = new LinkTestBuilder()
                .input(inputState)
                .config(inputConfig)
                .tables(romTables)
                .collision(collision)
                .sprites(null)
                .player(new PlayerState())
                .items(new ItemRegistry())
                .build();
        link.setPixelPosition(0x20, 0x20);

        runUntilPitFallStarts(link);
        int fallX = link.pixelX();

        assertTrue(link.isFallingIntoPit());
        assertEquals(0x55, resolvedAnimationState(link));

        for (int i = 0; i < 16; i++) {
            link.update();
        }
        assertEquals(fallX, link.pixelX());
        assertEquals(0x56, resolvedAnimationState(link));

        for (int i = 0; i < 16; i++) {
            link.update();
        }
        assertEquals(0x57, resolvedAnimationState(link));
    }

    @Test
    void pitSlipCentersJavaTopLeftAtPitCellOriginBeforeFallNudge() throws Exception {
        byte[] rom = loadRom();
        RomTables romTables = RomTables.loadFromRom(rom);
        InputConfig inputConfig = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        int[] roomObjects = emptyRoomObjectsArea();
        roomObjects[ROOM_OBJECTS_BASE + 2 * ROOM_OBJECT_ROW_STRIDE + 2] = OBJECT_PIT;
        InputState inputState = new InputState();
        OverworldCollision collision = new OverworldCollision(romTables);
        collision.setRoom(roomObjects);
        Link link = new LinkTestBuilder()
                .input(inputState)
                .config(inputConfig)
                .tables(romTables)
                .collision(collision)
                .sprites(null)
                .player(new PlayerState())
                .items(new ItemRegistry())
                .build();
        link.setPixelPosition(0x20, 0x20);

        for (int i = 0; i < 4; i++) {
            link.update();
        }

        assertTrue(link.isFallingIntoPit());
        assertEquals(0x20, link.pixelX());
        assertEquals(0x23, link.pixelY());
    }

    @Test
    void pitFallAnimationTakesPrecedenceOverItemAnimationOverride() throws Exception {
        byte[] rom = loadRom();
        RomTables romTables = RomTables.loadFromRom(rom);
        InputConfig inputConfig = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        int[] roomObjects = emptyRoomObjectsArea();
        roomObjects[ROOM_OBJECTS_BASE + 2 * ROOM_OBJECT_ROW_STRIDE + 2] = OBJECT_PIT;
        InputState inputState = new InputState();
        OverworldCollision collision = new OverworldCollision(romTables);
        collision.setRoom(roomObjects);
        PlayerState playerState = new PlayerState();
        ItemRegistry itemRegistry = new ItemRegistry();
        itemRegistry.register(playerState.itemA(), new AnimationOverrideItem(0x22));
        Link link = new LinkTestBuilder()
                .input(inputState)
                .config(inputConfig)
                .tables(romTables)
                .collision(collision)
                .sprites(null)
                .player(playerState)
                .items(itemRegistry)
                .build();
        link.setPixelPosition(0x20, 0x20);

        runUntilPitFallStarts(link);

        assertEquals(0x55, resolvedAnimationState(link));
    }

    @Test
    void itemUsePreconditionMatchesEquippedMotionCarryAndPitGates() throws Exception {
        InputState inputState = new InputState();
        InputConfig inputConfig = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        PlayerState playerState = new PlayerState();
        ItemRegistry itemRegistry = new ItemRegistry();
        Link link = new LinkTestBuilder()
                .input(inputState)
                .config(inputConfig)
                .tables(null)
                .collision(null)
                .sprites(null)
                .player(playerState)
                .items(itemRegistry)
                .build();

        assertTrue(link.canUseItems());

        Sword sword = new Sword(null, null);
        itemRegistry.register(playerState.itemA(), sword);
        sword.onPress();
        assertFalse(link.canUseItems());

        HookshotTarget hookshotTarget = new HookshotTarget();
        playerState.setItemA(PlayerState.INVENTORY_HOOKSHOT);
        itemRegistry.register(playerState.itemA(), new Hookshot(hookshotTarget));
        hookshotTarget.active = true;
        assertFalse(link.canUseItems());

        hookshotTarget.active = false;
        link.setCarryingLiftedObjectState(1, 3);
        assertFalse(link.canUseItems());

        link.setCarryingLiftedObjectState(0, 3);
        assertTrue(link.canUseItems());

        link.useRocsFeather();
        assertTrue(link.isAirborne());
        assertTrue(link.canUseItems());

        Link pitLink = pitLink(inputConfig);
        runUntilPitFallStarts(pitLink);
        assertFalse(pitLink.canUseItems());
    }

    @Test
    void pitWarpArrivalSpinsWithGlobalFramePhaseAndLocksInputUntilLanding() throws Exception {
        RomTables tables = RomTables.loadFromRom(loadRom());
        Link link = linkInRoom(new InputConfig(1, 2, 3, 4, 5, 6, 7), tables, emptyRoomObjectsArea());
        link.setRoomEntryPixelPosition(64, 64);
        link.linkStateMachine.frameCounter = 0;
        link.setPitWarpArrival(0x70);
        int[] directions = {Link.DIRECTION_RIGHT, Link.DIRECTION_DOWN, Link.DIRECTION_LEFT, Link.DIRECTION_UP};
        int frame = 0;
        while (link.isAirborne() && frame < 128) {
            frame++;
            link.update();
            assertEquals(directions[(frame >>> 2) & 3], link.direction(), "frame " + frame);
            assertEquals(64, link.pixelX());
            assertEquals(64, link.pixelY());
            assertEquals(0, link.romSpeedX());
            assertEquals(0, link.romSpeedY());
            assertEquals(link.isAirborne(), link.linkAirborne.spinningPitWarpArrival);
        }
        assertTrue(frame > 16 && frame < 128);
        assertFalse(link.linkAirborne.spinningPitWarpArrival);
        assertEquals(0, link.romEntityZ());
        int finalDirection = link.direction();
        assertEquals(directions[(frame >>> 2) & 3], finalDirection);
        link.linkServices.inputState.onKeyEvent(link.linkServices.inputConfig.rightKey(), GLFW_RELEASE);
        link.update();
        assertEquals(finalDirection, link.direction());
        assertEquals(64, link.pixelX());
        link.linkServices.inputState.onKeyEvent(link.linkServices.inputConfig.rightKey(), GLFW_PRESS);
        link.update();
        assertEquals(Link.DIRECTION_RIGHT, link.direction());
        assertTrue(link.pixelX() > 64);
    }

    @Test
    void walkingIntoPitMovesOntoPitThenStartsPitHandling() throws Exception {
        byte[] rom = loadRom();
        RomTables romTables = RomTables.loadFromRom(rom);
        InputConfig inputConfig = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        int[] roomObjects = emptyRoomObjectsArea();
        roomObjects[ROOM_OBJECTS_BASE + 2 * ROOM_OBJECT_ROW_STRIDE + 3] = OBJECT_PIT;
        Link link = linkInRoom(inputConfig, romTables, roomObjects);
        link.setPixelPosition(0x20, 0x20);

        for (int i = 0; i < 20; i++) {
            link.update();
        }

        assertTrue(link.pixelX() > 0x24);
        assertTrue(link.pixelX() <= 0x30);
        assertFalse(link.isAirborne());

        runUntilPitFallStarts(link);
        assertTrue(link.isFallingIntoPit());
    }

    @Test
    void walkingIntoPitWarpDoesNotUseNormalPitFallRecovery() throws Exception {
        byte[] rom = loadRom();
        RomTables romTables = RomTables.loadFromRom(rom);
        InputConfig inputConfig = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        int[] roomObjects = emptyRoomObjectsArea();
        roomObjects[ROOM_OBJECTS_BASE + 2 * ROOM_OBJECT_ROW_STRIDE + 3] = OBJECT_INDOOR_PIT_WARP;
        PlayerState playerState = new PlayerState();
        playerState.setHealth(PlayerState.HP_PER_HEART);
        Link link = linkInRoom(inputConfig, romTables, roomObjects, playerState, RomTables.PHYSICS_TABLE_INDOORS1);
        link.setPixelPosition(0x20, 0x20);

        for (int i = 0; i < 160; i++) {
            link.update();
        }

        assertTrue(link.pixelX() > 0x24);
        assertFalse(link.isFallingIntoPit());
        assertTrue(link.pitWarpPending());
        assertTrue(link.recoveryBlocksInput());
        assertEquals(PlayerState.HP_PER_HEART, playerState.health());
        assertEquals(0, playerState.invincibilityCounter());
    }

    @Test
    void walkingIntoNonPitSolidRemainsBlocked() throws Exception {
        byte[] rom = loadRom();
        RomTables romTables = RomTables.loadFromRom(rom);
        InputConfig inputConfig = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        int[] roomObjects = emptyRoomObjectsArea();
        roomObjects[ROOM_OBJECTS_BASE + 2 * ROOM_OBJECT_ROW_STRIDE + 3] = OBJECT_TREE_TOP_LEFT;
        Link link = linkInRoom(inputConfig, romTables, roomObjects);
        link.setPixelPosition(0x20, 0x20);

        for (int i = 0; i < 20; i++) {
            link.update();
        }

        assertEquals(0x24, link.pixelX());
        assertFalse(link.isFallingIntoPit());
    }

    @Test
    void matchingDirectionalLedgeStartsAfterTwelveRomCollisionProbes() throws Exception {
        byte[] rom = loadRom();
        RomTables romTables = RomTables.loadFromRom(rom);
        InputConfig inputConfig = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        InputState inputState = new InputState();
        inputState.onKeyEvent(inputConfig.downKey(), GLFW_PRESS);
        int[] roomObjects = emptyRoomObjectsArea();
        roomObjects[ROOM_OBJECTS_BASE + 3 * ROOM_OBJECT_ROW_STRIDE + 2] = 0x21;
        OverworldCollision collision = new OverworldCollision(romTables);
        collision.setPhysicsTable(RomTables.PHYSICS_TABLE_INDOORS1);
        collision.setRoom(roomObjects);
        Link link = new LinkTestBuilder()
                .input(inputState)
                .config(inputConfig)
                .tables(romTables)
                .collision(collision)
                .sprites(null)
                .player(new PlayerState())
                .items(new ItemRegistry())
                .build();
        link.setPixelPosition(0x20, 0x20);

        assertEquals(0xD3, collision.objectPhysicsFlagAtPoint(0x26, 0x30));
        for (int frame = 0; frame < 5; frame++) {
            link.update();
            assertFalse(link.isAirborne(), "frame " + frame);
            assertEquals(0x20, link.pixelY(), "frame " + frame);
        }

        link.update();

        assertTrue(link.isAirborne());
        assertEquals(0x1A, link.zVelocity());
        assertEquals(0x10, link.romSpeedY());
        assertEquals(0x20, link.pixelY());

        // $02:47CC writes each pose before incrementing its eight-frame
        // timer. The directional ledge jump lasts longer than the flip.
        for (int frame = 1; frame <= 24; frame++) {
            assertTrue(link.isAirborne(), "frame " + frame);
            assertEquals(0x64 + (frame - 1) / 8, resolvedAnimationState(link), "frame " + frame);
            link.update();
        }
        assertTrue(link.isAirborne());
        assertEquals(0x01, resolvedAnimationState(link));
        assertTrue(link.pixelY() > 0x20);
    }

    @Test
    void directionalLedgeCountdownResetsAfterAProbeInAnotherDirection() throws Exception {
        byte[] rom = loadRom();
        RomTables romTables = RomTables.loadFromRom(rom);
        InputConfig inputConfig = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        InputState inputState = new InputState();
        inputState.onKeyEvent(inputConfig.downKey(), GLFW_PRESS);
        int[] roomObjects = emptyRoomObjectsArea();
        roomObjects[ROOM_OBJECTS_BASE + 3 * ROOM_OBJECT_ROW_STRIDE + 2] = 0x21;
        OverworldCollision collision = new OverworldCollision(romTables);
        collision.setPhysicsTable(RomTables.PHYSICS_TABLE_INDOORS1);
        collision.setRoom(roomObjects);
        Link link = new LinkTestBuilder()
                .input(inputState)
                .config(inputConfig)
                .tables(romTables)
                .collision(collision)
                .sprites(null)
                .player(new PlayerState())
                .items(new ItemRegistry())
                .build();
        link.setPixelPosition(0x20, 0x20);

        for (int frame = 0; frame < 4; frame++) {
            link.update();
        }
        inputState.onKeyEvent(inputConfig.downKey(), GLFW_RELEASE);
        inputState.onKeyEvent(inputConfig.upKey(), GLFW_PRESS);
        link.update();
        inputState.onKeyEvent(inputConfig.upKey(), GLFW_RELEASE);
        inputState.onKeyEvent(inputConfig.downKey(), GLFW_PRESS);
        link.setPixelPosition(0x20, 0x20);
        for (int frame = 0; frame < 5; frame++) {
            link.update();
        }

        assertFalse(link.isAirborne());
        link.update();
        assertTrue(link.isAirborne());
    }

    @Test
    void overworldLedgeEntersUnstuckingWithoutDirectionalCountdown() throws Exception {
        byte[] rom = loadRom();
        RomTables romTables = RomTables.loadFromRom(rom);
        InputConfig inputConfig = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        InputState inputState = new InputState();
        inputState.onKeyEvent(inputConfig.downKey(), GLFW_PRESS);
        int[] roomObjects = emptyRoomObjectsArea();
        roomObjects[ROOM_OBJECTS_BASE + 3 * ROOM_OBJECT_ROW_STRIDE + 2] = 0x43;
        OverworldCollision collision = new OverworldCollision(romTables);
        collision.setRoom(roomObjects);
        RecordingGameplaySoundSink soundSink = new RecordingGameplaySoundSink();
        Link link = new LinkTestBuilder()
                .input(inputState)
                .config(inputConfig)
                .tables(romTables)
                .collision(collision)
                .sprites(null)
                .player(new PlayerState())
                .items(new ItemRegistry())
                .sounds(soundSink)
                .build();
        link.setPixelPosition(0x20, 0x28);

        assertEquals(PhysicsFlags.CAT_LEDGE_OVERWORLD, collision.objectPhysicsFlagAtPoint(0x26, 0x38));
        link.update();

        assertFalse(link.isAirborne(), "The unstucking handler runs on the next frame");
        assertEquals(0x02, link.romMotionState());
        assertEquals(0x28, link.pixelY());
        assertEquals(List.of(GameplaySoundEvent.LEDGE_FALL), soundSink.events);
        link.update();
        assertTrue(link.isAirborne());
        assertEquals(0x28 + 21, link.pixelY());
        // Unlike LinkMotionDefault, LinkMotionUnstuckingHandler does not
        // call UpdateLinkWalkingAnimation while descending.
        int frame = 1;
        while (link.romMotionState() == 0x02) {
            assertEquals(0x64 + Math.min(2, (frame - 1) / 8), resolvedAnimationState(link), "frame " + frame);
            assertTrue(frame < 64, "cliff fall must land");
            link.update();
            frame++;
        }
        assertEquals(Link.LINK_MOTION_DEFAULT, link.romMotionState());
        assertTrue(link.isAirborne(), "$02:49B6 writes 1 even on the landing frame");
        assertEquals(0x64, resolvedAnimationState(link));
        link.update();
        assertFalse(link.isAirborne());
    }

    @Test
    void outdoorHorizontalDirectionalLedgesUseTheRomHalfTileFilters() throws Exception {
        byte[] rom = loadRom();
        RomTables romTables = RomTables.loadFromRom(rom);
        InputConfig inputConfig = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        InputState inputState = new InputState();
        inputState.onKeyEvent(inputConfig.rightKey(), GLFW_PRESS);
        int[] roomObjects = emptyRoomObjectsArea();
        roomObjects[ROOM_OBJECTS_BASE + 2 * ROOM_OBJECT_ROW_STRIDE + 3] = 0xF3;
        OverworldCollision collision = new OverworldCollision(romTables);
        collision.setRoom(roomObjects);
        Link link = new LinkTestBuilder()
                .input(inputState)
                .config(inputConfig)
                .tables(romTables)
                .collision(collision)
                .sprites(null)
                .player(new PlayerState())
                .items(new ItemRegistry())
                .build();
        link.setPixelPosition(0x24, 0x20);

        assertEquals(0xD0, collision.objectPhysicsFlagAtPoint(0x30, 0x29));
        link.update();

        assertEquals(0x25, link.pixelX());
        assertFalse(link.isAirborne());
    }

    @Test
    void pegasusDashStartsMatchingDirectionalLedgeJumpImmediatelyAndResetsDash() throws Exception {
        byte[] rom = loadRom();
        RomTables romTables = RomTables.loadFromRom(rom);
        InputConfig inputConfig = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        InputState inputState = new InputState();
        inputState.onKeyEvent(inputConfig.rightKey(), GLFW_PRESS);
        PlayerState playerState = new PlayerState();
        playerState.setRunningWithPegasusBoots(true);
        int[] roomObjects = emptyRoomObjectsArea();
        roomObjects[ROOM_OBJECTS_BASE + 2 * ROOM_OBJECT_ROW_STRIDE + 3] = 0x23;
        OverworldCollision collision = new OverworldCollision(romTables);
        collision.setPhysicsTable(RomTables.PHYSICS_TABLE_INDOORS1);
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
        link.setPixelPosition(0x24, 0x20);

        link.update();

        assertTrue(link.isAirborne());
        assertFalse(playerState.runningWithPegasusBoots());
        assertEquals(0x1A, link.zVelocity());
        assertEquals(0x10, link.romSpeedX());
        assertEquals(null, link.consumePegasusScreenShakeRequest());
    }

    @Test
    void rocsFeatherJumpCanClearPit() throws Exception {
        byte[] rom = loadRom();
        RomTables romTables = RomTables.loadFromRom(rom);
        InputConfig inputConfig = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        int[] roomObjects = emptyRoomObjectsArea();
        roomObjects[ROOM_OBJECTS_BASE + 2 * ROOM_OBJECT_ROW_STRIDE + 3] = OBJECT_PIT;
        Link link = linkInRoom(inputConfig, romTables, roomObjects);
        link.setPixelPosition(0x24, 0x20);

        link.useRocsFeather();

        assertTrue(link.isAirborne());
        assertEquals(0x20, link.zVelocity());

        int guard = 0;
        while (link.isAirborne() && guard++ < 80) {
            link.update();
        }

        assertFalse(link.isAirborne());
        assertTrue(link.pixelX() > 0x40);
    }

    @Test
    void rocsFeatherDoesNotStartWhileAlreadyOverPit() throws Exception {
        byte[] rom = loadRom();
        RomTables romTables = RomTables.loadFromRom(rom);
        InputConfig inputConfig = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        int[] roomObjects = emptyRoomObjectsArea();
        roomObjects[ROOM_OBJECTS_BASE + 2 * ROOM_OBJECT_ROW_STRIDE + 2] = OBJECT_PIT;
        Link link = linkInRoom(inputConfig, romTables, roomObjects);
        link.setPixelPosition(0x20, 0x20);

        link.update();
        link.useRocsFeather();

        assertFalse(link.isAirborne());
        assertEquals(0, link.zVelocity());
    }

    @Test
    void groundedLinkOverPitSlipsTowardPitCenterEveryFourthFrame() throws Exception {
        byte[] rom = loadRom();
        RomTables romTables = RomTables.loadFromRom(rom);
        InputConfig inputConfig = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        int[] roomObjects = emptyRoomObjectsArea();
        roomObjects[ROOM_OBJECTS_BASE + 2 * ROOM_OBJECT_ROW_STRIDE + 2] = OBJECT_PIT;
        InputState inputState = new InputState();
        OverworldCollision collision = new OverworldCollision(romTables);
        collision.setRoom(roomObjects);
        Link link = new LinkTestBuilder()
                .input(inputState)
                .config(inputConfig)
                .tables(romTables)
                .collision(collision)
                .sprites(null)
                .player(new PlayerState())
                .items(new ItemRegistry())
                .build();
        link.setPixelPosition(0x1C, 0x18);

        link.update();
        assertEquals(0x1C, link.pixelX());
        assertEquals(0x18, link.pixelY());

        link.update();
        link.update();
        link.update();

        assertEquals(0x1D, link.pixelX());
        assertEquals(0x19, link.pixelY());
        assertFalse(link.isFallingIntoPit());
    }

    @Test
    void normalPitFallDamagesHalfHeartAndRecoversToRoomEntryPosition() throws Exception {
        byte[] rom = loadRom();
        RomTables romTables = RomTables.loadFromRom(rom);
        InputConfig inputConfig = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        int[] roomObjects = emptyRoomObjectsArea();
        roomObjects[ROOM_OBJECTS_BASE + 2 * ROOM_OBJECT_ROW_STRIDE + 2] = OBJECT_PIT;
        PlayerState playerState = new PlayerState();
        playerState.setHealth(PlayerState.HP_PER_HEART);
        InputState inputState = new InputState();
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

        placeAtRoomEntry(link, 0x10, 0x20);
        link.setPixelPosition(0x20, 0x20);
        runUntilPitFallStarts(link);
        runUntilPitFallFinishes(link);

        assertFalse(link.isFallingIntoPit());
        assertEquals(0x10, link.pixelX());
        assertEquals(0x20, link.pixelY());
        assertEquals(PlayerState.HP_PER_HEART / 2, playerState.health());
        assertEquals(0x40, playerState.invincibilityCounter());
    }

    @Test
    void normalPitFallRecoversToRoomEntryPositionNotLastSafeGroundNearPit() throws Exception {
        byte[] rom = loadRom();
        RomTables romTables = RomTables.loadFromRom(rom);
        InputConfig inputConfig = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        int[] roomObjects = emptyRoomObjectsArea();
        roomObjects[ROOM_OBJECTS_BASE + 2 * ROOM_OBJECT_ROW_STRIDE + 2] = OBJECT_PIT;
        PlayerState playerState = new PlayerState();
        playerState.setHealth(PlayerState.HP_PER_HEART);
        InputState inputState = new InputState();
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

        placeAtRoomEntry(link, 0x10, 0x20);
        link.setPixelPosition(0x17, 0x20);
        link.update();
        link.setPixelPosition(0x20, 0x20);
        runUntilPitFallStarts(link);
        runUntilPitFallFinishes(link);

        assertFalse(link.isFallingIntoPit());
        assertEquals(0x10, link.pixelX());
        assertEquals(0x20, link.pixelY());
        assertEquals(PlayerState.HP_PER_HEART / 2, playerState.health());
        assertEquals(0x40, playerState.invincibilityCounter());
    }

    @Test
    void pitRecoveryInvincibilityCountsDownOnSubsequentUpdates() throws Exception {
        byte[] rom = loadRom();
        RomTables romTables = RomTables.loadFromRom(rom);
        InputConfig inputConfig = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        int[] roomObjects = emptyRoomObjectsArea();
        roomObjects[ROOM_OBJECTS_BASE + 2 * ROOM_OBJECT_ROW_STRIDE + 2] = OBJECT_PIT;
        PlayerState playerState = new PlayerState();
        playerState.setHealth(PlayerState.HP_PER_HEART);
        InputState inputState = new InputState();
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

        placeAtRoomEntry(link, 0x10, 0x20);
        link.setPixelPosition(0x20, 0x20);
        runUntilPitFallStarts(link);
        runUntilPitFallFinishes(link);
        assertEquals(0x40, playerState.invincibilityCounter());

        link.update();

        assertEquals(0x3F, playerState.invincibilityCounter());
    }

    @Test
    void roomEntryPositionDoesNotUpdateWhileAirborneOrPitHandling() throws Exception {
        byte[] rom = loadRom();
        RomTables romTables = RomTables.loadFromRom(rom);
        InputConfig inputConfig = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        int[] roomObjects = emptyRoomObjectsArea();
        roomObjects[ROOM_OBJECTS_BASE + 2 * ROOM_OBJECT_ROW_STRIDE + 2] = OBJECT_PIT;
        PlayerState playerState = new PlayerState();
        InputState inputState = new InputState();
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

        placeAtRoomEntry(link, 0x10, 0x20);
        link.useRocsFeather();
        link.setPixelPosition(0x18, 0x20);
        link.update();
        while (link.isAirborne()) {
            link.update();
        }
        link.setPixelPosition(0x20, 0x20);
        runUntilPitFallStarts(link);
        runUntilPitFallFinishes(link);

        assertEquals(0x10, link.pixelX());
        assertEquals(0x20, link.pixelY());
    }

    @Test
    void landingOnPitAfterJumpDoesNotLeaveLinkStandingThere() throws Exception {
        byte[] rom = loadRom();
        RomTables romTables = RomTables.loadFromRom(rom);
        InputConfig inputConfig = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        int[] roomObjects = emptyRoomObjectsArea();
        // Releasing direction preserves airborne momentum: land in column 4.
        roomObjects[ROOM_OBJECTS_BASE + 2 * ROOM_OBJECT_ROW_STRIDE + 4] = OBJECT_PIT;
        InputState inputState = new InputState();
        inputState.onKeyEvent(inputConfig.rightKey(), GLFW_PRESS);
        OverworldCollision collision = new OverworldCollision(romTables);
        collision.setRoom(roomObjects);
        Link link = new LinkTestBuilder()
                .input(inputState)
                .config(inputConfig)
                .tables(romTables)
                .collision(collision)
                .sprites(null)
                .player(new PlayerState())
                .items(new ItemRegistry())
                .build();
        link.setPixelPosition(0x24, 0x20);

        link.useRocsFeather();
        for (int i = 0; i < 12; i++) {
            link.update();
        }
        inputState.onKeyEvent(inputConfig.rightKey(), GLFW_RELEASE);
        assertTrue(link.pixelX() >= 0x30);

        int guard = 0;
        while (link.isAirborne() && guard++ < 80) {
            link.update();
        }

        assertFalse(link.isAirborne());
        runUntilPitFallStarts(link);
        assertTrue(link.isFallingIntoPit());
        assertTrue(link.pixelX() >= 0x30);
    }
}
