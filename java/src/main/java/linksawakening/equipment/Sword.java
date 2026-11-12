package linksawakening.equipment;

import java.util.function.IntSupplier;
import linksawakening.gameplay.GameplaySoundSink;
import linksawakening.rom.RomTables;

/**
 * Port of the sword swing + charge logic from the LADX disassembly (see
 * {@code check_items_to_use.asm}, {@code bank2.asm:UpdateLinkAnimation},
 * and {@code bank0.asm:ApplyLinkMotionState}).
 *
 * <p>The swing runs NONE -> DRAW -> SWING_START -> SWING_MIDDLE, and if the
 * button is still held at the end, settles into HOLDING where a
 * charge meter builds up (matches {@code wSwordCharge} /
 * {@code MAX_SWORD_CHARGE = $28}). At max charge the blade flashes between
 * its normal palette and a brighter "charged" palette every 4 frames,
 * mirroring the attribute-bit-4 toggle gated by bit 2 of the frame counter
 * in {@code bank0.asm:17BF-17C5} (and the GBC palette-4 branch in
 * {@code func_020_4AB3}).
 *
 * <p>The blade position, per-frame orientation, and sprite-tile selection
 * all come from ROM tables loaded into {@link RomTables}: the same
 * {@code LinkDirectionToSwordDirection}, {@code LinkDirectionTo_wC13A/139/13B},
 * and {@code Data_020_4A93/4AA3} the original uses.
 */
public final class Sword extends SwordChargeMotion implements EquipmentItem {

    /** Optional bridge to the room-owned ROM sword-beam entity. */
    public interface BeamTarget {
        boolean fireSwordBeam();
    }

    /** The ROM's wC140..wC143 rectangle for enemy collision. */
    public record CollisionBox(boolean active, int x, int width, int y, int height)
            implements EquipmentItem.CollisionBox {
        public static CollisionBox inactive() {
            return new CollisionBox(false, 0, 0, 0, 0);
        }
    }

    public static final int STATE_NONE = 0;
    public static final int STATE_DRAW = 1;
    public static final int STATE_SWING_START = 2;
    public static final int STATE_SWING_MIDDLE = 3;
    public static final int STATE_SWING_END = 4;
    public static final int STATE_HOLDING = 5;
    public static final int MAX_CHARGE = 40;

    public Sword(RomTables romTables, SwordSpriteSheet spriteSheet) {
        super(romTables, spriteSheet);
    }

    public Sword(
            RomTables romTables,
            SwordSpriteSheet spriteSheet,
            GameplaySoundSink soundSink,
            IntSupplier randomByteSupplier) {
        super(romTables, spriteSheet, soundSink, randomByteSupplier);
    }

    public Sword(
            RomTables romTables,
            SwordSpriteSheet spriteSheet,
            GameplaySoundSink soundSink,
            IntSupplier randomByteSupplier,
            SwordPalette swordPalette) {
        super(romTables, spriteSheet, soundSink, randomByteSupplier, swordPalette);
    }

    public Sword(
            RomTables romTables,
            SwordSpriteSheet spriteSheet,
            GameplaySoundSink soundSink,
            IntSupplier randomByteSupplier,
            SwordPalette swordPalette,
            BeamTarget beamTarget) {
        super(romTables, spriteSheet, soundSink, randomByteSupplier, swordPalette, beamTarget);
    }
}
