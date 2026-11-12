package linksawakening.app.gameplay;

import java.util.Arrays;
import java.util.List;
import linksawakening.rom.Rom;
import linksawakening.rom.RomBank;
import linksawakening.state.PlayerState;
import linksawakening.ui.FileMenuFrameSnapshot;
import linksawakening.ui.GameOverMenu;

/** $01:420F-42D8. Cursor and interactive choices are deliberately a separate stage. */
final class GameOverPresentation {
    private final GameOverMenu menu;
    private boolean cursorVisible;
    private int stage = 1;
    private int countdown = 16;
    private FileMenuFrameSnapshot snapshot;
    private int[] tiles = new int[1024];
    private int[] attributes = new int[1024];
    private final int[][] background;
    private final int[][] objects;

    GameOverPresentation(GameplaySimulation context) {
        menu = new GameOverMenu(new Rom(context.romData()));
        var room = context.roomSession().renderSnapshot();
        background = room.palettes();
        objects = room.entities().spriteSelection().objectPalettes();
        // LoadTileset0F ($01:6CE3): only the 20 visible columns are written.
        for (int y = 0; y < 32; y++)
            for (int x = 0; x < 20; x++) {
                tiles[y * 32 + x] = 0xAE + ((x ^ y) & 1);
            }
        Arrays.fill(attributes, 5); // GAMEPLAY_WORLD path, $01:6D11.
        context.playerState().incrementDeathCount();
        context.roomSession().clearGameOverPowerCounter();
        context.playerState().setActivePowerUp(PlayerState.ACTIVE_POWER_UP_NONE);
        context.link().presentGameOverStage(stage, countdown);
        refresh(true);
    }

    void tick(GameplaySimulation context) {
        if (countdown > 0) countdown--;
        switch (stage) {
            case 1 -> {
                context.gpu().loadSaveMenuTiles(context.romData()); // $00:2E5E
                stage = 2;
            }
            case 2 -> {
                // TilemapsPointersTable[$0A] points into bank $08; attrmaps are bank $24.
                tiles = RomBank.decodeBackgroundMap(context.romData(), 0x08, 0x698F, tiles);
                attributes = RomBank.decodeBackgroundMap(context.romData(), 0x24, 0x61E5, attributes);
                stage = 3;
            }
            case 3 -> {
                if (countdown == 0) {
                    stage = 4;
                    context.musicDirector().playDirectMusic(0x03);
                }
            }
            case 4 -> {
                cursorVisible = true;
                int option = menu.tick(context.inputState(), context.inputConfig(), context.gameplaySoundSink());
                if (option >= 0) {
                    context.gameOverOutcome().accept(option);
                    return;
                }
            }
            default -> {}
        }
        context.link().presentGameOverStage(stage, countdown);
        // Stage 2 restores original palettes via LoadFileMenuBG ($20:6C00).
        refresh(stage < 3);
    }

    FileMenuFrameSnapshot snapshot() {
        return snapshot;
    }

    private void refresh(boolean white) {
        int[][] palettes = background;
        if (white) {
            palettes = new int[8][4];
            for (int[] palette : palettes) Arrays.fill(palette, 0xF8F8F8);
        }
        snapshot = new FileMenuFrameSnapshot(
                "game_over",
                menu.selected(),
                false,
                false,
                0,
                0,
                new int[5],
                tiles,
                attributes,
                palettes,
                objects,
                cursorVisible ? List.of(menu.cursor()) : List.of());
    }
}
