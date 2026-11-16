package linksawakening.world.room;

import static linksawakening.world.room.RoomConstants.ROOM_OBJECTS_BASE;
import static linksawakening.world.room.RoomNavigationFixture.PowderApproach;
import static linksawakening.world.room.RoomNavigationFixture.SwordApproach;
import static linksawakening.world.room.RoomNavigationFixture.collisionGrid;
import static linksawakening.world.room.RoomNavigationFixture.firstObjectLocation;
import static linksawakening.world.room.RoomNavigationFixture.jumpOverPitWithFeather;
import static linksawakening.world.room.RoomNavigationFixture.letBowWowEatAllGoponga;
import static linksawakening.world.room.RoomNavigationFixture.loadRom;
import static linksawakening.world.room.RoomNavigationFixture.newSession;
import static linksawakening.world.room.RoomNavigationFixture.objectAt;
import static linksawakening.world.room.RoomNavigationFixture.reachableBoundaryPath;
import static linksawakening.world.room.RoomNavigationFixture.reachableBoundaryPosition;
import static linksawakening.world.room.RoomNavigationFixture.reachableEntityContactPath;
import static linksawakening.world.room.RoomNavigationFixture.reachablePositionPath;
import static linksawakening.world.room.RoomNavigationFixture.reachablePositionPathWithFeather;
import static linksawakening.world.room.RoomNavigationFixture.reachablePowderApproach;
import static linksawakening.world.room.RoomNavigationFixture.reachableSwordContactPath;
import static linksawakening.world.room.RoomNavigationFixture.reachableSwordContactPathWithFeather;
import static linksawakening.world.room.RoomNavigationFixture.reachableWarpPath;
import static linksawakening.world.room.RoomNavigationFixture.setDirectionalInput;
import static linksawakening.world.room.RoomNavigationFixture.signedByteDelta;
import static linksawakening.world.room.RoomNavigationFixture.tickInteractiveEntities;
import static linksawakening.world.room.RoomNavigationFixture.tickLiveRoom3B;
import static linksawakening.world.room.RoomNavigationFixture.walkCollisionPathWithFeather;
import static linksawakening.world.room.RoomNavigationFixture.walkIntoDoor;
import static linksawakening.world.room.RoomNavigationFixture.walkToAndCrossIndoorBoundary;
import static linksawakening.world.room.RoomNavigationFixture.walkToAndCrossOverworldBoundary;
import static linksawakening.world.room.RoomNavigationFixture.walkToAndExitIndoorFrontDoor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;

import java.util.ArrayList;
import java.util.List;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.entity.Link;
import linksawakening.equipment.ItemRegistry;
import linksawakening.equipment.RocsFeather;
import linksawakening.equipment.Sword;
import linksawakening.gameplay.BeachSwordRewardConsumer;
import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
import linksawakening.physics.OverworldCollision;
import linksawakening.rom.Rom;
import linksawakening.rom.RomTables;
import linksawakening.startup.NewGameStartProfile;
import linksawakening.startup.TarinShieldMotion;
import linksawakening.state.Direction;
import linksawakening.state.InventoryItem;
import linksawakening.state.PlayerState;
import linksawakening.testing.TestRoms;
import linksawakening.world.entity.EnemyAttackContext;
import linksawakening.world.entity.EntityRoomLoader;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityRuntime;
import linksawakening.world.entity.RoomEntitySnapshot;
import linksawakening.world.entity.event.ChestRewardEvent;
import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.ToadstoolRewardEvent;
import linksawakening.world.entity.motion.BooBuddyMotion;
import linksawakening.world.entity.motion.HinoxMotion;
import linksawakening.world.entity.motion.MotionTestAccess;
import linksawakening.world.transition.RoomBoundaryController;
import linksawakening.world.transition.ScrollController;
import linksawakening.world.transition.TransitionController;
import linksawakening.world.transition.Warp;

/** Ordered multi-room gameplay scenario using the retained route fixture. */
final class GameplayRouteScenario {
    static void initializeBeachAndForest(GameplayRouteState route) throws Exception {

        route.rom = loadRom();
        route.romTables = RomTables.load(TestRoms.partial(route.rom));
        route.collision = new OverworldCollision(route.romTables);
        route.session = newSession(route.rom, route.romTables, route.collision);
        route.playerState = new PlayerState();
        route.profile = NewGameStartProfile.romDefaults();
        route.profile.initializePlayerState(route.playerState);
        route.session.story().initializeNewGameWorldState();
        route.session.rooms().loadIndoor(route.profile.mapId(), route.profile.roomId());

        assertTrue(route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                .anyMatch(entity -> entity.type() == 0x3E));
        route.tarin = route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x3F)
                .findFirst()
                .orElseThrow();

        route.shieldMotion = new TarinShieldMotion();
        assertEquals(
                0x54,
                route.shieldMotion
                        .tick(
                                false,
                                0x60,
                                true,
                                route.playerState.equipmentLevels().shieldLevel())
                        .dialogLowId());
        route.shieldMotion.tick(
                true, 0x60, false, route.playerState.equipmentLevels().shieldLevel());
        route.shieldMotion.tick(
                false, 0x60, false, route.playerState.equipmentLevels().shieldLevel());
        route.shieldReward = null;
        for (int frame = 0; frame < 0x80; frame++) {
            route.shieldReward = route.shieldMotion.tick(
                    false, 0x60, false, route.playerState.equipmentLevels().shieldLevel());
        }
        assertTrue(route.shieldReward.grantShield());
        route.playerState.chestRewards().applyChestReward(ChestContentsTable.CHEST_SHIELD);
        assertEquals(1, route.playerState.equipmentLevels().shieldLevel());
        assertEquals(0, route.playerState.equipmentLevels().swordLevel());
        assertTrue(route.tarin.spriteDefinition().supported());

        route.scroll = new ScrollController();
        route.transition = new TransitionController();
        route.coordinator =
                route.session.transitions().coordinator(new RoomBoundaryController(), route.transition, route.scroll);
        route.inputState = new InputState();
        route.inputConfig = new InputConfig(1, 2, 3, 4, 5, 6, 7);
        route.itemRegistry = new ItemRegistry();
        route.link = new Link(
                route.inputState,
                route.inputConfig,
                route.romTables,
                route.collision,
                null,
                route.playerState,
                route.itemRegistry);
        route.itemRegistry.register(
                InventoryItem.ROCS_FEATHER.romValue(), new RocsFeather(route.link.airborne()::useRocsFeather));
        route.link.position().setPixelPosition(0x48, RoomConstants.ROOM_PIXEL_HEIGHT);
        route.coordinator.handleWarpAndIndoorBoundaries(route.link);
        while (route.transition.isActive()) route.transition.tick();
        assertEquals(0xA2, route.session.rooms().currentRoomId());

        for (int expectedRoom = 0xB2; expectedRoom <= 0xF2; expectedRoom += 0x10) {
            route.link.position().setPixelPosition(route.link.position().pixelX(), RoomConstants.ROOM_PIXEL_HEIGHT);
            route.coordinator.handleOverworldBoundary(route.link);
            assertEquals(expectedRoom, route.session.rooms().currentRoomId());
            while (route.scroll.isActive()) {
                route.scroll.tick(8);
            }
            // A north-edge cliff now runs the source's post-scroll drop.
            // Let it land before teleporting to the next boundary.
            for (int frame = 0; frame < 64 && route.link.airborne().isAirborne(); frame++)
                route.link.motion().update();
            assertFalse(route.link.airborne().isAirborne());
        }

        route.sword = route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x31)
                .findFirst()
                .orElseThrow();
        route.owl = route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x41)
                .findFirst()
                .orElseThrow();
        assertEquals(0x58, route.sword.x());
        assertEquals(0x60, route.sword.y());
        assertTrue(route.sword.spriteDefinition().supported());
        assertTrue(route.owl.spriteDefinition().supported());

        route.session
                .story()
                .setChestPlayerLevels(
                        route.playerState.equipmentLevels().shieldLevel(),
                        route.playerState.equipmentLevels().swordLevel(),
                        route.playerState.equipmentLevels().powerBraceletLevel());
        tickInteractiveEntities(route.session, 0, 0x3F, 0x44);
        tickInteractiveEntities(route.session, 1, 0x40, 0x44);
        assertEquals(0x22, route.session.consumePendingMusicTrack());

        route.frame = 2;
        route.owlDialogOpened = false;
        while ((RoomDebugFixture.of(route.session).overworldRoomStatusForTest(0xF2) & 0x20) == 0
                && route.frame < 0x300) {
            tickInteractiveEntities(route.session, route.frame++, 0x58, 0x60);
            route.owlDialogOpened |= route.session.entityState().consumeEntityDialogRequests().stream()
                    .anyMatch(request -> request.globalDialogId() == 0x0D9);
        }
        assertTrue(route.owlDialogOpened);

        while (route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                        .anyMatch(entity -> entity.type() == 0x41)
                && route.frame < 0x400) {
            tickInteractiveEntities(route.session, route.frame++, 0x58, 0x60);
        }
        route.pickupFrame = (route.frame & 0xFE) | ((route.sword.slot() ^ 1) & 1);
        route.pickup = route.session
                .entities()
                .collectEntityIfNeeded(route.pickupFrame, route.sword.x(), route.sword.y(), false, true, 3, 0);
        assertNotNull(route.pickup);

        route.frame = route.pickupFrame + 1;
        while (route.playerState.equipmentLevels().swordLevel() == 0 && route.frame < route.pickupFrame + 0x300) {
            route.session.entities().tickEntities(route.frame++, route.sword.x(), route.sword.y());
            route.session.entityState().consumeEntityDialogRequests();
            BeachSwordRewardConsumer.consume(route.session, route.playerState);
        }
        assertEquals(1, route.playerState.equipmentLevels().swordLevel());
        assertEquals(0x30, RoomDebugFixture.of(route.session).overworldRoomStatusForTest(0xF2) & 0x30);

        route.session.rooms().loadInitialOverworld(0xF2);
        assertFalse(route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                .anyMatch(entity -> entity.type() == 0x31 || entity.type() == 0x41));

        route.session
                .story()
                .setChestPlayerLevels(
                        route.playerState.equipmentLevels().shieldLevel(),
                        route.playerState.equipmentLevels().swordLevel(),
                        route.playerState.equipmentLevels().powerBraceletLevel());
        route.link.position().setPixelPosition(0x50, 0x50);
        route.routeToForest = new int[] {
            ScrollController.LEFT, ScrollController.LEFT, ScrollController.UP,
            ScrollController.UP, ScrollController.RIGHT, ScrollController.UP,
            ScrollController.LEFT, ScrollController.UP, ScrollController.UP,
            ScrollController.UP, ScrollController.UP
        };
        route.expectedRooms = new int[] {0xF1, 0xF0, 0xE0, 0xD0, 0xD1, 0xC1, 0xC0, 0xB0, 0xA0, 0x90, 0x80};
        for (int index = 0; index < route.routeToForest.length; index++) {
            walkToAndCrossOverworldBoundary(
                    route.session,
                    route.coordinator,
                    route.scroll,
                    route.collision,
                    route.link,
                    route.routeToForest[index],
                    index + 1 < route.routeToForest.length ? route.routeToForest[index + 1] : ScrollController.NONE);
            assertEquals(route.expectedRooms[index], route.session.rooms().currentRoomId());
        }
        assertEquals(0x80, route.session.rooms().currentRoomId());
        assertTrue(route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                .anyMatch(entity -> entity.type() == 0x41));

        tickInteractiveEntities(route.session, route.frame++, 0x50, 0x50);
        tickInteractiveEntities(route.session, route.frame++, 0x50, 0x50);
        assertEquals(0x22, route.session.consumePendingMusicTrack());
        route.forestOwlDialogOpened = false;
        while (!route.forestOwlDialogOpened && route.frame < route.pickupFrame + 0x600) {
            tickInteractiveEntities(route.session, route.frame++, 0x50, 0x50);
            route.forestOwlDialogOpened |= route.session.entityState().consumeEntityDialogRequests().stream()
                    .anyMatch(request -> request.globalDialogId() == 0x0C0);
        }
        assertTrue(route.forestOwlDialogOpened);
        while ((RoomDebugFixture.of(route.session).overworldRoomStatusForTest(0x80) & 0x20) == 0
                && route.frame < route.pickupFrame + 0x700) {
            tickInteractiveEntities(route.session, route.frame++, 0x50, 0x50);
        }
        assertEquals(0x20, RoomDebugFixture.of(route.session).overworldRoomStatusForTest(0x80) & 0x20);

        route.routeToToadstool = new int[] {
            ScrollController.UP, ScrollController.UP, ScrollController.RIGHT,
            ScrollController.RIGHT, ScrollController.UP, ScrollController.UP,
            ScrollController.LEFT, ScrollController.LEFT, ScrollController.DOWN
        };
        route.toadstoolRouteRooms = new int[] {0x70, 0x60, 0x61, 0x62, 0x52, 0x42, 0x41, 0x40, 0x50};
        for (int index = 0; index < route.routeToToadstool.length; index++) {
            walkToAndCrossOverworldBoundary(
                    route.session,
                    route.coordinator,
                    route.scroll,
                    route.collision,
                    route.link,
                    route.routeToToadstool[index],
                    index + 1 < route.routeToToadstool.length
                            ? route.routeToToadstool[index + 1]
                            : ScrollController.NONE);
            assertEquals(route.toadstoolRouteRooms[index], route.session.rooms().currentRoomId());
        }
        assertEquals(0x50, route.session.rooms().currentRoomId());
        route.toadstool = route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x3A)
                .findFirst()
                .orElseThrow();
        route.session.entityState().setToadstoolPlayerState(false, 0);
        route.session.entities().tickEntities(route.frame++, route.toadstool.x(), route.toadstool.y());
        route.toadstool =
                route.session.rooms().activeRoomOrNull().entities().slots().get(route.toadstool.slot());
        route.toadstoolPickup = route.session
                .entities()
                .collectEntityIfNeeded(
                        (route.frame & 0xFE) | ((route.toadstool.slot() ^ 1) & 1),
                        route.toadstool.x(),
                        route.toadstool.y(),
                        false,
                        true,
                        Direction.RIGHT.javaIndex(),
                        0);
        assertNotNull(route.toadstoolPickup);
        route.toadstoolRewards = List.of();
        while (route.toadstoolRewards.isEmpty() && route.frame < route.pickupFrame + 0x900) {
            route.session.entities().tickEntities(route.frame++, route.toadstool.x(), route.toadstool.y());
            route.session.entityState().consumeEntityDialogRequests();
            route.toadstoolRewards =
                    route.session.components.entityBridge().outbox.consumeToadstoolRewards();
        }
        assertEquals(List.of(new ToadstoolRewardEvent(route.toadstool.slot())), route.toadstoolRewards);
        // Mirrors Main's live event consumer; RoomSession owns the entity
        // cadence and PlayerState owns the resulting inventory mutation.
        route.playerState.playerRewards().applyToadstoolReward();
        assertTrue(route.playerState.ammunition().hasToadstool());
        assertEquals(
                InventoryItem.MAGIC_POWDER.romValue(),
                route.playerState.inventory().subscreenItem(0));

        route.routeToWitchHut = new int[] {
            ScrollController.UP, ScrollController.UP, ScrollController.DOWN,
            ScrollController.RIGHT, ScrollController.RIGHT, ScrollController.RIGHT,
            ScrollController.RIGHT, ScrollController.DOWN, ScrollController.DOWN,
            ScrollController.RIGHT
        };
        route.witchRouteRooms = new int[] {0x40, 0x30, 0x40, 0x41, 0x42, 0x43, 0x44, 0x54, 0x64, 0x65};
        for (int index = 0; index < route.routeToWitchHut.length; index++) {
            walkToAndCrossOverworldBoundary(
                    route.session,
                    route.coordinator,
                    route.scroll,
                    route.collision,
                    route.link,
                    route.routeToWitchHut[index],
                    index + 1 < route.routeToWitchHut.length
                            ? route.routeToWitchHut[index + 1]
                            : ScrollController.NONE);
            assertEquals(route.witchRouteRooms[index], route.session.rooms().currentRoomId());
        }
        assertEquals(0x65, route.session.rooms().currentRoomId());
        route.witchHut = route.session.rooms().activeRoomOrNull().warps().stream()
                .filter(warp -> warp.destMap() == 0x0E && warp.destRoom() == 0xA2)
                .findFirst()
                .orElseThrow();
        assertEquals(0x24, route.witchHut.tileLocation());
        route.coordinator =
                route.session.transitions().coordinator(new RoomBoundaryController(), route.transition, route.scroll);
        route.link
                .position()
                .setPixelPosition(
                        (route.witchHut.tileLocation() & 0x0F) * 16, (route.witchHut.tileLocation() >>> 4) * 16);
        walkIntoDoor(route.link, route.inputState, route.inputConfig, route.coordinator, route.transition);
        while (route.transition.isActive()) route.transition.tick();
        assertEquals(0x0E, route.session.rooms().activeRoomOrNull().mapId());
        assertEquals(0xA2, route.session.rooms().currentRoomId());

        route.witch = route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x40)
                .findFirst()
                .orElseThrow();
        route.playerState.inventory().swapItemAWithSubscreen(0);
        assertEquals(
                InventoryItem.MAGIC_POWDER.romValue(),
                route.playerState.inventory().itemA());
        route.session.entityState().setToadstoolPlayerState(true, 0);
        route.session.setEntityInventorySlots(
                route.playerState.inventory().itemA(),
                route.playerState.inventory().itemB());
        route.session.entityState().setEntityActionButtonsHeld(true, false);
        route.session.entities().tickEntitiesWithProjectileEvents(route.frame++, route.witch.x(), 0x56, 0, 0, 1, false);
        route.session.entities().tickEntitiesWithProjectileEvents(route.frame++, route.witch.x(), 0x56, 0, 0, 1, false);
        route.exchangeEvents = route.session.components.entityBridge().outbox.consumeWitchExchangeEvents();
        assertEquals(1, route.exchangeEvents.size());
        route.playerState
                .playerRewards()
                .beginWitchToadstoolExchange(route.exchangeEvents.getFirst().inventorySlot());
        assertFalse(route.playerState.ammunition().hasToadstool());
        assertEquals(
                InventoryItem.EMPTY.romValue(), route.playerState.inventory().itemA());

        route.session.entityState().setEntityActionButtonsHeld(false, false);
        route.brewingDialog = false;
        while (!route.brewingDialog && route.frame < route.pickupFrame + 0xA00) {
            route.session
                    .entities()
                    .tickEntitiesWithProjectileEvents(route.frame++, route.witch.x(), 0x56, 0, 0, 1, false);
            route.brewingDialog |= route.session.entityState().consumeEntityDialogRequests().stream()
                    .anyMatch(request -> request.globalDialogId() == 0x009);
        }
        assertTrue(route.brewingDialog);
        route.session.entityState().setEntityDialogActive(true);
        route.session.entities().tickEntitiesWithProjectileEvents(route.frame++, route.witch.x(), 0x56, 0, 0, 1, false);
        route.session.entityState().setEntityDialogActive(false);
        route.readyDialog = false;
        while (!route.readyDialog && route.frame < route.pickupFrame + 0xB00) {
            route.session
                    .entities()
                    .tickEntitiesWithProjectileEvents(route.frame++, route.witch.x(), 0x56, 0, 0, 1, false);
            route.readyDialog |= route.session.entityState().consumeEntityDialogRequests().stream()
                    .anyMatch(request -> request.globalDialogId() == 0x0FE);
        }
        assertTrue(route.readyDialog);
        route.session.entityState().setEntityDialogActive(true);
        route.session.entities().tickEntitiesWithProjectileEvents(route.frame++, route.witch.x(), 0x56, 0, 0, 1, false);
        route.session.entityState().setEntityDialogActive(false);
        route.session.entities().tickEntitiesWithProjectileEvents(route.frame++, route.witch.x(), 0x56, 0, 0, 1, false);
        assertEquals(
                1,
                route.session
                        .components
                        .entityBridge()
                        .outbox
                        .consumeWitchRewardEvents()
                        .size());
        route.playerState.playerRewards().applyWitchMagicPowderReward();
        assertEquals(
                InventoryItem.MAGIC_POWDER.romValue(),
                route.playerState.inventory().itemA());
        assertEquals(20, route.playerState.ammunition().magicPowderCount());

        route.link.position().setPixelPosition(route.link.position().pixelX(), RoomConstants.ROOM_PIXEL_HEIGHT);
        route.coordinator.handleWarpAndIndoorBoundaries(route.link);
        while (route.transition.isActive()) route.transition.tick();
        assertEquals(Warp.CATEGORY_OVERWORLD, route.session.rooms().mapCategory());
        assertEquals(0x65, route.session.rooms().currentRoomId());

        route.routeToRaccoon = new int[] {
            ScrollController.LEFT, ScrollController.UP, ScrollController.UP,
            ScrollController.LEFT, ScrollController.LEFT, ScrollController.DOWN,
            ScrollController.DOWN, ScrollController.LEFT, ScrollController.UP
        };
        route.raccoonRouteRooms = new int[] {0x64, 0x54, 0x44, 0x43, 0x42, 0x52, 0x62, 0x61, 0x51};
        for (int index = 0; index < route.routeToRaccoon.length; index++) {
            walkToAndCrossOverworldBoundary(
                    route.session,
                    route.coordinator,
                    route.scroll,
                    route.collision,
                    route.link,
                    route.routeToRaccoon[index],
                    index + 1 < route.routeToRaccoon.length ? route.routeToRaccoon[index + 1] : ScrollController.NONE);
            assertEquals(route.raccoonRouteRooms[index], route.session.rooms().currentRoomId());
        }
        assertEquals(0x51, route.session.rooms().currentRoomId());
        assertTrue(route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                .anyMatch(entity -> entity.type() == 0x3F));

        route.session.setEntityInventorySlots(
                route.playerState.inventory().itemA(),
                route.playerState.inventory().itemB());
        route.session
                .entityState()
                .setToadstoolPlayerState(false, route.playerState.ammunition().magicPowderCount());
        route.session
                .entities()
                .tickEntitiesWithProjectileEvents(route.frame++, 0x6A, 0x40, 0, 0, Direction.RIGHT.javaIndex(), false);
        // ROM direction zero adds $0E to Link's entity X, placing the
        // sprinkle at Tarin's live $78 coordinate.
        assertTrue(route.session.playerItems().sprinkleMagicPowder(0x6A, 0x40, 0, 0));
        route.transformedDialogOpened = false;
        while ((!route.transformedDialogOpened
                        || (RoomDebugFixture.of(route.session).overworldRoomStatusForTest(0x51) & 0x10) == 0)
                && route.frame < route.pickupFrame + 0xD00) {
            route.session
                    .entities()
                    .tickEntitiesWithProjectileEvents(
                            route.frame++, 0x6A, 0x40, 0, 0, Direction.RIGHT.javaIndex(), false);
            route.transformedDialogOpened |= route.session.entityState().consumeEntityDialogRequests().stream()
                    .anyMatch(request -> request.globalDialogId() == 0x00A);
        }
        assertEquals(0x10, RoomDebugFixture.of(route.session).overworldRoomStatusForTest(0x51) & 0x10);
        assertTrue(route.transformedDialogOpened);
        assertTrue(
                route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                        .anyMatch(entity -> entity.type() == 0x3F),
                "the persisted flag must not truncate Tarin's live transformation");

        // Deliberately reload the room to prove the completed transformation
        // survives reconstruction before the persisted route continues.
        route.session.rooms().loadInitialOverworld(0x51);
        route.session
                .entities()
                .tickEntitiesWithProjectileEvents(route.frame, 0x6A, 0x40, 0, 0, Direction.RIGHT.javaIndex(), false);
        assertFalse(route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                .anyMatch(entity -> entity.type() == 0x3F));

        walkToAndCrossOverworldBoundary(
                route.session, route.coordinator, route.scroll, route.collision, route.link, ScrollController.UP);
        assertEquals(0x41, route.session.rooms().currentRoomId());
        assertEquals(0x40, RoomDebugFixture.of(route.session).overworldRoomStatusForTest(0x41) & 0x40);
        assertTrue(route.session.consumeEntityEvents().stream()
                .anyMatch(event ->
                        event.soundChannel() == EntityCombatEvent.SoundChannel.JINGLE && event.soundId() == 0x02));

        // Exercise the same action/facing bridge that Main calls after input
        // has positioned Link at the chest.
        route.tailKeyChest = route.session
                .doors()
                .tryOpenChest(
                        0x38,
                        0x31,
                        Direction.fromJavaOrUnknown(Direction.UP.javaIndex()),
                        true,
                        route.playerState.equipmentLevels().swordLevel());
        assertTrue(route.tailKeyChest.opened());
        assertEquals(ChestContentsTable.CHEST_TAIL_KEY, route.tailKeyChest.itemType());
        route.session.entities().tickEntities(route.frame++, 0x38, 0x31);
        route.session
                .components
                .entityBridge()
                .outbox
                .consumeChestRewardEvents()
                .forEach(reward -> route.playerState.chestRewards().applyChestReward(reward.itemType()));
        assertEquals(1, route.playerState.progression().tailKeyCount());
    }

    static void unlockTailCave(GameplayRouteState route) throws Exception {

        route.session.story().setTailKeyOwned(route.playerState.progression().tailKeyCount() != 0);

        route.tailKeyOwlDialogOpened = false;
        route.tailKeyOwlDeadline = route.frame + 0x300;
        while (!route.tailKeyOwlDialogOpened && route.frame < route.tailKeyOwlDeadline) {
            tickInteractiveEntities(route.session, route.frame++, 0x50, 0x50);
            route.tailKeyOwlDialogOpened |= route.session.entityState().consumeEntityDialogRequests().stream()
                    .anyMatch(request -> request.globalDialogId() == 0x0C1);
        }
        assertTrue(route.tailKeyOwlDialogOpened);
        route.tailKeyOwlExitDeadline = route.frame + 0x100;
        while ((RoomDebugFixture.of(route.session).overworldRoomStatusForTest(0x41) & 0x20) == 0
                && route.frame < route.tailKeyOwlExitDeadline) {
            tickInteractiveEntities(route.session, route.frame++, 0x50, 0x50);
        }
        assertEquals(0x20, RoomDebugFixture.of(route.session).overworldRoomStatusForTest(0x41) & 0x20);

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
            assertEquals(route.tailCaveRouteRooms[index], route.session.rooms().currentRoomId());
        }

        // Exercise Main's collision bridge with the source upward collision
        // bit and the two source keyhole-probe coordinates.
        route.tailCaveSounds = new java.util.ArrayList<GameplaySoundEvent>();
        route.session.entityState().setColorShellSoundSink(route.tailCaveSounds::add);
        assertTrue(route.session
                .doors()
                .tryUnlockTailCaveKeyhole(
                        0x5A,
                        0x4A,
                        Direction.fromJavaOrUnknown(Direction.UP.javaIndex()),
                        0x01,
                        route.playerState.progression().tailKeyCount() != 0));
        assertEquals(0x10, RoomDebugFixture.of(route.session).overworldRoomStatusForTest(0xD3) & 0x10);
        assertEquals(
                0xDF,
                route.session
                        .components
                        .roomDoorInteractions()
                        .tailCaveKeyholeCountdownForTest(route.session.loadedRoomOrNull().contents));
        route.rumbleTicks = 0;
        route.shookRight = false;
        route.shookLeft = false;
        while (route.session.doors().tailCaveKeyholeSequenceActive()) {
            int shake = route.session.doors().tickTailCaveKeyholeSequence();
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
        assertEquals(0xE3, route.session.rooms().activeRoomOrNull().roomObjectsArea()[ROOM_OBJECTS_BASE + 0x16]);

        route.tailCaveEntrance = route.session.rooms().activeRoomOrNull().warps().stream()
                .filter(warp -> warp.tileLocation() == 0x16)
                .findFirst()
                .orElseThrow();
        route.link
                .position()
                .setPixelPosition(
                        (route.tailCaveEntrance.tileLocation() & 0x0F) * 16,
                        (route.tailCaveEntrance.tileLocation() >>> 4) * 16);
        walkIntoDoor(route.link, route.inputState, route.inputConfig, route.coordinator, route.transition);
        while (route.transition.isActive()) route.transition.tick();
        assertEquals(Warp.CATEGORY_INDOOR, route.session.rooms().mapCategory());
        assertEquals(0x00, route.session.rooms().activeRoomOrNull().mapId());
        assertEquals(0x17, route.session.rooms().currentRoomId());

        // Room $16 is the ROM's third Small Key source. Lure both Hardhat
        // Beetles into the continuous north pit row, then collect the live
        // DropKeyEffectHandler entity produced by event $81.
        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.LEFT);
        assertEquals(0x16, route.session.rooms().currentRoomId());
        route.session
                .entities()
                .tickEntitiesWithProjectileEvents(route.frame++, 0x50, 0x50, 0, 0, Direction.UP.javaIndex(), false);
        route.hardhatSlots = route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x20)
                .map(RoomEntity::slot)
                .toList();
        assertEquals(2, route.hardhatSlots.size());
        for (int hardhatSlot : route.hardhatSlots) {
            for (int hit = 0;
                    hit < 8
                            && route.session
                                            .rooms()
                                            .activeRoomOrNull()
                                            .entities()
                                            .slots()
                                            .get(hardhatSlot)
                                            .status()
                                    == EntityStatus.ACTIVE;
                    hit++) {
                RoomEntity hardhat = route.session
                        .rooms()
                        .activeRoomOrNull()
                        .entities()
                        .slots()
                        .get(hardhatSlot);
                List<EntityCombatEvent> recoil = route.session
                        .entities()
                        .resolveEntityCombat(new EntityCombatInputs(
                                hardhatSlot ^ 1,
                                hardhat.x(),
                                hardhat.y() + 0x20,
                                false,
                                0,
                                true,
                                new SwordBox(true, hardhat.x(), 0x10, hardhat.y(), 0x10),
                                linksawakening.world.entity.EnemyAttackContext.standard()));
                assertTrue(recoil.stream().anyMatch(event -> event.slot() == hardhatSlot && event.swordHit()));
                for (int recoilFrame = 0; recoilFrame < 0x30; recoilFrame++) {
                    route.session
                            .entities()
                            .tickEntitiesWithProjectileEvents(
                                    route.frame++,
                                    hardhat.x(),
                                    hardhat.y() + 0x20,
                                    0,
                                    0,
                                    Direction.UP.javaIndex(),
                                    false);
                }
            }
        }
        route.room16Deadline = route.frame + 0x300;
        while (RoomDebugFixture.of(route.session).activeRoomEventForTest() != 0 && route.frame < route.room16Deadline) {
            route.session
                    .entities()
                    .tickEntitiesWithProjectileEvents(route.frame++, 0x50, 0x50, 0, 0, Direction.UP.javaIndex(), false);
        }
        assertEquals(
                0,
                RoomDebugFixture.of(route.session).activeRoomEventForTest(),
                route.session
                        .rooms()
                        .activeRoomOrNull()
                        .entities()
                        .loadedEntities()
                        .toString());
        route.droppedSmallKey = route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x30)
                .findFirst()
                .orElseThrow();
        route.keyLandingDeadline = route.frame + 0x200;
        while (route.droppedSmallKey.z() != 0 && route.frame < route.keyLandingDeadline) {
            route.session
                    .entities()
                    .tickEntitiesWithProjectileEvents(
                            route.frame++, 0x50, 0x50, 0, 0, Direction.DOWN.javaIndex(), false);
            route.droppedSmallKey =
                    route.session.rooms().activeRoomOrNull().entities().slots().get(route.droppedSmallKey.slot());
        }
        assertEquals(0, route.droppedSmallKey.z());
        route.keyPickupFrame = (route.droppedSmallKey.slot() & 0x01) == 0 ? route.frame | 1 : route.frame & ~1;
        assertNotNull(route.session
                .entities()
                .collectEntityIfNeeded(
                        route.keyPickupFrame,
                        route.droppedSmallKey.x(),
                        route.droppedSmallKey.y(),
                        false,
                        true,
                        Direction.DOWN.javaIndex(),
                        0));
        assertEquals(1, route.session.dungeon().currentDungeonItemFlagsSnapshot()[DungeonItemState.SMALL_KEYS_INDEX]);
        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.RIGHT);
        assertEquals(0x17, route.session.rooms().currentRoomId());
        assertEquals(0, RoomDebugFixture.of(route.session).activeRoomEventForTest());
        assertEquals(0, route.session.rooms().activeRoomOrNull().shutterDoorMask());

        route.entranceExit = reachableBoundaryPosition(
                route.collision,
                route.link.position().pixelX(),
                route.link.position().pixelY(),
                ScrollController.UP);
        assertNotNull(route.entranceExit, collisionGrid(route.collision));
        route.link.position().setPixelPosition(route.entranceExit[0], -1);
        route.coordinator.handleWarpAndIndoorBoundaries(route.link);
        while (route.scroll.isActive()) route.scroll.tick(8);
        assertEquals(0x13, route.session.rooms().currentRoomId());

        route.chestIndex = ROOM_OBJECTS_BASE + 0x28;
        assertEquals(0x63, RoomDebugFixture.of(route.session).activeRoomEventForTest());
        assertFalse(route.session.rooms().activeRoomOrNull().roomObjectsArea()[route.chestIndex] == 0xA0);
        route.buttonDeadline = route.frame + 0x40;
        while (RoomDebugFixture.of(route.session).activeRoomEventForTest() != 0 && route.frame < route.buttonDeadline) {
            route.session.entities().tickEntities(route.frame++, 0x58, 0x40);
        }
        assertEquals(0, RoomDebugFixture.of(route.session).activeRoomEventForTest());
        route.chestAppearanceDeadline = route.frame + 0x20;
        while (route.session.rooms().activeRoomOrNull().roomObjectsArea()[route.chestIndex] != 0xA0
                && route.frame < route.chestAppearanceDeadline) {
            route.session.entities().tickEntities(route.frame++, 0x40, 0x60);
        }
        assertEquals(0xA0, route.session.rooms().activeRoomOrNull().roomObjectsArea()[route.chestIndex]);

        route.firstSmallKeyChest = route.session
                .doors()
                .tryOpenChest(
                        0x78,
                        0x21,
                        Direction.fromJavaOrUnknown(Direction.UP.javaIndex()),
                        true,
                        route.playerState.equipmentLevels().swordLevel());
        assertTrue(route.firstSmallKeyChest.opened());
        assertEquals(ChestContentsTable.CHEST_SMALL_KEY, route.firstSmallKeyChest.itemType());
        route.session.entities().tickEntities(route.frame++, 0x78, 0x21);
        assertEquals(2, route.session.dungeon().currentDungeonItemFlagsSnapshot()[DungeonItemState.SMALL_KEYS_INDEX]);

        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.RIGHT);
        assertEquals(0x14, route.session.rooms().currentRoomId());
        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.UP);
        assertEquals(0x0F, route.session.rooms().currentRoomId());

        route.upperKeyDoorIndex = ROOM_OBJECTS_BASE + 0x39;
        route.lowerKeyDoorIndex = ROOM_OBJECTS_BASE + 0x49;
        assertEquals(0x33, route.session.rooms().activeRoomOrNull().roomObjectsArea()[route.upperKeyDoorIndex]);
        assertEquals(0x34, route.session.rooms().activeRoomOrNull().roomObjectsArea()[route.lowerKeyDoorIndex]);
        // bank2's right-facing key-door branch probes Link at ($85,$38)
        // with collision bit $08 against the vertical door pair $39/$49.
        route.firstKeyDoorLinkX = 0x85;
        route.firstKeyDoorLinkY = 0x38;
        route.rightCollisionBit = 0x08;
        route.link.position().setPixelPosition(route.firstKeyDoorLinkX, route.firstKeyDoorLinkY);
        assertTrue(route.session
                .doors()
                .tryUnlockIndoorKeyDoor(
                        route.link.position().pixelX(),
                        route.link.position().pixelY(),
                        Direction.fromJavaOrUnknown(Direction.RIGHT.javaIndex()),
                        route.rightCollisionBit));
        assertEquals(1, route.session.dungeon().currentDungeonItemFlagsSnapshot()[DungeonItemState.SMALL_KEYS_INDEX]);
        for (int tick = 0; tick < 8; tick++) {
            route.session
                    .entities()
                    .tickEntities(
                            route.frame++,
                            route.link.position().pixelX(),
                            route.link.position().pixelY());
            assertTrue(route.session.components.entityBridge().consumeWorldLinkMotionBlockRequest());
        }
        route.session
                .entities()
                .tickEntities(
                        route.frame++,
                        route.link.position().pixelX(),
                        route.link.position().pixelY());
        assertFalse(route.session.components.entityBridge().consumeWorldLinkMotionBlockRequest());
        assertEquals(0x0B, route.session.rooms().activeRoomOrNull().roomObjectsArea()[route.upperKeyDoorIndex]);
        assertEquals(0x0C, route.session.rooms().activeRoomOrNull().roomObjectsArea()[route.lowerKeyDoorIndex]);
        assertEquals(0x01, RoomDebugFixture.of(route.session).indoorRoomStatusForTest(0x00, 0x0F) & 0x01);
        assertEquals(0x02, RoomDebugFixture.of(route.session).indoorRoomStatusForTest(0x00, 0x10) & 0x02);

        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.RIGHT);
        assertEquals(0x10, route.session.rooms().currentRoomId());
        assertEquals(0, RoomDebugFixture.of(route.session).activeRoomEventForTest());
        assertTrue(route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                .anyMatch(entity -> entity.type() == 0x1E));

        // Rolling Bones is reachable to the east, but the progression-bearing
        // route is north: room $0A supplies the stone beak and its three-card
        // puzzle is the next sword-solvable room before Link owns the feather.
        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.UP);
        assertEquals(0x0A, route.session.rooms().currentRoomId());
        assertEquals(0x61, RoomDebugFixture.of(route.session).activeRoomEventForTest());
        route.cards = route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == EntitySpriteHandlerCatalog.ENTITY_THREE_OF_A_KIND)
                .toList();
        assertEquals(3, route.cards.size());
        assertTrue(route.cards.stream().allMatch(card -> card.spriteDefinition().supported()));

        // The Stone Beak branch is optional. The required route returns south
        // and west to the ROM's static small-key chest in room $0E.
        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.DOWN);
        assertEquals(0x10, route.session.rooms().currentRoomId());
        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.LEFT);
        assertEquals(0x0F, route.session.rooms().currentRoomId());
        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.LEFT);
        assertEquals(0x0E, route.session.rooms().currentRoomId());

        route.secondSmallKeyChest = route.session
                .doors()
                .tryOpenChest(
                        0x38,
                        0x21,
                        Direction.fromJavaOrUnknown(Direction.UP.javaIndex()),
                        true,
                        route.playerState.equipmentLevels().swordLevel());
        assertTrue(route.secondSmallKeyChest.opened());
        assertEquals(ChestContentsTable.CHEST_SMALL_KEY, route.secondSmallKeyChest.itemType());
        route.session
                .entities()
                .tickEntitiesWithProjectileEvents(route.frame++, 0x38, 0x21, 0, 0, Direction.DOWN.javaIndex(), false);
        assertEquals(2, route.session.dungeon().currentDungeonItemFlagsSnapshot()[DungeonItemState.SMALL_KEYS_INDEX]);

        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.LEFT);
        assertEquals(0x0D, route.session.rooms().currentRoomId());
        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.UP);
        assertEquals(0x07, route.session.rooms().currentRoomId());

        route.link.position().setPixelPosition(0x48, 0x05);
        assertTrue(route.session
                .doors()
                .tryUnlockIndoorKeyDoor(
                        route.link.position().pixelX(),
                        route.link.position().pixelY(),
                        Direction.fromJavaOrUnknown(Direction.UP.javaIndex()),
                        0x01));
        for (int tick = 0; tick <= 8; tick++) {
            route.session
                    .entities()
                    .tickEntitiesWithProjectileEvents(
                            route.frame++,
                            route.link.position().pixelX(),
                            route.link.position().pixelY(),
                            0,
                            0,
                            Direction.UP.javaIndex(),
                            false);
        }
        assertEquals(1, route.session.dungeon().currentDungeonItemFlagsSnapshot()[DungeonItemState.SMALL_KEYS_INDEX]);
        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.UP);
        assertEquals(0x04, route.session.rooms().currentRoomId());

        route.movableBlockIndex = ROOM_OBJECTS_BASE + 0x32;
        assertEquals(0xA7, route.session.rooms().activeRoomOrNull().roomObjectsArea()[route.movableBlockIndex]);
        for (int tick = 0; tick < 64; tick++) {
            assertTrue(route.session
                    .doors()
                    .tryInteractWithIndoorBlock(
                            0x15, 0x28, Direction.fromJavaOrUnknown(Direction.RIGHT.javaIndex()), 0x08));
        }
        for (int tick = 0; tick < 33; tick++) {
            route.session.entities().tickEntities(route.frame++, 0x15, 0x28);
        }
        assertEquals(0, route.session.rooms().activeRoomOrNull().shutterDoorMask());
        assertEquals(0, RoomDebugFixture.of(route.session).indoorRoomStatusForTest(0x00, 0x04) & 0x10);

        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.LEFT);
        assertEquals(0x03, route.session.rooms().currentRoomId());
        assertEquals(0xA1, RoomDebugFixture.of(route.session).activeRoomEventForTest());
        route.hiddenStairsIndex = ROOM_OBJECTS_BASE + 0x18;
        assertEquals(0x0D, route.session.rooms().activeRoomOrNull().roomObjectsArea()[route.hiddenStairsIndex]);
        route.beetles = route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x2C)
                .toList();
        assertEquals(2, route.beetles.size());

        // EntityInitHandler runs before the first interactive collision pass.
        route.session
                .entities()
                .tickEntitiesWithProjectileEvents(route.frame++, 0, 0, 0, 0, Direction.DOWN.javaIndex(), false);
        route.beetles = route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x2C)
                .toList();
        // The raised shield flips beetles; the sword then damages the underside.
        for (RoomEntity beetle : route.beetles) {
            List<EntityCombatEvent> flipEvents = route.session
                    .entities()
                    .resolveEntityCombat(new EntityCombatInputs(
                            beetle.slot() ^ 1,
                            beetle.x() - 8,
                            beetle.y(),
                            false,
                            0,
                            true,
                            new SwordBox(false, 0, 0, 0, 0),
                            new EnemyAttackContext(1, false, false, false, false, true, 0, 0)));
            assertTrue(
                    flipEvents.stream()
                            .anyMatch(event ->
                                    event.slot() == beetle.slot() && !event.swordHit() && event.soundId() == 0x09),
                    flipEvents.toString());
        }
        route.session
                .entities()
                .tickEntitiesWithProjectileEvents(route.frame++, 0, 0, 0, 0, Direction.DOWN.javaIndex(), false);
        route.beetles = route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x2C)
                .toList();
        for (RoomEntity beetle : route.beetles) {
            List<EntityCombatEvent> damageEvents = route.session
                    .entities()
                    .resolveEntityCombat(new EntityCombatInputs(
                            beetle.slot() ^ 1,
                            0,
                            0,
                            false,
                            0,
                            true,
                            new SwordBox(true, beetle.x() + 0x08, 1, beetle.y() - beetle.z() + 0x08, 1),
                            linksawakening.world.entity.EnemyAttackContext.standard()));
            assertTrue(
                    damageEvents.stream()
                            .anyMatch(event ->
                                    event.slot() == beetle.slot() && event.swordHit() && event.enemyDamage() > 0),
                    damageEvents.toString());
        }
        for (int attack = 0; attack < 3; attack++) {
            for (int recovery = 0; recovery < 12; recovery++) {
                route.session
                        .entities()
                        .tickEntitiesWithProjectileEvents(route.frame++, 0, 0, 0, 0, Direction.DOWN.javaIndex(), false);
            }
            route.beetles = route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                    .filter(entity -> entity.type() == 0x2C)
                    .toList();
            for (RoomEntity beetle : route.beetles) {
                route.session
                        .entities()
                        .resolveEntityCombat(new EntityCombatInputs(
                                beetle.slot() ^ 1,
                                0,
                                0,
                                false,
                                0,
                                true,
                                new SwordBox(true, beetle.x() + 0x08, 1, beetle.y() - beetle.z() + 0x08, 1),
                                linksawakening.world.entity.EnemyAttackContext.standard()));
            }
        }
        route.beetleDeadline = route.frame + 0x200;
        while (RoomDebugFixture.of(route.session).activeRoomEventForTest() != 0 && route.frame < route.beetleDeadline) {
            route.session
                    .entities()
                    .tickEntitiesWithProjectileEvents(route.frame++, 0, 0, 0, 0, Direction.DOWN.javaIndex(), false);
        }
        assertEquals(
                0,
                RoomDebugFixture.of(route.session).activeRoomEventForTest(),
                route.session
                        .rooms()
                        .activeRoomOrNull()
                        .entities()
                        .loadedEntities()
                        .toString());
        assertEquals(0x0D, route.session.rooms().activeRoomOrNull().roomObjectsArea()[route.hiddenStairsIndex]);
        for (int revealFrame = 0; revealFrame < 11; revealFrame++) {
            route.session
                    .entities()
                    .tickEntitiesWithProjectileEvents(route.frame++, 0, 0, 0, 0, Direction.DOWN.javaIndex(), false);
        }
        assertEquals(0xBE, route.session.rooms().activeRoomOrNull().roomObjectsArea()[route.hiddenStairsIndex]);
    }

    static void completeTailCavePuzzles(GameplayRouteState route) throws Exception {

        assertEquals(0x10, RoomDebugFixture.of(route.session).indoorRoomStatusForTest(0x00, 0x03) & 0x10);
        route.session.rooms().loadIndoor(0x00, 0x03);
        assertEquals(0xBF, route.session.rooms().activeRoomOrNull().roomObjectsArea()[route.hiddenStairsIndex]);

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
        assertEquals(0x0D, route.session.rooms().activeRoomOrNull().roomObjectsArea()[ROOM_OBJECTS_BASE + 0x52]);
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

        route.rollingBones = route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == EntitySpriteHandlerCatalog.ENTITY_ROLLING_BONES)
                .findFirst()
                .orElseThrow();
        route.session
                .entities()
                .tickEntitiesWithProjectileEvents(route.frame++, 80, 72, 0, 0, Direction.DOWN.javaIndex(), false);
        route.rollingBones =
                route.session.rooms().activeRoomOrNull().entities().slots().get(route.rollingBones.slot());
        for (int hit = 0;
                hit < 16
                        && route.session
                                        .rooms()
                                        .activeRoomOrNull()
                                        .entities()
                                        .slots()
                                        .get(route.rollingBones.slot())
                                        .status()
                                == EntityStatus.ACTIVE;
                hit++) {
            route.rollingBones =
                    route.session.rooms().activeRoomOrNull().entities().slots().get(route.rollingBones.slot());
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
                        .activeRoomOrNull()
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
        assertEquals(0x43, route.session.rooms().activeRoomOrNull().roomObjectsArea()[ROOM_OBJECTS_BASE + 0x04]);
        assertEquals(0x44, route.session.rooms().activeRoomOrNull().roomObjectsArea()[ROOM_OBJECTS_BASE + 0x05]);
        assertEquals(0x04, RoomDebugFixture.of(route.session).indoorRoomStatusForTest(0x00, 0x0B) & 0x04);

        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.UP);
        assertEquals(0x06, route.session.rooms().currentRoomId());
        route.session
                .entities()
                .tickEntitiesWithProjectileEvents(route.frame++, 0, 0, 0, 0, Direction.DOWN.javaIndex(), false);
        route.moldorm = route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == EntitySpriteHandlerCatalog.ENTITY_MOLDORM)
                .findFirst()
                .orElseThrow();
        for (int hit = 0; hit < 4; hit++) {
            int tailX = 0;
            int tailY = 0;
            int extensionDeadline = route.frame + 0x100;
            do {
                route.moldorm = route.session
                        .rooms()
                        .activeRoomOrNull()
                        .entities()
                        .slots()
                        .get(route.moldorm.slot());
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
                        .activeRoomOrNull()
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
            route.heartContainer = route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
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
        assertEquals(0, route.session.rooms().activeRoomOrNull().shutterDoorMask());

        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.UP);
        assertEquals(0x02, route.session.rooms().currentRoomId());
        route.instrument = route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
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
        assertEquals(0x1B, route.session.consumePendingMusicTrack());
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
        route.session.setEntityMusicActive(true);
        route.session
                .entities()
                .tickEntitiesWithProjectileEvents(route.frame++, 0x50, 0x60, 0, 0, Direction.DOWN.javaIndex(), false);
        route.session.entityState().setEntityDialogActive(false);
        route.session
                .entities()
                .tickEntitiesWithProjectileEvents(route.frame++, 0x50, 0x60, 0, 0, Direction.DOWN.javaIndex(), false);
        route.session.setEntityMusicActive(false);
        route.session
                .entities()
                .tickEntitiesWithProjectileEvents(route.frame++, 0x50, 0x60, 0, 0, Direction.DOWN.javaIndex(), false);
        assertEquals(0x20, route.session.consumePendingMusicTrack());
        route.ticksToWarpJingle = 0;
        route.heardWarpJingle = false;
        while (!route.heardWarpJingle && route.ticksToWarpJingle < 0x100) {
            route.session
                    .entities()
                    .tickEntitiesWithProjectileEvents(
                            route.frame++, 0x50, 0x60, 0, 0, Direction.DOWN.javaIndex(), false);
            route.ticksToWarpJingle++;
            route.heardWarpJingle = route.session.consumeEntityEvents().stream()
                    .anyMatch(event ->
                            event.soundChannel() == EntityCombatEvent.SoundChannel.JINGLE && event.soundId() == 0x2B);
        }
        assertTrue(route.heardWarpJingle);
        assertEquals(0xFF, route.ticksToWarpJingle);

        route.slowWarpTicks = 0;
    }

    static void performInstrumentWarp(GameplayRouteState route) throws Exception {

        while (!route.transition.isActive() && route.slowWarpTicks < 0x204) {
            route.session
                    .entities()
                    .tickEntitiesWithProjectileEvents(
                            route.frame++, 0x50, 0x60, 0, 0, Direction.DOWN.javaIndex(), false);
            route.slowWarpTicks++;
            if (route.coordinator.handlePendingInstrumentTransition(route.link)) {
                break;
            }
        }
        assertTrue(route.transition.isActive());
        assertTrue(
                route.slowWarpTicks >= 0x1FD && route.slowWarpTicks <= 0x200,
                "source $80 slow countdown completed in " + route.slowWarpTicks + " ticks");
        while (route.transition.isActive()) route.transition.tick();
        assertEquals(Warp.CATEGORY_OVERWORLD, route.session.rooms().mapCategory());
        assertEquals(0xD3, route.session.rooms().currentRoomId());

        walkToAndCrossOverworldBoundary(
                route.session, route.coordinator, route.scroll, route.collision, route.link, ScrollController.LEFT);
        assertEquals(0xD2, route.session.rooms().currentRoomId());
        assertTrue(route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                .anyMatch(entity -> entity.type() == 0x41));
        assertEquals(0x80, route.session.story().bowWowState());

        route.instrumentOwlDialogOpened = false;
        route.instrumentOwlDialogs = new java.util.ArrayList<Integer>();
        route.instrumentOwlDeadline = route.frame + 0x100;
        while (!route.instrumentOwlDialogOpened && route.frame < route.instrumentOwlDeadline) {
            tickInteractiveEntities(route.session, route.frame++, 0x50, 0x50);
            route.instrumentOwlDialogs.addAll(route.session.entityState().consumeEntityDialogRequests().stream()
                    .map(DialogRequest::globalDialogId)
                    .toList());
            route.instrumentOwlDialogOpened |= route.instrumentOwlDialogs.contains(0x0C2);
        }
        assertTrue(route.instrumentOwlDialogOpened, route.instrumentOwlDialogs.toString());
        route.instrumentOwlExitDeadline = route.frame + 0x100;
        while ((RoomDebugFixture.of(route.session).overworldRoomStatusForTest(0xD2) & 0x20) == 0
                && route.frame < route.instrumentOwlExitDeadline) {
            tickInteractiveEntities(route.session, route.frame++, 0x50, 0x50);
        }
        assertEquals(0x20, RoomDebugFixture.of(route.session).overworldRoomStatusForTest(0xD2) & 0x20);

        route.routeToKidnappingWarning =
                new int[] {ScrollController.UP, ScrollController.LEFT, ScrollController.LEFT, ScrollController.UP};
        route.kidnappingWarningRooms = new int[] {0xC2, 0xC1, 0xC0, 0xB0};
        for (int index = 0; index < route.routeToKidnappingWarning.length; index++) {
            try {
                walkToAndCrossOverworldBoundary(
                        route.session,
                        route.coordinator,
                        route.scroll,
                        route.collision,
                        route.link,
                        route.routeToKidnappingWarning[index],
                        index + 1 < route.routeToKidnappingWarning.length
                                ? route.routeToKidnappingWarning[index + 1]
                                : ScrollController.NONE);
            } catch (AssertionError error) {
                throw new AssertionError(
                        "kidnapping route index " + index + " from room "
                                + Integer.toHexString(route.session.rooms().currentRoomId()),
                        error);
            }
            assertEquals(
                    route.kidnappingWarningRooms[index], route.session.rooms().currentRoomId());
        }
        route.warningKid = route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x71)
                .findFirst()
                .orElseThrow();
        route.session.entities().tickEntities(route.frame++, 0x10, 0x10);
        route.session.entities().tickEntities(route.frame++, 0x10, 0x10);
        assertEquals(0x0E, route.session.consumePendingMusicTrack());
        route.session.entities().tickEntities(route.frame++, route.warningKid.x(), route.warningKid.y());
        assertEquals(
                0x220,
                route.session
                        .entityState()
                        .consumeEntityDialogRequests()
                        .getFirst()
                        .globalDialogId());

        route.routeToMoblinHideout = new int[] {
            ScrollController.UP, ScrollController.UP, ScrollController.UP,
            ScrollController.UP, ScrollController.UP, ScrollController.RIGHT,
            ScrollController.RIGHT, ScrollController.UP, ScrollController.UP,
            ScrollController.RIGHT, ScrollController.RIGHT, ScrollController.UP,
            ScrollController.RIGHT
        };
        route.moblinHideoutRouteRooms =
                new int[] {0xA0, 0x90, 0x80, 0x70, 0x60, 0x61, 0x62, 0x52, 0x42, 0x43, 0x44, 0x34, 0x35};
        for (int index = 0; index < route.routeToMoblinHideout.length; index++) {
            try {
                walkToAndCrossOverworldBoundary(
                        route.session,
                        route.coordinator,
                        route.scroll,
                        route.collision,
                        route.link,
                        route.routeToMoblinHideout[index],
                        index + 1 < route.routeToMoblinHideout.length
                                ? route.routeToMoblinHideout[index + 1]
                                : ScrollController.NONE);
            } catch (AssertionError error) {
                throw new AssertionError(
                        "hideout route index " + index + " from room "
                                + Integer.toHexString(route.session.rooms().currentRoomId()),
                        error);
            }
            assertEquals(
                    route.moblinHideoutRouteRooms[index], route.session.rooms().currentRoomId());
        }
        route.moblinHideoutEntrance = route.session.rooms().activeRoomOrNull().warps().stream()
                .filter(warp -> warp.destMap() == 0x15 && warp.destRoom() == 0xF0)
                .findFirst()
                .orElseThrow();
        route.link
                .position()
                .setPixelPosition(
                        (route.moblinHideoutEntrance.tileLocation() & 0x0F) * 16,
                        (route.moblinHideoutEntrance.tileLocation() >>> 4) * 16);
        walkIntoDoor(route.link, route.inputState, route.inputConfig, route.coordinator, route.transition);
        while (route.transition.isActive()) route.transition.tick();
        assertEquals(0x15, route.session.rooms().activeRoomOrNull().mapId());
        assertEquals(0xF0, route.session.rooms().currentRoomId());

        route.hideoutGuard = route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x14)
                .findFirst()
                .orElseThrow();
        route.hideoutGuardDialogOpened = false;
        route.guardIntroDeadline = route.frame + 0x100;
        while (!route.hideoutGuardDialogOpened && route.frame < route.guardIntroDeadline) {
            route.session
                    .entities()
                    .tickEntitiesWithProjectileEvents(
                            route.frame++, 0x70, route.hideoutGuard.y(), 0, 0, Direction.LEFT.javaIndex(), false);
            route.hideoutGuardDialogOpened |= route.session.entityState().consumeEntityDialogRequests().stream()
                    .anyMatch(request -> request.globalDialogId() == 0x190);
        }
        assertTrue(route.hideoutGuardDialogOpened);
        for (int hit = 0; hit < 2; hit++) {
            route.hideoutGuard =
                    route.session.rooms().activeRoomOrNull().entities().slots().get(route.hideoutGuard.slot());
            int hideoutGuardSlot = route.hideoutGuard.slot();
            List<EntityCombatEvent> guardHit = route.session
                    .entities()
                    .resolveEntityCombat(new EntityCombatInputs(
                            hideoutGuardSlot ^ 1,
                            route.hideoutGuard.x(),
                            route.hideoutGuard.y(),
                            false,
                            0,
                            true,
                            new SwordBox(true, route.hideoutGuard.x(), 0x08, route.hideoutGuard.y(), 0x08),
                            linksawakening.world.entity.EnemyAttackContext.standard()));
            assertTrue(
                    guardHit.stream().anyMatch(event -> event.slot() == hideoutGuardSlot && event.enemyDamage() > 0));
            for (int recovery = 0; recovery < 0x20; recovery++) {
                route.session
                        .entities()
                        .tickEntitiesWithProjectileEvents(
                                route.frame++, 0x20, 0x70, 0, 0, Direction.RIGHT.javaIndex(), false);
            }
        }
        route.guardDeadline = route.frame + 0x100;
        while (RoomDebugFixture.of(route.session).activeRoomEventForTest() != 0 && route.frame < route.guardDeadline) {
            route.session
                    .entities()
                    .tickEntitiesWithProjectileEvents(
                            route.frame++, 0x20, 0x70, 0, 0, Direction.RIGHT.javaIndex(), false);
        }
        assertEquals(0, RoomDebugFixture.of(route.session).activeRoomEventForTest());
        assertEquals(0, route.session.rooms().activeRoomOrNull().shutterDoorMask() & 0x01);

        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.UP);
        assertEquals(0xE0, route.session.rooms().currentRoomId());
        route.hideoutMoblinSlots = route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x0B)
                .map(RoomEntity::slot)
                .toList();
        assertEquals(4, route.hideoutMoblinSlots.size());
        route.session
                .entities()
                .tickEntitiesWithProjectileEvents(route.frame++, 0x50, 0x50, 0, 0, Direction.RIGHT.javaIndex(), false);
        for (int moblinSlot : route.hideoutMoblinSlots) {
            for (int hit = 0;
                    hit < 4
                            && route.session
                                            .rooms()
                                            .activeRoomOrNull()
                                            .entities()
                                            .slots()
                                            .get(moblinSlot)
                                            .status()
                                    == EntityStatus.ACTIVE;
                    hit++) {
                RoomEntity moblin = route.session
                        .rooms()
                        .activeRoomOrNull()
                        .entities()
                        .slots()
                        .get(moblinSlot);
                List<EntityCombatEvent> moblinHit = route.session
                        .entities()
                        .resolveEntityCombat(new EntityCombatInputs(
                                moblinSlot ^ 1,
                                moblin.x(),
                                moblin.y(),
                                false,
                                0,
                                true,
                                new SwordBox(true, moblin.x(), 0x08, moblin.y(), 0x08),
                                linksawakening.world.entity.EnemyAttackContext.standard()));
                assertTrue(moblinHit.stream().anyMatch(event -> event.slot() == moblinSlot && event.enemyDamage() > 0));
                for (int recovery = 0; recovery < 0x20; recovery++) {
                    route.session
                            .entities()
                            .tickEntitiesWithProjectileEvents(
                                    route.frame++, 0x50, 0x50, 0, 0, Direction.RIGHT.javaIndex(), false);
                }
            }
        }
        route.moblinRoomDeadline = route.frame + 0x200;
        while (RoomDebugFixture.of(route.session).activeRoomEventForTest() != 0
                && route.frame < route.moblinRoomDeadline) {
            route.session
                    .entities()
                    .tickEntitiesWithProjectileEvents(
                            route.frame++, 0x50, 0x50, 0, 0, Direction.RIGHT.javaIndex(), false);
        }
        assertEquals(0, RoomDebugFixture.of(route.session).activeRoomEventForTest());
        assertEquals(0, route.session.rooms().activeRoomOrNull().shutterDoorMask() & 0x08);
        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.RIGHT);
        assertEquals(0xE1, route.session.rooms().currentRoomId());

        route.moblinKing = route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0xE4)
                .findFirst()
                .orElseThrow();
        route.moblinKingIntroOpened = false;
        route.moblinKingHits = 0;
        route.moblinKingDeadline = route.frame + 0x2000;
        while (route.session
                        .rooms()
                        .activeRoomOrNull()
                        .entities()
                        .slots()
                        .get(route.moblinKing.slot())
                        .loaded()
                && route.frame < route.moblinKingDeadline) {
            route.session
                    .entities()
                    .tickEntitiesWithProjectileEvents(
                            route.frame++, 0x00, 0x20, 0, 0, Direction.RIGHT.javaIndex(), false);
            route.moblinKingIntroOpened |= route.session.entityState().consumeEntityDialogRequests().stream()
                    .anyMatch(request -> request.globalDialogId() == 0x191);
            route.moblinKing =
                    route.session.rooms().activeRoomOrNull().entities().slots().get(route.moblinKing.slot());
            if (route.moblinKing.status() != EntityStatus.ACTIVE) {
                continue;
            }

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
                            route.moblinKing.x()
                                    - (initialSwordBox.x()
                                            - route.link.position().pixelX()),
                            route.moblinKing.y()
                                    - (initialSwordBox.y()
                                            - route.link.position().pixelY()));
            Sword.CollisionBox swordBox = liveSword.enemyCollisionBox(
                    route.link.position().romEntityX(),
                    route.link.position().romSwordCollisionY(),
                    route.link.animation().direction());
            int moblinKingSlot = route.moblinKing.slot();
            List<EntityCombatEvent> kingHit = route.session
                    .entities()
                    .resolveEntityCombat(new EntityCombatInputs(
                            moblinKingSlot ^ 1,
                            route.link.position().romEntityX(),
                            route.link.position().romEntityY(),
                            false,
                            0,
                            true,
                            new SwordBox(
                                    swordBox.active(), swordBox.x(), swordBox.width(), swordBox.y(), swordBox.height()),
                            linksawakening.world.entity.EnemyAttackContext.standard()));
            if (kingHit.stream().anyMatch(event -> event.slot() == moblinKingSlot && event.enemyDamage() > 0)) {
                route.moblinKingHits++;
            }
        }
        assertTrue(route.moblinKingIntroOpened);
        assertEquals(8, route.moblinKingHits);
        assertFalse(route.session
                .rooms()
                .activeRoomOrNull()
                .entities()
                .slots()
                .get(route.moblinKing.slot())
                .loaded());
        assertEquals(0x20, RoomDebugFixture.of(route.session).indoorRoomStatusForTest(0x15, 0xE1) & 0x20);

        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.RIGHT);
        assertEquals(0xE2, route.session.rooms().currentRoomId());
        route.kidnappedBowWow = route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x6D)
                .findFirst()
                .orElseThrow();
        route.bowWowRescueDeadline = route.frame + 0x40;
        while (route.session.story().bowWowState() != 0x01 && route.frame < route.bowWowRescueDeadline) {
            route.session.tickEntities(new EntityTickInputs(
                    route.frame++,
                    new LinkPosition(
                            route.kidnappedBowWow.x(), route.kidnappedBowWow.y(), 0, Direction.RIGHT.javaIndex(), 1),
                    new LinkMotion(false, 0, false, 0, 0, 0),
                    1,
                    new SwordBox(false, 0, 0, 0, 0)));
        }
        assertEquals(0x01, route.session.story().bowWowState());
        assertEquals(0x10, route.session.consumePendingMusicTrack());
        assertTrue(route.session.entityState().consumeEntityDialogRequests().stream()
                .anyMatch(request -> request.globalDialogId() == 0x16C));

        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.LEFT);
        route.session
                .entities()
                .tickEntitiesWithProjectileEvents(
                        route.frame,
                        route.link.position().romEntityX(),
                        route.link.position().romEntityY(),
                        0,
                        0,
                        Direction.LEFT.javaIndex(),
                        false);
        assertTrue(route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                .anyMatch(entity -> entity.type() == 0x6D));

        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.LEFT);
        assertEquals(0xE0, route.session.rooms().currentRoomId());
        route.session
                .entities()
                .tickEntitiesWithProjectileEvents(
                        route.frame++,
                        route.link.position().romEntityX(),
                        route.link.position().romEntityY(),
                        0,
                        0,
                        Direction.DOWN.javaIndex(),
                        false);
        assertEquals(0, RoomDebugFixture.of(route.session).activeRoomEventForTest());
        assertEquals(0, route.session.rooms().activeRoomOrNull().shutterDoorMask() & 0x02);
        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.DOWN);
        assertEquals(0xF0, route.session.rooms().currentRoomId());
        route.hideoutExit = route.session.rooms().activeRoomOrNull().warps().stream()
                .filter(warp -> warp.destMap() == 0x00 && warp.destRoom() == 0x35)
                .findFirst()
                .orElseThrow();
        assertEquals(route.hideoutExit, route.session.rooms().activeRoomOrNull().firstWarp());
        // A room load reconstructs shutters; the ordinary frame resolves its
        // kill-all event before Link can leave through the south entrance.
        route.session
                .entities()
                .tickEntitiesWithProjectileEvents(
                        route.frame++,
                        route.link.position().romEntityX(),
                        route.link.position().romEntityY(),
                        0,
                        0,
                        Direction.DOWN.javaIndex(),
                        false);
        assertEquals(
                0,
                route.session.rooms().activeRoomOrNull().shutterDoorMask(),
                route.session
                        .rooms()
                        .activeRoomOrNull()
                        .entities()
                        .loadedEntities()
                        .toString());
        assertTrue(route.session.rooms().activeRoomOrNull().indoorHasSouthEntrance());
        walkToAndExitIndoorFrontDoor(route.coordinator, route.transition, route.collision, route.link);
        assertEquals(Warp.CATEGORY_OVERWORLD, route.session.rooms().mapCategory());
        assertEquals(0x35, route.session.rooms().currentRoomId());

        walkToAndCrossOverworldBoundary(
                route.session, route.coordinator, route.scroll, route.collision, route.link, ScrollController.RIGHT);
        assertEquals(0x36, route.session.rooms().currentRoomId());
        assertTrue(route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                .anyMatch(entity -> entity.type() == 0x41));
        route.swampOwlDialogOpened = false;
        route.swampOwlDeadline = route.frame + 0x100;
        while (!route.swampOwlDialogOpened && route.frame < route.swampOwlDeadline) {
            tickInteractiveEntities(route.session, route.frame++, 0x50, 0x50);
            route.swampOwlDialogOpened |= route.session.entityState().consumeEntityDialogRequests().stream()
                    .anyMatch(request -> request.globalDialogId() == 0x0C3);
        }
        assertTrue(route.swampOwlDialogOpened);
        route.swampOwlExitDeadline = route.frame + 0x100;
        while ((RoomDebugFixture.of(route.session).overworldRoomStatusForTest(0x36) & 0x20) == 0
                && route.frame < route.swampOwlExitDeadline) {
            tickInteractiveEntities(route.session, route.frame++, 0x50, 0x50);
        }
        assertEquals(0x20, RoomDebugFixture.of(route.session).overworldRoomStatusForTest(0x36) & 0x20);
        route.swampOwlDepartureDeadline = route.frame + 0x100;
        while (route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                        .anyMatch(entity -> entity.type() == 0x41)
                && route.frame < route.swampOwlDepartureDeadline) {
            tickInteractiveEntities(route.session, route.frame++, 0x50, 0x50);
        }
        assertFalse(route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                .anyMatch(entity -> entity.type() == 0x41));

        route.routeToGopongaSwamp = new int[] {
            ScrollController.LEFT, ScrollController.LEFT, ScrollController.DOWN,
            ScrollController.LEFT, ScrollController.LEFT, ScrollController.LEFT,
            ScrollController.LEFT, ScrollController.UP, ScrollController.RIGHT,
            ScrollController.RIGHT, ScrollController.UP, ScrollController.RIGHT,
            ScrollController.DOWN
        };
        route.gopongaSwampRouteRooms =
                new int[] {0x35, 0x34, 0x44, 0x43, 0x42, 0x41, 0x40, 0x30, 0x31, 0x32, 0x22, 0x23, 0x33};
        for (int index = 0; index < route.routeToGopongaSwamp.length; index++) {
            // Enter the eastern swamp channel, where both flowers are reachable.
            int next = index + 1 < route.routeToGopongaSwamp.length
                    ? route.routeToGopongaSwamp[index + 1]
                    : ScrollController.RIGHT;
            walkToAndCrossOverworldBoundary(
                    route.session,
                    route.coordinator,
                    route.scroll,
                    route.collision,
                    route.link,
                    route.routeToGopongaSwamp[index],
                    next);
            assertEquals(
                    route.gopongaSwampRouteRooms[index], route.session.rooms().currentRoomId());
        }
        assertEquals(
                2,
                route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                        .filter(entity -> entity.type() == 0x7C || entity.type() == 0x7E)
                        .count());
        route.frame = letBowWowEatAllGoponga(route.session, route.collision, route.link, route.frame);

        walkToAndCrossOverworldBoundary(
                route.session, route.coordinator, route.scroll, route.collision, route.link, ScrollController.UP);
        assertEquals(0x23, route.session.rooms().currentRoomId());
        assertEquals(
                1,
                route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                        .filter(entity -> entity.type() == 0x7C)
                        .count());
        route.frame = letBowWowEatAllGoponga(route.session, route.collision, route.link, route.frame);

        walkToAndCrossOverworldBoundary(
                route.session, route.coordinator, route.scroll, route.collision, route.link, ScrollController.DOWN);
        assertEquals(0x33, route.session.rooms().currentRoomId());
        assertFalse(route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                .anyMatch(entity -> entity.type() == 0x7C || entity.type() == 0x7E));
        walkToAndCrossOverworldBoundary(
                route.session, route.coordinator, route.scroll, route.collision, route.link, ScrollController.UP);
        assertEquals(0x23, route.session.rooms().currentRoomId());
        assertFalse(route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                .anyMatch(entity -> entity.type() == 0x7C));

        walkToAndCrossOverworldBoundary(
                route.session, route.coordinator, route.scroll, route.collision, route.link, ScrollController.LEFT);
        assertEquals(0x22, route.session.rooms().currentRoomId());
        assertEquals(
                2,
                route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                        .filter(entity -> entity.type() == 0x7E)
                        .count());
        route.frame = letBowWowEatAllGoponga(route.session, route.collision, route.link, route.frame);
        walkToAndCrossOverworldBoundary(
                route.session, route.coordinator, route.scroll, route.collision, route.link, ScrollController.RIGHT);
        assertEquals(0x23, route.session.rooms().currentRoomId());
        assertFalse(route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                .anyMatch(entity -> entity.type() == 0x7C));
    }

    static void enterBottleGrotto(GameplayRouteState route) throws Exception {

        walkToAndCrossOverworldBoundary(
                route.session, route.coordinator, route.scroll, route.collision, route.link, ScrollController.RIGHT);
        assertEquals(0x24, route.session.rooms().currentRoomId());
        assertEquals(
                5,
                route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                        .filter(entity -> entity.type() == 0x7E)
                        .count());
        assertTrue(route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                .anyMatch(entity -> entity.type() == 0x6D));
        route.frame = letBowWowEatAllGoponga(route.session, route.collision, route.link, route.frame);

        walkToAndCrossOverworldBoundary(
                route.session, route.coordinator, route.scroll, route.collision, route.link, ScrollController.LEFT);
        assertEquals(0x23, route.session.rooms().currentRoomId());
        walkToAndCrossOverworldBoundary(
                route.session, route.coordinator, route.scroll, route.collision, route.link, ScrollController.RIGHT);
        assertEquals(0x24, route.session.rooms().currentRoomId());
        assertFalse(route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                .anyMatch(entity -> entity.type() == 0x7E));
        assertTrue(route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                .anyMatch(entity -> entity.type() == 0x6D));

        route.bottleGrottoEntrance = route.session.rooms().activeRoomOrNull().warps().stream()
                .filter(warp -> warp.destMap() == 0x01 && warp.destRoom() == 0x36)
                .findFirst()
                .orElseThrow();
        assertEquals(0x13, route.bottleGrottoEntrance.tileLocation());
        route.entrancePath = reachableWarpPath(
                route.collision,
                route.link.position().pixelX(),
                route.link.position().pixelY(),
                route.bottleGrottoEntrance.tileLocation());
        assertNotNull(
                route.entrancePath,
                "No collision-valid path from room $24's west entry " + "to Bottle Grotto warp $13\n"
                        + collisionGrid(route.collision));
        for (int[] position : route.entrancePath) {
            route.link.position().setPixelPosition(position[0], position[1]);
        }
        assertEquals(
                0x13,
                Warp.packTileLocation(
                        route.link.position().pixelX(), route.link.position().pixelY()));
        route.link.position().setPixelPosition(0x30, 0x10); // align both probes with the entrance lane
        walkIntoDoor(route.link, route.inputState, route.inputConfig, route.coordinator, route.transition);
        assertTrue(route.transition.isActive());
        while (route.transition.isActive()) {
            route.transition.tick();
        }

        assertEquals(Warp.CATEGORY_INDOOR, route.session.rooms().mapCategory());
        assertEquals(0x01, route.session.rooms().activeRoomOrNull().mapId());
        assertEquals(0x36, route.session.rooms().currentRoomId());
        assertEquals(0x48, route.link.position().pixelX());
        assertEquals(0x6C, route.link.position().pixelY());
        assertEquals(0x3A, route.session.rooms().indoorRoomPositionForSave());
        assertEquals(0x01, route.session.story().bowWowState());
        assertFalse(route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                .anyMatch(entity -> entity.type() == 0x6D));
        assertEquals(
                1,
                route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                        .filter(entity -> entity.type() == 0x61)
                        .count());
        assertEquals(
                4,
                route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                        .filter(entity -> entity.type() == 0x2D)
                        .count());
        route.bottleGrottoExit = route.session.rooms().activeRoomOrNull().warps().stream()
                .filter(warp -> warp.destMap() == 0x00 && warp.destRoom() == 0x24)
                .findFirst()
                .orElseThrow();
        assertEquals(0x38, route.bottleGrottoExit.destX());
        assertEquals(0x22, route.bottleGrottoExit.destY());

        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.UP);
        assertEquals(0x31, route.session.rooms().currentRoomId());
        assertEquals(0x25, RoomDebugFixture.of(route.session).activeRoomEventForTest());
        assertEquals(0xAB, objectAt(route.session, 0x34));
        assertEquals(0xAB, objectAt(route.session, 0x35));
        assertEquals(0x08, route.session.rooms().activeRoomOrNull().shutterDoorMask() & 0x08);
        route.firstTorchApproach = reachablePowderApproach(
                route.collision,
                route.link.position().pixelX(),
                route.link.position().pixelY(),
                0x34);
        assertNotNull(route.firstTorchApproach, collisionGrid(route.collision));
        for (int[] position : route.firstTorchApproach.path()) {
            route.link.position().setPixelPosition(position[0], position[1]);
        }
        assertTrue(route.session
                .playerItems()
                .sprinkleMagicPowder(
                        route.link.position().romEntityX(),
                        route.link.position().romEntityY(),
                        0,
                        route.firstTorchApproach.romDirection()));
        assertTrue(route.playerState.ammunition().consumeMagicPowder());
        for (int tick = 0; tick < 16; tick++) {
            tickInteractiveEntities(
                    route.session,
                    route.frame++,
                    route.link.position().romEntityX(),
                    route.link.position().romEntityY());
        }
        route.secondTorchApproach = reachablePowderApproach(
                route.collision,
                route.link.position().pixelX(),
                route.link.position().pixelY(),
                0x35);
        assertNotNull(route.secondTorchApproach, collisionGrid(route.collision));
        for (int[] position : route.secondTorchApproach.path()) {
            route.link.position().setPixelPosition(position[0], position[1]);
        }
        assertTrue(route.session
                .playerItems()
                .sprinkleMagicPowder(
                        route.link.position().romEntityX(),
                        route.link.position().romEntityY(),
                        0,
                        route.secondTorchApproach.romDirection()));
        assertTrue(route.playerState.ammunition().consumeMagicPowder());
        for (int tick = 0; tick < 16; tick++) {
            tickInteractiveEntities(
                    route.session,
                    route.frame++,
                    route.link.position().romEntityX(),
                    route.link.position().romEntityY());
        }
        assertEquals(2, RoomDebugFixture.of(route.session).roomTriggerCountForTest());
        assertEquals(0, RoomDebugFixture.of(route.session).activeRoomEventForTest());
        assertEquals(0, route.session.rooms().activeRoomOrNull().shutterDoorMask() & 0x08);

        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.RIGHT);
        assertEquals(0x32, route.session.rooms().currentRoomId());
        assertEquals(0x81, RoomDebugFixture.of(route.session).activeRoomEventForTest());
        route.bottleStalfosSlots = route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x1A || entity.type() == 0x1E)
                .map(RoomEntity::slot)
                .toList();
        assertEquals(2, route.bottleStalfosSlots.size());
        route.session
                .entities()
                .tickEntitiesWithProjectileEvents(
                        route.frame++,
                        route.link.position().romEntityX(),
                        route.link.position().romEntityY(),
                        0,
                        0,
                        Direction.RIGHT.javaIndex(),
                        false);
        for (int stalfosSlot : route.bottleStalfosSlots) {
            for (int hit = 0;
                    hit < 8
                            && route.session
                                            .rooms()
                                            .activeRoomOrNull()
                                            .entities()
                                            .slots()
                                            .get(stalfosSlot)
                                            .status()
                                    == EntityStatus.ACTIVE;
                    hit++) {
                RoomEntity stalfos = route.session
                        .rooms()
                        .activeRoomOrNull()
                        .entities()
                        .slots()
                        .get(stalfosSlot);
                List<int[]> combatPath = reachableEntityContactPath(
                        route.collision,
                        route.link.position().pixelX(),
                        route.link.position().pixelY(),
                        stalfos);
                assertNotNull(combatPath, collisionGrid(route.collision));
                for (int[] position : combatPath) {
                    route.link.position().setPixelPosition(position[0], position[1]);
                }
                route.session
                        .entities()
                        .resolveEntityCombat(new EntityCombatInputs(
                                stalfosSlot ^ 1,
                                route.link.position().romEntityX(),
                                route.link.position().romEntityY(),
                                false,
                                0,
                                true,
                                new SwordBox(true, stalfos.x(), 0x08, stalfos.y(), 0x08),
                                linksawakening.world.entity.EnemyAttackContext.standard()));
                for (int recovery = 0; recovery < 0x20; recovery++) {
                    route.session
                            .entities()
                            .tickEntitiesWithProjectileEvents(
                                    route.frame++,
                                    route.link.position().romEntityX(),
                                    route.link.position().romEntityY(),
                                    0,
                                    0,
                                    Direction.RIGHT.javaIndex(),
                                    false);
                }
            }
        }
        route.firstBottleKeyDeadline = route.frame + 0x300;
        while (RoomDebugFixture.of(route.session).activeRoomEventForTest() != 0
                && route.frame < route.firstBottleKeyDeadline) {
            route.session
                    .entities()
                    .tickEntitiesWithProjectileEvents(
                            route.frame++,
                            route.link.position().romEntityX(),
                            route.link.position().romEntityY(),
                            0,
                            0,
                            Direction.RIGHT.javaIndex(),
                            false);
        }
        assertEquals(
                0,
                RoomDebugFixture.of(route.session).activeRoomEventForTest(),
                route.session
                        .rooms()
                        .activeRoomOrNull()
                        .entities()
                        .loadedEntities()
                        .toString());
        route.firstBottleKey = route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x30)
                .findFirst()
                .orElseThrow();
        route.bottleKeyLandingDeadline = route.frame + 0x200;
        while (route.firstBottleKey.z() != 0 && route.frame < route.bottleKeyLandingDeadline) {
            route.session
                    .entities()
                    .tickEntitiesWithProjectileEvents(
                            route.frame++,
                            route.link.position().romEntityX(),
                            route.link.position().romEntityY(),
                            0,
                            0,
                            Direction.DOWN.javaIndex(),
                            false);
            route.firstBottleKey =
                    route.session.rooms().activeRoomOrNull().entities().slots().get(route.firstBottleKey.slot());
        }
        assertEquals(0, route.firstBottleKey.z());
        route.keyPath = reachableEntityContactPath(
                route.collision,
                route.link.position().pixelX(),
                route.link.position().pixelY(),
                route.firstBottleKey);
        assertNotNull(route.keyPath, collisionGrid(route.collision));
        for (int[] position : route.keyPath) {
            route.link.position().setPixelPosition(position[0], position[1]);
        }
        route.bottleKeyPickupFrame = (route.firstBottleKey.slot() & 0x01) == 0 ? route.frame | 1 : route.frame & ~1;
        assertNotNull(route.session
                .entities()
                .collectEntityIfNeeded(
                        route.bottleKeyPickupFrame,
                        route.link.position().romEntityX(),
                        route.link.position().romEntityY(),
                        false,
                        true,
                        Direction.DOWN.javaIndex(),
                        0));
        assertEquals(1, route.session.dungeon().currentDungeonItemFlagsSnapshot()[DungeonItemState.SMALL_KEYS_INDEX]);
        assertEquals(18, route.playerState.ammunition().magicPowderCount());

        // Room $33 is the first post-key Bottle Grotto room.  Enter it through
        // the live collision/scroll path so its ROM-authored state is part of
        // the same ordered-play trace.
        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.RIGHT);
        assertEquals(0x33, route.session.rooms().currentRoomId());
        assertEquals(0x00, RoomDebugFixture.of(route.session).entitySwitchBlocksStateForTest());
        route.room33RaisedBlockLocation = 0x62;
        assertEquals(0xDC, objectAt(route.session, route.room33RaisedBlockLocation));
        assertTrue(
                RoomDebugFixture.of(route.session)
                        .linkCollisionPointBlockedForTest(
                                (route.room33RaisedBlockLocation & 0x0F) << 4, route.room33RaisedBlockLocation & 0xF0),
                "room $33 raised $DC must block before the switch animation at "
                        + String.format("%02X", route.room33RaisedBlockLocation));
        assertEquals(
                6,
                java.util.stream.IntStream.range(0, 6)
                        .filter(offset -> objectAt(route.session, 0x62 + offset) == 0xDC)
                        .count());
        assertEquals(0xC0, objectAt(route.session, 0x23));
        assertEquals(0xC0, objectAt(route.session, 0x26));
        assertEquals(
                1,
                route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                        .filter(entity -> entity.type() == 0x66)
                        .count());
        assertEquals(
                1,
                route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                        .filter(entity -> entity.type() == 0x42)
                        .count());
        assertEquals(
                1,
                route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                        .filter(entity -> entity.type() == 0x16)
                        .count());
        tickInteractiveEntities(
                route.session,
                route.frame++,
                route.link.position().romEntityX(),
                route.link.position().romEntityY());

        route.crystalSwitch = route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x66)
                .findFirst()
                .orElseThrow();
        route.switchPath = reachableEntityContactPath(
                route.collision,
                route.link.position().pixelX(),
                route.link.position().pixelY(),
                route.crystalSwitch);
        assertNotNull(route.switchPath, collisionGrid(route.collision));
        for (int[] position : route.switchPath) {
            route.link.position().setPixelPosition(position[0], position[1]);
        }
        route.liveSword = new Sword(route.romTables, null);
        route.liveSword.onPress();
        for (int swingFrame = 0; swingFrame < 4; swingFrame++) {
            route.liveSword.tick(false);
        }
        route.link.animation().setDirection(Direction.RIGHT.javaIndex());
        route.initialSwordBox = route.liveSword.enemyCollisionBox(
                route.link.position().romEntityX(),
                route.link.position().romSwordCollisionY(),
                route.link.animation().direction());
        route.link
                .position()
                .setPixelPosition(
                        route.crystalSwitch.x()
                                - (route.initialSwordBox.x()
                                        - route.link.position().pixelX()),
                        route.crystalSwitch.y()
                                - (route.initialSwordBox.y()
                                        - route.link.position().pixelY()));
        route.swordBox = route.liveSword.enemyCollisionBox(
                route.link.position().romEntityX(),
                route.link.position().romSwordCollisionY(),
                route.link.animation().direction());
        assertTrue(route.swordBox.active());
        assertTrue(
                linksawakening.world.entity.EntityCombatTestAccess.overlapsSword(
                        route.crystalSwitch,
                        route.swordBox.x(),
                        route.swordBox.width(),
                        route.swordBox.y(),
                        route.swordBox.height()),
                "sword=" + route.swordBox + " crystal=" + route.crystalSwitch);
        route.switchHit = route.session
                .entities()
                .resolveEntityCombat(new EntityCombatInputs(
                        route.frame++,
                        route.link.position().romEntityX(),
                        route.link.position().romEntityY(),
                        false,
                        0,
                        true,
                        new SwordBox(
                                true,
                                route.swordBox.x(),
                                route.swordBox.width(),
                                route.swordBox.y(),
                                route.swordBox.height()),
                        linksawakening.world.entity.EnemyAttackContext.standard()));
        assertEquals(EntityStatus.ACTIVE, route.crystalSwitch.status());
        assertTrue(
                route.switchHit.stream()
                        .anyMatch(event ->
                                event.slot() == route.crystalSwitch.slot() && event.type() == 0x66 && event.swordHit()),
                route.switchHit.toString());
        tickInteractiveEntities(
                route.session,
                route.frame++,
                route.link.position().romEntityX(),
                route.link.position().romEntityY());
        assertEquals(0x01, RoomDebugFixture.of(route.session).switchableObjectAnimationStageForTest());
        assertTrue(route.session.consumeEntityEvents().stream()
                .anyMatch(event -> event.type() == 0x66
                        && event.soundChannel() == EntityCombatEvent.SoundChannel.WAVE
                        && event.soundId() == 0x0E));

        assertTrue(RoomDebugFixture.of(route.session).linkCollisionPointBlockedForTest(0x20, 0x60));
        for (int stage = 0; stage < 9; stage++) {
            route.session.entityState().tickGameplayVBlank();
        }
        assertEquals(0x02, RoomDebugFixture.of(route.session).entitySwitchBlocksStateForTest());
        assertEquals(0x00, RoomDebugFixture.of(route.session).switchableObjectAnimationStageForTest());
        assertFalse(
                RoomDebugFixture.of(route.session)
                        .linkCollisionPointBlockedForTest(
                                (route.room33RaisedBlockLocation & 0x0F) << 4, route.room33RaisedBlockLocation & 0xF0),
                "room $33 raised $DC must pass after the switch animation");

        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.RIGHT);
        assertEquals(0x34, route.session.rooms().currentRoomId());
        assertEquals(0x81, RoomDebugFixture.of(route.session).activeRoomEventForTest());
        assertEquals(
                2,
                route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                        .filter(entity -> entity.type() == 0x8F)
                        .count());
        assertEquals(
                1,
                route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                        .filter(entity -> entity.type() == 0x2E)
                        .count());
        assertEquals(0x02, RoomDebugFixture.of(route.session).entitySwitchBlocksStateForTest());
        tickInteractiveEntities(
                route.session,
                route.frame++,
                route.link.position().romEntityX(),
                route.link.position().romEntityY());
        assertEquals(
                2,
                route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                        .filter(entity -> entity.type() == 0x8F)
                        .count());
        assertEquals(
                1,
                route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                        .filter(entity -> entity.type() == 0x2E)
                        .count());

        route.maskedMimicSlots = route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x8F)
                .map(RoomEntity::slot)
                .toList();
        route.defeatedMaskedMimics = 0;
        for (int maskedMimicSlot : route.maskedMimicSlots) {
            for (int hit = 0; hit < 2; hit++) {
                RoomEntity maskedMimic = route.session
                        .rooms()
                        .activeRoomOrNull()
                        .entities()
                        .slots()
                        .get(maskedMimicSlot);
                assertEquals(EntityStatus.ACTIVE, maskedMimic.status());
                int linkEntityX = maskedMimic.x();
                int linkEntityY = (maskedMimic.y() + 0x20) & 0xFF;
                // The first handler tick mirrors Link's down input. The next
                // sees Link behind that facing and selects vulnerable options $08.
                route.session.setEntityPressedButtonsMask(0x08);
                tickInteractiveEntities(route.session, route.frame++, linkEntityX, linkEntityY);
                tickInteractiveEntities(route.session, route.frame++, linkEntityX, linkEntityY);
                maskedMimic = route.session
                        .rooms()
                        .activeRoomOrNull()
                        .entities()
                        .slots()
                        .get(maskedMimicSlot);
                List<EntityCombatEvent> hitEvents = route.session
                        .entities()
                        .resolveEntityCombat(new EntityCombatInputs(
                                route.frame++,
                                linkEntityX,
                                linkEntityY,
                                false,
                                0,
                                true,
                                new SwordBox(true, maskedMimic.x() + 0x08, 0x08, maskedMimic.y() + 0x08, 0x08),
                                linksawakening.world.entity.EnemyAttackContext.standard()));
                assertTrue(
                        hitEvents.stream()
                                .anyMatch(event -> event.slot() == maskedMimicSlot
                                        && event.swordHit()
                                        && event.enemyDamage() == 1),
                        hitEvents.toString());
                route.session.setEntityPressedButtonsMask(0);
                for (int recovery = 0; recovery < 0x30; recovery++) {
                    route.session
                            .entities()
                            .tickEntitiesWithProjectileEvents(
                                    route.frame++,
                                    route.link.position().romEntityX(),
                                    route.link.position().romEntityY(),
                                    0,
                                    0,
                                    route.link.animation().direction(),
                                    false);
                }
            }
            int mimicDeathDeadline = route.frame + 0x200;
            while (route.session
                                    .rooms()
                                    .activeRoomOrNull()
                                    .entities()
                                    .slots()
                                    .get(maskedMimicSlot)
                                    .status()
                            != EntityStatus.DISABLED
                    && route.frame < mimicDeathDeadline) {
                tickInteractiveEntities(
                        route.session,
                        route.frame++,
                        route.link.position().romEntityX(),
                        route.link.position().romEntityY());
            }
            assertEquals(
                    EntityStatus.DISABLED,
                    route.session
                            .rooms()
                            .activeRoomOrNull()
                            .entities()
                            .slots()
                            .get(maskedMimicSlot)
                            .status());
            route.defeatedMaskedMimics++;
            if (route.defeatedMaskedMimics == 1) {
                assertEquals(0x81, RoomDebugFixture.of(route.session).activeRoomEventForTest());
                assertEquals(
                        0,
                        route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                                .filter(entity -> entity.type() == 0x30)
                                .count());
            }
        }
        route.session.setEntityPressedButtonsMask(0);
        route.mimicEventDeadline = route.frame + 0x300;
        while (RoomDebugFixture.of(route.session).activeRoomEventForTest() != 0
                && route.frame < route.mimicEventDeadline) {
            tickInteractiveEntities(
                    route.session,
                    route.frame++,
                    route.link.position().romEntityX(),
                    route.link.position().romEntityY());
        }
        assertEquals(0, RoomDebugFixture.of(route.session).activeRoomEventForTest());
        assertTrue(route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                .anyMatch(entity -> entity.type() == 0x2E));
        route.secondBottleKey = route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x30)
                .findFirst()
                .orElseThrow();
        route.secondKeyLandingDeadline = route.frame + 0x200;
    }

    static void collectBottleGrottoKeys(GameplayRouteState route) throws Exception {

        while (route.secondBottleKey.z() != 0 && route.frame < route.secondKeyLandingDeadline) {
            tickInteractiveEntities(
                    route.session,
                    route.frame++,
                    route.link.position().romEntityX(),
                    route.link.position().romEntityY());
            route.secondBottleKey =
                    route.session.rooms().activeRoomOrNull().entities().slots().get(route.secondBottleKey.slot());
        }
        assertEquals(0, route.secondBottleKey.z());
        route.secondKeyPath = reachableEntityContactPath(
                route.collision,
                route.link.position().pixelX(),
                route.link.position().pixelY(),
                route.secondBottleKey);
        assertNotNull(route.secondKeyPath, collisionGrid(route.collision));
        for (int[] position : route.secondKeyPath) {
            route.link.position().setPixelPosition(position[0], position[1]);
        }
        route.secondKeyPickupFrame = (route.secondBottleKey.slot() & 0x01) == 0 ? route.frame | 1 : route.frame & ~1;
        assertNotNull(route.session
                .entities()
                .collectEntityIfNeeded(
                        route.secondKeyPickupFrame,
                        route.link.position().romEntityX(),
                        route.link.position().romEntityY(),
                        false,
                        true,
                        Direction.DOWN.javaIndex(),
                        0));
        assertEquals(2, route.session.dungeon().currentDungeonItemFlagsSnapshot()[DungeonItemState.SMALL_KEYS_INDEX]);
        assertEquals(0x10, RoomDebugFixture.of(route.session).indoorRoomStatusForTest(0x01, 0x34) & 0x10);

        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.LEFT);
        assertEquals(0x33, route.session.rooms().currentRoomId());
        tickInteractiveEntities(
                route.session,
                route.frame++,
                route.link.position().romEntityX(),
                route.link.position().romEntityY());
        route.returnCrystalSwitch = route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x66)
                .findFirst()
                .orElseThrow();
        route.returnSwitchPath = reachableEntityContactPath(
                route.collision,
                route.link.position().pixelX(),
                route.link.position().pixelY(),
                route.returnCrystalSwitch);
        assertNotNull(route.returnSwitchPath, collisionGrid(route.collision));
        for (int[] position : route.returnSwitchPath) {
            route.link.position().setPixelPosition(position[0], position[1]);
        }
        route.returnSwitchSword = new Sword(route.romTables, null);
        route.returnSwitchSword.onPress();
        for (int swingFrame = 0; swingFrame < 4; swingFrame++) {
            route.returnSwitchSword.tick(false);
        }
        route.link.animation().setDirection(Direction.RIGHT.javaIndex());
        route.initialReturnSwordBox = route.returnSwitchSword.enemyCollisionBox(
                route.link.position().romEntityX(),
                route.link.position().romSwordCollisionY(),
                route.link.animation().direction());
        route.link
                .position()
                .setPixelPosition(
                        route.returnCrystalSwitch.x()
                                - (route.initialReturnSwordBox.x()
                                        - route.link.position().pixelX()),
                        route.returnCrystalSwitch.y()
                                - (route.initialReturnSwordBox.y()
                                        - route.link.position().pixelY()));
        route.returnSwordBox = route.returnSwitchSword.enemyCollisionBox(
                route.link.position().romEntityX(),
                route.link.position().romSwordCollisionY(),
                route.link.animation().direction());
        assertTrue(route.returnSwordBox.active());
        route.returnSwitchHit = route.session
                .entities()
                .resolveEntityCombat(new EntityCombatInputs(
                        route.frame++,
                        route.link.position().romEntityX(),
                        route.link.position().romEntityY(),
                        false,
                        0,
                        true,
                        new SwordBox(
                                true,
                                route.returnSwordBox.x(),
                                route.returnSwordBox.width(),
                                route.returnSwordBox.y(),
                                route.returnSwordBox.height()),
                        linksawakening.world.entity.EnemyAttackContext.standard()));
        route.currentReturnCrystalSwitch =
                route.session.rooms().activeRoomOrNull().entities().slots().get(route.returnCrystalSwitch.slot());
        assertEquals(EntityStatus.ACTIVE, route.currentReturnCrystalSwitch.status());
        assertTrue(
                route.returnSwitchHit.stream()
                        .anyMatch(event -> event.slot() == route.returnCrystalSwitch.slot()
                                && event.type() == 0x66
                                && event.swordHit()),
                route.returnSwitchHit.toString());
        tickInteractiveEntities(
                route.session,
                route.frame++,
                route.link.position().romEntityX(),
                route.link.position().romEntityY());
        assertEquals(0x01, RoomDebugFixture.of(route.session).switchableObjectAnimationStageForTest());
        assertTrue(route.session.consumeEntityEvents().stream()
                .anyMatch(event -> event.type() == 0x66
                        && event.soundChannel() == EntityCombatEvent.SoundChannel.WAVE
                        && event.soundId() == 0x0E));
        for (int stage = 0; stage < 9; stage++) {
            route.session.entityState().tickGameplayVBlank();
        }
        assertEquals(0x00, RoomDebugFixture.of(route.session).entitySwitchBlocksStateForTest());
        assertEquals(0x00, RoomDebugFixture.of(route.session).switchableObjectAnimationStageForTest());
        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.LEFT);
        assertEquals(0x32, route.session.rooms().currentRoomId());
        assertEquals(2, route.session.dungeon().currentDungeonItemFlagsSnapshot()[DungeonItemState.SMALL_KEYS_INDEX]);
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
                route.link.position().pixelX(),
                route.link.position().pixelY(),
                (x, y) -> x == 0x48
                        && (route.collision.objectPhysicsFlagAtPoint(x + 0x06, y + 1 + 0x0F) == 0x91
                                || route.collision.objectPhysicsFlagAtPoint(x + 0x09, y + 1 + 0x0F) == 0x91));
        assertNotNull(route.bottomDoorPath, collisionGrid(route.collision));
        for (int[] position : route.bottomDoorPath) {
            route.link.position().setPixelPosition(position[0], position[1]);
        }
        route.bottomDoorContactX = route.link.position().pixelX();
        route.bottomDoorContactY = route.link.position().pixelY();
        route.link.position().setPixelPosition(route.bottomDoorContactX, route.bottomDoorContactY + 1);
        assertEquals(route.bottomDoorContactX, route.link.position().pixelX());
        assertEquals(route.bottomDoorContactY + 1, route.link.position().pixelY());
        assertTrue(route.collision.objectPhysicsFlagAtPoint(
                                route.link.position().pixelX() + 0x06,
                                route.link.position().pixelY() + 0x0F)
                        == 0x91
                || route.collision.objectPhysicsFlagAtPoint(
                                route.link.position().pixelX() + 0x09,
                                route.link.position().pixelY() + 0x0F)
                        == 0x91);
        assertTrue(route.session
                .doors()
                .tryUnlockIndoorKeyDoor(
                        route.link.position().pixelX(),
                        route.link.position().pixelY(),
                        Direction.fromJavaOrUnknown(Direction.DOWN.javaIndex()),
                        0x02));
        assertEquals(1, route.session.dungeon().currentDungeonItemFlagsSnapshot()[DungeonItemState.SMALL_KEYS_INDEX]);
        for (int tick = 0; tick < 8; tick++) {
            route.session
                    .entities()
                    .tickEntities(
                            route.frame++,
                            route.link.position().pixelX(),
                            route.link.position().pixelY());
            assertTrue(route.session.components.entityBridge().consumeWorldLinkMotionBlockRequest());
        }
        route.session
                .entities()
                .tickEntities(
                        route.frame++,
                        route.link.position().pixelX(),
                        route.link.position().pixelY());
        assertFalse(route.session.components.entityBridge().consumeWorldLinkMotionBlockRequest());
        assertEquals(0x8C, objectAt(route.session, route.bottomKeyDoorLocation));
        assertEquals(0x08, objectAt(route.session, route.bottomKeyDoorLocation + 1));
        assertEquals(0x08, RoomDebugFixture.of(route.session).indoorRoomStatusForTest(0x01, 0x32) & 0x08);
        assertEquals(0x04, RoomDebugFixture.of(route.session).indoorRoomStatusForTest(0x01, 0x37) & 0x04);

        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.DOWN);
        assertEquals(0x37, route.session.rooms().currentRoomId());
        assertEquals(0x61, RoomDebugFixture.of(route.session).activeRoomEventForTest());
        assertEquals(
                1,
                route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                        .filter(entity -> entity.type() == 0x8F)
                        .count());
        route.room37RupeeSlots = route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x2E)
                .map(RoomEntity::slot)
                .toList();
        assertEquals(2, route.room37RupeeSlots.size());
        tickInteractiveEntities(
                route.session,
                route.frame++,
                route.link.position().romEntityX(),
                route.link.position().romEntityY());

        route.maskedMimic = route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x8F)
                .findFirst()
                .orElseThrow();
        route.maskedMimicSlot = route.maskedMimic.slot();
        for (int hit = 0; hit < 2; hit++) {
            route.maskedMimic =
                    route.session.rooms().activeRoomOrNull().entities().slots().get(route.maskedMimicSlot);
            assertEquals(EntityStatus.ACTIVE, route.maskedMimic.status());
            int linkEntityX = route.maskedMimic.x();
            int linkEntityY = (route.maskedMimic.y() + 0x20) & 0xFF;
            route.session.setEntityPressedButtonsMask(0x08);
            tickInteractiveEntities(route.session, route.frame++, linkEntityX, linkEntityY);
            tickInteractiveEntities(route.session, route.frame++, linkEntityX, linkEntityY);
            route.maskedMimic =
                    route.session.rooms().activeRoomOrNull().entities().slots().get(route.maskedMimicSlot);
            List<EntityCombatEvent> hitEvents = route.session
                    .entities()
                    .resolveEntityCombat(new EntityCombatInputs(
                            route.frame++,
                            linkEntityX,
                            linkEntityY,
                            false,
                            0,
                            true,
                            new SwordBox(true, route.maskedMimic.x() + 0x08, 0x08, route.maskedMimic.y() + 0x08, 0x08),
                            linksawakening.world.entity.EnemyAttackContext.standard()));
            assertTrue(
                    hitEvents.stream()
                            .anyMatch(event -> event.slot() == route.maskedMimicSlot
                                    && event.swordHit()
                                    && event.enemyDamage() == 1),
                    hitEvents.toString());
            if (hit == 1) {
                assertEquals(0x61, RoomDebugFixture.of(route.session).activeRoomEventForTest());
                assertEquals(
                        2,
                        route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                                .filter(entity -> entity.type() == 0x2E)
                                .count());
            }
            route.session.setEntityPressedButtonsMask(0);
            for (int recovery = 0; recovery < 0x30; recovery++) {
                route.session
                        .entities()
                        .tickEntitiesWithProjectileEvents(
                                route.frame++,
                                route.link.position().romEntityX(),
                                route.link.position().romEntityY(),
                                0,
                                0,
                                route.link.animation().direction(),
                                false);
            }
        }
        route.mimicDeathDeadline = route.frame + 0x200;
        while (route.session
                                .rooms()
                                .activeRoomOrNull()
                                .entities()
                                .slots()
                                .get(route.maskedMimicSlot)
                                .status()
                        != EntityStatus.DISABLED
                && route.frame < route.mimicDeathDeadline) {
            tickInteractiveEntities(
                    route.session,
                    route.frame++,
                    route.link.position().romEntityX(),
                    route.link.position().romEntityY());
        }
        assertEquals(
                EntityStatus.DISABLED,
                route.session
                        .rooms()
                        .activeRoomOrNull()
                        .entities()
                        .slots()
                        .get(route.maskedMimicSlot)
                        .status());

        route.room37EventDeadline = route.frame + 0x300;
        while (RoomDebugFixture.of(route.session).activeRoomEventForTest() != 0
                && route.frame < route.room37EventDeadline) {
            tickInteractiveEntities(
                    route.session,
                    route.frame++,
                    route.link.position().romEntityX(),
                    route.link.position().romEntityY());
        }
        assertEquals(0, RoomDebugFixture.of(route.session).activeRoomEventForTest());
        route.compassChestIndex = ROOM_OBJECTS_BASE + 0x28;
        route.chestRevealDeadline = route.frame + 0x40;
        while (route.session.rooms().activeRoomOrNull().roomObjectsArea()[route.compassChestIndex] != 0xA0
                && route.frame < route.chestRevealDeadline) {
            route.session
                    .entities()
                    .tickEntities(
                            route.frame++,
                            route.link.position().romEntityX(),
                            route.link.position().romEntityY());
        }
        assertEquals(0xA0, route.session.rooms().activeRoomOrNull().roomObjectsArea()[route.compassChestIndex]);
        assertEquals(
                2,
                route.room37RupeeSlots.stream()
                        .filter(slot -> {
                            RoomEntity entity = route.session
                                    .rooms()
                                    .activeRoomOrNull()
                                    .entities()
                                    .slots()
                                    .get(slot);
                            return entity.loaded() && entity.type() == 0x2E;
                        })
                        .count());
        assertEquals(0, RoomDebugFixture.of(route.session).indoorRoomStatusForTest(0x01, 0x37) & 0x10);
        route.compassChestPath = reachablePositionPath(
                route.collision,
                route.link.position().pixelX(),
                route.link.position().pixelY(),
                (x, y) -> Math.floorDiv(x + 0x08, 0x10) == (0x28 & 0x0F)
                        && Math.floorDiv(y + 0x05, 0x10) == ((0x28 & 0xF0) >>> 4));
        assertNotNull(route.compassChestPath, collisionGrid(route.collision));
        for (int[] position : route.compassChestPath) {
            route.link.position().setPixelPosition(position[0], position[1]);
        }
        route.compassChestContactX = route.link.position().pixelX();
        route.compassChestContactY = route.link.position().pixelY();
        route.link.position().setPixelPosition(route.compassChestContactX, route.compassChestContactY - 1);
        assertEquals(route.compassChestContactX, route.link.position().pixelX());
        assertEquals(route.compassChestContactY - 1, route.link.position().pixelY());
        route.compassChestInteractionLocation =
                (Math.floorDiv(route.link.position().pixelY() + 0x05, 0x10) << 4)
                        | Math.floorDiv(route.link.position().pixelX() + 0x08, 0x10);
        assertEquals(0x28, route.compassChestInteractionLocation);
        route.compassChest = route.session
                .doors()
                .tryOpenChest(
                        route.link.position().pixelX(),
                        route.link.position().pixelY(),
                        Direction.fromJavaOrUnknown(Direction.UP.javaIndex()),
                        true,
                        route.playerState.equipmentLevels().swordLevel());
        assertTrue(route.compassChest.opened());
        assertEquals(ChestContentsTable.CHEST_COMPASS, route.compassChest.itemType());
        assertEquals(0x28, route.compassChest.location());
        assertEquals(0x10, RoomDebugFixture.of(route.session).indoorRoomStatusForTest(0x01, 0x37) & 0x10);
        route.compassChestSlot = route.compassChest.entitySlot();
        route.compassRewardObserved = false;
        route.compassDialogObserved = false;
        route.compassChestEnded = false;
        for (int chestTick = 0; chestTick < 0x40; chestTick++) {
            route.session
                    .entities()
                    .tickEntities(
                            route.frame++,
                            route.link.position().romEntityX(),
                            route.link.position().romEntityY());
            for (ChestRewardEvent reward :
                    route.session.components.entityBridge().outbox.consumeChestRewardEvents()) {
                route.compassRewardObserved |= reward.itemType() == ChestContentsTable.CHEST_COMPASS;
                route.playerState.chestRewards().applyChestReward(reward.itemType());
            }
            route.compassDialogObserved |= route.session.entityState().consumeEntityDialogRequests().stream()
                    .anyMatch(request -> request.globalDialogId() == 0x0A7);
            RoomEntity chestEntity =
                    route.session.rooms().activeRoomOrNull().entities().slots().get(route.compassChestSlot);
            if (!chestEntity.loaded() || chestEntity.status() == EntityStatus.DISABLED) {
                route.compassChestEnded = true;
                break;
            }
        }
        assertTrue(route.compassRewardObserved);
        assertTrue(route.compassDialogObserved);
        assertTrue(route.compassChestEnded);
        assertEquals(1, route.session.dungeon().currentDungeonItemFlagsSnapshot()[DungeonItemState.COMPASS_INDEX]);
        assertEquals(1, route.session.dungeon().currentDungeonItemFlagsSnapshot()[DungeonItemState.SMALL_KEYS_INDEX]);

        // MapLayout1 adjacency is not sufficient: room $37's east and west
        // edges are closed. Return north and follow room $32's west opening.
        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.UP);
        assertEquals(0x32, route.session.rooms().currentRoomId());
        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.LEFT);
        assertEquals(0x31, route.session.rooms().currentRoomId());
        // The doorway stays open on entry; its shutter closes only once
        // Link is inside, independently of the minimap's directional bits.
        assertEquals(0x01, RoomDebugFixture.of(route.session).indoorRoomStatusForTest(0x01, 0x31) & 0x01);
        assertEquals(0x02, RoomDebugFixture.of(route.session).indoorRoomStatusForTest(0x01, 0x32) & 0x02);
        assertEquals(0x08, route.session.rooms().activeRoomOrNull().shutterDoorMask());
        assertEquals(0x0B, objectAt(route.session, 0x39));
        assertEquals(0x0C, objectAt(route.session, 0x49));
        // Room $31's west key door is the last Small Key gate in this route.
        assertEquals(0x31, objectAt(route.session, 0x30));
        assertEquals(0x32, objectAt(route.session, 0x40));
        route.westDoorPath = reachablePositionPath(
                route.collision,
                route.link.position().pixelX(),
                route.link.position().pixelY(),
                (x, y) -> y == 0x38
                        && (route.collision.objectPhysicsFlagAtPoint(x + 0x03, y + 0x09) == 0x92
                                || route.collision.objectPhysicsFlagAtPoint(x + 0x03, y + 0x0C) == 0x92));
        assertNotNull(route.westDoorPath, collisionGrid(route.collision));
        for (int[] position : route.westDoorPath) {
            route.link.position().setPixelPosition(position[0], position[1]);
        }
        route.westDoorContactX = route.link.position().pixelX();
        route.westDoorContactY = route.link.position().pixelY();
        route.link.position().setPixelPosition(route.westDoorContactX - 1, route.westDoorContactY);
        assertEquals(route.westDoorContactX - 1, route.link.position().pixelX());
        assertEquals(route.westDoorContactY, route.link.position().pixelY());
        assertTrue(route.collision.objectPhysicsFlagAtPoint(
                                route.link.position().pixelX() + 0x04,
                                route.link.position().pixelY() + 0x09)
                        == 0x92
                || route.collision.objectPhysicsFlagAtPoint(
                                route.link.position().pixelX() + 0x04,
                                route.link.position().pixelY() + 0x0C)
                        == 0x92);
        assertTrue(route.session
                .doors()
                .tryUnlockIndoorKeyDoor(
                        route.link.position().pixelX(),
                        route.link.position().pixelY(),
                        Direction.fromJavaOrUnknown(Direction.LEFT.javaIndex()),
                        0x04));
        assertEquals(0, route.session.dungeon().currentDungeonItemFlagsSnapshot()[DungeonItemState.SMALL_KEYS_INDEX]);
        for (int tick = 0; tick < 8; tick++) {
            route.session
                    .entities()
                    .tickEntities(
                            route.frame++,
                            route.link.position().pixelX(),
                            route.link.position().pixelY());
            assertTrue(route.session.components.entityBridge().consumeWorldLinkMotionBlockRequest());
        }
        route.session
                .entities()
                .tickEntities(
                        route.frame++,
                        route.link.position().pixelX(),
                        route.link.position().pixelY());
        assertFalse(route.session.components.entityBridge().consumeWorldLinkMotionBlockRequest());
        assertEquals(0x09, objectAt(route.session, 0x30));
        assertEquals(0x0A, objectAt(route.session, 0x40));
        assertEquals(0x02, RoomDebugFixture.of(route.session).indoorRoomStatusForTest(0x01, 0x31) & 0x02);
        assertEquals(0x01, RoomDebugFixture.of(route.session).indoorRoomStatusForTest(0x01, 0x30) & 0x01);
        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.LEFT);
        assertEquals(0x30, route.session.rooms().currentRoomId());

        assertEquals(0x21, RoomDebugFixture.of(route.session).activeRoomEventForTest());
        assertEquals(
                4,
                route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                        .filter(entity -> entity.type() == 0x27)
                        .count());
        assertEquals(
                2,
                route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                        .filter(entity -> entity.type() == 0x19)
                        .count());
        assertTrue((route.session.rooms().activeRoomOrNull().shutterDoorMask() & 0x01) != 0);
        route.room30KeeseSlots = route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x19)
                .map(RoomEntity::slot)
                .toList();
    }

    static void clearBottleGrottoEnemies(GameplayRouteState route) throws Exception {

        for (int keeseSlot : route.room30KeeseSlots) {
            RoomEntity keese =
                    route.session.rooms().activeRoomOrNull().entities().slots().get(keeseSlot);
            List<int[]> keesePath = reachableEntityContactPath(
                    route.collision,
                    route.link.position().pixelX(),
                    route.link.position().pixelY(),
                    keese);
            assertNotNull(keesePath, collisionGrid(route.collision));
            for (int[] position : keesePath) {
                route.link.position().setPixelPosition(position[0], position[1]);
            }
            tickInteractiveEntities(
                    route.session,
                    route.frame++,
                    route.link.position().romEntityX(),
                    route.link.position().romEntityY());
            keese = route.session.rooms().activeRoomOrNull().entities().slots().get(keeseSlot);
            int keeseDeltaX = signedByteDelta(keese.x(), route.link.position().romEntityX());
            int keeseDeltaY = signedByteDelta(keese.y(), route.link.position().romEntityY());
            route.link
                    .animation()
                    .setDirection(
                            Math.abs(keeseDeltaX) >= Math.abs(keeseDeltaY)
                                    ? keeseDeltaX < 0 ? Direction.LEFT.javaIndex() : Direction.RIGHT.javaIndex()
                                    : keeseDeltaY < 0 ? Direction.UP.javaIndex() : Direction.DOWN.javaIndex());
            Sword keeseSword = new Sword(route.romTables, null);
            keeseSword.onPress();
            for (int swingFrame = 0; swingFrame < 4; swingFrame++) {
                keeseSword.tick(false);
            }
            Sword.CollisionBox keeseSwordBox = keeseSword.enemyCollisionBox(
                    route.link.position().romEntityX(),
                    route.link.position().romSwordCollisionY(),
                    route.link.animation().direction());
            assertTrue(
                    linksawakening.world.entity.EntityCombatTestAccess.overlapsSword(
                            keese, keeseSwordBox.x(), keeseSwordBox.width(), keeseSwordBox.y(), keeseSwordBox.height()),
                    "sword=" + keeseSwordBox + " keese=" + keese);
            List<EntityCombatEvent> keeseHit = route.session
                    .entities()
                    .resolveEntityCombat(new EntityCombatInputs(
                            route.frame++,
                            route.link.position().romEntityX(),
                            route.link.position().romEntityY(),
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
                tickInteractiveEntities(
                        route.session,
                        route.frame++,
                        route.link.position().romEntityX(),
                        route.link.position().romEntityY());
            }
            int keeseDeathDeadline = route.frame + 0x200;
            while (route.session
                                    .rooms()
                                    .activeRoomOrNull()
                                    .entities()
                                    .slots()
                                    .get(keeseSlot)
                                    .status()
                            != EntityStatus.DISABLED
                    && route.frame < keeseDeathDeadline) {
                tickInteractiveEntities(
                        route.session,
                        route.frame++,
                        route.link.position().romEntityX(),
                        route.link.position().romEntityY());
            }
            assertEquals(
                    EntityStatus.DISABLED,
                    route.session
                            .rooms()
                            .activeRoomOrNull()
                            .entities()
                            .slots()
                            .get(keeseSlot)
                            .status());
            assertEquals(
                    4,
                    route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                            .filter(entity -> entity.type() == 0x27)
                            .count());
            if (route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                    .anyMatch(entity -> entity.type() == 0x19)) {
                assertEquals(0x21, RoomDebugFixture.of(route.session).activeRoomEventForTest());
            }
        }
        route.session.setEntityPressedButtonsMask(0);
        route.room30EventDeadline = route.frame + 0x300;
        while (RoomDebugFixture.of(route.session).activeRoomEventForTest() != 0
                && route.frame < route.room30EventDeadline) {
            tickInteractiveEntities(
                    route.session,
                    route.frame++,
                    route.link.position().romEntityX(),
                    route.link.position().romEntityY());
        }
        assertEquals(0, RoomDebugFixture.of(route.session).activeRoomEventForTest());
        assertEquals(0, route.session.rooms().activeRoomOrNull().shutterDoorMask() & 0x01);
        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.UP);
        assertEquals(0x2E, route.session.rooms().currentRoomId());
        assertEquals(0x21, RoomDebugFixture.of(route.session).activeRoomEventForTest());
        assertEquals(
                1,
                route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                        .filter(entity -> entity.type() == 0x20)
                        .count());
        assertEquals(0x0F, objectAt(route.session, 0x13));
        assertEquals(0x0F, objectAt(route.session, 0x23));
        assertEquals(0x0F, objectAt(route.session, 0x33));
        assertEquals(0xA0, objectAt(route.session, 0x24));

        route.hardhat = route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x20)
                .findFirst()
                .orElseThrow();
        tickInteractiveEntities(
                route.session,
                route.frame++,
                route.link.position().romEntityX(),
                route.link.position().romEntityY());
        route.hardhat =
                route.session.rooms().activeRoomOrNull().entities().slots().get(route.hardhat.slot());
        assertEquals(EntityStatus.ACTIVE, route.hardhat.status());
        assertEquals(0x21, RoomDebugFixture.of(route.session).activeRoomEventForTest());

        route.stoneBeakFeatherPath = reachablePositionPathWithFeather(
                route.collision,
                route.romTables,
                route.link.position().pixelX(),
                route.link.position().pixelY(),
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

        route.stoneBeakChestContactX = route.link.position().pixelX();
        route.stoneBeakChestContactY = route.link.position().pixelY();
        route.link.position().setPixelPosition(route.stoneBeakChestContactX, route.stoneBeakChestContactY - 1);
        route.stoneBeakChestInteractionLocation =
                (Math.floorDiv(route.link.position().pixelY() + 0x05, 0x10) << 4)
                        | Math.floorDiv(route.link.position().pixelX() + 0x08, 0x10);
        assertEquals(0x24, route.stoneBeakChestInteractionLocation);
        route.stoneBeakChest = route.session
                .doors()
                .tryOpenChest(
                        route.link.position().pixelX(),
                        route.link.position().pixelY(),
                        Direction.fromJavaOrUnknown(Direction.UP.javaIndex()),
                        true,
                        route.playerState.equipmentLevels().swordLevel());
        assertTrue(route.stoneBeakChest.opened());
        assertEquals(ChestContentsTable.CHEST_STONE_BEAK, route.stoneBeakChest.itemType());
        assertEquals(0x24, route.stoneBeakChest.location());
        assertEquals(0x10, RoomDebugFixture.of(route.session).indoorRoomStatusForTest(0x01, 0x2E) & 0x10);
        route.stoneBeakChestSlot = route.stoneBeakChest.entitySlot();
        route.stoneBeakDialogId = new ChestContentsTable((route.rom == null ? (Rom) null : TestRoms.partial(route.rom)))
                .dialogLowIdFor(
                        ChestContentsTable.CHEST_STONE_BEAK,
                        route.playerState.equipmentLevels().shieldLevel(),
                        route.playerState.equipmentLevels().swordLevel(),
                        route.playerState.equipmentLevels().powerBraceletLevel(),
                        0x01,
                        0x2E);
        route.stoneBeakRewardObserved = false;
        route.stoneBeakDialogObserved = false;
        route.stoneBeakChestEnded = false;
        for (int chestTick = 0; chestTick < 0x40; chestTick++) {
            route.session
                    .entities()
                    .tickEntities(
                            route.frame++,
                            route.link.position().romEntityX(),
                            route.link.position().romEntityY());
            for (ChestRewardEvent reward :
                    route.session.components.entityBridge().outbox.consumeChestRewardEvents()) {
                route.stoneBeakRewardObserved |= reward.itemType() == ChestContentsTable.CHEST_STONE_BEAK;
                route.playerState.chestRewards().applyChestReward(reward.itemType());
            }
            route.stoneBeakDialogObserved |= route.session.entityState().consumeEntityDialogRequests().stream()
                    .anyMatch(request -> request.globalDialogId() == route.stoneBeakDialogId);
            RoomEntity chestEntity =
                    route.session.rooms().activeRoomOrNull().entities().slots().get(route.stoneBeakChestSlot);
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
                    route.session.rooms().activeRoomOrNull().entities().slots().get(route.hardhat.slot());
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
                route.session
                        .rooms()
                        .activeRoomOrNull()
                        .entities()
                        .loadedEntities()
                        .toString());

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
        tickInteractiveEntities(
                route.session,
                route.frame++,
                route.link.position().romEntityX(),
                route.link.position().romEntityY());

        route.postStoneCrystalSwitch = route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x66)
                .findFirst()
                .orElseThrow();
        route.postStoneSwitchPath = reachableEntityContactPath(
                route.collision,
                route.link.position().pixelX(),
                route.link.position().pixelY(),
                route.postStoneCrystalSwitch);
        assertNotNull(route.postStoneSwitchPath, collisionGrid(route.collision));
        for (int[] position : route.postStoneSwitchPath) {
            route.link.position().setPixelPosition(position[0], position[1]);
        }
        route.postStoneSword = new Sword(route.romTables, null);
        route.postStoneSword.onPress();
        for (int swingFrame = 0; swingFrame < 4; swingFrame++) {
            route.postStoneSword.tick(false);
        }
        route.link.animation().setDirection(Direction.RIGHT.javaIndex());
        route.postStoneInitialSwordBox = route.postStoneSword.enemyCollisionBox(
                route.link.position().romEntityX(),
                route.link.position().romSwordCollisionY(),
                route.link.animation().direction());
        route.link
                .position()
                .setPixelPosition(
                        route.postStoneCrystalSwitch.x()
                                - (route.postStoneInitialSwordBox.x()
                                        - route.link.position().pixelX()),
                        route.postStoneCrystalSwitch.y()
                                - (route.postStoneInitialSwordBox.y()
                                        - route.link.position().pixelY()));
        route.postStoneSwordBox = route.postStoneSword.enemyCollisionBox(
                route.link.position().romEntityX(),
                route.link.position().romSwordCollisionY(),
                route.link.animation().direction());
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
                        route.link.position().romEntityX(),
                        route.link.position().romEntityY(),
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
        tickInteractiveEntities(
                route.session,
                route.frame++,
                route.link.position().romEntityX(),
                route.link.position().romEntityY());
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
                route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                        .filter(entity -> entity.type() == 0x14)
                        .count());
        route.room38MoblinSword = route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x14)
                .findFirst()
                .orElseThrow();
        assertEquals(0x28, route.room38MoblinSword.x());
        assertEquals(0x70, route.room38MoblinSword.y());
        route.room38CrystalSwitch = route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
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
                route.collision,
                route.link.position().pixelX(),
                route.link.position().pixelY(),
                route.room38CrystalSwitch,
                route.romTables);
        assertNotNull(route.room38SwitchApproach, collisionGrid(route.collision));
        for (int[] position : route.room38SwitchApproach.path()) {
            route.link.position().setPixelPosition(position[0], position[1]);
        }
        assertTrue(
                route.collision.objectUnderLinkFeet(
                                route.link.position().pixelX(),
                                route.link.position().pixelY())
                        != 0xDB,
                "sword approach must remain off the raised $DB ring");
        route.link.animation().setDirection(route.room38SwitchApproach.direction());
        for (int tick = 0; tick < 0x10; tick++) {
            tickInteractiveEntities(
                    route.session,
                    route.frame++,
                    route.link.position().romEntityX(),
                    route.link.position().romEntityY());
        }
        route.postStoneLoweringSword = new Sword(route.romTables, null);
        route.postStoneLoweringSword.onPress();
        for (int swingFrame = 0; swingFrame < 4; swingFrame++) {
            route.postStoneLoweringSword.tick(false);
        }
        route.postStoneLoweringBox = route.postStoneLoweringSword.enemyCollisionBox(
                route.link.position().romEntityX(),
                route.link.position().romSwordCollisionY(),
                route.link.animation().direction());
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
                        route.link.position().romEntityX(),
                        route.link.position().romEntityY(),
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
        tickInteractiveEntities(
                route.session,
                route.frame++,
                route.link.position().romEntityX(),
                route.link.position().romEntityY());
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
                route.link.position().pixelX(),
                route.link.position().pixelY(),
                (x, y) -> Math.floorDiv(x + 0x08, 0x10) == (0x43 & 0x0F)
                        && Math.floorDiv(y + 0x05, 0x10) == ((0x43 & 0xF0) >>> 4));
        assertNotNull(route.room38ChestPath, collisionGrid(route.collision));
        for (int[] position : route.room38ChestPath) {
            route.link.position().setPixelPosition(position[0], position[1]);
        }
        route.link.animation().setDirection(Direction.UP.javaIndex());
        route.room38Chest = route.session
                .doors()
                .tryOpenChest(
                        route.link.position().pixelX(),
                        route.link.position().pixelY(),
                        Direction.fromJavaOrUnknown(Direction.UP.javaIndex()),
                        true,
                        route.playerState.equipmentLevels().swordLevel());
        assertTrue(route.room38Chest.opened());
        assertEquals(ChestContentsTable.CHEST_SMALL_KEY, route.room38Chest.itemType());
        assertEquals(0x43, route.room38Chest.location());
        assertEquals(0x10, RoomDebugFixture.of(route.session).indoorRoomStatusForTest(0x01, 0x38) & 0x10);
        route.room38ChestSlot = route.room38Chest.entitySlot();
        route.room38DialogId = new ChestContentsTable((route.rom == null ? (Rom) null : TestRoms.partial(route.rom)))
                .dialogLowIdFor(
                        ChestContentsTable.CHEST_SMALL_KEY,
                        route.playerState.equipmentLevels().shieldLevel(),
                        route.playerState.equipmentLevels().swordLevel(),
                        route.playerState.equipmentLevels().powerBraceletLevel(),
                        0x01,
                        0x38);
        route.room38RewardObserved = false;
        route.room38DialogObserved = false;
        route.room38ChestEnded = false;
        for (int chestTick = 0; chestTick < 0x40; chestTick++) {
            route.session
                    .entities()
                    .tickEntities(
                            route.frame++,
                            route.link.position().romEntityX(),
                            route.link.position().romEntityY());
            for (ChestRewardEvent reward :
                    route.session.components.entityBridge().outbox.consumeChestRewardEvents()) {
                route.room38RewardObserved |= reward.itemType() == ChestContentsTable.CHEST_SMALL_KEY;
                route.playerState.chestRewards().applyChestReward(reward.itemType());
            }
            route.room38DialogObserved |= route.session.entityState().consumeEntityDialogRequests().stream()
                    .anyMatch(request -> request.globalDialogId() == route.room38DialogId);
            RoomEntity chestEntity =
                    route.session.rooms().activeRoomOrNull().entities().slots().get(route.room38ChestSlot);
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

    static void solveBottleGrottoSwitches(GameplayRouteState route) throws Exception {

        assertEquals(1, route.session.dungeon().currentDungeonItemFlagsSnapshot()[DungeonItemState.SMALL_KEYS_INDEX]);

        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.RIGHT);
        assertEquals(0x39, route.session.rooms().currentRoomId());
        assertEquals(0x63, RoomDebugFixture.of(route.session).activeRoomEventForTest());

        route.room39ButtonPath = reachablePositionPath(
                route.collision,
                route.link.position().pixelX(),
                route.link.position().pixelY(),
                (x, y) -> route.collision
                                .groundInteractionSample(x + 0x08, y + 0x10)
                                .objectId()
                        == 0xAA);
        assertNotNull(route.room39ButtonPath, collisionGrid(route.collision));
        for (int[] position : route.room39ButtonPath) {
            route.link.position().setPixelPosition(position[0], position[1]);
            tickInteractiveEntities(
                    route.session,
                    route.frame++,
                    route.link.position().romEntityX(),
                    route.link.position().romEntityY());
        }
        route.room39EventDeadline = route.frame + 0x40;
        while (RoomDebugFixture.of(route.session).activeRoomEventForTest() != 0
                && route.frame < route.room39EventDeadline) {
            tickInteractiveEntities(
                    route.session,
                    route.frame++,
                    route.link.position().romEntityX(),
                    route.link.position().romEntityY());
        }
        assertEquals(0, RoomDebugFixture.of(route.session).activeRoomEventForTest());
        route.room39ChestDeadline = route.frame + 0x20;
        while (objectAt(route.session, 0x28) != 0xA0 && route.frame < route.room39ChestDeadline) {
            tickInteractiveEntities(
                    route.session,
                    route.frame++,
                    route.link.position().romEntityX(),
                    route.link.position().romEntityY());
        }
        assertEquals(0xA0, objectAt(route.session, 0x28));
        route.room39ChestPath = reachablePositionPath(
                route.collision,
                route.link.position().pixelX(),
                route.link.position().pixelY(),
                (x, y) -> Math.floorDiv(x + 0x08, 0x10) == 0x08 && Math.floorDiv(y + 0x05, 0x10) == 0x02);
        assertNotNull(route.room39ChestPath, collisionGrid(route.collision));
        for (int[] position : route.room39ChestPath) {
            route.link.position().setPixelPosition(position[0], position[1]);
        }
        route.link.animation().setDirection(Direction.UP.javaIndex());
        route.room39Chest = route.session
                .doors()
                .tryOpenChest(
                        route.link.position().pixelX(),
                        route.link.position().pixelY(),
                        Direction.fromJavaOrUnknown(Direction.UP.javaIndex()),
                        true,
                        route.playerState.equipmentLevels().swordLevel());
        assertTrue(route.room39Chest.opened());
        assertEquals(ChestContentsTable.CHEST_SMALL_KEY, route.room39Chest.itemType());
        route.room39ChestSlot = route.room39Chest.entitySlot();
        route.room39RewardObserved = false;
        route.room39ChestEnded = false;
        for (int chestTick = 0; chestTick < 0x40; chestTick++) {
            route.session
                    .entities()
                    .tickEntities(
                            route.frame++,
                            route.link.position().romEntityX(),
                            route.link.position().romEntityY());
            for (ChestRewardEvent reward :
                    route.session.components.entityBridge().outbox.consumeChestRewardEvents()) {
                route.room39RewardObserved |= reward.itemType() == ChestContentsTable.CHEST_SMALL_KEY;
                route.playerState.chestRewards().applyChestReward(reward.itemType());
            }
            RoomEntity chestEntity =
                    route.session.rooms().activeRoomOrNull().entities().slots().get(route.room39ChestSlot);
            if (!chestEntity.loaded() || chestEntity.status() == EntityStatus.DISABLED) {
                route.room39ChestEnded = true;
                break;
            }
        }
        assertTrue(route.room39RewardObserved);
        assertTrue(route.room39ChestEnded);
        assertEquals(2, route.session.dungeon().currentDungeonItemFlagsSnapshot()[DungeonItemState.SMALL_KEYS_INDEX]);

        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.LEFT);
        assertEquals(0x38, route.session.rooms().currentRoomId());
        assertEquals(0xA1, objectAt(route.session, 0x43));
        assertEquals(0x10, RoomDebugFixture.of(route.session).indoorRoomStatusForTest(0x01, 0x38) & 0x10);
        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.RIGHT);
        assertEquals(0x39, route.session.rooms().currentRoomId());
        assertEquals(0x63, RoomDebugFixture.of(route.session).activeRoomEventForTest());
        assertEquals(0xA1, objectAt(route.session, 0x28));
        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.UP);
        assertEquals(0x34, route.session.rooms().currentRoomId());
        assertTrue(
                route.link.position().pixelX() > 0x30,
                "room $34 entry must land east of its ROM $A6 partition: x="
                        + String.format("%02X", route.link.position().pixelX()));
        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.RIGHT);
        assertEquals(0x35, route.session.rooms().currentRoomId());
        assertEquals(0, RoomDebugFixture.of(route.session).activeRoomEventForTest());
        assertEquals(
                2,
                route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                        .filter(entity -> entity.type() == 0x2C)
                        .count());
        assertEquals(2, route.session.dungeon().currentDungeonItemFlagsSnapshot()[DungeonItemState.SMALL_KEYS_INDEX]);

        route.link.position().setPixelPosition(0x75, 0x05);
        assertTrue(route.session
                .doors()
                .tryUnlockIndoorKeyDoor(
                        route.link.position().pixelX(),
                        route.link.position().pixelY(),
                        Direction.fromJavaOrUnknown(Direction.UP.javaIndex()),
                        0x01));
        assertEquals(1, route.session.dungeon().currentDungeonItemFlagsSnapshot()[DungeonItemState.SMALL_KEYS_INDEX]);
        assertEquals(0x40, RoomDebugFixture.of(route.session).indoorRoomStatusForTest(0x01, 0x35) & 0x40);
        for (int tick = 0; tick < 8; tick++) {
            route.session
                    .entities()
                    .tickEntities(
                            route.frame++,
                            route.link.position().pixelX(),
                            route.link.position().pixelY());
            assertTrue(route.session.components.entityBridge().consumeWorldLinkMotionBlockRequest());
        }
        route.session
                .entities()
                .tickEntities(
                        route.frame++,
                        route.link.position().pixelX(),
                        route.link.position().pixelY());
        assertFalse(route.session.components.entityBridge().consumeWorldLinkMotionBlockRequest());
        assertEquals(0x43, objectAt(route.session, 0x07));
        assertEquals(0x44, objectAt(route.session, 0x08));
        assertEquals(0x04, RoomDebugFixture.of(route.session).indoorRoomStatusForTest(0x01, 0x35) & 0x04);
        assertEquals(0x08, RoomDebugFixture.of(route.session).indoorRoomStatusForTest(0x01, 0x2F) & 0x08);

        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.UP);
        assertEquals(0x2F, route.session.rooms().currentRoomId());
        assertEquals(0xA7, RoomDebugFixture.of(route.session).activeRoomEventForTest());
        assertEquals(0xA7, objectAt(route.session, 0x33));
        assertEquals(0xA7, objectAt(route.session, 0x36));
        assertEquals(0x0D, objectAt(route.session, 0x18));
        assertEquals(
                2,
                route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                        .filter(entity -> entity.type() == 0x19)
                        .count());
        assertEquals(
                1,
                route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                        .filter(entity -> entity.type() == 0x17)
                        .count());
        assertEquals(
                2,
                route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                        .filter(entity -> entity.type() == 0x2D)
                        .count());
        assertEquals(
                1,
                route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                        .filter(entity -> entity.type() == 0x42)
                        .count());

        route.link.position().setPixelPosition(0x25, 0x28);
        for (int tick = 0; tick < 64; tick++) {
            assertTrue(route.session
                    .doors()
                    .tryInteractWithIndoorBlock(
                            route.link.position().pixelX(),
                            route.link.position().pixelY(),
                            Direction.fromJavaOrUnknown(Direction.RIGHT.javaIndex()),
                            0x08));
        }
        assertEquals(0xA7, RoomDebugFixture.of(route.session).activeRoomEventForTest());
        for (int tick = 0; tick < 33; tick++) {
            route.session
                    .entities()
                    .tickEntities(
                            route.frame++,
                            route.link.position().romEntityX(),
                            route.link.position().romEntityY());
        }
        assertEquals(0xA6, objectAt(route.session, 0x34));
        assertEquals(0xA7, RoomDebugFixture.of(route.session).activeRoomEventForTest());

        route.link.position().setPixelPosition(0x5C, 0x28);
        for (int tick = 0; tick < 64; tick++) {
            assertTrue(route.session
                    .doors()
                    .tryInteractWithIndoorBlock(
                            route.link.position().pixelX(),
                            route.link.position().pixelY(),
                            Direction.fromJavaOrUnknown(Direction.LEFT.javaIndex()),
                            0x04));
        }
        assertEquals(0xA7, RoomDebugFixture.of(route.session).activeRoomEventForTest());
        for (int tick = 0; tick < 33; tick++) {
            route.session
                    .entities()
                    .tickEntities(
                            route.frame++,
                            route.link.position().romEntityX(),
                            route.link.position().romEntityY());
        }
        assertEquals(0xA6, objectAt(route.session, 0x34));
        assertEquals(0xA6, objectAt(route.session, 0x35));
        assertEquals(0xA7, RoomDebugFixture.of(route.session).activeRoomEventForTest());
        route.session
                .entities()
                .tickEntities(
                        route.frame++,
                        route.link.position().romEntityX(),
                        route.link.position().romEntityY());
        assertEquals(0, RoomDebugFixture.of(route.session).activeRoomEventForTest());
        for (int tick = 0; tick < 11; tick++) {
            route.session
                    .entities()
                    .tickEntities(
                            route.frame++,
                            route.link.position().romEntityX(),
                            route.link.position().romEntityY());
        }
        assertEquals(0xBE, objectAt(route.session, 0x18));
        assertEquals(0x10, RoomDebugFixture.of(route.session).indoorRoomStatusForTest(0x01, 0x2F) & 0x10);
        assertEquals(1, route.session.dungeon().currentDungeonItemFlagsSnapshot()[DungeonItemState.SMALL_KEYS_INDEX]);

        assertTrue(route.session.rooms().activeRoomOrNull().hasWarps());
        route.staircaseExitPath = reachablePositionPath(
                route.collision,
                route.link.position().pixelX(),
                route.link.position().pixelY(),
                (x, y) -> x <= 0x60 && y == 0x10);
        assertNotNull(route.staircaseExitPath, collisionGrid(route.collision));
        for (int[] position : route.staircaseExitPath) {
            route.link.position().setPixelPosition(position[0], position[1]);
            route.coordinator.handleWarpAndIndoorBoundaries(route.link);
        }
        assertFalse(route.transition.isActive());
        route.staircaseEntryPath = reachablePositionPath(
                route.collision,
                route.link.position().pixelX(),
                route.link.position().pixelY(),
                (x, y) -> x == 0x80 && y == 0x10);
        assertNotNull(route.staircaseEntryPath, collisionGrid(route.collision));
        for (int[] position : route.staircaseEntryPath) {
            route.link.position().setPixelPosition(position[0], position[1]);
            route.coordinator.handleWarpAndIndoorBoundaries(route.link);
            if (route.transition.isActive()) {
                break;
            }
        }
        assertTrue(route.transition.isActive());
        while (route.transition.isActive()) {
            route.transition.tick();
        }
        assertEquals(Warp.CATEGORY_SIDESCROLL, route.session.rooms().mapCategory());
        assertEquals(0x3F, route.session.rooms().currentRoomId());
        assertEquals(0x88, route.link.position().romEntityX());
        assertEquals(0x10, route.link.position().romEntityY());
        assertEquals(
                0x01, route.link.motion().romPhysicsModifier(), "side-view warp initializes hLinkPhysicsModifier=$01");

        route.sideViewExitPath = reachablePositionPath(
                route.collision,
                route.link.position().pixelX(),
                route.link.position().pixelY(),
                (x, y) -> x == 0);
        assertNotNull(route.sideViewExitPath, collisionGrid(route.collision));
        for (int[] position : route.sideViewExitPath) {
            route.link.position().setPixelPosition(position[0], position[1]);
            route.coordinator.handleWarpAndIndoorBoundaries(route.link);
            assertFalse(route.transition.isActive());
            assertFalse(route.scroll.isActive());
        }
        route.link.position().setPixelPosition(-1, route.link.position().pixelY());
        route.coordinator.handleWarpAndIndoorBoundaries(route.link);
        assertTrue(route.scroll.isActive());
        assertEquals(ScrollController.LEFT, route.scroll.direction());
        while (route.scroll.isActive()) {
            route.scroll.tick(8);
        }
        assertEquals(Warp.CATEGORY_SIDESCROLL, route.session.rooms().mapCategory());
        assertEquals(0x3E, route.session.rooms().currentRoomId());
        assertEquals(1, route.session.rooms().activeRoomOrNull().warps().size());
        route.sideViewExit = route.session.rooms().activeRoomOrNull().firstWarp();
        assertEquals(Warp.CATEGORY_INDOOR, route.sideViewExit.category());
        assertEquals(0x01, route.sideViewExit.destMap());
        assertEquals(0x2C, route.sideViewExit.destRoom());
        assertEquals(0x78, route.sideViewExit.destX());
        assertEquals(0x70, route.sideViewExit.destY());
        route.sideViewReturnPath = reachableBoundaryPath(
                route.collision,
                route.link.position().pixelX(),
                route.link.position().pixelY(),
                ScrollController.RIGHT);
        assertNotNull(route.sideViewReturnPath, collisionGrid(route.collision));
        for (int[] position : route.sideViewReturnPath) {
            route.link.position().setPixelPosition(position[0], position[1]);
            route.coordinator.handleWarpAndIndoorBoundaries(route.link);
            assertFalse(route.transition.isActive());
            assertFalse(route.scroll.isActive());
        }
        route.link
                .position()
                .setPixelPosition(
                        RoomConstants.ROOM_PIXEL_WIDTH, route.link.position().pixelY());
        route.coordinator.handleWarpAndIndoorBoundaries(route.link);
        assertTrue(route.scroll.isActive());
        assertEquals(ScrollController.RIGHT, route.scroll.direction());
        while (route.scroll.isActive()) {
            route.scroll.tick(8);
        }
        assertEquals(0x3F, route.session.rooms().currentRoomId());
        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.LEFT);
        assertEquals(0x3E, route.session.rooms().currentRoomId());
        route.sideViewWarpPath = reachableBoundaryPath(
                route.collision,
                route.link.position().pixelX(),
                route.link.position().pixelY(),
                ScrollController.UP);
        assertNotNull(route.sideViewWarpPath, collisionGrid(route.collision));
        for (int[] position : route.sideViewWarpPath) {
            route.link.position().setPixelPosition(position[0], position[1]);
            route.coordinator.handleWarpAndIndoorBoundaries(route.link);
            assertFalse(route.transition.isActive());
        }
        route.link.position().setPixelPosition(route.link.position().pixelX(), -5);
        route.coordinator.handleWarpAndIndoorBoundaries(route.link);
        assertTrue(route.transition.isActive());
        while (route.transition.isActive()) {
            route.transition.tick();
        }
        assertEquals(Warp.CATEGORY_INDOOR, route.session.rooms().mapCategory());
        assertEquals(0x2C, route.session.rooms().currentRoomId());
        assertEquals(0x78, route.link.position().romEntityX());
        assertEquals(0x70, route.link.position().romEntityY());
        assertEquals(0, RoomDebugFixture.of(route.session).activeRoomEventForTest());
        route.room2CKeese = route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x19)
                .findFirst()
                .orElseThrow();
        assertEquals(0x28, route.room2CKeese.x());
        assertEquals(0x30, route.room2CKeese.y());
        route.room2CSpark = route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x16)
                .findFirst()
                .orElseThrow();
        assertEquals(0x18, route.room2CSpark.x());
        assertEquals(0x30, route.room2CSpark.y());
        route.room2CFloatingItem = route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0xE5)
                .findFirst()
                .orElseThrow();
        assertEquals(0x48, route.room2CFloatingItem.x());
        assertEquals(0x50, route.room2CFloatingItem.y());

        // Room-$28's source-authored south entry is the D2 ledge at column 7;
        // columns 4/5 are the solid $45/$46 objects. Reach that X in the
        // preceding room before crossing the shared north boundary.
        route.room28EntryColumnPath = reachablePositionPath(
                route.collision,
                route.link.position().pixelX(),
                route.link.position().pixelY(),
                (x, y) -> x == 0x70);
        assertNotNull(
                route.room28EntryColumnPath,
                "Room-$2C cannot reach the room-$28 entry column\n" + collisionGrid(route.collision));
        for (int[] position : route.room28EntryColumnPath) {
            route.link.position().setPixelPosition(position[0], position[1]);
            route.coordinator.handleWarpAndIndoorBoundaries(route.link);
            assertFalse(route.transition.isActive());
            assertFalse(route.scroll.isActive());
        }
        assertEquals(0x70, route.link.position().pixelX(), "entry-column path must end at room-$28 column 7");
        // Cross only after reaching that aligned X; the generic helper would
        // choose the first reachable top-edge column and lose this alignment.
        route.link.position().setPixelPosition(route.link.position().pixelX(), -1);
        route.coordinator.handleWarpAndIndoorBoundaries(route.link);
        assertTrue(route.scroll.isActive());
        assertEquals(ScrollController.UP, route.scroll.direction());
        assertEquals(0x70, route.link.position().pixelX());
        assertEquals(
                RoomConstants.ROOM_PIXEL_HEIGHT - Link.SPRITE_SIZE,
                route.link.position().pixelY());
        while (route.scroll.isActive()) {
            route.scroll.tick(8);
        }
        assertEquals(Warp.CATEGORY_INDOOR, route.session.rooms().mapCategory());
        assertEquals(0x28, route.session.rooms().currentRoomId());
        assertEquals(0xC1, RoomDebugFixture.of(route.session).activeRoomEventForTest());
        route.hinox = route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x89)
                .findFirst()
                .orElseThrow();
        assertEquals(0x58, route.hinox.x());
        assertEquals(0x30, route.hinox.y());
        route.unresolvedHinoxWarp = route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x61)
                .findFirst()
                .orElseThrow();
        assertEquals(0x50, route.unresolvedHinoxWarp.x());
        assertEquals(0x48, route.unresolvedHinoxWarp.y());
        assertFalse(route.session.rooms().activeRoomOrNull().hasWarps());
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
            route.link.motion().update();
            route.room28LedgeJumpObserved |= route.link.airborne().isAirborne();
            if (route.room28LedgeJumpObserved && !route.link.airborne().isAirborne()) {
                route.room28LedgeJumpLanded = true;
                break;
            }
            route.coordinator.handleWarpAndIndoorBoundaries(route.link);
            assertFalse(route.transition.isActive());
            assertFalse(route.scroll.isActive());
        }
    }

    static void fightHinox(GameplayRouteState route) throws Exception {

        setDirectionalInput(route.inputState, route.inputConfig, 0x04, GLFW_RELEASE);
        assertTrue(
                route.room28LedgeJumpObserved,
                "Room-$28 entry must trigger the source D2 ledge jump at ("
                        + route.link.position().pixelX() + ","
                        + route.link.position().pixelY() + ") object="
                        + String.format(
                                "%02X",
                                route.collision.objectIdAtPoint(
                                        route.link.position().pixelX() + 6,
                                        route.link.position().pixelY() + 6))
                        + " physics="
                        + String.format(
                                "%02X",
                                route.collision.objectPhysicsFlagAtPoint(
                                        route.link.position().pixelX() + 6,
                                        route.link.position().pixelY() + 6)));
        assertTrue(route.room28LedgeJumpLanded, "Room-$28 D2 ledge jump did not land");
        assertFalse(route.link.airborne().isAirborne());
        assertFalse(route.link.airborne().isFallingIntoPit());

        route.dormantWarpPath = reachablePositionPath(
                route.collision,
                route.link.position().pixelX(),
                route.link.position().pixelY(),
                (x, y) -> Math.abs(signedByteDelta(route.unresolvedHinoxWarp.x(), x + 8)) <= 0x02
                        && Math.abs(signedByteDelta(route.unresolvedHinoxWarp.y(), y + 16)) <= 0x02);
        assertNotNull(route.dormantWarpPath, "pre-clear entity-$61 warp is not collision reachable");
        for (int index = 1; index < route.dormantWarpPath.size(); index++) {
            int[] position = route.dormantWarpPath.get(index);
            route.link.position().setPixelPosition(position[0], position[1]);
            tickInteractiveEntities(
                    route.session,
                    route.frame++,
                    route.link.position().romEntityX(),
                    route.link.position().romEntityY());
            route.coordinator.handleWarpAndIndoorBoundaries(route.link);
            assertFalse(route.transition.isActive());
        }
        for (int dormantTick = 0; dormantTick < 4; dormantTick++) {
            tickInteractiveEntities(
                    route.session,
                    route.frame++,
                    route.link.position().romEntityX(),
                    route.link.position().romEntityY());
            route.coordinator.handleWarpAndIndoorBoundaries(route.link);
            assertEquals(
                    0,
                    RoomDebugFixture.of(route.session)
                            .entityTransitionCountdownForTest(route.unresolvedHinoxWarp.slot()));
            assertFalse(route.transition.isActive(), "entity-$61 must remain gated before the Hinox clear");
        }

        route.hinoxContact = reachableSwordContactPath(
                route.collision,
                route.link.position().pixelX(),
                route.link.position().pixelY(),
                route.hinox,
                route.romTables);
        assertNotNull(
                route.hinoxContact,
                "No collision-valid path from room-$28 ledge landing to Hinox\n" + collisionGrid(route.collision));
        for (int[] position : route.hinoxContact.path()) {
            route.link.position().setPixelPosition(position[0], position[1]);
            route.coordinator.handleWarpAndIndoorBoundaries(route.link);
            assertFalse(route.transition.isActive());
            assertFalse(route.scroll.isActive());
        }
        route.link.animation().setDirection(route.hinoxContact.direction());
        tickInteractiveEntities(
                route.session,
                route.frame++,
                route.link.position().romEntityX(),
                route.link.position().romEntityY());
        route.hinox =
                route.session.rooms().activeRoomOrNull().entities().slots().get(route.hinoxSlot);
        route.firstSwordPosition =
                new int[] {route.link.position().pixelX(), route.link.position().pixelY()};
        route.hinoxSword = new Sword(route.romTables, null);
        route.hinoxSword.onPress();
        for (int swingFrame = 0; swingFrame < 4; swingFrame++) {
            route.hinoxSword.tick(false);
        }
        route.hinoxSwordBox = route.hinoxSword.enemyCollisionBox(
                route.link.position().romEntityX(),
                route.link.position().romSwordCollisionY(),
                route.link.animation().direction());
        assertTrue(route.hinoxSwordBox.active(), "sword must be live at the Hinox contact");
        assertTrue(
                linksawakening.world.entity.EntityCombatTestAccess.overlapsSword(
                        route.hinox,
                        route.hinoxSwordBox.x(),
                        route.hinoxSwordBox.width(),
                        route.hinoxSwordBox.y(),
                        route.hinoxSwordBox.height()),
                "position=" + java.util.Arrays.toString(route.firstSwordPosition) + " sword=" + route.hinoxSwordBox
                        + " hinox=" + route.hinox);
        route.hinoxHit = route.session
                .entities()
                .resolveEntityCombat(new EntityCombatInputs(
                        route.frame++,
                        route.link.position().romEntityX(),
                        route.link.position().romEntityY(),
                        false,
                        0,
                        true,
                        new SwordBox(
                                true,
                                route.hinoxSwordBox.x(),
                                route.hinoxSwordBox.width(),
                                route.hinoxSwordBox.y(),
                                route.hinoxSwordBox.height()),
                        linksawakening.world.entity.EnemyAttackContext.standard()));
        assertTrue(
                route.hinoxHit.stream()
                        .anyMatch(event -> event.slot() == route.hinoxSlot
                                && event.type() == HinoxMotion.ENTITY_TYPE
                                && event.swordHit()
                                && event.enemyDamage() > 0),
                route.hinoxHit.toString());

        route.hinoxBombResponseObserved = false;
        route.bombDeadline = route.frame + 0x100;
        while (route.frame < route.bombDeadline && !route.hinoxBombResponseObserved) {
            tickInteractiveEntities(
                    route.session,
                    route.frame++,
                    route.link.position().romEntityX(),
                    route.link.position().romEntityY());
            route.hinoxBombResponseObserved =
                    route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                            .anyMatch(entity -> entity.type() == MotionTestAccess.HinoxMotion_ENTITY_BOMB);
        }
        assertTrue(route.hinoxBombResponseObserved, "a live sword flash must enter Hinox's bomb response");

        // Continue using the ordinary sword collision/death path. The test
        // never writes enemy health or replaces an entity slot.
        route.hinoxDeathStarted = false;
        route.deathDeadline = route.frame + 0x1000;
        while (route.frame < route.deathDeadline) {
            RoomEntity liveHinox =
                    route.session.rooms().activeRoomOrNull().entities().slots().get(route.hinoxSlot);
            if (liveHinox.status() == EntityStatus.DISABLED) {
                break;
            }
            if (liveHinox.status() == EntityStatus.ACTIVE) {
                SwordApproach approach = reachableSwordContactPath(
                        route.collision,
                        route.link.position().pixelX(),
                        route.link.position().pixelY(),
                        liveHinox,
                        route.romTables);
                if (approach != null) {
                    for (int[] position : approach.path()) {
                        route.link.position().setPixelPosition(position[0], position[1]);
                    }
                    route.link.animation().setDirection(approach.direction());
                    Sword swordStrike = new Sword(route.romTables, null);
                    swordStrike.onPress();
                    for (int swingFrame = 0; swingFrame < 4; swingFrame++) {
                        swordStrike.tick(false);
                    }
                    Sword.CollisionBox box = swordStrike.enemyCollisionBox(
                            route.link.position().romEntityX(),
                            route.link.position().romSwordCollisionY(),
                            route.link.animation().direction());
                    if (box.active()
                            && linksawakening.world.entity.EntityCombatTestAccess.overlapsSword(
                                    liveHinox, box.x(), box.width(), box.y(), box.height())) {
                        List<EntityCombatEvent> hit = route.session
                                .entities()
                                .resolveEntityCombat(new EntityCombatInputs(
                                        route.frame++,
                                        route.link.position().romEntityX(),
                                        route.link.position().romEntityY(),
                                        false,
                                        0,
                                        true,
                                        new SwordBox(true, box.x(), box.width(), box.y(), box.height()),
                                        linksawakening.world.entity.EnemyAttackContext.standard()));
                        route.hinoxDeathStarted |= hit.stream()
                                .anyMatch(event ->
                                        event.slot() == route.hinoxSlot && event.swordHit() && event.enemyDamage() > 0);
                    }
                }
            }
            tickInteractiveEntities(
                    route.session,
                    route.frame++,
                    route.link.position().romEntityX(),
                    route.link.position().romEntityY());
        }
        assertTrue(route.hinoxDeathStarted, "live sword combat must start Hinox death");
        assertEquals(
                EntityStatus.DISABLED,
                route.session
                        .rooms()
                        .activeRoomOrNull()
                        .entities()
                        .slots()
                        .get(route.hinoxSlot)
                        .status());
        assertEquals(0x20, RoomDebugFixture.of(route.session).indoorRoomStatusForTest(0x01, 0x28) & 0x20);
        assertEquals(route.hinoxSourceLoadOrder, route.hinox.sourceLoadOrder());

        route.clearEventDeadline = route.frame + 0x100;
        while (RoomDebugFixture.of(route.session).activeRoomEventForTest() != 0
                && route.frame < route.clearEventDeadline) {
            tickInteractiveEntities(
                    route.session,
                    route.frame++,
                    route.link.position().romEntityX(),
                    route.link.position().romEntityY());
        }
        assertEquals(0, RoomDebugFixture.of(route.session).activeRoomEventForTest());
        assertEquals(0x20, RoomDebugFixture.of(route.session).indoorRoomStatusForTest(0x01, 0x28) & 0x20);
        assertEquals(
                0x01,
                RoomDebugFixture.of(route.session).indoorRoomStatusForTest(0x01, 0x28) & 0x01,
                "clear-midboss must persist room $28's open right door");

        route.clearedWarp = route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x61)
                .findFirst()
                .orElseThrow();
        // The ROM handler deliberately remains in state 1 until Link leaves
        // its contact window.  The fight ends with Link already standing on
        // this warp, so first walk away through collision-valid positions and
        // give the handler a tick there before approaching it again.
        route.disengagePath = reachablePositionPath(
                route.collision,
                route.link.position().pixelX(),
                route.link.position().pixelY(),
                (x, y) -> Math.abs(signedByteDelta(route.clearedWarp.x(), x + 8)) > 0x08
                        || Math.abs(signedByteDelta(route.clearedWarp.y(), y + 16)) > 0x08);
        assertNotNull(route.disengagePath, "cleared entity-$61 warp cannot be disengaged");
        for (int index = 1; index < route.disengagePath.size(); index++) {
            int[] position = route.disengagePath.get(index);
            route.link.position().setPixelPosition(position[0], position[1]);
            tickInteractiveEntities(
                    route.session,
                    route.frame++,
                    route.link.position().romEntityX(),
                    route.link.position().romEntityY());
            route.coordinator.handleWarpAndIndoorBoundaries(route.link);
            assertFalse(route.transition.isActive());
            assertFalse(route.scroll.isActive());
        }
        route.awayX = route.link.position().romEntityX();
        route.awayY = route.link.position().romEntityY();
        assertTrue(Math.abs(signedByteDelta(route.clearedWarp.x(), route.awayX)) > 0x08
                || Math.abs(signedByteDelta(route.clearedWarp.y(), route.awayY)) > 0x08);

        route.warpPath = reachablePositionPath(
                route.collision,
                route.link.position().pixelX(),
                route.link.position().pixelY(),
                (x, y) -> Math.abs(signedByteDelta(route.clearedWarp.x(), x + 8)) <= 0x02
                        && Math.abs(signedByteDelta(route.clearedWarp.y(), y + 16)) <= 0x02);
        assertNotNull(route.warpPath, "cleared entity-$61 warp is not collision reachable");
        for (int index = 1; index < route.warpPath.size(); index++) {
            int[] position = route.warpPath.get(index);
            route.link.position().setPixelPosition(position[0], position[1]);
            tickInteractiveEntities(
                    route.session,
                    route.frame++,
                    route.link.position().romEntityX(),
                    route.link.position().romEntityY());
            route.coordinator.handleWarpAndIndoorBoundaries(route.link);
            assertFalse(route.transition.isActive());
            assertFalse(route.scroll.isActive());
        }
        assertTrue(Math.abs(signedByteDelta(
                                route.clearedWarp.x(), route.link.position().romEntityX()))
                        <= 0x02
                && Math.abs(signedByteDelta(
                                route.clearedWarp.y(), route.link.position().romEntityY()))
                        <= 0x02);

        // Contact can occur one or two pixels before the path's final target,
        // so some of the exact $50 countdown may already have elapsed. Wait
        // the remaining source countdown and require the fade only on zero.
        route.warpCountdown =
                RoomDebugFixture.of(route.session).entityTransitionCountdownForTest(route.clearedWarp.slot());
        assertTrue(
                route.warpCountdown > 0 && route.warpCountdown <= 0x50,
                "warp did not enter countdown at contact link="
                        + String.format(
                                "%02X/%02X entity=%02X/%02X",
                                route.link.position().romEntityX(),
                                route.link.position().romEntityY(),
                                route.clearedWarp.x(),
                                route.clearedWarp.y()));
        for (int remaining = route.warpCountdown; remaining > 0; remaining--) {
            tickInteractiveEntities(
                    route.session,
                    route.frame++,
                    route.link.position().romEntityX(),
                    route.link.position().romEntityY());
            route.coordinator.handleWarpAndIndoorBoundaries(route.link);
            if (remaining > 1) {
                assertFalse(route.transition.isActive());
            }
        }
        assertTrue(
                route.transition.isActive(),
                "cleared entity-$61 should start a fade: room="
                        + String.format("%02X", route.session.rooms().currentRoomId())
                        + " status="
                        + String.format(
                                "%02X", RoomDebugFixture.of(route.session).indoorRoomStatusForTest(0x01, 0x28))
                        + " link="
                        + String.format(
                                "%02X/%02X/%02X",
                                route.link.position().romEntityX(),
                                route.link.position().romEntityY(),
                                route.link.animation().romEntityZ())
                        + " progress="
                        + String.format("%02X", route.session.dungeon().dungeonProgressFlagsSnapshot()[1])
                        + " warpCountdown="
                        + RoomDebugFixture.of(route.session)
                                .entityTransitionCountdownForTest(route.clearedWarp.slot()));
        while (route.transition.isActive()) {
            route.transition.tick();
        }
        assertEquals(Warp.CATEGORY_INDOOR, route.session.rooms().mapCategory());
        assertEquals(0x36, route.session.rooms().currentRoomId());
        assertEquals(0x50, route.link.position().romEntityX());
        assertEquals(0x48, route.link.position().romEntityY());

        // Room $36's entity-$61 is the opposite endpoint of the cleared
        // Hinox miniboss warp.  Leave its initial contact window, let the
        // source handler arm, then approach it again through live collision.
        route.room36Warp = route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x61)
                .findFirst()
                .orElseThrow();
        assertEquals(0x50, route.room36Warp.x());
        assertEquals(0x48, route.room36Warp.y());
        route.room36WarpSlot = route.room36Warp.slot();
        tickInteractiveEntities(
                route.session,
                route.frame++,
                route.link.position().romEntityX(),
                route.link.position().romEntityY());
        route.room36WarpDisengagePath = reachablePositionPath(
                route.collision,
                route.link.position().pixelX(),
                route.link.position().pixelY(),
                (x, y) -> Math.abs(signedByteDelta(route.room36Warp.x(), x + 8)) > 0x08
                        || Math.abs(signedByteDelta(route.room36Warp.y(), y + 16)) > 0x08);
        assertNotNull(route.room36WarpDisengagePath, collisionGrid(route.collision));
        for (int index = 1; index < route.room36WarpDisengagePath.size(); index++) {
            int[] position = route.room36WarpDisengagePath.get(index);
            route.link.position().setPixelPosition(position[0], position[1]);
            tickInteractiveEntities(
                    route.session,
                    route.frame++,
                    route.link.position().romEntityX(),
                    route.link.position().romEntityY());
            route.coordinator.handleWarpAndIndoorBoundaries(route.link);
            assertFalse(route.transition.isActive());
            assertFalse(route.scroll.isActive());
        }
        assertEquals(0, RoomDebugFixture.of(route.session).entityTransitionCountdownForTest(route.room36WarpSlot));

        route.armedRoom36Warp =
                route.session.rooms().activeRoomOrNull().entities().slots().get(route.room36WarpSlot);
        route.room36WarpContactPath = reachablePositionPath(
                route.collision,
                route.link.position().pixelX(),
                route.link.position().pixelY(),
                (x, y) -> {
                    int linkX = x + 8;
                    int linkY = y + 16;
                    int deltaX = signedByteDelta(linkX, route.armedRoom36Warp.x());
                    int deltaY = signedByteDelta(linkY, route.armedRoom36Warp.y());
                    return deltaX >= -3 && deltaX <= 2 && deltaY >= -3 && deltaY <= 2;
                });
        assertNotNull(route.room36WarpContactPath, collisionGrid(route.collision));
        for (int index = 1; index < route.room36WarpContactPath.size(); index++) {
            int[] position = route.room36WarpContactPath.get(index);
            route.link.position().setPixelPosition(position[0], position[1]);
            tickInteractiveEntities(
                    route.session,
                    route.frame++,
                    route.link.position().romEntityX(),
                    route.link.position().romEntityY());
            route.coordinator.handleWarpAndIndoorBoundaries(route.link);
            assertFalse(route.transition.isActive());
            assertFalse(route.scroll.isActive());
        }
        route.room36WarpCountdown =
                RoomDebugFixture.of(route.session).entityTransitionCountdownForTest(route.room36WarpSlot);
        assertTrue(
                route.room36WarpCountdown > 0 && route.room36WarpCountdown <= 0x50,
                "warp contact did not arm: link="
                        + String.format(
                                "%02X/%02X",
                                route.link.position().romEntityX(),
                                route.link.position().romEntityY())
                        + " entity=" + String.format("%02X/%02X", route.armedRoom36Warp.x(), route.armedRoom36Warp.y())
                        + " pathEnd=" + java.util.Arrays.toString(route.room36WarpContactPath.getLast()));
        for (int remaining = route.room36WarpCountdown; remaining > 0; remaining--) {
            tickInteractiveEntities(
                    route.session,
                    route.frame++,
                    route.link.position().romEntityX(),
                    route.link.position().romEntityY());
            route.coordinator.handleWarpAndIndoorBoundaries(route.link);
            if (remaining > 1) {
                assertFalse(route.transition.isActive());
            }
        }
        assertTrue(route.transition.isActive());
        while (route.transition.isActive()) {
            route.transition.tick();
        }
        assertEquals(Warp.CATEGORY_INDOOR, route.session.rooms().mapCategory());
        assertEquals(0x28, route.session.rooms().currentRoomId());
        assertEquals(0x50, route.link.position().romEntityX());
        assertEquals(0x48, route.link.position().romEntityY());

        route.routeToRoom21 = new int[] {ScrollController.RIGHT, ScrollController.UP, ScrollController.UP};
        route.routeToRoom21Rooms = new int[] {0x29, 0x26, 0x21};
        for (int index = 0; index < route.routeToRoom21.length; index++) {
            walkToAndCrossIndoorBoundary(
                    route.coordinator,
                    route.transition,
                    route.scroll,
                    route.collision,
                    route.link,
                    route.routeToRoom21[index]);
            assertEquals(route.routeToRoom21Rooms[index], route.session.rooms().currentRoomId());
        }

        assertEquals(0x21, route.session.rooms().currentRoomId());
        assertEquals(0x00, RoomDebugFixture.of(route.session).activeRoomEventForTest());
        assertEquals(0xA0, objectAt(route.session, 0x27));
        assertEquals(
                ChestContentsTable.CHEST_RUPEES_20,
                new ChestContentsTable((route.rom == null ? (Rom) null : TestRoms.partial(route.rom)))
                        .itemForSpawn(
                                0x01, 0x21, route.playerState.equipmentLevels().swordLevel(), true));
        assertEquals(1, route.session.dungeon().currentDungeonItemFlagsSnapshot()[DungeonItemState.SMALL_KEYS_INDEX]);

        route.braceletDoorPath = reachablePositionPath(
                route.collision,
                route.link.position().pixelX(),
                route.link.position().pixelY(),
                (x, y) -> y == 0x38
                        && (route.collision.objectPhysicsFlagAtPoint(x + 0x03, y + 0x09) == 0x92
                                || route.collision.objectPhysicsFlagAtPoint(x + 0x03, y + 0x0C) == 0x92));
        assertNotNull(route.braceletDoorPath, collisionGrid(route.collision));
        for (int[] position : route.braceletDoorPath) {
            route.link.position().setPixelPosition(position[0], position[1]);
        }
        route.link
                .position()
                .setPixelPosition(
                        route.link.position().pixelX() - 1,
                        route.link.position().pixelY());
        assertTrue(route.session
                .doors()
                .tryUnlockIndoorKeyDoor(
                        route.link.position().pixelX(),
                        route.link.position().pixelY(),
                        Direction.fromJavaOrUnknown(Direction.LEFT.javaIndex()),
                        0x04));
        assertEquals(0, route.session.dungeon().currentDungeonItemFlagsSnapshot()[DungeonItemState.SMALL_KEYS_INDEX]);
        for (int tick = 0; tick < 8; tick++) {
            route.session
                    .entities()
                    .tickEntities(
                            route.frame++,
                            route.link.position().pixelX(),
                            route.link.position().pixelY());
            assertTrue(route.session.components.entityBridge().consumeWorldLinkMotionBlockRequest());
        }
        route.session
                .entities()
                .tickEntities(
                        route.frame++,
                        route.link.position().pixelX(),
                        route.link.position().pixelY());
        assertFalse(route.session.components.entityBridge().consumeWorldLinkMotionBlockRequest());
        assertEquals(0x02, RoomDebugFixture.of(route.session).indoorRoomStatusForTest(0x01, 0x21) & 0x02);
        assertEquals(0x01, RoomDebugFixture.of(route.session).indoorRoomStatusForTest(0x01, 0x20) & 0x01);
        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.LEFT);
        assertEquals(0x20, route.session.rooms().currentRoomId());
        assertEquals(0x61, RoomDebugFixture.of(route.session).activeRoomEventForTest());
        assertEquals(
                2,
                route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                        .filter(entity -> entity.type() == 0x50)
                        .count());
        assertTrue(route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                .anyMatch(entity -> entity.type() == 0x50 && entity.x() == 0x48 && entity.y() == 0x30));
        assertTrue(route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                .anyMatch(entity -> entity.type() == 0x50 && entity.x() == 0x58 && entity.y() == 0x50));
        assertEquals(0x07, objectAt(route.session, 0x28));
        assertEquals(
                ChestContentsTable.CHEST_POWER_BRACELET,
                new ChestContentsTable((route.rom == null ? (Rom) null : TestRoms.partial(route.rom)))
                        .itemForSpawn(
                                0x01, 0x20, route.playerState.equipmentLevels().swordLevel(), true));

        assertEquals(0xAB, objectAt(route.session, 0x22));
        assertEquals(0xAB, objectAt(route.session, 0x65));
        for (int torchLocation : new int[] {0x22, 0x65}) {
            PowderApproach torchApproach = reachablePowderApproach(
                    route.collision,
                    route.link.position().pixelX(),
                    route.link.position().pixelY(),
                    torchLocation);
            assertNotNull(
                    torchApproach,
                    "No collision-valid powder approach to room $20 torch $" + Integer.toHexString(torchLocation) + "\n"
                            + collisionGrid(route.collision));
            for (int[] position : torchApproach.path()) {
                route.link.position().setPixelPosition(position[0], position[1]);
            }
            assertTrue(route.session
                    .playerItems()
                    .sprinkleMagicPowder(
                            route.link.position().romEntityX(),
                            route.link.position().romEntityY(),
                            0,
                            torchApproach.romDirection()));
            assertTrue(route.playerState.ammunition().consumeMagicPowder());
            for (int tick = 0; tick < 16; tick++) {
                tickInteractiveEntities(
                        route.session,
                        route.frame++,
                        route.link.position().romEntityX(),
                        route.link.position().romEntityY());
            }
            assertEquals(0xAC, objectAt(route.session, torchLocation));
        }
        assertEquals(2, RoomDebugFixture.of(route.session).roomTriggerCountForTest());

        route.booSlots = route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == BooBuddyMotion.ENTITY_TYPE)
                .map(RoomEntity::slot)
                .toList();
        assertEquals(2, route.booSlots.size());
    }

    static void clearBooBuddies(GameplayRouteState route) throws Exception {

        for (int booSlot : route.booSlots) {
            boolean booHit = false;
            int booDeadline = route.frame + 0x100;
            while (route.frame < booDeadline
                    && route.session
                                    .rooms()
                                    .activeRoomOrNull()
                                    .entities()
                                    .slots()
                                    .get(booSlot)
                                    .status()
                            != EntityStatus.DISABLED) {
                RoomEntity boo = route.session
                        .rooms()
                        .activeRoomOrNull()
                        .entities()
                        .slots()
                        .get(booSlot);
                SwordApproach approach = reachableSwordContactPath(
                        route.collision,
                        route.link.position().pixelX(),
                        route.link.position().pixelY(),
                        boo,
                        route.romTables);
                if (approach != null) {
                    for (int[] position : approach.path()) {
                        route.link.position().setPixelPosition(position[0], position[1]);
                    }
                    route.link.animation().setDirection(approach.direction());
                    Sword booSword = new Sword(route.romTables, null);
                    booSword.onPress();
                    for (int swingFrame = 0; swingFrame < 4; swingFrame++) {
                        booSword.tick(false);
                    }
                    Sword.CollisionBox box = booSword.enemyCollisionBox(
                            route.link.position().romEntityX(),
                            route.link.position().romSwordCollisionY(),
                            route.link.animation().direction());
                    if (box.active()
                            && linksawakening.world.entity.EntityCombatTestAccess.overlapsSword(
                                    boo, box.x(), box.width(), box.y(), box.height())) {
                        List<EntityCombatEvent> hit = route.session
                                .entities()
                                .resolveEntityCombat(new EntityCombatInputs(
                                        route.frame++,
                                        route.link.position().romEntityX(),
                                        route.link.position().romEntityY(),
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
                tickInteractiveEntities(
                        route.session,
                        route.frame++,
                        route.link.position().romEntityX(),
                        route.link.position().romEntityY());
            }
            assertTrue(booHit, "room $20 Boo Buddy must be defeated by live sword combat");
            assertEquals(
                    EntityStatus.DISABLED,
                    route.session
                            .rooms()
                            .activeRoomOrNull()
                            .entities()
                            .slots()
                            .get(booSlot)
                            .status());
        }

        route.braceletRevealDeadline = route.frame + 0x40;
        while (objectAt(route.session, 0x28) != 0xA0 && route.frame < route.braceletRevealDeadline) {
            tickInteractiveEntities(
                    route.session,
                    route.frame++,
                    route.link.position().romEntityX(),
                    route.link.position().romEntityY());
        }
        assertEquals(0, RoomDebugFixture.of(route.session).activeRoomEventForTest());
        assertEquals(0xA0, objectAt(route.session, 0x28));

        route.braceletChestPath = reachablePositionPath(
                route.collision,
                route.link.position().pixelX(),
                route.link.position().pixelY(),
                (x, y) -> Math.floorDiv(x + 0x08, 0x10) == 0x08 && Math.floorDiv(y + 0x05, 0x10) == 0x02);
        assertNotNull(route.braceletChestPath, collisionGrid(route.collision));
        for (int[] position : route.braceletChestPath) {
            route.link.position().setPixelPosition(position[0], position[1]);
        }
        route.link.animation().setDirection(Direction.UP.javaIndex());
        route.braceletChest = route.session
                .doors()
                .tryOpenChest(
                        route.link.position().pixelX(),
                        route.link.position().pixelY(),
                        Direction.fromJavaOrUnknown(Direction.UP.javaIndex()),
                        true,
                        route.playerState.equipmentLevels().swordLevel());
        assertTrue(route.braceletChest.opened());
        assertEquals(ChestContentsTable.CHEST_POWER_BRACELET, route.braceletChest.itemType());
        route.braceletChestSlot = route.braceletChest.entitySlot();
        route.braceletRewardObserved = false;
        route.braceletChestEnded = false;
        for (int chestTick = 0; chestTick < 0x40; chestTick++) {
            route.session
                    .entities()
                    .tickEntities(
                            route.frame++,
                            route.link.position().romEntityX(),
                            route.link.position().romEntityY());
            for (ChestRewardEvent reward :
                    route.session.components.entityBridge().outbox.consumeChestRewardEvents()) {
                route.braceletRewardObserved |= reward.itemType() == ChestContentsTable.CHEST_POWER_BRACELET;
                route.playerState.chestRewards().applyChestReward(reward.itemType());
            }
            RoomEntity chestEntity =
                    route.session.rooms().activeRoomOrNull().entities().slots().get(route.braceletChestSlot);
            if (!chestEntity.loaded() || chestEntity.status() == EntityStatus.DISABLED) {
                route.braceletChestEnded = true;
                break;
            }
        }
        assertTrue(route.braceletRewardObserved);
        assertTrue(route.braceletChestEnded);
        assertEquals(1, route.playerState.equipmentLevels().powerBraceletLevel());
        assertEquals(0x10, RoomDebugFixture.of(route.session).indoorRoomStatusForTest(0x01, 0x20) & 0x10);

        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.RIGHT);
        assertEquals(0x21, route.session.rooms().currentRoomId());
        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.LEFT);
        assertEquals(0x20, route.session.rooms().currentRoomId());
        for (int reloadTick = 0; reloadTick < 0x10; reloadTick++) {
            tickInteractiveEntities(
                    route.session,
                    route.frame++,
                    route.link.position().romEntityX(),
                    route.link.position().romEntityY());
        }
        assertEquals(0xA1, objectAt(route.session, 0x28));
        assertEquals(
                0x61,
                RoomDebugFixture.of(route.session).activeRoomEventForTest(),
                "the source event byte reloads but is inert once status bit $10 is set");
        assertFalse(route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
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
        assertEquals(0x00, RoomDebugFixture.of(route.session).entitySwitchBlocksStateForTest());

        route.room21PotRoute = new int[][] {
            {0x60, 0x29, Direction.UP.javaIndex(), 0x01, 0x08, 0x26},
            {0x60, 0x19, Direction.UP.javaIndex(), 0x01, 0x08, 0x16},
            {0x64, 0x10, Direction.RIGHT.javaIndex(), 0x08, 0x02, 0x17},
            {0x74, 0x10, Direction.RIGHT.javaIndex(), 0x08, 0x02, 0x18},
            {0x80, 0x11, Direction.DOWN.javaIndex(), 0x02, 0x04, 0x28}
        };
        for (int potIndex = 0; potIndex < route.room21PotRoute.length; potIndex++) {
            int[] lift = route.room21PotRoute[potIndex];
            List<int[]> room21PotPath = reachablePositionPath(
                    route.collision,
                    route.link.position().pixelX(),
                    route.link.position().pixelY(),
                    (x, y) -> x == lift[0] && y == lift[1]);
            assertNotNull(
                    room21PotPath,
                    "source pot $" + Integer.toHexString(lift[5]) + " must be reachable in room $21\n"
                            + collisionGrid(route.collision));
            for (int[] position : room21PotPath) {
                route.link.position().setPixelPosition(position[0], position[1]);
            }
            route.link.animation().setDirection(lift[2]);
            for (int pullFrame = 0; pullFrame < 8; pullFrame++) {
                assertTrue(
                        route.session
                                .doors()
                                .tryLiftIndoorObject(
                                        route.link.position().pixelX(),
                                        route.link.position().pixelY(),
                                        Direction.fromJavaOrUnknown(lift[2]),
                                        route.playerState.equipmentLevels().powerBraceletLevel() != 0,
                                        lift[4]),
                        "pot $" + Integer.toHexString(lift[5]) + " pull " + pullFrame);
            }
            assertEquals(0x0D, objectAt(route.session, lift[5]));
            int liftDeadline = route.frame + 0x40;
            while (route.session.rooms().liftedEntityState().carryState() != 0x01 && route.frame < liftDeadline) {
                tickInteractiveEntities(
                        route.session,
                        route.frame++,
                        route.link.position().romEntityX(),
                        route.link.position().romEntityY());
            }
            assertEquals(0x01, route.session.rooms().liftedEntityState().carryState());
            assertTrue(route.session
                    .playerItems()
                    .throwLiftedEntity(Direction.fromJava(route.link.animation().direction())));
        }

        route.room21EastDoorPath = reachablePositionPath(
                route.collision,
                route.link.position().pixelX(),
                route.link.position().pixelY(),
                (x, y) -> x == RoomConstants.ROOM_PIXEL_WIDTH - Link.SPRITE_SIZE && y >= 0x19 && y <= 0x28);
        assertNotNull(route.room21EastDoorPath, collisionGrid(route.collision));
        for (int[] position : route.room21EastDoorPath) {
            route.link.position().setPixelPosition(position[0], position[1]);
        }
        route.link
                .position()
                .setPixelPosition(
                        RoomConstants.ROOM_PIXEL_WIDTH, route.link.position().pixelY());
        route.coordinator.handleWarpAndIndoorBoundaries(route.link);
        assertTrue(route.scroll.isActive());
        assertEquals(ScrollController.RIGHT, route.scroll.direction());
        while (route.scroll.isActive()) {
            route.scroll.tick(8);
        }
        assertEquals(0x22, route.session.rooms().currentRoomId());
        assertEquals(0x00, RoomDebugFixture.of(route.session).entitySwitchBlocksStateForTest());
        assertEquals(0x00, route.session.rooms().liftedEntityState().carryState());

        assertEquals(0x00, RoomDebugFixture.of(route.session).activeRoomEventForTest());
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
        route.room22CrystalSwitch = route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x66)
                .findFirst()
                .orElseThrow();
        assertEquals(0x48, route.room22CrystalSwitch.x());
        assertEquals(0x30, route.room22CrystalSwitch.y());
        route.room22Hearts = route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x2D)
                .toList();
        assertEquals(4, route.room22Hearts.size());
        assertTrue(route.room22Hearts.stream().allMatch(entity -> entity.y() == 0x60));
        assertEquals(
                java.util.Set.of(0x38, 0x48, 0x58, 0x68),
                route.room22Hearts.stream().map(RoomEntity::x).collect(java.util.stream.Collectors.toSet()));
        assertEquals(
                ChestContentsTable.CHEST_SMALL_KEY,
                new ChestContentsTable((route.rom == null ? (Rom) null : TestRoms.partial(route.rom)))
                        .itemForSpawn(
                                0x01, 0x22, route.playerState.equipmentLevels().swordLevel(), true));
        tickInteractiveEntities(
                route.session,
                route.frame++,
                route.link.position().romEntityX(),
                route.link.position().romEntityY());

        route.room22SwitchApproach = reachableSwordContactPathWithFeather(
                route.collision,
                route.romTables,
                route.link.position().pixelX(),
                route.link.position().pixelY(),
                route.room22CrystalSwitch);
        assertNotNull(route.room22SwitchApproach, collisionGrid(route.collision));
        for (int[] position : route.room22SwitchApproach.path()) {
            route.link.position().setPixelPosition(position[0], position[1]);
        }
        route.link.animation().setDirection(route.room22SwitchApproach.direction());
        route.room22Sword = new Sword(route.romTables, null);
        route.room22Sword.onPress();
        for (int swingFrame = 0; swingFrame < 4; swingFrame++) {
            route.room22Sword.tick(false);
        }
        route.initialRoom22SwordBox = route.room22Sword.enemyCollisionBox(
                route.link.position().romEntityX(),
                route.link.position().romSwordCollisionY(),
                route.link.animation().direction());
        route.link
                .position()
                .setPixelPosition(
                        route.room22CrystalSwitch.x()
                                - (route.initialRoom22SwordBox.x()
                                        - route.link.position().pixelX()),
                        route.room22CrystalSwitch.y()
                                - (route.initialRoom22SwordBox.y()
                                        - route.link.position().pixelY()));
        route.room22SwordBox = route.room22Sword.enemyCollisionBox(
                route.link.position().romEntityX(),
                route.link.position().romSwordCollisionY(),
                route.link.animation().direction());
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
                            route.link.position().romEntityX(),
                            route.link.position().romEntityY(),
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
        tickInteractiveEntities(
                route.session,
                route.frame++,
                route.link.position().romEntityX(),
                route.link.position().romEntityY());
        while (RoomDebugFixture.of(route.session).switchableObjectAnimationStageForTest() != 0) {
            route.session.entityState().tickGameplayVBlank();
        }
        assertEquals(0x02, RoomDebugFixture.of(route.session).entitySwitchBlocksStateForTest());
        route.collision.refreshLinkGroundInteraction(
                route.link.position().pixelX(), route.link.position().pixelY());

        route.room22ChestPath = reachablePositionPathWithFeather(
                route.collision,
                route.romTables,
                route.link.position().pixelX(),
                route.link.position().pixelY(),
                (x, y) -> Math.floorDiv(x + 0x08, 0x10) == 0x07 && Math.floorDiv(y + 0x05, 0x10) == 0x02);
        assertNotNull(route.room22ChestPath, collisionGrid(route.collision));
        for (int[] position : route.room22ChestPath) {
            route.link.position().setPixelPosition(position[0], position[1]);
        }
        route.link.animation().setDirection(Direction.UP.javaIndex());
        route.room22Chest = route.session
                .doors()
                .tryOpenChest(
                        route.link.position().pixelX(),
                        route.link.position().pixelY(),
                        Direction.fromJavaOrUnknown(Direction.UP.javaIndex()),
                        true,
                        route.playerState.equipmentLevels().swordLevel());
        assertTrue(route.room22Chest.opened());
        assertEquals(ChestContentsTable.CHEST_SMALL_KEY, route.room22Chest.itemType());
        assertEquals(0x27, route.room22Chest.location());
        route.room22ChestSlot = route.room22Chest.entitySlot();
        route.room22DialogId = new ChestContentsTable((route.rom == null ? (Rom) null : TestRoms.partial(route.rom)))
                .dialogLowIdFor(
                        ChestContentsTable.CHEST_SMALL_KEY,
                        route.playerState.equipmentLevels().shieldLevel(),
                        route.playerState.equipmentLevels().swordLevel(),
                        route.playerState.equipmentLevels().powerBraceletLevel(),
                        0x01,
                        0x22);
        route.room22RewardObserved = false;
        route.room22DialogObserved = false;
        route.room22ChestEnded = false;
        for (int chestTick = 0; chestTick < 0x40; chestTick++) {
            tickInteractiveEntities(
                    route.session,
                    route.frame++,
                    route.link.position().romEntityX(),
                    route.link.position().romEntityY());
            for (ChestRewardEvent reward :
                    route.session.components.entityBridge().outbox.consumeChestRewardEvents()) {
                route.room22RewardObserved |= reward.itemType() == ChestContentsTable.CHEST_SMALL_KEY;
                route.playerState.chestRewards().applyChestReward(reward.itemType());
            }
            route.room22DialogObserved |= route.session.entityState().consumeEntityDialogRequests().stream()
                    .anyMatch(request -> request.globalDialogId() == route.room22DialogId);
            RoomEntity chestEntity =
                    route.session.rooms().activeRoomOrNull().entities().slots().get(route.room22ChestSlot);
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
        assertEquals(0x10, RoomDebugFixture.of(route.session).indoorRoomStatusForTest(0x01, 0x22) & 0x10);
        route.collision.refreshLinkGroundInteraction(
                route.link.position().pixelX(), route.link.position().pixelY());

        assertEquals(0x02, RoomDebugFixture.of(route.session).entitySwitchBlocksStateForTest());

        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.RIGHT);
        assertEquals(0x23, route.session.rooms().currentRoomId());
        assertEquals(0x00, RoomDebugFixture.of(route.session).activeRoomEventForTest());
        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.RIGHT);
        assertEquals(0x24, route.session.rooms().currentRoomId());
        assertEquals(0x00, RoomDebugFixture.of(route.session).activeRoomEventForTest());
        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.DOWN);
        assertEquals(0x27, route.session.rooms().currentRoomId());
        assertEquals(0x66, RoomDebugFixture.of(route.session).activeRoomEventForTest());

        route.room27PolsVoice = route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                .filter(entity -> entity.sourceLoadOrder() == 0 && entity.type() == 0x18)
                .findFirst()
                .orElseThrow();
        route.room27Keese = route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                .filter(entity -> entity.sourceLoadOrder() == 1 && entity.type() == 0x19)
                .findFirst()
                .orElseThrow();
        route.room27Moblin = route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                .filter(entity -> entity.sourceLoadOrder() == 2 && entity.type() == 0x0B)
                .findFirst()
                .orElseThrow();
        assertEquals(0x64, ((route.room27PolsVoice.y() - 0x10) & 0xF0) | ((route.room27PolsVoice.x() - 0x08) >>> 4));
        assertEquals(0x67, ((route.room27Keese.y() - 0x10) & 0xF0) | ((route.room27Keese.x() - 0x08) >>> 4));
        assertEquals(0x33, ((route.room27Moblin.y() - 0x10) & 0xF0) | ((route.room27Moblin.x() - 0x08) >>> 4));
        assertEquals(0x20, objectAt(route.session, 0x22));

        route.link.position().setPixelPosition(0x20, 0x1B);
        route.link.animation().setDirection(Direction.UP.javaIndex());
        for (int pullFrame = 0; pullFrame < 8; pullFrame++) {
            assertTrue(route.session
                    .doors()
                    .tryLiftIndoorObject(
                            route.link.position().pixelX(),
                            route.link.position().pixelY(),
                            Direction.fromJavaOrUnknown(Direction.UP.javaIndex()),
                            route.playerState.equipmentLevels().powerBraceletLevel() != 0,
                            0x08));
        }
        assertEquals(0x0D, objectAt(route.session, 0x22));
        route.room27LiftDeadline = route.frame + 0x40;
        while (route.session.rooms().liftedEntityState().carryState() != 0x01
                && route.frame < route.room27LiftDeadline) {
            tickInteractiveEntities(
                    route.session,
                    route.frame++,
                    route.link.position().romEntityX(),
                    route.link.position().romEntityY());
        }
        assertEquals(0x01, route.session.rooms().liftedEntityState().carryState());

        route.room27PolsVoice =
                route.session.rooms().activeRoomOrNull().entities().slots().get(route.room27PolsVoice.slot());
        route.link
                .position()
                .setPixelPosition(
                        route.room27PolsVoice.x() - 0x0B, route.room27PolsVoice.y() - route.room27PolsVoice.z() - 0x02);
        route.link.animation().setDirection(Direction.RIGHT.javaIndex());
        route.session
                .entities()
                .tickEntitiesWithProjectileEvents(
                        route.frame++,
                        route.link.position().romEntityX(),
                        route.link.position().romEntityY(),
                        0,
                        0,
                        Direction.RIGHT.javaIndex(),
                        false);
        route.alignedPot = route.session
                .rooms()
                .activeRoomOrNull()
                .entities()
                .slots()
                .get(route.session.rooms().liftedEntityState().slot());
        route.room27PolsVoice =
                route.session.rooms().activeRoomOrNull().entities().slots().get(route.room27PolsVoice.slot());
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
        assertTrue(route.session.playerItems().throwLiftedEntity(Direction.fromJava(Direction.RIGHT.javaIndex())));
        while (((route.frame ^ route.room27PolsVoice.slot()) & 1) != 0) {
            route.frame++;
        }
        route.session
                .entities()
                .tickEntitiesWithProjectileEvents(
                        route.frame++,
                        route.link.position().romEntityX(),
                        route.link.position().romEntityY(),
                        0,
                        0,
                        Direction.RIGHT.javaIndex(),
                        false);
        assertEquals(
                EntityStatus.DYING,
                route.session
                        .rooms()
                        .activeRoomOrNull()
                        .entities()
                        .slots()
                        .get(route.room27PolsVoice.slot())
                        .status());
        route.polsDeathDeadline = route.frame + 0x80;
    }

    static void completeKillOrderChest(GameplayRouteState route) throws Exception {

        while (route.session
                        .rooms()
                        .activeRoomOrNull()
                        .entities()
                        .slots()
                        .get(route.room27PolsVoice.slot())
                        .loaded()
                && route.frame < route.polsDeathDeadline) {
            tickInteractiveEntities(
                    route.session,
                    route.frame++,
                    route.link.position().romEntityX(),
                    route.link.position().romEntityY());
        }
        assertFalse(route.session
                .rooms()
                .activeRoomOrNull()
                .entities()
                .slots()
                .get(route.room27PolsVoice.slot())
                .loaded());
        assertEquals(0x66, RoomDebugFixture.of(route.session).activeRoomEventForTest());

        for (RoomEntity orderedEnemy : new RoomEntity[] {route.room27Keese, route.room27Moblin}) {
            int enemySlot = orderedEnemy.slot();
            int attackDeadline = route.frame + 0x200;
            while (route.session
                                    .rooms()
                                    .activeRoomOrNull()
                                    .entities()
                                    .slots()
                                    .get(enemySlot)
                                    .status()
                            == EntityStatus.ACTIVE
                    && route.frame < attackDeadline) {
                RoomEntity target = route.session
                        .rooms()
                        .activeRoomOrNull()
                        .entities()
                        .slots()
                        .get(enemySlot);
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
                                                .activeRoomOrNull()
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
                            .activeRoomOrNull()
                            .entities()
                            .slots()
                            .get(enemySlot)
                            .status());
            int orderedEnemyDeathDeadline = route.frame + 0x80;
            while (route.session
                            .rooms()
                            .activeRoomOrNull()
                            .entities()
                            .slots()
                            .get(enemySlot)
                            .loaded()
                    && route.frame < orderedEnemyDeathDeadline) {
                tickInteractiveEntities(route.session, route.frame++, 0, 0);
            }
            assertFalse(route.session
                    .rooms()
                    .activeRoomOrNull()
                    .entities()
                    .slots()
                    .get(enemySlot)
                    .loaded());
        }

        route.room27ChestIndex = RoomConstants.ROOM_OBJECTS_BASE + 0x28;
        assertEquals(0, RoomDebugFixture.of(route.session).activeRoomEventForTest());
        route.room27ChestRevealDeadline = route.frame + 0x40;
        while (route.session.rooms().activeRoomOrNull().roomObjectsArea()[route.room27ChestIndex] != 0xA0
                && route.frame < route.room27ChestRevealDeadline) {
            tickInteractiveEntities(
                    route.session,
                    route.frame++,
                    route.link.position().romEntityX(),
                    route.link.position().romEntityY());
        }
        assertEquals(0xA0, route.session.rooms().activeRoomOrNull().roomObjectsArea()[route.room27ChestIndex]);

        route.link.position().setPixelPosition(0x80, 0x21);
        route.link.animation().setDirection(Direction.UP.javaIndex());
        route.room27Chest = route.session
                .doors()
                .tryOpenChest(
                        route.link.position().pixelX(),
                        route.link.position().pixelY(),
                        Direction.fromJavaOrUnknown(Direction.UP.javaIndex()),
                        true,
                        route.playerState.equipmentLevels().swordLevel());
        assertTrue(route.room27Chest.opened());
        assertEquals(ChestContentsTable.CHEST_NIGHTMARE_KEY, route.room27Chest.itemType());
        assertEquals(0x28, route.room27Chest.location());
        route.room27ChestSlot = route.room27Chest.entitySlot();
        route.room27DialogId = new ChestContentsTable((route.rom == null ? (Rom) null : TestRoms.partial(route.rom)))
                .dialogLowIdFor(
                        ChestContentsTable.CHEST_NIGHTMARE_KEY,
                        route.playerState.equipmentLevels().shieldLevel(),
                        route.playerState.equipmentLevels().swordLevel(),
                        route.playerState.equipmentLevels().powerBraceletLevel(),
                        0x01,
                        0x27);
        route.nightmareKeyRewardObserved = false;
        route.nightmareKeyDialogObserved = false;
        route.room27ChestEnded = false;
        for (int chestTick = 0; chestTick < 0x40; chestTick++) {
            tickInteractiveEntities(
                    route.session,
                    route.frame++,
                    route.link.position().romEntityX(),
                    route.link.position().romEntityY());
            for (ChestRewardEvent reward :
                    route.session.components.entityBridge().outbox.consumeChestRewardEvents()) {
                route.nightmareKeyRewardObserved |= reward.itemType() == ChestContentsTable.CHEST_NIGHTMARE_KEY;
                route.playerState.chestRewards().applyChestReward(reward.itemType());
            }
            route.nightmareKeyDialogObserved |= route.session.entityState().consumeEntityDialogRequests().stream()
                    .anyMatch(request -> request.globalDialogId() == route.room27DialogId);
            RoomEntity chestEntity =
                    route.session.rooms().activeRoomOrNull().entities().slots().get(route.room27ChestSlot);
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
        assertEquals(0x10, RoomDebugFixture.of(route.session).indoorRoomStatusForTest(0x01, 0x27) & 0x10);

        route.collision.refreshLinkGroundInteraction(
                route.link.position().pixelX(), route.link.position().pixelY());
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
                RoomDebugFixture.of(route.session).activeRoomEventForTest(),
                "the source event byte reloads but status $10 keeps it inert");
        assertFalse(route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
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
        tickInteractiveEntities(
                route.session,
                route.frame++,
                route.link.position().romEntityX(),
                route.link.position().romEntityY());
        route.room24CrystalSwitch = route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x66)
                .findFirst()
                .orElseThrow();
        route.room24SwitchApproach = reachableSwordContactPathWithFeather(
                route.collision,
                route.romTables,
                route.link.position().pixelX(),
                route.link.position().pixelY(),
                route.room24CrystalSwitch);
        assertNotNull(route.room24SwitchApproach, collisionGrid(route.collision));
        for (int[] position : route.room24SwitchApproach.path()) {
            route.link.position().setPixelPosition(position[0], position[1]);
        }
        route.link.animation().setDirection(route.room24SwitchApproach.direction());
        route.room24Sword = new Sword(route.romTables, null);
        route.room24Sword.onPress();
        for (int swingFrame = 0; swingFrame < 4; swingFrame++) {
            route.room24Sword.tick(false);
        }
        route.initialRoom24SwordBox = route.room24Sword.enemyCollisionBox(
                route.link.position().romEntityX(),
                route.link.position().romSwordCollisionY(),
                route.link.animation().direction());
        route.link
                .position()
                .setPixelPosition(
                        route.room24CrystalSwitch.x()
                                - (route.initialRoom24SwordBox.x()
                                        - route.link.position().pixelX()),
                        route.room24CrystalSwitch.y()
                                - (route.initialRoom24SwordBox.y()
                                        - route.link.position().pixelY()));
        route.room24SwordBox = route.room24Sword.enemyCollisionBox(
                route.link.position().romEntityX(),
                route.link.position().romSwordCollisionY(),
                route.link.animation().direction());
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
                            route.link.position().romEntityX(),
                            route.link.position().romEntityY(),
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
        tickInteractiveEntities(
                route.session,
                route.frame++,
                route.link.position().romEntityX(),
                route.link.position().romEntityY());
        while (RoomDebugFixture.of(route.session).switchableObjectAnimationStageForTest() != 0) {
            route.session.entityState().tickGameplayVBlank();
        }
        assertEquals(0x00, RoomDebugFixture.of(route.session).entitySwitchBlocksStateForTest());
        route.room24SwitchReachablePosition = route.room24SwitchApproach.path().getLast();
        route.link
                .position()
                .setPixelPosition(route.room24SwitchReachablePosition[0], route.room24SwitchReachablePosition[1]);
        route.collision.refreshLinkGroundInteraction(
                route.link.position().pixelX(), route.link.position().pixelY());

        route.link.position().setPixelPosition(0x85, 0x38);
        assertTrue(route.collision.objectPhysicsFlagAtPoint(
                                route.link.position().pixelX() + 0x0B,
                                route.link.position().pixelY() + 0x09)
                        == 0x93
                || route.collision.objectPhysicsFlagAtPoint(
                                route.link.position().pixelX() + 0x0B,
                                route.link.position().pixelY() + 0x0C)
                        == 0x93);
        assertTrue(route.session
                .doors()
                .tryUnlockIndoorKeyDoor(
                        route.link.position().pixelX(),
                        route.link.position().pixelY(),
                        Direction.fromJavaOrUnknown(Direction.RIGHT.javaIndex()),
                        0x08));
        assertEquals(0, route.session.dungeon().currentDungeonItemFlagsSnapshot()[DungeonItemState.SMALL_KEYS_INDEX]);
        for (int tick = 0; tick < 8; tick++) {
            route.session
                    .entities()
                    .tickEntities(
                            route.frame++,
                            route.link.position().pixelX(),
                            route.link.position().pixelY());
            assertTrue(route.session.components.entityBridge().consumeWorldLinkMotionBlockRequest());
        }
        route.session
                .entities()
                .tickEntities(
                        route.frame++,
                        route.link.position().pixelX(),
                        route.link.position().pixelY());
        assertFalse(route.session.components.entityBridge().consumeWorldLinkMotionBlockRequest());
        assertEquals(0x01, RoomDebugFixture.of(route.session).indoorRoomStatusForTest(0x01, 0x24) & 0x01);
        assertEquals(0x02, RoomDebugFixture.of(route.session).indoorRoomStatusForTest(0x01, 0x25) & 0x02);

        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.RIGHT);
        assertEquals(0x25, route.session.rooms().currentRoomId());
        assertEquals(0xA1, RoomDebugFixture.of(route.session).activeRoomEventForTest());
        assertEquals(
                2,
                route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                        .filter(entity -> entity.type() == 0x18)
                        .count());
        assertEquals(
                1,
                route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                        .filter(entity -> entity.type() == 0x1B)
                        .count());
        assertEquals(0x0D, objectAt(route.session, 0x18));
        tickInteractiveEntities(
                route.session,
                route.frame++,
                route.link.position().romEntityX(),
                route.link.position().romEntityY());

        route.room25PolsVoices = route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x18)
                .sorted(java.util.Comparator.comparingInt(RoomEntity::sourceLoadOrder))
                .toList();
        route.room25PotLocations = new int[] {0x11, 0x12};
        route.room25PotLinkX = new int[] {0x10, 0x20};
        for (int polsIndex = 0; polsIndex < route.room25PolsVoices.size(); polsIndex++) {
            RoomEntity polsVoice = route.room25PolsVoices.get(polsIndex);
            int potLocation = route.room25PotLocations[polsIndex];
            assertEquals(0x20, objectAt(route.session, potLocation));
            route.link.position().setPixelPosition(route.room25PotLinkX[polsIndex], 0x0B);
            route.link.animation().setDirection(Direction.UP.javaIndex());
            for (int pullFrame = 0; pullFrame < 8; pullFrame++) {
                assertTrue(route.session
                        .doors()
                        .tryLiftIndoorObject(
                                route.link.position().pixelX(),
                                route.link.position().pixelY(),
                                Direction.fromJavaOrUnknown(Direction.UP.javaIndex()),
                                route.playerState.equipmentLevels().powerBraceletLevel() != 0,
                                0x08));
            }
            assertEquals(0x0D, objectAt(route.session, potLocation));
            int liftDeadline = route.frame + 0x40;
            while (route.session.rooms().liftedEntityState().carryState() != 0x01 && route.frame < liftDeadline) {
                tickInteractiveEntities(
                        route.session,
                        route.frame++,
                        route.link.position().romEntityX(),
                        route.link.position().romEntityY());
            }
            assertEquals(0x01, route.session.rooms().liftedEntityState().carryState());

            polsVoice =
                    route.session.rooms().activeRoomOrNull().entities().slots().get(polsVoice.slot());
            route.link.position().setPixelPosition(polsVoice.x() - 0x0B, polsVoice.y() - polsVoice.z() - 0x02);
            route.link.animation().setDirection(Direction.RIGHT.javaIndex());
            route.session
                    .entities()
                    .tickEntitiesWithProjectileEvents(
                            route.frame++,
                            route.link.position().romEntityX(),
                            route.link.position().romEntityY(),
                            0,
                            0,
                            Direction.RIGHT.javaIndex(),
                            false);
            polsVoice =
                    route.session.rooms().activeRoomOrNull().entities().slots().get(polsVoice.slot());
            assertTrue(route.session.playerItems().throwLiftedEntity(Direction.fromJava(Direction.RIGHT.javaIndex())));
            while (((route.frame ^ polsVoice.slot()) & 1) != 0) {
                route.frame++;
            }
            route.session
                    .entities()
                    .tickEntitiesWithProjectileEvents(
                            route.frame++,
                            route.link.position().romEntityX(),
                            route.link.position().romEntityY(),
                            0,
                            0,
                            Direction.RIGHT.javaIndex(),
                            false);
            assertEquals(
                    EntityStatus.DYING,
                    route.session
                            .rooms()
                            .activeRoomOrNull()
                            .entities()
                            .slots()
                            .get(polsVoice.slot())
                            .status());
            int room25PolsDeathDeadline = route.frame + 0x80;
            while (route.session
                            .rooms()
                            .activeRoomOrNull()
                            .entities()
                            .slots()
                            .get(polsVoice.slot())
                            .loaded()
                    && route.frame < room25PolsDeathDeadline) {
                tickInteractiveEntities(
                        route.session,
                        route.frame++,
                        route.link.position().romEntityX(),
                        route.link.position().romEntityY());
            }
            assertFalse(route.session
                    .rooms()
                    .activeRoomOrNull()
                    .entities()
                    .slots()
                    .get(polsVoice.slot())
                    .loaded());
            assertEquals(0xA1, RoomDebugFixture.of(route.session).activeRoomEventForTest());
        }

        route.room25Zol = route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x1B)
                .findFirst()
                .orElseThrow();
        route.room25ZolDeadline = route.frame + 0x200;
        while (route.session
                                .rooms()
                                .activeRoomOrNull()
                                .entities()
                                .slots()
                                .get(route.room25Zol.slot())
                                .status()
                        == EntityStatus.ACTIVE
                && route.frame < route.room25ZolDeadline) {
            RoomEntity target =
                    route.session.rooms().activeRoomOrNull().entities().slots().get(route.room25Zol.slot());
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
                                            .activeRoomOrNull()
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
                        .activeRoomOrNull()
                        .entities()
                        .slots()
                        .get(route.room25Zol.slot())
                        .status());
        route.room25ZolDeathDeadline = route.frame + 0x80;
        while (route.session
                        .rooms()
                        .activeRoomOrNull()
                        .entities()
                        .slots()
                        .get(route.room25Zol.slot())
                        .loaded()
                && route.frame < route.room25ZolDeathDeadline) {
            tickInteractiveEntities(route.session, route.frame++, 0, 0);
        }
        assertFalse(route.session
                .rooms()
                .activeRoomOrNull()
                .entities()
                .slots()
                .get(route.room25Zol.slot())
                .loaded());

        // The Red Zol reuses its slot and spawns a second Gel (06:7C50).
        // Clearing the room requires defeating both descendants.
        route.gelDeadline = route.frame + 0x400;
        while (route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                        .anyMatch(entity -> entity.type() == 0x1C)
                && route.frame < route.gelDeadline) {
            for (RoomEntity gel : route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
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
        while (RoomDebugFixture.of(route.session).activeRoomEventForTest() != 0
                && route.frame < route.room25StairRevealDeadline) {
            tickInteractiveEntities(
                    route.session,
                    route.frame++,
                    route.link.position().romEntityX(),
                    route.link.position().romEntityY());
        }
        assertEquals(0, RoomDebugFixture.of(route.session).activeRoomEventForTest());
        while (objectAt(route.session, 0x18) != 0xBE && route.frame < route.room25StairRevealDeadline) {
            tickInteractiveEntities(
                    route.session,
                    route.frame++,
                    route.link.position().romEntityX(),
                    route.link.position().romEntityY());
        }
        assertEquals(0xBE, objectAt(route.session, 0x18));
        assertEquals(0x10, RoomDebugFixture.of(route.session).indoorRoomStatusForTest(0x01, 0x25) & 0x10);

        route.coordinator.handleWarpAndIndoorBoundaries(route.link);
        assertFalse(route.transition.isActive());
        route.link.position().setPixelPosition(0x80, 0x10);
        route.coordinator.handleWarpAndIndoorBoundaries(route.link);
        assertTrue(route.transition.isActive());
        while (route.transition.isActive()) {
            route.transition.tick();
        }
        assertEquals(Warp.CATEGORY_SIDESCROLL, route.session.rooms().mapCategory());
        assertEquals(0x01, route.session.rooms().activeRoomOrNull().mapId());
        assertEquals(0x3B, route.session.rooms().currentRoomId());
        assertEquals(0x88, route.link.position().romEntityX());
        assertEquals(0x10, route.link.position().romEntityY());
        assertEquals(
                1,
                route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                        .filter(entity -> entity.type() == 0xA5)
                        .count());
        assertEquals(
                2,
                route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                        .filter(entity -> entity.type() == 0xD6)
                        .count());

        route.room3BPlatform = route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0xA5)
                .findFirst()
                .orElseThrow();
        route.room3BPots = route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
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

        route.session.setEntityInventorySlots(0x00, 0x03);
        route.session.entityState().setEntityPowerBraceletButtonHeld(true);
        route.session.entityState().setEntityActionButtonsHeld(false, true);
        route.session.entityState().setEntityAttackStepAnimationCountdown(0);

        // Main's live ordering: Link moves first, AnimateEntities consumes the
        // captured final X, then typed Link writes are applied before the next
        // Link update. Start at the authored A5 contact window.
        tickLiveRoom3B(route.session, route.link, route.coordinator, route.transition, route.scroll, route.frame++);
        route.link.position().setPixelPosition(route.room3BPlatform.x(), route.room3BPlatform.y() - 0x20);
        route.room3BPlatformEvents = tickLiveRoom3B(
                route.session, route.link, route.coordinator, route.transition, route.scroll, route.frame++);
        assertTrue(route.link.sideView().standingOnSideViewEntity());
        assertFalse(route.transition.isActive());
        assertFalse(route.scroll.isActive());
        assertTrue(route.room3BPlatformEvents.stream()
                .noneMatch(event -> event.slot() == route.room3BPlatform.slot() && event.linkDamage() != 0));
        route.firstPlatformY = route.session
                .rooms()
                .activeRoomOrNull()
                .entities()
                .slots()
                .get(route.room3BPlatform.slot())
                .y();
        for (int rideFrame = 0; rideFrame < 16; rideFrame++) {
            tickLiveRoom3B(route.session, route.link, route.coordinator, route.transition, route.scroll, route.frame++);
        }
    }

    static void rideMovingPlatform(GameplayRouteState route) throws Exception {

        route.riddenPlatformY = route.session
                .rooms()
                .activeRoomOrNull()
                .entities()
                .slots()
                .get(route.room3BPlatform.slot())
                .y();
        assertTrue(
                route.riddenPlatformY > route.firstPlatformY, "A5 platform did not move while Link was standing on it");

        // Contact the first authored D6 through the same request/update path,
        // then let its countdown settle before throwing it rightward.
        route.firstPot = route.session
                .rooms()
                .activeRoomOrNull()
                .entities()
                .slots()
                .get(route.room3BPots.getFirst().slot());
        route.link.sideView().clearSideViewPlatformState();
        // The preceding live Link ticks may have left the attack-step timer
        // nonzero; ReturnIfNonInteractive_19's pickup path requires zero.
        route.link.equipment().clearRomAttackStepAnimationCountdown();
        route.session.entityState().setEntityAttackStepAnimationCountdown(0);
        // Keep the first pot in its raw -$12..+$11 lift window while placing
        // Link just outside the second pot's descending-slot window.
        route.link.position().setPixelPosition(route.firstPot.x() - 0x0B, route.firstPot.y() - 0x10);
        // EntityGetLiftedUp prioritizes the B inventory slot. A held alone
        // must not lift when B contains the bracelet but is not held.
        route.session.entityState().setEntityActionButtonsHeld(true, false);
        route.link.equipment().clearRomAttackStepAnimationCountdown();
        route.session.entityState().setEntityAttackStepAnimationCountdown(0);
        tickLiveRoom3B(route.session, route.link, route.coordinator, route.transition, route.scroll, route.frame++);
        assertEquals(
                EntityStatus.ACTIVE,
                route.session
                        .rooms()
                        .activeRoomOrNull()
                        .entities()
                        .slots()
                        .get(route.firstPot.slot())
                        .status());

        // The same contact with B held is the successful exact-priority path.
        route.session.entityState().setEntityActionButtonsHeld(false, true);
        route.link.position().setPixelPosition(route.firstPot.x() - 0x0B, route.firstPot.y() - 0x10);
        route.potPickupEvents = tickLiveRoom3B(
                route.session, route.link, route.coordinator, route.transition, route.scroll, route.frame++);
        assertEquals(
                EntityStatus.LIFTED,
                route.session
                        .rooms()
                        .activeRoomOrNull()
                        .entities()
                        .slots()
                        .get(route.firstPot.slot())
                        .status(),
                String.format(
                        "pot=%02X/%02X link=%02X/%02X",
                        route.firstPot.x(),
                        route.firstPot.y(),
                        route.link.position().romEntityX(),
                        route.link.position().romEntityY()));
        assertTrue(route.session.rooms().liftedEntityState().active());
        assertTrue(route.potPickupEvents.stream()
                .anyMatch(event ->
                        event.soundChannel() == EntityCombatEvent.SoundChannel.WAVE && event.soundId() == 0x02));

        for (int liftFrame = 0;
                liftFrame < 0x20 && route.session.rooms().liftedEntityState().carryState() != 0x01;
                liftFrame++) {
            tickLiveRoom3B(route.session, route.link, route.coordinator, route.transition, route.scroll, route.frame++);
        }
        assertEquals(0x01, route.session.rooms().liftedEntityState().carryState());
        assertTrue(route.session.playerItems().throwLiftedEntity(Direction.fromJava(Direction.RIGHT.javaIndex())));
        assertEquals(
                EntityStatus.ACTIVE,
                route.session
                        .rooms()
                        .activeRoomOrNull()
                        .entities()
                        .slots()
                        .get(route.firstPot.slot())
                        .status());
        route.throwEvents = tickLiveRoom3B(
                route.session, route.link, route.coordinator, route.transition, route.scroll, route.frame++);
        route.thrownPot =
                route.session.rooms().activeRoomOrNull().entities().slots().get(route.firstPot.slot());
        assertEquals(EntityStatus.ACTIVE, route.thrownPot.status());
        assertTrue(route.thrownPot.x() != route.firstPot.x() || route.thrownPot.y() != route.firstPot.y());
        assertTrue(route.throwEvents.stream()
                .anyMatch(event ->
                        event.soundChannel() == EntityCombatEvent.SoundChannel.JINGLE && event.soundId() == 0x08));
        assertEquals(0x0C, route.link.equipment().romAttackStepAnimationCountdown());

        route.room3BExit = route.session.rooms().activeRoomOrNull().warps().stream()
                .filter(warp -> warp.destMap() == 0x01
                        && warp.destRoom() == 0x25
                        && warp.destX() == 0x88
                        && warp.destY() == 0x20)
                .findFirst()
                .orElseThrow();
        assertEquals(-1, route.room3BExit.tileLocation());
        route.link.position().setPixelPosition(0x80, -5);
        route.coordinator.handleWarpAndIndoorBoundaries(route.link);
        assertTrue(route.transition.isActive());
        while (route.transition.isActive()) {
            route.transition.tick();
        }
        assertEquals(Warp.CATEGORY_INDOOR, route.session.rooms().mapCategory());
        assertEquals(0x01, route.session.rooms().activeRoomOrNull().mapId());
        assertEquals(0x25, route.session.rooms().currentRoomId());
        assertEquals(0x88, route.link.position().romEntityX());
        assertEquals(0x20, route.link.position().romEntityY());

        // The authored return path re-enters side-view room $3B, crosses left
        // into $3A, then uses $3A's E1 warp to room $2D. This bypasses room
        // $2A's instrument/shutter path and preserves the Nightmare Key until
        // the north boss door in $2D is opened.
        assertEquals(
                1, route.session.dungeon().currentDungeonItemFlagsSnapshot()[DungeonItemState.NIGHTMARE_KEY_INDEX]);
        route.room25SideViewEntry = route.session.rooms().activeRoomOrNull().warps().stream()
                .filter(warp -> warp.destMap() == 0x01
                        && warp.destRoom() == 0x3B
                        && warp.destX() == 0x88
                        && warp.destY() == 0x10)
                .findFirst()
                .orElseThrow();
        assertEquals(-1, route.room25SideViewEntry.tileLocation());
        route.link.position().setPixelPosition(0x80, 0x10);
        route.coordinator.handleWarpAndIndoorBoundaries(route.link);
        assertFalse(route.transition.isActive());
        route.link.position().setPixelPosition(0x90, 0x10);
        route.coordinator.handleWarpAndIndoorBoundaries(route.link);
        assertFalse(route.transition.isActive());
        route.link.position().setPixelPosition(0x80, 0x10);
        route.coordinator.handleWarpAndIndoorBoundaries(route.link);
        assertTrue(route.transition.isActive());
        while (route.transition.isActive()) {
            route.transition.tick();
        }
        assertEquals(Warp.CATEGORY_SIDESCROLL, route.session.rooms().mapCategory());
        assertEquals(0x3B, route.session.rooms().currentRoomId());
        assertEquals(0x88, route.link.position().romEntityX());
        assertEquals(0x10, route.link.position().romEntityY());

        tickLiveRoom3B(route.session, route.link, route.coordinator, route.transition, route.scroll, route.frame++);
        route.link.position().setPixelPosition(-5, 0x30);
        route.coordinator.handleWarpAndIndoorBoundaries(route.link);
        assertTrue(route.scroll.isActive());
        assertEquals(0x3A, route.session.rooms().currentRoomId());
        while (route.scroll.isActive()) {
            route.scroll.tick(8);
        }
        assertEquals(Warp.CATEGORY_SIDESCROLL, route.session.rooms().mapCategory());
        assertEquals(0x3A, route.session.rooms().currentRoomId());
        assertTrue(route.session.rooms().currentRoomId() != 0x2A);

        route.room3AWarpTo2D = route.session.rooms().activeRoomOrNull().warps().stream()
                .filter(warp -> warp.destMap() == 0x01
                        && warp.destRoom() == 0x2D
                        && warp.destX() == 0x88
                        && warp.destY() == 0x20)
                .findFirst()
                .orElseThrow();
        assertEquals(-1, route.room3AWarpTo2D.tileLocation());
        route.link.position().setPixelPosition(0x80, -5);
        route.coordinator.handleWarpAndIndoorBoundaries(route.link);
        assertTrue(route.transition.isActive());
        while (route.transition.isActive()) {
            route.transition.tick();
        }
        assertEquals(Warp.CATEGORY_INDOOR, route.session.rooms().mapCategory());
        assertEquals(0x2D, route.session.rooms().currentRoomId());
        assertEquals(0x88, route.link.position().romEntityX());
        assertEquals(0x20, route.link.position().romEntityY());
        assertTrue(route.session.rooms().currentRoomId() != 0x2A);
        assertEquals(
                1, route.session.dungeon().currentDungeonItemFlagsSnapshot()[DungeonItemState.NIGHTMARE_KEY_INDEX]);

        // Source-shaped north boss-door contact: the C4/$14 and C4/$15
        // records expand to the top-facing A4/A5 pair at room cells $14/$15.
        // Link's leading-edge probes therefore use the corresponding Java
        // top-left position (x=$48, y=$FA/-6), aligned with the door's
        // eight-pixel opening window; probes land on x=$4E/$51,y=$00.
        route.link.position().setPixelPosition(0x48, -6);
        route.link.animation().setDirection(Direction.UP.javaIndex());
        assertEquals(0x48, route.link.position().pixelX());
        assertEquals(-6, route.link.position().pixelY());
        assertTrue(
                route.session
                        .doors()
                        .tryUnlockIndoorKeyDoor(
                                route.link.position().pixelX(),
                                route.link.position().pixelY(),
                                Direction.fromJavaOrUnknown(Direction.UP.javaIndex()),
                                0x01),
                "room2D boss-door leading-edge probes did not contact A4/A5");
        // Source bank2 checks wHasDungeonBossKey but does not decrement it
        // (only the $90-$93 small-key branch consumes a key).
        assertEquals(
                1, route.session.dungeon().currentDungeonItemFlagsSnapshot()[DungeonItemState.NIGHTMARE_KEY_INDEX]);
        for (int tick = 0; tick < 8; tick++) {
            route.session
                    .entities()
                    .tickEntities(
                            route.frame++,
                            route.link.position().pixelX(),
                            route.link.position().pixelY());
            assertTrue(route.session.components.entityBridge().consumeWorldLinkMotionBlockRequest());
        }
        route.session
                .entities()
                .tickEntities(
                        route.frame++,
                        route.link.position().pixelX(),
                        route.link.position().pixelY());
        assertFalse(route.session.components.entityBridge().consumeWorldLinkMotionBlockRequest());
        assertEquals(0x43, objectAt(route.session, 0x04));
        assertEquals(0x44, objectAt(route.session, 0x05));
        assertEquals(0x04, RoomDebugFixture.of(route.session).indoorRoomStatusForTest(0x01, 0x2D) & 0x04);

        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.UP);
        assertEquals(0x2B, route.session.rooms().currentRoomId());
        assertTrue(route.session.rooms().currentRoomId() != 0x2A);
        route.genieEntities = route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x5C)
                .toList();
        assertEquals(1, route.genieEntities.size());
        route.genie = route.genieEntities.getFirst();
        assertEquals(0x48, route.genie.x());
        assertEquals(0x30, route.genie.y());
        assertTrue((route.romTables.entityPropertyTables().entityOptions1(0x5C) & 0x80) != 0);

        // Once Genie is gone, room $2B's event $21 opens the west shutter.
        // Crossing that authored boundary reaches the instrument room $2A;
        // it is never a pre-boss shortcut.
        route.clearedBossSlots = new ArrayList<>();
        for (int slot = 0; slot < EntityRoomLoader.MAX_ENTITIES; slot++) {
            route.clearedBossSlots.add(RoomEntity.disabled(slot));
        }
        RoomDebugFixture.of(route.session)
                .replaceEntityRuntimeForTest(RoomEntityRuntime.builder(new RoomEntitySnapshot(route.clearedBossSlots))
                        .indoorRoom(true)
                        .build());
        route.session
                .entities()
                .tickEntities(
                        route.frame++,
                        route.link.position().pixelX(),
                        route.link.position().pixelY());
        assertEquals(0, RoomDebugFixture.of(route.session).activeRoomEventForTest());
        route.link.position().setPixelPosition(0x50, 0x50);
        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.LEFT);
        assertEquals(0x2A, route.session.rooms().currentRoomId());
        assertTrue(route.session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                .anyMatch(entity -> entity.type() == EntitySpriteHandlerCatalog.ENTITY_INSTRUMENT_OF_THE_SIRENS));
    }
}
