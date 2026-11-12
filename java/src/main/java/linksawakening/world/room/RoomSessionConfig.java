package linksawakening.world.room;

import static linksawakening.world.room.RoomSession.*;

import linksawakening.gpu.GPU;
import linksawakening.physics.OverworldCollision;
import linksawakening.vfx.TransientVfxSystem;
import linksawakening.world.entity.RomRandomByteSource;

public record RoomSessionConfig(
        byte[] romData,
        GPU gpu,
        RoomLoader roomLoader,
        OverworldTilesetTable overworldTilesetTable,
        OverworldCollision overworldCollision,
        Presentation presentation,
        RomRandomByteSource randomByteSource) {
    public record Presentation(
            TransientVfxSystem transientVfxSystem,
            DroppableRupeeSystem droppableRupeeSystem,
            RoomLoadListener roomLoadListener) {}
}
