package linksawakening.world.room;

import static linksawakening.world.room.RoomSession.*;

import linksawakening.physics.OverworldCollision;
import linksawakening.rom.RomTables;
import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.object.OverworldBushInteraction;
import linksawakening.world.transition.Warp;

final class RoomShovelActions {
    private final Dependencies dependencies;

    RoomShovelActions(Dependencies dependencies) {
        this.dependencies = dependencies;
    }

    ShovelStartResult startShovel(int linkEntityX, int linkEntityY, int romDirection, boolean linkAirborne) {
        if (romDirection < 0 || romDirection > 3) {
            throw new IllegalArgumentException("ROM shovel direction out of range: " + romDirection);
        }
        if (dependencies.location().activeRoom == null || linkAirborne || dependencies.items().shovelUseState != 0) {
            return new ShovelStartResult(false, true, -1);
        }
        ShovelProbe probe =
                dependencies.roomPlayerItemActions().get().probeShovel(linkEntityX, linkEntityY, romDirection);
        dependencies.items().shovelUseState = SHOVEL_STATE_ACTIVE;
        return new ShovelStartResult(true, !probe.valid(), probe.location());
    }

    void advanceShovel(int linkEntityX, int linkEntityY, int romDirection, int timer) {
        if (dependencies.location().activeRoom == null || dependencies.items().shovelUseState == 0) {
            return;
        }
        int normalizedTimer = timer & 0xFF;
        if (normalizedTimer == SHOVEL_DIG_TIMER) {
            ShovelProbe probe =
                    dependencies.roomPlayerItemActions().get().probeShovel(linkEntityX, linkEntityY, romDirection);
            if (!probe.valid()) {
                return;
            }
            dependencies.items().shovelUseState = SHOVEL_STATE_DUG;
            dependencies.roomPlayerItemActions().get().writeShovelHole(probe);
            dependencies.items().pendingShovelDrop = new PendingShovelDrop(
                    probe.objectLeft(),
                    probe.objectTop(),
                    linkEntityX,
                    linkEntityY,
                    dependencies.location().activeRoom.mapCategory() != Warp.CATEGORY_OVERWORLD
                            || dependencies.location().activeRoom.roomId() != 0x0E);
        } else if (normalizedTimer == SHOVEL_FINISH_TIMER) {
            if (dependencies.items().shovelUseState == SHOVEL_STATE_DUG
                    && dependencies.followers().followingNpcState.marinFollowing()) {
                dependencies
                        .entityBridge()
                        .get()
                        .outbox
                        .pendingRoomDialogRequests
                        .add(new DialogRequest(SHOVEL_DIALOG_TABLE, SHOVEL_DIALOG_ID));
            }
            dependencies.items().shovelUseState = 0;
        }
    }

    int shovelAnimationState(int javaDirection, int timer) {
        return dependencies
                .romTables()
                .shovelAnimationState(timer, RoomInteractionGeometry.romDirectionForProjectileCollision(javaDirection));
    }

    ShovelProbe probeShovel(int linkEntityX, int linkEntityY, int romDirection) {
        if (dependencies.location().activeRoom == null
                || dependencies.location().activeRoom.mapCategory() == Warp.CATEGORY_SIDESCROLL) {
            return ShovelProbe.invalid();
        }

        // func_002_4D20 subtracts Link's sprite-center/bottom origin before
        // masking to the room-object grid. These are hLinkPositionX/Y, not
        // Java's top-left pixel coordinates.
        int objectLeft = (linkEntityX + SHOVEL_TARGET_X[romDirection] - 0x08) & 0xF0;
        int objectTop = (linkEntityY + SHOVEL_TARGET_Y[romDirection] - 0x10) & 0xF0;
        int cellX = objectLeft >>> 4;
        int cellY = objectTop >>> 4;
        if (cellX < 0
                || cellX >= RoomConstants.OBJECTS_PER_ROW
                || cellY < 0
                || cellY >= RoomConstants.OBJECTS_PER_COLUMN) {
            return ShovelProbe.invalid();
        }

        int location = objectTop | cellX;
        int objectId = dependencies.roomSessionViews().get().objectAtRoomLocation(location);
        int physicsTable = dependencies.location().activeRoom.mapCategory() == Warp.CATEGORY_OVERWORLD
                ? RomTables.PHYSICS_TABLE_OVERWORLD
                : dependencies.location().activeRoom.mapId() == 0xFF
                        ? RomTables.PHYSICS_TABLE_INDOORS2
                        : RomTables.PHYSICS_TABLE_INDOORS1;
        boolean valid = dependencies.romTables().objectPhysicsFlag(physicsTable, objectId) == 0;
        if (dependencies.location().activeRoom.mapCategory() == Warp.CATEGORY_OVERWORLD) {
            valid &= objectId != OBJECT_SHOVEL_BLOCKER_A
                    && objectId != OBJECT_SHOVEL_BLOCKER_B
                    && objectId != OBJECT_SHOVEL_BLOCKER_C;
        } else {
            valid &= objectId == OBJECT_SHOVEL_INDOOR_DIGGABLE;
        }
        return new ShovelProbe(valid, location, objectLeft, objectTop);
    }

    void writeShovelHole(ShovelProbe probe) {
        int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + probe.location();
        int[] objects = dependencies.location().activeRoom.roomObjectsArea();
        if (areaIndex < 0 || areaIndex >= objects.length) {
            return;
        }
        objects[areaIndex] = OBJECT_SHOVEL_HOLE;
        if (dependencies.location().activeRoom.renderValues() != null
                && areaIndex < dependencies.location().activeRoom.renderValues().length) {
            dependencies.location().activeRoom.renderValues()[areaIndex] = OBJECT_SHOVEL_HOLE;
        }
        if (dependencies.location().activeRoom.mapCategory() == Warp.CATEGORY_OVERWORLD) {
            // On GBC the mutable WRAM2 overlay is the render source for both
            // tile ids and attributes; rebuilding from the immutable ROM
            // overlay here would immediately undo the hole.
            dependencies
                    .overworldBushInteraction()
                    .refreshRoomObjectCell(
                            dependencies.location().activeRoom.roomId(),
                            probe.location(),
                            objects,
                            dependencies.location().activeRoom.renderValues(),
                            dependencies.location().activeRoom.gbcOverlay(),
                            dependencies.location().activeRoom.tileIds(),
                            dependencies.location().activeRoom.tileAttrs());
            dependencies.roomObjectMutations().get().refreshOverworldCollisionAfterObjectMutation();
        } else {
            dependencies.roomTileRedraw().get().refreshActiveRoomTilemap();
            dependencies
                    .overworldCollision()
                    .setRoom(dependencies.location().activeRoom.roomObjectsArea());
        }
    }

    record Dependencies(
            java.util.function.Supplier<RoomEntityBridge> entityBridge,
            RoomFollowerState followers,
            RoomItemUseState items,
            RoomLocationState location,
            OverworldBushInteraction overworldBushInteraction,
            OverworldCollision overworldCollision,
            RomTables romTables,
            java.util.function.Supplier<RoomObjectMutations> roomObjectMutations,
            java.util.function.Supplier<RoomPlayerItemActions> roomPlayerItemActions,
            java.util.function.Supplier<RoomSessionViews> roomSessionViews,
            java.util.function.Supplier<RoomTileRedraw> roomTileRedraw) {}
}
