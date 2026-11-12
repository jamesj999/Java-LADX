package linksawakening.app;

import java.util.function.BooleanSupplier;
import java.util.function.IntUnaryOperator;
import java.util.function.Supplier;
import linksawakening.app.gameplay.BushInteraction;
import linksawakening.app.gameplay.DialogInteractions;
import linksawakening.app.gameplay.LiftInteraction;
import linksawakening.app.gameplay.PassOutController;
import linksawakening.app.story.OpeningStory;
import linksawakening.audio.music.FrameMusicPlayer;
import linksawakening.audio.music.GameplayMusicController;
import linksawakening.dialog.DialogController;
import linksawakening.entity.Link;
import linksawakening.equipment.EquipmentController;
import linksawakening.equipment.ItemRegistry;
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

/** Live access to session-owned services and state. */
public final class SessionContext {
    private final GameSession session;

    public SessionContext(GameSession session) {
        this.session = java.util.Objects.requireNonNull(session);
    }

    public PassOutController simulation_passOutController() {
        return session.passOutController;
    }

    private java.util.function.IntConsumer simulation_gameOverOutcome;

    public java.util.function.IntConsumer simulation_gameOverOutcome() {
        if (simulation_gameOverOutcome == null) simulation_gameOverOutcome = session::finishGameOver;
        return simulation_gameOverOutcome;
    }

    public GPU simulation_gpu() {
        return session.gpu;
    }

    public byte[] simulation_romData() {
        return session.services.romData();
    }

    public RoomSession simulation_roomSession() {
        return session.services.roomSession();
    }

    public TransitionController simulation_transitionController() {
        return session.transitionController;
    }

    public RoomTransitionCoordinator simulation_roomTransitionCoordinator() {
        return session.services.roomTransitionCoordinator();
    }

    public boolean simulation_debugNoClip() {
        return session.debug.debugNoClip;
    }

    public int simulation_frameCounter() {
        return session.frameCounter;
    }

    public int simulation_ROOM_PIXEL_HEIGHT() {
        return session.ROOM_PIXEL_HEIGHT;
    }

    public ScrollController simulation_scrollController() {
        return session.scrollController;
    }

    public InputConfig simulation_inputConfig() {
        return session.services.inputConfig();
    }

    public InputState simulation_inputState() {
        return session.services.inputState();
    }

    public PlayerState simulation_playerState() {
        return session.services.playerState();
    }

    public InventoryMenu simulation_inventoryMenu() {
        return session.services.inventoryMenu();
    }

    public InventoryController simulation_inventoryController() {
        return session.services.inventoryController();
    }

    public DialogController simulation_dialogController() {
        return session.services.dialogController();
    }

    public DialogTextLoader simulation_dialogTextLoader() {
        return session.services.dialogTextLoader();
    }

    public GameplaySoundSink simulation_gameplaySoundSink() {
        return session.gameplaySoundSink;
    }

    public FrameMusicPlayer simulation_musicPlayer() {
        return session.services.musicPlayer();
    }

    public GameplayMusicController simulation_gameplayMusicController() {
        return session.services.gameplayMusicController();
    }

    public MusicDirector simulation_musicDirector() {
        return session.musicDirector;
    }

    public TransientVfxSystem simulation_transientVfxSystem() {
        return session.services.transientVfxSystem();
    }

    public DroppableRupeeSystem simulation_droppableRupeeSystem() {
        return session.services.droppableRupeeSystem();
    }

    public ItemRegistry simulation_itemRegistry() {
        return session.services.itemRegistry();
    }

    public EquipmentController simulation_equipmentController() {
        return session.services.equipmentController();
    }

    public Link simulation_link() {
        return session.services.link();
    }

    public OpeningStory simulation_story() {
        return session.openingStory();
    }

    public LiftInteraction simulation_lift() {
        return session.lifting();
    }

    public BushInteraction simulation_bush() {
        return session.bushes();
    }

    public DialogInteractions simulation_dialogInteractions() {
        return session.dialogs();
    }

    private Supplier<OverworldDialogBlockers> simulation_dialogBlockers;

    public Supplier<OverworldDialogBlockers> simulation_dialogBlockers() {
        if (simulation_dialogBlockers == null) simulation_dialogBlockers = session::currentOverworldDialogBlockers;
        return simulation_dialogBlockers;
    }

    private BooleanSupplier simulation_debugScroll;

    public BooleanSupplier simulation_debugScroll() {
        if (simulation_debugScroll == null)
            simulation_debugScroll = () -> session.inputRouter().tryDebugScreenScroll();
        return simulation_debugScroll;
    }

    private IntUnaryOperator simulation_direction;

    public IntUnaryOperator simulation_direction() {
        if (simulation_direction == null) simulation_direction = session::romDirectionForLink;
        return simulation_direction;
    }

    private Runnable simulation_dialogSounds;

    public Runnable simulation_dialogSounds() {
        if (simulation_dialogSounds == null) simulation_dialogSounds = session::routeDialogSounds;
        return simulation_dialogSounds;
    }
}
