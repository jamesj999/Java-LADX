package linksawakening.world.room;

import static linksawakening.world.room.RoomConstants.ROOM_OBJECTS_BASE;
import static linksawakening.world.room.RoomTransitionTestSupport.*;
import static linksawakening.world.room.RoomTransitionTestSupport2.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import linksawakening.entity.Link;
import linksawakening.equipment.Sword;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.event.ChestRewardEvent;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.transition.ScrollController;

final class GameplayRouteStage6 {
    static void run(GameplayRouteState route) throws Exception {

        while (route.secondBottleKey.z() != 0 && route.frame < route.secondKeyLandingDeadline) {
            tickInteractiveEntities(route.session, route.frame++, route.link.romEntityX(), route.link.romEntityY());
            route.secondBottleKey =
                    route.session.activeRoom().entities().slots().get(route.secondBottleKey.slot());
        }
        assertEquals(0, route.secondBottleKey.z());
        route.secondKeyPath = reachableEntityContactPath(
                route.collision, route.link.pixelX(), route.link.pixelY(), route.secondBottleKey);
        assertNotNull(route.secondKeyPath, collisionGrid(route.collision));
        for (int[] position : route.secondKeyPath) {
            route.link.setPixelPosition(position[0], position[1]);
        }
        route.secondKeyPickupFrame = (route.secondBottleKey.slot() & 0x01) == 0 ? route.frame | 1 : route.frame & ~1;
        assertNotNull(route.session.collectEntityIfNeeded(
                route.secondKeyPickupFrame,
                route.link.romEntityX(),
                route.link.romEntityY(),
                false,
                true,
                Link.DIRECTION_DOWN,
                0));
        assertEquals(2, route.session.currentDungeonItemFlagsSnapshot()[DungeonItemState.SMALL_KEYS_INDEX]);
        assertEquals(0x10, route.session.indoorRoomStatusForTest(0x01, 0x34) & 0x10);

        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.LEFT);
        assertEquals(0x33, route.session.currentRoomId());
        tickInteractiveEntities(route.session, route.frame++, route.link.romEntityX(), route.link.romEntityY());
        route.returnCrystalSwitch = route.session.activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x66)
                .findFirst()
                .orElseThrow();
        route.returnSwitchPath = reachableEntityContactPath(
                route.collision, route.link.pixelX(), route.link.pixelY(), route.returnCrystalSwitch);
        assertNotNull(route.returnSwitchPath, collisionGrid(route.collision));
        for (int[] position : route.returnSwitchPath) {
            route.link.setPixelPosition(position[0], position[1]);
        }
        route.returnSwitchSword = new Sword(route.romTables, null);
        route.returnSwitchSword.onPress();
        for (int swingFrame = 0; swingFrame < 4; swingFrame++) {
            route.returnSwitchSword.tick(false);
        }
        route.link.setDirection(Link.DIRECTION_RIGHT);
        route.initialReturnSwordBox = route.returnSwitchSword.enemyCollisionBox(
                route.link.romEntityX(), route.link.romSwordCollisionY(), route.link.direction());
        route.link.setPixelPosition(
                route.returnCrystalSwitch.x() - (route.initialReturnSwordBox.x() - route.link.pixelX()),
                route.returnCrystalSwitch.y() - (route.initialReturnSwordBox.y() - route.link.pixelY()));
        route.returnSwordBox = route.returnSwitchSword.enemyCollisionBox(
                route.link.romEntityX(), route.link.romSwordCollisionY(), route.link.direction());
        assertTrue(route.returnSwordBox.active());
        route.returnSwitchHit = route.session.resolveEntityCombat(
                route.frame++,
                route.link.romEntityX(),
                route.link.romEntityY(),
                false,
                true,
                true,
                route.returnSwordBox.x(),
                route.returnSwordBox.width(),
                route.returnSwordBox.y(),
                route.returnSwordBox.height());
        route.currentReturnCrystalSwitch =
                route.session.activeRoom().entities().slots().get(route.returnCrystalSwitch.slot());
        assertEquals(EntityStatus.ACTIVE, route.currentReturnCrystalSwitch.status());
        assertTrue(
                route.returnSwitchHit.stream()
                        .anyMatch(event -> event.slot() == route.returnCrystalSwitch.slot()
                                && event.type() == 0x66
                                && event.swordHit()),
                route.returnSwitchHit.toString());
        tickInteractiveEntities(route.session, route.frame++, route.link.romEntityX(), route.link.romEntityY());
        assertEquals(0x01, route.session.switchableObjectAnimationStageForTest());
        assertTrue(route.session.consumeEntityEvents().stream()
                .anyMatch(event -> event.type() == 0x66
                        && event.soundChannel() == EntityCombatEvent.SoundChannel.WAVE
                        && event.soundId() == 0x0E));
        for (int stage = 0; stage < 9; stage++) {
            route.session.tickGameplayVBlank();
        }
        assertEquals(0x00, route.session.entitySwitchBlocksStateForTest());
        assertEquals(0x00, route.session.switchableObjectAnimationStageForTest());
        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.LEFT);
        assertEquals(0x32, route.session.currentRoomId());
        assertEquals(2, route.session.currentDungeonItemFlagsSnapshot()[DungeonItemState.SMALL_KEYS_INDEX]);
        route.bottomKeyDoorLocation = -1;
        for (int row = 0; row < RoomConstants.OBJECTS_PER_COLUMN; row++) {
            for (int column = 0; column < RoomConstants.OBJECTS_PER_ROW - 1; column++) {
                int location = (row << 4) | column;
                if (objectAt(route.session, location) == 0x2F && objectAt(route.session, location + 1) == 0x30) {
                    route.bottomKeyDoorLocation = location;
                }
            }
        }
        assertEquals(0x74, route.bottomKeyDoorLocation);

        route.bottomDoorPath = reachablePositionPath(
                route.collision,
                route.link.pixelX(),
                route.link.pixelY(),
                (x, y) -> x == 0x48
                        && (route.collision.objectPhysicsFlagAtPoint(x + 0x06, y + 1 + 0x0F) == 0x91
                                || route.collision.objectPhysicsFlagAtPoint(x + 0x09, y + 1 + 0x0F) == 0x91));
        assertNotNull(route.bottomDoorPath, collisionGrid(route.collision));
        for (int[] position : route.bottomDoorPath) {
            route.link.setPixelPosition(position[0], position[1]);
        }
        route.bottomDoorContactX = route.link.pixelX();
        route.bottomDoorContactY = route.link.pixelY();
        route.link.setPixelPosition(route.bottomDoorContactX, route.bottomDoorContactY + 1);
        assertEquals(route.bottomDoorContactX, route.link.pixelX());
        assertEquals(route.bottomDoorContactY + 1, route.link.pixelY());
        assertTrue(route.collision.objectPhysicsFlagAtPoint(route.link.pixelX() + 0x06, route.link.pixelY() + 0x0F)
                        == 0x91
                || route.collision.objectPhysicsFlagAtPoint(route.link.pixelX() + 0x09, route.link.pixelY() + 0x0F)
                        == 0x91);
        assertTrue(route.session.tryUnlockIndoorKeyDoor(
                route.link.pixelX(), route.link.pixelY(), Link.DIRECTION_DOWN, 0x02));
        assertEquals(1, route.session.currentDungeonItemFlagsSnapshot()[DungeonItemState.SMALL_KEYS_INDEX]);
        for (int tick = 0; tick < 8; tick++) {
            route.session.tickEntities(route.frame++, route.link.pixelX(), route.link.pixelY());
            assertTrue(route.session.consumeWorldLinkMotionBlockRequest());
        }
        route.session.tickEntities(route.frame++, route.link.pixelX(), route.link.pixelY());
        assertFalse(route.session.consumeWorldLinkMotionBlockRequest());
        assertEquals(0x8C, objectAt(route.session, route.bottomKeyDoorLocation));
        assertEquals(0x08, objectAt(route.session, route.bottomKeyDoorLocation + 1));
        assertEquals(0x08, route.session.indoorRoomStatusForTest(0x01, 0x32) & 0x08);
        assertEquals(0x04, route.session.indoorRoomStatusForTest(0x01, 0x37) & 0x04);

        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.DOWN);
        assertEquals(0x37, route.session.currentRoomId());
        assertEquals(0x61, route.session.activeRoomEventForTest());
        assertEquals(
                1,
                route.session.activeRoom().entities().loadedEntities().stream()
                        .filter(entity -> entity.type() == 0x8F)
                        .count());
        route.room37RupeeSlots = route.session.activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x2E)
                .map(RoomEntity::slot)
                .toList();
        assertEquals(2, route.room37RupeeSlots.size());
        tickInteractiveEntities(route.session, route.frame++, route.link.romEntityX(), route.link.romEntityY());

        route.maskedMimic = route.session.activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x8F)
                .findFirst()
                .orElseThrow();
        route.maskedMimicSlot = route.maskedMimic.slot();
        for (int hit = 0; hit < 2; hit++) {
            route.maskedMimic = route.session.activeRoom().entities().slots().get(route.maskedMimicSlot);
            assertEquals(EntityStatus.ACTIVE, route.maskedMimic.status());
            int linkEntityX = route.maskedMimic.x();
            int linkEntityY = (route.maskedMimic.y() + 0x20) & 0xFF;
            route.session.setEntityPressedButtonsMask(0x08);
            tickInteractiveEntities(route.session, route.frame++, linkEntityX, linkEntityY);
            tickInteractiveEntities(route.session, route.frame++, linkEntityX, linkEntityY);
            route.maskedMimic = route.session.activeRoom().entities().slots().get(route.maskedMimicSlot);
            List<EntityCombatEvent> hitEvents = route.session.resolveEntityCombat(
                    route.frame++,
                    linkEntityX,
                    linkEntityY,
                    false,
                    true,
                    true,
                    route.maskedMimic.x() + 0x08,
                    0x08,
                    route.maskedMimic.y() + 0x08,
                    0x08);
            assertTrue(
                    hitEvents.stream()
                            .anyMatch(event -> event.slot() == route.maskedMimicSlot
                                    && event.swordHit()
                                    && event.enemyDamage() == 1),
                    hitEvents.toString());
            if (hit == 1) {
                assertEquals(0x61, route.session.activeRoomEventForTest());
                assertEquals(
                        2,
                        route.session.activeRoom().entities().loadedEntities().stream()
                                .filter(entity -> entity.type() == 0x2E)
                                .count());
            }
            route.session.setEntityPressedButtonsMask(0);
            for (int recovery = 0; recovery < 0x30; recovery++) {
                route.session.tickEntitiesWithProjectileEvents(
                        route.frame++,
                        route.link.romEntityX(),
                        route.link.romEntityY(),
                        0,
                        0,
                        route.link.direction(),
                        false);
            }
        }
        route.mimicDeathDeadline = route.frame + 0x200;
        while (route.session
                                .activeRoom()
                                .entities()
                                .slots()
                                .get(route.maskedMimicSlot)
                                .status()
                        != EntityStatus.DISABLED
                && route.frame < route.mimicDeathDeadline) {
            tickInteractiveEntities(route.session, route.frame++, route.link.romEntityX(), route.link.romEntityY());
        }
        assertEquals(
                EntityStatus.DISABLED,
                route.session
                        .activeRoom()
                        .entities()
                        .slots()
                        .get(route.maskedMimicSlot)
                        .status());

        route.room37EventDeadline = route.frame + 0x300;
        while (route.session.activeRoomEventForTest() != 0 && route.frame < route.room37EventDeadline) {
            tickInteractiveEntities(route.session, route.frame++, route.link.romEntityX(), route.link.romEntityY());
        }
        assertEquals(0, route.session.activeRoomEventForTest());
        route.compassChestIndex = ROOM_OBJECTS_BASE + 0x28;
        route.chestRevealDeadline = route.frame + 0x40;
        while (route.session.activeRoom().roomObjectsArea()[route.compassChestIndex] != 0xA0
                && route.frame < route.chestRevealDeadline) {
            route.session.tickEntities(route.frame++, route.link.romEntityX(), route.link.romEntityY());
        }
        assertEquals(0xA0, route.session.activeRoom().roomObjectsArea()[route.compassChestIndex]);
        assertEquals(
                2,
                route.room37RupeeSlots.stream()
                        .filter(slot -> {
                            RoomEntity entity = route.session
                                    .activeRoom()
                                    .entities()
                                    .slots()
                                    .get(slot);
                            return entity.loaded() && entity.type() == 0x2E;
                        })
                        .count());
        assertEquals(0, route.session.indoorRoomStatusForTest(0x01, 0x37) & 0x10);
        route.compassChestPath = reachablePositionPath(
                route.collision,
                route.link.pixelX(),
                route.link.pixelY(),
                (x, y) -> Math.floorDiv(x + 0x08, 0x10) == (0x28 & 0x0F)
                        && Math.floorDiv(y + 0x05, 0x10) == ((0x28 & 0xF0) >>> 4));
        assertNotNull(route.compassChestPath, collisionGrid(route.collision));
        for (int[] position : route.compassChestPath) {
            route.link.setPixelPosition(position[0], position[1]);
        }
        route.compassChestContactX = route.link.pixelX();
        route.compassChestContactY = route.link.pixelY();
        route.link.setPixelPosition(route.compassChestContactX, route.compassChestContactY - 1);
        assertEquals(route.compassChestContactX, route.link.pixelX());
        assertEquals(route.compassChestContactY - 1, route.link.pixelY());
        route.compassChestInteractionLocation = (Math.floorDiv(route.link.pixelY() + 0x05, 0x10) << 4)
                | Math.floorDiv(route.link.pixelX() + 0x08, 0x10);
        assertEquals(0x28, route.compassChestInteractionLocation);
        route.compassChest = route.session.tryOpenChest(
                route.link.pixelX(), route.link.pixelY(), Link.DIRECTION_UP, true, route.playerState.swordLevel());
        assertTrue(route.compassChest.opened());
        assertEquals(ChestContentsTable.CHEST_COMPASS, route.compassChest.itemType());
        assertEquals(0x28, route.compassChest.location());
        assertEquals(0x10, route.session.indoorRoomStatusForTest(0x01, 0x37) & 0x10);
        route.compassChestSlot = route.compassChest.entitySlot();
        route.compassRewardObserved = false;
        route.compassDialogObserved = false;
        route.compassChestEnded = false;
        for (int chestTick = 0; chestTick < 0x40; chestTick++) {
            route.session.tickEntities(route.frame++, route.link.romEntityX(), route.link.romEntityY());
            for (ChestRewardEvent reward : route.session.consumeChestRewardEvents()) {
                route.compassRewardObserved |= reward.itemType() == ChestContentsTable.CHEST_COMPASS;
                route.playerState.applyChestReward(reward.itemType());
            }
            route.compassDialogObserved |= route.session.consumeEntityDialogRequests().stream()
                    .anyMatch(request -> request.globalDialogId() == 0x0A7);
            RoomEntity chestEntity =
                    route.session.activeRoom().entities().slots().get(route.compassChestSlot);
            if (!chestEntity.loaded() || chestEntity.status() == EntityStatus.DISABLED) {
                route.compassChestEnded = true;
                break;
            }
        }
        assertTrue(route.compassRewardObserved);
        assertTrue(route.compassDialogObserved);
        assertTrue(route.compassChestEnded);
        assertEquals(1, route.session.currentDungeonItemFlagsSnapshot()[DungeonItemState.COMPASS_INDEX]);
        assertEquals(1, route.session.currentDungeonItemFlagsSnapshot()[DungeonItemState.SMALL_KEYS_INDEX]);

        // MapLayout1 adjacency is not sufficient: room $37's east and west
        // edges are closed. Return north and follow room $32's west opening.
        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.UP);
        assertEquals(0x32, route.session.currentRoomId());
        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.LEFT);
        assertEquals(0x31, route.session.currentRoomId());
        // The doorway stays open on entry; its shutter closes only once
        // Link is inside, independently of the minimap's directional bits.
        assertEquals(0x01, route.session.indoorRoomStatusForTest(0x01, 0x31) & 0x01);
        assertEquals(0x02, route.session.indoorRoomStatusForTest(0x01, 0x32) & 0x02);
        assertEquals(0x08, route.session.activeRoom().shutterDoorMask());
        assertEquals(0x0B, objectAt(route.session, 0x39));
        assertEquals(0x0C, objectAt(route.session, 0x49));
        // Room $31's west key door is the last Small Key gate in this route.
        assertEquals(0x31, objectAt(route.session, 0x30));
        assertEquals(0x32, objectAt(route.session, 0x40));
        route.westDoorPath = reachablePositionPath(
                route.collision,
                route.link.pixelX(),
                route.link.pixelY(),
                (x, y) -> y == 0x38
                        && (route.collision.objectPhysicsFlagAtPoint(x + 0x03, y + 0x09) == 0x92
                                || route.collision.objectPhysicsFlagAtPoint(x + 0x03, y + 0x0C) == 0x92));
        assertNotNull(route.westDoorPath, collisionGrid(route.collision));
        for (int[] position : route.westDoorPath) {
            route.link.setPixelPosition(position[0], position[1]);
        }
        route.westDoorContactX = route.link.pixelX();
        route.westDoorContactY = route.link.pixelY();
        route.link.setPixelPosition(route.westDoorContactX - 1, route.westDoorContactY);
        assertEquals(route.westDoorContactX - 1, route.link.pixelX());
        assertEquals(route.westDoorContactY, route.link.pixelY());
        assertTrue(route.collision.objectPhysicsFlagAtPoint(route.link.pixelX() + 0x04, route.link.pixelY() + 0x09)
                        == 0x92
                || route.collision.objectPhysicsFlagAtPoint(route.link.pixelX() + 0x04, route.link.pixelY() + 0x0C)
                        == 0x92);
        assertTrue(route.session.tryUnlockIndoorKeyDoor(
                route.link.pixelX(), route.link.pixelY(), Link.DIRECTION_LEFT, 0x04));
        assertEquals(0, route.session.currentDungeonItemFlagsSnapshot()[DungeonItemState.SMALL_KEYS_INDEX]);
        for (int tick = 0; tick < 8; tick++) {
            route.session.tickEntities(route.frame++, route.link.pixelX(), route.link.pixelY());
            assertTrue(route.session.consumeWorldLinkMotionBlockRequest());
        }
        route.session.tickEntities(route.frame++, route.link.pixelX(), route.link.pixelY());
        assertFalse(route.session.consumeWorldLinkMotionBlockRequest());
        assertEquals(0x09, objectAt(route.session, 0x30));
        assertEquals(0x0A, objectAt(route.session, 0x40));
        assertEquals(0x02, route.session.indoorRoomStatusForTest(0x01, 0x31) & 0x02);
        assertEquals(0x01, route.session.indoorRoomStatusForTest(0x01, 0x30) & 0x01);
        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.LEFT);
        assertEquals(0x30, route.session.currentRoomId());

        assertEquals(0x21, route.session.activeRoomEventForTest());
        assertEquals(
                4,
                route.session.activeRoom().entities().loadedEntities().stream()
                        .filter(entity -> entity.type() == 0x27)
                        .count());
        assertEquals(
                2,
                route.session.activeRoom().entities().loadedEntities().stream()
                        .filter(entity -> entity.type() == 0x19)
                        .count());
        assertTrue((route.session.activeRoom().shutterDoorMask() & 0x01) != 0);
        route.room30KeeseSlots = route.session.activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x19)
                .map(RoomEntity::slot)
                .toList();
    }
}
