package linksawakening.app.wiring;

import linksawakening.app.audio.MusicDirector;
import linksawakening.app.audio.SessionDialogSounds;
import linksawakening.app.gameplay.interaction.BushInteraction;
import linksawakening.app.gameplay.interaction.DialogBlockerPolicy;
import linksawakening.app.gameplay.interaction.DialogInteractions;
import linksawakening.app.gameplay.interaction.GameplayInteractions;
import linksawakening.app.gameplay.interaction.LiftInteraction;
import linksawakening.app.input.FrameInputCompletion;
import linksawakening.app.services.SessionServices;
import linksawakening.app.state.SessionState;
import linksawakening.app.story.OpeningActions;
import linksawakening.app.story.OpeningStory;

/**
 * Builds dialog policy and gameplay interactions before navigation flows. Stateless factory used during
 * session initialization.
 */
final class GameplayWiring {
    private GameplayWiring() {}

    record PreparedGameplay(
            GameplayInteractions interactions,
            MusicDirector music,
            SessionDialogSounds dialogSounds,
            DialogBlockerPolicy blockers,
            FrameInputCompletion completion) {}

    static PreparedGameplay build(SessionServices s, SessionState state) {
        var music = new MusicDirector(s.audio().musicPlayer(), s.audio().gameplayMusicController());
        var dialogSounds =
                new SessionDialogSounds(s.ui().dialogController(), s.audio().dialogSoundSink());
        var blockers = new DialogBlockerPolicy(
                s.ui().inventoryController(),
                s.ui().dialogController(),
                s.world().scrollController(),
                s.world().transitionController(),
                state.input());
        var completion = new FrameInputCompletion(s.input().inputState(), state.input());
        var lift = new LiftInteraction(
                s.input().inputState(),
                s.input().inputConfig(),
                s.player().playerState(),
                s.player().link(),
                s.world().roomSession());
        var dialogs = new DialogInteractions(s.input(), s.world(), s.player(), s.ui(), blockers);
        var bush = new BushInteraction(s.world(), s.player(), s.visual(), state.sounds());
        var story = new OpeningStory(
                s.player(),
                s.world(),
                s.input(),
                s.ui(),
                state,
                new OpeningActions(s.visual().newGameTarinShieldPalette(), blockers, music));
        return new PreparedGameplay(
                new GameplayInteractions(story, lift, bush, dialogs), music, dialogSounds, blockers, completion);
    }
}
