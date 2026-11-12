package linksawakening.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;

import linksawakening.equipment.ItemRegistry;
import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
import linksawakening.physics.OverworldCollision;
import linksawakening.physics.PhysicsFlags;
import linksawakening.rom.RomTables;
import linksawakening.state.Direction;
import linksawakening.state.LinkMotionState;
import linksawakening.state.PlayerState;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class LinkAdditionalTest extends LinkFixture {

    @Test
    void savedLoadsCanRestoreFacingAndRejectInvalidDirections() {
        Link link = new LinkTestBuilder()
                .input(new InputState())
                .config(new InputConfig(1, 2, 3, 4, 5, 6, 7))
                .tables(null)
                .collision(null)
                .sprites(null)
                .player(new PlayerState())
                .items(new ItemRegistry())
                .build();

        link.animation().setDirection(Direction.UP.javaIndex());

        assertEquals(Direction.UP.javaIndex(), link.animation().direction());
        assertThrows(IllegalArgumentException.class, () -> link.animation().setDirection(4));
    }

    @Test
    void grabbingAndPullingHoldFacingAndPositionUntilReleased() throws Exception {
        InputState input = new InputState();
        InputConfig config = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        RomTables tables = RomTables.loadFromRom(loadRom());
        OverworldCollision collision = new OverworldCollision(tables);
        collision.setRoom(emptyRoomObjectsArea());
        Link link = new LinkTestBuilder()
                .input(input)
                .config(config)
                .tables(tables)
                .collision(collision)
                .sprites(null)
                .player(new PlayerState())
                .items(new ItemRegistry())
                .build();
        link.position().setPixelPosition(64, 64);
        link.animation().setDirection(Direction.UP.javaIndex());
        input.onKeyEvent(config.downKey(), GLFW_PRESS);
        link.actions().setGrabbingObject(true, false);
        link.motion().update();
        assertEquals(Direction.UP.javaIndex(), link.animation().direction());
        assertEquals(64, link.position().pixelY());
        assertEquals(0x3A, resolvedAnimationState(link));
        link.actions().setGrabbingObject(true, true);
        link.motion().update();
        assertEquals(0x3B, resolvedAnimationState(link));
        assertEquals(64, link.position().pixelY());
        link.actions().setGrabbingObject(false, false);
        link.motion().update();
        assertEquals(Direction.DOWN.javaIndex(), link.animation().direction());
    }

    @Test
    void fullyHeldObjectsUseTheRomLiftingAnimationListByDirection() throws Exception {
        Link link = new LinkTestBuilder()
                .input(new InputState())
                .config(new InputConfig(1, 2, 3, 4, 5, 6, 7))
                .tables(null)
                .collision(null)
                .sprites(null)
                .player(new PlayerState())
                .items(new ItemRegistry())
                .build();

        link.actions().setCarryingLiftedObjectState(1, 0);
        assertEquals(0x3E, resolvedAnimationState(link));
        link.actions().setCarryingLiftedObjectState(1, 1);
        assertEquals(0x40, resolvedAnimationState(link));
        link.actions().setCarryingLiftedObjectState(1, 2);
        assertEquals(0x42, resolvedAnimationState(link));
        link.actions().setCarryingLiftedObjectState(1, 3);
        assertEquals(0x44, resolvedAnimationState(link));
    }

    @Test
    void intermediateLiftStatesAreRomAnimationStatesAndBlockMotion() throws Exception {
        InputState inputState = new InputState();
        InputConfig inputConfig = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        Link link = new LinkTestBuilder()
                .input(inputState)
                .config(inputConfig)
                .tables(null)
                .collision(null)
                .sprites(null)
                .player(new PlayerState())
                .items(new ItemRegistry())
                .build();

        link.actions().setCarryingLiftedObjectState(0x37, 3);
        inputState.onKeyEvent(inputConfig.rightKey(), GLFW_PRESS);
        link.motion().update();

        assertEquals(0x37, resolvedAnimationState(link));
        assertEquals(Direction.DOWN.javaIndex(), link.animation().direction());
    }

    @Test
    void clingingGelAllowsMovementOnlyOnEveryFourthWorldFrame() throws Exception {
        InputState input = new InputState();
        InputConfig config = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        Link link = linkWithEmptyRoom(input);
        link.position().setPixelPosition(40, 40);
        input.onKeyEvent(config.rightKey(), GLFW_PRESS);
        for (int frame = 0; frame < 8; frame++) {
            int before = link.position().pixelX();
            link.motion().setGelClinging(true, frame);
            link.motion().update();
            assertEquals(before + ((frame & 3) == 0 ? 1 : 0), link.position().pixelX());
        }
        link.motion().setGelClinging(false, 8);
        link.motion().update();
        assertEquals(43, link.position().pixelX());
    }

    @Test
    void consumesRomResponseSpeedOnTheNextMotionUpdate() throws Exception {
        InputState inputState = new InputState();
        InputConfig inputConfig = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        inputState.onKeyEvent(inputConfig.rightKey(), GLFW_PRESS);
        RomTables romTables = RomTables.loadFromRom(loadRom());
        OverworldCollision collision = new OverworldCollision(romTables);
        collision.setRoom(emptyRoomObjectsArea());
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

        link.position().applyRomSpeed(0xF8, 0x00);
        link.motion().update();

        assertEquals(0x1F, link.position().pixelX());
    }

    @Test
    void blockedMotionKeepsPreviousFacingForDiagonalInput() {
        InputState inputState = new InputState();
        InputConfig inputConfig = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        PlayerState playerState = new PlayerState();
        ItemRegistry itemRegistry = new ItemRegistry();
        itemRegistry.register(playerState.inventory().itemA(), new BlockingItem());
        Link link = new LinkTestBuilder()
                .input(inputState)
                .config(inputConfig)
                .tables(null)
                .collision(null)
                .sprites(null)
                .player(playerState)
                .items(itemRegistry)
                .build();

        inputState.onKeyEvent(inputConfig.leftKey(), GLFW_PRESS);
        link.motion().update();
        assertEquals(Direction.LEFT.javaIndex(), link.animation().direction());

        inputState.onKeyEvent(inputConfig.upKey(), GLFW_PRESS);
        link.motion().update();

        assertEquals(Direction.LEFT.javaIndex(), link.animation().direction());
    }

    @Test
    void tallGrassUsesWorldFramePhaseAfterPausedLinkUpdates() throws Exception {
        RomTables tables = RomTables.loadFromRom(loadRom());
        InputConfig config = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        int[] objects = emptyRoomObjectsArea();
        objects[ROOM_OBJECTS_BASE + 2 * ROOM_OBJECT_ROW_STRIDE + 2] = OBJECT_TALL_GRASS;
        Link link = linkInRoom(config, tables, objects);
        link.position().setPixelPosition(0x20, 0x20);
        // Link's motion has not ticked through the intervening room scroll.
        // The global clock, rather than the number of Link updates, gates grass.
        link.motion().setGelClinging(false, 211);
        link.motion().update();
        assertEquals(0x21, link.position().pixelX());
        link.motion().setGelClinging(false, 212);
        link.motion().update();
        assertEquals(0x21, link.position().pixelX());
        link.motion().setGelClinging(false, 219);
        link.motion().update();
        assertEquals(0x22, link.position().pixelX());
        link.motion().setGelClinging(false, 220);
        link.motion().update();
        assertEquals(0x22, link.position().pixelX());
    }

    @Test
    void tallGrassSlowsLinkMovement() throws Exception {
        byte[] rom = loadRom();
        RomTables romTables = RomTables.loadFromRom(rom);
        InputConfig inputConfig = new InputConfig(1, 2, 3, 4, 5, 6, 7);

        Link normal = linkInRoom(inputConfig, romTables, emptyRoomObjectsArea());
        int[] grassyRoomObjects = emptyRoomObjectsArea();
        grassyRoomObjects[ROOM_OBJECTS_BASE + 2 * ROOM_OBJECT_ROW_STRIDE + 2] = OBJECT_TALL_GRASS;
        Link slowed = linkInRoom(inputConfig, romTables, grassyRoomObjects);

        normal.position().setPixelPosition(0x20, 0x20);
        slowed.position().setPixelPosition(0x20, 0x20);

        for (int i = 0; i < 8; i++) {
            normal.motion().update();
            slowed.motion().update();
        }

        assertEquals(0x28, normal.position().pixelX());
        assertEquals(0x26, slowed.position().pixelX());
    }

    @Test
    void lavaIsPassableAndStartsRecoveryWithoutFlippers() throws Exception {
        RomTables tables = RomTables.loadFromRom(loadRom());
        int lava = -1;
        for (int id = 0; id < 256; id++) {
            if (tables.objectPhysicsFlag(RomTables.PHYSICS_TABLE_INDOORS1, id) == PhysicsFlags.LAVA) {
                lava = id;
                break;
            }
        }
        assertTrue(lava >= 0);
        int[] objects = emptyRoomObjectsArea();
        objects[ROOM_OBJECTS_BASE + 2 * ROOM_OBJECT_ROW_STRIDE + 3] = lava;
        PlayerState player = new PlayerState();
        Link link = linkInRoom(
                new InputConfig(1, 2, 3, 4, 5, 6, 7), tables, objects, player, RomTables.PHYSICS_TABLE_INDOORS1);
        link.position().setRoomEntryPixelPosition(0x20, 0x20);
        for (int i = 0; i < 32 && link.airborne().romMotionState() != LinkMotionState.RECOVER.romValue(); i++)
            link.motion().update();
        assertEquals(LinkMotionState.RECOVER.romValue(), link.airborne().romMotionState());
        assertEquals(lava, link.linkStateMachine.physicsModifier);
        assertEquals(0x50, link.linkStateMachine.recoverCountdown);
        for (int i = 0; i < 80; i++) link.motion().update();
        assertEquals(0x20, link.position().pixelX());
        assertEquals(0x20, link.position().pixelY());
        assertEquals(lava == 6 ? 4 : 0, player.vitals().subtractHealthBuffer());
    }

    @Test
    void recoveryUsesRomCountdownPosesDamageConditionAndMapEntry() throws Exception {
        RomTables tables = RomTables.loadFromRom(loadRom());
        InputConfig config = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        for (int object : new int[] {6, OBJECT_DEEP_WATER}) {
            PlayerState player = new PlayerState();
            Link link = linkInRoom(config, tables, emptyRoomObjectsArea(), player, RomTables.PHYSICS_TABLE_OVERWORLD);
            link.position().setRoomEntryPixelPosition(16, 24);
            link.position().setPixelPosition(64, 64);
            link.linkStateMachine.motionState = LinkMotionState.RECOVER;
            link.linkStateMachine.physicsModifier = object;
            link.linkStateMachine.recoverCountdown = 0x50;
            for (int frame = 1; frame < 80; frame++) {
                link.motion().update();
                int remaining = 80 - frame;
                assertEquals(remaining >= 64 ? 0x4C : remaining >= 48 ? 0x4E : 0xFF, resolvedAnimationState(link));
                assertEquals(LinkMotionState.RECOVER.romValue(), link.airborne().romMotionState());
                assertEquals(64, link.position().pixelX());
            }
            link.motion().update();
            assertEquals(LinkMotionState.DEFAULT.romValue(), link.airborne().romMotionState());
            assertEquals(16, link.position().pixelX());
            assertEquals(24, link.position().pixelY());
            assertEquals(0x40, player.vitals().invincibilityCounter());
            assertEquals(object == 6 ? 4 : 0, player.vitals().subtractHealthBuffer());
        }
    }

    @Test
    void reportsRomCollisionTypeForEachBlockedDirectionAndResetsNextUpdate() throws Exception {
        byte[] rom = loadRom();
        RomTables romTables = RomTables.loadFromRom(rom);
        InputConfig inputConfig = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        CollisionAttempt[] attempts = {
            new CollisionAttempt(Direction.UP.javaIndex(), 0x01, 0x20, 0x40, 3, 2, 0x26, 0x3F),
            new CollisionAttempt(Direction.DOWN.javaIndex(), 0x02, 0x20, 0x20, 3, 2, 0x26, 0x30),
            new CollisionAttempt(Direction.LEFT.javaIndex(), 0x04, 0x40, 0x20, 2, 3, 0x3F, 0x29),
            new CollisionAttempt(Direction.RIGHT.javaIndex(), 0x08, 0x20, 0x20, 2, 3, 0x30, 0x29),
        };

        for (CollisionAttempt attempt : attempts) {
            InputState inputState = new InputState();
            pressDirection(inputState, inputConfig, attempt.direction());
            int[] roomObjects = emptyRoomObjectsArea();
            int blockingCell = ROOM_OBJECTS_BASE + attempt.blockRow() * ROOM_OBJECT_ROW_STRIDE + attempt.blockColumn();
            roomObjects[blockingCell] = 0x00;

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
            link.position().setPixelPosition(attempt.startX(), attempt.startY());

            assertFalse(collision.linkOnNormalPit(attempt.startX(), attempt.startY()));
            assertTrue(collision.pointBlocked(attempt.probeX(), attempt.probeY()));

            int guard = 0;
            while (link.position().romCollisionType() == 0 && guard++ < 16) {
                link.motion().update();
            }

            assertEquals(attempt.sourceBit(), link.position().romCollisionType(), "direction " + attempt.direction());

            roomObjects[blockingCell] = 0xFF;
            link.motion().update();

            assertEquals(
                    0,
                    link.position().romCollisionType(),
                    "collision type should reset for direction " + attempt.direction());
        }
    }

    @Test
    void blocksLinkFromEnteringAMismatchedRaisedSwitchBlock() throws Exception {
        byte[] rom = loadRom();
        RomTables romTables = RomTables.loadFromRom(rom);
        InputConfig inputConfig = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        int[] roomObjects = emptyRoomObjectsArea();
        roomObjects[ROOM_OBJECTS_BASE + 2 * ROOM_OBJECT_ROW_STRIDE + 1] = 0xDC;
        InputState inputState = new InputState();
        inputState.onKeyEvent(inputConfig.rightKey(), GLFW_PRESS);
        OverworldCollision collision = new OverworldCollision(romTables);
        collision.setPhysicsTable(RomTables.PHYSICS_TABLE_INDOORS1);
        collision.setRoom(roomObjects);
        collision.setSwitchBlocksState(0x00);
        Link link = new LinkTestBuilder()
                .input(inputState)
                .config(inputConfig)
                .tables(romTables)
                .collision(collision)
                .sprites(null)
                .player(new PlayerState())
                .items(new ItemRegistry())
                .build();
        link.position().setPixelPosition(0x07, 0x20);

        assertTrue(collision.pointBlocked(0x13, 0x29));
        link.motion().update();

        assertEquals(0x07, link.position().pixelX());
    }

    @Test
    void allowsLinkToEnterAMatchingRaisedSwitchBlock() throws Exception {
        byte[] rom = loadRom();
        RomTables romTables = RomTables.loadFromRom(rom);
        InputConfig inputConfig = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        int[] roomObjects = emptyRoomObjectsArea();
        roomObjects[ROOM_OBJECTS_BASE + 2 * ROOM_OBJECT_ROW_STRIDE + 1] = 0xDC;
        InputState inputState = new InputState();
        inputState.onKeyEvent(inputConfig.rightKey(), GLFW_PRESS);
        OverworldCollision collision = new OverworldCollision(romTables);
        collision.setPhysicsTable(RomTables.PHYSICS_TABLE_INDOORS1);
        collision.setRoom(roomObjects);
        collision.setSwitchBlocksState(0x02);
        Link link = new LinkTestBuilder()
                .input(inputState)
                .config(inputConfig)
                .tables(romTables)
                .collision(collision)
                .sprites(null)
                .player(new PlayerState())
                .items(new ItemRegistry())
                .build();
        link.position().setPixelPosition(0x07, 0x20);

        link.motion().update();

        assertEquals(0x08, link.position().pixelX());
    }

    @Test
    void standingOverrideLetsLinkLeaveAMismatchedSwitchBlock() throws Exception {
        byte[] rom = loadRom();
        RomTables romTables = RomTables.loadFromRom(rom);
        InputConfig inputConfig = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        int[] roomObjects = emptyRoomObjectsArea();
        roomObjects[ROOM_OBJECTS_BASE + 2 * ROOM_OBJECT_ROW_STRIDE] = 0xDC;
        InputState inputState = new InputState();
        inputState.onKeyEvent(inputConfig.rightKey(), GLFW_PRESS);
        OverworldCollision collision = new OverworldCollision(romTables);
        collision.setPhysicsTable(RomTables.PHYSICS_TABLE_INDOORS1);
        collision.setRoom(roomObjects);
        collision.setSwitchBlocksState(0x00);
        Link link = new LinkTestBuilder()
                .input(inputState)
                .config(inputConfig)
                .tables(romTables)
                .collision(collision)
                .sprites(null)
                .player(new PlayerState())
                .items(new ItemRegistry())
                .build();
        link.position().setPixelPosition(0x00, 0x20);

        link.motion().update();

        assertEquals(0x01, link.position().pixelX());
        assertTrue(collision.linkStandingOnSwitchBlock());
    }

    @Test
    void singleProbeCollisionSlidesAlongCornersOnBothAxes() throws Exception {
        RomTables tables = RomTables.loadFromRom(loadRom());
        // direction, start X/Y, expected X/Y. One solid tile at (32,32).
        int[][] cases = {
            {Direction.DOWN.javaIndex(), 25, 16, 24, 16},
            {Direction.DOWN.javaIndex(), 41, 16, 42, 16},
            {Direction.UP.javaIndex(), 25, 42, 24, 42},
            {Direction.UP.javaIndex(), 41, 42, 42, 42},
            {Direction.RIGHT.javaIndex(), 20, 20, 20, 19},
            {Direction.RIGHT.javaIndex(), 20, 38, 20, 39},
            {Direction.LEFT.javaIndex(), 44, 20, 44, 19},
            {Direction.LEFT.javaIndex(), 44, 38, 44, 39},
            {Direction.DOWN.javaIndex(), 32, 16, 32, 16}, // Both probes: no slide.
            {Direction.RIGHT.javaIndex(), 20, 32, 20, 32},
        };
        for (int[] c : cases) {
            InputConfig config = new InputConfig(1, 2, 3, 4, 5, 6, 7);
            InputState input = new InputState();
            int key =
                    switch (Direction.fromJavaOrUnknown(c[0])) {
                        case DOWN -> config.downKey();
                        case UP -> config.upKey();
                        case LEFT -> config.leftKey();
                        default -> config.rightKey();
                    };
            input.onKeyEvent(key, GLFW_PRESS);
            int[] objects = emptyRoomObjectsArea();
            objects[ROOM_OBJECTS_BASE + 2 * ROOM_OBJECT_ROW_STRIDE + 2] = 0x21;
            OverworldCollision collision = new OverworldCollision(tables);
            collision.setRoom(objects);
            Link link = new LinkTestBuilder()
                    .input(input)
                    .config(config)
                    .tables(tables)
                    .collision(collision)
                    .sprites(null)
                    .player(new PlayerState())
                    .items(new ItemRegistry())
                    .build();
            link.position().setPixelPosition(c[1], c[2]);
            link.motion().update();
            assertEquals(c[3], link.position().pixelX(), java.util.Arrays.toString(c));
            assertEquals(c[4], link.position().pixelY(), java.util.Arrays.toString(c));
        }
    }
}
