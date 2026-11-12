package linksawakening.world.room;

import static linksawakening.world.room.RoomSession.*;

/** Immutable snapshot of the app inputs used by one entity frame. */
public record EntityFrameInputs(
        int frameCounter, LinkInputs link, PlayerInputs player, UiInputs ui, ButtonInputs buttons, SwordBox sword) {}
