package linksawakening.app.gameplay;

import java.util.Objects;
import linksawakening.app.*;
import linksawakening.app.debug.*;
import linksawakening.app.host.*;
import linksawakening.app.screen.*;
import linksawakening.app.story.*;
import linksawakening.world.transition.*;

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
