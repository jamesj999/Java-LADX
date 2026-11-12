package linksawakening.world.room;

import static linksawakening.world.room.RoomConstants.ROOM_OBJECTS_BASE;
import static linksawakening.world.room.RoomTransitionTestSupport.*;
import static linksawakening.world.room.RoomTransitionTestSupport2.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.equipment.Sword;
import linksawakening.state.Direction;
import linksawakening.state.InventoryItem;
import linksawakening.state.PlayerState;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.transition.ScrollController;
import linksawakening.world.transition.Warp;

final class GameplayRouteStage3 {
    static void run(GameplayRouteState route) throws Exception {

        assertEquals(0x10, RoomDebugFixture.of(route.session).indoorRoomStatusForTest(0x00, 0x03) & 0x10);
        route.session.rooms().loadIndoor(0x00, 0x03);
        assertEquals(0xBF, route.session.rooms().activeRoom().roomObjectsArea()[route.hiddenStairsIndex]);

        // .configureStairs leaves the staircase inactive when the room loads.
        // The source arms it only after Link leaves the 12x12 center box.
        route.link.position().setPixelPosition(0x80, 0x10); // ROM entity center ($88,$20)
        route.coordinator.handleWarpAndIndoorBoundaries(route.link);
        assertFalse(route.transition.isActive());
        route.link.position().setPixelPosition(0x90, 0x10); // ROM entity center ($98,$20)
        route.coordinator.handleWarpAndIndoorBoundaries(route.link);
        assertFalse(route.transition.isActive());
        route.link.position().setPixelPosition(0x80, 0x10);
        route.coordinator.handleWarpAndIndoorBoundaries(route.link);
        assertTrue(route.transition.isActive());
        while (route.transition.isActive()) route.transition.tick();
        assertEquals(Warp.CATEGORY_SIDESCROLL, route.session.rooms().mapCategory());
        assertEquals(0x19, route.session.rooms().currentRoomId());
        assertEquals(0x70, route.link.position().pixelX());
        assertEquals(0x00, route.link.position().pixelY());

        route.link.position().setPixelPosition(-5, 0x30);
        route.coordinator.handleWarpAndIndoorBoundaries(route.link);
        assertTrue(route.scroll.isActive());
        assertEquals(0x18, route.session.rooms().currentRoomId());
        while (route.scroll.isActive()) route.scroll.tick(8);

        route.link.position().setPixelPosition(0x20, -5);
        route.coordinator.handleWarpAndIndoorBoundaries(route.link);
        assertTrue(route.transition.isActive());
        while (route.transition.isActive()) route.transition.tick();
        assertEquals(Warp.CATEGORY_INDOOR, route.session.rooms().mapCategory());
        assertEquals(0x01, route.session.rooms().currentRoomId());
        assertEquals(0x40, route.link.position().pixelX());
        assertEquals(0x50, route.link.position().pixelY());

        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.UP);
        assertEquals(0x1C, route.session.rooms().currentRoomId());
        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.UP);
        assertEquals(0x1D, route.session.rooms().currentRoomId());

        route.featherChest = route.session
                .doors()
                .tryOpenChest(
                        0x38,
                        0x21,
                        Direction.fromJavaOrUnknown(Direction.UP.javaIndex()),
                        true,
                        route.playerState.equipmentLevels().swordLevel());
        assertTrue(route.featherChest.opened());
        assertEquals(ChestContentsTable.CHEST_FEATHER, route.featherChest.itemType());
        route.session
                .entities()
                .tickEntitiesWithProjectileEvents(route.frame++, 0x38, 0x21, 0, 0, Direction.UP.javaIndex(), false);
        route.session
                .components
                .entityBridge()
                .outbox
                .consumeChestRewardEvents()
                .forEach(reward -> route.playerState.chestRewards().applyChestReward(reward.itemType()));
        route.hasFeather = route.playerState.inventory().itemA() == InventoryItem.ROCS_FEATHER.romValue()
                || route.playerState.inventory().itemB() == InventoryItem.ROCS_FEATHER.romValue();
        route.featherSubscreenSlot = -1;
        for (int slot = 0; slot < PlayerState.SUBSCREEN_SLOT_COUNT; slot++) {
            route.hasFeather |=
                    route.playerState.inventory().subscreenItem(slot) == InventoryItem.ROCS_FEATHER.romValue();
            if (route.playerState.inventory().subscreenItem(slot) == InventoryItem.ROCS_FEATHER.romValue()) {
                route.featherSubscreenSlot = slot;
            }
        }
        assertTrue(route.hasFeather);
        if (route.playerState.inventory().itemA() != InventoryItem.ROCS_FEATHER.romValue()) {
            assertTrue(route.featherSubscreenSlot >= 0);
            route.playerState.inventory().swapItemAWithSubscreen(route.featherSubscreenSlot);
        }
        assertEquals(
                InventoryItem.ROCS_FEATHER.romValue(),
                route.playerState.inventory().itemA());

        // Return through the source-authored side-view passage. Room $01's
        // staircase is inactive at load and arms only after Link leaves it.
        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.DOWN);
        assertEquals(0x1C, route.session.rooms().currentRoomId());
        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.DOWN);
        assertEquals(0x01, route.session.rooms().currentRoomId());
        route.link.position().setPixelPosition(0x40, 0x50);
        route.coordinator.handleWarpAndIndoorBoundaries(route.link);
        assertFalse(route.transition.isActive());
        route.link.position().setPixelPosition(0x50, 0x50);
        route.coordinator.handleWarpAndIndoorBoundaries(route.link);
        assertFalse(route.transition.isActive());
        route.link.position().setPixelPosition(0x40, 0x50);
        route.coordinator.handleWarpAndIndoorBoundaries(route.link);
        assertTrue(route.transition.isActive());
        while (route.transition.isActive()) route.transition.tick();
        assertEquals(Warp.CATEGORY_SIDESCROLL, route.session.rooms().mapCategory());
        assertEquals(0x18, route.session.rooms().currentRoomId());

        route.link.position().setPixelPosition(RoomConstants.ROOM_PIXEL_WIDTH, 0x30);
        route.coordinator.handleWarpAndIndoorBoundaries(route.link);
        assertTrue(route.scroll.isActive());
        assertEquals(0x19, route.session.rooms().currentRoomId());
        while (route.scroll.isActive()) route.scroll.tick(8);
        route.link.position().setPixelPosition(0x70, -5);
        route.coordinator.handleWarpAndIndoorBoundaries(route.link);
        assertTrue(route.transition.isActive());
        while (route.transition.isActive()) route.transition.tick();
        assertEquals(Warp.CATEGORY_INDOOR, route.session.rooms().mapCategory());
        assertEquals(0x03, route.session.rooms().currentRoomId());

        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.RIGHT);
        assertEquals(0x04, route.session.rooms().currentRoomId());
        assertEquals(
                null,
                reachableBoundaryPath(
                        route.collision,
                        route.link.position().pixelX(),
                        route.link.position().pixelY(),
                        ScrollController.DOWN,
                        true),
                "room $04's south route must cross the ROM-authored pit\n" + collisionGrid(route.collision));
        route.link.position().setPixelPosition(0x20, 0x40);
        jumpOverPitWithFeather(
                route.collision,
                route.link,
                route.inputState,
                route.inputConfig,
                route.itemRegistry,
                route.playerState,
                0x08);
        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.DOWN);
        assertEquals(0x07, route.session.rooms().currentRoomId());

        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.DOWN);
        assertEquals(0x0D, route.session.rooms().currentRoomId());
        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.RIGHT);
        assertEquals(0x0E, route.session.rooms().currentRoomId());
        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.RIGHT);
        assertEquals(0x0F, route.session.rooms().currentRoomId());
        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.UP);
        assertEquals(0x09, route.session.rooms().currentRoomId());

        // Hold against bank 2's interactive keyhole block for its source
        // $20-frame threshold. This spends room $16's third key.
        for (int tick = 0; tick < 0x20; tick++) {
            assertTrue(route.session
                    .doors()
                    .tryInteractWithIndoorBlock(
                            0x20, 0x4A, Direction.fromJavaOrUnknown(Direction.UP.javaIndex()), 0x01));
        }
        assertEquals(0, route.session.dungeon().currentDungeonItemFlagsSnapshot()[DungeonItemState.SMALL_KEYS_INDEX]);
        assertEquals(0x0D, route.session.rooms().activeRoom().roomObjectsArea()[ROOM_OBJECTS_BASE + 0x52]);
        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.LEFT);
        assertEquals(0x08, route.session.rooms().currentRoomId());

        route.link.position().setPixelPosition(0x38, 0x1A);
        route.nightmareKeyChest = route.session
                .doors()
                .tryOpenChest(
                        route.link.position().pixelX(),
                        route.link.position().pixelY(),
                        Direction.fromJavaOrUnknown(Direction.UP.javaIndex()),
                        true,
                        route.playerState.equipmentLevels().swordLevel());
        assertTrue(route.nightmareKeyChest.opened());
        assertEquals(ChestContentsTable.CHEST_NIGHTMARE_KEY, route.nightmareKeyChest.itemType());
        route.session
                .entities()
                .tickEntitiesWithProjectileEvents(route.frame++, 0x38, 0x1B, 0, 0, Direction.UP.javaIndex(), false);
        route.session
                .components
                .entityBridge()
                .outbox
                .consumeChestRewardEvents()
                .forEach(reward -> route.playerState.chestRewards().applyChestReward(reward.itemType()));
        assertEquals(
                1, route.session.dungeon().currentDungeonItemFlagsSnapshot()[DungeonItemState.NIGHTMARE_KEY_INDEX]);

        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.RIGHT);
        assertEquals(0x09, route.session.rooms().currentRoomId());
        route.link.position().setPixelPosition(0x40, 0x60);
        jumpOverPitWithFeather(
                route.collision,
                route.link,
                route.inputState,
                route.inputConfig,
                route.itemRegistry,
                route.playerState,
                0x01);
        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.DOWN);
        assertEquals(0x0F, route.session.rooms().currentRoomId());
        route.link.position().setPixelPosition(0x50, 0x50);
        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.RIGHT);
        assertEquals(0x10, route.session.rooms().currentRoomId());
        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.RIGHT);
        assertEquals(0x11, route.session.rooms().currentRoomId());

        route.rollingBones = route.session.rooms().activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == EntitySpriteHandlerCatalog.ENTITY_ROLLING_BONES)
                .findFirst()
                .orElseThrow();
        route.session
                .entities()
                .tickEntitiesWithProjectileEvents(route.frame++, 80, 72, 0, 0, Direction.DOWN.javaIndex(), false);
        route.rollingBones =
                route.session.rooms().activeRoom().entities().slots().get(route.rollingBones.slot());
        for (int hit = 0;
                hit < 16
                        && route.session
                                        .rooms()
                                        .activeRoom()
                                        .entities()
                                        .slots()
                                        .get(route.rollingBones.slot())
                                        .status()
                                == EntityStatus.ACTIVE;
                hit++) {
            route.rollingBones =
                    route.session.rooms().activeRoom().entities().slots().get(route.rollingBones.slot());
            int rollingBonesSlot = route.rollingBones.slot();
            List<EntityCombatEvent> hitEvents = route.session
                    .entities()
                    .resolveEntityCombat(new EntityCombatInputs(
                            rollingBonesSlot ^ 1,
                            route.rollingBones.x(),
                            route.rollingBones.y(),
                            false,
                            0,
                            true,
                            new SwordBox(
                                    true,
                                    route.rollingBones.x(),
                                    0x08,
                                    route.rollingBones.y() - route.rollingBones.z(),
                                    0x08),
                            linksawakening.world.entity.EnemyAttackContext.standard()));
            assertTrue(
                    hitEvents.stream().anyMatch(event -> event.slot() == rollingBonesSlot && event.enemyDamage() > 0),
                    "hit " + hit + ": " + hitEvents);
            for (int recovery = 0; recovery < 0x80; recovery++) {
                route.session
                        .entities()
                        .tickEntitiesWithProjectileEvents(
                                route.frame++, 80, 72, 0, 0, Direction.DOWN.javaIndex(), false);
            }
        }
        assertEquals(
                EntityStatus.DYING,
                route.session
                        .rooms()
                        .activeRoom()
                        .entities()
                        .slots()
                        .get(route.rollingBones.slot())
                        .status());
        route.rollingBonesDeadline = route.frame + 0x200;
        while (RoomDebugFixture.of(route.session).activeRoomEventForTest() != 0
                && route.frame < route.rollingBonesDeadline) {
            route.session
                    .entities()
                    .tickEntitiesWithProjectileEvents(route.frame++, 0, 0, 0, 0, Direction.DOWN.javaIndex(), false);
        }
        assertEquals(0, RoomDebugFixture.of(route.session).activeRoomEventForTest());
        assertEquals(0x20, RoomDebugFixture.of(route.session).indoorRoomStatusForTest(0x00, 0x11) & 0x20);

        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.UP);
        assertEquals(0x0B, route.session.rooms().currentRoomId());
        assertTrue(route.session
                .doors()
                .tryUnlockIndoorKeyDoor(0x48, 0x00, Direction.fromJavaOrUnknown(Direction.UP.javaIndex()), 0x01));
        for (int tick = 0; tick < 8; tick++) {
            route.session
                    .entities()
                    .tickEntitiesWithProjectileEvents(route.frame++, 0x48, 0x00, 0, 0, Direction.UP.javaIndex(), false);
        }
        assertEquals(0x43, route.session.rooms().activeRoom().roomObjectsArea()[ROOM_OBJECTS_BASE + 0x04]);
        assertEquals(0x44, route.session.rooms().activeRoom().roomObjectsArea()[ROOM_OBJECTS_BASE + 0x05]);
        assertEquals(0x04, RoomDebugFixture.of(route.session).indoorRoomStatusForTest(0x00, 0x0B) & 0x04);

        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.UP);
        assertEquals(0x06, route.session.rooms().currentRoomId());
        route.session
                .entities()
                .tickEntitiesWithProjectileEvents(route.frame++, 0, 0, 0, 0, Direction.DOWN.javaIndex(), false);
        route.moldorm = route.session.rooms().activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == EntitySpriteHandlerCatalog.ENTITY_MOLDORM)
                .findFirst()
                .orElseThrow();
        for (int hit = 0; hit < 4; hit++) {
            int tailX = 0;
            int tailY = 0;
            int extensionDeadline = route.frame + 0x100;
            do {
                route.moldorm =
                        route.session.rooms().activeRoom().entities().slots().get(route.moldorm.slot());
                List<EntitySpriteDefinition.DynamicSprite> sprites =
                        route.moldorm.spriteDefinition().dynamicVariant(0);
                assertEquals(16, sprites.size());
                EntitySpriteDefinition.DynamicSprite vulnerableTail = sprites.get(14);
                tailX = (route.moldorm.x() + vulnerableTail.xOffset()) & 0xFF;
                tailY = (route.moldorm.y() + vulnerableTail.yOffset() + route.moldorm.z()) & 0xFF;
                if (!linksawakening.world.entity.EntityCombatTestAccess.overlapsSword(
                        route.moldorm, tailX, 0x08, tailY, 0x08)) {
                    break;
                }
                route.session
                        .entities()
                        .tickEntitiesWithProjectileEvents(route.frame++, 0, 0, 0, 0, Direction.DOWN.javaIndex(), false);
            } while (route.frame < extensionDeadline);
            assertFalse(linksawakening.world.entity.EntityCombatTestAccess.overlapsSword(
                    route.moldorm, tailX, 0x08, tailY, 0x08));
            int moldormSlot = route.moldorm.slot();
            Sword liveSword = new Sword(route.romTables, null);
            liveSword.onPress();
            for (int swingFrame = 0; swingFrame < 4; swingFrame++) {
                liveSword.tick(false);
            }
            route.link.animation().setDirection(Direction.RIGHT.javaIndex());
            Sword.CollisionBox initialSwordBox = liveSword.enemyCollisionBox(
                    route.link.position().romEntityX(),
                    route.link.position().romSwordCollisionY(),
                    route.link.animation().direction());
            route.link
                    .position()
                    .setPixelPosition(
                            tailX - (initialSwordBox.x() - route.link.position().pixelX()),
                            tailY - (initialSwordBox.y() - route.link.position().pixelY()));
            Sword.CollisionBox swordBox = liveSword.enemyCollisionBox(
                    route.link.position().romEntityX(),
                    route.link.position().romSwordCollisionY(),
                    route.link.animation().direction());
            assertTrue(swordBox.active());
            List<EntityCombatEvent> tailHit = route.session
                    .entities()
                    .resolveEntityCombat(new EntityCombatInputs(
                            moldormSlot ^ 1,
                            route.link.position().romEntityX(),
                            route.link.position().romEntityY(),
                            route.link.airborne().isAirborne(),
                            0,
                            true,
                            new SwordBox(
                                    swordBox.active(), swordBox.x(), swordBox.width(), swordBox.y(), swordBox.height()),
                            linksawakening.world.entity.EnemyAttackContext.standard()));
            assertTrue(
                    tailHit.stream().anyMatch(event -> event.slot() == moldormSlot && event.enemyDamage() == 1),
                    "Moldorm tail hit " + hit + " at (" + tailX + "," + tailY + "): " + tailHit);
            for (int recovery = 0; recovery < 0x30; recovery++) {
                route.session
                        .entities()
                        .tickEntitiesWithProjectileEvents(route.frame++, 0, 0, 0, 0, Direction.DOWN.javaIndex(), false);
            }
        }
        assertEquals(
                EntityStatus.DYING,
                route.session
                        .rooms()
                        .activeRoom()
                        .entities()
                        .slots()
                        .get(route.moldorm.slot())
                        .status());

        route.moldormDeadline = route.frame + 0x400;
        route.heartContainer = null;
        while (route.heartContainer == null && route.frame < route.moldormDeadline) {
            route.session
                    .entities()
                    .tickEntitiesWithProjectileEvents(route.frame++, 0, 0, 0, 0, Direction.DOWN.javaIndex(), false);
            route.heartContainer = route.session.rooms().activeRoom().entities().loadedEntities().stream()
                    .filter(entity -> entity.type() == 0x36)
                    .findFirst()
                    .orElse(null);
        }
        assertNotNull(route.heartContainer);
        route.heartPickupFrame = (route.heartContainer.slot() & 0x01) == 0 ? route.frame | 1 : route.frame & ~1;
        assertNotNull(route.session
                .entities()
                .collectEntityIfNeeded(
                        route.heartPickupFrame,
                        route.heartContainer.x(),
                        route.heartContainer.y(),
                        false,
                        true,
                        Direction.DOWN.javaIndex(),
                        route.heartContainer.z()));
        route.previousMaxHearts = route.playerState.vitals().maxHearts();
        for (int held = 0; held < 0x70; held++) {
            route.session
                    .entities()
                    .tickEntitiesWithProjectileEvents(
                            route.frame++, 0x50, 0x60, 0, 0, Direction.DOWN.javaIndex(), false);
        }
        route.session
                .components
                .entityBridge()
                .outbox
                .consumeHeartContainerRewards()
                .forEach(reward -> route.playerState.vitals().applyHeartContainerReward());
        assertEquals(route.previousMaxHearts + 1, route.playerState.vitals().maxHearts());
        assertEquals(0x20, RoomDebugFixture.of(route.session).indoorRoomStatusForTest(0x00, 0x06) & 0x20);
        assertEquals(0, route.session.rooms().activeRoom().shutterDoorMask());

        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.UP);
        assertEquals(0x02, route.session.rooms().currentRoomId());
        route.instrument = route.session.rooms().activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == EntitySpriteHandlerCatalog.ENTITY_INSTRUMENT_OF_THE_SIRENS)
                .findFirst()
                .orElseThrow();
        route.session
                .entities()
                .tickEntitiesWithProjectileEvents(
                        route.frame++,
                        route.instrument.x(),
                        route.instrument.y(),
                        0,
                        0,
                        Direction.DOWN.javaIndex(),
                        false);
        assertNotNull(route.session
                .entities()
                .collectEntityIfNeeded(
                        (route.instrument.slot() ^ 1) & 1,
                        route.instrument.x(),
                        route.instrument.y(),
                        false,
                        true,
                        Direction.DOWN.javaIndex(),
                        0));
        assertEquals(0x1B, route.session.components.entityBridge().outbox.consumePendingMusicTrack());
        for (int performance = 1; performance <= 88; performance++) {
            route.session
                    .entities()
                    .tickEntitiesWithProjectileEvents(
                            route.frame++, 0x50, 0x60, 0, 0, Direction.DOWN.javaIndex(), false);
        }
        assertTrue(RoomDebugFixture.of(route.session).hasDungeonInstrumentForTest(0x00));
        assertEquals(
                0x100,
                route.session
                        .entityState()
                        .consumeEntityDialogRequests()
                        .getFirst()
                        .globalDialogId());
        route.session.entityState().setEntityDialogActive(true);
        route.session.components.entityInputs().setEntityMusicActive(true);
        route.session
                .entities()
                .tickEntitiesWithProjectileEvents(route.frame++, 0x50, 0x60, 0, 0, Direction.DOWN.javaIndex(), false);
        route.session.entityState().setEntityDialogActive(false);
        route.session
                .entities()
                .tickEntitiesWithProjectileEvents(route.frame++, 0x50, 0x60, 0, 0, Direction.DOWN.javaIndex(), false);
        route.session.components.entityInputs().setEntityMusicActive(false);
        route.session
                .entities()
                .tickEntitiesWithProjectileEvents(route.frame++, 0x50, 0x60, 0, 0, Direction.DOWN.javaIndex(), false);
        assertEquals(0x20, route.session.components.entityBridge().outbox.consumePendingMusicTrack());
        route.ticksToWarpJingle = 0;
        route.heardWarpJingle = false;
        while (!route.heardWarpJingle && route.ticksToWarpJingle < 0x100) {
            route.session
                    .entities()
                    .tickEntitiesWithProjectileEvents(
                            route.frame++, 0x50, 0x60, 0, 0, Direction.DOWN.javaIndex(), false);
            route.ticksToWarpJingle++;
            route.heardWarpJingle = route.session.components.entityBridge().outbox.consumeEntityEvents().stream()
                    .anyMatch(event ->
                            event.soundChannel() == EntityCombatEvent.SoundChannel.JINGLE && event.soundId() == 0x2B);
        }
        assertTrue(route.heardWarpJingle);
        assertEquals(0xFF, route.ticksToWarpJingle);

        route.slowWarpTicks = 0;
    }
}
