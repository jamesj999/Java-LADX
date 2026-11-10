package linksawakening.app.gameplay;



import linksawakening.app.MusicDirector;
import linksawakening.app.story.OpeningStory;

import linksawakening.dialog.DialogController;
import linksawakening.entity.Link;
import linksawakening.equipment.EquipmentController;
import linksawakening.equipment.ItemRegistry;
import linksawakening.equipment.Sword;
import linksawakening.audio.music.GameplayMusicController;
import linksawakening.audio.music.FrameMusicPlayer;
import linksawakening.gameplay.DialogTextLoader;
import linksawakening.gameplay.GameplaySoundSink;
import linksawakening.gameplay.OverworldDialogBlockers;
import linksawakening.gpu.GPU;
import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
import linksawakening.state.PlayerState;
import linksawakening.ui.InventoryMenu;
import linksawakening.ui.InventoryController;
import linksawakening.vfx.TransientVfxSystem;
import linksawakening.world.room.DroppableRupeeSystem;
import linksawakening.world.room.RoomSession;
import linksawakening.world.room.RoomTransitionCoordinator;
import linksawakening.world.transition.ScrollController;
import linksawakening.world.transition.TransitionController;

import static linksawakening.world.room.RoomConstants.*;

import java.util.function.Supplier;
import java.util.function.IntUnaryOperator;
import java.util.function.BooleanSupplier;

/** ROM-ordered simulation between transition advancement and input completion. */
public record GameplaySimulation(
        PassOutController passOutController,
        java.util.function.IntConsumer gameOverOutcome,
        GPU gpu,
        byte[] romData,
        RoomSession roomSession,
        TransitionController transitionController,
        RoomTransitionCoordinator roomTransitionCoordinator,
        boolean debugNoClip,
        int frameCounter,
        int ROOM_PIXEL_HEIGHT,
        ScrollController scrollController,
        InputConfig inputConfig,
        InputState inputState,
        PlayerState playerState,
        InventoryMenu inventoryMenu,
        InventoryController inventoryController,
        DialogController dialogController,
        DialogTextLoader dialogTextLoader,
        GameplaySoundSink gameplaySoundSink,
        FrameMusicPlayer musicPlayer,
        GameplayMusicController gameplayMusicController,
        MusicDirector musicDirector,
        TransientVfxSystem transientVfxSystem,
        DroppableRupeeSystem droppableRupeeSystem,
        ItemRegistry itemRegistry,
        EquipmentController equipmentController,
        Link link,
        OpeningStory story,
        LiftInteraction lift,
        BushInteraction bush,
        DialogInteractions dialogInteractions,
        Supplier<OverworldDialogBlockers> dialogBlockers,
        BooleanSupplier debugScroll,
        IntUnaryOperator direction,
        Runnable dialogSounds) {
    /** The simulation's strict per-frame order, including opening-story gates. */
    public void tick() {
        if (!new DialogStoryPhase().tick(this)) return;
        boolean dialogBlocksGameplay = dialogBlockers.get().pausesGameplay();
        new LinkEquipmentPhase().tick(this, dialogBlocksGameplay);
        if (link != null && (link.isPassingOut() || passOutController.handledFrame())) return;
        Sword.CollisionBox swordBox = new CombatPhase().tick(this, dialogBlocksGameplay);
        new EntityPhase().tick(this, swordBox);
        completeGameplayFrame(dialogController);
        new AnimatedTilesPhase().tick(this, dialogBlocksGameplay);
    }

    public static boolean shouldTickRoomEntities(boolean scrollActive,
                                          boolean transitionInputBlocked,
                                          boolean inventoryBlocksInput,
                                          boolean inventoryTransitioning) {
        return !scrollActive
            && !transitionInputBlocked
            && (!inventoryBlocksInput || inventoryTransitioning);
    }

    public static void completeGameplayFrame(DialogController dialog) {
        if (dialog != null) {
            dialog.tickGameplayFrameCooldown();
        }
    }

}
