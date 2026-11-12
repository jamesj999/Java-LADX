package linksawakening.world.room;

import static linksawakening.world.room.RoomConstants.ROOM_OBJECTS_BASE;
import static linksawakening.world.room.RoomTransitionTestSupport2.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;

import java.util.List;
import linksawakening.entity.Link;
import linksawakening.equipment.ItemRegistry;
import linksawakening.gpu.GPU;
import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
import linksawakening.physics.OverworldCollision;
import linksawakening.rom.RomTables;
import linksawakening.state.PlayerState;
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

final class RoomTransitionTestSupport {
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
            link.update();
            coordinator.handleWarpAndIndoorBoundaries(link);
        }
        input.onKeyEvent(config.upKey(), GLFW_RELEASE);
        assertTrue(transition.isActive(), "moving collision probe must enter the doorway");
    }

    static RoomSession newSession(byte[] rom, RomTables romTables, OverworldCollision collision) {
        return new RoomSession(
                rom,
                new GPU(),
                new RoomLoader(rom),
                new OverworldTilesetTable(rom),
                collision,
                new TransientVfxSystem(16),
                null);
    }

    static void tickInteractiveEntities(RoomSession session, int frame, int linkEntityX, int linkEntityY) {
        session.entities()
                .tickEntitiesWithProjectileEvents(frame, linkEntityX, linkEntityY, 0, 0, Link.DIRECTION_DOWN, false);
    }

    static List<EntityCombatEvent> tickLiveRoom3B(
            RoomSession session,
            Link link,
            RoomTransitionCoordinator coordinator,
            TransitionController transition,
            ScrollController scroll,
            int frame) {
        link.captureRomFinalPosition();
        link.update();
        session.entityState().setEntityAttackStepAnimationCountdown(link.romAttackStepAnimationCountdown());
        session.components.entityInputs().setEntityFinalPositionX(link.romFinalPositionX());
        RoomEntityTickBuilder.frame(frame)
                .position(link.romEntityX(), link.romEntityY(), link.romEntityZ())
                .motion(link.isAirborne(), link.romMotionState(), link.direction(), link.romCollisionType())
                .shield(link.isUsingShield(), 1, 0)
                .sword(false, 0, 0, 0, 0)
                .speed(link.romSpeedX(), link.romSpeedY())
                .tick(session);
        // Main clears this per-frame flag after AnimateEntities and before
        // applying the current descending-slot platform requests.
        link.clearStandingOnSideViewEntity();

        List<SideViewPlatformLinkRequest> platforms =
                session.components.entityBridge().outbox.consumeSideViewPlatformLinkRequests();
        List<SideViewPotLinkRequest> pots =
                session.components.entityBridge().outbox.consumeSideViewPotLinkRequests();
        List<LinkFinalPositionRequest> finalPositions =
                session.components.entityBridge().outbox.consumeLinkFinalPositionRequests();
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
                    link.applySideViewPlatformContact(
                            request.horizontalDelta(), request.positionY(), request.speedY(), true);
                    coordinator.handleWarpAndIndoorBoundaries(link);
                    coordinator.handleOverworldBoundary(link);
                    if (scroll.isActive() || transition.isInputBlocked()) {
                        link.clearSideViewPlatformState();
                    }
                }
            } else if (stream == 1) {
                LinkFinalPositionRequest request = finalPositions.get(finalPositionIndex++);
                link.restoreRomFinalPosition();
                if (request.clearLinkPositionIncrement()) {
                    link.clearRomPositionIncrement();
                }
                if (request.markLinkPushing()) {
                    link.markRomLinkPushing(0x03);
                }
                if (request.resetPegasusBoots()) {
                    link.resetPegasusBoots();
                }
            } else {
                SideViewPotLinkRequest request = pots.get(potIndex++);
                if (request.restoreFinalPositionX()) {
                    link.restoreRomFinalPositionX();
                }
                if (request.resetPegasusBoots()) {
                    link.resetPegasusBoots();
                }
                if (request.speedX() != 0) {
                    link.applyRomSpeedX(request.speedX());
                }
                if (request.ignoreCollisionCountdown() != 0) {
                    link.setCollisionIgnoreFrames(request.ignoreCollisionCountdown());
                }
                if (request.snapTop()) {
                    link.applySideViewPotContact(request.positionY(), request.speedY());
                }
            }
        }
        for (LinkMotionBlockRequest request :
                session.components.entityBridge().outbox.consumeLinkMotionBlockRequests()) {
            link.blockNextRomMotionFrame();
        }
        // These Main streams are not emitted by the authored A5/D6 route;
        // fail loudly rather than silently dropping a new request type.
        assertTrue(
                session.components.entityBridge().consumeWarpLinkStateRequests().isEmpty());
        assertFalse(session.components.entityBridge().consumeWorldLinkMotionBlockRequest());
        assertTrue(session.components
                .entityBridge()
                .outbox
                .consumeLinkFallenPoseRequests()
                .isEmpty());
        assertTrue(session.components
                .entityBridge()
                .outbox
                .consumeLinkFacingRequests()
                .isEmpty());
        List<LinkAttackClearRequest> attackClears = session.entityState().consumeLinkAttackClearRequests();
        List<LinkAttackStepRequest> attackSteps =
                session.components.entityBridge().outbox.consumeLinkAttackStepRequests();
        int attackClearIndex = 0;
        int attackStepIndex = 0;
        while (attackClearIndex < attackClears.size() || attackStepIndex < attackSteps.size()) {
            boolean applyClear = attackStepIndex >= attackSteps.size()
                    || (attackClearIndex < attackClears.size()
                            && attackClears.get(attackClearIndex).sourceSlot()
                                    > attackSteps.get(attackStepIndex).sourceSlot());
            if (applyClear) {
                attackClearIndex++;
                link.clearRomAttackStepAnimationCountdown();
            } else {
                // A same-slot tie is not emitted by the current handlers;
                // preserve Main's throw-step-wins ordering if one appears.
                attackStepIndex++;
                link.startRomItemAttackStep();
            }
        }
        assertTrue(session.components
                .entityBridge()
                .outbox
                .consumeLinkHeldItemPoseRequests()
                .isEmpty());
        assertTrue(session.components
                .entityBridge()
                .outbox
                .consumeLinkSwordSpinPoseRequests()
                .isEmpty());
        assertTrue(session.components
                .entityBridge()
                .outbox
                .consumeLinkSwordFinalPoseRequests()
                .isEmpty());
        LiftedEntityState lifted = session.rooms().liftedEntityState();
        if (lifted.active()) {
            link.setCarryingLiftedObjectState(lifted.carryState(), lifted.effectiveRomDirection());
        } else {
            link.setCarryingLiftedObjectState(
                    0,
                    switch (link.direction()) {
                        case Link.DIRECTION_RIGHT -> 0;
                        case Link.DIRECTION_LEFT -> 1;
                        case Link.DIRECTION_UP -> 2;
                        case Link.DIRECTION_DOWN -> 3;
                        default -> throw new AssertionError(link.direction());
                    });
        }
        return session.components.entityBridge().outbox.consumeEntityEvents();
    }

    static int letBowWowEatAllGoponga(RoomSession session, OverworldCollision collision, Link link, int frame) {
        int deadline = frame + 0x4000;
        while (session.rooms().activeRoom().entities().loadedEntities().stream()
                        .anyMatch(entity -> entity.type() == 0x7C || entity.type() == 0x7E)
                && frame < deadline) {
            RoomEntity target = session.rooms().activeRoom().entities().loadedEntities().stream()
                    .filter(entity -> entity.type() == 0x7C || entity.type() == 0x7E)
                    .findFirst()
                    .orElseThrow();
            RoomEntity bowWow = session.rooms().activeRoom().entities().loadedEntities().stream()
                    .filter(entity -> entity.type() == 0x6D)
                    .findFirst()
                    .orElseThrow();
            List<int[]> leashPath = reachableFollowerLeashPath(collision, link.pixelX(), link.pixelY(), bowWow);
            // BowWow can temporarily be outside Link's reachable approach
            // window. Run the real follower handler until Link can approach;
            // an immediate path depends on earlier movement and RNG timing.
            while (leashPath == null && frame < deadline) {
                tickInteractiveEntities(session, frame++, link.romEntityX(), link.romEntityY());
                session.components.entityBridge().outbox.consumeEntityEvents();
                bowWow = session.rooms().activeRoom().entities().loadedEntities().stream()
                        .filter(entity -> entity.type() == 0x6D)
                        .findFirst()
                        .orElseThrow();
                leashPath = reachableFollowerLeashPath(collision, link.pixelX(), link.pixelY(), bowWow);
            }
            assertNotNull(
                    leashPath, "No collision-valid Link path back into BowWow's leash\n" + collisionGrid(collision));
            for (int[] position : leashPath) {
                link.setPixelPosition(position[0], position[1]);
            }
            tickInteractiveEntities(session, frame++, link.romEntityX(), link.romEntityY());
            session.components.entityBridge().outbox.consumeEntityEvents();
            List<int[]> path = reachableEntityWindowPath(collision, link.pixelX(), link.pixelY(), target);
            assertNotNull(
                    path,
                    "No collision-valid Link path into BowWow's target window for "
                            + String.format("%02X@(%02X,%02X)\n", target.type(), target.x(), target.y())
                            + collisionGrid(collision));
            for (int[] position : path) {
                link.setPixelPosition(position[0], position[1]);
                tickInteractiveEntities(session, frame++, link.romEntityX(), link.romEntityY());
                session.components.entityBridge().outbox.consumeEntityEvents();
                while (bowWowOutsideLeash(session, link) && frame < deadline) {
                    tickInteractiveEntities(session, frame++, link.romEntityX(), link.romEntityY());
                    session.components.entityBridge().outbox.consumeEntityEvents();
                }
            }
            while (session.rooms().activeRoom().entities().loadedEntities().stream()
                            .anyMatch(entity ->
                                    entity.slot() == target.slot() && (entity.type() == 0x7C || entity.type() == 0x7E))
                    && frame < deadline) {
                tickInteractiveEntities(session, frame++, link.romEntityX(), link.romEntityY());
                session.components.entityBridge().outbox.consumeEntityEvents();
            }
        }
        assertFalse(
                session.rooms().activeRoom().entities().loadedEntities().stream()
                        .anyMatch(entity -> entity.type() == 0x7C || entity.type() == 0x7E),
                session.rooms().activeRoom().entities().loadedEntities().stream()
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
                .activeRoom()
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
        int[] tiles = session.rooms().activeRoom().tileIds();
        int rowStride = RoomConstants.ROOM_TILE_WIDTH;
        return new int[] {
            tiles[tileY * rowStride + tileX],
            tiles[tileY * rowStride + tileX + 1],
            tiles[(tileY + 1) * rowStride + tileX],
            tiles[(tileY + 1) * rowStride + tileX + 1]
        };
    }

    static boolean bowWowOutsideLeash(RoomSession session, Link link) {
        RoomEntity bowWow = session.rooms().activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0x6D)
                .findFirst()
                .orElseThrow();
        return Math.abs(signedByteDelta(link.romEntityX(), bowWow.x())) > 0x18
                || Math.abs(signedByteDelta(link.romEntityY(), bowWow.y())) > 0x18;
    }

    static void crossOverworldBoundary(
            RoomTransitionCoordinator coordinator, ScrollController scroll, Link link, int direction) {
        switch (direction) {
            case ScrollController.UP -> link.setPixelPosition(link.pixelX(), -5);
            case ScrollController.DOWN -> link.setPixelPosition(link.pixelX(), 116);
            case ScrollController.LEFT -> link.setPixelPosition(-5, link.pixelY());
            case ScrollController.RIGHT -> link.setPixelPosition(148, link.pixelY());
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
                    case ScrollController.UP -> Link.DIRECTION_UP;
                    case ScrollController.DOWN -> Link.DIRECTION_DOWN;
                    case ScrollController.LEFT -> Link.DIRECTION_LEFT;
                    case ScrollController.RIGHT -> Link.DIRECTION_RIGHT;
                    default -> throw new IllegalArgumentException();
                };
        // Choose a seam with walkable ground on both sides. The old edge snap
        // masked exits that landed inside the destination's border wall.
        int[] exit = reachableBoundaryPosition(collision, link.pixelX(), link.pixelY(), direction, (x, y) -> {
            int landingX = direction == ScrollController.LEFT ? 140 : direction == ScrollController.RIGHT ? 3 : x;
            int landingY = direction == ScrollController.UP ? 111 : direction == ScrollController.DOWN ? 0 : y;
            int dx = direction == ScrollController.LEFT ? -1 : direction == ScrollController.RIGHT ? 1 : 0;
            int dy = direction == ScrollController.UP ? -1 : direction == ScrollController.DOWN ? 1 : 0;
            return !leadingEdgeBlocked(landingCollision, landingX + dx, landingY + dy, movementDirection, true)
                    && (nextDirection == ScrollController.NONE
                            || reachableBoundaryPosition(landingCollision, landingX, landingY, nextDirection) != null);
        });
        assertNotNull(
                exit,
                "No sword-accessible collision path from ("
                        + link.pixelX() + "," + link.pixelY() + ") toward " + direction
                        + "\n" + collisionGrid(collision));
        link.setPixelPosition(exit[0], exit[1]);
        // Gameplay publishes the destination Link pose before the room-load
        // CreateFollowingNpcEntity call. This low-level harness must do so too.
        int arrivalX = direction == ScrollController.LEFT ? 140 : direction == ScrollController.RIGHT ? 3 : exit[0];
        int arrivalY = direction == ScrollController.UP ? 111 : direction == ScrollController.DOWN ? 0 : exit[1];
        session.story()
                .setFollowingNpcState(
                        session.story().followingNpcState(), arrivalX + 8, arrivalY + 16, 0, 0, link.direction());
        crossOverworldBoundary(coordinator, scroll, link, direction);
    }

    static void walkToAndCrossIndoorBoundary(
            RoomTransitionCoordinator coordinator,
            TransitionController transition,
            ScrollController scroll,
            OverworldCollision collision,
            Link link,
            int direction) {
        List<int[]> path = reachableBoundaryPath(collision, link.pixelX(), link.pixelY(), direction);
        assertNotNull(path, "No collision path to indoor boundary " + direction + "\n" + collisionGrid(collision));
        for (int[] position : path) {
            link.setPixelPosition(position[0], position[1]);
            coordinator.handleWarpAndIndoorBoundaries(link);
            assertFalse(transition.isActive(), "collision path crossed an active indoor warp");
            assertFalse(scroll.isActive(), "collision path left the room before its target boundary");
        }
        int[] exit = path.getLast();
        link.setPixelPosition(exit[0], exit[1]);
        switch (direction) {
            case ScrollController.UP -> link.setPixelPosition(link.pixelX(), -5);
            case ScrollController.DOWN -> link.setPixelPosition(link.pixelX(), RoomConstants.ROOM_PIXEL_HEIGHT);
            case ScrollController.LEFT -> link.setPixelPosition(-5, link.pixelY());
            case ScrollController.RIGHT -> link.setPixelPosition(RoomConstants.ROOM_PIXEL_WIDTH, link.pixelY());
            default -> throw new IllegalArgumentException("Unknown indoor scroll direction: " + direction);
        }
        coordinator.handleWarpAndIndoorBoundaries(link);
        assertTrue(scroll.isActive());
        assertEquals(direction, scroll.direction());
        switch (direction) {
            case ScrollController.UP -> assertEquals(RoomConstants.ROOM_PIXEL_HEIGHT - Link.SPRITE_SIZE, link.pixelY());
            case ScrollController.DOWN -> assertEquals(0, link.pixelY());
            case ScrollController.LEFT -> assertEquals(
                    RoomConstants.ROOM_PIXEL_WIDTH - Link.SPRITE_SIZE, link.pixelX());
            case ScrollController.RIGHT -> assertEquals(0, link.pixelX());
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
        for (int frame = 0; frame < 64 && link.isAirborne(); frame++) link.update();
        assertFalse(link.isAirborne());
        List<int[]> path = reachableBoundaryPath(collision, link.pixelX(), link.pixelY(), ScrollController.DOWN);
        assertNotNull(path, "No collision path to indoor front door\n" + collisionGrid(collision));
        for (int[] position : path) {
            link.setPixelPosition(position[0], position[1]);
            coordinator.handleWarpAndIndoorBoundaries(link);
            assertFalse(transition.isActive(), "collision path crossed the front-door boundary early");
        }
        int[] exit = path.getLast();
        link.setPixelPosition(exit[0], RoomConstants.ROOM_PIXEL_HEIGHT);
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
        List<int[]> path =
                reachableBoundaryPathWithFeather(collision, romTables, link.pixelX(), link.pixelY(), direction);
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
        link.setPixelPosition(exit[0], exit[1]);
        switch (direction) {
            case ScrollController.UP -> link.setPixelPosition(link.pixelX(), -5);
            case ScrollController.DOWN -> link.setPixelPosition(link.pixelX(), RoomConstants.ROOM_PIXEL_HEIGHT);
            case ScrollController.LEFT -> link.setPixelPosition(-5, link.pixelY());
            case ScrollController.RIGHT -> link.setPixelPosition(RoomConstants.ROOM_PIXEL_WIDTH, link.pixelY());
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
                link.setPixelPosition(step[0], step[1]);
            } else {
                int encodedMasks = action - 4;
                int firstMask = encodedMasks & 0x0F;
                int secondMask = (encodedMasks >>> 4) & 0x0F;
                int thirdMask = (encodedMasks >>> 8) & 0x0F;
                setDirectionalInput(inputState, inputConfig, firstMask, GLFW_PRESS);
                int jumpStartX = link.pixelX();
                int jumpStartY = link.pixelY();
                itemRegistry.lookup(playerState.itemA()).onPress();
                assertTrue(link.isAirborne(), "Feather did not start at (" + jumpStartX + "," + jumpStartY + ")");
                boolean crossedPit = false;
                int deadline = 0x40;
                int jumpFrame = 0;
                while (link.isAirborne() && deadline-- > 0) {
                    if (jumpFrame == 11) {
                        setDirectionalInput(inputState, inputConfig, firstMask, GLFW_RELEASE);
                        setDirectionalInput(inputState, inputConfig, secondMask, GLFW_PRESS);
                    } else if (jumpFrame == 22) {
                        setDirectionalInput(inputState, inputConfig, secondMask, GLFW_RELEASE);
                        setDirectionalInput(inputState, inputConfig, thirdMask, GLFW_PRESS);
                    }
                    link.update();
                    crossedPit |= collision.linkOnNormalPit(link.pixelX(), link.pixelY());
                    jumpFrame++;
                }
                setDirectionalInput(inputState, inputConfig, firstMask, GLFW_RELEASE);
                setDirectionalInput(inputState, inputConfig, secondMask, GLFW_RELEASE);
                setDirectionalInput(inputState, inputConfig, thirdMask, GLFW_RELEASE);
                assertTrue(
                        crossedPit,
                        "planned jump " + firstMask + "/"
                                + secondMask + "/" + thirdMask + " from (" + jumpStartX
                                + "," + jumpStartY + ") landed at (" + link.pixelX() + ","
                                + link.pixelY() + "), expected (" + step[0] + "," + step[1]
                                + ")\n" + collisionGrid(collision));
                assertFalse(link.isAirborne());
                assertFalse(link.isFallingIntoPit());
                assertFalse(collision.linkOnNormalPit(link.pixelX(), link.pixelY()));
                // The planner models integer pixels; Link retains subpixel
                // inertia during the real jump and can land one pixel beyond
                // that modelled point without changing the collision cell.
                assertTrue(
                        Math.abs(step[0] - link.pixelX()) <= 1,
                        "jump " + firstMask + "/" + secondMask + "/" + thirdMask + " from " + jumpStartX + ","
                                + jumpStartY + " expected " + step[0] + "," + step[1] + " actual " + link.pixelX() + ","
                                + link.pixelY());
                assertTrue(Math.abs(step[1] - link.pixelY()) <= 1);
                link.update();
                assertFalse(link.isFallingIntoPit());
                jumpCount++;
            }
            coordinator.handleWarpAndIndoorBoundaries(link);
            assertFalse(transition.isActive());
            assertFalse(scroll.isActive());
        }
        return jumpCount;
    }
}
