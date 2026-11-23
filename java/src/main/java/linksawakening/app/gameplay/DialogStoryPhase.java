package linksawakening.app.gameplay;

import static linksawakening.app.gameplay.GameplaySimulation.completeGameplayFrame;

import java.util.Objects;
import linksawakening.app.gameplay.interaction.DialogSoundRouting;
import linksawakening.app.state.DebugState;
import linksawakening.app.story.OpeningStory;
import linksawakening.dialog.DialogController;
import linksawakening.entity.Link;
import linksawakening.gameplay.DialogTextLoader;
import linksawakening.gameplay.LinkDialogPosition;
import linksawakening.gameplay.SignpostDialogRef;
import linksawakening.state.PlayerState;
import linksawakening.ui.InventoryMenu;
import linksawakening.world.room.RoomSession;
import linksawakening.world.transition.ScrollController;
import linksawakening.world.transition.TransitionController;

/** Advances dialog and opening-story gates before player motion. Retained for one simulation. */
public final class DialogStoryPhase {
    private final DebugState debugState;
    private final DialogController dialogController;
    private final DialogSoundRouting dialogSounds;
    private final DialogTextLoader dialogTextLoader;
    private final InventoryMenu inventoryMenu;
    private final Link playerLink;
    private final PlayerState playerState;
    private final RoomSession rooms;
    private final ScrollController scrollController;
    private final OpeningStory story;
    private final TransitionController transitionController;

    public DialogStoryPhase(Inputs inputs) {
        this.debugState = Objects.requireNonNull(inputs.debugNoClip);
        this.dialogController = Objects.requireNonNull(inputs.dialogController);
        this.dialogSounds = Objects.requireNonNull(inputs.dialogSounds);
        this.dialogTextLoader = Objects.requireNonNull(inputs.dialogTextLoader);
        this.inventoryMenu = Objects.requireNonNull(inputs.inventoryMenu);
        this.playerLink = Objects.requireNonNull(inputs.link);
        this.playerState = Objects.requireNonNull(inputs.playerState);
        this.rooms = Objects.requireNonNull(inputs.roomSession);
        this.scrollController = Objects.requireNonNull(inputs.scrollController);
        this.story = Objects.requireNonNull(inputs.story);
        this.transitionController = Objects.requireNonNull(inputs.transitionController);
    }

    public boolean tick() {
        if (transitionController.isManboTransition()
                || transitionController.state() == TransitionController.State.ROOM_LOAD) return false;
        if (this.inventoryMenu.isFullyClosed()) {
            this.playerState.vitals().latchFullHearts();
        }
        {
            // $00:0F22 runs before world transitions and dialog gates.
            // The separate inventory gameplay handler does not run it.
            rooms.entityState()
                    .advanceBoomerangSoundClock(this.inventoryMenu.transition().worldHandlerActive());
        }
        this.scrollController.tickScreenShake();
        if (!this.transitionController.isInputBlocked()) {
            int dungeonTitle = rooms.transitions()
                    .tickDungeonTitleMessage(
                            this.dialogController.isActive(),
                            this.inventoryMenu.isFullyClosed(),
                            this.playerLink.airborne().romMotionState(),
                            this.debugState.debugNoClip());
            if (dungeonTitle >= 0) {
                this.dialogController.openPreformattedForLinkY(
                        this.dialogTextLoader.load(new SignpostDialogRef(0, dungeonTitle)),
                        LinkDialogPosition.dialogYFromTopLeft(
                                this.playerLink.position().pixelY()));
            }
        }
        {
            this.dialogController.tick();
            this.dialogSounds.route();
        }
        this.story.updateNewGameMarinPresentation();
        if (this.story.tickNewGameWakeUp()) {
            completeGameplayFrame(this.dialogController);
            return false;
        }
        if (this.story.tickNewGameTarinShield()) {
            completeGameplayFrame(this.dialogController);
            return false;
        }

        return true;
    }

    /** Named construction inputs, consumed once by the phase constructor. */
    public static final class Inputs {
        private DebugState debugNoClip;
        private DialogController dialogController;
        private DialogSoundRouting dialogSounds;
        private DialogTextLoader dialogTextLoader;
        private InventoryMenu inventoryMenu;
        private Link link;
        private PlayerState playerState;
        private RoomSession roomSession;
        private ScrollController scrollController;
        private OpeningStory story;
        private TransitionController transitionController;

        public Inputs debugNoClip(DebugState value) {
            this.debugNoClip = value;
            return this;
        }

        public Inputs dialogController(DialogController value) {
            this.dialogController = value;
            return this;
        }

        public Inputs dialogSounds(DialogSoundRouting value) {
            this.dialogSounds = value;
            return this;
        }

        public Inputs dialogTextLoader(DialogTextLoader value) {
            this.dialogTextLoader = value;
            return this;
        }

        public Inputs inventoryMenu(InventoryMenu value) {
            this.inventoryMenu = value;
            return this;
        }

        public Inputs link(Link value) {
            this.link = value;
            return this;
        }

        public Inputs playerState(PlayerState value) {
            this.playerState = value;
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

        public Inputs story(OpeningStory value) {
            this.story = value;
            return this;
        }

        public Inputs transitionController(TransitionController value) {
            this.transitionController = value;
            return this;
        }
    }
}
