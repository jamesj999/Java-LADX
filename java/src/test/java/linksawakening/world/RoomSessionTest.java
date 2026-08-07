package linksawakening.world;

import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.entity.Link;
import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.gpu.GPU;
import linksawakening.physics.OverworldCollision;
import linksawakening.physics.PhysicsFlags;
import linksawakening.rom.RomBank;
import linksawakening.rom.RomTables;
import linksawakening.vfx.TransientVfxSystem;
import linksawakening.vfx.TransientVfxType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RoomSessionTest {

    @Test
    void loadsInitialOverworldRoomIntoActiveRoomState() {
        RoomSession session = newSession();

        session.loadInitialOverworld(0x92);

        assertTrue(session.hasActiveRoom());
        assertEquals(0x92, session.currentRoomId());
        assertEquals(Warp.CATEGORY_OVERWORLD, session.mapCategory());
        assertNotNull(session.activeRoom().roomObjectsArea());
        assertEquals(0x92, session.activeRoom().entities().spriteSelection().roomId());
        assertNotNull(session.activeRoom().entities().spriteTiles());
        assertNotNull(session.renderSnapshot());
    }

    @Test
    void adjacentOverworldScrollLoadsNextRoomAndStartsScroll() {
        RoomSession session = newSession();
        ScrollController scrollController = new ScrollController();
        session.loadInitialOverworld(0x92);
        var previousEntities = session.activeRoom().entities();

        session.startAdjacentOverworldScroll(scrollController, ScrollController.RIGHT, 161, 40);

        assertEquals(0x93, session.currentRoomId());
        assertNotSame(previousEntities, session.activeRoom().entities());
        assertNotSame(previousEntities.spriteTiles(), session.activeRoom().entities().spriteTiles());
        assertEquals(0x93, session.activeRoom().entities().spriteSelection().roomId());
        assertEquals(previousEntities, scrollController.previousRoom().entities());
        assertNotNull(scrollController.previousRoom().entities().spriteTiles());
        assertTrue(scrollController.isActive());
        assertEquals(ScrollController.RIGHT, scrollController.direction());
        assertNotNull(scrollController.previousRoom());
    }

    @Test
    void notifiesListenerAfterRoomLoads() {
        List<Integer> loadedRoomIds = new ArrayList<>();
        RoomSession session = newSession(room -> loadedRoomIds.add(room.roomId()));
        ScrollController scrollController = new ScrollController();

        session.loadInitialOverworld(0x92);
        session.startAdjacentOverworldScroll(scrollController, ScrollController.RIGHT, 161, 40);

        assertEquals(List.of(0x92, 0x93), loadedRoomIds);
    }

    @Test
    void ticksTheLoadedEntityRuntimeAndRefreshesTheRenderSnapshot() {
        RoomSession session = newSession();
        session.loadInitialOverworld(0x92);

        session.tickEntities(0);

        assertEquals(EntityStatus.ACTIVE, session.activeRoom().entities().slots().get(0).status());
        assertEquals(0x6E, session.activeRoom().entities().slots().get(3).type());
        assertEquals(1, session.activeRoom().entities().slots().get(3).spriteVariant());
        assertEquals(session.activeRoom().entities(), session.renderSnapshot().entities());
    }

    @Test
    void forwardsLiveOcarinaPlaybackToAPolsVoiceEntity() {
        RoomSession session = newSession();
        session.loadIndoor(0x00, 0x25);
        RoomEntity polsVoice = session.activeRoom().entities().loadedEntities().stream()
            .filter(entity -> entity.type() == 0x18)
            .findFirst()
            .orElseThrow();

        session.tickEntities(0, polsVoice.x(), polsVoice.y(), polsVoice.z(), 0);
        polsVoice = session.activeRoom().entities().loadedEntities().stream()
            .filter(entity -> entity.type() == 0x18)
            .findFirst()
            .orElseThrow();

        assertTrue(session.startOcarina(0x01, 0x04, 0x00));
        assertTrue(session.ocarinaPlaying());
        session.tickEntities(1, polsVoice.x(), polsVoice.y(), polsVoice.z(), 0);

        RoomEntity dyingPolsVoice = session.activeRoom().entities().slots().get(polsVoice.slot());
        assertEquals(EntityStatus.DYING, dyingPolsVoice.status());
        assertFalse(session.ocarinaPlaying());
        assertEquals(0x13, session.consumeEntityEvents().stream()
            .filter(event -> event.type() == 0x18)
            .findFirst().orElseThrow().soundId());
    }

    @Test
    void forwardsPerFrameLinkCollisionTypeToGhiniAndOldOverloadDefaultsToZero() {
        RoomSession session = newSession();
        session.loadInitialOverworld(0x67);
        RoomEntity hiddenGhini = session.activeRoom().entities().loadedEntities().stream()
            .filter(entity -> entity.type() == 0x10)
            .findFirst()
            .orElseThrow();

        session.tickEntities(0, hiddenGhini.x(), hiddenGhini.y(), hiddenGhini.z(), 0);
        RoomEntity initialized = session.activeRoom().entities().slots().get(hiddenGhini.slot());

        session.tickEntitiesWithProjectileEvents(
            1, initialized.x(), initialized.y(), initialized.z(), 0, 0, 0x01,
            false, 1, 0, false, 0, 0, 0, 0);
        RoomEntity revealTick = session.activeRoom().entities().slots().get(hiddenGhini.slot());
        assertEquals(-1, revealTick.spriteVariant());

        session.tickEntitiesWithProjectileEvents(
            2, revealTick.x(), revealTick.y(), revealTick.z(), 0, 0,
            false, 1, 0, false, 0, 0, 0, 0);
        RoomEntity visibleTick = session.activeRoom().entities().slots().get(hiddenGhini.slot());
        assertTrue(visibleTick.spriteVariant() >= 0);

        RoomSession oldOverloadSession = newSession();
        oldOverloadSession.loadInitialOverworld(0x67);
        RoomEntity oldOverloadGhini = oldOverloadSession.activeRoom().entities().loadedEntities()
            .stream()
            .filter(entity -> entity.type() == 0x10)
            .findFirst()
            .orElseThrow();
        oldOverloadSession.tickEntities(
            0, oldOverloadGhini.x(), oldOverloadGhini.y(), oldOverloadGhini.z(), 0);
        oldOverloadSession.tickEntitiesWithProjectileEvents(
            1, oldOverloadGhini.x(), oldOverloadGhini.y(), oldOverloadGhini.z(), 0, 0,
            false, 1, 0, false, 0, 0, 0, 0);

        assertEquals(-1, oldOverloadSession.activeRoom().entities().slots()
            .get(oldOverloadGhini.slot()).spriteVariant());
    }

    @Test
    void hookshotLaunchPublishesTheRomProjectileAndHonorsFireGuards() {
        RoomSession session = newSession();
        session.loadInitialOverworld(0x92);

        assertFalse(session.fireHookshot(0x40, 0x50, 0, 0, true, false));
        assertFalse(session.hookshotActive());
        assertFalse(session.fireHookshot(0x40, 0x50, 0, 0, false, true));
        assertFalse(session.hookshotActive());

        assertTrue(session.fireHookshot(0x40, 0x50, 0, 0, false, false));
        assertTrue(session.hookshotActive());
        assertFalse(session.fireHookshot(0x40, 0x50, 0, 0, false, false));
        assertEquals(0x03, session.activeRoom().entities().loadedEntities().stream()
            .filter(entity -> entity.type() == 0x03)
            .findFirst().orElseThrow().type());
    }

    @Test
    void bombPlacementPublishesRomCoordinatesThroughTheCanonicalBridge() {
        RoomSession session = newSession();

        assertFalse(session.placeBomb(0x40, 0x50, 0x07, 3));
        session.loadInitialOverworld(0x92);

        assertTrue(session.placeBomb(0x40, 0x50, 0x07, 3));
        assertTrue(session.bombActive());

        RoomEntity bomb = session.activeRoom().entities().loadedEntities().stream()
            .filter(entity -> entity.type() == 0x02)
            .findFirst().orElseThrow();
        assertEquals(0x40, bomb.x());
        assertEquals(0x54, bomb.y());
        assertEquals(0x08, bomb.z());
        assertEquals(3, session.bombDirectionForTest(bomb.slot()));
        assertEquals(session.activeRoom().entities(), session.renderSnapshot().entities());

        assertFalse(session.placeBomb(0x44, 0x54, 0, 1));
        assertEquals(1, session.activeRoom().entities().loadedEntities().stream()
            .filter(entity -> entity.type() == 0x02).count());
    }

    @Test
    void openingAClosedChestMutatesTheRoomAndSpawnsTheRomChestEntity() {
        RoomSession session = newSession();
        session.loadIndoor(0x00, 0x20);
        int location = 0x22;
        int areaIndex = RoomConstants.ROOM_OBJECTS_BASE
            + (location & 0xF0) + (location & 0x0F);
        session.activeRoom().roomObjectsArea()[areaIndex] = 0xA0;

        RoomSession.ChestOpenResult result = session.tryOpenChest(
            0x18, 0x21, linksawakening.entity.Link.DIRECTION_UP, true, 1);

        assertTrue(result.opened());
        assertEquals(0xA1, session.activeRoom().roomObjectsArea()[areaIndex]);
        assertTrue(session.activeRoom().renderValues() == null);
        assertEquals(0x10, session.indoorRoomStatusForTest(0x00, 0x20) & 0x10);
        RoomEntity chest = session.activeRoom().entities().loadedEntities().stream()
            .filter(entity -> entity.type() == EntitySpriteHandlerCatalog.ENTITY_CHEST_WITH_ITEM)
            .findFirst().orElseThrow();
        assertEquals(EntityStatus.INIT, chest.status());
        assertEquals(0x28, chest.x());
        assertEquals(0x30, chest.y());
        assertEquals(result.itemType(), chest.spriteVariant());
    }

    @Test
    void consumesBombExplosionObjectWindowThroughTheDedicatedSessionSeam() {
        RoomSession session = newSession();
        session.loadInitialOverworld(0x92);
        assertTrue(session.placeBomb(0x40, 0x50, 0, 0));
        int bombSlot = session.activeRoom().entities().loadedEntities().stream()
            .filter(entity -> entity.type() == 0x02)
            .findFirst().orElseThrow().slot();

        for (int frame = 0; frame < 136; frame++) {
            int currentFrame = frame;
            session.tickEntities(frame, 0, 0);
            List<BombExplosionEvent> events = session.consumeBombExplosionEvents();
            assertTrue(events.isEmpty(),
                () -> "frame=" + currentFrame + " countdown="
                    + session.entityTransitionCountdownForTest(bombSlot)
                    + " events=" + events);
        }
        session.tickEntities(136, 0, 0);

        List<BombExplosionEvent> events = session.consumeBombExplosionEvents();
        assertEquals(1, events.size());
        assertTrue(events.getFirst().targetsRoomObjects());
        assertEquals(0x16, events.getFirst().countdown());
        assertEquals(BombExplosionEvent.DAMAGE_TYPE_BOMB, events.getFirst().damageType());
        assertTrue(session.consumeBombExplosionEvents().isEmpty());
    }

    @Test
    void bombExplosionRevealsAnOverworldBushAtTheRomBasicCandidateCell() {
        TransientVfxSystem vfx = new TransientVfxSystem(16);
        RoomSession session = newSession(vfx);
        session.loadInitialOverworld(0x92);

        int location = 0x55;
        int areaIndex = RoomConstants.ROOM_OBJECTS_BASE
            + (location & 0xF0) + (location & 0x0F);
        session.activeRoom().roomObjectsArea()[areaIndex] = 0x5C;
        session.activeRoom().renderValues()[areaIndex] = 0x5C;

        assertTrue(session.placeBomb(0x40, 0x50, 0, 0));
        for (int frame = 0; frame <= 136; frame++) {
            session.tickEntities(frame, 0, 0);
        }

        assertEquals(0x04, session.activeRoom().roomObjectsArea()[areaIndex]);
        assertEquals(0x04, session.activeRoom().renderValues()[areaIndex]);
        assertEquals(0, vfx.activeCount());
        assertTrue(session.consumeBombExplosionEvents().stream()
            .anyMatch(event -> event.targetsRoomObjects() && event.countdown() == 0x16));
    }

    @Test
    void magicRodBurnsAnOverworldBushThroughTheLiveRoomBoundary() {
        TransientVfxSystem vfx = new TransientVfxSystem(16);
        RoomSession session = newSession(vfx);
        session.loadInitialOverworld(0x92);

        int location = 0x55;
        int areaIndex = RoomConstants.ROOM_OBJECTS_BASE
            + (location & 0xF0) + (location & 0x0F);
        session.activeRoom().roomObjectsArea()[areaIndex] = 0x5C;
        session.activeRoom().renderValues()[areaIndex] = 0x5C;

        assertTrue(session.fireMagicRodFireball(0x50, 0x58, 0, 0));
        session.tickEntitiesWithProjectileEvents(0, 0x50, 0x58, 0, 0, 0, false);

        assertEquals(0x04, session.activeRoom().roomObjectsArea()[areaIndex]);
        assertEquals(0x04, session.activeRoom().renderValues()[areaIndex]);
        assertEquals(1, vfx.activeCount());
        assertTrue(session.consumeEntityEvents().stream()
            .anyMatch(event -> event.type() == 0x04
                && event.soundChannel() == EntityCombatEvent.SoundChannel.NOISE
                && event.soundId() == 0x13));
    }

    @Test
    void magicRodBurnsAnIndoorFrozenBlockIntoTheSideViewEmptyObject() {
        RoomSession session = newSession();
        session.loadIndoor(0x00, 0x00, Warp.CATEGORY_SIDESCROLL);

        int location = 0x22;
        int areaIndex = RoomConstants.ROOM_OBJECTS_BASE
            + (location & 0xF0) + (location & 0x0F);
        session.activeRoom().roomObjectsArea()[areaIndex] = 0x8A;

        assertTrue(session.fireMagicRodFireball(0x20, 0x28, 0, 0));
        session.tickEntitiesWithProjectileEvents(0, 0x20, 0x28, 0, 0, 0, false);

        assertEquals(0x04, session.activeRoom().roomObjectsArea()[areaIndex]);
        assertTrue(session.consumeEntityEvents().stream()
            .anyMatch(event -> event.type() == 0x04 && event.soundId() == 0x13));
    }

    @Test
    void magicPowderRevealsAnOverworldBushThroughTheLiveRoomBoundary() {
        TransientVfxSystem vfx = new TransientVfxSystem(16);
        RoomSession session = newSession(vfx);
        session.loadInitialOverworld(0x92);

        int location = 0x55;
        int areaIndex = RoomConstants.ROOM_OBJECTS_BASE
            + (location & 0xF0) + (location & 0x0F);
        session.activeRoom().roomObjectsArea()[areaIndex] = 0x5C;
        session.activeRoom().renderValues()[areaIndex] = 0x5C;

        // The source entity cell is computed from entity X/Y as
        // ((x - 1) & F0, (y - 9) & F0); these Link coordinates place it on
        // room cell $55 after the ROM right-facing +$0E offset.
        assertTrue(session.sprinkleMagicPowder(0x43, 0x59, 0, 0));
        for (int frame = 0; frame < 16; frame++) {
            session.tickEntitiesWithProjectileEvents(frame, 0x20, 0x20,
                0, 0, 0, false);
        }

        assertEquals(0x04, session.activeRoom().roomObjectsArea()[areaIndex]);
        assertEquals(0x04, session.activeRoom().renderValues()[areaIndex]);
        assertEquals(1, vfx.activeCount());
        assertTrue(session.consumeEntityEvents().stream()
            .anyMatch(event -> event.type() == 0x08
                && event.soundChannel() == EntityCombatEvent.SoundChannel.JINGLE
                && event.soundId() == 0x2F));
    }

    @Test
    void magicPowderIgnitesAnIndoorTorchThroughTheLiveRoomBoundary() {
        RoomSession session = newSession();
        session.loadIndoor(0x00, 0x00);

        int location = 0x22;
        int areaIndex = RoomConstants.ROOM_OBJECTS_BASE
            + (location & 0xF0) + (location & 0x0F);
        session.activeRoom().roomObjectsArea()[areaIndex] = 0xAB;

        assertTrue(session.sprinkleMagicPowder(0x13, 0x29, 0, 0));
        for (int frame = 0; frame < 16; frame++) {
            session.tickEntitiesWithProjectileEvents(frame, 0x20, 0x20,
                0, 0, 0, false);
        }

        assertEquals(0xAC, session.activeRoom().roomObjectsArea()[areaIndex]);
        assertTrue(session.consumeEntityEvents().stream()
            .anyMatch(event -> event.type() == 0x08
                && event.soundChannel() == EntityCombatEvent.SoundChannel.NOISE
                && event.soundId() == 0x12));
    }

    @Test
    void shovelUsesTheAdjacentRomCellAndTurnsItIntoAConfiguredHoleOnTimerTen() {
        RoomSession session = newSession();
        session.loadInitialOverworld(0x92);

        int location = 0x55;
        int areaIndex = RoomConstants.ROOM_OBJECTS_BASE
            + (location & 0xF0) + (location & 0x0F);
        session.activeRoom().roomObjectsArea()[areaIndex] = 0x04;
        session.activeRoom().renderValues()[areaIndex] = 0x04;

        RoomSession.ShovelStartResult start = session.startShovel(0x44, 0x56, 0, false);

        assertTrue(start.started());
        assertFalse(start.poking());
        assertEquals(0x55, start.location());
        assertEquals(0x71, session.shovelAnimationState(Link.DIRECTION_RIGHT, 1));
        assertEquals(0x72, session.shovelAnimationState(Link.DIRECTION_RIGHT, 0x10));

        for (int timer = 1; timer <= 15; timer++) {
            session.advanceShovel(0x44, 0x56, 0, timer);
        }
        assertEquals(0x04, session.activeRoom().roomObjectsArea()[areaIndex]);

        session.advanceShovel(0x44, 0x56, 0, 0x10);

        assertEquals(0xCC, session.activeRoom().roomObjectsArea()[areaIndex]);
        assertEquals(0xCC, session.activeRoom().renderValues()[areaIndex]);
    }

    @Test
    void shovelKeepsTheSourcePokePathForBlockedIndoorObjects() {
        RoomSession session = newSession();
        session.loadIndoor(0x00, 0x00);

        int location = 0x22;
        int areaIndex = RoomConstants.ROOM_OBJECTS_BASE
            + (location & 0xF0) + (location & 0x0F);
        session.activeRoom().roomObjectsArea()[areaIndex] = 0x04;

        RoomSession.ShovelStartResult start = session.startShovel(0x14, 0x26, 0, false);

        assertTrue(start.started());
        assertTrue(start.poking());
        for (int timer = 1; timer <= 0x10; timer++) {
            session.advanceShovel(0x14, 0x26, 0, timer);
        }
        assertEquals(0x04, session.activeRoom().roomObjectsArea()[areaIndex]);
    }

    @Test
    void shovelQueuesTheRomRandomDropAfterTheHoleIsDrawn() {
        RoomSession session = newSession();
        session.loadInitialOverworld(0x92);

        int location = 0x55;
        int areaIndex = RoomConstants.ROOM_OBJECTS_BASE
            + (location & 0xF0) + (location & 0x0F);
        session.activeRoom().roomObjectsArea()[areaIndex] = 0x04;
        session.activeRoom().renderValues()[areaIndex] = 0x04;

        assertTrue(session.startShovel(0x44, 0x56, 0, false).started());
        session.advanceShovel(0x44, 0x56, 0, 0x10);

        // With the ROM seed and rLY policy, frame fourteen produces the
        // source's successful 1/8 gate and selects the heart on its second
        // byte (RRA carry set).
        session.tickEntities(14, 0x44, 0x56);

        RoomEntity drop = session.activeRoom().entities().loadedEntities().stream()
            .filter(entity -> entity.sourceLoadOrder() == -1
                && (entity.type() == 0x2D || entity.type() == 0x2E))
            .findFirst()
            .orElseThrow();
        assertEquals(0x2D, drop.type());
        assertEquals(0x58, drop.x());
        assertEquals(0x60, drop.y());
        assertEquals(0x02, drop.z());
        assertEquals(0x20 - 0x02, session.entityDropSpeedZForTest(drop.slot()));
        assertEquals(0x0C, session.entityDropSpeedXForTest(drop.slot()));
    }

    @Test
    void shovelRequestsMarinsSourceDialogAfterACompletedDig() {
        RoomSession session = newSession();
        session.loadInitialOverworld(0x92);
        session.setFollowingNpcState(
            new FollowingNpcState(false, 0, true, false, 0, 0, false),
            0x44, 0x56, 0, 0, 0);

        int location = 0x55;
        int areaIndex = RoomConstants.ROOM_OBJECTS_BASE
            + (location & 0xF0) + (location & 0x0F);
        session.activeRoom().roomObjectsArea()[areaIndex] = 0x04;
        session.activeRoom().renderValues()[areaIndex] = 0x04;

        assertTrue(session.startShovel(0x44, 0x56, 0, false).started());
        session.advanceShovel(0x44, 0x56, 0, 0x10);
        session.advanceShovel(0x44, 0x56, 0, 0x18);

        assertEquals(List.of(new RoomEntityRuntime.DialogRequest(0, 0x79)),
            session.consumeEntityDialogRequests());
    }

    @Test
    void bombedOverworldBushSpawnsTheRomLiftableRockSmashEntity() {
        RoomSession session = newSession();
        session.loadInitialOverworld(0x92);

        int location = 0x55;
        int areaIndex = RoomConstants.ROOM_OBJECTS_BASE
            + (location & 0xF0) + (location & 0x0F);
        session.activeRoom().roomObjectsArea()[areaIndex] = 0x5C;
        session.activeRoom().renderValues()[areaIndex] = 0x5C;

        assertTrue(session.placeBomb(0x40, 0x50, 0, 0));
        for (int frame = 0; frame <= 136; frame++) {
            session.tickEntities(frame, 0, 0);
        }

        RoomEntity smash = session.activeRoom().entities().loadedEntities().stream()
            .filter(entity -> entity.type() == 0x05)
            .findFirst()
            .orElseThrow();
        assertEquals(0x58, smash.x());
        assertEquals(0x60, smash.y());
        assertEquals(0x06, smash.spriteVariant());
        assertEquals(EntitySpriteDefinition.Shape.DYNAMIC, smash.spriteDefinition().shape());
        assertEquals(0x19, smash.spriteDefinition().bank());
        assertEquals(0x7B50, smash.spriteDefinition().address());
    }

    @Test
    void bombExplosionTurnsTheGiantSkullIntoPersistentRockyGround() {
        RoomSession session = newSession();
        session.loadInitialOverworld(0x97);

        assertTrue(session.placeBomb(0x40, 0x50, 0, 0));
        for (int frame = 0; frame <= 140; frame++) {
            session.tickEntities(frame, 0, 0);
        }

        int[] skullLocations = {0x44, 0x45, 0x54, 0x55};
        for (int location : skullLocations) {
            int areaIndex = RoomConstants.ROOM_OBJECTS_BASE
                + (location & 0xF0) + (location & 0x0F);
            assertEquals(0x09, session.activeRoom().roomObjectsArea()[areaIndex]);
            assertEquals(0x09, session.activeRoom().renderValues()[areaIndex]);
        }
        assertEquals(0x04, session.overworldRoomStatusForTest(0x97));

        session.loadOverworld(0x97);
        for (int location : skullLocations) {
            int areaIndex = RoomConstants.ROOM_OBJECTS_BASE
                + (location & 0xF0) + (location & 0x0F);
            assertEquals(0x09, session.activeRoom().roomObjectsArea()[areaIndex]);
        }
    }

    @Test
    void bombedGiantSkullSpawnsTheRomRubbleGridAndPuzzleJingle() {
        RoomSession session = newSession();
        session.loadInitialOverworld(0x97);

        assertTrue(session.placeBomb(0x40, 0x50, 0, 0));
        boolean destroyed = false;
        for (int frame = 0; frame <= 140; frame++) {
            session.tickEntities(frame, 0, 0);
            if (session.overworldRoomStatusForTest(0x97) == 0x04) {
                destroyed = true;
                break;
            }
        }

        assertTrue(destroyed);
        int[][] expectedPositions = {
            {0x48, 0x4F}, {0x58, 0x4F}, {0x48, 0x5F}, {0x58, 0x5F}
        };
        List<RoomEntity> rubble = session.activeRoom().entities().loadedEntities().stream()
            .filter(entity -> entity.type() == 0x05)
            .toList();
        assertEquals(4, rubble.size());
        for (int[] position : expectedPositions) {
            RoomEntity entity = rubble.stream()
                .filter(candidate -> candidate.x() == position[0]
                    && candidate.y() == position[1])
                .findFirst()
                .orElseThrow();
            assertEquals(0x05, entity.type());
            assertEquals(0x05, entity.spriteVariant());
            assertEquals(EntitySpriteDefinition.Shape.DYNAMIC, entity.spriteDefinition().shape());
            assertEquals(0x19, entity.spriteDefinition().bank());
            assertEquals(0x7B50, entity.spriteDefinition().address());
        }
        List<EntityCombatEvent> events = session.consumeEntityEvents();
        assertEquals(1, events.size());
        assertEquals(EntityCombatEvent.SoundChannel.JINGLE, events.getFirst().soundChannel());
        assertEquals(0x02, events.getFirst().soundId());
    }

    @Test
    void bombExplosionOpensAnOutdoorCaveDoorWithTheRomGbcTilesAndPersistsIt() {
        RoomSession session = newSession();
        session.loadInitialOverworld(0x13);

        int location = 0x05;
        int areaIndex = RoomConstants.ROOM_OBJECTS_BASE
            + (location & 0xF0) + (location & 0x0F);
        assertEquals(0xBA, session.activeRoom().roomObjectsArea()[areaIndex]);
        assertEquals(0xBA, session.activeRoom().renderValues()[areaIndex]);
        assertEquals(location, session.activeRoom().firstWarp().tileLocation());

        assertTrue(session.placeBomb(0x50, 0x18, 0, 0));
        for (int frame = 0; frame <= 146; frame++) {
            session.tickEntities(frame, 0, 0);
        }

        assertEquals(0xE1, session.activeRoom().roomObjectsArea()[areaIndex]);
        assertEquals(0xE1, session.activeRoom().renderValues()[areaIndex]);
        assertEquals(0x04, session.overworldRoomStatusForTest(0x13));
        assertEquals(location, session.activeRoom().firstWarp().tileLocation());

        int tileIndex = 0x0A;
        int[] tileIds = session.activeRoom().tileIds();
        assertEquals(0x64, tileIds[tileIndex]);
        assertEquals(0x66, tileIds[tileIndex + 1]);
        assertEquals(0x64, tileIds[tileIndex + RoomConstants.ROOM_TILE_WIDTH]);
        assertEquals(0x66, tileIds[tileIndex + RoomConstants.ROOM_TILE_WIDTH + 1]);

        List<EntityCombatEvent> events = session.consumeEntityEvents();
        assertEquals(1, events.size());
        assertEquals(EntityCombatEvent.SoundChannel.JINGLE, events.getFirst().soundChannel());
        assertEquals(0x02, events.getFirst().soundId());

        session.loadOverworld(0x13);
        assertEquals(0xE1, session.activeRoom().roomObjectsArea()[areaIndex]);
        assertEquals(0xE1, session.activeRoom().renderValues()[areaIndex]);
        tileIds = session.activeRoom().tileIds();
        assertEquals(0x64, tileIds[tileIndex]);
        // The dedicated BombedCaveDoorTilesIndexesGBC order is an immediate
        // draw command. A later LoadRoomTilemap uses E1's ordinary object
        // table, just as the source does.
        assertEquals(0x64, tileIds[tileIndex + 1]);
        assertEquals(0x66, tileIds[tileIndex + RoomConstants.ROOM_TILE_WIDTH]);
        assertEquals(0x66, tileIds[tileIndex + RoomConstants.ROOM_TILE_WIDTH + 1]);
    }

    @Test
    void bombExplosionReplacesAnIndoorWallAndPersistsTheTwoDoorStatuses() {
        RoomSession session = newSession();
        session.loadIndoor(0x06, 0x18);

        int location = 0x02;
        int areaIndex = RoomConstants.ROOM_OBJECTS_BASE
            + (location & 0xF0) + (location & 0x0F);
        assertEquals(0x3F, session.activeRoom().roomObjectsArea()[areaIndex]);

        assertTrue(session.placeBomb(0x30, 0x18, 0, 0));
        for (int frame = 0; frame <= 146; frame++) {
            session.tickEntities(frame, 0, 0);
        }

        assertEquals(0x3D, session.activeRoom().roomObjectsArea()[areaIndex]);
        assertEquals(0x04, session.indoorRoomStatusForTest(0x06, 0x18));
        assertEquals(0x08, session.indoorRoomStatusForTest(0x06, 0x14));
        List<EntityCombatEvent> events = session.consumeEntityEvents();
        assertEquals(1, events.size());
        assertEquals(EntityCombatEvent.SoundChannel.JINGLE, events.getFirst().soundChannel());
        assertEquals(0x02, events.getFirst().soundId());

        int tileIndex = 0x04;
        int[] tileIds = session.activeRoom().tileIds();
        assertEquals(0x72, tileIds[tileIndex]);
        assertEquals(0x72, tileIds[tileIndex + 1]);
        assertEquals(0x73, tileIds[tileIndex + RoomConstants.ROOM_TILE_WIDTH]);
        assertEquals(0x73, tileIds[tileIndex + RoomConstants.ROOM_TILE_WIDTH + 1]);

        session.loadIndoor(0x06, 0x18);
        assertEquals(0x3D, session.activeRoom().roomObjectsArea()[areaIndex]);
        tileIds = session.activeRoom().tileIds();
        assertEquals(0x72, tileIds[tileIndex]);
        assertEquals(0x73, tileIds[tileIndex + 1]);
        assertEquals(0x72, tileIds[tileIndex + RoomConstants.ROOM_TILE_WIDTH]);
        assertEquals(0x73, tileIds[tileIndex + RoomConstants.ROOM_TILE_WIDTH + 1]);
    }

    @Test
    void bombExplosionUsesTheRomHorizontalIndoorPassageTiles() {
        RoomSession session = newSession();
        session.loadIndoor(0x00, 0x0D);

        int location = 0x30;
        int areaIndex = RoomConstants.ROOM_OBJECTS_BASE
            + (location & 0xF0) + (location & 0x0F);
        assertEquals(0x41, session.activeRoom().roomObjectsArea()[areaIndex]);

        assertTrue(session.placeBomb(0x10, 0x48, 0, 0));
        for (int frame = 0; frame <= 146; frame++) {
            session.tickEntities(frame, 0, 0);
        }

        assertEquals(0x3E, session.activeRoom().roomObjectsArea()[areaIndex]);
        assertEquals(0x02, session.indoorRoomStatusForTest(0x00, 0x0D));
        assertEquals(0x01, session.indoorRoomStatusForTest(0x00, 0x0C));

        int tileIndex = 3 * 2 * RoomConstants.ROOM_TILE_WIDTH;
        int[] tileIds = session.activeRoom().tileIds();
        assertEquals(0x69, tileIds[tileIndex]);
        assertEquals(0x79, tileIds[tileIndex + 1]);
        assertEquals(0x69, tileIds[tileIndex + RoomConstants.ROOM_TILE_WIDTH]);
        assertEquals(0x79, tileIds[tileIndex + RoomConstants.ROOM_TILE_WIDTH + 1]);
    }

    @Test
    void bombExplosionBreaksAnIndoorBombableBlockAndPersistsTheFloor() {
        RoomSession session = newSession();
        session.loadIndoor(0x0A, 0x45);

        int location = 0x22;
        int areaIndex = RoomConstants.ROOM_OBJECTS_BASE
            + (location & 0xF0) + (location & 0x0F);
        assertEquals(0xA9, session.activeRoom().roomObjectsArea()[areaIndex]);

        assertTrue(session.placeBomb(0x30, 0x34, 0, 0));
        for (int frame = 0; frame <= 146; frame++) {
            session.tickEntities(frame, 0, 0);
        }

        assertEquals(0x0D, session.activeRoom().roomObjectsArea()[areaIndex]);
        assertEquals(0x40, session.indoorRoomStatusForTest(0x0A, 0x45));

        int tileIndex = 4 * RoomConstants.ROOM_TILE_WIDTH + 4;
        int[] tileIds = session.activeRoom().tileIds();
        assertEquals(0x10, tileIds[tileIndex]);
        assertEquals(0x12, tileIds[tileIndex + 1]);
        assertEquals(0x11, tileIds[tileIndex + RoomConstants.ROOM_TILE_WIDTH]);
        assertEquals(0x13, tileIds[tileIndex + RoomConstants.ROOM_TILE_WIDTH + 1]);

        RoomEntity smash = session.activeRoom().entities().loadedEntities().stream()
            .filter(entity -> entity.type() == 0x05
                && entity.x() == 0x28 && entity.y() == 0x30)
            .findFirst()
            .orElseThrow();
        assertTrue(smash.spriteVariant() >= 0x02 && smash.spriteVariant() <= 0x05);
        assertEquals(EntitySpriteDefinition.Shape.DYNAMIC, smash.spriteDefinition().shape());

        session.loadIndoor(0x0A, 0x45);
        assertEquals(0x0D, session.activeRoom().roomObjectsArea()[areaIndex]);
        tileIds = session.activeRoom().tileIds();
        assertEquals(0x10, tileIds[tileIndex]);
        assertEquals(0x11, tileIds[tileIndex + 1]);
        assertEquals(0x12, tileIds[tileIndex + RoomConstants.ROOM_TILE_WIDTH]);
        assertEquals(0x13, tileIds[tileIndex + RoomConstants.ROOM_TILE_WIDTH + 1]);
    }

    @Test
    void indoorHookshotBridgeRewritesPaddedObjectsAndBackgroundTiles() {
        RoomSession session = newSession();
        session.loadIndoor(0x00, 0x0F);
        fillActiveObjects(session, 0x9E);

        assertTrue(session.fireHookshot(0x40, 0x40, 0, 2, false, false));
        session.tickEntities(0, 0x40, 0x40, 0, 2);

        int objectIndex = RoomConstants.ROOM_OBJECTS_BASE + 0x30 + (0x30 >>> 4);
        assertEquals(0x9D, session.activeRoom().roomObjectsArea()[objectIndex]);
        RoomEntity bridge = session.activeRoom().entities().loadedEntities().stream()
            .filter(entity -> entity.type() == HookshotBridgeMotion.ENTITY_TYPE)
            .findFirst().orElseThrow();
        assertEquals(0x40, bridge.y());

        int tileIndex = (0x30 >>> 3) * RoomConstants.ROOM_TILE_WIDTH + (0x30 >>> 3);
        int[] tileIds = session.activeRoom().tileIds();
        assertEquals(0x04, tileIds[tileIndex]);
        assertEquals(0x05, tileIds[tileIndex + 1]);
        assertEquals(0x08, tileIds[tileIndex + RoomConstants.ROOM_TILE_WIDTH]);
        assertEquals(0x09, tileIds[tileIndex + RoomConstants.ROOM_TILE_WIDTH + 1]);

        session.tickEntities(1, 0x40, 0x40, 0, 2);
        RoomEntity movedBridge = session.activeRoom().entities().loadedEntities().stream()
            .filter(entity -> entity.type() == HookshotBridgeMotion.ENTITY_TYPE)
            .findFirst().orElseThrow();
        assertEquals(0x43, movedBridge.y());

        session.activeRoom().roomObjectsArea()[objectIndex] = 0x00;
        session.tickEntities(2, 0x40, 0x40, 0, 2);
        assertFalse(session.activeRoom().entities().loadedEntities().stream()
            .anyMatch(entity -> entity.type() == HookshotBridgeMotion.ENTITY_TYPE));
        assertEquals(0x9D, session.activeRoom().roomObjectsArea()[objectIndex]);
        tileIds = session.activeRoom().tileIds();
        assertEquals(0x04, tileIds[tileIndex]);
        assertEquals(0x05, tileIds[tileIndex + 1]);
        assertEquals(0x04, tileIds[tileIndex + RoomConstants.ROOM_TILE_WIDTH]);
        assertEquals(0x05, tileIds[tileIndex + RoomConstants.ROOM_TILE_WIDTH + 1]);
    }

    @Test
    void liveRoomEntityProbePreservesHookshotablePhysicsForTheChain() {
        RoomSession session = newSession();
        session.loadInitialOverworld(0x92);
        fillActiveObjects(session, 0x6E);

        EntityBackgroundCollisionResult result = session.entityBackgroundCollisionResultForTest(
            syntheticEntity(HookshotChainMotion.ENTITY_TYPE),
            EntityBackgroundCollisionResult.RIGHT, 0x40, 0x40);

        assertEquals(0x6E, result.objectId());
        assertEquals(0x60, result.physicsFlag());
        assertTrue(result.blocked());
    }

    @Test
    void forwardsHeldActionButtonsToTheLiveEvasiveStalfosHandler() {
        RoomSession session = newSession();
        session.loadIndoor(0x00, 0x0F);
        RoomEntity initial = session.activeRoom().entities().loadedEntities().stream()
            .filter(entity -> entity.type() == 0x1E)
            .findFirst()
            .orElseThrow();

        session.setEntityActionButtonsHeld(true);
        session.tickEntities(0, initial.x() + 0x10, initial.y());
        session.tickEntities(1, initial.x() + 0x10, initial.y());
        session.setEntityActionButtonsHeld(false);
        session.tickEntities(2, initial.x() + 0x10, initial.y());

        RoomEntity jumping = session.activeRoom().entities().slots().get(initial.slot());
        assertEquals(0x1E, jumping.type());
        assertEquals(1, jumping.z());
        assertEquals(2, jumping.spriteVariant());
        assertEquals(0x4E7D, jumping.spriteDefinition().address());
    }

    @Test
    void positiveZSkipsRomGroundSamplingForAnAirborneEntity() {
        RoomSession session = newSession();
        session.loadIndoor(0x00, 0x0F);
        RoomEntity initial = session.activeRoom().entities().loadedEntities().stream()
            .filter(entity -> entity.type() == 0x1E)
            .findFirst()
            .orElseThrow();
        fillActiveObjects(session, 0x1B);

        session.setEntityActionButtonsHeld(true);
        session.tickEntities(0, initial.x() + 0x10, initial.y());
        session.tickEntities(1, initial.x() + 0x10, initial.y());
        session.setEntityActionButtonsHeld(false);
        session.tickEntities(2, initial.x() + 0x10, initial.y());

        RoomEntity airborne = session.activeRoom().entities().slots().get(initial.slot());
        assertEquals(1, airborne.z());
        assertEquals(0, session.entityGroundStatusForTest(initial.slot()));
    }

    @Test
    void liveAnglersTunnelEvasiveStalfosCreatesTheRomFleeingCloneAfterLanding() {
        RoomSession session = newSession();
        session.loadIndoor(0x03, 0x0F);
        RoomEntity initial = session.activeRoom().entities().loadedEntities().stream()
            .filter(entity -> entity.type() == 0x1E)
            .findFirst()
            .orElseThrow();
        int linkX = initial.x() + 0x10;

        session.setEntityActionButtonsHeld(true);
        session.tickEntities(0, linkX, initial.y());
        session.tickEntities(1, linkX, initial.y());
        session.setEntityActionButtonsHeld(false);

        RoomEntity clone = null;
        for (int frame = 2; frame < 80; frame++) {
            session.tickEntities(frame, linkX, initial.y());
            clone = session.activeRoom().entities().slots().stream()
                .filter(entity -> entity.sourceLoadOrder() == -1 && entity.type() == 0x1E)
                .findFirst()
                .orElse(null);
            if (clone != null) {
                break;
            }
        }

        assertNotNull(clone);
        assertEquals(0x4E8E, clone.spriteDefinition().address());
        List<EntityCombatEvent> events = session.consumeEntityEvents();
        assertEquals(1, events.size());
        assertEquals(EntityCombatEvent.SoundChannel.NOISE, events.getFirst().soundChannel());
        assertEquals(0x0A, events.getFirst().soundId());
    }

    @Test
    void waterTektiteMovesThroughDeepWaterInTheLiveRoomSession() {
        RoomSession session = newSession();
        session.loadIndoor(0x00, 0x65);
        RoomEntity initial = session.activeRoom().entities().loadedEntities().stream()
            .filter(entity -> entity.type() == 0x99)
            .findFirst()
            .orElseThrow();
        assertEquals(0x58, initial.x());
        assertEquals(0x40, initial.y());

        int[] roomObjects = session.activeRoom().roomObjectsArea();
        for (int row = 0; row < RoomConstants.OBJECTS_PER_COLUMN; row++) {
            for (int column = 0; column < RoomConstants.OBJECTS_PER_ROW; column++) {
                roomObjects[RoomConstants.ROOM_OBJECTS_BASE
                    + row * RoomConstants.ROOM_OBJECT_ROW_STRIDE + column] = 0x0E;
            }
        }

        for (int frame = 0; frame <= 8; frame++) {
            session.tickEntities(frame, 0, 0);
        }

        RoomEntity moved = session.activeRoom().entities().slots().get(initial.slot());
        assertTrue(moved.x() != initial.x() || moved.y() != initial.y());
    }

    @Test
    void waterTektiteStillStopsOnSolidTerrainInTheLiveRoomSession() {
        RoomSession session = newSession();
        session.loadIndoor(0x00, 0x65);
        RoomEntity initial = session.activeRoom().entities().loadedEntities().stream()
            .filter(entity -> entity.type() == 0x99)
            .findFirst()
            .orElseThrow();

        int[] roomObjects = session.activeRoom().roomObjectsArea();
        for (int row = 0; row < RoomConstants.OBJECTS_PER_COLUMN; row++) {
            for (int column = 0; column < RoomConstants.OBJECTS_PER_ROW; column++) {
                roomObjects[RoomConstants.ROOM_OBJECTS_BASE
                    + row * RoomConstants.ROOM_OBJECT_ROW_STRIDE + column] = 0x10;
            }
        }

        for (int frame = 0; frame <= 8; frame++) {
            session.tickEntities(frame, 0, 0);
        }

        RoomEntity blocked = session.activeRoom().entities().slots().get(initial.slot());
        assertEquals(initial.x(), blocked.x());
        assertEquals(initial.y(), blocked.y());
    }

    @Test
    void peaHatRetainsTheRomDeepWaterGroundStatus() {
        RoomSession session = newSession();
        session.loadIndoor(0x00, 0x6B);
        List<RoomEntity> peaHats = session.activeRoom().entities().loadedEntities().stream()
            .filter(entity -> entity.type() == 0xA0)
            .toList();
        assertTrue(!peaHats.isEmpty());
        for (RoomEntity entity : session.activeRoom().entities().loadedEntities()) {
            if (entity.type() != 0xA0) {
                session.clearEntity(entity.slot());
            }
        }
        fillActiveObjects(session, 0x0E);

        session.tickEntities(0);
        session.tickEntities(1);

        for (RoomEntity peaHat : peaHats) {
            RoomEntity current = session.activeRoom().entities().slots().get(peaHat.slot());
            assertEquals(EntityStatus.ACTIVE, current.status());
            assertEquals(0x02, session.entityGroundStatusForTest(peaHat.slot()));
        }
        assertEquals(0, session.consumeEntityEvents().size());
    }

    @Test
    void ordinaryEntitiesReceiveTheRomConveyorNudgeEveryFourFrames() {
        RoomSession passableSession = newSession();
        RoomSession conveyorSession = newSession();
        passableSession.loadInitialOverworld(0x2F);
        conveyorSession.loadInitialOverworld(0x2F);

        RoomEntity passableInitial = passableSession.activeRoom().entities().loadedEntities().stream()
            .filter(entity -> entity.type() == 0x09)
            .findFirst()
            .orElseThrow();
        RoomEntity conveyorInitial = conveyorSession.activeRoom().entities().slots()
            .get(passableInitial.slot());
        assertEquals(passableInitial.x(), conveyorInitial.x());
        assertEquals(passableInitial.y(), conveyorInitial.y());
        fillActiveObjects(passableSession, 0x01);
        fillActiveObjects(conveyorSession, 0xCF);

        for (int frame = 0; frame <= 4; frame++) {
            passableSession.tickEntities(frame, 0, 0);
            conveyorSession.tickEntities(frame, 0, 0);
        }

        RoomEntity passable = passableSession.activeRoom().entities().slots()
            .get(passableInitial.slot());
        RoomEntity conveyor = conveyorSession.activeRoom().entities().slots()
            .get(conveyorInitial.slot());
        // Overworld $CF is physics $F4, whose bank-$03 movement table entry is
        // (+1,+1). The ordinary entity's own motion is identical in both runs.
        assertEquals((passable.x() + 1) & 0xFF, conveyor.x());
        assertEquals((passable.y() + 1) & 0xFF, conveyor.y());
    }

    @Test
    void noGroundInteractionEntitiesIgnoreTheRomConveyorNudge() {
        RoomSession passableSession = newSession();
        RoomSession conveyorSession = newSession();
        passableSession.loadIndoor(0x00, 0x05);
        conveyorSession.loadIndoor(0x00, 0x05);

        RoomEntity passableInitial = passableSession.activeRoom().entities().loadedEntities().stream()
            .filter(entity -> entity.type() == 0x17)
            .findFirst()
            .orElseThrow();
        RoomEntity conveyorInitial = conveyorSession.activeRoom().entities().slots()
            .get(passableInitial.slot());
        fillActiveObjects(passableSession, 0x02);
        fillActiveObjects(conveyorSession, 0xCF);

        for (int frame = 0; frame <= 4; frame++) {
            passableSession.tickEntities(frame, 0, 0);
            conveyorSession.tickEntities(frame, 0, 0);
        }

        RoomEntity passable = passableSession.activeRoom().entities().slots()
            .get(passableInitial.slot());
        RoomEntity conveyor = conveyorSession.activeRoom().entities().slots()
            .get(conveyorInitial.slot());
        assertEquals(passable.x(), conveyor.x());
        assertEquals(passable.y(), conveyor.y());
    }

    @Test
    void ordinaryEntityKeepsTheRomShallowWaterGroundStatus() {
        RoomSession session = newSession();
        session.loadInitialOverworld(0x2F);
        RoomEntity initial = session.activeRoom().entities().loadedEntities().stream()
            .filter(entity -> entity.type() == 0x09)
            .findFirst()
            .orElseThrow();
        fillActiveObjects(session, 0x1B);

        session.tickEntities(0);
        session.tickEntities(1);

        RoomEntity entity = session.activeRoom().entities().slots().get(initial.slot());
        assertEquals(EntityStatus.ACTIVE, entity.status());
        assertEquals(0x02, session.entityGroundStatusForTest(initial.slot()));
    }

    @Test
    void liveOctorokUsesEntityPhysicsInsteadOfLinkBlockingPolicy() {
        RoomSession session = newSession();
        session.loadInitialOverworld(0x2F);
        RoomEntity octorok = session.activeRoom().entities().loadedEntities().stream()
            .filter(entity -> entity.type() == 0x09)
            .findFirst()
            .orElseThrow();

        fillActiveObjects(session, 0x0E); // Overworld deep-water physics $07.
        EntityBackgroundCollisionResult water =
            session.entityBackgroundCollisionResultForTest(
                octorok, EntityBackgroundCollisionResult.RIGHT, octorok.x(), octorok.y());
        assertFalse(water.blocked());
        assertEquals(0x0E, water.objectId());
        assertEquals(PhysicsFlags.DEEP_WATER, water.physicsFlag());

        fillActiveObjects(session, 0x00); // Overworld solid physics $01.
        EntityBackgroundCollisionResult solid =
            session.entityBackgroundCollisionResultForTest(
                octorok, EntityBackgroundCollisionResult.RIGHT, octorok.x(), octorok.y());
        assertTrue(solid.blocked());
        assertEquals(0x00, solid.objectId());
        assertEquals(PhysicsFlags.SOLID, solid.physicsFlag());
    }

    @Test
    void liveEntityCollisionProbeUsesActiveIgnoreHitsCountdownForGroundedPits() {
        RoomSession session = newSession();
        session.loadIndoor(0x00, 0x0F);
        RoomEntity entity = session.activeRoom().entities().loadedEntities().stream()
            .filter(candidate -> candidate.type() == 0x1E)
            .findFirst()
            .orElseThrow();
        fillActiveObjects(session, 0x01); // Indoors1 object $01 is normal pit.

        session.setEntityIgnoreHitsCountdownForTest(entity.slot(), 1);
        assertFalse(session.entityBackgroundCollisionResultForTest(
            entity, EntityBackgroundCollisionResult.RIGHT, entity.x(), entity.y()).blocked());

        session.setEntityIgnoreHitsCountdownForTest(entity.slot(), 0);
        assertTrue(session.entityBackgroundCollisionResultForTest(
            entity, EntityBackgroundCollisionResult.RIGHT, entity.x(), entity.y()).blocked());
    }

    @Test
    void liveEntityCollisionProbeUsesRomLedgeTimerCadenceForIndoorAndOverworldRooms() {
        RoomSession indoor = newSession();
        indoor.loadIndoor(0x00, 0x0F);
        RoomEntity indoorEntity = indoor.activeRoom().entities().loadedEntities().stream()
            .filter(entity -> entity.type() == 0x1E)
            .findFirst()
            .orElseThrow();
        fillActiveObjects(indoor, 0x23); // Indoors1 object $23 has physics $D0.
        indoor.setEntityThrownDirectionForTest(indoorEntity.slot(), 0x01);
        indoor.setEntityLedgeTimerForTest(indoorEntity.slot(), 0x02);

        EntityBackgroundCollisionResult indoorResult =
            indoor.entityBackgroundCollisionResultForTest(
                indoorEntity, EntityBackgroundCollisionResult.RIGHT,
                indoorEntity.x(), indoorEntity.y(), 0x01);
        assertFalse(indoorResult.blocked());
        assertEquals(0x01, indoor.entityLedgeTimerForTest(indoorEntity.slot()));

        RoomSession outdoor = newSession();
        outdoor.loadInitialOverworld(0x2F);
        RoomEntity outdoorEntity = outdoor.activeRoom().entities().loadedEntities().stream()
            .filter(entity -> entity.type() == 0x09)
            .findFirst()
            .orElseThrow();
        fillActiveObjects(outdoor, 0xF3); // Overworld object $F3 has physics $D0.
        outdoor.setEntityThrownDirectionForTest(outdoorEntity.slot(), 0x01);
        outdoor.setEntityLedgeTimerForTest(outdoorEntity.slot(), 0x02);

        EntityBackgroundCollisionResult evenFrame =
            outdoor.entityBackgroundCollisionResultForTest(
                outdoorEntity, EntityBackgroundCollisionResult.RIGHT,
                outdoorEntity.x(), outdoorEntity.y(), 0x02);
        assertFalse(evenFrame.blocked());
        assertEquals(0x02, outdoor.entityLedgeTimerForTest(outdoorEntity.slot()));

        EntityBackgroundCollisionResult oddFrame =
            outdoor.entityBackgroundCollisionResultForTest(
                outdoorEntity, EntityBackgroundCollisionResult.RIGHT,
                outdoorEntity.x(), outdoorEntity.y(), 0x03);
        assertFalse(oddFrame.blocked());
        assertEquals(0x01, outdoor.entityLedgeTimerForTest(outdoorEntity.slot()));
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
        session.setEntitySwitchBlocksStateForTest(0x00);
        EntityBackgroundCollisionResult lower = session.entityBackgroundCollisionResultForTest(
            entity, EntityBackgroundCollisionResult.RIGHT, entity.x(), entity.y());
        assertFalse(lower.blocked());
        assertEquals(0xDB, lower.objectId());
        assertEquals(0x04, lower.physicsFlag());

        fillActiveObjects(session, 0xDC);
        EntityBackgroundCollisionResult raisedStateZero =
            session.entityBackgroundCollisionResultForTest(
                entity, EntityBackgroundCollisionResult.RIGHT, entity.x(), entity.y());
        assertTrue(raisedStateZero.blocked());

        session.setEntitySwitchBlocksStateForTest(0x02);
        EntityBackgroundCollisionResult raisedStateTwo =
            session.entityBackgroundCollisionResultForTest(
                entity, EntityBackgroundCollisionResult.RIGHT, entity.x(), entity.y());
        assertFalse(raisedStateTwo.blocked());
    }

    @Test
    void gameplayVblankAdvancesTheRoomOwnedSwitchStageBeforeOrdinaryAnimations() {
        RoomSession session = newSession();
        session.setEntitySwitchBlocksStateForTest(0x00);
        session.setSwitchableObjectAnimationStageForTest(0x02);

        session.tickGameplayVBlank();

        assertEquals(0x03, session.switchableObjectAnimationStageForTest());
        assertEquals(0x02, session.entitySwitchBlocksStateForTest());

        session.setSwitchableObjectAnimationStageForTest(0x09);
        session.tickGameplayVBlank();

        assertEquals(0x00, session.switchableObjectAnimationStageForTest());
        assertEquals(0x02, session.entitySwitchBlocksStateForTest());
    }

    @Test
    void gameplayVblankSynchronizesTheToggledStateIntoLinkCollision() {
        RoomSession session = newSession();
        session.loadIndoor(0x00, 0x0F);
        fillActiveObjects(session, 0xDC);
        session.setEntitySwitchBlocksStateForTest(0x00);

        assertTrue(session.linkCollisionPointBlockedForTest(0x20, 0x20));

        session.setSwitchableObjectAnimationStageForTest(0x02);
        session.tickGameplayVBlank();

        assertEquals(0x02, session.entitySwitchBlocksStateForTest());
        assertFalse(session.linkCollisionPointBlockedForTest(0x20, 0x20));
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

        List<EntityCombatEvent> hit = session.resolveEntityCombat(
            1, 0, 0, false, false, true,
            crystal.x() + 0x08, 1, crystal.y() + 0x08, 1);

        assertEquals(1, hit.size());
        assertEquals(0x66, hit.get(0).type());
        session.tickEntities(1, 0, 0);

        assertEquals(0x01, session.switchableObjectAnimationStageForTest());
        assertEquals(List.of(new EntityCombatEvent(crystal.slot(), 0x66, 0, false,
            EntityCombatEvent.SoundChannel.WAVE, 0x0E)),
            session.consumeEntityEvents());
    }

    @Test
    void ordinaryEntityEntersRomFallingStateOnPitPhysics() {
        RoomSession session = newSession();
        session.loadIndoor(0x00, 0x0F);
        RoomEntity initial = session.activeRoom().entities().loadedEntities().stream()
            .filter(entity -> entity.type() == 0x1E)
            .findFirst()
            .orElseThrow();
        for (RoomEntity entity : session.activeRoom().entities().loadedEntities()) {
            if (entity.slot() != initial.slot()) {
                session.clearEntity(entity.slot());
            }
        }
        // Indoors1 uses object $01 for the ordinary pit; $7B is the
        // overworld table's pit object and is shallow water indoors.
        fillActiveObjects(session, 0x01);
        session.setEntityIgnoreHitsCountdownForTest(initial.slot(), 0x02);

        session.tickEntities(0, 0, 0);
        session.tickEntities(1, 0, 0);

        RoomEntity falling = session.activeRoom().entities().slots().get(initial.slot());
        assertEquals(EntityStatus.FALLING, falling.status());
        assertEquals((falling.x() - 1) & 0xF0,
            session.entityFallingTargetXForTest(initial.slot()) - 0x08 & 0xF0);
        assertEquals(0x48, session.entityTransitionCountdownForTest(initial.slot()));
    }

    @Test
    void pitTransitionKeepsRomFollowerAndPickupExceptions() {
        OverworldCollision.GroundInteractionSample pit =
            new OverworldCollision.GroundInteractionSample(
                0x01, PhysicsFlags.NORMAL_PIT, 0x20, 0x30);
        for (int type : new int[] {0x6D, 0xD5, 0x36}) {
            RoomEntity entity = syntheticEntity(type);
            assertNull(RoomSession.pitTransitionFor(
                entity, pit, EnemyProjectileCollision.LINK_MOTION_NON_INTERACTIVE),
                "type=" + Integer.toHexString(type));
        }

        RoomEntity marin = syntheticEntity(0xC1);
        assertNull(RoomSession.pitTransitionFor(
            marin, pit, RoomSession.LINK_MOTION_FALLING_DOWN),
            "Marin only falls through a well");

        OverworldCollision.GroundInteractionSample well =
            new OverworldCollision.GroundInteractionSample(
                0x61, PhysicsFlags.SOLID, 0x40, 0x50);
        assertNull(RoomSession.pitTransitionFor(
            marin, well, EnemyProjectileCollision.LINK_MOTION_NON_INTERACTIVE));
        assertEquals(new RoomEntityGroundInteraction.PitTransition(0x48, 0x60),
            RoomSession.pitTransitionFor(
                marin, well, RoomSession.LINK_MOTION_FALLING_DOWN));
    }

    @Test
    void ordinaryEntityEnteringRomDeepWaterUnloadsAndCreatesSplashSideEffects() {
        TransientVfxSystem vfx = new TransientVfxSystem(16);
        RoomSession session = newSession(vfx);
        session.loadInitialOverworld(0x2F);
        RoomEntity initial = session.activeRoom().entities().loadedEntities().stream()
            .filter(entity -> entity.type() == 0x09)
            .findFirst()
            .orElseThrow();
        for (RoomEntity entity : session.activeRoom().entities().loadedEntities()) {
            if (entity.slot() != initial.slot()) {
                session.clearEntity(entity.slot());
            }
        }
        fillActiveObjects(session, 0x0E);

        session.tickEntities(0);
        session.tickEntities(1);

        assertEquals(EntityStatus.DISABLED,
            session.activeRoom().entities().slots().get(initial.slot()).status());
        assertEquals(0, session.entityGroundStatusForTest(initial.slot()));
        assertEquals(1, vfx.activeCount());
        assertEquals(TransientVfxType.WATER_SPLASH,
            vfx.activeSlots().getFirst().type());
        List<EntityCombatEvent> events = session.consumeEntityEvents();
        assertEquals(1, events.size());
        assertEquals(EntityCombatEvent.SoundChannel.JINGLE,
            events.getFirst().soundChannel());
        assertEquals(0x0E, events.getFirst().soundId());
    }

    @Test
    void returnsProjectileEventsFromTheEntityPassWhenLinkStateIsProvided() {
        RoomSession session = newSession();
        session.loadInitialOverworld(0x92);

        List<EntityProjectileEvent> events = session.tickEntitiesWithProjectileEvents(
            0, 0x08, 0x10, 0x00, 0x02, 0x00, false);

        assertTrue(events.isEmpty());
        assertEquals(session.activeRoom().entities(), session.renderSnapshot().entities());
    }

    @Test
    void publishesARealRomProjectileIntoTheSessionRenderSnapshot() {
        RoomSession session = newSession();
        session.loadInitialOverworld(0x2F);
        RoomEntity parent = session.activeRoom().entities().loadedEntities().stream()
            .filter(entity -> entity.type() == 0x09)
            .findFirst()
            .orElseThrow();

        RoomEntity projectile = null;
        for (int frame = 0; frame < 0x40 && projectile == null; frame++) {
            RoomEntity currentParent = session.activeRoom().entities().slots()
                .get(parent.slot());
            session.tickEntitiesWithProjectileEvents(
                frame, (currentParent.x() + 0x20) & 0xFF, currentParent.y(),
                0x00, 0x00, 0x00, false);
            projectile = session.activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x0A)
                .findFirst()
                .orElse(null);
        }

        assertNotNull(projectile);
        assertEquals(-1, projectile.sourceLoadOrder());
        assertEquals(0x03, projectile.spriteDefinition().bank());
        assertEquals(0x6A1E, projectile.spriteDefinition().address());
        assertTrue(projectile.spriteDefinition().supported());
        assertEquals(session.activeRoom().entities(), session.renderSnapshot().entities());
        assertNotNull(session.renderSnapshot().entities().spriteTiles());
    }

    @Test
    void synchronizesDynamicFollowerSpawningAndFollowerDisplaySelection() {
        RoomSession session = newSession();
        session.loadInitialOverworld(0x92);

        session.setFollowingNpcState(
            new FollowingNpcState(true, 0, false, false, 0, 0, false),
            0x50, 0x60, 0x00, 0x00, 0x00);

        RoomEntity rooster = session.activeRoom().entities().slots().get(15);
        assertEquals(0xD5, rooster.type());
        assertEquals(EntityStatus.ACTIVE, rooster.status());
        assertEquals(0x50, rooster.x());
        assertEquals(0x60, rooster.y());
        assertEquals(0x19, rooster.spriteDefinition().bank());
        assertEquals(0x59BC, rooster.spriteDefinition().address());
        assertEquals(0x59BC, session.activeRoom().entities().spriteSelection()
            .spriteOverrideFor(0xD5).address());
        assertEquals(rooster, session.activeRoom().entities().slots().get(15));
    }

    @Test
    void sideScrollingRoomExcludesFollowingNpcDuringRoomSessionSynchronization() {
        RoomSession session = newSession();
        session.loadInitialOverworld(0x92);
        session.loadIndoor(0x10, 0xA3, Warp.CATEGORY_SIDESCROLL);

        session.setFollowingNpcState(
            new FollowingNpcState(true, 0, false, false, 0, 0, false),
            0x50, 0x60, 0x00, 0x00, 0x00);

        assertTrue(session.activeRoom().entities().sideScrolling());
        assertFalse(session.activeRoom().entities().loadedEntities().stream()
            .anyMatch(entity -> entity.type() == FollowingNpcEntitySpawner.ENTITY_ROOSTER));
    }

    @Test
    void loadsColorDungeonEntityRowsThroughTheSpecialRoomPath() {
        byte[] rom = loadRom();
        RoomSession session = newSession();

        session.loadIndoor(0xFF, 0x00);

        int sourceOffset = RomBank.romOffset(0x35, 0x5100);
        int expectedPixel = ((Byte.toUnsignedInt(rom[sourceOffset]) >>> 7) & 0x01)
            | (((Byte.toUnsignedInt(rom[sourceOffset + 1]) >>> 7) & 0x01) << 1);
        assertEquals(EntityRoomLoader.RoomTable.COLOR_DUNGEON,
            session.activeRoom().entities().spriteSelection().roomTable());
        assertNotNull(session.activeRoom().entities().spriteTiles());
        assertEquals(expectedPixel,
            session.activeRoom().entities().spriteTiles().tile(0x40).getPixel(0, 0));

        int objectId = session.activeRoom().roomObjectsArea()[RoomConstants.ROOM_OBJECTS_BASE];
        int expectedTileOffset = RomBank.romOffset(0x08, 0x4760) + objectId * 4;
        int expectedAttrOffset = RomBank.romOffset(0x23, 0x6000) + objectId * 4;
        int expectedPaletteOffset = RomBank.romOffset(0x21, 0x67D0);
        int expectedPaletteColor = RomBank.decodeRgb555(
            Byte.toUnsignedInt(rom[expectedPaletteOffset])
                | (Byte.toUnsignedInt(rom[expectedPaletteOffset + 1]) << 8));
        assertEquals(expectedPaletteColor, session.activeRoom().palettes()[0][0]);
        assertEquals(Byte.toUnsignedInt(rom[expectedTileOffset]),
            session.activeRoom().tileIds()[0]);
        assertEquals(Byte.toUnsignedInt(rom[expectedAttrOffset]),
            session.activeRoom().tileAttrs()[0]);
    }

    @Test
    void colorShellPuzzleWritesBackIntoTheLiveRoomTilemap() {
        RoomSession session = newSession();
        session.loadIndoor(0xFF, 0x07);
        session.tickEntities(0);

        RoomEntity shell = session.activeRoom().entities().loadedEntities().stream()
            .filter(entity -> entity.type() == 0xE9)
            .findFirst()
            .orElseThrow();
        int objectIndex = RoomConstants.ROOM_OBJECTS_BASE
            + ((shell.y() - 0x07) & 0xF0)
            + (((shell.x() - 0x01) & 0xF0) >>> 4);
        session.activeRoom().roomObjectsArea()[objectIndex - 1] = 0x5E;
        int[] beforeTiles = session.activeRoom().tileIds().clone();
        session.setColorShellStateForTest(shell.slot(), 0x08, 1, 0, 0, 0);

        session.tickEntities(1);

        assertEquals(0x67, session.activeRoom().roomObjectsArea()[objectIndex]);
        assertNotEquals(Arrays.toString(beforeTiles),
            Arrays.toString(session.activeRoom().tileIds()));
    }

    @Test
    void colorShellRomSoundWritesReachTheGameplaySoundBoundary() {
        RoomSession session = newSession();
        List<GameplaySoundEvent> sounds = new ArrayList<>();
        session.setColorShellSoundSink(sounds::add);
        session.loadIndoor(0xFF, 0x07);
        session.tickEntities(0);

        RoomEntity shell = session.activeRoom().entities().loadedEntities().stream()
            .filter(entity -> entity.type() == 0xE9)
            .findFirst()
            .orElseThrow();
        int objectIndex = RoomConstants.ROOM_OBJECTS_BASE
            + ((shell.y() - 0x07) & 0xF0)
            + (((shell.x() - 0x01) & 0xF0) >>> 4);
        session.activeRoom().roomObjectsArea()[objectIndex - 1] = 0x00;
        session.setColorShellStateForTest(shell.slot(), 0x08, 1, 0, 0, 0);

        session.tickEntities(1);

        assertEquals(List.of(GameplaySoundEvent.WRONG_ANSWER), sounds);
    }

    @Test
    void persistsClearedFirstEightEntitySlotsWhenTheRoomReloads() {
        RoomSession session = newSession();
        session.loadInitialOverworld(0x92);

        session.clearEntity(0);
        session.loadOverworld(0x92);

        assertEquals(0x3E, session.activeRoom().entities().slots().get(0).type());
        assertEquals(1, session.activeRoom().entities().slots().get(0).sourceLoadOrder());
    }

    @Test
    void collectsAStaticPickupThroughTheActiveRoomSessionAndPersistsIt() {
        RoomSession session = newSession();
        session.loadInitialOverworld(0xA4);

        RoomEntity pickup = session.activeRoom().entities().loadedEntities().stream()
            .filter(entity -> entity.type() == 0x3D)
            .findFirst()
            .orElseThrow();
        session.tickEntities(0);
        int collectionFrame = (pickup.slot() & 1) == 0 ? 1 : 0;

        EntityPickupEvent event = session.collectEntityIfNeeded(
            collectionFrame, pickup.x(), pickup.y(), false, true);

        assertNotNull(event);
        assertEquals(pickup.slot(), event.slot());
        assertEquals(EntityStatus.DISABLED,
            session.activeRoom().entities().slots().get(pickup.slot()).status());

        session.loadOverworld(0xA4);

        assertEquals(0xAE, session.activeRoom().entities().slots().get(0).type());
    }

    @Test
    void loadsTheDisassemblyNewGameRoomThroughTheIndoorPath() {
        RoomSession session = newSession();

        session.loadIndoor(0x10, 0xA3);

        assertEquals(0x10, session.mapId());
        assertEquals(0xA3, session.currentRoomId());
        assertEquals(Warp.CATEGORY_INDOOR, session.mapCategory());
    }

    @Test
    void exposesAndRestoresTheRoomStatusTablesUsedBySaveGameToFile() {
        RoomSession session = newSession();
        byte[] overworld = pattern(0x100, 0x10);
        byte[] indoorA = pattern(0x100, 0x40);
        byte[] indoorB = pattern(0x100, 0x70);
        byte[] colorDungeon = pattern(0x20, 0xA0);

        session.restoreRoomStatuses(overworld, indoorA, indoorB, colorDungeon);

        assertArrayEquals(overworld, session.overworldRoomStatusSnapshot());
        assertArrayEquals(indoorA, session.indoorARoomStatusSnapshot());
        assertArrayEquals(indoorB, session.indoorBRoomStatusSnapshot());
        assertArrayEquals(colorDungeon, session.colorDungeonRoomStatusSnapshot());
    }

    @Test
    void exposesAndRestoresDungeonItemFlagsUsedByTheWorldHandler() {
        RoomSession session = newSession();
        byte[] dungeonFlags = pattern(DungeonItemState.DUNGEON_ITEM_FLAGS_SIZE, 0x10);
        byte[] colorFlags = pattern(DungeonItemState.COLOR_DUNGEON_ITEM_FLAGS_SIZE, 0xE0);

        session.restoreDungeonItemFlags(dungeonFlags, colorFlags);
        session.loadIndoor(2, 0x25);

        assertArrayEquals(dungeonFlags, session.dungeonItemFlagsSnapshot());
        assertArrayEquals(colorFlags, session.colorDungeonItemFlagsSnapshot());
        assertArrayEquals(new byte[] {
            dungeonFlags[2 * DungeonItemState.ITEM_FLAG_SIZE],
            dungeonFlags[2 * DungeonItemState.ITEM_FLAG_SIZE + 1],
            dungeonFlags[2 * DungeonItemState.ITEM_FLAG_SIZE + 2],
            dungeonFlags[2 * DungeonItemState.ITEM_FLAG_SIZE + 3],
            dungeonFlags[2 * DungeonItemState.ITEM_FLAG_SIZE + 4]
        }, session.currentDungeonItemFlagsSnapshot());
    }

    private static RoomSession newSession() {
        return newSession(room -> {
        });
    }

    private static RoomSession newSession(RoomLoadListener roomLoadListener) {
        return newSession(new TransientVfxSystem(16), roomLoadListener);
    }

    private static RoomSession newSession(TransientVfxSystem transientVfxSystem) {
        return newSession(transientVfxSystem, room -> {
        });
    }

    private static RoomSession newSession(TransientVfxSystem transientVfxSystem,
                                          RoomLoadListener roomLoadListener) {
        byte[] rom = loadRom();
        RomTables romTables = RomTables.loadFromRom(rom);
        return new RoomSession(
            rom,
            new GPU(),
            new RoomLoader(rom),
            new OverworldTilesetTable(rom),
            new OverworldCollision(romTables),
            transientVfxSystem,
            null,
            roomLoadListener
        );
    }

    private static byte[] loadRom() {
        try (var stream = RoomSessionTest.class.getClassLoader().getResourceAsStream("rom/azle.gbc")) {
            if (stream == null) {
                throw new IllegalStateException("ROM resource missing");
            }
            return stream.readAllBytes();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load ROM", e);
        }
    }

    private static byte[] pattern(int length, int start) {
        byte[] values = new byte[length];
        for (int index = 0; index < values.length; index++) {
            values[index] = (byte) (start + index);
        }
        return values;
    }

    private static void fillActiveObjects(RoomSession session, int objectId) {
        int[] roomObjects = session.activeRoom().roomObjectsArea();
        for (int row = 0; row < RoomConstants.OBJECTS_PER_COLUMN; row++) {
            for (int column = 0; column < RoomConstants.OBJECTS_PER_ROW; column++) {
                roomObjects[RoomConstants.ROOM_OBJECTS_BASE
                    + row * RoomConstants.ROOM_OBJECT_ROW_STRIDE + column] = objectId;
            }
        }
    }

    private static RoomEntity syntheticEntity(int type) {
        return new RoomEntity(0, 0, type, 0x20, 0x30, EntityStatus.ACTIVE,
            EntitySpriteDefinition.unsupported(type), -1);
    }
}
