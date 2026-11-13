package linksawakening.world.room;

import static linksawakening.world.room.RoomSessionRules.INDOOR_POT_PULL_FRAMES;
import static linksawakening.world.room.RoomSessionRules.OBJECT_LIFTABLE_POT;
import static linksawakening.world.room.RoomSessionRules.OBJECT_POT_WITH_SWITCH;
import static linksawakening.world.room.RoomSessionRules.OBJECT_SWITCH_BUTTON;

import linksawakening.physics.OverworldCollision;
import linksawakening.state.Direction;
import linksawakening.world.object.OverworldBushInteraction;
import linksawakening.world.transition.Warp;

/** Bracelet target probing and pull countdown, including statue lifting ($19:4076). */
final class RoomLiftInteractions {
    private final Dependencies dependencies;

    RoomLiftInteractions(Dependencies dependencies) {
        this.dependencies = dependencies;
    }

    boolean tryLiftIndoorObject(
            RoomRuntime loaded,
            int linkPixelX,
            int linkPixelY,
            Direction linkDirection,
            boolean braceletButtonHeld,
            int pressedButtonsMask) {
        return dependencies
                .roomDoorInteractions()
                .tryLiftIndoorObject(
                        loaded, linkPixelX, linkPixelY, linkDirection, braceletButtonHeld, pressedButtonsMask, false);
    }

    boolean tryLiftIndoorObject(
            RoomRuntime loaded,
            int linkPixelX,
            int linkPixelY,
            Direction linkDirection,
            boolean braceletButtonHeld,
            int pressedButtonsMask,
            boolean pieceOfPowerActive) {
        return dependencies
                .owner()
                .tryLiftObject(
                        loaded,
                        linkPixelX,
                        linkPixelY,
                        linkDirection,
                        braceletButtonHeld,
                        pressedButtonsMask,
                        pieceOfPowerActive,
                        1);
    }

    boolean tryLiftObject(
            RoomRuntime loaded,
            int linkPixelX,
            int linkPixelY,
            Direction linkDirection,
            boolean braceletButtonHeld,
            int pressedButtonsMask,
            boolean pieceOfPowerActive,
            int braceletLevel) {
        return dependencies
                .owner()
                .tryLiftObject(
                        loaded,
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
            RoomRuntime loaded,
            int linkPixelX,
            int linkPixelY,
            Direction linkDirection,
            boolean braceletButtonHeld,
            int pressedButtonsMask,
            boolean pieceOfPowerActive,
            int braceletLevel,
            boolean anyPowerUpActive) {
        if (loaded.room.mapCategory() == Warp.CATEGORY_SIDESCROLL
                || !braceletButtonHeld
                || loaded.entities.inspection().liftedEntityState().active()) {
            loaded.contents.puzzles.indoorPotPullFrames = 0;
            return false;
        }
        var target = probeLiftTarget(loaded, linkPixelX, linkPixelY, linkDirection);
        return advanceLift(
                loaded,
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

    private void publishEntityView(RoomRuntime loaded) {
        loaded.room.replaceEntities(loaded.entities.inspection().view());
    }

    record Dependencies(
            RoomEntitySessionState entityRuntimeState,
            RoomFollowerState followers,
            OverworldBushInteraction overworldBushInteraction,
            OverworldCollision overworldCollision,
            RoomDoorInteractions roomDoorInteractions,
            RoomObjectMutations roomObjectMutations,
            RoomSessionViews roomSessionViews,
            RoomObjectMutations roomTileRedraw,
            RoomDoorInteractions owner) {
        Dependencies {
            java.util.Objects.requireNonNull(entityRuntimeState, "entityRuntimeState");
            java.util.Objects.requireNonNull(followers, "followers");
            java.util.Objects.requireNonNull(overworldBushInteraction, "overworldBushInteraction");
            java.util.Objects.requireNonNull(overworldCollision, "overworldCollision");
            java.util.Objects.requireNonNull(roomDoorInteractions, "roomDoorInteractions");
            java.util.Objects.requireNonNull(roomObjectMutations, "roomObjectMutations");
            java.util.Objects.requireNonNull(roomSessionViews, "roomSessionViews");
            java.util.Objects.requireNonNull(roomTileRedraw, "roomTileRedraw");
            java.util.Objects.requireNonNull(owner, "owner");
        }
    }

    private LiftTarget probeLiftTarget(RoomRuntime loaded, int linkPixelX, int linkPixelY, Direction linkDirection) {
        int oppositeButton =
                switch (linkDirection) {
                    case UP -> 0x08;
                    case DOWN -> 0x04;
                    case LEFT -> 0x01;
                    case RIGHT -> 0x02;
                    default -> 0;
                };
        int probeOffsetX =
                switch (linkDirection) {
                    case RIGHT -> 0x0C;
                    case LEFT -> 0x03;
                    case UP, DOWN -> 0x08;
                    default -> 0;
                };
        int probeOffsetY =
                switch (linkDirection) {
                    case RIGHT, LEFT -> 0x0A;
                    case UP -> 0x05;
                    case DOWN -> 0x10;
                    default -> 0;
                };
        int objectLeft = (linkPixelX + probeOffsetX) & 0xF0;
        int objectTop = (linkPixelY + probeOffsetY) & 0xF0;
        int touchedLocation = objectTop | (objectLeft >>> 4);
        int touchedObject = dependencies.roomSessionViews().objectAtRoomLocation(loaded.room, touchedLocation);
        boolean outdoor = loaded.room.mapCategory() == Warp.CATEGORY_OVERWORLD;
        // Statue standing handler, $19:4076: asymmetric 32 by 48 contact window.
        linksawakening.world.entity.RoomEntity statue = null;
        if (!outdoor) {
            for (var entity : loaded.entities.inspection().view().loadedEntities()) {
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

    private boolean advanceLift(RoomRuntime loaded, LiftRequest request, LiftTarget target) {
        if (target.statue() == null
                && target.touchedObject() != OBJECT_LIFTABLE_POT
                && !(target.outdoor() && target.touchedObject() == 0x5C)
                && target.touchedObject() != OBJECT_POT_WITH_SWITCH) {
            loaded.contents.puzzles.indoorPotPullFrames = 0;
            return false;
        }
        if (target.oppositeButton() == 0
                || (request.pressedButtonsMask() & target.oppositeButton()) == 0
                || (target.statue() != null && request.braceletLevel() != 2)) {
            loaded.contents.puzzles.indoorPotPullFrames = 0;
            return true;
        }
        dependencies.followers().followingEntityYOffset = request.linkDirection() == Direction.UP ? 4 : 0;
        loaded.contents.puzzles.indoorPotPullFrames++;
        int pullFrames = (target.statue() != null ? request.anyPowerUpActive() : request.pieceOfPowerActive())
                ? 0x03
                : INDOOR_POT_PULL_FRAMES;
        if (loaded.contents.puzzles.indoorPotPullFrames < pullFrames) {
            return true;
        }
        loaded.contents.puzzles.indoorPotPullFrames = 0;

        if (target.statue() != null) {
            completeStatueLift(loaded, target.statue(), request.linkDirection());
            return true;
        }
        replaceLiftedObject(loaded.contents, target);
        spawnLiftedObject(loaded, request, target);
        return true;
    }

    private void completeStatueLift(
            RoomRuntime loaded, linksawakening.world.entity.RoomEntity statue, Direction linkDirection) {

        loaded.entities.beginLift(
                statue.slot(), RoomInteractionGeometry.romDirectionForProjectileCollision(linkDirection));
        dependencies
                .entityRuntimeState()
                .outbox
                .pendingRoomEntityEvents
                .add(new linksawakening.world.entity.event.EntityCombatEvent(
                        statue.slot(),
                        0x9D,
                        0,
                        false,
                        linksawakening.world.entity.event.EntityCombatEvent.SoundChannel.WAVE,
                        2));
        publishEntityView(loaded);
    }

    private void replaceLiftedObject(RoomContents contents, LiftTarget target) {
        if (target.outdoor()) {
            if (target.touchedObject() == 0x5C) {
                dependencies
                        .overworldBushInteraction()
                        .revealObjectAtLocation(
                                target.touchedLocation(),
                                contents.room.roomId(),
                                true,
                                contents.room.roomObjectsArea(),
                                contents.room.renderValues(),
                                contents.room.gbcOverlay(),
                                contents.room.tileIds(),
                                contents.room.tileAttrs());
            } else {
                int room = contents.room.roomId();
                int replacement =
                        room < 0x20 || room == 0xE0 || room == 0xE1 || room == 0xE3 || room == 0xE4 || (room == 0x75)
                                ? 3
                                : 4;
                if (room == 0x52 || room == 0x04) replacement = 0xC6;
                dependencies
                        .roomObjectMutations()
                        .writeBombPuzzleObject(contents, target.touchedLocation(), replacement);
            }
            dependencies.roomObjectMutations().refreshOverworldCollisionAfterObjectMutation(contents);
        } else {
            contents.room.roomObjectsArea()[RoomConstants.ROOM_OBJECTS_BASE + target.touchedLocation()] =
                    target.touchedObject() == OBJECT_POT_WITH_SWITCH ? OBJECT_SWITCH_BUTTON : 0x0D;
            dependencies.roomTileRedraw().refreshActiveRoomTilemap(contents);
            dependencies.overworldCollision().setRoom(contents.room.roomObjectsArea());
            dependencies.overworldCollision().setGbcOverlay(null);
        }
    }

    private void spawnLiftedObject(RoomRuntime loaded, LiftRequest request, LiftTarget target) {
        int x = request.linkPixelX() + 0x08;
        int y = request.linkPixelY() + 0x10;
        loaded.entities.spawnLiftedRoomObject(
                x,
                y,
                RoomInteractionGeometry.romDirectionForProjectileCollision(request.linkDirection()),
                target.touchedObject() == 0x5C ? 1 : 0);
        publishEntityView(loaded);
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
            Direction linkDirection,
            int pressedButtonsMask,
            boolean pieceOfPowerActive,
            int braceletLevel,
            boolean anyPowerUpActive) {}
}
