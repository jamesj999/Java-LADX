package linksawakening.world.room;

import static linksawakening.world.room.RoomSession.*;

import linksawakening.physics.OverworldCollision;
import linksawakening.rom.RomBank;
import linksawakening.world.transition.Warp;

final class RoomShutterController {
    private final Dependencies dependencies;

    RoomShutterController(Dependencies dependencies) {
        this.dependencies = dependencies;
    }

    boolean tickShutterClosing() {
        if (!dependencies.puzzles().shutterCloseStarted || dependencies.location().activeRoom == null) return false;
        if (dependencies.puzzles().shutterCloseDirection < 0) {
            if (dependencies.puzzles().pendingShutterCloseMask == 0) return false;
            dependencies.puzzles().shutterCloseDirection =
                    Integer.numberOfTrailingZeros(dependencies.puzzles().pendingShutterCloseMask);
            dependencies.puzzles().pendingShutterCloseMask &= ~(1 << dependencies.puzzles().shutterCloseDirection);
            dependencies.puzzles().shutterCloseLocation = new RoomObjectParser(dependencies.romData())
                    .indoorShutterLocation(
                            dependencies.location().activeRoom.roomObjectsArea(),
                            dependencies.puzzles().shutterCloseDirection);
            dependencies.puzzles().shutterCloseTicks = 0;
            return true;
        }
        dependencies.entityFrame().worldLinkMotionBlockPending = true;
        if (dependencies.puzzles().shutterCloseLocation >= 0) {
            int direction = dependencies.puzzles().shutterCloseDirection;
            int left = (dependencies.puzzles().shutterCloseLocation & 15) * 16
                    + Byte.toUnsignedInt(dependencies.romData()[RomBank.romOffset(2, 0x5BE0 + direction + 4)]);
            int top = (dependencies.puzzles().shutterCloseLocation >>> 4) * 16;
            int source = RomBank.romOffset(2, 0x5BC4 + direction * 4);
            int rowOffset = RomBank.romOffset(2, 0x5BE8 + direction + 4);
            for (int row = 0; row < 2; row++) {
                int y = top + Byte.toUnsignedInt(dependencies.romData()[rowOffset + row * 4]);
                int index = (y / 8) * RoomConstants.ROOM_TILE_WIDTH + left / 8;
                for (int col = 0; col < 2; col++) {
                    int tile = Byte.toUnsignedInt(dependencies.romData()[source + row * 2 + col]);
                    if (index + col < dependencies.location().activeRoom.tileIds().length) {
                        dependencies.location().activeRoom.tileIds()[index + col] = tile;
                        dependencies.puzzles().shutterTileOverrides.put(index + col, tile);
                    }
                }
            }
        }
        if (++dependencies.puzzles().shutterCloseTicks < 8) return true;
        int direction = dependencies.puzzles().shutterCloseDirection;
        dependencies.puzzles().shutterTileOverrides.clear();
        new RoomObjectParser(dependencies.romData())
                .applyIndoorShutterClosedState(
                        dependencies.location().activeRoom.roomObjectsArea(), 1 << direction);
        dependencies.puzzles().closedShutterMask |= 1 << direction;
        // $02:5D21 uses indoor A even for indoor B (the original's bank selection).
        byte[] status = dependencies.location().activeRoom.mapId() == 0xFF
                ? dependencies.roomState().colorDungeonRoomStatus
                : dependencies.roomState().indoorARoomStatus;
        status[dependencies.location().activeRoom.roomId()] &= (byte) ~KEY_DOOR_CURRENT_STATUS[direction];
        dependencies.puzzles().shutterCloseDirection = dependencies.puzzles().shutterCloseLocation = -1;
        dependencies.roomTileRedraw().get().refreshActiveRoomTilemap();
        dependencies
                .overworldCollision()
                .setRoom(dependencies.location().activeRoom.roomObjectsArea());
        return true;
    }

    void openActiveRoomShutterDoors() {
        dependencies.roomPuzzleController().get().openActiveRoomShutterDoors(false);
    }

    void openActiveRoomShutterDoors(boolean persistDirectionalStatus) {
        dependencies.puzzles().pendingShutterCloseMask = dependencies.puzzles().closedShutterMask = 0;
        dependencies.puzzles().shutterCloseDirection = dependencies.puzzles().shutterCloseLocation = -1;
        dependencies.puzzles().shutterTileOverrides.clear();
        if (dependencies.location().activeRoom == null) {
            return;
        }
        int shutterMask = dependencies.location().activeRoom.shutterDoorMask();
        if (shutterMask != 0 && dependencies.location().activeRoom.mapCategory() != Warp.CATEGORY_OVERWORLD) {
            RoomObjectParser parser = new RoomObjectParser(dependencies.romData());
            int[] openDoorTypes = {0xF4, 0xF5, 0xF6, 0xF7};
            for (int direction = 0; direction < openDoorTypes.length; direction++) {
                if ((shutterMask & (1 << direction)) != 0) {
                    int location = parser.indoorShutterLocation(
                            dependencies.location().activeRoom.roomObjectsArea(), direction);
                    if (location < 0) {
                        continue;
                    }
                    parser.applyIndoorDoorMacro(
                            dependencies.location().activeRoom.roomObjectsArea(), openDoorTypes[direction], location);
                    if (persistDirectionalStatus) {
                        byte[] status = dependencies
                                .roomSessionViews()
                                .get()
                                .indoorStatusTableForMap(
                                        dependencies.location().activeRoom.mapId());
                        status[dependencies.location().activeRoom.roomId()] |=
                                (byte) KEY_DOOR_CURRENT_STATUS[direction];
                        int adjacentRoom =
                                dependencies.roomSessionViews().get().adjacentIndoorRoomIdForBombWall(direction);
                        status[adjacentRoom] |= (byte) KEY_DOOR_ADJACENT_STATUS[direction];
                    }
                }
            }
            dependencies.roomTileRedraw().get().refreshActiveRoomTilemap();
            dependencies
                    .overworldCollision()
                    .setRoom(dependencies.location().activeRoom.roomObjectsArea());
            dependencies.overworldCollision().setGbcOverlay(null);
        }
        dependencies.location().activeRoom.openShutterDoors();
    }

    record Dependencies(
            RoomEntityFrameState entityFrame,
            RoomLocationState location,
            OverworldCollision overworldCollision,
            RoomPuzzleState puzzles,
            byte[] romData,
            java.util.function.Supplier<RoomPuzzleController> roomPuzzleController,
            java.util.function.Supplier<RoomSessionViews> roomSessionViews,
            RoomWorldState roomState,
            java.util.function.Supplier<RoomTileRedraw> roomTileRedraw) {}
}
