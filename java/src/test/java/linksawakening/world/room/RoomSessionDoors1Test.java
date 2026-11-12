package linksawakening.world.room;

import static linksawakening.world.room.RoomSessionTestSupport.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.entity.Link;
import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.rom.RomBank;
import linksawakening.world.entity.EntityRoomLoader;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityRuntime;
import linksawakening.world.entity.RoomEntitySnapshot;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.transition.ScrollController;
import org.junit.jupiter.api.Test;

@linksawakening.testing.RequiresRom
final class RoomSessionDoors1Test {

    @Test
    void owlStatuePushUsesVisualOffsetBeforeDialogGateButSkipsAirborneLink() {
        RoomSession session = newSession();
        session.loadIndoor(2, 0x54);
        session.tickEntities(0, 0x38, 0x53, 0, Link.DIRECTION_UP);
        session.setEntityDialogActive(true);
        session.tickEntitiesWithProjectileEvents(1, 0x38, 0x4A, 0, 0, Link.DIRECTION_UP, false);
        var pushes = session.components.entityBridge().outbox.consumeLinkFinalPositionRequests();
        assertEquals(1, pushes.size());
        assertTrue(pushes.getFirst().resetPegasusBoots());
        assertTrue(pushes.getFirst().resetHookshotChain());
        session.tickEntitiesWithProjectileEvents(2, 0x38, 0x4B, 0, 0, Link.DIRECTION_UP, false);
        assertTrue(session.components
                .entityBridge()
                .outbox
                .consumeLinkFinalPositionRequests()
                .isEmpty());
        session.tickEntitiesWithProjectileEvents(3, 0x38, 0x4A, 1, 0, Link.DIRECTION_UP, false);
        assertTrue(session.components
                .entityBridge()
                .outbox
                .consumeLinkFinalPositionRequests()
                .isEmpty());
        session.tickEntitiesWithProjectileEvents(4, 0x38, 0x4A, 0, 2, Link.DIRECTION_UP, false);
        assertTrue(session.components
                .entityBridge()
                .outbox
                .consumeLinkFinalPositionRequests()
                .isEmpty());
    }

    @Test
    void indoorStaircaseArmsAfterLinkLeavesAndThenReturnsWarpZero() {
        RoomSession session = newSession();
        byte[] indoorA = new byte[0x100];
        indoorA[0x03] = 0x10;
        session.restoreRoomStatuses(new byte[0x100], indoorA, new byte[0x100], new byte[0x20]);
        session.loadIndoor(0x00, 0x03);

        assertEquals(0x18, session.activeRoom().staircaseLocation());
        assertNull(session.transitions().pollStaircaseWarp(0x88, 0x20, 0, false));
        assertNull(session.transitions().pollStaircaseWarp(0x98, 0x20, 0, false));
        assertEquals(session.activeRoom().firstWarp(), session.transitions().pollStaircaseWarp(0x88, 0x20, 0, false));
        assertNull(session.transitions().pollStaircaseWarp(0x88, 0x20, 0, false));
    }

    @Test
    void activeIndoorStaircaseRejectsAirborneAndCarryingLink() {
        RoomSession session = newSession();
        byte[] indoorA = new byte[0x100];
        indoorA[0x03] = 0x10;
        session.restoreRoomStatuses(new byte[0x100], indoorA, new byte[0x100], new byte[0x20]);
        session.loadIndoor(0x00, 0x03);

        assertNull(session.transitions().pollStaircaseWarp(0x98, 0x20, 0, false));
        assertNull(session.transitions().pollStaircaseWarp(0x88, 0x20, 1, false));
        assertNull(session.transitions().pollStaircaseWarp(0x88, 0x20, 0, true));
        assertEquals(session.activeRoom().firstWarp(), session.transitions().pollStaircaseWarp(0x88, 0x20, 0, false));
    }

    @Test
    void savedIndoorRoomPositionIsRestoredIndependentlyOfTheRoomId() {
        RoomSession session = newSession();

        session.loadIndoorFromSavedPosition(0x10, 0xA3, 0x27);

        assertEquals(0xA3, session.currentRoomId());
        assertEquals(0x27, session.indoorRoomPositionForSave());
    }

    @Test
    void tailCaveSingleBlockRequiresSixtyFourUninterruptedPushTicks() {
        RoomSession session = newSession();
        session.loadIndoor(0x00, 0x04);
        int blockIndex = RoomConstants.ROOM_OBJECTS_BASE + 0x32;
        assertEquals(0xA7, session.activeRoom().roomObjectsArea()[blockIndex]);

        for (int tick = 0; tick < 32; tick++) {
            assertTrue(session.doors().tryPushIndoorBlock(0x15, 0x28, Link.DIRECTION_RIGHT, 0x08));
        }
        assertFalse(session.doors().tryPushIndoorBlock(0x15, 0x28, Link.DIRECTION_RIGHT, 0x00));
        for (int tick = 0; tick < 32; tick++) {
            assertTrue(session.doors().tryPushIndoorBlock(0x15, 0x28, Link.DIRECTION_RIGHT, 0x08));
        }
        assertEquals(0xA7, session.activeRoom().roomObjectsArea()[blockIndex]);

        for (int tick = 32; tick < 63; tick++) {
            assertTrue(session.doors().tryPushIndoorBlock(0x15, 0x28, Link.DIRECTION_RIGHT, 0x08));
        }
        assertEquals(0xA7, session.activeRoom().roomObjectsArea()[blockIndex]);

        assertTrue(session.doors().tryPushIndoorBlock(0x15, 0x28, Link.DIRECTION_RIGHT, 0x08));
        assertEquals(0x0D, session.activeRoom().roomObjectsArea()[blockIndex]);

        RoomSession liveDispatch = newSession();
        liveDispatch.loadIndoor(0x00, 0x04);
        for (int tick = 0; tick < 64; tick++) {
            assertTrue(liveDispatch.tryInteractWithIndoorBlock(0x15, 0x28, Link.DIRECTION_RIGHT, 0x08));
        }
        assertEquals(0x0D, liveDispatch.activeRoom().roomObjectsArea()[blockIndex]);
    }

    @Test
    void bottleGrottoTwoHorizontalBlocksRevealTheRoom2FStaircase() {
        RoomSession session = newSession();
        session.loadIndoor(0x01, 0x2F);

        int firstBlockIndex = RoomConstants.ROOM_OBJECTS_BASE + 0x33;
        int secondBlockIndex = RoomConstants.ROOM_OBJECTS_BASE + 0x36;
        int firstDestinationIndex = RoomConstants.ROOM_OBJECTS_BASE + 0x34;
        int secondDestinationIndex = RoomConstants.ROOM_OBJECTS_BASE + 0x35;
        int stairsIndex = RoomConstants.ROOM_OBJECTS_BASE + 0x18;
        assertEquals(0xA7, session.components.roomSessionViews().activeRoomEventForTest());
        assertEquals(0xA7, session.activeRoom().roomObjectsArea()[firstBlockIndex]);
        assertEquals(0xA7, session.activeRoom().roomObjectsArea()[secondBlockIndex]);
        // ConfigureRoomObjects omits concealed $BF until room status $10 is set;
        // the source room-template floor remains in the active grid initially.
        assertEquals(0x0D, session.activeRoom().roomObjectsArea()[stairsIndex]);

        for (int tick = 0; tick < 64; tick++) {
            assertTrue(session.doors().tryPushIndoorBlock(0x25, 0x28, Link.DIRECTION_RIGHT, 0x08));
        }
        assertEquals(0xA7, session.components.roomSessionViews().activeRoomEventForTest());
        for (int frame = 0; frame < 33; frame++) {
            session.tickEntities(frame, 0x25, 0x28);
        }
        assertEquals(0x0D, session.activeRoom().roomObjectsArea()[firstBlockIndex]);
        assertEquals(0xA6, session.activeRoom().roomObjectsArea()[firstDestinationIndex]);
        assertEquals(0xA7, session.components.roomSessionViews().activeRoomEventForTest());

        for (int tick = 0; tick < 64; tick++) {
            assertTrue(session.doors().tryPushIndoorBlock(0x5C, 0x28, Link.DIRECTION_LEFT, 0x04));
        }
        assertEquals(0xA7, session.components.roomSessionViews().activeRoomEventForTest());
        for (int frame = 33; frame < 66; frame++) {
            session.tickEntities(frame, 0x5C, 0x28);
        }
        assertEquals(0x0D, session.activeRoom().roomObjectsArea()[secondBlockIndex]);
        assertEquals(0xA6, session.activeRoom().roomObjectsArea()[secondDestinationIndex]);
        assertEquals(0xA7, session.components.roomSessionViews().activeRoomEventForTest());
        session.tickEntities(66, 0, 0);
        assertEquals(0, session.components.roomSessionViews().activeRoomEventForTest());

        for (int frame = 67; frame < 78; frame++) {
            session.tickEntities(frame, 0, 0);
        }
        assertEquals(0xBE, session.activeRoom().roomObjectsArea()[stairsIndex]);
        assertEquals(0x10, session.components.roomSessionViews().indoorRoomStatusForTest(0x01, 0x2F) & 0x10);
        session.loadIndoor(0x01, 0x2F);
        assertEquals(0xBF, session.activeRoom().roomObjectsArea()[stairsIndex]);
    }

    @Test
    void bottleGrottoRoom2fSettlementUsesFacingSideAndRejectsVerticalPushes() {
        RoomSession oppositeSide = newSession();
        oppositeSide.loadIndoor(0x01, 0x2F);
        for (int tick = 0; tick < 64; tick++) {
            assertTrue(oppositeSide.doors().tryPushIndoorBlock(0x25, 0x28, Link.DIRECTION_RIGHT, 0x08));
        }
        // Arrange an opposite-side settled neighbor while the real block is
        // in motion; the settlement branch must still inspect only its facing
        // side through the normal push/tick path.
        oppositeSide.activeRoom().roomObjectsArea()[RoomConstants.ROOM_OBJECTS_BASE + 0x33] = 0xA6;
        for (int frame = 0; frame < 33; frame++) {
            oppositeSide.tickEntities(frame, 0x25, 0x28);
        }
        assertEquals(0xA6, oppositeSide.activeRoom().roomObjectsArea()[RoomConstants.ROOM_OBJECTS_BASE + 0x34]);
        assertEquals(0xA7, oppositeSide.components.roomSessionViews().activeRoomEventForTest());
        oppositeSide.tickEntities(33, 0, 0);
        assertEquals(0xA7, oppositeSide.components.roomSessionViews().activeRoomEventForTest());

        RoomSession vertical = newSession();
        vertical.loadIndoor(0x01, 0x2F);
        for (int tick = 0; tick < 64; tick++) {
            assertTrue(vertical.doors().tryPushIndoorBlock(0x2A, 0x38, Link.DIRECTION_UP, 0x01));
        }
        for (int frame = 0; frame < 33; frame++) {
            vertical.tickEntities(frame, 0x2A, 0x38);
        }
        assertEquals(0xA6, vertical.activeRoom().roomObjectsArea()[RoomConstants.ROOM_OBJECTS_BASE + 0x23]);
        assertEquals(0xA7, vertical.components.roomSessionViews().activeRoomEventForTest());
        vertical.tickEntities(33, 0, 0);
        assertEquals(0xA7, vertical.components.roomSessionViews().activeRoomEventForTest());
    }

    @Test
    void tailCaveKeyholeBlockRequiresThirtyTwoContactTicksAndConsumesAKey() {
        RoomSession noKey = newSession();
        noKey.loadIndoor(0x00, 0x09);
        assertTrue(noKey.tryInteractWithIndoorBlock(0x20, 0x4A, Link.DIRECTION_UP, 0x01));
        assertEquals(0x8C, noKey.consumeEntityDialogRequests().getFirst().dialogLowId());

        RoomSession session = newSession();
        List<GameplaySoundEvent> sounds = new ArrayList<>();
        session.setColorShellSoundSink(sounds::add);
        byte[] dungeonFlags = new byte[DungeonItemState.DUNGEON_ITEM_FLAGS_SIZE];
        dungeonFlags[DungeonItemState.SMALL_KEYS_INDEX] = 1;
        session.restoreDungeonItemFlags(dungeonFlags, new byte[DungeonItemState.COLOR_DUNGEON_ITEM_FLAGS_SIZE]);
        session.loadIndoor(0x00, 0x09);
        int blockIndex = RoomConstants.ROOM_OBJECTS_BASE + 0x52;
        assertEquals(0xDE, session.activeRoom().roomObjectsArea()[blockIndex]);

        for (int tick = 0; tick < 31; tick++) {
            assertTrue(session.tryInteractWithIndoorBlock(0x20, 0x4A, Link.DIRECTION_UP, 0x01));
        }
        assertEquals(0xDE, session.activeRoom().roomObjectsArea()[blockIndex]);
        assertTrue(session.tryInteractWithIndoorBlock(0x20, 0x4A, Link.DIRECTION_UP, 0x01));

        assertEquals(0x0D, session.activeRoom().roomObjectsArea()[blockIndex]);
        assertEquals(0, session.currentDungeonItemFlagsSnapshot()[DungeonItemState.SMALL_KEYS_INDEX]);
        assertEquals(0x40, session.components.roomSessionViews().indoorRoomStatusForTest(0x00, 0x09) & 0x40);
        assertEquals(List.of(GameplaySoundEvent.DOOR_UNLOCKED), sounds);
        session.loadIndoor(0x00, 0x09);
        assertEquals(0x0D, session.activeRoom().roomObjectsArea()[blockIndex]);
    }

    @Test
    void movingBlockRestoresContactingLinkEvenInAirButNotDuringDialogue() {
        RoomSession session = newSession();
        session.loadIndoor(0, 4);
        for (int tick = 0; tick < 64; tick++) {
            session.doors().tryPushIndoorBlock(0x15, 0x28, Link.DIRECTION_RIGHT, 0x08);
        }
        session.tickEntitiesWithProjectileEvents(0, 0x1B, 0x40, 12, 0, Link.DIRECTION_RIGHT, false);
        var requests = session.components.entityBridge().outbox.consumeLinkFinalPositionRequests();
        assertEquals(1, requests.size());
        assertTrue(requests.getFirst().markLinkPushing());
        assertFalse(requests.getFirst().clearLinkPositionIncrement());
        assertFalse(requests.getFirst().resetPegasusBoots());
        session.setEntityDialogActive(true);
        session.tickEntitiesWithProjectileEvents(1, 0x28, 0x40, 0, 0, Link.DIRECTION_RIGHT, false);
        assertTrue(session.components
                .entityBridge()
                .outbox
                .consumeLinkFinalPositionRequests()
                .isEmpty());
        session.setEntityDialogActive(false);
        session.tickEntitiesWithProjectileEvents(2, 0x80, 0x70, 0, 0, Link.DIRECTION_RIGHT, false);
        assertTrue(session.components
                .entityBridge()
                .outbox
                .consumeLinkFinalPositionRequests()
                .isEmpty());
    }

    @Test
    void settledTailCaveSingleBlockOpensDoorUntilTheRoomReloads() {
        RoomSession session = newSession();
        List<GameplaySoundEvent> sounds = new ArrayList<>();
        session.setColorShellSoundSink(sounds::add);
        session.loadIndoor(0x00, 0x04);
        int destinationIndex = RoomConstants.ROOM_OBJECTS_BASE + 0x33;
        int initialDestination = session.activeRoom().roomObjectsArea()[destinationIndex];
        assertEquals(0x22, session.components.roomSessionViews().activeRoomEventForTest());
        assertNotEquals(0, session.activeRoom().shutterDoorMask());

        for (int tick = 0; tick < 64; tick++) {
            assertTrue(session.doors().tryPushIndoorBlock(0x15, 0x28, Link.DIRECTION_RIGHT, 0x08));
        }
        session.setEntityDialogActive(true);
        for (int frame = 0; frame < 8; frame++) {
            session.tickEntities(frame, 0x15, 0x28);
        }
        RoomEntity pausedBlock = session.activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x06)
                .findFirst()
                .orElseThrow();
        assertEquals(0x28, pausedBlock.x());
        session.setEntityDialogActive(false);
        for (int frame = 0; frame < 32; frame++) {
            session.tickEntities(frame, 0x15, 0x28);
        }
        RoomEntity movingBlock = session.activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x06)
                .findFirst()
                .orElseThrow();
        assertTrue(movingBlock.spriteDefinition().supported());
        assertEquals(0x38, movingBlock.x());
        assertEquals(0x40, movingBlock.y());
        assertEquals(initialDestination, session.activeRoom().roomObjectsArea()[destinationIndex]);
        assertNotEquals(0, session.activeRoom().shutterDoorMask());

        session.tickEntities(32, 0x15, 0x28);

        assertEquals(0xA6, session.activeRoom().roomObjectsArea()[destinationIndex]);
        assertEquals(0, session.activeRoom().shutterDoorMask());
        assertEquals(0, session.components.roomSessionViews().activeRoomEventForTest());
        assertEquals(0, session.components.roomSessionViews().indoorRoomStatusForTest(0x00, 0x04) & 0x10);
        assertEquals(0x02, session.components.roomSessionViews().indoorRoomStatusForTest(0x00, 0x04) & 0x02);
        assertEquals(0x01, session.components.roomSessionViews().indoorRoomStatusForTest(0x00, 0x03) & 0x01);
        assertEquals(
                List.of(
                        GameplaySoundEvent.BLOCK_PUSH,
                        GameplaySoundEvent.DOOR_CLOSED,
                        GameplaySoundEvent.PUZZLE_SOLVED,
                        GameplaySoundEvent.DOOR_UNLOCKED),
                sounds);

        session.loadIndoor(0x00, 0x04);
        assertNotEquals(0, session.activeRoom().shutterDoorMask());
        assertEquals(0x22, session.components.roomSessionViews().activeRoomEventForTest());
    }

    @Test
    void blockedOrUnallocatedPushedBlockLeavesItsSourceObjectIntact() {
        RoomSession blocked = newSession();
        blocked.loadIndoor(0x00, 0x04);
        int sourceIndex = RoomConstants.ROOM_OBJECTS_BASE + 0x32;
        int destinationIndex = RoomConstants.ROOM_OBJECTS_BASE + 0x33;
        blocked.activeRoom().roomObjectsArea()[destinationIndex] = 0xA6;
        for (int tick = 0; tick < 64; tick++) {
            assertTrue(blocked.doors().tryPushIndoorBlock(0x15, 0x28, Link.DIRECTION_RIGHT, 0x08));
        }
        assertEquals(0xA7, blocked.activeRoom().roomObjectsArea()[sourceIndex]);
        assertFalse(
                blocked.activeRoom().entities().loadedEntities().stream().anyMatch(entity -> entity.type() == 0x06));

        RoomSession full = newSession();
        full.loadIndoor(0x00, 0x04);
        List<RoomEntity> occupiedSlots = new ArrayList<>();
        for (int slot = 0; slot < EntityRoomLoader.MAX_ENTITIES; slot++) {
            occupiedSlots.add(new RoomEntity(
                    slot,
                    -1,
                    0x42,
                    0x08,
                    0x10,
                    EntityStatus.ACTIVE,
                    EntitySpriteDefinition.unsupported(0x42),
                    -1,
                    0,
                    0,
                    0,
                    -1,
                    false));
        }
        full.components
                .roomSessionViews()
                .replaceEntityRuntimeForTest(RoomEntityRuntime.from(new RoomEntitySnapshot(occupiedSlots), true));
        for (int tick = 0; tick < 64; tick++) {
            assertTrue(full.doors().tryPushIndoorBlock(0x15, 0x28, Link.DIRECTION_RIGHT, 0x08));
        }
        assertEquals(0xA7, full.activeRoom().roomObjectsArea()[sourceIndex]);
    }

    @Test
    void tailCaveCardPuzzleRevealsAndAwardsTheStoneBeakThroughCombat() {
        RoomSession session = newSession();
        session.loadIndoor(0x00, 0x0A);
        session.tickEntitiesWithProjectileEvents(1, 0, 0, 0, 0, Link.DIRECTION_DOWN, false);
        List<RoomEntity> cards = session.activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == EntitySpriteHandlerCatalog.ENTITY_THREE_OF_A_KIND)
                .toList();
        assertEquals(3, cards.size());
        assertEquals(0x61, session.components.roomSessionViews().activeRoomEventForTest());

        // Resolve each sword collision on its source alternating-slot cadence
        // without advancing the shared card face between hits.
        for (RoomEntity card : cards) {
            List<EntityCombatEvent> hit = session.resolveEntityCombat(new EntityCombatInputs(
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
        while (session.components.roomSessionViews().activeRoomEventForTest() != 0 && frame < deadline) {
            session.tickEntitiesWithProjectileEvents(frame++, 0, 0, 0, 0, Link.DIRECTION_DOWN, false);
        }

        assertEquals(
                0,
                session.components.roomSessionViews().activeRoomEventForTest(),
                session.activeRoom().entities().loadedEntities().toString());
        int chestIndex = RoomConstants.ROOM_OBJECTS_BASE + 0x28;
        int appearanceDeadline = frame + 0x20;
        while (session.activeRoom().roomObjectsArea()[chestIndex] != 0xA0 && frame < appearanceDeadline) {
            session.tickEntitiesWithProjectileEvents(frame++, 0, 0, 0, 0, Link.DIRECTION_DOWN, false);
        }
        assertEquals(0xA0, session.activeRoom().roomObjectsArea()[chestIndex]);

        RoomSession.ChestOpenResult chest = session.tryOpenChest(0x78, 0x21, Link.DIRECTION_UP, true, 1);
        assertTrue(chest.opened());
        assertEquals(ChestContentsTable.CHEST_STONE_BEAK, chest.itemType());
        session.tickEntitiesWithProjectileEvents(frame, 0x78, 0x21, 0, 0, Link.DIRECTION_DOWN, false);
        assertEquals(1, session.currentDungeonItemFlagsSnapshot()[DungeonItemState.STONE_BEAK_INDEX]);
    }

    @Test
    void rollingBonesRoomOpensItsRomShuttersAndPersistsTheMinibossFlag() {
        RoomSession session = newSession();
        session.loadIndoor(0x00, 0x11);
        assertEquals(0x05, session.activeRoom().shutterDoorMask());
        assertEquals(0xC1, session.components.roomSessionViews().activeRoomEventForTest());

        List<RoomEntity> emptySlots = new ArrayList<>();
        for (int slot = 0; slot < EntityRoomLoader.MAX_ENTITIES; slot++) {
            emptySlots.add(RoomEntity.disabled(slot));
        }
        session.components
                .roomSessionViews()
                .replaceEntityRuntimeForTest(RoomEntityRuntime.from(new RoomEntitySnapshot(emptySlots), true));

        session.tickEntities(0, 0x50, 0x50);

        assertEquals(0, session.activeRoom().shutterDoorMask());
        assertEquals(0, session.components.roomSessionViews().activeRoomEventForTest());
        assertEquals(0x20, session.components.roomSessionViews().indoorRoomStatusForTest(0x00, 0x11) & 0x20);
        assertTrue(session.components.entityBridge().outbox.consumeEntityEvents().stream()
                .anyMatch(event ->
                        event.soundChannel() == EntityCombatEvent.SoundChannel.JINGLE && event.soundId() == 0x1B));

        session.loadIndoor(0x00, 0x11);
        assertEquals(0, session.activeRoom().shutterDoorMask());
    }

    @Test
    void tailCaveRoom16KillAllEventDropsTheThirdSmallKey() {
        RoomSession session = newSession();
        session.loadIndoor(0x00, 0x16);
        assertEquals(0x81, session.components.roomSessionViews().activeRoomEventForTest());

        List<RoomEntity> emptySlots = new ArrayList<>();
        for (int slot = 0; slot < EntityRoomLoader.MAX_ENTITIES; slot++) {
            emptySlots.add(RoomEntity.disabled(slot));
        }
        session.components
                .roomSessionViews()
                .replaceEntityRuntimeForTest(RoomEntityRuntime.from(new RoomEntitySnapshot(emptySlots), true));

        session.tickEntities(0, 0x50, 0x50);

        RoomEntity key = session.activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x30)
                .findFirst()
                .orElseThrow();
        assertEquals(0x28, key.x());
        assertEquals(0x3C, key.y());
        assertEquals(0x70, key.z());
        assertEquals(0, session.components.roomSessionViews().activeRoomEventForTest());
    }

    @Test
    void moldormRoomKillAllEventOpensBothBossRoomShutters() {
        RoomSession session = newSession();
        session.loadIndoor(0x00, 0x06);
        assertEquals(0x03, session.activeRoom().shutterDoorMask());
        assertEquals(0x21, session.components.roomSessionViews().activeRoomEventForTest());

        List<RoomEntity> emptySlots = new ArrayList<>();
        for (int slot = 0; slot < EntityRoomLoader.MAX_ENTITIES; slot++) {
            emptySlots.add(RoomEntity.disabled(slot));
        }
        session.components
                .roomSessionViews()
                .replaceEntityRuntimeForTest(RoomEntityRuntime.from(new RoomEntitySnapshot(emptySlots), true));

        session.tickEntities(0, 0x50, 0x50);

        assertEquals(0, session.activeRoom().shutterDoorMask());
        assertEquals(0, session.components.roomSessionViews().activeRoomEventForTest());
    }

    @Test
    void shuttersWaitForLinkToEnterThenCloseEachDoorOverEightFrames() {
        RoomSession session = newSession();
        List<GameplaySoundEvent> sounds = new ArrayList<>();
        session.setColorShellSoundSink(sounds::add);
        session.loadIndoor(0, 6);
        int north = RoomConstants.ROOM_OBJECTS_BASE + 0x04;
        int south = RoomConstants.ROOM_OBJECTS_BASE + 0x74;
        int openNorth = session.activeRoom().roomObjectsArea()[north];
        int openSouth = session.activeRoom().roomObjectsArea()[south];
        for (int frame = 0; frame < 20; frame++) session.tickEntities(frame, 80, 0x15);
        assertEquals(openNorth, session.activeRoom().roomObjectsArea()[north]);
        assertTrue(sounds.isEmpty());
        session.tickEntities(20, 80, 0x16); // Select north shutter at the entry boundary.
        assertEquals(List.of(GameplaySoundEvent.DOOR_CLOSED), sounds);
        for (int frame = 21; frame < 28; frame++) {
            session.tickEntities(frame, 80, 64);
            assertTrue(session.components.entityBridge().consumeWorldLinkMotionBlockRequest());
            assertEquals(openNorth, session.activeRoom().roomObjectsArea()[north]);
        }
        session.tickEntities(28, 80, 64);
        assertEquals(0x35, session.activeRoom().roomObjectsArea()[north]);
        assertEquals(openSouth, session.activeRoom().roomObjectsArea()[south]);
        assertEquals(0, session.components.roomSessionViews().indoorRoomStatusForTest(0, 6) & 4);
        session.tickEntities(29, 80, 64); // Select south shutter on a separate frame.
        for (int frame = 30; frame < 37; frame++) {
            session.tickEntities(frame, 80, 64);
            assertEquals(openSouth, session.activeRoom().roomObjectsArea()[south]);
        }
        session.tickEntities(37, 80, 64);
        assertEquals(0x37, session.activeRoom().roomObjectsArea()[south]);
        assertEquals(0, session.components.roomSessionViews().indoorRoomStatusForTest(0, 6) & 8);
        assertEquals(List.of(GameplaySoundEvent.DOOR_CLOSED), sounds);
    }

    @Test
    void minimapExitBitsDoNotBypassAnUnsolvedShutterPuzzle() {
        RoomSession session = newSession();
        byte[] indoorA = new byte[0x100];
        indoorA[0x06] = 0x04;
        session.restoreRoomStatuses(new byte[0x100], indoorA, new byte[0x100], new byte[0x20]);

        session.loadIndoor(0x00, 0x06);
        for (int frame = 0; frame < 18; frame++) session.tickEntities(frame, 80, 64);

        assertEquals(0x35, session.activeRoom().roomObjectsArea()[RoomConstants.ROOM_OBJECTS_BASE + 0x04]);
        assertEquals(0x36, session.activeRoom().roomObjectsArea()[RoomConstants.ROOM_OBJECTS_BASE + 0x05]);
        assertEquals(0x37, session.activeRoom().roomObjectsArea()[RoomConstants.ROOM_OBJECTS_BASE + 0x74]);
        assertEquals(0x38, session.activeRoom().roomObjectsArea()[RoomConstants.ROOM_OBJECTS_BASE + 0x75]);
        assertEquals(0x03, session.activeRoom().shutterDoorMask());
    }

    @Test
    void verticalKeyDoorRequiresTheRomEightPixelAlignmentWindow() {
        for (int romY : new int[] {67, 68, 75, 76}) {
            RoomSession session = newSession();
            byte[] flags = new byte[DungeonItemState.DUNGEON_ITEM_FLAGS_SIZE];
            flags[DungeonItemState.SMALL_KEYS_INDEX] = 1;
            session.restoreDungeonItemFlags(flags, new byte[DungeonItemState.COLOR_DUNGEON_ITEM_FLAGS_SIZE]);
            session.loadIndoor(0, 0x0F);
            boolean aligned = romY >= 68 && romY <= 75;
            assertEquals(
                    aligned, session.tryUnlockIndoorKeyDoor(0x85, romY - 16, Link.DIRECTION_RIGHT, 8), "ROM Y=" + romY);
            assertEquals(aligned ? 0 : 1, session.currentDungeonItemFlagsSnapshot()[DungeonItemState.SMALL_KEYS_INDEX]);
            assertEquals(
                    aligned ? 0x40 : 0, session.components.roomSessionViews().indoorRoomStatusForTest(0, 0x0F) & 0x40);
        }
    }

    @Test
    void bossDoorRequiresAlignmentBeforeRequestingItsMissingKeyDialogue() {
        for (int romX : new int[] {75, 76, 83, 84}) {
            RoomSession session = newSession();
            session.loadIndoor(0, 0x0B);
            boolean aligned = romX >= 76 && romX <= 83;
            assertEquals(aligned, session.tryUnlockIndoorKeyDoor(romX - 8, 0, Link.DIRECTION_UP, 1), "ROM X=" + romX);
            assertEquals(aligned ? 1 : 0, session.consumeEntityDialogRequests().size());
        }
    }

    @Test
    void tailCaveFirstKeyDoorConsumesTheKeyAndSynchronizesBothRooms() {
        RoomSession session = newSession();
        List<GameplaySoundEvent> sounds = new ArrayList<>();
        session.setColorShellSoundSink(sounds::add);
        byte[] dungeonFlags = new byte[DungeonItemState.DUNGEON_ITEM_FLAGS_SIZE];
        dungeonFlags[DungeonItemState.SMALL_KEYS_INDEX] = 1;
        session.restoreDungeonItemFlags(dungeonFlags, new byte[DungeonItemState.COLOR_DUNGEON_ITEM_FLAGS_SIZE]);
        session.loadIndoor(0x00, 0x0F);
        int upperDoorIndex = RoomConstants.ROOM_OBJECTS_BASE + 0x39;
        int lowerDoorIndex = RoomConstants.ROOM_OBJECTS_BASE + 0x49;

        assertEquals(0x33, session.activeRoom().roomObjectsArea()[upperDoorIndex]);
        assertEquals(0x34, session.activeRoom().roomObjectsArea()[lowerDoorIndex]);
        assertTrue(session.tryUnlockIndoorKeyDoor(0x85, 0x38, Link.DIRECTION_RIGHT, 0x08));
        assertEquals(0, session.currentDungeonItemFlagsSnapshot()[DungeonItemState.SMALL_KEYS_INDEX]);
        assertEquals(0x40, session.components.roomSessionViews().indoorRoomStatusForTest(0x00, 0x0F) & 0x40);
        assertEquals(List.of(GameplaySoundEvent.DOOR_UNLOCKED), sounds);

        for (int frame = 0; frame < 8; frame++) {
            session.tickEntities(frame, 0x85, 0x38);
            assertTrue(session.components.entityBridge().consumeWorldLinkMotionBlockRequest());
        }

        assertEquals(0x0B, session.activeRoom().roomObjectsArea()[upperDoorIndex]);
        assertEquals(0x0C, session.activeRoom().roomObjectsArea()[lowerDoorIndex]);
        assertEquals(0x01, session.components.roomSessionViews().indoorRoomStatusForTest(0x00, 0x0F) & 0x01);
        assertEquals(0x02, session.components.roomSessionViews().indoorRoomStatusForTest(0x00, 0x10) & 0x02);

        session.loadIndoor(0x00, 0x0F);
        assertEquals(0x0B, session.activeRoom().roomObjectsArea()[upperDoorIndex]);
        assertEquals(0x0C, session.activeRoom().roomObjectsArea()[lowerDoorIndex]);
    }

    @Test
    void tailCaveBossDoorRequiresTheNightmareKeyAndPersistsOpen() {
        RoomSession session = newSession();
        List<GameplaySoundEvent> sounds = new ArrayList<>();
        session.setColorShellSoundSink(sounds::add);
        session.loadIndoor(0x00, 0x0B);
        int leftDoorIndex = RoomConstants.ROOM_OBJECTS_BASE + 0x04;
        int rightDoorIndex = RoomConstants.ROOM_OBJECTS_BASE + 0x05;

        assertEquals(0xA4, session.activeRoom().roomObjectsArea()[leftDoorIndex]);
        assertEquals(0xA5, session.activeRoom().roomObjectsArea()[rightDoorIndex]);
        assertTrue(session.tryUnlockIndoorKeyDoor(0x48, 0x00, Link.DIRECTION_UP, 0x01));
        assertEquals(0x07, session.consumeEntityDialogRequests().getFirst().dialogLowId());
        assertEquals(0xA4, session.activeRoom().roomObjectsArea()[leftDoorIndex]);

        byte[] dungeonFlags = new byte[DungeonItemState.DUNGEON_ITEM_FLAGS_SIZE];
        dungeonFlags[DungeonItemState.NIGHTMARE_KEY_INDEX] = 1;
        session.restoreDungeonItemFlags(dungeonFlags, new byte[DungeonItemState.COLOR_DUNGEON_ITEM_FLAGS_SIZE]);
        session.loadIndoor(0x00, 0x0B);

        assertTrue(session.tryUnlockIndoorKeyDoor(0x48, 0x00, Link.DIRECTION_UP, 0x01));
        assertEquals(List.of(GameplaySoundEvent.DOOR_UNLOCKED), sounds);
        for (int frame = 0; frame < 8; frame++) {
            session.tickEntities(frame, 0x48, 0x00);
        }

        assertEquals(0x43, session.activeRoom().roomObjectsArea()[leftDoorIndex]);
        assertEquals(0x44, session.activeRoom().roomObjectsArea()[rightDoorIndex]);
        assertEquals(1, session.currentDungeonItemFlagsSnapshot()[DungeonItemState.NIGHTMARE_KEY_INDEX]);
        assertEquals(0x04, session.components.roomSessionViews().indoorRoomStatusForTest(0x00, 0x0B) & 0x04);
        assertEquals(0x08, session.components.roomSessionViews().indoorRoomStatusForTest(0x00, 0x06) & 0x08);

        session.loadIndoor(0x00, 0x0B);
        assertEquals(0x43, session.activeRoom().roomObjectsArea()[leftDoorIndex]);
        assertEquals(0x44, session.activeRoom().roomObjectsArea()[rightDoorIndex]);
    }

    @Test
    void tailCaveEntryAppliesAndRetainsTheLiveEntrancePalette() {
        RoomSession session = newSession();
        session.loadInitialOverworld(0xD3);
        session.loadWarpDestination(session.activeRoom().warps().stream()
                .filter(warp -> warp.destRoom() == 0x17)
                .findFirst()
                .orElseThrow());
        byte[] rom = loadRom();
        int offset = RomBank.romOffset(0x21, 0x60B0);
        for (int room : new int[] {0x17, 0x16}) {
            if (room == 0x16)
                session.transitions().startAdjacentIndoorScroll(new ScrollController(), ScrollController.LEFT, 0, 64);
            assertEquals(room, session.currentRoomId());
            for (int c = 0; c < 4; c++)
                assertEquals(
                        RomBank.decodeRgb555((rom[offset + c * 2] & 0xFF) | ((rom[offset + c * 2 + 1] & 0xFF) << 8)),
                        session.activeRoom().palettes()[6][c]);
        }
    }
}
