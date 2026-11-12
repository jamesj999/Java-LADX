package linksawakening.app;

import static linksawakening.world.room.RoomConstants.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.function.Consumer;
import linksawakening.app.host.Host;
import linksawakening.audio.apu.GameBoyApu;
import linksawakening.audio.music.AreaMusicResolver;
import linksawakening.audio.music.FrameAudioPlayer;
import linksawakening.audio.music.FrameMusicPlayer;
import linksawakening.audio.music.GameplayMusicController;
import linksawakening.audio.music.MusicCatalog;
import linksawakening.audio.sfx.SoundEffectCatalog;
import linksawakening.audio.sfx.SoundEffectNamespace;
import linksawakening.audio.sfx.SoundEffectPlayer;
import linksawakening.config.AppConfig;
import linksawakening.cutscene.CutsceneManager;
import linksawakening.dialog.DialogController;
import linksawakening.entity.Link;
import linksawakening.entity.LinkSpriteSheet;
import linksawakening.entity.LinkTunicPalette;
import linksawakening.equipment.Arrow;
import linksawakening.equipment.Bomb;
import linksawakening.equipment.Boomerang;
import linksawakening.equipment.EquipmentController;
import linksawakening.equipment.Hookshot;
import linksawakening.equipment.ItemRegistry;
import linksawakening.equipment.MagicPowder;
import linksawakening.equipment.MagicRod;
import linksawakening.equipment.Ocarina;
import linksawakening.equipment.RocsFeather;
import linksawakening.equipment.Shield;
import linksawakening.equipment.Shovel;
import linksawakening.equipment.Sword;
import linksawakening.equipment.SwordPalette;
import linksawakening.equipment.SwordSpriteSheet;
import linksawakening.gameplay.DialogSoundEffectMap;
import linksawakening.gameplay.DialogSoundSink;
import linksawakening.gameplay.DialogTextLoader;
import linksawakening.gameplay.GameplayDialogInput;
import linksawakening.gameplay.GameplaySoundEffectMap;
import linksawakening.gameplay.GameplaySoundSink;
import linksawakening.gameplay.OverworldDialogInteraction;
import linksawakening.gameplay.PcmSoundOutput;
import linksawakening.gameplay.SignpostDialogTable;
import linksawakening.gpu.Framebuffer;
import linksawakening.gpu.GPU;
import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
import linksawakening.physics.OverworldCollision;
import linksawakening.rom.Rom;
import linksawakening.rom.RomBank;
import linksawakening.rom.RomTables;
import linksawakening.save.SaveRamStore;
import linksawakening.scene.BackgroundScene;
import linksawakening.scene.BackgroundSceneCatalog;
import linksawakening.scene.BackgroundSceneLoader;
import linksawakening.state.Direction;
import linksawakening.state.PlayerState;
import linksawakening.ui.InventoryController;
import linksawakening.ui.InventoryMenu;
import linksawakening.ui.InventoryTilemapLoader;
import linksawakening.vfx.CutLeavesEffectRenderer;
import linksawakening.vfx.TransientVfxSpriteSheet;
import linksawakening.vfx.TransientVfxSystem;
import linksawakening.world.entity.RomRandomByteSource;
import linksawakening.world.object.OverworldBushInteraction;
import linksawakening.world.room.ActiveRoom;
import linksawakening.world.room.ArrowShotResult;
import linksawakening.world.room.DroppableRupeeSystem;
import linksawakening.world.room.OverworldTilesetTable;
import linksawakening.world.room.RoomLoader;
import linksawakening.world.room.RoomSession;
import linksawakening.world.room.RoomSessionConfig;
import linksawakening.world.room.RoomTransitionCoordinator;
import linksawakening.world.room.ShovelStartResult;
import linksawakening.world.transition.RoomBoundaryController;
import linksawakening.world.transition.ScrollController;
import linksawakening.world.transition.TransitionController;

/** Builds a session's ROM-backed collaborators before any frames run. */
public final class SessionBootstrap {
    public record Callbacks(Consumer<String> cutsceneScene, Consumer<ActiveRoom> roomMusic) {}

    public static final class Collaborators {
        private final int[][] bgPalettes;
        private final int[][] objPalettes;
        private final AppConfig appConfig;
        private final GPU gpu;
        private final Rom romData;
        private final int[] currentTilemap;
        private final int[] currentAttrmap;
        private final byte[] indexedDisplayBuffer;
        private final RoomSession roomSession;
        private final RoomTransitionCoordinator roomTransitionCoordinator;
        private final InputConfig inputConfig;
        private final InputState inputState;
        private final PlayerState playerState;
        private final BackgroundSceneLoader backgroundSceneLoader;
        private final InventoryMenu inventoryMenu;
        private final InventoryController inventoryController;
        private final DialogController dialogController;
        private final DialogTextLoader dialogTextLoader;
        private final OverworldDialogInteraction overworldDialogInteraction;
        private final DialogSoundSink dialogSoundSink;
        private final PcmSoundOutput dialogSoundOutput;
        private final GameplaySoundSink gameplaySoundSink;
        private final FrameMusicPlayer musicPlayer;
        private final GameplayMusicController gameplayMusicController;
        private final CutsceneManager cutsceneManager;
        private final SaveRamStore saveRamStore;
        private final RomTables romTables;
        private final OverworldCollision overworldCollision;
        private final OverworldBushInteraction overworldBushInteraction;
        private final LinkSpriteSheet linkSpriteSheet;
        private final TransientVfxSystem transientVfxSystem;
        private final DroppableRupeeSystem droppableRupeeSystem;
        private final CutLeavesEffectRenderer cutLeavesEffectRenderer;
        private final ItemRegistry itemRegistry;
        private final EquipmentController equipmentController;
        private final Link link;
        private final int[] newGameTarinShieldPalette;

        private Collaborators(SessionBootstrap builder) {
            this.bgPalettes = Objects.requireNonNull(builder.bgPalettes, "bgPalettes");
            this.objPalettes = Objects.requireNonNull(builder.objPalettes, "objPalettes");
            this.appConfig = Objects.requireNonNull(builder.config, "appConfig");
            this.gpu = Objects.requireNonNull(builder.gpu, "gpu");
            this.romData = Objects.requireNonNull(builder.romData, "romData");
            this.currentTilemap = Objects.requireNonNull(builder.currentTilemap, "currentTilemap");
            this.currentAttrmap = Objects.requireNonNull(builder.currentAttrmap, "currentAttrmap");
            this.indexedDisplayBuffer = Objects.requireNonNull(builder.indexedDisplayBuffer, "indexedDisplayBuffer");
            this.roomSession = Objects.requireNonNull(builder.roomSession, "roomSession");
            this.roomTransitionCoordinator =
                    Objects.requireNonNull(builder.roomTransitionCoordinator, "roomTransitionCoordinator");
            this.inputConfig = Objects.requireNonNull(builder.inputConfig, "inputConfig");
            this.inputState = Objects.requireNonNull(builder.inputState, "inputState");
            this.playerState = Objects.requireNonNull(builder.playerState, "playerState");
            this.backgroundSceneLoader = Objects.requireNonNull(builder.backgroundSceneLoader, "backgroundSceneLoader");
            this.inventoryMenu = Objects.requireNonNull(builder.inventoryMenu, "inventoryMenu");
            this.inventoryController = Objects.requireNonNull(builder.inventoryController, "inventoryController");
            this.dialogController = Objects.requireNonNull(builder.dialogController, "dialogController");
            this.dialogTextLoader = Objects.requireNonNull(builder.dialogTextLoader, "dialogTextLoader");
            this.overworldDialogInteraction =
                    Objects.requireNonNull(builder.overworldDialogInteraction, "overworldDialogInteraction");
            this.dialogSoundSink = Objects.requireNonNull(builder.dialogSoundSink, "dialogSoundSink");
            this.dialogSoundOutput = Objects.requireNonNull(builder.dialogSoundOutput, "dialogSoundOutput");
            this.gameplaySoundSink = Objects.requireNonNull(builder.gameplaySoundSink, "gameplaySoundSink");
            this.musicPlayer = Objects.requireNonNull(builder.musicPlayer, "musicPlayer");
            this.gameplayMusicController =
                    Objects.requireNonNull(builder.gameplayMusicController, "gameplayMusicController");
            this.cutsceneManager = Objects.requireNonNull(builder.cutsceneManager, "cutsceneManager");
            this.saveRamStore = Objects.requireNonNull(builder.saveRamStore, "saveRamStore");
            this.romTables = Objects.requireNonNull(builder.romTables, "romTables");
            this.overworldCollision = Objects.requireNonNull(builder.overworldCollision, "overworldCollision");
            this.overworldBushInteraction =
                    Objects.requireNonNull(builder.overworldBushInteraction, "overworldBushInteraction");
            this.linkSpriteSheet = Objects.requireNonNull(builder.linkSpriteSheet, "linkSpriteSheet");
            this.transientVfxSystem = Objects.requireNonNull(builder.transientVfxSystem, "transientVfxSystem");
            this.droppableRupeeSystem = Objects.requireNonNull(builder.droppableRupeeSystem, "droppableRupeeSystem");
            this.cutLeavesEffectRenderer =
                    Objects.requireNonNull(builder.cutLeavesEffectRenderer, "cutLeavesEffectRenderer");
            this.itemRegistry = Objects.requireNonNull(builder.itemRegistry, "itemRegistry");
            this.equipmentController = Objects.requireNonNull(builder.equipmentController, "equipmentController");
            this.link = Objects.requireNonNull(builder.link, "link");
            this.newGameTarinShieldPalette =
                    Objects.requireNonNull(builder.newGameTarinShieldPalette, "newGameTarinShieldPalette");
        }

        public int[][] bgPalettes() {
            return bgPalettes;
        }

        public int[][] objPalettes() {
            return objPalettes;
        }

        public AppConfig appConfig() {
            return appConfig;
        }

        public GPU gpu() {
            return gpu;
        }

        public Rom romData() {
            return romData;
        }

        public int[] currentTilemap() {
            return currentTilemap;
        }

        public int[] currentAttrmap() {
            return currentAttrmap;
        }

        public byte[] indexedDisplayBuffer() {
            return indexedDisplayBuffer;
        }

        public RoomSession roomSession() {
            return roomSession;
        }

        public RoomTransitionCoordinator roomTransitionCoordinator() {
            return roomTransitionCoordinator;
        }

        public InputConfig inputConfig() {
            return inputConfig;
        }

        public InputState inputState() {
            return inputState;
        }

        public PlayerState playerState() {
            return playerState;
        }

        public BackgroundSceneLoader backgroundSceneLoader() {
            return backgroundSceneLoader;
        }

        public InventoryMenu inventoryMenu() {
            return inventoryMenu;
        }

        public InventoryController inventoryController() {
            return inventoryController;
        }

        public DialogController dialogController() {
            return dialogController;
        }

        public DialogTextLoader dialogTextLoader() {
            return dialogTextLoader;
        }

        public OverworldDialogInteraction overworldDialogInteraction() {
            return overworldDialogInteraction;
        }

        public DialogSoundSink dialogSoundSink() {
            return dialogSoundSink;
        }

        public PcmSoundOutput dialogSoundOutput() {
            return dialogSoundOutput;
        }

        public GameplaySoundSink gameplaySoundSink() {
            return gameplaySoundSink;
        }

        public FrameMusicPlayer musicPlayer() {
            return musicPlayer;
        }

        public GameplayMusicController gameplayMusicController() {
            return gameplayMusicController;
        }

        public CutsceneManager cutsceneManager() {
            return cutsceneManager;
        }

        public SaveRamStore saveRamStore() {
            return saveRamStore;
        }

        public RomTables romTables() {
            return romTables;
        }

        public OverworldCollision overworldCollision() {
            return overworldCollision;
        }

        public OverworldBushInteraction overworldBushInteraction() {
            return overworldBushInteraction;
        }

        public LinkSpriteSheet linkSpriteSheet() {
            return linkSpriteSheet;
        }

        public TransientVfxSystem transientVfxSystem() {
            return transientVfxSystem;
        }

        public DroppableRupeeSystem droppableRupeeSystem() {
            return droppableRupeeSystem;
        }

        public CutLeavesEffectRenderer cutLeavesEffectRenderer() {
            return cutLeavesEffectRenderer;
        }

        public ItemRegistry itemRegistry() {
            return itemRegistry;
        }

        public EquipmentController equipmentController() {
            return equipmentController;
        }

        public Link link() {
            return link;
        }

        public int[] newGameTarinShieldPalette() {
            return newGameTarinShieldPalette;
        }
    }

    private final Host host;
    private final Callbacks callbacks;
    private final RomRandomByteSource random;
    private final GPU gpu;
    private final TransitionController transitionController;
    private final ScrollController scrollController;
    private static final int GAME_BOY_BG_MAP_TILES = 32 * 32;
    private int[][] bgPalettes;
    private int[][] objPalettes;
    private AppConfig config;
    private Rom romData;
    private int[] currentTilemap;
    private int[] currentAttrmap;
    private byte[] indexedDisplayBuffer;
    private RoomSession roomSession;
    private RoomTransitionCoordinator roomTransitionCoordinator;
    private InputConfig inputConfig;
    private InputState inputState;
    private PlayerState playerState;
    private BackgroundSceneLoader backgroundSceneLoader;
    private InventoryMenu inventoryMenu;
    private InventoryController inventoryController;
    private DialogController dialogController;
    private DialogTextLoader dialogTextLoader;
    private OverworldDialogInteraction overworldDialogInteraction;
    private DialogSoundSink dialogSoundSink;
    private PcmSoundOutput dialogSoundOutput;
    private GameplaySoundSink gameplaySoundSink;
    private FrameMusicPlayer musicPlayer;
    private GameplayMusicController gameplayMusicController;
    private CutsceneManager cutsceneManager;
    private SaveRamStore saveRamStore;
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
    private int[] newGameTarinShieldPalette;

    public SessionBootstrap(
            RomRandomByteSource random,
            GPU gpu,
            TransitionController transition,
            ScrollController scroll,
            Callbacks callbacks,
            Host host) {
        this.host = host;
        this.random = random;
        this.gpu = gpu;
        this.transitionController = transition;
        this.scrollController = scroll;
        this.callbacks = callbacks;
    }

    public Collaborators build(boolean headless) {
        loadROM();
        transitionController.setRomData(romData.unsafeBytes());
        config = headless
                ? AppConfig.parse("{\"itemProfile\":\"NEW_GAME\",\"debugEnabled\":false}")
                : AppConfig.loadFromResources();
        loadGraphicsData();
        initMenuSystem(headless);
        return new Collaborators(this);
    }

    private void loadROM() {
        try (InputStream is = SessionBootstrap.class.getClassLoader().getResourceAsStream("rom/azle.gbc")) {
            if (is == null) {
                throw new IllegalStateException("ROM not found in resources");
            }

            romData = Rom.of(is.readAllBytes());
            DebugLog.log("ROM loaded: " + romData.size() + " bytes");
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load ROM", e);
        }
    }

    private void initMenuSystem() {
        initMenuSystem(false);
    }

    private void initMenuSystem(boolean headless) {
        try {
            saveRamStore = headless ? SaveRamStore.inMemory() : SaveRamStore.open(SaveRamStore.defaultPath());
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to open save image", exception);
        }
        inputConfig = config.inputConfig();
        inputState = new InputState();
        playerState = new PlayerState();
        dialogSoundOutput = host.createPcmOutput();
        initMusicSystem(headless);
        var audio = (FrameAudioPlayer) musicPlayer;
        var catalog = new SoundEffectPlayer(new GameBoyApu(48_000), romData).catalog();
        var dialogEffects = DialogSoundEffectMap.fromCatalog(catalog);
        var gameplayEffects = GameplaySoundEffectMap.fromCatalog(catalog);
        dialogSoundSink = event -> dialogEffects.resolve(event).ifPresent(audio::playSoundEffect);
        gameplaySoundSink = event -> gameplayEffects.resolve(event).ifPresent(audio::playSoundEffect);
        InventoryTilemapLoader tilemapLoader = InventoryTilemapLoader.loadFromRom(romData.unsafeBytes());
        inventoryMenu = new InventoryMenu(tilemapLoader, playerState, gameplaySoundSink, romData.unsafeBytes());
        inventoryController = new InventoryController(inputState, inputConfig, inventoryMenu);
        dialogController = DialogController.forGameplay();
        dialogTextLoader = DialogTextLoader.loadFromRom(romData.unsafeBytes());
        overworldDialogInteraction = new OverworldDialogInteraction(
                SignpostDialogTable.loadFromRom(romData.unsafeBytes()), dialogTextLoader);
        cutsceneManager = new CutsceneManager(dialogController, callbacks.cutsceneScene());
        var introCatalog = SoundEffectCatalog.fromRom(romData);
        cutsceneManager.setIntroNoiseSound(id -> introCatalog
                .find(SoundEffectNamespace.NOISE, id)
                .ifPresent(((FrameAudioPlayer) musicPlayer)::playSoundEffect));
        romTables = RomTables.load(romData);
        newGameTarinShieldPalette =
                RomBank.loadPalettes(romData, new linksawakening.rom.RomAddress(0x05, 0x4C94), 1)[0];
        overworldCollision = new OverworldCollision(romTables);
        overworldBushInteraction = new OverworldBushInteraction(romData.unsafeBytes(), romTables);
        linkSpriteSheet = LinkSpriteSheet.loadFromRom(romData.unsafeBytes());
        SwordSpriteSheet swordSpriteSheet = SwordSpriteSheet.loadFromRom(romData.unsafeBytes());
        SwordPalette swordPalette = SwordPalette.loadFromRom(romData.unsafeBytes());
        TransientVfxSpriteSheet transientVfxSpriteSheet = TransientVfxSpriteSheet.loadFromRom(romData.unsafeBytes());
        transientVfxSystem = new TransientVfxSystem(16);
        droppableRupeeSystem = new DroppableRupeeSystem(16, random);
        cutLeavesEffectRenderer = new CutLeavesEffectRenderer(transientVfxSpriteSheet);
        itemRegistry = new ItemRegistry();
        roomSession = new RoomSession(new RoomSessionConfig(
                romData.unsafeBytes(),
                gpu,
                new RoomLoader(romData.unsafeBytes()),
                new OverworldTilesetTable(romData.unsafeBytes()),
                overworldCollision,
                new RoomSessionConfig.Presentation(
                        transientVfxSystem, droppableRupeeSystem, callbacks.roomMusic()::accept),
                random));
        roomSession.entityState().setColorShellSoundSink(gameplaySoundSink);
        LinkTunicPalette linkTunicPalette = LinkTunicPalette.loadFromRom(romData.unsafeBytes());
        link = new Link(
                inputState,
                inputConfig,
                romTables,
                overworldCollision,
                linkSpriteSheet,
                playerState,
                itemRegistry,
                gameplaySoundSink,
                linkTunicPalette);
        link.setShieldInputAllowed(() -> !GameplayDialogInput.blocksGameplay(dialogController)
                && (roomSession == null
                        || (!roomSession.entityState().powerUpPickupSequenceActive()
                                && !roomSession.rooms().heartPiecePickupActive())));
        itemRegistry.register(
                PlayerState.INVENTORY_OCARINA,
                new Ocarina(playerState, gameplaySoundSink, new Ocarina.PlaybackTarget() {
                    @Override
                    public boolean startOcarina(int countdown, int songFlags, int selectedSong) {
                        return roomSession != null
                                && roomSession.transitions().startOcarina(countdown, songFlags, selectedSong);
                    }

                    @Override
                    public boolean ocarinaPlaying() {
                        return roomSession != null && roomSession.transitions().ocarinaPlaying();
                    }

                    @Override
                    public boolean canStartOcarina() {
                        return link != null
                                && roomSession != null
                                && link.canUseItems()
                                && !link.isAirborne()
                                && !roomSession.playerItems().hookshotActive();
                    }

                    @Override
                    public int ocarinaAnimationState() {
                        return roomSession == null
                                ? -1
                                : roomSession.transitions().ocarinaAnimationState();
                    }
                }));
        itemRegistry.register(
                PlayerState.INVENTORY_SWORD,
                new Sword(romTables, swordSpriteSheet, gameplaySoundSink, random, swordPalette, new Sword.BeamTarget() {
                    @Override
                    public boolean fireSwordBeam() {
                        if (link == null || roomSession == null || playerState == null) {
                            return false;
                        }
                        int romDirection = link.applyRomItemDirectionFromInput();
                        if (playerState.swordLevel() != 2
                                || !playerState.fullHearts()
                                || roomSession.playerItems().activeProjectileCount() != 0) {
                            return false;
                        }
                        boolean spawned = roomSession
                                .playerItems()
                                .fireSwordBeam(link.romEntityX(), link.romEntityY(), link.romEntityZ(), romDirection);
                        if (spawned) {
                            link.startRomItemAttackStep();
                        }
                        return spawned;
                    }
                }));
        itemRegistry.register(
                PlayerState.INVENTORY_MAGIC_ROD,
                new MagicRod(
                        romTables,
                        linkSpriteSheet,
                        linkTunicPalette,
                        gameplaySoundSink,
                        new MagicRod.LaunchTarget() {
                            @Override
                            public void startMagicRodAttackStep() {
                                if (link != null) {
                                    link.startRomMagicRodAttackStep();
                                }
                            }

                            @Override
                            public boolean fireMagicRodFireball() {
                                if (link == null || roomSession == null || playerState == null) {
                                    return false;
                                }
                                int romDirection = link.applyRomItemDirectionFromInput();
                                boolean spawned = roomSession
                                        .playerItems()
                                        .fireMagicRodFireball(
                                                link.romEntityX(),
                                                link.romEntityY(),
                                                link.romEntityZ(),
                                                romDirection,
                                                playerState.activePowerUp()
                                                        == PlayerState.ACTIVE_POWER_UP_PIECE_OF_POWER);
                                return spawned;
                            }

                            @Override
                            public int activeProjectileCount() {
                                return roomSession == null
                                        ? 0
                                        : roomSession.playerItems().activeProjectileCount();
                            }
                        },
                        link::canUseItems));
        itemRegistry.register(
                PlayerState.INVENTORY_MAGIC_POWDER,
                new MagicPowder(
                        playerState,
                        gameplaySoundSink,
                        new MagicPowder.SprinkleTarget() {
                            @Override
                            public boolean sprinkleMagicPowder() {
                                if (link == null || roomSession == null) {
                                    return false;
                                }
                                int romDirection = link.applyRomItemDirectionFromInput();
                                return roomSession
                                        .playerItems()
                                        .sprinkleMagicPowder(
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

                            @Override
                            public int linkPositionZ() {
                                return link.romEntityZ();
                            }

                            @Override
                            public void presentToadstool() {
                                link.presentToadstool();
                            }
                        },
                        link::canUseItems));
        itemRegistry.register(PlayerState.INVENTORY_SHIELD, new Shield(gameplaySoundSink, link::canUseItems));
        itemRegistry.register(
                PlayerState.INVENTORY_SHOVEL,
                new Shovel(
                        gameplaySoundSink,
                        new Shovel.DigTarget() {
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
                                ShovelStartResult result = roomSession
                                        .playerItems()
                                        .startShovel(linkEntityX, linkEntityY, romDirection, link.isAirborne());
                                return new Shovel.StartResult(result.started(), result.poking());
                            }

                            @Override
                            public void advanceShovel(int timer) {
                                if (roomSession != null) {
                                    roomSession
                                            .playerItems()
                                            .advanceShovel(linkEntityX, linkEntityY, romDirection, timer);
                                }
                            }

                            @Override
                            public int shovelAnimationState(int javaDirection, int timer) {
                                return roomSession == null
                                        ? -1
                                        : roomSession.playerItems().shovelAnimationState(javaDirection, timer);
                            }
                        },
                        link::canUseItems));
        itemRegistry.register(PlayerState.INVENTORY_ROCS_FEATHER, new RocsFeather(link));
        itemRegistry.register(
                PlayerState.INVENTORY_BOMBS,
                new Bomb(
                        playerState,
                        gameplaySoundSink,
                        new RoomBombPlacement(link, roomSession.playerItems()),
                        link::canUseItems));
        itemRegistry.register(
                PlayerState.INVENTORY_BOW,
                new Arrow(
                        playerState,
                        gameplaySoundSink,
                        new Arrow.ShootTarget() {
                            private boolean playWhooshForLastShot;

                            @Override
                            public boolean shootArrow() {
                                if (link == null || roomSession == null) {
                                    playWhooshForLastShot = false;
                                    return false;
                                }
                                ArrowShotResult result = roomSession
                                        .playerItems()
                                        .shootArrowResult(
                                                link.romEntityX(),
                                                link.romEntityY(),
                                                link.romEntityZ(),
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
                                return roomSession == null
                                        ? 0
                                        : roomSession.playerItems().activeProjectileCount();
                            }
                        },
                        link::canUseItems));
        itemRegistry.register(PlayerState.INVENTORY_HOOKSHOT, new Hookshot(new Hookshot.LaunchTarget() {
            @Override
            public boolean fireHookshot() {
                if (link == null || roomSession == null) {
                    return false;
                }
                return roomSession
                        .playerItems()
                        .fireHookshot(
                                link.romEntityX(),
                                link.romEntityY(),
                                link.romEntityZ(),
                                Direction.fromJava(link.direction()).romIndex(),
                                link.isAirborne(),
                                link.isRomLinkPushing());
            }

            @Override
            public boolean hookshotActive() {
                return roomSession != null && roomSession.playerItems().hookshotActive();
            }
        }));
        itemRegistry.register(
                PlayerState.INVENTORY_BOOMERANG,
                new Boomerang(
                        new Boomerang.LaunchTarget() {
                            @Override
                            public boolean fireBoomerang() {
                                if (link == null || roomSession == null) {
                                    return false;
                                }
                                boolean spawned = roomSession
                                        .playerItems()
                                        .fireBoomerang(
                                                link.romEntityX(),
                                                link.romEntityY(),
                                                link.romEntityZ(),
                                                link.applyRomItemDirectionFromInput(),
                                                link.romPressedButtonsMask());
                                if (spawned) {
                                    link.startRomItemAttackStep();
                                }
                                return spawned;
                            }

                            @Override
                            public boolean boomerangActive() {
                                return roomSession != null
                                        && roomSession.playerItems().boomerangActive();
                            }
                        },
                        link::canUseItems));
        equipmentController = new EquipmentController(inputState, inputConfig, playerState, itemRegistry);
        roomSession.progress().onMapBoundaryWarp(playerState::clearActivePowerUpForWarp);
        roomTransitionCoordinator = new RoomTransitionCoordinator(
                roomSession, new RoomBoundaryController(), transitionController, scrollController, gameplaySoundSink);
    }

    private void initMusicSystem(boolean headless) {
        MusicCatalog musicCatalog = MusicCatalog.fromRom(romData);
        musicPlayer = new FrameAudioPlayer(romData, 48_000, dialogSoundOutput);
        // ROM background tracks contain their own loop commands. Host-level
        // restarting would also loop finite fanfares such as $1B forever,
        // preventing wActiveMusicIndex-gated acquisition handlers advancing.
        gameplayMusicController =
                new GameplayMusicController(AreaMusicResolver.fromRom(romData), musicCatalog, musicPlayer);
    }

    private void loadGraphicsData() {
        if (romData == null || romData.size() < 0x8000) {
            throw new IllegalStateException("ROM data is not available");
        }

        backgroundSceneLoader = new BackgroundSceneLoader(romData.unsafeBytes());
        gpu.loadTitleScreenTiles(romData);
        BackgroundScene scene = backgroundSceneLoader.load(BackgroundSceneCatalog.TITLE);
        currentTilemap = scene.tilemap();
        currentAttrmap = scene.attrmap();
        bgPalettes = scene.palettes();
        objPalettes = scene.objectPalettes();
        indexedDisplayBuffer = new byte[Framebuffer.WIDTH * Framebuffer.HEIGHT * 4];

        if (currentTilemap.length != GAME_BOY_BG_MAP_TILES) {
            throw new IllegalStateException("Decoded title tilemap has unexpected size: " + currentTilemap.length);
        }
        if (currentAttrmap.length != GAME_BOY_BG_MAP_TILES) {
            throw new IllegalStateException("Decoded title attrmap has unexpected size: " + currentAttrmap.length);
        }
    }
}
