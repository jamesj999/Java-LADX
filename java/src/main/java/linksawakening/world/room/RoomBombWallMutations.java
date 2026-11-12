package linksawakening.world.room;

import static linksawakening.world.room.RoomSessionRules.*;

import linksawakening.physics.OverworldCollision;
import linksawakening.rom.RomTables;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.object.BombObjectInteraction;
import linksawakening.world.object.OverworldBushInteraction;

final class RoomBombWallMutations {
    private final Dependencies dependencies;

    RoomBombWallMutations(Dependencies dependencies) {
        this.dependencies = dependencies;
    }

    void applyOutdoorBombableCaveDoorCandidate(BombObjectInteraction.Candidate candidate, int sourceSlot) {
        int objectId = dependencies.roomSessionViews().get().objectAtRoomLocation(candidate.location());
        int doorIndex = dependencies.romTables().objectPhysicsFlag(RomTables.PHYSICS_TABLE_OVERWORLD, objectId)
                - BOMBABLE_WALL_PHYSICS_BASE;
        if (objectId != OBJECT_BOMBABLE_CAVE_DOOR || doorIndex < 0 || doorIndex >= 4) {
            return;
        }

        int areaIndex = RoomConstants.ROOM_OBJECTS_BASE
                + (candidate.objectTop() & 0xF0)
                + ((candidate.objectLeft() & 0xF0) >>> 4);
        int[] objects = dependencies.location().activeRoom.roomObjectsArea();
        if (areaIndex < 0 || areaIndex >= objects.length) {
            return;
        }

        dependencies.roomObjectMutations().get().writeBombPuzzleObject(candidate.location(), OBJECT_ROCKY_CAVE_DOOR);
        dependencies.terrain().bombedCaveDoorTileOverrides.add(areaIndex);
        dependencies.roomState()
                .overworldRoomStatus[dependencies.location().activeRoom.roomId()] |= (byte) OW_ROOM_STATUS_OPENED;
        dependencies.roomObjectMutations().get().queuePuzzleSolvedJingle(sourceSlot);
        dependencies.roomTileRedraw().get().refreshActiveRoomTilemap();
        dependencies.roomObjectMutations().get().refreshOverworldCollisionAfterObjectMutation();
    }

    void writeBombPuzzleObject(int location, int objectId) {
        int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + (location & 0xF0) + (location & 0x0F);
        int[] objects = dependencies.location().activeRoom.roomObjectsArea();
        if (areaIndex < 0 || areaIndex >= objects.length) {
            return;
        }
        objects[areaIndex] = objectId & 0xFF;
        if (dependencies.location().activeRoom.renderValues() != null) {
            dependencies.location().activeRoom.renderValues()[areaIndex] = objectId & 0xFF;
        }
        dependencies
                .overworldBushInteraction()
                .refreshRoomObjectCell(
                        dependencies.location().activeRoom.roomId(),
                        location,
                        objects,
                        dependencies.location().activeRoom.renderValues(),
                        dependencies.location().activeRoom.gbcOverlay(),
                        dependencies.location().activeRoom.tileIds(),
                        dependencies.location().activeRoom.tileAttrs());
    }

    void refreshOverworldCollisionAfterObjectMutation() {
        dependencies
                .overworldCollision()
                .setRoom(dependencies.location().activeRoom.roomObjectsArea());
        dependencies
                .overworldCollision()
                .setGbcOverlay(dependencies.location().activeRoom.gbcOverlay());
    }

    void applyIndoorBombableWallCandidate(BombObjectInteraction.Candidate candidate, int sourceSlot) {
        if (candidate == null) {
            return;
        }
        int location = candidate.location();
        int objectId = dependencies.roomSessionViews().get().objectAtRoomLocation(location);
        int physicsTable = dependencies.location().activeRoom.mapId() == 0xFF
                ? RomTables.PHYSICS_TABLE_INDOORS2
                : RomTables.PHYSICS_TABLE_INDOORS1;
        int wallIndex = dependencies.romTables().objectPhysicsFlag(physicsTable, objectId) - BOMBABLE_WALL_PHYSICS_BASE;
        if (wallIndex < 0 || wallIndex >= 4) {
            return;
        }

        int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + (location & 0xF0) + (location & 0x0F);
        int[] objects = dependencies.location().activeRoom.roomObjectsArea();
        if (areaIndex < 0 || areaIndex >= objects.length) {
            return;
        }
        objects[areaIndex] = wallIndex < 2 ? OBJECT_BOMBED_PASSAGE_VERTICAL : OBJECT_BOMBED_PASSAGE_HORIZONTAL;

        byte[] status = dependencies
                .roomSessionViews()
                .get()
                .indoorStatusTableForMap(dependencies.location().activeRoom.mapId());
        int currentStatus =
                switch (wallIndex) {
                    case 0 -> 0x04;
                    case 1 -> 0x08;
                    case 2 -> 0x02;
                    case 3 -> 0x01;
                    default -> throw new AssertionError(wallIndex);
                };
        int adjacentStatus =
                switch (wallIndex) {
                    case 0 -> 0x08;
                    case 1 -> 0x04;
                    case 2 -> 0x01;
                    case 3 -> 0x02;
                    default -> throw new AssertionError(wallIndex);
                };
        int adjacentRoom = dependencies.roomSessionViews().get().adjacentIndoorRoomIdForBombWall(wallIndex);
        status[dependencies.location().activeRoom.roomId()] |= (byte) currentStatus;
        status[adjacentRoom] |= (byte) adjacentStatus;
        dependencies.terrain().bombedWallTileOverrides.put(areaIndex, wallIndex < 2 ? 0 : 1);
        dependencies.roomObjectMutations().get().queuePuzzleSolvedJingle(sourceSlot);

        dependencies.roomTileRedraw().get().refreshActiveRoomTilemap();
        dependencies
                .overworldCollision()
                .setRoom(dependencies.location().activeRoom.roomObjectsArea());
        dependencies.overworldCollision().setGbcOverlay(null);
    }

    void queuePuzzleSolvedJingle(int sourceSlot) {
        dependencies
                .entityBridge()
                .get()
                .outbox
                .pendingRoomEntityEvents
                .add(new EntityCombatEvent(sourceSlot, 0x02, 0, false, EntityCombatEvent.SoundChannel.JINGLE, 0x02));
    }

    boolean applyIndoorBombableBlockCandidate(BombObjectInteraction.Candidate candidate) {
        if (candidate == null) {
            return false;
        }
        int location = candidate.location();
        if (dependencies.roomSessionViews().get().objectAtRoomLocation(location) != OBJECT_BOMBABLE_BLOCK) {
            return false;
        }
        int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + (location & 0xF0) + (location & 0x0F);
        int[] objects = dependencies.location().activeRoom.roomObjectsArea();
        if (areaIndex < 0 || areaIndex >= objects.length) {
            return false;
        }

        objects[areaIndex] = OBJECT_FLOOR_OD;
        dependencies
                        .roomSessionViews()
                        .get()
                        .indoorStatusTableForMap(
                                dependencies.location().activeRoom.mapId())[
                        dependencies.location().activeRoom.roomId()] |=
                (byte) INDOOR_ROOM_STATUS_EVENT_3;
        dependencies.terrain().bombedBlockTileOverrides.add(areaIndex);

        if (dependencies.entityBridge().get().entityRuntime != null) {
            dependencies
                    .entityBridge()
                    .get()
                    .entityRuntime
                    .spawnLiftableRockSmash(candidate.objectLeft() + 0x08, candidate.objectTop() + 0x10, 0x00);
        }

        dependencies.roomTileRedraw().get().refreshActiveRoomTilemap();
        dependencies
                .overworldCollision()
                .setRoom(dependencies.location().activeRoom.roomObjectsArea());
        dependencies.overworldCollision().setGbcOverlay(null);
        return true;
    }

    int colorShellObjectAt(RoomEntity entity, int relativeOffset) {
        if (dependencies.location().activeRoom == null || entity == null) {
            return 0xFF;
        }
        int index = RoomInteractionGeometry.colorShellObjectIndex(entity) + relativeOffset;
        int[] objects = dependencies.location().activeRoom.roomObjectsArea();
        return index >= 0 && index < objects.length ? objects[index] : 0xFF;
    }

    void writeColorShellObject(RoomEntity entity, int objectId) {
        if (dependencies.location().activeRoom == null || entity == null) {
            return;
        }
        int index = RoomInteractionGeometry.colorShellObjectIndex(entity);
        int[] objects = dependencies.location().activeRoom.roomObjectsArea();
        if (index < 0 || index >= objects.length) {
            return;
        }
        objects[index] = objectId & 0xFF;
        dependencies.roomTileRedraw().get().refreshActiveRoomTilemap();
    }

    record Dependencies(
            java.util.function.Supplier<RoomEntityBridge> entityBridge,
            RoomLocationState location,
            OverworldBushInteraction overworldBushInteraction,
            OverworldCollision overworldCollision,
            RomTables romTables,
            java.util.function.Supplier<RoomObjectMutations> roomObjectMutations,
            java.util.function.Supplier<RoomSessionViews> roomSessionViews,
            RoomWorldState roomState,
            java.util.function.Supplier<RoomTileRedraw> roomTileRedraw,
            RoomTerrainState terrain) {}
}
