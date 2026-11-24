package linksawakening.world.room;

/** Read-only tick registers. Borrowed implementations are valid only during the synchronous tick. */
public interface EntityTickValues {
    int frameCounter();

    default boolean incomingScrollOnly() {
        return false;
    }

    EntityFrameValues.Position position();

    EntityFrameValues.Motion motion();

    int shieldLevel();

    SwordBox sword();
}
