package linksawakening.app;

import java.util.Objects;
import linksawakening.entity.Link;
import linksawakening.gameplay.GameplaySoundSink;
import linksawakening.rom.RomTables;
import linksawakening.state.PlayerState;
import linksawakening.world.entity.RomRandomByteSource;
import linksawakening.world.room.RoomSession;

/** Dependencies shared by equipment target adapters; no session reference is retained. */
public record EquipmentInputs(
        PlayerState playerState,
        Link link,
        RoomSession roomSession,
        RomTables romTables,
        PlayerGraphics graphics,
        GameplaySoundSink gameplaySoundSink,
        RomRandomByteSource random) {
    public EquipmentInputs {
        Objects.requireNonNull(playerState);
        Objects.requireNonNull(link);
        Objects.requireNonNull(roomSession);
        Objects.requireNonNull(romTables);
        Objects.requireNonNull(graphics);
        Objects.requireNonNull(gameplaySoundSink);
        Objects.requireNonNull(random);
    }
}
