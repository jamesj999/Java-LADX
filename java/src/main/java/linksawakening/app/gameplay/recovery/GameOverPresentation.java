package linksawakening.app.gameplay.recovery;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import linksawakening.app.audio.MusicDirector;
import linksawakening.app.state.GameplaySounds;
import linksawakening.entity.Link;
import linksawakening.gpu.GPU;
import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
import linksawakening.rom.Rom;
import linksawakening.rom.RomAddress;
import linksawakening.rom.RomBank;
import linksawakening.state.PlayerState;
import linksawakening.state.PowerUp;
import linksawakening.ui.FileMenuFrameSnapshot;
import linksawakening.ui.GameOverMenu;
import linksawakening.world.room.RoomSession;

/**
 * $01:420F-42D8. Cursor and interactive choices are deliberately a separate stage. Created for one pass-out
 * sequence and discarded on recovery.
 */
public final class GameOverPresentation {
    private final GameOverActions gameOverOutcome;
    private final GameplaySounds sounds;
    private final GPU gpu;
    private final InputConfig inputConfig;
    private final InputState inputState;
    private final Link playerLink;
    private final MusicDirector musicDirector;
    private final PlayerState playerState;
    private final Rom rom;
    private final RoomSession rooms;

    private final GameOverMenu menu;
    private final FileMenuFrameSnapshot.Builder snapshotBuilder = new FileMenuFrameSnapshot.Builder();
    private boolean cursorVisible;
    private int stage = 1;
    private int countdown = 16;
    private FileMenuFrameSnapshot snapshot;
    private int[] tiles = new int[1024];
    private int[] attributes = new int[1024];
    private final int[][] background;
    private final int[][] objects;

    GameOverPresentation(Inputs inputs) {

        this.gameOverOutcome = Objects.requireNonNull(inputs.gameOverOutcome);
        this.sounds = Objects.requireNonNull(inputs.gameplaySoundSink);
        this.gpu = Objects.requireNonNull(inputs.gpu);
        this.inputConfig = Objects.requireNonNull(inputs.inputConfig);
        this.inputState = Objects.requireNonNull(inputs.inputState);
        this.playerLink = Objects.requireNonNull(inputs.link);
        this.musicDirector = Objects.requireNonNull(inputs.musicDirector);
        this.playerState = Objects.requireNonNull(inputs.playerState);
        this.rom = Objects.requireNonNull(inputs.romData);
        this.rooms = Objects.requireNonNull(inputs.roomSession);
        menu = new GameOverMenu(this.rom);
        var room = this.rooms.rooms().renderSnapshotOrNull();
        background = room.palettes();
        objects = room.entities().spriteSelection().objectPalettes();
        // LoadTileset0F ($01:6CE3): only the 20 visible columns are written.
        for (int y = 0; y < 32; y++)
            for (int x = 0; x < 20; x++) {
                tiles[y * 32 + x] = 0xAE + ((x ^ y) & 1);
            }
        Arrays.fill(attributes, 5); // GAMEPLAY_WORLD path, $01:6D11.
        this.playerState.progression().incrementDeathCount();
        this.rooms.progress().clearGameOverPowerCounter();
        this.playerState.equipmentLevels().setActivePowerUp(PowerUp.NONE.romValue());
        this.playerLink.status().presentGameOverStage(stage, countdown);
        refresh(true);
    }

    public void tick() {
        if (countdown > 0) countdown--;
        switch (stage) {
            case 1 -> {
                this.gpu.loadSaveMenuTiles(this.rom); // $00:2E5E
                stage = 2;
            }
            case 2 -> {
                // TilemapsPointersTable[$0A] points into bank $08; attrmaps are bank $24.
                tiles = RomBank.decodeBackgroundMap(this.rom, new RomAddress(0x08, 0x698F), tiles);
                attributes = RomBank.decodeBackgroundMap(this.rom, new RomAddress(0x24, 0x61E5), attributes);
                stage = 3;
            }
            case 3 -> {
                if (countdown == 0) {
                    stage = 4;
                    this.musicDirector.playDirectMusic(0x03);
                }
            }
            case 4 -> {
                cursorVisible = true;
                int option = menu.tick(this.inputState, this.inputConfig, this.sounds.sink());
                if (option >= 0) {
                    this.gameOverOutcome.finishGameOver(option);
                    return;
                }
            }
            default -> {}
        }
        this.playerLink.status().presentGameOverStage(stage, countdown);
        // Stage 2 restores original palettes via LoadFileMenuBG ($20:6C00).
        refresh(stage < 3);
    }

    public FileMenuFrameSnapshot snapshot() {
        return snapshot;
    }

    private void refresh(boolean white) {
        int[][] palettes = background;
        if (white) {
            palettes = new int[8][4];
            for (int[] palette : palettes) Arrays.fill(palette, 0xF8F8F8);
        }
        snapshot = snapshotBuilder
                .sceneId("game_over")
                .selectedSlot(menu.selected())
                .commandRowArrowShifted(false)
                .creation(false)
                .selectedCharacter(0)
                .namePosition(0)
                .nameBytes(new int[5])
                .tilemap(tiles)
                .attrmap(attributes)
                .bgPalettes(palettes)
                .objectPalettes(objects)
                .sprites(cursorVisible ? List.of(menu.cursor()) : List.of())
                .build();
    }

    /** Named construction inputs, consumed once by the phase constructor. */
    public static final class Inputs {
        private GameOverActions gameOverOutcome;
        private GameplaySounds gameplaySoundSink;
        private GPU gpu;
        private InputConfig inputConfig;
        private InputState inputState;
        private Link link;
        private MusicDirector musicDirector;
        private PlayerState playerState;
        private Rom romData;
        private RoomSession roomSession;

        public Inputs gameOverOutcome(GameOverActions value) {
            this.gameOverOutcome = value;
            return this;
        }

        public Inputs gameplaySoundSink(GameplaySounds value) {
            this.gameplaySoundSink = value;
            return this;
        }

        public Inputs gpu(GPU value) {
            this.gpu = value;
            return this;
        }

        public Inputs inputConfig(InputConfig value) {
            this.inputConfig = value;
            return this;
        }

        public Inputs inputState(InputState value) {
            this.inputState = value;
            return this;
        }

        public Inputs link(Link value) {
            this.link = value;
            return this;
        }

        public Inputs musicDirector(MusicDirector value) {
            this.musicDirector = value;
            return this;
        }

        public Inputs playerState(PlayerState value) {
            this.playerState = value;
            return this;
        }

        public Inputs romData(Rom value) {
            this.romData = value;
            return this;
        }

        public Inputs roomSession(RoomSession value) {
            this.roomSession = value;
            return this;
        }
    }
}
