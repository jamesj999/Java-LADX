package linksawakening.world.room;

import static linksawakening.world.room.RoomTransitionTestSupport.*;
import static linksawakening.world.room.RoomTransitionTestSupport2.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import linksawakening.entity.Link;
import linksawakening.equipment.Sword;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.transition.ScrollController;
import linksawakening.world.transition.Warp;

final class GameplayRouteStage4 {
    static void run(GameplayRouteState route) throws Exception {

        while (!route.transition.isActive() && route.slowWarpTicks < 0x204) {
            route.session.tickEntitiesWithProjectileEvents(route.frame++, 0x50, 0x60, 0, 0, Link.DIRECTION_DOWN, false);
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
        assertEquals(Warp.CATEGORY_OVERWORLD, route.session.mapCategory());
        assertEquals(0xD3, route.session.currentRoomId());

        walkToAndCrossOverworldBoundary(
                route.session, route.coordinator, route.scroll, route.collision, route.link, ScrollController.LEFT);
        assertEquals(0xD2, route.session.currentRoomId());
        assertTrue(route.session.activeRoom().entities().loadedEntities().stream()
                .anyMatch(entity -> entity.type() == 0x41));
        assertEquals(0x80, route.session.bowWowState());

        route.instrumentOwlDialogOpened = false;
        route.instrumentOwlDialogs = new java.util.ArrayList<Integer>();
        route.instrumentOwlDeadline = route.frame + 0x100;
        while (!route.instrumentOwlDialogOpened && route.frame < route.instrumentOwlDeadline) {
            tickInteractiveEntities(route.session, route.frame++, 0x50, 0x50);
            route.instrumentOwlDialogs.addAll(route.session.consumeEntityDialogRequests().stream()
                    .map(DialogRequest::globalDialogId)
                    .toList());
            route.instrumentOwlDialogOpened |= route.instrumentOwlDialogs.contains(0x0C2);
        }
        assertTrue(route.instrumentOwlDialogOpened, route.instrumentOwlDialogs.toString());
        route.instrumentOwlExitDeadline = route.frame + 0x100;
        while ((route.session.components.roomSessionViews().overworldRoomStatusForTest(0xD2) & 0x20) == 0
                && route.frame < route.instrumentOwlExitDeadline) {
            tickInteractiveEntities(route.session, route.frame++, 0x50, 0x50);
        }
        assertEquals(0x20, route.session.components.roomSessionViews().overworldRoomStatusForTest(0xD2) & 0x20);

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
                                + Integer.toHexString(route.session.currentRoomId()),
                        error);
            }
            assertEquals(route.kidnappingWarningRooms[index], route.session.currentRoomId());
        }
        route.warningKid = route.session.activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x71)
                .findFirst()
                .orElseThrow();
        route.session.tickEntities(route.frame++, 0x10, 0x10);
        route.session.tickEntities(route.frame++, 0x10, 0x10);
        assertEquals(0x0E, route.session.components.entityBridge().outbox.consumePendingMusicTrack());
        route.session.tickEntities(route.frame++, route.warningKid.x(), route.warningKid.y());
        assertEquals(
                0x220, route.session.consumeEntityDialogRequests().getFirst().globalDialogId());

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
                                + Integer.toHexString(route.session.currentRoomId()),
                        error);
            }
            assertEquals(route.moblinHideoutRouteRooms[index], route.session.currentRoomId());
        }
        route.moblinHideoutEntrance = route.session.activeRoom().warps().stream()
                .filter(warp -> warp.destMap() == 0x15 && warp.destRoom() == 0xF0)
                .findFirst()
                .orElseThrow();
        route.link.setPixelPosition(
                (route.moblinHideoutEntrance.tileLocation() & 0x0F) * 16,
                (route.moblinHideoutEntrance.tileLocation() >>> 4) * 16);
        walkIntoDoor(route.link, route.inputState, route.inputConfig, route.coordinator, route.transition);
        while (route.transition.isActive()) route.transition.tick();
        assertEquals(0x15, route.session.activeRoom().mapId());
        assertEquals(0xF0, route.session.currentRoomId());

        route.hideoutGuard = route.session.activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x14)
                .findFirst()
                .orElseThrow();
        route.hideoutGuardDialogOpened = false;
        route.guardIntroDeadline = route.frame + 0x100;
        while (!route.hideoutGuardDialogOpened && route.frame < route.guardIntroDeadline) {
            route.session.tickEntitiesWithProjectileEvents(
                    route.frame++, 0x70, route.hideoutGuard.y(), 0, 0, Link.DIRECTION_LEFT, false);
            route.hideoutGuardDialogOpened |= route.session.consumeEntityDialogRequests().stream()
                    .anyMatch(request -> request.globalDialogId() == 0x190);
        }
        assertTrue(route.hideoutGuardDialogOpened);
        for (int hit = 0; hit < 2; hit++) {
            route.hideoutGuard = route.session.activeRoom().entities().slots().get(route.hideoutGuard.slot());
            int hideoutGuardSlot = route.hideoutGuard.slot();
            List<EntityCombatEvent> guardHit = route.session.resolveEntityCombat(
                    hideoutGuardSlot ^ 1,
                    route.hideoutGuard.x(),
                    route.hideoutGuard.y(),
                    false,
                    true,
                    true,
                    route.hideoutGuard.x(),
                    0x08,
                    route.hideoutGuard.y(),
                    0x08);
            assertTrue(
                    guardHit.stream().anyMatch(event -> event.slot() == hideoutGuardSlot && event.enemyDamage() > 0));
            for (int recovery = 0; recovery < 0x20; recovery++) {
                route.session.tickEntitiesWithProjectileEvents(
                        route.frame++, 0x20, 0x70, 0, 0, Link.DIRECTION_RIGHT, false);
            }
        }
        route.guardDeadline = route.frame + 0x100;
        while (route.session.components.roomSessionViews().activeRoomEventForTest() != 0
                && route.frame < route.guardDeadline) {
            route.session.tickEntitiesWithProjectileEvents(
                    route.frame++, 0x20, 0x70, 0, 0, Link.DIRECTION_RIGHT, false);
        }
        assertEquals(0, route.session.components.roomSessionViews().activeRoomEventForTest());
        assertEquals(0, route.session.activeRoom().shutterDoorMask() & 0x01);

        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.UP);
        assertEquals(0xE0, route.session.currentRoomId());
        route.hideoutMoblinSlots = route.session.activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x0B)
                .map(RoomEntity::slot)
                .toList();
        assertEquals(4, route.hideoutMoblinSlots.size());
        route.session.tickEntitiesWithProjectileEvents(route.frame++, 0x50, 0x50, 0, 0, Link.DIRECTION_RIGHT, false);
        for (int moblinSlot : route.hideoutMoblinSlots) {
            for (int hit = 0;
                    hit < 4
                            && route.session
                                            .activeRoom()
                                            .entities()
                                            .slots()
                                            .get(moblinSlot)
                                            .status()
                                    == EntityStatus.ACTIVE;
                    hit++) {
                RoomEntity moblin =
                        route.session.activeRoom().entities().slots().get(moblinSlot);
                List<EntityCombatEvent> moblinHit = route.session.resolveEntityCombat(
                        moblinSlot ^ 1, moblin.x(), moblin.y(), false, true, true, moblin.x(), 0x08, moblin.y(), 0x08);
                assertTrue(moblinHit.stream().anyMatch(event -> event.slot() == moblinSlot && event.enemyDamage() > 0));
                for (int recovery = 0; recovery < 0x20; recovery++) {
                    route.session.tickEntitiesWithProjectileEvents(
                            route.frame++, 0x50, 0x50, 0, 0, Link.DIRECTION_RIGHT, false);
                }
            }
        }
        route.moblinRoomDeadline = route.frame + 0x200;
        while (route.session.components.roomSessionViews().activeRoomEventForTest() != 0
                && route.frame < route.moblinRoomDeadline) {
            route.session.tickEntitiesWithProjectileEvents(
                    route.frame++, 0x50, 0x50, 0, 0, Link.DIRECTION_RIGHT, false);
        }
        assertEquals(0, route.session.components.roomSessionViews().activeRoomEventForTest());
        assertEquals(0, route.session.activeRoom().shutterDoorMask() & 0x08);
        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.RIGHT);
        assertEquals(0xE1, route.session.currentRoomId());

        route.moblinKing = route.session.activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0xE4)
                .findFirst()
                .orElseThrow();
        route.moblinKingIntroOpened = false;
        route.moblinKingHits = 0;
        route.moblinKingDeadline = route.frame + 0x2000;
        while (route.session
                        .activeRoom()
                        .entities()
                        .slots()
                        .get(route.moblinKing.slot())
                        .loaded()
                && route.frame < route.moblinKingDeadline) {
            route.session.tickEntitiesWithProjectileEvents(
                    route.frame++, 0x00, 0x20, 0, 0, Link.DIRECTION_RIGHT, false);
            route.moblinKingIntroOpened |= route.session.consumeEntityDialogRequests().stream()
                    .anyMatch(request -> request.globalDialogId() == 0x191);
            route.moblinKing = route.session.activeRoom().entities().slots().get(route.moblinKing.slot());
            if (route.moblinKing.status() != EntityStatus.ACTIVE) {
                continue;
            }

            Sword liveSword = new Sword(route.romTables, null);
            liveSword.onPress();
            for (int swingFrame = 0; swingFrame < 4; swingFrame++) {
                liveSword.tick(false);
            }
            route.link.setDirection(Link.DIRECTION_RIGHT);
            Sword.CollisionBox initialSwordBox = liveSword.enemyCollisionBox(
                    route.link.romEntityX(), route.link.romSwordCollisionY(), route.link.direction());
            route.link.setPixelPosition(
                    route.moblinKing.x() - (initialSwordBox.x() - route.link.pixelX()),
                    route.moblinKing.y() - (initialSwordBox.y() - route.link.pixelY()));
            Sword.CollisionBox swordBox = liveSword.enemyCollisionBox(
                    route.link.romEntityX(), route.link.romSwordCollisionY(), route.link.direction());
            int moblinKingSlot = route.moblinKing.slot();
            List<EntityCombatEvent> kingHit = route.session.resolveEntityCombat(
                    moblinKingSlot ^ 1,
                    route.link.romEntityX(),
                    route.link.romEntityY(),
                    false,
                    true,
                    swordBox.active(),
                    swordBox.x(),
                    swordBox.width(),
                    swordBox.y(),
                    swordBox.height());
            if (kingHit.stream().anyMatch(event -> event.slot() == moblinKingSlot && event.enemyDamage() > 0)) {
                route.moblinKingHits++;
            }
        }
        assertTrue(route.moblinKingIntroOpened);
        assertEquals(8, route.moblinKingHits);
        assertFalse(route.session
                .activeRoom()
                .entities()
                .slots()
                .get(route.moblinKing.slot())
                .loaded());
        assertEquals(0x20, route.session.components.roomSessionViews().indoorRoomStatusForTest(0x15, 0xE1) & 0x20);

        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.RIGHT);
        assertEquals(0xE2, route.session.currentRoomId());
        route.kidnappedBowWow = route.session.activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x6D)
                .findFirst()
                .orElseThrow();
        route.bowWowRescueDeadline = route.frame + 0x40;
        while (route.session.bowWowState() != 0x01 && route.frame < route.bowWowRescueDeadline) {
            route.session.tickEntitiesWithProjectileEvents(
                    route.frame++,
                    route.kidnappedBowWow.x(),
                    route.kidnappedBowWow.y(),
                    0,
                    0,
                    Link.DIRECTION_RIGHT,
                    1,
                    false,
                    1,
                    0,
                    false,
                    0,
                    0,
                    0,
                    0);
        }
        assertEquals(0x01, route.session.bowWowState());
        assertEquals(0x10, route.session.components.entityBridge().outbox.consumePendingMusicTrack());
        assertTrue(route.session.consumeEntityDialogRequests().stream()
                .anyMatch(request -> request.globalDialogId() == 0x16C));

        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.LEFT);
        route.session.tickEntitiesWithProjectileEvents(
                route.frame, route.link.romEntityX(), route.link.romEntityY(), 0, 0, Link.DIRECTION_LEFT, false);
        assertTrue(route.session.activeRoom().entities().loadedEntities().stream()
                .anyMatch(entity -> entity.type() == 0x6D));

        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.LEFT);
        assertEquals(0xE0, route.session.currentRoomId());
        route.session.tickEntitiesWithProjectileEvents(
                route.frame++, route.link.romEntityX(), route.link.romEntityY(), 0, 0, Link.DIRECTION_DOWN, false);
        assertEquals(0, route.session.components.roomSessionViews().activeRoomEventForTest());
        assertEquals(0, route.session.activeRoom().shutterDoorMask() & 0x02);
        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.DOWN);
        assertEquals(0xF0, route.session.currentRoomId());
        route.hideoutExit = route.session.activeRoom().warps().stream()
                .filter(warp -> warp.destMap() == 0x00 && warp.destRoom() == 0x35)
                .findFirst()
                .orElseThrow();
        assertEquals(route.hideoutExit, route.session.activeRoom().firstWarp());
        // A room load reconstructs shutters; the ordinary frame resolves its
        // kill-all event before Link can leave through the south entrance.
        route.session.tickEntitiesWithProjectileEvents(
                route.frame++, route.link.romEntityX(), route.link.romEntityY(), 0, 0, Link.DIRECTION_DOWN, false);
        assertEquals(
                0,
                route.session.activeRoom().shutterDoorMask(),
                route.session.activeRoom().entities().loadedEntities().toString());
        assertTrue(route.session.activeRoom().indoorHasSouthEntrance());
        walkToAndExitIndoorFrontDoor(route.coordinator, route.transition, route.collision, route.link);
        assertEquals(Warp.CATEGORY_OVERWORLD, route.session.mapCategory());
        assertEquals(0x35, route.session.currentRoomId());

        walkToAndCrossOverworldBoundary(
                route.session, route.coordinator, route.scroll, route.collision, route.link, ScrollController.RIGHT);
        assertEquals(0x36, route.session.currentRoomId());
        assertTrue(route.session.activeRoom().entities().loadedEntities().stream()
                .anyMatch(entity -> entity.type() == 0x41));
        route.swampOwlDialogOpened = false;
        route.swampOwlDeadline = route.frame + 0x100;
        while (!route.swampOwlDialogOpened && route.frame < route.swampOwlDeadline) {
            tickInteractiveEntities(route.session, route.frame++, 0x50, 0x50);
            route.swampOwlDialogOpened |= route.session.consumeEntityDialogRequests().stream()
                    .anyMatch(request -> request.globalDialogId() == 0x0C3);
        }
        assertTrue(route.swampOwlDialogOpened);
        route.swampOwlExitDeadline = route.frame + 0x100;
        while ((route.session.components.roomSessionViews().overworldRoomStatusForTest(0x36) & 0x20) == 0
                && route.frame < route.swampOwlExitDeadline) {
            tickInteractiveEntities(route.session, route.frame++, 0x50, 0x50);
        }
        assertEquals(0x20, route.session.components.roomSessionViews().overworldRoomStatusForTest(0x36) & 0x20);
        route.swampOwlDepartureDeadline = route.frame + 0x100;
        while (route.session.activeRoom().entities().loadedEntities().stream().anyMatch(entity -> entity.type() == 0x41)
                && route.frame < route.swampOwlDepartureDeadline) {
            tickInteractiveEntities(route.session, route.frame++, 0x50, 0x50);
        }
        assertFalse(route.session.activeRoom().entities().loadedEntities().stream()
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
            assertEquals(route.gopongaSwampRouteRooms[index], route.session.currentRoomId());
        }
        assertEquals(
                2,
                route.session.activeRoom().entities().loadedEntities().stream()
                        .filter(entity -> entity.type() == 0x7C || entity.type() == 0x7E)
                        .count());
        route.frame = letBowWowEatAllGoponga(route.session, route.collision, route.link, route.frame);

        walkToAndCrossOverworldBoundary(
                route.session, route.coordinator, route.scroll, route.collision, route.link, ScrollController.UP);
        assertEquals(0x23, route.session.currentRoomId());
        assertEquals(
                1,
                route.session.activeRoom().entities().loadedEntities().stream()
                        .filter(entity -> entity.type() == 0x7C)
                        .count());
        route.frame = letBowWowEatAllGoponga(route.session, route.collision, route.link, route.frame);

        walkToAndCrossOverworldBoundary(
                route.session, route.coordinator, route.scroll, route.collision, route.link, ScrollController.DOWN);
        assertEquals(0x33, route.session.currentRoomId());
        assertFalse(route.session.activeRoom().entities().loadedEntities().stream()
                .anyMatch(entity -> entity.type() == 0x7C || entity.type() == 0x7E));
        walkToAndCrossOverworldBoundary(
                route.session, route.coordinator, route.scroll, route.collision, route.link, ScrollController.UP);
        assertEquals(0x23, route.session.currentRoomId());
        assertFalse(route.session.activeRoom().entities().loadedEntities().stream()
                .anyMatch(entity -> entity.type() == 0x7C));

        walkToAndCrossOverworldBoundary(
                route.session, route.coordinator, route.scroll, route.collision, route.link, ScrollController.LEFT);
        assertEquals(0x22, route.session.currentRoomId());
        assertEquals(
                2,
                route.session.activeRoom().entities().loadedEntities().stream()
                        .filter(entity -> entity.type() == 0x7E)
                        .count());
        route.frame = letBowWowEatAllGoponga(route.session, route.collision, route.link, route.frame);
        walkToAndCrossOverworldBoundary(
                route.session, route.coordinator, route.scroll, route.collision, route.link, ScrollController.RIGHT);
        assertEquals(0x23, route.session.currentRoomId());
        assertFalse(route.session.activeRoom().entities().loadedEntities().stream()
                .anyMatch(entity -> entity.type() == 0x7C));
    }
}
