package linksawakening.app.gameplay;

import static linksawakening.world.room.RoomConstants.*;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;
import java.util.function.IntUnaryOperator;
import java.util.function.Supplier;
import linksawakening.app.MusicDirector;
import linksawakening.app.SessionContext;
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
    private final SessionContext context;

    public GameplaySimulation(SessionContext context) {
        this.context = Objects.requireNonNull(context);
    }

    public PassOutController passOutController() {
        return context.passOutController();
    }

    public IntConsumer gameOverOutcome() {
        return context.gameOverOutcome();
    }

    public GPU gpu() {
        return context.gpu();
    }

    public linksawakening.rom.Rom romData() {
        return context.romData();
    }

    public RoomSession roomSession() {
        return context.roomSession();
    }

    public TransitionController transitionController() {
        return context.transitionController();
    }

    public RoomTransitionCoordinator roomTransitionCoordinator() {
        return context.roomTransitionCoordinator();
    }

    public boolean debugNoClip() {
        return context.debugNoClip();
    }

    public int frameCounter() {
        return context.frameCounter();
    }

    public int ROOM_PIXEL_HEIGHT() {
        return context.roomPixelHeight();
    }

    public ScrollController scrollController() {
        return context.scrollController();
    }

    public InputConfig inputConfig() {
        return context.inputConfig();
    }

    public InputState inputState() {
        return context.inputState();
    }

    public PlayerState playerState() {
        return context.playerState();
    }

    public InventoryMenu inventoryMenu() {
        return context.inventoryMenu();
    }

    public InventoryController inventoryController() {
        return context.inventoryController();
    }

    public DialogController dialogController() {
        return context.dialogController();
    }

    public DialogTextLoader dialogTextLoader() {
        return context.dialogTextLoader();
    }

    public GameplaySoundSink gameplaySoundSink() {
        return context.gameplaySoundSink();
    }

    public FrameMusicPlayer musicPlayer() {
        return context.musicPlayer();
    }

    public GameplayMusicController gameplayMusicController() {
        return context.gameplayMusicController();
    }

    public MusicDirector musicDirector() {
        return context.musicDirector();
    }

    public TransientVfxSystem transientVfxSystem() {
        return context.transientVfxSystem();
    }

    public DroppableRupeeSystem droppableRupeeSystem() {
        return context.droppableRupeeSystem();
    }

    public ItemRegistry itemRegistry() {
        return context.itemRegistry();
    }

    public EquipmentController equipmentController() {
        return context.equipmentController();
    }

    public Link link() {
        return context.link();
    }

    public OpeningStory story() {
        return context.story();
    }

    public LiftInteraction lift() {
        return context.lift();
    }

    public BushInteraction bush() {
        return context.bush();
    }

    public DialogInteractions dialogInteractions() {
        return context.dialogInteractions();
    }

    public Supplier<OverworldDialogBlockers> dialogBlockers() {
        return context.dialogBlockers();
    }

    public BooleanSupplier debugScroll() {
        return context.simulationDebugScroll();
    }

    public IntUnaryOperator direction() {
        return context.direction();
    }

    public Runnable dialogSounds() {
        return context.dialogSounds();
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
        if (!dialogStoryPhase.tick(this)) return;
        boolean dialogBlocksGameplay = dialogBlockers().get().pausesGameplay();
        linkEquipmentPhase.tick(this, dialogBlocksGameplay, inventoryBlockedAtStart);
        if ((link().isPassingOut() || passOutController().handledFrame())) return;
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
