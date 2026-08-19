package linksawakening;

import linksawakening.config.AppConfig;
import linksawakening.cutscene.CutsceneManager;
import linksawakening.cutscene.IntroCutsceneScript;
import linksawakening.dialog.DialogController;
import linksawakening.entity.Link;
import linksawakening.entity.LinkSpriteSheet;
import linksawakening.entity.LinkTunicPalette;
import linksawakening.equipment.EquipmentController;
import linksawakening.equipment.Arrow;
import linksawakening.equipment.Bomb;
import linksawakening.equipment.Boomerang;
import linksawakening.equipment.Hookshot;
import linksawakening.equipment.ItemRegistry;
import linksawakening.equipment.MagicPowder;
import linksawakening.equipment.MagicRod;
import linksawakening.equipment.Ocarina;
import linksawakening.equipment.RocsFeather;
import linksawakening.equipment.Shovel;
import linksawakening.equipment.Shield;
import linksawakening.equipment.Sword;
import linksawakening.equipment.SwordPalette;
import linksawakening.equipment.SwordSpriteSheet;
import linksawakening.audio.apu.GameBoyApu;
import linksawakening.audio.music.AreaMusicResolver;
import linksawakening.audio.music.GameplayMusicController;
import linksawakening.audio.music.MusicCatalog;
import linksawakening.audio.music.MusicDriver;
import linksawakening.audio.music.MusicTrackIds;
import linksawakening.audio.music.RoomMusicContext;
import linksawakening.audio.openal.OpenAlMusicPlayer;
import linksawakening.audio.openal.OpenAlPcmSoundOutput;
import linksawakening.gameplay.BeachSwordRewardConsumer;
import linksawakening.gameplay.DialogTextLoader;
import linksawakening.gameplay.DialogSoundRouter;
import linksawakening.gameplay.DialogSoundSink;
import linksawakening.gameplay.EnemyCombatEventConsumer;
import linksawakening.gameplay.EnemyProjectileEventConsumer;
import linksawakening.gameplay.GameplaySoundSink;
import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.gameplay.GameplayDialogInput;
import linksawakening.gameplay.LinkDialogPosition;
import linksawakening.gameplay.OverworldDialogBlockers;
import linksawakening.gameplay.OverworldDialogInteraction;
import linksawakening.gameplay.PcmSoundOutput;
import linksawakening.gameplay.SfxDialogSoundSink;
import linksawakening.gameplay.SfxGameplaySoundSink;
import linksawakening.gameplay.SignpostDialogRef;
import linksawakening.gameplay.SignpostDialogTable;
import linksawakening.gpu.GPU;
import linksawakening.gpu.Framebuffer;
import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
import linksawakening.physics.OverworldCollision;
import linksawakening.render.GameFrameSceneBuilder;
import linksawakening.render.GameFrameState;
import linksawakening.render.OpenGlFramePresenter;
import linksawakening.render.RenderScreen;
import linksawakening.rom.RomTables;
import linksawakening.rom.RomBank;
import linksawakening.save.SaveRamStore;
import linksawakening.save.SaveSlotState;
import linksawakening.scene.BackgroundScene;
import linksawakening.scene.BackgroundSceneCatalog;
import linksawakening.scene.BackgroundSceneLoader;
import linksawakening.scene.BackgroundSceneSpec;
import linksawakening.state.PlayerState;
import linksawakening.startup.NewGameStartProfile;
import linksawakening.startup.MarinWakeUpMotion;
import linksawakening.startup.StartupCoordinator;
import linksawakening.startup.TarinShieldMotion;
import linksawakening.ui.InventoryMenu;
import linksawakening.ui.InventoryController;
import linksawakening.ui.InventoryTilemapLoader;
import linksawakening.ui.FileMenuAction;
import linksawakening.ui.FileMenuController;
import linksawakening.ui.FileMenuRomData;
import linksawakening.ui.FileSaveAction;
import linksawakening.ui.FileSaveController;
import linksawakening.vfx.CutLeavesEffectRenderer;
import linksawakening.vfx.TransientVfxSpriteSheet;
import linksawakening.vfx.TransientVfxSystem;
import linksawakening.vfx.TransientVfxType;
import linksawakening.world.DroppableRupeeSystem;
import linksawakening.world.ActiveRoom;
import linksawakening.world.EnemyAttackContext;
import linksawakening.world.EntityCombatEvent;
import linksawakening.world.EntityPickupEvent;
import linksawakening.world.EntityProjectileEvent;
import linksawakening.world.OverworldBushInteraction;
import linksawakening.world.OverworldTilesetTable;
import linksawakening.world.RoomBoundaryController;
import linksawakening.world.RoomLoader;
import linksawakening.world.RoomSession;
import linksawakening.world.RoomTransitionCoordinator;
import linksawakening.world.ScrollController;
import linksawakening.world.TransitionController;
import linksawakening.world.Warp;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWKeyCallback;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ThreadLocalRandom;

import static linksawakening.world.RoomConstants.*;
import static org.lwjgl.glfw.GLFW.*;

public class Main {

    private static final int OBJECT_GROUND_STAIRS = 0xC6;
    private static final int GAME_BOY_BG_MAP_TILES = 32 * 32;

    private static long window;
    private static GPU gpu;
    private static OpenGlFramePresenter framePresenter;
    private static final GameFrameSceneBuilder frameSceneBuilder = new GameFrameSceneBuilder();
    private static byte[] romData;
    private static int[] currentTilemap;
    private static int[] currentAttrmap;
    private static int[][] bgPalettes;
    private static int[][] objPalettes;
    private static byte[] indexedDisplayBuffer;
    private static RoomSession roomSession;
    private static final TransitionController transitionController = new TransitionController();
    private static RoomTransitionCoordinator roomTransitionCoordinator;

    private static boolean running = true;
    private static boolean debugNoClip;
    private static int frameCounter;
    private static int currentScreen = 0;
    static final int SCREEN_TITLE = 0;
    static final int SCREEN_OVERWORLD = 1;
    static final int SCREEN_CUTSCENE = 2;
    static final int SCREEN_FILE_MENU = 3;
    static final int SCREEN_FILE_SAVE = 4;

    // Room pixel dimensions
    private static final int ROOM_PIXEL_WIDTH = ROOM_TILE_WIDTH * 8;   // 160
    private static final int ROOM_PIXEL_HEIGHT = ROOM_TILE_HEIGHT * 8; // 128

    // Scroll state
    private static final int SCROLL_SPEED = 4; // pixels per frame (original uses ~2-4px)

    private static final ScrollController scrollController = new ScrollController();
    // Mirrors the first OBJ palette entry used for Link and leaf particles.
    private static final int[] GREEN_OBJECTS_SPRITE_PALETTE = {
        0x00000000,
        0x00000000,
        0x0010A840,
        0x00F8B888,
    };

    private static InputConfig inputConfig;
    private static InputState inputState;
    private static PlayerState playerState;
    private static BackgroundSceneLoader backgroundSceneLoader;
    private static InventoryMenu inventoryMenu;
    private static InventoryController inventoryController;
    private static DialogController dialogController;
    private static DialogTextLoader dialogTextLoader;
    private static OverworldDialogInteraction overworldDialogInteraction;
    private static boolean overworldDialogInputConsumedThisFrame;
    private static DialogSoundSink dialogSoundSink;
    private static PcmSoundOutput dialogSoundOutput;
    private static GameplaySoundSink gameplaySoundSink = GameplaySoundSink.none();
    private static OpenAlMusicPlayer musicPlayer;
    private static GameplayMusicController gameplayMusicController;
    private static boolean overworldIntroContinuationPending;
    private static CutsceneManager cutsceneManager;
    private static FileMenuController fileMenuController;
    private static FileSaveController fileSaveController;
    private static SaveRamStore saveRamStore;
    private static int currentSaveSlot = -1;
    private static RomTables romTables;
    private static OverworldCollision overworldCollision;
    private static OverworldBushInteraction overworldBushInteraction;
    private static LinkSpriteSheet linkSpriteSheet;
    private static TransientVfxSystem transientVfxSystem;
    private static DroppableRupeeSystem droppableRupeeSystem;
    private static CutLeavesEffectRenderer cutLeavesEffectRenderer;
    private static ItemRegistry itemRegistry;
    private static EquipmentController equipmentController;
    private static Link link;
    private static MarinWakeUpMotion newGameWakeUpMotion;
    private static TarinShieldMotion newGameTarinShieldMotion;
    private static int[] newGameTarinShieldPalette;
    private static AppConfig appConfig;


    private static final long FRAME_DURATION_NS = 1_000_000_000L / 60L;

    public static void main(String[] args) {
        gpu = new GPU();

        loadROM();
        transitionController.setRomData(romData);
        appConfig = AppConfig.loadFromResources();
        loadGraphicsData();
        initMenuSystem();
        if (StartupCoordinator.shouldStartIntroCutscene(currentAppConfig())) {
            startIntroCutscene();
        } else if (currentAppConfig().startMode() == AppConfig.StartMode.NORMAL
            || !currentAppConfig().showTitleScreen()) {
            startConfiguredGameplay();
        } else {
            startTitleScreenWithoutIntro();
        }

        initGLFW();
        initOpenGL();

        long nextFrameNs = System.nanoTime();

        while (!glfwWindowShouldClose(window) && running) {
            glfwPollEvents();
            update();
            composeFrameBuffer();
            framePresenter.present(indexedDisplayBuffer);

            nextFrameNs += FRAME_DURATION_NS;
            long sleepNs = nextFrameNs - System.nanoTime();
            if (sleepNs > 0) {
                try {
                    Thread.sleep(sleepNs / 1_000_000L, (int) (sleepNs % 1_000_000L));
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            } else {
                // We're behind — drop the backlog so we don't spiral.
                nextFrameNs = System.nanoTime();
            }
        }

        cleanup();
    }

    private static void loadROM() {
        try (InputStream is = Main.class.getClassLoader().getResourceAsStream("rom/azle.gbc")) {
            if (is == null) {
                throw new IllegalStateException("ROM not found in resources");
            }

            romData = is.readAllBytes();
            System.out.println("ROM loaded: " + romData.length + " bytes");
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load ROM", e);
        }
    }

    private static void initMenuSystem() {
        try {
            saveRamStore = SaveRamStore.open(SaveRamStore.defaultPath());
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to open save image", exception);
        }
        inputConfig = currentAppConfig().inputConfig();
        inputState = new InputState();
        playerState = new PlayerState();
        initMusicSystem();
        dialogSoundOutput = new OpenAlPcmSoundOutput();
        dialogSoundSink = new SfxDialogSoundSink(dialogSoundOutput);
        gameplaySoundSink = new SfxGameplaySoundSink(romData, dialogSoundOutput);
        InventoryTilemapLoader tilemapLoader = InventoryTilemapLoader.loadFromRom(romData);
        inventoryMenu = new InventoryMenu(tilemapLoader, playerState, gameplaySoundSink, romData);
        inventoryController = new InventoryController(inputState, inputConfig, inventoryMenu);
        dialogController = new DialogController(16);
        dialogTextLoader = DialogTextLoader.loadFromRom(romData);
        overworldDialogInteraction = new OverworldDialogInteraction(
            SignpostDialogTable.loadFromRom(romData), dialogTextLoader);
        cutsceneManager = new CutsceneManager(dialogController, Main::setCutsceneScene);
        romTables = RomTables.loadFromRom(romData);
        newGameTarinShieldPalette = RomBank.loadPalettes(romData, 0x05, 0x4C94, 1)[0];
        overworldCollision = new OverworldCollision(romTables);
        overworldBushInteraction = new OverworldBushInteraction(romData, romTables);
        linkSpriteSheet = LinkSpriteSheet.loadFromRom(romData);
        SwordSpriteSheet swordSpriteSheet = SwordSpriteSheet.loadFromRom(romData);
        SwordPalette swordPalette = SwordPalette.loadFromRom(romData);
        TransientVfxSpriteSheet transientVfxSpriteSheet = TransientVfxSpriteSheet.loadFromRom(romData);
        transientVfxSystem = new TransientVfxSystem(16);
        droppableRupeeSystem = new DroppableRupeeSystem(16,
            () -> ThreadLocalRandom.current().nextInt(0x100));
        cutLeavesEffectRenderer = new CutLeavesEffectRenderer(transientVfxSpriteSheet);
        itemRegistry = new ItemRegistry();
        roomSession = new RoomSession(romData, gpu, new RoomLoader(romData),
            new OverworldTilesetTable(romData), overworldCollision, transientVfxSystem, droppableRupeeSystem,
            Main::selectMusicForLoadedRoom);
        roomSession.setColorShellSoundSink(gameplaySoundSink);
        LinkTunicPalette linkTunicPalette = LinkTunicPalette.loadFromRom(romData);
        link = new Link(inputState, inputConfig, romTables, overworldCollision,
                        linkSpriteSheet, playerState, itemRegistry, gameplaySoundSink,
                        linkTunicPalette);
        itemRegistry.register(PlayerState.INVENTORY_OCARINA, new Ocarina(
            playerState, gameplaySoundSink, new Ocarina.PlaybackTarget() {
                @Override
                public boolean startOcarina(int countdown, int songFlags, int selectedSong) {
                    return roomSession != null
                        && roomSession.startOcarina(countdown, songFlags, selectedSong);
                }

                @Override
                public boolean ocarinaPlaying() {
                    return roomSession != null && roomSession.ocarinaPlaying();
                }

                @Override
                public boolean canStartOcarina() {
                    return link != null && roomSession != null && link.canUseItems()
                        && !link.isAirborne() && !roomSession.hookshotActive();
                }

                @Override
                public int ocarinaAnimationState() {
                    return roomSession == null ? -1 : roomSession.ocarinaAnimationState();
                }
            }));
        itemRegistry.register(PlayerState.INVENTORY_SWORD, new Sword(romTables, swordSpriteSheet,
            gameplaySoundSink, () -> ThreadLocalRandom.current().nextInt(0x100), swordPalette,
            new Sword.BeamTarget() {
                @Override
                public boolean fireSwordBeam() {
                    if (link == null || roomSession == null || playerState == null) {
                        return false;
                    }
                    int romDirection = link.applyRomItemDirectionFromInput();
                    if (playerState.swordLevel() != 2
                        || playerState.health() != playerState.maxHearts()
                            * PlayerState.HP_PER_HEART
                        || roomSession.activeProjectileCount() != 0) {
                        return false;
                    }
                    boolean spawned = roomSession.fireSwordBeam(
                        link.romEntityX(), link.romEntityY(), link.romEntityZ(), romDirection);
                    if (spawned) {
                        link.startRomItemAttackStep();
                    }
                    return spawned;
                }
            }));
        itemRegistry.register(PlayerState.INVENTORY_MAGIC_ROD, new MagicRod(
            romTables, linkSpriteSheet, linkTunicPalette, gameplaySoundSink,
            new MagicRod.LaunchTarget() {
                @Override
                public void startMagicRodAttackStep() {
                    if (link != null) {
                        link.startRomMagicRodAttackStep();
                    }
                }

                @Override
                public boolean fireMagicRodFireball() {
                    if (link == null || roomSession == null) {
                        return false;
                    }
                    int romDirection = link.applyRomItemDirectionFromInput();
                    boolean spawned = roomSession.fireMagicRodFireball(
                        link.romEntityX(), link.romEntityY(), link.romEntityZ(), romDirection);
                    return spawned;
                }

                @Override
                public int activeProjectileCount() {
                    return roomSession == null ? 0 : roomSession.activeProjectileCount();
                }
            }, link::canUseItems));
        itemRegistry.register(PlayerState.INVENTORY_MAGIC_POWDER, new MagicPowder(
            playerState, gameplaySoundSink, new MagicPowder.SprinkleTarget() {
                @Override
                public boolean sprinkleMagicPowder() {
                    if (link == null || roomSession == null) {
                        return false;
                    }
                    int romDirection = link.applyRomItemDirectionFromInput();
                    return roomSession.sprinkleMagicPowder(
                        link.romEntityX(), link.romEntityY(), link.romEntityZ(), romDirection);
                }

                @Override
                public void startMagicPowderAttackStep() {
                    if (link != null) {
                        link.startRomMagicPowderAttackStep();
                    }
                }

                @Override
                public boolean magicPowderAttackStepActive() {
                    return link != null && link.romAttackStepAnimationCountdown() != 0;
            }
        }, link::canUseItems));
        itemRegistry.register(PlayerState.INVENTORY_SHIELD,
            new Shield(gameplaySoundSink, link::canUseItems));
        itemRegistry.register(PlayerState.INVENTORY_SHOVEL, new Shovel(
            gameplaySoundSink, new Shovel.DigTarget() {
                private int linkEntityX;
                private int linkEntityY;
                private int romDirection;

                @Override
                public Shovel.StartResult startShovel() {
                    if (link == null || roomSession == null) {
                        return new Shovel.StartResult(false, true);
                    }
                    romDirection = link.applyRomItemDirectionFromInput();
                    linkEntityX = link.romEntityX();
                    linkEntityY = link.romEntityY();
                    RoomSession.ShovelStartResult result = roomSession.startShovel(
                        linkEntityX, linkEntityY, romDirection, link.isAirborne());
                    return new Shovel.StartResult(result.started(), result.poking());
                }

                @Override
                public void advanceShovel(int timer) {
                    if (roomSession != null) {
                        roomSession.advanceShovel(linkEntityX, linkEntityY, romDirection, timer);
                    }
                }

                @Override
                public int shovelAnimationState(int javaDirection, int timer) {
                    return roomSession == null ? -1
                        : roomSession.shovelAnimationState(javaDirection, timer);
                }
            }, link::canUseItems));
        itemRegistry.register(PlayerState.INVENTORY_ROCS_FEATHER, new RocsFeather(link));
        itemRegistry.register(PlayerState.INVENTORY_BOMBS, new Bomb(
            playerState, gameplaySoundSink, new Bomb.PlacementTarget() {
                private boolean playBumpForLastPlacement;

                @Override
                public boolean placeBomb() {
                    if (link == null || roomSession == null) {
                        playBumpForLastPlacement = false;
                        return false;
                    }
                    boolean placed = roomSession.placeBomb(
                        link.romEntityX(), link.romEntityY(), link.romEntityZ(),
                        romDirectionForLink(link.direction()));
                    playBumpForLastPlacement = placed
                        && roomSession.lastBombPlacementPlayedBump();
                    if (placed) {
                        link.startRomItemAttackStep();
                    }
                    return placed;
                }

                @Override
                public boolean playBumpForLastPlacement() {
                    return playBumpForLastPlacement;
                }

                @Override
                public boolean bombActive() {
                    return roomSession != null && roomSession.bombActive();
                }
            }, link::canUseItems));
        itemRegistry.register(PlayerState.INVENTORY_BOW, new Arrow(
            playerState, gameplaySoundSink, new Arrow.ShootTarget() {
                private boolean playWhooshForLastShot;

                @Override
                public boolean shootArrow() {
                    if (link == null || roomSession == null) {
                        playWhooshForLastShot = false;
                        return false;
                    }
                    RoomSession.ArrowShotResult result = roomSession.shootArrowResult(
                        link.romEntityX(), link.romEntityY(), link.romEntityZ(),
                        link.applyRomItemDirectionFromInput(),
                        playerState.activePowerUp()
                            == PlayerState.ACTIVE_POWER_UP_PIECE_OF_POWER);
                    playWhooshForLastShot = result.playWhoosh();
                    if (result.spawned()) {
                        link.startRomItemAttackStep();
                    }
                    return result.spawned();
                }

                @Override
                public boolean playWhooshForLastShot() {
                    return playWhooshForLastShot;
                }

                @Override
                public int activeProjectileCount() {
                    return roomSession == null ? 0 : roomSession.activeProjectileCount();
                }
            }, link::canUseItems));
        itemRegistry.register(PlayerState.INVENTORY_HOOKSHOT, new Hookshot(
            new Hookshot.LaunchTarget() {
                @Override
                public boolean fireHookshot() {
                    if (link == null || roomSession == null) {
                        return false;
                    }
                    return roomSession.fireHookshot(
                        link.romEntityX(), link.romEntityY(), link.romEntityZ(),
                        romDirectionForLink(link.direction()), link.isAirborne(),
                        link.isRomLinkPushing());
                }

                @Override
                public boolean hookshotActive() {
                    return roomSession != null && roomSession.hookshotActive();
                }
            }));
        itemRegistry.register(PlayerState.INVENTORY_BOOMERANG, new Boomerang(
            new Boomerang.LaunchTarget() {
                @Override
                public boolean fireBoomerang() {
                    if (link == null || roomSession == null) {
                        return false;
                    }
                    boolean spawned = roomSession.fireBoomerang(
                        link.romEntityX(), link.romEntityY(), link.romEntityZ(),
                        link.applyRomItemDirectionFromInput(),
                        link.romPressedButtonsMask());
                    if (spawned) {
                        link.startRomItemAttackStep();
                    }
                    return spawned;
                }

                @Override
                public boolean boomerangActive() {
                    return roomSession != null && roomSession.boomerangActive();
                }
            }, link::canUseItems));
        equipmentController = new EquipmentController(inputState, inputConfig, playerState, itemRegistry);
        roomTransitionCoordinator = new RoomTransitionCoordinator(
            roomSession, new RoomBoundaryController(), transitionController, scrollController);
    }

    private static void initMusicSystem() {
        GameBoyApu musicApu = new GameBoyApu(48_000);
        MusicCatalog musicCatalog = MusicCatalog.fromRom(romData);
        musicPlayer = new OpenAlMusicPlayer(new MusicDriver(romData, musicApu), musicApu);
        // ROM background tracks contain their own loop commands. Host-level
        // restarting would also loop finite fanfares such as $1B forever,
        // preventing wActiveMusicIndex-gated acquisition handlers advancing.
        musicPlayer.setLoopEnabled(false);
        gameplayMusicController = new GameplayMusicController(
            AreaMusicResolver.fromRom(romData), musicCatalog, musicPlayer);
    }

    private static void loadGraphicsData() {
        if (romData == null || romData.length < 0x8000) {
            throw new IllegalStateException("ROM data is not available");
        }

        backgroundSceneLoader = new BackgroundSceneLoader(romData);
        gpu.loadTitleScreenTiles(romData);
        applyBackgroundScene(backgroundSceneLoader.load(BackgroundSceneCatalog.TITLE));
        indexedDisplayBuffer = new byte[Framebuffer.WIDTH * Framebuffer.HEIGHT * 4];

        if (currentTilemap.length != GAME_BOY_BG_MAP_TILES) {
            throw new IllegalStateException("Decoded title tilemap has unexpected size: " + currentTilemap.length);
        }
        if (currentAttrmap.length != GAME_BOY_BG_MAP_TILES) {
            throw new IllegalStateException("Decoded title attrmap has unexpected size: " + currentAttrmap.length);
        }
    }

    private static void onKeyEvent(int key, int action) {
        if (inputState != null) {
            inputState.onKeyEvent(key, action);
        }

        if (key == GLFW_KEY_ESCAPE && action == GLFW_PRESS) {
            running = false;
        }

        if (action == GLFW_PRESS && currentAppConfig().debugEnabled()) {
            if (key == GLFW_KEY_F2) {
                startDebugGameplay();
                return;
            }
            if (key == GLFW_KEY_F3 && link != null) {
                debugNoClip = !debugNoClip;
                link.setDebugNoClip(debugNoClip);
                System.out.println("Debug no-clip: " + (debugNoClip ? "ON" : "OFF"));
                return;
            }
        }

        if (currentScreen == SCREEN_TITLE) {
            if (shouldEnterFileSelection(currentScreen, key, action)) {
                startFileSelection();
            }
            return;
        }

        if (currentScreen == SCREEN_CUTSCENE) {
            if (skipIntroCutsceneIfRequested(key, action, cutsceneManager)) {
                currentScreen = SCREEN_TITLE;
                playDirectMusic(directTitleScreenMusicTrack());
                return;
            }
            if (action == GLFW_PRESS && dialogController != null
                && (key == inputConfig.aKey() || key == inputConfig.bKey() || key == GLFW_KEY_ENTER)) {
                dialogController.advance();
            }
            return;
        }

        if (currentScreen == SCREEN_FILE_MENU) {
            return;
        }

        if (currentScreen == SCREEN_FILE_SAVE) {
            return;
        }

        if (currentScreen != SCREEN_OVERWORLD || action != GLFW_PRESS) {
            return;
        }

        if (roomSession != null && roomSession.tailCaveKeyholeSequenceActive()) {
            return;
        }

        if (marinWakeUpBlocksKeyInput()) {
            if (GameplayDialogInput.handleOverworldKeyPress(
                key, action, inputConfig, dialogController)) {
                overworldDialogInputConsumedThisFrame = true;
            }
            return;
        }

        if (shouldEnterFileSave(inputConfig, inputState, key, action)
            && canPresentFileSave()) {
            startFileSave();
            return;
        }

        if (GameplayDialogInput.handleOverworldKeyPress(key, action, inputConfig, dialogController)) {
            overworldDialogInputConsumedThisFrame = true;
            return;
        }
        if (tryOpenTarinShieldDialog(key)) {
            return;
        }
        if (tryOpenMarinFollowUpDialog(key)) {
            return;
        }
        if (tryOpenChest(key)) {
            return;
        }
        if (tryOpenOverworldDialog(key)) {
            return;
        }

        inventoryController.dispatchToggleInput();

        // Debug: F1 dumps Link's surrounding cells with object IDs and
        // physics flags, to diagnose walk-through-wall/tree reports.
        if (shouldRunDebugDump(currentAppConfig(), key, action)) {
            dumpLinkSurroundings();
        }
    }

    static boolean skipIntroCutsceneIfRequested(int key, int action, CutsceneManager manager) {
        return key == GLFW_KEY_ENTER
            && action == GLFW_PRESS
            && manager != null
            && manager.skipIntroToTitle();
    }

    static boolean shouldRunDebugDump(AppConfig config, int key, int action) {
        return config.debugEnabled() && key == GLFW_KEY_F1 && action == GLFW_PRESS;
    }

    static boolean shouldTickRoomEntities(boolean scrollActive,
                                          boolean transitionInputBlocked,
                                          boolean inventoryBlocksInput,
                                          boolean inventoryTransitioning) {
        return !scrollActive
            && !transitionInputBlocked
            && (!inventoryBlocksInput || inventoryTransitioning);
    }

    static void completeGameplayFrame(DialogController dialog) {
        if (dialog != null) {
            dialog.tickGameplayFrameCooldown();
        }
    }

    private static void dumpLinkSurroundings() {
        if (link == null || roomSession == null || !roomSession.hasActiveRoom()) return;
        ActiveRoom room = roomSession.activeRoom();
        int[] roomObjectsArea = room.roomObjectsArea();
        int lx = link.pixelX();
        int ly = link.pixelY();
        System.out.println("=== Link debug dump ===");
        System.out.println("roomId=" + String.format("%02X", room.roomId())
            + " mapCat=" + room.mapCategory() + " mapId=" + String.format("%02X", room.mapId()));
        System.out.println("Link pixel (" + lx + "," + ly + ") tile="
            + String.format("%02X", Warp.packTileLocation(lx, ly)));
        int centerCol = (lx + 8) >> 4;
        int centerRow = (ly + 8) >> 4;
        int tableIdx = (room.mapCategory() == Warp.CATEGORY_OVERWORLD)
            ? RomTables.PHYSICS_TABLE_OVERWORLD : RomTables.PHYSICS_TABLE_INDOORS1;
        for (int dy = -1; dy <= 2; dy++) {
            StringBuilder sb = new StringBuilder(" row " + (centerRow + dy) + ":");
            for (int dx = -1; dx <= 2; dx++) {
                int col = centerCol + dx;
                int row = centerRow + dy;
                if (col < 0 || col > 9 || row < 0 || row > 7) {
                    sb.append(" --");
                    continue;
                }
                int areaIndex = ROOM_OBJECTS_BASE + row * ROOM_OBJECT_ROW_STRIDE + col;
                int id = roomObjectsArea[areaIndex];
                int flag = romTables.objectPhysicsFlag(tableIdx, id);
                sb.append(" id=").append(String.format("%02X", id))
                  .append("(fl=").append(String.format("%02X", flag)).append(")");
            }
            System.out.println(sb.toString());
        }
    }

    private static void update() {
        frameCounter = (frameCounter + 1) & 0xFF;

        if (musicPlayer != null) {
            musicPlayer.update();
            if (overworldIntroContinuationPending
                && gameplayMusicController != null
                && gameplayMusicController.currentTrackId()
                    == MusicTrackIds.MUSIC_OVERWORLD_INTRO
                && !musicPlayer.isMusicPlaying()) {
                overworldIntroContinuationPending = false;
                gameplayMusicController.playDirect(MusicTrackIds.MUSIC_OVERWORLD);
            }
        }

        if (currentScreen == SCREEN_CUTSCENE) {
            if (dialogController != null) {
                dialogController.tick();
                routeDialogSounds();
            }
            if (cutsceneManager != null && cutsceneManager.isActive()) {
                cutsceneManager.tick();
            }
            if (cutsceneManager != null && !cutsceneManager.isActive()
                && cutsceneManager.isShowingTitleScene()) {
                currentScreen = SCREEN_TITLE;
            }
            inputState.tickEdges();
            return;
        }

        if (currentScreen == SCREEN_FILE_MENU) {
            if (fileMenuController == null) {
                throw new IllegalStateException("File menu controller is not initialized");
            }
            FileMenuAction action = fileMenuController.tick(inputState, inputConfig);
            if (action.type() == FileMenuAction.Type.START_NEW_GAME) {
                try {
                    saveRamStore.createNewGame(action.selectedSlot(), action.nameBytes());
                    saveRamStore.flush();
                } catch (IOException exception) {
                    throw new IllegalStateException("Failed to persist new save file", exception);
                }
                currentSaveSlot = action.selectedSlot();
                startNewGame();
            } else if (action.type() == FileMenuAction.Type.LOAD_GAME) {
                SaveSlotState saved = saveRamStore.readSlot(action.selectedSlot());
                currentSaveSlot = action.selectedSlot();
                if (saved.spawnPositionX() == 0) {
                    startNewGame();
                } else {
                    startSavedGame(saved);
                }
            } else if (action.type() == FileMenuAction.Type.ERASE_SLOT) {
                persistFileMenuMutation(() -> saveRamStore.eraseSlot(action.selectedSlot()));
            } else if (action.type() == FileMenuAction.Type.COPY_SLOT) {
                persistFileMenuMutation(() ->
                    saveRamStore.copySlot(action.selectedSlot(), action.targetSlot()));
            }
            inputState.tickEdges();
            overworldDialogInputConsumedThisFrame = false;
            return;
        }

        if (currentScreen == SCREEN_FILE_SAVE) {
            if (fileSaveController == null) {
                throw new IllegalStateException("File save controller is not initialized");
            }
            FileSaveAction action = fileSaveController.tick(inputState, inputConfig);
            if (action.type() == FileSaveAction.Type.RETURN_TO_GAME) {
                fileSaveController = null;
                currentScreen = SCREEN_OVERWORLD;
            } else if (action.type() == FileSaveAction.Type.SAVE_AND_QUIT) {
                saveCurrentPlayerState();
                startFileSelection();
            }
            inputState.tickEdges();
            overworldDialogInputConsumedThisFrame = false;
            return;
        }

        if (scrollController.isActive()) {
            scrollController.tick(SCROLL_SPEED);
        }

        // Tick the warp-transition state machine. When it's not IDLE we
        // disable Link movement, edge-scrolling, and animated-tile updates,
        // matching the disassembly's gating on wLinkMotionState ==
        // LINK_MOTION_MAP_FADE_OUT (bank0.asm:888-899).
        transitionController.tick();

        if (currentScreen == SCREEN_OVERWORLD) {
            scrollController.tickScreenShake();
            if (dialogController != null) {
                dialogController.tick();
                routeDialogSounds();
            }
            updateNewGameMarinPresentation();
            if (tickNewGameWakeUp()) {
                completeGameplayFrame(dialogController);
                inputState.tickEdges();
                overworldDialogInputConsumedThisFrame = false;
                return;
            }
            if (tickNewGameTarinShield()) {
                completeGameplayFrame(dialogController);
                inputState.tickEdges();
                overworldDialogInputConsumedThisFrame = false;
                return;
            }
            OverworldDialogBlockers blockers = currentOverworldDialogBlockers();
            boolean dialogBlocksGameplay = blockers.pausesGameplay();

            if (!dialogBlocksGameplay) {
                inventoryController.tick();
                if (playerState != null) {
                    playerState.tickResourceBuffers(frameCounter);
                }
                if (transientVfxSystem != null) {
                    transientVfxSystem.tick();
                }
                if (droppableRupeeSystem != null && link != null) {
                    droppableRupeeSystem.tick(link.pixelX(), link.pixelY(), playerState);
                }
            }

            if (!dialogBlocksGameplay && inventoryMenu.isFullyOpen()) {
                inventoryController.dispatchMenuInput();
            }

            boolean linkActive = !scrollController.isActive()
                && !transitionController.isInputBlocked()
                && !inventoryController.shouldBlockOverworldInput()
                && !dialogBlocksGameplay
                && (roomSession == null || !roomSession.gotItemPresentationActive());
            boolean swordPickupSequenceActive = roomSession != null
                && roomSession.swordPickupSequenceActive();
            linkActive = linkActive && !swordPickupSequenceActive;
            if (linkActive && link != null) {
                // AnimateEntities runs after Link motion and its normal NPC
                // handlers restore hLinkFinalPosition when they push Link.
                // The ROM seeds that shadow from hLinkPosition before the
                // frame's motion work (bank0.asm:$0F97), so capture it before
                // any Link movement can change the position.
                link.captureRomFinalPosition();
                boolean keyholeSequenceActive = roomSession != null
                    && roomSession.tailCaveKeyholeSequenceActive();
                if (keyholeSequenceActive) {
                    link.blockNextRomMotionFrame();
                }
                boolean powerBraceletButtonHeld = isPowerBraceletButtonHeld();
                boolean bombButtonHeld = isBombButtonHeld();
                if (roomSession != null && !keyholeSequenceActive) {
                    roomSession.setEntityPowerBraceletButtonHeld(powerBraceletButtonHeld);
                    roomSession.setEntityBombButtonHeld(bombButtonHeld);
                    var liftedState = roomSession.liftedEntityState();
                    boolean bombBeingThrown = liftedState.type() == 0x02;
                    boolean liftedThrowButtonHeld = bombBeingThrown
                        ? bombButtonHeld : powerBraceletButtonHeld;
                    if (liftedThrowButtonHeld && liftedState.carryState() == 0x01) {
                        boolean thrown = roomSession.throwLiftedEntity(link.direction());
                        if (thrown && bombBeingThrown) {
                            link.startRomItemAttackStep();
                        }
                    }
                }
                if (!keyholeSequenceActive && !link.isCarryingLiftedObject()) {
                    if (!swordPickupSequenceActive) {
                        equipmentController.dispatchButtonEdges();
                    }
                    equipmentController.tickEquippedItems(frameCounter);
                }
                if (debugNoClip && tryDebugScreenScroll()) {
                    link.tickAnimation();
                } else {
                    link.tickRomLinkPushing();
                    link.update();
                }
                if (roomSession != null && playerState != null) {
                    roomSession.tryUnlockTailCaveKeyhole(
                        link.pixelX(), link.pixelY(), link.direction(),
                        link.romCollisionType(), playerState.tailKeyCount() != 0);
                    roomSession.tryUnlockIndoorKeyDoor(
                        link.pixelX(), link.pixelY(), link.direction(),
                        link.romCollisionType());
                    roomSession.tryInteractWithIndoorBlock(
                        link.pixelX(), link.pixelY(), link.direction(),
                        link.romCollisionType());
                }
                Link.ScreenShakeRequest pegasusShake =
                    link.consumePegasusScreenShakeRequest();
                if (pegasusShake != null) {
                    scrollController.startScreenShake(
                        pegasusShake.countdown(), pegasusShake.phase());
                }
                if (roomSession != null) {
                    roomSession.setSecretSeashellPegasusCollisionState(
                        scrollController.screenShakeCountdown() != 0,
                        link.pegasusBootsCollisionCountdown() != 0,
                        link.pegasusBootsCollisionPosX(),
                        link.pegasusBootsCollisionPosY());
                }
                if (roomSession != null && playerState != null) {
                    roomSession.setChestPlayerLevels(
                        playerState.shieldLevel(), playerState.swordLevel(),
                        playerState.powerBraceletLevel());
                    roomSession.setToadstoolPlayerState(
                        playerState.hasToadstool(), playerState.magicPowderCount());
                    EntityPickupEvent pickup = roomSession.collectEntityIfNeeded(
                        frameCounter, link.pixelX(), link.pixelY(), link.isAirborne(), true,
                        link.direction(), link.romEntityZ());
                    if (pickup != null) {
                        if (pickup.type() == 0x36) {
                            playerState.setActivePowerUp(PlayerState.ACTIVE_POWER_UP_NONE);
                            playDirectMusic(0x25);
                        } else if (pickup.sourceVariant() >= 0) {
                            playerState.applyFloatingItemPickup(
                                pickup.type(), pickup.sourceVariant());
                        } else {
                            playerState.applyEntityPickup(pickup.type());
                        }
                    }
                }
                maybeCutBushWithSword();
                // Warp-transition check runs BEFORE edge-scroll/clamp:
                // indoor rooms clamp Link at y=112 (== ROOM_PIXEL_HEIGHT −
                // SPRITE_SIZE) in the edge-scroll fallback, but the
                // indoor-exit trigger needs to see Link at y > 112 to fire.
                // If we clamp first, Link never crosses the threshold and
                // the south-edge warp never fires.
                roomTransitionCoordinator.handleWarpAndIndoorBoundaries(link);
                roomTransitionCoordinator.handleOverworldBoundary(link);
            } else if (scrollController.isActive() && link != null) {
                // Keep Link's walk animation cycling during a room transition.
                link.tickAnimation();
            }

            if (!dialogBlocksGameplay && roomSession != null) {
                int rumbleHorizontal = roomSession.tickTailCaveKeyholeSequence();
                scrollController.setScriptedScreenShakeHorizontal(
                    roomSession.tailCaveKeyholeSequenceActive(), rumbleHorizontal);
            }

            Sword.CollisionBox swordBoxForEntityTick = Sword.CollisionBox.inactive();
            if (link != null && roomSession != null
                && !scrollController.isActive()
                && !transitionController.isInputBlocked()
                && !inventoryController.shouldBlockOverworldInput()
                && !dialogBlocksGameplay) {
                Sword sword = equipmentController.activeSword();
                swordBoxForEntityTick = sword == null
                    ? Sword.CollisionBox.inactive()
                    : sword.enemyCollisionBox(link.romEntityX(), link.romSwordCollisionY(),
                        link.direction());
                EnemyAttackContext attackContext = new EnemyAttackContext(
                    playerState.swordLevel(),
                    playerState.tunicType() == PlayerState.TUNIC_RED,
                    playerState.activePowerUp() == PlayerState.ACTIVE_POWER_UP_PIECE_OF_POWER,
                    sword != null && sword.spinAttackActive(),
                    playerState.runningWithPegasusBoots());
                var combatEvents = roomSession.resolveEntityCombat(
                    frameCounter, link.romEntityX(), link.romEntityY(),
                    link.isAirborne(), link.zVelocity(), !debugNoClip,
                    swordBoxForEntityTick.active(), swordBoxForEntityTick.x(),
                    swordBoxForEntityTick.width(), swordBoxForEntityTick.y(),
                    swordBoxForEntityTick.height(), attackContext);
                EnemyCombatEventConsumer.consume(combatEvents, gameplaySoundSink, transientVfxSystem);
                for (EntityCombatEvent event : combatEvents) {
                    if (event.linkDamage() > 0 && playerState.invincibilityCounter() == 0) {
                        playerState.applyRomEnemyDamage(event.linkDamage());
                        if (event.linkCollisionResponse().active()) {
                            EntityCombatEvent.LinkCollisionResponse response =
                                event.linkCollisionResponse();
                            link.applyRomSpeed(response.speedX(), response.speedY());
                            link.setCollisionIgnoreFrames(
                                response.ignoreCollisionCountdown());
                            if (response.invincibilityCountdown() != 0) {
                                playerState.setInvincibilityCounter(
                                    response.invincibilityCountdown());
                            }
                            playerState.setRunningWithPegasusBoots(false);
                            // ApplyLinkCollisionWithEnemy writes WAVE_SFX_LINK_HURT
                            // before the shared Link response helper.
                            gameplaySoundSink.play(GameplaySoundEvent.LINK_HURT);
                        }
                    }
                    if (event.linkAction()
                        == EntityCombatEvent.LinkAction.GOOMBA_BOUNCE_TOP_DOWN) {
                        link.bounceFromGoomba();
                    } else if (event.linkAction()
                        == EntityCombatEvent.LinkAction.GOOMBA_BOUNCE_SIDE_SCROLLING) {
                        link.applyRomSpeed(0, 0xF0);
                    }
                }
            }

            // The original calls AnimateEntities after Link movement and
            // room-transition application. Give handlers the same current
            // position and active-room snapshot.
            if (roomSession != null
                && shouldTickRoomEntities(
                    scrollController.isActive(),
                    transitionController.isInputBlocked(),
                    inventoryController.shouldBlockOverworldInput(),
                    inventoryMenu != null && inventoryMenu.isTransitioning())) {
                roomSession.setEntityDialogActive(
                    dialogController != null && dialogController.isActive());
                roomSession.setEntityTalkState(
                    inventoryMenu != null && inventoryMenu.isTransitioning(),
                    dialogController == null ? 0 : dialogController.dialogCooldown(),
                    inventoryMenu == null ? 0x80 : inventoryMenu.windowY());
                roomSession.setEntityMusicActive(
                    musicPlayer != null && musicPlayer.isMusicPlaying());
                roomSession.setEntityActionButtonsHeld(
                    inputState != null && inputConfig != null
                        && inputState.isDown(inputConfig.aKey()),
                    inputState != null && inputConfig != null
                        && inputState.isDown(inputConfig.bKey()));
                roomSession.setEntityJoypadHeld(
                    inputState != null && inputConfig != null
                        && (inputState.isDown(inputConfig.upKey())
                            || inputState.isDown(inputConfig.downKey())
                            || inputState.isDown(inputConfig.leftKey())
                            || inputState.isDown(inputConfig.rightKey())
                            || inputState.isDown(inputConfig.aKey())
                            || inputState.isDown(inputConfig.bKey())
                            || inputState.isDown(inputConfig.selectKey())
                            || inputState.isDown(inputConfig.menuOpenKey())));
                roomSession.setEntityPressedButtonsMask(
                    link == null ? 0 : link.romPressedButtonsMask());
                roomSession.setEntityPowerBraceletButtonHeld(isPowerBraceletButtonHeld());
                roomSession.setEntityBombButtonHeld(isBombButtonHeld());
                roomSession.setEntityAttackStepAnimationCountdown(
                    link == null ? 0 : link.romAttackStepAnimationCountdown());
                roomSession.setBirdKeyOwned(playerState != null && playerState.birdKeyCount() != 0);
                roomSession.setTailKeyOwned(playerState != null && playerState.tailKeyCount() != 0);
                roomSession.setEntityInventorySlots(
                    playerState == null ? PlayerState.INVENTORY_EMPTY : playerState.itemA(),
                    playerState == null ? PlayerState.INVENTORY_EMPTY : playerState.itemB());
                roomSession.setChestPlayerLevels(
                    playerState == null ? 1 : playerState.shieldLevel(),
                    playerState == null ? 1 : playerState.swordLevel(),
                    playerState == null ? 1 : playerState.powerBraceletLevel());
                roomSession.setToadstoolPlayerState(
                    playerState != null && playerState.hasToadstool(),
                    playerState == null ? 0 : playerState.magicPowderCount());
                roomSession.setEntityGoldenLeavesCount(
                    playerState == null ? 0 : playerState.goldenLeavesCount());
                roomSession.setEnemyDropPlayerState(
                    playerState.maxHearts(), playerState.health(),
                    playerState.activePowerUp() != PlayerState.ACTIVE_POWER_UP_NONE);
                var projectileEvents = roomSession.tickEntitiesWithProjectileEvents(
                    frameCounter,
                    link == null ? 0x08 : link.romEntityX(),
                    link == null ? 0x10 : link.romEntityY(),
                    link == null ? 0x00 : link.romEntityZ(),
                    link != null && link.isAirborne(),
                    link == null ? 0x02 : link.romMotionState(),
                    link == null ? 0x00 : link.direction(),
                    link == null ? 0x00 : link.romCollisionType(),
                    link != null && link.isUsingShield(),
                    playerState == null ? 1 : playerState.shieldLevel(),
                    playerState == null ? 0 : playerState.invincibilityCounter(),
                    swordBoxForEntityTick.active(), swordBoxForEntityTick.x(),
                    swordBoxForEntityTick.width(), swordBoxForEntityTick.y(),
                    swordBoxForEntityTick.height(),
                    link == null ? 0 : link.romSpeedX(),
                    link == null ? 0 : link.romSpeedY());
                for (var request : roomSession.consumeLinkFinalPositionRequests()) {
                    if (link != null) {
                        link.restoreRomFinalPosition();
                        if (request.clearLinkPositionIncrement()) {
                            link.clearRomPositionIncrement();
                        }
                        if (request.markLinkPushing()) {
                            link.markRomLinkPushing(0x03);
                        }
                    }
                    if (request.resetPegasusBoots()) {
                        if (link != null) {
                            link.resetPegasusBoots();
                        } else if (playerState != null) {
                            playerState.setRunningWithPegasusBoots(false);
                        }
                    }
                }
                for (var request : roomSession.consumeLinkMotionBlockRequests()) {
                    if (link != null) {
                        link.blockNextRomMotionFrame();
                    }
                }
                if (roomSession.consumeWorldLinkMotionBlockRequest() && link != null) {
                    link.blockNextRomMotionFrame();
                }
                for (var request : roomSession.consumeLinkFallenPoseRequests()) {
                    if (link != null) {
                        link.showRomFallenPose();
                    }
                }
                for (var request : roomSession.consumeLinkFacingRequests()) {
                    if (link != null) {
                        if (request.preserveWalkingPhase()) {
                            link.faceRomDirectionPreservingWalkPhase(request.romDirection());
                        } else {
                            link.setRomDirection(request.romDirection());
                        }
                    }
                }
                for (var request : roomSession.consumeLinkAttackClearRequests()) {
                    if (link != null) {
                        link.clearRomAttackStepAnimationCountdown();
                    }
                }
                for (var request : roomSession.consumeLinkHeldItemPoseRequests()) {
                    if (link != null) {
                        link.showHeldItemPose();
                    }
                    Sword heldPoseSword = equipmentController.activeSword();
                    if (heldPoseSword != null) {
                        heldPoseSword.resetSpinAttack();
                    }
                }
                for (var request : roomSession.consumeLinkSwordSpinPoseRequests()) {
                    int acquisitionBaseDirection = link == null ? Link.DIRECTION_DOWN
                        : link.direction();
                    if (link != null) {
                        link.showSwordAcquisitionSpinPose(request.countdown());
                    }
                    Sword acquisitionSword = itemRegistry == null ? null
                        : itemRegistry.lookup(PlayerState.INVENTORY_SWORD) instanceof Sword sword
                            ? sword : null;
                    if (acquisitionSword != null) {
                        if (request.countdown() == 0x20) {
                            acquisitionSword.startSwordAcquisitionSpin(acquisitionBaseDirection);
                        } else if (acquisitionSword.spinAttackActive()) {
                            acquisitionSword.tick(false, frameCounter);
                        }
                    }
                }
                for (var request : roomSession.consumeLinkSwordFinalPoseRequests()) {
                    if (link != null) {
                        link.showSwordAcquisitionFinalPose();
                    }
                    if (itemRegistry != null
                        && itemRegistry.lookup(PlayerState.INVENTORY_SWORD) instanceof Sword sword) {
                        sword.resetSpinAttack();
                    }
                }
                for (var request : roomSession.consumeScreenShakeRequests()) {
                    scrollController.startScreenShake(request.countdown(), request.phase());
                }
                EnemyProjectileEventConsumer.consume(projectileEvents, playerState, link,
                    gameplaySoundSink);
                EnemyCombatEventConsumer.consume(roomSession.consumeEntityEvents(),
                    gameplaySoundSink, transientVfxSystem);
                for (var reward : roomSession.consumeChestRewardEvents()) {
                    if (playerState != null) {
                        playerState.applyChestReward(reward.itemType());
                    }
                }
                for (var reward : roomSession.consumeKeyRewardEvents()) {
                    if (playerState != null) {
                        playerState.applyChestReward(reward.itemType());
                    }
                }
                for (var reward : roomSession.consumeSlimeKeyRewardEvents()) {
                    if (playerState != null) {
                        playerState.setGoldenLeavesCount(reward.goldenLeavesCount());
                    }
                }
                for (var reward : roomSession.consumeHeartContainerRewards()) {
                    if (playerState != null) {
                        playerState.applyHeartContainerReward();
                    }
                    playDirectMusic(0x18);
                }
                BeachSwordRewardConsumer.consume(roomSession, playerState);
                for (var reward : roomSession.consumeToadstoolRewards()) {
                    if (playerState != null) {
                        playerState.applyToadstoolReward();
                    }
                }
                for (var event : roomSession.consumeWitchExchangeEvents()) {
                    if (playerState != null) {
                        playerState.beginWitchToadstoolExchange(event.inventorySlot());
                    }
                }
                for (var reward : roomSession.consumeWitchRewardEvents()) {
                    if (playerState != null) {
                        playerState.applyWitchMagicPowderReward();
                    }
                }
                roomSession.setBirdKeyOwned(playerState != null && playerState.birdKeyCount() != 0);
                roomSession.setTailKeyOwned(playerState != null && playerState.tailKeyCount() != 0);
                int chestMusicTrack = roomSession.consumePendingMusicTrack();
                if (chestMusicTrack >= 0) {
                    playDirectMusic(chestMusicTrack);
                }
                for (var event : roomSession.consumeLikeLikeEvents()) {
                    if (event.kind()
                        == linksawakening.world.RoomEntityRuntime.LikeLikeEvent.Kind.CAPTURE) {
                        if (event.stolenInventorySlot() == 0) {
                            playerState.setItemB(PlayerState.INVENTORY_EMPTY);
                        } else if (event.stolenInventorySlot() == 1) {
                            playerState.setItemA(PlayerState.INVENTORY_EMPTY);
                        }
                        if (link != null) {
                            link.applyLikeLikeCapture(event.entityX(), event.entityY());
                        }
                    } else if (link != null) {
                        link.releaseLikeLikeCapture();
                    }
                }
                openEntityDialogRequests();
                if (roomTransitionCoordinator.handlePendingManboTransition(link)) {
                    gameplaySoundSink.play(linksawakening.gameplay.GameplaySoundEvent.MANBO_WARP);
                }
                roomTransitionCoordinator.handlePendingInstrumentTransition(link);
                for (var event : projectileEvents) {
                    // Accepted LINK_DAMAGE responses are applied by the
                    // projectile consumer before PlayerState becomes
                    // invincible. Keep this pass for shield reflection,
                    // sword-poke, and hookshot responses only.
                    if (event.kind() == EntityProjectileEvent.Kind.LINK_DAMAGE) {
                        boolean hasLinkResponse = event.linkIgnoreCollisionCountdown() != 0
                            || event.linkSpeedX() != 0 || event.linkSpeedY() != 0;
                        if (hasLinkResponse) {
                            Sword interruptedSword = equipmentController.activeSword();
                            if (interruptedSword != null) {
                                interruptedSword.resetSpinAttack();
                            }
                        }
                        continue;
                    }
                    boolean hookshotPull = event.kind() == EntityProjectileEvent.Kind.HOOKSHOT_PULL;
                    boolean hasLinkResponse = event.linkIgnoreCollisionCountdown() != 0
                        || event.linkSpeedX() != 0 || event.linkSpeedY() != 0;
                    if ((!hookshotPull && !hasLinkResponse)
                        || link == null) {
                        continue;
                    }
                    if (hookshotPull) {
                        link.applyRomFinalPosition(event.linkSpeedX(), event.linkSpeedY());
                        continue;
                    }
                    link.applyRomSpeed(event.linkSpeedX(), event.linkSpeedY());
                    link.setCollisionIgnoreFrames(event.linkIgnoreCollisionCountdown());
                    Sword reflectedSword = equipmentController.activeSword();
                    if (reflectedSword != null) {
                        reflectedSword.resetSpinAttack();
                    }
                }
                for (var request : roomSession.consumeRoosterLinkStateRequests()) {
                    if (link != null) {
                        link.applyRoosterFlightState(request.positionZ(), request.velocityZ(),
                            request.speedX(), request.speedY(), request.romDirection());
                    }
                }
                for (var request : roomSession.consumeHinoxLinkEffectRequests()) {
                    if (link != null) {
                        link.applyHinoxGrabState(request.heldX(), request.heldY(), request.heldZ(),
                            request.speedX(), request.speedY(), request.velocityZ(),
                            request.airborneState(), request.motionBlocked(),
                            request.applyHeldLinkPose());
                    }
                    if (request.damage() != 0 && playerState != null) {
                        playerState.applyRomEnemyDamage(request.damage());
                    }
                }
                synchronizeLinkLiftedPresentation();
            }
            completeGameplayFrame(dialogController);

            // Advance the animated BG tiles (waterfalls, weather vanes, etc.).
            // Original gates this behind wRoomTransitionState == 0 and the
            // inventory window not overlapping; mirror both here.
            if (!scrollController.isActive()
                && !transitionController.isInputBlocked()
                && !inventoryController.shouldBlockOverworldInput()
                && !dialogBlocksGameplay) {
                if (roomSession == null) {
                    gpu.tickAnimatedTiles(romData);
                } else {
                    roomSession.tickGameplayVBlank();
                }
            }
        }

        // Snapshot input for next-frame edge detection. Run last so every
        // consumer this frame (item dispatch, menu toggle) still sees the
        // correct "just pressed" state.
        inputState.tickEdges();
        overworldDialogInputConsumedThisFrame = false;
    }

    private static void routeDialogSounds() {
        DialogSoundRouter.routePending(dialogController, dialogSoundSink);
    }

    private static boolean isPowerBraceletButtonHeld() {
        if (inputState == null || inputConfig == null || playerState == null) {
            return false;
        }
        boolean braceletOnA = playerState.itemA() == PlayerState.INVENTORY_POWER_BRACELET
            && inputState.isDown(inputConfig.aKey());
        boolean braceletOnB = playerState.itemB() == PlayerState.INVENTORY_POWER_BRACELET
            && inputState.isDown(inputConfig.bKey());
        return braceletOnA || braceletOnB;
    }

    /** Mirrors BombEntityHandler's B-slot-first equipped-bomb input check. */
    private static boolean isBombButtonHeld() {
        if (inputState == null || inputConfig == null || playerState == null) {
            return false;
        }
        if (playerState.itemB() == PlayerState.INVENTORY_BOMBS) {
            return inputState.isDown(inputConfig.bKey());
        }
        return playerState.itemA() == PlayerState.INVENTORY_BOMBS
            && inputState.isDown(inputConfig.aKey());
    }

    private static void synchronizeLinkLiftedPresentation() {
        if (link == null || roomSession == null) {
            return;
        }
        var state = roomSession.liftedEntityState();
        if (state.active()) {
            link.setCarryingLiftedObjectState(state.carryState(),
                state.effectiveRomDirection());
        } else {
            if (link.isRoosterCarryActive()) {
                link.clearRoosterCarryState();
            }
            link.setCarryingLiftedObjectState(0, romDirectionForLink(link.direction()));
        }
    }

    private static int romDirectionForLink(int javaDirection) {
        return switch (javaDirection) {
            case Link.DIRECTION_RIGHT -> 0;
            case Link.DIRECTION_LEFT -> 1;
            case Link.DIRECTION_UP -> 2;
            case Link.DIRECTION_DOWN -> 3;
            default -> throw new IllegalArgumentException("Link direction out of range: "
                + javaDirection);
        };
    }

    private static void selectMusicForLoadedRoom(ActiveRoom room) {
        if (gameplayMusicController == null || playerState == null || room == null) {
            return;
        }
        gameplayMusicController.selectAfterTransition(RoomMusicContext.from(room, playerState));
        if (roomSession != null) {
            roomSession.setEntityDefaultMusicTrack(gameplayMusicController.currentTrackId());
        }
    }

    private static void maybeCutBushWithSword() {
        if (roomSession == null
            || !roomSession.hasActiveRoom()
            || roomSession.mapCategory() != Warp.CATEGORY_OVERWORLD
            || overworldBushInteraction == null
            || link == null) {
            return;
        }
        ActiveRoom room = roomSession.activeRoom();

        Sword sword = equipmentController.activeSword();
        if (sword == null || !sword.staticCollisionActive()) {
            return;
        }

        int location = overworldBushInteraction.swordHitRoomObjectLocationForCollisionIndex(
            true, sword.staticCollisionMapIndex(link.direction()), link.pixelX(), link.pixelY());
        if (location < 0) {
            return;
        }

        int areaIndex = ROOM_OBJECTS_BASE + location;
        if (areaIndex < 0 || areaIndex >= room.roomObjectsArea().length) {
            return;
        }
        int originalObjectId = room.roomObjectsArea()[areaIndex];

        OverworldBushInteraction.CutResult cutResult = overworldBushInteraction.cutObjectAtLocation(
            location,
            room.roomId(),
            true,
            room.roomObjectsArea(),
            room.renderValues(),
            room.gbcOverlay(),
            room.tileIds(),
            room.tileAttrs()
        );
        if (!cutResult.changed()) {
            return;
        }

        gameplaySoundSink.play(cutResult.soundEvent());

        if (transientVfxSystem != null && cutResult.bushLeavesVisible()) {
            transientVfxSystem.spawn(
                TransientVfxType.BUSH_LEAVES,
                overworldBushInteraction.effectOriginXForLocation(location),
                overworldBushInteraction.effectOriginYForLocation(location)
            );
        }

        if (droppableRupeeSystem != null) {
            droppableRupeeSystem.maybeSpawnFromBush(
                originalObjectId,
                overworldBushInteraction.effectOriginXForLocation(location),
                overworldBushInteraction.effectOriginYForLocation(location)
            );
        }

        if (areaIndex >= 0 && areaIndex < room.roomObjectsArea().length
            && room.roomObjectsArea()[areaIndex] == OBJECT_GROUND_STAIRS) {
            room.replaceFirstWarpTile(location);
        }
    }

    private static boolean tryOpenOverworldDialog(int key) {
        if (!GameplayDialogInput.isActionButtonKey(key, inputConfig)
            || overworldDialogInteraction == null
            || roomSession == null
            || !roomSession.hasActiveRoom()
            || link == null) {
            return false;
        }
        ActiveRoom room = roomSession.activeRoom();
        return overworldDialogInteraction.tryOpenSignpostDialog(
            room.roomId(),
            room.mapCategory(),
            room.roomObjectsArea(),
            link.pixelX(),
            link.pixelY(),
            link.direction(),
            true,
            currentOverworldDialogBlockers(),
            dialogController);
    }

    private static boolean tryOpenChest(int key) {
        if (!GameplayDialogInput.isActionButtonKey(key, inputConfig)
            || roomSession == null
            || !roomSession.hasActiveRoom()
            || link == null
            || playerState == null
            || currentOverworldDialogBlockers().blocksOpening()) {
            return false;
        }
        RoomSession.ChestOpenResult result = roomSession.tryOpenChest(
            link.pixelX(), link.pixelY(), link.direction(), true, playerState.swordLevel());
        return result.opened();
    }

    private static void openEntityDialogRequests() {
        if (roomSession == null || dialogController == null || dialogTextLoader == null
            || link == null || dialogController.isActive()) {
            return;
        }
        for (var request : roomSession.consumeEntityDialogRequests()) {
            SignpostDialogRef dialogRef = new SignpostDialogRef(
                request.tableId(), request.dialogLowId());
            dialogController.openPreformattedForLinkY(
                dialogTextLoader.load(dialogRef),
                LinkDialogPosition.dialogYFromTopLeft(link.pixelY()));
            return;
        }
    }

    private static boolean tickNewGameWakeUp() {
        if (newGameWakeUpMotion == null || newGameWakeUpMotion.complete()) {
            return false;
        }
        boolean directionPressed = inputState.isDown(inputConfig.upKey())
            || inputState.isDown(inputConfig.downKey())
            || inputState.isDown(inputConfig.leftKey())
            || inputState.isDown(inputConfig.rightKey());
        MarinWakeUpMotion.Update update = newGameWakeUpMotion.tick(frameCounter,
            dialogController != null && dialogController.isActive(), directionPressed);
        if (update.linkMotionBlocked()) {
            link.setPixelPosition(update.linkRomX() - 0x08, update.linkRomY() - 0x10);
            link.blockNextRomMotionFrame();
            link.showMarinWakeUpBed(update.bedSpriteVariant());
        }
        if (update.openWakeDialog()) {
            dialogController.openPreformattedForLinkY(
                dialogTextLoader.load(new SignpostDialogRef(0, update.dialogLowId())),
                LinkDialogPosition.dialogYFromTopLeft(link.pixelY()));
        }
        if (update.leaveBed()) {
            link.leaveMarinWakeUpBed();
        }
        return true;
    }

    private static boolean marinWakeUpBlocksKeyInput() {
        return newGameWakeUpMotion != null && !newGameWakeUpMotion.complete();
    }

    private static void updateNewGameMarinPresentation() {
        if (newGameWakeUpMotion == null || roomSession == null
            || !roomSession.hasActiveRoom() || link == null
            || roomSession.activeRoom().mapId() != 0x10
            || roomSession.activeRoom().roomId() != 0xA3) {
            return;
        }
        for (var entity : roomSession.activeRoom().entities().loadedEntities()) {
            if (entity.type() != 0x3E) {
                continue;
            }
            MarinWakeUpMotion.MarinPresentation presentation =
                newGameWakeUpMotion.tickMarinPresentation(frameCounter,
                    entity.x(), entity.y(), link.romEntityX(), link.romEntityY());
            roomSession.applyMarinWakeUpPresentation(presentation.romX(), presentation.romY(),
                presentation.spriteVariant());
            return;
        }
    }

    private static boolean tryOpenTarinShieldDialog(int key) {
        if ((newGameWakeUpMotion != null && !newGameWakeUpMotion.complete())
            || newGameTarinShieldMotion == null
            || key != inputConfig.aKey()
            || roomSession == null || !roomSession.hasActiveRoom() || link == null
            || dialogController == null || dialogTextLoader == null
            || currentOverworldDialogBlockers().blocksOpening()) {
            return false;
        }
        ActiveRoom room = roomSession.activeRoom();
        if (room.mapId() != 0x10 || room.roomId() != 0xA3) {
            return false;
        }
        for (var entity : room.entities().loadedEntities()) {
            if (entity.type() == 0x3F && TarinShieldMotion.canTalkToEntity(
                entity.x(), entity.y(), link.romEntityX(), link.romEntityY(),
                romDirectionForLink(link.direction()), link.isAirborne(), true,
                dialogController.isActive())) {
                TarinShieldMotion.Update update = newGameTarinShieldMotion.tick(
                    false, link.romEntityY(), true, playerState.shieldLevel());
                if (update.dialogLowId() >= 0) {
                    dialogController.openPreformattedForLinkY(dialogTextLoader.load(
                        new SignpostDialogRef(0, update.dialogLowId())),
                        LinkDialogPosition.dialogYFromTopLeft(link.pixelY()));
                }
                return true;
            }
        }
        return false;
    }

    private static boolean tryOpenMarinFollowUpDialog(int key) {
        if (newGameWakeUpMotion == null || !newGameWakeUpMotion.complete()
            || key != inputConfig.aKey()
            || roomSession == null || !roomSession.hasActiveRoom() || link == null
            || dialogController == null || dialogTextLoader == null
            || currentOverworldDialogBlockers().blocksOpening()) {
            return false;
        }
        ActiveRoom room = roomSession.activeRoom();
        if (room.mapId() != 0x10 || room.roomId() != 0xA3) {
            return false;
        }
        for (var entity : room.entities().loadedEntities()) {
            if (entity.type() == 0x3E && newGameWakeUpMotion.canOpenFollowUpDialog(
                entity.x(), entity.y(), link.romEntityX(), link.romEntityY(),
                romDirectionForLink(link.direction()), link.isAirborne(), true,
                dialogController.isActive())) {
                dialogController.openPreformattedForLinkY(dialogTextLoader.load(
                    new SignpostDialogRef(0, newGameWakeUpMotion.followUpDialogLowId())),
                    LinkDialogPosition.dialogYFromTopLeft(link.pixelY()));
                return true;
            }
        }
        return false;
    }

    private static boolean tickNewGameTarinShield() {
        if (newGameTarinShieldMotion == null || roomSession == null
            || !roomSession.hasActiveRoom() || link == null || playerState == null
            || roomSession.activeRoom().mapId() != 0x10
            || roomSession.activeRoom().roomId() != 0xA3) {
            return false;
        }
        TarinShieldMotion.Update update = newGameTarinShieldMotion.tick(
            dialogController != null && dialogController.isActive(),
            link.romEntityY(), false, playerState.shieldLevel());
        if (update.presentShield()) {
            link.showTarinShieldPresentation(newGameTarinShieldPalette);
        } else {
            link.clearTarinShieldPresentation();
        }
        if (update.linkY() != link.romEntityY()) {
            link.setPixelPosition(link.pixelX(), update.linkY() - 0x10);
            link.blockNextRomMotionFrame();
        }
        if (update.grantShield()) {
            playerState.applyChestReward(linksawakening.world.ChestContentsTable.CHEST_SHIELD);
            link.showStandingShieldDownPose();
            // MUSIC_OBTAIN_ITEM is a jingle in the ROM.  The host music
            // player loops direct tracks, so resume the house track when
            // TarinShield2Handler awards the shield and opens Dialog091.
            playDirectMusic(newGameHouseMusicTrack());
        }
        if (update.playObtainItemMusic()) {
            playDirectMusic(MusicTrackIds.MUSIC_OBTAIN_ITEM);
        }
        if (update.dialogLowId() >= 0 && (dialogController == null
            || !dialogController.isActive())) {
            dialogController.openPreformattedForLinkY(dialogTextLoader.load(
                new SignpostDialogRef(0, update.dialogLowId())),
                LinkDialogPosition.dialogYFromTopLeft(link.pixelY()));
        }
        return update.linkMotionBlocked();
    }

    private static OverworldDialogBlockers currentOverworldDialogBlockers() {
        return new OverworldDialogBlockers(
            inventoryController != null && inventoryController.shouldBlockOverworldInput(),
            scrollController.isActive(),
            transitionController.isInputBlocked(),
            GameplayDialogInput.blocksGameplay(dialogController),
            overworldDialogInputConsumedThisFrame);
    }

    private static AppConfig currentAppConfig() {
        return appConfig != null ? appConfig : AppConfig.defaults();
    }

    /** Applies the configured player profile before a direct gameplay launch. */
    static void applyConfiguredItemProfile(AppConfig config, PlayerState playerState) {
        if (config.itemProfile() == AppConfig.ItemProfile.NEW_GAME) {
            NewGameStartProfile.romDefaults().initializePlayerState(playerState);
        }
    }

    private static void startConfiguredGameplay() {
        dispatchConfiguredGameplay(
            currentAppConfig(), Main::startNewGame, Main::startConfiguredLocationGameplay);
    }

    private static void startDebugGameplay() {
        if (playerState == null || roomSession == null || link == null) {
            return;
        }
        if (currentScreen == SCREEN_CUTSCENE && cutsceneManager != null) {
            cutsceneManager.skipIntroToTitle();
        }
        fileMenuController = null;
        fileSaveController = null;
        newGameWakeUpMotion = null;
        newGameTarinShieldMotion = null;
        currentSaveSlot = -1;
        link.resetTransientStateForDebug();
        playerState.initializeDebugState();
        roomSession.initializeNewGameWorldState();
        roomSession.setBowWowState(0);
        roomSession.setBirdKeyOwned(true);
        roomSession.setTailKeyOwned(true);
        currentScreen = SCREEN_OVERWORLD;
        debugNoClip = false;
        link.setDirection(Link.DIRECTION_DOWN);
        gpu.loadBaseTiles(romData);
        loadOverworldScreen();
        System.out.println("Debug gameplay started (F3 toggles no-clip)");
    }

    private static boolean tryDebugScreenScroll() {
        if (!inputState.isDown(GLFW_KEY_LEFT_SHIFT) && !inputState.isDown(GLFW_KEY_RIGHT_SHIFT)) {
            return false;
        }
        int direction = inputState.isDown(inputConfig.leftKey()) ? ScrollController.LEFT
            : inputState.isDown(inputConfig.rightKey()) ? ScrollController.RIGHT
            : inputState.isDown(inputConfig.upKey()) ? ScrollController.UP
            : inputState.isDown(inputConfig.downKey()) ? ScrollController.DOWN
            : ScrollController.NONE;
        if (direction == ScrollController.NONE || scrollController.isActive()
            || transitionController.isInputBlocked() || roomSession == null
            || !roomSession.hasActiveRoom()) {
            return false;
        }
        switch (direction) {
            case ScrollController.LEFT -> link.setPixelPosition(-1, link.pixelY());
            case ScrollController.RIGHT -> link.setPixelPosition(ROOM_PIXEL_WIDTH, link.pixelY());
            case ScrollController.UP -> link.setPixelPosition(link.pixelX(), -1);
            case ScrollController.DOWN -> link.setPixelPosition(link.pixelX(), ROOM_PIXEL_HEIGHT);
            default -> { return false; }
        }
        if (roomSession.activeRoom().mapCategory() == Warp.CATEGORY_OVERWORLD) {
            roomTransitionCoordinator.handleOverworldBoundary(link);
        } else {
            roomTransitionCoordinator.handleWarpAndIndoorBoundaries(link);
        }
        return scrollController.isActive();
    }

    static void dispatchConfiguredGameplay(AppConfig config, Runnable startNewGame,
                                           Runnable startConfiguredLocation) {
        if (StartupCoordinator.shouldStartNewGameGameplay(config)) {
            startNewGame.run();
        } else {
            startConfiguredLocation.run();
        }
    }

    private static void startConfiguredLocationGameplay() {
        fileMenuController = null;
        fileSaveController = null;
        currentSaveSlot = -1;
        newGameWakeUpMotion = null;
        newGameTarinShieldMotion = null;
        currentScreen = SCREEN_OVERWORLD;
        applyConfiguredItemProfile(currentAppConfig(), playerState);
        if (currentAppConfig().playIntroStory()) {
            System.err.println("Intro story startup is not implemented yet; spawning at start location");
        }
        loadOverworldScreen();
    }

    private static void startNewGame() {
        fileMenuController = null;
        fileSaveController = null;
        currentScreen = SCREEN_OVERWORLD;

        NewGameStartProfile profile = NewGameStartProfile.romDefaults();
        profile.initializePlayerState(playerState);
        roomSession.initializeNewGameWorldState();
        roomSession.setBirdKeyOwned(playerState.birdKeyCount() != 0);
        roomSession.setTailKeyOwned(playerState.tailKeyCount() != 0);
        roomSession.setBowWowState(0);
        link.setDirection(Link.DIRECTION_DOWN);
        gpu.loadBaseTiles(romData);
        roomSession.loadIndoor(profile.mapId(), profile.roomId());
        link.setRoomEntryRomPosition(profile.entryX(), profile.entryY());
        newGameWakeUpMotion = new MarinWakeUpMotion();
        newGameTarinShieldMotion = new TarinShieldMotion();
        playDirectMusic(newGameHouseMusicTrack());
        updateNewGameMarinPresentation();
    }

    private static void startSavedGame(SaveSlotState saved) {
        fileMenuController = null;
        fileSaveController = null;
        currentScreen = SCREEN_OVERWORLD;
        newGameWakeUpMotion = null;
        newGameTarinShieldMotion = null;
        playerState.applySavedGame(saved);
        roomSession.setBirdKeyOwned(playerState.birdKeyCount() != 0);
        roomSession.setTailKeyOwned(playerState.tailKeyCount() != 0);
        roomSession.restoreRoomStatuses(saved.overworldRoomStatus(), saved.indoorARoomStatus(),
            saved.indoorBRoomStatus(), saved.colorDungeonRoomStatus());
        roomSession.restoreDungeonItemFlags(saved.dungeonItemFlags(),
            saved.colorDungeonItemFlags());
        roomSession.restoreDungeonProgressFlags(saved.dungeonProgressFlags());
        roomSession.setBowWowState(saved.bowWowState());
        roomSession.setTarinFlag(saved.tarinFlag());
        roomSession.setChestPlayerLevels(playerState.shieldLevel(), playerState.swordLevel(),
            playerState.powerBraceletLevel());
        if (saved.spawnIsIndoor() != 0) {
            gpu.loadBaseTiles(romData);
            roomSession.loadIndoorFromSavedPosition(
                saved.spawnMapId(), saved.spawnMapRoom(), saved.spawnIndoorRoom());
            link.setDirection(Link.DIRECTION_UP);
        } else {
            roomSession.loadInitialOverworld(saved.spawnMapRoom());
            link.setDirection(Link.DIRECTION_DOWN);
        }
        link.setRoomEntryRomPosition(saved.spawnPositionX(), saved.spawnPositionY());
        restoreNewGameHouseRuntimeIfNeeded(saved);
    }

    private static void restoreNewGameHouseRuntimeIfNeeded(SaveSlotState saved) {
        if (saved.spawnIsIndoor() != 0 && saved.spawnMapId() == 0x10
            && saved.spawnMapRoom() == 0xA3) {
            if (playerState.swordLevel() == 0) {
                newGameWakeUpMotion = MarinWakeUpMotion.postWake();
                updateNewGameMarinPresentation();
            }
            if (playerState.shieldLevel() == 0) {
                newGameTarinShieldMotion = new TarinShieldMotion();
            }
        }
    }

    private static void startIntroCutscene() {
        fileMenuController = null;
        fileSaveController = null;
        currentScreen = SCREEN_CUTSCENE;
        playDirectMusic(introCutsceneMusicTrack());
        gpu.loadIntroSequenceTiles(romData);
        cutsceneManager.startIntro(romData, Main::loadIntroBackgroundScene);
    }

    private static BackgroundScene loadIntroBackgroundScene(String sceneId) {
        BackgroundSceneSpec spec = BackgroundSceneCatalog.forCutsceneScene(sceneId);
        if (spec == null) {
            throw new IllegalArgumentException("No intro background scene for " + sceneId);
        }
        return backgroundSceneLoader.load(spec);
    }

    private static void startTitleScreenWithoutIntro() {
        fileMenuController = null;
        fileSaveController = null;
        currentScreen = SCREEN_TITLE;
        playDirectMusic(directTitleScreenMusicTrack());
    }

    private static void startFileSelection() {
        currentScreen = SCREEN_FILE_MENU;
        fileSaveController = null;
        gpu.loadMenuTiles(romData);
        fileMenuController = new FileMenuController(
            new FileMenuRomData(romData),
            Main::loadFileMenuBackgroundScene,
            saveRamStore.saveFilesMask(),
            saveRamStore.savedNames());
        playDirectMusic(fileSelectionMusicTrack());
        // The Enter event that leaves the title must not also activate the
        // first empty file. Its edge belongs to the title screen.
        inputState.tickEdges();
    }

    private static void persistFileMenuMutation(Runnable mutation) {
        mutation.run();
        try {
            saveRamStore.flush();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to persist file-menu mutation", exception);
        }
        startFileSelection();
    }

    private static void startFileSave() {
        currentScreen = SCREEN_FILE_SAVE;
        fileMenuController = null;
        gpu.loadSaveMenuTiles(romData);
        fileSaveController = new FileSaveController(Main::loadFileMenuBackgroundScene);
        inputState.tickEdges();
    }

    private static void saveCurrentPlayerState() {
        if (currentSaveSlot < 0 || playerState == null || saveRamStore == null) {
            return;
        }
        saveRamStore.writePlayerState(currentSaveSlot, playerState);
        saveCurrentSpawnLocation();
        if (roomSession != null) {
            saveRamStore.writeRoomStatuses(currentSaveSlot,
                roomSession.overworldRoomStatusSnapshot(),
                roomSession.indoorARoomStatusSnapshot(),
                roomSession.indoorBRoomStatusSnapshot(),
                roomSession.colorDungeonRoomStatusSnapshot());
            saveRamStore.writeDungeonItemFlags(currentSaveSlot,
                roomSession.dungeonItemFlagsSnapshot(),
                roomSession.colorDungeonItemFlagsSnapshot());
            saveRamStore.writeDungeonProgressFlags(currentSaveSlot,
                roomSession.dungeonProgressFlagsSnapshot());
            saveRamStore.writeBowWowState(currentSaveSlot, roomSession.bowWowState());
            saveRamStore.writeTarinFlag(currentSaveSlot, roomSession.tarinFlag());
        }
        try {
            saveRamStore.flush();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to persist player save state", exception);
        }
    }

    /** Mirrors the source's wSpawnLocationData fields copied by SaveGameToFile. */
    private static void saveCurrentSpawnLocation() {
        if (roomSession == null || !roomSession.hasActiveRoom() || link == null) {
            return;
        }

        ActiveRoom room = roomSession.activeRoom();
        boolean isIndoor = room.mapCategory() != Warp.CATEGORY_OVERWORLD;
        int indoorRoom = roomSession.indoorRoomPositionForSave();
        if (indoorRoom < 0) {
            // Small houses/caves have no map-layout table. The source leaves
            // wIndoorRoom at its previous value in that case.
            indoorRoom = saveRamStore.readSlot(currentSaveSlot).spawnIndoorRoom();
        }
        saveRamStore.writeSpawnLocation(
            currentSaveSlot,
            isIndoor ? 1 : 0,
            isIndoor ? room.mapId() : 0,
            room.roomId(),
            link.roomEntryRomPositionX(),
            link.roomEntryRomPositionY(),
            indoorRoom);
    }

    private static BackgroundScene loadFileMenuBackgroundScene(String sceneId) {
        BackgroundSceneSpec spec = BackgroundSceneCatalog.forFileMenuScene(sceneId);
        if (spec == null) {
            throw new IllegalArgumentException("No file-menu background scene for " + sceneId);
        }
        return backgroundSceneLoader.load(spec);
    }

    private static void setCutsceneScene(String sceneId) {
        if (BackgroundSceneCatalog.requiresTitleTileset(sceneId)) {
            gpu.loadTitleScreenTiles(romData);
        }
        if (IntroCutsceneScript.SCENE_TITLE.equals(sceneId) && currentScreen == SCREEN_CUTSCENE) {
            playDirectMusic(naturalIntroTitleMusicTrack());
        }
        BackgroundSceneSpec spec = BackgroundSceneCatalog.forCutsceneScene(sceneId);
        if (spec != null) {
            applyBackgroundScene(backgroundSceneLoader.load(spec));
        }
    }

    private static void playDirectMusic(int trackId) {
        if (gameplayMusicController != null) {
            gameplayMusicController.playDirect(trackId);
            overworldIntroContinuationPending =
                (trackId & 0xFF) == MusicTrackIds.MUSIC_OVERWORLD_INTRO;
        }
    }

    static int introCutsceneMusicTrack() {
        return MusicTrackIds.MUSIC_TITLE_CUTSCENE;
    }

    static int naturalIntroTitleMusicTrack() {
        return MusicTrackIds.MUSIC_TITLE_SCREEN;
    }

    static int newGameHouseMusicTrack() {
        return MusicTrackIds.MUSIC_INTRO_WAKE_UP;
    }

    static int directTitleScreenMusicTrack() {
        return MusicTrackIds.MUSIC_TITLE_SCREEN_NO_INTRO;
    }

    static int fileSelectionMusicTrack() {
        return MusicTrackIds.MUSIC_FILE_SELECT;
    }

    static boolean shouldEnterFileSelection(int screen, int key, int action) {
        return screen == SCREEN_TITLE && key == GLFW_KEY_ENTER && action == GLFW_PRESS;
    }

    static boolean shouldEnterFileSave(InputConfig inputConfig, InputState inputState,
                                       int key, int action) {
        if (inputConfig == null || inputState == null || action != GLFW_PRESS
            || key != inputConfig.selectKey()) {
            return false;
        }
        return inputState.isDown(inputConfig.aKey())
            && inputState.isDown(inputConfig.bKey())
            && inputState.isDown(inputConfig.menuOpenKey())
            && inputState.isDown(inputConfig.selectKey());
    }

    private static boolean canPresentFileSave() {
        return !scrollController.isActive()
            && !transitionController.isInputBlocked()
            && (inventoryController == null || !inventoryController.shouldBlockOverworldInput())
            && (dialogController == null || !GameplayDialogInput.blocksGameplay(dialogController));
    }

    private static void applyBackgroundScene(BackgroundScene scene) {
        currentTilemap = scene.tilemap();
        currentAttrmap = scene.attrmap();
        bgPalettes = scene.palettes();
        objPalettes = scene.objectPalettes();
    }

    private static void loadOverworldScreen() {
        roomSession.loadInitialOverworld(StartupCoordinator.gameplayStartRoomId(currentAppConfig()));
        // Center of Mabe Village Square has the statue (solid). Spawn Link two
        // tiles to the left of the room center so he lands on walkable ground.
        int startX = ROOM_PIXEL_WIDTH / 2 - Link.SPRITE_SIZE / 2 - 32 + 10;
        int startY = ROOM_PIXEL_HEIGHT / 2 - Link.SPRITE_SIZE / 2 - 10;
        link.setRoomEntryPixelPosition(startX, startY);
    }

    private static void initGLFW() {
        GLFWErrorCallback.createPrint(System.err).set();

        if (!glfwInit()) {
            throw new IllegalStateException("Failed to initialize GLFW");
        }

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);

        window = glfwCreateWindow(
            Framebuffer.WIDTH * Framebuffer.SCALE,
            Framebuffer.HEIGHT * Framebuffer.SCALE,
            "Link's Awakening DX",
            0,
            0
        );

        if (window == 0) {
            throw new IllegalStateException("Failed to create window");
        }

        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);
        glfwShowWindow(window);

        try {
            Thread.sleep(200);
        } catch (InterruptedException ignored) {
        }

        glfwSetKeyCallback(window, new GLFWKeyCallback() {
            @Override
            public void invoke(long handle, int key, int scancode, int action, int mods) {
                onKeyEvent(key, action);
            }
        });
    }

    private static void initOpenGL() {
        framePresenter = OpenGlFramePresenter.initialize(window);
    }

    private static void composeFrameBuffer() {
        frameSceneBuilder.build(currentGameFrameState()).drawTo(indexedDisplayBuffer, gpu);
    }

    private static GameFrameState currentGameFrameState() {
        return GameFrameState.empty()
            .withScreen(currentRenderScreen())
            .withBackground(currentTilemap, currentAttrmap, bgPalettes, objPalettes)
            .withCutsceneManager(cutsceneManager)
            .withIntroFrameSnapshot(cutsceneManager == null ? null : cutsceneManager.frameSnapshot())
            .withFileMenuFrame(currentScreen == SCREEN_FILE_MENU && fileMenuController != null
                ? fileMenuController.snapshot()
                : currentScreen == SCREEN_FILE_SAVE && fileSaveController != null
                    ? fileSaveController.snapshot() : null)
            .withRoom(roomSession == null ? null : roomSession.renderSnapshot(), scrollController, transitionController)
            .withLink(link)
            .withTransientVfx(transientVfxSystem, cutLeavesEffectRenderer, GREEN_OBJECTS_SPRITE_PALETTE)
            .withDroppableRupees(droppableRupeeSystem)
            .withInventoryController(inventoryController)
            .withDialogController(dialogController)
            .withFrameCounter(frameCounter);
    }

    private static RenderScreen currentRenderScreen() {
        return renderScreenFor(currentScreen);
    }

    static RenderScreen renderScreenFor(int screen) {
        switch (screen) {
            case SCREEN_OVERWORLD:
                return RenderScreen.OVERWORLD;
            case SCREEN_CUTSCENE:
                return RenderScreen.CUTSCENE;
            case SCREEN_FILE_MENU:
                return RenderScreen.FILE_MENU;
            case SCREEN_FILE_SAVE:
                return RenderScreen.FILE_SAVE;
            case SCREEN_TITLE:
            default:
                return RenderScreen.TITLE;
        }
    }

    private static void cleanup() {
        if (framePresenter != null) {
            framePresenter.cleanup();
        }
        if (dialogSoundOutput != null) {
            dialogSoundOutput.close();
        }
        if (musicPlayer != null) {
            musicPlayer.close();
        }
        glfwDestroyWindow(window);
        glfwTerminate();
    }
}
