package linksawakening.world.room;

import static linksawakening.world.room.RoomSessionTestSupport.fillActiveObjects;
import static linksawakening.world.room.RoomSessionTestSupport.loadRom;
import static linksawakening.world.room.RoomSessionTestSupport.newSession;
import static linksawakening.world.room.RoomSessionTestSupport.pattern;
import static linksawakening.world.room.RoomSessionTestSupport.runtimeWithKillOrder;
import static linksawakening.world.room.RoomSessionTestSupport.torchTiles;
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
import linksawakening.rom.RomBank;
import linksawakening.state.Direction;
import linksawakening.vfx.TransientVfxSystem;
import linksawakening.vfx.TransientVfxType;
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
final class RoomStoryPersistenceTest {
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
        assertNotNull(session.rooms().activeRoomOrNull().roomObjectsArea());
        assertEquals(
                0x92,
                session.rooms().activeRoomOrNull().entities().spriteSelection().roomId());
        assertNotNull(session.rooms().activeRoomOrNull().entities().spriteTiles());
        assertNotNull(session.rooms().renderSnapshotOrNull());
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
        assertTrue(RoomSessionInspection.component(session, RoomEntityBridge.class).consumeEntityEvents().stream()
                .anyMatch(event ->
                        event.soundChannel() == EntityCombatEvent.SoundChannel.JINGLE && event.soundId() == 0x02));

        session.rooms().loadInitialOverworld(0x51);
        session.transitions().startAdjacentOverworldScroll(new ScrollController(), ScrollController.UP, 0x50, 0x20);
        assertFalse(RoomSessionInspection.component(session, RoomEntityBridge.class).consumeEntityEvents().stream()
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
                session.rooms().activeRoomOrNull().entities().slots().get(0).status());
        assertEquals(
                0x6E,
                session.rooms().activeRoomOrNull().entities().slots().get(3).type());
        assertEquals(
                1, session.rooms().activeRoomOrNull().entities().slots().get(3).spriteVariant());
        assertEquals(
                session.rooms().activeRoomOrNull().entities(),
                session.rooms().renderSnapshotOrNull().entities());
    }

    @Test
    void forwardsPerFrameLinkCollisionTypeToGhiniAndOldOverloadDefaultsToZero() {
        RoomSession session = newSession();
        session.rooms().loadInitialOverworld(0x67);
        RoomEntity hiddenGhini = session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x10)
                .findFirst()
                .orElseThrow();

        session.entities().tickEntities(0, hiddenGhini.x(), hiddenGhini.y(), hiddenGhini.z(), 0);
        RoomEntity initialized =
                session.rooms().activeRoomOrNull().entities().slots().get(hiddenGhini.slot());

        session.tickEntities(new EntityTickInputs(
                1,
                new LinkPosition(initialized.x(), initialized.y(), initialized.z(), 0, 0x01),
                new LinkMotion(false, 0, false, 0, 0, 0),
                1,
                new SwordBox(false, 0, 0, 0, 0)));
        RoomEntity revealTick =
                session.rooms().activeRoomOrNull().entities().slots().get(hiddenGhini.slot());
        assertEquals(-1, revealTick.spriteVariant());

        session.tickEntities(new EntityTickInputs(
                2,
                new LinkPosition(revealTick.x(), revealTick.y(), revealTick.z(), 0, 0),
                new LinkMotion(false, 0, false, 0, 0, 0),
                1,
                new SwordBox(false, 0, 0, 0, 0)));
        RoomEntity visibleTick =
                session.rooms().activeRoomOrNull().entities().slots().get(hiddenGhini.slot());
        assertTrue(visibleTick.spriteVariant() >= 0);

        RoomSession oldOverloadSession = newSession();
        oldOverloadSession.rooms().loadInitialOverworld(0x67);
        RoomEntity oldOverloadGhini = oldOverloadSession.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x10)
                .findFirst()
                .orElseThrow();
        oldOverloadSession
                .entities()
                .tickEntities(0, oldOverloadGhini.x(), oldOverloadGhini.y(), oldOverloadGhini.z(), 0);
        oldOverloadSession.tickEntities(new EntityTickInputs(
                1,
                new LinkPosition(oldOverloadGhini.x(), oldOverloadGhini.y(), oldOverloadGhini.z(), 0, 0),
                new LinkMotion(false, 0, false, 0, 0, 0),
                1,
                new SwordBox(false, 0, 0, 0, 0)));

        assertEquals(
                -1,
                oldOverloadSession
                        .rooms()
                        .activeRoomOrNull()
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
        assertNotEquals(0xA0, session.rooms().activeRoomOrNull().roomObjectsArea()[chestIndex]);
        assertTrue(vfx.activeSlots().isEmpty());
    }

    @Test
    void dungeonTitleCountsOnlyInteractiveClosedInventoryFramesAfterOverworldEntry() {
        RoomSession session = newSession();
        session.rooms().loadInitialOverworld(0xD3);
        Warp entrance = session.rooms().activeRoomOrNull().warps().stream()
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
            RoomEntity spawn = session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                    .filter(entity -> entity.type() == 0x04)
                    .findFirst()
                    .orElseThrow();
            assertEquals(0x54, spawn.x());
            assertEquals(0x5C, spawn.y());

            session.entities().tickEntitiesWithProjectileEvents(0, 0x50, 0x58, 0, 0, 0, false);
            RoomEntity moved = session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
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
            int[] objects = session.rooms().activeRoomOrNull().roomObjectsArea();
            int[] renderValues = session.rooms().activeRoomOrNull().renderValues();
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
            assertTrue(RoomSessionInspection.component(session, RoomEntityBridge.class).consumeEntityEvents().stream()
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
        session.rooms().activeRoomOrNull().roomObjectsArea()[areaIndex] = 0xAB;
        assertTrue(session.playerItems().sprinkleMagicPowder(0x13, 0x29, 0, 0));
        for (int frame = 0; frame < 16; frame++) {
            session.entities().tickEntitiesWithProjectileEvents(frame, 0x20, 0x20, 0, 0, 0, false);
        }
        // The skipped draw must retain VRAM, including attributes changed
        // since ignition, rather than rebuild graphics from the old object ID.
        int topLeft = 4 * RoomConstants.ROOM_TILE_WIDTH + 4;
        session.rooms().activeRoomOrNull().tileIds()[topLeft] = 0x61;
        session.rooms().activeRoomOrNull().tileAttrs()[topLeft] = 0x25;
        int[] litTiles = torchTiles(session, 0x22);
        for (int frame = 16; frame < 1024; frame++) {
            session.entities().tickEntitiesWithProjectileEvents(frame, 0x20, 0x20, 0, 0, 0, false);
        }
        assertEquals(0xAB, session.rooms().activeRoomOrNull().roomObjectsArea()[areaIndex]);
        assertEquals(1, RoomDebugFixture.of(session).roomTriggerCountForTest());
        assertArrayEquals(litTiles, torchTiles(session, 0x22));

        session.rooms().activeRoomOrNull().roomObjectsArea()[RoomConstants.ROOM_OBJECTS_BASE + 0x33] = 0xAB;
        assertTrue(session.playerItems().sprinkleMagicPowder(0x23, 0x39, 0, 0));
        for (int frame = 1024; frame < 1040; frame++) {
            session.entities().tickEntitiesWithProjectileEvents(frame, 0x20, 0x20, 0, 0, 0, false);
        }
        assertEquals(
                0xAC, session.rooms().activeRoomOrNull().roomObjectsArea()[RoomConstants.ROOM_OBJECTS_BASE + 0x33]);
        assertArrayEquals(litTiles, torchTiles(session, 0x22));
    }

    @Test
    void torchExpiryWritesItsSavedObjectLocationEvenIfTheObjectHasChanged() {
        RoomSession session = newSession();
        session.rooms().loadIndoor(0, 0);
        int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + 0x22;
        session.rooms().activeRoomOrNull().roomObjectsArea()[areaIndex] = 0xAB;
        assertTrue(session.playerItems().sprinkleMagicPowder(0x13, 0x29, 0, 0));
        for (int frame = 0; frame < 16; frame++) {
            session.entities().tickEntitiesWithProjectileEvents(frame, 0x20, 0x20, 0, 0, 0, false);
        }
        session.rooms().activeRoomOrNull().roomObjectsArea()[areaIndex] = 0x0D;
        for (int frame = 16; frame < 1024; frame++) {
            session.entities().tickEntitiesWithProjectileEvents(frame, 0x20, 0x20, 0, 0, 0, false);
        }
        assertEquals(0xAB, session.rooms().activeRoomOrNull().roomObjectsArea()[areaIndex]);
        assertEquals(0, RoomDebugFixture.of(session).roomTriggerCountForTest());
    }

    @Test
    void magicPowderTorchExpiryDecrementsTheRomRoomTriggerCounter() {
        RoomSession session = newSession();
        session.rooms().loadIndoor(0x00, 0x00);

        int location = 0x22;
        int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + (location & 0xF0) + (location & 0x0F);
        session.rooms().activeRoomOrNull().roomObjectsArea()[areaIndex] = 0xAB;

        assertTrue(session.playerItems().sprinkleMagicPowder(0x13, 0x29, 0, 0));
        for (int frame = 0; frame < 16; frame++) {
            session.entities().tickEntitiesWithProjectileEvents(frame, 0x20, 0x20, 0, 0, 0, false);
        }
        assertEquals(1, RoomDebugFixture.of(session).roomTriggerCountForTest());

        for (int frame = 16;
                frame < 1024 && session.rooms().activeRoomOrNull().roomObjectsArea()[areaIndex] == 0xAC;
                frame++) {
            session.entities().tickEntitiesWithProjectileEvents(frame, 0x20, 0x20, 0, 0, 0, false);
        }

        assertEquals(0xAB, session.rooms().activeRoomOrNull().roomObjectsArea()[areaIndex]);
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
        session.rooms().activeRoomOrNull().roomObjectsArea()[areaIndex] = 0x04;
        session.rooms().activeRoomOrNull().renderValues()[areaIndex] = 0x04;

        ShovelStartResult start = session.playerItems().startShovel(0x44, 0x56, 0, false);

        assertTrue(start.started());
        assertFalse(start.poking());
        assertEquals(0x55, start.location());
        assertEquals(
                0x71, session.playerItems().shovelAnimationState(Direction.fromJava(Direction.RIGHT.javaIndex()), 1));
        assertEquals(
                0x72,
                session.playerItems().shovelAnimationState(Direction.fromJava(Direction.RIGHT.javaIndex()), 0x10));

        for (int timer = 1; timer <= 15; timer++) {
            session.playerItems().advanceShovel(0x44, 0x56, 0, timer);
        }
        assertEquals(0x04, session.rooms().activeRoomOrNull().roomObjectsArea()[areaIndex]);

        session.playerItems().advanceShovel(0x44, 0x56, 0, 0x10);

        assertEquals(0xCC, session.rooms().activeRoomOrNull().roomObjectsArea()[areaIndex]);
        assertEquals(0xCC, session.rooms().activeRoomOrNull().renderValues()[areaIndex]);
    }

    @Test
    void shovelQueuesTheRomRandomDropAfterTheHoleIsDrawn() {
        RoomSession session = newSession();
        session.rooms().loadInitialOverworld(0x92);

        int location = 0x55;
        int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + (location & 0xF0) + (location & 0x0F);
        session.rooms().activeRoomOrNull().roomObjectsArea()[areaIndex] = 0x04;
        session.rooms().activeRoomOrNull().renderValues()[areaIndex] = 0x04;

        assertTrue(session.playerItems().startShovel(0x44, 0x56, 0, false).started());
        session.playerItems().advanceShovel(0x44, 0x56, 0, 0x10);

        // With the ROM seed and rLY policy, frame fourteen produces the
        // source's successful 1/8 gate and selects the heart on its second
        // byte (RRA carry set).
        session.entities().tickEntities(14, 0x44, 0x56);

        RoomEntity drop = session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
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
        RoomEntity initial = session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x1E)
                .findFirst()
                .orElseThrow();

        session.entityState().setEntityActionButtonsHeld(true);
        session.entities().tickEntities(0, initial.x() + 0x10, initial.y());
        session.entities().tickEntities(1, initial.x() + 0x10, initial.y());
        session.entityState().setEntityActionButtonsHeld(false);
        session.entities().tickEntities(2, initial.x() + 0x10, initial.y());

        RoomEntity jumping =
                session.rooms().activeRoomOrNull().entities().slots().get(initial.slot());
        assertEquals(0x1E, jumping.type());
        assertEquals(1, jumping.z());
        assertEquals(2, jumping.spriteVariant());
        assertEquals(0x4E7D, jumping.spriteDefinition().address());
    }

    @Test
    void positiveZSkipsRomGroundSamplingForAnAirborneEntity() {
        RoomSession session = newSession();
        session.rooms().loadIndoor(0x00, 0x0F);
        RoomEntity initial = session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x1E)
                .findFirst()
                .orElseThrow();
        fillActiveObjects(session, 0x1B);

        session.entityState().setEntityActionButtonsHeld(true);
        session.entities().tickEntities(0, initial.x() + 0x10, initial.y());
        session.entities().tickEntities(1, initial.x() + 0x10, initial.y());
        session.entityState().setEntityActionButtonsHeld(false);
        session.entities().tickEntities(2, initial.x() + 0x10, initial.y());

        RoomEntity airborne =
                session.rooms().activeRoomOrNull().entities().slots().get(initial.slot());
        assertEquals(1, airborne.z());
        assertEquals(0, RoomDebugFixture.of(session).entityGroundStatusForTest(initial.slot()));
    }

    @Test
    void liveAnglersTunnelEvasiveStalfosCreatesTheRomFleeingCloneAfterLanding() {
        RoomSession session = newSession();
        session.rooms().loadIndoor(0x03, 0x0F);
        RoomEntity initial = session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
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
            clone = session.rooms().activeRoomOrNull().entities().slots().stream()
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
                RoomSessionInspection.component(session, RoomEntityBridge.class).consumeEntityEvents();
        assertEquals(1, events.size());
        assertEquals(EntityCombatEvent.SoundChannel.NOISE, events.getFirst().soundChannel());
        assertEquals(0x0A, events.getFirst().soundId());
    }

    @Test
    void waterTektiteMovesThroughDeepWaterInTheLiveRoomSession() {
        RoomSession session = newSession();
        session.rooms().loadIndoor(0x00, 0x65);
        RoomEntity initial = session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x99)
                .findFirst()
                .orElseThrow();
        assertEquals(0x58, initial.x());
        assertEquals(0x40, initial.y());

        int[] roomObjects = session.rooms().activeRoomOrNull().roomObjectsArea();
        for (int row = 0; row < RoomConstants.OBJECTS_PER_COLUMN; row++) {
            for (int column = 0; column < RoomConstants.OBJECTS_PER_ROW; column++) {
                roomObjects[RoomConstants.ROOM_OBJECTS_BASE + row * RoomConstants.ROOM_OBJECT_ROW_STRIDE + column] =
                        0x0E;
            }
        }

        for (int frame = 0; frame <= 8; frame++) {
            session.entities().tickEntities(frame, 0, 0);
        }

        RoomEntity moved = session.rooms().activeRoomOrNull().entities().slots().get(initial.slot());
        assertTrue(moved.x() != initial.x() || moved.y() != initial.y());
    }

    @Test
    void waterTektiteStillStopsOnSolidTerrainInTheLiveRoomSession() {
        RoomSession session = newSession();
        session.rooms().loadIndoor(0x00, 0x65);
        RoomEntity initial = session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x99)
                .findFirst()
                .orElseThrow();

        int[] roomObjects = session.rooms().activeRoomOrNull().roomObjectsArea();
        for (int row = 0; row < RoomConstants.OBJECTS_PER_COLUMN; row++) {
            for (int column = 0; column < RoomConstants.OBJECTS_PER_ROW; column++) {
                roomObjects[RoomConstants.ROOM_OBJECTS_BASE + row * RoomConstants.ROOM_OBJECT_ROW_STRIDE + column] =
                        0x10;
            }
        }

        for (int frame = 0; frame <= 8; frame++) {
            session.entities().tickEntities(frame, 0, 0);
        }

        RoomEntity blocked =
                session.rooms().activeRoomOrNull().entities().slots().get(initial.slot());
        assertEquals(initial.x(), blocked.x());
        assertEquals(initial.y(), blocked.y());
    }

    @Test
    void peaHatRetainsTheRomDeepWaterGroundStatus() {
        RoomSession session = newSession();
        session.rooms().loadIndoor(0x00, 0x6B);
        List<RoomEntity> peaHats = session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0xA0)
                .toList();
        assertTrue(!peaHats.isEmpty());
        for (RoomEntity entity : session.rooms().activeRoomOrNull().entities().loadedEntities()) {
            if (entity.type() != 0xA0) {
                session.entityState().clearEntity(entity.slot());
            }
        }
        fillActiveObjects(session, 0x0E);

        session.entities().tickEntities(0);
        session.entities().tickEntities(1);

        for (RoomEntity peaHat : peaHats) {
            RoomEntity current =
                    session.rooms().activeRoomOrNull().entities().slots().get(peaHat.slot());
            assertEquals(EntityStatus.ACTIVE, current.status());
            assertEquals(0x02, RoomDebugFixture.of(session).entityGroundStatusForTest(peaHat.slot()));
        }
        assertEquals(
                0,
                RoomSessionInspection.component(session, RoomEntityBridge.class)
                        .consumeEntityEvents()
                        .size());
    }

    @Test
    void ordinaryEntitiesReceiveTheRomConveyorNudgeEveryFourFrames() {
        RoomSession passableSession = newSession();
        RoomSession conveyorSession = newSession();
        passableSession.rooms().loadInitialOverworld(0x2F);
        conveyorSession.rooms().loadInitialOverworld(0x2F);

        RoomEntity passableInitial = passableSession.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x09)
                .findFirst()
                .orElseThrow();
        RoomEntity conveyorInitial =
                conveyorSession.rooms().activeRoomOrNull().entities().slots().get(passableInitial.slot());
        assertEquals(passableInitial.x(), conveyorInitial.x());
        assertEquals(passableInitial.y(), conveyorInitial.y());
        fillActiveObjects(passableSession, 0x01);
        fillActiveObjects(conveyorSession, 0xCF);

        for (int frame = 0; frame <= 4; frame++) {
            passableSession.entities().tickEntities(frame, 0, 0);
            conveyorSession.entities().tickEntities(frame, 0, 0);
        }

        RoomEntity passable =
                passableSession.rooms().activeRoomOrNull().entities().slots().get(passableInitial.slot());
        RoomEntity conveyor =
                conveyorSession.rooms().activeRoomOrNull().entities().slots().get(conveyorInitial.slot());
        // Overworld $CF is physics $F4, whose bank-$03 movement table entry is
        // (+1,+1). The ordinary entity's own motion is identical in both runs.
        assertEquals((passable.x() + 1) & 0xFF, conveyor.x());
        assertEquals((passable.y() + 1) & 0xFF, conveyor.y());
    }

    @Test
    void noGroundInteractionEntitiesIgnoreTheRomConveyorNudge() {
        RoomSession passableSession = newSession();
        RoomSession conveyorSession = newSession();
        passableSession.rooms().loadIndoor(0x00, 0x05);
        conveyorSession.rooms().loadIndoor(0x00, 0x05);

        RoomEntity passableInitial = passableSession.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x17)
                .findFirst()
                .orElseThrow();
        RoomEntity conveyorInitial =
                conveyorSession.rooms().activeRoomOrNull().entities().slots().get(passableInitial.slot());
        fillActiveObjects(passableSession, 0x02);
        fillActiveObjects(conveyorSession, 0xCF);

        for (int frame = 0; frame <= 4; frame++) {
            passableSession.entities().tickEntities(frame, 0, 0);
            conveyorSession.entities().tickEntities(frame, 0, 0);
        }

        RoomEntity passable =
                passableSession.rooms().activeRoomOrNull().entities().slots().get(passableInitial.slot());
        RoomEntity conveyor =
                conveyorSession.rooms().activeRoomOrNull().entities().slots().get(conveyorInitial.slot());
        assertEquals(passable.x(), conveyor.x());
        assertEquals(passable.y(), conveyor.y());
    }

    @Test
    void ordinaryEntityKeepsTheRomShallowWaterGroundStatus() {
        RoomSession session = newSession();
        session.rooms().loadInitialOverworld(0x2F);
        RoomEntity initial = session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x09)
                .findFirst()
                .orElseThrow();
        fillActiveObjects(session, 0x1B);

        session.entities().tickEntities(0);
        session.entities().tickEntities(1);

        RoomEntity entity =
                session.rooms().activeRoomOrNull().entities().slots().get(initial.slot());
        assertEquals(EntityStatus.ACTIVE, entity.status());
        assertEquals(0x02, RoomDebugFixture.of(session).entityGroundStatusForTest(initial.slot()));
    }

    @Test
    void gameplayVblankSynchronizesTheToggledStateIntoLinkCollision() {
        RoomSession session = newSession();
        session.rooms().loadIndoor(0x00, 0x0F);
        fillActiveObjects(session, 0xDC);
        RoomDebugFixture.of(session).setEntitySwitchBlocksStateForTest(0x00);

        assertTrue(RoomDebugFixture.of(session).linkCollisionPointBlockedForTest(0x20, 0x20));

        RoomDebugFixture.of(session).setSwitchableObjectAnimationStageForTest(0x02);
        session.entityState().tickGameplayVBlank();

        assertEquals(0x02, RoomDebugFixture.of(session).entitySwitchBlocksStateForTest());
        assertFalse(RoomDebugFixture.of(session).linkCollisionPointBlockedForTest(0x20, 0x20));
    }

    @Test
    void ordinaryEntityEnteringRomDeepWaterUnloadsAndCreatesSplashSideEffects() {
        TransientVfxSystem vfx = new TransientVfxSystem(16);
        RoomSession session = newSession(vfx);
        session.rooms().loadInitialOverworld(0x2F);
        RoomEntity initial = session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x09)
                .findFirst()
                .orElseThrow();
        for (RoomEntity entity : session.rooms().activeRoomOrNull().entities().loadedEntities()) {
            if (entity.slot() != initial.slot()) {
                session.entityState().clearEntity(entity.slot());
            }
        }
        fillActiveObjects(session, 0x0E);

        session.entities().tickEntities(0);
        session.entities().tickEntities(1);

        assertEquals(
                EntityStatus.DISABLED,
                session.rooms()
                        .activeRoomOrNull()
                        .entities()
                        .slots()
                        .get(initial.slot())
                        .status());
        assertEquals(0, RoomDebugFixture.of(session).entityGroundStatusForTest(initial.slot()));
        assertEquals(1, vfx.activeCount());
        assertEquals(TransientVfxType.WATER_SPLASH, vfx.activeSlots().getFirst().type());
        List<EntityCombatEvent> events =
                RoomSessionInspection.component(session, RoomEntityBridge.class).consumeEntityEvents();
        assertEquals(1, events.size());
        assertEquals(EntityCombatEvent.SoundChannel.JINGLE, events.getFirst().soundChannel());
        assertEquals(0x0E, events.getFirst().soundId());
    }

    @Test
    void loadsColorDungeonEntityRowsThroughTheSpecialRoomPath() {
        byte[] rom = loadRom();
        RoomSession session = newSession();

        session.rooms().loadIndoor(0xFF, 0x00);

        int sourceOffset = RomBank.romOffset(0x35, 0x5100);
        int expectedPixel = ((Byte.toUnsignedInt(rom[sourceOffset]) >>> 7) & 0x01)
                | (((Byte.toUnsignedInt(rom[sourceOffset + 1]) >>> 7) & 0x01) << 1);
        assertEquals(
                EntityRoomLoader.RoomTable.COLOR_DUNGEON,
                session.rooms().activeRoomOrNull().entities().spriteSelection().roomTable());
        assertNotNull(session.rooms().activeRoomOrNull().entities().spriteTiles());
        assertEquals(
                expectedPixel,
                session.rooms()
                        .activeRoomOrNull()
                        .entities()
                        .spriteTiles()
                        .tile(0x40)
                        .getPixel(0, 0));

        int objectId = session.rooms().activeRoomOrNull().roomObjectsArea()[RoomConstants.ROOM_OBJECTS_BASE];
        int expectedTileOffset = RomBank.romOffset(0x08, 0x4760) + objectId * 4;
        int expectedAttrOffset = RomBank.romOffset(0x23, 0x6000) + objectId * 4;
        int expectedPaletteOffset = RomBank.romOffset(0x21, 0x67D0);
        int expectedPaletteColor = RomBank.decodeRgb555(Byte.toUnsignedInt(rom[expectedPaletteOffset])
                | (Byte.toUnsignedInt(rom[expectedPaletteOffset + 1]) << 8));
        assertEquals(expectedPaletteColor, session.rooms().activeRoomOrNull().palettes()[0][0]);
        assertEquals(
                Byte.toUnsignedInt(rom[expectedTileOffset]),
                session.rooms().activeRoomOrNull().tileIds()[0]);
        assertEquals(
                Byte.toUnsignedInt(rom[expectedAttrOffset]),
                session.rooms().activeRoomOrNull().tileAttrs()[0]);
    }

    @Test
    void colorShellPuzzleWritesBackIntoTheLiveRoomTilemap() {
        RoomSession session = newSession();
        session.rooms().loadIndoor(0xFF, 0x07);
        session.entities().tickEntities(0);

        RoomEntity shell = session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0xE9)
                .findFirst()
                .orElseThrow();
        int objectIndex =
                RoomConstants.ROOM_OBJECTS_BASE + ((shell.y() - 0x07) & 0xF0) + (((shell.x() - 0x01) & 0xF0) >>> 4);
        session.rooms().activeRoomOrNull().roomObjectsArea()[objectIndex - 1] = 0x5E;
        int[] beforeTiles = session.rooms().activeRoomOrNull().tileIds().clone();
        RoomDebugFixture.of(session).setColorShellStateForTest(shell.slot(), 0x08, 1, 0, 0, 0);

        session.entities().tickEntities(1);

        assertEquals(0x67, session.rooms().activeRoomOrNull().roomObjectsArea()[objectIndex]);
        assertNotEquals(
                Arrays.toString(beforeTiles),
                Arrays.toString(session.rooms().activeRoomOrNull().tileIds()));
    }

    @Test
    void persistsClearedFirstEightEntitySlotsWhenTheRoomReloads() {
        RoomSession session = newSession();
        session.rooms().loadInitialOverworld(0x92);

        session.entityState().clearEntity(0);
        session.rooms().loadOverworld(0x92);

        assertEquals(
                0x3E,
                session.rooms().activeRoomOrNull().entities().slots().get(0).type());
        assertEquals(
                1, session.rooms().activeRoomOrNull().entities().slots().get(0).sourceLoadOrder());
    }

    @Test
    void exposesAndRestoresTheRoomStatusTablesUsedBySaveGameToFile() {
        RoomSession session = newSession();
        byte[] overworld = pattern(0x100, 0x10);
        byte[] indoorA = pattern(0x100, 0x40);
        byte[] indoorB = pattern(0x100, 0x70);
        byte[] colorDungeon = pattern(0x20, 0xA0);

        session.story().restoreRoomStatuses(overworld, indoorA, indoorB, colorDungeon);

        assertArrayEquals(overworld, session.dungeon().overworldRoomStatusSnapshot());
        assertArrayEquals(indoorA, session.dungeon().indoorARoomStatusSnapshot());
        assertArrayEquals(indoorB, session.dungeon().indoorBRoomStatusSnapshot());
        assertArrayEquals(colorDungeon, session.dungeon().colorDungeonRoomStatusSnapshot());
    }

    @Test
    void exposesAndRestoresDungeonItemFlagsUsedByTheWorldHandler() {
        RoomSession session = newSession();
        byte[] dungeonFlags = pattern(DungeonItemState.DUNGEON_ITEM_FLAGS_SIZE, 0x10);
        byte[] colorFlags = pattern(DungeonItemState.COLOR_DUNGEON_ITEM_FLAGS_SIZE, 0xE0);

        session.dungeon().restoreDungeonItemFlags(dungeonFlags, colorFlags);
        session.rooms().loadIndoor(2, 0x25);

        assertArrayEquals(dungeonFlags, session.dungeon().dungeonItemFlagsSnapshot());
        assertArrayEquals(colorFlags, session.dungeon().colorDungeonItemFlagsSnapshot());
        assertArrayEquals(
                new byte[] {
                    dungeonFlags[2 * DungeonItemState.ITEM_FLAG_SIZE],
                    dungeonFlags[2 * DungeonItemState.ITEM_FLAG_SIZE + 1],
                    dungeonFlags[2 * DungeonItemState.ITEM_FLAG_SIZE + 2],
                    dungeonFlags[2 * DungeonItemState.ITEM_FLAG_SIZE + 3],
                    dungeonFlags[2 * DungeonItemState.ITEM_FLAG_SIZE + 4]
                },
                session.dungeon().currentDungeonItemFlagsSnapshot());
    }

    @Test
    void exposesAndRestoresTheEightSourceDungeonProgressBytes() {
        RoomSession session = newSession();
        byte[] flags = pattern(0x08, 0x01);

        session.dungeon().restoreDungeonProgressFlags(flags);

        assertArrayEquals(flags, session.dungeon().dungeonProgressFlagsSnapshot());
        assertTrue(RoomDebugFixture.of(session).hasDungeonInstrumentForTest(1));
        assertTrue(RoomDebugFixture.of(session).hasDungeonInstrumentForTest(2));
    }

    @Test
    void madamMeowMeowRejectsWrongFacingWrongButtonAndTalkStateGates() {
        RoomSession session = newSession();
        session.story().setBowWowState(0x80);
        session.rooms().loadIndoor(0x10, 0xA7);
        RoomEntity madam = session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x79)
                .findFirst()
                .orElseThrow();
        int linkY = madam.y() + 0x10;

        session.entityState().setEntityActionButtonsHeld(true, false);
        session.entities().tickEntities(0, madam.x(), linkY, 0, Direction.DOWN.javaIndex());
        assertTrue(session.entityState().consumeEntityDialogRequests().isEmpty());

        session.entityState().setEntityActionButtonsHeld(false, true);
        session.entities().tickEntities(1, madam.x(), linkY, 0, Direction.UP.javaIndex());
        assertTrue(session.entityState().consumeEntityDialogRequests().isEmpty());

        session.entityState().setEntityActionButtonsHeld(true, false);
        session.entityState().setEntityTalkState(false, 1, 0x80);
        session.entities().tickEntities(2, madam.x(), linkY, 0, Direction.UP.javaIndex());
        assertTrue(session.entityState().consumeEntityDialogRequests().isEmpty());

        session.entityState().setEntityTalkState(false, 0, 0x70);
        session.entities().tickEntities(3, madam.x(), linkY, 0, Direction.UP.javaIndex());
        assertTrue(session.entityState().consumeEntityDialogRequests().isEmpty());

        session.entityState().setEntityTalkState(false, 0, 0x80);
        session.entities().tickEntities(4, madam.x(), linkY, 0, true, Direction.UP.javaIndex());
        assertTrue(session.entityState().consumeEntityDialogRequests().isEmpty());
    }
}
