package linksawakening.entity;

import linksawakening.equipment.EquippedItem;
import linksawakening.equipment.ItemRegistry;
import linksawakening.gpu.Framebuffer;
import linksawakening.gpu.Tile;
import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
import linksawakening.physics.OverworldCollision;
import linksawakening.rom.RomTables;
import linksawakening.state.PlayerState;

/**
 * Link's state and per-frame update, ported from the LADX disassembly's
 * {@code LinkMotionDefault} path (bank2.asm:161).
 *
 * <p>Position is stored in sub-pixels (1 pixel = 16 sub-pixels), matching the
 * scale of the original {@code HorizontalIncrementForLinkPosition} /
 * {@code VerticalIncrementForLinkPosition} tables.
 *
 * <p>The 16x16 AABB used for both rendering and collision has its origin at
 * Link's top-left.
 */
public final class Link {

    public static final int DIRECTION_DOWN = 0;
    public static final int DIRECTION_UP = 1;
    public static final int DIRECTION_LEFT = 2;
    public static final int DIRECTION_RIGHT = 3;

    public static final int SPRITE_SIZE = 16;
    public static final int SUB_PIXEL_SHIFT = 4;

    // Bit order matches JoypadToLinkDirection (bank2.asm:1183): entry 1 = right,
    // 2 = left, 4 = up, 8 = down.
    private static final int JOY_RIGHT = 1 << 0;
    private static final int JOY_LEFT = 1 << 1;
    private static final int JOY_UP = 1 << 2;
    private static final int JOY_DOWN = 1 << 3;

    // JoypadToLinkDirection table from bank2.asm:1183 — any two-axis combination
    // keeps the previous facing. Encoded as one of DIRECTION_* or -1 (= KEEP).
    private static final int[] JOYPAD_TO_DIRECTION = {
        -1,                 //  0000 none       -> keep
        DIRECTION_RIGHT,    //  0001 right
        DIRECTION_LEFT,     //  0010 left
        -1,                 //  0011 right+left -> keep
        DIRECTION_UP,       //  0100 up
        -1,                 //  0101 up+right   -> keep
        -1,                 //  0110 up+left    -> keep
        -1,                 //  0111 keep
        DIRECTION_DOWN,     //  1000 down
        -1,                 //  1001 down+right -> keep
        -1,                 //  1010 down+left  -> keep
        -1,                 //  1011 keep
        -1,                 //  1100 down+up    -> keep
        -1,                 //  1101 keep
        -1,                 //  1110 keep
        -1,                 //  1111 keep
    };

    // hLinkAnimationState values from gameplay.asm. Index = direction, inner
    // index = walking-frame bit (0 = standing, 1 = walking alt). These match
    // LinkAnimationsList_WalkingNoShield (bank2.asm:1208).
    private static final int[][] ANIMATION_STATE = {
        { 0x00, 0x01 },  // DOWN:  standing, walking
        { 0x04, 0x05 },  // UP
        { 0x06, 0x07 },  // LEFT
        { 0x0A, 0x0B },  // RIGHT
    };

    // Mirrors the first entry of ObjectPalettes (palettes.asm:1477) — the
    // "green objects" palette that covers Link, leaf particles, etc. OBJ
    // color 0 is transparent by hardware convention, so its RGB is unused.
    private static final int[] SPRITE_PALETTE = {
        0x00000000, // 0: transparent (OBJ convention; #F8F888 in ROM, never rendered)
        0x00000000, // 1: #000000 — outlines, hair
        0x0010A840, // 2: #10A840 — green tunic / hat
        0x00F8B888, // 3: #F8B888 — skin, highlights
    };

    private static final int WALK_FRAME_TICKS = 8;

    private final InputState inputState;
    private final InputConfig inputConfig;
    private final RomTables romTables;
    private final OverworldCollision collision;
    private final LinkSpriteSheet spriteSheet;
    private final PlayerState playerState;
    private final ItemRegistry itemRegistry;

    // Two collision check points per direction, as offsets from the
    // sprite's top-left. Ported from LinkCollisionPointsX/Y
    // (bank2.asm:6302-6319). Only the leading edge of movement is
    // checked, which is why Link can walk out of a "stuck in tree row"
    // position by pressing UP (top-edge points are in the clear row)
    // even though his feet are still overlapping a solid cell below.
    //
    // Indexed by DIRECTION_* constants below.
    private static final int[][] COLLISION_POINTS_X = {
        { 6, 9 },   // DOWN
        { 6, 9 },   // UP
        { 4, 4 },   // LEFT
        { 11, 11 }, // RIGHT
    };
    private static final int[][] COLLISION_POINTS_Y = {
        { 15, 15 }, // DOWN — bottom edge
        { 6,  6 },  // UP — top edge (not 0; matches LA's offset)
        { 9,  12 }, // LEFT — middle-left
        { 9,  12 }, // RIGHT — middle-right
    };

    private int subX;
    private int subY;
    private int direction = DIRECTION_DOWN;
    private int walkTickCounter;
    private int walkFrame;
    private boolean movingThisFrame;
    private int collisionIgnoreFramesRemaining;
    private final Tile[] composedTiles = new Tile[4];

    public Link(InputState inputState,
                InputConfig inputConfig,
                RomTables romTables,
                OverworldCollision collision,
                LinkSpriteSheet spriteSheet,
                PlayerState playerState,
                ItemRegistry itemRegistry) {
        this.inputState = inputState;
        this.inputConfig = inputConfig;
        this.romTables = romTables;
        this.collision = collision;
        this.spriteSheet = spriteSheet;
        this.playerState = playerState;
        this.itemRegistry = itemRegistry;
    }

    public void setPixelPosition(int pixelX, int pixelY) {
        subX = pixelX << SUB_PIXEL_SHIFT;
        subY = pixelY << SUB_PIXEL_SHIFT;
    }

    /**
     * Ignore collision on Link's next {@code frames} movement updates. Used
     * after a warp so that Link can walk off his entrance tile even though
     * the door/stairs object beneath him is flagged SOLID — the LADX
     * disassembly uses {@code wIgnoreLinkCollisionsCountdown} for this
     * (bank2.asm:589).
     */
    public void setCollisionIgnoreFrames(int frames) {
        collisionIgnoreFramesRemaining = Math.max(0, frames);
    }

    public int pixelX() {
        return subX >> SUB_PIXEL_SHIFT;
    }

    public int pixelY() {
        return subY >> SUB_PIXEL_SHIFT;
    }

    public int direction() {
        return direction;
    }

    /** Advance the walking-cycle timer without processing input or collision. */
    public void tickAnimation() {
        walkTickCounter++;
        if (walkTickCounter >= WALK_FRAME_TICKS) {
            walkTickCounter = 0;
            walkFrame ^= 1;
        }
    }

    public void update() {
        int mask = buildJoypadMask();
        int newDirection = JOYPAD_TO_DIRECTION[mask];
        if (newDirection != -1 && !itemsLockFacing()) {
            direction = newDirection;
        }

        // Equipped items (sword swing, etc.) can block motion for a window of
        // frames. Mirrors the disassembly's hLinkInteractiveMotionBlocked flag
        // set by UpdateLinkAnimation while wSwordAnimationState != NONE. The
        // ROM still refreshes hLinkDirection from held single-axis input before
        // this early-out path, so direction update must happen above.
        if (itemsBlockMotion()) {
            movingThisFrame = false;
            walkTickCounter = 0;
            walkFrame = 0;
            if (collisionIgnoreFramesRemaining > 0) {
                collisionIgnoreFramesRemaining--;
            }
            return;
        }

        int speedX = (byte) romTables.linkSpeedX(mask);
        int speedY = (byte) romTables.linkSpeedY(mask);
        movingThisFrame = (speedX != 0 || speedY != 0);

        if (speedX != 0) {
            tryMoveAxis(speedX, true);
        }
        if (speedY != 0) {
            tryMoveAxis(speedY, false);
        }

        if (collisionIgnoreFramesRemaining > 0) {
            collisionIgnoreFramesRemaining--;
        }

        if (movingThisFrame) {
            walkTickCounter++;
            if (walkTickCounter >= WALK_FRAME_TICKS) {
                walkTickCounter = 0;
                walkFrame ^= 1;
            }
        } else {
            walkTickCounter = 0;
            walkFrame = 0;
        }
    }

    private boolean itemsBlockMotion() {
        if (itemRegistry == null || playerState == null) {
            return false;
        }
        EquippedItem a = itemRegistry.lookup(playerState.itemA());
        if (a != null && a.blocksMotion()) {
            return true;
        }
        EquippedItem b = itemRegistry.lookup(playerState.itemB());
        return b != null && b.blocksMotion();
    }

    private boolean itemsLockFacing() {
        if (itemRegistry == null || playerState == null) {
            return false;
        }
        EquippedItem a = itemRegistry.lookup(playerState.itemA());
        if (a != null && a.locksFacing()) {
            return true;
        }
        EquippedItem b = itemRegistry.lookup(playerState.itemB());
        return b != null && b.locksFacing();
    }

    private void tryMoveAxis(int signedSpeed, boolean isXAxis) {
        int candidateSubX = isXAxis ? subX + signedSpeed : subX;
        int candidateSubY = isXAxis ? subY : subY + signedSpeed;

        int candidatePixelX = candidateSubX >> SUB_PIXEL_SHIFT;
        int candidatePixelY = candidateSubY >> SUB_PIXEL_SHIFT;

        int moveDirection;
        if (isXAxis) {
            moveDirection = signedSpeed > 0 ? DIRECTION_RIGHT : DIRECTION_LEFT;
        } else {
            moveDirection = signedSpeed > 0 ? DIRECTION_DOWN : DIRECTION_UP;
        }

        if (collisionIgnoreFramesRemaining == 0
            && leadingEdgeBlocked(candidatePixelX, candidatePixelY, moveDirection)) {
            return;
        }

        subX = candidateSubX;
        subY = candidateSubY;
    }

    /**
     * Point-based collision check on the leading edge of movement,
     * mirroring {@code LinkCollisionPointsX/Y} (bank2.asm:6302). Tests
     * two specific points on Link's sprite for the given direction; if
     * either is in a blocked cell, the move is rejected. This is what
     * lets Link step up and out of a scrolled-into-a-tree-row
     * situation — his upper-edge collision points are in the clear row
     * above even while his feet still overlap the tree cells below.
     */
    private boolean leadingEdgeBlocked(int spriteX, int spriteY, int dir) {
        int[] xs = COLLISION_POINTS_X[dir];
        int[] ys = COLLISION_POINTS_Y[dir];
        for (int i = 0; i < xs.length; i++) {
            if (collision.pointBlocked(spriteX + xs[i], spriteY + ys[i])) {
                return true;
            }
        }
        return false;
    }

    private int buildJoypadMask() {
        int mask = 0;
        if (inputState.isDown(inputConfig.downKey())) mask |= JOY_DOWN;
        if (inputState.isDown(inputConfig.upKey())) mask |= JOY_UP;
        if (inputState.isDown(inputConfig.leftKey())) mask |= JOY_LEFT;
        if (inputState.isDown(inputConfig.rightKey())) mask |= JOY_RIGHT;
        return mask;
    }

    public void render(byte[] displayBuffer, int offsetX, int offsetY) {
        int animationState = resolveAnimationState();
        spriteSheet.resolveTiles(animationState, composedTiles);

        boolean leftFlipX = spriteSheet.leftHalfFlipX(animationState);
        boolean leftFlipY = spriteSheet.leftHalfFlipY(animationState);
        boolean rightFlipX = spriteSheet.rightHalfFlipX(animationState);
        boolean rightFlipY = spriteSheet.rightHalfFlipY(animationState);

        int originX = pixelX() + offsetX;
        int originY = pixelY() + offsetY;

        // Column-major tile layout: [0]=UL, [1]=LL, [2]=UR, [3]=LR.
        // GB 8x16 flipY swaps the two stacked tiles inside a column and flips
        // each vertically.
        Tile leftTop = composedTiles[leftFlipY ? 1 : 0];
        Tile leftBottom = composedTiles[leftFlipY ? 0 : 1];
        Tile rightTop = composedTiles[rightFlipY ? 3 : 2];
        Tile rightBottom = composedTiles[rightFlipY ? 2 : 3];

        drawTile(displayBuffer, leftTop,     originX,     originY,     leftFlipX,  leftFlipY);
        drawTile(displayBuffer, leftBottom,  originX,     originY + 8, leftFlipX,  leftFlipY);
        drawTile(displayBuffer, rightTop,    originX + 8, originY,     rightFlipX, rightFlipY);
        drawTile(displayBuffer, rightBottom, originX + 8, originY + 8, rightFlipX, rightFlipY);

        // Let equipped items paint their own sprites on top (e.g. the sword
        // blade during a swing). Matches the disassembly's ordering where
        // DrawLinkSprite writes Link's body to wLinkOAMBuffer slots 1-2 and
        // ApplyLinkMotionState/func_020_4AB3 writes the sword into slots 4-5.
        renderEquippedItems(displayBuffer, offsetX, offsetY);
    }

    private void renderEquippedItems(byte[] displayBuffer, int offsetX, int offsetY) {
        if (itemRegistry == null || playerState == null) {
            return;
        }
        EquippedItem a = itemRegistry.lookup(playerState.itemA());
        if (a != null) {
            a.render(displayBuffer, pixelX(), pixelY(), direction, offsetX, offsetY);
        }
        EquippedItem b = itemRegistry.lookup(playerState.itemB());
        if (b != null && b != a) {
            b.render(displayBuffer, pixelX(), pixelY(), direction, offsetX, offsetY);
        }
    }

    private int resolveAnimationState() {
        int baseState = ANIMATION_STATE[direction][walkFrame];
        if (itemRegistry == null || playerState == null) {
            return baseState;
        }
        int override = queryOverride(itemRegistry.lookup(playerState.itemA()));
        if (override >= 0) {
            return override;
        }
        override = queryOverride(itemRegistry.lookup(playerState.itemB()));
        if (override >= 0) {
            return override;
        }
        return baseState;
    }

    private int queryOverride(EquippedItem item) {
        if (item == null) {
            return -1;
        }
        return item.overrideAnimationState(direction, walkFrame);
    }

    private static void drawTile(byte[] buffer, Tile tile, int screenX, int screenY,
                                 boolean flipX, boolean flipY) {
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
                    continue; // transparent
                }
                int color = SPRITE_PALETTE[colorIndex];
                int offset = (py * Framebuffer.WIDTH + px) * 4;
                buffer[offset] = (byte) ((color >> 16) & 0xFF);
                buffer[offset + 1] = (byte) ((color >> 8) & 0xFF);
                buffer[offset + 2] = (byte) (color & 0xFF);
                buffer[offset + 3] = (byte) 0xFF;
            }
        }
    }
}
