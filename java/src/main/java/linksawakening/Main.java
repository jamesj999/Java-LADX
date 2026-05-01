package linksawakening;

import linksawakening.config.AppConfig;
import linksawakening.cutscene.CutsceneManager;
import linksawakening.cutscene.IntroCutsceneScript;
import linksawakening.dialog.DialogController;
import linksawakening.entity.Link;
import linksawakening.entity.LinkSpriteSheet;
import linksawakening.equipment.EquipmentController;
import linksawakening.equipment.ItemRegistry;
import linksawakening.equipment.RocsFeather;
import linksawakening.equipment.Sword;
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
import linksawakening.gameplay.DialogTextLoader;
import linksawakening.gameplay.DialogSoundRouter;
import linksawakening.gameplay.DialogSoundSink;
import linksawakening.gameplay.GameplaySoundSink;
import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.gameplay.GameplayDialogInput;
import linksawakening.gameplay.OverworldDialogBlockers;
import linksawakening.gameplay.OverworldDialogInteraction;
import linksawakening.gameplay.PcmSoundOutput;
import linksawakening.gameplay.SfxDialogSoundSink;
import linksawakening.gameplay.SfxGameplaySoundSink;
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
import linksawakening.scene.BackgroundScene;
import linksawakening.scene.BackgroundSceneCatalog;
import linksawakening.scene.BackgroundSceneLoader;
import linksawakening.scene.BackgroundSceneSpec;
import linksawakening.state.PlayerState;
import linksawakening.startup.StartupCoordinator;
import linksawakening.ui.InventoryMenu;
import linksawakening.ui.InventoryController;
import linksawakening.ui.InventoryTilemapLoader;
import linksawakening.vfx.CutLeavesEffectRenderer;
import linksawakening.vfx.TransientVfxSpriteSheet;
import linksawakening.vfx.TransientVfxSystem;
import linksawakening.vfx.TransientVfxType;
import linksawakening.world.DroppableRupeeSystem;
import linksawakening.world.ActiveRoom;
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
    private static int currentScreen = 0;
    private static final int SCREEN_TITLE = 0;
    private static final int SCREEN_OVERWORLD = 1;
    private static final int SCREEN_CUTSCENE = 2;

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
    private static OverworldDialogInteraction overworldDialogInteraction;
    private static boolean overworldDialogInputConsumedThisFrame;
    private static DialogSoundSink dialogSoundSink;
    private static PcmSoundOutput dialogSoundOutput;
    private static GameplaySoundSink gameplaySoundSink = GameplaySoundSink.none();
    private static OpenAlMusicPlayer musicPlayer;
    private static GameplayMusicController gameplayMusicController;
    private static CutsceneManager cutsceneManager;
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
    private static AppConfig appConfig;


    private static final long FRAME_DURATION_NS = 1_000_000_000L / 60L;

    public static void main(String[] args) {
        gpu = new GPU();

        loadROM();
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
        inputConfig = currentAppConfig().inputConfig();
        inputState = new InputState();
        playerState = new PlayerState();
        initMusicSystem();
        dialogSoundOutput = new OpenAlPcmSoundOutput();
        dialogSoundSink = new SfxDialogSoundSink(dialogSoundOutput);
        gameplaySoundSink = new SfxGameplaySoundSink(romData, dialogSoundOutput);
        InventoryTilemapLoader tilemapLoader = InventoryTilemapLoader.loadFromRom(romData);
        inventoryMenu = new InventoryMenu(tilemapLoader, playerState, gameplaySoundSink);
        inventoryController = new InventoryController(inputState, inputConfig, inventoryMenu);
        dialogController = new DialogController(16);
        overworldDialogInteraction = new OverworldDialogInteraction(
            SignpostDialogTable.loadFromRom(romData), DialogTextLoader.loadFromRom(romData));
        cutsceneManager = new CutsceneManager(dialogController, Main::setCutsceneScene);
        romTables = RomTables.loadFromRom(romData);
        overworldCollision = new OverworldCollision(romTables);
        overworldBushInteraction = new OverworldBushInteraction(romData, romTables);
        linkSpriteSheet = LinkSpriteSheet.loadFromRom(romData);
        SwordSpriteSheet swordSpriteSheet = SwordSpriteSheet.loadFromRom(romData);
        TransientVfxSpriteSheet transientVfxSpriteSheet = TransientVfxSpriteSheet.loadFromRom(romData);
        transientVfxSystem = new TransientVfxSystem(16);
        droppableRupeeSystem = new DroppableRupeeSystem(16,
            () -> ThreadLocalRandom.current().nextInt(0x100));
        cutLeavesEffectRenderer = new CutLeavesEffectRenderer(transientVfxSpriteSheet);
        itemRegistry = new ItemRegistry();
        roomSession = new RoomSession(romData, gpu, new RoomLoader(romData),
            new OverworldTilesetTable(romData), overworldCollision, transientVfxSystem, droppableRupeeSystem,
            Main::selectMusicForLoadedRoom);
        link = new Link(inputState, inputConfig, romTables, overworldCollision,
                        linkSpriteSheet, playerState, itemRegistry, gameplaySoundSink);
        itemRegistry.register(PlayerState.INVENTORY_SWORD, new Sword(romTables, swordSpriteSheet,
            gameplaySoundSink, () -> ThreadLocalRandom.current().nextInt(0x100)));
        itemRegistry.register(PlayerState.INVENTORY_ROCS_FEATHER, new RocsFeather(link));
        equipmentController = new EquipmentController(inputState, inputConfig, playerState, itemRegistry);
        roomTransitionCoordinator = new RoomTransitionCoordinator(
            roomSession, new RoomBoundaryController(), transitionController, scrollController);
    }

    private static void initMusicSystem() {
        GameBoyApu musicApu = new GameBoyApu(48_000);
        MusicCatalog musicCatalog = MusicCatalog.fromRom(romData);
        musicPlayer = new OpenAlMusicPlayer(new MusicDriver(romData, musicApu), musicApu);
        musicPlayer.setLoopEnabled(true);
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

        if (currentScreen == SCREEN_TITLE) {
            if (key == GLFW_KEY_ENTER && action == GLFW_PRESS) {
                startConfiguredGameplay();
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

        if (currentScreen != SCREEN_OVERWORLD || action != GLFW_PRESS) {
            return;
        }

        if (GameplayDialogInput.handleOverworldKeyPress(key, action, inputConfig, dialogController)) {
            overworldDialogInputConsumedThisFrame = true;
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
        if (musicPlayer != null) {
            musicPlayer.update();
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

        if (scrollController.isActive()) {
            scrollController.tick(SCROLL_SPEED);
        }

        // Tick the warp-transition state machine. When it's not IDLE we
        // disable Link movement, edge-scrolling, and animated-tile updates,
        // matching the disassembly's gating on wLinkMotionState ==
        // LINK_MOTION_MAP_FADE_OUT (bank0.asm:888-899).
        transitionController.tick();

        if (currentScreen == SCREEN_OVERWORLD) {
            if (dialogController != null) {
                dialogController.tick();
                routeDialogSounds();
            }
            OverworldDialogBlockers blockers = currentOverworldDialogBlockers();
            boolean dialogBlocksGameplay = blockers.pausesGameplay();

            if (!dialogBlocksGameplay) {
                inventoryController.tick();
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
                && !dialogBlocksGameplay;
            if (linkActive && link != null) {
                equipmentController.dispatchButtonEdges();
                equipmentController.tickEquippedItems();
                link.update();
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

            // Advance the animated BG tiles (waterfalls, weather vanes, etc.).
            // Original gates this behind wRoomTransitionState == 0 and the
            // inventory window not overlapping; mirror both here.
            if (!scrollController.isActive()
                && !transitionController.isInputBlocked()
                && !inventoryController.shouldBlockOverworldInput()
                && !dialogBlocksGameplay) {
                gpu.tickAnimatedTiles(romData);
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

    private static void selectMusicForLoadedRoom(ActiveRoom room) {
        if (gameplayMusicController == null || playerState == null || room == null) {
            return;
        }
        gameplayMusicController.selectAfterTransition(RoomMusicContext.from(room, playerState));
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

    private static void startConfiguredGameplay() {
        currentScreen = SCREEN_OVERWORLD;
        if (currentAppConfig().playIntroStory()) {
            System.err.println("Intro story startup is not implemented yet; spawning at start location");
        }
        loadOverworldScreen();
    }

    private static void startIntroCutscene() {
        currentScreen = SCREEN_CUTSCENE;
        playDirectMusic(introCutsceneMusicTrack());
        gpu.loadIntroSequenceTiles(romData);
        cutsceneManager.startIntro();
    }

    private static void startTitleScreenWithoutIntro() {
        currentScreen = SCREEN_TITLE;
        playDirectMusic(directTitleScreenMusicTrack());
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
        }
    }

    static int introCutsceneMusicTrack() {
        return MusicTrackIds.MUSIC_TITLE_CUTSCENE;
    }

    static int naturalIntroTitleMusicTrack() {
        return MusicTrackIds.MUSIC_TITLE_SCREEN;
    }

    static int directTitleScreenMusicTrack() {
        return MusicTrackIds.MUSIC_TITLE_SCREEN_NO_INTRO;
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
        int startX = ROOM_PIXEL_WIDTH / 2 - Link.SPRITE_SIZE / 2 - 32;
        int startY = ROOM_PIXEL_HEIGHT / 2 - Link.SPRITE_SIZE / 2;
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
            .withRoom(roomSession == null ? null : roomSession.renderSnapshot(), scrollController, transitionController)
            .withLink(link)
            .withTransientVfx(transientVfxSystem, cutLeavesEffectRenderer, GREEN_OBJECTS_SPRITE_PALETTE)
            .withDroppableRupees(droppableRupeeSystem)
            .withInventoryController(inventoryController)
            .withDialogController(dialogController);
    }

    private static RenderScreen currentRenderScreen() {
        switch (currentScreen) {
            case SCREEN_OVERWORLD:
                return RenderScreen.OVERWORLD;
            case SCREEN_CUTSCENE:
                return RenderScreen.CUTSCENE;
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
