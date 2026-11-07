package linksawakening;

import linksawakening.app.screen.*;
import linksawakening.app.gameplay.*;
import linksawakening.app.SessionBootstrap;
import linksawakening.app.SaveCoordinator;
import linksawakening.app.MusicDirector;
import linksawakening.app.story.OpeningStory;

import linksawakening.config.AppConfig;
import linksawakening.cutscene.CutsceneManager;
import linksawakening.cutscene.IntroCutsceneScript;
import linksawakening.dialog.DialogController;
import linksawakening.entity.Link;
import linksawakening.entity.LinkSpriteSheet;
import linksawakening.equipment.EquipmentController;
import linksawakening.equipment.ItemRegistry;
import linksawakening.equipment.Sword;
import linksawakening.audio.music.GameplayMusicController;
import linksawakening.audio.music.FrameMusicPlayer;
import linksawakening.gameplay.DialogTextLoader;
import linksawakening.gameplay.DialogSoundRouter;
import linksawakening.gameplay.DialogSoundSink;
import linksawakening.gameplay.GameplaySoundSink;
import linksawakening.gameplay.GameplayDialogInput;
import linksawakening.gameplay.OverworldDialogBlockers;
import linksawakening.gameplay.OverworldDialogInteraction;
import linksawakening.gameplay.PcmSoundOutput;
import linksawakening.gpu.GPU;
import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
import linksawakening.physics.OverworldCollision;
import linksawakening.render.GameFrameSceneBuilder;
import linksawakening.render.GameFrameState;
import linksawakening.render.RenderScreen;
import linksawakening.rom.RomTables;
import linksawakening.save.SaveRamStore;
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
import linksawakening.ui.FileMenuController;
import linksawakening.ui.FileMenuRomData;
import linksawakening.ui.FileSaveController;
import linksawakening.vfx.CutLeavesEffectRenderer;
import linksawakening.vfx.TransientVfxSystem;
import linksawakening.world.DroppableRupeeSystem;
import linksawakening.world.ActiveRoom;
import linksawakening.world.OverworldBushInteraction;
import linksawakening.world.RoomSession;
import linksawakening.world.RoomTransitionCoordinator;
import linksawakening.world.ScrollController;
import linksawakening.world.TransitionController;
import linksawakening.world.Warp;

import linksawakening.world.RomRandomByteSource;

import static linksawakening.world.RoomConstants.*;
import static org.lwjgl.glfw.GLFW.*;

public class GameSession {

    private final int OBJECT_GROUND_STAIRS = 0xC6;
    private final int GAME_BOY_BG_MAP_TILES = 32 * 32;

    private final RomRandomByteSource random;

    public GameSession() {
        this(new RomRandomByteSource());
    }

    public GameSession(RomRandomByteSource random) {
        this.random = java.util.Objects.requireNonNull(random);
    }

    private Host host = new HeadlessHost();
    private GPU gpu;
    private final GameFrameSceneBuilder frameSceneBuilder = new GameFrameSceneBuilder();
    private byte[] romData;
    private int[] currentTilemap;
    private int[] currentAttrmap;
    private int[][] bgPalettes;
    private int[][] objPalettes;
    private byte[] indexedDisplayBuffer;
    private RoomSession roomSession;
    private final TransitionController transitionController = new TransitionController();
    private RoomTransitionCoordinator roomTransitionCoordinator;

    private boolean running = true;
    private boolean debugNoClip;
    private int frameCounter;

    // Room pixel dimensions
    private final int ROOM_PIXEL_WIDTH = ROOM_TILE_WIDTH * 8;   // 160
    private final int ROOM_PIXEL_HEIGHT = ROOM_TILE_HEIGHT * 8; // 128

    // Scroll state
    private final int SCROLL_SPEED = 4; // pixels per frame (original uses ~2-4px)

    private final ScrollController scrollController = new ScrollController();
    // Mirrors the first OBJ palette entry used for Link and leaf particles.
    private final int[] GREEN_OBJECTS_SPRITE_PALETTE = {
        0x00000000,
        0x00000000,
        0x0010A840,
        0x00F8B888,
    };

    private InputConfig inputConfig;
    private InputState inputState;
    private PlayerState playerState;
    private BackgroundSceneLoader backgroundSceneLoader;
    private InventoryMenu inventoryMenu;
    private InventoryController inventoryController;
    private DialogController dialogController;
    private DialogTextLoader dialogTextLoader;
    private OverworldDialogInteraction overworldDialogInteraction;
    private boolean overworldDialogInputConsumedThisFrame;
    private DialogSoundSink dialogSoundSink;
    private PcmSoundOutput dialogSoundOutput;
    private GameplaySoundSink gameplaySoundSink = GameplaySoundSink.none();
    private FrameMusicPlayer musicPlayer;
    private GameplayMusicController gameplayMusicController;
    private MusicDirector musicDirector;
    private CutsceneManager cutsceneManager;
    private FileMenuController fileMenuController;
    private FileSaveController fileSaveController;
    private SaveRamStore saveRamStore;
    private int currentSaveSlot = -1;
    private RomTables romTables;
    private OverworldCollision overworldCollision;
    private OverworldBushInteraction overworldBushInteraction;
    private LinkSpriteSheet linkSpriteSheet;
    private TransientVfxSystem transientVfxSystem;
    private DroppableRupeeSystem droppableRupeeSystem;
    private CutLeavesEffectRenderer cutLeavesEffectRenderer;
    private ItemRegistry itemRegistry;
    private EquipmentController equipmentController;
    private Link link;
    private MarinWakeUpMotion newGameWakeUpMotion;
    private TarinShieldMotion newGameTarinShieldMotion;
    private int[] newGameTarinShieldPalette;
    private AppConfig appConfig;
    private final Screen titleScreen = new TitleScreen(new TitleScreen.Dependencies(() -> cutsceneManager, () -> inputState, this::startFileSelection));
    private final Screen cutsceneScreen = new CutsceneScreen(new CutsceneScreen.Dependencies(() -> cutsceneManager, () -> inputState, () -> dialogController, () -> inputConfig, this::routeDialogSounds, () -> currentScreen = titleScreen, () -> { currentScreen = titleScreen; musicDirector.playDirectMusic(MusicDirector.directTitleScreenMusicTrack()); }));
    private final Screen fileMenuScreen = new FileMenuScreen(new FileMenuScreen.Dependencies(() -> fileMenuController, () -> inputState, () -> inputConfig, () -> saveRamStore, slot -> currentSaveSlot = slot, () -> saves().startNewGame(), saved -> saves().startSavedGame(saved), mutation -> saves().persistFileMenuMutation(mutation), this::completeInputFrame));
    private final Screen fileSaveScreen = new FileSaveScreen(new FileSaveScreen.Dependencies(() -> fileSaveController, () -> inputState, () -> inputConfig, () -> { fileSaveController = null; currentScreen = this.gameplayScreen; }, () -> saves().saveCurrentPlayerState(), this::startFileSelection, this::completeInputFrame));
    private final Screen gameplayScreen = new GameplayScreen(this::tickGameplayScreen, this::gameplayKey);
    private Screen currentScreen = titleScreen;

    private final long FRAME_DURATION_NS = 1_000_000_000L / 60L;

    public void run(String[] args) {
        initialize(false);
        if (StartupCoordinator.shouldStartIntroCutscene(currentAppConfig())) {
            startIntroCutscene();
        } else if (currentAppConfig().startMode() == AppConfig.StartMode.NORMAL
            || !currentAppConfig().showTitleScreen()) {
            startConfiguredGameplay();
        } else {
            startTitleScreenWithoutIntro();
        }

        host = new DesktopHost();
        host.open(this::onKeyEvent);

        long nextFrameNs = System.nanoTime();

        while (!host.shouldClose() && running) {
            host.pollEvents();
            update();
            composeFrameBuffer();
            host.present(indexedDisplayBuffer);

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

    /** Initializes this session using the desktop update, input and render paths. */
    void initializeHeadlessNewGame() {
        if (inputState != null) throw new IllegalStateException("Game already initialized");
        initialize(true);
        dialogController.setPlayerName("A");
        saves().startNewGame();
    }

    void headlessButton(String button, boolean pressed) {
        if (button.equals("none")) return;
        if (button.contains("+")) {
            for (String part : button.split("\\+")) headlessButton(part, pressed);
            return;
        }
        int key = switch (button) {
            case "up" -> inputConfig.upKey();
            case "down" -> inputConfig.downKey();
            case "left" -> inputConfig.leftKey();
            case "right" -> inputConfig.rightKey();
            case "a" -> inputConfig.aKey();
            case "b" -> inputConfig.bKey();
            case "start" -> inputConfig.menuOpenKey();
            case "select" -> inputConfig.selectKey();
            default -> throw new IllegalArgumentException("Unknown button: " + button);
        };
        onKeyEvent(key, pressed ? GLFW_PRESS : GLFW_RELEASE);
    }

    byte[] headlessFrame() {
        update();
        composeFrameBuffer();
        return indexedDisplayBuffer.clone();
    }

    boolean headlessHasInventoryItem(int item) {
        if (playerState.itemA() == item || playerState.itemB() == item) return true;
        for (int slot = 0; slot < PlayerState.SUBSCREEN_SLOT_COUNT; slot++) {
            if (playerState.subscreenItem(slot) == item) return true;
        }
        return false;
    }

    String headlessInstrumentTrace() {
        return roomSession == null ? "" : roomSession.instrumentTrace();
    }

    String headlessMoblinKingTrace() {
        return roomSession == null ? "-1" : roomSession.moblinKingTrace() + "," + playerState.activePowerUp();
    }

    int headlessBowWowState() { return roomSession == null ? 0 : roomSession.bowWowState(); }

    String headlessCombatTrace() {
        Sword sword = equipmentController.activeSword();
        Sword.CollisionBox box = sword == null ? Sword.CollisionBox.inactive()
            : sword.enemyCollisionBox(link.romEntityX(), link.romSwordCollisionY(), link.direction());
        return "%d,%d,%d,%d,%d;%s".formatted(box.active() ? 1 : 0, box.x(), box.width(), box.y(), box.height(),
            roomSession.activeRoom().entities().loadedEntities().stream()
                .filter(e -> e.type() == 0x81 || e.type() == 0x82 || e.type() == 0x59)
                .map(e -> "%d,%d,%d,%d,%d,%d".formatted(e.type(), e.x(), e.y(), e.z(),
                    roomSession.entityHealth(e.slot()), e.status() == linksawakening.world.EntityStatus.ACTIVE ? 5 : 1))
                .collect(java.util.stream.Collectors.joining(";")));
    }

    String headlessEntityPositions() {
        return roomSession.activeRoom().entities().loadedEntities().stream()
            .map(e -> "%02X:%s@%d,%d".formatted(e.type(), e.status(), e.x(), e.y()))
            .collect(java.util.stream.Collectors.joining(" "));
    }

    HeadlessGameDriver.State headlessState() {
        var tarin = roomSession.activeRoom().entities().loadedEntities().stream()
            .filter(entity -> entity.type() == 0x3F).findFirst().orElse(null);
        return new HeadlessGameDriver.State(frameCounter, roomSession.mapId(), roomSession.currentRoomId(),
            link.romEntityX(), link.romEntityY(), dialogController.isActive(),
            dialogController.isWaitingForPageAdvance(), dialogController.visibleText(),
            playerState.shieldLevel(), playerState.health(), playerState.swordLevel(), (int) roomSession.activeRoom().entities().loadedEntities().stream()
                .filter(entity -> entity.type() == 0xC5 && entity.status() == linksawakening.world.EntityStatus.ACTIVE).count(), playerState.hasToadstool(), playerState.magicPowderCount(), inventoryMenu.instrumentPaletteTick(), tarin == null ? -1 : tarin.x(), tarin == null ? -1 : tarin.y(),
                (roomSession.overworldRoomStatusSnapshot()[0x51] & 0x10) != 0,
                playerState.tailKeyCount(), gpu.animatedTilesFrameCount(),
                roomSession.currentDungeonItemFlagsSnapshot()[4] & 0xFF,
                roomSession.currentDungeonItemFlagsSnapshot()[1] & 0xFF,
                roomSession.currentDungeonItemFlagsSnapshot()[0] & 0xFF,
                roomSession.currentDungeonItemFlagsSnapshot()[3] & 0xFF, link.romAnimationState(), 3 - link.direction(), equipmentController.activeSword() == null ? 0 : equipmentController.activeSword().state(), link.romEntityZ(), link.zVelocity(), link.romSpeedX(), link.romSpeedY(), scrollController.isActive() || transitionController.isInputBlocked(), playerState.maxHearts(), roomSession.dungeonProgressFlagsSnapshot()[0] & 0xFF);
    }

    private void onKeyEvent(int key, int action) {
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

        currentScreen.onKey(key, action);
    }

    private void gameplayKey(int key, int action) {
        if (action != GLFW_PRESS) return;

        if (roomSession != null && roomSession.tailCaveKeyholeSequenceActive()) {
            return;
        }

        if (openingStory().marinWakeUpBlocksKeyInput()) {
            if (GameplayDialogInput.handleOverworldKeyPress(
                key, action, inputConfig, dialogController)) {
                overworldDialogInputConsumedThisFrame = true;
            }
            return;
        }

        if (shouldEnterFileSave(inputConfig, inputState, key, action)
            && canPresentFileSave()) {
            saves().startFileSave();
            return;
        }

        if (GameplayDialogInput.handleOverworldKeyPress(key, action, inputConfig, dialogController)) {
            overworldDialogInputConsumedThisFrame = true;
            return;
        }
        if (openingStory().tryOpenTarinShieldDialog(key)) {
            return;
        }
        if (openingStory().tryOpenMarinFollowUpDialog(key)) {
            return;
        }
        if (dialogs().tryOpenChest(key)) {
            return;
        }
        if (dialogs().tryOpenOverworldDialog(key)) {
            return;
        }

        inventoryController.dispatchToggleInput();

        // Debug: F1 dumps Link's surrounding cells with object IDs and
        // physics flags, to diagnose walk-through-wall/tree reports.
        if (shouldRunDebugDump(currentAppConfig(), key, action)) {
            dumpLinkSurroundings();
        }
    }

    boolean skipIntroCutsceneIfRequested(int key, int action, CutsceneManager manager) {
        return key == GLFW_KEY_ENTER
            && action == GLFW_PRESS
            && manager != null
            && manager.skipIntroToTitle();
    }

    boolean shouldRunDebugDump(AppConfig config, int key, int action) {
        return config.debugEnabled() && key == GLFW_KEY_F1 && action == GLFW_PRESS;
    }

    private void dumpLinkSurroundings() {
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

    private void update() {
        frameCounter = (frameCounter + 1) & 0xFF;
        // The engine's frame boundary uses scanline zero for non-entity draws.
        random.beginFrame(frameCounter, 0);

        musicDirector.tick();

        currentScreen.tick();
    }

    private void tickGameplayScreen() {
        new GameplayFrame(
            new TransitionPhase(scrollController, transitionController, link, SCROLL_SPEED),
            () -> simulation().tick(),
            this::completeInputFrame).tick();
    }

    private void completeInputFrame() {
        // Run last so all consumers observe this frame's input edges.
        inputState.tickEdges();
        overworldDialogInputConsumedThisFrame = false;
    }

    private void routeDialogSounds() {
        DialogSoundRouter.routePending(dialogController, dialogSoundSink);
    }

    /** Mirrors BombEntityHandler's B-slot-first equipped-bomb input check. */

    private int romDirectionForLink(int javaDirection) {
        return switch (javaDirection) {
            case Link.DIRECTION_RIGHT -> 0;
            case Link.DIRECTION_LEFT -> 1;
            case Link.DIRECTION_UP -> 2;
            case Link.DIRECTION_DOWN -> 3;
            default -> throw new IllegalArgumentException("Link direction out of range: "
                + javaDirection);
        };
    }

    private OverworldDialogBlockers currentOverworldDialogBlockers() {
        return new OverworldDialogBlockers(
            inventoryController != null && inventoryController.shouldBlockOverworldInput(),
            scrollController.isActive(),
            transitionController.isInputBlocked(),
            GameplayDialogInput.blocksGameplay(dialogController),
            overworldDialogInputConsumedThisFrame);
    }

    private AppConfig currentAppConfig() {
        return appConfig != null ? appConfig : AppConfig.defaults();
    }

    /** Applies the configured player profile before a direct gameplay launch. */
    void applyConfiguredItemProfile(AppConfig config, PlayerState playerState) {
        if (config.itemProfile() == AppConfig.ItemProfile.NEW_GAME) {
            NewGameStartProfile.romDefaults().initializePlayerState(playerState);
        }
    }

    private void startConfiguredGameplay() {
        dispatchConfiguredGameplay(
            currentAppConfig(), () -> saves().startNewGame(), this::startConfiguredLocationGameplay);
    }

    private void startDebugGameplay() {
        if (playerState == null || roomSession == null || link == null) {
            return;
        }
        if (currentScreen == cutsceneScreen && cutsceneManager != null) {
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
        currentScreen = gameplayScreen;
        debugNoClip = false;
        link.setDirection(Link.DIRECTION_DOWN);
        gpu.loadBaseTiles(romData);
        loadOverworldScreen();
        System.out.println("Debug gameplay started (F3 toggles no-clip)");
    }

    private boolean tryDebugScreenScroll() {
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

    void dispatchConfiguredGameplay(AppConfig config, Runnable startNewGame,
                                           Runnable startConfiguredLocation) {
        if (StartupCoordinator.shouldStartNewGameGameplay(config)) {
            startNewGame.run();
        } else {
            startConfiguredLocation.run();
        }
    }

    private void startConfiguredLocationGameplay() {
        fileMenuController = null;
        fileSaveController = null;
        currentSaveSlot = -1;
        newGameWakeUpMotion = null;
        newGameTarinShieldMotion = null;
        currentScreen = gameplayScreen;
        applyConfiguredItemProfile(currentAppConfig(), playerState);
        if (currentAppConfig().playIntroStory()) {
            System.err.println("Intro story startup is not implemented yet; spawning at start location");
        }
        loadOverworldScreen();
    }

    private void startIntroCutscene() {
        fileMenuController = null;
        fileSaveController = null;
        currentScreen = cutsceneScreen;
        musicDirector.playDirectMusic(MusicDirector.introCutsceneMusicTrack());
        gpu.loadIntroSequenceTiles(romData);
        cutsceneManager.startIntro(romData, this::loadIntroBackgroundScene);
    }

    private BackgroundScene loadIntroBackgroundScene(String sceneId) {
        BackgroundSceneSpec spec = BackgroundSceneCatalog.forCutsceneScene(sceneId);
        if (spec == null) {
            throw new IllegalArgumentException("No intro background scene for " + sceneId);
        }
        return backgroundSceneLoader.load(spec);
    }

    private void startTitleScreenWithoutIntro() {
        fileMenuController = null;
        fileSaveController = null;
        currentScreen = titleScreen;
        musicDirector.playDirectMusic(MusicDirector.directTitleScreenMusicTrack());
    }

    private void startFileSelection() {
        currentScreen = fileMenuScreen;
        fileSaveController = null;
        gpu.loadMenuTiles(romData);
        fileMenuController = new FileMenuController(
            new FileMenuRomData(romData),
            this::loadFileMenuBackgroundScene,
            saveRamStore.saveFilesMask(),
            saveRamStore.savedNames(),
            new int[]{saveRamStore.readSlot(0).maxHearts(), saveRamStore.readSlot(1).maxHearts(),
                saveRamStore.readSlot(2).maxHearts()},
            new int[]{saveRamStore.readSlot(0).deathCountBcd(), saveRamStore.readSlot(1).deathCountBcd(),
                saveRamStore.readSlot(2).deathCountBcd()});
        musicDirector.playDirectMusic(MusicDirector.fileSelectionMusicTrack());
        // The Enter event that leaves the title must not also activate the
        // first empty file. Its edge belongs to the title screen.
        inputState.tickEdges();
    }

    /** Mirrors the source's wSpawnLocationData fields copied by SaveGameToFile. */

    private BackgroundScene loadFileMenuBackgroundScene(String sceneId) {
        BackgroundSceneSpec spec = BackgroundSceneCatalog.forFileMenuScene(sceneId);
        if (spec == null) {
            throw new IllegalArgumentException("No file-menu background scene for " + sceneId);
        }
        return backgroundSceneLoader.load(spec);
    }

    private void setCutsceneScene(String sceneId) {
        if (BackgroundSceneCatalog.requiresTitleTileset(sceneId)) {
            gpu.loadTitleScreenTiles(romData);
        }
        if (IntroCutsceneScript.SCENE_TITLE.equals(sceneId) && currentScreen == cutsceneScreen) {
            musicDirector.playDirectMusic(MusicDirector.naturalIntroTitleMusicTrack());
        }
        BackgroundSceneSpec spec = BackgroundSceneCatalog.forCutsceneScene(sceneId);
        if (spec != null) {
            applyBackgroundScene(backgroundSceneLoader.load(spec));
        }
    }

    boolean shouldEnterFileSelection(Screen screen, int key, int action) {
        return screen instanceof TitleScreen && key == GLFW_KEY_ENTER && action == GLFW_PRESS;
    }

    boolean shouldEnterFileSave(InputConfig inputConfig, InputState inputState,
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

    private boolean canPresentFileSave() {
        return !scrollController.isActive()
            && !transitionController.isInputBlocked()
            && (inventoryController == null || !inventoryController.shouldBlockOverworldInput())
            && (dialogController == null || !GameplayDialogInput.blocksGameplay(dialogController));
    }

    private void applyBackgroundScene(BackgroundScene scene) {
        currentTilemap = scene.tilemap();
        currentAttrmap = scene.attrmap();
        bgPalettes = scene.palettes();
        objPalettes = scene.objectPalettes();
    }

    private void loadOverworldScreen() {
        roomSession.loadInitialOverworld(StartupCoordinator.gameplayStartRoomId(currentAppConfig()));
        // Center of Mabe Village Square has the statue (solid). Spawn Link two
        // tiles to the left of the room center so he lands on walkable ground.
        int startX = ROOM_PIXEL_WIDTH / 2 - Link.SPRITE_SIZE / 2 - 32 + 10;
        int startY = ROOM_PIXEL_HEIGHT / 2 - Link.SPRITE_SIZE / 2 - 10;
        link.setRoomEntryPixelPosition(startX, startY);
    }

    private void composeFrameBuffer() {
        if (inventoryMenu != null && roomSession != null && roomSession.activeRoom() != null) {
            inventoryMenu.setWorldContext(roomSession.activeRoom().mapCategory() != Warp.CATEGORY_OVERWORLD,
                roomSession.activeRoom().mapId(), roomSession.dungeonProgressFlagsSnapshot());
            inventoryMenu.setDungeonContext(roomSession.currentDungeonItemFlagsSnapshot(),
                roomSession.dungeonMinimapRoomStatusSnapshot(), roomSession.indoorRoomPositionForSave(), frameCounter);
        }
        frameSceneBuilder.build(currentGameFrameState()).drawTo(indexedDisplayBuffer, gpu);
    }

    private GameFrameState currentGameFrameState() {
        return currentScreen.renderState(GameFrameState.empty()
            .withScreen(currentRenderScreen())
            .withBackground(currentTilemap, currentAttrmap, bgPalettes, objPalettes)
            .withCutsceneManager(cutsceneManager)
            .withIntroFrameSnapshot(cutsceneManager == null ? null : cutsceneManager.frameSnapshot())
            .withRoom(roomSession == null ? null : roomSession.renderSnapshot(), scrollController, transitionController)
            .withLink(link)
            .withTransientVfx(transientVfxSystem, cutLeavesEffectRenderer, GREEN_OBJECTS_SPRITE_PALETTE)
            .withDroppableRupees(droppableRupeeSystem)
            .withInventoryController(inventoryController)
            .withDialogController(dialogController)
            .withFrameCounter(frameCounter));
    }

    private RenderScreen currentRenderScreen() {
        return renderScreenFor(currentScreen);
    }

    RenderScreen renderScreenFor(Screen screen) {
        return screen.renderScreen();
    }

    private void cleanup() {
        host.releasePresentation();
        if (dialogSoundOutput != null) {
            dialogSoundOutput.close();
        }
        if (musicPlayer != null) {
            musicPlayer.close();
        }

        host.close();
    }

    private OpeningStory openingStory() {
        return new OpeningStory(frameCounter, newGameWakeUpMotion, newGameTarinShieldMotion, newGameTarinShieldPalette, inputState, inputConfig, dialogController, dialogTextLoader, roomSession, link, playerState,
            this::currentOverworldDialogBlockers, musicDirector::playDirectMusic,
            MusicDirector::newGameHouseMusicTrack, this::romDirectionForLink);
    }
    private void initialize(boolean headless) {
        gpu = new GPU();
        var built = new SessionBootstrap(random, gpu, transitionController, scrollController,
            new SessionBootstrap.Callbacks(this::setCutsceneScene, room -> musicDirector.selectMusicForLoadedRoom(room, playerState, roomSession),
                this::romDirectionForLink)).build(headless);
        bgPalettes = built.bgPalettes();
        objPalettes = built.objPalettes();
        appConfig = built.appConfig();
        romData = built.romData();
        currentTilemap = built.currentTilemap();
        currentAttrmap = built.currentAttrmap();
        indexedDisplayBuffer = built.indexedDisplayBuffer();
        roomSession = built.roomSession();
        roomTransitionCoordinator = built.roomTransitionCoordinator();
        inputConfig = built.inputConfig();
        inputState = built.inputState();
        playerState = built.playerState();
        backgroundSceneLoader = built.backgroundSceneLoader();
        inventoryMenu = built.inventoryMenu();
        inventoryController = built.inventoryController();
        dialogController = built.dialogController();
        dialogTextLoader = built.dialogTextLoader();
        overworldDialogInteraction = built.overworldDialogInteraction();
        dialogSoundSink = built.dialogSoundSink();
        dialogSoundOutput = built.dialogSoundOutput();
        gameplaySoundSink = built.gameplaySoundSink();
        musicPlayer = built.musicPlayer();
        gameplayMusicController = built.gameplayMusicController();
        musicDirector = new MusicDirector(musicPlayer, gameplayMusicController);
        cutsceneManager = built.cutsceneManager();
        saveRamStore = built.saveRamStore();
        romTables = built.romTables();
        overworldCollision = built.overworldCollision();
        overworldBushInteraction = built.overworldBushInteraction();
        linkSpriteSheet = built.linkSpriteSheet();
        transientVfxSystem = built.transientVfxSystem();
        droppableRupeeSystem = built.droppableRupeeSystem();
        cutLeavesEffectRenderer = built.cutLeavesEffectRenderer();
        itemRegistry = built.itemRegistry();
        equipmentController = built.equipmentController();
        link = built.link();
        newGameTarinShieldPalette = built.newGameTarinShieldPalette();
    }
    private SaveCoordinator saves() {
        return new SaveCoordinator(playerState, roomSession, link, gpu, romData, saveRamStore, currentSaveSlot, inputState,
            () -> { fileMenuController = null; fileSaveController = null; currentScreen = gameplayScreen; },
            () -> { newGameWakeUpMotion = null; newGameTarinShieldMotion = null; },
            () -> { newGameWakeUpMotion = new MarinWakeUpMotion(); newGameTarinShieldMotion = new TarinShieldMotion(); },
            () -> musicDirector.playDirectMusic(MusicDirector.newGameHouseMusicTrack()),
            () -> openingStory().updateNewGameMarinPresentation(),
            saved -> openingStory().restoreNewGameHouseRuntimeIfNeeded(saved, motion -> newGameWakeUpMotion = motion, motion -> newGameTarinShieldMotion = motion, () -> openingStory().updateNewGameMarinPresentation()),
            this::startFileSelection,
            () -> { currentScreen = fileSaveScreen; fileMenuController = null; },
            controller -> fileSaveController = controller,
            this::loadFileMenuBackgroundScene);
    }
    private BushInteraction bushes() {
        return new BushInteraction(roomSession, overworldBushInteraction, link, equipmentController, gameplaySoundSink, transientVfxSystem, droppableRupeeSystem);
    }
    private DialogInteractions dialogs() {
        return new DialogInteractions(inputConfig, overworldDialogInteraction, roomSession, link, playerState, dialogController, dialogTextLoader, this::currentOverworldDialogBlockers);
    }
    private LiftInteraction lifting() {
        return new LiftInteraction(inputState, inputConfig, playerState, link, roomSession, this::romDirectionForLink);
    }
    private GameplaySimulation simulation() {
        return new GameplaySimulation(gpu, romData, roomSession, transitionController, roomTransitionCoordinator, debugNoClip, frameCounter, ROOM_PIXEL_HEIGHT, scrollController, inputConfig, inputState, playerState, inventoryMenu, inventoryController, dialogController, dialogTextLoader, gameplaySoundSink, musicPlayer, gameplayMusicController, musicDirector, transientVfxSystem, droppableRupeeSystem, itemRegistry, equipmentController, link,
            openingStory(), lifting(), bushes(), dialogs(), this::currentOverworldDialogBlockers, this::tryDebugScreenScroll, this::romDirectionForLink, this::routeDialogSounds);
    }
}
