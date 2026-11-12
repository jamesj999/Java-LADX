package linksawakening.world.room;

import static linksawakening.world.room.RoomSessionTestSupport.*;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.entity.Link;
import linksawakening.gameplay.BeachSwordRewardConsumer;
import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.rom.RomBank;
import linksawakening.startup.NewGameStartProfile;
import linksawakening.state.PlayerState;
import linksawakening.vfx.TransientVfxSystem;
import linksawakening.vfx.TransientVfxType;
import linksawakening.world.entity.EnemyDropResolver;
import linksawakening.world.entity.EntityPickupEvent;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityObjectSample;
import linksawakening.world.entity.RoomEntityRuntime;
import linksawakening.world.entity.RoomEntitySnapshot;
import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.KeyRewardEvent;
import linksawakening.world.entity.event.SlimeKeyRewardEvent;
import linksawakening.world.entity.event.WitchExchangeEvent;
import linksawakening.world.entity.event.WitchRewardEvent;
import linksawakening.world.transition.Warp;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class RoomSessionRewards1Test {

    @Test
    void newGameWorldInitializationRestoresTheBeachOpeningEntities() {
        RoomSession session = newSession();
        byte[] completedOverworld = new byte[0x100];
        completedOverworld[0xF2] = 0x30;
        session.restoreRoomStatuses(completedOverworld, new byte[0x100], new byte[0x100], new byte[0x20]);
        session.setEnemyDropCountersForTest(7, 9);
        session.setSwitchButtonPressedForTest(0x60);
        session.loadInitialOverworld(0xF2);
        assertFalse(session.activeRoom().entities().loadedEntities().stream()
                .anyMatch(entity -> entity.type() == 0x31 || entity.type() == 0x41));

        session.initializeNewGameWorldState();
        session.loadInitialOverworld(0xF2);

        assertEquals(new EnemyDropResolver.CounterState(0, 0), session.enemyDropCountersForTest());
        assertEquals(0, session.switchButtonPressedForTest());
        assertArrayEquals(new byte[DungeonItemState.ITEM_FLAG_SIZE], session.currentDungeonItemFlagsSnapshot());
        assertTrue(session.activeRoom().entities().loadedEntities().stream().anyMatch(entity -> entity.type() == 0x31));
        assertTrue(session.activeRoom().entities().loadedEntities().stream().anyMatch(entity -> entity.type() == 0x41));
    }

    @Test
    void swordlessBeachUsesTheSwordDisplayListBeforeTheFirstEntityTick() {
        RoomSession session = newSession();
        session.loadInitialOverworld(0xF2);
        session.setChestPlayerLevels(0, 0, 0);

        RoomEntity sword = session.activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x31)
                .findFirst()
                .orElseThrow();

        assertEquals(0x84, sword.spriteDefinition().variant(0).first().tile());
    }

    @Test
    void openingBeachEventsCompleteInPlayOrderAndStayGoneAfterReload() {
        RoomSession session = newSession();
        PlayerState playerState = new PlayerState();
        NewGameStartProfile.romDefaults().initializePlayerState(playerState);
        session.loadInitialOverworld(0xF2);
        session.setChestPlayerLevels(0, 0, 0);

        tickInteractive(session, 0, 0x3F, 0x44);
        tickInteractive(session, 1, 0x40, 0x44);
        assertEquals(0x22, session.consumePendingMusicTrack());

        boolean owlDialogOpened = false;
        int frame = 2;
        while ((session.overworldRoomStatusForTest(0xF2) & 0x20) == 0 && frame < 0x300) {
            tickInteractive(session, frame++, 0x58, 0x60);
            owlDialogOpened |= session.consumeEntityDialogRequests().stream()
                    .anyMatch(request -> request.globalDialogId() == 0x0D9);
        }
        assertTrue(owlDialogOpened);
        assertEquals(0x20, session.overworldRoomStatusForTest(0xF2) & 0x20);
        assertEquals(0, session.overworldRoomStatusForTest(0xF2) & 0x10);

        boolean owlMusicRestored = false;
        while (session.activeRoom().entities().loadedEntities().stream().anyMatch(entity -> entity.type() == 0x41)
                && frame < 0x400) {
            tickInteractive(session, frame++, 0x58, 0x60);
            owlMusicRestored |= session.consumePendingMusicTrack() == 0x1D;
        }
        assertTrue(owlMusicRestored);

        RoomEntity sword = session.activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x31)
                .findFirst()
                .orElseThrow();
        int pickupFrame = (frame & 0xFE) | ((sword.slot() ^ 1) & 1);
        EntityPickupEvent pickup = session.collectEntityIfNeeded(pickupFrame, sword.x(), sword.y(), false, true, 3, 0);
        assertNotNull(pickup);
        assertEquals(0x0F, session.consumePendingMusicTrack());

        frame = pickupFrame + 1;
        while (playerState.swordLevel() == 0 && frame < pickupFrame + 0x300) {
            session.tickEntities(frame++, sword.x(), sword.y());
            session.consumeEntityDialogRequests();
            BeachSwordRewardConsumer.consume(session, playerState);
        }
        assertEquals(1, playerState.swordLevel());
        assertEquals(PlayerState.INVENTORY_SWORD, playerState.itemB());
        assertEquals(0x20, session.overworldRoomStatusForTest(0xF2) & 0x20);
        assertEquals(0x10, session.overworldRoomStatusForTest(0xF2) & 0x10);
        // $03:5BCE-5BD7 requests MUSIC_OVERWORLD_INTRO; MUSIC_OVERWORLD is only
        // written to hDefaultMusicTrack/hNextDefaultMusicTrack.
        assertEquals(0x31, session.consumePendingMusicTrack());

        session.loadInitialOverworld(0xF2);
        assertFalse(session.activeRoom().entities().loadedEntities().stream()
                .anyMatch(entity -> entity.type() == 0x31 || entity.type() == 0x41));
    }

    @Test
    void postInstrumentOwlWaitsForInteractiveLinkBeforeOpeningDialog() {
        RoomSession session = newSession();
        byte[] instrumentFlags = new byte[0x08];
        instrumentFlags[0] = 0x02;
        session.restoreDungeonProgressFlags(instrumentFlags);
        session.setChestPlayerLevels(0, 1, 0);
        session.loadInitialOverworld(0xD2);

        session.tickEntities(0, 0x50, 0x50);
        session.tickEntities(1, 0x50, 0x50);

        assertTrue(session.consumeEntityDialogRequests().isEmpty());
        tickInteractive(session, 2, 0x50, 0x50);
        assertEquals(0x0C2, session.consumeEntityDialogRequests().getFirst().globalDialogId());
    }

    @Test
    void witchExchangeRunsThroughTheLiveIndoorRoomInSourceOrder() {
        RoomSession session = newSession();
        session.loadIndoor(0x0E, 0xA2);
        RoomEntity witch = session.activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x40)
                .findFirst()
                .orElseThrow();
        assertTrue(witch.spriteDefinition().supported());
        assertEquals(0x04, session.indoorTorchPaletteEffectAddressForTest());

        session.setToadstoolPlayerState(true, 0);
        session.setEntityInventorySlots(0, 0x0C);
        session.setEntityActionButtonsHeld(false, true);
        session.setEntityDefaultMusicTrack(0x0C);
        tickInteractive(session, 0, witch);
        tickInteractive(session, 1, witch);

        assertEquals(List.of(new WitchExchangeEvent(witch.slot(), 0)), session.consumeWitchExchangeEvents());

        session.setEntityActionButtonsHeld(false, false);
        int frame = 2;
        boolean blocked = false;
        boolean brewingDialog = false;
        while (!brewingDialog && frame < 0x40) {
            tickInteractive(session, frame++, witch);
            blocked |= !session.consumeLinkMotionBlockRequests().isEmpty();
            brewingDialog |= session.consumeEntityDialogRequests().stream()
                    .anyMatch(request -> request.globalDialogId() == 0x009);
        }
        assertTrue(blocked);
        assertTrue(brewingDialog);

        session.setEntityDialogActive(true);
        // A reader may leave this dialogue open longer than the entire brew.
        for (int tick = 0; tick < 300; tick++) {
            tickInteractive(session, frame++, witch);
            assertTrue(session.consumeEntityDialogRequests().isEmpty());
            assertTrue(session.consumeWitchRewardEvents().isEmpty());
        }
        session.setEntityDialogActive(false);
        for (int tick = 1; tick <= 0xC0; tick++) {
            tickInteractive(session, frame++, witch);
            var dialogs = session.consumeEntityDialogRequests();
            if (tick < 0xC0) {
                assertTrue(dialogs.isEmpty(), "Brewing must wait for 192 interactive frames");
            } else {
                assertTrue(dialogs.stream().anyMatch(request -> request.globalDialogId() == 0x0FE));
            }
        }
        assertEquals(0x0C, session.consumePendingMusicTrack());

        session.setEntityDialogActive(true);
        tickInteractive(session, frame++, witch);
        session.setEntityDialogActive(false);
        tickInteractive(session, frame++, witch);
        assertEquals(List.of(new WitchRewardEvent(witch.slot())), session.consumeWitchRewardEvents());
        assertTrue(session.gotItemPresentationActive());
        assertTrue(session.consumeEntityEvents().stream()
                .anyMatch(event ->
                        event.soundChannel() == EntityCombatEvent.SoundChannel.JINGLE && event.soundId() == 0x01));

        boolean powderDialog = false;
        boolean powderHeldPose = false;
        boolean powderMotionBlocked = false;
        while (!powderDialog && frame < 0x420) {
            tickInteractive(session, frame++, witch);
            powderHeldPose |= !session.consumeLinkHeldItemPoseRequests().isEmpty();
            powderMotionBlocked |= !session.consumeLinkMotionBlockRequests().isEmpty();
            powderDialog |= session.consumeEntityDialogRequests().stream()
                    .anyMatch(request -> request.globalDialogId() == 0x099);
        }
        assertTrue(powderHeldPose);
        assertTrue(powderMotionBlocked);
        assertTrue(powderDialog);

        session.setEntityDialogActive(true);
        tickInteractive(session, frame++, witch);
        session.setEntityDialogActive(false);
        tickInteractive(session, frame++, witch);

        session.setRoomTriggerCountForTest(1);
        session.igniteIndoorTorchPaletteForTest();
        // Two eleven-frame cycles, then a frame without a palette upload.
        for (int tick = 0; tick < 24; tick++) {
            tickInteractive(session, frame++, witch);
        }
        assertTrue(
                session.consumeEntityDialogRequests().stream().anyMatch(request -> request.globalDialogId() == 0x17E));
    }

    @Test
    void keyDropPointCollectionMarksTheRoomAndPublishesItsSmallKeyReward() {
        RoomSession session = newSession();
        byte[] indoorA = new byte[0x100];
        indoorA[0x69] = 0x10;
        session.restoreRoomStatuses(new byte[0x100], indoorA, new byte[0x100], new byte[0x20]);
        session.loadIndoor(0x03, 0x7C, Warp.CATEGORY_SIDESCROLL);

        session.tickEntities(0);
        RoomEntity key = session.activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x30)
                .findFirst()
                .orElseThrow();
        int pickupFrame = (key.slot() & 0x01) == 0 ? 1 : 0;

        EntityPickupEvent pickup = session.collectEntityIfNeeded(pickupFrame, key.x(), key.y(), false, true, 0, 0);

        assertNotNull(pickup);
        assertEquals(0x30, pickup.type());
        assertEquals(
                EntityStatus.DISABLED,
                session.activeRoom().entities().slots().get(key.slot()).status());
        assertEquals(0x10, session.indoorRoomStatusForTest(0x03, 0x7C) & 0x10);
        assertEquals(
                List.of(new KeyRewardEvent(key.slot(), ChestContentsTable.CHEST_SMALL_KEY)),
                session.consumeKeyRewardEvents());
    }

    @Test
    void heartContainerCompletionMarksBossRoomAndAnglersStaircase() {
        RoomSession session = newSession();
        session.loadIndoor(0x03, 0x65, Warp.CATEGORY_INDOOR);

        session.markHeartContainerCollected();

        assertEquals(0x20, session.indoorRoomStatusForTest(0x03, 0x65) & 0x20);
        assertEquals(0x20, session.indoorRoomStatusForTest(0x03, 0x66) & 0x20);
    }

    @Test
    void heartContainerCompletionMarksEaglesTowerStaircase() {
        RoomSession session = newSession();
        session.loadIndoor(0x06, 0x2F, Warp.CATEGORY_INDOOR);

        session.markHeartContainerCollected();

        assertEquals(0x20, session.indoorRoomStatusForTest(0x06, 0x2F) & 0x20);
        assertEquals(0x20, session.indoorRoomStatusForTest(0x06, 0x2E) & 0x20);
    }

    @Test
    void harvestingHidingSlimeKeyPublishesItsLeafCountAndCompletesTheRoom() {
        RoomSession session = newSession();
        session.loadOverworld(0xC6);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(new RoomEntitySnapshot(List.of(
                new RoomEntity(
                        0,
                        0,
                        0x3C,
                        0x40,
                        0x50,
                        EntityStatus.INIT,
                        EntitySpriteDefinition.unsupported(0x3C),
                        -1,
                        0,
                        0,
                        0,
                        -1,
                        false),
                RoomEntity.disabled(1),
                RoomEntity.disabled(2),
                RoomEntity.disabled(3),
                RoomEntity.disabled(4),
                RoomEntity.disabled(5),
                RoomEntity.disabled(6),
                RoomEntity.disabled(7),
                RoomEntity.disabled(8),
                RoomEntity.disabled(9),
                RoomEntity.disabled(10),
                RoomEntity.disabled(11),
                RoomEntity.disabled(12),
                RoomEntity.disabled(13),
                RoomEntity.disabled(14),
                RoomEntity.disabled(15))));
        linksawakening.world.entity.EntityRuntimeTestAccess.setEntityRoomIdForTest(runtime, 0xC6);
        runtime.configuration().setObjectQuery(entity -> new RoomEntityObjectSample(0xCC, 0, 0x40, 0x50));
        session.replaceEntityRuntimeForTest(runtime);
        session.setEntityGoldenLeavesCount(5);

        session.tickEntities(0, 0x20, 0x30);
        session.tickEntities(1, 0x20, 0x30);
        linksawakening.world.entity.EntityRuntimeTestAccess.setHidingSlimeKeyTransitionCountdownForTest(
                runtime, 0, 0x11);
        session.tickEntities(2, 0x20, 0x30);

        assertEquals(List.of(new SlimeKeyRewardEvent(0, 6, 0xA2)), session.consumeSlimeKeyRewardEvents());
        assertEquals(0x10, session.overworldRoomStatusSnapshot()[0xC6] & 0x10);
        assertEquals(List.of(new DialogRequest(0, 0xA2)), session.consumeEntityDialogRequests());
    }

    @Test
    void tailKeyChestUsesTheRomUpwardInteractionReach() {
        RoomSession session = newSession();
        session.loadInitialOverworld(0x41);
        // Real replay: entity (72,80), then approach to the source boundary (72,74).
        assertFalse(session.tryOpenChest(64, 64, Link.DIRECTION_UP, true, 1).opened());
        assertFalse(session.tryOpenChest(64, 59, Link.DIRECTION_UP, true, 1).opened());
        var opened = session.tryOpenChest(64, 58, Link.DIRECTION_UP, true, 1);
        assertTrue(opened.opened());
        assertEquals(ChestContentsTable.CHEST_TAIL_KEY, opened.itemType());
        assertEquals(0x34, opened.location());
        int tileIndex = 6 * RoomConstants.ROOM_TILE_WIDTH + 8;
        // $03:5053 is the CGB open-chest draw command, column-major.
        byte[] rom = loadRom();
        int source = RomBank.romOffset(3, 0x5053);
        int[] positions = {
            tileIndex,
            tileIndex + RoomConstants.ROOM_TILE_WIDTH,
            tileIndex + 1,
            tileIndex + RoomConstants.ROOM_TILE_WIDTH + 1
        };
        for (int i = 0; i < 4; i++) {
            assertEquals(rom[source + i] & 0xFF, session.activeRoom().tileIds()[positions[i]]);
        }
        session.loadInitialOverworld(0x51);
        session.loadInitialOverworld(0x41);
        assertFalse(session.tryOpenChest(64, 58, Link.DIRECTION_UP, true, 1).opened());
        for (int i = 0; i < 4; i++) {
            assertEquals(rom[source + i] & 0xFF, session.activeRoom().tileIds()[positions[i]]);
        }
    }

    @Test
    void openingAClosedChestMutatesTheRoomAndSpawnsTheRomChestEntity() {
        RoomSession session = newSession();
        session.loadIndoor(0x00, 0x20);
        int location = 0x22;
        int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + (location & 0xF0) + (location & 0x0F);
        session.activeRoom().roomObjectsArea()[areaIndex] = 0xA0;

        RoomSession.ChestOpenResult result =
                session.tryOpenChest(0x18, 0x21, linksawakening.entity.Link.DIRECTION_UP, true, 1);

        assertTrue(result.opened());
        assertEquals(0xA1, session.activeRoom().roomObjectsArea()[areaIndex]);
        assertTrue(session.activeRoom().renderValues() == null);
        assertEquals(0x10, session.indoorRoomStatusForTest(0x00, 0x20) & 0x10);
        RoomEntity chest = session.activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == EntitySpriteHandlerCatalog.ENTITY_CHEST_WITH_ITEM)
                .findFirst()
                .orElseThrow();
        assertEquals(EntityStatus.INIT, chest.status());
        assertEquals(0x28, chest.x());
        assertEquals(0x30, chest.y());
        assertEquals(result.itemType(), chest.spriteVariant());
    }

    @Test
    void tailCaveFirstRoomRevealsItsSmallKeyChestAfterStandingOnTheButton() {
        TransientVfxSystem vfx = new TransientVfxSystem(16);
        RoomSession session = newSession(vfx);
        List<GameplaySoundEvent> sounds = new ArrayList<>();
        session.setColorShellSoundSink(sounds::add);
        session.loadIndoor(0x00, 0x13);
        int chestLocation = 0x28;
        int chestIndex = RoomConstants.ROOM_OBJECTS_BASE + chestLocation;
        int buttonIndex = RoomConstants.ROOM_OBJECTS_BASE + 0x35;

        assertEquals(0x63, session.activeRoomEventForTest());
        assertEquals(0xAA, session.activeRoom().roomObjectsArea()[buttonIndex]);
        assertNotEquals(0xA0, session.activeRoom().roomObjectsArea()[chestIndex]);
        for (int frame = 0; frame < 23; frame++) {
            session.tickEntities(frame, 0x58, 0x40);
        }
        assertTrue(sounds.isEmpty());
        assertEquals(0x63, session.activeRoomEventForTest());

        session.tickEntities(23, 0x58, 0x40);

        assertEquals(0, session.activeRoomEventForTest());
        assertEquals(List.of(GameplaySoundEvent.FLOOR_SWITCH, GameplaySoundEvent.PUZZLE_SOLVED), sounds);
        assertEquals(
                TransientVfxType.CHEST_APPEARS, vfx.activeSlots().getFirst().type());
        assertEquals(0x0F, vfx.activeSlots().getFirst().countdown());
        assertNotEquals(0xA0, session.activeRoom().roomObjectsArea()[chestIndex]);

        for (int frame = 24; frame <= 34; frame++) {
            session.tickEntities(frame, 0x40, 0x60);
        }

        assertEquals(0xA0, session.activeRoom().roomObjectsArea()[chestIndex]);
        RoomSession.ChestOpenResult chest = session.tryOpenChest(0x78, 0x21, Link.DIRECTION_UP, true, 1);
        assertTrue(chest.opened());
        assertEquals(ChestContentsTable.CHEST_SMALL_KEY, chest.itemType());
    }

    @Test
    void bottleGrottoRoom27RevealsNightmareKeyChestForSourceKillOrder012() {
        byte[] rom = loadRom();
        RoomEntityRuntime runtime = runtimeWithKillOrder(rom, 0, 1, 2);
        assertEquals(3, linksawakening.world.entity.EntityRuntimeTestAccess.killCount(runtime));
        assertEquals(0, runtime.inspection().killOrderAt(0));
        assertEquals(1, runtime.inspection().killOrderAt(1));
        assertEquals(2, runtime.inspection().killOrderAt(2));

        TransientVfxSystem vfx = new TransientVfxSystem(16);
        RoomSession session = newSession(vfx);
        List<GameplaySoundEvent> sounds = new ArrayList<>();
        session.setColorShellSoundSink(sounds::add);
        session.loadIndoor(0x01, 0x27);
        session.replaceEntityRuntimeForTest(runtime);
        int chestIndex = RoomConstants.ROOM_OBJECTS_BASE + 0x28;
        assertEquals(0x66, session.activeRoomEventForTest());
        assertNotEquals(0xA0, session.activeRoom().roomObjectsArea()[chestIndex]);

        int frame = 0;
        session.tickEntities(frame++, 0x50, 0x60);

        assertEquals(0, session.activeRoomEventForTest());
        assertEquals(List.of(GameplaySoundEvent.PUZZLE_SOLVED), sounds);
        assertEquals(
                TransientVfxType.CHEST_APPEARS, vfx.activeSlots().getFirst().type());
        while (session.activeRoom().roomObjectsArea()[chestIndex] != 0xA0 && frame < 0x200) {
            session.tickEntities(frame++, 0x50, 0x60);
        }
        assertEquals(0xA0, session.activeRoom().roomObjectsArea()[chestIndex]);
        assertEquals(
                ChestContentsTable.CHEST_NIGHTMARE_KEY, new ChestContentsTable(rom).itemForSpawn(0x01, 0x27, 1, true));
    }

    @Test
    void openingCompassChestDoesNotCancelTheKillAllShutterEvent() {
        RoomSession session = newSession();
        session.loadIndoor(0, 0x15);
        assertEquals(0x21, session.activeRoomEventForTest());
        assertTrue(session.tryOpenChest(48, 58, Link.DIRECTION_UP, true, 1).opened());
        assertEquals(0x10, session.indoorRoomStatusForTest(0, 0x15) & 0x10);
        session.tickEntities(0, 56, 74);
        assertNotEquals(0, session.activeRoom().shutterDoorMask());
        // Reloading the opened chest must not bypass the still-live enemies.
        session.loadIndoor(0, 0x15);
        assertNotEquals(0, session.activeRoom().shutterDoorMask());
        var hazardsOnly = session.activeRoom().entities().slots().stream()
                .map(entity -> entity.type() == 0x9B ? RoomEntity.disabled(entity.slot()) : entity)
                .toList();
        session.replaceEntityRuntimeForTest(RoomEntityRuntime.from(new RoomEntitySnapshot(hazardsOnly), true));
        session.tickEntities(1, 56, 74);
        assertEquals(0, session.activeRoomEventForTest());
        assertEquals(0, session.activeRoom().shutterDoorMask());
        assertEquals(0x10, session.indoorRoomStatusForTest(0, 0x15) & 0x10);
    }

    @Test
    void tailCaveInstrumentUsesItsRomSpriteAndAwardsAtTheRomCountdownBoundary() {
        TransientVfxSystem transientVfx = new TransientVfxSystem(16);
        RoomSession session = newSession(transientVfx);
        session.loadIndoor(0x00, 0x02);
        RoomEntity instrument = session.activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == EntitySpriteHandlerCatalog.ENTITY_INSTRUMENT_OF_THE_SIRENS)
                .findFirst()
                .orElseThrow();
        assertEquals(0x03, instrument.spriteDefinition().bank());
        assertEquals(0x5D83, instrument.spriteDefinition().address());
        assertEquals(4, instrument.spriteDefinition().variantCount());

        session.tickEntities(0, instrument.x(), instrument.y());
        EntityPickupEvent pickup =
                session.collectEntityIfNeeded((instrument.slot() ^ 1) & 1, instrument.x(), instrument.y(), false, true);

        assertNotNull(pickup);
        assertEquals(EntitySpriteHandlerCatalog.ENTITY_INSTRUMENT_OF_THE_SIRENS, pickup.type());
        assertFalse(session.hasDungeonInstrumentForTest(0x00));
        assertEquals(0, session.indoorRoomStatusForTest(0x00, 0x02) & 0x10);
        assertTrue(session.consumeEntityDialogRequests().isEmpty());
        assertEquals(0x1B, session.consumePendingMusicTrack());

        session.tickEntities(1, instrument.x(), instrument.y());
        assertEquals(4, transientVfx.activeCount());

        for (int frame = 2; frame < 88; frame++) {
            session.tickEntities(frame, 0x50, 0x60);
            assertFalse(session.hasDungeonInstrumentForTest(0x00));
            assertTrue(session.consumeEntityDialogRequests().isEmpty());
        }

        session.tickEntities(88, 0x50, 0x60);

        assertTrue(session.hasDungeonInstrumentForTest(0x00));
        assertEquals(0x10, session.indoorRoomStatusForTest(0x00, 0x02) & 0x10);
        assertEquals(0x100, session.consumeEntityDialogRequests().getFirst().globalDialogId());
        RoomEntity heldInstrument = session.activeRoom().entities().slots().get(instrument.slot());
        assertEquals(EntityStatus.ACTIVE, heldInstrument.status());
        assertEquals(0x50, heldInstrument.x());
        assertEquals(0x54, heldInstrument.y());
    }

    @Test
    void tailCaveInstrumentWaitsForDialogBeforePlayingTheCelloTrack() {
        RoomSession session = newSession();
        session.loadIndoor(0x00, 0x02);
        RoomEntity instrument = session.activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == EntitySpriteHandlerCatalog.ENTITY_INSTRUMENT_OF_THE_SIRENS)
                .findFirst()
                .orElseThrow();
        session.tickEntities(0, instrument.x(), instrument.y());
        assertNotNull(session.collectEntityIfNeeded(
                (instrument.slot() ^ 1) & 1, instrument.x(), instrument.y(), false, true));
        assertEquals(0x1B, session.consumePendingMusicTrack());

        for (int frame = 1; frame <= 88; frame++) {
            session.tickEntities(frame, 0x50, 0x60);
        }
        assertEquals(0x100, session.consumeEntityDialogRequests().getFirst().globalDialogId());

        session.setEntityMusicActive(true);
        session.setEntityDialogActive(true);
        session.tickEntities(89, 0x50, 0x60);
        assertEquals(-1, session.consumePendingMusicTrack());

        session.setEntityDialogActive(false);
        session.tickEntities(90, 0x50, 0x60);
        assertEquals(-1, session.consumePendingMusicTrack());

        session.setEntityMusicActive(false);
        session.tickEntities(91, 0x50, 0x60);

        assertEquals(0x20, session.consumePendingMusicTrack());
        assertEquals(0xFF, session.entityTransitionCountdownForTest(instrument.slot()));
    }

    @Test
    void heartPieceFinishesItsRewardWithoutBecomingACarriedObjectAndPersists() {
        RoomSession session = newSession();
        session.loadOverworld(0x44);
        session.tickEntities(0, 72, 80);
        RoomEntity piece = session.activeRoom().entities().loadedEntities().stream()
                .filter(e -> e.type() == 0x35)
                .findFirst()
                .orElseThrow();
        assertNotNull(session.collectEntityIfNeeded((piece.slot() ^ 1) & 1, 72, 80, false, true));
        int pieces = 0;
        boolean completed = false, dialogOpened = false;
        for (int frame = 1; frame < 220 && !completed; frame++) {
            session.setHeartPiecesCount(pieces);
            session.tickEntities(frame, 72, 80);
            assertFalse(session.liftedEntityState().active());
            for (var update : session.consumeHeartPieceUpdates()) {
                if (update.dialogId() == 0x4F) {
                    dialogOpened = true;
                    session.setEntityDialogActive(true);
                }
                if (update.incrementPiece()) pieces++;
                if (dialogOpened && !update.interactionLocked()) session.setEntityDialogActive(false);
                completed |= update.complete();
            }
        }
        assertTrue(completed);
        assertTrue(dialogOpened);
        assertEquals(1, pieces);
        assertFalse(session.heartPiecePickupActive());
        session.loadOverworld(0x43);
        session.loadOverworld(0x44);
        session.tickEntities(240, 72, 80);
        assertTrue(session.activeRoom().entities().loadedEntities().stream().noneMatch(e -> e.type() == 0x35));
    }

    @Test
    void instrumentCyclesOnlyTheTwoRomObjectPaletteColours() {
        RoomSession session = newSession();
        session.loadIndoor(0, 2);
        byte[] rom = loadRom();
        int[][] before = session.renderSnapshot().entities().spriteSelection().objectPalettes();
        session.tickEntities(0, 0x50, 0x70); // Initialize the room entity first.
        for (int frame : new int[] {8, 16, 64, 120}) {
            session.tickEntities(frame, 0x50, 0x70);
            int[][] after =
                    session.renderSnapshot().entities().spriteSelection().objectPalettes();
            int offset = 0x36 * 0x4000 + 0x3096 + ((frame >> 1) & 0x3C);
            for (int c = 0; c < 2; c++) {
                int word = (rom[offset + c * 2] & 255) | ((rom[offset + c * 2 + 1] & 255) << 8);
                assertEquals(linksawakening.rom.RomUtil.decodeRgb555(word), after[1][c + 2]);
            }
            assertEquals(before[1][0], after[1][0]);
            assertEquals(before[1][1], after[1][1]);
            assertEquals(before[2][2], after[2][2]);
        }
    }
}
