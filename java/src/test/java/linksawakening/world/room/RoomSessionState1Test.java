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
        session.rooms().loadInitialOverworld(0x92);
        RoomDebugFixture.of(session).setEnemyDropCountersForTest(7, 9);
        session.progress().clearGameOverPowerCounter();
        assertEquals(
                new EnemyDropResolver.CounterState(7, 0),
                RoomDebugFixture.of(session).enemyDropCountersForTest());
    }

    @Test
    void loadsInitialOverworldRoomIntoActiveRoomState() {
        RoomSession session = newSession();

        session.rooms().loadInitialOverworld(0x92);

        assertTrue(session.rooms().hasActiveRoom());
        assertEquals(0x92, session.rooms().currentRoomId());
        assertEquals(Warp.CATEGORY_OVERWORLD, session.rooms().mapCategory());
        assertNotNull(session.rooms().activeRoom().roomObjectsArea());
        assertEquals(
                0x92, session.rooms().activeRoom().entities().spriteSelection().roomId());
        assertNotNull(session.rooms().activeRoom().entities().spriteTiles());
        assertNotNull(session.rooms().renderSnapshot());
    }

    @Test
    void keyDropPointQuicksandFallPropagatesBothRomRoomStatusWrites() {
        RoomSession session = newSession();
        session.rooms().loadOverworld(0xCE);
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
        RoomEntityRuntime runtime =
                RoomEntityRuntime.builder(new RoomEntitySnapshot(slots)).build();
        linksawakening.world.entity.EntityRuntimeTestAccess.setEntityRoomIdForTest(runtime, 0xCE);
        RoomDebugFixture.of(session).replaceEntityRuntimeForTest(runtime);

        session.entities().tickEntities(0);

        assertEquals(0x04, session.dungeon().overworldRoomStatusSnapshot()[0xCE] & 0x04);
        assertEquals(0x20, session.dungeon().indoorARoomStatusSnapshot()[0xF8] & 0x20);
    }

    @Test
    void enteringTailKeyRoomFromBelowMarksDiscoveryAndQueuesItsPuzzleJingleOnce() {
        RoomSession session = newSession();
        ScrollController scrollController = new ScrollController();
        session.rooms().loadInitialOverworld(0x51);

        session.transitions().startAdjacentOverworldScroll(scrollController, ScrollController.UP, 0x50, 0x20);

        assertEquals(0x41, session.rooms().currentRoomId());
        assertEquals(0x40, RoomDebugFixture.of(session).overworldRoomStatusForTest(0x41) & 0x40);
        assertTrue(session.components.entityBridge().outbox.consumeEntityEvents().stream()
                .anyMatch(event ->
                        event.soundChannel() == EntityCombatEvent.SoundChannel.JINGLE && event.soundId() == 0x02));

        session.rooms().loadInitialOverworld(0x51);
        session.transitions().startAdjacentOverworldScroll(new ScrollController(), ScrollController.UP, 0x50, 0x20);
        assertFalse(session.components.entityBridge().outbox.consumeEntityEvents().stream()
                .anyMatch(event ->
                        event.soundChannel() == EntityCombatEvent.SoundChannel.JINGLE && event.soundId() == 0x02));
    }

    @Test
    void colorDungeonBossRoomRetainsItsRealZeroValuedLayoutPosition() {
        RoomSession session = newSession();
        session.rooms().loadIndoor(0xFF, 0x00);
        assertEquals(0x19, session.rooms().indoorRoomPositionForSave());

        session.transitions().startAdjacentIndoorScroll(new ScrollController(), ScrollController.DOWN, 0x40, 0x70);

        assertEquals(0x04, session.rooms().currentRoomId());
        assertEquals(0x21, session.rooms().indoorRoomPositionForSave());
    }

    @Test
    void notifiesListenerAfterRoomLoads() {
        List<Integer> loadedRoomIds = new ArrayList<>();
        RoomSession session = newSession(room -> loadedRoomIds.add(room.roomId()));
        ScrollController scrollController = new ScrollController();

        session.rooms().loadInitialOverworld(0x92);
        session.transitions().startAdjacentOverworldScroll(scrollController, ScrollController.RIGHT, 161, 40);

        assertEquals(List.of(0x92, 0x93), loadedRoomIds);
    }

    @Test
    void ticksTheLoadedEntityRuntimeAndRefreshesTheRenderSnapshot() {
        RoomSession session = newSession();
        session.rooms().loadInitialOverworld(0x92);

        session.entities().tickEntities(0);

        assertEquals(
                EntityStatus.ACTIVE,
                session.rooms().activeRoom().entities().slots().get(0).status());
        assertEquals(
                0x6E, session.rooms().activeRoom().entities().slots().get(3).type());
        assertEquals(1, session.rooms().activeRoom().entities().slots().get(3).spriteVariant());
        assertEquals(
                session.rooms().activeRoom().entities(),
                session.rooms().renderSnapshot().entities());
    }

    @Test
    void forwardsPerFrameLinkCollisionTypeToGhiniAndOldOverloadDefaultsToZero() {
        RoomSession session = newSession();
        session.rooms().loadInitialOverworld(0x67);
        RoomEntity hiddenGhini = session.rooms().activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x10)
                .findFirst()
                .orElseThrow();

        session.entities().tickEntities(0, hiddenGhini.x(), hiddenGhini.y(), hiddenGhini.z(), 0);
        RoomEntity initialized = session.rooms().activeRoom().entities().slots().get(hiddenGhini.slot());

        session.tickEntities(new EntityTickInputs(
                1,
                new LinkPosition(
                        initialized.x(),
                        initialized.y(),
                        initialized.z(),
                        linksawakening.state.Direction.fromJavaOrUnknown(0),
                        0x01,
                        0),
                new LinkMotion(false, linksawakening.world.room.LinkMotion.stateForRomByte(0), false, 0, 0, 0, 0),
                1,
                new SwordBox(false, 0, 0, 0, 0)));
        RoomEntity revealTick = session.rooms().activeRoom().entities().slots().get(hiddenGhini.slot());
        assertEquals(-1, revealTick.spriteVariant());

        session.tickEntities(new EntityTickInputs(
                2,
                new LinkPosition(
                        revealTick.x(),
                        revealTick.y(),
                        revealTick.z(),
                        linksawakening.state.Direction.fromJavaOrUnknown(0),
                        0,
                        0),
                new LinkMotion(false, linksawakening.world.room.LinkMotion.stateForRomByte(0), false, 0, 0, 0, 0),
                1,
                new SwordBox(false, 0, 0, 0, 0)));
        RoomEntity visibleTick = session.rooms().activeRoom().entities().slots().get(hiddenGhini.slot());
        assertTrue(visibleTick.spriteVariant() >= 0);

        RoomSession oldOverloadSession = newSession();
        oldOverloadSession.rooms().loadInitialOverworld(0x67);
        RoomEntity oldOverloadGhini = oldOverloadSession.rooms().activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x10)
                .findFirst()
                .orElseThrow();
        oldOverloadSession
                .entities()
                .tickEntities(0, oldOverloadGhini.x(), oldOverloadGhini.y(), oldOverloadGhini.z(), 0);
        oldOverloadSession.tickEntities(new EntityTickInputs(
                1,
                new LinkPosition(
                        oldOverloadGhini.x(),
                        oldOverloadGhini.y(),
                        oldOverloadGhini.z(),
                        linksawakening.state.Direction.fromJavaOrUnknown(0),
                        0,
                        0),
                new LinkMotion(false, linksawakening.world.room.LinkMotion.stateForRomByte(0), false, 0, 0, 0, 0),
                1,
                new SwordBox(false, 0, 0, 0, 0)));

        assertEquals(
                -1,
                oldOverloadSession
                        .rooms()
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
        session.rooms().loadIndoor(0x01, 0x27);
        RoomDebugFixture.of(session).replaceEntityRuntimeForTest(runtime);
        int chestIndex = RoomConstants.ROOM_OBJECTS_BASE + 0x28;

        for (int frame = 0; frame < 0x20; frame++) {
            session.entities().tickEntities(frame, 0x50, 0x60);
        }

        assertEquals(0x66, RoomDebugFixture.of(session).activeRoomEventForTest());
        assertNotEquals(0xA0, session.rooms().activeRoom().roomObjectsArea()[chestIndex]);
        assertTrue(vfx.activeSlots().isEmpty());
    }

    @Test
    void dungeonTitleCountsOnlyInteractiveClosedInventoryFramesAfterOverworldEntry() {
        RoomSession session = newSession();
        session.rooms().loadInitialOverworld(0xD3);
        Warp entrance = session.rooms().activeRoom().warps().stream()
                .filter(warp -> warp.destRoom() == 0x17)
                .findFirst()
                .orElseThrow();
        session.transitions().loadWarpDestination(entrance);
        for (int i = 0; i < 80; i++) {
            assertEquals(-1, session.transitions().tickDungeonTitleMessage(true, true, 0, false));
            assertEquals(-1, session.transitions().tickDungeonTitleMessage(false, false, 0, false));
        }
        for (int i = 0; i < 47; i++)
            assertEquals(-1, session.transitions().tickDungeonTitleMessage(false, true, 0, false));
        assertEquals(0x56, session.transitions().tickDungeonTitleMessage(false, true, 0, false));
        assertEquals(-1, session.transitions().tickDungeonTitleMessage(false, true, 0, false));
        session.transitions().loadWarpDestination(new Warp(Warp.CATEGORY_INDOOR, 0, 0x13, 80, 112, 0));
        for (int i = 0; i < 60; i++)
            assertEquals(-1, session.transitions().tickDungeonTitleMessage(false, true, 0, false));
        session.rooms().loadInitialOverworld(0xD3);
        session.transitions().loadWarpDestination(entrance);
        for (int i = 0; i < 48; i++)
            assertEquals(-1, session.transitions().tickDungeonTitleMessage(false, true, 6, false));
        assertEquals(-1, session.transitions().tickDungeonTitleMessage(false, true, 0, false));
    }

    @Test
    void magicRodLaunchThroughTheRoomSessionPreservesThePowerSpeedSelection() {
        for (boolean power : new boolean[] {false, true}) {
            RoomSession session = newSession();
            session.rooms().loadInitialOverworld(0x92);
            assertTrue(session.playerItems().fireMagicRodFireball(0x50, 0x58, 0, 0, power));
            RoomEntity spawn = session.rooms().activeRoom().entities().loadedEntities().stream()
                    .filter(entity -> entity.type() == 0x04)
                    .findFirst()
                    .orElseThrow();
            assertEquals(0x54, spawn.x());
            assertEquals(0x5C, spawn.y());

            session.entities().tickEntitiesWithProjectileEvents(0, 0x50, 0x58, 0, 0, 0, false);
            RoomEntity moved = session.rooms().activeRoom().entities().loadedEntities().stream()
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
            session.rooms().loadInitialOverworld(0x92);
            int[] objects = session.rooms().activeRoom().roomObjectsArea();
            int[] renderValues = session.rooms().activeRoom().renderValues();
            for (int location : new int[] {0x55, 0x56, 0x65, 0x66}) {
                int index = RoomConstants.ROOM_OBJECTS_BASE + location;
                objects[index] = location == 0x55 ? 0x5C : 0x04;
                renderValues[index] = objects[index];
            }
            assertTrue(session.playerItems().sprinkleMagicPowder(position[0] - 0x0E, position[1], 0, 0));
            for (int frame = 0; frame < 16; frame++) {
                session.entities().tickEntities(frame, 0x20, 0x20);
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
        session.rooms().loadIndoor(0x0E, 0xA2);
        RoomDebugFixture.of(session).igniteIndoorTorchPaletteForTest();
        for (int frame = 0; frame < 32; frame++) {
            session.entities().tickEntitiesWithProjectileEvents(frame, 0x20, 0x20, 0, 1, 0, false);
        }
        assertEquals(4, RoomDebugFixture.of(session).indoorTorchPaletteEffectAddressForTest());
        for (int frame = 32; frame < 42; frame++) {
            session.entities().tickEntitiesWithProjectileEvents(frame, 0x20, 0x20, 0, 0, 0, false);
        }
        assertEquals(2, RoomDebugFixture.of(session).indoorTorchPaletteEffectAddressForTest());
    }

    @Test
    void room74TorchExpiryKeepsItsDrawnTilesEvenWhenAnotherTorchRefreshesTheRoom() {
        RoomSession session = newSession();
        session.rooms().loadIndoor(0, 0x74);
        int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + 0x22;
        session.rooms().activeRoom().roomObjectsArea()[areaIndex] = 0xAB;
        assertTrue(session.playerItems().sprinkleMagicPowder(0x13, 0x29, 0, 0));
        for (int frame = 0; frame < 16; frame++) {
            session.entities().tickEntitiesWithProjectileEvents(frame, 0x20, 0x20, 0, 0, 0, false);
        }
        // The skipped draw must retain VRAM, including attributes changed
        // since ignition, rather than rebuild graphics from the old object ID.
        int topLeft = 4 * RoomConstants.ROOM_TILE_WIDTH + 4;
        session.rooms().activeRoom().tileIds()[topLeft] = 0x61;
        session.rooms().activeRoom().tileAttrs()[topLeft] = 0x25;
        int[] litTiles = torchTiles(session, 0x22);
        for (int frame = 16; frame < 1024; frame++) {
            session.entities().tickEntitiesWithProjectileEvents(frame, 0x20, 0x20, 0, 0, 0, false);
        }
        assertEquals(0xAB, session.rooms().activeRoom().roomObjectsArea()[areaIndex]);
        assertEquals(1, RoomDebugFixture.of(session).roomTriggerCountForTest());
        assertArrayEquals(litTiles, torchTiles(session, 0x22));

        session.rooms().activeRoom().roomObjectsArea()[RoomConstants.ROOM_OBJECTS_BASE + 0x33] = 0xAB;
        assertTrue(session.playerItems().sprinkleMagicPowder(0x23, 0x39, 0, 0));
        for (int frame = 1024; frame < 1040; frame++) {
            session.entities().tickEntitiesWithProjectileEvents(frame, 0x20, 0x20, 0, 0, 0, false);
        }
        assertEquals(0xAC, session.rooms().activeRoom().roomObjectsArea()[RoomConstants.ROOM_OBJECTS_BASE + 0x33]);
        assertArrayEquals(litTiles, torchTiles(session, 0x22));
    }

    @Test
    void torchExpiryWritesItsSavedObjectLocationEvenIfTheObjectHasChanged() {
        RoomSession session = newSession();
        session.rooms().loadIndoor(0, 0);
        int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + 0x22;
        session.rooms().activeRoom().roomObjectsArea()[areaIndex] = 0xAB;
        assertTrue(session.playerItems().sprinkleMagicPowder(0x13, 0x29, 0, 0));
        for (int frame = 0; frame < 16; frame++) {
            session.entities().tickEntitiesWithProjectileEvents(frame, 0x20, 0x20, 0, 0, 0, false);
        }
        session.rooms().activeRoom().roomObjectsArea()[areaIndex] = 0x0D;
        for (int frame = 16; frame < 1024; frame++) {
            session.entities().tickEntitiesWithProjectileEvents(frame, 0x20, 0x20, 0, 0, 0, false);
        }
        assertEquals(0xAB, session.rooms().activeRoom().roomObjectsArea()[areaIndex]);
        assertEquals(0, RoomDebugFixture.of(session).roomTriggerCountForTest());
    }

    @Test
    void magicPowderTorchExpiryDecrementsTheRomRoomTriggerCounter() {
        RoomSession session = newSession();
        session.rooms().loadIndoor(0x00, 0x00);

        int location = 0x22;
        int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + (location & 0xF0) + (location & 0x0F);
        session.rooms().activeRoom().roomObjectsArea()[areaIndex] = 0xAB;

        assertTrue(session.playerItems().sprinkleMagicPowder(0x13, 0x29, 0, 0));
        for (int frame = 0; frame < 16; frame++) {
            session.entities().tickEntitiesWithProjectileEvents(frame, 0x20, 0x20, 0, 0, 0, false);
        }
        assertEquals(1, RoomDebugFixture.of(session).roomTriggerCountForTest());

        for (int frame = 16;
                frame < 1024 && session.rooms().activeRoom().roomObjectsArea()[areaIndex] == 0xAC;
                frame++) {
            session.entities().tickEntitiesWithProjectileEvents(frame, 0x20, 0x20, 0, 0, 0, false);
        }

        assertEquals(0xAB, session.rooms().activeRoom().roomObjectsArea()[areaIndex]);
        assertEquals(0, RoomDebugFixture.of(session).roomTriggerCountForTest());

        session.rooms().loadIndoor(0x00, 0x00);
        assertEquals(0, RoomDebugFixture.of(session).roomTriggerCountForTest());
    }

    @Test
    void shovelUsesTheAdjacentRomCellAndTurnsItIntoAConfiguredHoleOnTimerTen() {
        RoomSession session = newSession();
        session.rooms().loadInitialOverworld(0x92);

        int location = 0x55;
        int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + (location & 0xF0) + (location & 0x0F);
        session.rooms().activeRoom().roomObjectsArea()[areaIndex] = 0x04;
        session.rooms().activeRoom().renderValues()[areaIndex] = 0x04;

        ShovelStartResult start = session.playerItems().startShovel(0x44, 0x56, 0, false);

        assertTrue(start.started());
        assertFalse(start.poking());
        assertEquals(0x55, start.location());
        assertEquals(
                0x71,
                session.playerItems()
                        .shovelAnimationState(linksawakening.state.Direction.fromJava(Link.DIRECTION_RIGHT), 1));
        assertEquals(
                0x72,
                session.playerItems()
                        .shovelAnimationState(linksawakening.state.Direction.fromJava(Link.DIRECTION_RIGHT), 0x10));

        for (int timer = 1; timer <= 15; timer++) {
            session.playerItems().advanceShovel(0x44, 0x56, 0, timer);
        }
        assertEquals(0x04, session.rooms().activeRoom().roomObjectsArea()[areaIndex]);

        session.playerItems().advanceShovel(0x44, 0x56, 0, 0x10);

        assertEquals(0xCC, session.rooms().activeRoom().roomObjectsArea()[areaIndex]);
        assertEquals(0xCC, session.rooms().activeRoom().renderValues()[areaIndex]);
    }

    @Test
    void shovelQueuesTheRomRandomDropAfterTheHoleIsDrawn() {
        RoomSession session = newSession();
        session.rooms().loadInitialOverworld(0x92);

        int location = 0x55;
        int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + (location & 0xF0) + (location & 0x0F);
        session.rooms().activeRoom().roomObjectsArea()[areaIndex] = 0x04;
        session.rooms().activeRoom().renderValues()[areaIndex] = 0x04;

        assertTrue(session.playerItems().startShovel(0x44, 0x56, 0, false).started());
        session.playerItems().advanceShovel(0x44, 0x56, 0, 0x10);

        // With the ROM seed and rLY policy, frame fourteen produces the
        // source's successful 1/8 gate and selects the heart on its second
        // byte (RRA carry set).
        session.entities().tickEntities(14, 0x44, 0x56);

        RoomEntity drop = session.rooms().activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.sourceLoadOrder() == -1 && (entity.type() == 0x2D || entity.type() == 0x2E))
                .findFirst()
                .orElseThrow();
        assertEquals(0x2D, drop.type());
        assertEquals(0x58, drop.x());
        assertEquals(0x60, drop.y());
        assertEquals(0x02, drop.z());
        assertEquals(0x20 - 0x02, RoomDebugFixture.of(session).entityDropSpeedZForTest(drop.slot()));
        assertEquals(0x0C, RoomDebugFixture.of(session).entityDropSpeedXForTest(drop.slot()));
    }

    @Test
    void forwardsHeldActionButtonsToTheLiveEvasiveStalfosHandler() {
        RoomSession session = newSession();
        session.rooms().loadIndoor(0x00, 0x0F);
        RoomEntity initial = session.rooms().activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x1E)
                .findFirst()
                .orElseThrow();

        session.entityState().setEntityActionButtonsHeld(true);
        session.entities().tickEntities(0, initial.x() + 0x10, initial.y());
        session.entities().tickEntities(1, initial.x() + 0x10, initial.y());
        session.entityState().setEntityActionButtonsHeld(false);
        session.entities().tickEntities(2, initial.x() + 0x10, initial.y());

        RoomEntity jumping = session.rooms().activeRoom().entities().slots().get(initial.slot());
        assertEquals(0x1E, jumping.type());
        assertEquals(1, jumping.z());
        assertEquals(2, jumping.spriteVariant());
        assertEquals(0x4E7D, jumping.spriteDefinition().address());
    }

    @Test
    void positiveZSkipsRomGroundSamplingForAnAirborneEntity() {
        RoomSession session = newSession();
        session.rooms().loadIndoor(0x00, 0x0F);
        RoomEntity initial = session.rooms().activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x1E)
                .findFirst()
                .orElseThrow();
        fillActiveObjects(session, 0x1B);

        session.entityState().setEntityActionButtonsHeld(true);
        session.entities().tickEntities(0, initial.x() + 0x10, initial.y());
        session.entities().tickEntities(1, initial.x() + 0x10, initial.y());
        session.entityState().setEntityActionButtonsHeld(false);
        session.entities().tickEntities(2, initial.x() + 0x10, initial.y());

        RoomEntity airborne = session.rooms().activeRoom().entities().slots().get(initial.slot());
        assertEquals(1, airborne.z());
        assertEquals(0, RoomDebugFixture.of(session).entityGroundStatusForTest(initial.slot()));
    }

    @Test
    void liveAnglersTunnelEvasiveStalfosCreatesTheRomFleeingCloneAfterLanding() {
        RoomSession session = newSession();
        session.rooms().loadIndoor(0x03, 0x0F);
        RoomEntity initial = session.rooms().activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x1E)
                .findFirst()
                .orElseThrow();
        int linkX = initial.x() + 0x10;

        session.entityState().setEntityActionButtonsHeld(true);
        session.entities().tickEntities(0, linkX, initial.y());
        session.entities().tickEntities(1, linkX, initial.y());
        session.entityState().setEntityActionButtonsHeld(false);

        RoomEntity clone = null;
        for (int frame = 2; frame < 80; frame++) {
            session.entities().tickEntities(frame, linkX, initial.y());
            clone = session.rooms().activeRoom().entities().slots().stream()
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
        session.rooms().loadIndoor(0x00, 0x65);
        RoomEntity initial = session.rooms().activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x99)
                .findFirst()
                .orElseThrow();
        assertEquals(0x58, initial.x());
        assertEquals(0x40, initial.y());

        int[] roomObjects = session.rooms().activeRoom().roomObjectsArea();
        for (int row = 0; row < RoomConstants.OBJECTS_PER_COLUMN; row++) {
            for (int column = 0; column < RoomConstants.OBJECTS_PER_ROW; column++) {
                roomObjects[RoomConstants.ROOM_OBJECTS_BASE + row * RoomConstants.ROOM_OBJECT_ROW_STRIDE + column] =
                        0x0E;
            }
        }

        for (int frame = 0; frame <= 8; frame++) {
            session.entities().tickEntities(frame, 0, 0);
        }

        RoomEntity moved = session.rooms().activeRoom().entities().slots().get(initial.slot());
        assertTrue(moved.x() != initial.x() || moved.y() != initial.y());
    }

    @Test
    void waterTektiteStillStopsOnSolidTerrainInTheLiveRoomSession() {
        RoomSession session = newSession();
        session.rooms().loadIndoor(0x00, 0x65);
        RoomEntity initial = session.rooms().activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x99)
                .findFirst()
                .orElseThrow();

        int[] roomObjects = session.rooms().activeRoom().roomObjectsArea();
        for (int row = 0; row < RoomConstants.OBJECTS_PER_COLUMN; row++) {
            for (int column = 0; column < RoomConstants.OBJECTS_PER_ROW; column++) {
                roomObjects[RoomConstants.ROOM_OBJECTS_BASE + row * RoomConstants.ROOM_OBJECT_ROW_STRIDE + column] =
                        0x10;
            }
        }

        for (int frame = 0; frame <= 8; frame++) {
            session.entities().tickEntities(frame, 0, 0);
        }

        RoomEntity blocked = session.rooms().activeRoom().entities().slots().get(initial.slot());
        assertEquals(initial.x(), blocked.x());
        assertEquals(initial.y(), blocked.y());
    }

    @Test
    void peaHatRetainsTheRomDeepWaterGroundStatus() {
        RoomSession session = newSession();
        session.rooms().loadIndoor(0x00, 0x6B);
        List<RoomEntity> peaHats = session.rooms().activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0xA0)
                .toList();
        assertTrue(!peaHats.isEmpty());
        for (RoomEntity entity : session.rooms().activeRoom().entities().loadedEntities()) {
            if (entity.type() != 0xA0) {
                session.entityState().clearEntity(entity.slot());
            }
        }
        fillActiveObjects(session, 0x0E);

        session.entities().tickEntities(0);
        session.entities().tickEntities(1);

        for (RoomEntity peaHat : peaHats) {
            RoomEntity current = session.rooms().activeRoom().entities().slots().get(peaHat.slot());
            assertEquals(EntityStatus.ACTIVE, current.status());
            assertEquals(0x02, RoomDebugFixture.of(session).entityGroundStatusForTest(peaHat.slot()));
        }
        assertEquals(
                0,
                session.components.entityBridge().outbox.consumeEntityEvents().size());
    }

    @Test
    void ordinaryEntitiesReceiveTheRomConveyorNudgeEveryFourFrames() {
        RoomSession passableSession = newSession();
        RoomSession conveyorSession = newSession();
        passableSession.rooms().loadInitialOverworld(0x2F);
        conveyorSession.rooms().loadInitialOverworld(0x2F);

        RoomEntity passableInitial = passableSession.rooms().activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x09)
                .findFirst()
                .orElseThrow();
        RoomEntity conveyorInitial =
                conveyorSession.rooms().activeRoom().entities().slots().get(passableInitial.slot());
        assertEquals(passableInitial.x(), conveyorInitial.x());
        assertEquals(passableInitial.y(), conveyorInitial.y());
        fillActiveObjects(passableSession, 0x01);
        fillActiveObjects(conveyorSession, 0xCF);

        for (int frame = 0; frame <= 4; frame++) {
            passableSession.entities().tickEntities(frame, 0, 0);
            conveyorSession.entities().tickEntities(frame, 0, 0);
        }

        RoomEntity passable =
                passableSession.rooms().activeRoom().entities().slots().get(passableInitial.slot());
        RoomEntity conveyor =
                conveyorSession.rooms().activeRoom().entities().slots().get(conveyorInitial.slot());
        // Overworld $CF is physics $F4, whose bank-$03 movement table entry is
        // (+1,+1). The ordinary entity's own motion is identical in both runs.
        assertEquals((passable.x() + 1) & 0xFF, conveyor.x());
        assertEquals((passable.y() + 1) & 0xFF, conveyor.y());
    }
}
