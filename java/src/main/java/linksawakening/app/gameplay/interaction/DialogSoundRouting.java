package linksawakening.app.gameplay.interaction;

/**
 * Routes pending dialog effects at the source-defined point in a frame. Implementations are connected when
 * the owning session is wired.
 */
public interface DialogSoundRouting {
    void route();
}
