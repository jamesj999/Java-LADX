package linksawakening.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;

import java.lang.reflect.Field;
import linksawakening.equipment.ItemRegistry;
import linksawakening.equipment.Ocarina;
import linksawakening.equipment.Shield;
import linksawakening.equipment.Sword;
import linksawakening.gameplay.GameplaySoundSink;
import linksawakening.gpu.Framebuffer;
import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
import linksawakening.physics.OverworldCollision;
import linksawakening.rom.Rom;
import linksawakening.rom.RomTables;
import linksawakening.state.Direction;
import linksawakening.state.InventoryItem;
import linksawakening.state.PlayerState;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class LinkEquipmentBehaviorTest extends LinkFixture {

    @Test
    void swordSwingPausesJumpFlipCounterUntilAfterItsFinalAnimationFrame() throws Exception {
        RomTables tables = RomTables.loadFromRom(loadRom());
        InputConfig config = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        PlayerState player = new PlayerState();
        ItemRegistry registry = new ItemRegistry();
        Sword sword = new Sword(tables, null);
        registry.register(InventoryItem.SWORD.romValue(), sword);
        player.inventory().setItemA(InventoryItem.SWORD.romValue());
        Link link = new LinkTestBuilder()
                .input(new InputState())
                .config(config)
                .tables(tables)
                .collision(null)
                .sprites(null)
                .player(player)
                .items(registry)
                .build();
        link.animation().setDirection(Direction.RIGHT.javaIndex());
        link.airborne().useRocsFeather();
        sword.onPress();
        for (int frame = 1; frame <= 24; frame++) {
            sword.tick(false);
            link.motion().update();
            if (frame == 15)
                assertEquals(
                        0x0B,
                        resolvedAnimationState(link),
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
        itemRegistry.register(playerState.inventory().itemA(), new AnimationOverrideItem(0x22));
        Link link = new LinkTestBuilder()
                .input(inputState)
                .config(inputConfig)
                .tables(null)
                .collision(null)
                .sprites(null)
                .player(playerState)
                .items(itemRegistry)
                .build();

        link.airborne().useRocsFeather();

        assertEquals(0x22, resolvedAnimationState(link));
    }

    @Test
    void swordAcquisitionSpinUsesRomDirectionAndAnimationTables() throws Exception {
        byte[] rom = loadRom();
        RomTables romTables = RomTables.loadFromRom(rom);
        InputConfig inputConfig = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        for (int baseRomDirection : new int[] {0, 3}) {
            Link link = new LinkTestBuilder()
                    .input(new InputState())
                    .config(inputConfig)
                    .tables(romTables)
                    .collision(null)
                    .sprites(null)
                    .player(new PlayerState())
                    .items(new ItemRegistry())
                    .build();
            link.animation().setDirection(javaDirectionForRomDirection(baseRomDirection));

            int previousAnimationState = resolvedAnimationState(link);
            for (int countdown = 0x20; countdown >= 0; countdown--) {
                link.animation().showSwordAcquisitionSpinPose(countdown);

                int sector = Math.min(7, countdown >> 2);
                int tableIndex = 0x02 * 0x4000 + (0x46C9 - 0x4000) + baseRomDirection * 8 + sector;
                int swordState = Byte.toUnsignedInt(rom[tableIndex]);
                int absoluteDirection = Byte.toUnsignedInt(rom[tableIndex + 0x20]);
                int expectedAnimationState = romTables.swordDirectionAnimState(absoluteDirection, swordState);
                if (expectedAnimationState == 0xFF) {
                    expectedAnimationState = previousAnimationState;
                }

                assertEquals(
                        expectedAnimationState,
                        resolvedAnimationState(link),
                        "base=" + baseRomDirection + ", countdown=" + countdown);
                assertEquals(
                        javaDirectionForRomDirection(absoluteDirection),
                        link.animation().direction(),
                        "base=" + baseRomDirection + ", direction countdown=" + countdown);
                previousAnimationState = expectedAnimationState;
            }
        }
    }

    @Test
    void swordAcquisitionSpinOverridesAQueuedMotionBlockedButtPose() throws Exception {
        RomTables romTables = RomTables.loadFromRom(loadRom());
        InputConfig inputConfig = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        Link link = new LinkTestBuilder()
                .input(new InputState())
                .config(inputConfig)
                .tables(romTables)
                .collision(null)
                .sprites(null)
                .player(new PlayerState())
                .items(new ItemRegistry())
                .build();
        link.animation().setDirection(Direction.RIGHT.javaIndex());

        link.animation().showSwordAcquisitionSpinPose(0x20);
        int swordState = romTables.swordSpinAnimationState(0, 7);
        int absoluteDirection = romTables.swordSpinAbsoluteDirection(0, 7);
        int expectedAnimationState = romTables.swordDirectionAnimState(absoluteDirection, swordState);

        link.motion().blockNextRomMotionFrame();
        link.animation().showSwordAcquisitionSpinPose(0x1F);

        assertEquals(expectedAnimationState, resolvedAnimationState(link));
    }

    @Test
    void swordAcquisitionRendersTheUnslottedSwordBeforeTheRewardIsGranted() throws Exception {
        byte[] rom = loadRom();
        InputConfig inputConfig = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        PlayerState playerState = new PlayerState();
        playerState.playerInitialization().initializeNewGame(0, 0, 0);
        ItemRegistry itemRegistry = new ItemRegistry();
        RecordingRenderItem sword = new RecordingRenderItem();
        itemRegistry.register(InventoryItem.SWORD.romValue(), sword);
        Link link = new LinkTestBuilder()
                .input(new InputState())
                .config(inputConfig)
                .tables(RomTables.loadFromRom(rom))
                .collision(null)
                .sprites(LinkSpriteSheet.loadFromRom(Rom.of(rom)))
                .player(playerState)
                .items(itemRegistry)
                .sounds(GameplaySoundSink.none())
                .palette(LinkTunicPalette.loadFromRom(Rom.of(rom)))
                .build();
        link.position().setPixelPosition(40, 40);
        link.animation().showSwordAcquisitionSpinPose(0x20);

        link.animation().render(new byte[Framebuffer.WIDTH * Framebuffer.HEIGHT * 4], 0, 0);

        assertEquals(1, sword.renderCalls);
    }

    @Test
    void ocarinaPlaybackUsesTheRomBodyAnimationStates() throws Exception {
        PlayerState playerState = new PlayerState();
        playerState.inventory().setItemA(InventoryItem.OCARINA.romValue());
        ItemRegistry itemRegistry = new ItemRegistry();
        OcarinaAnimationTarget target = new OcarinaAnimationTarget();
        itemRegistry.register(
                playerState.inventory().itemA(), new Ocarina(playerState, GameplaySoundSink.none(), target));
        Link link = new LinkTestBuilder()
                .input(new InputState())
                .config(new InputConfig(1, 2, 3, 4, 5, 6, 7))
                .tables(null)
                .collision(null)
                .sprites(null)
                .player(playerState)
                .items(itemRegistry)
                .build();

        target.playing = true;
        target.animationState = 0x76;
        assertEquals(0x76, resolvedAnimationState(link));

        target.animationState = 0x75;
        assertEquals(0x75, resolvedAnimationState(link));

        target.playing = false;
        assertEquals(0x00, resolvedAnimationState(link));
    }

    @Test
    void tarinRewardSwitchesFromHeldItemToStandingShieldDown() throws Exception {
        Link link = new LinkTestBuilder()
                .input(new InputState())
                .config(new InputConfig(1, 2, 3, 4, 5, 6, 7))
                .tables(null)
                .collision(null)
                .sprites(null)
                .player(new PlayerState())
                .items(new ItemRegistry())
                .build();
        link.animation().showTarinShieldPresentation(new int[] {0, 1, 2, 3});
        assertEquals(0x6C, resolvedAnimationState(link));

        link.animation().showStandingShieldDownPose();

        assertEquals(0x22, resolvedAnimationState(link));
        assertFalse(link.animation().isTarinShieldPresentationVisible());
    }

    @Test
    void itemAttackStepCountdownStartsAndDecrementsLikeRom() {
        PlayerState playerState = new PlayerState();
        ItemRegistry itemRegistry = new ItemRegistry();
        itemRegistry.register(playerState.inventory().itemA(), new BlockingItem());
        Link link = new LinkTestBuilder()
                .input(new InputState())
                .config(new InputConfig(1, 2, 3, 4, 5, 6, 7))
                .tables(null)
                .collision(null)
                .sprites(null)
                .player(playerState)
                .items(itemRegistry)
                .build();

        assertEquals(0, link.equipment().romAttackStepAnimationCountdown());

        link.equipment().startRomItemAttackStep();

        assertEquals(0x0C, link.equipment().romAttackStepAnimationCountdown());
        link.motion().update();
        assertEquals(0x0B, link.equipment().romAttackStepAnimationCountdown());

        for (int i = 0; i < 0x0B; i++) {
            link.motion().update();
        }
        assertEquals(0, link.equipment().romAttackStepAnimationCountdown());

        link.motion().update();
        assertEquals(0, link.equipment().romAttackStepAnimationCountdown());
    }

    @Test
    void tarinCanClearTheLiveAttackStepCountdown() {
        Link link = new LinkTestBuilder()
                .input(new InputState())
                .config(new InputConfig(1, 2, 3, 4, 5, 6, 7))
                .tables(null)
                .collision(null)
                .sprites(null)
                .player(null)
                .items(null)
                .build();
        link.equipment().startRomMagicPowderAttackStep();

        link.equipment().clearRomAttackStepAnimationCountdown();

        assertEquals(0, link.equipment().romAttackStepAnimationCountdown());
    }

    @Test
    void zeroDurationBitSevenAttackStepClearsTheFullRomByte() throws Exception {
        PlayerState playerState = new PlayerState();
        ItemRegistry itemRegistry = new ItemRegistry();
        itemRegistry.register(playerState.inventory().itemA(), new BlockingItem());
        Link link = new LinkTestBuilder()
                .input(new InputState())
                .config(new InputConfig(1, 2, 3, 4, 5, 6, 7))
                .tables(null)
                .collision(null)
                .sprites(null)
                .player(playerState)
                .items(itemRegistry)
                .build();
        Field countdown = LinkItemState.class.getDeclaredField("romAttackStepAnimationCountdown");
        countdown.setAccessible(true);
        countdown.setInt(link.linkItemState, 0x80);

        assertEquals(0x80, link.equipment().romAttackStepAnimationCountdown());

        link.motion().update();

        assertEquals(0, link.equipment().romAttackStepAnimationCountdown());
    }

    @Test
    void exposesShieldUseOnlyForTheEquippedSlotWhoseButtonIsHeld() {
        InputState inputState = new InputState();
        InputConfig inputConfig = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        PlayerState playerState = new PlayerState();
        playerState.inventory().setItemA(InventoryItem.SHIELD.romValue());
        Link link = new LinkTestBuilder()
                .input(inputState)
                .config(inputConfig)
                .tables(null)
                .collision(null)
                .sprites(null)
                .player(playerState)
                .items(new ItemRegistry())
                .build();

        assertEquals(0, link.airborne().romMotionState());
        assertFalse(link.equipment().isUsingShield());

        inputState.onKeyEvent(inputConfig.aKey(), GLFW_PRESS);
        assertTrue(link.equipment().isUsingShield());

        inputState.onKeyEvent(inputConfig.aKey(), GLFW_RELEASE);
        playerState.inventory().setItemA(InventoryItem.SWORD.romValue());
        playerState.inventory().setItemB(InventoryItem.SHIELD.romValue());
        inputState.onKeyEvent(inputConfig.bKey(), GLFW_PRESS);
        assertTrue(link.equipment().isUsingShield());

        inputState.onKeyEvent(inputConfig.bKey(), GLFW_RELEASE);
        assertFalse(link.equipment().isUsingShield());
    }

    @Test
    void dialogueBlocksShieldUseAndRaisedPoseUntilGameplayResumes() throws Exception {
        InputState input = new InputState();
        InputConfig config = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        PlayerState player = new PlayerState();
        player.equipmentLevels().setShieldLevel(1);
        player.inventory().setItemB(InventoryItem.SHIELD.romValue());
        Link link = new LinkTestBuilder()
                .input(input)
                .config(config)
                .tables(null)
                .collision(null)
                .sprites(LinkSpriteSheet.loadFromRom(Rom.of(loadRom())))
                .player(player)
                .items(new ItemRegistry())
                .build();
        java.util.concurrent.atomic.AtomicBoolean dialogue = new java.util.concurrent.atomic.AtomicBoolean(true);
        link.equipment().setShieldInputAllowed(() -> !dialogue.get());
        int carryingPose = resolvedAnimationState(link);
        input.onKeyEvent(config.bKey(), GLFW_PRESS);
        assertFalse(link.equipment().isUsingShield());
        assertEquals(carryingPose, resolvedAnimationState(link));
        dialogue.set(false);
        assertTrue(link.equipment().isUsingShield());
        assertTrue(carryingPose != resolvedAnimationState(link));
    }

    @Test
    void equippedShieldSelectsRomCarryingPoseWithoutAButtonPress() throws Exception {
        byte[] rom = loadRom();
        InputState input = new InputState();
        InputConfig config = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        PlayerState player = new PlayerState();
        player.inventory().setItemA(InventoryItem.SWORD.romValue());
        player.inventory().setItemB(InventoryItem.SHIELD.romValue());
        Link link = new LinkTestBuilder()
                .input(input)
                .config(config)
                .tables(null)
                .collision(null)
                .sprites(LinkSpriteSheet.loadFromRom(Rom.of(rom)))
                .player(player)
                .items(new ItemRegistry())
                .build();
        for (int level : new int[] {1, 2}) {
            player.equipmentLevels().setShieldLevel(level);
            for (int direction = 0; direction < 4; direction++) {
                link.animation().setRomDirection(direction);
                int table = level == 1 ? 0x4918 : 0x4928;
                int offset = 2 * 0x4000 + table - 0x4000 + direction * 2;
                assertEquals(rom[offset] & 0xFF, resolvedAnimationState(link));
                input.onKeyEvent(config.bKey(), GLFW_PRESS);
                assertEquals(rom[offset + 8] & 0xFF, resolvedAnimationState(link));
                input.onKeyEvent(config.bKey(), GLFW_RELEASE);
            }
        }
        player.inventory().setItemB(InventoryItem.MAGIC_POWDER.romValue());
        link.animation().setRomDirection(2);
        assertEquals(rom[2 * 0x4000 + 0x0914] & 0xFF, resolvedAnimationState(link));
    }

    @Test
    void shieldUseSelectsTheRomDefaultAndMirrorBodyAnimationTables() throws Exception {
        InputState inputState = new InputState();
        InputConfig inputConfig = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        PlayerState playerState = new PlayerState();
        playerState.inventory().setItemA(InventoryItem.SHIELD.romValue());
        ItemRegistry itemRegistry = new ItemRegistry();
        itemRegistry.register(InventoryItem.SHIELD.romValue(), new Shield(GameplaySoundSink.none()));
        Link link = new LinkTestBuilder()
                .input(inputState)
                .config(inputConfig)
                .tables(null)
                .collision(null)
                .sprites(null)
                .player(playerState)
                .items(itemRegistry)
                .build();

        inputState.onKeyEvent(inputConfig.aKey(), GLFW_PRESS);
        assertEquals(0x24, resolvedAnimationState(link));

        playerState.equipmentLevels().setShieldLevel(2);
        assertEquals(0x26, resolvedAnimationState(link));

        link.animation().setDirection(Direction.RIGHT.javaIndex());
        assertEquals(0x2E, resolvedAnimationState(link));
    }

    @Test
    void swordSwingBlocksWalkingButDoesNotStopAFeatherJump() throws Exception {
        InputState input = new InputState();
        InputConfig config = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        PlayerState player = new PlayerState();
        ItemRegistry registry = new ItemRegistry();
        Sword sword = new Sword(null, null);
        registry.register(player.inventory().itemA(), sword);
        RomTables tables = RomTables.loadFromRom(loadRom());
        OverworldCollision collision = new OverworldCollision(tables);
        collision.setRoom(emptyRoomObjectsArea());
        Link link = new LinkTestBuilder()
                .input(input)
                .config(config)
                .tables(tables)
                .collision(collision)
                .sprites(null)
                .player(player)
                .items(registry)
                .build();
        link.position().setPixelPosition(40, 40);
        input.onKeyEvent(config.rightKey(), GLFW_PRESS);
        sword.onPress();
        sword.tick(true);
        link.motion().update();
        assertEquals(41, link.position().pixelX(), "Launch-frame XY precedes sword animation");
        sword.tick(true);
        link.motion().update();
        assertEquals(41, link.position().pixelX());
        link.airborne().useRocsFeather();
        link.motion().update();
        assertEquals(42, link.position().pixelX());
        assertTrue(link.airborne().isAirborne());
        assertTrue(sword.blocksMotion());
        link.motion().update();
        assertEquals(43, link.position().pixelX());
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
        itemRegistry.register(playerState.inventory().itemA(), sword);
        RomTables romTables = RomTables.loadFromRom(loadRom());
        Link link = new LinkTestBuilder()
                .input(inputState)
                .config(inputConfig)
                .tables(romTables)
                .collision(new OverworldCollision(romTables))
                .sprites(null)
                .player(playerState)
                .items(itemRegistry)
                .build();

        inputState.onKeyEvent(inputConfig.rightKey(), GLFW_PRESS);
        link.motion().update();
        assertEquals(Direction.RIGHT.javaIndex(), link.animation().direction());

        sword.onPress();
        while (sword.state() != Sword.STATE_HOLDING) {
            sword.tick(true);
        }

        inputState.onKeyEvent(inputConfig.rightKey(), GLFW_RELEASE);
        inputState.onKeyEvent(inputConfig.upKey(), GLFW_PRESS);
        link.motion().update();

        assertEquals(Direction.RIGHT.javaIndex(), link.animation().direction());
    }
}
