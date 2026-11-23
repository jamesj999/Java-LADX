package linksawakening.app.screen;

/** Stop request issued by screen input. Implementations are connected when the owning session is wired. */
public interface SessionLifecycle {
    void requestStop();
}
