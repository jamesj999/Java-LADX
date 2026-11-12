package linksawakening.world.room;

import static linksawakening.world.room.RoomSessionTestSupport.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.Link;
import linksawakening.vfx.TransientVfxSystem;
import linksawakening.world.entity.EntityBackgroundCollisionResult;
import linksawakening.world.entity.EntityRoomLoader;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityRuntime;
import linksawakening.world.entity.RoomEntitySnapshot;
import linksawakening.world.entity.event.BombExplosionEvent;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.EntityProjectileEvent;
import linksawakening.world.entity.event.RichardProgressEvent;
import linksawakening.world.entity.event.SideViewPlatformLinkRequest;
import linksawakening.world.entity.motion.HookshotChainMotion;
import linksawakening.world.entity.motion.SideViewPlatformMotion;
import linksawakening.world.transition.Warp;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class RoomSessionProjectilesTest {

    @Test
    void forwardsSideViewPlatformLinkRequestsAndConsumesThemOnce() {
        RoomSession session = newSession();
        session.rooms().loadIndoor(0x03, 0x7C, Warp.CATEGORY_SIDESCROLL);

        List<RoomEntity> slots = new ArrayList<>();
        slots.add(new RoomEntity(
                0,
                0,
                SideViewPlatformMotion.ENTITY_TYPE,
                0x40,
                0x50,
                EntityStatus.ACTIVE,
                EntitySpriteDefinition.unsupported(SideViewPlatformMotion.ENTITY_TYPE),
                0,
                0,
                0,
                0,
                -1,
                false));
        for (int slot = 1; slot < EntityRoomLoader.MAX_ENTITIES; slot++) {
            slots.add(RoomEntity.disabled(slot));
        }
        RoomEntityRuntime runtime =
                RoomEntityRuntime.builder(new RoomEntitySnapshot(slots)).build();
        runtime.configuration().setGroundInteractionSideScrolling(true);
        linksawakening.world.entity.EntityRuntimeTestAccess.setEntityRoomIdForTest(runtime, 0x3B);
        linksawakening.world.entity.EntityRuntimeTestAccess.setTransitionSequenceCounterForTest(runtime, 0x04);
        RoomDebugFixture.of(session).replaceEntityRuntimeForTest(runtime);

        session.entities().tickEntitiesWithProjectileEvents(0, 0x48, 0x40, 0x00, false, 0, Link.DIRECTION_DOWN, false);

        assertEquals(
                List.of(new SideViewPlatformLinkRequest(0, 0, 0x40, 0x02, true, true)),
                session.components.entityBridge().outbox.consumeSideViewPlatformLinkRequests());
        assertTrue(session.components
                .entityBridge()
                .outbox
                .consumeSideViewPlatformLinkRequests()
                .isEmpty());
    }

    @Test
    void hookshotLaunchPublishesTheRomProjectileAndHonorsFireGuards() {
        RoomSession session = newSession();
        session.rooms().loadInitialOverworld(0x92);

        assertFalse(session.playerItems().fireHookshot(0x40, 0x50, 0, 0, true, false));
        assertFalse(session.playerItems().hookshotActive());
        assertFalse(session.playerItems().fireHookshot(0x40, 0x50, 0, 0, false, true));
        assertFalse(session.playerItems().hookshotActive());

        assertTrue(session.playerItems().fireHookshot(0x40, 0x50, 0, 0, false, false));
        assertTrue(session.playerItems().hookshotActive());
        assertFalse(session.playerItems().fireHookshot(0x40, 0x50, 0, 0, false, false));
        assertEquals(
                0x03,
                session.rooms().activeRoom().entities().loadedEntities().stream()
                        .filter(entity -> entity.type() == 0x03)
                        .findFirst()
                        .orElseThrow()
                        .type());
    }

    @Test
    void bombPlacementPublishesRomCoordinatesThroughTheCanonicalBridge() {
        RoomSession session = newSession();

        assertFalse(session.playerItems().placeBomb(0x40, 0x50, 0x07, 3));
        session.rooms().loadInitialOverworld(0x92);

        assertTrue(session.playerItems().placeBomb(0x40, 0x50, 0x07, 3));
        assertTrue(session.playerItems().bombActive());

        RoomEntity bomb = session.rooms().activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x02)
                .findFirst()
                .orElseThrow();
        assertEquals(0x40, bomb.x());
        assertEquals(0x54, bomb.y());
        assertEquals(0x08, bomb.z());
        assertEquals(3, RoomDebugFixture.of(session).bombDirectionForTest(bomb.slot()));
        assertEquals(
                session.rooms().activeRoom().entities(),
                session.rooms().renderSnapshot().entities());

        assertFalse(session.playerItems().placeBomb(0x44, 0x54, 0, 1));
        assertEquals(
                1,
                session.rooms().activeRoom().entities().loadedEntities().stream()
                        .filter(entity -> entity.type() == 0x02)
                        .count());
    }

    @Test
    void consumesBombExplosionObjectWindowThroughTheDedicatedSessionSeam() {
        RoomSession session = newSession();
        session.rooms().loadInitialOverworld(0x92);
        assertTrue(session.playerItems().placeBomb(0x40, 0x50, 0, 0));
        int bombSlot = session.rooms().activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x02)
                .findFirst()
                .orElseThrow()
                .slot();

        for (int frame = 0; frame < 136; frame++) {
            int currentFrame = frame;
            session.entities().tickEntities(frame, 0, 0);
            List<BombExplosionEvent> events =
                    session.components.entityBridge().outbox.consumeBombExplosionEvents();
            assertTrue(
                    events.isEmpty(),
                    () -> "frame=" + currentFrame + " countdown="
                            + RoomDebugFixture.of(session).entityTransitionCountdownForTest(bombSlot)
                            + " events=" + events);
        }
        session.entities().tickEntities(136, 0, 0);

        List<BombExplosionEvent> events =
                session.components.entityBridge().outbox.consumeBombExplosionEvents();
        assertEquals(1, events.size());
        assertTrue(events.getFirst().targetsRoomObjects());
        assertEquals(0x16, events.getFirst().countdown());
        assertEquals(BombExplosionEvent.DAMAGE_TYPE_BOMB, events.getFirst().damageType());
        assertTrue(session.components
                .entityBridge()
                .outbox
                .consumeBombExplosionEvents()
                .isEmpty());
    }

    @Test
    void bombExplosionRevealsAnOverworldBushAtTheRomBasicCandidateCell() {
        TransientVfxSystem vfx = new TransientVfxSystem(16);
        RoomSession session = newSession(vfx);
        session.rooms().loadInitialOverworld(0x92);

        int location = 0x55;
        int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + (location & 0xF0) + (location & 0x0F);
        session.rooms().activeRoom().roomObjectsArea()[areaIndex] = 0x5C;
        session.rooms().activeRoom().renderValues()[areaIndex] = 0x5C;

        assertTrue(session.playerItems().placeBomb(0x40, 0x50, 0, 0));
        for (int frame = 0; frame <= 136; frame++) {
            session.entities().tickEntities(frame, 0, 0);
        }

        assertEquals(0x04, session.rooms().activeRoom().roomObjectsArea()[areaIndex]);
        assertEquals(0x04, session.rooms().activeRoom().renderValues()[areaIndex]);
        assertEquals(0, vfx.activeCount());
        assertTrue(session.components.entityBridge().outbox.consumeBombExplosionEvents().stream()
                .anyMatch(event -> event.targetsRoomObjects() && event.countdown() == 0x16));
    }

    @Test
    void bombedOverworldBushSpawnsTheRomLiftableRockSmashEntity() {
        RoomSession session = newSession();
        session.rooms().loadInitialOverworld(0x92);

        int location = 0x55;
        int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + (location & 0xF0) + (location & 0x0F);
        session.rooms().activeRoom().roomObjectsArea()[areaIndex] = 0x5C;
        session.rooms().activeRoom().renderValues()[areaIndex] = 0x5C;

        assertTrue(session.playerItems().placeBomb(0x40, 0x50, 0, 0));
        for (int frame = 0; frame <= 136; frame++) {
            session.entities().tickEntities(frame, 0, 0);
        }

        RoomEntity smash = session.rooms().activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x05)
                .findFirst()
                .orElseThrow();
        assertEquals(0x58, smash.x());
        assertEquals(0x60, smash.y());
        assertEquals(0x06, smash.spriteVariant());
        assertEquals(
                EntitySpriteDefinition.Shape.DYNAMIC, smash.spriteDefinition().shape());
        assertEquals(0x19, smash.spriteDefinition().bank());
        assertEquals(0x7B50, smash.spriteDefinition().address());
    }

    @Test
    void bombExplosionTurnsTheGiantSkullIntoPersistentRockyGround() {
        RoomSession session = newSession();
        session.rooms().loadInitialOverworld(0x97);

        assertTrue(session.playerItems().placeBomb(0x40, 0x50, 0, 0));
        for (int frame = 0; frame <= 140; frame++) {
            session.entities().tickEntities(frame, 0, 0);
        }

        int[] skullLocations = {0x44, 0x45, 0x54, 0x55};
        for (int location : skullLocations) {
            int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + (location & 0xF0) + (location & 0x0F);
            assertEquals(0x09, session.rooms().activeRoom().roomObjectsArea()[areaIndex]);
            assertEquals(0x09, session.rooms().activeRoom().renderValues()[areaIndex]);
        }
        assertEquals(0x04, RoomDebugFixture.of(session).overworldRoomStatusForTest(0x97));

        session.rooms().loadOverworld(0x97);
        for (int location : skullLocations) {
            int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + (location & 0xF0) + (location & 0x0F);
            assertEquals(0x09, session.rooms().activeRoom().roomObjectsArea()[areaIndex]);
        }
    }

    @Test
    void bombedGiantSkullSpawnsTheRomRubbleGridAndPuzzleJingle() {
        RoomSession session = newSession();
        session.rooms().loadInitialOverworld(0x97);

        assertTrue(session.playerItems().placeBomb(0x40, 0x50, 0, 0));
        boolean destroyed = false;
        for (int frame = 0; frame <= 140; frame++) {
            session.entities().tickEntities(frame, 0, 0);
            if (RoomDebugFixture.of(session).overworldRoomStatusForTest(0x97) == 0x04) {
                destroyed = true;
                break;
            }
        }

        assertTrue(destroyed);
        int[][] expectedPositions = {{0x48, 0x50}, {0x58, 0x50}, {0x48, 0x60}, {0x58, 0x60}};
        List<RoomEntity> rubble = session.rooms().activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x05)
                .toList();
        assertEquals(4, rubble.size());
        for (int[] position : expectedPositions) {
            RoomEntity entity = rubble.stream()
                    .filter(candidate -> candidate.x() == position[0] && candidate.y() == position[1])
                    .findFirst()
                    .orElseThrow();
            assertEquals(0x05, entity.type());
            assertEquals(0x05, entity.spriteVariant());
            assertEquals(
                    EntitySpriteDefinition.Shape.DYNAMIC,
                    entity.spriteDefinition().shape());
            assertEquals(0x19, entity.spriteDefinition().bank());
            assertEquals(0x7B50, entity.spriteDefinition().address());
        }
        List<EntityCombatEvent> events =
                session.components.entityBridge().outbox.consumeEntityEvents();
        assertEquals(1, events.size());
        assertEquals(EntityCombatEvent.SoundChannel.JINGLE, events.getFirst().soundChannel());
        assertEquals(0x02, events.getFirst().soundId());
    }

    @Test
    void liveRoomEntityProbePreservesHookshotablePhysicsForTheChain() {
        RoomSession session = newSession();
        session.rooms().loadInitialOverworld(0x92);
        fillActiveObjects(session, 0x6E);

        EntityBackgroundCollisionResult result = session.components
                .roomCollisionQueries()
                .entityBackgroundCollisionResultForTest(
                        syntheticEntity(HookshotChainMotion.ENTITY_TYPE),
                        EntityBackgroundCollisionResult.RIGHT,
                        0x40,
                        0x40);

        assertEquals(0x6E, result.objectId());
        assertEquals(0x60, result.physicsFlag());
        assertTrue(result.blocked());
    }

    @Test
    void returnsProjectileEventsFromTheEntityPassWhenLinkStateIsProvided() {
        RoomSession session = newSession();
        session.rooms().loadInitialOverworld(0x92);

        List<EntityProjectileEvent> events =
                session.entities().tickEntitiesWithProjectileEvents(0, 0x08, 0x10, 0x00, 0x02, 0x00, false);

        assertTrue(events.isEmpty());
        assertEquals(
                session.rooms().activeRoom().entities(),
                session.rooms().renderSnapshot().entities());
    }

    @Test
    void publishesARealRomProjectileIntoTheSessionRenderSnapshot() {
        RoomSession session = newSession();
        session.rooms().loadInitialOverworld(0x2F);
        RoomEntity parent = session.rooms().activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x09)
                .findFirst()
                .orElseThrow();

        RoomEntity projectile = null;
        for (int frame = 0; frame < 0x40 && projectile == null; frame++) {
            RoomEntity currentParent =
                    session.rooms().activeRoom().entities().slots().get(parent.slot());
            session.entities()
                    .tickEntitiesWithProjectileEvents(
                            frame, (currentParent.x() + 0x20) & 0xFF, currentParent.y(), 0x00, 0x00, 0x00, false);
            projectile = session.rooms().activeRoom().entities().loadedEntities().stream()
                    .filter(entity -> entity.type() == 0x0A)
                    .findFirst()
                    .orElse(null);
        }

        assertNotNull(projectile);
        assertEquals(-1, projectile.sourceLoadOrder());
        assertEquals(0x03, projectile.spriteDefinition().bank());
        assertEquals(0x6A1E, projectile.spriteDefinition().address());
        assertTrue(projectile.spriteDefinition().supported());
        assertEquals(
                session.rooms().activeRoom().entities(),
                session.rooms().renderSnapshot().entities());
        assertNotNull(session.rooms().renderSnapshot().entities().spriteTiles());
    }

    @Test
    void richardAcceptsFiveLeavesMovesAsideAndOpensThePotholeFieldPassage() {
        RoomSession session = newSession();
        byte[] indoorB = new byte[0x100];
        indoorB[0xC7] = 0x10;
        session.story().restoreRoomStatuses(new byte[0x100], new byte[0x100], indoorB, new byte[0x20]);
        session.components.entityInputs().setEntityGoldenLeavesCount(5);
        session.rooms().loadIndoor(0x10, 0xC7);
        RoomEntity richard = session.rooms().activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x95)
                .findFirst()
                .orElseThrow();
        int linkX = richard.x() + 0x0F;

        session.entities().tickEntities(0, linkX, richard.y(), 0, Link.DIRECTION_LEFT);
        session.entities().tickEntities(1, linkX, richard.y(), 0, Link.DIRECTION_LEFT);
        session.entityState().setEntityActionButtonsHeld(true, false);
        session.entities().tickEntities(2, linkX, richard.y(), 0, Link.DIRECTION_LEFT);

        assertEquals(
                0x13D,
                session.entityState().consumeEntityDialogRequests().getFirst().globalDialogId());
        assertEquals(
                List.of(new RichardProgressEvent(richard.slot(), RichardProgressEvent.Kind.RETURN_ALL_LEAVES, 0xFF)),
                session.components.entityBridge().outbox.consumeRichardProgressEvents());

        session.entityState().setEntityActionButtonsHeld(false, false);
        for (int frame = 3; frame <= 34; frame++) {
            session.entities().tickEntities(frame, 0x20, 0x70, 0, Link.DIRECTION_DOWN);
        }
        RoomEntity movedRichard =
                session.rooms().activeRoom().entities().slots().get(richard.slot());
        assertEquals(0x58, movedRichard.x());

        session.entities().tickEntities(35, 0x78, 0x20, 0, Link.DIRECTION_DOWN);
        Warp warp = session.components.roomTransitionController().consumeClearedDungeonWarp();
        assertEquals(Warp.CATEGORY_INDOOR, warp.category());
        assertEquals(0x11, warp.destMap());
        assertEquals(0xD8, warp.destRoom());
        assertEquals(0x88, warp.destX());
        assertEquals(0x70, warp.destY());
        assertEquals(0x30, session.components.entityBridge().outbox.consumePendingMusicFadeOutCountdown());
        assertEquals(
                List.of(new EntityCombatEvent(
                        richard.slot(), 0x95, 0, false, EntityCombatEvent.SoundChannel.NOISE, 0x06)),
                session.components.entityBridge().outbox.consumeEntityEvents());
    }

    @Test
    void boomerangSoundClockSurvivesRoomChangesAndWorldFramesWithoutEntityUpdates() {
        RoomSession session = newSession();
        session.rooms().loadOverworld(0x00);
        for (int frame = 0; frame < 12; frame++) {
            session.entityState().advanceBoomerangSoundClock(true);
            session.entities().tickEntities(frame, 0, 0);
        }
        session.rooms().loadOverworld(0x01);
        // Scrolling/fades keep WorldHandler's clock running without entities.
        for (int frame = 12; frame < 25; frame++) {
            session.entityState().advanceBoomerangSoundClock(true);
        }
        // The inventory gameplay handler leaves the world clock alone.
        for (int frame = 0; frame < 8; frame++) {
            session.entityState().advanceBoomerangSoundClock(false);
        }
        assertTrue(session.playerItems().fireBoomerang(0x40, 0x50, 0, 0, 0));
        session.components.entityBridge().outbox.consumeEntityEvents();
        session.entityState().advanceBoomerangSoundClock(true);
        session.entities().tickEntities(0, 0, 0);
        assertTrue(session.components.entityBridge().outbox.consumeEntityEvents().stream()
                .anyMatch(event -> event.type() == 1 && event.soundId() == 0x2D));
        session.entityState().advanceBoomerangSoundClock(true);
        session.entities().tickEntities(1, 0, 0);
        assertFalse(session.components.entityBridge().outbox.consumeEntityEvents().stream()
                .anyMatch(event -> event.type() == 1 && event.soundId() == 0x2D));
    }
}
