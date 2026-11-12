package linksawakening.world.room;

import static linksawakening.world.room.RoomSession.*;

import java.util.List;
import java.util.Map;
import linksawakening.rom.Rom;
import linksawakening.rom.RomAddress;
import linksawakening.rom.RomSymbols;
import linksawakening.world.entity.event.HookshotBridgeUpdate;
import linksawakening.world.entity.motion.HookshotBridgeMotion;
import linksawakening.world.transition.Warp;

/** RoomTileRedraw operations for the live room session. */
final class RoomTileRedraw {
    private final RoomSession session;

    RoomTileRedraw(RoomSession session) {
        this.session = session;
    }

    void refreshActiveRoomTilemap() {
        RoomTilemap tilemap = session.context.activeRoom.mapCategory() == Warp.CATEGORY_OVERWORLD
                ? session.context.roomTilemapBuilder.buildOverworld(
                        session.context.activeRoom.roomId(), session.context.activeRoom.roomObjectsArea())
                : session.context.roomTilemapBuilder.buildIndoor(
                        session.context.activeRoom.mapId(),
                        session.context.activeRoom.roomId(),
                        session.context.activeRoom.roomObjectsArea());
        session.context.activeRoom.replaceTilemap(tilemap.tileIds(), tilemap.tileAttrs());
        session.applyHookshotBridgeTileOverrides();
        session.applyBombedWallTileOverrides();
        session.applyBombedBlockTileOverrides();
        session.applyBombedCaveDoorTileOverrides();
        session.applyRetainedTorchTiles();
        session.context.shutterTileOverrides.forEach(
                (index, tile) -> session.context.activeRoom.tileIds()[index] = tile);
    }

    void applyHookshotBridgeUpdates(List<HookshotBridgeUpdate> updates) {
        if (session.context.activeRoom == null || updates == null || updates.isEmpty()) {
            return;
        }
        int[] objects = session.context.activeRoom.roomObjectsArea();
        boolean changed = false;
        for (HookshotBridgeUpdate update : updates) {
            int areaIndex = session.hookshotObjectAreaIndex(update.objectLeft(), update.objectTop());
            if (areaIndex < 0 || areaIndex >= objects.length) {
                continue;
            }
            HookshotBridgeUpdate previous = session.context.hookshotBridgeTileOverrides.put(areaIndex, update);
            if (objects[areaIndex] != 0x9D || !update.equals(previous)) {
                objects[areaIndex] = 0x9D;
                changed = true;
            }
        }
        if (changed) {
            session.refreshActiveRoomTilemap();
        }
    }

    void applyHookshotBridgeTileOverrides() {
        if (session.context.activeRoom == null || session.context.hookshotBridgeTileOverrides.isEmpty()) {
            return;
        }
        int[] tileIds = session.context.activeRoom.tileIds();
        for (HookshotBridgeUpdate update : session.context.hookshotBridgeTileOverrides.values()) {
            int tileX = (update.objectLeft() & 0xFF) >>> 3;
            int tileY = (update.objectTop() & 0xFF) >>> 3;
            if (tileX < 0
                    || tileX + 1 >= RoomConstants.ROOM_TILE_WIDTH
                    || tileY < 0
                    || tileY + 1 >= RoomConstants.ROOM_TILE_HEIGHT) {
                continue;
            }
            int topLeft = tileY * RoomConstants.ROOM_TILE_WIDTH + tileX;
            int bottomLeft = topLeft + RoomConstants.ROOM_TILE_WIDTH;
            if (update.direction() == HookshotBridgeMotion.PULL_DOWN_DIRECTION) {
                tileIds[topLeft] = 0x04;
                tileIds[topLeft + 1] = 0x05;
                tileIds[bottomLeft] = update.active() ? 0x08 : 0x04;
                tileIds[bottomLeft + 1] = update.active() ? 0x09 : 0x05;
            } else {
                tileIds[topLeft] = update.active() ? 0x0A : 0x04;
                tileIds[topLeft + 1] = update.active() ? 0x0B : 0x05;
                tileIds[bottomLeft] = 0x04;
                tileIds[bottomLeft + 1] = 0x05;
            }
        }
    }

    void applyBombedWallTileOverrides() {
        if (session.context.activeRoom == null || session.context.bombedWallTileOverrides.isEmpty()) {
            return;
        }
        int[] tileIds = session.context.activeRoom.tileIds();
        for (Map.Entry<Integer, Integer> entry : session.context.bombedWallTileOverrides.entrySet()) {
            int areaOffset = entry.getKey() - RoomConstants.ROOM_OBJECTS_BASE;
            int objectRow = (areaOffset >>> 4) & 0x0F;
            int objectColumn = areaOffset & 0x0F;
            int tileX = objectColumn * 2;
            int tileY = objectRow * 2;
            if (objectRow >= RoomConstants.OBJECTS_PER_COLUMN
                    || objectColumn >= RoomConstants.OBJECTS_PER_ROW
                    || tileX + 1 >= RoomConstants.ROOM_TILE_WIDTH
                    || tileY + 1 >= RoomConstants.ROOM_TILE_HEIGHT) {
                continue;
            }

            int[] replacement =
                    entry.getValue() == 0 ? new int[] {0x72, 0x72, 0x73, 0x73} : new int[] {0x69, 0x79, 0x69, 0x79};
            int topLeft = tileY * RoomConstants.ROOM_TILE_WIDTH + tileX;
            tileIds[topLeft] = replacement[0];
            tileIds[topLeft + 1] = replacement[1];
            tileIds[topLeft + RoomConstants.ROOM_TILE_WIDTH] = replacement[2];
            tileIds[topLeft + RoomConstants.ROOM_TILE_WIDTH + 1] = replacement[3];
        }
    }

    void applyBombedBlockTileOverrides() {
        if (session.context.activeRoom == null || session.context.bombedBlockTileOverrides.isEmpty()) {
            return;
        }
        int[] tileIds = session.context.activeRoom.tileIds();
        for (int areaIndex : session.context.bombedBlockTileOverrides) {
            int areaOffset = areaIndex - RoomConstants.ROOM_OBJECTS_BASE;
            int objectRow = (areaOffset >>> 4) & 0x0F;
            int objectColumn = areaOffset & 0x0F;
            int tileX = objectColumn * 2;
            int tileY = objectRow * 2;
            if (objectRow >= RoomConstants.OBJECTS_PER_COLUMN
                    || objectColumn >= RoomConstants.OBJECTS_PER_ROW
                    || tileX + 1 >= RoomConstants.ROOM_TILE_WIDTH
                    || tileY + 1 >= RoomConstants.ROOM_TILE_HEIGHT) {
                continue;
            }

            int topLeft = tileY * RoomConstants.ROOM_TILE_WIDTH + tileX;
            tileIds[topLeft] = 0x10;
            tileIds[topLeft + 1] = 0x12;
            tileIds[topLeft + RoomConstants.ROOM_TILE_WIDTH] = 0x11;
            tileIds[topLeft + RoomConstants.ROOM_TILE_WIDTH + 1] = 0x13;
        }
    }

    void applyBombedCaveDoorTileOverrides() {
        if (session.context.activeRoom == null || session.context.bombedCaveDoorTileOverrides.isEmpty()) {
            return;
        }
        int[] tileIds = session.context.activeRoom.tileIds();
        for (int areaIndex : session.context.bombedCaveDoorTileOverrides) {
            int areaOffset = areaIndex - RoomConstants.ROOM_OBJECTS_BASE;
            int objectRow = (areaOffset >>> 4) & 0x0F;
            int objectColumn = areaOffset & 0x0F;
            int tileX = objectColumn * 2;
            int tileY = objectRow * 2;
            if (objectRow >= RoomConstants.OBJECTS_PER_COLUMN
                    || objectColumn >= RoomConstants.OBJECTS_PER_ROW
                    || tileX + 1 >= RoomConstants.ROOM_TILE_WIDTH
                    || tileY + 1 >= RoomConstants.ROOM_TILE_HEIGHT) {
                continue;
            }

            RomAddress tileTable = RomSymbols.BombedCaveDoorTilesIndexesGBC;
            int topLeft = tileY * RoomConstants.ROOM_TILE_WIDTH + tileX;
            tileIds[topLeft] = new Rom(session.context.romData).u8(tileTable);
            tileIds[topLeft + 1] = new Rom(session.context.romData).u8(tileTable.plus(1));
            tileIds[topLeft + RoomConstants.ROOM_TILE_WIDTH] = new Rom(session.context.romData).u8(tileTable.plus(2));
            tileIds[topLeft + RoomConstants.ROOM_TILE_WIDTH + 1] =
                    new Rom(session.context.romData).u8(tileTable.plus(3));
        }
    }
}
