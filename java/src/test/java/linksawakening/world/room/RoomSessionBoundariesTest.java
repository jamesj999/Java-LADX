package linksawakening.world.room;

import static linksawakening.world.room.RoomSessionTestSupport.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.vfx.TransientVfxSystem;
import linksawakening.world.entity.EntityBackgroundCollisionResult;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.FollowingNpcEntitySpawner;
import linksawakening.world.entity.FollowingNpcState;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.transition.ScrollController;
import linksawakening.world.transition.Warp;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class RoomSessionBoundariesTest {

    @Test
    void roomLoadClearsAnUnconsumedDungeonWarpRequest() throws Exception {
        RoomSession session = newSession();
        session.loadIndoor(0x01, 0x28);
        session.context.pendingDungeonWarp = new Warp(Warp.CATEGORY_INDOOR, 0x01, 0x36, 0x50, 0x48, 0x34);

        session.loadIndoor(0x01, 0x36);

        assertNull(session.consumeClearedDungeonWarp());
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
        assertNotSame(
                previousEntities.spriteTiles(), session.activeRoom().entities().spriteTiles());
        assertEquals(0x93, session.activeRoom().entities().spriteSelection().roomId());
        assertEquals(previousEntities, scrollController.previousRoom().entities());
        assertNotNull(scrollController.previousRoom().entities().spriteTiles());
        assertTrue(scrollController.isActive());
        assertEquals(ScrollController.RIGHT, scrollController.direction());
        assertNotNull(scrollController.previousRoom());
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
        assertEquals(
                0x13,
                session.consumeEntityEvents().stream()
                        .filter(event -> event.type() == 0x18)
                        .findFirst()
                        .orElseThrow()
                        .soundId());
    }

    @Test
    void flipsTheOcarinaBodyPoseAtTheRomFiftySixFrameCadence() {
        RoomSession session = newSession();
        session.loadIndoor(0x00, 0x25);

        assertTrue(session.startOcarina(0xFF, 0x01, 0x00));
        assertEquals(0x76, session.ocarinaAnimationState());

        for (int frame = 0; frame < 55; frame++) {
            session.tickEntities(frame);
        }
        assertEquals(0x76, session.ocarinaAnimationState());

        session.tickEntities(55);
        assertEquals(0x75, session.ocarinaAnimationState());
    }

    @Test
    void magicRodBurnsAnOverworldBushThroughTheLiveRoomBoundary() {
        TransientVfxSystem vfx = new TransientVfxSystem(16);
        RoomSession session = newSession(vfx);
        session.loadInitialOverworld(0x92);

        int location = 0x55;
        int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + (location & 0xF0) + (location & 0x0F);
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
    void magicPowderRevealsAnOverworldBushThroughTheLiveRoomBoundary() {
        TransientVfxSystem vfx = new TransientVfxSystem(16);
        RoomSession session = newSession(vfx);
        session.loadInitialOverworld(0x92);

        int location = 0x55;
        int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + (location & 0xF0) + (location & 0x0F);
        session.activeRoom().roomObjectsArea()[areaIndex] = 0x5C;
        session.activeRoom().renderValues()[areaIndex] = 0x5C;

        // The source entity cell is computed from entity X/Y as
        // ((x - 1) & F0, (y - 9) & F0); these Link coordinates place it on
        // room cell $55 after the ROM right-facing +$0E offset.
        assertTrue(session.sprinkleMagicPowder(0x43, 0x59, 0, 0));
        for (int frame = 0; frame < 16; frame++) {
            session.tickEntitiesWithProjectileEvents(frame, 0x20, 0x20, 0, 0, 0, false);
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
                        entity, EntityBackgroundCollisionResult.RIGHT, entity.x(), entity.y())
                .blocked());

        session.setEntityIgnoreHitsCountdownForTest(entity.slot(), 0);
        assertTrue(session.entityBackgroundCollisionResultForTest(
                        entity, EntityBackgroundCollisionResult.RIGHT, entity.x(), entity.y())
                .blocked());
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
        assertEquals((falling.x() - 1) & 0xF0, session.entityFallingTargetXForTest(initial.slot()) - 0x08 & 0xF0);
        assertEquals(0x48, session.entityTransitionCountdownForTest(initial.slot()));
    }

    @Test
    void sideScrollingRoomExcludesFollowingNpcDuringRoomSessionSynchronization() {
        RoomSession session = newSession();
        session.loadInitialOverworld(0x92);
        session.loadIndoor(0x10, 0xA3, Warp.CATEGORY_SIDESCROLL);

        session.setFollowingNpcState(
                new FollowingNpcState(true, 0, false, false, 0, 0, false), 0x50, 0x60, 0x00, 0x00, 0x00);

        assertTrue(session.activeRoom().entities().sideScrolling());
        assertFalse(session.activeRoom().entities().loadedEntities().stream()
                .anyMatch(entity -> entity.type() == FollowingNpcEntitySpawner.ENTITY_ROOSTER));
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
        int objectIndex =
                RoomConstants.ROOM_OBJECTS_BASE + ((shell.y() - 0x07) & 0xF0) + (((shell.x() - 0x01) & 0xF0) >>> 4);
        session.activeRoom().roomObjectsArea()[objectIndex - 1] = 0x00;
        session.setColorShellStateForTest(shell.slot(), 0x08, 1, 0, 0, 0);

        session.tickEntities(1);

        assertEquals(List.of(GameplaySoundEvent.WRONG_ANSWER), sounds);
    }
}
