package linksawakening.ui;

import linksawakening.cutscene.IntroSprite;
import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.gameplay.GameplaySoundSink;
import linksawakening.rom.RomBank;
import linksawakening.scene.BackgroundScene;

import java.util.List;

/** LinkPassOut and the English DX game-over menu. */
public final class GameOverSequence {
    private static final int PASS_OUT_FRAMES = 0xBF;
    // Stage 1 waits for tiles, then stage 2 loads the menu before the input countdown expires.
    private static final int MENU_VISIBLE_FRAME = PASS_OUT_FRAMES + 11;
    private static final int INTERACTIVE_FRAME = PASS_OUT_FRAMES + 24;
    private final byte[] rom;
    private final BackgroundScene menu;
    private int elapsed;
    private int selection;

    public enum Choice { NONE, SAVE_AND_CONTINUE, SAVE_AND_QUIT, CONTINUE }

    public GameOverSequence(byte[] rom, BackgroundScene menu) {
        this.rom = rom;
        this.menu = menu;
    }

    public boolean passingOut() { return elapsed < PASS_OUT_FRAMES; }
    public boolean menuVisible() { return elapsed >= MENU_VISIBLE_FRAME; }
    public boolean cursorVisible() { return elapsed > INTERACTIVE_FRAME; }
    public boolean musicStarts() { return elapsed == INTERACTIVE_FRAME; }
    public boolean countDeath() { return elapsed == PASS_OUT_FRAMES; }
    public int animationState() {
        int index = (PASS_OUT_FRAMES - elapsed) >>> 3;
        return Byte.toUnsignedInt(rom[RomBank.romOffset(1, 0x41CF) + index]);
    }

    public void fadeRoom(byte[] buffer) {
        int steps = Math.min(31, elapsed);
        for (int i = 0; i < buffer.length; i += 4) {
            for (int channel = 0; channel < 3; channel++) {
                int color = Math.min(31, (Byte.toUnsignedInt(buffer[i + channel]) >>> 3) + steps);
                buffer[i + channel] = (byte) ((color << 3) | (color >>> 2));
            }
        }
    }

    public Choice tick(InputState input, InputConfig keys, GameplaySoundSink sounds) {
        if (elapsed <= INTERACTIVE_FRAME) elapsed++;
        if (!cursorVisible()) return Choice.NONE;
        boolean up = input.wasPressed(keys.upKey());
        boolean down = input.wasPressed(keys.downKey());
        if (up || down) {
            selection = Math.floorMod(selection + (down ? 1 : 0) - (up ? 1 : 0), 3);
            sounds.play(GameplaySoundEvent.MENU_MOVE);
        }
        if (input.wasPressed(keys.aKey()) || input.wasPressed(keys.bKey()) || input.wasPressed(keys.menuOpenKey())) {
            return switch (selection) {
                case 0 -> Choice.SAVE_AND_CONTINUE;
                case 1 -> Choice.SAVE_AND_QUIT;
                default -> Choice.CONTINUE;
            };
        }
        return Choice.NONE;
    }

    public FileMenuFrameSnapshot snapshot() {
        int y = Byte.toUnsignedInt(rom[RomBank.romOffset(1, 0x4336) + selection]) - 16;
        return new FileMenuFrameSnapshot("game_over", selection, false, false, 0, 0, new int[5],
            menu.tilemap(), menu.attrmap(), menu.palettes(), menu.objectPalettes(),
            List.of(new IntroSprite(0xBE, 28, y, 0, false, false, 16)));
    }
}
