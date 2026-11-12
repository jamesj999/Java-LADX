package linksawakening.world.room;

import static linksawakening.world.room.RoomSessionRules.*;

import linksawakening.gpu.GPU;
import linksawakening.physics.OverworldCollision;
import linksawakening.rom.Rom;
import linksawakening.vfx.TransientVfxSystem;
import linksawakening.world.entity.RomRandomByteSource;

/** Typed room wiring; all collaborators share the caller-owned Rom. */
public record RoomSessionConfig(
        Rom romData,
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
