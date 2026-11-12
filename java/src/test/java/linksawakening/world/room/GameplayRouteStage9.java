package linksawakening.world.room;

import static linksawakening.world.room.RoomTransitionTestSupport.*;
import static linksawakening.world.room.RoomTransitionTestSupport2.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;

import java.util.List;
import linksawakening.equipment.Sword;
import linksawakening.rom.Rom;
import linksawakening.state.Direction;
import linksawakening.testing.TestRoms;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.motion.BooBuddyMotion;
import linksawakening.world.entity.motion.HinoxMotion;
import linksawakening.world.entity.motion.MotionTestAccess;
import linksawakening.world.transition.ScrollController;
import linksawakening.world.transition.Warp;

final class GameplayRouteStage9 {
    static void run(GameplayRouteState route) throws Exception {

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
        route.hinox = route.session.rooms().activeRoom().entities().slots().get(route.hinoxSlot);
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
            route.hinoxBombResponseObserved = route.session.rooms().activeRoom().entities().loadedEntities().stream()
                    .anyMatch(entity -> entity.type() == MotionTestAccess.HinoxMotion_ENTITY_BOMB);
        }
        assertTrue(route.hinoxBombResponseObserved, "a live sword flash must enter Hinox's bomb response");

        // Continue using the ordinary sword collision/death path. The test
        // never writes enemy health or replaces an entity slot.
        route.hinoxDeathStarted = false;
        route.deathDeadline = route.frame + 0x1000;
        while (route.frame < route.deathDeadline) {
            RoomEntity liveHinox =
                    route.session.rooms().activeRoom().entities().slots().get(route.hinoxSlot);
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
                        .activeRoom()
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

        route.clearedWarp = route.session.rooms().activeRoom().entities().loadedEntities().stream()
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
        route.room36Warp = route.session.rooms().activeRoom().entities().loadedEntities().stream()
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
                route.session.rooms().activeRoom().entities().slots().get(route.room36WarpSlot);
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
                route.session.rooms().activeRoom().entities().loadedEntities().stream()
                        .filter(entity -> entity.type() == 0x50)
                        .count());
        assertTrue(route.session.rooms().activeRoom().entities().loadedEntities().stream()
                .anyMatch(entity -> entity.type() == 0x50 && entity.x() == 0x48 && entity.y() == 0x30));
        assertTrue(route.session.rooms().activeRoom().entities().loadedEntities().stream()
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

        route.booSlots = route.session.rooms().activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == BooBuddyMotion.ENTITY_TYPE)
                .map(RoomEntity::slot)
                .toList();
        assertEquals(2, route.booSlots.size());
    }
}
