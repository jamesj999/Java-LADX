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
import linksawakening.state.Direction;
import linksawakening.state.InventoryItem;
import linksawakening.state.LinkMotionState;
import linksawakening.state.PlayerState;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class LinkPitBehaviorTest extends LinkFixture {

    @Test
    void startingPitFallPlaysLinkFallWaveEffectOnce() throws Exception {
        byte[] rom = loadRom();
        RomTables romTables = RomTables.load(linksawakening.rom.Rom.unchecked(rom));
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
        link.position().setPixelPosition(0x20, 0x20);

        runUntilPitFallStarts(link);
        for (int i = 0; i < 8; i++) {
            link.motion().update();
        }

        assertEquals(List.of(GameplaySoundEvent.PIT_FALL), soundSink.events);
    }

    @Test
    void fallingIntoPitUsesRomAnimationStatesAndBlocksMovement() throws Exception {
        byte[] rom = loadRom();
        RomTables romTables = RomTables.load(linksawakening.rom.Rom.unchecked(rom));
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
        link.position().setPixelPosition(0x20, 0x20);

        runUntilPitFallStarts(link);
        int fallX = link.position().pixelX();

        assertTrue(link.airborne().isFallingIntoPit());
        assertEquals(0x55, resolvedAnimationState(link));

        for (int i = 0; i < 16; i++) {
            link.motion().update();
        }
        assertEquals(fallX, link.position().pixelX());
        assertEquals(0x56, resolvedAnimationState(link));

        for (int i = 0; i < 16; i++) {
            link.motion().update();
        }
        assertEquals(0x57, resolvedAnimationState(link));
    }

    @Test
    void pitSlipCentersJavaTopLeftAtPitCellOriginBeforeFallNudge() throws Exception {
        byte[] rom = loadRom();
        RomTables romTables = RomTables.load(linksawakening.rom.Rom.unchecked(rom));
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
        link.position().setPixelPosition(0x20, 0x20);

        for (int i = 0; i < 4; i++) {
            link.motion().update();
        }

        assertTrue(link.airborne().isFallingIntoPit());
        assertEquals(0x20, link.position().pixelX());
        assertEquals(0x23, link.position().pixelY());
    }

    @Test
    void pitFallAnimationTakesPrecedenceOverItemAnimationOverride() throws Exception {
        byte[] rom = loadRom();
        RomTables romTables = RomTables.load(linksawakening.rom.Rom.unchecked(rom));
        InputConfig inputConfig = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        int[] roomObjects = emptyRoomObjectsArea();
        roomObjects[ROOM_OBJECTS_BASE + 2 * ROOM_OBJECT_ROW_STRIDE + 2] = OBJECT_PIT;
        InputState inputState = new InputState();
        OverworldCollision collision = new OverworldCollision(romTables);
        collision.setRoom(roomObjects);
        PlayerState playerState = new PlayerState();
        ItemRegistry itemRegistry = new ItemRegistry();
        itemRegistry.register(playerState.inventory().itemA(), new AnimationOverrideItem(0x22));
        Link link = new LinkTestBuilder()
                .input(inputState)
                .config(inputConfig)
                .tables(romTables)
                .collision(collision)
                .sprites(null)
                .player(playerState)
                .items(itemRegistry)
                .build();
        link.position().setPixelPosition(0x20, 0x20);

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

        assertTrue(link.equipment().canUseItems());

        Sword sword = new Sword(null, null);
        itemRegistry.register(playerState.inventory().itemA(), sword);
        sword.onPress();
        assertFalse(link.equipment().canUseItems());

        HookshotTarget hookshotTarget = new HookshotTarget();
        playerState.inventory().setItemA(InventoryItem.HOOKSHOT.romValue());
        itemRegistry.register(playerState.inventory().itemA(), new Hookshot(hookshotTarget));
        hookshotTarget.active = true;
        assertFalse(link.equipment().canUseItems());

        hookshotTarget.active = false;
        link.actions().setCarryingLiftedObjectState(1, 3);
        assertFalse(link.equipment().canUseItems());

        link.actions().setCarryingLiftedObjectState(0, 3);
        assertTrue(link.equipment().canUseItems());

        link.airborne().useRocsFeather();
        assertTrue(link.airborne().isAirborne());
        assertTrue(link.equipment().canUseItems());

        Link pitLink = pitLink(inputConfig);
        runUntilPitFallStarts(pitLink);
        assertFalse(pitLink.equipment().canUseItems());
    }

    @Test
    void pitWarpArrivalSpinsWithGlobalFramePhaseAndLocksInputUntilLanding() throws Exception {
        RomTables tables = RomTables.load(linksawakening.rom.Rom.of(loadRom()));
        Link link = linkInRoom(new InputConfig(1, 2, 3, 4, 5, 6, 7), tables, emptyRoomObjectsArea());
        link.position().setRoomEntryPixelPosition(64, 64);
        link.linkStateMachine.status().frameCounter(0);
        link.airborne().setPitWarpArrival(0x70);
        int[] directions = {
            Direction.RIGHT.javaIndex(),
            Direction.DOWN.javaIndex(),
            Direction.LEFT.javaIndex(),
            Direction.UP.javaIndex()
        };
        int frame = 0;
        while (link.airborne().isAirborne() && frame < 128) {
            frame++;
            link.motion().update();
            assertEquals(directions[(frame >>> 2) & 3], link.animation().direction(), "frame " + frame);
            assertEquals(64, link.position().pixelX());
            assertEquals(64, link.position().pixelY());
            assertEquals(0, link.position().romSpeedX());
            assertEquals(0, link.position().romSpeedY());
            assertEquals(link.airborne().isAirborne(), link.linkAirborne.spinningPitWarpArrival());
        }
        assertTrue(frame > 16 && frame < 128);
        assertFalse(link.linkAirborne.spinningPitWarpArrival());
        assertEquals(0, link.animation().romEntityZ());
        int finalDirection = link.animation().direction();
        assertEquals(directions[(frame >>> 2) & 3], finalDirection);
        link.linkServices.inputState.onKeyEvent(link.linkServices.inputConfig.rightKey(), GLFW_RELEASE);
        link.motion().update();
        assertEquals(finalDirection, link.animation().direction());
        assertEquals(64, link.position().pixelX());
        link.linkServices.inputState.onKeyEvent(link.linkServices.inputConfig.rightKey(), GLFW_PRESS);
        link.motion().update();
        assertEquals(Direction.RIGHT.javaIndex(), link.animation().direction());
        assertTrue(link.position().pixelX() > 64);
    }

    @Test
    void walkingIntoPitMovesOntoPitThenStartsPitHandling() throws Exception {
        byte[] rom = loadRom();
        RomTables romTables = RomTables.load(linksawakening.rom.Rom.unchecked(rom));
        InputConfig inputConfig = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        int[] roomObjects = emptyRoomObjectsArea();
        roomObjects[ROOM_OBJECTS_BASE + 2 * ROOM_OBJECT_ROW_STRIDE + 3] = OBJECT_PIT;
        Link link = linkInRoom(inputConfig, romTables, roomObjects);
        link.position().setPixelPosition(0x20, 0x20);

        for (int i = 0; i < 20; i++) {
            link.motion().update();
        }

        assertTrue(link.position().pixelX() > 0x24);
        assertTrue(link.position().pixelX() <= 0x30);
        assertFalse(link.airborne().isAirborne());

        runUntilPitFallStarts(link);
        assertTrue(link.airborne().isFallingIntoPit());
    }

    @Test
    void walkingIntoPitWarpDoesNotUseNormalPitFallRecovery() throws Exception {
        byte[] rom = loadRom();
        RomTables romTables = RomTables.load(linksawakening.rom.Rom.unchecked(rom));
        InputConfig inputConfig = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        int[] roomObjects = emptyRoomObjectsArea();
        roomObjects[ROOM_OBJECTS_BASE + 2 * ROOM_OBJECT_ROW_STRIDE + 3] = OBJECT_INDOOR_PIT_WARP;
        PlayerState playerState = new PlayerState();
        playerState.vitals().setHealth(PlayerState.HP_PER_HEART);
        Link link = linkInRoom(inputConfig, romTables, roomObjects, playerState, RomTables.PHYSICS_TABLE_INDOORS1);
        link.position().setPixelPosition(0x20, 0x20);

        for (int i = 0; i < 160; i++) {
            link.motion().update();
        }

        assertTrue(link.position().pixelX() > 0x24);
        assertFalse(link.airborne().isFallingIntoPit());
        assertTrue(link.status().pitWarpPending());
        assertTrue(link.status().recoveryBlocksInput());
        assertEquals(PlayerState.HP_PER_HEART, playerState.vitals().health());
        assertEquals(0, playerState.vitals().invincibilityCounter());
    }

    @Test
    void walkingIntoNonPitSolidRemainsBlocked() throws Exception {
        byte[] rom = loadRom();
        RomTables romTables = RomTables.load(linksawakening.rom.Rom.unchecked(rom));
        InputConfig inputConfig = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        int[] roomObjects = emptyRoomObjectsArea();
        roomObjects[ROOM_OBJECTS_BASE + 2 * ROOM_OBJECT_ROW_STRIDE + 3] = OBJECT_TREE_TOP_LEFT;
        Link link = linkInRoom(inputConfig, romTables, roomObjects);
        link.position().setPixelPosition(0x20, 0x20);

        for (int i = 0; i < 20; i++) {
            link.motion().update();
        }

        assertEquals(0x24, link.position().pixelX());
        assertFalse(link.airborne().isFallingIntoPit());
    }

    @Test
    void matchingDirectionalLedgeStartsAfterTwelveRomCollisionProbes() throws Exception {
        byte[] rom = loadRom();
        RomTables romTables = RomTables.load(linksawakening.rom.Rom.unchecked(rom));
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
        link.position().setPixelPosition(0x20, 0x20);

        assertEquals(0xD3, collision.objectPhysicsFlagAtPoint(0x26, 0x30));
        for (int frame = 0; frame < 5; frame++) {
            link.motion().update();
            assertFalse(link.airborne().isAirborne(), "frame " + frame);
            assertEquals(0x20, link.position().pixelY(), "frame " + frame);
        }

        link.motion().update();

        assertTrue(link.airborne().isAirborne());
        assertEquals(0x1A, link.airborne().zVelocity());
        assertEquals(0x10, link.position().romSpeedY());
        assertEquals(0x20, link.position().pixelY());

        // $02:47CC writes each pose before incrementing its eight-frame
        // timer. The directional ledge jump lasts longer than the flip.
        for (int frame = 1; frame <= 24; frame++) {
            assertTrue(link.airborne().isAirborne(), "frame " + frame);
            assertEquals(0x64 + (frame - 1) / 8, resolvedAnimationState(link), "frame " + frame);
            link.motion().update();
        }
        assertTrue(link.airborne().isAirborne());
        assertEquals(0x01, resolvedAnimationState(link));
        assertTrue(link.position().pixelY() > 0x20);
    }

    @Test
    void directionalLedgeCountdownResetsAfterAProbeInAnotherDirection() throws Exception {
        byte[] rom = loadRom();
        RomTables romTables = RomTables.load(linksawakening.rom.Rom.unchecked(rom));
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
        link.position().setPixelPosition(0x20, 0x20);

        for (int frame = 0; frame < 4; frame++) {
            link.motion().update();
        }
        inputState.onKeyEvent(inputConfig.downKey(), GLFW_RELEASE);
        inputState.onKeyEvent(inputConfig.upKey(), GLFW_PRESS);
        link.motion().update();
        inputState.onKeyEvent(inputConfig.upKey(), GLFW_RELEASE);
        inputState.onKeyEvent(inputConfig.downKey(), GLFW_PRESS);
        link.position().setPixelPosition(0x20, 0x20);
        for (int frame = 0; frame < 5; frame++) {
            link.motion().update();
        }

        assertFalse(link.airborne().isAirborne());
        link.motion().update();
        assertTrue(link.airborne().isAirborne());
    }

    @Test
    void overworldLedgeEntersUnstuckingWithoutDirectionalCountdown() throws Exception {
        byte[] rom = loadRom();
        RomTables romTables = RomTables.load(linksawakening.rom.Rom.unchecked(rom));
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
        link.position().setPixelPosition(0x20, 0x28);

        assertEquals(PhysicsFlags.CAT_LEDGE_OVERWORLD, collision.objectPhysicsFlagAtPoint(0x26, 0x38));
        link.motion().update();

        assertFalse(link.airborne().isAirborne(), "The unstucking handler runs on the next frame");
        assertEquals(0x02, link.airborne().romMotionState());
        assertEquals(0x28, link.position().pixelY());
        assertEquals(List.of(GameplaySoundEvent.LEDGE_FALL), soundSink.events);
        link.motion().update();
        assertTrue(link.airborne().isAirborne());
        assertEquals(0x28 + 21, link.position().pixelY());
        // Unlike LinkMotionDefault, LinkMotionUnstuckingHandler does not
        // call UpdateLinkWalkingAnimation while descending.
        int frame = 1;
        while (link.airborne().romMotionState() == 0x02) {
            assertEquals(0x64 + Math.min(2, (frame - 1) / 8), resolvedAnimationState(link), "frame " + frame);
            assertTrue(frame < 64, "cliff fall must land");
            link.motion().update();
            frame++;
        }
        assertEquals(LinkMotionState.DEFAULT.romValue(), link.airborne().romMotionState());
        assertTrue(link.airborne().isAirborne(), "$02:49B6 writes 1 even on the landing frame");
        assertEquals(0x64, resolvedAnimationState(link));
        link.motion().update();
        assertFalse(link.airborne().isAirborne());
    }

    @Test
    void outdoorHorizontalDirectionalLedgesUseTheRomHalfTileFilters() throws Exception {
        byte[] rom = loadRom();
        RomTables romTables = RomTables.load(linksawakening.rom.Rom.unchecked(rom));
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
        link.position().setPixelPosition(0x24, 0x20);

        assertEquals(0xD0, collision.objectPhysicsFlagAtPoint(0x30, 0x29));
        link.motion().update();

        assertEquals(0x25, link.position().pixelX());
        assertFalse(link.airborne().isAirborne());
    }

    @Test
    void pegasusDashStartsMatchingDirectionalLedgeJumpImmediatelyAndResetsDash() throws Exception {
        byte[] rom = loadRom();
        RomTables romTables = RomTables.load(linksawakening.rom.Rom.unchecked(rom));
        InputConfig inputConfig = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        InputState inputState = new InputState();
        inputState.onKeyEvent(inputConfig.rightKey(), GLFW_PRESS);
        PlayerState playerState = new PlayerState();
        playerState.equipmentLevels().setRunningWithPegasusBoots(true);
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
        link.position().setPixelPosition(0x24, 0x20);

        link.motion().update();

        assertTrue(link.airborne().isAirborne());
        assertFalse(playerState.equipmentLevels().runningWithPegasusBoots());
        assertEquals(0x1A, link.airborne().zVelocity());
        assertEquals(0x10, link.position().romSpeedX());
        assertEquals(null, link.motion().consumePegasusScreenShakeRequest());
    }

    @Test
    void rocsFeatherJumpCanClearPit() throws Exception {
        byte[] rom = loadRom();
        RomTables romTables = RomTables.load(linksawakening.rom.Rom.unchecked(rom));
        InputConfig inputConfig = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        int[] roomObjects = emptyRoomObjectsArea();
        roomObjects[ROOM_OBJECTS_BASE + 2 * ROOM_OBJECT_ROW_STRIDE + 3] = OBJECT_PIT;
        Link link = linkInRoom(inputConfig, romTables, roomObjects);
        link.position().setPixelPosition(0x24, 0x20);

        link.airborne().useRocsFeather();

        assertTrue(link.airborne().isAirborne());
        assertEquals(0x20, link.airborne().zVelocity());

        int guard = 0;
        while (link.airborne().isAirborne() && guard++ < 80) {
            link.motion().update();
        }

        assertFalse(link.airborne().isAirborne());
        assertTrue(link.position().pixelX() > 0x40);
    }

    @Test
    void rocsFeatherDoesNotStartWhileAlreadyOverPit() throws Exception {
        byte[] rom = loadRom();
        RomTables romTables = RomTables.load(linksawakening.rom.Rom.unchecked(rom));
        InputConfig inputConfig = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        int[] roomObjects = emptyRoomObjectsArea();
        roomObjects[ROOM_OBJECTS_BASE + 2 * ROOM_OBJECT_ROW_STRIDE + 2] = OBJECT_PIT;
        Link link = linkInRoom(inputConfig, romTables, roomObjects);
        link.position().setPixelPosition(0x20, 0x20);

        link.motion().update();
        link.airborne().useRocsFeather();

        assertFalse(link.airborne().isAirborne());
        assertEquals(0, link.airborne().zVelocity());
    }

    @Test
    void groundedLinkOverPitSlipsTowardPitCenterEveryFourthFrame() throws Exception {
        byte[] rom = loadRom();
        RomTables romTables = RomTables.load(linksawakening.rom.Rom.unchecked(rom));
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
        link.position().setPixelPosition(0x1C, 0x18);

        link.motion().update();
        assertEquals(0x1C, link.position().pixelX());
        assertEquals(0x18, link.position().pixelY());

        link.motion().update();
        link.motion().update();
        link.motion().update();

        assertEquals(0x1D, link.position().pixelX());
        assertEquals(0x19, link.position().pixelY());
        assertFalse(link.airborne().isFallingIntoPit());
    }

    @Test
    void normalPitFallDamagesHalfHeartAndRecoversToRoomEntryPosition() throws Exception {
        byte[] rom = loadRom();
        RomTables romTables = RomTables.load(linksawakening.rom.Rom.unchecked(rom));
        InputConfig inputConfig = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        int[] roomObjects = emptyRoomObjectsArea();
        roomObjects[ROOM_OBJECTS_BASE + 2 * ROOM_OBJECT_ROW_STRIDE + 2] = OBJECT_PIT;
        PlayerState playerState = new PlayerState();
        playerState.vitals().setHealth(PlayerState.HP_PER_HEART);
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
        link.position().setPixelPosition(0x20, 0x20);
        runUntilPitFallStarts(link);
        runUntilPitFallFinishes(link);

        assertFalse(link.airborne().isFallingIntoPit());
        assertEquals(0x10, link.position().pixelX());
        assertEquals(0x20, link.position().pixelY());
        assertEquals(PlayerState.HP_PER_HEART / 2, playerState.vitals().health());
        assertEquals(0x40, playerState.vitals().invincibilityCounter());
    }

    @Test
    void normalPitFallRecoversToRoomEntryPositionNotLastSafeGroundNearPit() throws Exception {
        byte[] rom = loadRom();
        RomTables romTables = RomTables.load(linksawakening.rom.Rom.unchecked(rom));
        InputConfig inputConfig = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        int[] roomObjects = emptyRoomObjectsArea();
        roomObjects[ROOM_OBJECTS_BASE + 2 * ROOM_OBJECT_ROW_STRIDE + 2] = OBJECT_PIT;
        PlayerState playerState = new PlayerState();
        playerState.vitals().setHealth(PlayerState.HP_PER_HEART);
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
        link.position().setPixelPosition(0x17, 0x20);
        link.motion().update();
        link.position().setPixelPosition(0x20, 0x20);
        runUntilPitFallStarts(link);
        runUntilPitFallFinishes(link);

        assertFalse(link.airborne().isFallingIntoPit());
        assertEquals(0x10, link.position().pixelX());
        assertEquals(0x20, link.position().pixelY());
        assertEquals(PlayerState.HP_PER_HEART / 2, playerState.vitals().health());
        assertEquals(0x40, playerState.vitals().invincibilityCounter());
    }

    @Test
    void pitRecoveryInvincibilityCountsDownOnSubsequentUpdates() throws Exception {
        byte[] rom = loadRom();
        RomTables romTables = RomTables.load(linksawakening.rom.Rom.unchecked(rom));
        InputConfig inputConfig = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        int[] roomObjects = emptyRoomObjectsArea();
        roomObjects[ROOM_OBJECTS_BASE + 2 * ROOM_OBJECT_ROW_STRIDE + 2] = OBJECT_PIT;
        PlayerState playerState = new PlayerState();
        playerState.vitals().setHealth(PlayerState.HP_PER_HEART);
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
        link.position().setPixelPosition(0x20, 0x20);
        runUntilPitFallStarts(link);
        runUntilPitFallFinishes(link);
        assertEquals(0x40, playerState.vitals().invincibilityCounter());

        link.motion().update();

        assertEquals(0x3F, playerState.vitals().invincibilityCounter());
    }

    @Test
    void roomEntryPositionDoesNotUpdateWhileAirborneOrPitHandling() throws Exception {
        byte[] rom = loadRom();
        RomTables romTables = RomTables.load(linksawakening.rom.Rom.unchecked(rom));
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
        link.airborne().useRocsFeather();
        link.position().setPixelPosition(0x18, 0x20);
        link.motion().update();
        while (link.airborne().isAirborne()) {
            link.motion().update();
        }
        link.position().setPixelPosition(0x20, 0x20);
        runUntilPitFallStarts(link);
        runUntilPitFallFinishes(link);

        assertEquals(0x10, link.position().pixelX());
        assertEquals(0x20, link.position().pixelY());
    }

    @Test
    void landingOnPitAfterJumpDoesNotLeaveLinkStandingThere() throws Exception {
        byte[] rom = loadRom();
        RomTables romTables = RomTables.load(linksawakening.rom.Rom.unchecked(rom));
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
        link.position().setPixelPosition(0x24, 0x20);

        link.airborne().useRocsFeather();
        for (int i = 0; i < 12; i++) {
            link.motion().update();
        }
        inputState.onKeyEvent(inputConfig.rightKey(), GLFW_RELEASE);
        assertTrue(link.position().pixelX() >= 0x30);

        int guard = 0;
        while (link.airborne().isAirborne() && guard++ < 80) {
            link.motion().update();
        }

        assertFalse(link.airborne().isAirborne());
        runUntilPitFallStarts(link);
        assertTrue(link.airborne().isFallingIntoPit());
        assertTrue(link.position().pixelX() >= 0x30);
    }
}
