package linksawakening.app.gameplay;

import java.util.Objects;
import linksawakening.app.gameplay.recovery.GameOverActions;
import linksawakening.app.gameplay.recovery.PassOutController;
import linksawakening.app.state.FrameClock;
import linksawakening.app.state.GameplaySounds;
import linksawakening.dialog.DialogController;
import linksawakening.entity.Link;
import linksawakening.gameplay.DialogTextLoader;
import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.gameplay.LinkDialogPosition;
import linksawakening.gameplay.SignpostDialogRef;
import linksawakening.gpu.GPU;
import linksawakening.rom.Rom;
import linksawakening.state.PlayerState;
import linksawakening.ui.InventoryController;
import linksawakening.vfx.TransientVfxSystem;
import linksawakening.world.room.DroppableRupeeSystem;
import linksawakening.world.room.RoomSession;
import linksawakening.world.transition.ScrollController;
import linksawakening.world.transition.TransitionController;

/** Advances player resources, equipment and Link motion in ROM order. Retained for one simulation. */
public final class LinkEquipmentPhase {
    private final DialogController dialogController;
    private final DialogTextLoader dialogTextLoader;
    private final DroppableRupeeSystem droppableRupeeSystem;
    private final FrameClock clock;
    private final GameplaySounds sounds;
    private final GPU gpu;
    private final InventoryController inventoryController;
    private final Link playerLink;
    private final PassOutController passOutController;
    private final PlayerState playerState;
    private final Rom rom;
    private final RoomSession rooms;
    private final ScrollController scrollController;
    private final TransientVfxSystem transientVfxSystem;
    private final TransitionController transitionController;
    private final ActiveLinkPhase active;
    private final LinkGroundPhase ground;
    private final GameOverActions gameOverOutcome;

    public LinkEquipmentPhase(Inputs inputs) {
        this.dialogController = Objects.requireNonNull(inputs.dialogController);
        this.dialogTextLoader = Objects.requireNonNull(inputs.dialogTextLoader);
        this.droppableRupeeSystem = Objects.requireNonNull(inputs.droppableRupeeSystem);
        this.clock = Objects.requireNonNull(inputs.frameCounter);
        this.sounds = Objects.requireNonNull(inputs.gameplaySoundSink);
        this.gpu = Objects.requireNonNull(inputs.gpu);
        this.inventoryController = Objects.requireNonNull(inputs.inventoryController);
        this.playerLink = Objects.requireNonNull(inputs.link);
        this.passOutController = Objects.requireNonNull(inputs.passOutController);
        this.playerState = Objects.requireNonNull(inputs.playerState);
        this.rom = Objects.requireNonNull(inputs.romData);
        this.rooms = Objects.requireNonNull(inputs.roomSession);
        this.scrollController = Objects.requireNonNull(inputs.scrollController);
        this.transientVfxSystem = Objects.requireNonNull(inputs.transientVfxSystem);
        this.transitionController = Objects.requireNonNull(inputs.transitionController);
        this.active = Objects.requireNonNull(inputs.active);
        this.ground = Objects.requireNonNull(inputs.ground);
        this.gameOverOutcome = Objects.requireNonNull(inputs.gameOverOutcome);
    }

    public void tick(boolean dialogBlocksGameplay, boolean inventoryBlockedAtStart) {
        prepare(dialogBlocksGameplay);
        if (this.passOutController.tick(dialogBlocksGameplay, gameOverOutcome)) return;

        // LinkMotionMapFadeInHandler ($00:19DE-$00:1A21) restores normal
        // motion and returns; held movement starts on the following frame.
        boolean linkActive = !inventoryBlockedAtStart
                && !this.scrollController.isActive()
                && !this.transitionController.isInputBlocked()
                && !this.inventoryController.shouldBlockOverworldInput()
                && !dialogBlocksGameplay
                && !this.playerLink.animation().dialogGotItemActive()
                && !this.rooms.entityState().gotItemPresentationActive();
        boolean swordPickupSequenceActive = this.rooms.entityState().swordPickupSequenceActive();
        linkActive = linkActive && !swordPickupSequenceActive;
        if (linkActive) {
            this.active.tick(swordPickupSequenceActive);
        } else {
            if (this.scrollController.isActive()) {
                // Keep Link's walk animation cycling during a room transition.
                this.playerLink.animation().tickAnimation();
            }
            // $02:75C4 does not imply an unconditional pause: $76C0 can
            // return to Default via $7757 and reset wC1C9 during dialogs.
            if (!inventoryBlockedAtStart
                    && !this.inventoryController.shouldBlockOverworldInput()
                    && (dialogBlocksGameplay
                            || this.scrollController.isActive()
                            || this.transitionController.isInputBlocked()
                            || rooms.entityState().gotItemPresentationActive())) {
                this.ground.tickCrackedFloor(this.playerLink.motion().romInteractiveMotionBlockValue() != 0);
            }
        }

        if (!dialogBlocksGameplay) {
            int rumbleHorizontal = this.rooms.doors().tickTailCaveKeyholeSequence();
            this.scrollController.setScriptedScreenShakeHorizontal(
                    this.rooms.doors().tailCaveKeyholeSequenceActive(), rumbleHorizontal);
        }
    }

    private void prepare(boolean dialogBlocksGameplay) {
        if (playerLink.airborne().romMotionState() < 2
                && !scrollController.isActive()
                && !transitionController.isInputBlocked()
                && !inventoryController.shouldBlockOverworldInput()
                && dialogController.allowsResourceUpdates()) {
            playerState.playerResources().tickResourceBuffers(this.clock.value());
            var resourceSound =
                    playerState.playerSounds().takeResourceSound(sound -> GameplaySoundEvent.valueOf(sound.name()));
            if (resourceSound != null) this.sounds.sink().play(resourceSound);
        }
        if (!dialogBlocksGameplay) {
            {
                this.transientVfxSystem.tick();
            }
            {
                this.droppableRupeeSystem.tick(
                        this.playerLink.position().pixelX(),
                        this.playerLink.position().pixelY(),
                        this.playerState);
            }
        }

        // ApplyGotItem $00:0FC7 runs before normal Link/item work. A
        // toadstool requested by that work starts decrementing next frame.
        {
            int itemDialog = this.playerLink.airborne().tickDialogGotItem(dialogBlocksGameplay);
            if (itemDialog == 2) {
                this.dialogController.openPreformattedForLinkY(
                        this.dialogTextLoader.load(new SignpostDialogRef(0, 0x0E)),
                        LinkDialogPosition.dialogYFromTopLeft(
                                this.playerLink.position().pixelY()));
            }
            if (this.playerLink.animation().presentedDialogGotItem() == 2) {
                this.gpu.updatePowderInventoryTiles(this.rom, true);
            }
        }
    }

    /** Named construction inputs, consumed once by the phase constructor. */
    public static final class Inputs {
        private DialogController dialogController;
        private DialogTextLoader dialogTextLoader;
        private DroppableRupeeSystem droppableRupeeSystem;
        private FrameClock frameCounter;
        private GameplaySounds gameplaySoundSink;
        private GPU gpu;
        private InventoryController inventoryController;
        private Link link;
        private PassOutController passOutController;
        private PlayerState playerState;
        private Rom romData;
        private RoomSession roomSession;
        private ScrollController scrollController;
        private TransientVfxSystem transientVfxSystem;
        private TransitionController transitionController;
        private ActiveLinkPhase active;
        private LinkGroundPhase ground;
        private GameOverActions gameOverOutcome;

        public Inputs dialogController(DialogController value) {
            this.dialogController = value;
            return this;
        }

        public Inputs dialogTextLoader(DialogTextLoader value) {
            this.dialogTextLoader = value;
            return this;
        }

        public Inputs droppableRupeeSystem(DroppableRupeeSystem value) {
            this.droppableRupeeSystem = value;
            return this;
        }

        public Inputs frameCounter(FrameClock value) {
            this.frameCounter = value;
            return this;
        }

        public Inputs gameplaySoundSink(GameplaySounds value) {
            this.gameplaySoundSink = value;
            return this;
        }

        public Inputs gpu(GPU value) {
            this.gpu = value;
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

        public Inputs passOutController(PassOutController value) {
            this.passOutController = value;
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

        public Inputs transientVfxSystem(TransientVfxSystem value) {
            this.transientVfxSystem = value;
            return this;
        }

        public Inputs transitionController(TransitionController value) {
            this.transitionController = value;
            return this;
        }

        public Inputs active(ActiveLinkPhase value) {
            this.active = value;
            return this;
        }

        public Inputs ground(LinkGroundPhase value) {
            this.ground = value;
            return this;
        }

        public Inputs gameOverOutcome(GameOverActions value) {
            this.gameOverOutcome = value;
            return this;
        }
    }
}
