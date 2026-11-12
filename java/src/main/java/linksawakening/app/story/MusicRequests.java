package linksawakening.app.story;

/**
 * Track requests issued by story and screen orchestration. Implementations are connected when the owning
 * session is wired.
 */
public interface MusicRequests {
    void playDirectMusic(int trackId);
}
