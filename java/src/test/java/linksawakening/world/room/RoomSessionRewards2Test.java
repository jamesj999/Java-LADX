package linksawakening.world.room;

import static linksawakening.world.room.RoomSessionTestSupport.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.entity.Link;
import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.state.PlayerState;
import linksawakening.world.entity.EntityBackgroundCollisionResult;
import linksawakening.world.entity.EntityPickupEvent;
import linksawakening.world.entity.EntityRoomLoader;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityRuntime;
import linksawakening.world.entity.RoomEntitySnapshot;
import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.HeartContainerRewardEvent;
import linksawakening.world.entity.event.InstrumentWarpVisual;
import linksawakening.world.transition.Warp;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class RoomSessionRewards2Test {

    @Test
    void tailCaveInstrumentRunsThePerformanceAndWarpCountdownsBeforeReleasingLink() {
        RoomSession session = newSession();
        session.loadIndoor(0x00, 0x02);
        RoomEntity instrument = session.activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == EntitySpriteHandlerCatalog.ENTITY_INSTRUMENT_OF_THE_SIRENS)
                .findFirst()
                .orElseThrow();
        session.tickEntities(0, instrument.x(), instrument.y());
        assertNotNull(session.collectEntityIfNeeded(
                (instrument.slot() ^ 1) & 1, instrument.x(), instrument.y(), false, true));
        session.context.entityBridge.outbox.consumePendingMusicTrack();
        for (int frame = 1; frame <= 88; frame++) {
            session.tickEntities(frame, 0x50, 0x60);
        }
        session.consumeEntityDialogRequests();
        session.tickEntities(89, 0x50, 0x60);
        assertEquals(0x20, session.context.entityBridge.outbox.consumePendingMusicTrack());

        session.tickEntities(90, 0x50, 0x60);
        RoomEntity instrumentShard = session.activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.slot() != instrument.slot()
                        && entity.type() == EntitySpriteHandlerCatalog.ENTITY_INSTRUMENT_OF_THE_SIRENS)
                .findFirst()
                .orElseThrow();
        assertEquals(0x03, instrumentShard.spriteDefinition().bank());
        assertEquals(0x5E8B, instrumentShard.spriteDefinition().address());
        assertEquals(0x4A, instrumentShard.x());
        assertEquals(0x4C, instrumentShard.y());

        for (int frame = 91; frame < 344; frame++) {
            session.tickEntities(frame, 0x50, 0x60);
            assertTrue(session.context.entityBridge.outbox.consumeEntityEvents().isEmpty());
        }
        session.tickEntities(344, 0x50, 0x60);

        EntityCombatEvent warpJingle =
                session.context.entityBridge.outbox.consumeEntityEvents().getFirst();
        assertEquals(EntityCombatEvent.SoundChannel.JINGLE, warpJingle.soundChannel());
        assertEquals(0x2B, warpJingle.soundId());
        assertEquals(
                EntityStatus.ACTIVE,
                session.activeRoom().entities().slots().get(instrument.slot()).status());

        for (int frame = 345; frame < 832; frame++) {
            session.tickEntities(frame, 0x50, 0x60);
            assertTrue(session.consumeEntityDialogRequests().isEmpty());
        }
        session.tickEntities(832, 0x50, 0x60);
        assertEquals(0x150, session.consumeEntityDialogRequests().getFirst().globalDialogId());
        InstrumentWarpVisual visual = session.renderSnapshot().instrumentWarp();
        assertEquals(31, visual.backgroundFade());
        assertEquals(31, visual.objectFade());
        assertEquals(9, visual.commandBatches());
        assertEquals(0x4F, visual.centerX());
        assertEquals(0x54, visual.centerY());
        assertEquals(256, visual.swirl().length);
        assertEquals(0x60, visual.swirl()[2]);
        assertEquals(0xD8, visual.swirl()[1]);
        assertEquals(0x7F, visual.tiles()[359]);
        assertEquals(1, visual.attributes()[359]); // Shipped ROM uses palette 1.
        session.setEntityDialogActive(true);
        for (int frame = 833; frame <= 932; frame++) {
            session.tickEntities(frame, 0x50, 0x60);
            assertNull(session.transitions().consumeInstrumentTransitionRequest());
            assertTrue(session.consumeEntityDialogRequests().isEmpty());
        }
        assertEquals(
                EntityStatus.ACTIVE,
                session.activeRoom().entities().slots().get(instrument.slot()).status());
        session.setEntityDialogActive(false);
        for (int frame = 933; frame < 956; frame++) {
            session.tickEntities(frame, 0x50, 0x60);
            assertNull(session.transitions().consumeInstrumentTransitionRequest());
        }
        session.tickEntities(956, 0x50, 0x60);

        assertFalse(
                session.activeRoom().entities().slots().get(instrument.slot()).loaded());
        assertTrue(session.context
                .entityBridge
                .outbox
                .consumeLinkMotionBlockRequests()
                .isEmpty());
        assertEquals(session.activeRoom().firstWarp(), session.transitions().consumeInstrumentTransitionRequest());
        assertEquals(0x80, session.bowWowState());
        assertEquals(9, session.renderSnapshot().instrumentWarp().commandBatches());
        session.loadOverworld(0xD3);
        assertNull(session.renderSnapshot().instrumentWarp());
    }

    @Test
    void bottleGrottoBossClearOpensTheInstrumentRoomAndAwardsTheConchHorn() {
        RoomSession session = newSession();
        session.loadIndoor(0x01, 0x2B);
        assertEquals(0x21, session.context.roomSessionViews.activeRoomEventForTest());
        assertNotEquals(0, session.activeRoom().shutterDoorMask());

        List<RoomEntity> emptySlots = new ArrayList<>();
        for (int slot = 0; slot < EntityRoomLoader.MAX_ENTITIES; slot++) {
            emptySlots.add(RoomEntity.disabled(slot));
        }
        session.context.roomSessionViews.replaceEntityRuntimeForTest(
                RoomEntityRuntime.from(new RoomEntitySnapshot(emptySlots), true));
        session.tickEntities(0, 0x50, 0x50);

        assertEquals(0, session.context.roomSessionViews.activeRoomEventForTest());
        assertEquals(0, session.activeRoom().shutterDoorMask());

        session.loadIndoor(0x01, 0x2A);
        assertEquals(0x21, session.context.roomSessionViews.activeRoomEventForTest());
        RoomEntity instrument = session.activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == EntitySpriteHandlerCatalog.ENTITY_INSTRUMENT_OF_THE_SIRENS)
                .findFirst()
                .orElseThrow();
        Warp exit = session.activeRoom().firstWarp();
        assertEquals(Warp.CATEGORY_OVERWORLD, exit.category());
        assertEquals(0x00, exit.destMap());
        assertEquals(0x24, exit.destRoom());
        assertEquals(0x38, exit.destX());
        assertEquals(0x22, exit.destY());

        session.tickEntities(1, instrument.x(), instrument.y());
        assertNotNull(session.collectEntityIfNeeded(
                (instrument.slot() ^ 1) & 1, instrument.x(), instrument.y(), false, true));
        assertEquals(0x1B, session.context.entityBridge.outbox.consumePendingMusicTrack());
        for (int frame = 2; frame <= 88; frame++) {
            session.tickEntities(frame, 0x50, 0x60);
        }
        session.tickEntities(89, 0x50, 0x60);

        assertTrue(session.context.roomSessionViews.hasDungeonInstrumentForTest(0x01));
        assertEquals(0x10, session.context.roomSessionViews.indoorRoomStatusForTest(0x01, 0x2A) & 0x10);
        assertEquals(0x101, session.consumeEntityDialogRequests().getFirst().globalDialogId());
        session.tickEntities(90, 0x50, 0x60);
        assertEquals(0x28, session.context.entityBridge.outbox.consumePendingMusicTrack());

        Warp completedExit = null;
        for (int frame = 91; frame <= 900 && completedExit == null; frame++) {
            session.tickEntities(frame, 0x50, 0x60);
            completedExit = session.transitions().consumeInstrumentTransitionRequest();
        }
        assertEquals(exit, completedExit);
        assertEquals(0x02, session.tarinFlag());
    }

    @Test
    void bottleGrottoGenieDeathHeartPickupPersistsBossRoomCompletion() {
        RoomSession session = newSession();
        session.loadIndoor(0x01, 0x2B);
        List<RoomEntity> dyingBossSlots = new ArrayList<>();
        dyingBossSlots.add(new RoomEntity(
                0,
                0,
                0x5C,
                0x50,
                0x40,
                EntityStatus.DYING,
                EntitySpriteDefinition.unsupported(0x5C),
                0,
                0,
                0,
                0,
                -1,
                false));
        for (int slot = 1; slot < EntityRoomLoader.MAX_ENTITIES; slot++) {
            dyingBossSlots.add(RoomEntity.disabled(slot));
        }
        RoomEntityRuntime runtime = RoomEntityRuntime.from(new RoomEntitySnapshot(dyingBossSlots), true);
        linksawakening.world.entity.EntityRuntimeTestAccess.setGeniePrivateState1ForTest(runtime, 0, 1);
        session.context.roomSessionViews.replaceEntityRuntimeForTest(runtime);

        session.tickEntities(0, 0x50, 0x50);
        linksawakening.world.entity.EntityRuntimeTestAccess.setTransitionCountdownForTest(runtime, 0, 1);
        session.tickEntities(1, 0x50, 0x50);
        linksawakening.world.entity.EntityRuntimeTestAccess.setTransitionCountdownForTest(runtime, 0, 1);
        session.tickEntities(2, 0x50, 0x50);

        RoomEntity heart = session.activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x36)
                .findFirst()
                .orElseThrow();
        EntityPickupEvent pickup =
                session.collectEntityIfNeeded((heart.slot() ^ 1) & 1, heart.x(), heart.y(), false, true);
        assertNotNull(pickup);
        assertEquals(0x36, pickup.type());

        for (int frame = 3; frame < 0x80; frame++) {
            session.tickEntities(frame, 0x50, 0x50);
        }

        assertEquals(0x20, session.context.roomSessionViews.indoorRoomStatusForTest(0x01, 0x2B) & 0x20);
        assertEquals(
                List.of(new HeartContainerRewardEvent(heart.slot())),
                session.context.entityBridge.outbox.consumeHeartContainerRewards());
    }

    @Test
    void keyCavernKeyholeRequiresTheSlimeKeyAndRunsTheSharedRumbleSequence() {
        RoomSession session = newSession();
        List<GameplaySoundEvent> sounds = new ArrayList<>();
        session.setColorShellSoundSink(sounds::add);
        session.loadInitialOverworld(0xB5);
        int gateIndex = RoomConstants.ROOM_OBJECTS_BASE + 0x16;

        assertEquals(0xC2, session.activeRoom().roomObjectsArea()[gateIndex]);
        assertTrue(session.tryUnlockKeyCavernKeyhole(0x5A, 0x4A, Link.DIRECTION_UP, 0x01, 5));
        assertEquals(0x231, session.consumeEntityDialogRequests().getFirst().globalDialogId());
        assertEquals(0, session.context.roomSessionViews.overworldRoomStatusForTest(0xB5) & 0x10);

        assertTrue(session.tryUnlockKeyCavernKeyhole(0x5A, 0x4A, Link.DIRECTION_UP, 0x01, 6));
        assertEquals(0x10, session.context.roomSessionViews.overworldRoomStatusForTest(0xB5) & 0x10);
        assertEquals(0xDF, session.context.roomDoorInteractions.tailCaveKeyholeCountdownForTest());
        int gateTopLeft = 2 * RoomConstants.ROOM_TILE_WIDTH + 12;
        while (session.context.roomDoorInteractions.tailCaveKeyholeCountdownForTest() > 0xD8) {
            session.tickTailCaveKeyholeSequence();
        }
        assertEquals(0x0C, session.activeRoom().tileIds()[gateTopLeft]);
        assertEquals(0x1C, session.activeRoom().tileIds()[gateTopLeft + RoomConstants.ROOM_TILE_WIDTH]);
        while (session.context.roomDoorInteractions.tailCaveKeyholeCountdownForTest() > 0xC8) {
            session.tickTailCaveKeyholeSequence();
        }
        assertEquals(0x0C, session.activeRoom().tileIds()[gateTopLeft]);
        while (session.tailCaveKeyholeSequenceActive()) {
            session.tickTailCaveKeyholeSequence();
        }

        assertEquals(0xE3, session.activeRoom().roomObjectsArea()[gateIndex]);
        assertEquals(
                List.of(
                        GameplaySoundEvent.DOOR_UNLOCKED,
                        GameplaySoundEvent.OPEN_KEY_CAVERN,
                        GameplaySoundEvent.DUNGEON_OPENED),
                sounds);
        Warp entrance = session.activeRoom().warps().stream()
                .filter(warp -> warp.destMap() == 0x02 && warp.destRoom() == 0x52)
                .findFirst()
                .orElseThrow();
        assertEquals(0x16, entrance.tileLocation());
        assertEquals(0x02, entrance.destMap());
        assertEquals(0x52, entrance.destRoom());
    }

    @Test
    void mysteriousForestChestFeedsTheCanonicalTailCaveUnlock() {
        RoomSession session = newSession();
        PlayerState playerState = new PlayerState();
        session.loadInitialOverworld(0x41);

        RoomSession.ChestOpenResult chest =
                session.tryOpenChest(0x38, 0x31, Link.DIRECTION_UP, true, playerState.swordLevel());
        assertTrue(chest.opened());
        assertEquals(ChestContentsTable.CHEST_TAIL_KEY, chest.itemType());
        session.tickEntities(0, 0x40, 0x40);
        session.context
                .entityBridge
                .outbox
                .consumeChestRewardEvents()
                .forEach(reward -> playerState.applyChestReward(reward.itemType()));
        assertEquals(1, playerState.tailKeyCount());
        session.setTailKeyOwned(playerState.tailKeyCount() != 0);

        int frame = 1;
        boolean owlDialogOpened = false;
        while (!owlDialogOpened && frame < 0x300) {
            tickInteractive(session, frame++, 0x50, 0x50);
            owlDialogOpened |= session.consumeEntityDialogRequests().stream()
                    .anyMatch(request -> request.globalDialogId() == 0x0C1);
        }
        assertTrue(owlDialogOpened);
        while ((session.context.roomSessionViews.overworldRoomStatusForTest(0x41) & 0x20) == 0 && frame < 0x340) {
            tickInteractive(session, frame++, 0x50, 0x50);
        }
        assertEquals(0x20, session.context.roomSessionViews.overworldRoomStatusForTest(0x41) & 0x20);

        session.loadInitialOverworld(0x41);
        assertEquals(0x20, session.context.roomSessionViews.overworldRoomStatusForTest(0x41) & 0x20);
        assertFalse(
                session.activeRoom().entities().loadedEntities().stream().anyMatch(entity -> entity.type() == 0x41));

        session.loadInitialOverworld(0xD3);
        assertTrue(
                session.tryUnlockTailCaveKeyhole(0x5A, 0x4A, Link.DIRECTION_UP, 0x01, playerState.tailKeyCount() != 0));
        assertEquals(0x10, session.context.roomSessionViews.overworldRoomStatusForTest(0xD3) & 0x10);
    }

    @Test
    void liveEntityCollisionProbeUsesRomSwitchBlockStateAndObjectKind() {
        RoomSession session = newSession();
        session.loadIndoor(0x00, 0x0F);
        RoomEntity entity = session.activeRoom().entities().loadedEntities().stream()
                .filter(candidate -> candidate.type() == 0x1E)
                .findFirst()
                .orElseThrow();

        fillActiveObjects(session, 0xDB);
        session.context.roomSessionViews.setEntitySwitchBlocksStateForTest(0x00);
        EntityBackgroundCollisionResult lower =
                session.context.roomCollisionQueries.entityBackgroundCollisionResultForTest(
                        entity, EntityBackgroundCollisionResult.RIGHT, entity.x(), entity.y());
        assertFalse(lower.blocked());
        assertEquals(0xDB, lower.objectId());
        assertEquals(0x04, lower.physicsFlag());

        fillActiveObjects(session, 0xDC);
        EntityBackgroundCollisionResult raisedStateZero =
                session.context.roomCollisionQueries.entityBackgroundCollisionResultForTest(
                        entity, EntityBackgroundCollisionResult.RIGHT, entity.x(), entity.y());
        assertTrue(raisedStateZero.blocked());

        session.context.roomSessionViews.setEntitySwitchBlocksStateForTest(0x02);
        EntityBackgroundCollisionResult raisedStateTwo =
                session.context.roomCollisionQueries.entityBackgroundCollisionResultForTest(
                        entity, EntityBackgroundCollisionResult.RIGHT, entity.x(), entity.y());
        assertFalse(raisedStateTwo.blocked());
    }

    @Test
    void gameplayVblankAdvancesTheRoomOwnedSwitchStageBeforeOrdinaryAnimations() {
        RoomSession session = newSession();
        session.context.roomSessionViews.setEntitySwitchBlocksStateForTest(0x00);
        session.context.roomSessionViews.setSwitchableObjectAnimationStageForTest(0x02);

        session.tickGameplayVBlank();

        assertEquals(0x03, session.context.roomSessionViews.switchableObjectAnimationStageForTest());
        assertEquals(0x02, session.context.roomSessionViews.entitySwitchBlocksStateForTest());

        session.context.roomSessionViews.setSwitchableObjectAnimationStageForTest(0x09);
        session.tickGameplayVBlank();

        assertEquals(0x00, session.context.roomSessionViews.switchableObjectAnimationStageForTest());
        assertEquals(0x02, session.context.roomSessionViews.entitySwitchBlocksStateForTest());
    }

    @Test
    void shippedCrystalRoomCarriesAHitIntoTheRoomOwnedSwitchStage() {
        RoomSession session = newSession();
        session.loadIndoor(0x00, 0x22);
        RoomEntity crystal = session.activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x66)
                .findFirst()
                .orElseThrow();
        session.tickEntities(0, 0, 0);

        List<EntityCombatEvent> hit =
                session.resolveEntityCombat(1, 0, 0, false, false, true, crystal.x() + 0x08, 1, crystal.y() + 0x08, 1);

        assertEquals(1, hit.size());
        assertEquals(0x66, hit.get(0).type());
        session.tickEntities(1, 0, 0);

        assertEquals(0x01, session.context.roomSessionViews.switchableObjectAnimationStageForTest());
        assertEquals(
                List.of(new EntityCombatEvent(
                        crystal.slot(), 0x66, 0, false, EntityCombatEvent.SoundChannel.WAVE, 0x0E)),
                session.context.entityBridge.outbox.consumeEntityEvents());
    }

    @Test
    void collectsAStaticPickupThroughTheActiveRoomSessionAndPersistsIt() {
        RoomSession session = newSession();
        session.loadInitialOverworld(0xA4);

        RoomEntity pickup = session.activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x3D)
                .findFirst()
                .orElseThrow();
        // Room A4 is one of the two ROM tree-shell rooms. Its shell remains
        // hidden until the Pegasus-Boots collision branch fires.
        session.setSecretSeashellPegasusCollisionState(
                true, true, (pickup.x() + 0x08) & 0xFF, (pickup.y() + 0x08) & 0xFF);
        session.tickEntities(0);
        session.tickEntities(1);
        // DroppableRevealOrReturnIfNeeded gives a revealed shell private
        // countdown $18, and PickableCollectIfNeeded refuses collection until
        // that countdown reaches zero.
        for (int frame = 2; frame <= 0x19; frame++) {
            session.tickEntities(frame);
        }
        pickup = session.activeRoom().entities().slots().get(pickup.slot());
        int collectionFrame = (pickup.slot() & 1) == 0 ? 1 : 0;

        EntityPickupEvent event = session.collectEntityIfNeeded(collectionFrame, pickup.x(), pickup.y(), false, true);

        assertNotNull(event);
        assertEquals(pickup.slot(), event.slot());
        assertEquals(
                EntityStatus.DISABLED,
                session.activeRoom().entities().slots().get(pickup.slot()).status());
        assertEquals(0x10, session.context.roomSessionViews.overworldRoomStatusForTest(0xA4) & 0x10);
        assertTrue(session.consumeEntityDialogRequests().contains(new DialogRequest(0, 0xEF)));
        assertTrue(session.context.entityBridge.outbox.consumeEntityEvents().stream()
                .anyMatch(soundEvent -> soundEvent.type() == 0x3D
                        && soundEvent.soundChannel() == EntityCombatEvent.SoundChannel.WAVE
                        && soundEvent.soundId() == 0x01));

        session.loadOverworld(0xA4);

        assertEquals(0xAE, session.activeRoom().entities().slots().get(0).type());
    }
}
