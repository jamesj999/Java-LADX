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

    /** @deprecated Pass Rom; retained for main/java/linksawakening/app/wiring/WorldWiring.java:rooms; test/java/linksawakening/app/MusicDirectorTest.java; test/java/linksawakening/render/DialogEmulatorCapture.java; test/java/linksawakening/render/HouseEmulatorCapture.java. Legacy partial buffers retain their validation timing. */
    @Deprecated(forRemoval = true)
    public RoomSessionConfig(
            byte[] romData,
            linksawakening.gpu.GPU gpu,
            RoomLoader roomLoader,
            OverworldTilesetTable overworldTilesetTable,
            linksawakening.physics.OverworldCollision overworldCollision,
            Presentation presentation,
            linksawakening.world.entity.RomRandomByteSource randomByteSource) {
        this(
                romData == null ? null : Rom.unchecked(romData),
                gpu,
                roomLoader,
                overworldTilesetTable,
                overworldCollision,
                presentation,
                randomByteSource);
    }
}
