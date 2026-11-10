package linksawakening.app.gameplay;

import java.util.Objects;
import linksawakening.rom.Rom;
import linksawakening.rom.RomAddress;

/** Bank 1 LinkPassOutHandler's countdown-driven presentation, before screen loading. */
public final class LinkPassOutAnimation {
    // Exact unadjusted bank-1 labels in src/code/game_over.asm.
    private static final RomAddress ANIMATION = new RomAddress(1, 0x41CF);
    private static final RomAddress PALETTE = new RomAddress(1, 0x41E7);
    private final Rom rom;
    private int countdown = 0xBF;

    public LinkPassOutAnimation(Rom rom) {
        this.rom = Objects.requireNonNull(rom);
    }

    /** Trigger frame uses BF; subsequent gameplay frames decrement before dispatch. */
    public void advanceFrame() {
        if (countdown > 0) countdown--;
    }

    public int countdown() { return countdown; }
    public boolean finished() { return countdown == 0; }
    public int animationState() {
        return finished() ? 0xFF : rom.u8(ANIMATION.plus(countdown >>> 3));
    }
    public int paletteEffect() {
        return finished() ? 0 : rom.u8(PALETTE.plus(countdown >>> 3));
    }

    /** Bank 0 $1104: dialog and transitions bypass this trigger. */
    public static boolean shouldStart(int health, int blockItemUsage, int inventoryAppearing,
                                      boolean dialog, boolean transition, boolean alreadyPassingOut) {
        return !dialog && !transition && !alreadyPassingOut
            && (health | blockItemUsage | inventoryAppearing) == 0;
    }
}
