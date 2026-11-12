package linksawakening.app.gameplay;

import java.util.Objects;
import linksawakening.app.story.OpeningStory;

/** Retained gameplay interactions in their dependency order. */
public record GameplayInteractions(
        OpeningStory story, LiftInteraction lift, BushInteraction bush, DialogInteractions dialogs) {
    public GameplayInteractions {
        Objects.requireNonNull(story);
        Objects.requireNonNull(lift);
        Objects.requireNonNull(bush);
        Objects.requireNonNull(dialogs);
    }
}
