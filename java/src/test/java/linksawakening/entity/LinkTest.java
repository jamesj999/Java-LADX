package linksawakening.entity;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import linksawakening.equipment.EquipmentItem;
import linksawakening.equipment.Hookshot;
import linksawakening.equipment.ItemRegistry;
import linksawakening.equipment.Ocarina;
import linksawakening.equipment.Sword;
import linksawakening.equipment.Shield;
import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.gameplay.GameplaySoundSink;
import linksawakening.gpu.Framebuffer;
import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
import linksawakening.physics.OverworldCollision;
import linksawakening.physics.PhysicsFlags;
import linksawakening.rom.RomTables;
import linksawakening.state.PlayerState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;

@linksawakening.testing.RequiresRom
final class LinkTest {

    private static final int ROOM_OBJECTS_BASE = 0x11;
    private static final int ROOM_OBJECT_ROW_STRIDE = 0x10;
    private static final int OBJECT_TALL_GRASS = 0x0A;
    private static final int OBJECT_TREE_TOP_LEFT = 0x25;
    private static final int OBJECT_DEEP_WATER = 0x0E;
    private static final int OBJECT_PIT = 0xE8;
    private static final int OBJECT_INDOOR_PIT_WARP = 0x1C;

    @Test
    void revolvingDoorPinsLinkThenRequestsTransitionAfterSeventyTwoFrames() throws Exception {
        RomTables tables = RomTables.loadFromRom(loadRom());
        InputConfig config = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        InputState input = new InputState();
        input.onKeyEvent(config.upKey(), GLFW_PRESS);
        int[] objects = emptyRoomObjectsArea();
        objects[ROOM_OBJECTS_BASE + 4] = 0xB1;
        objects[ROOM_OBJECTS_BASE + 5] = 0xB2;
        OverworldCollision collision = new OverworldCollision(tables);
        collision.setPhysicsTable(RomTables.PHYSICS_TABLE_INDOORS1);
        collision.setRoom(objects);
        PlayerState player = new PlayerState();
        Link link = new Link(input, config, tables, collision, null, player, new ItemRegistry());
        link.setPixelPosition(72, 1);
        for (int tick = 0; tick < 20 && link.romMotionState() != 5; tick++) link.update();
        assertEquals(5, link.romMotionState());
        assertFalse(link.canUseItems());
        input.onKeyEvent(config.upKey(), GLFW_RELEASE);
        input.onKeyEvent(config.rightKey(), GLFW_PRESS);
        for (int frame = 1; frame <= 72; frame++) {
            link.update();
            assertEquals(80, link.romEntityX());
            assertEquals(frame < 56 ? 16 : -5, link.romEntityY());
            if (frame < 72) {
                assertEquals(5, link.romMotionState());
                assertFalse(link.consumeRevolvingDoorTransition());
            }
        }
        assertEquals(0, link.romMotionState());
        assertTrue(link.consumeRevolvingDoorTransition());
        assertFalse(link.consumeRevolvingDoorTransition());
    }

    @Test
    void activeRecoilCannotKnockLinkAcrossANormalRoomBoundary() throws Exception {
        Link link = linkWithEmptyRoom(new InputState());
        link.setPixelPosition(80, 115); // ROM y=$83, just inside the south edge.
        link.applyRomSpeed(0, 0x20);
        link.setCollisionIgnoreFrames(16);
        link.update();
        assertEquals(115, link.pixelY());
        assertEquals(0, link.romSpeedY());
        link.update();
        assertEquals(115, link.pixelY());
    }

    @Test
    void enemyRecoilPreservesSpeedForCountdownAndStillStopsAtWalls() throws Exception {
        InputState input = new InputState();
        Link link = linkWithEmptyRoom(input);
        link.setPixelPosition(40, 40);
        input.onKeyEvent(4, GLFW_PRESS); // Opposing input cannot replace recoil.
        link.applyRomSpeed(0x20, 0);
        link.setCollisionIgnoreFrames(4);
        for (int i = 1; i <= 4; i++) {
            link.update();
            assertEquals(40 + 2 * i, link.pixelX());
        }

        RomTables tables = RomTables.loadFromRom(loadRom());
        int[] room = emptyRoomObjectsArea();
        for (int row = 0; row < 8; row++)
            room[ROOM_OBJECTS_BASE + row * ROOM_OBJECT_ROW_STRIDE + 4] = OBJECT_TREE_TOP_LEFT;
        OverworldCollision collision = new OverworldCollision(tables);
        collision.setRoom(room);
        Link atWall = new Link(new InputState(), new InputConfig(1, 2, 3, 4, 5, 6, 7),
            tables, collision, null, new PlayerState(), new ItemRegistry());
        atWall.setPixelPosition(48, 40);
        atWall.applyRomSpeed(0x20, 0);
        atWall.setCollisionIgnoreFrames(16);
        for (int i = 0; i < 16; i++) atWall.update();
        assertTrue(atWall.pixelX() < 64 - 8, "Recoil must not cross the solid column");
    }

    @Test
    void airborneJumpAnimationStartsByDirectionAndAdvancesEveryEightFrames() throws Exception {
        assertJumpAnimation(Link.DIRECTION_RIGHT, 0x61, 0x62, 0x63);
        assertJumpAnimation(Link.DIRECTION_LEFT, 0x5E, 0x5F, 0x60);
        assertJumpAnimation(Link.DIRECTION_UP, 0x67, 0x68, 0x69);
        assertJumpAnimation(Link.DIRECTION_DOWN, 0x64, 0x65, 0x66);
    }

    @Test
    void swordSwingPausesJumpFlipCounterUntilAfterItsFinalAnimationFrame() throws Exception {
        RomTables tables = RomTables.loadFromRom(loadRom());
        InputConfig config = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        PlayerState player = new PlayerState();
        ItemRegistry registry = new ItemRegistry();
        Sword sword = new Sword(tables, null);
        registry.register(PlayerState.INVENTORY_SWORD, sword);
        player.setItemA(PlayerState.INVENTORY_SWORD);
        Link link = new Link(new InputState(), config, tables, null, null, player, registry);
        link.setDirection(Link.DIRECTION_RIGHT);
        link.useRocsFeather();
        sword.onPress();
        for (int frame = 1; frame <= 24; frame++) {
            sword.tick(false);
            link.update();
            if (frame == 15) assertEquals(0x0B, resolvedAnimationState(link),
                "The final swing frame skips the flip even though the sword becomes NONE");
            if (frame >= 16 && frame <= 23) assertEquals(0x61, resolvedAnimationState(link));
        }
        assertEquals(0x62, resolvedAnimationState(link));
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
    void swordAcquisitionSpinUsesRomDirectionAndAnimationTables() throws Exception {
        byte[] rom = loadRom();
        RomTables romTables = RomTables.loadFromRom(rom);
        InputConfig inputConfig = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        for (int baseRomDirection : new int[] {0, 3}) {
            Link link = new Link(new InputState(), inputConfig, romTables, null, null,
                new PlayerState(), new ItemRegistry());
            link.setDirection(javaDirectionForRomDirection(baseRomDirection));

            int previousAnimationState = resolvedAnimationState(link);
            for (int countdown = 0x20; countdown >= 0; countdown--) {
                link.showSwordAcquisitionSpinPose(countdown);

                int sector = Math.min(7, countdown >> 2);
                int tableIndex = 0x02 * 0x4000 + (0x46C9 - 0x4000)
                    + baseRomDirection * 8 + sector;
                int swordState = Byte.toUnsignedInt(rom[tableIndex]);
                int absoluteDirection = Byte.toUnsignedInt(rom[tableIndex + 0x20]);
                int expectedAnimationState = romTables.swordDirectionAnimState(
                    absoluteDirection, swordState);
                if (expectedAnimationState == 0xFF) {
                    expectedAnimationState = previousAnimationState;
                }

                assertEquals(expectedAnimationState, resolvedAnimationState(link),
                    "base=" + baseRomDirection + ", countdown=" + countdown);
                assertEquals(javaDirectionForRomDirection(absoluteDirection), link.direction(),
                    "base=" + baseRomDirection + ", direction countdown=" + countdown);
                previousAnimationState = expectedAnimationState;
            }
        }
    }

    @Test
    void swordAcquisitionSpinOverridesAQueuedMotionBlockedButtPose() throws Exception {
        RomTables romTables = RomTables.loadFromRom(loadRom());
        InputConfig inputConfig = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        Link link = new Link(new InputState(), inputConfig, romTables, null, null,
            new PlayerState(), new ItemRegistry());
        link.setDirection(Link.DIRECTION_RIGHT);

        link.showSwordAcquisitionSpinPose(0x20);
        int swordState = romTables.swordSpinAnimationState(0, 7);
        int absoluteDirection = romTables.swordSpinAbsoluteDirection(0, 7);
        int expectedAnimationState = romTables.swordDirectionAnimState(
            absoluteDirection, swordState);

        link.blockNextRomMotionFrame();
        link.showSwordAcquisitionSpinPose(0x1F);

        assertEquals(expectedAnimationState, resolvedAnimationState(link));
    }

    @Test
    void swordAcquisitionRendersTheUnslottedSwordBeforeTheRewardIsGranted() throws Exception {
        byte[] rom = loadRom();
        InputConfig inputConfig = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        PlayerState playerState = new PlayerState();
        playerState.initializeNewGame(0, 0, 0);
        ItemRegistry itemRegistry = new ItemRegistry();
        RecordingRenderItem sword = new RecordingRenderItem();
        itemRegistry.register(PlayerState.INVENTORY_SWORD, sword);
        Link link = new Link(new InputState(), inputConfig, RomTables.loadFromRom(rom), null,
            LinkSpriteSheet.loadFromRom(rom), playerState, itemRegistry,
            GameplaySoundSink.none(), LinkTunicPalette.loadFromRom(rom));
        link.setPixelPosition(40, 40);
        link.showSwordAcquisitionSpinPose(0x20);

        link.render(new byte[Framebuffer.WIDTH * Framebuffer.HEIGHT * 4], 0, 0);

        assertEquals(1, sword.renderCalls);
    }

    @Test
    void debugResetClearsScriptedPresentationAndCaptureState() throws Exception {
        Link link = new Link(new InputState(), new InputConfig(1, 2, 3, 4, 5, 6, 7),
            null, null, null, new PlayerState(), new ItemRegistry());

        link.blockNextRomMotionFrame();
        link.showMarinWakeUpBed(4);
        link.applyLikeLikeCapture(0x40, 0x40);
        link.resetTransientStateForDebug();

        assertFalse(link.isMarinWakeUpBedVisible());
        assertFalse(link.isLikeLikeCaptured());
        assertEquals(0, resolvedAnimationState(link));
        assertFalse(link.isAirborne());
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
    void sideViewPotContactRestoresOnlyCapturedFinalX() {
        Link link = new Link(new InputState(), new InputConfig(1, 2, 3, 4, 5, 6, 7),
            null, null, null, new PlayerState(), new ItemRegistry());
        link.setPixelPosition(20, 30);
        link.captureRomFinalPosition();
        link.applyRomFinalPosition(0x10, 0x10);

        link.restoreRomFinalPositionX();

        assertEquals(20, link.pixelX());
        assertEquals(31, link.pixelY());
    }

    @Test
    void sideViewPotTopSnapPublishesStandingAndNextVerticalSpeed() {
        Link link = new Link(new InputState(), new InputConfig(1, 2, 3, 4, 5, 6, 7),
            null, null, null, new PlayerState(), new ItemRegistry());

        link.setPixelPosition(20, 30);
        link.applySideViewPotContact(0x50, 0x02);

        assertEquals(0x40, link.pixelY());
        assertEquals(0x02, link.romSpeedY());
        assertTrue(link.standingOnSideViewEntity());
    }

    @Test
    void linkPushingFlagUsesTheRomThreeFrameCountdown() {
        Link link = new Link(new InputState(), new InputConfig(1, 2, 3, 4, 5, 6, 7),
            null, null, null, new PlayerState(), new ItemRegistry());

        link.markRomLinkPushing(3);

        assertTrue(link.isRomLinkPushing());
        assertFalse(link.canUseItems());
        link.tickRomLinkPushing();
        assertTrue(link.isRomLinkPushing());
        link.tickRomLinkPushing();
        assertTrue(link.isRomLinkPushing());
        link.tickRomLinkPushing();
        assertFalse(link.isRomLinkPushing());
    }

    @Test
    void tarinRewardSwitchesFromHeldItemToStandingShieldDown() throws Exception {
        Link link = new Link(new InputState(), new InputConfig(1, 2, 3, 4, 5, 6, 7),
            null, null, null, new PlayerState(), new ItemRegistry());
        link.showTarinShieldPresentation(new int[] {0, 1, 2, 3});
        assertEquals(0x6C, resolvedAnimationState(link));

        link.showStandingShieldDownPose();

        assertEquals(0x22, resolvedAnimationState(link));
        assertFalse(link.isTarinShieldPresentationVisible());
    }

    @Test
    void holdingPegasusBootsChargesForTheRomThirtyTwoFramesThenStartsTheDash()
        throws IOException {
        InputConfig inputConfig = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        InputState inputState = new InputState();
        inputState.onKeyEvent(inputConfig.bKey(), GLFW_PRESS);
        PlayerState playerState = new PlayerState();
        playerState.setItemB(PlayerState.INVENTORY_PEGASUS_BOOTS);
        RomTables romTables = RomTables.loadFromRom(loadRom());
        OverworldCollision collision = new OverworldCollision(romTables);
        collision.setRoom(emptyRoomObjectsArea());
        Link link = new Link(inputState, inputConfig, romTables, collision, null,
            playerState, new ItemRegistry());

        for (int frame = 0; frame < 0x1F; frame++) {
            link.update();
        }

        assertFalse(playerState.runningWithPegasusBoots());
        assertEquals(0x1F, link.pegasusBootsChargeMeter());

        link.update();

        assertTrue(playerState.runningWithPegasusBoots());
        assertEquals(0x20, link.pegasusBootsChargeMeter());
        assertEquals(0x20, link.romSpeedY());
    }

    @Test
    void pegasusDashCollisionPublishesTheRomTwoFrameDirectionalRecord() throws IOException {
        InputConfig inputConfig = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        InputState inputState = new InputState();
        inputState.onKeyEvent(inputConfig.bKey(), GLFW_PRESS);
        inputState.onKeyEvent(inputConfig.rightKey(), GLFW_PRESS);
        PlayerState playerState = new PlayerState();
        playerState.setItemB(PlayerState.INVENTORY_PEGASUS_BOOTS);
        RomTables romTables = RomTables.loadFromRom(loadRom());
        int[] roomObjects = emptyRoomObjectsArea();
        roomObjects[ROOM_OBJECTS_BASE + 2 * ROOM_OBJECT_ROW_STRIDE + 3] = 0x00;
        OverworldCollision collision = new OverworldCollision(romTables);
        collision.setRoom(roomObjects);
        Link link = new Link(inputState, inputConfig, romTables, collision, null,
            playerState, new ItemRegistry());
        link.setPixelPosition(0x20, 0x20);

        Link.ScreenShakeRequest shake = null;
        int guard = 0;
        while (shake == null && guard++ < 64) {
            link.update();
            shake = link.consumePegasusScreenShakeRequest();
        }

        assertNotNull(shake);
        assertEquals(0x20, shake.countdown());
        assertEquals(0, shake.phase());
        assertEquals(0x02, link.pegasusBootsCollisionCountdown());
        assertEquals((link.romEntityX() + 0x10) & 0xFF,
            link.pegasusBootsCollisionPosX());
        assertEquals((link.romEntityY() + 0x0C) & 0xFF,
            link.pegasusBootsCollisionPosY());
        assertFalse(playerState.runningWithPegasusBoots());
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
    void likeLikeCanHideLinkWithoutReplacingHisPositionOrAirborneMotion() throws IOException {
        Link link = linkWithEmptyRoom(new InputState());
        link.setPixelPosition(0x20, 0x20);
        link.useRocsFeather();
        link.update();
        int beforeZ = link.romEntityZ();
        assertTrue(beforeZ > 0);

        link.applyLikeLikeCapture(0x40, 0x50, false);
        assertTrue(link.isLikeLikeCaptured());
        assertEquals(0x20, link.pixelX());
        assertEquals(0x20, link.pixelY());
        assertEquals(beforeZ, link.romEntityZ());
        // Hidden Link returns before touching the absent sprite sheet/buffer.
        link.render(null, 0, 0);
        link.update();
        assertTrue(link.romEntityZ() > beforeZ);

        link.applyLikeLikeCapture(0x40, 0x50, true);
        assertEquals(0x38, link.pixelX());
        assertEquals(0x40, link.pixelY());
        assertEquals(0, link.romEntityZ());
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
    void renderUsesTheRomFlashPaletteOnInvincibilityBitTwo() throws Exception {
        byte[] rom = loadRom();
        LinkSpriteSheet spriteSheet = LinkSpriteSheet.loadFromRom(rom);
        LinkTunicPalette tunicPalette = LinkTunicPalette.loadFromRom(rom);
        InputConfig inputConfig = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        PlayerState playerState = new PlayerState();
        Link link = new Link(new InputState(), inputConfig, null, null, spriteSheet,
            playerState, new ItemRegistry(), GameplaySoundSink.none(), tunicPalette);
        link.setPixelPosition(40, 40);

        byte[] normal = new byte[linksawakening.gpu.Framebuffer.WIDTH
            * linksawakening.gpu.Framebuffer.HEIGHT * 4];
        link.render(normal, 0, 0);

        playerState.setInvincibilityCounter(0x04);
        byte[] flashing = new byte[normal.length];
        link.render(flashing, 0, 0);

        int changedPixels = 0;
        int[] flashPalette = tunicPalette.forObjectPalette(4);
        for (int i = 0; i < normal.length; i += 4) {
            int normalColor = pixelColor(normal, i);
            int flashingColor = pixelColor(flashing, i);
            if (normalColor != flashingColor) {
                changedPixels++;
                assertTrue(flashingColor == flashPalette[0]
                    || flashingColor == flashPalette[1]
                    || flashingColor == flashPalette[2]
                    || flashingColor == flashPalette[3]);
            }
        }
        assertTrue(changedPixels > 0, "The Link body should flash with object palette 4");
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
    void tarinCanClearTheLiveAttackStepCountdown() {
        Link link = new Link(new InputState(), new InputConfig(1, 2, 3, 4, 5, 6, 7),
            null, null, null, null, null);
        link.startRomMagicPowderAttackStep();

        link.clearRomAttackStepAnimationCountdown();

        assertEquals(0, link.romAttackStepAnimationCountdown());
    }

    @Test
    void tarinFacingPreservesTheCurrentWalkingAnimationPhase() throws Exception {
        Link link = new Link(new InputState(), new InputConfig(1, 2, 3, 4, 5, 6, 7),
            null, null, null, null, null);
        Field walkFrame = Link.class.getDeclaredField("walkFrame");
        walkFrame.setAccessible(true);
        walkFrame.setInt(link, 1);
        Field walkTickCounter = Link.class.getDeclaredField("walkTickCounter");
        walkTickCounter.setAccessible(true);
        walkTickCounter.setInt(link, 3);

        link.faceRomDirectionPreservingWalkPhase(0);

        assertEquals(Link.DIRECTION_RIGHT, link.direction());
        assertEquals(0x0B, resolvedAnimationState(link));
        assertEquals(1, walkFrame.getInt(link));
        assertEquals(3, walkTickCounter.getInt(link));
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
    void restoresTheCapturedRomFinalPositionAfterAnEntityPush() {
        Link link = new Link(new InputState(), new InputConfig(1, 2, 3, 4, 5, 6, 7),
            null, null, null, new PlayerState(), new ItemRegistry());
        link.setPixelPosition(0x40, 0x50);

        link.captureRomFinalPosition();
        link.applyRomFinalPosition(0x10, 0xF0);
        link.restoreRomFinalPosition();

        assertEquals(0x40, link.pixelX());
        assertEquals(0x50, link.pixelY());
    }

    @Test
    void romInteractiveMotionBlockStopsOneFrameWithoutInventingAnAnimationState()
            throws Exception {
        RomTables romTables = RomTables.loadFromRom(loadRom());
        InputState inputState = new InputState();
        InputConfig inputConfig = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        Link link = new Link(inputState, inputConfig, romTables, null, null,
            new PlayerState(), new ItemRegistry());
        link.setPixelPosition(0x40, 0x50);
        inputState.onKeyEvent(inputConfig.rightKey(), GLFW_PRESS);
        link.useRocsFeather();
        assertEquals(0x20, link.zVelocity());
        int priorAnimationState = resolvedAnimationState(link);

        link.blockNextRomMotionFrame();
        link.update();

        assertEquals(0x40, link.pixelX());
        assertEquals(0x50, link.pixelY());
        assertEquals(0, link.zVelocity());
        assertEquals(priorAnimationState, resolvedAnimationState(link));

        inputState.onKeyEvent(inputConfig.rightKey(), GLFW_RELEASE);
        link.update();
        assertEquals(0x00, resolvedAnimationState(link));
    }

    @Test
    void explicitRomFallenPoseUsesAnimationState6A() throws Exception {
        RomTables romTables = RomTables.loadFromRom(loadRom());
        Link link = new Link(new InputState(), new InputConfig(1, 2, 3, 4, 5, 6, 7),
            romTables, null, null, new PlayerState(), new ItemRegistry());
        link.setPixelPosition(0x40, 0x50);

        link.showRomFallenPose();
        link.update();

        assertEquals(0x40, link.pixelX());
        assertEquals(0x50, link.pixelY());
        assertEquals(0x6A, resolvedAnimationState(link));
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
    void sideViewPlatformContactPreservesFractionsWithoutForcingReleasedInput()
        throws Exception {
        InputConfig inputConfig = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        RomTables romTables = RomTables.loadFromRom(loadRom());
        InputState inputState = new InputState();
        inputState.onKeyEvent(inputConfig.rightKey(), GLFW_PRESS);
        OverworldCollision collision = new OverworldCollision(romTables);
        collision.setRoom(emptyRoomObjectsArea());
        Link link = new Link(inputState, inputConfig, romTables, collision, null,
            new PlayerState(), new ItemRegistry());
        link.update();
        int currentRomSpeedX = link.romSpeedX();
        link.setPixelPosition(0x40, 0x20);
        link.useRocsFeather();

        // Establish non-zero low subpixel nibbles through the source-shaped
        // final-position path, then let the contact snap only the ROM bytes.
        link.applyRomFinalPosition(1, 0);
        link.applyRomFinalPosition(0, 0x0F);
        link.applySideViewPlatformContact(1, 0x50, 0x02, true);

        assertEquals(0x41, link.pixelX());
        assertEquals(0x40, link.pixelY());
        assertEquals(currentRomSpeedX, link.romSpeedX());
        assertEquals(0x02, link.romSpeedY());
        assertTrue(link.standingOnSideViewEntity());

        // The preserved X fraction carries when 15 more subpixels arrive.
        link.applyRomFinalPosition(15, 0);
        assertEquals(0x42, link.pixelX());
        assertEquals(0x40, link.pixelY());

        // A platform contact must not queue the old horizontal speed as a
        // forced movement after the player releases the direction.
        inputState.onKeyEvent(inputConfig.rightKey(), GLFW_RELEASE);
        link.update();
        assertEquals(0x41, link.pixelY());
        assertEquals(0x42, link.pixelX());
        assertEquals(0x02, link.romSpeedY());
        assertFalse(link.isAirborne());
        assertEquals(0, link.zVelocity());

        link.clearStandingOnSideViewEntity();
        assertFalse(link.standingOnSideViewEntity());
    }

    @Test
    void sideViewPlatformContactUsesSignedDeltaAndByteWrap() {
        Link link = new Link(new InputState(), new InputConfig(1, 2, 3, 4, 5, 6, 7),
            null, null, null, new PlayerState(), new ItemRegistry());
        link.setPixelPosition(0, 0);

        link.applySideViewPlatformContact(-1, 0x10, 0x02, true);

        assertEquals(0xFF, link.pixelX());
        assertEquals(0x00, link.pixelY());
    }

    @Test
    void sideViewPlatformContactValidatesSignedDeltaAndRomBytes() {
        Link link = new Link(new InputState(), new InputConfig(1, 2, 3, 4, 5, 6, 7),
            null, null, null, new PlayerState(), new ItemRegistry());

        assertThrows(IllegalArgumentException.class,
            () -> link.applySideViewPlatformContact(-0x81, 0x10, 0x02, true));
        assertThrows(IllegalArgumentException.class,
            () -> link.applySideViewPlatformContact(0, 0x100, 0x02, true));
        assertThrows(IllegalArgumentException.class,
            () -> link.applySideViewPlatformContact(0, 0x10, 0x100, true));
    }

    @Test
    void roomEntryClearsSideViewPlatformStateBeforeNextMotion() throws Exception {
        Link link = linkWithEmptyRoom(new InputState());
        link.setPixelPosition(0x40, 0x20);
        link.applySideViewPlatformContact(0, 0x50, 0x02, true);

        link.setRoomEntryPixelPosition(0x20, 0x30);
        assertFalse(link.standingOnSideViewEntity());

        link.update();
        assertEquals(0x30, link.pixelY());
        assertEquals(0, link.romSpeedY());
    }

    @Test
    void blockedMotionConsumesButDoesNotLeakPendingPlatformVerticalSpeed() throws Exception {
        InputConfig inputConfig = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        InputState inputState = new InputState();
        PlayerState playerState = new PlayerState();
        ItemRegistry itemRegistry = new ItemRegistry();
        itemRegistry.register(playerState.itemA(), new BlockingItem());
        RomTables romTables = RomTables.loadFromRom(loadRom());
        OverworldCollision collision = new OverworldCollision(romTables);
        collision.setRoom(emptyRoomObjectsArea());
        Link link = new Link(inputState, inputConfig, romTables, collision, null,
            playerState, itemRegistry);
        link.setPixelPosition(0x40, 0x20);
        link.applySideViewPlatformContact(0, 0x50, 0x02, true);

        link.update();
        playerState.setItemA(PlayerState.INVENTORY_EMPTY);
        link.update();

        assertEquals(0x40, link.pixelX());
        assertEquals(0x40, link.pixelY());
        assertEquals(0, link.romSpeedY());
    }

    @Test
    void clearingRomPositionIncrementCancelsPendingPlatformVerticalSpeed() throws Exception {
        Link link = linkWithEmptyRoom(new InputState());
        link.setPixelPosition(0x40, 0x20);
        link.applyRomFinalPosition(0, 0x0F);
        link.applySideViewPlatformContact(0, 0x50, 0x02, true);

        link.clearRomPositionIncrement();
        link.update();

        assertEquals(0x40, link.pixelY());
        assertEquals(0, link.romSpeedY());
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
    void grabbingAndPullingHoldFacingAndPositionUntilReleased() throws Exception {
        InputState input = new InputState();
        InputConfig config = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        RomTables tables = RomTables.loadFromRom(loadRom());
        OverworldCollision collision = new OverworldCollision(tables);
        collision.setRoom(emptyRoomObjectsArea());
        Link link = new Link(input, config, tables, collision, null, new PlayerState(), new ItemRegistry());
        link.setPixelPosition(64, 64);
        link.setDirection(Link.DIRECTION_UP);
        input.onKeyEvent(config.downKey(), GLFW_PRESS);
        link.setGrabbingObject(true, false);
        link.update();
        assertEquals(Link.DIRECTION_UP, link.direction());
        assertEquals(64, link.pixelY());
        assertEquals(0x3A, resolvedAnimationState(link));
        link.setGrabbingObject(true, true);
        link.update();
        assertEquals(0x3B, resolvedAnimationState(link));
        assertEquals(64, link.pixelY());
        link.setGrabbingObject(false, false);
        link.update();
        assertEquals(Link.DIRECTION_DOWN, link.direction());
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
    void dialogueBlocksShieldUseAndRaisedPoseUntilGameplayResumes() throws Exception {
        InputState input = new InputState();
        InputConfig config = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        PlayerState player = new PlayerState();
        player.setShieldLevel(1);
        player.setItemB(PlayerState.INVENTORY_SHIELD);
        Link link = new Link(input, config, null, null, LinkSpriteSheet.loadFromRom(loadRom()),
            player, new ItemRegistry());
        java.util.concurrent.atomic.AtomicBoolean dialogue = new java.util.concurrent.atomic.AtomicBoolean(true);
        link.setShieldInputAllowed(() -> !dialogue.get());
        int carryingPose = resolvedAnimationState(link);
        input.onKeyEvent(config.bKey(), GLFW_PRESS);
        assertFalse(link.isUsingShield());
        assertEquals(carryingPose, resolvedAnimationState(link));
        dialogue.set(false);
        assertTrue(link.isUsingShield());
        assertTrue(carryingPose != resolvedAnimationState(link));
    }

    @Test
    void clingingGelAllowsMovementOnlyOnEveryFourthWorldFrame() throws Exception {
        InputState input = new InputState();
        InputConfig config = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        Link link = linkWithEmptyRoom(input);
        link.setPixelPosition(40, 40);
        input.onKeyEvent(config.rightKey(), GLFW_PRESS);
        for (int frame = 0; frame < 8; frame++) {
            int before = link.pixelX();
            link.setGelClinging(true, frame);
            link.update();
            assertEquals(before + ((frame & 3) == 0 ? 1 : 0), link.pixelX());
        }
        link.setGelClinging(false, 8);
        link.update();
        assertEquals(43, link.pixelX());
    }

    @Test
    void equippedShieldSelectsRomCarryingPoseWithoutAButtonPress() throws Exception {
        byte[] rom = loadRom();
        InputState input = new InputState();
        InputConfig config = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        PlayerState player = new PlayerState();
        player.setItemA(PlayerState.INVENTORY_SWORD);
        player.setItemB(PlayerState.INVENTORY_SHIELD);
        Link link = new Link(input, config, null, null, LinkSpriteSheet.loadFromRom(rom),
            player, new ItemRegistry());
        for (int level : new int[]{1, 2}) {
            player.setShieldLevel(level);
            for (int direction = 0; direction < 4; direction++) {
                link.setRomDirection(direction);
                int table = level == 1 ? 0x4918 : 0x4928;
                int offset = 2 * 0x4000 + table - 0x4000 + direction * 2;
                assertEquals(rom[offset] & 0xFF, resolvedAnimationState(link));
                input.onKeyEvent(config.bKey(), GLFW_PRESS);
                assertEquals(rom[offset + 8] & 0xFF, resolvedAnimationState(link));
                input.onKeyEvent(config.bKey(), GLFW_RELEASE);
            }
        }
        player.setItemB(PlayerState.INVENTORY_MAGIC_POWDER);
        link.setRomDirection(2);
        assertEquals(rom[2 * 0x4000 + 0x0914] & 0xFF, resolvedAnimationState(link));
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
    void blockedMotionStillTicksJumpGravity() throws Exception {
        InputState inputState = new InputState();
        InputConfig inputConfig = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        PlayerState playerState = new PlayerState();
        ItemRegistry itemRegistry = new ItemRegistry();
        itemRegistry.register(playerState.itemA(), new BlockingItem());
        Link link = new Link(inputState, inputConfig, RomTables.loadFromRom(loadRom()), null, null, playerState, itemRegistry);

        link.useRocsFeather();
        link.update();

        assertTrue(link.isAirborne());
        assertEquals(0x1E, link.zVelocity());
    }

    @Test
    void swordSwingBlocksWalkingButDoesNotStopAFeatherJump() throws Exception {
        InputState input = new InputState();
        InputConfig config = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        PlayerState player = new PlayerState();
        ItemRegistry registry = new ItemRegistry();
        Sword sword = new Sword(null, null);
        registry.register(player.itemA(), sword);
        RomTables tables = RomTables.loadFromRom(loadRom());
        OverworldCollision collision = new OverworldCollision(tables);
        collision.setRoom(emptyRoomObjectsArea());
        Link link = new Link(input, config, tables, collision, null, player, registry);
        link.setPixelPosition(40, 40);
        input.onKeyEvent(config.rightKey(), GLFW_PRESS);
        sword.onPress();
        sword.tick(true);
        link.update();
        assertEquals(41, link.pixelX(), "Launch-frame XY precedes sword animation");
        sword.tick(true);
        link.update();
        assertEquals(41, link.pixelX());
        link.useRocsFeather();
        link.update();
        assertEquals(42, link.pixelX());
        assertTrue(link.isAirborne());
        assertTrue(sword.blocksMotion());
        link.update();
        assertEquals(43, link.pixelX());
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
    void tallGrassUsesWorldFramePhaseAfterPausedLinkUpdates() throws Exception {
        RomTables tables = RomTables.loadFromRom(loadRom());
        InputConfig config = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        int[] objects = emptyRoomObjectsArea();
        objects[ROOM_OBJECTS_BASE + 2 * ROOM_OBJECT_ROW_STRIDE + 2] = OBJECT_TALL_GRASS;
        Link link = linkInRoom(config, tables, objects);
        link.setPixelPosition(0x20, 0x20);
        // Link's motion has not ticked through the intervening room scroll.
        // The global clock, rather than the number of Link updates, gates grass.
        link.setGelClinging(false, 211);
        link.update();
        assertEquals(0x21, link.pixelX());
        link.setGelClinging(false, 212);
        link.update();
        assertEquals(0x21, link.pixelX());
        link.setGelClinging(false, 219);
        link.update();
        assertEquals(0x22, link.pixelX());
        link.setGelClinging(false, 220);
        link.update();
        assertEquals(0x22, link.pixelX());
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
        Link withFlippers = linkInRoom(
            inputConfig, romTables, roomObjects, playerState, RomTables.PHYSICS_TABLE_OVERWORLD);
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
        assertEquals(0x40, withoutFlippers.playerState.invincibilityCounter());

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
        link.inputState.onKeyEvent(link.inputConfig.rightKey(), GLFW_RELEASE);
        link.update();
        assertEquals(finalDirection, link.direction());
        assertEquals(64, link.pixelX());
        link.inputState.onKeyEvent(link.inputConfig.rightKey(), GLFW_PRESS);
        link.update();
        assertEquals(Link.DIRECTION_RIGHT, link.direction());
        assertTrue(link.pixelX() > 64);
    }

    @Test
    void waterEntryQueuesRomPositionSplashForBothBranchesAndGatesRecovery() throws Exception {
        RomTables tables = RomTables.loadFromRom(loadRom());
        for (boolean flippers : new boolean[] {false, true}) {
            PlayerState player = new PlayerState();
            player.setHasFlippers(flippers);
            int[] objects = emptyRoomObjectsArea();
            objects[ROOM_OBJECTS_BASE + 2 * ROOM_OBJECT_ROW_STRIDE + 2] = OBJECT_DEEP_WATER;
            Link link = linkInRoom(new InputConfig(1, 2, 3, 4, 5, 6, 7), tables, objects, player, RomTables.PHYSICS_TABLE_OVERWORLD);
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
    void lavaIsPassableAndStartsRecoveryWithoutFlippers() throws Exception {
        RomTables tables = RomTables.loadFromRom(loadRom());
        int lava = -1;
        for (int id = 0; id < 256; id++) {
            if (tables.objectPhysicsFlag(RomTables.PHYSICS_TABLE_INDOORS1, id) == PhysicsFlags.LAVA) { lava = id; break; }
        }
        assertTrue(lava >= 0);
        int[] objects = emptyRoomObjectsArea();
        objects[ROOM_OBJECTS_BASE + 2 * ROOM_OBJECT_ROW_STRIDE + 3] = lava;
        PlayerState player = new PlayerState();
        Link link = linkInRoom(new InputConfig(1, 2, 3, 4, 5, 6, 7), tables, objects, player, RomTables.PHYSICS_TABLE_INDOORS1);
        link.setRoomEntryPixelPosition(0x20, 0x20);
        for (int i = 0; i < 32 && link.romMotionState() != Link.LINK_MOTION_RECOVER; i++) link.update();
        assertEquals(Link.LINK_MOTION_RECOVER, link.romMotionState());
        assertEquals(lava, link.linkStateMachine.physicsModifier);
        assertEquals(0x50, link.linkStateMachine.recoverCountdown);
        for (int i = 0; i < 80; i++) link.update();
        assertEquals(0x20, link.pixelX());
        assertEquals(0x20, link.pixelY());
        assertEquals(lava == 6 ? 4 : 0, player.subtractHealthBuffer());
    }

    @Test
    void recoveryUsesRomCountdownPosesDamageConditionAndMapEntry() throws Exception {
        RomTables tables = RomTables.loadFromRom(loadRom());
        InputConfig config = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        for (int object : new int[] {6, OBJECT_DEEP_WATER}) {
            PlayerState player = new PlayerState();
            Link link = linkInRoom(config, tables, emptyRoomObjectsArea(), player, RomTables.PHYSICS_TABLE_OVERWORLD);
            link.setRoomEntryPixelPosition(16, 24);
            link.setPixelPosition(64, 64);
            link.linkStateMachine.motionState = Link.LINK_MOTION_RECOVER;
            link.linkStateMachine.physicsModifier = object;
            link.linkStateMachine.recoverCountdown = 0x50;
            for (int frame = 1; frame < 80; frame++) {
                link.update();
                int remaining = 80 - frame;
                assertEquals(remaining >= 64 ? 0x4C : remaining >= 48 ? 0x4E : 0xFF, resolvedAnimationState(link));
                assertEquals(Link.LINK_MOTION_RECOVER, link.romMotionState());
                assertEquals(64, link.pixelX());
            }
            link.update();
            assertEquals(Link.LINK_MOTION_DEFAULT, link.romMotionState());
            assertEquals(16, link.pixelX());
            assertEquals(24, link.pixelY());
            assertEquals(0x40, player.invincibilityCounter());
            assertEquals(object == 6 ? 4 : 0, player.subtractHealthBuffer());
        }
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
        Link link = new Link(inputState, inputConfig, romTables, collision, null,
            new PlayerState(), new ItemRegistry());
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
            assertEquals(0x64 + (frame - 1) / 8, resolvedAnimationState(link),
                "frame " + frame);
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
        Link link = new Link(inputState, inputConfig, romTables, collision, null,
            new PlayerState(), new ItemRegistry());
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
    void overworldLedgeUsesImmediateUnstuckingFallInsteadOfDirectionalCountdown()
            throws Exception {
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
        Link link = new Link(inputState, inputConfig, romTables, collision, null,
            new PlayerState(), new ItemRegistry(), soundSink);
        link.setPixelPosition(0x20, 0x28);

        assertEquals(PhysicsFlags.CAT_LEDGE_OVERWORLD,
            collision.objectPhysicsFlagAtPoint(0x26, 0x38));
        link.update();

        assertTrue(link.isAirborne());
        assertEquals(0x02, link.romMotionState());
        assertTrue(link.pixelY() > 0x28);
        assertEquals(List.of(GameplaySoundEvent.LEDGE_FALL), soundSink.events);
        // Unlike LinkMotionDefault, LinkMotionUnstuckingHandler does not
        // call UpdateLinkWalkingAnimation while descending.
        int frame = 1;
        while (link.isAirborne()) {
            assertEquals(0x64 + Math.min(2, (frame - 1) / 8),
                resolvedAnimationState(link), "frame " + frame);
            assertTrue(frame < 64, "cliff fall must land");
            link.update();
            frame++;
        }
        assertEquals(Link.LINK_MOTION_DEFAULT, link.romMotionState());
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
        Link link = new Link(inputState, inputConfig, romTables, collision, null,
            new PlayerState(), new ItemRegistry());
        link.setPixelPosition(0x24, 0x20);

        assertEquals(0xD0, collision.objectPhysicsFlagAtPoint(0x30, 0x29));
        link.update();

        assertEquals(0x25, link.pixelX());
        assertFalse(link.isAirborne());
    }

    @Test
    void pegasusDashStartsMatchingDirectionalLedgeJumpImmediatelyAndResetsDash()
            throws Exception {
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
        Link link = new Link(inputState, inputConfig, romTables, collision, null,
            playerState, new ItemRegistry());
        link.setPixelPosition(0x24, 0x20);

        link.update();

        assertTrue(link.isAirborne());
        assertFalse(playerState.runningWithPegasusBoots());
        assertEquals(0x1A, link.zVelocity());
        assertEquals(0x10, link.romSpeedX());
        assertEquals(null, link.consumePegasusScreenShakeRequest());
    }

    @Test
    void airborneSteeringRetainsMomentumAndChangesSpeedAfterMovement() throws Exception {
        InputState input = new InputState();
        InputConfig config = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        Link link = linkWithEmptyRoom(input);
        link.setPixelPosition(40, 40);
        input.onKeyEvent(config.rightKey(), GLFW_PRESS);
        link.useRocsFeather();
        link.update();
        assertEquals(41, link.pixelX());
        assertEquals(0x10, link.romSpeedX());
        input.onKeyEvent(config.rightKey(), GLFW_RELEASE);
        link.update();
        assertEquals(42, link.pixelX(), "Releasing input must not stop airborne momentum");
        assertEquals(0x10, link.romSpeedX());
        input.onKeyEvent(config.leftKey(), GLFW_PRESS);
        input.onKeyEvent(config.upKey(), GLFW_PRESS);
        link.update();
        assertEquals(43, link.pixelX(), "XY movement precedes the steering adjustment");
        assertEquals(40, link.pixelY());
        assertEquals(0x0F, link.romSpeedX());
        assertEquals(0xFF, link.romSpeedY());
        assertEquals(Link.DIRECTION_RIGHT, link.direction(),
            "Airborne steering must preserve the takeoff facing direction");
        link.update();
        assertEquals(0x0E, link.romSpeedX());
        assertEquals(0xFE, link.romSpeedY());
        input.onKeyEvent(config.leftKey(), GLFW_RELEASE);
        input.onKeyEvent(config.upKey(), GLFW_RELEASE);
        link.update();
        assertEquals(0x0E, link.romSpeedX());
        assertEquals(0xFE, link.romSpeedY());
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
        // Releasing direction preserves airborne momentum: land in column 4.
        roomObjects[ROOM_OBJECTS_BASE + 2 * ROOM_OBJECT_ROW_STRIDE + 4] = OBJECT_PIT;
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
        // Releasing direction preserves airborne momentum: land in column 4.
        roomObjects[ROOM_OBJECTS_BASE + 2 * ROOM_OBJECT_ROW_STRIDE + 4] = OBJECT_PIT;
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

    /**
     * Bank-$02 ApplyCollisionWithOceanOrSwitchBlock does not consult
     * wIsLinkInTheAir. A Roc's Feather jump therefore remains blocked by a
     * mismatched raised switch block; only pits are explicitly jump-clearable.
     */
    @Test
    void rocsFeatherCannotCrossAMismatchedRaisedSwitchBlock()
            throws Exception {
        byte[] rom = loadRom();
        RomTables romTables = RomTables.loadFromRom(rom);
        InputConfig inputConfig = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        InputState inputState = new InputState();
        inputState.onKeyEvent(inputConfig.rightKey(), GLFW_PRESS);
        int[] roomObjects = emptyRoomObjectsArea();
        roomObjects[ROOM_OBJECTS_BASE + 2 * ROOM_OBJECT_ROW_STRIDE + 1] = 0xDC;
        OverworldCollision collision = new OverworldCollision(romTables);
        collision.setPhysicsTable(RomTables.PHYSICS_TABLE_INDOORS1);
        collision.setRoom(roomObjects);
        collision.setSwitchBlocksState(0x00);
        Link link = new Link(inputState, inputConfig, romTables, collision, null,
            new PlayerState(), new ItemRegistry());
        link.setPixelPosition(0x07, 0x20);

        link.useRocsFeather();
        assertTrue(link.isAirborne());
        int jumpFrames = 0;
        while (link.isAirborne() && jumpFrames++ < 0x50) {
            link.update();
            assertEquals(0x07, link.pixelX(),
                "airborne Link must remain on the original side of the raised block");
        }

        assertTrue(jumpFrames < 0x50, "Roc's Feather jump did not land");
        assertFalse(link.isAirborne());
        assertEquals(0x07, link.pixelX());
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

    @Test
    void warpState3KeepsCollisionImmunitySeparateFromInvincibilityCounter() {
        PlayerState playerState = new PlayerState();
        Link link = new Link(new InputState(), new InputConfig(1, 2, 3, 4, 5, 6, 7),
            null, null, null, playerState, new ItemRegistry());

        link.applyWarpState3(0x48, 0x40, 0x20);
        assertEquals(0, playerState.invincibilityCounter());
        assertTrue(link.isCollisionDamageImmune());

        link.applyWarpState3(0x48, 0x40, 0);
        assertEquals(0, playerState.invincibilityCounter());
        assertTrue(link.isCollisionDamageImmune());

        link.initializeRoomPhysicsForRoom(1L, 0x01, 0x36, 1);
        assertFalse(link.isCollisionDamageImmune());
        link.applyWarpState3(0x48, 0x40, 0x20);
        link.resetTransientStateForDebug();
        assertFalse(link.isCollisionDamageImmune());
    }

    private static byte[] loadRom() throws IOException {
        try (InputStream in = linksawakening.testing.TestRom.open()) {
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

    private static Link linkWithEmptyRoom(InputState inputState) throws IOException {
        InputConfig inputConfig = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        RomTables romTables = RomTables.loadFromRom(loadRom());
        OverworldCollision collision = new OverworldCollision(romTables);
        collision.setRoom(emptyRoomObjectsArea());
        return new Link(inputState, inputConfig, romTables, collision, null,
            new PlayerState(), new ItemRegistry());
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
        int[] poses = {frame0, frame1, frame2};
        for (int frame = 0; frame < 24; frame++) {
            link.update();
            assertTrue(link.isAirborne(), "frame " + frame);
            assertEquals(poses[frame / 8], resolvedAnimationState(link), "frame " + frame);
        }
        link.update();
        assertTrue(link.isAirborne());
        assertEquals(new int[]{0x01, 0x05, 0x07, 0x0B}[direction],
            resolvedAnimationState(link));
    }

    private static Link linkFacing(int direction) throws Exception {
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
        Link link = new Link(inputState, inputConfig, RomTables.loadFromRom(loadRom()), null, null, playerState, itemRegistry);
        link.update();
        return link;
    }

    private static int resolvedAnimationState(Link link) throws Exception {
        return link.romAnimationState();
    }

    private static int javaDirectionForRomDirection(int romDirection) {
        return switch (romDirection) {
            case 0 -> Link.DIRECTION_RIGHT;
            case 1 -> Link.DIRECTION_LEFT;
            case 2 -> Link.DIRECTION_UP;
            case 3 -> Link.DIRECTION_DOWN;
            default -> throw new AssertionError(romDirection);
        };
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

    @Test
    void singleProbeCollisionSlidesAlongCornersOnBothAxes() throws Exception {
        RomTables tables = RomTables.loadFromRom(loadRom());
        // direction, start X/Y, expected X/Y. One solid tile at (32,32).
        int[][] cases = {
            {Link.DIRECTION_DOWN, 25, 16, 24, 16},
            {Link.DIRECTION_DOWN, 41, 16, 42, 16},
            {Link.DIRECTION_UP, 25, 42, 24, 42},
            {Link.DIRECTION_UP, 41, 42, 42, 42},
            {Link.DIRECTION_RIGHT, 20, 20, 20, 19},
            {Link.DIRECTION_RIGHT, 20, 38, 20, 39},
            {Link.DIRECTION_LEFT, 44, 20, 44, 19},
            {Link.DIRECTION_LEFT, 44, 38, 44, 39},
            {Link.DIRECTION_DOWN, 32, 16, 32, 16}, // Both probes: no slide.
            {Link.DIRECTION_RIGHT, 20, 32, 20, 32},
        };
        for (int[] c : cases) {
            InputConfig config = new InputConfig(1, 2, 3, 4, 5, 6, 7);
            InputState input = new InputState();
            int key = switch (c[0]) {
                case Link.DIRECTION_DOWN -> config.downKey();
                case Link.DIRECTION_UP -> config.upKey();
                case Link.DIRECTION_LEFT -> config.leftKey();
                default -> config.rightKey();
            };
            input.onKeyEvent(key, GLFW_PRESS);
            int[] objects = emptyRoomObjectsArea();
            objects[ROOM_OBJECTS_BASE + 2 * ROOM_OBJECT_ROW_STRIDE + 2] = 0x21;
            OverworldCollision collision = new OverworldCollision(tables);
            collision.setRoom(objects);
            Link link = new Link(input, config, tables, collision, null,
                new PlayerState(), new ItemRegistry());
            link.setPixelPosition(c[1], c[2]);
            link.update();
            assertEquals(c[3], link.pixelX(), java.util.Arrays.toString(c));
            assertEquals(c[4], link.pixelY(), java.util.Arrays.toString(c));
        }
    }

    private static int[] emptyRoomObjectsArea() {
        int[] roomObjectsArea = new int[0x100];
        java.util.Arrays.fill(roomObjectsArea, 0xFF);
        return roomObjectsArea;
    }

    private record CollisionAttempt(int direction, int sourceBit, int startX, int startY,
                                    int blockRow, int blockColumn, int probeX, int probeY) {
    }

    private static final class BlockingItem implements EquipmentItem {
        @Override
        public boolean blocksMotion() {
            return true;
        }
    }

    private static final class RecordingRenderItem implements EquipmentItem {
        private int renderCalls;

        @Override
        public void render(byte[] displayBuffer, int linkPixelX, int linkPixelY,
                           int direction, int offsetX, int offsetY) {
            renderCalls++;
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

    private static final class AnimationOverrideItem implements EquipmentItem {
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
