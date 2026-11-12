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
import linksawakening.world.transition.ScrollController;

final class GameplayRouteStage7 {
    static void run(GameplayRouteState route) throws Exception {

        for (int keeseSlot : route.room30KeeseSlots) {
            RoomEntity keese =
                    route.session.rooms().activeRoom().entities().slots().get(keeseSlot);
            List<int[]> keesePath =
                    reachableEntityContactPath(route.collision, route.link.pixelX(), route.link.pixelY(), keese);
            assertNotNull(keesePath, collisionGrid(route.collision));
            for (int[] position : keesePath) {
                route.link.setPixelPosition(position[0], position[1]);
            }
            tickInteractiveEntities(route.session, route.frame++, route.link.romEntityX(), route.link.romEntityY());
            keese = route.session.rooms().activeRoom().entities().slots().get(keeseSlot);
            int keeseDeltaX = signedByteDelta(keese.x(), route.link.romEntityX());
            int keeseDeltaY = signedByteDelta(keese.y(), route.link.romEntityY());
            route.link.setDirection(
                    Math.abs(keeseDeltaX) >= Math.abs(keeseDeltaY)
                            ? keeseDeltaX < 0 ? Link.DIRECTION_LEFT : Link.DIRECTION_RIGHT
                            : keeseDeltaY < 0 ? Link.DIRECTION_UP : Link.DIRECTION_DOWN);
            Sword keeseSword = new Sword(route.romTables, null);
            keeseSword.onPress();
            for (int swingFrame = 0; swingFrame < 4; swingFrame++) {
                keeseSword.tick(false);
            }
            Sword.CollisionBox keeseSwordBox = keeseSword.enemyCollisionBox(
                    route.link.romEntityX(), route.link.romSwordCollisionY(), route.link.direction());
            assertTrue(
                    linksawakening.world.entity.EntityCombatTestAccess.overlapsSword(
                            keese, keeseSwordBox.x(), keeseSwordBox.width(), keeseSwordBox.y(), keeseSwordBox.height()),
                    "sword=" + keeseSwordBox + " keese=" + keese);
            List<EntityCombatEvent> keeseHit = route.session
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
                                    keeseSwordBox.x(),
                                    keeseSwordBox.width(),
                                    keeseSwordBox.y(),
                                    keeseSwordBox.height()),
                            linksawakening.world.entity.EnemyAttackContext.standard()));
            assertTrue(
                    keeseHit.stream()
                            .anyMatch(
                                    event -> event.slot() == keeseSlot && event.swordHit() && event.enemyDamage() > 0),
                    keeseHit.toString());
            for (int recovery = 0; recovery < 0x30; recovery++) {
                tickInteractiveEntities(route.session, route.frame++, route.link.romEntityX(), route.link.romEntityY());
            }
            int keeseDeathDeadline = route.frame + 0x200;
            while (route.session
                                    .rooms()
                                    .activeRoom()
                                    .entities()
                                    .slots()
                                    .get(keeseSlot)
                                    .status()
                            != EntityStatus.DISABLED
                    && route.frame < keeseDeathDeadline) {
                tickInteractiveEntities(route.session, route.frame++, route.link.romEntityX(), route.link.romEntityY());
            }
            assertEquals(
                    EntityStatus.DISABLED,
                    route.session
                            .rooms()
                            .activeRoom()
                            .entities()
                            .slots()
                            .get(keeseSlot)
                            .status());
            assertEquals(
                    4,
                    route.session.rooms().activeRoom().entities().loadedEntities().stream()
                            .filter(entity -> entity.type() == 0x27)
                            .count());
            if (route.session.rooms().activeRoom().entities().loadedEntities().stream()
                    .anyMatch(entity -> entity.type() == 0x19)) {
                assertEquals(0x21, RoomDebugFixture.of(route.session).activeRoomEventForTest());
            }
        }
        route.session.components.entityInputs().setEntityPressedButtonsMask(0);
        route.room30EventDeadline = route.frame + 0x300;
        while (RoomDebugFixture.of(route.session).activeRoomEventForTest() != 0
                && route.frame < route.room30EventDeadline) {
            tickInteractiveEntities(route.session, route.frame++, route.link.romEntityX(), route.link.romEntityY());
        }
        assertEquals(0, RoomDebugFixture.of(route.session).activeRoomEventForTest());
        assertEquals(0, route.session.rooms().activeRoom().shutterDoorMask() & 0x01);
        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.UP);
        assertEquals(0x2E, route.session.rooms().currentRoomId());
        assertEquals(0x21, RoomDebugFixture.of(route.session).activeRoomEventForTest());
        assertEquals(
                1,
                route.session.rooms().activeRoom().entities().loadedEntities().stream()
                        .filter(entity -> entity.type() == 0x20)
                        .count());
        assertEquals(0x0F, objectAt(route.session, 0x13));
        assertEquals(0x0F, objectAt(route.session, 0x23));
        assertEquals(0x0F, objectAt(route.session, 0x33));
        assertEquals(0xA0, objectAt(route.session, 0x24));

        route.hardhat = route.session.rooms().activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x20)
                .findFirst()
                .orElseThrow();
        tickInteractiveEntities(route.session, route.frame++, route.link.romEntityX(), route.link.romEntityY());
        route.hardhat = route.session.rooms().activeRoom().entities().slots().get(route.hardhat.slot());
        assertEquals(EntityStatus.ACTIVE, route.hardhat.status());
        assertEquals(0x21, RoomDebugFixture.of(route.session).activeRoomEventForTest());

        route.stoneBeakFeatherPath = reachablePositionPathWithFeather(
                route.collision,
                route.romTables,
                route.link.pixelX(),
                route.link.pixelY(),
                (x, y) -> Math.floorDiv(x + 0x08, 0x10) == (0x24 & 0x0F)
                        && Math.floorDiv(y + 0x05, 0x10) == ((0x24 & 0xF0) >>> 4));
        assertNotNull(
                route.stoneBeakFeatherPath,
                "No Feather-valid path across room $2E's trench\n" + collisionGrid(route.collision));
        assertTrue(walkCollisionPathWithFeather(
                        route.coordinator,
                        route.transition,
                        route.scroll,
                        route.collision,
                        route.link,
                        route.inputState,
                        route.inputConfig,
                        route.itemRegistry,
                        route.playerState,
                        route.stoneBeakFeatherPath)
                > 0);

        route.stoneBeakChestContactX = route.link.pixelX();
        route.stoneBeakChestContactY = route.link.pixelY();
        route.link.setPixelPosition(route.stoneBeakChestContactX, route.stoneBeakChestContactY - 1);
        route.stoneBeakChestInteractionLocation = (Math.floorDiv(route.link.pixelY() + 0x05, 0x10) << 4)
                | Math.floorDiv(route.link.pixelX() + 0x08, 0x10);
        assertEquals(0x24, route.stoneBeakChestInteractionLocation);
        route.stoneBeakChest = route.session
                .doors()
                .tryOpenChest(
                        route.link.pixelX(),
                        route.link.pixelY(),
                        linksawakening.state.Direction.fromJavaOrUnknown(Link.DIRECTION_UP),
                        true,
                        route.playerState.swordLevel());
        assertTrue(route.stoneBeakChest.opened());
        assertEquals(ChestContentsTable.CHEST_STONE_BEAK, route.stoneBeakChest.itemType());
        assertEquals(0x24, route.stoneBeakChest.location());
        assertEquals(0x10, RoomDebugFixture.of(route.session).indoorRoomStatusForTest(0x01, 0x2E) & 0x10);
        route.stoneBeakChestSlot = route.stoneBeakChest.entitySlot();
        route.stoneBeakDialogId = new ChestContentsTable(
                        (route.rom == null ? (linksawakening.rom.Rom) null : new linksawakening.rom.Rom(route.rom)))
                .dialogLowIdFor(
                        ChestContentsTable.CHEST_STONE_BEAK,
                        route.playerState.shieldLevel(),
                        route.playerState.swordLevel(),
                        route.playerState.powerBraceletLevel(),
                        0x01,
                        0x2E);
        route.stoneBeakRewardObserved = false;
        route.stoneBeakDialogObserved = false;
        route.stoneBeakChestEnded = false;
        for (int chestTick = 0; chestTick < 0x40; chestTick++) {
            route.session.entities().tickEntities(route.frame++, route.link.romEntityX(), route.link.romEntityY());
            for (ChestRewardEvent reward :
                    route.session.components.entityBridge().outbox.consumeChestRewardEvents()) {
                route.stoneBeakRewardObserved |= reward.itemType() == ChestContentsTable.CHEST_STONE_BEAK;
                route.playerState.applyChestReward(reward.itemType());
            }
            route.stoneBeakDialogObserved |= route.session.entityState().consumeEntityDialogRequests().stream()
                    .anyMatch(request -> request.globalDialogId() == route.stoneBeakDialogId);
            RoomEntity chestEntity =
                    route.session.rooms().activeRoom().entities().slots().get(route.stoneBeakChestSlot);
            if (!chestEntity.loaded() || chestEntity.status() == EntityStatus.DISABLED) {
                route.stoneBeakChestEnded = true;
                break;
            }
        }
        assertTrue(route.stoneBeakRewardObserved);
        assertTrue(route.stoneBeakDialogObserved);
        assertTrue(route.stoneBeakChestEnded);
        assertEquals(1, route.session.dungeon().currentDungeonItemFlagsSnapshot()[DungeonItemState.STONE_BEAK_INDEX]);
        assertEquals(0, route.session.dungeon().currentDungeonItemFlagsSnapshot()[DungeonItemState.SMALL_KEYS_INDEX]);

        // The chest does not solve room $2E's kill-all trigger. Knock its
        // surviving Beetle into the trench before using the south shutter.
        for (int hit = 0; hit < 64 && RoomDebugFixture.of(route.session).activeRoomEventForTest() != 0; hit++) {
            RoomEntity beetle =
                    route.session.rooms().activeRoom().entities().slots().get(route.hardhat.slot());
            int attackY = beetle.y() - 0x20;
            if (beetle.status() == EntityStatus.ACTIVE) {
                var beetleHits = route.session
                        .entities()
                        .resolveEntityCombat(new EntityCombatInputs(
                                beetle.slot() ^ 1,
                                beetle.x(),
                                attackY,
                                false,
                                0,
                                true,
                                new SwordBox(true, beetle.x(), 0x10, beetle.y(), 0x10),
                                linksawakening.world.entity.EnemyAttackContext.standard()));
                assertTrue(
                        beetleHits.stream().anyMatch(event -> event.swordHit()),
                        "Beetle hit " + hit + ": " + beetle + " " + beetleHits);
            }
            for (int recoil = 0; recoil < 0x0C; recoil++) {
                tickInteractiveEntities(route.session, route.frame++, beetle.x(), attackY);
            }
        }
        assertEquals(
                0,
                RoomDebugFixture.of(route.session).activeRoomEventForTest(),
                route.session.rooms().activeRoom().entities().loadedEntities().toString());

        // The post-Stone-Beak route backtracks south through room $30, then
        // east across the cleared $31/$32 shutters to the live switch in $33.
        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.DOWN);
        assertEquals(0x30, route.session.rooms().currentRoomId());
        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.RIGHT);
        assertEquals(0x31, route.session.rooms().currentRoomId());
        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.RIGHT);
        assertEquals(0x32, route.session.rooms().currentRoomId());
        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.RIGHT);
        assertEquals(0x33, route.session.rooms().currentRoomId());
        assertEquals(0x00, RoomDebugFixture.of(route.session).entitySwitchBlocksStateForTest());
        tickInteractiveEntities(route.session, route.frame++, route.link.romEntityX(), route.link.romEntityY());

        route.postStoneCrystalSwitch = route.session.rooms().activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x66)
                .findFirst()
                .orElseThrow();
        route.postStoneSwitchPath = reachableEntityContactPath(
                route.collision, route.link.pixelX(), route.link.pixelY(), route.postStoneCrystalSwitch);
        assertNotNull(route.postStoneSwitchPath, collisionGrid(route.collision));
        for (int[] position : route.postStoneSwitchPath) {
            route.link.setPixelPosition(position[0], position[1]);
        }
        route.postStoneSword = new Sword(route.romTables, null);
        route.postStoneSword.onPress();
        for (int swingFrame = 0; swingFrame < 4; swingFrame++) {
            route.postStoneSword.tick(false);
        }
        route.link.setDirection(Link.DIRECTION_RIGHT);
        route.postStoneInitialSwordBox = route.postStoneSword.enemyCollisionBox(
                route.link.romEntityX(), route.link.romSwordCollisionY(), route.link.direction());
        route.link.setPixelPosition(
                route.postStoneCrystalSwitch.x() - (route.postStoneInitialSwordBox.x() - route.link.pixelX()),
                route.postStoneCrystalSwitch.y() - (route.postStoneInitialSwordBox.y() - route.link.pixelY()));
        route.postStoneSwordBox = route.postStoneSword.enemyCollisionBox(
                route.link.romEntityX(), route.link.romSwordCollisionY(), route.link.direction());
        assertTrue(route.postStoneSwordBox.active());
        assertTrue(linksawakening.world.entity.EntityCombatTestAccess.overlapsSword(
                route.postStoneCrystalSwitch,
                route.postStoneSwordBox.x(),
                route.postStoneSwordBox.width(),
                route.postStoneSwordBox.y(),
                route.postStoneSwordBox.height()));
        route.postStoneSwitchHit = route.session
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
                                route.postStoneSwordBox.x(),
                                route.postStoneSwordBox.width(),
                                route.postStoneSwordBox.y(),
                                route.postStoneSwordBox.height()),
                        linksawakening.world.entity.EnemyAttackContext.standard()));
        route.postStoneCrystalSwitchSlot = route.postStoneCrystalSwitch.slot();
        assertTrue(
                route.postStoneSwitchHit.stream()
                        .anyMatch(event -> event.slot() == route.postStoneCrystalSwitchSlot
                                && event.type() == 0x66
                                && event.swordHit()),
                "events=" + route.postStoneSwitchHit
                        + " switch=" + route.postStoneCrystalSwitch
                        + " sword=" + route.postStoneSwordBox + " frame=" + route.frame);
        tickInteractiveEntities(route.session, route.frame++, route.link.romEntityX(), route.link.romEntityY());
        for (int stage = 0; stage < 9; stage++) {
            route.session.entityState().tickGameplayVBlank();
        }
        assertEquals(0x02, RoomDebugFixture.of(route.session).entitySwitchBlocksStateForTest());

        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.DOWN);
        assertEquals(0x38, route.session.rooms().currentRoomId());
        assertEquals(0x00, RoomDebugFixture.of(route.session).activeRoomEventForTest());
        assertEquals(0xA0, objectAt(route.session, 0x43));
        assertEquals(
                1,
                route.session.rooms().activeRoom().entities().loadedEntities().stream()
                        .filter(entity -> entity.type() == 0x14)
                        .count());
        route.room38MoblinSword = route.session.rooms().activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x14)
                .findFirst()
                .orElseThrow();
        assertEquals(0x28, route.room38MoblinSword.x());
        assertEquals(0x70, route.room38MoblinSword.y());
        route.room38CrystalSwitch = route.session.rooms().activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x66)
                .findFirst()
                .orElseThrow();
        assertEquals(0x58, route.room38CrystalSwitch.x());
        assertEquals(0x50, route.room38CrystalSwitch.y());
        route.room38LoweredBlockLocation = firstObjectLocation(route.session, 0xDB);
        assertTrue(route.room38LoweredBlockLocation >= 0);
        assertTrue(
                RoomDebugFixture.of(route.session)
                        .linkCollisionPointBlockedForTest(
                                (route.room38LoweredBlockLocation & 0x0F) << 4,
                                route.room38LoweredBlockLocation & 0xF0),
                "room $38 lowered $DB must block while global state is $02");

        // The raised $DB ring encloses room $38's own source switch.  A
        // Feather jump cannot cross a mismatched switch block (LinkTest
        // covers the source behavior), so use the source-valid sword range
        // from the reachable side of the ring instead of teleporting Link.
        route.room38SwitchApproach = reachableSwordContactPath(
                route.collision, route.link.pixelX(), route.link.pixelY(), route.room38CrystalSwitch, route.romTables);
        assertNotNull(route.room38SwitchApproach, collisionGrid(route.collision));
        for (int[] position : route.room38SwitchApproach.path()) {
            route.link.setPixelPosition(position[0], position[1]);
        }
        assertTrue(
                route.collision.objectUnderLinkFeet(route.link.pixelX(), route.link.pixelY()) != 0xDB,
                "sword approach must remain off the raised $DB ring");
        route.link.setDirection(route.room38SwitchApproach.direction());
        for (int tick = 0; tick < 0x10; tick++) {
            tickInteractiveEntities(route.session, route.frame++, route.link.romEntityX(), route.link.romEntityY());
        }
        route.postStoneLoweringSword = new Sword(route.romTables, null);
        route.postStoneLoweringSword.onPress();
        for (int swingFrame = 0; swingFrame < 4; swingFrame++) {
            route.postStoneLoweringSword.tick(false);
        }
        route.postStoneLoweringBox = route.postStoneLoweringSword.enemyCollisionBox(
                route.link.romEntityX(), route.link.romSwordCollisionY(), route.link.direction());
        assertTrue(linksawakening.world.entity.EntityCombatTestAccess.overlapsSword(
                route.room38CrystalSwitch,
                route.postStoneLoweringBox.x(),
                route.postStoneLoweringBox.width(),
                route.postStoneLoweringBox.y(),
                route.postStoneLoweringBox.height()));
        route.postStoneLoweringHit = route.session
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
                                route.postStoneLoweringBox.x(),
                                route.postStoneLoweringBox.width(),
                                route.postStoneLoweringBox.y(),
                                route.postStoneLoweringBox.height()),
                        linksawakening.world.entity.EnemyAttackContext.standard()));
        route.postStoneLoweringSwitchSlot = route.room38CrystalSwitch.slot();
        assertTrue(
                route.postStoneLoweringHit.stream()
                        .anyMatch(event -> event.slot() == route.postStoneLoweringSwitchSlot
                                && event.type() == 0x66
                                && event.swordHit()),
                route.postStoneLoweringHit.toString());
        tickInteractiveEntities(route.session, route.frame++, route.link.romEntityX(), route.link.romEntityY());
        for (int stage = 0; stage < 9; stage++) {
            route.session.entityState().tickGameplayVBlank();
        }
        assertEquals(0x00, RoomDebugFixture.of(route.session).entitySwitchBlocksStateForTest());
        assertFalse(
                RoomDebugFixture.of(route.session)
                        .linkCollisionPointBlockedForTest(
                                (route.room38LoweredBlockLocation & 0x0F) << 4,
                                route.room38LoweredBlockLocation & 0xF0),
                "room $38 lowered $DB must pass after the switch animation");

        route.room38ChestPath = reachablePositionPath(
                route.collision,
                route.link.pixelX(),
                route.link.pixelY(),
                (x, y) -> Math.floorDiv(x + 0x08, 0x10) == (0x43 & 0x0F)
                        && Math.floorDiv(y + 0x05, 0x10) == ((0x43 & 0xF0) >>> 4));
        assertNotNull(route.room38ChestPath, collisionGrid(route.collision));
        for (int[] position : route.room38ChestPath) {
            route.link.setPixelPosition(position[0], position[1]);
        }
        route.link.setDirection(Link.DIRECTION_UP);
        route.room38Chest = route.session
                .doors()
                .tryOpenChest(
                        route.link.pixelX(),
                        route.link.pixelY(),
                        linksawakening.state.Direction.fromJavaOrUnknown(Link.DIRECTION_UP),
                        true,
                        route.playerState.swordLevel());
        assertTrue(route.room38Chest.opened());
        assertEquals(ChestContentsTable.CHEST_SMALL_KEY, route.room38Chest.itemType());
        assertEquals(0x43, route.room38Chest.location());
        assertEquals(0x10, RoomDebugFixture.of(route.session).indoorRoomStatusForTest(0x01, 0x38) & 0x10);
        route.room38ChestSlot = route.room38Chest.entitySlot();
        route.room38DialogId = new ChestContentsTable(
                        (route.rom == null ? (linksawakening.rom.Rom) null : new linksawakening.rom.Rom(route.rom)))
                .dialogLowIdFor(
                        ChestContentsTable.CHEST_SMALL_KEY,
                        route.playerState.shieldLevel(),
                        route.playerState.swordLevel(),
                        route.playerState.powerBraceletLevel(),
                        0x01,
                        0x38);
        route.room38RewardObserved = false;
        route.room38DialogObserved = false;
        route.room38ChestEnded = false;
        for (int chestTick = 0; chestTick < 0x40; chestTick++) {
            route.session.entities().tickEntities(route.frame++, route.link.romEntityX(), route.link.romEntityY());
            for (ChestRewardEvent reward :
                    route.session.components.entityBridge().outbox.consumeChestRewardEvents()) {
                route.room38RewardObserved |= reward.itemType() == ChestContentsTable.CHEST_SMALL_KEY;
                route.playerState.applyChestReward(reward.itemType());
            }
            route.room38DialogObserved |= route.session.entityState().consumeEntityDialogRequests().stream()
                    .anyMatch(request -> request.globalDialogId() == route.room38DialogId);
            RoomEntity chestEntity =
                    route.session.rooms().activeRoom().entities().slots().get(route.room38ChestSlot);
            if (!chestEntity.loaded() || chestEntity.status() == EntityStatus.DISABLED) {
                route.room38ChestEnded = true;
                break;
            }
        }
        assertTrue(route.room38RewardObserved);
        assertTrue(route.room38DialogObserved);
        assertTrue(route.room38ChestEnded);
        assertEquals(0xA1, objectAt(route.session, 0x43));
        assertEquals(0x10, RoomDebugFixture.of(route.session).indoorRoomStatusForTest(0x01, 0x38) & 0x10);
    }
}
