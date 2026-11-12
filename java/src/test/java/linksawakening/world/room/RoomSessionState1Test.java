package linksawakening.world.room;

import static linksawakening.world.room.RoomSessionTestSupport.*;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.Link;
import linksawakening.vfx.TransientVfxSystem;
import linksawakening.world.entity.EnemyDropResolver;
import linksawakening.world.entity.EntityRoomLoader;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityRuntime;
import linksawakening.world.entity.RoomEntitySnapshot;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.transition.ScrollController;
import linksawakening.world.transition.Warp;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class RoomSessionState1Test {

    @Test
    void gameOverClearsOnlyPieceOfPowerDropCounter() {
        RoomSession session = newSession();
        session.loadInitialOverworld(0x92);
        session.components.roomSessionViews().setEnemyDropCountersForTest(7, 9);
        session.clearGameOverPowerCounter();
        assertEquals(
                new EnemyDropResolver.CounterState(7, 0),
                session.components.roomSessionViews().enemyDropCountersForTest());
    }

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
    void keyDropPointQuicksandFallPropagatesBothRomRoomStatusWrites() {
        RoomSession session = newSession();
        session.loadOverworld(0xCE);
        List<RoomEntity> slots = new ArrayList<>();
        for (int slot = 0; slot < EntityRoomLoader.MAX_ENTITIES; slot++) {
            slots.add(
                    slot == 0
                            ? new RoomEntity(
                                    0,
                                    -1,
                                    0x30,
                                    0x50,
                                    0x48,
                                    EntityStatus.ACTIVE,
                                    EntitySpriteDefinition.unsupported(0x30),
                                    0,
                                    0,
                                    0,
                                    0,
                                    -1,
                                    false)
                            : RoomEntity.disabled(slot));
        }
        RoomEntityRuntime runtime = RoomEntityRuntime.from(new RoomEntitySnapshot(slots));
        linksawakening.world.entity.EntityRuntimeTestAccess.setEntityRoomIdForTest(runtime, 0xCE);
        session.components.roomSessionViews().replaceEntityRuntimeForTest(runtime);

        session.tickEntities(0);

        assertEquals(0x04, session.overworldRoomStatusSnapshot()[0xCE] & 0x04);
        assertEquals(0x20, session.indoorARoomStatusSnapshot()[0xF8] & 0x20);
    }

    @Test
    void enteringTailKeyRoomFromBelowMarksDiscoveryAndQueuesItsPuzzleJingleOnce() {
        RoomSession session = newSession();
        ScrollController scrollController = new ScrollController();
        session.loadInitialOverworld(0x51);

        session.startAdjacentOverworldScroll(scrollController, ScrollController.UP, 0x50, 0x20);

        assertEquals(0x41, session.currentRoomId());
        assertEquals(0x40, session.components.roomSessionViews().overworldRoomStatusForTest(0x41) & 0x40);
        assertTrue(session.components.entityBridge().outbox.consumeEntityEvents().stream()
                .anyMatch(event ->
                        event.soundChannel() == EntityCombatEvent.SoundChannel.JINGLE && event.soundId() == 0x02));

        session.loadInitialOverworld(0x51);
        session.startAdjacentOverworldScroll(new ScrollController(), ScrollController.UP, 0x50, 0x20);
        assertFalse(session.components.entityBridge().outbox.consumeEntityEvents().stream()
                .anyMatch(event ->
                        event.soundChannel() == EntityCombatEvent.SoundChannel.JINGLE && event.soundId() == 0x02));
    }

    @Test
    void colorDungeonBossRoomRetainsItsRealZeroValuedLayoutPosition() {
        RoomSession session = newSession();
        session.loadIndoor(0xFF, 0x00);
        assertEquals(0x19, session.indoorRoomPositionForSave());

        session.transitions().startAdjacentIndoorScroll(new ScrollController(), ScrollController.DOWN, 0x40, 0x70);

        assertEquals(0x04, session.currentRoomId());
        assertEquals(0x21, session.indoorRoomPositionForSave());
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

        assertEquals(
                EntityStatus.ACTIVE,
                session.activeRoom().entities().slots().get(0).status());
        assertEquals(0x6E, session.activeRoom().entities().slots().get(3).type());
        assertEquals(1, session.activeRoom().entities().slots().get(3).spriteVariant());
        assertEquals(session.activeRoom().entities(), session.renderSnapshot().entities());
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
                1, initialized.x(), initialized.y(), initialized.z(), 0, 0, 0x01, false, 1, 0, false, 0, 0, 0, 0);
        RoomEntity revealTick = session.activeRoom().entities().slots().get(hiddenGhini.slot());
        assertEquals(-1, revealTick.spriteVariant());

        session.tickEntitiesWithProjectileEvents(
                2, revealTick.x(), revealTick.y(), revealTick.z(), 0, 0, false, 1, 0, false, 0, 0, 0, 0);
        RoomEntity visibleTick = session.activeRoom().entities().slots().get(hiddenGhini.slot());
        assertTrue(visibleTick.spriteVariant() >= 0);

        RoomSession oldOverloadSession = newSession();
        oldOverloadSession.loadInitialOverworld(0x67);
        RoomEntity oldOverloadGhini = oldOverloadSession.activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x10)
                .findFirst()
                .orElseThrow();
        oldOverloadSession.tickEntities(0, oldOverloadGhini.x(), oldOverloadGhini.y(), oldOverloadGhini.z(), 0);
        oldOverloadSession.tickEntitiesWithProjectileEvents(
                1,
                oldOverloadGhini.x(),
                oldOverloadGhini.y(),
                oldOverloadGhini.z(),
                0,
                0,
                false,
                1,
                0,
                false,
                0,
                0,
                0,
                0);

        assertEquals(
                -1,
                oldOverloadSession
                        .activeRoom()
                        .entities()
                        .slots()
                        .get(oldOverloadGhini.slot())
                        .spriteVariant());
    }

    @Test
    void bottleGrottoRoom27RejectsTheWrongEnemyKillOrder() {
        byte[] rom = loadRom();
        RoomEntityRuntime runtime = runtimeWithKillOrder(rom, 1, 0, 2);
        TransientVfxSystem vfx = new TransientVfxSystem(16);
        RoomSession session = newSession(vfx);
        session.loadIndoor(0x01, 0x27);
        session.components.roomSessionViews().replaceEntityRuntimeForTest(runtime);
        int chestIndex = RoomConstants.ROOM_OBJECTS_BASE + 0x28;

        for (int frame = 0; frame < 0x20; frame++) {
            session.tickEntities(frame, 0x50, 0x60);
        }

        assertEquals(0x66, session.components.roomSessionViews().activeRoomEventForTest());
        assertNotEquals(0xA0, session.activeRoom().roomObjectsArea()[chestIndex]);
        assertTrue(vfx.activeSlots().isEmpty());
    }

    @Test
    void dungeonTitleCountsOnlyInteractiveClosedInventoryFramesAfterOverworldEntry() {
        RoomSession session = newSession();
        session.loadInitialOverworld(0xD3);
        Warp entrance = session.activeRoom().warps().stream()
                .filter(warp -> warp.destRoom() == 0x17)
                .findFirst()
                .orElseThrow();
        session.loadWarpDestination(entrance);
        for (int i = 0; i < 80; i++) {
            assertEquals(-1, session.tickDungeonTitleMessage(true, true, 0, false));
            assertEquals(-1, session.tickDungeonTitleMessage(false, false, 0, false));
        }
        for (int i = 0; i < 47; i++) assertEquals(-1, session.tickDungeonTitleMessage(false, true, 0, false));
        assertEquals(0x56, session.tickDungeonTitleMessage(false, true, 0, false));
        assertEquals(-1, session.tickDungeonTitleMessage(false, true, 0, false));
        session.loadWarpDestination(new Warp(Warp.CATEGORY_INDOOR, 0, 0x13, 80, 112, 0));
        for (int i = 0; i < 60; i++) assertEquals(-1, session.tickDungeonTitleMessage(false, true, 0, false));
        session.loadInitialOverworld(0xD3);
        session.loadWarpDestination(entrance);
        for (int i = 0; i < 48; i++) assertEquals(-1, session.tickDungeonTitleMessage(false, true, 6, false));
        assertEquals(-1, session.tickDungeonTitleMessage(false, true, 0, false));
    }

    @Test
    void magicRodLaunchThroughTheRoomSessionPreservesThePowerSpeedSelection() {
        for (boolean power : new boolean[] {false, true}) {
            RoomSession session = newSession();
            session.loadInitialOverworld(0x92);
            assertTrue(session.fireMagicRodFireball(0x50, 0x58, 0, 0, power));
            RoomEntity spawn = session.activeRoom().entities().loadedEntities().stream()
                    .filter(entity -> entity.type() == 0x04)
                    .findFirst()
                    .orElseThrow();
            assertEquals(0x54, spawn.x());
            assertEquals(0x5C, spawn.y());

            session.tickEntitiesWithProjectileEvents(0, 0x50, 0x58, 0, 0, 0, false);
            RoomEntity moved = session.activeRoom().entities().loadedEntities().stream()
                    .filter(entity -> entity.type() == 0x04)
                    .findFirst()
                    .orElseThrow();
            assertEquals(power ? 0x58 : 0x57, moved.x());
            assertEquals(0x5C, moved.y());
        }
    }

    @Test
    void magicPowderSamplesTheCorrectBushAtExactCellBoundaries() {
        int[][] powderPositions = {{0x60, 0x59}, {0x51, 0x68}, {0x60, 0x68}};
        for (int[] position : powderPositions) {
            RoomSession session = newSession();
            session.loadInitialOverworld(0x92);
            int[] objects = session.activeRoom().roomObjectsArea();
            int[] renderValues = session.activeRoom().renderValues();
            for (int location : new int[] {0x55, 0x56, 0x65, 0x66}) {
                int index = RoomConstants.ROOM_OBJECTS_BASE + location;
                objects[index] = location == 0x55 ? 0x5C : 0x04;
                renderValues[index] = objects[index];
            }
            assertTrue(session.sprinkleMagicPowder(position[0] - 0x0E, position[1], 0, 0));
            for (int frame = 0; frame < 16; frame++) {
                session.tickEntities(frame, 0x20, 0x20);
            }
            assertEquals(
                    0x04, objects[RoomConstants.ROOM_OBJECTS_BASE + 0x55], "Powder at " + Arrays.toString(position));
            assertTrue(session.components.entityBridge().outbox.consumeEntityEvents().stream()
                    .anyMatch(event -> event.type() == 8 && event.soundId() == 0x2F));
        }
    }

    @Test
    void liveTorchPaletteFadeWaitsForDefaultLinkMotion() {
        RoomSession session = newSession();
        session.loadIndoor(0x0E, 0xA2);
        session.components.roomSessionViews().igniteIndoorTorchPaletteForTest();
        for (int frame = 0; frame < 32; frame++) {
            session.tickEntitiesWithProjectileEvents(frame, 0x20, 0x20, 0, 1, 0, false);
        }
        assertEquals(4, session.components.roomSessionViews().indoorTorchPaletteEffectAddressForTest());
        for (int frame = 32; frame < 42; frame++) {
            session.tickEntitiesWithProjectileEvents(frame, 0x20, 0x20, 0, 0, 0, false);
        }
        assertEquals(2, session.components.roomSessionViews().indoorTorchPaletteEffectAddressForTest());
    }

    @Test
    void room74TorchExpiryKeepsItsDrawnTilesEvenWhenAnotherTorchRefreshesTheRoom() {
        RoomSession session = newSession();
        session.loadIndoor(0, 0x74);
        int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + 0x22;
        session.activeRoom().roomObjectsArea()[areaIndex] = 0xAB;
        assertTrue(session.sprinkleMagicPowder(0x13, 0x29, 0, 0));
        for (int frame = 0; frame < 16; frame++) {
            session.tickEntitiesWithProjectileEvents(frame, 0x20, 0x20, 0, 0, 0, false);
        }
        // The skipped draw must retain VRAM, including attributes changed
        // since ignition, rather than rebuild graphics from the old object ID.
        int topLeft = 4 * RoomConstants.ROOM_TILE_WIDTH + 4;
        session.activeRoom().tileIds()[topLeft] = 0x61;
        session.activeRoom().tileAttrs()[topLeft] = 0x25;
        int[] litTiles = torchTiles(session, 0x22);
        for (int frame = 16; frame < 1024; frame++) {
            session.tickEntitiesWithProjectileEvents(frame, 0x20, 0x20, 0, 0, 0, false);
        }
        assertEquals(0xAB, session.activeRoom().roomObjectsArea()[areaIndex]);
        assertEquals(1, session.components.roomSessionViews().roomTriggerCountForTest());
        assertArrayEquals(litTiles, torchTiles(session, 0x22));

        session.activeRoom().roomObjectsArea()[RoomConstants.ROOM_OBJECTS_BASE + 0x33] = 0xAB;
        assertTrue(session.sprinkleMagicPowder(0x23, 0x39, 0, 0));
        for (int frame = 1024; frame < 1040; frame++) {
            session.tickEntitiesWithProjectileEvents(frame, 0x20, 0x20, 0, 0, 0, false);
        }
        assertEquals(0xAC, session.activeRoom().roomObjectsArea()[RoomConstants.ROOM_OBJECTS_BASE + 0x33]);
        assertArrayEquals(litTiles, torchTiles(session, 0x22));
    }

    @Test
    void torchExpiryWritesItsSavedObjectLocationEvenIfTheObjectHasChanged() {
        RoomSession session = newSession();
        session.loadIndoor(0, 0);
        int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + 0x22;
        session.activeRoom().roomObjectsArea()[areaIndex] = 0xAB;
        assertTrue(session.sprinkleMagicPowder(0x13, 0x29, 0, 0));
        for (int frame = 0; frame < 16; frame++) {
            session.tickEntitiesWithProjectileEvents(frame, 0x20, 0x20, 0, 0, 0, false);
        }
        session.activeRoom().roomObjectsArea()[areaIndex] = 0x0D;
        for (int frame = 16; frame < 1024; frame++) {
            session.tickEntitiesWithProjectileEvents(frame, 0x20, 0x20, 0, 0, 0, false);
        }
        assertEquals(0xAB, session.activeRoom().roomObjectsArea()[areaIndex]);
        assertEquals(0, session.components.roomSessionViews().roomTriggerCountForTest());
    }

    @Test
    void magicPowderTorchExpiryDecrementsTheRomRoomTriggerCounter() {
        RoomSession session = newSession();
        session.loadIndoor(0x00, 0x00);

        int location = 0x22;
        int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + (location & 0xF0) + (location & 0x0F);
        session.activeRoom().roomObjectsArea()[areaIndex] = 0xAB;

        assertTrue(session.sprinkleMagicPowder(0x13, 0x29, 0, 0));
        for (int frame = 0; frame < 16; frame++) {
            session.tickEntitiesWithProjectileEvents(frame, 0x20, 0x20, 0, 0, 0, false);
        }
        assertEquals(1, session.components.roomSessionViews().roomTriggerCountForTest());

        for (int frame = 16; frame < 1024 && session.activeRoom().roomObjectsArea()[areaIndex] == 0xAC; frame++) {
            session.tickEntitiesWithProjectileEvents(frame, 0x20, 0x20, 0, 0, 0, false);
        }

        assertEquals(0xAB, session.activeRoom().roomObjectsArea()[areaIndex]);
        assertEquals(0, session.components.roomSessionViews().roomTriggerCountForTest());

        session.loadIndoor(0x00, 0x00);
        assertEquals(0, session.components.roomSessionViews().roomTriggerCountForTest());
    }

    @Test
    void shovelUsesTheAdjacentRomCellAndTurnsItIntoAConfiguredHoleOnTimerTen() {
        RoomSession session = newSession();
        session.loadInitialOverworld(0x92);

        int location = 0x55;
        int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + (location & 0xF0) + (location & 0x0F);
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
    void shovelQueuesTheRomRandomDropAfterTheHoleIsDrawn() {
        RoomSession session = newSession();
        session.loadInitialOverworld(0x92);

        int location = 0x55;
        int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + (location & 0xF0) + (location & 0x0F);
        session.activeRoom().roomObjectsArea()[areaIndex] = 0x04;
        session.activeRoom().renderValues()[areaIndex] = 0x04;

        assertTrue(session.startShovel(0x44, 0x56, 0, false).started());
        session.advanceShovel(0x44, 0x56, 0, 0x10);

        // With the ROM seed and rLY policy, frame fourteen produces the
        // source's successful 1/8 gate and selects the heart on its second
        // byte (RRA carry set).
        session.tickEntities(14, 0x44, 0x56);

        RoomEntity drop = session.activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.sourceLoadOrder() == -1 && (entity.type() == 0x2D || entity.type() == 0x2E))
                .findFirst()
                .orElseThrow();
        assertEquals(0x2D, drop.type());
        assertEquals(0x58, drop.x());
        assertEquals(0x60, drop.y());
        assertEquals(0x02, drop.z());
        assertEquals(0x20 - 0x02, session.components.roomSessionViews().entityDropSpeedZForTest(drop.slot()));
        assertEquals(0x0C, session.components.roomSessionViews().entityDropSpeedXForTest(drop.slot()));
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
        assertEquals(0, session.components.roomSessionViews().entityGroundStatusForTest(initial.slot()));
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
        List<EntityCombatEvent> events =
                session.components.entityBridge().outbox.consumeEntityEvents();
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
                roomObjects[RoomConstants.ROOM_OBJECTS_BASE + row * RoomConstants.ROOM_OBJECT_ROW_STRIDE + column] =
                        0x0E;
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
                roomObjects[RoomConstants.ROOM_OBJECTS_BASE + row * RoomConstants.ROOM_OBJECT_ROW_STRIDE + column] =
                        0x10;
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
            assertEquals(0x02, session.components.roomSessionViews().entityGroundStatusForTest(peaHat.slot()));
        }
        assertEquals(
                0,
                session.components.entityBridge().outbox.consumeEntityEvents().size());
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
        RoomEntity conveyorInitial =
                conveyorSession.activeRoom().entities().slots().get(passableInitial.slot());
        assertEquals(passableInitial.x(), conveyorInitial.x());
        assertEquals(passableInitial.y(), conveyorInitial.y());
        fillActiveObjects(passableSession, 0x01);
        fillActiveObjects(conveyorSession, 0xCF);

        for (int frame = 0; frame <= 4; frame++) {
            passableSession.tickEntities(frame, 0, 0);
            conveyorSession.tickEntities(frame, 0, 0);
        }

        RoomEntity passable = passableSession.activeRoom().entities().slots().get(passableInitial.slot());
        RoomEntity conveyor = conveyorSession.activeRoom().entities().slots().get(conveyorInitial.slot());
        // Overworld $CF is physics $F4, whose bank-$03 movement table entry is
        // (+1,+1). The ordinary entity's own motion is identical in both runs.
        assertEquals((passable.x() + 1) & 0xFF, conveyor.x());
        assertEquals((passable.y() + 1) & 0xFF, conveyor.y());
    }
}
