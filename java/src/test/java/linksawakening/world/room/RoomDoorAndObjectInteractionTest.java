package linksawakening.world.room;

import static linksawakening.world.room.RoomSessionTestSupport.fillActiveObjects;
import static linksawakening.world.room.RoomSessionTestSupport.loadRom;
import static linksawakening.world.room.RoomSessionTestSupport.newSession;
import static linksawakening.world.room.RoomSessionTestSupport.torchTiles;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.physics.PhysicsFlags;
import linksawakening.rom.RomBank;
import linksawakening.state.Direction;
import linksawakening.world.entity.EntityBackgroundCollisionResult;
import linksawakening.world.entity.EntityRoomLoader;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityBuilder;
import linksawakening.world.entity.RoomEntityRuntimeBuilder;
import linksawakening.world.entity.RoomEntitySnapshot;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.motion.HookshotBridgeMotion;
import linksawakening.world.transition.ScrollController;
import linksawakening.world.transition.Warp;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class RoomDoorAndObjectInteractionTest {
    @Test
    void owlStatuePushUsesVisualOffsetBeforeDialogGateButSkipsAirborneLink() {
        RoomSession session = newSession();
        session.rooms().loadIndoor(2, 0x54);
        session.entities().tickEntities(0, 0x38, 0x53, 0, Direction.UP.javaIndex());
        session.entityState().setEntityDialogActive(true);
        session.entities().tickEntitiesWithProjectileEvents(1, 0x38, 0x4A, 0, 0, Direction.UP.javaIndex(), false);
        var pushes =
                RoomSessionInspection.component(session, RoomEntityBridge.class).consumeLinkFinalPositionRequests();
        assertEquals(1, pushes.size());
        assertTrue(pushes.getFirst().resetPegasusBoots());
        assertTrue(pushes.getFirst().resetHookshotChain());
        session.entities().tickEntitiesWithProjectileEvents(2, 0x38, 0x4B, 0, 0, Direction.UP.javaIndex(), false);
        assertTrue(RoomSessionInspection.component(session, RoomEntityBridge.class)
                .consumeLinkFinalPositionRequests()
                .isEmpty());
        session.entities().tickEntitiesWithProjectileEvents(3, 0x38, 0x4A, 1, 0, Direction.UP.javaIndex(), false);
        assertTrue(RoomSessionInspection.component(session, RoomEntityBridge.class)
                .consumeLinkFinalPositionRequests()
                .isEmpty());
        session.entities().tickEntitiesWithProjectileEvents(4, 0x38, 0x4A, 0, 2, Direction.UP.javaIndex(), false);
        assertTrue(RoomSessionInspection.component(session, RoomEntityBridge.class)
                .consumeLinkFinalPositionRequests()
                .isEmpty());
    }

    @Test
    void indoorStaircaseArmsAfterLinkLeavesAndThenReturnsWarpZero() {
        RoomSession session = newSession();
        byte[] indoorA = new byte[0x100];
        indoorA[0x03] = 0x10;
        session.story().restoreRoomStatuses(new byte[0x100], indoorA, new byte[0x100], new byte[0x20]);
        session.rooms().loadIndoor(0x00, 0x03);

        assertEquals(0x18, session.rooms().activeRoomOrNull().staircaseLocation());
        assertNull(session.transitions().pollStaircaseWarp(0x88, 0x20, 0, false));
        assertNull(session.transitions().pollStaircaseWarp(0x98, 0x20, 0, false));
        assertEquals(
                session.rooms().activeRoomOrNull().firstWarp(),
                session.transitions().pollStaircaseWarp(0x88, 0x20, 0, false));
        assertNull(session.transitions().pollStaircaseWarp(0x88, 0x20, 0, false));
    }

    @Test
    void activeIndoorStaircaseRejectsAirborneAndCarryingLink() {
        RoomSession session = newSession();
        byte[] indoorA = new byte[0x100];
        indoorA[0x03] = 0x10;
        session.story().restoreRoomStatuses(new byte[0x100], indoorA, new byte[0x100], new byte[0x20]);
        session.rooms().loadIndoor(0x00, 0x03);

        assertNull(session.transitions().pollStaircaseWarp(0x98, 0x20, 0, false));
        assertNull(session.transitions().pollStaircaseWarp(0x88, 0x20, 1, false));
        assertNull(session.transitions().pollStaircaseWarp(0x88, 0x20, 0, true));
        assertEquals(
                session.rooms().activeRoomOrNull().firstWarp(),
                session.transitions().pollStaircaseWarp(0x88, 0x20, 0, false));
    }

    @Test
    void savedIndoorRoomPositionIsRestoredIndependentlyOfTheRoomId() {
        RoomSession session = newSession();

        session.rooms().loadIndoorFromSavedPosition(0x10, 0xA3, 0x27);

        assertEquals(0xA3, session.rooms().currentRoomId());
        assertEquals(0x27, session.rooms().indoorRoomPositionForSave());
    }

    @Test
    void tailCaveSingleBlockRequiresSixtyFourUninterruptedPushTicks() {
        RoomSession session = newSession();
        session.rooms().loadIndoor(0x00, 0x04);
        int blockIndex = RoomConstants.ROOM_OBJECTS_BASE + 0x32;
        assertEquals(0xA7, session.rooms().activeRoomOrNull().roomObjectsArea()[blockIndex]);

        for (int tick = 0; tick < 32; tick++) {
            assertTrue(session.doors()
                    .tryPushIndoorBlock(0x15, 0x28, Direction.fromJavaOrUnknown(Direction.RIGHT.javaIndex()), 0x08));
        }
        assertFalse(session.doors()
                .tryPushIndoorBlock(0x15, 0x28, Direction.fromJavaOrUnknown(Direction.RIGHT.javaIndex()), 0x00));
        for (int tick = 0; tick < 32; tick++) {
            assertTrue(session.doors()
                    .tryPushIndoorBlock(0x15, 0x28, Direction.fromJavaOrUnknown(Direction.RIGHT.javaIndex()), 0x08));
        }
        assertEquals(0xA7, session.rooms().activeRoomOrNull().roomObjectsArea()[blockIndex]);

        for (int tick = 32; tick < 63; tick++) {
            assertTrue(session.doors()
                    .tryPushIndoorBlock(0x15, 0x28, Direction.fromJavaOrUnknown(Direction.RIGHT.javaIndex()), 0x08));
        }
        assertEquals(0xA7, session.rooms().activeRoomOrNull().roomObjectsArea()[blockIndex]);

        assertTrue(session.doors()
                .tryPushIndoorBlock(0x15, 0x28, Direction.fromJavaOrUnknown(Direction.RIGHT.javaIndex()), 0x08));
        assertEquals(0x0D, session.rooms().activeRoomOrNull().roomObjectsArea()[blockIndex]);

        RoomSession liveDispatch = newSession();
        liveDispatch.rooms().loadIndoor(0x00, 0x04);
        for (int tick = 0; tick < 64; tick++) {
            assertTrue(liveDispatch
                    .doors()
                    .tryInteractWithIndoorBlock(
                            0x15, 0x28, Direction.fromJavaOrUnknown(Direction.RIGHT.javaIndex()), 0x08));
        }
        assertEquals(0x0D, liveDispatch.rooms().activeRoomOrNull().roomObjectsArea()[blockIndex]);
    }

    @Test
    void bottleGrottoTwoHorizontalBlocksRevealTheRoom2FStaircase() {
        RoomSession session = newSession();
        session.rooms().loadIndoor(0x01, 0x2F);

        int firstBlockIndex = RoomConstants.ROOM_OBJECTS_BASE + 0x33;
        int secondBlockIndex = RoomConstants.ROOM_OBJECTS_BASE + 0x36;
        int firstDestinationIndex = RoomConstants.ROOM_OBJECTS_BASE + 0x34;
        int secondDestinationIndex = RoomConstants.ROOM_OBJECTS_BASE + 0x35;
        int stairsIndex = RoomConstants.ROOM_OBJECTS_BASE + 0x18;
        assertEquals(0xA7, RoomDebugFixture.of(session).activeRoomEventForTest());
        assertEquals(0xA7, session.rooms().activeRoomOrNull().roomObjectsArea()[firstBlockIndex]);
        assertEquals(0xA7, session.rooms().activeRoomOrNull().roomObjectsArea()[secondBlockIndex]);
        // ConfigureRoomObjects omits concealed $BF until room status $10 is set;
        // the source room-template floor remains in the active grid initially.
        assertEquals(0x0D, session.rooms().activeRoomOrNull().roomObjectsArea()[stairsIndex]);

        for (int tick = 0; tick < 64; tick++) {
            assertTrue(session.doors()
                    .tryPushIndoorBlock(0x25, 0x28, Direction.fromJavaOrUnknown(Direction.RIGHT.javaIndex()), 0x08));
        }
        assertEquals(0xA7, RoomDebugFixture.of(session).activeRoomEventForTest());
        for (int frame = 0; frame < 33; frame++) {
            session.entities().tickEntities(frame, 0x25, 0x28);
        }
        assertEquals(0x0D, session.rooms().activeRoomOrNull().roomObjectsArea()[firstBlockIndex]);
        assertEquals(0xA6, session.rooms().activeRoomOrNull().roomObjectsArea()[firstDestinationIndex]);
        assertEquals(0xA7, RoomDebugFixture.of(session).activeRoomEventForTest());

        for (int tick = 0; tick < 64; tick++) {
            assertTrue(session.doors()
                    .tryPushIndoorBlock(0x5C, 0x28, Direction.fromJavaOrUnknown(Direction.LEFT.javaIndex()), 0x04));
        }
        assertEquals(0xA7, RoomDebugFixture.of(session).activeRoomEventForTest());
        for (int frame = 33; frame < 66; frame++) {
            session.entities().tickEntities(frame, 0x5C, 0x28);
        }
        assertEquals(0x0D, session.rooms().activeRoomOrNull().roomObjectsArea()[secondBlockIndex]);
        assertEquals(0xA6, session.rooms().activeRoomOrNull().roomObjectsArea()[secondDestinationIndex]);
        assertEquals(0xA7, RoomDebugFixture.of(session).activeRoomEventForTest());
        session.entities().tickEntities(66, 0, 0);
        assertEquals(0, RoomDebugFixture.of(session).activeRoomEventForTest());

        for (int frame = 67; frame < 78; frame++) {
            session.entities().tickEntities(frame, 0, 0);
        }
        assertEquals(0xBE, session.rooms().activeRoomOrNull().roomObjectsArea()[stairsIndex]);
        assertEquals(0x10, RoomDebugFixture.of(session).indoorRoomStatusForTest(0x01, 0x2F) & 0x10);
        session.rooms().loadIndoor(0x01, 0x2F);
        assertEquals(0xBF, session.rooms().activeRoomOrNull().roomObjectsArea()[stairsIndex]);
    }

    @Test
    void bottleGrottoRoom2fSettlementUsesFacingSideAndRejectsVerticalPushes() {
        RoomSession oppositeSide = newSession();
        oppositeSide.rooms().loadIndoor(0x01, 0x2F);
        for (int tick = 0; tick < 64; tick++) {
            assertTrue(oppositeSide
                    .doors()
                    .tryPushIndoorBlock(0x25, 0x28, Direction.fromJavaOrUnknown(Direction.RIGHT.javaIndex()), 0x08));
        }
        // Arrange an opposite-side settled neighbor while the real block is
        // in motion; the settlement branch must still inspect only its facing
        // side through the normal push/tick path.
        oppositeSide.rooms().activeRoomOrNull().roomObjectsArea()[RoomConstants.ROOM_OBJECTS_BASE + 0x33] = 0xA6;
        for (int frame = 0; frame < 33; frame++) {
            oppositeSide.entities().tickEntities(frame, 0x25, 0x28);
        }
        assertEquals(
                0xA6,
                oppositeSide.rooms().activeRoomOrNull().roomObjectsArea()[RoomConstants.ROOM_OBJECTS_BASE + 0x34]);
        assertEquals(0xA7, RoomDebugFixture.of(oppositeSide).activeRoomEventForTest());
        oppositeSide.entities().tickEntities(33, 0, 0);
        assertEquals(0xA7, RoomDebugFixture.of(oppositeSide).activeRoomEventForTest());

        RoomSession vertical = newSession();
        vertical.rooms().loadIndoor(0x01, 0x2F);
        for (int tick = 0; tick < 64; tick++) {
            assertTrue(vertical.doors()
                    .tryPushIndoorBlock(0x2A, 0x38, Direction.fromJavaOrUnknown(Direction.UP.javaIndex()), 0x01));
        }
        for (int frame = 0; frame < 33; frame++) {
            vertical.entities().tickEntities(frame, 0x2A, 0x38);
        }
        assertEquals(
                0xA6, vertical.rooms().activeRoomOrNull().roomObjectsArea()[RoomConstants.ROOM_OBJECTS_BASE + 0x23]);
        assertEquals(0xA7, RoomDebugFixture.of(vertical).activeRoomEventForTest());
        vertical.entities().tickEntities(33, 0, 0);
        assertEquals(0xA7, RoomDebugFixture.of(vertical).activeRoomEventForTest());
    }

    @Test
    void tailCaveKeyholeBlockRequiresThirtyTwoContactTicksAndConsumesAKey() {
        RoomSession noKey = newSession();
        noKey.rooms().loadIndoor(0x00, 0x09);
        assertTrue(noKey.doors()
                .tryInteractWithIndoorBlock(0x20, 0x4A, Direction.fromJavaOrUnknown(Direction.UP.javaIndex()), 0x01));
        assertEquals(
                0x8C,
                noKey.entityState().consumeEntityDialogRequests().getFirst().dialogLowId());

        RoomSession session = newSession();
        List<GameplaySoundEvent> sounds = new ArrayList<>();
        session.entityState().setColorShellSoundSink(sounds::add);
        byte[] dungeonFlags = new byte[DungeonItemState.DUNGEON_ITEM_FLAGS_SIZE];
        dungeonFlags[DungeonItemState.SMALL_KEYS_INDEX] = 1;
        session.dungeon()
                .restoreDungeonItemFlags(dungeonFlags, new byte[DungeonItemState.COLOR_DUNGEON_ITEM_FLAGS_SIZE]);
        session.rooms().loadIndoor(0x00, 0x09);
        int blockIndex = RoomConstants.ROOM_OBJECTS_BASE + 0x52;
        assertEquals(0xDE, session.rooms().activeRoomOrNull().roomObjectsArea()[blockIndex]);

        for (int tick = 0; tick < 31; tick++) {
            assertTrue(session.doors()
                    .tryInteractWithIndoorBlock(
                            0x20, 0x4A, Direction.fromJavaOrUnknown(Direction.UP.javaIndex()), 0x01));
        }
        assertEquals(0xDE, session.rooms().activeRoomOrNull().roomObjectsArea()[blockIndex]);
        assertTrue(session.doors()
                .tryInteractWithIndoorBlock(0x20, 0x4A, Direction.fromJavaOrUnknown(Direction.UP.javaIndex()), 0x01));

        assertEquals(0x0D, session.rooms().activeRoomOrNull().roomObjectsArea()[blockIndex]);
        assertEquals(0, session.dungeon().currentDungeonItemFlagsSnapshot()[DungeonItemState.SMALL_KEYS_INDEX]);
        assertEquals(0x40, RoomDebugFixture.of(session).indoorRoomStatusForTest(0x00, 0x09) & 0x40);
        assertEquals(List.of(GameplaySoundEvent.DOOR_UNLOCKED), sounds);
        session.rooms().loadIndoor(0x00, 0x09);
        assertEquals(0x0D, session.rooms().activeRoomOrNull().roomObjectsArea()[blockIndex]);
    }

    @Test
    void movingBlockRestoresContactingLinkEvenInAirButNotDuringDialogue() {
        RoomSession session = newSession();
        session.rooms().loadIndoor(0, 4);
        for (int tick = 0; tick < 64; tick++) {
            session.doors()
                    .tryPushIndoorBlock(0x15, 0x28, Direction.fromJavaOrUnknown(Direction.RIGHT.javaIndex()), 0x08);
        }
        session.entities().tickEntitiesWithProjectileEvents(0, 0x1B, 0x40, 12, 0, Direction.RIGHT.javaIndex(), false);
        var requests =
                RoomSessionInspection.component(session, RoomEntityBridge.class).consumeLinkFinalPositionRequests();
        assertEquals(1, requests.size());
        assertTrue(requests.getFirst().markLinkPushing());
        assertFalse(requests.getFirst().clearLinkPositionIncrement());
        assertFalse(requests.getFirst().resetPegasusBoots());
        session.entityState().setEntityDialogActive(true);
        session.entities().tickEntitiesWithProjectileEvents(1, 0x28, 0x40, 0, 0, Direction.RIGHT.javaIndex(), false);
        assertTrue(RoomSessionInspection.component(session, RoomEntityBridge.class)
                .consumeLinkFinalPositionRequests()
                .isEmpty());
        session.entityState().setEntityDialogActive(false);
        session.entities().tickEntitiesWithProjectileEvents(2, 0x80, 0x70, 0, 0, Direction.RIGHT.javaIndex(), false);
        assertTrue(RoomSessionInspection.component(session, RoomEntityBridge.class)
                .consumeLinkFinalPositionRequests()
                .isEmpty());
    }

    @Test
    void settledTailCaveSingleBlockOpensDoorUntilTheRoomReloads() {
        RoomSession session = newSession();
        List<GameplaySoundEvent> sounds = new ArrayList<>();
        session.entityState().setColorShellSoundSink(sounds::add);
        session.rooms().loadIndoor(0x00, 0x04);
        int destinationIndex = RoomConstants.ROOM_OBJECTS_BASE + 0x33;
        int initialDestination = session.rooms().activeRoomOrNull().roomObjectsArea()[destinationIndex];
        assertEquals(0x22, RoomDebugFixture.of(session).activeRoomEventForTest());
        assertNotEquals(0, session.rooms().activeRoomOrNull().shutterDoorMask());

        for (int tick = 0; tick < 64; tick++) {
            assertTrue(session.doors()
                    .tryPushIndoorBlock(0x15, 0x28, Direction.fromJavaOrUnknown(Direction.RIGHT.javaIndex()), 0x08));
        }
        session.entityState().setEntityDialogActive(true);
        for (int frame = 0; frame < 8; frame++) {
            session.entities().tickEntities(frame, 0x15, 0x28);
        }
        RoomEntity pausedBlock = session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x06)
                .findFirst()
                .orElseThrow();
        assertEquals(0x28, pausedBlock.x());
        session.entityState().setEntityDialogActive(false);
        for (int frame = 0; frame < 32; frame++) {
            session.entities().tickEntities(frame, 0x15, 0x28);
        }
        RoomEntity movingBlock = session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x06)
                .findFirst()
                .orElseThrow();
        assertTrue(movingBlock.spriteDefinition().supported());
        assertEquals(0x38, movingBlock.x());
        assertEquals(0x40, movingBlock.y());
        assertEquals(initialDestination, session.rooms().activeRoomOrNull().roomObjectsArea()[destinationIndex]);
        assertNotEquals(0, session.rooms().activeRoomOrNull().shutterDoorMask());

        ActiveRoom room = session.rooms().activeRoomOrNull();
        int shutterIndex = RoomConstants.ROOM_OBJECTS_BASE + room.shutterLocation(2);
        int closedShutter = room.roomObjectsArea()[shutterIndex];
        session.entities().tickEntities(32, 0x15, 0x28);

        assertEquals(0xA6, session.rooms().activeRoomOrNull().roomObjectsArea()[destinationIndex]);
        assertEquals(0, RoomDebugFixture.of(session).activeRoomEventForTest());
        session.entities().tickEntities(33, 0x15, 0x28);
        session.entities().tickEntities(34, 0x15, 0x28);
        assertNotEquals(0, room.shutterDoorMask());
        assertEquals(closedShutter, room.roomObjectsArea()[shutterIndex]);
        for (int frame = 35; frame < 51 && room.shutterDoorMask() != 0; frame++) {
            session.entities().tickEntities(frame, 0x15, 0x28);
        }
        assertEquals(0, session.rooms().activeRoomOrNull().shutterDoorMask());
        assertNotEquals(closedShutter, room.roomObjectsArea()[shutterIndex]);
        assertEquals(0, RoomDebugFixture.of(session).indoorRoomStatusForTest(0x00, 0x04) & 0x10);
        assertEquals(0x02, RoomDebugFixture.of(session).indoorRoomStatusForTest(0x00, 0x04) & 0x02);
        assertEquals(0x01, RoomDebugFixture.of(session).indoorRoomStatusForTest(0x00, 0x03) & 0x01);
        assertEquals(
                List.of(
                        GameplaySoundEvent.BLOCK_PUSH,
                        GameplaySoundEvent.DOOR_CLOSED,
                        GameplaySoundEvent.PUZZLE_SOLVED,
                        GameplaySoundEvent.DOOR_UNLOCKED),
                sounds);

        session.rooms().loadIndoor(0x00, 0x04);
        assertNotEquals(0, session.rooms().activeRoomOrNull().shutterDoorMask());
        assertEquals(0x22, RoomDebugFixture.of(session).activeRoomEventForTest());
    }

    @Test
    void blockedOrUnallocatedPushedBlockLeavesItsSourceObjectIntact() {
        RoomSession blocked = newSession();
        blocked.rooms().loadIndoor(0x00, 0x04);
        int sourceIndex = RoomConstants.ROOM_OBJECTS_BASE + 0x32;
        int destinationIndex = RoomConstants.ROOM_OBJECTS_BASE + 0x33;
        blocked.rooms().activeRoomOrNull().roomObjectsArea()[destinationIndex] = 0xA6;
        for (int tick = 0; tick < 64; tick++) {
            assertTrue(blocked.doors()
                    .tryPushIndoorBlock(0x15, 0x28, Direction.fromJavaOrUnknown(Direction.RIGHT.javaIndex()), 0x08));
        }
        assertEquals(0xA7, blocked.rooms().activeRoomOrNull().roomObjectsArea()[sourceIndex]);
        assertFalse(blocked.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                .anyMatch(entity -> entity.type() == 0x06));

        RoomSession full = newSession();
        full.rooms().loadIndoor(0x00, 0x04);
        List<RoomEntity> occupiedSlots = new ArrayList<>();
        for (int slot = 0; slot < EntityRoomLoader.MAX_ENTITIES; slot++) {
            occupiedSlots.add(new RoomEntityBuilder()
                    .slot(slot)
                    .sourceLoadOrder(-1)
                    .type(0x42)
                    .at(0x08, 0x10)
                    .status(EntityStatus.ACTIVE)
                    .spriteDefinition(EntitySpriteDefinition.unsupported(0x42))
                    .spriteVariant(-1)
                    .entityFlipAttribute(0)
                    .spriteTileOffset(0)
                    .z(0)
                    .deathSpriteVariant(-1)
                    .powerRecoilDeath(false)
                    .build());
        }
        RoomDebugFixture.of(full)
                .replaceEntityRuntimeForTest(new RoomEntityRuntimeBuilder(new RoomEntitySnapshot(occupiedSlots))
                        .indoorRoom(true)
                        .build());
        for (int tick = 0; tick < 64; tick++) {
            assertTrue(full.doors()
                    .tryPushIndoorBlock(0x15, 0x28, Direction.fromJavaOrUnknown(Direction.RIGHT.javaIndex()), 0x08));
        }
        assertEquals(0xA7, full.rooms().activeRoomOrNull().roomObjectsArea()[sourceIndex]);
    }

    @Test
    void tailCaveCardPuzzleRevealsAndAwardsTheStoneBeakThroughCombat() {
        RoomSession session = newSession();
        session.rooms().loadIndoor(0x00, 0x0A);
        session.entities().tickEntitiesWithProjectileEvents(1, 0, 0, 0, 0, Direction.DOWN.javaIndex(), false);
        List<RoomEntity> cards = session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == EntitySpriteHandlerCatalog.ENTITY_THREE_OF_A_KIND)
                .toList();
        assertEquals(3, cards.size());
        assertEquals(0x61, RoomDebugFixture.of(session).activeRoomEventForTest());

        // Resolve each sword collision on its source alternating-slot cadence
        // without advancing the shared card face between hits.
        for (RoomEntity card : cards) {
            List<EntityCombatEvent> hit = session.entities()
                    .resolveEntityCombat(new EntityCombatInputs(
                            card.slot() ^ 1,
                            0,
                            0,
                            false,
                            0,
                            true,
                            new SwordBox(true, card.x() + 0x08, 1, card.y() + 0x08, 1),
                            linksawakening.world.entity.EnemyAttackContext.standard()));
            assertTrue(hit.stream().anyMatch(event -> event.slot() == card.slot() && event.swordHit()));
        }

        int frame = 2;
        int deadline = frame + 0x100;
        while (RoomDebugFixture.of(session).activeRoomEventForTest() != 0 && frame < deadline) {
            session.entities().tickEntitiesWithProjectileEvents(frame++, 0, 0, 0, 0, Direction.DOWN.javaIndex(), false);
        }

        assertEquals(
                0,
                RoomDebugFixture.of(session).activeRoomEventForTest(),
                session.rooms().activeRoomOrNull().entities().loadedEntities().toString());
        int chestIndex = RoomConstants.ROOM_OBJECTS_BASE + 0x28;
        int appearanceDeadline = frame + 0x20;
        while (session.rooms().activeRoomOrNull().roomObjectsArea()[chestIndex] != 0xA0 && frame < appearanceDeadline) {
            session.entities().tickEntitiesWithProjectileEvents(frame++, 0, 0, 0, 0, Direction.DOWN.javaIndex(), false);
        }
        assertEquals(0xA0, session.rooms().activeRoomOrNull().roomObjectsArea()[chestIndex]);

        ChestOpenResult chest = session.doors()
                .tryOpenChest(0x78, 0x21, Direction.fromJavaOrUnknown(Direction.UP.javaIndex()), true, 1);
        assertTrue(chest.opened());
        assertEquals(ChestContentsTable.CHEST_STONE_BEAK, chest.itemType());
        session.entities().tickEntitiesWithProjectileEvents(frame, 0x78, 0x21, 0, 0, Direction.DOWN.javaIndex(), false);
        assertEquals(1, session.dungeon().currentDungeonItemFlagsSnapshot()[DungeonItemState.STONE_BEAK_INDEX]);
    }

    @Test
    void rollingBonesRoomOpensItsRomShuttersAndPersistsTheMinibossFlag() {
        RoomSession session = newSession();
        session.rooms().loadIndoor(0x00, 0x11);
        assertEquals(0x05, session.rooms().activeRoomOrNull().shutterDoorMask());
        assertEquals(0xC1, RoomDebugFixture.of(session).activeRoomEventForTest());

        List<RoomEntity> emptySlots = new ArrayList<>();
        for (int slot = 0; slot < EntityRoomLoader.MAX_ENTITIES; slot++) {
            emptySlots.add(RoomEntity.disabled(slot));
        }
        RoomDebugFixture.of(session)
                .replaceEntityRuntimeForTest(new RoomEntityRuntimeBuilder(new RoomEntitySnapshot(emptySlots))
                        .indoorRoom(true)
                        .build());

        session.entities().tickEntities(0, 0x50, 0x50);

        assertEquals(0, session.rooms().activeRoomOrNull().shutterDoorMask());
        assertEquals(0, RoomDebugFixture.of(session).activeRoomEventForTest());
        assertEquals(0x20, RoomDebugFixture.of(session).indoorRoomStatusForTest(0x00, 0x11) & 0x20);
        assertTrue(RoomSessionInspection.component(session, RoomEntityBridge.class).consumeEntityEvents().stream()
                .anyMatch(event ->
                        event.soundChannel() == EntityCombatEvent.SoundChannel.JINGLE && event.soundId() == 0x1B));

        session.rooms().loadIndoor(0x00, 0x11);
        assertEquals(0, session.rooms().activeRoomOrNull().shutterDoorMask());
    }

    @Test
    void tailCaveRoom16KillAllEventDropsTheThirdSmallKey() {
        RoomSession session = newSession();
        session.rooms().loadIndoor(0x00, 0x16);
        assertEquals(0x81, RoomDebugFixture.of(session).activeRoomEventForTest());

        List<RoomEntity> emptySlots = new ArrayList<>();
        for (int slot = 0; slot < EntityRoomLoader.MAX_ENTITIES; slot++) {
            emptySlots.add(RoomEntity.disabled(slot));
        }
        RoomDebugFixture.of(session)
                .replaceEntityRuntimeForTest(new RoomEntityRuntimeBuilder(new RoomEntitySnapshot(emptySlots))
                        .indoorRoom(true)
                        .build());

        session.entities().tickEntities(0, 0x50, 0x50);

        RoomEntity key = session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x30)
                .findFirst()
                .orElseThrow();
        assertEquals(0x28, key.x());
        assertEquals(0x3C, key.y());
        assertEquals(0x70, key.z());
        assertEquals(0, RoomDebugFixture.of(session).activeRoomEventForTest());
    }

    @Test
    void moldormRoomKillAllEventOpensBothBossRoomShutters() {
        RoomSession session = newSession();
        session.rooms().loadIndoor(0x00, 0x06);
        assertEquals(0x03, session.rooms().activeRoomOrNull().shutterDoorMask());
        assertEquals(0x21, RoomDebugFixture.of(session).activeRoomEventForTest());

        List<RoomEntity> emptySlots = new ArrayList<>();
        for (int slot = 0; slot < EntityRoomLoader.MAX_ENTITIES; slot++) {
            emptySlots.add(RoomEntity.disabled(slot));
        }
        RoomDebugFixture.of(session)
                .replaceEntityRuntimeForTest(new RoomEntityRuntimeBuilder(new RoomEntitySnapshot(emptySlots))
                        .indoorRoom(true)
                        .build());

        session.entities().tickEntities(0, 0x50, 0x50);

        assertEquals(0, session.rooms().activeRoomOrNull().shutterDoorMask());
        assertEquals(0, RoomDebugFixture.of(session).activeRoomEventForTest());
    }

    @Test
    void shuttersWaitForLinkToEnterThenCloseEachDoorOverEightFrames() {
        RoomSession session = newSession();
        List<GameplaySoundEvent> sounds = new ArrayList<>();
        session.entityState().setColorShellSoundSink(sounds::add);
        session.rooms().loadIndoor(0, 6);
        int north = RoomConstants.ROOM_OBJECTS_BASE + 0x04;
        int south = RoomConstants.ROOM_OBJECTS_BASE + 0x74;
        int openNorth = session.rooms().activeRoomOrNull().roomObjectsArea()[north];
        int openSouth = session.rooms().activeRoomOrNull().roomObjectsArea()[south];
        int decoy = RoomConstants.ROOM_OBJECTS_BASE + 0x24;
        int[] objects = session.rooms().activeRoomOrNull().roomObjectsArea();
        int openSouthRight = objects[south + 1];
        objects[decoy] = openSouth;
        objects[decoy + 1] = openSouthRight;
        for (int frame = 0; frame < 20; frame++) session.entities().tickEntities(frame, 80, 0x15);
        assertEquals(openNorth, session.rooms().activeRoomOrNull().roomObjectsArea()[north]);
        assertTrue(sounds.isEmpty());
        session.entities().tickEntities(20, 80, 0x16); // Select north shutter at the entry boundary.
        assertEquals(List.of(GameplaySoundEvent.DOOR_CLOSED), sounds);
        for (int frame = 21; frame < 28; frame++) {
            session.entities().tickEntities(frame, 80, 64);
            assertTrue(RoomSessionInspection.component(session, RoomEntityBridge.class)
                    .consumeWorldLinkMotionBlockRequest());
            assertEquals(openNorth, session.rooms().activeRoomOrNull().roomObjectsArea()[north]);
        }
        session.entities().tickEntities(28, 80, 64);
        assertEquals(0x35, session.rooms().activeRoomOrNull().roomObjectsArea()[north]);
        assertEquals(openSouth, session.rooms().activeRoomOrNull().roomObjectsArea()[south]);
        assertEquals(0, RoomDebugFixture.of(session).indoorRoomStatusForTest(0, 6) & 4);
        session.entities().tickEntities(29, 80, 64); // Select south shutter on a separate frame.
        for (int frame = 30; frame < 37; frame++) {
            session.entities().tickEntities(frame, 80, 64);
            assertEquals(openSouth, session.rooms().activeRoomOrNull().roomObjectsArea()[south]);
        }
        session.entities().tickEntities(37, 80, 64);
        assertEquals(0x37, session.rooms().activeRoomOrNull().roomObjectsArea()[south]);
        assertEquals(openSouth, objects[decoy]);
        assertEquals(openSouthRight, objects[decoy + 1]);
        assertEquals(0, RoomDebugFixture.of(session).indoorRoomStatusForTest(0, 6) & 8);
        assertEquals(List.of(GameplaySoundEvent.DOOR_CLOSED), sounds);
    }

    @Test
    void minimapExitBitsDoNotBypassAnUnsolvedShutterPuzzle() {
        RoomSession session = newSession();
        byte[] indoorA = new byte[0x100];
        indoorA[0x06] = 0x04;
        session.story().restoreRoomStatuses(new byte[0x100], indoorA, new byte[0x100], new byte[0x20]);

        session.rooms().loadIndoor(0x00, 0x06);
        for (int frame = 0; frame < 18; frame++) session.entities().tickEntities(frame, 80, 64);

        assertEquals(
                0x35, session.rooms().activeRoomOrNull().roomObjectsArea()[RoomConstants.ROOM_OBJECTS_BASE + 0x04]);
        assertEquals(
                0x36, session.rooms().activeRoomOrNull().roomObjectsArea()[RoomConstants.ROOM_OBJECTS_BASE + 0x05]);
        assertEquals(
                0x37, session.rooms().activeRoomOrNull().roomObjectsArea()[RoomConstants.ROOM_OBJECTS_BASE + 0x74]);
        assertEquals(
                0x38, session.rooms().activeRoomOrNull().roomObjectsArea()[RoomConstants.ROOM_OBJECTS_BASE + 0x75]);
        assertEquals(0x03, session.rooms().activeRoomOrNull().shutterDoorMask());
    }

    @Test
    void verticalKeyDoorRequiresTheRomEightPixelAlignmentWindow() {
        for (int romY : new int[] {67, 68, 75, 76}) {
            RoomSession session = newSession();
            byte[] flags = new byte[DungeonItemState.DUNGEON_ITEM_FLAGS_SIZE];
            flags[DungeonItemState.SMALL_KEYS_INDEX] = 1;
            session.dungeon().restoreDungeonItemFlags(flags, new byte[DungeonItemState.COLOR_DUNGEON_ITEM_FLAGS_SIZE]);
            session.rooms().loadIndoor(0, 0x0F);
            boolean aligned = romY >= 68 && romY <= 75;
            assertEquals(
                    aligned,
                    session.doors()
                            .tryUnlockIndoorKeyDoor(
                                    0x85, romY - 16, Direction.fromJavaOrUnknown(Direction.RIGHT.javaIndex()), 8),
                    "ROM Y=" + romY);
            assertEquals(
                    aligned ? 0 : 1,
                    session.dungeon().currentDungeonItemFlagsSnapshot()[DungeonItemState.SMALL_KEYS_INDEX]);
            assertEquals(aligned ? 0x40 : 0, RoomDebugFixture.of(session).indoorRoomStatusForTest(0, 0x0F) & 0x40);
        }
    }

    @Test
    void bossDoorRequiresAlignmentBeforeRequestingItsMissingKeyDialogue() {
        for (int romX : new int[] {75, 76, 83, 84}) {
            RoomSession session = newSession();
            session.rooms().loadIndoor(0, 0x0B);
            boolean aligned = romX >= 76 && romX <= 83;
            assertEquals(
                    aligned,
                    session.doors()
                            .tryUnlockIndoorKeyDoor(
                                    romX - 8, 0, Direction.fromJavaOrUnknown(Direction.UP.javaIndex()), 1),
                    "ROM X=" + romX);
            assertEquals(
                    aligned ? 1 : 0,
                    session.entityState().consumeEntityDialogRequests().size());
        }
    }

    @Test
    void tailCaveFirstKeyDoorConsumesTheKeyAndSynchronizesBothRooms() {
        RoomSession session = newSession();
        List<GameplaySoundEvent> sounds = new ArrayList<>();
        session.entityState().setColorShellSoundSink(sounds::add);
        byte[] dungeonFlags = new byte[DungeonItemState.DUNGEON_ITEM_FLAGS_SIZE];
        dungeonFlags[DungeonItemState.SMALL_KEYS_INDEX] = 1;
        session.dungeon()
                .restoreDungeonItemFlags(dungeonFlags, new byte[DungeonItemState.COLOR_DUNGEON_ITEM_FLAGS_SIZE]);
        session.rooms().loadIndoor(0x00, 0x0F);
        int upperDoorIndex = RoomConstants.ROOM_OBJECTS_BASE + 0x39;
        int lowerDoorIndex = RoomConstants.ROOM_OBJECTS_BASE + 0x49;

        assertEquals(0x33, session.rooms().activeRoomOrNull().roomObjectsArea()[upperDoorIndex]);
        assertEquals(0x34, session.rooms().activeRoomOrNull().roomObjectsArea()[lowerDoorIndex]);
        assertTrue(session.doors()
                .tryUnlockIndoorKeyDoor(0x85, 0x38, Direction.fromJavaOrUnknown(Direction.RIGHT.javaIndex()), 0x08));
        assertEquals(0, session.dungeon().currentDungeonItemFlagsSnapshot()[DungeonItemState.SMALL_KEYS_INDEX]);
        assertEquals(0x40, RoomDebugFixture.of(session).indoorRoomStatusForTest(0x00, 0x0F) & 0x40);
        assertEquals(List.of(GameplaySoundEvent.DOOR_UNLOCKED), sounds);

        for (int frame = 0; frame < 8; frame++) {
            session.entities().tickEntities(frame, 0x85, 0x38);
            assertTrue(RoomSessionInspection.component(session, RoomEntityBridge.class)
                    .consumeWorldLinkMotionBlockRequest());
        }

        assertEquals(0x0B, session.rooms().activeRoomOrNull().roomObjectsArea()[upperDoorIndex]);
        assertEquals(0x0C, session.rooms().activeRoomOrNull().roomObjectsArea()[lowerDoorIndex]);
        assertEquals(0x01, RoomDebugFixture.of(session).indoorRoomStatusForTest(0x00, 0x0F) & 0x01);
        assertEquals(0x02, RoomDebugFixture.of(session).indoorRoomStatusForTest(0x00, 0x10) & 0x02);

        session.rooms().loadIndoor(0x00, 0x0F);
        assertEquals(0x0B, session.rooms().activeRoomOrNull().roomObjectsArea()[upperDoorIndex]);
        assertEquals(0x0C, session.rooms().activeRoomOrNull().roomObjectsArea()[lowerDoorIndex]);
    }

    @Test
    void tailCaveBossDoorRequiresTheNightmareKeyAndPersistsOpen() {
        RoomSession session = newSession();
        List<GameplaySoundEvent> sounds = new ArrayList<>();
        session.entityState().setColorShellSoundSink(sounds::add);
        session.rooms().loadIndoor(0x00, 0x0B);
        int leftDoorIndex = RoomConstants.ROOM_OBJECTS_BASE + 0x04;
        int rightDoorIndex = RoomConstants.ROOM_OBJECTS_BASE + 0x05;

        assertEquals(0xA4, session.rooms().activeRoomOrNull().roomObjectsArea()[leftDoorIndex]);
        assertEquals(0xA5, session.rooms().activeRoomOrNull().roomObjectsArea()[rightDoorIndex]);
        assertTrue(session.doors()
                .tryUnlockIndoorKeyDoor(0x48, 0x00, Direction.fromJavaOrUnknown(Direction.UP.javaIndex()), 0x01));
        assertEquals(
                0x07,
                session.entityState().consumeEntityDialogRequests().getFirst().dialogLowId());
        assertEquals(0xA4, session.rooms().activeRoomOrNull().roomObjectsArea()[leftDoorIndex]);

        byte[] dungeonFlags = new byte[DungeonItemState.DUNGEON_ITEM_FLAGS_SIZE];
        dungeonFlags[DungeonItemState.NIGHTMARE_KEY_INDEX] = 1;
        session.dungeon()
                .restoreDungeonItemFlags(dungeonFlags, new byte[DungeonItemState.COLOR_DUNGEON_ITEM_FLAGS_SIZE]);
        session.rooms().loadIndoor(0x00, 0x0B);

        assertTrue(session.doors()
                .tryUnlockIndoorKeyDoor(0x48, 0x00, Direction.fromJavaOrUnknown(Direction.UP.javaIndex()), 0x01));
        assertEquals(List.of(GameplaySoundEvent.DOOR_UNLOCKED), sounds);
        for (int frame = 0; frame < 8; frame++) {
            session.entities().tickEntities(frame, 0x48, 0x00);
        }

        assertEquals(0x43, session.rooms().activeRoomOrNull().roomObjectsArea()[leftDoorIndex]);
        assertEquals(0x44, session.rooms().activeRoomOrNull().roomObjectsArea()[rightDoorIndex]);
        assertEquals(1, session.dungeon().currentDungeonItemFlagsSnapshot()[DungeonItemState.NIGHTMARE_KEY_INDEX]);
        assertEquals(0x04, RoomDebugFixture.of(session).indoorRoomStatusForTest(0x00, 0x0B) & 0x04);
        assertEquals(0x08, RoomDebugFixture.of(session).indoorRoomStatusForTest(0x00, 0x06) & 0x08);

        session.rooms().loadIndoor(0x00, 0x0B);
        assertEquals(0x43, session.rooms().activeRoomOrNull().roomObjectsArea()[leftDoorIndex]);
        assertEquals(0x44, session.rooms().activeRoomOrNull().roomObjectsArea()[rightDoorIndex]);
    }

    @Test
    void tailCaveEntryAppliesAndRetainsTheLiveEntrancePalette() {
        RoomSession session = newSession();
        session.rooms().loadInitialOverworld(0xD3);
        session.transitions()
                .loadWarpDestination(session.rooms().activeRoomOrNull().warps().stream()
                        .filter(warp -> warp.destRoom() == 0x17)
                        .findFirst()
                        .orElseThrow());
        byte[] rom = loadRom();
        int offset = RomBank.romOffset(0x21, 0x60B0);
        for (int room : new int[] {0x17, 0x16}) {
            if (room == 0x16)
                session.transitions().startAdjacentIndoorScroll(new ScrollController(), ScrollController.LEFT, 0, 64);
            assertEquals(room, session.rooms().currentRoomId());
            for (int c = 0; c < 4; c++)
                assertEquals(
                        RomBank.decodeRgb555((rom[offset + c * 2] & 0xFF) | ((rom[offset + c * 2 + 1] & 0xFF) << 8)),
                        session.rooms().activeRoomOrNull().palettes()[6][c]);
        }
    }

    @Test
    void tailCaveKeyholeFollowsTheRomLockedAndUnlockedPaths() {
        RoomSession session = newSession();
        List<GameplaySoundEvent> sounds = new ArrayList<>();
        session.entityState().setColorShellSoundSink(sounds::add);
        session.rooms().loadInitialOverworld(0xD3);
        int gateIndex = RoomConstants.ROOM_OBJECTS_BASE + 0x16;

        assertEquals(0xC2, session.rooms().activeRoomOrNull().roomObjectsArea()[gateIndex]);
        assertTrue(session.doors()
                .tryUnlockTailCaveKeyhole(
                        0x5A, 0x4A, Direction.fromJavaOrUnknown(Direction.UP.javaIndex()), 0x01, false));
        assertEquals(0, RoomDebugFixture.of(session).overworldRoomStatusForTest(0xD3) & 0x10);
        assertEquals(0xC2, session.rooms().activeRoomOrNull().roomObjectsArea()[gateIndex]);
        assertEquals(
                0x230,
                session.entityState().consumeEntityDialogRequests().getFirst().globalDialogId());

        assertTrue(session.doors()
                .tryUnlockTailCaveKeyhole(
                        0x5A, 0x4A, Direction.fromJavaOrUnknown(Direction.UP.javaIndex()), 0x01, true));
        assertEquals(
                0x00,
                RoomSessionInspection.component(session, RoomEntityBridge.class).consumePendingMusicTrack());
        assertEquals(0x10, RoomDebugFixture.of(session).overworldRoomStatusForTest(0xD3) & 0x10);
        assertEquals(0xC2, session.rooms().activeRoomOrNull().roomObjectsArea()[gateIndex]);
        assertEquals(
                0xDF,
                RoomSessionInspection.component(session, RoomDoorInteractions.class)
                        .tailCaveKeyholeCountdownForTest(
                                RoomSessionInspection.loadedRoom(session).contents()));

        assertEquals(0, session.doors().tickTailCaveKeyholeSequence());
        assertEquals(List.of(GameplaySoundEvent.DOOR_UNLOCKED), sounds);
        while (RoomSessionInspection.component(session, RoomDoorInteractions.class)
                        .tailCaveKeyholeCountdownForTest(
                                RoomSessionInspection.loadedRoom(session).contents())
                > 0x18) {
            session.doors().tickTailCaveKeyholeSequence();
        }
        int gateTileX = 0x06 * 2;
        int gateTileY = 0x01 * 2;
        int gateTopLeft = gateTileY * RoomConstants.ROOM_TILE_WIDTH + gateTileX;
        assertEquals(0x0C, session.rooms().activeRoomOrNull().tileIds()[gateTopLeft]);
        assertEquals(0x1F, session.rooms().activeRoomOrNull().tileIds()[gateTopLeft + RoomConstants.ROOM_TILE_WIDTH]);
        while (RoomSessionInspection.component(session, RoomDoorInteractions.class)
                        .tailCaveKeyholeCountdownForTest(
                                RoomSessionInspection.loadedRoom(session).contents())
                > 0x08) {
            session.doors().tickTailCaveKeyholeSequence();
        }
        assertEquals(0xE3, session.rooms().activeRoomOrNull().roomObjectsArea()[gateIndex]);
        assertEquals(0xE3, session.rooms().activeRoomOrNull().gbcOverlay()[0x10]);
        assertEquals(0xE3, session.rooms().activeRoomOrNull().renderValues()[gateIndex]);
        assertEquals(
                List.of(
                        GameplaySoundEvent.DOOR_UNLOCKED,
                        GameplaySoundEvent.OPEN_KEY_CAVERN,
                        GameplaySoundEvent.DUNGEON_OPENED),
                sounds);
        int[] openedGateTiles = {
            session.rooms().activeRoomOrNull().tileIds()[gateTopLeft],
            session.rooms().activeRoomOrNull().tileIds()[gateTopLeft + 1],
            session.rooms().activeRoomOrNull().tileIds()[gateTopLeft + RoomConstants.ROOM_TILE_WIDTH],
            session.rooms().activeRoomOrNull().tileIds()[gateTopLeft + RoomConstants.ROOM_TILE_WIDTH + 1]
        };

        assertArrayEquals(new int[] {0x7E, 0x7E, 0x1F, 0x1F}, openedGateTiles);
        session.rooms().loadInitialOverworld(0xD3);
        assertEquals(0xE3, session.rooms().activeRoomOrNull().roomObjectsArea()[gateIndex]);
        assertEquals(0xE3, session.rooms().activeRoomOrNull().gbcOverlay()[0x10]);
        assertEquals(0xE3, session.rooms().activeRoomOrNull().renderValues()[gateIndex]);
        assertArrayEquals(openedGateTiles, new int[] {
            session.rooms().activeRoomOrNull().tileIds()[gateTopLeft],
            session.rooms().activeRoomOrNull().tileIds()[gateTopLeft + 1],
            session.rooms().activeRoomOrNull().tileIds()[gateTopLeft + RoomConstants.ROOM_TILE_WIDTH],
            session.rooms().activeRoomOrNull().tileIds()[gateTopLeft + RoomConstants.ROOM_TILE_WIDTH + 1]
        });
        Warp tailCaveWarp = session.rooms().activeRoomOrNull().warps().stream()
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
                        0x5A, 0x4A, Direction.fromJavaOrUnknown(Direction.DOWN.javaIndex()), 0x01, true));
        assertFalse(session.doors()
                .tryUnlockTailCaveKeyhole(
                        0x5A, 0x4A, Direction.fromJavaOrUnknown(Direction.UP.javaIndex()), 0x00, true));
        assertFalse(session.doors()
                .tryUnlockTailCaveKeyhole(
                        0x20, 0x20, Direction.fromJavaOrUnknown(Direction.UP.javaIndex()), 0x01, true));
        assertEquals(0, RoomDebugFixture.of(session).overworldRoomStatusForTest(0xD3) & 0x10);
        assertTrue(session.entityState().consumeEntityDialogRequests().isEmpty());
    }

    @Test
    void tailCaveKeyholeAcceptsTheSecondRomUpwardCollisionProbe() {
        RoomSession session = newSession();
        session.rooms().loadInitialOverworld(0xD3);

        assertTrue(session.doors()
                .tryUnlockTailCaveKeyhole(
                        0x58, 0x4A, Direction.fromJavaOrUnknown(Direction.UP.javaIndex()), 0x01, false));
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
                        0x5A, 0x4A, Direction.fromJavaOrUnknown(Direction.UP.javaIndex()), 0x01, true));
        assertEquals(
                0x00,
                RoomSessionInspection.component(session, RoomEntityBridge.class).consumePendingMusicTrack());

        while (RoomSessionInspection.component(session, RoomDoorInteractions.class)
                        .tailCaveKeyholeCountdownForTest(
                                RoomSessionInspection.loadedRoom(session).contents())
                > 0x0A) {
            session.doors().tickTailCaveKeyholeSequence();
        }
        assertEquals(
                0x50,
                RoomSessionInspection.component(session, RoomDoorInteractions.class)
                        .nextWorldMusicTrackCountdownForTest(
                                RoomSessionInspection.loadedRoom(session).contents()));
        while (RoomSessionInspection.component(session, RoomDoorInteractions.class)
                        .tailCaveKeyholeCountdownForTest(
                                RoomSessionInspection.loadedRoom(session).contents())
                > 1) {
            session.doors().tickTailCaveKeyholeSequence();
        }
        assertTrue(session.doors().tailCaveKeyholeSequenceActive());
        session.doors().tickTailCaveKeyholeSequence();
        assertTrue(session.doors().tailCaveKeyholeSequenceActive());
        session.doors().tickTailCaveKeyholeSequence();
        assertFalse(session.doors().tailCaveKeyholeSequenceActive());

        int remainingMusicFrames = RoomSessionInspection.component(session, RoomDoorInteractions.class)
                .nextWorldMusicTrackCountdownForTest(
                        RoomSessionInspection.loadedRoom(session).contents());
        for (int frame = 1; frame < remainingMusicFrames; frame++) {
            session.doors().tickTailCaveKeyholeSequence();
            assertEquals(
                    -1,
                    RoomSessionInspection.component(session, RoomEntityBridge.class)
                            .consumePendingMusicTrack());
        }
        session.doors().tickTailCaveKeyholeSequence();
        assertEquals(
                0x05,
                RoomSessionInspection.component(session, RoomEntityBridge.class).consumePendingMusicTrack());
    }

    @Test
    void magicRodBurnsAnIndoorFrozenBlockIntoTheSideViewEmptyObject() {
        RoomSession session = newSession();
        session.rooms().loadIndoor(0x00, 0x00, Warp.CATEGORY_SIDESCROLL);

        int location = 0x22;
        int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + (location & 0xF0) + (location & 0x0F);
        session.rooms().activeRoomOrNull().roomObjectsArea()[areaIndex] = 0x8A;

        assertTrue(session.playerItems().fireMagicRodFireball(0x20, 0x28, 0, 0));
        session.entities().tickEntitiesWithProjectileEvents(0, 0x20, 0x28, 0, 0, 0, false);

        assertEquals(0x04, session.rooms().activeRoomOrNull().roomObjectsArea()[areaIndex]);
        assertTrue(RoomSessionInspection.component(session, RoomEntityBridge.class).consumeEntityEvents().stream()
                .anyMatch(event -> event.type() == 0x04 && event.soundId() == 0x13));
    }

    @Test
    void magicPowderIgnitesAnIndoorTorchThroughTheLiveRoomBoundary() {
        RoomSession session = newSession();
        session.rooms().loadIndoor(0x00, 0x00);

        int location = 0x22;
        int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + (location & 0xF0) + (location & 0x0F);
        session.rooms().activeRoomOrNull().roomObjectsArea()[areaIndex] = 0xAB;

        assertTrue(session.playerItems().sprinkleMagicPowder(0x13, 0x29, 0, 0));
        for (int frame = 0; frame < 16; frame++) {
            session.entities().tickEntitiesWithProjectileEvents(frame, 0x20, 0x20, 0, 0, 0, false);
        }

        assertEquals(0xAC, session.rooms().activeRoomOrNull().roomObjectsArea()[areaIndex]);
        assertEquals(1, RoomDebugFixture.of(session).roomTriggerCountForTest());
        assertTrue(RoomSessionInspection.component(session, RoomEntityBridge.class).consumeEntityEvents().stream()
                .anyMatch(event -> event.type() == 0x08
                        && event.soundChannel() == EntityCombatEvent.SoundChannel.NOISE
                        && event.soundId() == 0x12));
    }

    @Test
    void revolvingDoorTorchExpiryStillChangesTheObjectAndRoomTrigger() {
        RoomSession session = newSession();
        session.rooms().loadIndoor(0x00, 0x00);
        int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + 0x22;
        session.rooms().activeRoomOrNull().roomObjectsArea()[areaIndex] = 0xAB;
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
        assertEquals(0xAB, session.rooms().activeRoomOrNull().roomObjectsArea()[areaIndex]);
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
        session.rooms().activeRoomOrNull().roomObjectsArea()[areaIndex] = 0x04;

        ShovelStartResult start = session.playerItems().startShovel(0x14, 0x26, 0, false);

        assertTrue(start.started());
        assertTrue(start.poking());
        for (int timer = 1; timer <= 0x10; timer++) {
            session.playerItems().advanceShovel(0x14, 0x26, 0, timer);
        }
        assertEquals(0x04, session.rooms().activeRoomOrNull().roomObjectsArea()[areaIndex]);
    }

    @Test
    void bombExplosionOpensAnOutdoorCaveDoorWithTheRomGbcTilesAndPersistsIt() {
        RoomSession session = newSession();
        session.rooms().loadInitialOverworld(0x13);

        int location = 0x05;
        int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + (location & 0xF0) + (location & 0x0F);
        assertEquals(0xBA, session.rooms().activeRoomOrNull().roomObjectsArea()[areaIndex]);
        assertEquals(0xBA, session.rooms().activeRoomOrNull().renderValues()[areaIndex]);
        assertEquals(location, session.rooms().activeRoomOrNull().firstWarp().tileLocation());

        assertTrue(session.playerItems().placeBomb(0x50, 0x18, 0, 0));
        for (int frame = 0; frame <= 146; frame++) {
            session.entities().tickEntities(frame, 0, 0);
        }

        assertEquals(0xE1, session.rooms().activeRoomOrNull().roomObjectsArea()[areaIndex]);
        assertEquals(0xE1, session.rooms().activeRoomOrNull().renderValues()[areaIndex]);
        assertEquals(0x04, RoomDebugFixture.of(session).overworldRoomStatusForTest(0x13));
        assertEquals(location, session.rooms().activeRoomOrNull().firstWarp().tileLocation());

        int tileIndex = 0x0A;
        int[] tileIds = session.rooms().activeRoomOrNull().tileIds();
        assertEquals(0x64, tileIds[tileIndex]);
        assertEquals(0x66, tileIds[tileIndex + 1]);
        assertEquals(0x64, tileIds[tileIndex + RoomConstants.ROOM_TILE_WIDTH]);
        assertEquals(0x66, tileIds[tileIndex + RoomConstants.ROOM_TILE_WIDTH + 1]);

        List<EntityCombatEvent> events =
                RoomSessionInspection.component(session, RoomEntityBridge.class).consumeEntityEvents();
        assertEquals(1, events.size());
        assertEquals(EntityCombatEvent.SoundChannel.JINGLE, events.getFirst().soundChannel());
        assertEquals(0x02, events.getFirst().soundId());

        session.rooms().loadOverworld(0x13);
        assertEquals(0xE1, session.rooms().activeRoomOrNull().roomObjectsArea()[areaIndex]);
        assertEquals(0xE1, session.rooms().activeRoomOrNull().renderValues()[areaIndex]);
        tileIds = session.rooms().activeRoomOrNull().tileIds();
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
        assertEquals(0x3F, session.rooms().activeRoomOrNull().roomObjectsArea()[areaIndex]);

        assertTrue(session.playerItems().placeBomb(0x30, 0x18, 0, 0));
        for (int frame = 0; frame <= 146; frame++) {
            session.entities().tickEntities(frame, 0, 0);
        }

        assertEquals(0x3D, session.rooms().activeRoomOrNull().roomObjectsArea()[areaIndex]);
        assertEquals(0x04, RoomDebugFixture.of(session).indoorRoomStatusForTest(0x06, 0x18) & 0x04);
        assertEquals(0x08, RoomDebugFixture.of(session).indoorRoomStatusForTest(0x06, 0x14));
        List<EntityCombatEvent> events =
                RoomSessionInspection.component(session, RoomEntityBridge.class).consumeEntityEvents();
        assertEquals(1, events.size());
        assertEquals(EntityCombatEvent.SoundChannel.JINGLE, events.getFirst().soundChannel());
        assertEquals(0x02, events.getFirst().soundId());

        int tileIndex = 0x04;
        int[] tileIds = session.rooms().activeRoomOrNull().tileIds();
        assertEquals(0x72, tileIds[tileIndex]);
        assertEquals(0x72, tileIds[tileIndex + 1]);
        assertEquals(0x73, tileIds[tileIndex + RoomConstants.ROOM_TILE_WIDTH]);
        assertEquals(0x73, tileIds[tileIndex + RoomConstants.ROOM_TILE_WIDTH + 1]);

        session.rooms().loadIndoor(0x06, 0x18);
        assertEquals(0x3D, session.rooms().activeRoomOrNull().roomObjectsArea()[areaIndex]);
        tileIds = session.rooms().activeRoomOrNull().tileIds();
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
        assertEquals(0x41, session.rooms().activeRoomOrNull().roomObjectsArea()[areaIndex]);

        assertTrue(session.playerItems().placeBomb(0x10, 0x48, 0, 0));
        for (int frame = 0; frame <= 146; frame++) {
            session.entities().tickEntities(frame, 0, 0);
        }

        assertEquals(0x3E, session.rooms().activeRoomOrNull().roomObjectsArea()[areaIndex]);
        assertEquals(0x02, RoomDebugFixture.of(session).indoorRoomStatusForTest(0x00, 0x0D) & 0x02);
        assertEquals(0x01, RoomDebugFixture.of(session).indoorRoomStatusForTest(0x00, 0x0C));

        int tileIndex = 3 * 2 * RoomConstants.ROOM_TILE_WIDTH;
        int[] tileIds = session.rooms().activeRoomOrNull().tileIds();
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
        assertEquals(0xA9, session.rooms().activeRoomOrNull().roomObjectsArea()[areaIndex]);

        assertTrue(session.playerItems().placeBomb(0x30, 0x34, 0, 0));
        for (int frame = 0; frame <= 146; frame++) {
            session.entities().tickEntities(frame, 0, 0);
        }

        assertEquals(0x0D, session.rooms().activeRoomOrNull().roomObjectsArea()[areaIndex]);
        assertEquals(0x40, RoomDebugFixture.of(session).indoorRoomStatusForTest(0x0A, 0x45) & 0x40);

        int tileIndex = 4 * RoomConstants.ROOM_TILE_WIDTH + 4;
        int[] tileIds = session.rooms().activeRoomOrNull().tileIds();
        assertEquals(0x10, tileIds[tileIndex]);
        assertEquals(0x12, tileIds[tileIndex + 1]);
        assertEquals(0x11, tileIds[tileIndex + RoomConstants.ROOM_TILE_WIDTH]);
        assertEquals(0x13, tileIds[tileIndex + RoomConstants.ROOM_TILE_WIDTH + 1]);

        RoomEntity smash = session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x05 && entity.x() == 0x28 && entity.y() == 0x30)
                .findFirst()
                .orElseThrow();
        assertTrue(smash.spriteVariant() >= 0x02 && smash.spriteVariant() <= 0x05);
        assertEquals(
                EntitySpriteDefinition.Shape.DYNAMIC, smash.spriteDefinition().shape());

        session.rooms().loadIndoor(0x0A, 0x45);
        assertEquals(0x0D, session.rooms().activeRoomOrNull().roomObjectsArea()[areaIndex]);
        tileIds = session.rooms().activeRoomOrNull().tileIds();
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
        assertEquals(0x9D, session.rooms().activeRoomOrNull().roomObjectsArea()[objectIndex]);
        RoomEntity bridge = session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == HookshotBridgeMotion.ENTITY_TYPE)
                .findFirst()
                .orElseThrow();
        assertEquals(0x40, bridge.y());

        int tileIndex = (0x30 >>> 3) * RoomConstants.ROOM_TILE_WIDTH + (0x30 >>> 3);
        int[] tileIds = session.rooms().activeRoomOrNull().tileIds();
        assertEquals(0x04, tileIds[tileIndex]);
        assertEquals(0x05, tileIds[tileIndex + 1]);
        assertEquals(0x08, tileIds[tileIndex + RoomConstants.ROOM_TILE_WIDTH]);
        assertEquals(0x09, tileIds[tileIndex + RoomConstants.ROOM_TILE_WIDTH + 1]);

        session.entities().tickEntities(1, 0x40, 0x40, 0, 2);
        RoomEntity movedBridge = session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == HookshotBridgeMotion.ENTITY_TYPE)
                .findFirst()
                .orElseThrow();
        assertEquals(0x43, movedBridge.y());

        session.rooms().activeRoomOrNull().roomObjectsArea()[objectIndex] = 0x00;
        session.entities().tickEntities(2, 0x40, 0x40, 0, 2);
        assertFalse(session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                .anyMatch(entity -> entity.type() == HookshotBridgeMotion.ENTITY_TYPE));
        assertEquals(0x9D, session.rooms().activeRoomOrNull().roomObjectsArea()[objectIndex]);
        tileIds = session.rooms().activeRoomOrNull().tileIds();
        assertEquals(0x04, tileIds[tileIndex]);
        assertEquals(0x05, tileIds[tileIndex + 1]);
        assertEquals(0x04, tileIds[tileIndex + RoomConstants.ROOM_TILE_WIDTH]);
        assertEquals(0x05, tileIds[tileIndex + RoomConstants.ROOM_TILE_WIDTH + 1]);
    }

    @Test
    void liveOctorokUsesEntityPhysicsInsteadOfLinkBlockingPolicy() {
        RoomSession session = newSession();
        session.rooms().loadInitialOverworld(0x2F);
        RoomEntity octorok = session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x09)
                .findFirst()
                .orElseThrow();

        fillActiveObjects(session, 0x0E); // Overworld deep-water physics $07.
        EntityBackgroundCollisionResult water = RoomSessionInspection.component(session, RoomCollisionQueries.class)
                .entityBackgroundCollisionResultForTest(
                        RoomSessionInspection.loadedRoom(session),
                        octorok,
                        EntityBackgroundCollisionResult.RIGHT,
                        octorok.x(),
                        octorok.y());
        assertFalse(water.blocked());
        assertEquals(0x0E, water.objectId());
        assertEquals(PhysicsFlags.DEEP_WATER, water.physicsFlag());

        fillActiveObjects(session, 0x00); // Overworld solid physics $01.
        EntityBackgroundCollisionResult solid = RoomSessionInspection.component(session, RoomCollisionQueries.class)
                .entityBackgroundCollisionResultForTest(
                        RoomSessionInspection.loadedRoom(session),
                        octorok,
                        EntityBackgroundCollisionResult.RIGHT,
                        octorok.x(),
                        octorok.y());
        assertTrue(solid.blocked());
        assertEquals(0x00, solid.objectId());
        assertEquals(PhysicsFlags.SOLID, solid.physicsFlag());
    }

    @Test
    void liveEntityCollisionProbeUsesRomLedgeTimerCadenceForIndoorAndOverworldRooms() {
        RoomSession indoor = newSession();
        indoor.rooms().loadIndoor(0x00, 0x0F);
        RoomEntity indoorEntity = indoor.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x1E)
                .findFirst()
                .orElseThrow();
        fillActiveObjects(indoor, 0x23); // Indoors1 object $23 has physics $D0.
        RoomDebugFixture.of(indoor).setEntityThrownDirectionForTest(indoorEntity.slot(), 0x01);
        RoomDebugFixture.of(indoor).setEntityLedgeTimerForTest(indoorEntity.slot(), 0x02);

        EntityBackgroundCollisionResult indoorResult = RoomSessionInspection.component(
                        indoor, RoomCollisionQueries.class)
                .entityBackgroundCollisionResultForTest(
                        RoomSessionInspection.loadedRoom(indoor),
                        indoorEntity,
                        EntityBackgroundCollisionResult.RIGHT,
                        indoorEntity.x(),
                        indoorEntity.y(),
                        0x01);
        assertFalse(indoorResult.blocked());
        assertEquals(0x01, RoomDebugFixture.of(indoor).entityLedgeTimerForTest(indoorEntity.slot()));

        RoomSession outdoor = newSession();
        outdoor.rooms().loadInitialOverworld(0x2F);
        RoomEntity outdoorEntity = outdoor.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x09)
                .findFirst()
                .orElseThrow();
        fillActiveObjects(outdoor, 0xF3); // Overworld object $F3 has physics $D0.
        RoomDebugFixture.of(outdoor).setEntityThrownDirectionForTest(outdoorEntity.slot(), 0x01);
        RoomDebugFixture.of(outdoor).setEntityLedgeTimerForTest(outdoorEntity.slot(), 0x02);

        EntityBackgroundCollisionResult evenFrame = RoomSessionInspection.component(outdoor, RoomCollisionQueries.class)
                .entityBackgroundCollisionResultForTest(
                        RoomSessionInspection.loadedRoom(outdoor),
                        outdoorEntity,
                        EntityBackgroundCollisionResult.RIGHT,
                        outdoorEntity.x(),
                        outdoorEntity.y(),
                        0x02);
        assertFalse(evenFrame.blocked());
        assertEquals(0x02, RoomDebugFixture.of(outdoor).entityLedgeTimerForTest(outdoorEntity.slot()));

        EntityBackgroundCollisionResult oddFrame = RoomSessionInspection.component(outdoor, RoomCollisionQueries.class)
                .entityBackgroundCollisionResultForTest(
                        RoomSessionInspection.loadedRoom(outdoor),
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
        assertEquals(
                0,
                session.rooms().activeRoomOrNull().entities().loadedEntities().size());

        session.transitions().loadWarpDestination(new Warp(Warp.CATEGORY_INDOOR, 0, 0x01, 40, 16, 0));
        session.transitions().loadWarpDestination(new Warp(Warp.CATEGORY_SIDESCROLL, 0, 0x18, 40, 16, 0));
        session.rooms().loadIndoor(0, 0x19, Warp.CATEGORY_SIDESCROLL);
        assertEquals(
                2,
                session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
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
                session.rooms().activeRoomOrNull().roomObjectsArea()[RoomConstants.ROOM_OBJECTS_BASE + location] =
                        object;
                for (int frame = 0; frame < 7; frame++) {
                    assertTrue(session.doors()
                            .tryLiftObject(
                                    0x30,
                                    0x2B,
                                    Direction.fromJavaOrUnknown(Direction.UP.javaIndex()),
                                    true,
                                    8,
                                    false,
                                    level));
                    assertEquals(
                            object,
                            session.rooms()
                                    .activeRoomOrNull()
                                    .roomObjectsArea()[RoomConstants.ROOM_OBJECTS_BASE + location]);
                }
                assertTrue(session.doors()
                        .tryLiftObject(
                                0x30,
                                0x2B,
                                Direction.fromJavaOrUnknown(Direction.UP.javaIndex()),
                                true,
                                8,
                                false,
                                level));
                assertEquals(
                        4,
                        session.rooms()
                                .activeRoomOrNull()
                                .roomObjectsArea()[RoomConstants.ROOM_OBJECTS_BASE + location]);
                assertTrue(session.rooms().liftedEntityState().active());
                var lifted = session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
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
        assertEquals(0x20, session.rooms().activeRoomOrNull().roomObjectsArea()[potIndex]);

        for (int frame = 0; frame < 8; frame++) {
            assertFalse(session.doors()
                    .tryLiftIndoorObject(
                            0x30, 0x1B, Direction.fromJavaOrUnknown(Direction.UP.javaIndex()), false, 0x08));
        }
        assertEquals(0x20, session.rooms().activeRoomOrNull().roomObjectsArea()[potIndex]);

        for (int frame = 0; frame < 7; frame++) {
            assertTrue(session.doors()
                    .tryLiftIndoorObject(
                            0x30, 0x1B, Direction.fromJavaOrUnknown(Direction.UP.javaIndex()), true, 0x08));
        }
        assertTrue(session.doors()
                .tryLiftIndoorObject(0x30, 0x1B, Direction.fromJavaOrUnknown(Direction.UP.javaIndex()), true, 0x00));
        for (int frame = 0; frame < 7; frame++) {
            assertTrue(session.doors()
                    .tryLiftIndoorObject(
                            0x30, 0x1B, Direction.fromJavaOrUnknown(Direction.UP.javaIndex()), true, 0x08));
        }
        assertEquals(0x20, session.rooms().activeRoomOrNull().roomObjectsArea()[potIndex]);

        assertTrue(session.doors()
                .tryLiftIndoorObject(0x30, 0x1B, Direction.fromJavaOrUnknown(Direction.UP.javaIndex()), true, 0x08));
        assertEquals(0x0D, session.rooms().activeRoomOrNull().roomObjectsArea()[potIndex]);
        RoomEntity liftedPot = session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x05 && entity.status() == EntityStatus.LIFTED)
                .findFirst()
                .orElseThrow();
        assertEquals(0x38, liftedPot.x());
        assertEquals(0x2B, liftedPot.y());
        assertEquals(0, liftedPot.spriteVariant());
        assertEquals(liftedPot.slot(), session.rooms().liftedEntityState().slot());
        assertEquals(0xD2, RoomDebugFixture.of(session).entityPhysicsFlagsForTest(liftedPot.slot()));

        int secondPotIndex = RoomConstants.ROOM_OBJECTS_BASE + 0x24;
        assertEquals(0x20, session.rooms().activeRoomOrNull().roomObjectsArea()[secondPotIndex]);
        assertFalse(session.doors()
                .tryLiftIndoorObject(0x40, 0x1B, Direction.fromJavaOrUnknown(Direction.UP.javaIndex()), true, 0x08));
        assertEquals(0x20, session.rooms().activeRoomOrNull().roomObjectsArea()[secondPotIndex]);
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
                            0x30, 0x1B, Direction.fromJavaOrUnknown(Direction.UP.javaIndex()), true, 0x08, true));
            assertEquals(0x20, session.rooms().activeRoomOrNull().roomObjectsArea()[potIndex]);
        }
        assertTrue(session.doors()
                .tryLiftIndoorObject(
                        0x30, 0x1B, Direction.fromJavaOrUnknown(Direction.UP.javaIndex()), true, 0x08, true));
        assertEquals(0x0D, session.rooms().activeRoomOrNull().roomObjectsArea()[potIndex]);
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
        ActiveRoom room = session.rooms().activeRoomOrNull();
        int closedShutter = room.roomObjectsArea()[northDoorIndex];
        assertTrue(closedShutter >= 0x35 && closedShutter < 0x3D);

        // Lift the authored pot at location $52, then throw it north at the shutter.
        for (int frame = 0; frame < 8; frame++) {
            assertTrue(session.doors()
                    .tryLiftIndoorObject(
                            0x18, 0x4B, Direction.fromJavaOrUnknown(Direction.UP.javaIndex()), true, 0x08));
        }
        int frame = 8;
        while (session.rooms().liftedEntityState().carryState() != 0x01 && frame < 0x40) {
            session.entities().tickEntities(frame++, 0x18, 0x4B, 0, Direction.UP.javaIndex());
        }
        assertEquals(0x01, session.rooms().liftedEntityState().carryState());
        session.entities().tickEntities(frame++, 0x48, 0x28, 0, Direction.UP.javaIndex());
        assertTrue(session.playerItems().throwLiftedEntity(Direction.fromJava(Direction.UP.javaIndex())));
        while (RoomDebugFixture.of(session).activeRoomEventForTest() != 0 && frame < 0x100) {
            session.entities().tickEntities(frame++, 0x48, 0x28, 0, Direction.UP.javaIndex());
        }

        assertEquals(0, RoomDebugFixture.of(session).activeRoomEventForTest());
        session.entities().tickEntities(frame++, 0x48, 0x28, 0, Direction.UP.javaIndex());
        session.entities().tickEntities(frame++, 0x48, 0x28, 0, Direction.UP.javaIndex());
        assertNotEquals(0, room.shutterDoorMask() & 1);
        assertEquals(closedShutter, room.roomObjectsArea()[northDoorIndex]);
        int openingDeadline = frame + 16;
        while ((room.shutterDoorMask() & 1) != 0 && frame < openingDeadline) {
            session.entities().tickEntities(frame++, 0x48, 0x28, 0, Direction.UP.javaIndex());
        }
        assertEquals(0, room.shutterDoorMask() & 1);
        assertFalse(session.rooms().activeRoomOrNull().roomObjectsArea()[northDoorIndex] >= 0x35
                && session.rooms().activeRoomOrNull().roomObjectsArea()[northDoorIndex] < 0x3D);
        assertTrue(sounds.contains(GameplaySoundEvent.PUZZLE_SOLVED));
        assertTrue(sounds.contains(GameplaySoundEvent.DOOR_UNLOCKED));

        session.rooms().loadIndoor(0x02, 0x52);
        for (int tick = 0; tick < 9; tick++) session.entities().tickEntities(frame++, 80, 64);
        assertTrue(session.rooms().activeRoomOrNull().roomObjectsArea()[northDoorIndex] >= 0x35
                && session.rooms().activeRoomOrNull().roomObjectsArea()[northDoorIndex] < 0x3D);
    }

    @Test
    void crackedFloorCollapsesAfterFortyFramesAndResetsOnMovementOrBlockedInteraction() {
        RoomSession session = newSession();
        session.rooms().loadIndoor(0, 0x17);
        int index = RoomConstants.ROOM_OBJECTS_BASE + 0x33;
        var frame = new LinkGroundFrame(40, 40, false, false, false, false, 0, 0, 0);
        session.terrain().tickCrackedFloor(frame, false); // seed the previous ground probe
        session.rooms().activeRoomOrNull().roomObjectsArea()[index] = 0xDF;
        var sounds = new ArrayList<GameplaySoundEvent>();
        session.entityState().setColorShellSoundSink(sounds::add);
        for (int i = 0; i < 39; i++) assertFalse(session.terrain().tickCrackedFloor(frame, false));
        assertFalse(session.terrain()
                .tickCrackedFloor(new LinkGroundFrame(40, 40, false, true, false, false, 0, 0, 0), false));
        assertEquals(
                0,
                RoomSessionInspection.component(session, RoomTerrainState.class).crackedFloorCounter());
        for (int i = 0; i < 39; i++) assertFalse(session.terrain().tickCrackedFloor(frame, false));
        assertFalse(session.terrain()
                .tickCrackedFloor(new LinkGroundFrame(40, 40, true, false, false, false, 0, 0, 0), false));
        assertEquals(
                39,
                RoomSessionInspection.component(session, RoomTerrainState.class)
                        .crackedFloorCounter()); // airborne bypass retains timer
        assertTrue(sounds.isEmpty());
        assertEquals(0xDF, session.rooms().activeRoomOrNull().roomObjectsArea()[index]);
        assertTrue(session.terrain().tickCrackedFloor(frame, false));
        assertEquals(0xAE, session.rooms().activeRoomOrNull().roomObjectsArea()[index]);
        assertEquals(List.of(GameplaySoundEvent.CRACKED_FLOOR_COLLAPSE), sounds);
        // A pit does not reach Default, so its frame retains wC1C9.
        assertFalse(session.terrain().tickCrackedFloor(frame, false));
        assertEquals(
                40,
                RoomSessionInspection.component(session, RoomTerrainState.class).crackedFloorCounter());
        session.rooms().activeRoomOrNull().roomObjectsArea()[index] = 0x0D;
        session.terrain().tickCrackedFloor(frame, false);
        assertEquals(
                0,
                RoomSessionInspection.component(session, RoomTerrainState.class)
                        .crackedFloorCounter()); // non-$DF on Default resets
    }

    @Test
    void pushedBlockPlaysRumbleOnlyWhenMovementStarts() {
        RoomSession session = newSession();
        session.rooms().loadIndoor(0x00, 0x04);
        var sounds = new ArrayList<GameplaySoundEvent>();
        session.entityState().setColorShellSoundSink(sounds::add);
        for (int tick = 0; tick < 63; tick++) {
            session.doors()
                    .tryPushIndoorBlock(0x15, 0x28, Direction.fromJavaOrUnknown(Direction.RIGHT.javaIndex()), 0x08);
        }
        assertTrue(sounds.isEmpty());
        session.doors().tryPushIndoorBlock(0x15, 0x28, Direction.fromJavaOrUnknown(Direction.RIGHT.javaIndex()), 0x08);
        assertEquals(List.of(GameplaySoundEvent.BLOCK_PUSH), sounds);
        session.doors().tryPushIndoorBlock(0x15, 0x28, Direction.fromJavaOrUnknown(Direction.RIGHT.javaIndex()), 0x08);
        assertEquals(1, sounds.size());

        RoomSession blocked = newSession();
        blocked.rooms().loadIndoor(0x00, 0x04);
        blocked.entityState().setColorShellSoundSink(sounds::add);
        blocked.rooms().activeRoomOrNull().roomObjectsArea()[RoomConstants.ROOM_OBJECTS_BASE + 0x33] = 0xA6;
        for (int tick = 0; tick < 64; tick++) {
            blocked.doors()
                    .tryPushIndoorBlock(0x15, 0x28, Direction.fromJavaOrUnknown(Direction.RIGHT.javaIndex()), 0x08);
        }
        assertEquals(1, sounds.size());
    }

    @Test
    void staticSwordCollisionBreaksOnlyIndoorSwordCrystal() {
        RoomSession session = newSession();
        session.rooms().loadIndoor(0x0A, 0xB0);
        int index = RoomConstants.ROOM_OBJECTS_BASE + 0x33;
        for (int object : new int[] {0x4E, 0x4F, 0x88, 0xA7}) {
            session.rooms().activeRoomOrNull().roomObjectsArea()[index] = object;
            assertFalse(session.terrain().breakIndoorSwordCrystal(0x33));
            assertEquals(object, session.rooms().activeRoomOrNull().roomObjectsArea()[index]);
        }
        session.rooms().activeRoomOrNull().roomObjectsArea()[index] = 0xDD;
        assertTrue(session.terrain().breakIndoorSwordCrystal(0x33));
        assertEquals(0x0D, session.rooms().activeRoomOrNull().roomObjectsArea()[index]);
        assertFalse(session.terrain().breakIndoorSwordCrystal(0x33));
        session.entities().tickEntities(0, 8, 16);
        assertTrue(RoomSessionInspection.component(session, RoomEntityBridge.class).consumeEntityEvents().stream()
                .anyMatch(event ->
                        event.soundChannel() == EntityCombatEvent.SoundChannel.NOISE && event.soundId() == 0x09));
        assertTrue(session.rooms().activeRoomOrNull().entities().slots().stream()
                .anyMatch(entity ->
                        entity.loaded() && entity.type() == 0x05 && entity.x() == 0x38 && entity.y() == 0x40));
    }
}
