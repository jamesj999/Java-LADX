package linksawakening.app.gameplay;

import static linksawakening.world.room.RoomConstants.*;

import java.util.function.BooleanSupplier;
import java.util.function.IntUnaryOperator;
import java.util.function.Supplier;
import linksawakening.app.MusicDirector;
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
import linksawakening.gameplay.OverworldDialogBlockers;
import linksawakening.gpu.GPU;
import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
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
public final class GameplaySimulation {
    private final AnimatedTilesPhase animatedTilesPhase = new AnimatedTilesPhase();
    private final EntityPhase entityPhase = new EntityPhase();
    private final CombatPhase combatPhase = new CombatPhase();
    private final LinkEquipmentPhase linkEquipmentPhase = new LinkEquipmentPhase();
    private final DialogStoryPhase dialogStoryPhase = new DialogStoryPhase();
    private final linksawakening.app.SessionContext context;

    public GameplaySimulation(linksawakening.app.SessionContext context) {
        this.context = java.util.Objects.requireNonNull(context);
    }

    public PassOutController passOutController() {
        return context.simulation_passOutController();
    }

    public java.util.function.IntConsumer gameOverOutcome() {
        return context.simulation_gameOverOutcome();
    }

    public GPU gpu() {
        return context.simulation_gpu();
    }

    public byte[] romData() {
        return context.simulation_romData();
    }

    public RoomSession roomSession() {
        return context.simulation_roomSession();
    }

    public TransitionController transitionController() {
        return context.simulation_transitionController();
    }

    public RoomTransitionCoordinator roomTransitionCoordinator() {
        return context.simulation_roomTransitionCoordinator();
    }

    public boolean debugNoClip() {
        return context.simulation_debugNoClip();
    }

    public int frameCounter() {
        return context.simulation_frameCounter();
    }

    public int ROOM_PIXEL_HEIGHT() {
        return context.simulation_ROOM_PIXEL_HEIGHT();
    }

    public ScrollController scrollController() {
        return context.simulation_scrollController();
    }

    public InputConfig inputConfig() {
        return context.simulation_inputConfig();
    }

    public InputState inputState() {
        return context.simulation_inputState();
    }

    public PlayerState playerState() {
        return context.simulation_playerState();
    }

    public InventoryMenu inventoryMenu() {
        return context.simulation_inventoryMenu();
    }

    public InventoryController inventoryController() {
        return context.simulation_inventoryController();
    }

    public DialogController dialogController() {
        return context.simulation_dialogController();
    }

    public DialogTextLoader dialogTextLoader() {
        return context.simulation_dialogTextLoader();
    }

    public GameplaySoundSink gameplaySoundSink() {
        return context.simulation_gameplaySoundSink();
    }

    public FrameMusicPlayer musicPlayer() {
        return context.simulation_musicPlayer();
    }

    public GameplayMusicController gameplayMusicController() {
        return context.simulation_gameplayMusicController();
    }

    public MusicDirector musicDirector() {
        return context.simulation_musicDirector();
    }

    public TransientVfxSystem transientVfxSystem() {
        return context.simulation_transientVfxSystem();
    }

    public DroppableRupeeSystem droppableRupeeSystem() {
        return context.simulation_droppableRupeeSystem();
    }

    public ItemRegistry itemRegistry() {
        return context.simulation_itemRegistry();
    }

    public EquipmentController equipmentController() {
        return context.simulation_equipmentController();
    }

    public Link link() {
        return context.simulation_link();
    }

    public OpeningStory story() {
        return context.simulation_story();
    }

    public LiftInteraction lift() {
        return context.simulation_lift();
    }

    public BushInteraction bush() {
        return context.simulation_bush();
    }

    public DialogInteractions dialogInteractions() {
        return context.simulation_dialogInteractions();
    }

    public Supplier<OverworldDialogBlockers> dialogBlockers() {
        return context.simulation_dialogBlockers();
    }

    public BooleanSupplier debugScroll() {
        return context.simulation_debugScroll();
    }

    public IntUnaryOperator direction() {
        return context.simulation_direction();
    }

    public Runnable dialogSounds() {
        return context.simulation_dialogSounds();
    }

    /** The simulation's strict per-frame order, including opening-story gates. */
    public void tick() {
        // GAMEPLAY_INVENTORY has its own handler ($00:0EA1/$20:5904):
        // world resources, effects, Link and entities do not advance behind it.
        boolean inventoryBlockedAtStart =
                inventoryController() != null && inventoryController().shouldBlockOverworldInput();
        if (inventoryBlockedAtStart) {
            boolean worldHandler = inventoryMenu().transition().worldHandlerActive();
            boolean interactive = inventoryMenu().isFullyOpen();
            inventoryController().tick();
            if (interactive && inventoryMenu().isFullyOpen())
                inventoryController().dispatchMenuInput();
            if (!worldHandler) return;
        }
        if (!dialogStoryPhase.tick(this)) return;
        boolean dialogBlocksGameplay = dialogBlockers().get().pausesGameplay();
        linkEquipmentPhase.tick(this, dialogBlocksGameplay, inventoryBlockedAtStart);
        if (link() != null && (link().isPassingOut() || passOutController().handledFrame())) return;
        Sword.CollisionBox swordBox = combatPhase.tick(this, dialogBlocksGameplay);
        entityPhase.tick(this, swordBox);
        completeGameplayFrame(dialogController());
        animatedTilesPhase.tick(this, dialogBlocksGameplay);
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
