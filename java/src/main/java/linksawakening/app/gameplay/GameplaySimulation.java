package linksawakening.app.gameplay;

import static linksawakening.world.room.RoomConstants.*;

import java.util.Objects;
import linksawakening.app.audio.MusicDirector;
import linksawakening.app.gameplay.entity.EntityPhase;
import linksawakening.app.gameplay.interaction.BushInteraction;
import linksawakening.app.gameplay.interaction.DialogBlockers;
import linksawakening.app.gameplay.interaction.DialogInteractions;
import linksawakening.app.gameplay.interaction.DialogSoundRouting;
import linksawakening.app.gameplay.interaction.LiftInteraction;
import linksawakening.app.gameplay.recovery.GameOverActions;
import linksawakening.app.gameplay.recovery.PassOutController;
import linksawakening.app.services.InputServices;
import linksawakening.app.services.PlayerServices;
import linksawakening.app.services.RomServices;
import linksawakening.app.services.UiServices;
import linksawakening.app.services.VisualServices;
import linksawakening.app.services.WorldServices;
import linksawakening.app.state.SessionState;
import linksawakening.app.story.OpeningStory;
import linksawakening.audio.music.FrameMusicPlayer;
import linksawakening.audio.music.GameplayMusicController;
import linksawakening.dialog.DialogController;
import linksawakening.entity.Link;
import linksawakening.equipment.EquipmentController;
import linksawakening.equipment.ItemRegistry;
import linksawakening.equipment.Sword;
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

/** ROM-ordered simulation between transition advancement and input completion. */
public final class GameplaySimulation implements GameplayFrame.Simulation {
    private final AnimatedTilesPhase animatedTilesPhase = new AnimatedTilesPhase();
    private final EntityPhase entityPhase;
    private final CombatPhase combatPhase = new CombatPhase();
    private final LinkEquipmentPhase linkEquipmentPhase = new LinkEquipmentPhase();
    private final DialogStoryPhase dialogStoryPhase = new DialogStoryPhase();
    private final RomServices rom;
    private final WorldServices world;
    private final PlayerServices player;
    private final UiServices ui;
    private final InputServices input;
    private final VisualServices visual;
    private final SessionState state;
    private final GameplayControl control;

    private final SimulationStages stages = new SimulationStages() {
        public boolean dialogStory() {
            return dialogStoryPhase.tick(GameplaySimulation.this);
        }

        public boolean dialogBlocks() {
            return dialogBlockers().current().pausesGameplay();
        }

        public void linkEquipment(boolean blocked, boolean inventory) {
            linkEquipmentPhase.tick(GameplaySimulation.this, blocked, inventory);
        }

        public boolean passingOut() {
            return link().status().isPassingOut() || passOutController().handledFrame();
        }

        public Sword.CollisionBox combat(boolean blocked) {
            return combatPhase.tick(GameplaySimulation.this, blocked);
        }

        public void entities(Sword.CollisionBox sword) {
            entityPhase.tick(GameplaySimulation.this, sword);
        }

        public void completeInput() {
            completeGameplayFrame(dialogController());
        }

        public void animatedTiles(boolean blocked) {
            animatedTilesPhase.tick(GameplaySimulation.this, blocked);
        }
    };

    public GameplaySimulation(
            RomServices rom,
            WorldServices world,
            PlayerServices player,
            UiServices ui,
            InputServices input,
            VisualServices visual,
            SessionState state,
            GameplayControl control) {
        this.rom = Objects.requireNonNull(rom);
        this.world = Objects.requireNonNull(world);
        this.player = Objects.requireNonNull(player);
        this.ui = Objects.requireNonNull(ui);
        this.input = Objects.requireNonNull(input);
        this.visual = Objects.requireNonNull(visual);
        this.state = Objects.requireNonNull(state);
        this.control = Objects.requireNonNull(control);
        entityPhase = new EntityPhase(
                world.roomSession(), player.link(), state.sounds(), visual.transientVfxSystem(), control.music());
    }

    public PassOutController passOutController() {
        return control.passOut();
    }

    public GameOverActions gameOverOutcome() {
        return control.gameOver();
    }

    public GPU gpu() {
        return rom.gpu();
    }

    public Rom romData() {
        return rom.romData();
    }

    public RoomSession roomSession() {
        return world.roomSession();
    }

    public TransitionController transitionController() {
        return world.transitionController();
    }

    public RoomTransitionCoordinator roomTransitionCoordinator() {
        return world.roomTransitionCoordinator();
    }

    public boolean debugNoClip() {
        return state.debug().debugNoClip;
    }

    public int frameCounter() {
        return state.clock().value();
    }

    public int ROOM_PIXEL_HEIGHT() {
        return 128;
    }

    public ScrollController scrollController() {
        return world.scrollController();
    }

    public InputConfig inputConfig() {
        return input.inputConfig();
    }

    public InputState inputState() {
        return input.inputState();
    }

    public PlayerState playerState() {
        return player.playerState();
    }

    public InventoryMenu inventoryMenu() {
        return ui.inventoryMenu();
    }

    public InventoryController inventoryController() {
        return ui.inventoryController();
    }

    public DialogController dialogController() {
        return ui.dialogController();
    }

    public DialogTextLoader dialogTextLoader() {
        return ui.dialogTextLoader();
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
        return visual.transientVfxSystem();
    }

    public DroppableRupeeSystem droppableRupeeSystem() {
        return visual.droppableRupeeSystem();
    }

    public ItemRegistry itemRegistry() {
        return player.itemRegistry();
    }

    public EquipmentController equipmentController() {
        return player.equipmentController();
    }

    public Link link() {
        return player.link();
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

    /** The simulation's strict per-frame order, including opening-story gates. */
    public void tick() {
        // GAMEPLAY_INVENTORY has its own handler ($00:0EA1/$20:5904):
        // world resources, effects, Link and entities do not advance behind it.
        boolean inventoryBlockedAtStart = inventoryController().shouldBlockOverworldInput();
        if (inventoryBlockedAtStart) {
            boolean worldHandler = inventoryMenu().transition().worldHandlerActive();
            boolean interactive = inventoryMenu().isFullyOpen();
            inventoryController().tick();
            if (interactive && inventoryMenu().isFullyOpen())
                inventoryController().dispatchMenuInput();
            if (!worldHandler) return;
        }
        stages.run(inventoryBlockedAtStart);
    }

    public static boolean shouldTickRoomEntities(
            boolean scrollActive,
            boolean transitionInputBlocked,
            boolean inventoryBlocksInput,
            boolean inventoryTransitioning) {
        return !scrollActive && !transitionInputBlocked && !inventoryBlocksInput;
    }

    public static void completeGameplayFrame(DialogController dialog) {
        if (dialog != null) {
            dialog.tickGameplayFrameCooldown();
        }
    }
}
