package linksawakening.world.room;

import static linksawakening.world.room.RoomConstants.ROOM_OBJECTS_BASE;
import static linksawakening.world.room.RoomConstants.ROOM_OBJECT_ROW_STRIDE;
import static linksawakening.world.room.RoomNavigationFixture.OBJECT_NORMAL_PIT;
import static linksawakening.world.room.RoomNavigationFixture.PowderApproach;
import static linksawakening.world.room.RoomNavigationFixture.SwordApproach;
import static linksawakening.world.room.RoomNavigationFixture.bowWowOutsideLeash;
import static linksawakening.world.room.RoomNavigationFixture.collisionGrid;
import static linksawakening.world.room.RoomNavigationFixture.crossOverworldBoundary;
import static linksawakening.world.room.RoomNavigationFixture.isTargetBoundary;
import static linksawakening.world.room.RoomNavigationFixture.leadingEdgeBlocked;
import static linksawakening.world.room.RoomNavigationFixture.objectAt;
import static linksawakening.world.room.RoomNavigationFixture.reachableBoundaryPath;
import static linksawakening.world.room.RoomNavigationFixture.reachableBoundaryPathWithFeather;
import static linksawakening.world.room.RoomNavigationFixture.reachableBoundaryPosition;
import static linksawakening.world.room.RoomNavigationFixture.reachableEntityWindowPath;
import static linksawakening.world.room.RoomNavigationFixture.reachableFollowerLeashPath;
import static linksawakening.world.room.RoomNavigationFixture.reachablePositionPath;
import static linksawakening.world.room.RoomNavigationFixture.reachablePositionPathWithFeather;
import static linksawakening.world.room.RoomNavigationFixture.setDirectionalInput;
import static linksawakening.world.room.RoomNavigationFixture.signedByteDelta;
import static linksawakening.world.room.RoomNavigationFixture.tickInteractiveEntities;
import static linksawakening.world.room.RoomNavigationFixture.walkCollisionPathWithFeather;
import static linksawakening.world.room.RoomNavigationFixture.walkToAndCrossOverworldBoundary;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.List;
import java.util.function.BiPredicate;
import linksawakening.entity.Link;
import linksawakening.equipment.ItemRegistry;
import linksawakening.equipment.Sword;
import linksawakening.gpu.GPU;
import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
import linksawakening.physics.OverworldCollision;
import linksawakening.rom.Rom;
import linksawakening.rom.RomTables;
import linksawakening.state.Direction;
import linksawakening.state.PlayerState;
import linksawakening.testing.TestRoms;
import linksawakening.vfx.TransientVfxSystem;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.LiftedEntityState;
import linksawakening.world.entity.event.LinkAttackClearRequest;
import linksawakening.world.entity.event.LinkAttackStepRequest;
import linksawakening.world.entity.event.LinkFinalPositionRequest;
import linksawakening.world.entity.event.LinkMotionBlockRequest;
import linksawakening.world.entity.event.SideViewPlatformLinkRequest;
import linksawakening.world.entity.event.SideViewPotLinkRequest;
import linksawakening.world.transition.ScrollController;
import linksawakening.world.transition.TransitionController;
import linksawakening.world.transition.Warp;

final class RoomNavigationFixture {
    static final int MAP_WITH_NO_WARP_ROOM = 0x01;

    static final int INDOOR_ROOM_WITH_NO_WARPS = 0x00;

    static final int OBJECT_NORMAL_PIT = 0xE8;

    static void walkIntoDoor(
            Link link,
            InputState input,
            InputConfig config,
            RoomTransitionCoordinator coordinator,
            TransitionController transition) {
        input.onKeyEvent(config.upKey(), GLFW_PRESS);
        for (int tick = 0; tick < 16 && !transition.isActive(); tick++) {
            link.motion().update();
            coordinator.handleWarpAndIndoorBoundaries(link);
        }
        input.onKeyEvent(config.upKey(), GLFW_RELEASE);
        assertTrue(transition.isActive(), "moving collision probe must enter the doorway");
    }

    static RoomSession newSession(byte[] rom, RomTables romTables, OverworldCollision collision) {
        return new RoomSession(new RoomSessionConfig(
                (rom == null ? (Rom) null : TestRoms.partial(rom)),
                new GPU(),
                new RoomLoader((rom == null ? (Rom) null : TestRoms.partial(rom))),
                new OverworldTilesetTable((rom == null ? (Rom) null : TestRoms.partial(rom))),
                collision,
                new RoomSessionConfig.Presentation(new TransientVfxSystem(16), null, null),
                new linksawakening.world.entity.RomRandomByteSource()));
    }

    static void tickInteractiveEntities(RoomSession session, int frame, int linkEntityX, int linkEntityY) {
        session.entities()
                .tickEntitiesWithProjectileEvents(
                        frame, linkEntityX, linkEntityY, 0, 0, Direction.DOWN.javaIndex(), false);
    }

    static List<EntityCombatEvent> tickLiveRoom3B(
            RoomSession session,
            Link link,
            RoomTransitionCoordinator coordinator,
            TransitionController transition,
            ScrollController scroll,
            int frame) {
        link.position().captureRomFinalPosition();
        link.motion().update();
        session.entityState()
                .setEntityAttackStepAnimationCountdown(link.equipment().romAttackStepAnimationCountdown());
        session.setEntityFinalPositionX(link.position().romFinalPositionX());
        RoomEntityTickBuilder.frame(frame)
                .position(
                        link.position().romEntityX(),
                        link.position().romEntityY(),
                        link.animation().romEntityZ())
                .motion(
                        link.airborne().isAirborne(),
                        link.airborne().romMotionState(),
                        link.animation().direction(),
                        link.position().romCollisionType())
                .shield(link.equipment().isUsingShield(), 1, 0)
                .sword(false, 0, 0, 0, 0)
                .speed(link.position().romSpeedX(), link.position().romSpeedY())
                .tick(session);
        // Main clears this per-frame flag after AnimateEntities and before
        // applying the current descending-slot platform requests.
        link.sideView().clearStandingOnSideViewEntity();

        List<SideViewPlatformLinkRequest> platforms = session.consumeSideViewPlatformLinkRequests();
        List<SideViewPotLinkRequest> pots = session.consumeSideViewPotLinkRequests();
        List<LinkFinalPositionRequest> finalPositions = session.consumeLinkFinalPositionRequests();
        int platformIndex = 0;
        int potIndex = 0;
        int finalPositionIndex = 0;
        while (platformIndex < platforms.size()
                || potIndex < pots.size()
                || finalPositionIndex < finalPositions.size()) {
            int stream = -1;
            int nextSlot = -1;
            if (platformIndex < platforms.size() && platforms.get(platformIndex).sourceSlot() > nextSlot) {
                stream = 0;
                nextSlot = platforms.get(platformIndex).sourceSlot();
            }
            if (finalPositionIndex < finalPositions.size()
                    && finalPositions.get(finalPositionIndex).sourceSlot() > nextSlot) {
                stream = 1;
                nextSlot = finalPositions.get(finalPositionIndex).sourceSlot();
            }
            if (potIndex < pots.size() && pots.get(potIndex).sourceSlot() >= nextSlot) {
                stream = 2;
            }
            if (stream == 0) {
                SideViewPlatformLinkRequest request = platforms.get(platformIndex++);
                if (request.standing()) {
                    link.sideView()
                            .applySideViewPlatformContact(
                                    request.horizontalDelta(), request.positionY(), request.speedY(), true);
                    coordinator.handleWarpAndIndoorBoundaries(link);
                    coordinator.handleOverworldBoundary(link);
                    if (scroll.isActive() || transition.isInputBlocked()) {
                        link.sideView().clearSideViewPlatformState();
                    }
                }
            } else if (stream == 1) {
                LinkFinalPositionRequest request = finalPositions.get(finalPositionIndex++);
                link.position().restoreRomFinalPosition();
                if (request.clearLinkPositionIncrement()) {
                    link.position().clearRomPositionIncrement();
                }
                if (request.markLinkPushing()) {
                    link.equipment().markRomLinkPushing(0x03);
                }
                if (request.resetPegasusBoots()) {
                    link.motion().resetPegasusBoots();
                }
            } else {
                SideViewPotLinkRequest request = pots.get(potIndex++);
                if (request.restoreFinalPositionX()) {
                    link.position().restoreRomFinalPositionX();
                }
                if (request.resetPegasusBoots()) {
                    link.motion().resetPegasusBoots();
                }
                if (request.speedX() != 0) {
                    link.position().applyRomSpeedX(request.speedX());
                }
                if (request.ignoreCollisionCountdown() != 0) {
                    link.status().setCollisionIgnoreFrames(request.ignoreCollisionCountdown());
                }
                if (request.snapTop()) {
                    link.sideView().applySideViewPotContact(request.positionY(), request.speedY());
                }
            }
        }
        for (LinkMotionBlockRequest request : session.consumeLinkMotionBlockRequests()) {
            link.motion().blockNextRomMotionFrame();
        }
        // These Main streams are not emitted by the authored A5/D6 route;
        // fail loudly rather than silently dropping a new request type.
        assertTrue(session.consumeWarpLinkStateRequests().isEmpty());
        assertFalse(session.components.entityBridge().consumeWorldLinkMotionBlockRequest());
        assertTrue(session.consumeLinkFallenPoseRequests().isEmpty());
        assertTrue(session.consumeLinkFacingRequests().isEmpty());
        List<LinkAttackClearRequest> attackClears = session.entityState().consumeLinkAttackClearRequests();
        List<LinkAttackStepRequest> attackSteps = session.consumeLinkAttackStepRequests();
        int attackClearIndex = 0;
        int attackStepIndex = 0;
        while (attackClearIndex < attackClears.size() || attackStepIndex < attackSteps.size()) {
            boolean applyClear = attackStepIndex >= attackSteps.size()
                    || (attackClearIndex < attackClears.size()
                            && attackClears.get(attackClearIndex).sourceSlot()
                                    > attackSteps.get(attackStepIndex).sourceSlot());
            if (applyClear) {
                attackClearIndex++;
                link.equipment().clearRomAttackStepAnimationCountdown();
            } else {
                // A same-slot tie is not emitted by the current handlers;
                // preserve Main's throw-step-wins ordering if one appears.
                attackStepIndex++;
                link.equipment().startRomItemAttackStep();
            }
        }
        assertTrue(session.consumeLinkHeldItemPoseRequests().isEmpty());
        assertTrue(session.consumeLinkSwordSpinPoseRequests().isEmpty());
        assertTrue(session.consumeLinkSwordFinalPoseRequests().isEmpty());
        LiftedEntityState lifted = session.rooms().liftedEntityState();
        if (lifted.active()) {
            link.actions().setCarryingLiftedObjectState(lifted.carryState(), lifted.effectiveRomDirection());
        } else {
            link.actions()
                    .setCarryingLiftedObjectState(
                            0,
                            switch (Direction.fromJavaOrUnknown(link.animation().direction())) {
                                case RIGHT -> 0;
                                case LEFT -> 1;
                                case UP -> 2;
                                case DOWN -> 3;
                                default -> throw new AssertionError(
                                        link.animation().direction());
                            });
        }
        return session.consumeEntityEvents();
    }

    static int letBowWowEatAllGoponga(RoomSession session, OverworldCollision collision, Link link, int frame) {
        int deadline = frame + 0x4000;
        while (session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                        .anyMatch(entity -> entity.type() == 0x7C || entity.type() == 0x7E)
                && frame < deadline) {
            RoomEntity target = session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                    .filter(entity -> entity.type() == 0x7C || entity.type() == 0x7E)
                    .findFirst()
                    .orElseThrow();
            RoomEntity bowWow = session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                    .filter(entity -> entity.type() == 0x6D)
                    .findFirst()
                    .orElseThrow();
            List<int[]> leashPath = reachableFollowerLeashPath(
                    collision, link.position().pixelX(), link.position().pixelY(), bowWow);
            // BowWow can temporarily be outside Link's reachable approach
            // window. Run the real follower handler until Link can approach;
            // an immediate path depends on earlier movement and RNG timing.
            while (leashPath == null && frame < deadline) {
                tickInteractiveEntities(
                        session,
                        frame++,
                        link.position().romEntityX(),
                        link.position().romEntityY());
                session.consumeEntityEvents();
                bowWow = session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                        .filter(entity -> entity.type() == 0x6D)
                        .findFirst()
                        .orElseThrow();
                leashPath = reachableFollowerLeashPath(
                        collision, link.position().pixelX(), link.position().pixelY(), bowWow);
            }
            assertNotNull(
                    leashPath, "No collision-valid Link path back into BowWow's leash\n" + collisionGrid(collision));
            for (int[] position : leashPath) {
                link.position().setPixelPosition(position[0], position[1]);
            }
            tickInteractiveEntities(
                    session,
                    frame++,
                    link.position().romEntityX(),
                    link.position().romEntityY());
            session.consumeEntityEvents();
            List<int[]> path = reachableEntityWindowPath(
                    collision, link.position().pixelX(), link.position().pixelY(), target);
            assertNotNull(
                    path,
                    "No collision-valid Link path into BowWow's target window for "
                            + String.format("%02X@(%02X,%02X)\n", target.type(), target.x(), target.y())
                            + collisionGrid(collision));
            for (int[] position : path) {
                link.position().setPixelPosition(position[0], position[1]);
                tickInteractiveEntities(
                        session,
                        frame++,
                        link.position().romEntityX(),
                        link.position().romEntityY());
                session.consumeEntityEvents();
                while (bowWowOutsideLeash(session, link) && frame < deadline) {
                    tickInteractiveEntities(
                            session,
                            frame++,
                            link.position().romEntityX(),
                            link.position().romEntityY());
                    session.consumeEntityEvents();
                }
            }
            while (session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                            .anyMatch(entity ->
                                    entity.slot() == target.slot() && (entity.type() == 0x7C || entity.type() == 0x7E))
                    && frame < deadline) {
                tickInteractiveEntities(
                        session,
                        frame++,
                        link.position().romEntityX(),
                        link.position().romEntityY());
                session.consumeEntityEvents();
            }
        }
        assertFalse(
                session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                        .anyMatch(entity -> entity.type() == 0x7C || entity.type() == 0x7E),
                session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                        .map(entity -> String.format(
                                "slot%d:%02X@(%02X,%02X,z=%02X,v=%d,s=%s)",
                                entity.slot(),
                                entity.type(),
                                entity.x(),
                                entity.y(),
                                entity.z(),
                                entity.spriteVariant(),
                                entity.status()))
                        .toList()
                        .toString());
        return frame;
    }

    static int signedByteDelta(int target, int source) {
        int delta = (target - source) & 0xFF;
        return delta < 0x80 ? delta : delta - 0x100;
    }

    static int objectAt(RoomSession session, int location) {
        return session.rooms()
                .activeRoomOrNull()
                .roomObjectsArea()[ROOM_OBJECTS_BASE + (location & 0xF0) + (location & 0x0F)];
    }

    static int firstObjectLocation(RoomSession session, int objectId) {
        for (int row = 0; row < RoomConstants.OBJECTS_PER_COLUMN; row++) {
            for (int column = 0; column < RoomConstants.OBJECTS_PER_ROW; column++) {
                int location = (row << 4) | column;
                if (objectAt(session, location) == objectId) {
                    return location;
                }
            }
        }
        return -1;
    }

    static int[] roomObjectTiles(RoomSession session, int location) {
        int tileX = (location & 0x0F) * 2;
        int tileY = ((location & 0xF0) >>> 4) * 2;
        int[] tiles = session.rooms().activeRoomOrNull().tileIds();
        int rowStride = RoomConstants.ROOM_TILE_WIDTH;
        return new int[] {
            tiles[tileY * rowStride + tileX],
            tiles[tileY * rowStride + tileX + 1],
            tiles[(tileY + 1) * rowStride + tileX],
            tiles[(tileY + 1) * rowStride + tileX + 1]
        };
    }

    static boolean bowWowOutsideLeash(RoomSession session, Link link) {
        RoomEntity bowWow = session.rooms().activeRoomOrNull().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x6D)
                .findFirst()
                .orElseThrow();
        return Math.abs(signedByteDelta(link.position().romEntityX(), bowWow.x())) > 0x18
                || Math.abs(signedByteDelta(link.position().romEntityY(), bowWow.y())) > 0x18;
    }

    static void crossOverworldBoundary(
            RoomTransitionCoordinator coordinator, ScrollController scroll, Link link, int direction) {
        switch (direction) {
            case ScrollController.UP -> link.position()
                    .setPixelPosition(link.position().pixelX(), -5);
            case ScrollController.DOWN -> link.position()
                    .setPixelPosition(link.position().pixelX(), 116);
            case ScrollController.LEFT -> link.position()
                    .setPixelPosition(-5, link.position().pixelY());
            case ScrollController.RIGHT -> link.position()
                    .setPixelPosition(148, link.position().pixelY());
            default -> throw new IllegalArgumentException("Unknown scroll direction: " + direction);
        }
        coordinator.handleOverworldBoundary(link);
        while (scroll.isActive()) {
            scroll.tick(8);
        }
    }

    static void walkToAndCrossOverworldBoundary(
            RoomSession session,
            RoomTransitionCoordinator coordinator,
            ScrollController scroll,
            OverworldCollision collision,
            Link link,
            int direction) {
        walkToAndCrossOverworldBoundary(
                session, coordinator, scroll, collision, link, direction, ScrollController.NONE);
    }

    static void walkToAndCrossOverworldBoundary(
            RoomSession session,
            RoomTransitionCoordinator coordinator,
            ScrollController scroll,
            OverworldCollision collision,
            Link link,
            int direction,
            int nextDirection) {
        // This is a collision-connectivity harness, not a keyboard-input
        // simulation: find a source-valid pixel path to the requested edge,
        // then exercise the real room-boundary transition from that edge.
        int nextRoom = session.rooms().currentRoomId()
                + switch (direction) {
                    case ScrollController.UP -> -16;
                    case ScrollController.DOWN -> 16;
                    case ScrollController.LEFT -> -1;
                    case ScrollController.RIGHT -> 1;
                    default -> throw new IllegalArgumentException();
                };
        var destination = session.components.roomLoader().loadOverworld(nextRoom);
        var landingCollision = new OverworldCollision(session.components.romTables());
        landingCollision.setRoom(destination.roomObjectsArea());
        landingCollision.setGbcOverlay(destination.gbcOverlay());
        int movementDirection =
                switch (direction) {
                    case ScrollController.UP -> Direction.UP.javaIndex();
                    case ScrollController.DOWN -> Direction.DOWN.javaIndex();
                    case ScrollController.LEFT -> Direction.LEFT.javaIndex();
                    case ScrollController.RIGHT -> Direction.RIGHT.javaIndex();
                    default -> throw new IllegalArgumentException();
                };
        // Choose a seam with walkable ground on both sides. The old edge snap
        // masked exits that landed inside the destination's border wall.
        int[] exit = reachableBoundaryPosition(
                collision, link.position().pixelX(), link.position().pixelY(), direction, (x, y) -> {
                    int landingX =
                            direction == ScrollController.LEFT ? 140 : direction == ScrollController.RIGHT ? 3 : x;
                    int landingY = direction == ScrollController.UP ? 111 : direction == ScrollController.DOWN ? 0 : y;
                    int dx = direction == ScrollController.LEFT ? -1 : direction == ScrollController.RIGHT ? 1 : 0;
                    int dy = direction == ScrollController.UP ? -1 : direction == ScrollController.DOWN ? 1 : 0;
                    return !leadingEdgeBlocked(landingCollision, landingX + dx, landingY + dy, movementDirection, true)
                            && (nextDirection == ScrollController.NONE
                                    || reachableBoundaryPosition(landingCollision, landingX, landingY, nextDirection)
                                            != null);
                });
        assertNotNull(
                exit,
                "No sword-accessible collision path from ("
                        + link.position().pixelX() + "," + link.position().pixelY() + ") toward " + direction
                        + "\n" + collisionGrid(collision));
        link.position().setPixelPosition(exit[0], exit[1]);
        // Gameplay publishes the destination Link pose before the room-load
        // CreateFollowingNpcEntity call. This low-level harness must do so too.
        int arrivalX = direction == ScrollController.LEFT ? 140 : direction == ScrollController.RIGHT ? 3 : exit[0];
        int arrivalY = direction == ScrollController.UP ? 111 : direction == ScrollController.DOWN ? 0 : exit[1];
        session.story()
                .setFollowingNpcState(
                        session.story().followingNpcState(),
                        arrivalX + 8,
                        arrivalY + 16,
                        0,
                        0,
                        link.animation().direction());
        crossOverworldBoundary(coordinator, scroll, link, direction);
    }

    static void walkToAndCrossIndoorBoundary(
            RoomTransitionCoordinator coordinator,
            TransitionController transition,
            ScrollController scroll,
            OverworldCollision collision,
            Link link,
            int direction) {
        List<int[]> path = reachableBoundaryPath(
                collision, link.position().pixelX(), link.position().pixelY(), direction);
        assertNotNull(path, "No collision path to indoor boundary " + direction + "\n" + collisionGrid(collision));
        for (int[] position : path) {
            link.position().setPixelPosition(position[0], position[1]);
            coordinator.handleWarpAndIndoorBoundaries(link);
            assertFalse(transition.isActive(), "collision path crossed an active indoor warp");
            assertFalse(scroll.isActive(), "collision path left the room before its target boundary");
        }
        int[] exit = path.getLast();
        link.position().setPixelPosition(exit[0], exit[1]);
        switch (direction) {
            case ScrollController.UP -> link.position()
                    .setPixelPosition(link.position().pixelX(), -5);
            case ScrollController.DOWN -> link.position()
                    .setPixelPosition(link.position().pixelX(), RoomConstants.ROOM_PIXEL_HEIGHT);
            case ScrollController.LEFT -> link.position()
                    .setPixelPosition(-5, link.position().pixelY());
            case ScrollController.RIGHT -> link.position()
                    .setPixelPosition(
                            RoomConstants.ROOM_PIXEL_WIDTH, link.position().pixelY());
            default -> throw new IllegalArgumentException("Unknown indoor scroll direction: " + direction);
        }
        coordinator.handleWarpAndIndoorBoundaries(link);
        assertTrue(scroll.isActive());
        assertEquals(direction, scroll.direction());
        switch (direction) {
            case ScrollController.UP -> assertEquals(
                    RoomConstants.ROOM_PIXEL_HEIGHT - Link.SPRITE_SIZE,
                    link.position().pixelY());
            case ScrollController.DOWN -> assertEquals(0, link.position().pixelY());
            case ScrollController.LEFT -> assertEquals(
                    RoomConstants.ROOM_PIXEL_WIDTH - Link.SPRITE_SIZE,
                    link.position().pixelX());
            case ScrollController.RIGHT -> assertEquals(0, link.position().pixelX());
            default -> throw new AssertionError(direction);
        }
        while (scroll.isActive()) {
            scroll.tick(8);
        }
    }

    static void walkToAndExitIndoorFrontDoor(
            RoomTransitionCoordinator coordinator,
            TransitionController transition,
            OverworldCollision collision,
            Link link) {
        // A downward scroll can leave Link completing the source landing hop.
        for (int frame = 0; frame < 64 && link.airborne().isAirborne(); frame++)
            link.motion().update();
        assertFalse(link.airborne().isAirborne());
        List<int[]> path = reachableBoundaryPath(
                collision, link.position().pixelX(), link.position().pixelY(), ScrollController.DOWN);
        assertNotNull(path, "No collision path to indoor front door\n" + collisionGrid(collision));
        for (int[] position : path) {
            link.position().setPixelPosition(position[0], position[1]);
            coordinator.handleWarpAndIndoorBoundaries(link);
            assertFalse(transition.isActive(), "collision path crossed the front-door boundary early");
        }
        int[] exit = path.getLast();
        link.position().setPixelPosition(exit[0], RoomConstants.ROOM_PIXEL_HEIGHT);
        coordinator.handleWarpAndIndoorBoundaries(link);
        assertTrue(transition.isActive());
        while (transition.isActive()) {
            transition.tick();
        }
    }

    static void walkToAndCrossIndoorBoundaryWithFeather(
            RoomTransitionCoordinator coordinator,
            TransitionController transition,
            ScrollController scroll,
            OverworldCollision collision,
            RomTables romTables,
            Link link,
            InputState inputState,
            InputConfig inputConfig,
            ItemRegistry itemRegistry,
            PlayerState playerState,
            int direction) {
        List<int[]> path = reachableBoundaryPathWithFeather(
                collision, romTables, link.position().pixelX(), link.position().pixelY(), direction);
        assertNotNull(
                path,
                "No Feather-valid collision path to indoor boundary " + direction + "\n" + collisionGrid(collision));
        int jumpCount = walkCollisionPathWithFeather(
                coordinator,
                transition,
                scroll,
                collision,
                link,
                inputState,
                inputConfig,
                itemRegistry,
                playerState,
                path);
        assertTrue(jumpCount > 0);

        int[] exit = path.getLast();
        link.position().setPixelPosition(exit[0], exit[1]);
        switch (direction) {
            case ScrollController.UP -> link.position()
                    .setPixelPosition(link.position().pixelX(), -5);
            case ScrollController.DOWN -> link.position()
                    .setPixelPosition(link.position().pixelX(), RoomConstants.ROOM_PIXEL_HEIGHT);
            case ScrollController.LEFT -> link.position()
                    .setPixelPosition(-5, link.position().pixelY());
            case ScrollController.RIGHT -> link.position()
                    .setPixelPosition(
                            RoomConstants.ROOM_PIXEL_WIDTH, link.position().pixelY());
            default -> throw new IllegalArgumentException("Unknown indoor scroll direction: " + direction);
        }
        coordinator.handleWarpAndIndoorBoundaries(link);
        assertTrue(scroll.isActive());
        assertEquals(direction, scroll.direction());
        while (scroll.isActive()) {
            scroll.tick(8);
        }
    }

    static int walkCollisionPathWithFeather(
            RoomTransitionCoordinator coordinator,
            TransitionController transition,
            ScrollController scroll,
            OverworldCollision collision,
            Link link,
            InputState inputState,
            InputConfig inputConfig,
            ItemRegistry itemRegistry,
            PlayerState playerState,
            List<int[]> path) {
        int jumpCount = 0;
        for (int index = 1; index < path.size(); index++) {
            int[] step = path.get(index);
            int action = step[2];
            if (action < 4) {
                link.position().setPixelPosition(step[0], step[1]);
            } else {
                int encodedMasks = action - 4;
                int firstMask = encodedMasks & 0x0F;
                int secondMask = (encodedMasks >>> 4) & 0x0F;
                int thirdMask = (encodedMasks >>> 8) & 0x0F;
                setDirectionalInput(inputState, inputConfig, firstMask, GLFW_PRESS);
                int jumpStartX = link.position().pixelX();
                int jumpStartY = link.position().pixelY();
                itemRegistry.item(playerState.inventory().itemA()).onPress();
                assertTrue(
                        link.airborne().isAirborne(),
                        "Feather did not start at (" + jumpStartX + "," + jumpStartY + ")");
                boolean crossedPit = false;
                int deadline = 0x40;
                int jumpFrame = 0;
                while (link.airborne().isAirborne() && deadline-- > 0) {
                    if (jumpFrame == 11) {
                        setDirectionalInput(inputState, inputConfig, firstMask, GLFW_RELEASE);
                        setDirectionalInput(inputState, inputConfig, secondMask, GLFW_PRESS);
                    } else if (jumpFrame == 22) {
                        setDirectionalInput(inputState, inputConfig, secondMask, GLFW_RELEASE);
                        setDirectionalInput(inputState, inputConfig, thirdMask, GLFW_PRESS);
                    }
                    link.motion().update();
                    crossedPit |= collision.linkOnNormalPit(
                            link.position().pixelX(), link.position().pixelY());
                    jumpFrame++;
                }
                setDirectionalInput(inputState, inputConfig, firstMask, GLFW_RELEASE);
                setDirectionalInput(inputState, inputConfig, secondMask, GLFW_RELEASE);
                setDirectionalInput(inputState, inputConfig, thirdMask, GLFW_RELEASE);
                assertTrue(
                        crossedPit,
                        "planned jump " + firstMask + "/"
                                + secondMask + "/" + thirdMask + " from (" + jumpStartX
                                + "," + jumpStartY + ") landed at ("
                                + link.position().pixelX() + ","
                                + link.position().pixelY() + "), expected (" + step[0] + "," + step[1]
                                + ")\n" + collisionGrid(collision));
                assertFalse(link.airborne().isAirborne());
                assertFalse(link.airborne().isFallingIntoPit());
                assertFalse(collision.linkOnNormalPit(
                        link.position().pixelX(), link.position().pixelY()));
                // The planner models integer pixels; Link retains subpixel
                // inertia during the real jump and can land one pixel beyond
                // that modelled point without changing the collision cell.
                assertTrue(
                        Math.abs(step[0] - link.position().pixelX()) <= 1,
                        "jump " + firstMask + "/" + secondMask + "/" + thirdMask + " from " + jumpStartX + ","
                                + jumpStartY + " expected " + step[0] + "," + step[1] + " actual "
                                + link.position().pixelX() + ","
                                + link.position().pixelY());
                assertTrue(Math.abs(step[1] - link.position().pixelY()) <= 1);
                link.motion().update();
                assertFalse(link.airborne().isFallingIntoPit());
                jumpCount++;
            }
            coordinator.handleWarpAndIndoorBoundaries(link);
            assertFalse(transition.isActive());
            assertFalse(scroll.isActive());
        }
        return jumpCount;
    }

    static void jumpOverPitWithFeather(
            OverworldCollision collision,
            Link link,
            InputState inputState,
            InputConfig inputConfig,
            ItemRegistry itemRegistry,
            PlayerState playerState,
            int joypadMask) {
        setDirectionalInput(inputState, inputConfig, joypadMask, GLFW_PRESS);
        itemRegistry.item(playerState.inventory().itemA()).onPress();
        assertTrue(link.airborne().isAirborne());
        boolean crossedPit = false;
        int deadline = 0x40;
        while (link.airborne().isAirborne() && deadline-- > 0) {
            link.motion().update();
            crossedPit |= collision.linkOnNormalPit(
                    link.position().pixelX(), link.position().pixelY());
        }
        setDirectionalInput(inputState, inputConfig, joypadMask, GLFW_RELEASE);
        assertTrue(crossedPit);
        assertFalse(link.airborne().isAirborne());
        assertFalse(link.airborne().isFallingIntoPit());
        assertFalse(collision.linkOnNormalPit(
                link.position().pixelX(), link.position().pixelY()));
        link.motion().update();
        assertFalse(link.airborne().isFallingIntoPit());
    }

    static List<int[]> reachableBoundaryPathWithFeather(
            OverworldCollision collision, RomTables romTables, int startX, int startY, int targetDirection) {
        return reachablePositionPathWithFeather(
                collision,
                romTables,
                startX,
                startY,
                (x, y) -> isTargetBoundary(
                        x,
                        y,
                        RoomConstants.ROOM_PIXEL_WIDTH - Link.SPRITE_SIZE + 1,
                        RoomConstants.ROOM_PIXEL_HEIGHT - Link.SPRITE_SIZE + 1,
                        targetDirection));
    }

    static List<int[]> reachablePositionPathWithFeather(
            OverworldCollision collision,
            RomTables romTables,
            int startX,
            int startY,
            java.util.function.BiPredicate<Integer, Integer> target) {
        int width = RoomConstants.ROOM_PIXEL_WIDTH - Link.SPRITE_SIZE + 1;
        int height = RoomConstants.ROOM_PIXEL_HEIGHT - Link.SPRITE_SIZE + 1;
        boolean[][] visited = new boolean[height][width];
        int[][] parentX = new int[height][width];
        int[][] parentY = new int[height][width];
        int[][] parentAction = new int[height][width];
        for (int row = 0; row < height; row++) {
            java.util.Arrays.fill(parentX[row], -1);
            java.util.Arrays.fill(parentY[row], -1);
            java.util.Arrays.fill(parentAction[row], -1);
        }
        int clampedX = Math.max(0, Math.min(startX, width - 1));
        int clampedY = Math.max(0, Math.min(startY, height - 1));
        visited[clampedY][clampedX] = true;
        parentX[clampedY][clampedX] = clampedX;
        parentY[clampedY][clampedX] = clampedY;
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        queue.add(new int[] {clampedX, clampedY});
        int[][] moves = {
            {0, -1, Direction.UP.javaIndex()},
            {0, 1, Direction.DOWN.javaIndex()},
            {-1, 0, Direction.LEFT.javaIndex()},
            {1, 0, Direction.RIGHT.javaIndex()}
        };
        int[] jumpMasks = {0x00, 0x04, 0x08, 0x02, 0x01, 0x06, 0x05, 0x0A, 0x09};

        while (!queue.isEmpty()) {
            int[] position = queue.removeFirst();
            if (target.test(position[0], position[1])) {
                ArrayDeque<int[]> reverse = new ArrayDeque<>();
                int x = position[0];
                int y = position[1];
                while (parentX[y][x] != x || parentY[y][x] != y) {
                    reverse.addFirst(new int[] {x, y, parentAction[y][x]});
                    int nextX = parentX[y][x];
                    int nextY = parentY[y][x];
                    x = nextX;
                    y = nextY;
                }
                reverse.addFirst(new int[] {x, y, -1});
                return List.copyOf(reverse);
            }

            for (int[] move : moves) {
                int x = position[0] + move[0];
                int y = position[1] + move[1];
                if (x < 0
                        || x >= width
                        || y < 0
                        || y >= height
                        || visited[y][x]
                        || leadingEdgeBlocked(collision, x, y, move[2], true)
                        || collision.linkOnNormalPit(x, y)) {
                    continue;
                }
                visited[y][x] = true;
                parentX[y][x] = position[0];
                parentY[y][x] = position[1];
                parentAction[y][x] = move[2];
                queue.addLast(new int[] {x, y});
            }

            for (int firstMask : jumpMasks) {
                for (int secondMask : jumpMasks) {
                    for (int thirdMask : jumpMasks) {
                        int subX = position[0] << Link.SUB_PIXEL_SHIFT;
                        int subY = position[1] << Link.SUB_PIXEL_SHIFT;
                        boolean crossedPit = false;
                        boolean blockedJump = false;
                        int speedX = (byte) romTables.linkMotionTables().linkSpeedX(firstMask);
                        int speedY = (byte) romTables.linkMotionTables().linkSpeedY(firstMask);
                        for (int frame = 0; frame < 33; frame++) {
                            int joypadMask = frame < 11 ? firstMask : frame < 22 ? secondMask : thirdMask;
                            if (speedX != 0) {
                                int candidateSubX = subX + speedX;
                                int nextX = candidateSubX >> Link.SUB_PIXEL_SHIFT;
                                int y = subY >> Link.SUB_PIXEL_SHIFT;
                                int moveDirection =
                                        speedX < 0 ? Direction.LEFT.javaIndex() : Direction.RIGHT.javaIndex();
                                if (nextX >= 0
                                        && nextX < width
                                        && !leadingEdgeBlocked(collision, nextX, y, moveDirection, false)) {
                                    subX = candidateSubX;
                                } else {
                                    blockedJump = true;
                                    break;
                                }
                            }
                            if (speedY != 0) {
                                int candidateSubY = subY + speedY;
                                int x = subX >> Link.SUB_PIXEL_SHIFT;
                                int nextY = candidateSubY >> Link.SUB_PIXEL_SHIFT;
                                int moveDirection = speedY < 0 ? Direction.UP.javaIndex() : Direction.DOWN.javaIndex();
                                if (nextY >= 0
                                        && nextY < height
                                        && !leadingEdgeBlocked(collision, x, nextY, moveDirection, false)) {
                                    subY = candidateSubY;
                                } else {
                                    blockedJump = true;
                                    break;
                                }
                            }
                            int x = subX >> Link.SUB_PIXEL_SHIFT;
                            int y = subY >> Link.SUB_PIXEL_SHIFT;
                            crossedPit |= collision.linkOnNormalPit(x, y);
                            // ApplyLinkGroundMotion steers after XY; released axes coast.
                            if ((joypadMask & 3) != 0) {
                                speedX += Integer.compare(
                                        romTables.linkMotionTables().airSpeedX(joypadMask & 3), speedX);
                            }
                            if ((joypadMask & 12) != 0) {
                                speedY += Integer.compare(
                                        romTables.linkMotionTables().airSpeedY((joypadMask >>> 2) & 3), speedY);
                            }
                        }
                        int x = subX >> Link.SUB_PIXEL_SHIFT;
                        int y = subY >> Link.SUB_PIXEL_SHIFT;
                        if (blockedJump || !crossedPit || collision.linkOnNormalPit(x, y) || visited[y][x]) {
                            continue;
                        }
                        visited[y][x] = true;
                        parentX[y][x] = position[0];
                        parentY[y][x] = position[1];
                        parentAction[y][x] = 4 + firstMask + (secondMask << 4) + (thirdMask << 8);
                        queue.addLast(new int[] {x, y});
                    }
                }
            }
        }
        return null;
    }

    static void setDirectionalInput(InputState inputState, InputConfig inputConfig, int joypadMask, int action) {
        if ((joypadMask & 0x04) != 0) inputState.onKeyEvent(inputConfig.upKey(), action);
        if ((joypadMask & 0x08) != 0) inputState.onKeyEvent(inputConfig.downKey(), action);
        if ((joypadMask & 0x02) != 0) inputState.onKeyEvent(inputConfig.leftKey(), action);
        if ((joypadMask & 0x01) != 0) inputState.onKeyEvent(inputConfig.rightKey(), action);
    }

    static List<int[]> reachableBoundaryPath(
            OverworldCollision collision, int startX, int startY, int targetDirection) {
        return reachableBoundaryPath(collision, startX, startY, targetDirection, false);
    }

    static List<int[]> reachableEntityWindowPath(
            OverworldCollision collision, int startX, int startY, RoomEntity target) {
        return reachablePositionPath(
                collision,
                startX,
                startY,
                (x, y) -> Math.abs(signedByteDelta(target.x(), x + 8)) <= 0x28
                        && Math.abs(signedByteDelta(target.y(), y + 16)) <= 0x28);
    }

    static List<int[]> reachableFollowerLeashPath(
            OverworldCollision collision, int startX, int startY, RoomEntity bowWow) {
        return reachablePositionPath(
                collision,
                startX,
                startY,
                (x, y) -> Math.abs(signedByteDelta(bowWow.x(), x + 8)) <= 0x10
                        && Math.abs(signedByteDelta(bowWow.y(), y + 16)) <= 0x10);
    }

    static List<int[]> reachableEntityContactPath(
            OverworldCollision collision, int startX, int startY, RoomEntity entity) {
        return reachablePositionPath(
                collision,
                startX,
                startY,
                (x, y) -> Math.abs(signedByteDelta(entity.x(), x + 8)) <= 0x04
                        && Math.abs(signedByteDelta(entity.y(), y + 16)) <= 0x04);
    }

    record SwordApproach(List<int[]> path, int direction) {}

    static SwordApproach reachableSwordContactPath(
            OverworldCollision collision, int startX, int startY, RoomEntity entity, RomTables romTables) {
        Sword sword = new Sword(
                new Sword.Assets(romTables.swordTables(), linksawakening.equipment.SwordSpriteSheet.invisible()));
        sword.onPress();
        for (int swingFrame = 0; swingFrame < 4; swingFrame++) {
            sword.tick(false);
        }
        for (int direction = Direction.DOWN.javaIndex(); direction <= Direction.RIGHT.javaIndex(); direction++) {
            int swordDirection = direction;
            List<int[]> path = reachablePositionPath(collision, startX, startY, (x, y) -> {
                Sword.CollisionBox box = sword.enemyCollisionBox(x + 8, y + 16, swordDirection);
                return box.active()
                        && linksawakening.world.entity.EntityCombatTestAccess.overlapsSword(
                                entity, box.x(), box.width(), box.y(), box.height());
            });
            if (path != null) {
                return new SwordApproach(path, swordDirection);
            }
        }
        return null;
    }

    static SwordApproach reachableSwordContactPathWithFeather(
            OverworldCollision collision, RomTables romTables, int startX, int startY, RoomEntity entity) {
        Sword sword = new Sword(
                new Sword.Assets(romTables.swordTables(), linksawakening.equipment.SwordSpriteSheet.invisible()));
        sword.onPress();
        for (int swingFrame = 0; swingFrame < 4; swingFrame++) {
            sword.tick(false);
        }
        for (int direction = Direction.DOWN.javaIndex(); direction <= Direction.RIGHT.javaIndex(); direction++) {
            int swordDirection = direction;
            List<int[]> path = reachablePositionPathWithFeather(collision, romTables, startX, startY, (x, y) -> {
                Sword.CollisionBox box = sword.enemyCollisionBox(x + 8, y + 16, swordDirection);
                return box.active()
                        && linksawakening.world.entity.EntityCombatTestAccess.overlapsSword(
                                entity, box.x(), box.width(), box.y(), box.height());
            });
            if (path != null) {
                return new SwordApproach(path, swordDirection);
            }
        }
        return null;
    }

    static PowderApproach reachablePowderApproach(
            OverworldCollision collision, int startX, int startY, int targetLocation) {
        int[] xOffsets = {0x0E, -0x0E, 0x00, 0x00};
        int[] yOffsets = {0x00, 0x00, -0x0C, 0x0C};
        for (int romDirection = 0; romDirection < 4; romDirection++) {
            int direction = romDirection;
            List<int[]> path = reachablePositionPath(collision, startX, startY, (x, y) -> {
                int sprinkleX = (x + 0x08 + xOffsets[direction]) & 0xFF;
                int sprinkleY = (y + 0x10 + yOffsets[direction]) & 0xFF;
                int objectLeft = ((sprinkleX - 0x01) & 0xFF) & 0xF0;
                int objectTop = ((sprinkleY - 0x09) & 0xFF) & 0xF0;
                int sampledLeft = sprinkleX & 0xF0;
                int sampledTop = ((sprinkleY - 0x08) & 0xFF) & 0xF0;
                return (objectTop | (objectLeft >>> 4)) == targetLocation
                        && (sampledTop | (sampledLeft >>> 4)) == targetLocation;
            });
            if (path != null) {
                return new PowderApproach(path, direction);
            }
        }
        return null;
    }

    record PowderApproach(List<int[]> path, int romDirection) {}

    static List<int[]> reachableWarpPath(OverworldCollision collision, int startX, int startY, int tileLocation) {
        return reachablePositionPath(collision, startX, startY, (x, y) -> Warp.packTileLocation(x, y) == tileLocation);
    }

    static List<int[]> reachablePositionPath(
            OverworldCollision collision, int startX, int startY, BiPredicate<Integer, Integer> goal) {
        int width = RoomConstants.ROOM_PIXEL_WIDTH - Link.SPRITE_SIZE + 1;
        int height = RoomConstants.ROOM_PIXEL_HEIGHT - Link.SPRITE_SIZE + 1;
        boolean[][] visited = new boolean[height][width];
        int[][] parentX = new int[height][width];
        int[][] parentY = new int[height][width];
        int clampedX = Math.max(0, Math.min(startX, width - 1));
        int clampedY = Math.max(0, Math.min(startY, height - 1));
        visited[clampedY][clampedX] = true;
        parentX[clampedY][clampedX] = clampedX;
        parentY[clampedY][clampedX] = clampedY;
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        queue.add(new int[] {clampedX, clampedY});
        int[][] moves = {
            {0, -1, Direction.UP.javaIndex()},
            {0, 1, Direction.DOWN.javaIndex()},
            {-1, 0, Direction.LEFT.javaIndex()},
            {1, 0, Direction.RIGHT.javaIndex()}
        };

        while (!queue.isEmpty()) {
            int[] position = queue.removeFirst();
            if (goal.test(position[0], position[1])) {
                ArrayDeque<int[]> reverse = new ArrayDeque<>();
                int x = position[0];
                int y = position[1];
                while (parentX[y][x] != x || parentY[y][x] != y) {
                    reverse.addFirst(new int[] {x, y});
                    int nextX = parentX[y][x];
                    int nextY = parentY[y][x];
                    x = nextX;
                    y = nextY;
                }
                reverse.addFirst(new int[] {x, y});
                return List.copyOf(reverse);
            }
            for (int[] move : moves) {
                int x = position[0] + move[0];
                int y = position[1] + move[1];
                if (x < 0
                        || x >= width
                        || y < 0
                        || y >= height
                        || visited[y][x]
                        || leadingEdgeBlocked(collision, x, y, move[2], false)) {
                    continue;
                }
                visited[y][x] = true;
                parentX[y][x] = position[0];
                parentY[y][x] = position[1];
                queue.addLast(new int[] {x, y});
            }
        }
        return null;
    }

    static List<int[]> reachableBoundaryPath(
            OverworldCollision collision, int startX, int startY, int targetDirection, boolean pitsBlock) {
        int width = RoomConstants.ROOM_PIXEL_WIDTH - Link.SPRITE_SIZE + 1;
        int height = RoomConstants.ROOM_PIXEL_HEIGHT - Link.SPRITE_SIZE + 1;
        boolean[][] visited = new boolean[height][width];
        int[][] parentX = new int[height][width];
        int[][] parentY = new int[height][width];
        for (int row = 0; row < height; row++) {
            java.util.Arrays.fill(parentX[row], -1);
            java.util.Arrays.fill(parentY[row], -1);
        }
        int clampedX = Math.max(0, Math.min(startX, width - 1));
        int clampedY = Math.max(0, Math.min(startY, height - 1));
        visited[clampedY][clampedX] = true;
        parentX[clampedY][clampedX] = clampedX;
        parentY[clampedY][clampedX] = clampedY;
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        queue.add(new int[] {clampedX, clampedY});
        int[][] moves = {
            {0, -1, Direction.UP.javaIndex()},
            {0, 1, Direction.DOWN.javaIndex()},
            {-1, 0, Direction.LEFT.javaIndex()},
            {1, 0, Direction.RIGHT.javaIndex()}
        };

        while (!queue.isEmpty()) {
            int[] position = queue.removeFirst();
            if (isTargetBoundary(position[0], position[1], width, height, targetDirection)) {
                ArrayDeque<int[]> reverse = new ArrayDeque<>();
                int x = position[0];
                int y = position[1];
                while (parentX[y][x] != x || parentY[y][x] != y) {
                    reverse.addFirst(new int[] {x, y});
                    int nextX = parentX[y][x];
                    int nextY = parentY[y][x];
                    x = nextX;
                    y = nextY;
                }
                reverse.addFirst(new int[] {x, y});
                return List.copyOf(reverse);
            }
            for (int[] move : moves) {
                int x = position[0] + move[0];
                int y = position[1] + move[1];
                if (x < 0
                        || x >= width
                        || y < 0
                        || y >= height
                        || visited[y][x]
                        || leadingEdgeBlocked(collision, x, y, move[2], pitsBlock)) {
                    continue;
                }
                visited[y][x] = true;
                parentX[y][x] = position[0];
                parentY[y][x] = position[1];
                queue.addLast(new int[] {x, y});
            }
        }
        return null;
    }

    static int[] reachableBoundaryPosition(OverworldCollision collision, int startX, int startY, int targetDirection) {
        return reachableBoundaryPosition(collision, startX, startY, targetDirection, (x, y) -> true);
    }

    static int[] reachableBoundaryPosition(
            OverworldCollision collision,
            int startX,
            int startY,
            int targetDirection,
            java.util.function.BiPredicate<Integer, Integer> landingAllowed) {
        int width = RoomConstants.ROOM_PIXEL_WIDTH - Link.SPRITE_SIZE + 1;
        int height = RoomConstants.ROOM_PIXEL_HEIGHT - Link.SPRITE_SIZE + 1;
        boolean[][] visited = new boolean[height][width];
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        int clampedX = Math.max(0, Math.min(startX, width - 1));
        int clampedY = Math.max(0, Math.min(startY, height - 1));
        visited[clampedY][clampedX] = true;
        queue.add(new int[] {clampedX, clampedY});

        int[][] moves = {
            {0, -1, Direction.UP.javaIndex()},
            {0, 1, Direction.DOWN.javaIndex()},
            {-1, 0, Direction.LEFT.javaIndex()},
            {1, 0, Direction.RIGHT.javaIndex()}
        };
        while (!queue.isEmpty()) {
            int[] position = queue.removeFirst();
            if (isTargetBoundary(position[0], position[1], width, height, targetDirection)
                    && landingAllowed.test(position[0], position[1])) {
                return position;
            }
            for (int[] move : moves) {
                int x = position[0] + move[0];
                int y = position[1] + move[1];
                if (x < 0
                        || x >= width
                        || y < 0
                        || y >= height
                        || visited[y][x]
                        || leadingEdgeBlocked(collision, x, y, move[2], false)) {
                    continue;
                }
                visited[y][x] = true;
                queue.addLast(new int[] {x, y});
            }
        }
        return null;
    }

    static boolean isTargetBoundary(int x, int y, int width, int height, int direction) {
        return switch (direction) {
            case ScrollController.UP -> y == 0;
            case ScrollController.DOWN -> y == height - 1;
            case ScrollController.LEFT -> x == 0;
            case ScrollController.RIGHT -> x == width - 1;
            default -> false;
        };
    }

    static boolean leadingEdgeBlocked(OverworldCollision collision, int x, int y, int direction, boolean pitsBlock) {
        int[][] pointX = {{6, 9}, {6, 9}, {4, 4}, {11, 11}};
        int[][] pointY = {{15, 15}, {6, 6}, {9, 12}, {9, 12}};
        for (int index = 0; index < 2; index++) {
            int sampleX = x + pointX[direction][index];
            int sampleY = y + pointY[direction][index];
            int object = collision.objectIdAtPoint(sampleX, sampleY);
            // This connectivity helper models the sword-owned route after its
            // already-covered static sword collision removes a bush.
            boolean swordCuttable = object == 0x5C || object == 0xD3;
            if (pitsBlock && collision.pointNormalPit(sampleX, sampleY)) return true;
            if (!swordCuttable
                    && collision.pointBlockedForLink(sampleX, sampleY, false)
                    && (pitsBlock || !collision.pointNormalPit(sampleX, sampleY))) {
                return true;
            }
        }
        return false;
    }

    static String collisionGrid(OverworldCollision collision) {
        StringBuilder result = new StringBuilder();
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 10; col++) {
                int x = col * 16 + 8;
                int y = row * 16 + 8;
                result.append(String.format(
                        "%02X%s ",
                        collision.objectIdAtPoint(x, y), collision.pointBlockedForLink(x, y, false) ? "#" : "."));
            }
            result.append('\n');
        }
        return result.toString();
    }

    static int[] roomWithPitAt(int col, int row) {
        int[] roomObjectsArea = new int[0x100];
        java.util.Arrays.fill(roomObjectsArea, 0xFF);
        roomObjectsArea[ROOM_OBJECTS_BASE + row * ROOM_OBJECT_ROW_STRIDE + col] = OBJECT_NORMAL_PIT;
        return roomObjectsArea;
    }

    static void runUntilPitFallStarts(Link link) {
        int guard = 0;
        while (!link.airborne().isFallingIntoPit() && guard++ < 128) {
            link.motion().update();
        }
        assertTrue(link.airborne().isFallingIntoPit());
    }

    static void runUntilPitFallFinishes(Link link) {
        int guard = 0;
        while (link.airborne().isFallingIntoPit() && guard++ < 128) {
            link.motion().update();
        }
        assertFalse(link.airborne().isFallingIntoPit());
    }

    static byte[] loadRom() throws IOException {
        try (var stream = linksawakening.testing.TestRom.open()) {
            if (stream == null) {
                throw new IllegalStateException("ROM resource missing");
            }
            return stream.readAllBytes();
        }
    }
}
