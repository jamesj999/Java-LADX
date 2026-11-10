package linksawakening.ui;

import linksawakening.cutscene.IntroSprite;
import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.gameplay.GameplaySoundSink;
import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
import linksawakening.rom.Rom;
import linksawakening.rom.RomAddress;

/** $01:42D9-4370; options are save/continue, save/quit, and continue. */
public final class GameOverMenu {
    private final Rom rom;
    private int selected;
    public GameOverMenu(Rom rom) { this.rom = rom; }
    public int selected() { return selected; }
    /** Returns -1 unless A, B or Start has a fresh press. Down precedes Up in the ROM. */
    public int tick(InputState input, InputConfig config, GameplaySoundSink sounds) {
        boolean down = input.wasPressed(config.downKey());
        boolean up = input.wasPressed(config.upKey());
        if (down || up) sounds.play(GameplaySoundEvent.MENU_MOVE);
        if (down) selected = (selected + 1) % 3;
        if (up) selected = (selected + 2) % 3;
        return input.wasPressed(config.aKey()) || input.wasPressed(config.bKey())
            || input.wasPressed(config.menuOpenKey()) ? selected : -1;
    }
    public IntroSprite cursor() {
        return new IntroSprite(0xBE, 0x24 - 8,
            rom.u8(new RomAddress(1, 0x4336).plus(selected)) - 16, 0, false, false, 16);
    }
}
