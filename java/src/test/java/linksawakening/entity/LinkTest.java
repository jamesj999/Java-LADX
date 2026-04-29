package linksawakening.entity;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import linksawakening.equipment.EquippedItem;
import linksawakening.equipment.ItemRegistry;
import linksawakening.equipment.Sword;
import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
import linksawakening.physics.OverworldCollision;
import linksawakening.rom.RomTables;
import linksawakening.state.PlayerState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;

final class LinkTest {

    private static final int ROOM_OBJECTS_BASE = 0x11;
    private static final int ROOM_OBJECT_ROW_STRIDE = 0x10;
    private static final int OBJECT_TALL_GRASS = 0x0A;
    private static final int OBJECT_TREE_TOP_LEFT = 0x25;
    private static final int OBJECT_PIT = 0xE8;
    private static final int OBJECT_INDOOR_PIT_WARP = 0x1C;

    @Test
    void airborneJumpAnimationStartsByDirectionAndAdvancesEveryEightFrames() throws Exception {
        assertJumpAnimation(Link.DIRECTION_RIGHT, 0x61, 0x62, 0x63);
        assertJumpAnimation(Link.DIRECTION_LEFT, 0x5E, 0x5F, 0x60);
        assertJumpAnimation(Link.DIRECTION_UP, 0x67, 0x68, 0x69);
        assertJumpAnimation(Link.DIRECTION_DOWN, 0x64, 0x65, 0x66);
    }

    @Test
    void itemAnimationOverrideTakesPrecedenceOverJumpAnimation() throws Exception {
        InputState inputState = new InputState();
        InputConfig inputConfig = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        PlayerState playerState = new PlayerState();
        ItemRegistry itemRegistry = new ItemRegistry();
        itemRegistry.register(playerState.itemA(), new AnimationOverrideItem(0x22));
        Link link = new Link(inputState, inputConfig, null, null, null, playerState, itemRegistry);

        link.useRocsFeather();

        assertEquals(0x22, resolvedAnimationState(link));
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
        Link link = new Link(inputState, inputConfig, romTables, collision, null, new PlayerState(), new ItemRegistry());
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
        Link link = new Link(inputState, inputConfig, romTables, collision, null, new PlayerState(), new ItemRegistry());
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
        Link link = new Link(inputState, inputConfig, romTables, collision, null, playerState, itemRegistry);
        link.setPixelPosition(0x20, 0x20);

        runUntilPitFallStarts(link);

        assertEquals(0x55, resolvedAnimationState(link));
    }

    @Test
    void blockedMotionStillRefreshesFacingFromHeldSingleDirection() {
        InputState inputState = new InputState();
        InputConfig inputConfig = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        PlayerState playerState = new PlayerState();
        ItemRegistry itemRegistry = new ItemRegistry();
        itemRegistry.register(playerState.itemA(), new BlockingItem());
        Link link = new Link(inputState, inputConfig, null, null, null, playerState, itemRegistry);

        inputState.onKeyEvent(inputConfig.rightKey(), GLFW_PRESS);
        link.update();

        assertEquals(Link.DIRECTION_RIGHT, link.direction());
    }

    @Test
    void blockedMotionKeepsPreviousFacingForDiagonalInput() {
        InputState inputState = new InputState();
        InputConfig inputConfig = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        PlayerState playerState = new PlayerState();
        ItemRegistry itemRegistry = new ItemRegistry();
        itemRegistry.register(playerState.itemA(), new BlockingItem());
        Link link = new Link(inputState, inputConfig, null, null, null, playerState, itemRegistry);

        inputState.onKeyEvent(inputConfig.leftKey(), GLFW_PRESS);
        link.update();
        assertEquals(Link.DIRECTION_LEFT, link.direction());

        inputState.onKeyEvent(inputConfig.upKey(), GLFW_PRESS);
        link.update();

        assertEquals(Link.DIRECTION_LEFT, link.direction());
    }

    @Test
    void blockedMotionStillTicksJumpGravity() {
        InputState inputState = new InputState();
        InputConfig inputConfig = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        PlayerState playerState = new PlayerState();
        ItemRegistry itemRegistry = new ItemRegistry();
        itemRegistry.register(playerState.itemA(), new BlockingItem());
        Link link = new Link(inputState, inputConfig, null, null, null, playerState, itemRegistry);

        link.useRocsFeather();
        link.update();

        assertTrue(link.isAirborne());
        assertEquals(0x1E, link.zVelocity());
    }

    @Test
    void holdingChargedSwordDoesNotBlockMovement() {
        Sword sword = new Sword(null, null);

        sword.onPress();
        while (sword.state() != Sword.STATE_HOLDING) {
            sword.tick(true);
        }

        assertEquals(false, sword.blocksMotion());
    }

    @Test
    void holdingSwordKeepsOriginalFacingDirection() throws Exception {
        InputState inputState = new InputState();
        InputConfig inputConfig = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        PlayerState playerState = new PlayerState();
        ItemRegistry itemRegistry = new ItemRegistry();
        Sword sword = new Sword(null, null);
        itemRegistry.register(playerState.itemA(), sword);
        RomTables romTables = RomTables.loadFromRom(loadRom());
        Link link = new Link(inputState, inputConfig, romTables,
            new OverworldCollision(romTables), null, playerState, itemRegistry);

        inputState.onKeyEvent(inputConfig.rightKey(), GLFW_PRESS);
        link.update();
        assertEquals(Link.DIRECTION_RIGHT, link.direction());

        sword.onPress();
        while (sword.state() != Sword.STATE_HOLDING) {
            sword.tick(true);
        }

        inputState.onKeyEvent(inputConfig.rightKey(), GLFW_RELEASE);
        inputState.onKeyEvent(inputConfig.upKey(), GLFW_PRESS);
        link.update();

        assertEquals(Link.DIRECTION_RIGHT, link.direction());
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

        normal.setPixelPosition(0x20, 0x20);
        slowed.setPixelPosition(0x20, 0x20);

        for (int i = 0; i < 8; i++) {
            normal.update();
            slowed.update();
        }

        assertEquals(0x28, normal.pixelX());
        assertEquals(0x26, slowed.pixelX());
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
        Link link = linkInRoom(
            inputConfig, romTables, roomObjects, playerState, RomTables.PHYSICS_TABLE_INDOORS1);
        link.setPixelPosition(0x20, 0x20);

        for (int i = 0; i < 160; i++) {
            link.update();
        }

        assertEquals(0x24, link.pixelX());
        assertFalse(link.isFallingIntoPit());
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
        Link link = new Link(inputState, inputConfig, romTables, collision, null, new PlayerState(), new ItemRegistry());
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
        Link link = new Link(inputState, inputConfig, romTables, collision, null, playerState, new ItemRegistry());

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
        Link link = new Link(inputState, inputConfig, romTables, collision, null, playerState, new ItemRegistry());

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
        Link link = new Link(inputState, inputConfig, romTables, collision, null, playerState, new ItemRegistry());

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
        Link link = new Link(inputState, inputConfig, romTables, collision, null, playerState, new ItemRegistry());

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
        roomObjects[ROOM_OBJECTS_BASE + 2 * ROOM_OBJECT_ROW_STRIDE + 3] = OBJECT_PIT;
        InputState inputState = new InputState();
        inputState.onKeyEvent(inputConfig.rightKey(), GLFW_PRESS);
        OverworldCollision collision = new OverworldCollision(romTables);
        collision.setRoom(roomObjects);
        Link link = new Link(inputState, inputConfig, romTables, collision, null, new PlayerState(), new ItemRegistry());
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

    @Test
    void rocsFeatherDoesNotStartDuringPitFallStateEvenIfFeetMoveOffPit() throws Exception {
        byte[] rom = loadRom();
        RomTables romTables = RomTables.loadFromRom(rom);
        InputConfig inputConfig = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        int[] roomObjects = emptyRoomObjectsArea();
        roomObjects[ROOM_OBJECTS_BASE + 2 * ROOM_OBJECT_ROW_STRIDE + 3] = OBJECT_PIT;
        InputState inputState = new InputState();
        inputState.onKeyEvent(inputConfig.rightKey(), GLFW_PRESS);
        OverworldCollision collision = new OverworldCollision(romTables);
        collision.setRoom(roomObjects);
        Link link = new Link(inputState, inputConfig, romTables, collision, null, new PlayerState(), new ItemRegistry());
        link.setPixelPosition(0x24, 0x20);

        link.useRocsFeather();
        for (int i = 0; i < 12; i++) {
            link.update();
        }
        inputState.onKeyEvent(inputConfig.rightKey(), GLFW_RELEASE);
        int guard = 0;
        while (link.isAirborne() && guard++ < 80) {
            link.update();
        }
        runUntilPitFallStarts(link);
        assertTrue(link.isFallingIntoPit());

        link.setPixelPosition(0x10, 0x20);
        link.useRocsFeather();

        assertFalse(link.isAirborne());
        assertEquals(0, link.zVelocity());
    }

    private static byte[] loadRom() throws IOException {
        try (InputStream in = LinkTest.class.getResourceAsStream("/rom/azle.gbc")) {
            if (in == null) {
                throw new IOException("Missing ROM resource /rom/azle.gbc");
            }
            return in.readAllBytes();
        }
    }

    private static Link linkInRoom(InputConfig inputConfig, RomTables romTables, int[] roomObjectsArea) {
        return linkInRoom(inputConfig, romTables, roomObjectsArea, new PlayerState(), RomTables.PHYSICS_TABLE_OVERWORLD);
    }

    private static Link linkInRoom(InputConfig inputConfig,
                                   RomTables romTables,
                                   int[] roomObjectsArea,
                                   PlayerState playerState,
                                   int physicsTableIndex) {
        InputState inputState = new InputState();
        inputState.onKeyEvent(inputConfig.rightKey(), GLFW_PRESS);
        OverworldCollision collision = new OverworldCollision(romTables);
        collision.setPhysicsTable(physicsTableIndex);
        collision.setRoom(roomObjectsArea);
        return new Link(inputState, inputConfig, romTables, collision, null, playerState, new ItemRegistry());
    }

    private static void assertJumpAnimation(int direction, int frame0, int frame1, int frame2) throws Exception {
        Link link = linkFacing(direction);
        link.useRocsFeather();

        assertEquals(frame0, resolvedAnimationState(link));
        for (int i = 0; i < 7; i++) {
            link.update();
        }
        assertEquals(frame0, resolvedAnimationState(link));
        link.update();
        assertEquals(frame1, resolvedAnimationState(link));
        for (int i = 0; i < 8; i++) {
            link.update();
        }
        assertEquals(frame2, resolvedAnimationState(link));
        for (int i = 0; i < 16; i++) {
            link.update();
        }
        assertEquals(frame2, resolvedAnimationState(link));
    }

    private static Link linkFacing(int direction) {
        InputState inputState = new InputState();
        InputConfig inputConfig = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        PlayerState playerState = new PlayerState();
        ItemRegistry itemRegistry = new ItemRegistry();
        itemRegistry.register(playerState.itemA(), new BlockingItem());
        switch (direction) {
            case Link.DIRECTION_RIGHT:
                inputState.onKeyEvent(inputConfig.rightKey(), GLFW_PRESS);
                break;
            case Link.DIRECTION_LEFT:
                inputState.onKeyEvent(inputConfig.leftKey(), GLFW_PRESS);
                break;
            case Link.DIRECTION_UP:
                inputState.onKeyEvent(inputConfig.upKey(), GLFW_PRESS);
                break;
            case Link.DIRECTION_DOWN:
                inputState.onKeyEvent(inputConfig.downKey(), GLFW_PRESS);
                break;
            default:
                throw new IllegalArgumentException("direction " + direction);
        }
        Link link = new Link(inputState, inputConfig, null, null, null, playerState, itemRegistry);
        link.update();
        return link;
    }

    private static int resolvedAnimationState(Link link) throws Exception {
        Method method = Link.class.getDeclaredMethod("resolveAnimationState");
        method.setAccessible(true);
        return (Integer) method.invoke(link);
    }

    private static void runUntilPitFallStarts(Link link) {
        int guard = 0;
        while (!link.isFallingIntoPit() && guard++ < 128) {
            link.update();
        }
        assertTrue(link.isFallingIntoPit());
    }

    private static void runUntilPitFallFinishes(Link link) {
        int guard = 0;
        while (link.isFallingIntoPit() && guard++ < 128) {
            link.update();
        }
        assertFalse(link.isFallingIntoPit());
    }

    private static void placeAtRoomEntry(Link link, int pixelX, int pixelY) {
        link.setRoomEntryPixelPosition(pixelX, pixelY);
    }

    private static int[] emptyRoomObjectsArea() {
        int[] roomObjectsArea = new int[0x100];
        java.util.Arrays.fill(roomObjectsArea, 0xFF);
        return roomObjectsArea;
    }

    private static final class BlockingItem implements EquippedItem {
        @Override
        public boolean blocksMotion() {
            return true;
        }
    }

    private static final class AnimationOverrideItem implements EquippedItem {
        private final int animationState;

        private AnimationOverrideItem(int animationState) {
            this.animationState = animationState;
        }

        @Override
        public int overrideAnimationState(int direction, int walkFrame) {
            return animationState;
        }
    }
}
