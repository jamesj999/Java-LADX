package linksawakening.world.room;

/** Read-only tick registers. Borrowed implementations are valid only during the synchronous tick. */
public interface EntityTickValues {
    int frameCounter();

    EntityFrameValues.Position position();

    EntityFrameValues.Motion motion();

    int shieldLevel();

    SwordBox sword();
}
