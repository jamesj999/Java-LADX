package linksawakening.app.gameplay.recovery;

/**
 * Ordered recovery requested by the game-over presentation. Implementations are connected when the owning
 * session is wired.
 */
public interface GameOverActions {
    void finishGameOver(int option);
}
