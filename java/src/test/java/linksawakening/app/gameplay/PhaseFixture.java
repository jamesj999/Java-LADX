package linksawakening.app.gameplay;

import linksawakening.app.GameSession;
import linksawakening.app.SessionTestAccess;
import linksawakening.app.audio.MusicDirector;
import linksawakening.app.audio.SessionDialogSounds;
import linksawakening.app.gameplay.entity.EntityDialogRouting;
import linksawakening.app.gameplay.entity.EntityFrameInputsFactory;
import linksawakening.app.gameplay.entity.EntityPhase;
import linksawakening.app.gameplay.entity.EntityRewardApplier;
import linksawakening.app.gameplay.entity.GameplayCombatEvents;
import linksawakening.app.gameplay.entity.LinkWriteApplier;
import linksawakening.app.gameplay.interaction.BushInteraction;
import linksawakening.app.gameplay.interaction.DialogBlockerPolicy;
import linksawakening.app.gameplay.interaction.DialogBlockers;
import linksawakening.app.gameplay.interaction.DialogInteractions;
import linksawakening.app.gameplay.interaction.DialogSoundRouting;
import linksawakening.app.gameplay.interaction.LiftInteraction;
import linksawakening.app.gameplay.recovery.GameOverActions;
import linksawakening.app.gameplay.recovery.GameOverFlow;
import linksawakening.app.gameplay.recovery.PassOutController;
import linksawakening.app.services.SessionServices;
import linksawakening.app.state.GameplaySounds;
import linksawakening.app.state.SessionState;
import linksawakening.app.story.OpeningStory;
import linksawakening.audio.music.FrameMusicPlayer;
import linksawakening.audio.music.GameplayMusicController;
import linksawakening.dialog.DialogController;
import linksawakening.entity.Link;
import linksawakening.equipment.EquipmentController;
import linksawakening.equipment.ItemRegistry;
import linksawakening.gameplay.DialogTextLoader;
import linksawakening.gameplay.GameplaySoundSink;
import linksawakening.gpu.GPU;
import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
import linksawakening.rom.Rom;
import linksawakening.state.PlayerState;
import linksawakening.ui.InventoryController;
import linksawakening.ui.InventoryMenu;
import linksawakening.vfx.TransientVfxSystem;
import linksawakening.world.room.DroppableRupeeSystem;
import linksawakening.world.room.RoomSession;
import linksawakening.world.room.RoomTransitionCoordinator;
import linksawakening.world.transition.ScrollController;
import linksawakening.world.transition.TransitionController;

/** Named phase setup against the same live session collaborators used by production wiring. */
public final class PhaseFixture {
    private final SessionServices s;
    private final SessionState state;
    private final GameplayControl control;
    private final FrameInputCache frameInputs;

    public PhaseFixture(GameSession game) {
        s = game.services();
        state = SessionTestAccess.context(game).state();
        var passOut = game.flows().simulation().passOutController();
        control = new GameplayControl(
                s.audio(),
                game.flows().music(),
                game.flows().interactions(),
                passOut,
                new GameOverFlow(
                        s.player().equipmentController(),
                        passOut,
                        game.flows().saves(),
                        s.visual().transientVfxSystem(),
                        s.visual().droppableRupeeSystem()),
                new DialogBlockerPolicy(
                        s.ui().inventoryController(),
                        s.ui().dialogController(),
                        s.world().scrollController(),
                        s.world().transitionController(),
                        state.input()),
                game.flows().inputRouter(),
                new SessionDialogSounds(s.ui().dialogController(), s.audio().dialogSoundSink()));
        frameInputs = new FrameInputCache(
                state.debug(),
                s.ui().dialogController(),
                state.clock(),
                s.player().link(),
                s.world().roomSession(),
                s.world().scrollController(),
                s.world().transitionController());
    }

    public boolean dialogBlocks() {
        return dialogBlockers().current().pausesGameplay();
    }

    public boolean passingOut() {
        return link().status().isPassingOut() || passOutController().handledFrame();
    }

    public PassOutController passOutController() {
        return control.passOut();
    }

    public GameOverActions gameOverOutcome() {
        return control.gameOver();
    }

    public GPU gpu() {
        return s.rom().gpu();
    }

    public Rom romData() {
        return s.rom().romData();
    }

    public RoomSession roomSession() {
        return s.world().roomSession();
    }

    public TransitionController transitionController() {
        return s.world().transitionController();
    }

    public RoomTransitionCoordinator roomTransitionCoordinator() {
        return s.world().roomTransitionCoordinator();
    }

    public boolean debugNoClip() {
        return state.debug().debugNoClip;
    }

    public int frameCounter() {
        return state.clock().value();
    }

    public ScrollController scrollController() {
        return s.world().scrollController();
    }

    public InputConfig inputConfig() {
        return s.input().inputConfig();
    }

    public InputState inputState() {
        return s.input().inputState();
    }

    public PlayerState playerState() {
        return s.player().playerState();
    }

    public InventoryMenu inventoryMenu() {
        return s.ui().inventoryMenu();
    }

    public InventoryController inventoryController() {
        return s.ui().inventoryController();
    }

    public DialogController dialogController() {
        return s.ui().dialogController();
    }

    public DialogTextLoader dialogTextLoader() {
        return s.ui().dialogTextLoader();
    }

    public GameplaySoundSink gameplaySoundSink() {
        return state.sounds().sink();
    }

    public FrameMusicPlayer musicPlayer() {
        return control.audio().musicPlayer();
    }

    public GameplayMusicController gameplayMusicController() {
        return control.audio().gameplayMusicController();
    }

    public MusicDirector musicDirector() {
        return control.music();
    }

    public TransientVfxSystem transientVfxSystem() {
        return s.visual().transientVfxSystem();
    }

    public DroppableRupeeSystem droppableRupeeSystem() {
        return s.visual().droppableRupeeSystem();
    }

    public ItemRegistry itemRegistry() {
        return s.player().itemRegistry();
    }

    public EquipmentController equipmentController() {
        return s.player().equipmentController();
    }

    public Link link() {
        return s.player().link();
    }

    public OpeningStory story() {
        return control.interactions().story();
    }

    public LiftInteraction lift() {
        return control.interactions().lift();
    }

    public BushInteraction bush() {
        return control.interactions().bush();
    }

    public DialogInteractions dialogInteractions() {
        return control.interactions().dialogs();
    }

    public DialogBlockers dialogBlockers() {
        return control.blockers();
    }

    public DebugScroll debugScroll() {
        return control.debugScroll();
    }

    public DialogSoundRouting dialogSounds() {
        return control.dialogSounds();
    }

    public LinkGroundPhase ground() {
        return new LinkGroundPhase(frameInputs, s.player().link(), s.world().roomSession());
    }

    public ActiveLinkPhase active(LinkMotionDriver motion) {
        return new ActiveLinkPhase(new ActiveLinkPhase.Inputs()
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
                .ground(ground())
                .motion(motion));
    }

    public LinkEquipmentPhase linkEquipment(LinkMotionDriver motion) {
        return new LinkEquipmentPhase(new LinkEquipmentPhase.Inputs()
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
                .active(active(motion))
                .ground(ground())
                .gameOverOutcome(control.gameOver()));
    }

    public DialogStoryPhase dialogStory() {
        return new DialogStoryPhase(new DialogStoryPhase.Inputs()
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
    }

    public CombatPhase combat() {
        return new CombatPhase(new CombatPhase.Inputs()
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
    }

    public AnimatedTilesPhase tiles() {
        return new AnimatedTilesPhase(
                s.ui().inventoryController(),
                s.world().roomSession(),
                s.world().scrollController(),
                s.world().transitionController());
    }

    public EntityFrameInputsFactory entityInputs() {
        return new EntityFrameInputsFactory(new EntityFrameInputsFactory.Inputs()
                .dialogController(s.ui().dialogController())
                .frameCounter(state.clock())
                .inputConfig(s.input().inputConfig())
                .inputState(s.input().inputState())
                .inventoryMenu(s.ui().inventoryMenu())
                .lift(control.interactions().lift())
                .link(s.player().link())
                .musicPlayer(control.audio().musicPlayer())
                .playerState(s.player().playerState()));
    }

    public EntityRewardApplier rewards() {
        return new EntityRewardApplier(
                s.ui().dialogController(),
                control.audio().gameplayMusicController(),
                s.rom().gpu(),
                s.player().link(),
                control.music(),
                s.player().playerState(),
                s.rom().romData(),
                s.world().roomSession());
    }

    public LinkWriteApplier linkWrites() {
        return new LinkWriteApplier(new LinkWriteApplier.Inputs()
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
    }

    public EntityPhase entityPhase(GameplayCombatEvents combatEvents, EntityDialogRouting routing) {
        return new EntityPhase(new EntityPhase.Inputs()
                .gameplaySoundSink(state.sounds())
                .inventoryController(s.ui().inventoryController())
                .inventoryMenu(s.ui().inventoryMenu())
                .link(s.player().link())
                .playerState(s.player().playerState())
                .roomSession(s.world().roomSession())
                .scrollController(s.world().scrollController())
                .transitionController(s.world().transitionController())
                .inputs(entityInputs())
                .rewards(rewards())
                .linkWrites(linkWrites())
                .combatEvents(combatEvents)
                .routing(routing));
    }

    public LinkEquipmentPhase linkEquipment() {
        return linkEquipment(LinkMotionDriver.DIRECT);
    }

    public EntityDialogRouting routing() {
        return EntityDialogRouting.direct(
                control.music(),
                control.interactions().dialogs(),
                s.world().roomTransitionCoordinator(),
                s.player().link(),
                state.sounds());
    }

    public EntityPhase entityPhase(
            RoomSession rooms, Link link, GameplaySounds sounds, TransientVfxSystem effects, MusicDirector music) {
        return entityPhase(new GameplayCombatEvents(rooms, link, sounds, effects, music), routing());
    }
}
