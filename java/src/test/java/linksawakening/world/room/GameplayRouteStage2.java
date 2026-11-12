package linksawakening.world.room;

import static linksawakening.world.room.RoomConstants.ROOM_OBJECTS_BASE;
import static linksawakening.world.room.RoomTransitionTestSupport.*;
import static linksawakening.world.room.RoomTransitionTestSupport2.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.entity.Link;
import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.world.entity.EnemyAttackContext;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.transition.ScrollController;
import linksawakening.world.transition.Warp;

final class GameplayRouteStage2 {
    static void run(GameplayRouteState route) throws Exception {

        route.session.setTailKeyOwned(route.playerState.tailKeyCount() != 0);

        route.tailKeyOwlDialogOpened = false;
        route.tailKeyOwlDeadline = route.frame + 0x300;
        while (!route.tailKeyOwlDialogOpened && route.frame < route.tailKeyOwlDeadline) {
            tickInteractiveEntities(route.session, route.frame++, 0x50, 0x50);
            route.tailKeyOwlDialogOpened |= route.session.consumeEntityDialogRequests().stream()
                    .anyMatch(request -> request.globalDialogId() == 0x0C1);
        }
        assertTrue(route.tailKeyOwlDialogOpened);
        route.tailKeyOwlExitDeadline = route.frame + 0x100;
        while ((route.session.components.roomSessionViews().overworldRoomStatusForTest(0x41) & 0x20) == 0
                && route.frame < route.tailKeyOwlExitDeadline) {
            tickInteractiveEntities(route.session, route.frame++, 0x50, 0x50);
        }
        assertEquals(0x20, route.session.components.roomSessionViews().overworldRoomStatusForTest(0x41) & 0x20);

        route.routeToTailCave = new int[] {
            ScrollController.DOWN, ScrollController.DOWN, ScrollController.DOWN,
            ScrollController.LEFT, ScrollController.DOWN, ScrollController.DOWN,
            ScrollController.DOWN, ScrollController.DOWN, ScrollController.DOWN,
            ScrollController.RIGHT, ScrollController.RIGHT, ScrollController.DOWN,
            ScrollController.RIGHT
        };
        route.tailCaveRouteRooms =
                new int[] {0x51, 0x61, 0x71, 0x70, 0x80, 0x90, 0xA0, 0xB0, 0xC0, 0xC1, 0xC2, 0xD2, 0xD3};
        for (int index = 0; index < route.routeToTailCave.length; index++) {
            walkToAndCrossOverworldBoundary(
                    route.session,
                    route.coordinator,
                    route.scroll,
                    route.collision,
                    route.link,
                    route.routeToTailCave[index],
                    index + 1 < route.routeToTailCave.length
                            ? route.routeToTailCave[index + 1]
                            : ScrollController.NONE);
            assertEquals(route.tailCaveRouteRooms[index], route.session.currentRoomId());
        }

        // Exercise Main's collision bridge with the source upward collision
        // bit and the two source keyhole-probe coordinates.
        route.tailCaveSounds = new java.util.ArrayList<GameplaySoundEvent>();
        route.session.setColorShellSoundSink(route.tailCaveSounds::add);
        assertTrue(route.session.tryUnlockTailCaveKeyhole(
                0x5A, 0x4A, Link.DIRECTION_UP, 0x01, route.playerState.tailKeyCount() != 0));
        assertEquals(0x10, route.session.components.roomSessionViews().overworldRoomStatusForTest(0xD3) & 0x10);
        assertEquals(0xDF, route.session.components.roomDoorInteractions().tailCaveKeyholeCountdownForTest());
        route.rumbleTicks = 0;
        route.shookRight = false;
        route.shookLeft = false;
        while (route.session.tailCaveKeyholeSequenceActive()) {
            int shake = route.session.tickTailCaveKeyholeSequence();
            route.shookRight |= shake == 1;
            route.shookLeft |= shake == -2;
            route.rumbleTicks++;
        }
        assertEquals(0xE0, route.rumbleTicks);
        assertTrue(route.shookRight);
        assertTrue(route.shookLeft);
        assertEquals(
                List.of(
                        GameplaySoundEvent.DOOR_UNLOCKED,
                        GameplaySoundEvent.OPEN_KEY_CAVERN,
                        GameplaySoundEvent.DUNGEON_OPENED),
                route.tailCaveSounds);
        assertEquals(0xE3, route.session.activeRoom().roomObjectsArea()[ROOM_OBJECTS_BASE + 0x16]);

        route.tailCaveEntrance = route.session.activeRoom().warps().stream()
                .filter(warp -> warp.tileLocation() == 0x16)
                .findFirst()
                .orElseThrow();
        route.link.setPixelPosition(
                (route.tailCaveEntrance.tileLocation() & 0x0F) * 16,
                (route.tailCaveEntrance.tileLocation() >>> 4) * 16);
        walkIntoDoor(route.link, route.inputState, route.inputConfig, route.coordinator, route.transition);
        while (route.transition.isActive()) route.transition.tick();
        assertEquals(Warp.CATEGORY_INDOOR, route.session.mapCategory());
        assertEquals(0x00, route.session.activeRoom().mapId());
        assertEquals(0x17, route.session.currentRoomId());

        // Room $16 is the ROM's third Small Key source. Lure both Hardhat
        // Beetles into the continuous north pit row, then collect the live
        // DropKeyEffectHandler entity produced by event $81.
        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.LEFT);
        assertEquals(0x16, route.session.currentRoomId());
        route.session.tickEntitiesWithProjectileEvents(route.frame++, 0x50, 0x50, 0, 0, Link.DIRECTION_UP, false);
        route.hardhatSlots = route.session.activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x20)
                .map(RoomEntity::slot)
                .toList();
        assertEquals(2, route.hardhatSlots.size());
        for (int hardhatSlot : route.hardhatSlots) {
            for (int hit = 0;
                    hit < 8
                            && route.session
                                            .activeRoom()
                                            .entities()
                                            .slots()
                                            .get(hardhatSlot)
                                            .status()
                                    == EntityStatus.ACTIVE;
                    hit++) {
                RoomEntity hardhat =
                        route.session.activeRoom().entities().slots().get(hardhatSlot);
                List<EntityCombatEvent> recoil = route.session.resolveEntityCombat(
                        hardhatSlot ^ 1,
                        hardhat.x(),
                        hardhat.y() + 0x20,
                        false,
                        true,
                        true,
                        hardhat.x(),
                        0x10,
                        hardhat.y(),
                        0x10);
                assertTrue(recoil.stream().anyMatch(event -> event.slot() == hardhatSlot && event.swordHit()));
                for (int recoilFrame = 0; recoilFrame < 0x30; recoilFrame++) {
                    route.session.tickEntitiesWithProjectileEvents(
                            route.frame++, hardhat.x(), hardhat.y() + 0x20, 0, 0, Link.DIRECTION_UP, false);
                }
            }
        }
        route.room16Deadline = route.frame + 0x300;
        while (route.session.components.roomSessionViews().activeRoomEventForTest() != 0
                && route.frame < route.room16Deadline) {
            route.session.tickEntitiesWithProjectileEvents(route.frame++, 0x50, 0x50, 0, 0, Link.DIRECTION_UP, false);
        }
        assertEquals(
                0,
                route.session.components.roomSessionViews().activeRoomEventForTest(),
                route.session.activeRoom().entities().loadedEntities().toString());
        route.droppedSmallKey = route.session.activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x30)
                .findFirst()
                .orElseThrow();
        route.keyLandingDeadline = route.frame + 0x200;
        while (route.droppedSmallKey.z() != 0 && route.frame < route.keyLandingDeadline) {
            route.session.tickEntitiesWithProjectileEvents(route.frame++, 0x50, 0x50, 0, 0, Link.DIRECTION_DOWN, false);
            route.droppedSmallKey =
                    route.session.activeRoom().entities().slots().get(route.droppedSmallKey.slot());
        }
        assertEquals(0, route.droppedSmallKey.z());
        route.keyPickupFrame = (route.droppedSmallKey.slot() & 0x01) == 0 ? route.frame | 1 : route.frame & ~1;
        assertNotNull(route.session.collectEntityIfNeeded(
                route.keyPickupFrame,
                route.droppedSmallKey.x(),
                route.droppedSmallKey.y(),
                false,
                true,
                Link.DIRECTION_DOWN,
                0));
        assertEquals(1, route.session.currentDungeonItemFlagsSnapshot()[DungeonItemState.SMALL_KEYS_INDEX]);
        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.RIGHT);
        assertEquals(0x17, route.session.currentRoomId());
        assertEquals(0, route.session.components.roomSessionViews().activeRoomEventForTest());
        assertEquals(0, route.session.activeRoom().shutterDoorMask());

        route.entranceExit = reachableBoundaryPosition(
                route.collision, route.link.pixelX(), route.link.pixelY(), ScrollController.UP);
        assertNotNull(route.entranceExit, collisionGrid(route.collision));
        route.link.setPixelPosition(route.entranceExit[0], -1);
        route.coordinator.handleWarpAndIndoorBoundaries(route.link);
        while (route.scroll.isActive()) route.scroll.tick(8);
        assertEquals(0x13, route.session.currentRoomId());

        route.chestIndex = ROOM_OBJECTS_BASE + 0x28;
        assertEquals(0x63, route.session.components.roomSessionViews().activeRoomEventForTest());
        assertFalse(route.session.activeRoom().roomObjectsArea()[route.chestIndex] == 0xA0);
        route.buttonDeadline = route.frame + 0x40;
        while (route.session.components.roomSessionViews().activeRoomEventForTest() != 0
                && route.frame < route.buttonDeadline) {
            route.session.tickEntities(route.frame++, 0x58, 0x40);
        }
        assertEquals(0, route.session.components.roomSessionViews().activeRoomEventForTest());
        route.chestAppearanceDeadline = route.frame + 0x20;
        while (route.session.activeRoom().roomObjectsArea()[route.chestIndex] != 0xA0
                && route.frame < route.chestAppearanceDeadline) {
            route.session.tickEntities(route.frame++, 0x40, 0x60);
        }
        assertEquals(0xA0, route.session.activeRoom().roomObjectsArea()[route.chestIndex]);

        route.firstSmallKeyChest =
                route.session.tryOpenChest(0x78, 0x21, Link.DIRECTION_UP, true, route.playerState.swordLevel());
        assertTrue(route.firstSmallKeyChest.opened());
        assertEquals(ChestContentsTable.CHEST_SMALL_KEY, route.firstSmallKeyChest.itemType());
        route.session.tickEntities(route.frame++, 0x78, 0x21);
        assertEquals(2, route.session.currentDungeonItemFlagsSnapshot()[DungeonItemState.SMALL_KEYS_INDEX]);

        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.RIGHT);
        assertEquals(0x14, route.session.currentRoomId());
        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.UP);
        assertEquals(0x0F, route.session.currentRoomId());

        route.upperKeyDoorIndex = ROOM_OBJECTS_BASE + 0x39;
        route.lowerKeyDoorIndex = ROOM_OBJECTS_BASE + 0x49;
        assertEquals(0x33, route.session.activeRoom().roomObjectsArea()[route.upperKeyDoorIndex]);
        assertEquals(0x34, route.session.activeRoom().roomObjectsArea()[route.lowerKeyDoorIndex]);
        // bank2's right-facing key-door branch probes Link at ($85,$38)
        // with collision bit $08 against the vertical door pair $39/$49.
        route.firstKeyDoorLinkX = 0x85;
        route.firstKeyDoorLinkY = 0x38;
        route.rightCollisionBit = 0x08;
        route.link.setPixelPosition(route.firstKeyDoorLinkX, route.firstKeyDoorLinkY);
        assertTrue(route.session.tryUnlockIndoorKeyDoor(
                route.link.pixelX(), route.link.pixelY(), Link.DIRECTION_RIGHT, route.rightCollisionBit));
        assertEquals(1, route.session.currentDungeonItemFlagsSnapshot()[DungeonItemState.SMALL_KEYS_INDEX]);
        for (int tick = 0; tick < 8; tick++) {
            route.session.tickEntities(route.frame++, route.link.pixelX(), route.link.pixelY());
            assertTrue(route.session.components.entityBridge().consumeWorldLinkMotionBlockRequest());
        }
        route.session.tickEntities(route.frame++, route.link.pixelX(), route.link.pixelY());
        assertFalse(route.session.components.entityBridge().consumeWorldLinkMotionBlockRequest());
        assertEquals(0x0B, route.session.activeRoom().roomObjectsArea()[route.upperKeyDoorIndex]);
        assertEquals(0x0C, route.session.activeRoom().roomObjectsArea()[route.lowerKeyDoorIndex]);
        assertEquals(0x01, route.session.components.roomSessionViews().indoorRoomStatusForTest(0x00, 0x0F) & 0x01);
        assertEquals(0x02, route.session.components.roomSessionViews().indoorRoomStatusForTest(0x00, 0x10) & 0x02);

        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.RIGHT);
        assertEquals(0x10, route.session.currentRoomId());
        assertEquals(0, route.session.components.roomSessionViews().activeRoomEventForTest());
        assertTrue(route.session.activeRoom().entities().loadedEntities().stream()
                .anyMatch(entity -> entity.type() == 0x1E));

        // Rolling Bones is reachable to the east, but the progression-bearing
        // route is north: room $0A supplies the stone beak and its three-card
        // puzzle is the next sword-solvable room before Link owns the feather.
        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.UP);
        assertEquals(0x0A, route.session.currentRoomId());
        assertEquals(0x61, route.session.components.roomSessionViews().activeRoomEventForTest());
        route.cards = route.session.activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == EntitySpriteHandlerCatalog.ENTITY_THREE_OF_A_KIND)
                .toList();
        assertEquals(3, route.cards.size());
        assertTrue(route.cards.stream().allMatch(card -> card.spriteDefinition().supported()));

        // The Stone Beak branch is optional. The required route returns south
        // and west to the ROM's static small-key chest in room $0E.
        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.DOWN);
        assertEquals(0x10, route.session.currentRoomId());
        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.LEFT);
        assertEquals(0x0F, route.session.currentRoomId());
        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.LEFT);
        assertEquals(0x0E, route.session.currentRoomId());

        route.secondSmallKeyChest =
                route.session.tryOpenChest(0x38, 0x21, Link.DIRECTION_UP, true, route.playerState.swordLevel());
        assertTrue(route.secondSmallKeyChest.opened());
        assertEquals(ChestContentsTable.CHEST_SMALL_KEY, route.secondSmallKeyChest.itemType());
        route.session.tickEntitiesWithProjectileEvents(route.frame++, 0x38, 0x21, 0, 0, Link.DIRECTION_DOWN, false);
        assertEquals(2, route.session.currentDungeonItemFlagsSnapshot()[DungeonItemState.SMALL_KEYS_INDEX]);

        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.LEFT);
        assertEquals(0x0D, route.session.currentRoomId());
        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.UP);
        assertEquals(0x07, route.session.currentRoomId());

        route.link.setPixelPosition(0x48, 0x05);
        assertTrue(route.session.tryUnlockIndoorKeyDoor(
                route.link.pixelX(), route.link.pixelY(), Link.DIRECTION_UP, 0x01));
        for (int tick = 0; tick <= 8; tick++) {
            route.session.tickEntitiesWithProjectileEvents(
                    route.frame++, route.link.pixelX(), route.link.pixelY(), 0, 0, Link.DIRECTION_UP, false);
        }
        assertEquals(1, route.session.currentDungeonItemFlagsSnapshot()[DungeonItemState.SMALL_KEYS_INDEX]);
        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.UP);
        assertEquals(0x04, route.session.currentRoomId());

        route.movableBlockIndex = ROOM_OBJECTS_BASE + 0x32;
        assertEquals(0xA7, route.session.activeRoom().roomObjectsArea()[route.movableBlockIndex]);
        for (int tick = 0; tick < 64; tick++) {
            assertTrue(route.session.tryInteractWithIndoorBlock(0x15, 0x28, Link.DIRECTION_RIGHT, 0x08));
        }
        for (int tick = 0; tick < 33; tick++) {
            route.session.tickEntities(route.frame++, 0x15, 0x28);
        }
        assertEquals(0, route.session.activeRoom().shutterDoorMask());
        assertEquals(0, route.session.components.roomSessionViews().indoorRoomStatusForTest(0x00, 0x04) & 0x10);

        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.LEFT);
        assertEquals(0x03, route.session.currentRoomId());
        assertEquals(0xA1, route.session.components.roomSessionViews().activeRoomEventForTest());
        route.hiddenStairsIndex = ROOM_OBJECTS_BASE + 0x18;
        assertEquals(0x0D, route.session.activeRoom().roomObjectsArea()[route.hiddenStairsIndex]);
        route.beetles = route.session.activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x2C)
                .toList();
        assertEquals(2, route.beetles.size());

        // EntityInitHandler runs before the first interactive collision pass.
        route.session.tickEntitiesWithProjectileEvents(route.frame++, 0, 0, 0, 0, Link.DIRECTION_DOWN, false);
        route.beetles = route.session.activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x2C)
                .toList();
        // The raised shield flips beetles; the sword then damages the underside.
        for (RoomEntity beetle : route.beetles) {
            List<EntityCombatEvent> flipEvents = route.session.resolveEntityCombat(
                    beetle.slot() ^ 1,
                    beetle.x() - 8,
                    beetle.y(),
                    false,
                    0,
                    true,
                    false,
                    0,
                    0,
                    0,
                    0,
                    new EnemyAttackContext(1, false, false, false, false, true, 0, 0));
            assertTrue(
                    flipEvents.stream()
                            .anyMatch(event ->
                                    event.slot() == beetle.slot() && !event.swordHit() && event.soundId() == 0x09),
                    flipEvents.toString());
        }
        route.session.tickEntitiesWithProjectileEvents(route.frame++, 0, 0, 0, 0, Link.DIRECTION_DOWN, false);
        route.beetles = route.session.activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x2C)
                .toList();
        for (RoomEntity beetle : route.beetles) {
            List<EntityCombatEvent> damageEvents = route.session.resolveEntityCombat(
                    beetle.slot() ^ 1,
                    0,
                    0,
                    false,
                    true,
                    true,
                    beetle.x() + 0x08,
                    1,
                    beetle.y() - beetle.z() + 0x08,
                    1);
            assertTrue(
                    damageEvents.stream()
                            .anyMatch(event ->
                                    event.slot() == beetle.slot() && event.swordHit() && event.enemyDamage() > 0),
                    damageEvents.toString());
        }
        for (int attack = 0; attack < 3; attack++) {
            for (int recovery = 0; recovery < 12; recovery++) {
                route.session.tickEntitiesWithProjectileEvents(route.frame++, 0, 0, 0, 0, Link.DIRECTION_DOWN, false);
            }
            route.beetles = route.session.activeRoom().entities().loadedEntities().stream()
                    .filter(entity -> entity.type() == 0x2C)
                    .toList();
            for (RoomEntity beetle : route.beetles) {
                route.session.resolveEntityCombat(
                        beetle.slot() ^ 1,
                        0,
                        0,
                        false,
                        true,
                        true,
                        beetle.x() + 0x08,
                        1,
                        beetle.y() - beetle.z() + 0x08,
                        1);
            }
        }
        route.beetleDeadline = route.frame + 0x200;
        while (route.session.components.roomSessionViews().activeRoomEventForTest() != 0
                && route.frame < route.beetleDeadline) {
            route.session.tickEntitiesWithProjectileEvents(route.frame++, 0, 0, 0, 0, Link.DIRECTION_DOWN, false);
        }
        assertEquals(
                0,
                route.session.components.roomSessionViews().activeRoomEventForTest(),
                route.session.activeRoom().entities().loadedEntities().toString());
        assertEquals(0x0D, route.session.activeRoom().roomObjectsArea()[route.hiddenStairsIndex]);
        for (int revealFrame = 0; revealFrame < 11; revealFrame++) {
            route.session.tickEntitiesWithProjectileEvents(route.frame++, 0, 0, 0, 0, Link.DIRECTION_DOWN, false);
        }
        assertEquals(0xBE, route.session.activeRoom().roomObjectsArea()[route.hiddenStairsIndex]);
    }
}
