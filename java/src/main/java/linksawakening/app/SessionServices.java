package linksawakening.app;

import static linksawakening.world.room.RoomConstants.*;

import java.util.Objects;

/** Subsystem bundles owned by a single game session. */
public record SessionServices(
        RomServices rom,
        AudioServices audio,
        InputServices input,
        WorldServices world,
        PlayerServices player,
        UiServices ui,
        VisualServices visual) {
    public SessionServices {
        Objects.requireNonNull(rom);
        Objects.requireNonNull(audio);
        Objects.requireNonNull(input);
        Objects.requireNonNull(world);
        Objects.requireNonNull(player);
        Objects.requireNonNull(ui);
        Objects.requireNonNull(visual);
    }
}
