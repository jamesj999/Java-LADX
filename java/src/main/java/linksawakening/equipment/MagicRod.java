package linksawakening.equipment;

import linksawakening.entity.Link;
import linksawakening.entity.LinkSpriteSheet;
import linksawakening.entity.LinkTunicPalette;
import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.gameplay.GameplaySoundSink;
import linksawakening.gpu.Framebuffer;
import linksawakening.gpu.Tile;
import linksawakening.rom.RomTables;

import java.util.Objects;
import java.util.function.BooleanSupplier;

/** Port of {@code UseMagicRod} and {@code label_002_5310}. */
public final class MagicRod implements EquippedItem {
    public static final int ATTACK_STEP_MAGIC_ROD = 0x8E;

    private static final int ATTACK_STEP_DURATION_MASK = 0x7F;
    // EquipmentController runs before Link's func_002_434A countdown tick.
    // The ROM spawns after that tick reaches $0C, so the host observes $0D
    // immediately before the equivalent decrement.
    private static final int FIRE_CHECK_COUNTDOWN = 0x0D;
    private static final int MAX_ACTIVE_PROJECTILES = 0x02;
    private static final int FORWARD_SWING_THRESHOLD = 0x08;
    private static final int ATTR_FLIP_X = 0x20;
    private static final int ATTR_FLIP_Y = 0x40;

    public interface LaunchTarget {
        void startMagicRodAttackStep();

        boolean fireMagicRodFireball();

        int activeProjectileCount();
    }

    private final GameplaySoundSink soundSink;
    private final LaunchTarget target;
    private final BooleanSupplier itemUseAllowed;
    private final RomTables romTables;
    private final LinkSpriteSheet spriteSheet;
    private final LinkTunicPalette tunicPalette;
    private int attackStepCountdown;

    public MagicRod(GameplaySoundSink soundSink, LaunchTarget target) {
        this(soundSink, target, () -> true, null, null, null);
    }

    public MagicRod(GameplaySoundSink soundSink, LaunchTarget target,
                    BooleanSupplier itemUseAllowed) {
        this(soundSink, target, itemUseAllowed, null, null, null);
    }

    public MagicRod(RomTables romTables, LinkSpriteSheet spriteSheet,
                    LinkTunicPalette tunicPalette, GameplaySoundSink soundSink,
                    LaunchTarget target, BooleanSupplier itemUseAllowed) {
        this(soundSink, target, itemUseAllowed, romTables, spriteSheet, tunicPalette);
    }

    private MagicRod(GameplaySoundSink soundSink, LaunchTarget target,
                     BooleanSupplier itemUseAllowed, RomTables romTables,
                     LinkSpriteSheet spriteSheet, LinkTunicPalette tunicPalette) {
        this.soundSink = Objects.requireNonNull(soundSink, "soundSink");
        this.target = Objects.requireNonNull(target, "target");
        this.itemUseAllowed = Objects.requireNonNull(itemUseAllowed, "itemUseAllowed");
        if ((romTables == null) != (spriteSheet == null)
            || (romTables == null) != (tunicPalette == null)) {
            throw new IllegalArgumentException("Magic Rod rendering dependencies must be complete");
        }
        this.romTables = romTables;
        this.spriteSheet = spriteSheet;
        this.tunicPalette = tunicPalette;
    }

    @Override
    public void onPress() {
        if (!itemUseAllowed.getAsBoolean() || attackStepCountdown != 0
            || target.activeProjectileCount() >= MAX_ACTIVE_PROJECTILES) {
            return;
        }
        attackStepCountdown = ATTACK_STEP_MAGIC_ROD;
        target.startMagicRodAttackStep();
    }

    @Override
    public void tick(boolean buttonHeld, int frameCounter) {
        if (attackStepCountdown == 0) {
            return;
        }

        if ((attackStepCountdown & ATTACK_STEP_DURATION_MASK) == FIRE_CHECK_COUNTDOWN) {
            if (target.fireMagicRodFireball()) {
                soundSink.play(GameplaySoundEvent.MAGIC_ROD);
            }
        }

        if ((attackStepCountdown & ATTACK_STEP_DURATION_MASK) == 0) {
            attackStepCountdown = 0;
        } else {
            attackStepCountdown = (attackStepCountdown - 1) & 0xFF;
        }
    }

    @Override
    public boolean blocksMotion() {
        return attackStepCountdown != 0;
    }

    @Override
    public boolean locksFacing() {
        return attackStepCountdown != 0;
    }

    @Override
    public void render(byte[] displayBuffer, int linkPixelX, int linkPixelY,
                       int direction, int offsetX, int offsetY) {
        if (attackStepCountdown == 0 || romTables == null) {
            return;
        }

        int rowOffset = (attackStepCountdown & ATTACK_STEP_DURATION_MASK)
            < FORWARD_SWING_THRESHOLD ? 0 : 4;
        int romDirection = toRomDirection(direction);
        int tableIndex = rowOffset + romDirection;
        int x = linkPixelX + romTables.magicRodXOffset(tableIndex) + offsetX;
        int y = linkPixelY + romTables.magicRodYOffset(tableIndex) + offsetY;
        int tile0 = romTables.magicRodTile(tableIndex, 0);
        int tile1 = romTables.magicRodTile(tableIndex, 1);
        int attr0 = romTables.magicRodAttribute(tableIndex, 0);
        int attr1 = romTables.magicRodAttribute(tableIndex, 1);
        if (tile0 != 0xFF) {
            drawTile(displayBuffer, spriteSheet.tile(tile0), x, y, attr0,
                tunicPalette.forObjectPalette(attr0 & 0x07));
        }
        if (tile1 != 0xFF) {
            drawTile(displayBuffer, spriteSheet.tile(tile1), x + 8, y, attr1,
                tunicPalette.forObjectPalette(attr1 & 0x07));
        }
    }

    int attackStepCountdownForTest() {
        return attackStepCountdown;
    }

    private static int toRomDirection(int javaDirection) {
        return switch (javaDirection) {
            case Link.DIRECTION_RIGHT -> 0;
            case Link.DIRECTION_LEFT -> 1;
            case Link.DIRECTION_UP -> 2;
            case Link.DIRECTION_DOWN -> 3;
            default -> throw new IllegalArgumentException("Link direction out of range: "
                + javaDirection);
        };
    }

    private static void drawTile(byte[] buffer, Tile tile, int screenX, int screenY,
                                 int attributes, int[] palette) {
        if (tile == null) {
            return;
        }
        boolean flipX = (attributes & ATTR_FLIP_X) != 0;
        boolean flipY = (attributes & ATTR_FLIP_Y) != 0;
        for (int ty = 0; ty < 8; ty++) {
            for (int tx = 0; tx < 8; tx++) {
                int px = screenX + tx;
                int py = screenY + ty;
                if (px < 0 || px >= Framebuffer.WIDTH || py < 0 || py >= Framebuffer.HEIGHT) {
                    continue;
                }
                int colorIndex = tile.getPixel(flipX ? 7 - tx : tx, flipY ? 7 - ty : ty);
                if (colorIndex == 0) {
                    continue;
                }
                int color = palette[colorIndex];
                int bufferOffset = (py * Framebuffer.WIDTH + px) * 4;
                buffer[bufferOffset] = (byte) ((color >> 16) & 0xFF);
                buffer[bufferOffset + 1] = (byte) ((color >> 8) & 0xFF);
                buffer[bufferOffset + 2] = (byte) (color & 0xFF);
                buffer[bufferOffset + 3] = (byte) 0xFF;
            }
        }
    }
}
