package linksawakening.world.room;

import static linksawakening.world.room.RoomSession.*;

import linksawakening.entity.Link;
import linksawakening.physics.OverworldCollision;
import linksawakening.world.object.OverworldBushInteraction;
import linksawakening.world.transition.Warp;

final class RoomLiftInteractions {
    private final Dependencies dependencies;

    RoomLiftInteractions(Dependencies dependencies) {
        this.dependencies = dependencies;
    }

    boolean tryLiftIndoorObject(
            int linkPixelX, int linkPixelY, int linkDirection, boolean braceletButtonHeld, int pressedButtonsMask) {
        return dependencies
                .roomDoorInteractions()
                .get()
                .tryLiftIndoorObject(
                        linkPixelX, linkPixelY, linkDirection, braceletButtonHeld, pressedButtonsMask, false);
    }

    boolean tryLiftIndoorObject(
            int linkPixelX,
            int linkPixelY,
            int linkDirection,
            boolean braceletButtonHeld,
            int pressedButtonsMask,
            boolean pieceOfPowerActive) {
        return dependencies
                .owner()
                .get()
                .tryLiftObject(
                        linkPixelX,
                        linkPixelY,
                        linkDirection,
                        braceletButtonHeld,
                        pressedButtonsMask,
                        pieceOfPowerActive,
                        1);
    }

    boolean tryLiftObject(
            int linkPixelX,
            int linkPixelY,
            int linkDirection,
            boolean braceletButtonHeld,
            int pressedButtonsMask,
            boolean pieceOfPowerActive,
            int braceletLevel) {
        return dependencies
                .owner()
                .get()
                .tryLiftObject(
                        linkPixelX,
                        linkPixelY,
                        linkDirection,
                        braceletButtonHeld,
                        pressedButtonsMask,
                        pieceOfPowerActive,
                        braceletLevel,
                        pieceOfPowerActive);
    }

    boolean tryLiftObject(
            int linkPixelX,
            int linkPixelY,
            int linkDirection,
            boolean braceletButtonHeld,
            int pressedButtonsMask,
            boolean pieceOfPowerActive,
            int braceletLevel,
            boolean anyPowerUpActive) {
        if (dependencies.location().activeRoom == null
                || dependencies.entityBridge().get().entityRuntime == null
                || dependencies.location().activeRoom.mapCategory() == Warp.CATEGORY_SIDESCROLL
                || !braceletButtonHeld
                || dependencies
                        .entityBridge()
                        .get()
                        .entityRuntime
                        .inspection()
                        .liftedEntityState()
                        .active()) {
            dependencies.puzzles().indoorPotPullFrames = 0;
            return false;
        }
        var target = probeLiftTarget(linkPixelX, linkPixelY, linkDirection);
        return advanceLift(
                new LiftRequest(
                        linkPixelX,
                        linkPixelY,
                        linkDirection,
                        pressedButtonsMask,
                        pieceOfPowerActive,
                        braceletLevel,
                        anyPowerUpActive),
                target);
    }

    record Dependencies(
            java.util.function.Supplier<RoomEntityBridge> entityBridge,
            RoomFollowerState followers,
            RoomLocationState location,
            OverworldBushInteraction overworldBushInteraction,
            OverworldCollision overworldCollision,
            RoomPuzzleState puzzles,
            java.util.function.Supplier<RoomDoorInteractions> roomDoorInteractions,
            java.util.function.Supplier<RoomObjectMutations> roomObjectMutations,
            java.util.function.Supplier<RoomSessionViews> roomSessionViews,
            java.util.function.Supplier<RoomTileRedraw> roomTileRedraw,
            java.util.function.Supplier<RoomDoorInteractions> owner) {}

    private LiftTarget probeLiftTarget(int linkPixelX, int linkPixelY, int linkDirection) {
        int oppositeButton =
                switch (linkDirection) {
                    case Link.DIRECTION_UP -> 0x08;
                    case Link.DIRECTION_DOWN -> 0x04;
                    case Link.DIRECTION_LEFT -> 0x01;
                    case Link.DIRECTION_RIGHT -> 0x02;
                    default -> 0;
                };
        int swordAreaX =
                switch (linkDirection) {
                    case Link.DIRECTION_RIGHT -> 0x0C;
                    case Link.DIRECTION_LEFT -> 0x03;
                    case Link.DIRECTION_UP, Link.DIRECTION_DOWN -> 0x08;
                    default -> 0;
                };
        int swordAreaY =
                switch (linkDirection) {
                    case Link.DIRECTION_RIGHT, Link.DIRECTION_LEFT -> 0x0A;
                    case Link.DIRECTION_UP -> 0x05;
                    case Link.DIRECTION_DOWN -> 0x10;
                    default -> 0;
                };
        int objectLeft = (linkPixelX + swordAreaX) & 0xF0;
        int objectTop = (linkPixelY + swordAreaY) & 0xF0;
        int touchedLocation = objectTop | (objectLeft >>> 4);
        int touchedObject = dependencies.roomSessionViews().get().objectAtRoomLocation(touchedLocation);
        boolean outdoor = dependencies.location().activeRoom.mapCategory() == Warp.CATEGORY_OVERWORLD;
        // Statue standing handler, $19:4076: asymmetric 32 by 48 contact window.
        linksawakening.world.entity.RoomEntity statue = null;
        if (!outdoor) {
            for (var entity : dependencies
                    .entityBridge()
                    .get()
                    .entityRuntime
                    .inspection()
                    .snapshot()
                    .loadedEntities()) {
                if (entity.type() == 0x9D
                        && entity.status() == linksawakening.world.entity.EntityStatus.ACTIVE
                        && (((linkPixelX + 8) - entity.x() + 0x10) & 0xFF) < 0x20
                        && (((linkPixelY + 16) - entity.y() + 0x20) & 0xFF) < 0x30) {
                    statue = entity;
                    break;
                }
            }
        }

        return new LiftTarget(oppositeButton, touchedLocation, touchedObject, outdoor, statue);
    }

    private boolean advanceLift(LiftRequest request, LiftTarget target) {
        if (target.statue() == null
                && target.touchedObject() != OBJECT_LIFTABLE_POT
                && !(target.outdoor() && target.touchedObject() == 0x5C)
                && target.touchedObject() != OBJECT_POT_WITH_SWITCH) {
            dependencies.puzzles().indoorPotPullFrames = 0;
            return false;
        }
        if (target.oppositeButton() == 0
                || (request.pressedButtonsMask() & target.oppositeButton()) == 0
                || (target.statue() != null && request.braceletLevel() != 2)) {
            dependencies.puzzles().indoorPotPullFrames = 0;
            return true;
        }
        dependencies.followers().followingEntityYOffset = request.linkDirection() == Link.DIRECTION_UP ? 4 : 0;
        dependencies.puzzles().indoorPotPullFrames++;
        int pullFrames = (target.statue() != null ? request.anyPowerUpActive() : request.pieceOfPowerActive())
                ? 0x03
                : INDOOR_POT_PULL_FRAMES;
        if (dependencies.puzzles().indoorPotPullFrames < pullFrames) {
            return true;
        }
        dependencies.puzzles().indoorPotPullFrames = 0;

        if (target.statue() != null) {
            completeStatueLift(target.statue(), request.linkDirection());
            return true;
        }
        replaceLiftedObject(target);
        spawnLiftedObject(request, target);
        return true;
    }

    private void completeStatueLift(linksawakening.world.entity.RoomEntity statue, int linkDirection) {

        dependencies
                .entityBridge()
                .get()
                .entityRuntime
                .beginLift(statue.slot(), RoomInteractionGeometry.romDirectionForProjectileCollision(linkDirection));
        dependencies
                .entityBridge()
                .get()
                .outbox
                .pendingRoomEntityEvents
                .add(new linksawakening.world.entity.event.EntityCombatEvent(
                        statue.slot(),
                        0x9D,
                        0,
                        false,
                        linksawakening.world.entity.event.EntityCombatEvent.SoundChannel.WAVE,
                        2));
        dependencies
                .location()
                .activeRoom
                .replaceEntities(dependencies
                        .entityBridge()
                        .get()
                        .entityRuntime
                        .inspection()
                        .snapshot());
    }

    private void replaceLiftedObject(LiftTarget target) {
        if (target.outdoor()) {
            if (target.touchedObject() == 0x5C) {
                dependencies
                        .overworldBushInteraction()
                        .revealObjectAtLocation(
                                target.touchedLocation(),
                                dependencies.location().activeRoom.roomId(),
                                true,
                                dependencies.location().activeRoom.roomObjectsArea(),
                                dependencies.location().activeRoom.renderValues(),
                                dependencies.location().activeRoom.gbcOverlay(),
                                dependencies.location().activeRoom.tileIds(),
                                dependencies.location().activeRoom.tileAttrs());
            } else {
                int room = dependencies.location().activeRoom.roomId();
                int replacement =
                        room < 0x20 || room == 0xE0 || room == 0xE1 || room == 0xE3 || room == 0xE4 || (room == 0x75)
                                ? 3
                                : 4;
                if (room == 0x52 || room == 0x04) replacement = 0xC6;
                dependencies.roomObjectMutations().get().writeBombPuzzleObject(target.touchedLocation(), replacement);
            }
            dependencies.roomObjectMutations().get().refreshOverworldCollisionAfterObjectMutation();
        } else {
            dependencies.location()
                            .activeRoom
                            .roomObjectsArea()[RoomConstants.ROOM_OBJECTS_BASE + target.touchedLocation()] =
                    target.touchedObject() == OBJECT_POT_WITH_SWITCH ? OBJECT_SWITCH_BUTTON : 0x0D;
            dependencies.roomTileRedraw().get().refreshActiveRoomTilemap();
            dependencies
                    .overworldCollision()
                    .setRoom(dependencies.location().activeRoom.roomObjectsArea());
            dependencies.overworldCollision().setGbcOverlay(null);
        }
    }

    private void spawnLiftedObject(LiftRequest request, LiftTarget target) {
        int x = request.linkPixelX() + 0x08;
        int y = request.linkPixelY() + 0x10;
        dependencies
                .entityBridge()
                .get()
                .entityRuntime
                .spawnLiftedRoomObject(
                        x,
                        y,
                        RoomInteractionGeometry.romDirectionForProjectileCollision(request.linkDirection()),
                        target.touchedObject() == 0x5C ? 1 : 0);
        dependencies
                .location()
                .activeRoom
                .replaceEntities(dependencies
                        .entityBridge()
                        .get()
                        .entityRuntime
                        .inspection()
                        .snapshot());
    }

    private record LiftTarget(
            int oppositeButton,
            int touchedLocation,
            int touchedObject,
            boolean outdoor,
            linksawakening.world.entity.RoomEntity statue) {}

    private record LiftRequest(
            int linkPixelX,
            int linkPixelY,
            int linkDirection,
            int pressedButtonsMask,
            boolean pieceOfPowerActive,
            int braceletLevel,
            boolean anyPowerUpActive) {}
}
