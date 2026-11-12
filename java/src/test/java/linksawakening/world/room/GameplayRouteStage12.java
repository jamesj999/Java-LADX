package linksawakening.world.room;

import static linksawakening.world.room.RoomTransitionTestSupport.*;
import static linksawakening.world.room.RoomTransitionTestSupport2.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.entity.Link;
import linksawakening.world.entity.EntityRoomLoader;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityRuntime;
import linksawakening.world.entity.RoomEntitySnapshot;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.transition.ScrollController;
import linksawakening.world.transition.Warp;

final class GameplayRouteStage12 {
    static void run(GameplayRouteState route) throws Exception {

        route.riddenPlatformY = route.session
                .activeRoom()
                .entities()
                .slots()
                .get(route.room3BPlatform.slot())
                .y();
        assertTrue(
                route.riddenPlatformY > route.firstPlatformY, "A5 platform did not move while Link was standing on it");

        // Contact the first authored D6 through the same request/update path,
        // then let its countdown settle before throwing it rightward.
        route.firstPot = route.session
                .activeRoom()
                .entities()
                .slots()
                .get(route.room3BPots.getFirst().slot());
        route.link.clearSideViewPlatformState();
        // The preceding live Link ticks may have left the attack-step timer
        // nonzero; ReturnIfNonInteractive_19's pickup path requires zero.
        route.link.clearRomAttackStepAnimationCountdown();
        route.session.setEntityAttackStepAnimationCountdown(0);
        // Keep the first pot in its raw -$12..+$11 lift window while placing
        // Link just outside the second pot's descending-slot window.
        route.link.setPixelPosition(route.firstPot.x() - 0x0B, route.firstPot.y() - 0x10);
        // EntityGetLiftedUp prioritizes the B inventory slot. A held alone
        // must not lift when B contains the bracelet but is not held.
        route.session.setEntityActionButtonsHeld(true, false);
        route.link.clearRomAttackStepAnimationCountdown();
        route.session.setEntityAttackStepAnimationCountdown(0);
        tickLiveRoom3B(route.session, route.link, route.coordinator, route.transition, route.scroll, route.frame++);
        assertEquals(
                EntityStatus.ACTIVE,
                route.session
                        .activeRoom()
                        .entities()
                        .slots()
                        .get(route.firstPot.slot())
                        .status());

        // The same contact with B held is the successful exact-priority path.
        route.session.setEntityActionButtonsHeld(false, true);
        route.link.setPixelPosition(route.firstPot.x() - 0x0B, route.firstPot.y() - 0x10);
        route.potPickupEvents = tickLiveRoom3B(
                route.session, route.link, route.coordinator, route.transition, route.scroll, route.frame++);
        assertEquals(
                EntityStatus.LIFTED,
                route.session
                        .activeRoom()
                        .entities()
                        .slots()
                        .get(route.firstPot.slot())
                        .status(),
                String.format(
                        "pot=%02X/%02X link=%02X/%02X",
                        route.firstPot.x(), route.firstPot.y(), route.link.romEntityX(), route.link.romEntityY()));
        assertTrue(route.session.liftedEntityState().active());
        assertTrue(route.potPickupEvents.stream()
                .anyMatch(event ->
                        event.soundChannel() == EntityCombatEvent.SoundChannel.WAVE && event.soundId() == 0x02));

        for (int liftFrame = 0;
                liftFrame < 0x20 && route.session.liftedEntityState().carryState() != 0x01;
                liftFrame++) {
            tickLiveRoom3B(route.session, route.link, route.coordinator, route.transition, route.scroll, route.frame++);
        }
        assertEquals(0x01, route.session.liftedEntityState().carryState());
        assertTrue(route.session.throwLiftedEntity(Link.DIRECTION_RIGHT));
        assertEquals(
                EntityStatus.ACTIVE,
                route.session
                        .activeRoom()
                        .entities()
                        .slots()
                        .get(route.firstPot.slot())
                        .status());
        route.throwEvents = tickLiveRoom3B(
                route.session, route.link, route.coordinator, route.transition, route.scroll, route.frame++);
        route.thrownPot = route.session.activeRoom().entities().slots().get(route.firstPot.slot());
        assertEquals(EntityStatus.ACTIVE, route.thrownPot.status());
        assertTrue(route.thrownPot.x() != route.firstPot.x() || route.thrownPot.y() != route.firstPot.y());
        assertTrue(route.throwEvents.stream()
                .anyMatch(event ->
                        event.soundChannel() == EntityCombatEvent.SoundChannel.JINGLE && event.soundId() == 0x08));
        assertEquals(0x0C, route.link.romAttackStepAnimationCountdown());

        route.room3BExit = route.session.activeRoom().warps().stream()
                .filter(warp -> warp.destMap() == 0x01
                        && warp.destRoom() == 0x25
                        && warp.destX() == 0x88
                        && warp.destY() == 0x20)
                .findFirst()
                .orElseThrow();
        assertEquals(-1, route.room3BExit.tileLocation());
        route.link.setPixelPosition(0x80, -5);
        route.coordinator.handleWarpAndIndoorBoundaries(route.link);
        assertTrue(route.transition.isActive());
        while (route.transition.isActive()) {
            route.transition.tick();
        }
        assertEquals(Warp.CATEGORY_INDOOR, route.session.mapCategory());
        assertEquals(0x01, route.session.activeRoom().mapId());
        assertEquals(0x25, route.session.currentRoomId());
        assertEquals(0x88, route.link.romEntityX());
        assertEquals(0x20, route.link.romEntityY());

        // The authored return path re-enters side-view room $3B, crosses left
        // into $3A, then uses $3A's E1 warp to room $2D. This bypasses room
        // $2A's instrument/shutter path and preserves the Nightmare Key until
        // the north boss door in $2D is opened.
        assertEquals(1, route.session.currentDungeonItemFlagsSnapshot()[DungeonItemState.NIGHTMARE_KEY_INDEX]);
        route.room25SideViewEntry = route.session.activeRoom().warps().stream()
                .filter(warp -> warp.destMap() == 0x01
                        && warp.destRoom() == 0x3B
                        && warp.destX() == 0x88
                        && warp.destY() == 0x10)
                .findFirst()
                .orElseThrow();
        assertEquals(-1, route.room25SideViewEntry.tileLocation());
        route.link.setPixelPosition(0x80, 0x10);
        route.coordinator.handleWarpAndIndoorBoundaries(route.link);
        assertFalse(route.transition.isActive());
        route.link.setPixelPosition(0x90, 0x10);
        route.coordinator.handleWarpAndIndoorBoundaries(route.link);
        assertFalse(route.transition.isActive());
        route.link.setPixelPosition(0x80, 0x10);
        route.coordinator.handleWarpAndIndoorBoundaries(route.link);
        assertTrue(route.transition.isActive());
        while (route.transition.isActive()) {
            route.transition.tick();
        }
        assertEquals(Warp.CATEGORY_SIDESCROLL, route.session.mapCategory());
        assertEquals(0x3B, route.session.currentRoomId());
        assertEquals(0x88, route.link.romEntityX());
        assertEquals(0x10, route.link.romEntityY());

        tickLiveRoom3B(route.session, route.link, route.coordinator, route.transition, route.scroll, route.frame++);
        route.link.setPixelPosition(-5, 0x30);
        route.coordinator.handleWarpAndIndoorBoundaries(route.link);
        assertTrue(route.scroll.isActive());
        assertEquals(0x3A, route.session.currentRoomId());
        while (route.scroll.isActive()) {
            route.scroll.tick(8);
        }
        assertEquals(Warp.CATEGORY_SIDESCROLL, route.session.mapCategory());
        assertEquals(0x3A, route.session.currentRoomId());
        assertTrue(route.session.currentRoomId() != 0x2A);

        route.room3AWarpTo2D = route.session.activeRoom().warps().stream()
                .filter(warp -> warp.destMap() == 0x01
                        && warp.destRoom() == 0x2D
                        && warp.destX() == 0x88
                        && warp.destY() == 0x20)
                .findFirst()
                .orElseThrow();
        assertEquals(-1, route.room3AWarpTo2D.tileLocation());
        route.link.setPixelPosition(0x80, -5);
        route.coordinator.handleWarpAndIndoorBoundaries(route.link);
        assertTrue(route.transition.isActive());
        while (route.transition.isActive()) {
            route.transition.tick();
        }
        assertEquals(Warp.CATEGORY_INDOOR, route.session.mapCategory());
        assertEquals(0x2D, route.session.currentRoomId());
        assertEquals(0x88, route.link.romEntityX());
        assertEquals(0x20, route.link.romEntityY());
        assertTrue(route.session.currentRoomId() != 0x2A);
        assertEquals(1, route.session.currentDungeonItemFlagsSnapshot()[DungeonItemState.NIGHTMARE_KEY_INDEX]);

        // Source-shaped north boss-door contact: the C4/$14 and C4/$15
        // records expand to the top-facing A4/A5 pair at room cells $14/$15.
        // Link's leading-edge probes therefore use the corresponding Java
        // top-left position (x=$48, y=$FA/-6), aligned with the door's
        // eight-pixel opening window; probes land on x=$4E/$51,y=$00.
        route.link.setPixelPosition(0x48, -6);
        route.link.setDirection(Link.DIRECTION_UP);
        assertEquals(0x48, route.link.pixelX());
        assertEquals(-6, route.link.pixelY());
        assertTrue(
                route.session.tryUnlockIndoorKeyDoor(route.link.pixelX(), route.link.pixelY(), Link.DIRECTION_UP, 0x01),
                "room2D boss-door leading-edge probes did not contact A4/A5");
        // Source bank2 checks wHasDungeonBossKey but does not decrement it
        // (only the $90-$93 small-key branch consumes a key).
        assertEquals(1, route.session.currentDungeonItemFlagsSnapshot()[DungeonItemState.NIGHTMARE_KEY_INDEX]);
        for (int tick = 0; tick < 8; tick++) {
            route.session.tickEntities(route.frame++, route.link.pixelX(), route.link.pixelY());
            assertTrue(route.session.components.entityBridge().consumeWorldLinkMotionBlockRequest());
        }
        route.session.tickEntities(route.frame++, route.link.pixelX(), route.link.pixelY());
        assertFalse(route.session.components.entityBridge().consumeWorldLinkMotionBlockRequest());
        assertEquals(0x43, objectAt(route.session, 0x04));
        assertEquals(0x44, objectAt(route.session, 0x05));
        assertEquals(0x04, route.session.components.roomSessionViews().indoorRoomStatusForTest(0x01, 0x2D) & 0x04);

        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.UP);
        assertEquals(0x2B, route.session.currentRoomId());
        assertTrue(route.session.currentRoomId() != 0x2A);
        route.genieEntities = route.session.activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x5C)
                .toList();
        assertEquals(1, route.genieEntities.size());
        route.genie = route.genieEntities.getFirst();
        assertEquals(0x48, route.genie.x());
        assertEquals(0x30, route.genie.y());
        assertTrue((route.romTables.entityOptions1(0x5C) & 0x80) != 0);

        // Once Genie is gone, room $2B's event $21 opens the west shutter.
        // Crossing that authored boundary reaches the instrument room $2A;
        // it is never a pre-boss shortcut.
        route.clearedBossSlots = new ArrayList<>();
        for (int slot = 0; slot < EntityRoomLoader.MAX_ENTITIES; slot++) {
            route.clearedBossSlots.add(RoomEntity.disabled(slot));
        }
        route.session
                .components
                .roomSessionViews()
                .replaceEntityRuntimeForTest(
                        RoomEntityRuntime.from(new RoomEntitySnapshot(route.clearedBossSlots), true));
        route.session.tickEntities(route.frame++, route.link.pixelX(), route.link.pixelY());
        assertEquals(0, route.session.components.roomSessionViews().activeRoomEventForTest());
        route.link.setPixelPosition(0x50, 0x50);
        walkToAndCrossIndoorBoundary(
                route.coordinator, route.transition, route.scroll, route.collision, route.link, ScrollController.LEFT);
        assertEquals(0x2A, route.session.currentRoomId());
        assertTrue(route.session.activeRoom().entities().loadedEntities().stream()
                .anyMatch(entity -> entity.type() == EntitySpriteHandlerCatalog.ENTITY_INSTRUMENT_OF_THE_SIRENS));
    }
}
