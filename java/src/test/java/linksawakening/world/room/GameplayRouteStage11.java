package linksawakening.world.room;

import static linksawakening.world.room.RoomTransitionTestSupport.*;
import static linksawakening.world.room.RoomTransitionTestSupport2.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import linksawakening.entity.Link;
import linksawakening.equipment.Sword;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.event.ChestRewardEvent;
import linksawakening.world.transition.ScrollController;
import linksawakening.world.transition.Warp;

final class GameplayRouteStage11 {
    static void run(GameplayRouteState route) throws Exception {

        while (route.session
                        .rooms()
                        .activeRoom()
                        .entities()
                        .slots()
                        .get(route.room27PolsVoice.slot())
                        .loaded()
                && route.frame < route.polsDeathDeadline) {
            tickInteractiveEntities(route.session, route.frame++, route.link.romEntityX(), route.link.romEntityY());
        }
        assertFalse(route.session
                .rooms()
                .activeRoom()
                .entities()
                .slots()
                .get(route.room27PolsVoice.slot())
                .loaded());
        assertEquals(0x66, route.session.components.roomSessionViews().activeRoomEventForTest());

        for (RoomEntity orderedEnemy : new RoomEntity[] {route.room27Keese, route.room27Moblin}) {
            int enemySlot = orderedEnemy.slot();
            int attackDeadline = route.frame + 0x200;
            while (route.session
                                    .rooms()
                                    .activeRoom()
                                    .entities()
                                    .slots()
                                    .get(enemySlot)
                                    .status()
                            == EntityStatus.ACTIVE
                    && route.frame < attackDeadline) {
                RoomEntity target =
                        route.session.rooms().activeRoom().entities().slots().get(enemySlot);
                route.session
                        .entities()
                        .resolveEntityCombat(new EntityCombatInputs(
                                route.frame++,
                                0,
                                0,
                                false,
                                0,
                                true,
                                new SwordBox(true, target.x() - 0x08, 0x10, target.y() - 0x08, 0x10),
                                linksawakening.world.entity.EnemyAttackContext.standard()));
                for (int cooldown = 0;
                        cooldown < 0x0B
                                && route.session
                                                .rooms()
                                                .activeRoom()
                                                .entities()
                                                .slots()
                                                .get(enemySlot)
                                                .status()
                                        == EntityStatus.ACTIVE;
                        cooldown++) {
                    tickInteractiveEntities(route.session, route.frame++, 0, 0);
                }
            }
            assertEquals(
                    EntityStatus.DYING,
                    route.session
                            .rooms()
                            .activeRoom()
                            .entities()
                            .slots()
                            .get(enemySlot)
                            .status());
            int orderedEnemyDeathDeadline = route.frame + 0x80;
            while (route.session
                            .rooms()
                            .activeRoom()
                            .entities()
                            .slots()
                            .get(enemySlot)
                            .loaded()
                    && route.frame < orderedEnemyDeathDeadline) {
                tickInteractiveEntities(route.session, route.frame++, 0, 0);
            }
            assertFalse(route.session
                    .rooms()
                    .activeRoom()
                    .entities()
                    .slots()
                    .get(enemySlot)
                    .loaded());
        }

        route.room27ChestIndex = RoomConstants.ROOM_OBJECTS_BASE + 0x28;
        assertEquals(0, route.session.components.roomSessionViews().activeRoomEventForTest());
        route.room27ChestRevealDeadline = route.frame + 0x40;
        while (route.session.rooms().activeRoom().roomObjectsArea()[route.room27ChestIndex] != 0xA0
                && route.frame < route.room27ChestRevealDeadline) {
            tickInteractiveEntities(route.session, route.frame++, route.link.romEntityX(), route.link.romEntityY());
        }
        assertEquals(0xA0, route.session.rooms().activeRoom().roomObjectsArea()[route.room27ChestIndex]);

        route.link.setPixelPosition(0x80, 0x21);
        route.link.setDirection(Link.DIRECTION_UP);
        route.room27Chest = route.session
                .doors()
                .tryOpenChest(
                        route.link.pixelX(),
                        route.link.pixelY(),
                        linksawakening.state.Direction.fromJavaOrUnknown(Link.DIRECTION_UP),
                        true,
                        route.playerState.swordLevel());
        assertTrue(route.room27Chest.opened());
        assertEquals(ChestContentsTable.CHEST_NIGHTMARE_KEY, route.room27Chest.itemType());
        assertEquals(0x28, route.room27Chest.location());
        route.room27ChestSlot = route.room27Chest.entitySlot();
        route.room27DialogId = new ChestContentsTable(
                        (route.rom == null ? (linksawakening.rom.Rom) null : new linksawakening.rom.Rom(route.rom)))
                .dialogLowIdFor(
                        ChestContentsTable.CHEST_NIGHTMARE_KEY,
                        route.playerState.shieldLevel(),
                        route.playerState.swordLevel(),
                        route.playerState.powerBraceletLevel(),
                        0x01,
                        0x27);
        route.nightmareKeyRewardObserved = false;
        route.nightmareKeyDialogObserved = false;
        route.room27ChestEnded = false;
        for (int chestTick = 0; chestTick < 0x40; chestTick++) {
            tickInteractiveEntities(route.session, route.frame++, route.link.romEntityX(), route.link.romEntityY());
            for (ChestRewardEvent reward :
                    route.session.components.entityBridge().outbox.consumeChestRewardEvents()) {
                route.nightmareKeyRewardObserved |= reward.itemType() == ChestContentsTable.CHEST_NIGHTMARE_KEY;
                route.playerState.applyChestReward(reward.itemType());
            }
            route.nightmareKeyDialogObserved |= route.session.entityState().consumeEntityDialogRequests().stream()
                    .anyMatch(request -> request.globalDialogId() == route.room27DialogId);
            RoomEntity chestEntity =
                    route.session.rooms().activeRoom().entities().slots().get(route.room27ChestSlot);
            if (!chestEntity.loaded() || chestEntity.status() == EntityStatus.DISABLED) {
                route.room27ChestEnded = true;
                break;
            }
        }
        assertTrue(route.nightmareKeyRewardObserved);
        assertTrue(route.nightmareKeyDialogObserved);
        assertTrue(route.room27ChestEnded);
        assertEquals(
                1, route.session.dungeon().currentDungeonItemFlagsSnapshot()[DungeonItemState.NIGHTMARE_KEY_INDEX]);
        assertEquals(0xA1, objectAt(route.session, 0x28));
        assertEquals(0x10, route.session.components.roomSessionViews().indoorRoomStatusForTest(0x01, 0x27) & 0x10);

        route.collision.refreshLinkGroundInteraction(route.link.pixelX(), route.link.pixelY());
        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.UP);
        assertEquals(0x24, route.session.rooms().currentRoomId());
        assertEquals(
                1, route.session.dungeon().currentDungeonItemFlagsSnapshot()[DungeonItemState.NIGHTMARE_KEY_INDEX]);
        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.DOWN);
        assertEquals(0x27, route.session.rooms().currentRoomId());
        assertEquals(0xA1, objectAt(route.session, 0x28));
        assertEquals(
                0x66,
                route.session.components.roomSessionViews().activeRoomEventForTest(),
                "the source event byte reloads but status $10 keeps it inert");
        assertFalse(route.session.rooms().activeRoom().entities().loadedEntities().stream()
                .anyMatch(entity -> entity.sourceLoadOrder() >= 0 && entity.sourceLoadOrder() <= 2));
        assertTrue(route.session
                .components
                .entityBridge()
                .outbox
                .consumeChestRewardEvents()
                .isEmpty());
        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.UP);
        assertEquals(0x24, route.session.rooms().currentRoomId());

        assertEquals(1, route.session.dungeon().currentDungeonItemFlagsSnapshot()[DungeonItemState.SMALL_KEYS_INDEX]);
        tickInteractiveEntities(route.session, route.frame++, route.link.romEntityX(), route.link.romEntityY());
        route.room24CrystalSwitch = route.session.rooms().activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x66)
                .findFirst()
                .orElseThrow();
        route.room24SwitchApproach = reachableSwordContactPathWithFeather(
                route.collision, route.romTables, route.link.pixelX(), route.link.pixelY(), route.room24CrystalSwitch);
        assertNotNull(route.room24SwitchApproach, collisionGrid(route.collision));
        for (int[] position : route.room24SwitchApproach.path()) {
            route.link.setPixelPosition(position[0], position[1]);
        }
        route.link.setDirection(route.room24SwitchApproach.direction());
        route.room24Sword = new Sword(route.romTables, null);
        route.room24Sword.onPress();
        for (int swingFrame = 0; swingFrame < 4; swingFrame++) {
            route.room24Sword.tick(false);
        }
        route.initialRoom24SwordBox = route.room24Sword.enemyCollisionBox(
                route.link.romEntityX(), route.link.romSwordCollisionY(), route.link.direction());
        route.link.setPixelPosition(
                route.room24CrystalSwitch.x() - (route.initialRoom24SwordBox.x() - route.link.pixelX()),
                route.room24CrystalSwitch.y() - (route.initialRoom24SwordBox.y() - route.link.pixelY()));
        route.room24SwordBox = route.room24Sword.enemyCollisionBox(
                route.link.romEntityX(), route.link.romSwordCollisionY(), route.link.direction());
        route.room24SwitchHit = java.util.List.of();
        for (int contactFrame = 0;
                contactFrame < 2
                        && route.room24SwitchHit.stream()
                                .noneMatch(
                                        event -> event.slot() == route.room24CrystalSwitch.slot() && event.swordHit());
                contactFrame++) {
            route.room24SwitchHit = route.session
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
                                    route.room24SwordBox.x(),
                                    route.room24SwordBox.width(),
                                    route.room24SwordBox.y(),
                                    route.room24SwordBox.height()),
                            linksawakening.world.entity.EnemyAttackContext.standard()));
        }
        assertTrue(route.room24SwitchHit.stream()
                .anyMatch(event -> event.slot() == route.room24CrystalSwitch.slot() && event.swordHit()));
        tickInteractiveEntities(route.session, route.frame++, route.link.romEntityX(), route.link.romEntityY());
        while (route.session.components.roomSessionViews().switchableObjectAnimationStageForTest() != 0) {
            route.session.entityState().tickGameplayVBlank();
        }
        assertEquals(0x00, route.session.components.roomSessionViews().entitySwitchBlocksStateForTest());
        route.room24SwitchReachablePosition = route.room24SwitchApproach.path().getLast();
        route.link.setPixelPosition(route.room24SwitchReachablePosition[0], route.room24SwitchReachablePosition[1]);
        route.collision.refreshLinkGroundInteraction(route.link.pixelX(), route.link.pixelY());

        route.link.setPixelPosition(0x85, 0x38);
        assertTrue(route.collision.objectPhysicsFlagAtPoint(route.link.pixelX() + 0x0B, route.link.pixelY() + 0x09)
                        == 0x93
                || route.collision.objectPhysicsFlagAtPoint(route.link.pixelX() + 0x0B, route.link.pixelY() + 0x0C)
                        == 0x93);
        assertTrue(route.session
                .doors()
                .tryUnlockIndoorKeyDoor(
                        route.link.pixelX(),
                        route.link.pixelY(),
                        linksawakening.state.Direction.fromJavaOrUnknown(Link.DIRECTION_RIGHT),
                        0x08));
        assertEquals(0, route.session.dungeon().currentDungeonItemFlagsSnapshot()[DungeonItemState.SMALL_KEYS_INDEX]);
        for (int tick = 0; tick < 8; tick++) {
            route.session.entities().tickEntities(route.frame++, route.link.pixelX(), route.link.pixelY());
            assertTrue(route.session.components.entityBridge().consumeWorldLinkMotionBlockRequest());
        }
        route.session.entities().tickEntities(route.frame++, route.link.pixelX(), route.link.pixelY());
        assertFalse(route.session.components.entityBridge().consumeWorldLinkMotionBlockRequest());
        assertEquals(0x01, route.session.components.roomSessionViews().indoorRoomStatusForTest(0x01, 0x24) & 0x01);
        assertEquals(0x02, route.session.components.roomSessionViews().indoorRoomStatusForTest(0x01, 0x25) & 0x02);

        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.RIGHT);
        assertEquals(0x25, route.session.rooms().currentRoomId());
        assertEquals(0xA1, route.session.components.roomSessionViews().activeRoomEventForTest());
        assertEquals(
                2,
                route.session.rooms().activeRoom().entities().loadedEntities().stream()
                        .filter(entity -> entity.type() == 0x18)
                        .count());
        assertEquals(
                1,
                route.session.rooms().activeRoom().entities().loadedEntities().stream()
                        .filter(entity -> entity.type() == 0x1B)
                        .count());
        assertEquals(0x0D, objectAt(route.session, 0x18));
        tickInteractiveEntities(route.session, route.frame++, route.link.romEntityX(), route.link.romEntityY());

        route.room25PolsVoices = route.session.rooms().activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x18)
                .sorted(java.util.Comparator.comparingInt(RoomEntity::sourceLoadOrder))
                .toList();
        route.room25PotLocations = new int[] {0x11, 0x12};
        route.room25PotLinkX = new int[] {0x10, 0x20};
        for (int polsIndex = 0; polsIndex < route.room25PolsVoices.size(); polsIndex++) {
            RoomEntity polsVoice = route.room25PolsVoices.get(polsIndex);
            int potLocation = route.room25PotLocations[polsIndex];
            assertEquals(0x20, objectAt(route.session, potLocation));
            route.link.setPixelPosition(route.room25PotLinkX[polsIndex], 0x0B);
            route.link.setDirection(Link.DIRECTION_UP);
            for (int pullFrame = 0; pullFrame < 8; pullFrame++) {
                assertTrue(route.session
                        .doors()
                        .tryLiftIndoorObject(
                                route.link.pixelX(),
                                route.link.pixelY(),
                                linksawakening.state.Direction.fromJavaOrUnknown(Link.DIRECTION_UP),
                                route.playerState.powerBraceletLevel() != 0,
                                0x08));
            }
            assertEquals(0x0D, objectAt(route.session, potLocation));
            int liftDeadline = route.frame + 0x40;
            while (route.session.rooms().liftedEntityState().carryState() != 0x01 && route.frame < liftDeadline) {
                tickInteractiveEntities(route.session, route.frame++, route.link.romEntityX(), route.link.romEntityY());
            }
            assertEquals(0x01, route.session.rooms().liftedEntityState().carryState());

            polsVoice = route.session.rooms().activeRoom().entities().slots().get(polsVoice.slot());
            route.link.setPixelPosition(polsVoice.x() - 0x0B, polsVoice.y() - polsVoice.z() - 0x02);
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
            polsVoice = route.session.rooms().activeRoom().entities().slots().get(polsVoice.slot());
            assertTrue(route.session
                    .playerItems()
                    .throwLiftedEntity(linksawakening.state.Direction.fromJava(Link.DIRECTION_RIGHT)));
            while (((route.frame ^ polsVoice.slot()) & 1) != 0) {
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
                            .get(polsVoice.slot())
                            .status());
            int room25PolsDeathDeadline = route.frame + 0x80;
            while (route.session
                            .rooms()
                            .activeRoom()
                            .entities()
                            .slots()
                            .get(polsVoice.slot())
                            .loaded()
                    && route.frame < room25PolsDeathDeadline) {
                tickInteractiveEntities(route.session, route.frame++, route.link.romEntityX(), route.link.romEntityY());
            }
            assertFalse(route.session
                    .rooms()
                    .activeRoom()
                    .entities()
                    .slots()
                    .get(polsVoice.slot())
                    .loaded());
            assertEquals(0xA1, route.session.components.roomSessionViews().activeRoomEventForTest());
        }

        route.room25Zol = route.session.rooms().activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x1B)
                .findFirst()
                .orElseThrow();
        route.room25ZolDeadline = route.frame + 0x200;
        while (route.session
                                .rooms()
                                .activeRoom()
                                .entities()
                                .slots()
                                .get(route.room25Zol.slot())
                                .status()
                        == EntityStatus.ACTIVE
                && route.frame < route.room25ZolDeadline) {
            RoomEntity target =
                    route.session.rooms().activeRoom().entities().slots().get(route.room25Zol.slot());
            route.session
                    .entities()
                    .resolveEntityCombat(new EntityCombatInputs(
                            route.frame++,
                            0,
                            0,
                            false,
                            0,
                            true,
                            new SwordBox(true, target.x() - 0x08, 0x10, target.y() - 0x08, 0x10),
                            linksawakening.world.entity.EnemyAttackContext.standard()));
            for (int cooldown = 0;
                    cooldown < 0x0B
                            && route.session
                                            .rooms()
                                            .activeRoom()
                                            .entities()
                                            .slots()
                                            .get(route.room25Zol.slot())
                                            .status()
                                    == EntityStatus.ACTIVE;
                    cooldown++) {
                tickInteractiveEntities(route.session, route.frame++, 0, 0);
            }
        }
        assertEquals(
                EntityStatus.DYING,
                route.session
                        .rooms()
                        .activeRoom()
                        .entities()
                        .slots()
                        .get(route.room25Zol.slot())
                        .status());
        route.room25ZolDeathDeadline = route.frame + 0x80;
        while (route.session
                        .rooms()
                        .activeRoom()
                        .entities()
                        .slots()
                        .get(route.room25Zol.slot())
                        .loaded()
                && route.frame < route.room25ZolDeathDeadline) {
            tickInteractiveEntities(route.session, route.frame++, 0, 0);
        }
        assertFalse(route.session
                .rooms()
                .activeRoom()
                .entities()
                .slots()
                .get(route.room25Zol.slot())
                .loaded());

        // The Red Zol reuses its slot and spawns a second Gel (06:7C50).
        // Clearing the room requires defeating both descendants.
        route.gelDeadline = route.frame + 0x400;
        while (route.session.rooms().activeRoom().entities().loadedEntities().stream()
                        .anyMatch(entity -> entity.type() == 0x1C)
                && route.frame < route.gelDeadline) {
            for (RoomEntity gel : route.session.rooms().activeRoom().entities().loadedEntities().stream()
                    .filter(entity -> entity.type() == 0x1C && entity.status() == EntityStatus.ACTIVE)
                    .toList()) {
                route.session
                        .entities()
                        .resolveEntityCombat(new EntityCombatInputs(
                                route.frame++,
                                0,
                                0,
                                false,
                                0,
                                true,
                                new SwordBox(true, gel.x() - 8, 16, gel.y() - 8, 16),
                                linksawakening.world.entity.EnemyAttackContext.standard()));
            }
            tickInteractiveEntities(route.session, route.frame++, 0, 0);
        }

        route.room25StairRevealDeadline = route.frame + 0x40;
        while (route.session.components.roomSessionViews().activeRoomEventForTest() != 0
                && route.frame < route.room25StairRevealDeadline) {
            tickInteractiveEntities(route.session, route.frame++, route.link.romEntityX(), route.link.romEntityY());
        }
        assertEquals(0, route.session.components.roomSessionViews().activeRoomEventForTest());
        while (objectAt(route.session, 0x18) != 0xBE && route.frame < route.room25StairRevealDeadline) {
            tickInteractiveEntities(route.session, route.frame++, route.link.romEntityX(), route.link.romEntityY());
        }
        assertEquals(0xBE, objectAt(route.session, 0x18));
        assertEquals(0x10, route.session.components.roomSessionViews().indoorRoomStatusForTest(0x01, 0x25) & 0x10);

        route.coordinator.handleWarpAndIndoorBoundaries(route.link);
        assertFalse(route.transition.isActive());
        route.link.setPixelPosition(0x80, 0x10);
        route.coordinator.handleWarpAndIndoorBoundaries(route.link);
        assertTrue(route.transition.isActive());
        while (route.transition.isActive()) {
            route.transition.tick();
        }
        assertEquals(Warp.CATEGORY_SIDESCROLL, route.session.rooms().mapCategory());
        assertEquals(0x01, route.session.rooms().activeRoom().mapId());
        assertEquals(0x3B, route.session.rooms().currentRoomId());
        assertEquals(0x88, route.link.romEntityX());
        assertEquals(0x10, route.link.romEntityY());
        assertEquals(
                1,
                route.session.rooms().activeRoom().entities().loadedEntities().stream()
                        .filter(entity -> entity.type() == 0xA5)
                        .count());
        assertEquals(
                2,
                route.session.rooms().activeRoom().entities().loadedEntities().stream()
                        .filter(entity -> entity.type() == 0xD6)
                        .count());

        route.room3BPlatform = route.session.rooms().activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0xA5)
                .findFirst()
                .orElseThrow();
        route.room3BPots = route.session.rooms().activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0xD6)
                .sorted(java.util.Comparator.comparingInt(RoomEntity::sourceLoadOrder))
                .toList();
        assertEquals(0x38, route.room3BPlatform.x());
        assertEquals(0x50, route.room3BPlatform.y());
        assertEquals(0x68, route.room3BPots.getFirst().x());
        assertEquals(0x70, route.room3BPots.getFirst().y());
        // The entity macro is (vertical, horizontal); the authored second
        // record is (6,7), hence ROM position $78/$70.
        assertEquals(0x78, route.room3BPots.get(1).x());
        assertEquals(0x70, route.room3BPots.get(1).y());

        route.session.components.entityInputs().setEntityInventorySlots(0x00, 0x03);
        route.session.entityState().setEntityPowerBraceletButtonHeld(true);
        route.session.entityState().setEntityActionButtonsHeld(false, true);
        route.session.entityState().setEntityAttackStepAnimationCountdown(0);

        // Main's live ordering: Link moves first, AnimateEntities consumes the
        // captured final X, then typed Link writes are applied before the next
        // Link update. Start at the authored A5 contact window.
        tickLiveRoom3B(route.session, route.link, route.coordinator, route.transition, route.scroll, route.frame++);
        route.link.setPixelPosition(route.room3BPlatform.x(), route.room3BPlatform.y() - 0x20);
        route.room3BPlatformEvents = tickLiveRoom3B(
                route.session, route.link, route.coordinator, route.transition, route.scroll, route.frame++);
        assertTrue(route.link.standingOnSideViewEntity());
        assertFalse(route.transition.isActive());
        assertFalse(route.scroll.isActive());
        assertTrue(route.room3BPlatformEvents.stream()
                .noneMatch(event -> event.slot() == route.room3BPlatform.slot() && event.linkDamage() != 0));
        route.firstPlatformY = route.session
                .rooms()
                .activeRoom()
                .entities()
                .slots()
                .get(route.room3BPlatform.slot())
                .y();
        for (int rideFrame = 0; rideFrame < 16; rideFrame++) {
            tickLiveRoom3B(route.session, route.link, route.coordinator, route.transition, route.scroll, route.frame++);
        }
    }
}
