package linksawakening.app.gameplay.recovery;

import java.util.Objects;
import linksawakening.app.audio.MusicDirector;
import linksawakening.app.state.GameplaySounds;
import linksawakening.entity.Link;
import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.gpu.GPU;
import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
import linksawakening.render.GameFrameState;
import linksawakening.render.RenderScreen;
import linksawakening.rom.Rom;
import linksawakening.state.PlayerState;
import linksawakening.ui.InventoryController;
import linksawakening.world.room.RoomSession;
import linksawakening.world.transition.ScrollController;
import linksawakening.world.transition.TransitionController;

/**
 * Bank 0 death dispatch and bank 1 animation; screen loading follows in a later stage. One instance is
 * retained for the owning session.
 */
public final class PassOutController {
    private final GameplaySounds sounds;
    private final InventoryController inventoryController;
    private final Link playerLink;
    private final MusicDirector musicDirector;
    private final PlayerState playerState;
    private final Rom rom;
    private final RoomSession rooms;
    private final ScrollController scrollController;
    private final TransitionController transitionController;
    private final GPU gpu;
    private final InputConfig inputConfig;
    private final InputState inputState;

    // Optional identity/sequence state: no death presentation exists before the death trigger.
    private Link owner;
    private Sequence sequence = Inactive.INSTANCE;

    private boolean handledFrame;

    public PassOutController(Inputs inputs) {
        this.sounds = Objects.requireNonNull(inputs.gameplaySoundSink);
        this.inventoryController = Objects.requireNonNull(inputs.inventoryController);
        this.playerLink = Objects.requireNonNull(inputs.link);
        this.musicDirector = Objects.requireNonNull(inputs.musicDirector);
        this.playerState = Objects.requireNonNull(inputs.playerState);
        this.rom = Objects.requireNonNull(inputs.romData);
        this.rooms = Objects.requireNonNull(inputs.roomSession);
        this.scrollController = Objects.requireNonNull(inputs.scrollController);
        this.transitionController = Objects.requireNonNull(inputs.transitionController);
        this.gpu = Objects.requireNonNull(inputs.gpu);
        this.inputConfig = Objects.requireNonNull(inputs.inputConfig);
        this.inputState = Objects.requireNonNull(inputs.inputState);
    }

    public boolean handledFrame() {
        return handledFrame;
    }

    public void clear() {
        owner = null;
        sequence = Inactive.INSTANCE;
    }

    /** Applies a menu only while the recovery sequence owns an active game-over presentation. */
    public GameFrameState renderState(GameFrameState state) {
        if (!(sequence instanceof GameOver over)) return state;
        var snapshot = over.menu().snapshot();
        return state.reset().withScreen(RenderScreen.FILE_SAVE).withFileMenuFrame(snapshot);
    }

    public boolean tick(boolean dialog, GameOverActions gameOverOutcome) {
        var link = this.playerLink;
        handledFrame = link.status().isPassingOut();
        if (owner != link) {
            owner = link;
            sequence = Inactive.INSTANCE;
        }
        if (sequence instanceof GameOver over) {
            over.menu().tick();
            return true;
        }
        boolean transition = this.scrollController.isActive() || this.transitionController.isInputBlocked();
        boolean itemBlocked = link.animation().dialogGotItemActive()
                || link.sideView().romItemAnimationFrameBlocked()
                || (this.rooms.entityState().gotItemPresentationActive()
                        || this.rooms.entityState().swordPickupSequenceActive());
        if (LinkPassOutAnimation.shouldStart(
                this.playerState.vitals().health(),
                itemBlocked ? 1 : 0,
                this.inventoryController.shouldBlockOverworldInput() ? 1 : 0,
                dialog,
                transition,
                link.status().isPassingOut())) {
            handledFrame = true;
            sequence = new Dying(new LinkPassOutAnimation(this.rom));
            link.status().beginPassOut();
            this.musicDirector.playDirectMusic(0);
            this.sounds.sink().play(GameplaySoundEvent.LINK_DIES);
        } else if (link.status().isPassingOut() && sequence instanceof Dying dying && !dialog && !transition) {
            dying.animation().advanceFrame();
        }
        if (!link.status().isPassingOut() || !(sequence instanceof Dying dying)) return false;
        var animation = dying.animation();
        this.scrollController.setScriptedScreenShakeHorizontal(false, 0);
        link.status().presentPassOut(animation.countdown(), animation.animationState(), animation.paletteEffect());
        link.motion().update();
        if (animation.finished())
            sequence = new GameOver(new GameOverPresentation(new GameOverPresentation.Inputs()
                    .gameOverOutcome(gameOverOutcome)
                    .gameplaySoundSink(sounds)
                    .gpu(gpu)
                    .inputConfig(inputConfig)
                    .inputState(inputState)
                    .link(playerLink)
                    .musicDirector(musicDirector)
                    .playerState(playerState)
                    .romData(rom)
                    .roomSession(rooms)));
        return true;
    }

    private sealed interface Sequence permits Inactive, Dying, GameOver {}

    private enum Inactive implements Sequence {
        INSTANCE
    }

    private record Dying(LinkPassOutAnimation animation) implements Sequence {
        Dying {
            Objects.requireNonNull(animation);
        }
    }

    private record GameOver(GameOverPresentation menu) implements Sequence {
        GameOver {
            Objects.requireNonNull(menu);
        }
    }

    /** Named construction inputs, consumed once by the phase constructor. */
    public static final class Inputs {
        private GameplaySounds gameplaySoundSink;
        private InventoryController inventoryController;
        private Link link;
        private MusicDirector musicDirector;
        private PlayerState playerState;
        private Rom romData;
        private RoomSession roomSession;
        private ScrollController scrollController;
        private TransitionController transitionController;
        private GPU gpu;
        private InputConfig inputConfig;
        private InputState inputState;

        public Inputs gameplaySoundSink(GameplaySounds value) {
            this.gameplaySoundSink = value;
            return this;
        }

        public Inputs inventoryController(InventoryController value) {
            this.inventoryController = value;
            return this;
        }

        public Inputs link(Link value) {
            this.link = value;
            return this;
        }

        public Inputs musicDirector(MusicDirector value) {
            this.musicDirector = value;
            return this;
        }

        public Inputs playerState(PlayerState value) {
            this.playerState = value;
            return this;
        }

        public Inputs romData(Rom value) {
            this.romData = value;
            return this;
        }

        public Inputs roomSession(RoomSession value) {
            this.roomSession = value;
            return this;
        }

        public Inputs scrollController(ScrollController value) {
            this.scrollController = value;
            return this;
        }

        public Inputs transitionController(TransitionController value) {
            this.transitionController = value;
            return this;
        }

        public Inputs gpu(GPU value) {
            this.gpu = value;
            return this;
        }

        public Inputs inputConfig(InputConfig value) {
            this.inputConfig = value;
            return this;
        }

        public Inputs inputState(InputState value) {
            this.inputState = value;
            return this;
        }
    }
}
