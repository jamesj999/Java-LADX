package linksawakening.world.room;

/** Immutable snapshot of the app inputs used by one entity frame. */
public record EntityFrameInputs(
        int frameCounter, LinkInputs link, PlayerInputs player, UiInputs ui, ButtonInputs buttons, SwordBox sword)
        implements EntityFrameValues {}
