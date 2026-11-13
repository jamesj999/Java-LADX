package linksawakening.world.room;

import static linksawakening.world.room.RoomConstants.ROOM_PIXEL_HEIGHT;
import static linksawakening.world.room.RoomConstants.ROOM_PIXEL_WIDTH;
import static linksawakening.world.room.RoomSessionRules.ROOM_STATUS_CHEST_OPEN;

import linksawakening.physics.OverworldCollision;
import linksawakening.rom.Rom;
import linksawakening.state.Direction;
import linksawakening.world.transition.Warp;

final class RoomChestInteractions {
    private final Dependencies dependencies;

    RoomChestInteractions(Dependencies dependencies) {
        this.dependencies = dependencies;
    }

    ChestOpenResult tryOpenChest(
            int linkPixelX, int linkPixelY, Direction linkDirection, boolean actionPressed, int swordLevel) {
        if (!actionPressed
                || linkDirection != Direction.UP
                || dependencies.location().activeRoom == null
                || dependencies.entityRuntimeState().runtime == null) {
            return new ChestOpenResult(false, -1, -1, -1);
        }
        if (linkPixelX < 0 || linkPixelY < 0 || linkPixelX >= ROOM_PIXEL_WIDTH || linkPixelY >= ROOM_PIXEL_HEIGHT) {
            return new ChestOpenResult(false, -1, -1, -1);
        }
        int column = Math.floorDiv(
                linkPixelX + (dependencies.romData().u8(linksawakening.rom.RomAddress.fromOffset(0x1F4B))), 0x10);
        int row = Math.floorDiv(
                linkPixelY + (dependencies.romData().u8(linksawakening.rom.RomAddress.fromOffset(0x1F4F))), 0x10);
        if (column < 0
                || column >= RoomConstants.OBJECTS_PER_ROW
                || row < 0
                || row >= RoomConstants.OBJECTS_PER_COLUMN) {
            return new ChestOpenResult(false, -1, -1, -1);
        }
        int location = (row << 4) | column;
        if (dependencies.roomSessionViews().objectAtRoomLocation(location) != 0xA0) {
            return new ChestOpenResult(false, -1, -1, location);
        }
        int itemType = dependencies
                .chestContentsTable()
                .itemForSpawn(
                        dependencies.location().activeRoom.mapId(),
                        dependencies.location().activeRoom.roomId(),
                        swordLevel,
                        dependencies.location().activeRoom.mapCategory() != Warp.CATEGORY_OVERWORLD);
        int slot = dependencies.entityRuntimeState().runtime.spawnChestWithItem(column << 4, row << 4, itemType);
        if (slot < 0) {
            return new ChestOpenResult(false, -1, -1, location);
        }

        dependencies.roomObjectMutations().writeBombPuzzleObject(location, 0xA1);
        byte[] status = dependencies.location().activeRoom.mapCategory() == Warp.CATEGORY_OVERWORLD
                ? dependencies.roomState().overworldRoomStatus
                : dependencies
                        .roomSessionViews()
                        .indoorStatusTableForMap(
                                dependencies.location().activeRoom.mapId());
        status[dependencies.location().activeRoom.roomId()] |= (byte) ROOM_STATUS_CHEST_OPEN;
        // writeBombPuzzleObject already updates the live GBC cell. Rebuilding
        // from the ROM overlay would discard other live mutations (cut bushes).
        if (dependencies.location().activeRoom.mapCategory() != Warp.CATEGORY_OVERWORLD) {
            dependencies.roomTileRedraw().refreshActiveRoomTilemap();
        }
        dependencies
                .overworldCollision()
                .setRoom(dependencies.location().activeRoom.roomObjectsArea());
        dependencies
                .overworldCollision()
                .setGbcOverlay(
                        dependencies.location().activeRoom.mapCategory() == Warp.CATEGORY_OVERWORLD
                                ? dependencies.location().activeRoom.gbcOverlay()
                                : null);
        dependencies
                .location()
                .activeRoom
                .replaceEntities(
                        dependencies.entityRuntimeState().runtime.inspection().view());
        return new ChestOpenResult(true, itemType, slot, location);
    }

    record Dependencies(
            ChestContentsTable chestContentsTable,
            RoomEntityFrameState entityRuntimeState,
            RoomLocationState location,
            OverworldCollision overworldCollision,
            Rom romData,
            RoomObjectMutations roomObjectMutations,
            RoomSessionViews roomSessionViews,
            RoomWorldState roomState,
            RoomObjectMutations roomTileRedraw) {
        Dependencies {
            java.util.Objects.requireNonNull(chestContentsTable, "chestContentsTable");
            java.util.Objects.requireNonNull(entityRuntimeState, "entityRuntimeState");
            java.util.Objects.requireNonNull(location, "location");
            java.util.Objects.requireNonNull(overworldCollision, "overworldCollision");
            java.util.Objects.requireNonNull(romData, "romData");
            java.util.Objects.requireNonNull(roomObjectMutations, "roomObjectMutations");
            java.util.Objects.requireNonNull(roomSessionViews, "roomSessionViews");
            java.util.Objects.requireNonNull(roomState, "roomState");
            java.util.Objects.requireNonNull(roomTileRedraw, "roomTileRedraw");
        }
    }
}
