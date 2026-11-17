package linksawakening.app.wiring;

import linksawakening.app.audio.MusicDirector;
import linksawakening.app.audio.SessionDialogSounds;
import linksawakening.app.gameplay.ActiveLinkPhase;
import linksawakening.app.gameplay.AnimatedTilesPhase;
import linksawakening.app.gameplay.CombatPhase;
import linksawakening.app.gameplay.DialogStoryPhase;
import linksawakening.app.gameplay.FrameInputCache;
import linksawakening.app.gameplay.GameplayControl;
import linksawakening.app.gameplay.GameplaySimulation;
import linksawakening.app.gameplay.LinkEquipmentPhase;
import linksawakening.app.gameplay.LinkGroundPhase;
import linksawakening.app.gameplay.LinkMotionDriver;
import linksawakening.app.gameplay.entity.EntityDialogRouting;
import linksawakening.app.gameplay.entity.EntityFrameInputsFactory;
import linksawakening.app.gameplay.entity.EntityPhase;
import linksawakening.app.gameplay.entity.EntityRewardApplier;
import linksawakening.app.gameplay.entity.GameplayCombatEvents;
import linksawakening.app.gameplay.entity.LinkWriteApplier;
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

    static GameplaySimulation simulation(SessionServices s, SessionState state, GameplayControl control) {
        var frameInputs = new FrameInputCache(
                state.debug(),
                s.ui().dialogController(),
                state.clock(),
                s.player().link(),
                s.world().roomSession(),
                s.world().scrollController(),
                s.world().transitionController());
        var ground =
                new LinkGroundPhase(frameInputs, s.player().link(), s.world().roomSession());
        var active = new ActiveLinkPhase(new ActiveLinkPhase.Inputs()
                .bush(control.interactions().bush())
                .debugNoClip(state.debug())
                .debugScroll(control.debugScroll())
                .equipmentController(s.player().equipmentController())
                .frameCounter(state.clock())
                .frameInputs(frameInputs)
                .lift(control.interactions().lift())
                .link(s.player().link())
                .musicDirector(control.music())
                .playerState(s.player().playerState())
                .roomSession(s.world().roomSession())
                .roomTransitionCoordinator(s.world().roomTransitionCoordinator())
                .scrollController(s.world().scrollController())
                .transientVfxSystem(s.visual().transientVfxSystem())
                .ground(ground)
                .motion(LinkMotionDriver.DIRECT));
        var inputs = new EntityFrameInputsFactory(new EntityFrameInputsFactory.Inputs()
                .dialogController(s.ui().dialogController())
                .frameCounter(state.clock())
                .inputConfig(s.input().inputConfig())
                .inputState(s.input().inputState())
                .inventoryMenu(s.ui().inventoryMenu())
                .lift(control.interactions().lift())
                .link(s.player().link())
                .musicPlayer(control.audio().musicPlayer())
                .playerState(s.player().playerState()));
        var rewards = new EntityRewardApplier(
                s.ui().dialogController(),
                control.audio().gameplayMusicController(),
                s.rom().gpu(),
                s.player().link(),
                control.music(),
                s.player().playerState(),
                s.rom().romData(),
                s.world().roomSession());
        var linkWrites = new LinkWriteApplier(new LinkWriteApplier.Inputs()
                .equipmentController(s.player().equipmentController())
                .frameCounter(state.clock())
                .itemRegistry(s.player().itemRegistry())
                .lift(control.interactions().lift())
                .link(s.player().link())
                .playerState(s.player().playerState())
                .roomSession(s.world().roomSession())
                .roomTransitionCoordinator(s.world().roomTransitionCoordinator())
                .scrollController(s.world().scrollController())
                .transitionController(s.world().transitionController()));
        var combatEvents = new GameplayCombatEvents(
                s.world().roomSession(),
                s.player().link(),
                state.sounds(),
                s.visual().transientVfxSystem(),
                control.music());
        var routing = EntityDialogRouting.direct(
                control.music(),
                control.interactions().dialogs(),
                s.world().roomTransitionCoordinator(),
                s.player().link(),
                state.sounds());
        var dialogStory = new DialogStoryPhase(new DialogStoryPhase.Inputs()
                .debugNoClip(state.debug())
                .dialogController(s.ui().dialogController())
                .dialogSounds(control.dialogSounds())
                .dialogTextLoader(s.ui().dialogTextLoader())
                .inventoryMenu(s.ui().inventoryMenu())
                .link(s.player().link())
                .playerState(s.player().playerState())
                .roomSession(s.world().roomSession())
                .scrollController(s.world().scrollController())
                .story(control.interactions().story())
                .transitionController(s.world().transitionController()));
        var combat = new CombatPhase(new CombatPhase.Inputs()
                .equipmentController(s.player().equipmentController())
                .frameInputs(frameInputs)
                .gameplaySoundSink(state.sounds())
                .inventoryController(s.ui().inventoryController())
                .link(s.player().link())
                .musicDirector(control.music())
                .playerState(s.player().playerState())
                .roomSession(s.world().roomSession())
                .scrollController(s.world().scrollController())
                .transientVfxSystem(s.visual().transientVfxSystem())
                .transitionController(s.world().transitionController()));
        var tiles = new AnimatedTilesPhase(
                s.ui().inventoryController(),
                s.world().roomSession(),
                s.world().scrollController(),
                s.world().transitionController());
        var linkEquipment = new LinkEquipmentPhase(new LinkEquipmentPhase.Inputs()
                .dialogController(s.ui().dialogController())
                .dialogTextLoader(s.ui().dialogTextLoader())
                .droppableRupeeSystem(s.visual().droppableRupeeSystem())
                .frameCounter(state.clock())
                .gameplaySoundSink(state.sounds())
                .gpu(s.rom().gpu())
                .inventoryController(s.ui().inventoryController())
                .link(s.player().link())
                .passOutController(control.passOut())
                .playerState(s.player().playerState())
                .romData(s.rom().romData())
                .roomSession(s.world().roomSession())
                .scrollController(s.world().scrollController())
                .transientVfxSystem(s.visual().transientVfxSystem())
                .transitionController(s.world().transitionController())
                .active(active)
                .ground(ground)
                .gameOverOutcome(control.gameOver()));
        var entities = new EntityPhase(new EntityPhase.Inputs()
                .gameplaySoundSink(state.sounds())
                .inventoryController(s.ui().inventoryController())
                .inventoryMenu(s.ui().inventoryMenu())
                .link(s.player().link())
                .playerState(s.player().playerState())
                .roomSession(s.world().roomSession())
                .scrollController(s.world().scrollController())
                .transitionController(s.world().transitionController())
                .inputs(inputs)
                .rewards(rewards)
                .linkWrites(linkWrites)
                .combatEvents(combatEvents)
                .routing(routing));
        return new GameplaySimulation(new GameplaySimulation.Inputs()
                .inventoryController(s.ui().inventoryController())
                .inventoryMenu(s.ui().inventoryMenu())
                .link(s.player().link())
                .passOutController(control.passOut())
                .dialogController(s.ui().dialogController())
                .dialogBlockers(control.blockers())
                .dialogStoryPhase(dialogStory)
                .linkEquipmentPhase(linkEquipment)
                .combatPhase(combat)
                .entityPhase(entities)
                .animatedTilesPhase(tiles));
    }

    record PreparedGameplay(
            GameplayInteractions interactions,
            MusicDirector music,
            SessionDialogSounds dialogSounds,
            DialogBlockerPolicy blockers,
            FrameInputCompletion completion) {}
}
