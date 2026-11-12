package linksawakening.world.room;

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
import linksawakening.world.entity.motion.BooBuddyMotion;
import linksawakening.world.transition.ScrollController;

final class GameplayRouteStage10 {
    static void run(GameplayRouteState route) throws Exception {

        for (int booSlot : route.booSlots) {
            boolean booHit = false;
            int booDeadline = route.frame + 0x100;
            while (route.frame < booDeadline
                    && route.session
                                    .rooms()
                                    .activeRoom()
                                    .entities()
                                    .slots()
                                    .get(booSlot)
                                    .status()
                            != EntityStatus.DISABLED) {
                RoomEntity boo =
                        route.session.rooms().activeRoom().entities().slots().get(booSlot);
                SwordApproach approach = reachableSwordContactPath(
                        route.collision, route.link.pixelX(), route.link.pixelY(), boo, route.romTables);
                if (approach != null) {
                    for (int[] position : approach.path()) {
                        route.link.setPixelPosition(position[0], position[1]);
                    }
                    route.link.setDirection(approach.direction());
                    Sword booSword = new Sword(route.romTables, null);
                    booSword.onPress();
                    for (int swingFrame = 0; swingFrame < 4; swingFrame++) {
                        booSword.tick(false);
                    }
                    Sword.CollisionBox box = booSword.enemyCollisionBox(
                            route.link.romEntityX(), route.link.romSwordCollisionY(), route.link.direction());
                    if (box.active()
                            && linksawakening.world.entity.EntityCombatTestAccess.overlapsSword(
                                    boo, box.x(), box.width(), box.y(), box.height())) {
                        List<EntityCombatEvent> hit = route.session
                                .entities()
                                .resolveEntityCombat(new EntityCombatInputs(
                                        route.frame++,
                                        route.link.romEntityX(),
                                        route.link.romEntityY(),
                                        false,
                                        0,
                                        true,
                                        new SwordBox(true, box.x(), box.width(), box.y(), box.height()),
                                        linksawakening.world.entity.EnemyAttackContext.standard()));
                        booHit |= hit.stream()
                                .anyMatch(event ->
                                        event.slot() == booSlot && event.swordHit() && event.enemyDamage() > 0);
                    }
                }
                tickInteractiveEntities(route.session, route.frame++, route.link.romEntityX(), route.link.romEntityY());
            }
            assertTrue(booHit, "room $20 Boo Buddy must be defeated by live sword combat");
            assertEquals(
                    EntityStatus.DISABLED,
                    route.session
                            .rooms()
                            .activeRoom()
                            .entities()
                            .slots()
                            .get(booSlot)
                            .status());
        }

        route.braceletRevealDeadline = route.frame + 0x40;
        while (objectAt(route.session, 0x28) != 0xA0 && route.frame < route.braceletRevealDeadline) {
            tickInteractiveEntities(route.session, route.frame++, route.link.romEntityX(), route.link.romEntityY());
        }
        assertEquals(0, route.session.components.roomSessionViews().activeRoomEventForTest());
        assertEquals(0xA0, objectAt(route.session, 0x28));

        route.braceletChestPath = reachablePositionPath(
                route.collision,
                route.link.pixelX(),
                route.link.pixelY(),
                (x, y) -> Math.floorDiv(x + 0x08, 0x10) == 0x08 && Math.floorDiv(y + 0x05, 0x10) == 0x02);
        assertNotNull(route.braceletChestPath, collisionGrid(route.collision));
        for (int[] position : route.braceletChestPath) {
            route.link.setPixelPosition(position[0], position[1]);
        }
        route.link.setDirection(Link.DIRECTION_UP);
        route.braceletChest = route.session
                .doors()
                .tryOpenChest(
                        route.link.pixelX(),
                        route.link.pixelY(),
                        Link.DIRECTION_UP,
                        true,
                        route.playerState.swordLevel());
        assertTrue(route.braceletChest.opened());
        assertEquals(ChestContentsTable.CHEST_POWER_BRACELET, route.braceletChest.itemType());
        route.braceletChestSlot = route.braceletChest.entitySlot();
        route.braceletRewardObserved = false;
        route.braceletChestEnded = false;
        for (int chestTick = 0; chestTick < 0x40; chestTick++) {
            route.session.entities().tickEntities(route.frame++, route.link.romEntityX(), route.link.romEntityY());
            for (ChestRewardEvent reward :
                    route.session.components.entityBridge().outbox.consumeChestRewardEvents()) {
                route.braceletRewardObserved |= reward.itemType() == ChestContentsTable.CHEST_POWER_BRACELET;
                route.playerState.applyChestReward(reward.itemType());
            }
            RoomEntity chestEntity =
                    route.session.rooms().activeRoom().entities().slots().get(route.braceletChestSlot);
            if (!chestEntity.loaded() || chestEntity.status() == EntityStatus.DISABLED) {
                route.braceletChestEnded = true;
                break;
            }
        }
        assertTrue(route.braceletRewardObserved);
        assertTrue(route.braceletChestEnded);
        assertEquals(1, route.playerState.powerBraceletLevel());
        assertEquals(0x10, route.session.components.roomSessionViews().indoorRoomStatusForTest(0x01, 0x20) & 0x10);

        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.RIGHT);
        assertEquals(0x21, route.session.rooms().currentRoomId());
        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.LEFT);
        assertEquals(0x20, route.session.rooms().currentRoomId());
        for (int reloadTick = 0; reloadTick < 0x10; reloadTick++) {
            tickInteractiveEntities(route.session, route.frame++, route.link.romEntityX(), route.link.romEntityY());
        }
        assertEquals(0xA1, objectAt(route.session, 0x28));
        assertEquals(
                0x61,
                route.session.components.roomSessionViews().activeRoomEventForTest(),
                "the source event byte reloads but is inert once status bit $10 is set");
        assertFalse(route.session.rooms().activeRoom().entities().loadedEntities().stream()
                .anyMatch(entity -> entity.type() == BooBuddyMotion.ENTITY_TYPE));
        assertTrue(route.session
                .components
                .entityBridge()
                .outbox
                .consumeChestRewardEvents()
                .isEmpty());

        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.RIGHT);
        assertEquals(0x21, route.session.rooms().currentRoomId());
        assertEquals(0x00, route.session.components.roomSessionViews().entitySwitchBlocksStateForTest());

        route.room21PotRoute = new int[][] {
            {0x60, 0x29, Link.DIRECTION_UP, 0x01, 0x08, 0x26},
            {0x60, 0x19, Link.DIRECTION_UP, 0x01, 0x08, 0x16},
            {0x64, 0x10, Link.DIRECTION_RIGHT, 0x08, 0x02, 0x17},
            {0x74, 0x10, Link.DIRECTION_RIGHT, 0x08, 0x02, 0x18},
            {0x80, 0x11, Link.DIRECTION_DOWN, 0x02, 0x04, 0x28}
        };
        for (int potIndex = 0; potIndex < route.room21PotRoute.length; potIndex++) {
            int[] lift = route.room21PotRoute[potIndex];
            List<int[]> room21PotPath = reachablePositionPath(
                    route.collision, route.link.pixelX(), route.link.pixelY(), (x, y) -> x == lift[0] && y == lift[1]);
            assertNotNull(
                    room21PotPath,
                    "source pot $" + Integer.toHexString(lift[5]) + " must be reachable in room $21\n"
                            + collisionGrid(route.collision));
            for (int[] position : room21PotPath) {
                route.link.setPixelPosition(position[0], position[1]);
            }
            route.link.setDirection(lift[2]);
            for (int pullFrame = 0; pullFrame < 8; pullFrame++) {
                assertTrue(
                        route.session
                                .doors()
                                .tryLiftIndoorObject(
                                        route.link.pixelX(),
                                        route.link.pixelY(),
                                        lift[2],
                                        route.playerState.powerBraceletLevel() != 0,
                                        lift[4]),
                        "pot $" + Integer.toHexString(lift[5]) + " pull " + pullFrame);
            }
            assertEquals(0x0D, objectAt(route.session, lift[5]));
            int liftDeadline = route.frame + 0x40;
            while (route.session.rooms().liftedEntityState().carryState() != 0x01 && route.frame < liftDeadline) {
                tickInteractiveEntities(route.session, route.frame++, route.link.romEntityX(), route.link.romEntityY());
            }
            assertEquals(0x01, route.session.rooms().liftedEntityState().carryState());
            assertTrue(route.session.playerItems().throwLiftedEntity(route.link.direction()));
        }

        route.room21EastDoorPath = reachablePositionPath(
                route.collision,
                route.link.pixelX(),
                route.link.pixelY(),
                (x, y) -> x == RoomConstants.ROOM_PIXEL_WIDTH - Link.SPRITE_SIZE && y >= 0x19 && y <= 0x28);
        assertNotNull(route.room21EastDoorPath, collisionGrid(route.collision));
        for (int[] position : route.room21EastDoorPath) {
            route.link.setPixelPosition(position[0], position[1]);
        }
        route.link.setPixelPosition(RoomConstants.ROOM_PIXEL_WIDTH, route.link.pixelY());
        route.coordinator.handleWarpAndIndoorBoundaries(route.link);
        assertTrue(route.scroll.isActive());
        assertEquals(ScrollController.RIGHT, route.scroll.direction());
        while (route.scroll.isActive()) {
            route.scroll.tick(8);
        }
        assertEquals(0x22, route.session.rooms().currentRoomId());
        assertEquals(0x00, route.session.components.roomSessionViews().entitySwitchBlocksStateForTest());
        assertEquals(0x00, route.session.rooms().liftedEntityState().carryState());

        assertEquals(0x00, route.session.components.roomSessionViews().activeRoomEventForTest());
        assertEquals(0xA0, objectAt(route.session, 0x27));
        for (int location : new int[] {0x11, 0x21, 0x31}) {
            assertEquals(0xDB, objectAt(route.session, location));
        }
        assertEquals(0xDC, objectAt(route.session, 0x34));
        for (int location : new int[] {0x51, 0x52, 0x57, 0x58}) {
            assertEquals(0xDC, objectAt(route.session, location));
        }
        for (int location = 0x63; location <= 0x66; location++) {
            assertEquals(0xDC, objectAt(route.session, location));
        }
        for (int location = 0x53; location <= 0x56; location++) {
            assertEquals(0x20, objectAt(route.session, location));
        }
        route.room22CrystalSwitch = route.session.rooms().activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x66)
                .findFirst()
                .orElseThrow();
        assertEquals(0x48, route.room22CrystalSwitch.x());
        assertEquals(0x30, route.room22CrystalSwitch.y());
        route.room22Hearts = route.session.rooms().activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x2D)
                .toList();
        assertEquals(4, route.room22Hearts.size());
        assertTrue(route.room22Hearts.stream().allMatch(entity -> entity.y() == 0x60));
        assertEquals(
                java.util.Set.of(0x38, 0x48, 0x58, 0x68),
                route.room22Hearts.stream().map(RoomEntity::x).collect(java.util.stream.Collectors.toSet()));
        assertEquals(
                ChestContentsTable.CHEST_SMALL_KEY,
                new ChestContentsTable(route.rom).itemForSpawn(0x01, 0x22, route.playerState.swordLevel(), true));
        tickInteractiveEntities(route.session, route.frame++, route.link.romEntityX(), route.link.romEntityY());

        route.room22SwitchApproach = reachableSwordContactPathWithFeather(
                route.collision, route.romTables, route.link.pixelX(), route.link.pixelY(), route.room22CrystalSwitch);
        assertNotNull(route.room22SwitchApproach, collisionGrid(route.collision));
        for (int[] position : route.room22SwitchApproach.path()) {
            route.link.setPixelPosition(position[0], position[1]);
        }
        route.link.setDirection(route.room22SwitchApproach.direction());
        route.room22Sword = new Sword(route.romTables, null);
        route.room22Sword.onPress();
        for (int swingFrame = 0; swingFrame < 4; swingFrame++) {
            route.room22Sword.tick(false);
        }
        route.initialRoom22SwordBox = route.room22Sword.enemyCollisionBox(
                route.link.romEntityX(), route.link.romSwordCollisionY(), route.link.direction());
        route.link.setPixelPosition(
                route.room22CrystalSwitch.x() - (route.initialRoom22SwordBox.x() - route.link.pixelX()),
                route.room22CrystalSwitch.y() - (route.initialRoom22SwordBox.y() - route.link.pixelY()));
        route.room22SwordBox = route.room22Sword.enemyCollisionBox(
                route.link.romEntityX(), route.link.romSwordCollisionY(), route.link.direction());
        route.room22SwitchHit = java.util.List.of();
        for (int contactFrame = 0;
                contactFrame < 2
                        && route.room22SwitchHit.stream()
                                .noneMatch(
                                        event -> event.slot() == route.room22CrystalSwitch.slot() && event.swordHit());
                contactFrame++) {
            route.room22SwitchHit = route.session
                    .entities()
                    .resolveEntityCombat(new EntityCombatInputs(
                            route.frame++,
                            route.link.romEntityX(),
                            route.link.romEntityY(),
                            false,
                            0,
                            true,
                            new SwordBox(
                                    true,
                                    route.room22SwordBox.x(),
                                    route.room22SwordBox.width(),
                                    route.room22SwordBox.y(),
                                    route.room22SwordBox.height()),
                            linksawakening.world.entity.EnemyAttackContext.standard()));
        }
        assertTrue(route.room22SwitchHit.stream()
                .anyMatch(event -> event.slot() == route.room22CrystalSwitch.slot() && event.swordHit()));
        tickInteractiveEntities(route.session, route.frame++, route.link.romEntityX(), route.link.romEntityY());
        while (route.session.components.roomSessionViews().switchableObjectAnimationStageForTest() != 0) {
            route.session.entityState().tickGameplayVBlank();
        }
        assertEquals(0x02, route.session.components.roomSessionViews().entitySwitchBlocksStateForTest());
        route.collision.refreshLinkGroundInteraction(route.link.pixelX(), route.link.pixelY());

        route.room22ChestPath = reachablePositionPathWithFeather(
                route.collision,
                route.romTables,
                route.link.pixelX(),
                route.link.pixelY(),
                (x, y) -> Math.floorDiv(x + 0x08, 0x10) == 0x07 && Math.floorDiv(y + 0x05, 0x10) == 0x02);
        assertNotNull(route.room22ChestPath, collisionGrid(route.collision));
        for (int[] position : route.room22ChestPath) {
            route.link.setPixelPosition(position[0], position[1]);
        }
        route.link.setDirection(Link.DIRECTION_UP);
        route.room22Chest = route.session
                .doors()
                .tryOpenChest(
                        route.link.pixelX(),
                        route.link.pixelY(),
                        Link.DIRECTION_UP,
                        true,
                        route.playerState.swordLevel());
        assertTrue(route.room22Chest.opened());
        assertEquals(ChestContentsTable.CHEST_SMALL_KEY, route.room22Chest.itemType());
        assertEquals(0x27, route.room22Chest.location());
        route.room22ChestSlot = route.room22Chest.entitySlot();
        route.room22DialogId = new ChestContentsTable(route.rom)
                .dialogLowIdFor(
                        ChestContentsTable.CHEST_SMALL_KEY,
                        route.playerState.shieldLevel(),
                        route.playerState.swordLevel(),
                        route.playerState.powerBraceletLevel(),
                        0x01,
                        0x22);
        route.room22RewardObserved = false;
        route.room22DialogObserved = false;
        route.room22ChestEnded = false;
        for (int chestTick = 0; chestTick < 0x40; chestTick++) {
            tickInteractiveEntities(route.session, route.frame++, route.link.romEntityX(), route.link.romEntityY());
            for (ChestRewardEvent reward :
                    route.session.components.entityBridge().outbox.consumeChestRewardEvents()) {
                route.room22RewardObserved |= reward.itemType() == ChestContentsTable.CHEST_SMALL_KEY;
                route.playerState.applyChestReward(reward.itemType());
            }
            route.room22DialogObserved |= route.session.entityState().consumeEntityDialogRequests().stream()
                    .anyMatch(request -> request.globalDialogId() == route.room22DialogId);
            RoomEntity chestEntity =
                    route.session.rooms().activeRoom().entities().slots().get(route.room22ChestSlot);
            if (!chestEntity.loaded() || chestEntity.status() == EntityStatus.DISABLED) {
                route.room22ChestEnded = true;
                break;
            }
        }
        assertTrue(route.room22RewardObserved);
        assertTrue(route.room22DialogObserved);
        assertTrue(route.room22ChestEnded);
        assertEquals(1, route.session.dungeon().currentDungeonItemFlagsSnapshot()[DungeonItemState.SMALL_KEYS_INDEX]);
        assertEquals(0xA1, objectAt(route.session, 0x27));
        assertEquals(0x10, route.session.components.roomSessionViews().indoorRoomStatusForTest(0x01, 0x22) & 0x10);
        route.collision.refreshLinkGroundInteraction(route.link.pixelX(), route.link.pixelY());

        assertEquals(0x02, route.session.components.roomSessionViews().entitySwitchBlocksStateForTest());

        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.RIGHT);
        assertEquals(0x23, route.session.rooms().currentRoomId());
        assertEquals(0x00, route.session.components.roomSessionViews().activeRoomEventForTest());
        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.RIGHT);
        assertEquals(0x24, route.session.rooms().currentRoomId());
        assertEquals(0x00, route.session.components.roomSessionViews().activeRoomEventForTest());
        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.DOWN);
        assertEquals(0x27, route.session.rooms().currentRoomId());
        assertEquals(0x66, route.session.components.roomSessionViews().activeRoomEventForTest());

        route.room27PolsVoice = route.session.rooms().activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.sourceLoadOrder() == 0 && entity.type() == 0x18)
                .findFirst()
                .orElseThrow();
        route.room27Keese = route.session.rooms().activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.sourceLoadOrder() == 1 && entity.type() == 0x19)
                .findFirst()
                .orElseThrow();
        route.room27Moblin = route.session.rooms().activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.sourceLoadOrder() == 2 && entity.type() == 0x0B)
                .findFirst()
                .orElseThrow();
        assertEquals(0x64, ((route.room27PolsVoice.y() - 0x10) & 0xF0) | ((route.room27PolsVoice.x() - 0x08) >>> 4));
        assertEquals(0x67, ((route.room27Keese.y() - 0x10) & 0xF0) | ((route.room27Keese.x() - 0x08) >>> 4));
        assertEquals(0x33, ((route.room27Moblin.y() - 0x10) & 0xF0) | ((route.room27Moblin.x() - 0x08) >>> 4));
        assertEquals(0x20, objectAt(route.session, 0x22));

        route.link.setPixelPosition(0x20, 0x1B);
        route.link.setDirection(Link.DIRECTION_UP);
        for (int pullFrame = 0; pullFrame < 8; pullFrame++) {
            assertTrue(route.session
                    .doors()
                    .tryLiftIndoorObject(
                            route.link.pixelX(),
                            route.link.pixelY(),
                            Link.DIRECTION_UP,
                            route.playerState.powerBraceletLevel() != 0,
                            0x08));
        }
        assertEquals(0x0D, objectAt(route.session, 0x22));
        route.room27LiftDeadline = route.frame + 0x40;
        while (route.session.rooms().liftedEntityState().carryState() != 0x01
                && route.frame < route.room27LiftDeadline) {
            tickInteractiveEntities(route.session, route.frame++, route.link.romEntityX(), route.link.romEntityY());
        }
        assertEquals(0x01, route.session.rooms().liftedEntityState().carryState());

        route.room27PolsVoice =
                route.session.rooms().activeRoom().entities().slots().get(route.room27PolsVoice.slot());
        route.link.setPixelPosition(
                route.room27PolsVoice.x() - 0x0B, route.room27PolsVoice.y() - route.room27PolsVoice.z() - 0x02);
        route.link.setDirection(Link.DIRECTION_RIGHT);
        route.session
                .entities()
                .tickEntitiesWithProjectileEvents(
                        route.frame++,
                        route.link.romEntityX(),
                        route.link.romEntityY(),
                        0,
                        0,
                        Link.DIRECTION_RIGHT,
                        false);
        route.alignedPot = route.session
                .rooms()
                .activeRoom()
                .entities()
                .slots()
                .get(route.session.rooms().liftedEntityState().slot());
        route.room27PolsVoice =
                route.session.rooms().activeRoom().entities().slots().get(route.room27PolsVoice.slot());
        assertTrue(
                Math.abs(signedByteDelta(route.alignedPot.x(), route.room27PolsVoice.x())) < 0x0C
                        && Math.abs(signedByteDelta(
                                        route.alignedPot.y() - route.alignedPot.z(),
                                        route.room27PolsVoice.y() - route.room27PolsVoice.z()))
                                < 0x0C,
                "pot/pols alignment pot="
                        + String.format(
                                "%02X/%02X/%02X", route.alignedPot.x(), route.alignedPot.y(), route.alignedPot.z())
                        + " pols="
                        + String.format(
                                "%02X/%02X/%02X",
                                route.room27PolsVoice.x(), route.room27PolsVoice.y(), route.room27PolsVoice.z()));
        assertTrue(route.session.playerItems().throwLiftedEntity(Link.DIRECTION_RIGHT));
        while (((route.frame ^ route.room27PolsVoice.slot()) & 1) != 0) {
            route.frame++;
        }
        route.session
                .entities()
                .tickEntitiesWithProjectileEvents(
                        route.frame++,
                        route.link.romEntityX(),
                        route.link.romEntityY(),
                        0,
                        0,
                        Link.DIRECTION_RIGHT,
                        false);
        assertEquals(
                EntityStatus.DYING,
                route.session
                        .rooms()
                        .activeRoom()
                        .entities()
                        .slots()
                        .get(route.room27PolsVoice.slot())
                        .status());
        route.polsDeathDeadline = route.frame + 0x80;
    }
}
