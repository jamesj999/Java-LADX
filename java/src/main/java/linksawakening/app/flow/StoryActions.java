package linksawakening.app.flow;

import java.util.Objects;
import linksawakening.app.story.MusicRequests;
import linksawakening.app.story.OpeningStory;

/**
 * Story effects used when entering a new or saved game. The immutable bundle is retained for the owning
 * session.
 */
public record StoryActions(MusicRequests music, OpeningStory story) {
    public StoryActions {
        Objects.requireNonNull(music);
        Objects.requireNonNull(story);
    }
}
