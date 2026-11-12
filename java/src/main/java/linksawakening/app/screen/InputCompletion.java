package linksawakening.app.screen;

/**
 * Completes input edges only after all consumers for a successful frame. Implementations are connected when
 * the owning session is wired.
 */
public interface InputCompletion {
    void complete();
}
