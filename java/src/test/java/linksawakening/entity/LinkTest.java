package linksawakening.entity;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import linksawakening.equipment.EquippedItem;
import linksawakening.equipment.Hookshot;
import linksawakening.equipment.ItemRegistry;
import linksawakening.equipment.Ocarina;
import linksawakening.equipment.Sword;
import linksawakening.equipment.Shield;
import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.gameplay.GameplaySoundSink;
import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
import linksawakening.physics.OverworldCollision;
import linksawakening.rom.RomTables;
import linksawakening.state.PlayerState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;

final class LinkTest {

    private static final int ROOM_OBJECTS_BASE = 0x11;
    private static final int ROOM_OBJECT_ROW_STRIDE = 0x10;
    private static final int OBJECT_TALL_GRASS = 0x0A;
    private static final int OBJECT_TREE_TOP_LEFT = 0x25;
    private static final int OBJECT_DEEP_WATER = 0x0E;
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
    void ocarinaPlaybackUsesTheRomBodyAnimationStates() throws Exception {
        PlayerState playerState = new PlayerState();
        playerState.setItemA(PlayerState.INVENTORY_OCARINA);
        ItemRegistry itemRegistry = new ItemRegistry();
        OcarinaAnimationTarget target = new OcarinaAnimationTarget();
        itemRegistry.register(playerState.itemA(), new Ocarina(
            playerState, GameplaySoundSink.none(), target));
        Link link = new Link(new InputState(), new InputConfig(1, 2, 3, 4, 5, 6, 7),
            null, null, null, playerState, itemRegistry);

        target.playing = true;
        target.animationState = 0x76;
        assertEquals(0x76, resolvedAnimationState(link));

        target.animationState = 0x75;
        assertEquals(0x75, resolvedAnimationState(link));

        target.playing = false;
        assertEquals(0x00, resolvedAnimationState(link));
    }

    @Test
    void acceptedRocsFeatherJumpPlaysJumpJingleOnce() {
        InputConfig inputConfig = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        RecordingGameplaySoundSink soundSink = new RecordingGameplaySoundSink();
        Link link = new Link(new InputState(), inputConfig, null, null, null,
                new PlayerState(), new ItemRegistry(), soundSink);

        link.useRocsFeather();
        link.useRocsFeather();

        assertEquals(List.of(GameplaySoundEvent.ROC_FEATHER_JUMP), soundSink.events);
    }

    @Test
    void exposesTheRomSpeedBytesWrittenBeforeEntityAnimation() throws IOException {
        InputConfig inputConfig = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        RomTables romTables = RomTables.loadFromRom(loadRom());
        Link link = linkInRoom(inputConfig, romTables, emptyRoomObjectsArea());

        link.applyRomSpeed(0xF0, 0x08);
        link.update();

        assertEquals(0xF0, link.romSpeedX());
        assertEquals(0x08, link.romSpeedY());
    }

    @Test
    void likeLikeCaptureUsesRomEntityCoordinatesAndFreezesLinkUntilRelease() {
        InputConfig inputConfig = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        Link link = new Link(new InputState(), inputConfig, null, null, null,
            new PlayerState(), new ItemRegistry());
        link.setPixelPosition(0x20, 0x20);

        link.applyLikeLikeCapture(0x40, 0x50);
        assertTrue(link.isLikeLikeCaptured());
        assertEquals(0x38, link.pixelX());
        assertEquals(0x40, link.pixelY());
        link.update();
        assertEquals(0x38, link.pixelX());
        assertEquals(0x40, link.pixelY());

        link.releaseLikeLikeCapture();
        assertFalse(link.isLikeLikeCaptured());
    }

    @Test
    void keepsRoomEntryCoordinatesInBothHostAndRomConventions() {
        InputConfig inputConfig = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        Link link = new Link(new InputState(), inputConfig, null, null, null,
            new PlayerState(), new ItemRegistry());

        link.setRoomEntryPixelPosition(0x30, 0x40);
        assertEquals(0x30, link.roomEntryPixelX());
        assertEquals(0x40, link.roomEntryPixelY());
        assertEquals(0x38, link.roomEntryRomPositionX());
        assertEquals(0x50, link.roomEntryRomPositionY());

        link.setRoomEntryRomPosition(0x52, 0x63);
        assertEquals(0x4A, link.roomEntryPixelX());
        assertEquals(0x53, link.roomEntryPixelY());
        assertEquals(0x52, link.roomEntryRomPositionX());
        assertEquals(0x63, link.roomEntryRomPositionY());
    }

    @Test
    void renderUsesTheSelectedRomTunicPaletteAtTheSameBodyPixels() throws Exception {
        byte[] rom = loadRom();
        LinkSpriteSheet spriteSheet = LinkSpriteSheet.loadFromRom(rom);
        LinkTunicPalette tunicPalette = LinkTunicPalette.loadFromRom(rom);
        InputConfig inputConfig = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        InputState inputState = new InputState();
        PlayerState playerState = new PlayerState();
        Link link = new Link(inputState, inputConfig, null, null, spriteSheet,
            playerState, new ItemRegistry(), GameplaySoundSink.none(), tunicPalette);
        link.setPixelPosition(40, 40);

        byte[] green = new byte[linksawakening.gpu.Framebuffer.WIDTH
            * linksawakening.gpu.Framebuffer.HEIGHT * 4];
        link.render(green, 0, 0);

        playerState.setTunicType(PlayerState.TUNIC_RED);
        byte[] red = new byte[green.length];
        link.render(red, 0, 0);

        int changedPixels = 0;
        int greenTunicColor = tunicPalette.forTunic(PlayerState.TUNIC_GREEN)[2];
        int redTunicColor = tunicPalette.forTunic(PlayerState.TUNIC_RED)[2];
        for (int i = 0; i < green.length; i += 4) {
            int greenColor = pixelColor(green, i);
            int redColor = pixelColor(red, i);
            if (greenColor != redColor) {
                changedPixels++;
                assertEquals(greenTunicColor, greenColor);
                assertEquals(redTunicColor, redColor);
            }
        }
        assertTrue(changedPixels > 0, "The Link body should contain tunic-colored pixels");
    }

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
        Link link = new Link(inputState, inputConfig, romTables, collision, null,
                new PlayerState(), new ItemRegistry(), soundSink);
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
    void itemUsePreconditionMatchesEquippedMotionCarryAndPitGates() throws Exception {
        InputState inputState = new InputState();
        InputConfig inputConfig = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        PlayerState playerState = new PlayerState();
        ItemRegistry itemRegistry = new ItemRegistry();
        Link link = new Link(inputState, inputConfig, null, null, null,
            playerState, itemRegistry);

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
    void itemAttackStepCountdownStartsAndDecrementsLikeRom() {
        PlayerState playerState = new PlayerState();
        ItemRegistry itemRegistry = new ItemRegistry();
        itemRegistry.register(playerState.itemA(), new BlockingItem());
        Link link = new Link(new InputState(), new InputConfig(1, 2, 3, 4, 5, 6, 7),
            null, null, null, playerState, itemRegistry);

        assertEquals(0, link.romAttackStepAnimationCountdown());

        link.startRomItemAttackStep();

        assertEquals(0x0C, link.romAttackStepAnimationCountdown());
        link.update();
        assertEquals(0x0B, link.romAttackStepAnimationCountdown());

        for (int i = 0; i < 0x0B; i++) {
            link.update();
        }
        assertEquals(0, link.romAttackStepAnimationCountdown());

        link.update();
        assertEquals(0, link.romAttackStepAnimationCountdown());
    }

    @Test
    void zeroDurationBitSevenAttackStepClearsTheFullRomByte() throws Exception {
        PlayerState playerState = new PlayerState();
        ItemRegistry itemRegistry = new ItemRegistry();
        itemRegistry.register(playerState.itemA(), new BlockingItem());
        Link link = new Link(new InputState(), new InputConfig(1, 2, 3, 4, 5, 6, 7),
            null, null, null, playerState, itemRegistry);
        Field countdown = Link.class.getDeclaredField("romAttackStepAnimationCountdown");
        countdown.setAccessible(true);
        countdown.setInt(link, 0x80);

        assertEquals(0x80, link.romAttackStepAnimationCountdown());

        link.update();

        assertEquals(0, link.romAttackStepAnimationCountdown());
    }

    @Test
    void appliesRomFinalPositionImmediatelyWhileMotionIsBlocked() {
        InputState inputState = new InputState();
        InputConfig inputConfig = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        PlayerState playerState = new PlayerState();
        ItemRegistry itemRegistry = new ItemRegistry();
        itemRegistry.register(playerState.itemA(), new BlockingItem());
        Link link = new Link(inputState, inputConfig, null, null, null,
            playerState, itemRegistry);
        link.setPixelPosition(0x40, 0x50);

        inputState.onKeyEvent(inputConfig.rightKey(), GLFW_PRESS);
        link.applyRomFinalPosition(0x30, 0xE8);

        assertEquals(0x43, link.pixelX());
        assertEquals(0x4E, link.pixelY());

        link.update();

        assertEquals(0x43, link.pixelX());
        assertEquals(0x4E, link.pixelY());
    }

    @Test
    void applyRomFinalPositionBypassesBlockedCollisionAndKeepsFractionalPosition() throws Exception {
        byte[] rom = loadRom();
        RomTables romTables = RomTables.loadFromRom(rom);
        int[] roomObjects = emptyRoomObjectsArea();
        roomObjects[ROOM_OBJECTS_BASE + 5 * ROOM_OBJECT_ROW_STRIDE + 5] = OBJECT_TREE_TOP_LEFT;
        OverworldCollision collision = new OverworldCollision(romTables);
        collision.setRoom(roomObjects);
        InputConfig inputConfig = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        Link link = new Link(new InputState(), inputConfig, romTables, collision, null,
            new PlayerState(), new ItemRegistry());
        link.setPixelPosition(0x4D, 0x50);

        assertTrue(collision.pointBlocked(0x5B, 0x59));

        for (int i = 0; i < 4; i++) {
            link.applyRomFinalPosition(0x0C, 0);
        }

        assertEquals(0x50, link.pixelX());
    }

    @Test
    void savedLoadsCanRestoreFacingAndRejectInvalidDirections() {
        Link link = new Link(new InputState(), new InputConfig(1, 2, 3, 4, 5, 6, 7),
            null, null, null, new PlayerState(), new ItemRegistry());

        link.setDirection(Link.DIRECTION_UP);

        assertEquals(Link.DIRECTION_UP, link.direction());
        assertThrows(IllegalArgumentException.class, () -> link.setDirection(4));
    }

    @Test
    void fullyHeldObjectsUseTheRomLiftingAnimationListByDirection() throws Exception {
        Link link = new Link(new InputState(), new InputConfig(1, 2, 3, 4, 5, 6, 7),
            null, null, null, new PlayerState(), new ItemRegistry());

        link.setCarryingLiftedObjectState(1, 0);
        assertEquals(0x3E, resolvedAnimationState(link));
        link.setCarryingLiftedObjectState(1, 1);
        assertEquals(0x40, resolvedAnimationState(link));
        link.setCarryingLiftedObjectState(1, 2);
        assertEquals(0x42, resolvedAnimationState(link));
        link.setCarryingLiftedObjectState(1, 3);
        assertEquals(0x44, resolvedAnimationState(link));
    }

    @Test
    void intermediateLiftStatesAreRomAnimationStatesAndBlockMotion() throws Exception {
        InputState inputState = new InputState();
        InputConfig inputConfig = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        Link link = new Link(inputState, inputConfig, null, null, null,
            new PlayerState(), new ItemRegistry());

        link.setCarryingLiftedObjectState(0x37, 3);
        inputState.onKeyEvent(inputConfig.rightKey(), GLFW_PRESS);
        link.update();

        assertEquals(0x37, resolvedAnimationState(link));
        assertEquals(Link.DIRECTION_DOWN, link.direction());
    }

    @Test
    void exposesShieldUseOnlyForTheEquippedSlotWhoseButtonIsHeld() {
        InputState inputState = new InputState();
        InputConfig inputConfig = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        PlayerState playerState = new PlayerState();
        playerState.setItemA(PlayerState.INVENTORY_SHIELD);
        Link link = new Link(inputState, inputConfig, null, null, null,
            playerState, new ItemRegistry());

        assertEquals(0, link.romMotionState());
        assertFalse(link.isUsingShield());

        inputState.onKeyEvent(inputConfig.aKey(), GLFW_PRESS);
        assertTrue(link.isUsingShield());

        inputState.onKeyEvent(inputConfig.aKey(), GLFW_RELEASE);
        playerState.setItemA(PlayerState.INVENTORY_SWORD);
        playerState.setItemB(PlayerState.INVENTORY_SHIELD);
        inputState.onKeyEvent(inputConfig.bKey(), GLFW_PRESS);
        assertTrue(link.isUsingShield());

        inputState.onKeyEvent(inputConfig.bKey(), GLFW_RELEASE);
        assertFalse(link.isUsingShield());
    }

    @Test
    void shieldUseSelectsTheRomDefaultAndMirrorBodyAnimationTables() throws Exception {
        InputState inputState = new InputState();
        InputConfig inputConfig = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        PlayerState playerState = new PlayerState();
        playerState.setItemA(PlayerState.INVENTORY_SHIELD);
        ItemRegistry itemRegistry = new ItemRegistry();
        itemRegistry.register(PlayerState.INVENTORY_SHIELD,
            new Shield(GameplaySoundSink.none()));
        Link link = new Link(inputState, inputConfig, null, null, null,
            playerState, itemRegistry);

        inputState.onKeyEvent(inputConfig.aKey(), GLFW_PRESS);
        assertEquals(0x24, resolvedAnimationState(link));

        playerState.setShieldLevel(2);
        assertEquals(0x26, resolvedAnimationState(link));

        link.setDirection(Link.DIRECTION_RIGHT);
        assertEquals(0x2E, resolvedAnimationState(link));
    }

    @Test
    void consumesRomResponseSpeedOnTheNextMotionUpdate() throws Exception {
        InputState inputState = new InputState();
        InputConfig inputConfig = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        inputState.onKeyEvent(inputConfig.rightKey(), GLFW_PRESS);
        RomTables romTables = RomTables.loadFromRom(loadRom());
        OverworldCollision collision = new OverworldCollision(romTables);
        collision.setRoom(emptyRoomObjectsArea());
        Link link = new Link(inputState, inputConfig, romTables, collision, null,
            new PlayerState(), new ItemRegistry());
        link.setPixelPosition(0x20, 0x20);

        link.applyRomSpeed(0xF8, 0x00);
        link.update();

        assertEquals(0x1F, link.pixelX());
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
    void deepWaterBlocksLinkUntilFlippersAreOwned() throws Exception {
        byte[] rom = loadRom();
        RomTables romTables = RomTables.loadFromRom(rom);
        InputConfig inputConfig = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        int[] roomObjects = emptyRoomObjectsArea();
        roomObjects[ROOM_OBJECTS_BASE + 2 * ROOM_OBJECT_ROW_STRIDE + 3] = OBJECT_DEEP_WATER;

        Link withoutFlippers = linkInRoom(inputConfig, romTables, roomObjects);
        withoutFlippers.setPixelPosition(0x20, 0x20);
        for (int i = 0; i < 20; i++) {
            withoutFlippers.update();
        }

        PlayerState playerState = new PlayerState();
        playerState.setHasFlippers(true);
        Link withFlippers = linkInRoom(
            inputConfig, romTables, roomObjects, playerState, RomTables.PHYSICS_TABLE_OVERWORLD);
        withFlippers.setPixelPosition(0x20, 0x20);
        for (int i = 0; i < 20 && !withFlippers.isSwimming(); i++) {
            withFlippers.update();
        }

        assertEquals(0x24, withoutFlippers.pixelX());
        assertTrue(withFlippers.isSwimming());
        assertEquals(0x01, withFlippers.romMotionState());
        assertTrue(withFlippers.pixelX() > withoutFlippers.pixelX());
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
        Link link = linkInRoom(
            inputConfig, romTables, roomObjects, playerState, RomTables.PHYSICS_TABLE_OVERWORLD);
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
                roomObjects[ROOM_OBJECTS_BASE + row * ROOM_OBJECT_ROW_STRIDE + column]
                    = OBJECT_DEEP_WATER;
            }
        }
        OverworldCollision collision = new OverworldCollision(romTables);
        collision.setRoom(roomObjects);
        Link link = new Link(inputState, inputConfig, romTables, collision, null,
            playerState, new ItemRegistry());
        link.setPixelPosition(0x20, 0x20);
        link.update();

        inputState.tickEdges();
        inputState.onKeyEvent(inputConfig.bKey(), GLFW_PRESS);
        link.update();

        assertTrue(link.isSwimming());
        assertTrue(link.isDiving());
        assertEquals(0x4E, resolvedAnimationState(link));
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
    void reportsRomCollisionTypeForEachBlockedDirectionAndResetsNextUpdate() throws Exception {
        byte[] rom = loadRom();
        RomTables romTables = RomTables.loadFromRom(rom);
        InputConfig inputConfig = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        CollisionAttempt[] attempts = {
            new CollisionAttempt(Link.DIRECTION_UP, 0x01, 0x20, 0x40,
                3, 2, 0x26, 0x3F),
            new CollisionAttempt(Link.DIRECTION_DOWN, 0x02, 0x20, 0x20,
                3, 2, 0x26, 0x30),
            new CollisionAttempt(Link.DIRECTION_LEFT, 0x04, 0x40, 0x20,
                2, 3, 0x3F, 0x29),
            new CollisionAttempt(Link.DIRECTION_RIGHT, 0x08, 0x20, 0x20,
                2, 3, 0x30, 0x29),
        };

        for (CollisionAttempt attempt : attempts) {
            InputState inputState = new InputState();
            pressDirection(inputState, inputConfig, attempt.direction());
            int[] roomObjects = emptyRoomObjectsArea();
            int blockingCell = ROOM_OBJECTS_BASE
                + attempt.blockRow() * ROOM_OBJECT_ROW_STRIDE + attempt.blockColumn();
            roomObjects[blockingCell] = 0x00;

            OverworldCollision collision = new OverworldCollision(romTables);
            collision.setRoom(roomObjects);
            Link link = new Link(inputState, inputConfig, romTables, collision, null,
                new PlayerState(), new ItemRegistry());
            link.setPixelPosition(attempt.startX(), attempt.startY());

            assertFalse(collision.linkOnNormalPit(attempt.startX(), attempt.startY()));
            assertTrue(collision.pointBlocked(attempt.probeX(), attempt.probeY()));

            int guard = 0;
            while (link.romCollisionType() == 0 && guard++ < 16) {
                link.update();
            }

            assertEquals(attempt.sourceBit(), link.romCollisionType(),
                "direction " + attempt.direction());

            roomObjects[blockingCell] = 0xFF;
            link.update();

            assertEquals(0, link.romCollisionType(),
                "collision type should reset for direction " + attempt.direction());
        }
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
        Link link = new Link(inputState, inputConfig, romTables, collision, null,
            new PlayerState(), new ItemRegistry());
        link.setPixelPosition(0x07, 0x20);

        assertTrue(collision.pointBlocked(0x13, 0x29));
        link.update();

        assertEquals(0x07, link.pixelX());
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
        Link link = new Link(inputState, inputConfig, romTables, collision, null,
            new PlayerState(), new ItemRegistry());
        link.setPixelPosition(0x07, 0x20);

        link.update();

        assertEquals(0x08, link.pixelX());
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
        Link link = new Link(inputState, inputConfig, romTables, collision, null,
            new PlayerState(), new ItemRegistry());
        link.setPixelPosition(0x00, 0x20);

        link.update();

        assertEquals(0x01, link.pixelX());
        assertTrue(collision.linkStandingOnSwitchBlock());
    }

    private static byte[] loadRom() throws IOException {
        try (InputStream in = LinkTest.class.getResourceAsStream("/rom/azle.gbc")) {
            if (in == null) {
                throw new IOException("Missing ROM resource /rom/azle.gbc");
            }
            return in.readAllBytes();
        }
    }

    private static int pixelColor(byte[] buffer, int offset) {
        return ((buffer[offset] & 0xFF) << 16)
            | ((buffer[offset + 1] & 0xFF) << 8)
            | (buffer[offset + 2] & 0xFF);
    }

    private static Link linkInRoom(InputConfig inputConfig, RomTables romTables, int[] roomObjectsArea) {
        return linkInRoom(inputConfig, romTables, roomObjectsArea, new PlayerState(), RomTables.PHYSICS_TABLE_OVERWORLD);
    }

    private static Link pitLink(InputConfig inputConfig) throws Exception {
        byte[] rom = loadRom();
        RomTables romTables = RomTables.loadFromRom(rom);
        int[] roomObjects = emptyRoomObjectsArea();
        roomObjects[ROOM_OBJECTS_BASE + 2 * ROOM_OBJECT_ROW_STRIDE + 2] = OBJECT_PIT;
        OverworldCollision collision = new OverworldCollision(romTables);
        collision.setRoom(roomObjects);
        Link link = new Link(new InputState(), inputConfig, romTables, collision, null,
            new PlayerState(), new ItemRegistry());
        link.setPixelPosition(0x20, 0x20);
        return link;
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

    private static void pressDirection(InputState inputState, InputConfig inputConfig, int direction) {
        switch (direction) {
            case Link.DIRECTION_DOWN:
                inputState.onKeyEvent(inputConfig.downKey(), GLFW_PRESS);
                break;
            case Link.DIRECTION_UP:
                inputState.onKeyEvent(inputConfig.upKey(), GLFW_PRESS);
                break;
            case Link.DIRECTION_LEFT:
                inputState.onKeyEvent(inputConfig.leftKey(), GLFW_PRESS);
                break;
            case Link.DIRECTION_RIGHT:
                inputState.onKeyEvent(inputConfig.rightKey(), GLFW_PRESS);
                break;
            default:
                throw new IllegalArgumentException("direction " + direction);
        }
    }

    private static void placeAtRoomEntry(Link link, int pixelX, int pixelY) {
        link.setRoomEntryPixelPosition(pixelX, pixelY);
    }

    private static int[] emptyRoomObjectsArea() {
        int[] roomObjectsArea = new int[0x100];
        java.util.Arrays.fill(roomObjectsArea, 0xFF);
        return roomObjectsArea;
    }

    private record CollisionAttempt(int direction, int sourceBit, int startX, int startY,
                                    int blockRow, int blockColumn, int probeX, int probeY) {
    }

    private static final class BlockingItem implements EquippedItem {
        @Override
        public boolean blocksMotion() {
            return true;
        }
    }

    private static final class HookshotTarget implements Hookshot.LaunchTarget {
        private boolean active;

        @Override
        public boolean fireHookshot() {
            return true;
        }

        @Override
        public boolean hookshotActive() {
            return active;
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

    private static final class OcarinaAnimationTarget implements Ocarina.PlaybackTarget {
        private boolean playing;
        private int animationState = -1;

        @Override
        public boolean startOcarina(int countdown, int songFlags, int selectedSong) {
            playing = true;
            return true;
        }

        @Override
        public boolean ocarinaPlaying() {
            return playing;
        }

        @Override
        public int ocarinaAnimationState() {
            return playing ? animationState : -1;
        }
    }

    private static final class RecordingGameplaySoundSink implements GameplaySoundSink {
        private final List<GameplaySoundEvent> events = new ArrayList<>();

        @Override
        public void play(GameplaySoundEvent event) {
            events.add(event);
        }
    }
}
