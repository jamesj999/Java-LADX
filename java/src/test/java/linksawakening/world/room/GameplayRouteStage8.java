package linksawakening.world.room;

import static linksawakening.world.room.RoomTransitionTestSupport.*;
import static linksawakening.world.room.RoomTransitionTestSupport2.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;

import linksawakening.entity.Link;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.event.ChestRewardEvent;
import linksawakening.world.transition.ScrollController;
import linksawakening.world.transition.Warp;

final class GameplayRouteStage8 {
    static void run(GameplayRouteState route) throws Exception {

        assertEquals(1, route.session.currentDungeonItemFlagsSnapshot()[DungeonItemState.SMALL_KEYS_INDEX]);

        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.RIGHT);
        assertEquals(0x39, route.session.currentRoomId());
        assertEquals(0x63, route.session.context.roomSessionViews.activeRoomEventForTest());

        route.room39ButtonPath = reachablePositionPath(
                route.collision,
                route.link.pixelX(),
                route.link.pixelY(),
                (x, y) -> route.collision
                                .groundInteractionSample(x + 0x08, y + 0x10)
                                .objectId()
                        == 0xAA);
        assertNotNull(route.room39ButtonPath, collisionGrid(route.collision));
        for (int[] position : route.room39ButtonPath) {
            route.link.setPixelPosition(position[0], position[1]);
            tickInteractiveEntities(route.session, route.frame++, route.link.romEntityX(), route.link.romEntityY());
        }
        route.room39EventDeadline = route.frame + 0x40;
        while (route.session.context.roomSessionViews.activeRoomEventForTest() != 0
                && route.frame < route.room39EventDeadline) {
            tickInteractiveEntities(route.session, route.frame++, route.link.romEntityX(), route.link.romEntityY());
        }
        assertEquals(0, route.session.context.roomSessionViews.activeRoomEventForTest());
        route.room39ChestDeadline = route.frame + 0x20;
        while (objectAt(route.session, 0x28) != 0xA0 && route.frame < route.room39ChestDeadline) {
            tickInteractiveEntities(route.session, route.frame++, route.link.romEntityX(), route.link.romEntityY());
        }
        assertEquals(0xA0, objectAt(route.session, 0x28));
        route.room39ChestPath = reachablePositionPath(
                route.collision,
                route.link.pixelX(),
                route.link.pixelY(),
                (x, y) -> Math.floorDiv(x + 0x08, 0x10) == 0x08 && Math.floorDiv(y + 0x05, 0x10) == 0x02);
        assertNotNull(route.room39ChestPath, collisionGrid(route.collision));
        for (int[] position : route.room39ChestPath) {
            route.link.setPixelPosition(position[0], position[1]);
        }
        route.link.setDirection(Link.DIRECTION_UP);
        route.room39Chest = route.session.tryOpenChest(
                route.link.pixelX(), route.link.pixelY(), Link.DIRECTION_UP, true, route.playerState.swordLevel());
        assertTrue(route.room39Chest.opened());
        assertEquals(ChestContentsTable.CHEST_SMALL_KEY, route.room39Chest.itemType());
        route.room39ChestSlot = route.room39Chest.entitySlot();
        route.room39RewardObserved = false;
        route.room39ChestEnded = false;
        for (int chestTick = 0; chestTick < 0x40; chestTick++) {
            route.session.tickEntities(route.frame++, route.link.romEntityX(), route.link.romEntityY());
            for (ChestRewardEvent reward : route.session.context.entityBridge.outbox.consumeChestRewardEvents()) {
                route.room39RewardObserved |= reward.itemType() == ChestContentsTable.CHEST_SMALL_KEY;
                route.playerState.applyChestReward(reward.itemType());
            }
            RoomEntity chestEntity =
                    route.session.activeRoom().entities().slots().get(route.room39ChestSlot);
            if (!chestEntity.loaded() || chestEntity.status() == EntityStatus.DISABLED) {
                route.room39ChestEnded = true;
                break;
            }
        }
        assertTrue(route.room39RewardObserved);
        assertTrue(route.room39ChestEnded);
        assertEquals(2, route.session.currentDungeonItemFlagsSnapshot()[DungeonItemState.SMALL_KEYS_INDEX]);

        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.LEFT);
        assertEquals(0x38, route.session.currentRoomId());
        assertEquals(0xA1, objectAt(route.session, 0x43));
        assertEquals(0x10, route.session.context.roomSessionViews.indoorRoomStatusForTest(0x01, 0x38) & 0x10);
        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.RIGHT);
        assertEquals(0x39, route.session.currentRoomId());
        assertEquals(0x63, route.session.context.roomSessionViews.activeRoomEventForTest());
        assertEquals(0xA1, objectAt(route.session, 0x28));
        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.UP);
        assertEquals(0x34, route.session.currentRoomId());
        assertTrue(
                route.link.pixelX() > 0x30,
                "room $34 entry must land east of its ROM $A6 partition: x="
                        + String.format("%02X", route.link.pixelX()));
        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.RIGHT);
        assertEquals(0x35, route.session.currentRoomId());
        assertEquals(0, route.session.context.roomSessionViews.activeRoomEventForTest());
        assertEquals(
                2,
                route.session.activeRoom().entities().loadedEntities().stream()
                        .filter(entity -> entity.type() == 0x2C)
                        .count());
        assertEquals(2, route.session.currentDungeonItemFlagsSnapshot()[DungeonItemState.SMALL_KEYS_INDEX]);

        route.link.setPixelPosition(0x75, 0x05);
        assertTrue(route.session.tryUnlockIndoorKeyDoor(
                route.link.pixelX(), route.link.pixelY(), Link.DIRECTION_UP, 0x01));
        assertEquals(1, route.session.currentDungeonItemFlagsSnapshot()[DungeonItemState.SMALL_KEYS_INDEX]);
        assertEquals(0x40, route.session.context.roomSessionViews.indoorRoomStatusForTest(0x01, 0x35) & 0x40);
        for (int tick = 0; tick < 8; tick++) {
            route.session.tickEntities(route.frame++, route.link.pixelX(), route.link.pixelY());
            assertTrue(route.session.context.entityBridge.consumeWorldLinkMotionBlockRequest());
        }
        route.session.tickEntities(route.frame++, route.link.pixelX(), route.link.pixelY());
        assertFalse(route.session.context.entityBridge.consumeWorldLinkMotionBlockRequest());
        assertEquals(0x43, objectAt(route.session, 0x07));
        assertEquals(0x44, objectAt(route.session, 0x08));
        assertEquals(0x04, route.session.context.roomSessionViews.indoorRoomStatusForTest(0x01, 0x35) & 0x04);
        assertEquals(0x08, route.session.context.roomSessionViews.indoorRoomStatusForTest(0x01, 0x2F) & 0x08);

        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.UP);
        assertEquals(0x2F, route.session.currentRoomId());
        assertEquals(0xA7, route.session.context.roomSessionViews.activeRoomEventForTest());
        assertEquals(0xA7, objectAt(route.session, 0x33));
        assertEquals(0xA7, objectAt(route.session, 0x36));
        assertEquals(0x0D, objectAt(route.session, 0x18));
        assertEquals(
                2,
                route.session.activeRoom().entities().loadedEntities().stream()
                        .filter(entity -> entity.type() == 0x19)
                        .count());
        assertEquals(
                1,
                route.session.activeRoom().entities().loadedEntities().stream()
                        .filter(entity -> entity.type() == 0x17)
                        .count());
        assertEquals(
                2,
                route.session.activeRoom().entities().loadedEntities().stream()
                        .filter(entity -> entity.type() == 0x2D)
                        .count());
        assertEquals(
                1,
                route.session.activeRoom().entities().loadedEntities().stream()
                        .filter(entity -> entity.type() == 0x42)
                        .count());

        route.link.setPixelPosition(0x25, 0x28);
        for (int tick = 0; tick < 64; tick++) {
            assertTrue(route.session.tryInteractWithIndoorBlock(
                    route.link.pixelX(), route.link.pixelY(), Link.DIRECTION_RIGHT, 0x08));
        }
        assertEquals(0xA7, route.session.context.roomSessionViews.activeRoomEventForTest());
        for (int tick = 0; tick < 33; tick++) {
            route.session.tickEntities(route.frame++, route.link.romEntityX(), route.link.romEntityY());
        }
        assertEquals(0xA6, objectAt(route.session, 0x34));
        assertEquals(0xA7, route.session.context.roomSessionViews.activeRoomEventForTest());

        route.link.setPixelPosition(0x5C, 0x28);
        for (int tick = 0; tick < 64; tick++) {
            assertTrue(route.session.tryInteractWithIndoorBlock(
                    route.link.pixelX(), route.link.pixelY(), Link.DIRECTION_LEFT, 0x04));
        }
        assertEquals(0xA7, route.session.context.roomSessionViews.activeRoomEventForTest());
        for (int tick = 0; tick < 33; tick++) {
            route.session.tickEntities(route.frame++, route.link.romEntityX(), route.link.romEntityY());
        }
        assertEquals(0xA6, objectAt(route.session, 0x34));
        assertEquals(0xA6, objectAt(route.session, 0x35));
        assertEquals(0xA7, route.session.context.roomSessionViews.activeRoomEventForTest());
        route.session.tickEntities(route.frame++, route.link.romEntityX(), route.link.romEntityY());
        assertEquals(0, route.session.context.roomSessionViews.activeRoomEventForTest());
        for (int tick = 0; tick < 11; tick++) {
            route.session.tickEntities(route.frame++, route.link.romEntityX(), route.link.romEntityY());
        }
        assertEquals(0xBE, objectAt(route.session, 0x18));
        assertEquals(0x10, route.session.context.roomSessionViews.indoorRoomStatusForTest(0x01, 0x2F) & 0x10);
        assertEquals(1, route.session.currentDungeonItemFlagsSnapshot()[DungeonItemState.SMALL_KEYS_INDEX]);

        assertTrue(route.session.activeRoom().hasWarps());
        route.staircaseExitPath = reachablePositionPath(
                route.collision, route.link.pixelX(), route.link.pixelY(), (x, y) -> x <= 0x60 && y == 0x10);
        assertNotNull(route.staircaseExitPath, collisionGrid(route.collision));
        for (int[] position : route.staircaseExitPath) {
            route.link.setPixelPosition(position[0], position[1]);
            route.coordinator.handleWarpAndIndoorBoundaries(route.link);
        }
        assertFalse(route.transition.isActive());
        route.staircaseEntryPath = reachablePositionPath(
                route.collision, route.link.pixelX(), route.link.pixelY(), (x, y) -> x == 0x80 && y == 0x10);
        assertNotNull(route.staircaseEntryPath, collisionGrid(route.collision));
        for (int[] position : route.staircaseEntryPath) {
            route.link.setPixelPosition(position[0], position[1]);
            route.coordinator.handleWarpAndIndoorBoundaries(route.link);
            if (route.transition.isActive()) {
                break;
            }
        }
        assertTrue(route.transition.isActive());
        while (route.transition.isActive()) {
            route.transition.tick();
        }
        assertEquals(Warp.CATEGORY_SIDESCROLL, route.session.mapCategory());
        assertEquals(0x3F, route.session.currentRoomId());
        assertEquals(0x88, route.link.romEntityX());
        assertEquals(0x10, route.link.romEntityY());
        assertEquals(0x01, route.link.romPhysicsModifier(), "side-view warp initializes hLinkPhysicsModifier=$01");

        route.sideViewExitPath =
                reachablePositionPath(route.collision, route.link.pixelX(), route.link.pixelY(), (x, y) -> x == 0);
        assertNotNull(route.sideViewExitPath, collisionGrid(route.collision));
        for (int[] position : route.sideViewExitPath) {
            route.link.setPixelPosition(position[0], position[1]);
            route.coordinator.handleWarpAndIndoorBoundaries(route.link);
            assertFalse(route.transition.isActive());
            assertFalse(route.scroll.isActive());
        }
        route.link.setPixelPosition(-1, route.link.pixelY());
        route.coordinator.handleWarpAndIndoorBoundaries(route.link);
        assertTrue(route.scroll.isActive());
        assertEquals(ScrollController.LEFT, route.scroll.direction());
        while (route.scroll.isActive()) {
            route.scroll.tick(8);
        }
        assertEquals(Warp.CATEGORY_SIDESCROLL, route.session.mapCategory());
        assertEquals(0x3E, route.session.currentRoomId());
        assertEquals(1, route.session.activeRoom().warps().size());
        route.sideViewExit = route.session.activeRoom().firstWarp();
        assertEquals(Warp.CATEGORY_INDOOR, route.sideViewExit.category());
        assertEquals(0x01, route.sideViewExit.destMap());
        assertEquals(0x2C, route.sideViewExit.destRoom());
        assertEquals(0x78, route.sideViewExit.destX());
        assertEquals(0x70, route.sideViewExit.destY());
        route.sideViewReturnPath = reachableBoundaryPath(
                route.collision, route.link.pixelX(), route.link.pixelY(), ScrollController.RIGHT);
        assertNotNull(route.sideViewReturnPath, collisionGrid(route.collision));
        for (int[] position : route.sideViewReturnPath) {
            route.link.setPixelPosition(position[0], position[1]);
            route.coordinator.handleWarpAndIndoorBoundaries(route.link);
            assertFalse(route.transition.isActive());
            assertFalse(route.scroll.isActive());
        }
        route.link.setPixelPosition(RoomConstants.ROOM_PIXEL_WIDTH, route.link.pixelY());
        route.coordinator.handleWarpAndIndoorBoundaries(route.link);
        assertTrue(route.scroll.isActive());
        assertEquals(ScrollController.RIGHT, route.scroll.direction());
        while (route.scroll.isActive()) {
            route.scroll.tick(8);
        }
        assertEquals(0x3F, route.session.currentRoomId());
        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.LEFT);
        assertEquals(0x3E, route.session.currentRoomId());
        route.sideViewWarpPath =
                reachableBoundaryPath(route.collision, route.link.pixelX(), route.link.pixelY(), ScrollController.UP);
        assertNotNull(route.sideViewWarpPath, collisionGrid(route.collision));
        for (int[] position : route.sideViewWarpPath) {
            route.link.setPixelPosition(position[0], position[1]);
            route.coordinator.handleWarpAndIndoorBoundaries(route.link);
            assertFalse(route.transition.isActive());
        }
        route.link.setPixelPosition(route.link.pixelX(), -5);
        route.coordinator.handleWarpAndIndoorBoundaries(route.link);
        assertTrue(route.transition.isActive());
        while (route.transition.isActive()) {
            route.transition.tick();
        }
        assertEquals(Warp.CATEGORY_INDOOR, route.session.mapCategory());
        assertEquals(0x2C, route.session.currentRoomId());
        assertEquals(0x78, route.link.romEntityX());
        assertEquals(0x70, route.link.romEntityY());
        assertEquals(0, route.session.context.roomSessionViews.activeRoomEventForTest());
        route.room2CKeese = route.session.activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x19)
                .findFirst()
                .orElseThrow();
        assertEquals(0x28, route.room2CKeese.x());
        assertEquals(0x30, route.room2CKeese.y());
        route.room2CSpark = route.session.activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x16)
                .findFirst()
                .orElseThrow();
        assertEquals(0x18, route.room2CSpark.x());
        assertEquals(0x30, route.room2CSpark.y());
        route.room2CFloatingItem = route.session.activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0xE5)
                .findFirst()
                .orElseThrow();
        assertEquals(0x48, route.room2CFloatingItem.x());
        assertEquals(0x50, route.room2CFloatingItem.y());

        // Room-$28's source-authored south entry is the D2 ledge at column 7;
        // columns 4/5 are the solid $45/$46 objects. Reach that X in the
        // preceding room before crossing the shared north boundary.
        route.room28EntryColumnPath =
                reachablePositionPath(route.collision, route.link.pixelX(), route.link.pixelY(), (x, y) -> x == 0x70);
        assertNotNull(
                route.room28EntryColumnPath,
                "Room-$2C cannot reach the room-$28 entry column\n" + collisionGrid(route.collision));
        for (int[] position : route.room28EntryColumnPath) {
            route.link.setPixelPosition(position[0], position[1]);
            route.coordinator.handleWarpAndIndoorBoundaries(route.link);
            assertFalse(route.transition.isActive());
            assertFalse(route.scroll.isActive());
        }
        assertEquals(0x70, route.link.pixelX(), "entry-column path must end at room-$28 column 7");
        // Cross only after reaching that aligned X; the generic helper would
        // choose the first reachable top-edge column and lose this alignment.
        route.link.setPixelPosition(route.link.pixelX(), -1);
        route.coordinator.handleWarpAndIndoorBoundaries(route.link);
        assertTrue(route.scroll.isActive());
        assertEquals(ScrollController.UP, route.scroll.direction());
        assertEquals(0x70, route.link.pixelX());
        assertEquals(RoomConstants.ROOM_PIXEL_HEIGHT - Link.SPRITE_SIZE, route.link.pixelY());
        while (route.scroll.isActive()) {
            route.scroll.tick(8);
        }
        assertEquals(Warp.CATEGORY_INDOOR, route.session.mapCategory());
        assertEquals(0x28, route.session.currentRoomId());
        assertEquals(0xC1, route.session.context.roomSessionViews.activeRoomEventForTest());
        route.hinox = route.session.activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x89)
                .findFirst()
                .orElseThrow();
        assertEquals(0x58, route.hinox.x());
        assertEquals(0x30, route.hinox.y());
        route.unresolvedHinoxWarp = route.session.activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x61)
                .findFirst()
                .orElseThrow();
        assertEquals(0x50, route.unresolvedHinoxWarp.x());
        assertEquals(0x48, route.unresolvedHinoxWarp.y());
        assertFalse(route.session.activeRoom().hasWarps());
        route.coordinator.handleWarpAndIndoorBoundaries(route.link);
        assertFalse(route.transition.isActive());

        // The ROM's entity-$61 warp is present but inert until the Hinox's
        // ordinary miniboss clear path records room status bit $20.
        route.hinoxSlot = route.hinox.slot();
        route.hinoxSourceLoadOrder = route.hinox.sourceLoadOrder();
        // The source leaves Link on the south D2 ledge. Holding UP drives the
        // real 12-contact directional ledge jump (bank2.asm:7053-7127).
        setDirectionalInput(route.inputState, route.inputConfig, 0x04, GLFW_PRESS);
        route.room28LedgeJumpObserved = false;
        route.room28LedgeJumpLanded = false;
        for (int ledgeFrame = 0; ledgeFrame < 0x80; ledgeFrame++) {
            route.link.update();
            route.room28LedgeJumpObserved |= route.link.isAirborne();
            if (route.room28LedgeJumpObserved && !route.link.isAirborne()) {
                route.room28LedgeJumpLanded = true;
                break;
            }
            route.coordinator.handleWarpAndIndoorBoundaries(route.link);
            assertFalse(route.transition.isActive());
            assertFalse(route.scroll.isActive());
        }
    }
}
