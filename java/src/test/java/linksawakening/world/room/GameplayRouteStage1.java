package linksawakening.world.room;

import static linksawakening.world.room.RoomTransitionTestSupport.*;
import static linksawakening.world.room.RoomTransitionTestSupport2.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import linksawakening.entity.Link;
import linksawakening.equipment.ItemRegistry;
import linksawakening.equipment.RocsFeather;
import linksawakening.gameplay.BeachSwordRewardConsumer;
import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
import linksawakening.physics.OverworldCollision;
import linksawakening.rom.RomTables;
import linksawakening.startup.NewGameStartProfile;
import linksawakening.startup.TarinShieldMotion;
import linksawakening.state.PlayerState;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.ToadstoolRewardEvent;
import linksawakening.world.transition.RoomBoundaryController;
import linksawakening.world.transition.ScrollController;
import linksawakening.world.transition.TransitionController;
import linksawakening.world.transition.Warp;

final class GameplayRouteStage1 {
    static void run(GameplayRouteState route) throws Exception {

        route.rom = loadRom();
        route.romTables = RomTables.loadFromRom(route.rom);
        route.collision = new OverworldCollision(route.romTables);
        route.session = newSession(route.rom, route.romTables, route.collision);
        route.playerState = new PlayerState();
        route.profile = NewGameStartProfile.romDefaults();
        route.profile.initializePlayerState(route.playerState);
        route.session.initializeNewGameWorldState();
        route.session.loadIndoor(route.profile.mapId(), route.profile.roomId());

        assertTrue(route.session.activeRoom().entities().loadedEntities().stream()
                .anyMatch(entity -> entity.type() == 0x3E));
        route.tarin = route.session.activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x3F)
                .findFirst()
                .orElseThrow();

        route.shieldMotion = new TarinShieldMotion();
        assertEquals(
                0x54,
                route.shieldMotion
                        .tick(false, 0x60, true, route.playerState.shieldLevel())
                        .dialogLowId());
        route.shieldMotion.tick(true, 0x60, false, route.playerState.shieldLevel());
        route.shieldMotion.tick(false, 0x60, false, route.playerState.shieldLevel());
        route.shieldReward = null;
        for (int frame = 0; frame < 0x80; frame++) {
            route.shieldReward = route.shieldMotion.tick(false, 0x60, false, route.playerState.shieldLevel());
        }
        assertTrue(route.shieldReward.grantShield());
        route.playerState.applyChestReward(ChestContentsTable.CHEST_SHIELD);
        assertEquals(1, route.playerState.shieldLevel());
        assertEquals(0, route.playerState.swordLevel());
        assertTrue(route.tarin.spriteDefinition().supported());

        route.scroll = new ScrollController();
        route.transition = new TransitionController();
        route.coordinator = new RoomTransitionCoordinator(
                route.session, new RoomBoundaryController(), route.transition, route.scroll);
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
        route.itemRegistry.register(PlayerState.INVENTORY_ROCS_FEATHER, new RocsFeather(route.link));
        route.link.setPixelPosition(0x48, RoomConstants.ROOM_PIXEL_HEIGHT);
        route.coordinator.handleWarpAndIndoorBoundaries(route.link);
        while (route.transition.isActive()) route.transition.tick();
        assertEquals(0xA2, route.session.currentRoomId());

        for (int expectedRoom = 0xB2; expectedRoom <= 0xF2; expectedRoom += 0x10) {
            route.link.setPixelPosition(route.link.pixelX(), RoomConstants.ROOM_PIXEL_HEIGHT);
            route.coordinator.handleOverworldBoundary(route.link);
            assertEquals(expectedRoom, route.session.currentRoomId());
            while (route.scroll.isActive()) {
                route.scroll.tick(8);
            }
            // A north-edge cliff now runs the source's post-scroll drop.
            // Let it land before teleporting to the next boundary.
            for (int frame = 0; frame < 64 && route.link.isAirborne(); frame++) route.link.update();
            assertFalse(route.link.isAirborne());
        }

        route.sword = route.session.activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x31)
                .findFirst()
                .orElseThrow();
        route.owl = route.session.activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x41)
                .findFirst()
                .orElseThrow();
        assertEquals(0x58, route.sword.x());
        assertEquals(0x60, route.sword.y());
        assertTrue(route.sword.spriteDefinition().supported());
        assertTrue(route.owl.spriteDefinition().supported());

        route.session.setChestPlayerLevels(
                route.playerState.shieldLevel(),
                route.playerState.swordLevel(),
                route.playerState.powerBraceletLevel());
        tickInteractiveEntities(route.session, 0, 0x3F, 0x44);
        tickInteractiveEntities(route.session, 1, 0x40, 0x44);
        assertEquals(0x22, route.session.consumePendingMusicTrack());

        route.frame = 2;
        route.owlDialogOpened = false;
        while ((route.session.overworldRoomStatusForTest(0xF2) & 0x20) == 0 && route.frame < 0x300) {
            tickInteractiveEntities(route.session, route.frame++, 0x58, 0x60);
            route.owlDialogOpened |= route.session.consumeEntityDialogRequests().stream()
                    .anyMatch(request -> request.globalDialogId() == 0x0D9);
        }
        assertTrue(route.owlDialogOpened);

        while (route.session.activeRoom().entities().loadedEntities().stream().anyMatch(entity -> entity.type() == 0x41)
                && route.frame < 0x400) {
            tickInteractiveEntities(route.session, route.frame++, 0x58, 0x60);
        }
        route.pickupFrame = (route.frame & 0xFE) | ((route.sword.slot() ^ 1) & 1);
        route.pickup = route.session.collectEntityIfNeeded(
                route.pickupFrame, route.sword.x(), route.sword.y(), false, true, 3, 0);
        assertNotNull(route.pickup);

        route.frame = route.pickupFrame + 1;
        while (route.playerState.swordLevel() == 0 && route.frame < route.pickupFrame + 0x300) {
            route.session.tickEntities(route.frame++, route.sword.x(), route.sword.y());
            route.session.consumeEntityDialogRequests();
            BeachSwordRewardConsumer.consume(route.session, route.playerState);
        }
        assertEquals(1, route.playerState.swordLevel());
        assertEquals(0x30, route.session.overworldRoomStatusForTest(0xF2) & 0x30);

        route.session.loadInitialOverworld(0xF2);
        assertFalse(route.session.activeRoom().entities().loadedEntities().stream()
                .anyMatch(entity -> entity.type() == 0x31 || entity.type() == 0x41));

        route.session.setChestPlayerLevels(
                route.playerState.shieldLevel(),
                route.playerState.swordLevel(),
                route.playerState.powerBraceletLevel());
        route.link.setPixelPosition(0x50, 0x50);
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
            assertEquals(route.expectedRooms[index], route.session.currentRoomId());
        }
        assertEquals(0x80, route.session.currentRoomId());
        assertTrue(route.session.activeRoom().entities().loadedEntities().stream()
                .anyMatch(entity -> entity.type() == 0x41));

        tickInteractiveEntities(route.session, route.frame++, 0x50, 0x50);
        tickInteractiveEntities(route.session, route.frame++, 0x50, 0x50);
        assertEquals(0x22, route.session.consumePendingMusicTrack());
        route.forestOwlDialogOpened = false;
        while (!route.forestOwlDialogOpened && route.frame < route.pickupFrame + 0x600) {
            tickInteractiveEntities(route.session, route.frame++, 0x50, 0x50);
            route.forestOwlDialogOpened |= route.session.consumeEntityDialogRequests().stream()
                    .anyMatch(request -> request.globalDialogId() == 0x0C0);
        }
        assertTrue(route.forestOwlDialogOpened);
        while ((route.session.overworldRoomStatusForTest(0x80) & 0x20) == 0
                && route.frame < route.pickupFrame + 0x700) {
            tickInteractiveEntities(route.session, route.frame++, 0x50, 0x50);
        }
        assertEquals(0x20, route.session.overworldRoomStatusForTest(0x80) & 0x20);

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
            assertEquals(route.toadstoolRouteRooms[index], route.session.currentRoomId());
        }
        assertEquals(0x50, route.session.currentRoomId());
        route.toadstool = route.session.activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x3A)
                .findFirst()
                .orElseThrow();
        route.session.setToadstoolPlayerState(false, 0);
        route.session.tickEntities(route.frame++, route.toadstool.x(), route.toadstool.y());
        route.toadstool = route.session.activeRoom().entities().slots().get(route.toadstool.slot());
        route.toadstoolPickup = route.session.collectEntityIfNeeded(
                (route.frame & 0xFE) | ((route.toadstool.slot() ^ 1) & 1),
                route.toadstool.x(),
                route.toadstool.y(),
                false,
                true,
                Link.DIRECTION_RIGHT,
                0);
        assertNotNull(route.toadstoolPickup);
        route.toadstoolRewards = List.of();
        while (route.toadstoolRewards.isEmpty() && route.frame < route.pickupFrame + 0x900) {
            route.session.tickEntities(route.frame++, route.toadstool.x(), route.toadstool.y());
            route.session.consumeEntityDialogRequests();
            route.toadstoolRewards = route.session.consumeToadstoolRewards();
        }
        assertEquals(List.of(new ToadstoolRewardEvent(route.toadstool.slot())), route.toadstoolRewards);
        // Mirrors Main's live event consumer; RoomSession owns the entity
        // cadence and PlayerState owns the resulting inventory mutation.
        route.playerState.applyToadstoolReward();
        assertTrue(route.playerState.hasToadstool());
        assertEquals(PlayerState.INVENTORY_MAGIC_POWDER, route.playerState.subscreenItem(0));

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
            assertEquals(route.witchRouteRooms[index], route.session.currentRoomId());
        }
        assertEquals(0x65, route.session.currentRoomId());
        route.witchHut = route.session.activeRoom().warps().stream()
                .filter(warp -> warp.destMap() == 0x0E && warp.destRoom() == 0xA2)
                .findFirst()
                .orElseThrow();
        assertEquals(0x24, route.witchHut.tileLocation());
        route.coordinator = new RoomTransitionCoordinator(
                route.session, new RoomBoundaryController(), route.transition, route.scroll);
        route.link.setPixelPosition(
                (route.witchHut.tileLocation() & 0x0F) * 16, (route.witchHut.tileLocation() >>> 4) * 16);
        walkIntoDoor(route.link, route.inputState, route.inputConfig, route.coordinator, route.transition);
        while (route.transition.isActive()) route.transition.tick();
        assertEquals(0x0E, route.session.activeRoom().mapId());
        assertEquals(0xA2, route.session.currentRoomId());

        route.witch = route.session.activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x40)
                .findFirst()
                .orElseThrow();
        route.playerState.swapItemAWithSubscreen(0);
        assertEquals(PlayerState.INVENTORY_MAGIC_POWDER, route.playerState.itemA());
        route.session.setToadstoolPlayerState(true, 0);
        route.session.setEntityInventorySlots(route.playerState.itemA(), route.playerState.itemB());
        route.session.setEntityActionButtonsHeld(true, false);
        route.session.tickEntitiesWithProjectileEvents(route.frame++, route.witch.x(), 0x56, 0, 0, 1, false);
        route.session.tickEntitiesWithProjectileEvents(route.frame++, route.witch.x(), 0x56, 0, 0, 1, false);
        route.exchangeEvents = route.session.consumeWitchExchangeEvents();
        assertEquals(1, route.exchangeEvents.size());
        route.playerState.beginWitchToadstoolExchange(
                route.exchangeEvents.getFirst().inventorySlot());
        assertFalse(route.playerState.hasToadstool());
        assertEquals(PlayerState.INVENTORY_EMPTY, route.playerState.itemA());

        route.session.setEntityActionButtonsHeld(false, false);
        route.brewingDialog = false;
        while (!route.brewingDialog && route.frame < route.pickupFrame + 0xA00) {
            route.session.tickEntitiesWithProjectileEvents(route.frame++, route.witch.x(), 0x56, 0, 0, 1, false);
            route.brewingDialog |= route.session.consumeEntityDialogRequests().stream()
                    .anyMatch(request -> request.globalDialogId() == 0x009);
        }
        assertTrue(route.brewingDialog);
        route.session.setEntityDialogActive(true);
        route.session.tickEntitiesWithProjectileEvents(route.frame++, route.witch.x(), 0x56, 0, 0, 1, false);
        route.session.setEntityDialogActive(false);
        route.readyDialog = false;
        while (!route.readyDialog && route.frame < route.pickupFrame + 0xB00) {
            route.session.tickEntitiesWithProjectileEvents(route.frame++, route.witch.x(), 0x56, 0, 0, 1, false);
            route.readyDialog |= route.session.consumeEntityDialogRequests().stream()
                    .anyMatch(request -> request.globalDialogId() == 0x0FE);
        }
        assertTrue(route.readyDialog);
        route.session.setEntityDialogActive(true);
        route.session.tickEntitiesWithProjectileEvents(route.frame++, route.witch.x(), 0x56, 0, 0, 1, false);
        route.session.setEntityDialogActive(false);
        route.session.tickEntitiesWithProjectileEvents(route.frame++, route.witch.x(), 0x56, 0, 0, 1, false);
        assertEquals(1, route.session.consumeWitchRewardEvents().size());
        route.playerState.applyWitchMagicPowderReward();
        assertEquals(PlayerState.INVENTORY_MAGIC_POWDER, route.playerState.itemA());
        assertEquals(20, route.playerState.magicPowderCount());

        route.link.setPixelPosition(route.link.pixelX(), RoomConstants.ROOM_PIXEL_HEIGHT);
        route.coordinator.handleWarpAndIndoorBoundaries(route.link);
        while (route.transition.isActive()) route.transition.tick();
        assertEquals(Warp.CATEGORY_OVERWORLD, route.session.mapCategory());
        assertEquals(0x65, route.session.currentRoomId());

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
            assertEquals(route.raccoonRouteRooms[index], route.session.currentRoomId());
        }
        assertEquals(0x51, route.session.currentRoomId());
        assertTrue(route.session.activeRoom().entities().loadedEntities().stream()
                .anyMatch(entity -> entity.type() == 0x3F));

        route.session.setEntityInventorySlots(route.playerState.itemA(), route.playerState.itemB());
        route.session.setToadstoolPlayerState(false, route.playerState.magicPowderCount());
        route.session.tickEntitiesWithProjectileEvents(route.frame++, 0x6A, 0x40, 0, 0, Link.DIRECTION_RIGHT, false);
        // ROM direction zero adds $0E to Link's entity X, placing the
        // sprinkle at Tarin's live $78 coordinate.
        assertTrue(route.session.sprinkleMagicPowder(0x6A, 0x40, 0, 0));
        route.transformedDialogOpened = false;
        while ((!route.transformedDialogOpened || (route.session.overworldRoomStatusForTest(0x51) & 0x10) == 0)
                && route.frame < route.pickupFrame + 0xD00) {
            route.session.tickEntitiesWithProjectileEvents(
                    route.frame++, 0x6A, 0x40, 0, 0, Link.DIRECTION_RIGHT, false);
            route.transformedDialogOpened |= route.session.consumeEntityDialogRequests().stream()
                    .anyMatch(request -> request.globalDialogId() == 0x00A);
        }
        assertEquals(0x10, route.session.overworldRoomStatusForTest(0x51) & 0x10);
        assertTrue(route.transformedDialogOpened);
        assertTrue(
                route.session.activeRoom().entities().loadedEntities().stream()
                        .anyMatch(entity -> entity.type() == 0x3F),
                "the persisted flag must not truncate Tarin's live transformation");

        // Deliberately reload the room to prove the completed transformation
        // survives reconstruction before the persisted route continues.
        route.session.loadInitialOverworld(0x51);
        route.session.tickEntitiesWithProjectileEvents(route.frame, 0x6A, 0x40, 0, 0, Link.DIRECTION_RIGHT, false);
        assertFalse(route.session.activeRoom().entities().loadedEntities().stream()
                .anyMatch(entity -> entity.type() == 0x3F));

        walkToAndCrossOverworldBoundary(
                route.session, route.coordinator, route.scroll, route.collision, route.link, ScrollController.UP);
        assertEquals(0x41, route.session.currentRoomId());
        assertEquals(0x40, route.session.overworldRoomStatusForTest(0x41) & 0x40);
        assertTrue(route.session.consumeEntityEvents().stream()
                .anyMatch(event ->
                        event.soundChannel() == EntityCombatEvent.SoundChannel.JINGLE && event.soundId() == 0x02));

        // Exercise the same action/facing bridge that Main calls after input
        // has positioned Link at the chest.
        route.tailKeyChest =
                route.session.tryOpenChest(0x38, 0x31, Link.DIRECTION_UP, true, route.playerState.swordLevel());
        assertTrue(route.tailKeyChest.opened());
        assertEquals(ChestContentsTable.CHEST_TAIL_KEY, route.tailKeyChest.itemType());
        route.session.tickEntities(route.frame++, 0x38, 0x31);
        route.session
                .consumeChestRewardEvents()
                .forEach(reward -> route.playerState.applyChestReward(reward.itemType()));
        assertEquals(1, route.playerState.tailKeyCount());
    }
}
