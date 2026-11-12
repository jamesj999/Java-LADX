package linksawakening.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;

import java.io.IOException;
import java.lang.reflect.Field;
import linksawakening.equipment.ItemRegistry;
import linksawakening.gameplay.GameplaySoundSink;
import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
import linksawakening.physics.OverworldCollision;
import linksawakening.rom.RomTables;
import linksawakening.state.PlayerState;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class LinkTest extends LinkFixture {

    @Test
    void solidContactSelectsPushPosesOnNextAnimationPassAndCountdownExpires() throws Exception {
        RomTables tables = RomTables.loadFromRom(loadRom());
        InputConfig config = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        InputState input = new InputState();
        input.onKeyEvent(config.rightKey(), GLFW_PRESS);
        int[] objects = emptyRoomObjectsArea();
        objects[ROOM_OBJECTS_BASE + 0x33] = OBJECT_TREE_TOP_LEFT;
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
        link.setPixelPosition(0x25, 0x28);
        for (int frame = 0; frame < 16; frame++) {
            link.tickRomLinkPushing();
            int previousSteps = link.linkAnimationState.romConsecutiveStepsCount;
            link.update();
            assertTrue(link.isRomLinkPushing());
            if (frame == 0) assertEquals(-1, link.linkAnimator.pushingAnimationState);
            else assertEquals(0x20 + ((previousSteps >> 3) & 1), link.linkAnimator.resolveAnimationState());
        }
        input.onKeyEvent(config.rightKey(), GLFW_RELEASE);
        link.tickRomLinkPushing();
        assertTrue(link.isRomLinkPushing());
        link.tickRomLinkPushing();
        assertTrue(link.isRomLinkPushing());
        link.tickRomLinkPushing();
        assertFalse(link.isRomLinkPushing());
        link.markRomLinkPushing(3); // Entity collision writes the same byte.
        int steps = link.linkAnimationState.romConsecutiveStepsCount;
        link.update();
        assertEquals(0x20 + ((steps >> 3) & 1), link.linkAnimator.resolveAnimationState());
    }

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
        Link link = new LinkTestBuilder()
                .input(input)
                .config(config)
                .tables(tables)
                .collision(collision)
                .sprites(null)
                .player(player)
                .items(new ItemRegistry())
                .build();
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
        Link atWall = new LinkTestBuilder()
                .input(new InputState())
                .config(new InputConfig(1, 2, 3, 4, 5, 6, 7))
                .tables(tables)
                .collision(collision)
                .sprites(null)
                .player(new PlayerState())
                .items(new ItemRegistry())
                .build();
        atWall.setPixelPosition(48, 40);
        atWall.applyRomSpeed(0x20, 0);
        atWall.setCollisionIgnoreFrames(16);
        for (int i = 0; i < 16; i++) atWall.update();
        assertTrue(atWall.pixelX() < 64 - 8, "Recoil must not cross the solid column");
    }

    @Test
    void debugResetClearsScriptedPresentationAndCaptureState() throws Exception {
        Link link = new LinkTestBuilder()
                .input(new InputState())
                .config(new InputConfig(1, 2, 3, 4, 5, 6, 7))
                .tables(null)
                .collision(null)
                .sprites(null)
                .player(new PlayerState())
                .items(new ItemRegistry())
                .build();

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
        Link link = new LinkTestBuilder()
                .input(new InputState())
                .config(new InputConfig(1, 2, 3, 4, 5, 6, 7))
                .tables(null)
                .collision(null)
                .sprites(null)
                .player(new PlayerState())
                .items(new ItemRegistry())
                .build();
        link.setPixelPosition(20, 30);
        link.captureRomFinalPosition();
        link.applyRomFinalPosition(0x10, 0x10);

        link.restoreRomFinalPositionX();

        assertEquals(20, link.pixelX());
        assertEquals(31, link.pixelY());
    }

    @Test
    void sideViewPotTopSnapPublishesStandingAndNextVerticalSpeed() {
        Link link = new LinkTestBuilder()
                .input(new InputState())
                .config(new InputConfig(1, 2, 3, 4, 5, 6, 7))
                .tables(null)
                .collision(null)
                .sprites(null)
                .player(new PlayerState())
                .items(new ItemRegistry())
                .build();

        link.setPixelPosition(20, 30);
        link.applySideViewPotContact(0x50, 0x02);

        assertEquals(0x40, link.pixelY());
        assertEquals(0x02, link.romSpeedY());
        assertTrue(link.standingOnSideViewEntity());
    }

    @Test
    void linkPushingFlagUsesTheRomThreeFrameCountdown() {
        Link link = new LinkTestBuilder()
                .input(new InputState())
                .config(new InputConfig(1, 2, 3, 4, 5, 6, 7))
                .tables(null)
                .collision(null)
                .sprites(null)
                .player(new PlayerState())
                .items(new ItemRegistry())
                .build();

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
    void holdingPegasusBootsChargesForTheRomThirtyTwoFramesThenStartsTheDash() throws IOException {
        InputConfig inputConfig = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        InputState inputState = new InputState();
        inputState.onKeyEvent(inputConfig.bKey(), GLFW_PRESS);
        PlayerState playerState = new PlayerState();
        playerState.setItemB(PlayerState.INVENTORY_PEGASUS_BOOTS);
        RomTables romTables = RomTables.loadFromRom(loadRom());
        OverworldCollision collision = new OverworldCollision(romTables);
        collision.setRoom(emptyRoomObjectsArea());
        Link link = new LinkTestBuilder()
                .input(inputState)
                .config(inputConfig)
                .tables(romTables)
                .collision(collision)
                .sprites(null)
                .player(playerState)
                .items(new ItemRegistry())
                .build();

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
        assertEquals((link.romEntityX() + 0x10) & 0xFF, link.pegasusBootsCollisionPosX());
        assertEquals((link.romEntityY() + 0x0C) & 0xFF, link.pegasusBootsCollisionPosY());
        assertFalse(playerState.runningWithPegasusBoots());
    }

    @Test
    void likeLikeCaptureUsesRomEntityCoordinatesAndFreezesLinkUntilRelease() {
        InputConfig inputConfig = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        Link link = new LinkTestBuilder()
                .input(new InputState())
                .config(inputConfig)
                .tables(null)
                .collision(null)
                .sprites(null)
                .player(new PlayerState())
                .items(new ItemRegistry())
                .build();
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
    void renderUsesTheSelectedRomTunicPaletteAtTheSameBodyPixels() throws Exception {
        byte[] rom = loadRom();
        LinkSpriteSheet spriteSheet = LinkSpriteSheet.loadFromRom(rom);
        LinkTunicPalette tunicPalette = LinkTunicPalette.loadFromRom(rom);
        InputConfig inputConfig = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        InputState inputState = new InputState();
        PlayerState playerState = new PlayerState();
        Link link = new LinkTestBuilder()
                .input(inputState)
                .config(inputConfig)
                .tables(null)
                .collision(null)
                .sprites(spriteSheet)
                .player(playerState)
                .items(new ItemRegistry())
                .sounds(GameplaySoundSink.none())
                .palette(tunicPalette)
                .build();
        link.setPixelPosition(40, 40);

        byte[] green = new byte[linksawakening.gpu.Framebuffer.WIDTH * linksawakening.gpu.Framebuffer.HEIGHT * 4];
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
        Link link = new LinkTestBuilder()
                .input(new InputState())
                .config(inputConfig)
                .tables(null)
                .collision(null)
                .sprites(spriteSheet)
                .player(playerState)
                .items(new ItemRegistry())
                .sounds(GameplaySoundSink.none())
                .palette(tunicPalette)
                .build();
        link.setPixelPosition(40, 40);

        byte[] normal = new byte[linksawakening.gpu.Framebuffer.WIDTH * linksawakening.gpu.Framebuffer.HEIGHT * 4];
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
    void blockedMotionStillRefreshesFacingFromHeldSingleDirection() {
        InputState inputState = new InputState();
        InputConfig inputConfig = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        PlayerState playerState = new PlayerState();
        ItemRegistry itemRegistry = new ItemRegistry();
        itemRegistry.register(playerState.itemA(), new BlockingItem());
        Link link = new LinkTestBuilder()
                .input(inputState)
                .config(inputConfig)
                .tables(null)
                .collision(null)
                .sprites(null)
                .player(playerState)
                .items(itemRegistry)
                .build();

        inputState.onKeyEvent(inputConfig.rightKey(), GLFW_PRESS);
        link.update();

        assertEquals(Link.DIRECTION_RIGHT, link.direction());
    }

    @Test
    void tarinFacingPreservesTheCurrentWalkingAnimationPhase() throws Exception {
        Link link = new LinkTestBuilder()
                .input(new InputState())
                .config(new InputConfig(1, 2, 3, 4, 5, 6, 7))
                .tables(null)
                .collision(null)
                .sprites(null)
                .player(null)
                .items(null)
                .build();
        Field walkFrame = LinkAnimationState.class.getDeclaredField("walkFrame");
        walkFrame.setAccessible(true);
        walkFrame.setInt(link.linkAnimationState, 1);
        Field walkTickCounter = LinkAnimationState.class.getDeclaredField("walkTickCounter");
        walkTickCounter.setAccessible(true);
        walkTickCounter.setInt(link.linkAnimationState, 3);

        link.faceRomDirectionPreservingWalkPhase(0);

        assertEquals(Link.DIRECTION_RIGHT, link.direction());
        assertEquals(0x0B, resolvedAnimationState(link));
        assertEquals(1, walkFrame.getInt(link.linkAnimationState));
        assertEquals(3, walkTickCounter.getInt(link.linkAnimationState));
    }

    @Test
    void appliesRomFinalPositionImmediatelyWhileMotionIsBlocked() {
        InputState inputState = new InputState();
        InputConfig inputConfig = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        PlayerState playerState = new PlayerState();
        ItemRegistry itemRegistry = new ItemRegistry();
        itemRegistry.register(playerState.itemA(), new BlockingItem());
        Link link = new LinkTestBuilder()
                .input(inputState)
                .config(inputConfig)
                .tables(null)
                .collision(null)
                .sprites(null)
                .player(playerState)
                .items(itemRegistry)
                .build();
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
        Link link = new LinkTestBuilder()
                .input(new InputState())
                .config(new InputConfig(1, 2, 3, 4, 5, 6, 7))
                .tables(null)
                .collision(null)
                .sprites(null)
                .player(new PlayerState())
                .items(new ItemRegistry())
                .build();
        link.setPixelPosition(0x40, 0x50);

        link.captureRomFinalPosition();
        link.applyRomFinalPosition(0x10, 0xF0);
        link.restoreRomFinalPosition();

        assertEquals(0x40, link.pixelX());
        assertEquals(0x50, link.pixelY());
    }

    @Test
    void romInteractiveMotionBlockStopsOneFrameWithoutInventingAnAnimationState() throws Exception {
        RomTables romTables = RomTables.loadFromRom(loadRom());
        InputState inputState = new InputState();
        InputConfig inputConfig = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        Link link = new LinkTestBuilder()
                .input(inputState)
                .config(inputConfig)
                .tables(romTables)
                .collision(null)
                .sprites(null)
                .player(new PlayerState())
                .items(new ItemRegistry())
                .build();
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
        Link link = new LinkTestBuilder()
                .input(new InputState())
                .config(new InputConfig(1, 2, 3, 4, 5, 6, 7))
                .tables(romTables)
                .collision(null)
                .sprites(null)
                .player(new PlayerState())
                .items(new ItemRegistry())
                .build();
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
        Link link = new LinkTestBuilder()
                .input(new InputState())
                .config(inputConfig)
                .tables(romTables)
                .collision(collision)
                .sprites(null)
                .player(new PlayerState())
                .items(new ItemRegistry())
                .build();
        link.setPixelPosition(0x4D, 0x50);

        assertTrue(collision.pointBlocked(0x5B, 0x59));

        for (int i = 0; i < 4; i++) {
            link.applyRomFinalPosition(0x0C, 0);
        }

        assertEquals(0x50, link.pixelX());
    }

    @Test
    void sideViewPlatformContactPreservesFractionsWithoutForcingReleasedInput() throws Exception {
        InputConfig inputConfig = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        RomTables romTables = RomTables.loadFromRom(loadRom());
        InputState inputState = new InputState();
        inputState.onKeyEvent(inputConfig.rightKey(), GLFW_PRESS);
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
        Link link = new LinkTestBuilder()
                .input(new InputState())
                .config(new InputConfig(1, 2, 3, 4, 5, 6, 7))
                .tables(null)
                .collision(null)
                .sprites(null)
                .player(new PlayerState())
                .items(new ItemRegistry())
                .build();
        link.setPixelPosition(0, 0);

        link.applySideViewPlatformContact(-1, 0x10, 0x02, true);

        assertEquals(0xFF, link.pixelX());
        assertEquals(0x00, link.pixelY());
    }

    @Test
    void sideViewPlatformContactValidatesSignedDeltaAndRomBytes() {
        Link link = new LinkTestBuilder()
                .input(new InputState())
                .config(new InputConfig(1, 2, 3, 4, 5, 6, 7))
                .tables(null)
                .collision(null)
                .sprites(null)
                .player(new PlayerState())
                .items(new ItemRegistry())
                .build();

        assertThrows(IllegalArgumentException.class, () -> link.applySideViewPlatformContact(-0x81, 0x10, 0x02, true));
        assertThrows(IllegalArgumentException.class, () -> link.applySideViewPlatformContact(0, 0x100, 0x02, true));
        assertThrows(IllegalArgumentException.class, () -> link.applySideViewPlatformContact(0, 0x10, 0x100, true));
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
        Link link = new LinkTestBuilder()
                .input(inputState)
                .config(inputConfig)
                .tables(romTables)
                .collision(collision)
                .sprites(null)
                .player(playerState)
                .items(itemRegistry)
                .build();
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
}
