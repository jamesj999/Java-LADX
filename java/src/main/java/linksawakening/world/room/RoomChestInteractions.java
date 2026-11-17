package linksawakening.world.room;

import static linksawakening.world.room.RoomConstants.ROOM_PIXEL_HEIGHT;
import static linksawakening.world.room.RoomConstants.ROOM_PIXEL_WIDTH;
import static linksawakening.world.room.RoomSessionRules.ROOM_STATUS_CHEST_OPEN;

import java.util.Objects;
import linksawakening.physics.OverworldCollision;
import linksawakening.rom.Rom;
import linksawakening.state.Direction;
import linksawakening.world.transition.Warp;

final class RoomChestInteractions {
    private final Dependencies dependencies;

    RoomChestInteractions(Dependencies dependencies) {
        this.dependencies = Objects.requireNonNull(dependencies, "dependencies");
    }

    ChestOpenResult tryOpenChest(
            RoomRuntime loaded,
            int linkPixelX,
            int linkPixelY,
            Direction linkDirection,
            boolean actionPressed,
            int swordLevel) {
        if (!actionPressed || linkDirection != Direction.UP) {
            return new ChestOpenResult(false, -1, -1, -1);
        }
        if (linkPixelX < 0 || linkPixelY < 0 || linkPixelX >= ROOM_PIXEL_WIDTH || linkPixelY >= ROOM_PIXEL_HEIGHT) {
            return new ChestOpenResult(false, -1, -1, -1);
        }
        int column = Math.floorDiv(
                linkPixelX + dependencies.romData().u8(linksawakening.rom.RomAddress.fromOffset(0x1F4B)), 0x10);
        int row = Math.floorDiv(
                linkPixelY + dependencies.romData().u8(linksawakening.rom.RomAddress.fromOffset(0x1F4F)), 0x10);
        if (column < 0
                || column >= RoomConstants.OBJECTS_PER_ROW
                || row < 0
                || row >= RoomConstants.OBJECTS_PER_COLUMN) {
            return new ChestOpenResult(false, -1, -1, -1);
        }
        int location = (row << 4) | column;
        if (dependencies.roomSessionViews().objectAtRoomLocation(loaded.room, location) != 0xA0) {
            return new ChestOpenResult(false, -1, -1, location);
        }
        int itemType = dependencies
                .chestContentsTable()
                .itemForSpawn(
                        loaded.room.mapId(),
                        loaded.room.roomId(),
                        swordLevel,
                        loaded.room.mapCategory() != Warp.CATEGORY_OVERWORLD);
        int slot = loaded.entities.spawns().spawnChestWithItem(column << 4, row << 4, itemType);
        if (slot < 0) {
            return new ChestOpenResult(false, -1, -1, location);
        }

        dependencies.roomObjectMutations().writeBombPuzzleObject(loaded.contents, location, 0xA1);
        byte[] status = loaded.room.mapCategory() == Warp.CATEGORY_OVERWORLD
                ? dependencies.roomState().overworldRoomStatus
                : dependencies.roomSessionViews().indoorStatusTableForMap(loaded.room.mapId());
        status[loaded.room.roomId()] |= (byte) ROOM_STATUS_CHEST_OPEN;
        // writeBombPuzzleObject already updates the live GBC cell. Rebuilding
        // from the ROM overlay would discard other live mutations (cut bushes).
        if (loaded.room.mapCategory() != Warp.CATEGORY_OVERWORLD) {
            dependencies.roomTileRedraw().refreshActiveRoomTilemap(loaded.contents);
        }
        dependencies.overworldCollision().setRoom(loaded.room.roomObjectsArea());
        dependencies
                .overworldCollision()
                .setGbcOverlay(loaded.room.mapCategory() == Warp.CATEGORY_OVERWORLD ? loaded.room.gbcOverlay() : null);
        loaded.room.replaceEntities(loaded.entities.inspection().view());
        return new ChestOpenResult(true, itemType, slot, location);
    }

    record Dependencies(
            ChestContentsTable chestContentsTable,
            OverworldCollision overworldCollision,
            Rom romData,
            RoomObjectMutations roomObjectMutations,
            RoomSessionViews roomSessionViews,
            RoomWorldState roomState,
            RoomObjectMutations roomTileRedraw) {
        Dependencies {
            Objects.requireNonNull(chestContentsTable, "chestContentsTable");
            Objects.requireNonNull(overworldCollision, "overworldCollision");
            Objects.requireNonNull(romData, "romData");
            Objects.requireNonNull(roomObjectMutations, "roomObjectMutations");
            Objects.requireNonNull(roomSessionViews, "roomSessionViews");
            Objects.requireNonNull(roomState, "roomState");
            Objects.requireNonNull(roomTileRedraw, "roomTileRedraw");
        }
    }
}
