package linksawakening.world.room;

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
    /** Required long-lived collaborators are validated once, before the session can run a frame. */
    public RoomSessionConfig {
        java.util.Objects.requireNonNull(romData, "romData");
        java.util.Objects.requireNonNull(gpu, "gpu");
        java.util.Objects.requireNonNull(roomLoader, "roomLoader");
        java.util.Objects.requireNonNull(overworldTilesetTable, "overworldTilesetTable");
        java.util.Objects.requireNonNull(overworldCollision, "overworldCollision");
        java.util.Objects.requireNonNull(presentation, "presentation");
        java.util.Objects.requireNonNull(randomByteSource, "randomByteSource");
    }

    /**
     * Public optional-output configuration. Null disables that output; eager room wiring replaces it
     * with a zero-slot effects/drop system or a no-op listener before any frame runs.
     */
    public record Presentation(
            TransientVfxSystem transientVfxSystem,
            DroppableRupeeSystem droppableRupeeSystem,
            RoomLoadListener roomLoadListener) {}
}
