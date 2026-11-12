package linksawakening.app;

import java.util.Objects;
import linksawakening.app.story.MusicRequests;
import linksawakening.app.story.OpeningStory;

/** Story effects used when entering a new or saved game. */
public record StoryActions(MusicRequests music, OpeningStory story) {
    public StoryActions {
        Objects.requireNonNull(music);
        Objects.requireNonNull(story);
    }
}
