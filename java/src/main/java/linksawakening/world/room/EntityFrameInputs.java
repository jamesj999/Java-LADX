package linksawakening.world.room;

import static linksawakening.world.room.RoomSessionRules.*;

/** Immutable snapshot of the app inputs used by one entity frame. */
public record EntityFrameInputs(
        int frameCounter, LinkInputs link, PlayerInputs player, UiInputs ui, ButtonInputs buttons, SwordBox sword) {}
