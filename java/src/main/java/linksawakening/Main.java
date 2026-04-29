package linksawakening;

import linksawakening.config.AppConfig;
import linksawakening.cutscene.CutsceneManager;
import linksawakening.cutscene.TitleReveal;
import linksawakening.dialog.DialogController;
import linksawakening.entity.Link;
import linksawakening.entity.LinkSpriteSheet;
import linksawakening.equipment.EquipmentController;
import linksawakening.equipment.ItemRegistry;
import linksawakening.equipment.Sword;
import linksawakening.equipment.SwordSpriteSheet;
import linksawakening.gpu.GPU;
import linksawakening.gpu.Framebuffer;
import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
import linksawakening.physics.OverworldCollision;
import linksawakening.render.BackgroundRenderLayer;
import linksawakening.render.CutsceneSpriteRenderLayer;
import linksawakening.render.DialogRenderLayer;
import linksawakening.render.DroppableRupeeRenderLayer;
import linksawakening.render.FrameScene;
import linksawakening.render.InventoryRenderLayer;
import linksawakening.render.LinkRenderLayer;
import linksawakening.render.RenderLayer;
import linksawakening.render.RoomRenderLayer;
import linksawakening.render.Shader;
import linksawakening.render.TransientVfxRenderLayer;
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
import linksawakening.world.LoadedRoom;
import linksawakening.world.OverworldBushInteraction;
import linksawakening.world.OverworldTilesetTable;
import linksawakening.world.RoomLoader;
import linksawakening.world.RoomRenderSnapshot;
import linksawakening.world.ScrollController;
import linksawakening.world.TransitionController;
import linksawakening.world.Warp;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static linksawakening.world.RoomConstants.*;
import static org.lwjgl.glfw.GLFW.*;

public class Main {

    private static final int BG_MAP_WIDTH = 32;
    private static final int BG_MAP_HEIGHT = 32;
    private static final int VIEWPORT_TILE_WIDTH = 20;
    private static final int VIEWPORT_TILE_HEIGHT = 18;

    private static final int MAP_OVERWORLD = 0x00;

    private static final int OBJECT_GROUND_STAIRS = 0xC6;
    private static final int W_TILESET_NO_UPDATE = 0xFF;

    private static long window;
    private static int vbo;
    private static int vao;
    private static Shader shader;
    private static GPU gpu;
    private static byte[] romData;
    private static int[] currentTilemap;
    private static int[] currentAttrmap;
    private static int[][] bgPalettes;
    private static int[][] objPalettes;
    private static int tileTexture;
    private static byte[] indexedDisplayBuffer;
    private static int currentRoomId;
    private static int[] roomObjectsArea;
    private static int[] roomGbcOverlay;
    private static int[] roomObjectRenderValues;
    private static int[] roomTileIds;
    private static int[] roomTileAttrs;
    private static int currentOverworldTilesetId = W_TILESET_NO_UPDATE;

    // Current map category & ID. Mirrors wIsIndoor (0=overworld, 1=indoor) and
    // hMapId from the disassembly.
    private static int currentMapCategory = Warp.CATEGORY_OVERWORLD;
    private static int currentMapId = MAP_OVERWORLD;
    // Warps extracted from the active room, used for door detection.
    private static final List<Warp> currentRoomWarps = new ArrayList<>();
    private static final TransitionController transitionController = new TransitionController();
    // Tile Link stood on after the last warp — prevents re-triggering the
    // mirror warp the instant the fade-in completes if Link's landing
    // position happens to be on a door tile. Cleared once Link leaves it.
    private static int suppressedWarpTile = -1;
    // Whether the current indoor room has an IndoorEntrance ($FD) macro on
    // its south edge. Populated during loadIndoorRoom by scanning for the
    // $C1/$C2 tiles the macro writes. Rooms with one exit to overworld via
    // fade when Link walks off the south edge; rooms without scroll to the
    // adjacent indoor room instead.
    private static boolean indoorHasSouthEntrance = false;

    private static boolean running = true;
    private static int currentScreen = 0;
    private static final int SCREEN_TITLE = 0;
    private static final int SCREEN_OVERWORLD = 1;
    private static final int SCREEN_CUTSCENE = 2;

    // Overworld grid: 16 columns x 16 rows = 256 rooms
    private static final int OVERWORLD_COLUMNS = 16;
    private static final int OVERWORLD_ROWS = 16;

    // Room pixel dimensions
    private static final int ROOM_PIXEL_WIDTH = ROOM_TILE_WIDTH * 8;   // 160
    private static final int ROOM_PIXEL_HEIGHT = ROOM_TILE_HEIGHT * 8; // 128

    // Scroll state
    private static final int SCROLL_NONE = ScrollController.NONE;
    private static final int SCROLL_UP = ScrollController.UP;
    private static final int SCROLL_DOWN = ScrollController.DOWN;
    private static final int SCROLL_LEFT = ScrollController.LEFT;
    private static final int SCROLL_RIGHT = ScrollController.RIGHT;
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
    private static CutsceneManager cutsceneManager;
    private static RomTables romTables;
    private static RoomLoader roomLoader;
    private static OverworldTilesetTable overworldTilesetTable;
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
        }

        initGLFW();
        initOpenGL();

        long nextFrameNs = System.nanoTime();

        while (!glfwWindowShouldClose(window) && running) {
            glfwPollEvents();
            update();
            renderFrame();
            uploadTexture();
            render();

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
        InventoryTilemapLoader tilemapLoader = InventoryTilemapLoader.loadFromRom(romData);
        inventoryMenu = new InventoryMenu(tilemapLoader, playerState);
        inventoryController = new InventoryController(inputState, inputConfig, inventoryMenu);
        dialogController = new DialogController(16);
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
        itemRegistry.register(PlayerState.INVENTORY_SWORD, new Sword(romTables, swordSpriteSheet));
        equipmentController = new EquipmentController(inputState, inputConfig, playerState, itemRegistry);
        overworldTilesetTable = new OverworldTilesetTable(romData);
        link = new Link(inputState, inputConfig, romTables, overworldCollision,
                        linkSpriteSheet, playerState, itemRegistry);
    }

    private static void loadGraphicsData() {
        if (romData == null || romData.length < 0x8000) {
            throw new IllegalStateException("ROM data is not available");
        }

        backgroundSceneLoader = new BackgroundSceneLoader(romData);
        roomLoader = new RoomLoader(romData);
        gpu.loadTitleScreenTiles(romData);
        applyBackgroundScene(backgroundSceneLoader.load(BackgroundSceneCatalog.TITLE));
        indexedDisplayBuffer = new byte[Framebuffer.WIDTH * Framebuffer.HEIGHT * 4];

        if (currentTilemap.length != BG_MAP_WIDTH * BG_MAP_HEIGHT) {
            throw new IllegalStateException("Decoded title tilemap has unexpected size: " + currentTilemap.length);
        }
        if (currentAttrmap.length != BG_MAP_WIDTH * BG_MAP_HEIGHT) {
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
            if (action == GLFW_PRESS && dialogController != null
                && (key == inputConfig.aKey() || key == inputConfig.bKey() || key == GLFW_KEY_ENTER)) {
                dialogController.advance();
            }
            return;
        }

        if (currentScreen != SCREEN_OVERWORLD || action != GLFW_PRESS) {
            return;
        }

        inventoryController.dispatchToggleInput();

        // Debug: F1 dumps Link's surrounding cells with object IDs and
        // physics flags, to diagnose walk-through-wall/tree reports.
        if (shouldRunDebugDump(currentAppConfig(), key, action)) {
            dumpLinkSurroundings();
        }
    }

    static boolean shouldRunDebugDump(AppConfig config, int key, int action) {
        return config.debugEnabled() && key == GLFW_KEY_F1 && action == GLFW_PRESS;
    }

    private static void dumpLinkSurroundings() {
        if (link == null || roomObjectsArea == null) return;
        int lx = link.pixelX();
        int ly = link.pixelY();
        System.out.println("=== Link debug dump ===");
        System.out.println("currentRoomId=" + String.format("%02X", currentRoomId)
            + " mapCat=" + currentMapCategory + " mapId=" + String.format("%02X", currentMapId));
        System.out.println("Link pixel (" + lx + "," + ly + ") tile="
            + String.format("%02X", Warp.packTileLocation(lx, ly)));
        int centerCol = (lx + 8) >> 4;
        int centerRow = (ly + 8) >> 4;
        int tableIdx = (currentMapCategory == Warp.CATEGORY_OVERWORLD)
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

    private static void startScroll(int direction, int linkScreenX, int linkScreenY) {
        RoomRenderSnapshot previousRoom = new RoomRenderSnapshot(roomTileIds, roomTileAttrs, bgPalettes);
        // Calculate the target room
        int nextRoomId = currentRoomId;
        switch (direction) {
            case SCROLL_UP:    nextRoomId -= OVERWORLD_COLUMNS; break;
            case SCROLL_DOWN:  nextRoomId += OVERWORLD_COLUMNS; break;
            case SCROLL_LEFT:  nextRoomId -= 1; break;
            case SCROLL_RIGHT: nextRoomId += 1; break;
        }

        // Load the new room's tileset if needed, then load room data
        int newTilesetId = overworldTilesetTable.tilesetIdForRoom(nextRoomId);
        if (shouldLoadRoomSpecificTileset(nextRoomId, newTilesetId)) {
            gpu.loadRoomSpecificTiles(romData, nextRoomId, newTilesetId);
            currentOverworldTilesetId = newTilesetId;
        }
        loadOverworldRoom(nextRoomId);

        int target = (direction == SCROLL_LEFT || direction == SCROLL_RIGHT)
            ? ROOM_PIXEL_WIDTH : ROOM_PIXEL_HEIGHT;
        scrollController.start(direction, linkScreenX, linkScreenY, previousRoom, target);
    }

    /**
     * Scroll-style transition between adjacent indoor rooms within the same
     * map, mirroring the disassembly's {@code .initiateRoomTransition} +
     * {@code IndoorRoomIncrement} path (room_transition.asm:658). Uses the
     * same overworld scroll animation — both rooms share indoor VRAM
     * tilesets within a building, so graphically the scroll is clean.
     */
    private static void startIndoorScroll(int direction, int linkScreenX, int linkScreenY) {
        RoomRenderSnapshot previousRoom = new RoomRenderSnapshot(roomTileIds, roomTileAttrs, bgPalettes);
        int nextRoomId = currentRoomId;
        switch (direction) {
            case SCROLL_UP:    nextRoomId -= 8; break;
            case SCROLL_DOWN:  nextRoomId += 8; break;
            case SCROLL_LEFT:  nextRoomId -= 1; break;
            case SCROLL_RIGHT: nextRoomId += 1; break;
        }
        nextRoomId &= 0xFF;

        loadIndoorRoom(currentMapId, nextRoomId);

        int target = (direction == SCROLL_LEFT || direction == SCROLL_RIGHT)
            ? ROOM_PIXEL_WIDTH : ROOM_PIXEL_HEIGHT;
        scrollController.start(direction, linkScreenX, linkScreenY, previousRoom, target);
    }

    private static void update() {
        if (currentScreen == SCREEN_CUTSCENE) {
            if (dialogController != null) {
                dialogController.tick();
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
            inventoryController.tick();
            if (transientVfxSystem != null) {
                transientVfxSystem.tick();
            }
            if (droppableRupeeSystem != null && link != null) {
                droppableRupeeSystem.tick(link.pixelX(), link.pixelY(), playerState);
            }

            if (inventoryMenu.isFullyOpen()) {
                inventoryController.dispatchMenuInput();
            }

            boolean linkActive = !scrollController.isActive()
                && !transitionController.isInputBlocked()
                && !inventoryController.shouldBlockOverworldInput();
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
                maybeTriggerWarpTransition();
                maybeTriggerEdgeScroll();
            } else if (scrollController.isActive() && link != null) {
                // Keep Link's walk animation cycling during a room transition.
                link.tickAnimation();
            }

            // Advance the animated BG tiles (waterfalls, weather vanes, etc.).
            // Original gates this behind wRoomTransitionState == 0 and the
            // inventory window not overlapping; mirror both here.
            if (!scrollController.isActive()
                && !transitionController.isInputBlocked()
                && !inventoryController.shouldBlockOverworldInput()) {
                gpu.tickAnimatedTiles(romData);
            }
        }

        // Snapshot input for next-frame edge detection. Run last so every
        // consumer this frame (item dispatch, menu toggle) still sees the
        // correct "just pressed" state.
        inputState.tickEdges();
    }

    private static void maybeCutBushWithSword() {
        if (currentMapCategory != Warp.CATEGORY_OVERWORLD
            || overworldBushInteraction == null
            || roomObjectsArea == null
            || roomTileIds == null
            || roomTileAttrs == null
            || link == null) {
            return;
        }

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
        if (areaIndex < 0 || areaIndex >= roomObjectsArea.length) {
            return;
        }
        int originalObjectId = roomObjectsArea[areaIndex];

        boolean changed = overworldBushInteraction.cutBushAtLocation(
            location,
            currentRoomId,
            true,
            roomObjectsArea,
            roomObjectRenderValues,
            roomGbcOverlay,
            roomTileIds,
            roomTileAttrs
        );
        if (!changed) {
            return;
        }

        if (transientVfxSystem != null) {
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

        if (areaIndex >= 0 && areaIndex < roomObjectsArea.length
            && roomObjectsArea[areaIndex] == OBJECT_GROUND_STAIRS
            && !currentRoomWarps.isEmpty()) {
            Warp warp0 = currentRoomWarps.get(0);
            currentRoomWarps.set(0, warp0.withTileLocation(location));
        }
    }

    /**
     * If Link is standing on a tile matching any active warp, queue a fade
     * transition. Mirrors the disassembly's position→warp lookup at
     * bank0.asm:2798-2826 (LinkMotionMapFadeOutHandler.loop).
     */
    private static void maybeTriggerWarpTransition() {
        if (currentRoomWarps.isEmpty() || transitionController.isActive()) {
            return;
        }

        // Indoor rooms exit by walking off the south edge, not by stepping
        // onto a specific tile. Mirrors CheckPositionForMapTransition
        // (bank2.asm:5966) which fires when hLinkPositionY >= $88 ($88 - 16
        // ≈ Link's top reaching pixel 120 in Java's top-left convention).
        // Top/left/right edges also exit — we reuse the first warp record
        // in the room, matching the dog-house-glitch behaviour where any
        // door-triggered transition picks the lowest-indexed warp.
        if (currentMapCategory != Warp.CATEGORY_OVERWORLD) {
            int x = link.pixelX();
            int y = link.pixelY();
            boolean offBottom = y + Link.SPRITE_SIZE > ROOM_PIXEL_HEIGHT;
            boolean offTop = y < 0;
            boolean offLeft = x < 0;
            boolean offRight = x + Link.SPRITE_SIZE > ROOM_PIXEL_WIDTH;
            if (offBottom && indoorHasSouthEntrance && !currentRoomWarps.isEmpty()) {
                // South edge of a room with an IndoorEntrance macro — the
                // "main front door" of a single-exit building. Fade out via
                // warp[0].
                Warp target = currentRoomWarps.get(0);
                System.out.println("Indoor front-door exit → cat=" + target.category()
                    + " map=" + String.format("%02X", target.destMap())
                    + " room=" + String.format("%02X", target.destRoom())
                    + " at (" + target.destX() + "," + target.destY() + ")");
                transitionController.startFadeOut(() -> applyWarp(target));
                return;
            }
            if (offBottom || offTop || offLeft || offRight) {
                // Scroll-style transition to the adjacent indoor room.
                // Direction encoding matches the overworld edge-scroll
                // constants and the post-scroll landing position is the
                // opposite edge of the new room.
                int scrollDir;
                int destX;
                int destY;
                if (offLeft) {
                    scrollDir = SCROLL_LEFT;
                    destX = ROOM_PIXEL_WIDTH - Link.SPRITE_SIZE;
                    destY = y;
                } else if (offRight) {
                    scrollDir = SCROLL_RIGHT;
                    destX = 0;
                    destY = y;
                } else if (offTop) {
                    scrollDir = SCROLL_UP;
                    destX = x;
                    destY = ROOM_PIXEL_HEIGHT - Link.SPRITE_SIZE;
                } else {
                    scrollDir = SCROLL_DOWN;
                    destX = x;
                    destY = 0;
                }
                link.setPixelPosition(destX, destY);
                startIndoorScroll(scrollDir, x, y);
                suppressedWarpTile = Warp.packTileLocation(destX, destY);
                return;
            }
        }

        int linkTile = Warp.packTileLocation(link.pixelX(), link.pixelY());
        if (linkTile != suppressedWarpTile) {
            suppressedWarpTile = -1;
        }
        if (linkTile == suppressedWarpTile) {
            return;
        }
        for (Warp warp : currentRoomWarps) {
            if (warp.tileLocation() == linkTile) {
                final Warp target = warp;
                System.out.println("Warp trigger: tile=" + String.format("%02X", linkTile)
                    + " → cat=" + target.category()
                    + " map=" + String.format("%02X", target.destMap())
                    + " room=" + String.format("%02X", target.destRoom())
                    + " at (" + target.destX() + "," + target.destY() + ")");
                transitionController.startFadeOut(() -> applyWarp(target));
                return;
            }
        }
    }

    /**
     * Execute a warp: load the destination room and reposition Link. Invoked
     * from the fade-out completion callback — the fade-in phase runs over
     * the freshly loaded room.
     */
    private static void applyWarp(Warp warp) {
        boolean wasIndoor = currentMapCategory != Warp.CATEGORY_OVERWORLD;
        if (warp.category() == Warp.CATEGORY_OVERWORLD) {
            if (wasIndoor) {
                // Indoor → overworld: restore the base overworld VRAM block
                // that an indoor tile load would have clobbered.
                gpu.loadBaseOverworldTiles(romData);
                currentOverworldTilesetId = W_TILESET_NO_UPDATE;
            }
            loadOverworldRoom(warp.destRoom());
            int newTilesetId = overworldTilesetTable.tilesetIdForRoom(warp.destRoom());
            if (shouldLoadRoomSpecificTileset(warp.destRoom(), newTilesetId)) {
                gpu.loadRoomSpecificTiles(romData, warp.destRoom(), newTilesetId);
                currentOverworldTilesetId = newTilesetId;
            }
        } else {
            // Category 1 (indoor) — CATEGORY_SIDESCROLL not implemented yet.
            loadIndoorRoom(warp.destMap(), warp.destRoom());
        }
        int landingX = warp.javaPixelX();
        int landingY = warp.javaPixelY();
        link.setPixelPosition(landingX, landingY);
        suppressedWarpTile = Warp.packTileLocation(landingX, landingY);
        // No explicit grace needed: door-type landing tiles ($C1/$C2,
        // $C5/$C6, etc.) are walkable via OverworldCollision's id
        // override, and if the landing happens to be on a blocked cell
        // anyway Link.tryMoveAxis's self-regulating "currently stuck"
        // check lets him step out.
    }

    private static void maybeTriggerEdgeScroll() {
        int x = link.pixelX();
        int y = link.pixelY();

        // Edge-scrolling is an overworld concept: Link walking off one
        // screen transitions to the adjacent overworld room. Indoor rooms
        // are standalone — walking off an edge should only clamp, never
        // scroll. Without this guard, currentRoomId would be interpreted
        // as an overworld grid index and scroll Link to a random room.
        boolean scrollAllowed = currentMapCategory == Warp.CATEGORY_OVERWORLD;
        int roomCol = currentRoomId % OVERWORLD_COLUMNS;
        int roomRow = currentRoomId / OVERWORLD_COLUMNS;

        if (scrollAllowed && x < 0 && roomCol > 0) {
            beginScrollWithLink(x, y, SCROLL_LEFT,
                                ROOM_PIXEL_WIDTH - Link.SPRITE_SIZE, y);
        } else if (scrollAllowed && x + Link.SPRITE_SIZE > ROOM_PIXEL_WIDTH && roomCol < OVERWORLD_COLUMNS - 1) {
            beginScrollWithLink(x, y, SCROLL_RIGHT, 0, y);
        } else if (scrollAllowed && y < 0 && roomRow > 0) {
            beginScrollWithLink(x, y, SCROLL_UP, x,
                                ROOM_PIXEL_HEIGHT - Link.SPRITE_SIZE);
        } else if (scrollAllowed && y + Link.SPRITE_SIZE > ROOM_PIXEL_HEIGHT && roomRow < OVERWORLD_ROWS - 1) {
            beginScrollWithLink(x, y, SCROLL_DOWN, x, 0);
        } else {
            // Clamp Link within the room if he hit an edge with no room
            // beyond it, or if we're indoors.
            int clampedX = Math.max(0, Math.min(x, ROOM_PIXEL_WIDTH - Link.SPRITE_SIZE));
            int clampedY = Math.max(0, Math.min(y, ROOM_PIXEL_HEIGHT - Link.SPRITE_SIZE));
            if (clampedX != x || clampedY != y) {
                link.setPixelPosition(clampedX, clampedY);
            }
        }
    }

    private static void beginScrollWithLink(int prevX, int prevY, int direction,
                                            int nextX, int nextY) {
        // Capture the screen position where Link crossed the edge so we can
        // keep him rendered at that spot throughout the transition.
        link.setPixelPosition(nextX, nextY);
        startScroll(direction, prevX, prevY);
    }

    // Starting room: Mabe Village Square (ROOM_OW_MABE_VILLAGE_SQUARE = $92
    // from rooms.asm).
    private static final int STARTING_ROOM_ID = 0x92;

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
        gpu.loadIntroSequenceTiles(romData);
        cutsceneManager.startIntro();
    }

    private static void setCutsceneScene(String sceneId) {
        if (BackgroundSceneCatalog.requiresTitleTileset(sceneId)) {
            gpu.loadTitleScreenTiles(romData);
        }
        BackgroundSceneSpec spec = BackgroundSceneCatalog.forCutsceneScene(sceneId);
        if (spec != null) {
            applyBackgroundScene(backgroundSceneLoader.load(spec));
        }
    }

    private static void applyBackgroundScene(BackgroundScene scene) {
        currentTilemap = scene.tilemap();
        currentAttrmap = scene.attrmap();
        bgPalettes = scene.palettes();
        objPalettes = scene.objectPalettes();
    }

    private static void loadOverworldScreen() {
        loadOverworldTiles();

        loadOverworldRoom(StartupCoordinator.gameplayStartRoomId(currentAppConfig()));
        // Center of Mabe Village Square has the statue (solid). Spawn Link two
        // tiles to the left of the room center so he lands on walkable ground.
        int startX = ROOM_PIXEL_WIDTH / 2 - Link.SPRITE_SIZE / 2 - 32;
        int startY = ROOM_PIXEL_HEIGHT / 2 - Link.SPRITE_SIZE / 2;
        link.setPixelPosition(startX, startY);
    }

    private static void loadOverworldTiles() {
        gpu.loadBaseOverworldTiles(romData);

        int startingRoomId = StartupCoordinator.gameplayStartRoomId(currentAppConfig());
        int tilesetId = overworldTilesetTable.tilesetIdForRoom(startingRoomId);
        if (shouldLoadRoomSpecificTileset(startingRoomId, tilesetId)) {
            gpu.loadRoomSpecificTiles(romData, startingRoomId, tilesetId);
            currentOverworldTilesetId = tilesetId;
        }
    }

    private static boolean shouldLoadRoomSpecificTileset(int roomId, int tilesetId) {
        return OverworldTilesetTable.shouldLoadRoomSpecificTileset(
            roomId, tilesetId, currentOverworldTilesetId);
    }

    private static void loadOverworldRoom(int roomId) {
        if (transientVfxSystem != null) {
            transientVfxSystem.clear();
        }
        if (droppableRupeeSystem != null) {
            droppableRupeeSystem.clear();
        }

        LoadedRoom room = roomLoader.loadOverworld(roomId);
        gpu.loadAnimatedTilesGroup(romData, room.animatedTilesGroup());
        applyLoadedRoom(room);

        if (overworldCollision != null) {
            overworldCollision.setRoom(roomObjectsArea);
            overworldCollision.setGbcOverlay(roomGbcOverlay);
            overworldCollision.setPhysicsTable(RomTables.PHYSICS_TABLE_OVERWORLD);
        }
    }

    /**
     * Load an indoor room (house / cave / dungeon). Mirrors the dispatch in
     * bank0.asm:5862: map IDs in [MAP_INDOORS_B_START, MAP_INDOORS_B_END) use
     * IndoorsBRoomPointers (bank $0B), everything else uses IndoorsA (bank
     * $0A). The stream format is identical to overworld (same parser). For
     * the first-iteration MVP the indoor render uses the overworld object
     * tilemap and the Mabe Village palette, so graphics will be visibly
     * wrong — the transition mechanic is the focus of this slice.
     */
    private static void loadIndoorRoom(int mapId, int roomId) {
        if (transientVfxSystem != null) {
            transientVfxSystem.clear();
        }
        if (droppableRupeeSystem != null) {
            droppableRupeeSystem.clear();
        }

        // Load the full indoor VRAM tile set (shared dungeon tiles + per-map
        // walls/floor/items + per-room Indoor(tilesetId)Tiles). Without this
        // the indoor BG would render the overworld tiles still sitting in
        // VRAM from before the warp.
        gpu.loadIndoorTiles(romData, mapId, roomId);
        LoadedRoom room = roomLoader.loadIndoor(mapId, roomId, bgPalettes);
        gpu.loadAnimatedTilesGroup(romData, room.animatedTilesGroup());
        applyLoadedRoom(room);

        if (overworldCollision != null) {
            overworldCollision.setRoom(roomObjectsArea);
            overworldCollision.setGbcOverlay(null);
            overworldCollision.setPhysicsTable(RomTables.PHYSICS_TABLE_INDOORS1);
        }
    }

    private static void applyLoadedRoom(LoadedRoom room) {
        currentRoomId = room.roomId();
        currentMapCategory = room.mapCategory();
        currentMapId = room.mapId();
        roomObjectsArea = room.roomObjectsArea();
        roomGbcOverlay = room.gbcOverlay();
        roomObjectRenderValues = room.renderValues();
        roomTileIds = room.tileIds();
        roomTileAttrs = room.tileAttrs();
        bgPalettes = room.palettes();
        currentRoomWarps.clear();
        currentRoomWarps.addAll(room.warps());
        indoorHasSouthEntrance = room.indoorHasSouthEntrance();
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
        GL.createCapabilities();
        GL11.glViewport(0, 0, Framebuffer.WIDTH * Framebuffer.SCALE, Framebuffer.HEIGHT * Framebuffer.SCALE);

        shader = new Shader();

        float[] vertices = {
            -1.0f, -1.0f, 0.0f, 1.0f,
             1.0f, -1.0f, 1.0f, 1.0f,
             1.0f,  1.0f, 1.0f, 0.0f,
            -1.0f,  1.0f, 0.0f, 0.0f
        };

        FloatBuffer vertexBuffer = BufferUtils.createFloatBuffer(vertices.length);
        vertexBuffer.put(vertices).flip();

        vao = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(vao);

        vbo = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertexBuffer, GL15.GL_STATIC_DRAW);

        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 16, 0);
        GL20.glEnableVertexAttribArray(1);
        GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, 16, 8);

        GL30.glBindVertexArray(0);

        tileTexture = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, tileTexture);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
    }

    private static void renderFrame() {
        currentFrameScene().render(indexedDisplayBuffer, gpu);
    }

    private static FrameScene currentFrameScene() {
        if (bgPalettes == null) {
            return FrameScene.empty();
        }

        List<RenderLayer> layers = new ArrayList<>();
        if (currentScreen == SCREEN_OVERWORLD && roomTileIds != null) {
            addOverworldLayers(layers);
        } else if (currentTilemap != null && currentAttrmap != null) {
            addBackgroundSceneLayers(layers);
        }

        if (dialogController != null) {
            layers.add(new DialogRenderLayer(dialogController));
        }
        return FrameScene.of(layers);
    }

    private static void addOverworldLayers(List<RenderLayer> layers) {
        layers.add(new RoomRenderLayer(currentRoomRenderSnapshot(), scrollController, transitionController));
        if (link != null) {
            layers.add(new LinkRenderLayer(link, scrollController));
        }
        if (transientVfxSystem != null && cutLeavesEffectRenderer != null) {
            layers.add(new TransientVfxRenderLayer(transientVfxSystem, cutLeavesEffectRenderer,
                scrollController, GREEN_OBJECTS_SPRITE_PALETTE));
        }
        if (droppableRupeeSystem != null) {
            layers.add(new DroppableRupeeRenderLayer(droppableRupeeSystem, scrollController));
        }
        if (inventoryController != null) {
            layers.add(new InventoryRenderLayer(inventoryController));
        }
    }

    private static void addBackgroundSceneLayers(List<RenderLayer> layers) {
        int scrollX = cutsceneManager != null ? cutsceneManager.scrollX() : 0;
        int scrollY = cutsceneManager != null ? cutsceneManager.scrollY() : 0;
        int[] tilemap = titleRevealTilemap();
        int[] lineScrollX = cutsceneManager != null
            ? cutsceneManager.lineScrollX(Framebuffer.HEIGHT)
            : null;

        if (lineScrollX != null) {
            layers.add(BackgroundRenderLayer.lineScrolled(tilemap, currentAttrmap, bgPalettes,
                BG_MAP_WIDTH, BG_MAP_HEIGHT, VIEWPORT_TILE_WIDTH, VIEWPORT_TILE_HEIGHT,
                lineScrollX, scrollY));
        } else {
            layers.add(BackgroundRenderLayer.scrolling(tilemap, currentAttrmap, bgPalettes,
                BG_MAP_WIDTH, BG_MAP_HEIGHT, VIEWPORT_TILE_WIDTH, VIEWPORT_TILE_HEIGHT,
                scrollX, scrollY));
        }
        if (cutsceneManager != null && objPalettes != null) {
            layers.add(new CutsceneSpriteRenderLayer(cutsceneManager.sprites(), objPalettes));
        }
    }

    private static RoomRenderSnapshot currentRoomRenderSnapshot() {
        return new RoomRenderSnapshot(roomTileIds, roomTileAttrs, bgPalettes);
    }

    private static int[] titleRevealTilemap() {
        if (cutsceneManager == null || !cutsceneManager.isShowingTitleScene()) {
            return currentTilemap;
        }
        return TitleReveal.maskedTilemap(currentTilemap, cutsceneManager.titleRevealRows(), BG_MAP_WIDTH);
    }

    private static void uploadTexture() {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, tileTexture);

        ByteBuffer texBuf = ByteBuffer.allocateDirect(indexedDisplayBuffer.length);
        texBuf.put(indexedDisplayBuffer).flip();

        GL11.glTexImage2D(
            GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA,
            Framebuffer.WIDTH, Framebuffer.HEIGHT,
            0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, texBuf
        );
    }

    private static void render() {
        glfwMakeContextCurrent(window);

        GL11.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, tileTexture);
        shader.use();
        shader.setUniform1i("uTexture", 0);

        GL30.glBindVertexArray(vao);
        GL11.glDrawArrays(GL11.GL_TRIANGLE_FAN, 0, 4);
        GL30.glBindVertexArray(0);

        glfwSwapBuffers(window);
    }

    private static void cleanup() {
        GL30.glDeleteVertexArrays(vao);
        GL15.glDeleteBuffers(vbo);
        shader.cleanup();
        GL11.glDeleteTextures(tileTexture);
        glfwDestroyWindow(window);
        glfwTerminate();
    }
}
