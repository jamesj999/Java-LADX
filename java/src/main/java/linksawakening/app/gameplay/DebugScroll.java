package linksawakening.app.gameplay;

/**
 * Optional debug boundary scroll, sampled at the ordinary Link-motion gate. Implementations are connected
 * when the owning session is wired.
 */
public interface DebugScroll {
    boolean tryDebugScreenScroll();
}
