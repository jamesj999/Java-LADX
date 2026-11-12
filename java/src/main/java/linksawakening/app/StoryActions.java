package linksawakening.app;

import java.util.Objects;
import linksawakening.app.debug.*;
import linksawakening.app.gameplay.*;
import linksawakening.app.host.*;
import linksawakening.app.screen.*;
import linksawakening.app.story.*;
import linksawakening.world.transition.*;

/** Story effects used when entering a new or saved game. */
public record StoryActions(MusicRequests music, OpeningStory story) {
    public StoryActions {
        Objects.requireNonNull(music);
        Objects.requireNonNull(story);
    }
}
