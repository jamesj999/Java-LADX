package linksawakening.app;

import static linksawakening.world.room.RoomConstants.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;
import java.util.function.IntUnaryOperator;
import linksawakening.audio.music.AreaMusicResolver;
import linksawakening.audio.music.FrameMusicPlayer;
import linksawakening.audio.music.GameplayMusicController;
import linksawakening.audio.music.MusicCatalog;
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
import linksawakening.gameplay.DialogSoundSink;
import linksawakening.gameplay.DialogTextLoader;
import linksawakening.gameplay.GameplayDialogInput;
import linksawakening.gameplay.GameplaySoundSink;
import linksawakening.gameplay.OverworldDialogInteraction;
import linksawakening.gameplay.PcmSoundOutput;
import linksawakening.gameplay.SignpostDialogTable;
import linksawakening.gpu.Framebuffer;
import linksawakening.gpu.GPU;
import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
import linksawakening.physics.OverworldCollision;
import linksawakening.rom.RomBank;
import linksawakening.rom.RomTables;
import linksawakening.save.SaveRamStore;
import linksawakening.scene.BackgroundScene;
import linksawakening.scene.BackgroundSceneCatalog;
import linksawakening.scene.BackgroundSceneLoader;
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
import linksawakening.world.room.DroppableRupeeSystem;
import linksawakening.world.room.OverworldTilesetTable;
import linksawakening.world.room.RoomLoader;
import linksawakening.world.room.RoomSession;
import linksawakening.world.room.RoomTransitionCoordinator;
import linksawakening.world.transition.RoomBoundaryController;
import linksawakening.world.transition.ScrollController;
import linksawakening.world.transition.TransitionController;

/** Builds a session's ROM-backed collaborators before any frames run. */
public final class SessionBootstrap {
    public record Callbacks(
            Consumer<String> cutsceneScene, Consumer<ActiveRoom> roomMusic, IntUnaryOperator romDirection) {}

    public record Collaborators(
            int[][] bgPalettes,
            int[][] objPalettes,
            AppConfig appConfig,
            GPU gpu,
            byte[] romData,
            int[] currentTilemap,
            int[] currentAttrmap,
            byte[] indexedDisplayBuffer,
            RoomSession roomSession,
            RoomTransitionCoordinator roomTransitionCoordinator,
            InputConfig inputConfig,
            InputState inputState,
            PlayerState playerState,
            BackgroundSceneLoader backgroundSceneLoader,
            InventoryMenu inventoryMenu,
            InventoryController inventoryController,
            DialogController dialogController,
            DialogTextLoader dialogTextLoader,
            OverworldDialogInteraction overworldDialogInteraction,
            DialogSoundSink dialogSoundSink,
            PcmSoundOutput dialogSoundOutput,
            GameplaySoundSink gameplaySoundSink,
            FrameMusicPlayer musicPlayer,
            GameplayMusicController gameplayMusicController,
            CutsceneManager cutsceneManager,
            SaveRamStore saveRamStore,
            RomTables romTables,
            OverworldCollision overworldCollision,
            OverworldBushInteraction overworldBushInteraction,
            LinkSpriteSheet linkSpriteSheet,
            TransientVfxSystem transientVfxSystem,
            DroppableRupeeSystem droppableRupeeSystem,
            CutLeavesEffectRenderer cutLeavesEffectRenderer,
            ItemRegistry itemRegistry,
            EquipmentController equipmentController,
            Link link,
            int[] newGameTarinShieldPalette) {}

    private final linksawakening.app.host.Host host;
    private final Callbacks callbacks;
    private final RomRandomByteSource random;
    private final GPU gpu;
    private final TransitionController transitionController;
    private final ScrollController scrollController;
    private final int GAME_BOY_BG_MAP_TILES = 32 * 32;
    private int[][] bgPalettes;
    private int[][] objPalettes;
    private AppConfig config;
    private byte[] romData;
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
            linksawakening.app.host.Host host) {
        this.host = host;
        this.random = random;
        this.gpu = gpu;
        this.transitionController = transition;
        this.scrollController = scroll;
        this.callbacks = callbacks;
    }

    public Collaborators build(boolean headless) {
        loadROM();
        transitionController.setRomData(romData);
        config = headless
                ? AppConfig.parse("{\"itemProfile\":\"NEW_GAME\",\"debugEnabled\":false}")
                : AppConfig.loadFromResources();
        loadGraphicsData();
        initMenuSystem(headless);
        return new Collaborators(
                bgPalettes,
                objPalettes,
                config,
                gpu,
                romData,
                currentTilemap,
                currentAttrmap,
                indexedDisplayBuffer,
                roomSession,
                roomTransitionCoordinator,
                inputConfig,
                inputState,
                playerState,
                backgroundSceneLoader,
                inventoryMenu,
                inventoryController,
                dialogController,
                dialogTextLoader,
                overworldDialogInteraction,
                dialogSoundSink,
                dialogSoundOutput,
                gameplaySoundSink,
                musicPlayer,
                gameplayMusicController,
                cutsceneManager,
                saveRamStore,
                romTables,
                overworldCollision,
                overworldBushInteraction,
                linkSpriteSheet,
                transientVfxSystem,
                droppableRupeeSystem,
                cutLeavesEffectRenderer,
                itemRegistry,
                equipmentController,
                link,
                newGameTarinShieldPalette);
    }

    private void loadROM() {
        try (InputStream is = SessionBootstrap.class.getClassLoader().getResourceAsStream("rom/azle.gbc")) {
            if (is == null) {
                throw new IllegalStateException("ROM not found in resources");
            }

            romData = is.readAllBytes();
            System.out.println("ROM loaded: " + romData.length + " bytes");
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
        var audio = (linksawakening.audio.music.FrameAudioPlayer) musicPlayer;
        var catalog = new linksawakening.audio.sfx.SoundEffectPlayer(
                        new linksawakening.audio.apu.GameBoyApu(48_000), romData)
                .catalog();
        var dialogEffects = linksawakening.gameplay.DialogSoundEffectMap.fromCatalog(catalog);
        var gameplayEffects = linksawakening.gameplay.GameplaySoundEffectMap.fromCatalog(catalog);
        dialogSoundSink = event -> dialogEffects.resolve(event).ifPresent(audio::playSoundEffect);
        gameplaySoundSink = event -> gameplayEffects.resolve(event).ifPresent(audio::playSoundEffect);
        InventoryTilemapLoader tilemapLoader = InventoryTilemapLoader.loadFromRom(romData);
        inventoryMenu = new InventoryMenu(tilemapLoader, playerState, gameplaySoundSink, romData);
        inventoryController = new InventoryController(inputState, inputConfig, inventoryMenu);
        dialogController = DialogController.forGameplay();
        dialogTextLoader = DialogTextLoader.loadFromRom(romData);
        overworldDialogInteraction =
                new OverworldDialogInteraction(SignpostDialogTable.loadFromRom(romData), dialogTextLoader);
        cutsceneManager = new CutsceneManager(dialogController, callbacks.cutsceneScene());
        var introCatalog = linksawakening.audio.sfx.SoundEffectCatalog.fromRom(romData);
        cutsceneManager.setIntroNoiseSound(id -> introCatalog
                .find(linksawakening.audio.sfx.SoundEffectNamespace.NOISE, id)
                .ifPresent(((linksawakening.audio.music.FrameAudioPlayer) musicPlayer)::playSoundEffect));
        romTables = RomTables.loadFromRom(romData);
        newGameTarinShieldPalette = RomBank.loadPalettes(romData, 0x05, 0x4C94, 1)[0];
        overworldCollision = new OverworldCollision(romTables);
        overworldBushInteraction = new OverworldBushInteraction(romData, romTables);
        linkSpriteSheet = LinkSpriteSheet.loadFromRom(romData);
        SwordSpriteSheet swordSpriteSheet = SwordSpriteSheet.loadFromRom(romData);
        SwordPalette swordPalette = SwordPalette.loadFromRom(romData);
        TransientVfxSpriteSheet transientVfxSpriteSheet = TransientVfxSpriteSheet.loadFromRom(romData);
        transientVfxSystem = new TransientVfxSystem(16);
        droppableRupeeSystem = new DroppableRupeeSystem(16, random);
        cutLeavesEffectRenderer = new CutLeavesEffectRenderer(transientVfxSpriteSheet);
        itemRegistry = new ItemRegistry();
        roomSession = new RoomSession(
                romData,
                gpu,
                new RoomLoader(romData),
                new OverworldTilesetTable(romData),
                overworldCollision,
                transientVfxSystem,
                droppableRupeeSystem,
                callbacks.roomMusic()::accept,
                random);
        roomSession.setColorShellSoundSink(gameplaySoundSink);
        LinkTunicPalette linkTunicPalette = LinkTunicPalette.loadFromRom(romData);
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
                        || (!roomSession.powerUpPickupSequenceActive() && !roomSession.heartPiecePickupActive())));
        itemRegistry.register(
                PlayerState.INVENTORY_OCARINA,
                new Ocarina(playerState, gameplaySoundSink, new Ocarina.PlaybackTarget() {
                    @Override
                    public boolean startOcarina(int countdown, int songFlags, int selectedSong) {
                        return roomSession != null && roomSession.startOcarina(countdown, songFlags, selectedSong);
                    }

                    @Override
                    public boolean ocarinaPlaying() {
                        return roomSession != null && roomSession.ocarinaPlaying();
                    }

                    @Override
                    public boolean canStartOcarina() {
                        return link != null
                                && roomSession != null
                                && link.canUseItems()
                                && !link.isAirborne()
                                && !roomSession.hookshotActive();
                    }

                    @Override
                    public int ocarinaAnimationState() {
                        return roomSession == null ? -1 : roomSession.ocarinaAnimationState();
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
                                boolean spawned = roomSession.fireMagicRodFireball(
                                        link.romEntityX(),
                                        link.romEntityY(),
                                        link.romEntityZ(),
                                        romDirection,
                                        playerState.activePowerUp() == PlayerState.ACTIVE_POWER_UP_PIECE_OF_POWER);
                                return spawned;
                            }

                            @Override
                            public int activeProjectileCount() {
                                return roomSession == null ? 0 : roomSession.activeProjectileCount();
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
                                return roomSession == null
                                        ? -1
                                        : roomSession.shovelAnimationState(javaDirection, timer);
                            }
                        },
                        link::canUseItems));
        itemRegistry.register(PlayerState.INVENTORY_ROCS_FEATHER, new RocsFeather(link));
        itemRegistry.register(
                PlayerState.INVENTORY_BOMBS,
                new Bomb(
                        playerState,
                        gameplaySoundSink,
                        new Bomb.PlacementTarget() {
                            private boolean playBumpForLastPlacement;

                            @Override
                            public boolean placeBomb() {
                                if (link == null || roomSession == null) {
                                    playBumpForLastPlacement = false;
                                    return false;
                                }
                                boolean placed = roomSession.placeBomb(
                                        link.romEntityX(),
                                        link.romEntityY(),
                                        link.romEntityZ(),
                                        callbacks.romDirection().applyAsInt(link.direction()));
                                playBumpForLastPlacement = placed && roomSession.lastBombPlacementPlayedBump();
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
                        },
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
                                RoomSession.ArrowShotResult result = roomSession.shootArrowResult(
                                        link.romEntityX(),
                                        link.romEntityY(),
                                        link.romEntityZ(),
                                        link.applyRomItemDirectionFromInput(),
                                        playerState.activePowerUp() == PlayerState.ACTIVE_POWER_UP_PIECE_OF_POWER);
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
                        },
                        link::canUseItems));
        itemRegistry.register(PlayerState.INVENTORY_HOOKSHOT, new Hookshot(new Hookshot.LaunchTarget() {
            @Override
            public boolean fireHookshot() {
                if (link == null || roomSession == null) {
                    return false;
                }
                return roomSession.fireHookshot(
                        link.romEntityX(),
                        link.romEntityY(),
                        link.romEntityZ(),
                        callbacks.romDirection().applyAsInt(link.direction()),
                        link.isAirborne(),
                        link.isRomLinkPushing());
            }

            @Override
            public boolean hookshotActive() {
                return roomSession != null && roomSession.hookshotActive();
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
                                boolean spawned = roomSession.fireBoomerang(
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
                                return roomSession != null && roomSession.boomerangActive();
                            }
                        },
                        link::canUseItems));
        equipmentController = new EquipmentController(inputState, inputConfig, playerState, itemRegistry);
        roomSession.onMapBoundaryWarp(playerState::clearActivePowerUpForWarp);
        roomTransitionCoordinator = new RoomTransitionCoordinator(
                roomSession, new RoomBoundaryController(), transitionController, scrollController, gameplaySoundSink);
    }

    private void initMusicSystem(boolean headless) {
        MusicCatalog musicCatalog = MusicCatalog.fromRom(romData);
        musicPlayer = new linksawakening.audio.music.FrameAudioPlayer(romData, 48_000, dialogSoundOutput);
        // ROM background tracks contain their own loop commands. Host-level
        // restarting would also loop finite fanfares such as $1B forever,
        // preventing wActiveMusicIndex-gated acquisition handlers advancing.
        gameplayMusicController =
                new GameplayMusicController(AreaMusicResolver.fromRom(romData), musicCatalog, musicPlayer);
    }

    private void loadGraphicsData() {
        if (romData == null || romData.length < 0x8000) {
            throw new IllegalStateException("ROM data is not available");
        }

        backgroundSceneLoader = new BackgroundSceneLoader(romData);
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
