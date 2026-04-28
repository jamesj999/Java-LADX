package linksawakening.equipment;

import linksawakening.entity.Link;
import linksawakening.gpu.Framebuffer;
import linksawakening.gpu.Tile;
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
public final class Sword implements EquippedItem {

    public static final int STATE_NONE = 0;
    public static final int STATE_DRAW = 1;
    public static final int STATE_SWING_START = 2;
    public static final int STATE_SWING_MIDDLE = 3;
    public static final int STATE_SWING_END = 4;
    public static final int STATE_HOLDING = 5;

    private static final int DRAW_FRAMES = 4;
    private static final int SWING_START_FRAMES = 3;
    private static final int SWING_MIDDLE_FRAMES = 8;
    private static final int SPIN_ATTACK_FRAMES = 0x20;
    public static final int MAX_CHARGE = 40; // MAX_SWORD_CHARGE ($28) in the ROM
    private static final int ATTR_FLIP_X = 0x20;
    private static final int ATTR_FLIP_Y = 0x40;
    // bank2.asm:46C9-46E8. Indexed as (base hLinkDirection << 3) + spin sector.
    private static final int[][] SPIN_ANIMATION_STATE_BY_BASE_DIRECTION = {
        { STATE_SWING_MIDDLE, STATE_SWING_START, STATE_SWING_MIDDLE, STATE_SWING_START,
          STATE_SWING_MIDDLE, STATE_SWING_START, STATE_SWING_MIDDLE, STATE_SWING_END },
        { STATE_SWING_MIDDLE, STATE_SWING_END, STATE_SWING_MIDDLE, STATE_SWING_START,
          STATE_SWING_MIDDLE, STATE_SWING_END, STATE_SWING_MIDDLE, STATE_SWING_END },
        { STATE_SWING_MIDDLE, STATE_SWING_START, STATE_SWING_MIDDLE, STATE_SWING_END,
          STATE_SWING_MIDDLE, STATE_SWING_END, STATE_SWING_MIDDLE, STATE_SWING_END },
        { STATE_SWING_MIDDLE, STATE_SWING_END, STATE_SWING_MIDDLE, STATE_SWING_END,
          STATE_SWING_MIDDLE, STATE_SWING_START, STATE_SWING_MIDDLE, STATE_SWING_END },
    };
    // bank2.asm:46E9-4708. Indexed as (base hLinkDirection << 3) + spin sector.
    private static final int[][] SPIN_ABSOLUTE_DIRECTION_BY_BASE_DIRECTION = {
        { Link.DIRECTION_RIGHT, Link.DIRECTION_UP, Link.DIRECTION_UP, Link.DIRECTION_LEFT,
          Link.DIRECTION_LEFT, Link.DIRECTION_DOWN, Link.DIRECTION_DOWN, Link.DIRECTION_RIGHT },
        { Link.DIRECTION_LEFT, Link.DIRECTION_UP, Link.DIRECTION_UP, Link.DIRECTION_RIGHT,
          Link.DIRECTION_RIGHT, Link.DIRECTION_DOWN, Link.DIRECTION_DOWN, Link.DIRECTION_LEFT },
        { Link.DIRECTION_UP, Link.DIRECTION_RIGHT, Link.DIRECTION_RIGHT, Link.DIRECTION_DOWN,
          Link.DIRECTION_DOWN, Link.DIRECTION_LEFT, Link.DIRECTION_LEFT, Link.DIRECTION_UP },
        { Link.DIRECTION_DOWN, Link.DIRECTION_LEFT, Link.DIRECTION_LEFT, Link.DIRECTION_UP,
          Link.DIRECTION_UP, Link.DIRECTION_RIGHT, Link.DIRECTION_RIGHT, Link.DIRECTION_DOWN },
    };

    // Silver/steel palette for the normal sword blade. Color 0 is
    // transparent (OBJ convention). The original uses GBC OBJ palette 3.
    private static final int[] BLADE_PALETTE = {
        0x00000000,
        0x00101010,
        0x00787878,
        0x00F0F0F0,
    };
    // "Charged" palette — GBC branch in func_020_4AB3 swaps to OBJ palette 4
    // when fully charged. We approximate with a warmer, brighter tint.
    private static final int[] BLADE_PALETTE_CHARGED = {
        0x00000000,
        0x00201000,
        0x00A89068,
        0x00F8F8B8,
    };

    private final RomTables romTables;
    private final SwordSpriteSheet spriteSheet;

    private int state = STATE_NONE;
    private int timer;
    private int charge;
    private int holdingFrames; // frames spent in HOLDING (drives charge + flash phase)
    private int spinFramesRemaining;
    private int spinFrameCounter;
    private int spinSector;
    private int spinBaseDirection = Link.DIRECTION_DOWN;
    private int lastKnownDirection = Link.DIRECTION_DOWN;
    private boolean spinAttackQueued;
    private int queuedSpinDelayFrames;

    public Sword(RomTables romTables, SwordSpriteSheet spriteSheet) {
        this.romTables = romTables;
        this.spriteSheet = spriteSheet;
    }

    public int state() {
        if (spinAttackActive()) {
            return spinAnimationState();
        }
        return state;
    }

    public int charge() {
        return charge;
    }

    /**
     * Mirrors CheckStaticSwordCollision: HOLDING does not cut static objects,
     * while normal swings and active spin frames do.
     */
    public boolean staticCollisionActive() {
        return spinAttackActive() || (state != STATE_NONE && state != STATE_HOLDING);
    }

    @Override
    public void onPress() {
        if (state != STATE_NONE) {
            return;
        }
        state = STATE_DRAW;
        timer = DRAW_FRAMES;
        charge = 0;
        holdingFrames = 0;
    }

    @Override
    public void onRelease() {
        if (state == STATE_HOLDING) {
            if (charge >= MAX_CHARGE) {
                spinBaseDirection = lastKnownDirection;
                spinAttackQueued = true;
                queuedSpinDelayFrames = 1;
            } else {
                state = STATE_NONE;
            }
            charge = 0;
            holdingFrames = 0;
        }
    }

    @Override
    public void tick(boolean buttonHeld) {
        if (spinAttackQueued) {
            if (queuedSpinDelayFrames > 0) {
                queuedSpinDelayFrames--;
                return;
            }
            spinAttackQueued = false;
            spinFramesRemaining = SPIN_ATTACK_FRAMES;
            // The ROM also decrements on hFrameCounter % 4 == 0. Without a
            // shared frame counter here, start on the 25-frame visible cadence.
            spinFrameCounter = 1;
            advanceSpinAttackAnimation();
            return;
        }
        if (spinAttackActive()) {
            advanceSpinAttackAnimation();
            return;
        }
        switch (state) {
            case STATE_NONE:
                return;
            case STATE_DRAW:
                if (--timer <= 0) {
                    state = STATE_SWING_START;
                    timer = SWING_START_FRAMES;
                }
                return;
            case STATE_SWING_START:
                if (--timer <= 0) {
                    state = STATE_SWING_MIDDLE;
                    timer = SWING_MIDDLE_FRAMES;
                }
                return;
            case STATE_SWING_MIDDLE:
                if (--timer <= 0) {
                    state = buttonHeld ? STATE_HOLDING : STATE_NONE;
                    if (state == STATE_NONE) {
                        charge = 0;
                    }
                }
                return;
            case STATE_SWING_END:
                state = buttonHeld ? STATE_HOLDING : STATE_NONE;
                if (state == STATE_NONE) {
                    charge = 0;
                }
                return;
            case STATE_HOLDING:
                holdingFrames++;
                if (charge < MAX_CHARGE) {
                    charge++;
                }
                return;
            default:
                state = STATE_NONE;
        }
    }

    @Override
    public boolean blocksMotion() {
        return spinAttackActive() || (state != STATE_NONE && state != STATE_HOLDING);
    }

    @Override
    public boolean locksFacing() {
        return spinAttackQueued || spinAttackActive() || state != STATE_NONE;
    }

    @Override
    public int overrideAnimationState(int direction, int walkFrame) {
        lastKnownDirection = direction;
        if (state == STATE_NONE && !spinAttackActive()) {
            return -1;
        }
        int effectiveDirection = effectiveDirection(direction);
        int lookupState = effectiveLinkAnimationState();
        int value = romTables.swordDirectionAnimState(toRomDirection(effectiveDirection), lookupState);
        if (value == 0xFF) {
            return -1;
        }
        return value;
    }

    @Override
    public void render(byte[] displayBuffer, int linkPixelX, int linkPixelY,
                       int direction, int offsetX, int offsetY) {
        lastKnownDirection = direction;
        if (state == STATE_NONE && !spinAttackActive()) {
            return;
        }
        int effectiveDirection = effectiveDirection(direction);
        int romDir = toRomDirection(effectiveDirection);
        int lookupState = effectiveBladeRenderState();
        int swordDir = romTables.swordDirectionFor(romDir, lookupState);
        int bladeX = linkPixelX + romTables.swordBladeXOffset(romDir, lookupState) + offsetX;
        int bladeY = linkPixelY + romTables.swordBladeYOffset(romDir, lookupState) + offsetY;

        int[] palette = chargedFlashActive() ? BLADE_PALETTE_CHARGED : BLADE_PALETTE;

        // Two 8x16 stacked sprites side by side, matching func_020_4AB3.
        // Tile $FF for the left half means only the right half is drawn
        // (the vertical blade cases).
        int tile0 = romTables.swordSpriteTile(swordDir, 0);
        int tile1 = romTables.swordSpriteTile(swordDir, 1);
        int attr0 = romTables.swordSpriteAttr(swordDir, 0);
        int attr1 = romTables.swordSpriteAttr(swordDir, 1);
        if (tile0 != 0xFF) {
            draw8x16(displayBuffer, tile0, bladeX, bladeY, attr0, palette);
        }
        if (tile1 != 0xFF) {
            draw8x16(displayBuffer, tile1, bladeX + 8, bladeY, attr1, palette);
        }
    }

    public int attackDirection(int linkDirection) {
        lastKnownDirection = linkDirection;
        return effectiveDirection(linkDirection);
    }

    public int staticCollisionMapIndex(int linkDirection) {
        lastKnownDirection = linkDirection;
        if (!spinAttackActive()) {
            return toRomDirection(linkDirection);
        }
        int effectiveDirection = effectiveDirection(linkDirection);
        int romDirection = toRomDirection(effectiveDirection);
        int swordDirection = romTables.swordDirectionFor(romDirection, spinAnimationState());
        return 4 + (swordDirection & 0x07);
    }

    public boolean spinAttackActive() {
        return spinFramesRemaining > 0;
    }

    // Flash is driven by bit 2 of a per-frame counter during HOLDING at max
    // charge (bank0.asm:17BF computes `(hFrameCounter << 2) & $10` which
    // flips every 4 frames). We piggyback on holdingFrames for the phase.
    private boolean chargedFlashActive() {
        return state == STATE_HOLDING && charge >= MAX_CHARGE && (holdingFrames & 0x04) != 0;
    }

    private int effectiveLinkAnimationState() {
        if (spinAttackActive()) {
            return spinAnimationState();
        }
        // HOLDING maps to HIDDEN (0xFF) in the ROM for hLinkAnimationState,
        // meaning "keep the previous frame". Since the previous frame was
        // SWING_END's sword-extended pose, substitute that lookup directly.
        return state == STATE_HOLDING ? STATE_SWING_END : state;
    }

    private int effectiveBladeRenderState() {
        if (spinAttackActive()) {
            return spinAnimationState();
        }
        return state;
    }

    private int effectiveDirection(int direction) {
        if (!spinAttackActive()) {
            return direction;
        }
        return SPIN_ABSOLUTE_DIRECTION_BY_BASE_DIRECTION[toSpinDirectionIndex(spinBaseDirection)][spinSector()];
    }

    private int spinAnimationState() {
        return SPIN_ANIMATION_STATE_BY_BASE_DIRECTION[toSpinDirectionIndex(spinBaseDirection)][spinSector()];
    }

    private int spinSector() {
        return Math.min(7, Math.max(0, spinSector));
    }

    private void advanceSpinAttackAnimation() {
        spinFramesRemaining--;
        if (spinFramesRemaining <= 0) {
            endSpinAttack();
            return;
        }

        spinSector = spinFramesRemaining >> 2;
        state = spinAnimationState();

        if ((spinFrameCounter & 0x03) == 0) {
            spinFramesRemaining--;
            if (spinFramesRemaining <= 0) {
                endSpinAttack();
                return;
            }
        }
        spinFrameCounter++;
    }

    private void endSpinAttack() {
        spinFramesRemaining = 0;
        spinFrameCounter = 0;
        spinSector = 0;
        state = STATE_NONE;
    }

    private static int toSpinDirectionIndex(int javaDirection) {
        switch (javaDirection) {
            case Link.DIRECTION_RIGHT: return 0;
            case Link.DIRECTION_LEFT:  return 1;
            case Link.DIRECTION_UP:    return 2;
            case Link.DIRECTION_DOWN:  return 3;
            default: return 3;
        }
    }

    private void draw8x16(byte[] buffer, int tileIndex, int screenX, int screenY,
                          int attrs, int[] palette) {
        boolean flipX = (attrs & ATTR_FLIP_X) != 0;
        boolean flipY = (attrs & ATTR_FLIP_Y) != 0;
        // GB 8x16 sprite mode: tile T and tile T+1 stacked. flipY swaps
        // which tile is on top and flips each vertically.
        Tile top = spriteSheet.tile(flipY ? tileIndex + 1 : tileIndex);
        Tile bottom = spriteSheet.tile(flipY ? tileIndex : tileIndex + 1);
        drawTile(buffer, top,    screenX, screenY,     flipX, flipY, palette);
        drawTile(buffer, bottom, screenX, screenY + 8, flipX, flipY, palette);
    }

    private static void drawTile(byte[] buffer, Tile tile, int screenX, int screenY,
                                 boolean flipX, boolean flipY, int[] palette) {
        if (tile == null) {
            return;
        }
        for (int ty = 0; ty < 8; ty++) {
            for (int tx = 0; tx < 8; tx++) {
                int px = screenX + tx;
                int py = screenY + ty;
                if (px < 0 || px >= Framebuffer.WIDTH || py < 0 || py >= Framebuffer.HEIGHT) {
                    continue;
                }
                int sx = flipX ? 7 - tx : tx;
                int sy = flipY ? 7 - ty : ty;
                int colorIndex = tile.getPixel(sx, sy);
                if (colorIndex == 0) {
                    continue;
                }
                int color = palette[colorIndex];
                int offset = (py * Framebuffer.WIDTH + px) * 4;
                buffer[offset] = (byte) ((color >> 16) & 0xFF);
                buffer[offset + 1] = (byte) ((color >> 8) & 0xFF);
                buffer[offset + 2] = (byte) (color & 0xFF);
                buffer[offset + 3] = (byte) 0xFF;
            }
        }
    }

    private static int toRomDirection(int javaDirection) {
        switch (javaDirection) {
            case Link.DIRECTION_RIGHT: return 0;
            case Link.DIRECTION_LEFT:  return 1;
            case Link.DIRECTION_UP:    return 2;
            case Link.DIRECTION_DOWN:  return 3;
            default: return 3;
        }
    }
}
