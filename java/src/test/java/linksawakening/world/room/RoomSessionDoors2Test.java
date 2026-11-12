package linksawakening.world.room;

import static linksawakening.world.room.RoomSessionTestSupport.*;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.Link;
import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.physics.PhysicsFlags;
import linksawakening.world.entity.EntityBackgroundCollisionResult;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.motion.HookshotBridgeMotion;
import linksawakening.world.transition.Warp;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class RoomSessionDoors2Test {

    @Test
    void tailCaveKeyholeFollowsTheRomLockedAndUnlockedPaths() {
        RoomSession session = newSession();
        List<GameplaySoundEvent> sounds = new ArrayList<>();
        session.entityState().setColorShellSoundSink(sounds::add);
        session.rooms().loadInitialOverworld(0xD3);
        int gateIndex = RoomConstants.ROOM_OBJECTS_BASE + 0x16;

        assertEquals(0xC2, session.rooms().activeRoom().roomObjectsArea()[gateIndex]);
        assertTrue(session.doors()
                .tryUnlockTailCaveKeyhole(
                        0x5A, 0x4A, linksawakening.state.Direction.fromJavaOrUnknown(Link.DIRECTION_UP), 0x01, false));
        assertEquals(0, RoomDebugFixture.of(session).overworldRoomStatusForTest(0xD3) & 0x10);
        assertEquals(0xC2, session.rooms().activeRoom().roomObjectsArea()[gateIndex]);
        assertEquals(
                0x230,
                session.entityState().consumeEntityDialogRequests().getFirst().globalDialogId());

        assertTrue(session.doors()
                .tryUnlockTailCaveKeyhole(
                        0x5A, 0x4A, linksawakening.state.Direction.fromJavaOrUnknown(Link.DIRECTION_UP), 0x01, true));
        assertEquals(0x00, session.components.entityBridge().outbox.consumePendingMusicTrack());
        assertEquals(0x10, RoomDebugFixture.of(session).overworldRoomStatusForTest(0xD3) & 0x10);
        assertEquals(0xC2, session.rooms().activeRoom().roomObjectsArea()[gateIndex]);
        assertEquals(0xDF, session.components.roomDoorInteractions().tailCaveKeyholeCountdownForTest());

        assertEquals(0, session.doors().tickTailCaveKeyholeSequence());
        assertEquals(List.of(GameplaySoundEvent.DOOR_UNLOCKED), sounds);
        while (session.components.roomDoorInteractions().tailCaveKeyholeCountdownForTest() > 0x18) {
            session.doors().tickTailCaveKeyholeSequence();
        }
        int gateTileX = 0x06 * 2;
        int gateTileY = 0x01 * 2;
        int gateTopLeft = gateTileY * RoomConstants.ROOM_TILE_WIDTH + gateTileX;
        assertEquals(0x0C, session.rooms().activeRoom().tileIds()[gateTopLeft]);
        assertEquals(0x1F, session.rooms().activeRoom().tileIds()[gateTopLeft + RoomConstants.ROOM_TILE_WIDTH]);
        while (session.components.roomDoorInteractions().tailCaveKeyholeCountdownForTest() > 0x08) {
            session.doors().tickTailCaveKeyholeSequence();
        }
        assertEquals(0xE3, session.rooms().activeRoom().roomObjectsArea()[gateIndex]);
        assertEquals(0xE3, session.rooms().activeRoom().gbcOverlay()[0x10]);
        assertEquals(0xE3, session.rooms().activeRoom().renderValues()[gateIndex]);
        assertEquals(
                List.of(
                        GameplaySoundEvent.DOOR_UNLOCKED,
                        GameplaySoundEvent.OPEN_KEY_CAVERN,
                        GameplaySoundEvent.DUNGEON_OPENED),
                sounds);
        int[] openedGateTiles = {
            session.rooms().activeRoom().tileIds()[gateTopLeft],
            session.rooms().activeRoom().tileIds()[gateTopLeft + 1],
            session.rooms().activeRoom().tileIds()[gateTopLeft + RoomConstants.ROOM_TILE_WIDTH],
            session.rooms().activeRoom().tileIds()[gateTopLeft + RoomConstants.ROOM_TILE_WIDTH + 1]
        };

        assertArrayEquals(new int[] {0x7E, 0x7E, 0x1F, 0x1F}, openedGateTiles);
        session.rooms().loadInitialOverworld(0xD3);
        assertEquals(0xE3, session.rooms().activeRoom().roomObjectsArea()[gateIndex]);
        assertEquals(0xE3, session.rooms().activeRoom().gbcOverlay()[0x10]);
        assertEquals(0xE3, session.rooms().activeRoom().renderValues()[gateIndex]);
        assertArrayEquals(openedGateTiles, new int[] {
            session.rooms().activeRoom().tileIds()[gateTopLeft],
            session.rooms().activeRoom().tileIds()[gateTopLeft + 1],
            session.rooms().activeRoom().tileIds()[gateTopLeft + RoomConstants.ROOM_TILE_WIDTH],
            session.rooms().activeRoom().tileIds()[gateTopLeft + RoomConstants.ROOM_TILE_WIDTH + 1]
        });
        Warp tailCaveWarp = session.rooms().activeRoom().warps().stream()
                .filter(warp -> warp.tileLocation() == 0x16)
                .findFirst()
                .orElseThrow();
        assertEquals(0x00, tailCaveWarp.destMap());
        assertEquals(0x17, tailCaveWarp.destRoom());
    }

    @Test
    void tailCaveKeyholeIgnoresFramesThatDidNotCollideFacingUp() {
        RoomSession session = newSession();
        session.rooms().loadInitialOverworld(0xD3);

        assertFalse(session.doors()
                .tryUnlockTailCaveKeyhole(
                        0x5A, 0x4A, linksawakening.state.Direction.fromJavaOrUnknown(Link.DIRECTION_DOWN), 0x01, true));
        assertFalse(session.doors()
                .tryUnlockTailCaveKeyhole(
                        0x5A, 0x4A, linksawakening.state.Direction.fromJavaOrUnknown(Link.DIRECTION_UP), 0x00, true));
        assertFalse(session.doors()
                .tryUnlockTailCaveKeyhole(
                        0x20, 0x20, linksawakening.state.Direction.fromJavaOrUnknown(Link.DIRECTION_UP), 0x01, true));
        assertEquals(0, RoomDebugFixture.of(session).overworldRoomStatusForTest(0xD3) & 0x10);
        assertTrue(session.entityState().consumeEntityDialogRequests().isEmpty());
    }

    @Test
    void tailCaveKeyholeAcceptsTheSecondRomUpwardCollisionProbe() {
        RoomSession session = newSession();
        session.rooms().loadInitialOverworld(0xD3);

        assertTrue(session.doors()
                .tryUnlockTailCaveKeyhole(
                        0x58, 0x4A, linksawakening.state.Direction.fromJavaOrUnknown(Link.DIRECTION_UP), 0x01, false));
        assertEquals(
                0x230,
                session.entityState().consumeEntityDialogRequests().getFirst().globalDialogId());
    }

    @Test
    void tailCaveRumbleRestoresDefaultMusicAndKeepsTheFinalMotionBlockFrame() {
        RoomSession session = newSession();
        session.story().setEntityDefaultMusicTrack(0x05);
        session.rooms().loadInitialOverworld(0xD3);
        assertTrue(session.doors()
                .tryUnlockTailCaveKeyhole(
                        0x5A, 0x4A, linksawakening.state.Direction.fromJavaOrUnknown(Link.DIRECTION_UP), 0x01, true));
        assertEquals(0x00, session.components.entityBridge().outbox.consumePendingMusicTrack());

        while (session.components.roomDoorInteractions().tailCaveKeyholeCountdownForTest() > 0x0A) {
            session.doors().tickTailCaveKeyholeSequence();
        }
        assertEquals(0x50, session.components.roomDoorInteractions().nextWorldMusicTrackCountdownForTest());
        while (session.components.roomDoorInteractions().tailCaveKeyholeCountdownForTest() > 1) {
            session.doors().tickTailCaveKeyholeSequence();
        }
        assertTrue(session.doors().tailCaveKeyholeSequenceActive());
        session.doors().tickTailCaveKeyholeSequence();
        assertTrue(session.doors().tailCaveKeyholeSequenceActive());
        session.doors().tickTailCaveKeyholeSequence();
        assertFalse(session.doors().tailCaveKeyholeSequenceActive());

        int remainingMusicFrames = session.components.roomDoorInteractions().nextWorldMusicTrackCountdownForTest();
        for (int frame = 1; frame < remainingMusicFrames; frame++) {
            session.doors().tickTailCaveKeyholeSequence();
            assertEquals(-1, session.components.entityBridge().outbox.consumePendingMusicTrack());
        }
        session.doors().tickTailCaveKeyholeSequence();
        assertEquals(0x05, session.components.entityBridge().outbox.consumePendingMusicTrack());
    }

    @Test
    void magicRodBurnsAnIndoorFrozenBlockIntoTheSideViewEmptyObject() {
        RoomSession session = newSession();
        session.rooms().loadIndoor(0x00, 0x00, Warp.CATEGORY_SIDESCROLL);

        int location = 0x22;
        int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + (location & 0xF0) + (location & 0x0F);
        session.rooms().activeRoom().roomObjectsArea()[areaIndex] = 0x8A;

        assertTrue(session.playerItems().fireMagicRodFireball(0x20, 0x28, 0, 0));
        session.entities().tickEntitiesWithProjectileEvents(0, 0x20, 0x28, 0, 0, 0, false);

        assertEquals(0x04, session.rooms().activeRoom().roomObjectsArea()[areaIndex]);
        assertTrue(session.components.entityBridge().outbox.consumeEntityEvents().stream()
                .anyMatch(event -> event.type() == 0x04 && event.soundId() == 0x13));
    }

    @Test
    void magicPowderIgnitesAnIndoorTorchThroughTheLiveRoomBoundary() {
        RoomSession session = newSession();
        session.rooms().loadIndoor(0x00, 0x00);

        int location = 0x22;
        int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + (location & 0xF0) + (location & 0x0F);
        session.rooms().activeRoom().roomObjectsArea()[areaIndex] = 0xAB;

        assertTrue(session.playerItems().sprinkleMagicPowder(0x13, 0x29, 0, 0));
        for (int frame = 0; frame < 16; frame++) {
            session.entities().tickEntitiesWithProjectileEvents(frame, 0x20, 0x20, 0, 0, 0, false);
        }

        assertEquals(0xAC, session.rooms().activeRoom().roomObjectsArea()[areaIndex]);
        assertEquals(1, RoomDebugFixture.of(session).roomTriggerCountForTest());
        assertTrue(session.components.entityBridge().outbox.consumeEntityEvents().stream()
                .anyMatch(event -> event.type() == 0x08
                        && event.soundChannel() == EntityCombatEvent.SoundChannel.NOISE
                        && event.soundId() == 0x12));
    }

    @Test
    void revolvingDoorTorchExpiryStillChangesTheObjectAndRoomTrigger() {
        RoomSession session = newSession();
        session.rooms().loadIndoor(0x00, 0x00);
        int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + 0x22;
        session.rooms().activeRoom().roomObjectsArea()[areaIndex] = 0xAB;
        assertTrue(session.playerItems().sprinkleMagicPowder(0x13, 0x29, 0, 0));
        for (int frame = 0; frame < 16; frame++) {
            session.entities().tickEntitiesWithProjectileEvents(frame, 0x20, 0x20, 0, 0, 0, false);
        }
        assertEquals(1, RoomDebugFixture.of(session).roomTriggerCountForTest());
        int[] litTiles = torchTiles(session, 0x22);
        int initialEffect = RoomDebugFixture.of(session).indoorTorchPaletteEffectAddressForTest();
        for (int frame = 16; frame < 1024; frame++) {
            session.entities().tickEntitiesWithProjectileEvents(frame, 0x20, 0x20, 0, 5, 0, false);
        }
        assertEquals(0xAB, session.rooms().activeRoom().roomObjectsArea()[areaIndex]);
        assertEquals(0, RoomDebugFixture.of(session).roomTriggerCountForTest());
        assertEquals(initialEffect, RoomDebugFixture.of(session).indoorTorchPaletteEffectAddressForTest());
        assertArrayEquals(litTiles, torchTiles(session, 0x22));
    }

    @Test
    void shovelKeepsTheSourcePokePathForBlockedIndoorObjects() {
        RoomSession session = newSession();
        session.rooms().loadIndoor(0x00, 0x00);

        int location = 0x22;
        int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + (location & 0xF0) + (location & 0x0F);
        session.rooms().activeRoom().roomObjectsArea()[areaIndex] = 0x04;

        ShovelStartResult start = session.playerItems().startShovel(0x14, 0x26, 0, false);

        assertTrue(start.started());
        assertTrue(start.poking());
        for (int timer = 1; timer <= 0x10; timer++) {
            session.playerItems().advanceShovel(0x14, 0x26, 0, timer);
        }
        assertEquals(0x04, session.rooms().activeRoom().roomObjectsArea()[areaIndex]);
    }

    @Test
    void bombExplosionOpensAnOutdoorCaveDoorWithTheRomGbcTilesAndPersistsIt() {
        RoomSession session = newSession();
        session.rooms().loadInitialOverworld(0x13);

        int location = 0x05;
        int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + (location & 0xF0) + (location & 0x0F);
        assertEquals(0xBA, session.rooms().activeRoom().roomObjectsArea()[areaIndex]);
        assertEquals(0xBA, session.rooms().activeRoom().renderValues()[areaIndex]);
        assertEquals(location, session.rooms().activeRoom().firstWarp().tileLocation());

        assertTrue(session.playerItems().placeBomb(0x50, 0x18, 0, 0));
        for (int frame = 0; frame <= 146; frame++) {
            session.entities().tickEntities(frame, 0, 0);
        }

        assertEquals(0xE1, session.rooms().activeRoom().roomObjectsArea()[areaIndex]);
        assertEquals(0xE1, session.rooms().activeRoom().renderValues()[areaIndex]);
        assertEquals(0x04, RoomDebugFixture.of(session).overworldRoomStatusForTest(0x13));
        assertEquals(location, session.rooms().activeRoom().firstWarp().tileLocation());

        int tileIndex = 0x0A;
        int[] tileIds = session.rooms().activeRoom().tileIds();
        assertEquals(0x64, tileIds[tileIndex]);
        assertEquals(0x66, tileIds[tileIndex + 1]);
        assertEquals(0x64, tileIds[tileIndex + RoomConstants.ROOM_TILE_WIDTH]);
        assertEquals(0x66, tileIds[tileIndex + RoomConstants.ROOM_TILE_WIDTH + 1]);

        List<EntityCombatEvent> events =
                session.components.entityBridge().outbox.consumeEntityEvents();
        assertEquals(1, events.size());
        assertEquals(EntityCombatEvent.SoundChannel.JINGLE, events.getFirst().soundChannel());
        assertEquals(0x02, events.getFirst().soundId());

        session.rooms().loadOverworld(0x13);
        assertEquals(0xE1, session.rooms().activeRoom().roomObjectsArea()[areaIndex]);
        assertEquals(0xE1, session.rooms().activeRoom().renderValues()[areaIndex]);
        tileIds = session.rooms().activeRoom().tileIds();
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
        session.rooms().loadIndoor(0x06, 0x18);

        int location = 0x02;
        int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + (location & 0xF0) + (location & 0x0F);
        assertEquals(0x3F, session.rooms().activeRoom().roomObjectsArea()[areaIndex]);

        assertTrue(session.playerItems().placeBomb(0x30, 0x18, 0, 0));
        for (int frame = 0; frame <= 146; frame++) {
            session.entities().tickEntities(frame, 0, 0);
        }

        assertEquals(0x3D, session.rooms().activeRoom().roomObjectsArea()[areaIndex]);
        assertEquals(0x04, RoomDebugFixture.of(session).indoorRoomStatusForTest(0x06, 0x18) & 0x04);
        assertEquals(0x08, RoomDebugFixture.of(session).indoorRoomStatusForTest(0x06, 0x14));
        List<EntityCombatEvent> events =
                session.components.entityBridge().outbox.consumeEntityEvents();
        assertEquals(1, events.size());
        assertEquals(EntityCombatEvent.SoundChannel.JINGLE, events.getFirst().soundChannel());
        assertEquals(0x02, events.getFirst().soundId());

        int tileIndex = 0x04;
        int[] tileIds = session.rooms().activeRoom().tileIds();
        assertEquals(0x72, tileIds[tileIndex]);
        assertEquals(0x72, tileIds[tileIndex + 1]);
        assertEquals(0x73, tileIds[tileIndex + RoomConstants.ROOM_TILE_WIDTH]);
        assertEquals(0x73, tileIds[tileIndex + RoomConstants.ROOM_TILE_WIDTH + 1]);

        session.rooms().loadIndoor(0x06, 0x18);
        assertEquals(0x3D, session.rooms().activeRoom().roomObjectsArea()[areaIndex]);
        tileIds = session.rooms().activeRoom().tileIds();
        assertEquals(0x72, tileIds[tileIndex]);
        assertEquals(0x73, tileIds[tileIndex + 1]);
        assertEquals(0x72, tileIds[tileIndex + RoomConstants.ROOM_TILE_WIDTH]);
        assertEquals(0x73, tileIds[tileIndex + RoomConstants.ROOM_TILE_WIDTH + 1]);
    }

    @Test
    void bombExplosionUsesTheRomHorizontalIndoorPassageTiles() {
        RoomSession session = newSession();
        session.rooms().loadIndoor(0x00, 0x0D);

        int location = 0x30;
        int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + (location & 0xF0) + (location & 0x0F);
        assertEquals(0x41, session.rooms().activeRoom().roomObjectsArea()[areaIndex]);

        assertTrue(session.playerItems().placeBomb(0x10, 0x48, 0, 0));
        for (int frame = 0; frame <= 146; frame++) {
            session.entities().tickEntities(frame, 0, 0);
        }

        assertEquals(0x3E, session.rooms().activeRoom().roomObjectsArea()[areaIndex]);
        assertEquals(0x02, RoomDebugFixture.of(session).indoorRoomStatusForTest(0x00, 0x0D) & 0x02);
        assertEquals(0x01, RoomDebugFixture.of(session).indoorRoomStatusForTest(0x00, 0x0C));

        int tileIndex = 3 * 2 * RoomConstants.ROOM_TILE_WIDTH;
        int[] tileIds = session.rooms().activeRoom().tileIds();
        assertEquals(0x69, tileIds[tileIndex]);
        assertEquals(0x79, tileIds[tileIndex + 1]);
        assertEquals(0x69, tileIds[tileIndex + RoomConstants.ROOM_TILE_WIDTH]);
        assertEquals(0x79, tileIds[tileIndex + RoomConstants.ROOM_TILE_WIDTH + 1]);
    }

    @Test
    void bombExplosionBreaksAnIndoorBombableBlockAndPersistsTheFloor() {
        RoomSession session = newSession();
        session.rooms().loadIndoor(0x0A, 0x45);

        int location = 0x22;
        int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + (location & 0xF0) + (location & 0x0F);
        assertEquals(0xA9, session.rooms().activeRoom().roomObjectsArea()[areaIndex]);

        assertTrue(session.playerItems().placeBomb(0x30, 0x34, 0, 0));
        for (int frame = 0; frame <= 146; frame++) {
            session.entities().tickEntities(frame, 0, 0);
        }

        assertEquals(0x0D, session.rooms().activeRoom().roomObjectsArea()[areaIndex]);
        assertEquals(0x40, RoomDebugFixture.of(session).indoorRoomStatusForTest(0x0A, 0x45) & 0x40);

        int tileIndex = 4 * RoomConstants.ROOM_TILE_WIDTH + 4;
        int[] tileIds = session.rooms().activeRoom().tileIds();
        assertEquals(0x10, tileIds[tileIndex]);
        assertEquals(0x12, tileIds[tileIndex + 1]);
        assertEquals(0x11, tileIds[tileIndex + RoomConstants.ROOM_TILE_WIDTH]);
        assertEquals(0x13, tileIds[tileIndex + RoomConstants.ROOM_TILE_WIDTH + 1]);

        RoomEntity smash = session.rooms().activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x05 && entity.x() == 0x28 && entity.y() == 0x30)
                .findFirst()
                .orElseThrow();
        assertTrue(smash.spriteVariant() >= 0x02 && smash.spriteVariant() <= 0x05);
        assertEquals(
                EntitySpriteDefinition.Shape.DYNAMIC, smash.spriteDefinition().shape());

        session.rooms().loadIndoor(0x0A, 0x45);
        assertEquals(0x0D, session.rooms().activeRoom().roomObjectsArea()[areaIndex]);
        tileIds = session.rooms().activeRoom().tileIds();
        assertEquals(0x10, tileIds[tileIndex]);
        assertEquals(0x11, tileIds[tileIndex + 1]);
        assertEquals(0x12, tileIds[tileIndex + RoomConstants.ROOM_TILE_WIDTH]);
        assertEquals(0x13, tileIds[tileIndex + RoomConstants.ROOM_TILE_WIDTH + 1]);
    }

    @Test
    void indoorHookshotBridgeRewritesPaddedObjectsAndBackgroundTiles() {
        RoomSession session = newSession();
        session.rooms().loadIndoor(0x00, 0x0F);
        fillActiveObjects(session, 0x9E);

        assertTrue(session.playerItems().fireHookshot(0x40, 0x40, 0, 2, false, false));
        session.entities().tickEntities(0, 0x40, 0x40, 0, 2);

        int objectIndex = RoomConstants.ROOM_OBJECTS_BASE + 0x30 + (0x30 >>> 4);
        assertEquals(0x9D, session.rooms().activeRoom().roomObjectsArea()[objectIndex]);
        RoomEntity bridge = session.rooms().activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == HookshotBridgeMotion.ENTITY_TYPE)
                .findFirst()
                .orElseThrow();
        assertEquals(0x40, bridge.y());

        int tileIndex = (0x30 >>> 3) * RoomConstants.ROOM_TILE_WIDTH + (0x30 >>> 3);
        int[] tileIds = session.rooms().activeRoom().tileIds();
        assertEquals(0x04, tileIds[tileIndex]);
        assertEquals(0x05, tileIds[tileIndex + 1]);
        assertEquals(0x08, tileIds[tileIndex + RoomConstants.ROOM_TILE_WIDTH]);
        assertEquals(0x09, tileIds[tileIndex + RoomConstants.ROOM_TILE_WIDTH + 1]);

        session.entities().tickEntities(1, 0x40, 0x40, 0, 2);
        RoomEntity movedBridge = session.rooms().activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == HookshotBridgeMotion.ENTITY_TYPE)
                .findFirst()
                .orElseThrow();
        assertEquals(0x43, movedBridge.y());

        session.rooms().activeRoom().roomObjectsArea()[objectIndex] = 0x00;
        session.entities().tickEntities(2, 0x40, 0x40, 0, 2);
        assertFalse(session.rooms().activeRoom().entities().loadedEntities().stream()
                .anyMatch(entity -> entity.type() == HookshotBridgeMotion.ENTITY_TYPE));
        assertEquals(0x9D, session.rooms().activeRoom().roomObjectsArea()[objectIndex]);
        tileIds = session.rooms().activeRoom().tileIds();
        assertEquals(0x04, tileIds[tileIndex]);
        assertEquals(0x05, tileIds[tileIndex + 1]);
        assertEquals(0x04, tileIds[tileIndex + RoomConstants.ROOM_TILE_WIDTH]);
        assertEquals(0x05, tileIds[tileIndex + RoomConstants.ROOM_TILE_WIDTH + 1]);
    }

    @Test
    void liveOctorokUsesEntityPhysicsInsteadOfLinkBlockingPolicy() {
        RoomSession session = newSession();
        session.rooms().loadInitialOverworld(0x2F);
        RoomEntity octorok = session.rooms().activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x09)
                .findFirst()
                .orElseThrow();

        fillActiveObjects(session, 0x0E); // Overworld deep-water physics $07.
        EntityBackgroundCollisionResult water = session.components
                .roomCollisionQueries()
                .entityBackgroundCollisionResultForTest(
                        octorok, EntityBackgroundCollisionResult.RIGHT, octorok.x(), octorok.y());
        assertFalse(water.blocked());
        assertEquals(0x0E, water.objectId());
        assertEquals(PhysicsFlags.DEEP_WATER, water.physicsFlag());

        fillActiveObjects(session, 0x00); // Overworld solid physics $01.
        EntityBackgroundCollisionResult solid = session.components
                .roomCollisionQueries()
                .entityBackgroundCollisionResultForTest(
                        octorok, EntityBackgroundCollisionResult.RIGHT, octorok.x(), octorok.y());
        assertTrue(solid.blocked());
        assertEquals(0x00, solid.objectId());
        assertEquals(PhysicsFlags.SOLID, solid.physicsFlag());
    }

    @Test
    void liveEntityCollisionProbeUsesRomLedgeTimerCadenceForIndoorAndOverworldRooms() {
        RoomSession indoor = newSession();
        indoor.rooms().loadIndoor(0x00, 0x0F);
        RoomEntity indoorEntity = indoor.rooms().activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x1E)
                .findFirst()
                .orElseThrow();
        fillActiveObjects(indoor, 0x23); // Indoors1 object $23 has physics $D0.
        RoomDebugFixture.of(indoor).setEntityThrownDirectionForTest(indoorEntity.slot(), 0x01);
        RoomDebugFixture.of(indoor).setEntityLedgeTimerForTest(indoorEntity.slot(), 0x02);

        EntityBackgroundCollisionResult indoorResult = indoor.components
                .roomCollisionQueries()
                .entityBackgroundCollisionResultForTest(
                        indoorEntity, EntityBackgroundCollisionResult.RIGHT, indoorEntity.x(), indoorEntity.y(), 0x01);
        assertFalse(indoorResult.blocked());
        assertEquals(0x01, RoomDebugFixture.of(indoor).entityLedgeTimerForTest(indoorEntity.slot()));

        RoomSession outdoor = newSession();
        outdoor.rooms().loadInitialOverworld(0x2F);
        RoomEntity outdoorEntity = outdoor.rooms().activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x09)
                .findFirst()
                .orElseThrow();
        fillActiveObjects(outdoor, 0xF3); // Overworld object $F3 has physics $D0.
        RoomDebugFixture.of(outdoor).setEntityThrownDirectionForTest(outdoorEntity.slot(), 0x01);
        RoomDebugFixture.of(outdoor).setEntityLedgeTimerForTest(outdoorEntity.slot(), 0x02);

        EntityBackgroundCollisionResult evenFrame = outdoor.components
                .roomCollisionQueries()
                .entityBackgroundCollisionResultForTest(
                        outdoorEntity,
                        EntityBackgroundCollisionResult.RIGHT,
                        outdoorEntity.x(),
                        outdoorEntity.y(),
                        0x02);
        assertFalse(evenFrame.blocked());
        assertEquals(0x02, RoomDebugFixture.of(outdoor).entityLedgeTimerForTest(outdoorEntity.slot()));

        EntityBackgroundCollisionResult oddFrame = outdoor.components
                .roomCollisionQueries()
                .entityBackgroundCollisionResultForTest(
                        outdoorEntity,
                        EntityBackgroundCollisionResult.RIGHT,
                        outdoorEntity.x(),
                        outdoorEntity.y(),
                        0x03);
        assertFalse(oddFrame.blocked());
        assertEquals(0x01, RoomDebugFixture.of(outdoor).entityLedgeTimerForTest(outdoorEntity.slot()));
    }

    @Test
    void staircaseWarpResetsTransientEnemyClearsBeforeReturning() {
        RoomSession session = newSession();
        session.rooms().loadIndoor(0, 0x19, Warp.CATEGORY_SIDESCROLL);
        session.entityState().clearEntity(0);
        session.entityState().clearEntity(1);
        session.rooms().loadIndoor(0, 0x19, Warp.CATEGORY_SIDESCROLL);
        assertEquals(0, session.rooms().activeRoom().entities().loadedEntities().size());

        session.transitions().loadWarpDestination(new Warp(Warp.CATEGORY_INDOOR, 0, 0x01, 40, 16, 0));
        session.transitions().loadWarpDestination(new Warp(Warp.CATEGORY_SIDESCROLL, 0, 0x18, 40, 16, 0));
        session.rooms().loadIndoor(0, 0x19, Warp.CATEGORY_SIDESCROLL);
        assertEquals(
                2,
                session.rooms().activeRoom().entities().loadedEntities().stream()
                        .filter(entity -> entity.type() == 0x9F)
                        .count());
    }

    @Test
    void loadsTheDisassemblyNewGameRoomThroughTheIndoorPath() {
        RoomSession session = newSession();

        session.rooms().loadIndoor(0x10, 0xA3);

        assertEquals(0x10, session.rooms().mapId());
        assertEquals(0xA3, session.rooms().currentRoomId());
        assertEquals(Warp.CATEGORY_INDOOR, session.rooms().mapCategory());
    }

    @Test
    void bothBraceletsLiftOutdoorRocksAndBushesAfterEightPullFrames() {
        for (int level = 1; level <= 2; level++) {
            for (int object : new int[] {0x20, 0x5C}) {
                RoomSession session = newSession();
                session.rooms().loadInitialOverworld(0x92);
                int location = 0x33;
                session.rooms().activeRoom().roomObjectsArea()[RoomConstants.ROOM_OBJECTS_BASE + location] = object;
                for (int frame = 0; frame < 7; frame++) {
                    assertTrue(session.doors()
                            .tryLiftObject(
                                    0x30,
                                    0x2B,
                                    linksawakening.state.Direction.fromJavaOrUnknown(Link.DIRECTION_UP),
                                    true,
                                    8,
                                    false,
                                    level));
                    assertEquals(
                            object,
                            session.rooms().activeRoom().roomObjectsArea()[RoomConstants.ROOM_OBJECTS_BASE + location]);
                }
                assertTrue(session.doors()
                        .tryLiftObject(
                                0x30,
                                0x2B,
                                linksawakening.state.Direction.fromJavaOrUnknown(Link.DIRECTION_UP),
                                true,
                                8,
                                false,
                                level));
                assertEquals(
                        4, session.rooms().activeRoom().roomObjectsArea()[RoomConstants.ROOM_OBJECTS_BASE + location]);
                assertTrue(session.rooms().liftedEntityState().active());
                var lifted = session.rooms().activeRoom().entities().loadedEntities().stream()
                        .filter(e -> e.status() == EntityStatus.LIFTED)
                        .findFirst()
                        .orElseThrow();
                assertEquals(object == 0x5C ? 1 : 0, lifted.spriteVariant());
            }
        }
    }

    @Test
    void indoorPowerBraceletPullLiftsSourcePotOnEighthFrame() {
        RoomSession session = newSession();
        session.rooms().loadInitialOverworld(0x00);
        session.rooms().loadIndoor(0x01, 0x21);
        int potLocation = 0x23;
        int potIndex = RoomConstants.ROOM_OBJECTS_BASE + potLocation;
        assertEquals(0x20, session.rooms().activeRoom().roomObjectsArea()[potIndex]);

        for (int frame = 0; frame < 8; frame++) {
            assertFalse(session.doors()
                    .tryLiftIndoorObject(
                            0x30,
                            0x1B,
                            linksawakening.state.Direction.fromJavaOrUnknown(Link.DIRECTION_UP),
                            false,
                            0x08));
        }
        assertEquals(0x20, session.rooms().activeRoom().roomObjectsArea()[potIndex]);

        for (int frame = 0; frame < 7; frame++) {
            assertTrue(session.doors()
                    .tryLiftIndoorObject(
                            0x30,
                            0x1B,
                            linksawakening.state.Direction.fromJavaOrUnknown(Link.DIRECTION_UP),
                            true,
                            0x08));
        }
        assertTrue(session.doors()
                .tryLiftIndoorObject(
                        0x30, 0x1B, linksawakening.state.Direction.fromJavaOrUnknown(Link.DIRECTION_UP), true, 0x00));
        for (int frame = 0; frame < 7; frame++) {
            assertTrue(session.doors()
                    .tryLiftIndoorObject(
                            0x30,
                            0x1B,
                            linksawakening.state.Direction.fromJavaOrUnknown(Link.DIRECTION_UP),
                            true,
                            0x08));
        }
        assertEquals(0x20, session.rooms().activeRoom().roomObjectsArea()[potIndex]);

        assertTrue(session.doors()
                .tryLiftIndoorObject(
                        0x30, 0x1B, linksawakening.state.Direction.fromJavaOrUnknown(Link.DIRECTION_UP), true, 0x08));
        assertEquals(0x0D, session.rooms().activeRoom().roomObjectsArea()[potIndex]);
        RoomEntity liftedPot = session.rooms().activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x05 && entity.status() == EntityStatus.LIFTED)
                .findFirst()
                .orElseThrow();
        assertEquals(0x38, liftedPot.x());
        assertEquals(0x2B, liftedPot.y());
        assertEquals(0, liftedPot.spriteVariant());
        assertEquals(liftedPot.slot(), session.rooms().liftedEntityState().slot());
        assertEquals(0xD2, RoomDebugFixture.of(session).entityPhysicsFlagsForTest(liftedPot.slot()));

        int secondPotIndex = RoomConstants.ROOM_OBJECTS_BASE + 0x24;
        assertEquals(0x20, session.rooms().activeRoom().roomObjectsArea()[secondPotIndex]);
        assertFalse(session.doors()
                .tryLiftIndoorObject(
                        0x40, 0x1B, linksawakening.state.Direction.fromJavaOrUnknown(Link.DIRECTION_UP), true, 0x08));
        assertEquals(0x20, session.rooms().activeRoom().roomObjectsArea()[secondPotIndex]);
    }

    @Test
    void pieceOfPowerShortensIndoorPotPullToThreeFrames() {
        RoomSession session = newSession();
        session.rooms().loadInitialOverworld(0x00);
        session.rooms().loadIndoor(0x01, 0x21);
        int potIndex = RoomConstants.ROOM_OBJECTS_BASE + 0x23;

        for (int frame = 0; frame < 2; frame++) {
            assertTrue(session.doors()
                    .tryLiftIndoorObject(
                            0x30,
                            0x1B,
                            linksawakening.state.Direction.fromJavaOrUnknown(Link.DIRECTION_UP),
                            true,
                            0x08,
                            true));
            assertEquals(0x20, session.rooms().activeRoom().roomObjectsArea()[potIndex]);
        }
        assertTrue(session.doors()
                .tryLiftIndoorObject(
                        0x30,
                        0x1B,
                        linksawakening.state.Direction.fromJavaOrUnknown(Link.DIRECTION_UP),
                        true,
                        0x08,
                        true));
        assertEquals(0x0D, session.rooms().activeRoom().roomObjectsArea()[potIndex]);
    }

    @Test
    void keyCavernEntranceRoomOpensNorthShutterWhenThrownPotHitsIt() {
        RoomSession session = newSession();
        List<GameplaySoundEvent> sounds = new ArrayList<>();
        session.entityState().setColorShellSoundSink(sounds::add);
        session.rooms().loadInitialOverworld(0xB5);
        session.rooms().loadIndoor(0x02, 0x52);

        int northDoorIndex = RoomConstants.ROOM_OBJECTS_BASE + 0x04;
        for (int frame = 0; frame < 9; frame++) session.entities().tickEntities(frame, 80, 64);
        assertEquals(0x2B, RoomDebugFixture.of(session).activeRoomEventForTest());
        assertTrue(session.rooms().activeRoom().roomObjectsArea()[northDoorIndex] >= 0x35);
        assertTrue(session.rooms().activeRoom().roomObjectsArea()[northDoorIndex] < 0x3D);

        // Lift the authored pot at location $52, then throw it north at the shutter.
        for (int frame = 0; frame < 8; frame++) {
            assertTrue(session.doors()
                    .tryLiftIndoorObject(
                            0x18,
                            0x4B,
                            linksawakening.state.Direction.fromJavaOrUnknown(Link.DIRECTION_UP),
                            true,
                            0x08));
        }
        int frame = 8;
        while (session.rooms().liftedEntityState().carryState() != 0x01 && frame < 0x40) {
            session.entities().tickEntities(frame++, 0x18, 0x4B, 0, Link.DIRECTION_UP);
        }
        assertEquals(0x01, session.rooms().liftedEntityState().carryState());
        session.entities().tickEntities(frame++, 0x48, 0x28, 0, Link.DIRECTION_UP);
        assertTrue(session.playerItems().throwLiftedEntity(linksawakening.state.Direction.fromJava(Link.DIRECTION_UP)));
        while (RoomDebugFixture.of(session).activeRoomEventForTest() != 0 && frame < 0x100) {
            session.entities().tickEntities(frame++, 0x48, 0x28, 0, Link.DIRECTION_UP);
        }

        assertEquals(0, RoomDebugFixture.of(session).activeRoomEventForTest());
        assertFalse(session.rooms().activeRoom().roomObjectsArea()[northDoorIndex] >= 0x35
                && session.rooms().activeRoom().roomObjectsArea()[northDoorIndex] < 0x3D);
        assertTrue(sounds.contains(GameplaySoundEvent.PUZZLE_SOLVED));
        assertTrue(sounds.contains(GameplaySoundEvent.DOOR_UNLOCKED));

        session.rooms().loadIndoor(0x02, 0x52);
        for (int tick = 0; tick < 9; tick++) session.entities().tickEntities(frame++, 80, 64);
        assertTrue(session.rooms().activeRoom().roomObjectsArea()[northDoorIndex] >= 0x35
                && session.rooms().activeRoom().roomObjectsArea()[northDoorIndex] < 0x3D);
    }
}
